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
                        animKF.isFreeze(),
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
                        oriKF.getTurnRate(),
                        oriKF.getTargetCharacterName());
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
                        modelKF.getCustomModel(),
                        modelKF.getRadius());
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
                        healthKF.getDuration(),
                        healthKF.getHealthbarSprite(),
                        healthKF.getMaxHealth(),
                        healthKF.getCurrentHealth(),
                        healthKF.getOrder(),
                        healthKF.getWidth());
            case SPOTANIM:
            case SPOTANIM2:
                SpotAnimKeyFrame spotKF = (SpotAnimKeyFrame) keyFrame;
                return new SpotAnimKeyFrame(
                        tick,
                        type,
                        spotKF.getSpotAnimId(),
                        spotKF.isLoop(),
                        spotKF.getHeight());
            case HITSPLAT_1:
            case HITSPLAT_2:
            case HITSPLAT_3:
            case HITSPLAT_4:
                HitsplatKeyFrame hitsplatKF = (HitsplatKeyFrame) keyFrame;
                return new HitsplatKeyFrame(
                        tick,
                        type,
                        hitsplatKF.getDuration(),
                        hitsplatKF.getSprite(),
                        hitsplatKF.getVariant(),
                        hitsplatKF.getDamage());
            case PROJECTILE:
                ProjectileKeyFrame projKF = (ProjectileKeyFrame) keyFrame;
                return new ProjectileKeyFrame(
                        tick,
                        projKF.getProjectileId(),
                        projKF.getTarget(),
                        projKF.getStartHeight(),
                        projKF.getEndHeight(),
                        projKF.getSlope(),
                        projKF.getStartPos(),
                        projKF.getDurationTicks(),
                        projKF.getStartDelayTicks(),
                        projKF.isFaceTrajectory());
            case SHIELD:
                ShieldKeyFrame shieldKF = (ShieldKeyFrame) keyFrame;
                return new ShieldKeyFrame(
                        tick,
                        shieldKF.getDuration(),
                        shieldKF.getRgb(),
                        shieldKF.getMaxValue(),
                        shieldKF.getCurrentValue(),
                        shieldKF.getOrder(),
                        shieldKF.getWidth());
            case SPECIAL:
                SpecialKeyFrame specialKF = (SpecialKeyFrame) keyFrame;
                return new SpecialKeyFrame(
                        tick,
                        specialKF.getDuration(),
                        specialKF.getRgb(),
                        specialKF.getMaxValue(),
                        specialKF.getCurrentValue(),
                        specialKF.getOrder(),
                        specialKF.getWidth());
            case SCREEN_FADE:
                ScreenFadeKeyFrame fadeKF = (ScreenFadeKeyFrame) keyFrame;
                return new ScreenFadeKeyFrame(
                        tick,
                        fadeKF.getRgb(),
                        fadeKF.getPeakAlpha(),
                        fadeKF.getRingRadius(),
                        fadeKF.getRingFeather(),
                        fadeKF.getFadeInTicks(),
                        fadeKF.getHoldTicks(),
                        fadeKF.getFadeOutTicks());
            case SCREEN_SHAKE:
                ScreenShakeKeyFrame shakeKF = (ScreenShakeKeyFrame) keyFrame;
                return new ScreenShakeKeyFrame(
                        tick,
                        shakeKF.getAmplitudeX(),
                        shakeKF.getAmplitudeY(),
                        shakeKF.getAmplitudeZ(),
                        shakeKF.getFrequency(),
                        shakeKF.getFadeInTicks(),
                        shakeKF.getHoldTicks(),
                        shakeKF.getFadeOutTicks());
        }
    }
}
