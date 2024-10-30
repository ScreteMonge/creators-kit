package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimationKeyFrame extends KeyFrame
{
    private int animationId;

    public AnimationKeyFrame(double tick, int animationId)
    {
        super(KeyFrameType.ANIMATION, tick);
        this.animationId = animationId;
    }
}
