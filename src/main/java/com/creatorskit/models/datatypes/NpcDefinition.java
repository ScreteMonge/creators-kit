package com.creatorskit.models.datatypes;

import lombok.Data;

@Data
public class NpcDefinition
{
    public final int id;
    public String name = "null";
    public int size = 1;
    public int[] models;
    public int standingAnimation = -1;
    public int idleRotateLeftAnimation = -1;
    public int idleRotateRightAnimation = -1;
    public int walkingAnimation = -1;
    public int rotate180Animation = -1;
    public int rotateLeftAnimation = -1;
    public int rotateRightAnimation = -1;
    public int runAnimation = -1;
    public int runRotate180Animation = -1;
    public int runRotateLeftAnimation = -1;
    public int runRotateRightAnimation = -1;
    public short[] recolorToFind = new short[0];
    public short[] recolorToReplace = new short[0];
    public int widthScale = 128;
    public int heightScale = 128;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}
