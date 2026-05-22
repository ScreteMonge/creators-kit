package com.creatorskit.programming;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.swing.timesheet.keyframe.ColourBlendMode;
import com.creatorskit.swing.timesheet.keyframe.ColourKeyFrame;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Per-Character lifecycle for {@link ColourKeyFrame}s.
 *
 * <p><b>Why this isn't just "mutate face colours":</b> CKObject.setModel
 * keeps a direct reference to whatever the engine passed in
 * (CustomModel.getModel() / client.loadModel(id)) -- those Model instances
 * are shared across every Character that happens to use the same model.
 * Mutating that shared model's getFaceColors1/2/3 arrays leaks the tint
 * to every other Character pointing at the same Model. The
 * controller's job is therefore in three parts:
 *
 * <ol>
 *   <li>On envelope activation, take a per-CKObject deep copy of the
 *   currently-rendered Model via {@code client.mergeModels(new Model[]{shared}, 1)}
 *   -- the same pattern ProjectileCKObject uses to escape the shared-buffer
 *   problem (see {@code ProjectileCKObject.java}). Swap the CKObject's
 *   model reference to the owned copy.</li>
 *
 *   <li>Snapshot the owned copy's face-colour arrays as the pristine
 *   baseline. Each tick during the envelope, lerp from baseline toward
 *   the target colour and write the result into the owned copy in place.
 *   Because the copy is private, no other Character sees the mutation.</li>
 *
 *   <li>On envelope end (or active-kf change), restore the CKObject's
 *   model reference back to the engine's original instance and drop our
 *   owned copy. If the engine has separately replaced the CKObject's
 *   model mid-envelope (a Model keyframe firing), don't fight it -- just
 *   take fresh ownership of whatever the engine put there on the next
 *   tick.</li>
 * </ol>
 *
 * <p>Blending math is unchanged: per-face HSL lerp directly on the
 * packed-short colours the renderer reads.
 */
@Singleton
public class ColourController
{
    /**
     * Per-CKObject "we own this Model now" record. Built on first
     * activation, swapped onto the CKObject, mutated in-place each tick.
     */
    private static class OwnedSlot
    {
        /** The engine's Model reference at the moment we took over. Restored on release. */
        final Model engineRef;
        /** Our deep-copy of {@link #engineRef}. The CKObject's setModel is pointed at this. */
        final Model owned;
        /** Pristine face colours of {@link #owned}, captured before any tint. */
        final int[] pristineFc1, pristineFc2, pristineFc3;

        OwnedSlot(Model engineRef, Model owned)
        {
            this.engineRef = engineRef;
            this.owned = owned;
            int[] f1 = owned.getFaceColors1();
            int[] f2 = owned.getFaceColors2();
            int[] f3 = owned.getFaceColors3();
            this.pristineFc1 = f1 == null ? null : f1.clone();
            this.pristineFc2 = f2 == null ? null : f2.clone();
            this.pristineFc3 = f3 == null ? null : f3.clone();
        }
    }

    /** Per-Character state -- which Colour kf is active and what we own for it. */
    private static class State
    {
        ColourKeyFrame activeKf;
        OwnedSlot mainSlot;
        OwnedSlot sp1Slot;
        OwnedSlot sp2Slot;
    }

    private final Client client;
    private final ClientThread clientThread;
    private final Map<Character, State> states = new IdentityHashMap<>();

    @Inject
    public ColourController(Client client, ClientThread clientThread)
    {
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Drives the Colour-keyframe lifecycle for one Character. Called from
     * Programmer.updateProgram (scrub) and Programmer.updateProgramsOnTick
     * (play). Idempotent and safe to call when no Colour kf is active --
     * just no-ops in that case after the cleanup pass.
     */
    public synchronized void update(Character character, double currentTime)
    {
        if (character == null) return;
        State s = states.computeIfAbsent(character, c -> new State());

        ColourKeyFrame active = findActiveKf(character, currentTime);

        if (active == null)
        {
            // No Colour kf active right now. If we had one going, release the
            // owned copies and put the engine's Models back on the CKObjects.
            if (s.activeKf != null)
            {
                final State sf = s;
                clientThread.invoke(() -> releaseSlots(character, sf));
                s.activeKf = null;
            }
            return;
        }

        // Active kf changed (or first activation): release whatever we owned for
        // the previous kf so the next tick takes fresh ownership against the
        // engine's current model state.
        if (s.activeKf != active)
        {
            final State sf = s;
            clientThread.invoke(() -> releaseSlots(character, sf));
            s.activeKf = active;
        }

        final ColourKeyFrame kf = active;
        final double t = kf.computeBlendFactor(currentTime);

        clientThread.invoke(() ->
        {
            // Ensure we have ownership of the right CKObject Models. If the
            // engine swapped a model out from under us (e.g. a Model keyframe
            // fired and called setModel(newShared)), that gets detected here
            // and we take fresh ownership of the new model.
            ensureOwned(s, character, kf);

            if (t <= 0)
            {
                // Inside the envelope but at the very edges -- restore the
                // owned copy's face colours to pristine. We keep ownership of
                // the copy in place because the envelope isn't over yet.
                restorePristine(s.mainSlot);
                if (kf.isAffectSpotAnims())
                {
                    restorePristine(s.sp1Slot);
                    restorePristine(s.sp2Slot);
                }
                return;
            }

            applyTint(s.mainSlot, kf, t);
            if (kf.isAffectSpotAnims())
            {
                applyTint(s.sp1Slot, kf, t);
                applyTint(s.sp2Slot, kf, t);
            }
        });
    }

    /**
     * Restores every CKObject we own for {@code character} and drops the
     * State entry. Call when the Character is being removed so we don't
     * leak references to its CKObject's models.
     */
    public synchronized void release(Character character)
    {
        if (character == null) return;
        State s = states.remove(character);
        if (s != null)
        {
            clientThread.invoke(() -> releaseSlots(character, s));
        }
    }

    /**
     * Selects the Colour kf that should be "playing" at {@code currentTime}.
     * Latest start wins -- mirrors how a layered audio mix would interpret
     * overlapping clips, and matches the user's intuition when two
     * envelopes touch (the newer one takes over).
     */
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

    // --- Slot management (client-thread only) -----------------------------

    private void ensureOwned(State s, Character character, ColourKeyFrame kf)
    {
        s.mainSlot = ensureSlotFor(s.mainSlot, character.getCkObject());
        if (kf.isAffectSpotAnims())
        {
            s.sp1Slot = ensureSlotFor(s.sp1Slot, character.getSpotAnim1());
            s.sp2Slot = ensureSlotFor(s.sp2Slot, character.getSpotAnim2());
        }
        else
        {
            // affectSpotAnims toggled off mid-envelope -- give the spotanim
            // CKObjects' engine refs back.
            s.sp1Slot = releaseSlotFor(s.sp1Slot, character.getSpotAnim1());
            s.sp2Slot = releaseSlotFor(s.sp2Slot, character.getSpotAnim2());
        }
    }

    /**
     * Either returns the existing slot (if we still own the CKObject's
     * current model) or takes fresh ownership of whatever the engine has
     * put there. Returns null and defers when no model is loaded yet --
     * the next tick will retry.
     */
    private OwnedSlot ensureSlotFor(OwnedSlot existing, CKObject ck)
    {
        if (ck == null)
        {
            // CKObject disappeared on us; can't restore (nowhere to put it back).
            return null;
        }

        Model current = ck.getBaseModel();
        if (current == null)
        {
            // Engine hasn't loaded a model into this CKObject yet (the
            // plugin.setModel client-thread task may not have fired). Defer
            // until next tick. This is the "no Model keyframe but the
            // manager-defined model is loading" case the user described --
            // the load finishes within a tick or two and we pick up then.
            return existing;
        }

        // Still in control: the CKObject is rendering OUR owned copy.
        if (existing != null && existing.owned == current)
        {
            return existing;
        }

        // The engine has placed a different Model on this CKObject. Could be:
        //   - first activation (existing == null), or
        //   - a Model keyframe firing mid-envelope and overwriting our setModel.
        // In either case, take fresh ownership of the new shared model. Don't
        // try to "restore" the OLD existing.engineRef -- the engine wants
        // `current`, and we'd undo the engine's intent.
        Model owned = client.mergeModels(new Model[]{current}, 1);
        if (owned == null)
        {
            // mergeModels failed (e.g. mid-load). Skip the tint this tick;
            // try again next tick.
            return existing;
        }

        OwnedSlot slot = new OwnedSlot(current, owned);
        ck.setModel(owned);
        return slot;
    }

    /**
     * Reverses {@link #ensureSlotFor}: puts the engine's Model reference
     * back on the CKObject and drops our owned copy. No-op if the engine
     * has separately replaced the CKObject's model since we took ownership
     * (we don't want to fight an in-flight Model keyframe).
     */
    private OwnedSlot releaseSlotFor(OwnedSlot slot, CKObject ck)
    {
        if (slot == null) return null;
        if (ck != null && ck.getBaseModel() == slot.owned)
        {
            ck.setModel(slot.engineRef);
        }
        return null;
    }

    private void releaseSlots(Character character, State s)
    {
        s.mainSlot = releaseSlotFor(s.mainSlot, character.getCkObject());
        s.sp1Slot = releaseSlotFor(s.sp1Slot, character.getSpotAnim1());
        s.sp2Slot = releaseSlotFor(s.sp2Slot, character.getSpotAnim2());
    }

    /** Copies pristine baselines back into the owned copy's face colour arrays. */
    private void restorePristine(OwnedSlot slot)
    {
        if (slot == null || slot.owned == null) return;
        int[] f1 = slot.owned.getFaceColors1();
        int[] f2 = slot.owned.getFaceColors2();
        int[] f3 = slot.owned.getFaceColors3();
        if (slot.pristineFc1 != null && f1 != null && f1.length == slot.pristineFc1.length)
        {
            System.arraycopy(slot.pristineFc1, 0, f1, 0, slot.pristineFc1.length);
        }
        if (slot.pristineFc2 != null && f2 != null && f2.length == slot.pristineFc2.length)
        {
            System.arraycopy(slot.pristineFc2, 0, f2, 0, slot.pristineFc2.length);
        }
        if (slot.pristineFc3 != null && f3 != null && f3.length == slot.pristineFc3.length)
        {
            System.arraycopy(slot.pristineFc3, 0, f3, 0, slot.pristineFc3.length);
        }
    }

    private void applyTint(OwnedSlot slot, ColourKeyFrame kf, double t)
    {
        if (slot == null || slot.owned == null) return;
        int[] f1 = slot.owned.getFaceColors1();
        int[] f2 = slot.owned.getFaceColors2();
        int[] f3 = slot.owned.getFaceColors3();
        if (f1 == null || slot.pristineFc1 == null) return;

        ColourBlendMode mode = kf.getBlendMode() == null ? ColourBlendMode.ADD : kf.getBlendMode();
        // Pack the user's RGB into the engine's HSL space once -- per-face we only
        // do cheap int unpacks + per-channel lerps. Brightness 1.0 keeps the
        // gamma-correction pass that rgbToHSL applies neutral.
        short targetHsl = JagexColor.rgbToHSL(kf.getColorRgb() & 0xFFFFFF, 1.0);
        int targetH = JagexColor.unpackHue(targetHsl);
        int targetS = JagexColor.unpackSaturation(targetHsl);
        int targetL = JagexColor.unpackLuminance(targetHsl);

        blendArray(f1, slot.pristineFc1, targetH, targetS, targetL, t, mode);
        if (f2 != null && slot.pristineFc2 != null) blendArray(f2, slot.pristineFc2, targetH, targetS, targetL, t, mode);
        if (f3 != null && slot.pristineFc3 != null) blendArray(f3, slot.pristineFc3, targetH, targetS, targetL, t, mode);
    }

    private static void blendArray(int[] dest, int[] pristine, int tH, int tS, int tL, double t, ColourBlendMode mode)
    {
        // Face colours are HSL-packed shorts stored in the lower 16 bits of each
        // int (upper bits can carry texture / shading flags -- preserve those).
        int upperMask = 0xFFFF0000;
        for (int i = 0; i < dest.length; i++)
        {
            int orig = pristine[i];
            int origPacked = orig & 0xFFFF;
            int oH = (origPacked >> 10) & JagexColor.HUE_MAX;
            int oS = (origPacked >> 7) & JagexColor.SATURATION_MAX;
            int oL = origPacked & JagexColor.LUMINANCE_MAX;

            int nH, nS, nL;
            switch (mode)
            {
                case REPLACE:
                    nH = lerpHue(oH, tH, t);
                    nS = lerpInt(oS, tS, t);
                    nL = lerpInt(oL, tL, t);
                    break;
                case MULTIPLY:
                    // Hue / sat ease toward target; luminance multiplies down toward (orig * target / MAX).
                    int mulL = (oL * tL) / JagexColor.LUMINANCE_MAX;
                    nH = lerpHue(oH, tH, t);
                    nS = lerpInt(oS, tS, t);
                    nL = lerpInt(oL, mulL, t);
                    break;
                default: // ADD / screen
                    // Hue / sat ease toward target; luminance brightens toward
                    // "screen" composite: orig + (MAX - orig) * (target / MAX).
                    int addL = oL + ((JagexColor.LUMINANCE_MAX - oL) * tL) / JagexColor.LUMINANCE_MAX;
                    nH = lerpHue(oH, tH, t);
                    nS = lerpInt(oS, tS, t);
                    nL = lerpInt(oL, addL, t);
            }

            int newPacked = ((nH & JagexColor.HUE_MAX) << 10)
                    | ((nS & JagexColor.SATURATION_MAX) << 7)
                    | (nL & JagexColor.LUMINANCE_MAX);
            dest[i] = (orig & upperMask) | (newPacked & 0xFFFF);
        }
    }

    private static int lerpInt(int a, int b, double t)
    {
        return (int) Math.round(a + (b - a) * t);
    }

    /** Hue is circular on a 0..HUE_MAX wheel; pick the short way around. */
    private static int lerpHue(int a, int b, double t)
    {
        int range = JagexColor.HUE_MAX + 1;
        int diff = b - a;
        if (diff > range / 2) diff -= range;
        else if (diff < -range / 2) diff += range;
        int v = (int) Math.round(a + diff * t);
        v %= range;
        if (v < 0) v += range;
        return v;
    }
}
