package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum OrientationToggle
{
    SMART_ORIENTATION("Smart Orientation"),
    MANUAL_ORIENTATION("Manual Orientation")
    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
