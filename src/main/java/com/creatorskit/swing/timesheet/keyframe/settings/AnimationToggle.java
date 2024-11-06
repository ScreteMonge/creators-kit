package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AnimationToggle
{
    SMART_ANIMATION("Smart Animation"),
    MANUAL_ANIMATION("Manual Animation")
    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
