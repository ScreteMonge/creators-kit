package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LightingStyle
{
    PRESET("---", 100, 100, 100, 100, 100),
    NONE("None", 128, 4000, -50, -50, 10),
    ACTOR("Actor", 64, 850, -30, -30, 50),
    DEFAULT("Default", 64, 768, -50, -50, 10),
    SPOTANIM("SpotAnim", 64, 850, -50, -50, 75),
    CUSTOM("Custom", 64, 768, -50, -50, 10)
    ;

    private final String string;
    private final int ambient;
    private final int contrast;
    private final int x;
    private final int y;
    private final int z;

    @Override
    public String toString()
    {
        return string;
    }
}
