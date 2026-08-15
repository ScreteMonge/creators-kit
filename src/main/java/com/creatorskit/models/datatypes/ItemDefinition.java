package com.creatorskit.models.datatypes;

import lombok.Data;

@Data
public class ItemDefinition
{
    public final int id;

    public String name = "null";

    public int resizeX = 128;
    public int resizeY = 128;
    public int resizeZ = 128;

    public int inventoryModel;

    public int wearPos1 = -1;
    public int wearPos2 = -1;
    public int wearPos3 = -1;

    public short[] colorFind = new short[0];
    public short[] colorReplace = new short[0];;
    public short[] textureFind = new short[0];;
    public short[] textureReplace = new short[0];;

    public int maleModel0 = -1;
    public int maleModel1 = -1;
    public int maleModel2 = -1;
    public int maleOffset;
    public int maleHeadModel = -1;
    public int maleHeadModel2 = -1;

    public int femaleModel0 = -1;
    public int femaleModel1 = -1;
    public int femaleModel2 = -1;
    public int femaleOffset;
    public int femaleHeadModel = -1;
    public int femaleHeadModel2 = -1;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}