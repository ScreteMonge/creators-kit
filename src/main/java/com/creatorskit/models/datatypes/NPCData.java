package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NPCData
{
    private final int id;
    private final String name;
    private final int[] models;
    private final int standingAnimation;
    private final int walkingAnimation;
    private final int widthScale;
    private final int heightScale;
    private final int[] recolorToReplace;
    private final int[] recolorToFind;
}
