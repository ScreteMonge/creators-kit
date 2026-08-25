package com.creatorskit.saves;

import com.creatorskit.programming.camera.CameraMotionType;
import com.creatorskit.programming.camera.CameraScript;
import com.creatorskit.programming.camera.EaseType;
import lombok.Getter;

@Getter
public class CameraScriptSave extends CameraScript
{
    private double tick;
    private boolean inPOH;
    private float focalX;
    private int offsetX;
    private float focalY;
    private float focalZ;
    private int offsetZ;
    private String id;

    public CameraScriptSave(CameraMotionType type, EaseType ease, double pitch, double yaw, int scale, double tick, boolean inPOH, float focalX, int offsetX, float focalY, float focalZ, int offsetZ, String id)
    {
        super(type, ease, pitch, yaw, scale);
        this.tick = tick;
        this.inPOH = inPOH;
        this.focalX = focalX;
        this.offsetX = offsetX;
        this.focalY = focalY;
        this.focalZ = focalZ;
        this.offsetZ = offsetZ;
        this.id = id;
    }
}
