package com.creatorskit.programming;

import com.creatorskit.swing.timesheet.keyframe.ColourBlendMode;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;

/**
 * Per-frame colour tint applied at the END of {@link com.creatorskit.CKObject#getModel}
 * (after the engine's animation pipeline has produced the per-frame deformed
 * mesh). The earlier approach -- {@code CKObject.setModel(mergedClone)} on
 * envelope start -- killed animations because the merged Model carries no
 * bone / animation data for {@code animationController.animate} to read.
 *
 * <p>This pattern dodges that by cloning the POST-animation frame instead,
 * the same way {@link com.creatorskit.ProjectileCKObject} clones for pitch.
 * The animation pipeline still runs against the engine-owned baseModel
 * (full bone data intact), then we deep-copy the resulting one-frame
 * mesh via {@code client.mergeModels(new Model[]{animatedFrame}, 1)} and
 * mutate the clone's face-colour arrays. The mutation never touches the
 * shared baseModel, so other Characters pointing at the same Model see
 * nothing.
 *
 * <p>Pristine face-colour baselines are captured once by the controller
 * at envelope activation (from {@code baseModel.getFaceColors1/2/3}) and
 * re-snapshotted whenever baseModel's reference changes (e.g. a Model
 * keyframe fires mid-envelope). Per-tick only {@link #setBlend} is
 * updated -- pristine arrays stay stable across ticks.
 *
 * <p>Mutable from the controller (EDT / programmer tick path) and read
 * from the render thread. Writes are coarse field assignments; volatile
 * keeps cross-thread visibility correct without a full lock per frame.
 */
public class ColourTint
{
    private volatile int[] pristineFc1;
    private volatile int[] pristineFc2;
    private volatile int[] pristineFc3;
    private volatile int targetH;
    private volatile int targetS;
    private volatile int targetL;
    private volatile double t;
    private volatile ColourBlendMode mode;

    public ColourTint()
    {
        this.mode = ColourBlendMode.ADD;
    }

    /**
     * Replaces the pristine baseline. Pass cloned arrays -- this class stores
     * them by reference (no defensive copy) since it only reads them per-frame.
     */
    public void setPristine(int[] fc1, int[] fc2, int[] fc3)
    {
        this.pristineFc1 = fc1;
        this.pristineFc2 = fc2;
        this.pristineFc3 = fc3;
    }

    /**
     * Per-tick update: pack the user's RGB into engine HSL once, store the
     * current envelope t-factor and blend mode. Cheap -- just 4 field
     * writes, no per-face work here.
     */
    public void setBlend(int rgb, ColourBlendMode mode, double t)
    {
        short hsl = JagexColor.rgbToHSL(rgb & 0xFFFFFF, 1.0);
        this.targetH = JagexColor.unpackHue(hsl);
        this.targetS = JagexColor.unpackSaturation(hsl);
        this.targetL = JagexColor.unpackLuminance(hsl);
        this.mode = mode == null ? ColourBlendMode.ADD : mode;
        this.t = t;
    }

    /**
     * Hook called from {@link com.creatorskit.CKObject#getModel} after the
     * animation pipeline. Returns a tinted clone of {@code animatedFrame},
     * or null if the clone failed (caller falls back to the original frame
     * so rendering doesn't drop).
     */
    public Model applyToFrame(Client client, Model animatedFrame)
    {
        if (client == null || animatedFrame == null) return null;
        if (pristineFc1 == null || t <= 0) return null;  // no tint to apply

        Model owned = client.mergeModels(new Model[]{animatedFrame}, 1);
        if (owned == null) return null;

        int[] fc1 = owned.getFaceColors1();
        int[] fc2 = owned.getFaceColors2();
        int[] fc3 = owned.getFaceColors3();

        // Defensive length check -- if the engine reshapes the model
        // mid-envelope (rare but possible) the pristine baseline doesn't
        // match, so skip the blend rather than write garbage.
        if (fc1 == null || fc1.length != pristineFc1.length) return owned;
        blendArray(fc1, pristineFc1);
        if (fc2 != null && pristineFc2 != null && fc2.length == pristineFc2.length)
        {
            blendArray(fc2, pristineFc2);
        }
        if (fc3 != null && pristineFc3 != null && fc3.length == pristineFc3.length)
        {
            blendArray(fc3, pristineFc3);
        }
        return owned;
    }

    private void blendArray(int[] dest, int[] pristine)
    {
        // Face colours are HSL-packed shorts stored in the lower 16 bits of
        // each int. Upper bits can carry texture / shading flags -- preserve.
        final int upperMask = 0xFFFF0000;
        final double tFinal = this.t;
        final int tH = this.targetH;
        final int tS = this.targetS;
        final int tL = this.targetL;
        final ColourBlendMode m = this.mode;

        for (int i = 0; i < dest.length; i++)
        {
            int orig = pristine[i];
            int origPacked = orig & 0xFFFF;
            int oH = (origPacked >> 10) & JagexColor.HUE_MAX;
            int oS = (origPacked >> 7) & JagexColor.SATURATION_MAX;
            int oL = origPacked & JagexColor.LUMINANCE_MAX;

            int nH, nS, nL;
            switch (m)
            {
                case REPLACE:
                    nH = lerpHue(oH, tH, tFinal);
                    nS = lerpInt(oS, tS, tFinal);
                    nL = lerpInt(oL, tL, tFinal);
                    break;
                case MULTIPLY:
                    // Luminance multiplies down toward (orig * target / MAX).
                    int mulL = (oL * tL) / JagexColor.LUMINANCE_MAX;
                    nH = lerpHue(oH, tH, tFinal);
                    nS = lerpInt(oS, tS, tFinal);
                    nL = lerpInt(oL, mulL, tFinal);
                    break;
                default: // ADD / screen
                    // Luminance brightens toward "screen" composite:
                    // orig + (MAX - orig) * (target / MAX).
                    int addL = oL + ((JagexColor.LUMINANCE_MAX - oL) * tL) / JagexColor.LUMINANCE_MAX;
                    nH = lerpHue(oH, tH, tFinal);
                    nS = lerpInt(oS, tS, tFinal);
                    nL = lerpInt(oL, addL, tFinal);
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
