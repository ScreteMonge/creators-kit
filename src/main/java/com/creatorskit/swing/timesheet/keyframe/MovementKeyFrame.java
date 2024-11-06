package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovementKeyFrame extends KeyFrame
{
    public MovementKeyFrame(double tick)
    {
        super(KeyFrameType.MOVEMENT, tick);
    }
}
