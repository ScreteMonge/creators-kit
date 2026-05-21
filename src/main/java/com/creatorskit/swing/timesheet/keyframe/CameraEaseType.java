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
    EXPO
}
