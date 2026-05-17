package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Global camera-shake keyframe. Drives the camera focal point with a chaotic
 * multi-octave noise curve so the screen vibrates / shakes for the keyframe's
 * lifetime. Camera-relative (horizontal jitter is always left/right on screen
 * regardless of yaw) and instant on/off -- no fade envelope.
 *
 * <p>Lives on a Character so it occupies a timeline lane like every other
 * keyframe, but the effect is GLOBAL (the whole canvas shakes regardless of
 * which Character owns the keyframe). Multiple overlapping shake keyframes
 * resolve to the most-recently-started one, same rule as ScreenFadeKeyFrame.
 *
 * <p>Requires the camera to be in free-camera mode (mode 1) -- without it the
 * focal-point setters throw. The manager silently no-ops if mode != 1.
 */
@Getter
@Setter
public class ScreenShakeKeyFrame extends KeyFrame
{
    /** Subtle default -- reads as a vibration. Bump for big slams. */
    public static final int DEFAULT_AMPLITUDE_HORIZONTAL = 4;
    public static final int DEFAULT_AMPLITUDE_VERTICAL = 4;
    /** Base oscillation rate in cycles per game tick. Higher = faster vibration. */
    public static final double DEFAULT_FREQUENCY = 25.0;
    public static final double DEFAULT_DURATION = 4.0;

    /** Peak horizontal (screen left/right) displacement, scene units (1 tile = 128). */
    private int amplitudeHorizontal;
    /** Peak vertical (screen up/down) displacement, scene units. */
    private int amplitudeVertical;
    /** Base frequency of the noise curve in cycles per game tick. */
    private double frequency;
    /** How long the shake lasts in game ticks. Starts and stops instantly -- no fade. */
    private double durationTicks;

    public ScreenShakeKeyFrame(double tick, int amplitudeHorizontal, int amplitudeVertical,
                               double frequency, double durationTicks)
    {
        super(KeyFrameType.SCREEN_SHAKE, tick);
        this.amplitudeHorizontal = amplitudeHorizontal;
        this.amplitudeVertical = amplitudeVertical;
        this.frequency = frequency;
        this.durationTicks = durationTicks;
    }
}
