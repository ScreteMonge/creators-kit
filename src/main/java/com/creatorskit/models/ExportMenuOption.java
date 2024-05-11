package com.creatorskit.models;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ExportMenuOption
{
    DEFAULT(""),
    T_POSE("T-Pose"),
    CURRENT("Current Pose"),
    ANIMATION("Animation");

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
