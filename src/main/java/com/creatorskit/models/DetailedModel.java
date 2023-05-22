package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class DetailedModel
{
    int modelId;
    int xTile;
    int yTile;
    int zTile;
    int xTranslate;
    int yTranslate;
    int zTranslate;
    int xScale;
    int yScale;
    int zScale;
    int rotate;
    String recolourNew;
    String recolourOld;
}
