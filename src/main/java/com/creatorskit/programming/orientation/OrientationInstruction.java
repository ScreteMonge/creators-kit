package com.creatorskit.programming.orientation;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OrientationInstruction
{
    private KeyFrameType type;
    private boolean setOrientation;
}
