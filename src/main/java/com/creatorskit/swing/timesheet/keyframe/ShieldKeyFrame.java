package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Overhead shield bar -- second bar in the Doom-of-Mokhaiotl-style HP/Shield/Special
 * stack. Same animation semantics as HealthKeyFrame (currentValue lerps toward
 * the rendered position over the keyframe's lifetime), but the bar uses a
 * user-chosen colour rather than the red/green sprite the health bar uses.
 * Rendered just above the HP bar (or in its place when no HP bar is active).
 */
@Getter
@Setter
public class ShieldKeyFrame extends KeyFrame
{
    /** Default stack position. Shield sits below HP and above Special by default. */
    public static final int DEFAULT_ORDER = 1;

    /** Colour of the filled portion, packed RGB. Depleted portion = same hue, darker. */
    private int rgb;
    private double duration;
    private int maxValue;
    private int currentValue;
    /**
     * Stack position relative to HP / Special. Lower values draw higher on screen.
     * See {@link HealthKeyFrame#order} for the shared convention.
     */
    private int order = DEFAULT_ORDER;

    public ShieldKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue)
    {
        this(tick, duration, rgb, maxValue, currentValue, DEFAULT_ORDER);
    }

    public ShieldKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue, int order)
    {
        super(KeyFrameType.SHIELD, tick);
        this.duration = duration;
        this.rgb = rgb;
        this.maxValue = maxValue;
        this.currentValue = currentValue;
        this.order = order;
    }
}
