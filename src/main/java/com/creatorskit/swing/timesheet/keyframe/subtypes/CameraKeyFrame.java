package com.creatorskit.swing.timesheet.keyframe.subtypes;

import com.creatorskit.programming.camera.CameraScript;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CameraKeyFrame extends KeyFrame
{
    private CameraScript script;

    public CameraKeyFrame(double tick, CameraScript script)
    {
        super(KeyFrameType.CAMERA, tick);
        this.script = script;
    }
}