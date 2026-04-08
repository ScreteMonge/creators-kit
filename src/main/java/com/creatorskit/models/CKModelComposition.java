package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Model;

@AllArgsConstructor
@Getter
@Setter
public class CKModelComposition
{
    private String name;
    private Model model;
    private int modelId;
    private int groupId;
    private int[] kitRecolours;
    private short[] coloursFrom;
    private short[] coloursTo;
    private short[] texturesFrom;
    private short[] texturesTo;
    private boolean invertFaces;
    private boolean overridePriority;
    private CustomLighting customLighting;

    private int tx;
    private int ty;
    private int tz;
    private int sx;
    private int sy;
    private int sz;
    private int rotate;
}
