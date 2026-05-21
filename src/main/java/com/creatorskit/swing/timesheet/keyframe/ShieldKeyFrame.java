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
    /** Sentinel for {@link #width}: 0 means "auto-scale from maxValue". */
    public static final int AUTO_WIDTH = 0;

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
    /** Explicit pixel width override. {@link #AUTO_WIDTH} (0) = auto-scale from maxValue. */
    private int width = AUTO_WIDTH;
    /** Created by the hitsplat -> bar auto-sync; cleaned up on move-away.
     *  See HealthKeyFrame#autoSynced for the full contract. */
    private boolean autoSynced = false;
    /** Inverted store of the "Sync hitsplats" toggle, default-false (= sync ON)
     *  so pre-existing saves load with sync enabled. See HealthKeyFrame. */
    private boolean syncHitsplatsDisabled = false;

    public boolean isSyncHitsplats()
    {
        return !syncHitsplatsDisabled;
    }

    public void setSyncHitsplats(boolean syncHitsplats)
    {
        this.syncHitsplatsDisabled = !syncHitsplats;
    }

    public ShieldKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue)
    {
        this(tick, duration, rgb, maxValue, currentValue, DEFAULT_ORDER, AUTO_WIDTH);
    }

    public ShieldKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue, int order)
    {
        this(tick, duration, rgb, maxValue, currentValue, order, AUTO_WIDTH);
    }

    public ShieldKeyFrame(double tick, double duration, int rgb, int maxValue, int currentValue, int order, int width)
    {
        super(KeyFrameType.SHIELD, tick);
        this.duration = duration;
        this.rgb = rgb;
        this.maxValue = maxValue;
        this.currentValue = currentValue;
        this.order = order;
        this.width = width;
    }
}
