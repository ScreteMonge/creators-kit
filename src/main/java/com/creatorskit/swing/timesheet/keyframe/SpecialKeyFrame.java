package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Overhead special bar -- third bar in the Doom-of-Mokhaiotl-style stack. Mirrors
 * {@link ShieldKeyFrame} field-for-field; rendered above the shield bar (or directly
 * above HP when no shield bar is active). The split into two classes keeps the
 * cards / colours / animation timelines independent so a Character can animate
 * HP, Shield, and Special on three separate tracks.
 */
@Getter
@Setter
public class SpecialKeyFrame extends KeyFrame
{
    /** Colour of the filled portion, packed RGB. */
    private int rgb;
    private double duration;
    private int maxValue;
    private int currentValue;

    public SpecialKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue)
    {
        super(KeyFrameType.SPECIAL, tick);
        this.duration = duration;
        this.rgb = rgb;
        this.maxValue = maxValue;
        this.currentValue = currentValue;
    }
}
