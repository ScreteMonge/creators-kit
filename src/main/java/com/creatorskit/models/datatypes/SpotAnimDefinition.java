package com.creatorskit.models.datatypes;

import lombok.Data;

@Data
public class SpotAnimDefinition
{
    public String name;
    public int rotation = 0;
    public short[] textureToReplace = new short[0];
    public int id;
    public short[] textureToFind = new short[0];
    public int resizeY = 128;
    public int animationId = -1;
    public short[] recolorToFind = new short[0];
    public short[] recolorToReplace = new short[0];
    public int resizeX = 128;
    public int modelId;
    public int ambient = 0;
    public int contrast = 0;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}
