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

        if (keyFrame instanceof SpawnKeyFrame)
        {
            SpawnKeyFrame kf = (SpawnKeyFrame) keyFrame;
            return new SpawnKeyFrame(
                    tick,
                    kf.isSpawnActive());
        }

        if (keyFrame instanceof OrientationKeyFrame)
        {
            OrientationKeyFrame kf = (OrientationKeyFrame) keyFrame;
            return new OrientationKeyFrame(
                    tick,
                    kf.getManualOrientation(),
                    kf.isManualOverride());
        }

        return null;
    }
}
