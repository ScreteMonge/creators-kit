package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextKeyFrame extends KeyFrame
{
    private int duration;
    private String text;

    public TextKeyFrame(double tick, int duration, String text)
    {
        super(KeyFrameType.TEXT, tick);
        this.duration = duration;
        this.text = text;
    }
}
