package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpotAnimKeyFrame extends KeyFrame
{
    private int spotAnimId1;
    private int spotAnimId2;
    private boolean loop1;
    private boolean loop2;

    public SpotAnimKeyFrame(double tick, int spotAnimId1, int spotAnimId2, boolean loop1, boolean loop2)
    {
        super(KeyFrameType.SPOTANIM, tick);
        this.spotAnimId1 = spotAnimId1;
        this.spotAnimId2 = spotAnimId2;
        this.loop1 = loop1;
        this.loop2 = loop2;
    }
}
