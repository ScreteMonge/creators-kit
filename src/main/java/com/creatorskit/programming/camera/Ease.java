package com.creatorskit.programming.camera;

import com.creatorskit.Character;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.subtypes.MovementKeyFrame;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class Ease
{
    public static CameraDirectionalScript interpolateTileTracking(boolean isInPOH, double ratio, EaseType easeType, CameraDirectionalScript currentScript, CameraDirectionalScript nextScript)
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
                CameraMotionType.TILE_TRACKING,
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

    public static Float interpolateObjectTrackingHeight(Client client, double currentTick, boolean playing, Character c, double modelHeight)
    {
        MovementKeyFrame keyFrame = (MovementKeyFrame) c.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        if (keyFrame == null)
        {
            return null;
        }

        WorldView worldView = client.getTopLevelWorldView();

        int currentStep = keyFrame.getCurrentStep();
        int[][] path = keyFrame.getPath();
        if (currentStep >= path.length)
        {
            currentStep = path.length - 1;
        }

        int[] current = path[currentStep];
        boolean inPOH = MovementManager.useLocalLocations(worldView);
        int pohFactor = 0;
        if (inPOH)
        {
            pohFactor = -232;
        }

        LocalPoint lp;
        if (inPOH)
        {
            lp = new LocalPoint(current[0], current[1], worldView);
        }
        else
        {
            lp = LocalPoint.fromWorld(worldView, new WorldPoint(current[0], current[1], worldView.getPlane()));
        }

        if (lp == null)
        {
            return null;
        }

        int currentTileHeight = worldView.getTileHeight(lp.getX(), lp.getY(), worldView.getPlane());
        float calculatedModelHeight = (float) (-0.55 * modelHeight);

        if (currentStep == path.length - 1)
        {
            return (float) currentTileHeight + calculatedModelHeight + pohFactor;
        }

        int[] next = path[currentStep + 1];
        LocalPoint nextLp;
        if (inPOH)
        {
            nextLp = new LocalPoint(next[0], next[1], worldView);
        }
        else
        {
            nextLp = LocalPoint.fromWorld(worldView, new WorldPoint(next[0], next[1], worldView.getPlane()));
        }

        if (nextLp == null)
        {
            return (float) currentTileHeight + calculatedModelHeight + pohFactor;
        }

        int nextTileHeight = worldView.getTileHeight(nextLp.getX(), nextLp.getY(), worldView.getPlane());
        double percentComplete = calculatePercentStepComplete(keyFrame, currentTick, client.getGameCycle(), currentStep, playing);
        double tileHeight = percentComplete * (nextTileHeight - currentTileHeight) + currentTileHeight;

        return (float) (tileHeight + calculatedModelHeight + pohFactor);
    }

    private static double calculatePercentStepComplete(MovementKeyFrame keyFrame, double currentTick, int gameCycle, int currentStep, boolean playing)
    {
        if (playing)
        {
            double tileSpeed = keyFrame.getSpeed();
            double speed = tileSpeed * Constants.CLIENT_TICK_LENGTH / Constants.GAME_TICK_LENGTH;
            int clientTicksPassed = gameCycle - keyFrame.getStepClientTick();
            double stepsComplete = clientTicksPassed * speed;
            return Math.abs(stepsComplete - currentStep);
        }

        int[][] path = keyFrame.getPath();
        int pathLength = path.length;

        double tileSpeed = keyFrame.getSpeed();
        double timePassed = currentTick - keyFrame.getTick();
        double stepsComplete = timePassed * tileSpeed;
        currentStep = (int) Math.floor(stepsComplete);
        double endSpeed = (pathLength - 1) - (Math.floor(((pathLength - 1) / tileSpeed)) * tileSpeed);

        if (stepsComplete + endSpeed > pathLength - 1)
        {
            double jumps = (pathLength - 1) % tileSpeed;
            if (jumps != 0)
            {
                double ticksPreSlowdown = (pathLength - 1 - endSpeed) / tileSpeed;
                double stepsPreSlowdown = ticksPreSlowdown * tileSpeed;

                stepsComplete = (timePassed - ticksPreSlowdown) * endSpeed + stepsPreSlowdown;
                currentStep = (int) (stepsComplete);
            }
        }

        if (currentStep > pathLength)
        {
            currentStep = pathLength;
        }

        return Math.abs(stepsComplete - currentStep);
    }

    public static CameraTrackingScript interpolateObjectTracking(double ratio, EaseType easeType, Character character, CameraTrackingScript currentScript, CameraTrackingScript nextScript)
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
                CameraMotionType.OBJECT_TRACKING,
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
            case EASE_IN_CUBIC:
                return cubicEaseIn(t);
            case EASE_IN_QUAD:
                return quadEaseIn(t);
            case EASE_IN_QUART:
                return quartEaseIn(t);
            case EASE_IN_QUINT:
                return quintEaseIn(t);
            case EASE_OUT_CUBIC:
                return cubicEaseOut(t);
            case EASE_OUT_QUAD:
                return quadEaseOut(t);
            case EASE_OUT_QUART:
                return quartEaseOut(t);
            case EASE_OUT_QUINT:
                return quintEaseOut(t);

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

    private static double quadEaseIn(double t) {
        return t * t;
    }

    private static double cubicEaseIn(double t) {
        return t * t * t;
    }

    private static double quartEaseIn(double t) {
        return t * t * t * t;
    }

    private static double quintEaseIn(double t) {
        return t * t * t * t * t;
    }

    private static double quadEaseOut(double t) {
        return 1 - Math.pow(1 - t, 2);
    }

    private static double cubicEaseOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    private static double quartEaseOut(double t) {
        return 1 - Math.pow(1 - t, 4);
    }

    private static double quintEaseOut(double t) {
        return 1 - Math.pow(1 - t, 5);
    }
}