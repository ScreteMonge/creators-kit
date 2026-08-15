package com.creatorskit.models.datatypes;

import lombok.Data;

@Data
public class KitDefinition
{
    private final int id;
    public short[] recolorToReplace = new short[0];
    public short[] recolorToFind = new short[0];
    public short[] retextureToFind = new short[0];
    public short[] retextureToReplace = new short[0];
    public int bodyPartId = -1;
    public int[] models;
    public int[] chatheadModels = new int[]
            {
                    -1, -1, -1, -1, -1
            };
}
