package com.creatorskit.swing.timesheet.keyframe.keyframeactions;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameCategory;
import lombok.Getter;

@Getter
public class KeyFrameCameraAction extends KeyFrameAction
{
    public KeyFrameCameraAction(KeyFrame keyFrame, KeyFrameActionType actionType)
    {
        super(actionType, KeyFrameCategory.CAMERA, keyFrame);
    }
}