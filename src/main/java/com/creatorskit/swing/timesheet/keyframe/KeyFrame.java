package com.creatorskit.swing.timesheet.keyframe;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class KeyFrame
{
    private KeyFrameType keyFrameType;
    private double tick;

    public static KeyFrame createCopy(KeyFrame keyFrame, double tick)
    {
        if (keyFrame instanceof MovementKeyFrame)
        {
            MovementKeyFrame kf = (MovementKeyFrame) keyFrame;
            int[][] path = kf.getPath();
            int[][] pathCopy = new int[path.length][];
            for (int i = 0; i < path.length; i++)
            {
                int[] coordinates = path[i];
                pathCopy[i] = new int[]{coordinates[0], coordinates[1]};
            }

            return new MovementKeyFrame(
                    tick,
                    kf.getPlane(),
                    kf.isPoh(),
                    pathCopy,
                    kf.getCurrentStep(),
                    kf.getStepClientTick(),
                    kf.isLoop(),
                    kf.getSpeed());
        }

        if (keyFrame instanceof AnimationKeyFrame)
        {
            AnimationKeyFrame kf = (AnimationKeyFrame) keyFrame;
            return new AnimationKeyFrame(
                    tick,
                    kf.isFreeze(),
                    kf.getActive(),
                    kf.getStartFrame(),
                    kf.isLoop(),
                    kf.getIdle(),
                    kf.getWalk(),
                    kf.getRun(),
                    kf.getWalk180(),
                    kf.getWalkRight(),
                    kf.getWalkLeft(),
                    kf.getIdleRight(),
                    kf.getIdleLeft());
        }

        if (keyFrame instanceof OrientationKeyFrame)
        {
            OrientationKeyFrame kf = (OrientationKeyFrame) keyFrame;
            return new OrientationKeyFrame(
                    tick,
                    kf.getManualOrientation(),
                    kf.isManualOverride());
        }

        if (keyFrame instanceof SpawnKeyFrame)
        {
            SpawnKeyFrame kf = (SpawnKeyFrame) keyFrame;
            return new SpawnKeyFrame(
                    tick,
                    kf.isSpawnActive());
        }

        if (keyFrame instanceof ModelKeyFrame)
        {
            ModelKeyFrame kf = (ModelKeyFrame) keyFrame;
            return new ModelKeyFrame(
                    tick,
                    kf.isUseCustomModel(),
                    kf.getModelId(),
                    kf.getCustomModel());
        }

        if (keyFrame instanceof TextKeyFrame)
        {
            TextKeyFrame kf = (TextKeyFrame) keyFrame;
            return new TextKeyFrame(
                    tick,
                    kf.getDuration(),
                    kf.getText());
        }

        if (keyFrame instanceof OverheadKeyFrame)
        {
            OverheadKeyFrame kf = (OverheadKeyFrame) keyFrame;
            return new OverheadKeyFrame(
                    tick,
                    kf.getSkullSprite(),
                    kf.getPrayerSprite());
        }

        if (keyFrame instanceof HealthKeyFrame)
        {
            HealthKeyFrame kf = (HealthKeyFrame) keyFrame;
            return new HealthKeyFrame(
                    tick,
                    kf.isEnabled(),
                    kf.getHealthbarSprite(),
                    kf.getMaxHealth(),
                    kf.getCurrentHealth(),
                    kf.getHitsplat1Sprite(),
                    kf.getHitsplat2Sprite(),
                    kf.getHitsplat3Sprite(),
                    kf.getHitsplat4Sprite(),
                    kf.getHitsplat1(),
                    kf.getHitsplat2(),
                    kf.getHitsplat3(),
                    kf.getHitsplat4());
        }

        if (keyFrame instanceof SpotAnimKeyFrame)
        {
            SpotAnimKeyFrame kf = (SpotAnimKeyFrame) keyFrame;
            return new SpotAnimKeyFrame(
                    tick,
                    kf.getSpotAnimType(),
                    kf.getSpotAnimId(),
                    kf.isLoop());
        }

        return null;
    }
}
