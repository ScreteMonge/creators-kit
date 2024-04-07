package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class DetailedModel
{
    private String name;
    private int modelId;
    private int group;
    private int xTile;
    private int yTile;
    private int zTile;
    private int xTranslate;
    private int yTranslate;
    private int zTranslate;
    private int xScale;
    private int yScale;
    private int zScale;
    private int rotate;
    private String recolourNew;
    private String recolourOld;
    private short[] coloursFrom;
    private short[] coloursTo;
    private short[] texturesFrom;
    private short[] texturesTo;
    private boolean invertFaces;
}
