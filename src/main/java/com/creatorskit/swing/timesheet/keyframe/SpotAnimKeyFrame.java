package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpotAnimKeyFrame extends KeyFrame
{
    private KeyFrameType spotAnimType;
    private int spotAnimId;
    private boolean loop;
    private int height;

    public SpotAnimKeyFrame(double tick, KeyFrameType spotAnimType, int spotAnimId, boolean loop, int height)
    {
        super(spotAnimType, tick);
        this.spotAnimId = spotAnimId;
        this.loop = loop;
        this.height = height;
    }
}
