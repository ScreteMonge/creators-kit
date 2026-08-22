package com.creatorskit.swing.timesheet.keyframe.subtypes;

import com.creatorskit.programming.camera.CameraScript;
import com.creatorskit.programming.camera.EaseType;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.Getter;

@Getter
public class CameraKeyFrame extends KeyFrame
{
    private final CameraScript script;
    private final EaseType ease;

    public CameraKeyFrame(double tick, CameraScript script, EaseType ease)
    {
        super(KeyFrameType.CAMERA, tick);
        this.script = script;
        this.ease = ease;
    }
}