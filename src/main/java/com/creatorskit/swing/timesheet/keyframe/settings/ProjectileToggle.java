package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;

/**
 * Source of a projectile's 3D model. Mirrors {@link ModelToggle} but with
 * projectile-appropriate labels: a projectile's "cache id" option is a
 * SpotAnim graphic id (the engine's native projectile look), not a raw
 * model id.
 */
@AllArgsConstructor
public enum ProjectileToggle
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
