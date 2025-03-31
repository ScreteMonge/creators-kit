package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.programming.OrientationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrientationKeyFrame extends KeyFrame
{
    private int manualOrientation;
    private OrientationType type;
    private boolean override;

    public OrientationKeyFrame(double tick, OrientationType type, int manualOrientation, boolean override)
    {
        super(KeyFrameType.ORIENTATION, tick);
        this.manualOrientation = manualOrientation;
        this.type = type;
        this.override = override;
    }
}
