package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BlenderModel
{
    boolean useVertexColours;
    int[][] vertices;
    int[][] faces;
    double[][] vertexColours;
    double[][] faceColours;
    int[] faceColourIndex;
    byte[] priorities;
}
