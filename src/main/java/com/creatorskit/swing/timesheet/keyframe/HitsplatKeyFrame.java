package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HitsplatKeyFrame extends KeyFrame
{
    private int duration;
    private HitsplatSprite sprite;
    private int damage;

    public HitsplatKeyFrame(double tick, KeyFrameType hitsplatType, int duration, HitsplatSprite sprite, int damage)
    {
        super(hitsplatType, tick);
        this.duration = duration;
        this.sprite = sprite;
        this.damage = damage;
    }
}
