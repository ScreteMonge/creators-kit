package com.creatorskit.swing.renderer;

import lombok.AllArgsConstructor;

import java.awt.*;

@AllArgsConstructor
public class Triangle
{
    Vertex v1;
    Vertex v2;
    Vertex v3;
    Color c1, c2, c3;
    Vector3 n1, n2, n3;
    int alpha;
}
