package com.creatorskit.swing.timesheet.keyframe.keyframeactions;

import com.creatorskit.Character;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameCategory;
import lombok.Getter;

@Getter
public class KeyFrameCharacterAction extends KeyFrameAction
{
    private final Character character;

    public KeyFrameCharacterAction(KeyFrame keyFrame, Character character, KeyFrameActionType actionType)
    {
        super(actionType, KeyFrameCategory.CHARACTER, keyFrame);
        this.character = character;
    }
}
