package com.creatorskit.swing.timesheet.keyframe.subtypes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.settings.OverheadSprite;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OverheadKeyFrame extends KeyFrame
{
    private OverheadSprite skullSprite;
    private OverheadSprite prayerSprite;

    public OverheadKeyFrame(double tick, OverheadSprite skullSprite, OverheadSprite prayerSprite)
    {
        super(KeyFrameType.OVERHEAD, tick);
        this.skullSprite = skullSprite;
        this.prayerSprite = prayerSprite;
    }
}
