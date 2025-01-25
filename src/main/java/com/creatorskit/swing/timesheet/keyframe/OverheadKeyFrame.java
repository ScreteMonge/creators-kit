package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.swing.timesheet.keyframe.settings.OverheadSprite;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OverheadKeyFrame extends KeyFrame
{
    private boolean toggleSkull;
    private OverheadSprite overheadSprite;

    public OverheadKeyFrame(double tick, boolean toggleSkull, OverheadSprite overheadSprite)
    {
        super(KeyFrameType.OVERHEAD, tick);
        this.toggleSkull = toggleSkull;
        this.overheadSprite = overheadSprite;
    }
}
