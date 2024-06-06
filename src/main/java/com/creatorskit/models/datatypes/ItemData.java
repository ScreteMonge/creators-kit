package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemData
{
    private final int id;
    private final String name;
    private final int inventoryModel;
    private final int maleModel0;
    private final int maleModel1;
    private final int maleModel2;
    private final int maleOffset;
    private final int femaleModel0;
    private final int femaleModel1;
    private final int femaleModel2;
    private final int femaleOffset;
    private final int maleHeadModel;
    private final int maleHeadModel2;
    private final int femaleHeadModel;
    private final int femaleHeadModel2;
    private final int resizeX;
    private final int resizeY;
    private final int resizeZ;
    private final int[] colorReplace;
    private final int[] colorFind;
    private final int[] textureReplace;
    private final int[] textureFind;
}
