package com.creatorskit.swing.renderer;

public class Matrix4
{
    double[] values;

    Matrix4(double[] values) {
        this.values = values;
    }

    public Matrix4 multiply(Matrix4 other)
    {
        double[] result = new double[16];
        for (int row = 0; row < 4; row++)
        {
            for (int col = 0; col < 4; col++)
            {
                for (int i = 0; i < 4; i++)
                {
                    result[row * 4 + col] += this.values[row * 4 + i] * other.values[i * 4 + col];
                }
            }
        }
        return new Matrix4(result);
    }

    public Vertex transform(Vertex in)
    {
        return new Vertex(
                in.x * values[0] + in.y * values[4] + in.z * values[8] + in.w * values[12],
                in.x * values[1] + in.y * values[5] + in.z * values[9] + in.w * values[13],
                in.x * values[2] + in.y * values[6] + in.z * values[10] + in.w * values[14],
                in.x * values[3] + in.y * values[7] + in.z * values[11] + in.w * values[15]
        );
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int row = 0; row < 4; row++)
        {
            for (int col = 0; col < 4; col++)
            {
                sb.append(values[row * 4 + col]);
                if (col != 3) {
                    sb.append(",");
                }
            }
            if (row != 3) {
                sb.append(";\n ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
