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
    /** Default stack position. Special sits at the bottom by default. */
    public static final int DEFAULT_ORDER = 2;
    /** Sentinel for {@link #width}: 0 means "auto-scale from maxValue". */
    public static final int AUTO_WIDTH = 0;

    /** Colour of the filled portion, packed RGB. Depleted portion = same hue, darker. */
    private int rgb;
    private double duration;
    private int maxValue;
    private int currentValue;
    /**
     * Stack position relative to HP / Shield. Lower values draw higher on screen.
     * See {@link HealthKeyFrame#order} for the shared convention.
     */
    private int order = DEFAULT_ORDER;
    /** Explicit pixel width override. {@link #AUTO_WIDTH} (0) = auto-scale from maxValue. */
    private int width = AUTO_WIDTH;

    public SpecialKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue)
    {
        this(tick, duration, rgb, maxValue, currentValue, DEFAULT_ORDER, AUTO_WIDTH);
    }

    public SpecialKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue, int order)
    {
        this(tick, duration, rgb, maxValue, currentValue, order, AUTO_WIDTH);
    }

    public SpecialKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue, int order, int width)
    {
        super(KeyFrameType.SPECIAL, tick);
        this.duration = duration;
        this.rgb = rgb;
        this.maxValue = maxValue;
        this.currentValue = currentValue;
        this.order = order;
        this.width = width;
    }
}
