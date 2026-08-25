package com.creatorskit.programming.camera;

import com.creatorskit.Character;

public class Ease
{
    public static CameraDirectionalScript interpolateDirectional(boolean isInPOH, double ratio, EaseType easeType, CameraDirectionalScript currentScript, CameraDirectionalScript nextScript)
    {
        if (nextScript == null)
        {
            return currentScript;
        }

        double interpolationFactor = calculateEasing(easeType, ratio);

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

        return new CameraDirectionalScript(
                CameraMotionType.DIRECTIONAL,
                currentScript.getEase(),
                lerp(currentScript.getPitch(), nextScript.getPitch(), interpolationFactor),
                lerp(currentYaw, nextYaw, interpolationFactor) % 16384,
                (int) lerp(currentScript.getScale(), nextScript.getScale(), interpolationFactor),
                isInPOH,
                (float) lerp(currentScript.getFocalX(), nextScript.getFocalX(), interpolationFactor),
                0,
                (float) lerp(currentScript.getFocalY(), nextScript.getFocalY(), interpolationFactor),
                (float) lerp(currentScript.getFocalZ(), nextScript.getFocalZ(), interpolationFactor),
                0
        );
    }

    public static CameraTrackingScript interplateTracking(double ratio, EaseType easeType, Character character, CameraTrackingScript currentScript, CameraTrackingScript nextScript)
    {
        if (nextScript == null)
        {
            return currentScript;
        }

        double interpolationFactor = calculateEasing(easeType, ratio);

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

        return new CameraTrackingScript(
                CameraMotionType.TRACKING,
                currentScript.getEase(),
                lerp(currentScript.getPitch(), nextScript.getPitch(), interpolationFactor),
                lerp(currentYaw, nextYaw, interpolationFactor) % 16384,
                (int) lerp(currentScript.getScale(), nextScript.getScale(), interpolationFactor),
                character
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