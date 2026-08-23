package com.creatorskit.programming.camera;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CameraScript
{
    private boolean inPOH;
    private float focalX;
    private int offsetX;
    private float focalY;
    private float focalZ;
    private int offsetZ;
    private double pitch;
    private double yaw;
    private int scale;
}
