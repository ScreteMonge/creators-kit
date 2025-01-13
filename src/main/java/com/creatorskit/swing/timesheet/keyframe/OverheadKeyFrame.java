package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.HeadIcon;

@Getter
@Setter
public class OverheadKeyFrame extends KeyFrame
{
    private boolean enabled;
    private HeadIcon headIcon;
    private int height;

    public OverheadKeyFrame(double tick, boolean enabled, HeadIcon headIcon, int height)
    {
        super(KeyFrameType.OVERHEAD, tick);
        this.enabled = enabled;
        this.headIcon = headIcon;
        this.height = height;
    }
}
