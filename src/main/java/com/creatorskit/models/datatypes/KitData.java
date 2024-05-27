package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KitData
{
    private final int id;
    private final int bodyPartId;
    private final int[] models;
    private final int[] chatheadModels;
    private final int[] recolorToReplace;
    private final int[] recolorToFind;
}
