package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthKeyFrame extends KeyFrame
{
    private boolean enabled;
    private HitsplatType hitsplatType;
    private int hitsplatHeight;
    private int maxHealth;
    private int currentHealth;
    private int healthbarHeight;

    public HealthKeyFrame(double tick, boolean enabled, HitsplatType hitsplatType, int hitsplatHeight, int maxHealth, int currentHealth, int healthbarHeight)
    {
        super(KeyFrameType.HEALTH, tick);
        this.enabled = enabled;
        this.hitsplatType = hitsplatType;
        this.hitsplatHeight = hitsplatHeight;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
        this.healthbarHeight = healthbarHeight;
    }
}
