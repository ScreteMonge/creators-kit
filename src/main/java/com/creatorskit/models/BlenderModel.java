package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BlenderModel
{
    boolean useVertexColours;
    int[][] vertices;
    int[][] faces;
    double[][] vertexColours;
    int[] vertexColourIndex;
    double[][] faceColours;
    int[] faceColourIndex;
    byte[] priorities;
    int[] clientTicks;
    int[][][] animVertices;

    public void scale(int widthScale, int heightScale)
    {
        for (int i = 0; i < vertices.length; i++)
        {
            vertices[i][0] = vertices[i][0] * widthScale / 128;
            vertices[i][1] = vertices[i][1] * heightScale / 128;
            vertices[i][2] = vertices[i][2] * widthScale / 128;
        }
    }
}
