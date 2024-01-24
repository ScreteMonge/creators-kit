package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CustomModelComp
{
    private int customModelId;
    private CustomModelType type;
    private int modelId;
    private ModelStats[] modelStats;
    private int[] kitRecolours;
    private DetailedModel[] detailedModels;
    private BlenderModel blenderModel;
    private LightingStyle lightingStyle;
    private boolean priority;
    private String name;
}
