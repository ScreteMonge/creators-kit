package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthKeyFrame extends KeyFrame
{
    /** Default stack position when no value is set in an old save. HP sits topmost by default. */
    public static final int DEFAULT_ORDER = 0;

    private double duration;
    private HealthbarSprite healthbarSprite;
    private int maxHealth;
    private int currentHealth;
    /**
     * Added 2.3.x. Stack position relative to other bars (HP/Shield/Special). Lower
     * values draw higher on screen. Defaults to {@link #DEFAULT_ORDER} so pre-2.3
     * saves -- where Gson fills missing ints with 0 -- preserve HP-at-top behaviour.
     */
    private int order = DEFAULT_ORDER;

    public HealthKeyFrame(double tick, double duration,
                          HealthbarSprite healthbarSprite,
                          int maxHealth, int currentHealth)
    {
        this(tick, duration, healthbarSprite, maxHealth, currentHealth, DEFAULT_ORDER);
    }

    public HealthKeyFrame(double tick, double duration,
                          HealthbarSprite healthbarSprite,
                          int maxHealth, int currentHealth,
                          int order)
    {
        super(KeyFrameType.HEALTH, tick);
        this.duration = duration;
        this.healthbarSprite = healthbarSprite;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
        this.order = order;
    }
}
