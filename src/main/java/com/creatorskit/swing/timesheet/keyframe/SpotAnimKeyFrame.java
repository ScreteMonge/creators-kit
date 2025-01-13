package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpotAnimKeyFrame extends KeyFrame
{
    private int spotAnimId1;
    private int spotAnimId2;

    public SpotAnimKeyFrame(double tick, int spotAnimId1, int spotAnimId2)
    {
        super(KeyFrameType.SPOTANIM, tick);
        this.spotAnimId1 = spotAnimId1;
        this.spotAnimId2 = spotAnimId2;
    }
}
