package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Temporarily recolours the Character's current model over a fadeIn / hold /
 * fadeOut envelope (all in ticks). The underlying model is whatever the
 * Character's CKObject is rendering at the time -- typically derived from
 * the nearest preceding {@link KeyFrameType#MODEL} keyframe, falling back to
 * the Character's base model when none has been authored yet. The Pulse
 * itself does not change the model; it only mutates face colours during
 * its window, and restores them on the way out.
 *
 * <p>Total lifecycle on the timeline = {@code tick} .. {@code tick + fadeIn +
 * hold + fadeOut}. Inside that window the blend factor t in [0,1] is
 * computed off the wall position within the envelope and optionally
 * smoothed by ease-in-out. The resulting tint is composed with each face's
 * original HSL via {@link PulseBlendMode}.
 */
@Getter
@Setter
public class PulseKeyFrame extends KeyFrame
{
    /** Pulse target colour packed as an ARGB int (alpha ignored). */
    private int colorRgb;
    /** Ticks to ramp the blend factor 0 -> 1. May be 0 for "instant on." */
    private double fadeInTicks;
    /** Ticks the blend factor stays at 1. May be 0 for "no hold." */
    private double holdTicks;
    /** Ticks to ramp the blend factor 1 -> 0. May be 0 for "instant off." */
    private double fadeOutTicks;
    /** How the pulse colour combines with each face's original HSL. */
    private PulseBlendMode blendMode;
    /** When true, smoothstep the fade ramps; when false, linear ramps. */
    private boolean easeInOut;
    /** When true, the tint is also applied to the Character's spotanim 1 / 2 CKObjects. */
    private boolean affectSpotAnims;

    public PulseKeyFrame(
            double tick,
            int colorRgb,
            double fadeInTicks,
            double holdTicks,
            double fadeOutTicks,
            PulseBlendMode blendMode,
            boolean easeInOut,
            boolean affectSpotAnims)
    {
        super(KeyFrameType.PULSE, tick);
        this.colorRgb = colorRgb;
        this.fadeInTicks = fadeInTicks;
        this.holdTicks = holdTicks;
        this.fadeOutTicks = fadeOutTicks;
        this.blendMode = blendMode;
        this.easeInOut = easeInOut;
        this.affectSpotAnims = affectSpotAnims;
    }

    /** End tick of the pulse envelope. */
    public double getEndTick()
    {
        return getTick() + fadeInTicks + holdTicks + fadeOutTicks;
    }

    /**
     * Current blend factor t in [0,1] for a given absolute tick, or 0 when
     * {@code currentTick} is outside the envelope. Applies easing if enabled.
     */
    public double computeBlendFactor(double currentTick)
    {
        double dt = currentTick - getTick();
        if (dt < 0) return 0;

        double total = fadeInTicks + holdTicks + fadeOutTicks;
        if (dt >= total) return 0;

        double t;
        if (dt < fadeInTicks)
        {
            t = fadeInTicks <= 0 ? 1 : dt / fadeInTicks;
        }
        else if (dt < fadeInTicks + holdTicks)
        {
            t = 1;
        }
        else
        {
            double remaining = dt - fadeInTicks - holdTicks;
            t = fadeOutTicks <= 0 ? 0 : 1.0 - (remaining / fadeOutTicks);
        }

        if (t < 0) t = 0;
        if (t > 1) t = 1;

        if (easeInOut)
        {
            // classic smoothstep (3t^2 - 2t^3) -- C1-continuous and trivially cheap
            t = t * t * (3 - 2 * t);
        }
        return t;
    }
}
