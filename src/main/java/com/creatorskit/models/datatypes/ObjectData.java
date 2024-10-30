package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ObjectData
{
    private final int id;
    private final String name;
    private final int animationId;
    private final int[] objectModels;
    private final int[] objectTypes;
    private final int modelSizeX;
    private final int modelSizeY;
    private final int modelSizeZ;
    private final int ambient;
    private final int contrast;
    private final int[] recolorToReplace;
    private final int[] recolorToFind;
    private final int[] textureToReplace;
    private final int[] retextureToFind;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}
