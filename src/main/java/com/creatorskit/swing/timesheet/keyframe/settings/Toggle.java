package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Toggle
{
    ENABLE("Enable"),
    DISABLE("Disable")
    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
