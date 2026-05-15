package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Global fullscreen fade with a circular "dodger" cutout in the middle -- mirrors the
 * Whisperer / Blackstone Fragment shadow-realm effect (game interface 174,
 * {@code FadeOverlay}). The OSRS widget tree has: FILTER (the tinted fullscreen
 * rect), FADER (alpha animator), DODGER (the circular hole in the centre), and an
 * optional MESSAGE banner. This keyframe drives those properties from the
 * timeline.
 *
 * <p>Although it lives on a Character (the keyframe system is per-Character), the
 * rendered effect is GLOBAL -- the overlay covers the whole canvas and is visible
 * regardless of which Character "owns" the keyframe. Convenience: park screen-fade
 * keyframes on a dedicated "scene controller" Character so they're easy to find,
 * but any Character works.
 */
@Getter
@Setter
public class ScreenFadeKeyFrame extends KeyFrame
{
    public static final int DEFAULT_RGB = 0x18002a;   // Whisperer-purple
    public static final int DEFAULT_PEAK_ALPHA = 220; // out of 255
    /**
     * Default centre cutout. 0 = uniform full-screen fade so the local player gets
     * faded too. Bump this for the Whisperer-style "safe ring around the player"
     * look (try ~180 with feather ~60).
     */
    public static final int DEFAULT_RING_RADIUS = 0;
    public static final int DEFAULT_RING_FEATHER = 60;
    public static final double DEFAULT_FADE_IN = 0.8;
    public static final double DEFAULT_HOLD = 1.5;
    public static final double DEFAULT_FADE_OUT = 0.8;

    /** Filter colour, packed RGB. */
    private int rgb;
    /** Peak alpha during the hold phase, 0-255. */
    private int peakAlpha;
    /** Radius of the centre dodger / clear ring, in screen pixels. */
    private int ringRadius;
    /** Soft-edge width of the ring's outer boundary, in screen pixels. 0 = hard edge. */
    private int ringFeather;
    /** Game ticks the fade takes to ramp up from 0 to peakAlpha. */
    private double fadeInTicks;
    /** Game ticks the fade stays at peakAlpha after ramp-up. */
    private double holdTicks;
    /** Game ticks the fade takes to ramp down from peakAlpha to 0. */
    private double fadeOutTicks;

    public ScreenFadeKeyFrame(double tick, int rgb, int peakAlpha, int ringRadius,
                              int ringFeather, double fadeInTicks, double holdTicks,
                              double fadeOutTicks)
    {
        super(KeyFrameType.SCREEN_FADE, tick);
        this.rgb = rgb;
        this.peakAlpha = peakAlpha;
        this.ringRadius = ringRadius;
        this.ringFeather = ringFeather;
        this.fadeInTicks = fadeInTicks;
        this.holdTicks = holdTicks;
        this.fadeOutTicks = fadeOutTicks;
    }

    /** Total animation length in game ticks. */
    public double totalDurationTicks()
    {
        return fadeInTicks + holdTicks + fadeOutTicks;
    }
}
