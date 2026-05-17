package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Global camera shake -- mirrors the in-game effect bosses like Sol Heredit
 * trigger during heavy slams. OSRS exposes only an on/off toggle for built-in
 * shakes ({@code Client.setCameraShakeDisabled}); to drive shakes from the
 * timeline we apply per-tick sin offsets to the camera focal point directly
 * (the same focal-point API the camera-lock feature uses).
 *
 * <p>Lives on a Character so it has a timeline lane like every other keyframe,
 * but the rendered effect is GLOBAL -- the shake applies to the canvas-wide
 * camera regardless of which Character "owns" the keyframe. Park it on a
 * scene-controller Character if you want it easy to find. Multiple overlapping
 * shake keyframes are resolved by the manager picking the most-recently-
 * started one inside its envelope (same rule as ScreenFadeKeyFrame).
 *
 * <p>Requires the camera to be in free-camera mode (mode 1) -- without it the
 * focal-point setters throw. The manager silently skips application if the
 * mode is wrong rather than spamming exceptions.
 */
@Getter
@Setter
public class ScreenShakeKeyFrame extends KeyFrame
{
    public static final int DEFAULT_AMPLITUDE_X = 40;
    public static final int DEFAULT_AMPLITUDE_Y = 25;
    public static final int DEFAULT_AMPLITUDE_Z = 40;
    /** Cycles per game tick. 3.0 = ~5 Hz at the OSRS 600ms tick rate. */
    public static final double DEFAULT_FREQUENCY = 3.0;
    public static final double DEFAULT_FADE_IN = 0.2;
    public static final double DEFAULT_HOLD = 1.0;
    public static final double DEFAULT_FADE_OUT = 0.6;

    /** Peak displacement on each axis, in scene units (1 tile = 128). */
    private int amplitudeX;
    /** Y in OSRS focal-point coords is HEIGHT -- vertical jitter. */
    private int amplitudeY;
    /** Z in OSRS focal-point coords is the second horizontal axis. */
    private int amplitudeZ;
    /** Oscillation frequency in cycles per game tick. */
    private double frequency;
    /** Game ticks to ramp amplitude from 0 to its peak. */
    private double fadeInTicks;
    /** Game ticks to hold at peak amplitude. */
    private double holdTicks;
    /** Game ticks to ramp amplitude from peak back to 0. */
    private double fadeOutTicks;

    public ScreenShakeKeyFrame(double tick, int amplitudeX, int amplitudeY, int amplitudeZ,
                               double frequency, double fadeInTicks, double holdTicks,
                               double fadeOutTicks)
    {
        super(KeyFrameType.SCREEN_SHAKE, tick);
        this.amplitudeX = amplitudeX;
        this.amplitudeY = amplitudeY;
        this.amplitudeZ = amplitudeZ;
        this.frequency = frequency;
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
