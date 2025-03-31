package com.creatorskit.programming;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum OrientationType
{
    GRADUAL("Gradual"),
    INSTANT("Instant");

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
