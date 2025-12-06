package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ModelStats
{
    private int modelId;
    private String name;
    private BodyPart bodyPart;
    private short[] recolourFrom;
    private short[] recolourTo;
    private short[] textureFrom;
    private short[] textureTo;
    private int resizeX;
    private int resizeY;
    private int resizeZ;
    private int translateZ;
    private CustomLighting lighting;
}
