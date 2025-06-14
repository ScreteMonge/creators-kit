package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HitsplatVariant
{
    NORMAL("Normal"),
    OTHER("Other"),
    MAX("Max")

    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
