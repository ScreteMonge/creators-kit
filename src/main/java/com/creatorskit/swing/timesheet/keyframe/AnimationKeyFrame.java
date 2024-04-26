package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimationKeyFrame extends KeyFrame
{
    private int animationId;

    public AnimationKeyFrame(KeyFrameType keyFrameType, int duration, int animationId)
    {
        super(keyFrameType, duration);
        this.animationId = animationId;
    }
}
