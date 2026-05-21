package com.creatorskit.swing.timesheet.keyframe;

/**
 * Interpolation between two consecutive {@link CameraKeyFrame}s. Ported from
 * mlgudi/keyframe-camera (LICENSE-compatible -- same author's open plugin).
 * Same six easing curves, same lerp semantics, same yaw-shortest-arc handling
 * so a user can swap between this and the source plugin without re-tuning.
 *
 * <p>Returns an intermediate {@link CameraKeyFrame} carrying the lerped focal
 * point + angles + zoom at t in [0, 1]. {@code from}'s easing curve is used
 * (the convention is "ease defines the curve approaching the NEXT keyframe").
 */
public final class CameraEase
{
    private CameraEase() {}

    public static CameraKeyFrame interpolate(CameraKeyFrame from, CameraKeyFrame to, double t)
    {
        if (to == null) return from;

        double factor = curve(from.getEase(), t);

        // Yaw shortest-arc: if the delta exceeds half a turn, the linear path
        // would visibly sweep the long way around. Push one endpoint by 2pi
        // so the lerp follows the shorter route. JAU range used so the
        // comparison matches the radian-to-JAU rounding the engine consumes.
        double fromYaw = from.getYaw();
        double toYaw = to.getYaw();
        double yawDeltaJau = radiansToJau(toYaw - fromYaw);
        if (Math.abs(yawDeltaJau) > 1024)
        {
            if (yawDeltaJau > 0) fromYaw += 2 * Math.PI;
            else toYaw += 2 * Math.PI;
        }

        return new CameraKeyFrame(
                from.getTick(),
                lerp(from.getFocalX(), to.getFocalX(), factor),
                lerp(from.getFocalY(), to.getFocalY(), factor),
                lerp(from.getFocalZ(), to.getFocalZ(), factor),
                lerp(from.getPitch(), to.getPitch(), factor),
                lerp(fromYaw, toYaw, factor),
                (int) lerp(from.getScale(), to.getScale(), factor),
                from.getEase(),
                from.getDurationTicks());
    }

    public static double lerp(double a, double b, double t)
    {
        return a + (b - a) * t;
    }

    private static final double RADIANS_TO_JAU = 2048.0 / (2 * Math.PI);
    public static int radiansToJau(double radians)
    {
        return (int) Math.round(radians * RADIANS_TO_JAU) % 2048;
    }

    private static double curve(CameraEaseType ease, double t)
    {
        switch (ease)
        {
            case LINEAR: return t;
            case SINE:   return -(Math.cos(Math.PI * t) - 1) / 2;
            case QUAD:   return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            case CUBIC:  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
            case QUART:  return t < 0.5 ? 8 * t * t * t * t : 1 - Math.pow(-2 * t + 2, 4) / 2;
            case QUINT:  return t < 0.5 ? 16 * t * t * t * t * t : 1 - Math.pow(-2 * t + 2, 5) / 2;
            case EXPO:
                if (t == 0) return 0;
                if (t == 1) return 1;
                return t < 0.5 ? Math.pow(2, 20 * t - 10) / 2 : (2 - Math.pow(2, -20 * t + 10)) / 2;
            default: return t;
        }
    }
}
