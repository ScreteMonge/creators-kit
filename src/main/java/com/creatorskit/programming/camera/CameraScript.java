package com.creatorskit.programming.camera;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CameraScript
{
    private CameraMotionType type;
    private EaseType ease;
    private double pitch;
    private double yaw;
    private int scale;
}
