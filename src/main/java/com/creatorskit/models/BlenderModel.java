package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BlenderModel
{
    int[][] vertices;
    int[][] faces;
    double[][] colours;
    byte[] transparencies;
    byte[] priorities;
}
