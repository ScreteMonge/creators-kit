package com.creatorskit.programming;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.swing.timesheet.keyframe.ColourBlendMode;
import com.creatorskit.swing.timesheet.keyframe.ColourKeyFrame;
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
 * <p>On every Programmer tick {@link #update(Character, double)} is called.
 * The controller finds the latest-starting Colour keyframe whose envelope
 * window contains the current tick, snapshots the underlying model's face
 * colours on first activation (and re-snapshots if a Model keyframe
 * swaps the model mid-envelope), then mutates those face colour arrays
 * in-place toward the target colour. Outside the envelope the snapshot
 * is restored and discarded so the model is unaffected when no Colour kf
 * is playing.
 *
 * <p>Blending is done in HSL space directly on the packed-short colours
 * the renderer reads -- much faster than RGB roundtrips, and visually
 * close enough for the short envelopes the user authors (fade-in / hold
 * / fade-out measured in ticks, not seconds).
 */
@Singleton
public class ColourController
{
    /** Captured face-colour state for one Model so it can be reverted on envelope end. */
    private static class Snapshot
    {
        final Model model;
        final int[] face1, face2, face3;

        Snapshot(Model m)
        {
            this.model = m;
            int[] f1 = m.getFaceColors1();
            int[] f2 = m.getFaceColors2();
            int[] f3 = m.getFaceColors3();
            this.face1 = f1 == null ? null : f1.clone();
            this.face2 = f2 == null ? null : f2.clone();
            this.face3 = f3 == null ? null : f3.clone();
        }

        void restore()
        {
            if (model == null) return;
            int[] f1 = model.getFaceColors1();
            int[] f2 = model.getFaceColors2();
            int[] f3 = model.getFaceColors3();
            if (face1 != null && f1 != null && f1.length == face1.length)
            {
                System.arraycopy(face1, 0, f1, 0, face1.length);
            }
            if (face2 != null && f2 != null && f2.length == face2.length)
            {
                System.arraycopy(face2, 0, f2, 0, face2.length);
            }
            if (face3 != null && f3 != null && f3.length == face3.length)
            {
                System.arraycopy(face3, 0, f3, 0, face3.length);
            }
        }
    }

    /** Per-Character state -- which Colour kf is active and what we snapshotted for it. */
    private static class State
    {
        ColourKeyFrame activeKf;
        Snapshot mainSnapshot;
        Snapshot sp1Snapshot;
        Snapshot sp2Snapshot;
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
     * (play). Idempotent: calling repeatedly with the same currentTime
     * re-applies the same tint, so a Model keyframe that swaps the
     * underlying model picks the envelope back up on the next update tick.
     */
    public synchronized void update(Character character, double currentTime)
    {
        if (character == null) return;
        State s = states.computeIfAbsent(character, c -> new State());

        ColourKeyFrame active = findActiveKf(character, currentTime);

        if (active == null)
        {
            // Transitioning out of any Colour kf -- restore and forget.
            if (s.activeKf != null)
            {
                final State sf = s;
                clientThread.invoke(() -> restoreAll(sf));
                s.activeKf = null;
            }
            return;
        }

        // Active kf changed (or first activation): drop old snapshot, capture new.
        if (s.activeKf != active)
        {
            final State sf = s;
            clientThread.invoke(() -> restoreAll(sf));
            s.activeKf = active;
            s.mainSnapshot = null;
            s.sp1Snapshot = null;
            s.sp2Snapshot = null;
        }

        final ColourKeyFrame kf = active;
        final double tFinal = kf.computeBlendFactor(currentTime);
        clientThread.invoke(() ->
        {
            // Re-snapshot whenever the underlying model identity changed (e.g. a Model
            // keyframe swapped baseModel mid-envelope). The == check is intentional --
            // Model instances are reference-identity per CKObject.setModel call.
            ensureSnapshotFor(s, character, kf);

            if (tFinal <= 0)
            {
                // Inside the envelope but at the very edges -- treat as restored.
                restoreAll(s);
                return;
            }

            applyToSnapshot(s.mainSnapshot, kf, tFinal);
            if (kf.isAffectSpotAnims())
            {
                applyToSnapshot(s.sp1Snapshot, kf, tFinal);
                applyToSnapshot(s.sp2Snapshot, kf, tFinal);
            }
        });
    }

    /**
     * Restores every snapshot we hold for {@code character} and drops the
     * State entry. Call when the Character is being removed so we don't
     * leak references to its CKObject's models.
     */
    public synchronized void release(Character character)
    {
        if (character == null) return;
        State s = states.remove(character);
        if (s != null)
        {
            clientThread.invoke(() -> restoreAll(s));
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

    private void ensureSnapshotFor(State s, Character character, ColourKeyFrame kf)
    {
        CKObject main = character.getCkObject();
        Model mainModel = main == null ? null : main.getBaseModel();
        if (mainModel != null && (s.mainSnapshot == null || s.mainSnapshot.model != mainModel))
        {
            if (s.mainSnapshot != null) s.mainSnapshot.restore();
            s.mainSnapshot = new Snapshot(mainModel);
        }
        else if (mainModel == null && s.mainSnapshot != null)
        {
            // Model has been cleared out from under us. Drop the stale snapshot
            // so the next valid model load triggers a fresh capture.
            s.mainSnapshot = null;
        }

        if (kf.isAffectSpotAnims())
        {
            s.sp1Snapshot = refreshOrCapture(s.sp1Snapshot, character.getSpotAnim1());
            s.sp2Snapshot = refreshOrCapture(s.sp2Snapshot, character.getSpotAnim2());
        }
        else
        {
            // affectSpotAnims toggled off mid-envelope -- release any held spotanim snapshots.
            if (s.sp1Snapshot != null) { s.sp1Snapshot.restore(); s.sp1Snapshot = null; }
            if (s.sp2Snapshot != null) { s.sp2Snapshot.restore(); s.sp2Snapshot = null; }
        }
    }

    private Snapshot refreshOrCapture(Snapshot existing, CKObject ck)
    {
        if (ck == null)
        {
            if (existing != null) existing.restore();
            return null;
        }
        Model m = ck.getBaseModel();
        if (m == null)
        {
            if (existing != null) existing.restore();
            return null;
        }
        if (existing != null && existing.model == m) return existing;
        if (existing != null) existing.restore();
        return new Snapshot(m);
    }

    private void restoreAll(State s)
    {
        if (s.mainSnapshot != null) { s.mainSnapshot.restore(); s.mainSnapshot = null; }
        if (s.sp1Snapshot != null) { s.sp1Snapshot.restore(); s.sp1Snapshot = null; }
        if (s.sp2Snapshot != null) { s.sp2Snapshot.restore(); s.sp2Snapshot = null; }
    }

    private void applyToSnapshot(Snapshot snap, ColourKeyFrame kf, double t)
    {
        if (snap == null || snap.model == null) return;
        int[] f1 = snap.model.getFaceColors1();
        int[] f2 = snap.model.getFaceColors2();
        int[] f3 = snap.model.getFaceColors3();
        if (f1 == null || snap.face1 == null) return;

        ColourBlendMode mode = kf.getBlendMode() == null ? ColourBlendMode.ADD : kf.getBlendMode();
        // Pack the user's RGB into the engine's HSL space once -- per-face we only
        // do cheap int unpacks + per-channel lerps. Brightness 1.0 keeps the
        // gamma-correction pass that rgbToHSL applies neutral.
        short targetHsl = JagexColor.rgbToHSL(kf.getColorRgb() & 0xFFFFFF, 1.0);
        int targetH = JagexColor.unpackHue(targetHsl);
        int targetS = JagexColor.unpackSaturation(targetHsl);
        int targetL = JagexColor.unpackLuminance(targetHsl);

        blendArray(f1, snap.face1, targetH, targetS, targetL, t, mode);
        if (f2 != null && snap.face2 != null) blendArray(f2, snap.face2, targetH, targetS, targetL, t, mode);
        if (f3 != null && snap.face3 != null) blendArray(f3, snap.face3, targetH, targetS, targetL, t, mode);
    }

    private static void blendArray(int[] dest, int[] original, int tH, int tS, int tL, double t, ColourBlendMode mode)
    {
        // The renderer reads face colour as an HSL-packed short stored in the lower
        // 16 bits of each int (upper bits can carry texture / shading flags --
        // preserve those by masking).
        int upperMask = 0xFFFF0000;
        for (int i = 0; i < dest.length; i++)
        {
            int orig = original[i];
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
