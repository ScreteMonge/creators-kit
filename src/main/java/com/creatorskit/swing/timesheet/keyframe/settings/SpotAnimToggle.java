package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;

/**
 * Source of a SpotAnim keyframe's 3D model: the cache SpotAnim graphic
 * (the existing behaviour, animation baked in) or a user Custom Model
 * (needs an explicit animation id). Mirrors {@link ProjectileToggle};
 * kept as its own enum so the two cards can evolve independently.
 */
@AllArgsConstructor
public enum SpotAnimToggle
{
    SPOTANIM_ID("SpotAnim ID"),
    CUSTOM_MODEL("Custom Model")
    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
