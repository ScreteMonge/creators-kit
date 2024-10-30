package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NPCData
{
    private final int id;
    private String name;
    private final int[] models;
    private final int standingAnimation;
    private final int walkingAnimation;
    private final int runAnimation;
    private final int idleRotateLeftAnimation;
    private final int idleRotateRightAnimation;
    private final int rotate180Animation;
    private final int rotateLeftAnimation;
    private final int rotateRightAnimation;
    private final int widthScale;
    private final int heightScale;
    private final int[] recolorToReplace;
    private final int[] recolorToFind;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}
