package com.creatorskit.saves;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.Getter;

@Getter
public class ProjectileKeyFrameSave extends KeyFrame
{
    private int spotAnimId;
    private int originIndex;
    private int targetIndex;
    private int slope;
    private int startHeightOffset;
    private int endHeightOffset;

    public ProjectileKeyFrameSave(double tick, int spotAnimId, int originIndex, int targetIndex, int slope, int startHeightOffset, int endHeightOffset)
    {
        super(KeyFrameType.PROJECTILE, tick);
        this.spotAnimId = spotAnimId;
        this.originIndex = originIndex;
        this.targetIndex = targetIndex;
        this.slope = slope;
        this.startHeightOffset = startHeightOffset;
        this.endHeightOffset = endHeightOffset;
    }
}
