package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ModelToggle
{
    MODEL_ID("Use Model Id"),
    CUSTOM_MODEL("Use Custom Model")
    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
