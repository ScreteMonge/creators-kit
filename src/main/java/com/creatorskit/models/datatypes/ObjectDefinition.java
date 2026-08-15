package com.creatorskit.models.datatypes;

import lombok.Data;

@Data
public class ObjectDefinition
{
    private int id;
    private short[] retextureToFind = new short[0];
    private String name = "null";
    private int[] objectModels;
    private int[] objectTypes;
    private short[] recolorToFind = new short[0];
    private short[] textureToReplace = new short[0];
    private int animationID = -1;
    private int ambient = 0;
    private int contrast = 0;
    private short[] recolorToReplace = new short[0];
    private int modelSizeX = 128;
    private int modelSizeHeight = 128;
    private int modelSizeY = 128;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}