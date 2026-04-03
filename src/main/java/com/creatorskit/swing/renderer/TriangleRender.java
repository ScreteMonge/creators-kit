package com.creatorskit.swing.renderer;

import java.awt.*;

public class TriangleRender extends Triangle
{
    double avgDepth;

    public TriangleRender(Vertex v1, Vertex v2, Vertex v3, Color c1, Color c2, Color c3, Vector3 n1, Vector3 n2, Vector3 n3, int alpha, double avgDepth)
    {
        super(v1, v2, v3, c1, c2, c3, n1, n2, n3, alpha);
        this.avgDepth = avgDepth;
    }
}
