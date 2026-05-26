package com.creatorskit.swing.timesheet.keyframe;

/**
 * Easing curves available between consecutive {@link CameraKeyFrame}s.
 * Ported from mlgudi/keyframe-camera so the interpolation feel is identical
 * to that plugin -- a user moving from one tool to the other gets the same
 * timing curves under the same names.
 */
public enum CameraEaseType
{
    LINEAR,
    SINE,
    QUAD,
    CUBIC,
    QUART,
    QUINT,
    EXPO,
    /**
     * User-authored spline. The actual control points live on the kf's
     * {@link CameraKeyFrame#getCustomCurve()} -- this enum value just
     * tags "look there instead of the hardcoded curve()" at playback.
     */
    CUSTOM;

    @Override
    public String toString()
    {
        // The Easing combo displays this. "Custom..." with the ellipsis
        // signals "opens a dialog" the same way menu items conventionally
        // do; the other names stay all-caps so the combo matches the
        // existing enum-name aesthetic.
        return this == CUSTOM ? "Custom..." : name();
    }
}
