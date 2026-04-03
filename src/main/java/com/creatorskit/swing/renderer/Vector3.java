package com.creatorskit.swing.renderer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Vector3
{
    double x, y, z;

    public Vector3 add(Vector3 other)
    {
        return new Vector3(
                this.x + other.x,
                this.y + other.y,
                this.z + other.z
        );
    }

    public double dot(Vector3 other)
    {
        return this.x * other.x +
                this.y * other.y +
                this.z * other.z;
    }

    public void normalize()
    {
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len != 0) {
            x /= len;
            y /= len;
            z /= len;
        }
    }
}
