package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BlenderModel
{
    boolean vertexColours;
    int[][] vertices;
    int[][] faces;
    double[][] colours;
    byte[] transparencies;
    byte[] priorities;
}
