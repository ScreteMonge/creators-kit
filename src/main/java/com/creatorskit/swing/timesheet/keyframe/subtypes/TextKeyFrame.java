package com.creatorskit.swing.timesheet.keyframe.subtypes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextKeyFrame extends KeyFrame
{
    private double duration;
    private String text;

    public TextKeyFrame(double tick, double duration, String text)
    {
        super(KeyFrameType.TEXT, tick);
        this.duration = duration;
        this.text = text;
    }
}
