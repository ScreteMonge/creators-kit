package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextKeyFrame extends KeyFrame
{
    private boolean enabled;
    private String text;

    public TextKeyFrame(double tick, boolean enabled, String text)
    {
        super(KeyFrameType.TEXT, tick);
        this.enabled = enabled;
        this.text = text;
    }
}
