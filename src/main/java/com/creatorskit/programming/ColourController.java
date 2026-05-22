package com.creatorskit.programming;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.swing.timesheet.keyframe.ColourKeyFrame;
import net.runelite.api.Model;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Per-Character lifecycle for {@link ColourKeyFrame}s.
 *
 * <p>The controller doesn't mutate any Model directly. Instead it installs
 * a {@link ColourTint} on each affected CKObject and updates the tint's
 * pristine baseline + per-tick blend factor. {@link CKObject#getModel}
 * then routes its post-animation output through the tint, which deep-
 * copies the per-frame animated mesh via {@code client.mergeModels} and
 * mutates the clone's face colours. This dodges two problems the earlier
 * "replace baseModel with a merged clone" approach had:
 *
 * <ul>
 *   <li><b>Animation freeze:</b> the merged clone carries no bone data,
 *   so {@code animationController.animate(baseModel)} produced no
 *   deformation while the Colour kf was active. The new approach keeps
 *   the engine's baseModel intact and only post-processes the animated
 *   output, so animation runs normally throughout the envelope.</li>
 *   <li><b>Cross-Character leak:</b> the shared baseModel is never
 *   written to. Only per-frame clones get tinted, and clones are
 *   per-CKObject by construction.</li>
 * </ul>
 *
 * <p>Pristine face-colour arrays are captured once per envelope start
 * (cloned from {@code baseModel.getFaceColors1/2/3}) and re-captured
 * when baseModel's reference changes -- e.g. a Model keyframe firing
 * mid-envelope swaps baseModel; we detect the swap on the next tick
 * and rebuild the baseline from the new model.
 */
@Singleton
public class ColourController
{
    /** Per-CKObject tint book-keeping: which Model the pristine baseline was captured from. */
    private static class Slot
    {
        Model baselineFromModel;  // identity of the baseModel that pristineFc came from
        ColourTint tint;          // the tint instance installed on the CKObject
    }

    /** Per-Character state -- which Colour kf is active and per-CKObject tint slots. */
    private static class State
    {
        ColourKeyFrame activeKf;
        Slot mainSlot;
        Slot sp1Slot;
        Slot sp2Slot;
    }

    private final ClientThread clientThread;
    private final Map<Character, State> states = new IdentityHashMap<>();

    @Inject
    public ColourController(ClientThread clientThread)
    {
        this.clientThread = clientThread;
    }

    /**
     * Drives the Colour-keyframe lifecycle for one Character. Called from
     * Programmer.updateProgram (scrub) and Programmer.updateProgramsOnTick
     * (play). Idempotent: re-runnable per tick with the same currentTime
     * and the tint stays in sync.
     */
    public synchronized void update(Character character, double currentTime)
    {
        if (character == null) return;
        State s = states.computeIfAbsent(character, c -> new State());

        ColourKeyFrame active = findActiveKf(character, currentTime);

        if (active == null)
        {
            // No Colour kf right now. If we had one going, clear the tints.
            if (s.activeKf != null)
            {
                final State sf = s;
                clientThread.invoke(() -> clearTints(character, sf));
                s.activeKf = null;
            }
            return;
        }

        // Active kf changed: drop the per-CKObject baselines so the next
        // tick re-captures from whatever model is currently on the CKObject.
        if (s.activeKf != active)
        {
            final State sf = s;
            clientThread.invoke(() -> clearTints(character, sf));
            s.activeKf = active;
        }

        final ColourKeyFrame kf = active;
        final double t = kf.computeBlendFactor(currentTime);
        clientThread.invoke(() ->
        {
            ensureTint(s, character.getCkObject(), kf, t);
            if (kf.isAffectSpotAnims())
            {
                ensureTintSlot(() -> s.sp1Slot, slot -> s.sp1Slot = slot, character.getSpotAnim1(), kf, t);
                ensureTintSlot(() -> s.sp2Slot, slot -> s.sp2Slot = slot, character.getSpotAnim2(), kf, t);
            }
            else
            {
                clearOneTint(character.getSpotAnim1(), s.sp1Slot);
                s.sp1Slot = null;
                clearOneTint(character.getSpotAnim2(), s.sp2Slot);
                s.sp2Slot = null;
            }
        });
    }

    /** Releases all tints + drops state. Call when the Character is removed. */
    public synchronized void release(Character character)
    {
        if (character == null) return;
        State s = states.remove(character);
        if (s != null)
        {
            clientThread.invoke(() -> clearTints(character, s));
        }
    }

    private ColourKeyFrame findActiveKf(Character character, double currentTime)
    {
        ColourKeyFrame[] kfs = character.getColourKeyFrames();
        if (kfs == null) return null;
        ColourKeyFrame best = null;
        for (ColourKeyFrame p : kfs)
        {
            if (p == null) continue;
            if (currentTime >= p.getTick() && currentTime <= p.getEndTick())
            {
                if (best == null || p.getTick() > best.getTick())
                {
                    best = p;
                }
            }
        }
        return best;
    }

    // --- Slot installation (client thread only) -----------------------------

    /** Convenience for the main slot (avoids the getter/setter pair). */
    private void ensureTint(State s, CKObject ck, ColourKeyFrame kf, double t)
    {
        s.mainSlot = ensureTintImpl(s.mainSlot, ck, kf, t);
    }

    /**
     * Variant for the spotanim slots -- they live on State fields whose
     * names differ, so the caller passes accessor lambdas.
     */
    private interface SlotGetter { Slot get(); }
    private interface SlotSetter { void set(Slot slot); }
    private void ensureTintSlot(SlotGetter getter, SlotSetter setter, CKObject ck, ColourKeyFrame kf, double t)
    {
        setter.set(ensureTintImpl(getter.get(), ck, kf, t));
    }

    /**
     * Builds or refreshes the per-CKObject tint slot:
     * <ul>
     *   <li>If the CKObject is null, no-op (nothing to render against).</li>
     *   <li>If baseModel is null, defer (engine's setModel hasn't fired yet).</li>
     *   <li>If baseModel changed since our baseline capture, re-snapshot.</li>
     *   <li>Always push the latest blend factor + target colour onto the
     *       tint -- this is what makes the envelope animate over time.</li>
     * </ul>
     */
    private Slot ensureTintImpl(Slot existing, CKObject ck, ColourKeyFrame kf, double t)
    {
        if (ck == null) return null;
        Model current = ck.getBaseModel();
        if (current == null)
        {
            // Engine hasn't loaded a model into this CKObject yet (the
            // plugin.setModel client-thread task may not have fired). Defer
            // until next tick. Common when the Character has no Model kf at
            // tick 0 and the manager-defined model is still mid-load.
            return existing;
        }

        Slot slot = existing;
        if (slot == null)
        {
            slot = new Slot();
            slot.tint = new ColourTint();
            ck.setColourTint(slot.tint);
        }

        // Re-snapshot pristine baseline if baseModel changed (Model kf
        // firing mid-envelope) or this is the first activation on this
        // CKObject after a clear.
        if (slot.baselineFromModel != current)
        {
            int[] fc1 = current.getFaceColors1();
            int[] fc2 = current.getFaceColors2();
            int[] fc3 = current.getFaceColors3();
            slot.tint.setPristine(
                    fc1 == null ? null : fc1.clone(),
                    fc2 == null ? null : fc2.clone(),
                    fc3 == null ? null : fc3.clone());
            slot.baselineFromModel = current;
        }

        slot.tint.setBlend(kf.getColorRgb(), kf.getBlendMode(), t);
        // Make sure the CKObject still references our tint (e.g. after a
        // Model kf path that didn't touch CKObject.colourTint, but in
        // theory could have via some future engine path).
        if (ck.getColourTint() != slot.tint)
        {
            ck.setColourTint(slot.tint);
        }
        return slot;
    }

    private void clearOneTint(CKObject ck, Slot slot)
    {
        if (ck != null && slot != null && ck.getColourTint() == slot.tint)
        {
            ck.setColourTint(null);
        }
    }

    private void clearTints(Character character, State s)
    {
        clearOneTint(character.getCkObject(), s.mainSlot);
        s.mainSlot = null;
        clearOneTint(character.getSpotAnim1(), s.sp1Slot);
        s.sp1Slot = null;
        clearOneTint(character.getSpotAnim2(), s.sp2Slot);
        s.sp2Slot = null;
    }
}
