package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Global free-cam camera state for a moment in time. Mirrors the keyframe
 * model from mlgudi/keyframe-camera: focal point (x/y/z in 1/128 scene units
 * like LocalPoint, but stored as doubles so interpolation doesn't drift),
 * pitch + yaw in RADIANS (converted to JAU at apply time), engine zoom scale,
 * and an easing curve for the ramp toward the NEXT camera keyframe.
 *
 * <p>The keyframe owns the duration to the next via {@link #durationTicks} so
 * the timeline sheet can render its bar width even when no following keyframe
 * exists yet (the duration is just visual then). When a next keyframe IS
 * present, interpolation uses the gap between this tick and the next tick
 * rather than this duration -- {@code duration} only matters for the bar.
 *
 * <p>Lives on a Character (per current global-keyframe pattern; same as
 * Screen Fade) but the rendered effect is GLOBAL -- camera is per-client.
 * Park camera keyframes on a dedicated "scene controller" Character for
 * convenience.
 */
@Getter
@Setter
public class CameraKeyFrame extends KeyFrame
{
    /** Defaults match mlgudi/keyframe-camera's first-keyframe defaults. */
    public static final double DEFAULT_FOCAL_X = 0;
    public static final double DEFAULT_FOCAL_Y = 0;
    public static final double DEFAULT_FOCAL_Z = 0;
    public static final double DEFAULT_PITCH = 0;
    public static final double DEFAULT_YAW = 0;
    public static final int DEFAULT_SCALE = 256;
    public static final CameraEaseType DEFAULT_EASE = CameraEaseType.LINEAR;
    public static final double DEFAULT_DURATION = 2.0;

    /** Camera focal point X in 1/128 scene units. */
    private double focalX;
    /** Camera focal point Y (vertical, 1/128 units). */
    private double focalY;
    /** Camera focal point Z in 1/128 scene units. */
    private double focalZ;
    /** Camera pitch in RADIANS. Converted to JAU at apply time. */
    private double pitch;
    /** Camera yaw in RADIANS. Converted to JAU at apply time. */
    private double yaw;
    /** Engine zoom scale (CAMERA_ZOOM_FIXED_VIEWPORT varc). */
    private int scale;
    /** Easing curve used for the ramp from this keyframe to the next. */
    private CameraEaseType ease;
    /**
     * Duration in game ticks to the next implicit endpoint -- used as the bar
     * width when no next CameraKeyFrame exists yet (visual only). When a next
     * keyframe exists, the actual interpolation duration is (next.tick - tick).
     */
    private double durationTicks;

    public CameraKeyFrame(double tick, double focalX, double focalY, double focalZ,
                          double pitch, double yaw, int scale, CameraEaseType ease,
                          double durationTicks)
    {
        super(KeyFrameType.CAMERA, tick);
        this.focalX = focalX;
        this.focalY = focalY;
        this.focalZ = focalZ;
        this.pitch = pitch;
        this.yaw = yaw;
        this.scale = scale;
        this.ease = ease == null ? DEFAULT_EASE : ease;
        this.durationTicks = durationTicks;
    }
}
