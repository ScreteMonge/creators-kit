package com.creatorssuite.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModelStats
{
    private int modelId;
    private short[] recolourFrom;
    private short[] recolourTo;
}
