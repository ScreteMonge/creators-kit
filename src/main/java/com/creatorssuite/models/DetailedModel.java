package com.creatorssuite.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class DetailedModel
{
    int modelId;
    int xTranslate;
    int yTranslate;
    int zTranslate;
    int xScale;
    int yScale;
    int zScale;
    int rotate;
    short[] recolourNew;
    short[] recolourOld;
}
