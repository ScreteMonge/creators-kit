package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthKeyFrame extends KeyFrame
{
    private boolean enabled;
    private HealthbarSprite healthbarSprite;
    private int maxHealth;
    private int currentHealth;

    public HealthKeyFrame(double tick, boolean enabled,
                          HealthbarSprite healthbarSprite,
                          int maxHealth, int currentHealth)
    {
        super(KeyFrameType.HEALTH, tick);
        this.enabled = enabled;
        this.healthbarSprite = healthbarSprite;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
    }
}
