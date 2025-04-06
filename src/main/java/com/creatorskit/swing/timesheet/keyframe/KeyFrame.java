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
        KeyFrameType type = keyFrame.getKeyFrameType();
        switch (type)
        {
            default:
            case MOVEMENT:
                MovementKeyFrame moveKF = (MovementKeyFrame) keyFrame;
                int[][] path = moveKF.getPath();
                int[][] pathCopy = new int[path.length][];
                for (int i = 0; i < path.length; i++)
                {
                    int[] coordinates = path[i];
                    pathCopy[i] = new int[]{coordinates[0], coordinates[1]};
                }

                return new MovementKeyFrame(
                        tick,
                        moveKF.getPlane(),
                        moveKF.isPoh(),
                        pathCopy,
                        moveKF.getCurrentStep(),
                        moveKF.getStepClientTick(),
                        moveKF.isLoop(),
                        moveKF.getSpeed(),
                        moveKF.getTurnRate());
            case ANIMATION:
                AnimationKeyFrame animKF = (AnimationKeyFrame) keyFrame;
                return new AnimationKeyFrame(
                        tick,
                        animKF.isStall(),
                        animKF.getActive(),
                        animKF.getStartFrame(),
                        animKF.isLoop(),
                        animKF.getIdle(),
                        animKF.getWalk(),
                        animKF.getRun(),
                        animKF.getWalk180(),
                        animKF.getWalkRight(),
                        animKF.getWalkLeft(),
                        animKF.getIdleRight(),
                        animKF.getIdleLeft());
            case ORIENTATION:
                OrientationKeyFrame oriKF = (OrientationKeyFrame) keyFrame;
                return new OrientationKeyFrame(
                        tick,
                        oriKF.getGoal(),
                        oriKF.getStart(),
                        oriKF.getEnd(),
                        oriKF.getDuration(),
                        oriKF.getTurnRate());
            case SPAWN:
                SpawnKeyFrame spawnKF = (SpawnKeyFrame) keyFrame;
                return new SpawnKeyFrame(
                        tick,
                        spawnKF.isSpawnActive());
            case MODEL:
                ModelKeyFrame modelKF = (ModelKeyFrame) keyFrame;
                return new ModelKeyFrame(
                        tick,
                        modelKF.isUseCustomModel(),
                        modelKF.getModelId(),
                        modelKF.getCustomModel());
            case TEXT:
                TextKeyFrame textKF = (TextKeyFrame) keyFrame;
                return new TextKeyFrame(
                        tick,
                        textKF.getDuration(),
                        textKF.getText());
            case OVERHEAD:
                OverheadKeyFrame overKF = (OverheadKeyFrame) keyFrame;
                return new OverheadKeyFrame(
                        tick,
                        overKF.getSkullSprite(),
                        overKF.getPrayerSprite());
            case HEALTH:
                HealthKeyFrame healthKF = (HealthKeyFrame) keyFrame;
                return new HealthKeyFrame(
                        tick,
                        healthKF.isEnabled(),
                        healthKF.getHealthbarSprite(),
                        healthKF.getMaxHealth(),
                        healthKF.getCurrentHealth(),
                        healthKF.getHitsplat1Sprite(),
                        healthKF.getHitsplat2Sprite(),
                        healthKF.getHitsplat3Sprite(),
                        healthKF.getHitsplat4Sprite(),
                        healthKF.getHitsplat1(),
                        healthKF.getHitsplat2(),
                        healthKF.getHitsplat3(),
                        healthKF.getHitsplat4());
            case SPOTANIM:
            case SPOTANIM2:
                SpotAnimKeyFrame spotKF = (SpotAnimKeyFrame) keyFrame;
                return new SpotAnimKeyFrame(
                        tick,
                        type,
                        spotKF.getSpotAnimId(),
                        spotKF.isLoop(),
                        spotKF.getHeight());
        }
    }
}
