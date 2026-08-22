package com.creatorskit.swing.timesheet.keyframe.subtypes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthKeyFrame extends KeyFrame
{
    private double duration;
    private HealthbarSprite healthbarSprite;
    private int maxHealth;
    private int currentHealth;

    public HealthKeyFrame(double tick, double duration,
                          HealthbarSprite healthbarSprite,
                          int maxHealth, int currentHealth)
    {
        super(KeyFrameType.HEALTH, tick);
        this.duration = duration;
        this.healthbarSprite = healthbarSprite;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
    }
}
