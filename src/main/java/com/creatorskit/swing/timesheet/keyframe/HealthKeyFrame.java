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
    private HitsplatSprite hitsplat1Sprite;
    private HitsplatSprite hitsplat2Sprite;
    private HitsplatSprite hitsplat3Sprite;
    private HitsplatSprite hitsplat4Sprite;
    private int hitsplat1;
    private int hitsplat2;
    private int hitsplat3;
    private int hitsplat4;

    public HealthKeyFrame(double tick, boolean enabled,
                          HealthbarSprite healthbarSprite,
                          int maxHealth, int currentHealth,
                          HitsplatSprite hitsplat1Sprite, HitsplatSprite hitsplat2Sprite, HitsplatSprite hitsplat3Sprite, HitsplatSprite hitsplat4Sprite,
                          int hitsplat1, int hitsplat2, int hitsplat3, int hitsplat4)
    {
        super(KeyFrameType.HEALTH, tick);
        this.enabled = enabled;
        this.healthbarSprite = healthbarSprite;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
        this.hitsplat1Sprite = hitsplat1Sprite;
        this.hitsplat2Sprite = hitsplat2Sprite;
        this.hitsplat3Sprite = hitsplat3Sprite;
        this.hitsplat4Sprite = hitsplat4Sprite;
        this.hitsplat1 = hitsplat1;
        this.hitsplat2 = hitsplat2;
        this.hitsplat3 = hitsplat3;
        this.hitsplat4 = hitsplat4;
    }
}
