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

    public KeyFrame createCopy(KeyFrame keyFrame, double tick)
    {
        if (keyFrame instanceof MovementKeyFrame)
        {
            MovementKeyFrame kf = (MovementKeyFrame) keyFrame;
            return new MovementKeyFrame(tick);
        }

        if (keyFrame instanceof AnimationKeyFrame)
        {
            AnimationKeyFrame kf = (AnimationKeyFrame) keyFrame;
            return new AnimationKeyFrame(
                    tick,
                    kf.getManualAnim(),
                    kf.isManualOverride(),
                    kf.getIdleAnim(),
                    kf.getWalkAnim(),
                    kf.getRunAnim(),
                    kf.getWalk180Anim(),
                    kf.getWalkRAnim(),
                    kf.getWalkLAnim(),
                    kf.getIdleRAnim(),
                    kf.getIdleLAnim());
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
                    kf.isEnabled(),
                    kf.getText());
        }

        if (keyFrame instanceof OverheadKeyFrame)
        {
            OverheadKeyFrame kf = (OverheadKeyFrame) keyFrame;
            return new OverheadKeyFrame(
                    tick,
                    kf.isToggleSkull(),
                    kf.getOverheadSprite());
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
                    kf.getSpotAnimId1(),
                    kf.getSpotAnimId2(),
                    kf.isLoop1(),
                    kf.isLoop2());
        }

        return null;
    }
}
