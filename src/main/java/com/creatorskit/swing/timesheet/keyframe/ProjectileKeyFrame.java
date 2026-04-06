package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.Character;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectileKeyFrame extends KeyFrame
{
    private int spotAnimId;
    private Character origin;
    private Character target;
    private int slope;
    private int startHeightOffset;
    private int endHeightOffset;

    public ProjectileKeyFrame(double tick, int spotAnimId, Character origin, Character target, int slope, int startHeightOffset, int endHeightOffset)
    {
        super(KeyFrameType.PROJECTILE, tick);
        this.spotAnimId = spotAnimId;
        this.origin = origin;
        this.target = target;
        this.slope = slope;
        this.startHeightOffset = startHeightOffset;
        this.endHeightOffset = endHeightOffset;
    }
}
