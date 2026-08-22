package com.creatorskit.programming.camera;

import com.creatorskit.swing.timesheet.keyframe.subtypes.CameraKeyFrame;

public class Ease
{
    public static CameraScript interpolate(double ratio, CameraKeyFrame currentKeyframe, CameraKeyFrame nextKeyframe)
    {
        if (nextKeyframe == null)
        {
            return currentKeyframe.getScript();
        }

        CameraScript currentScript = currentKeyframe.getScript();
        CameraScript nextScript = nextKeyframe.getScript();

        double interpolationFactor = calculateEasing(currentKeyframe.getEase(), ratio);

        double currentYaw = currentScript.getYaw();
        double nextYaw = nextScript.getYaw();

        double yawDiff = nextYaw - currentYaw;
        if (Math.abs(yawDiff) > 8192) {
            if (yawDiff > 0) {
                currentYaw += 16384;
            } else {
                nextYaw += 16384;
            }
        }

        return new CameraScript(
                (float) lerp(currentScript.getFocalX(), nextScript.getFocalX(), interpolationFactor),
                (float) lerp(currentScript.getFocalY(), nextScript.getFocalY(), interpolationFactor),
                (float) lerp(currentScript.getFocalZ(), nextScript.getFocalZ(), interpolationFactor),
                lerp(currentScript.getPitch(), nextScript.getPitch(), interpolationFactor),
                lerp(currentYaw, nextYaw, interpolationFactor) % 16384,
                (int) lerp(currentScript.getScale(), nextScript.getScale(), interpolationFactor)
        );
    }

    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static double calculateEasing(EaseType ease, double t) {
        switch (ease) {
            case LINEAR:
                return t;
            case SINE:
                return sinEaseInOut(t);
            case QUAD:
                return quadEaseInOut(t);
            case CUBIC:
                return cubicEaseInOut(t);
            case QUART:
                return quartEaseInOut(t);
            case QUINT:
                return quintEaseInOut(t);
            case EXPO:
                return expoEaseInOut(t);
            default:
                throw new IllegalArgumentException("Unknown easing type: " + ease);
        }
    }

    // Easing functions
    private static double sinEaseInOut(double t) { return (-(Math.cos(Math.PI * t) - 1) / 2); }

    private static double quadEaseInOut(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    private static double cubicEaseInOut(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private static double quartEaseInOut(double t) {
        return t < 0.5 ? 8 * t * t * t * t : 1 - Math.pow(-2 * t + 2, 4) / 2;
    }

    private static double quintEaseInOut(double t) {
        return t < 0.5 ? 16 * t * t * t * t * t : 1 - Math.pow(-2 * t + 2, 5) / 2;
    }

    private static double expoEaseInOut(double t) {
        return t == 0 ? 0 : t == 1 ? 1 : t < 0.5 ? Math.pow(2, 20 * t - 10) / 2
                : (2 - Math.pow(2, -20 * t + 10)) / 2;
    }
}