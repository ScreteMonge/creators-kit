package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModelStats
{
    private int modelId;
    private BodyPart bodyPart;
    private short[] recolourFrom;
    private short[] recolourTo;
    private int resizeX;
    private int resizeY;
    private int resizeZ;
    private Lighting lighting;
}
