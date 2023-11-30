package com.creatorskit;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Rotation
{
    _45_DEGREES("45 Degrees", 256),
    _90_DEGREES("90 Degrees", 512),
    _180_DEGREES("180 Degrees", 1024)

    ;
    public final String name;
    public final int degrees;

    @Override
    public String toString()
    {
        return name;
    }
}
