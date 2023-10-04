package com.creatorskit.models;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum LightingStyle
{
    NONE("No Lighting"),
    ACTOR("Actor Lighting"),
    DEFAULT("Default Lighting")
    ;

    private final String string;

    @Override
    public String toString()
    {
        return string;
    }
}
