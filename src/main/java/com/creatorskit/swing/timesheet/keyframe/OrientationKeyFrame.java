package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrientationKeyFrame extends KeyFrame
{
    private int manualOrientation;
    private boolean manualOverride;

    public OrientationKeyFrame(double tick, int manualOrientation, boolean manualOverride)
    {
        super(KeyFrameType.ORIENTATION, tick);
        this.manualOrientation = manualOrientation;
        this.manualOverride = manualOverride;
    }
}
