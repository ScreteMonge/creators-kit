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
    /**
     * Renders BOTH the overhead sprite bar (like {@link #DEFAULT}) AND the
     * pinned boss bar (like {@link #BOSS_HEALTH}) for the same keyframe. The
     * sprite IDs match DEFAULT so the overhead component draws via BarOverlay,
     * while {@link #isBoss()} returns true so BossHealthOverlay also draws the
     * pinned bar. Lets the user show a model-anchored bar and a top-of-screen
     * boss bar from a single Health keyframe.
     */
    BOTH("Both", 2176, SpriteID.HEALTHBAR_DEFAULT_BACK_30PX),
    ;

    private final String name;
    private final int foregroundSpriteID;
    private final int backgroundSpriteID;

    /**
     * True when this style participates in the pinned top-of-screen boss bar
     * (BossHealthOverlay). Both {@link #BOSS_HEALTH} (boss bar only) and
     * {@link #BOTH} (boss bar + overhead) qualify. Centralises the "should
     * the boss overlay draw this?" predicate so the three scan sites in
     * BossHealthOverlay stay in sync.
     */
    public boolean isBoss()
    {
        return this == BOSS_HEALTH || this == BOTH;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
