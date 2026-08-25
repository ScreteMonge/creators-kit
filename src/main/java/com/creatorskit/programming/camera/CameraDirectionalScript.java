package com.creatorskit.programming.camera;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CameraDirectionalScript extends CameraScript
{
    private boolean inPOH;
    private float focalX;
    private int offsetX;
    private float focalY;
    private float focalZ;
    private int offsetZ;

    public CameraDirectionalScript(CameraMotionType type, EaseType ease, double pitch, double yaw, int scale, boolean inPOH, float focalX, int offsetX, float focalY, float focalZ, int offsetZ)
    {
        super(type, ease, pitch, yaw, scale);
        this.inPOH = inPOH;
        this.focalX = focalX;
        this.offsetX = offsetX;
        this.focalY = focalY;
        this.focalZ = focalZ;
        this.offsetZ = offsetZ;
    }
}
