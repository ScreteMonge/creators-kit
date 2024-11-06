package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SpawnToggle
{
    SPAWN_ACTIVE("Active"),
    SPAWN_INACTIVE("Inactive")
    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
