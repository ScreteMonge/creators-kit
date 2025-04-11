package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpotanimData
{
    private final String name;
    private final int id;
    private final int modelId;
    private final int animationId;
    private final int resizeX;
    private final int resizeY;
    private final int ambient;
    private final int contrast;
    private final int[] recolorToReplace;
    private final int[] recolorToFind;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}
