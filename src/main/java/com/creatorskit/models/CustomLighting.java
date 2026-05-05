package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CustomLighting
{
    private int ambient;
    private int contrast;
    private int x;
    private int y;
    private int z;

    public static CustomLighting fromLightingStyle(LightingStyle ls)
    {
        return new CustomLighting(
                ls.getAmbient(),
                ls.getContrast(),
                ls.getX(),
                ls.getY(),
                ls.getZ()
        );
    }
}
