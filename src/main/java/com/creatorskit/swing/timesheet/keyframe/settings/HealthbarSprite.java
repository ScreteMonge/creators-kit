package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.SpriteID;

@Getter
@AllArgsConstructor
public enum HealthbarSprite
{
    DEFAULT("Default", 2176, SpriteID.HEALTHBAR_DEFAULT_BACK_30PX),
    /**
     * Pinned boss-style bar drawn by BossHealthOverlay instead of HealthOverlay
     * -- no overhead sprite. Sprite IDs are unused (0) and not read; rendering
     * is custom-drawn at the top of the canvas with a fading damage indicator.
     * Only one Character can hold an active BOSS_HEALTH keyframe at a time;
     * the overlay picks the most-recently-started one when multiple overlap.
     */
    BOSS_HEALTH("Boss health", 0, 0),
    ;

    private final String name;
    private final int foregroundSpriteID;
    private final int backgroundSpriteID;

    @Override
    public String toString()
    {
        return name;
    }
}
