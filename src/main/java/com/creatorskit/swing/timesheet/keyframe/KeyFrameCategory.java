package com.creatorskit.swing.timesheet.keyframe;

public enum KeyFrameCategory
{
    CHARACTER,
    CAMERA,
    SOUND;

    public static boolean contains(KeyFrameCategory category, KeyFrameType type)
    {
        if (category == KeyFrameCategory.CHARACTER)
        {
            if (type == KeyFrameType.MOVEMENT
            || type == KeyFrameType.ANIMATION
            || type == KeyFrameType.ORIENTATION
            || type == KeyFrameType.SPAWN
            || type == KeyFrameType.MODEL
            || type == KeyFrameType.SPOTANIM
            || type == KeyFrameType.SPOTANIM2
            || type == KeyFrameType.TEXT
            || type == KeyFrameType.OVERHEAD
            || type == KeyFrameType.HEALTH
            || type == KeyFrameType.HITSPLAT_1
            || type == KeyFrameType.HITSPLAT_2
            || type == KeyFrameType.HITSPLAT_3
            || type == KeyFrameType.HITSPLAT_4)
            {
                return true;
            }

            return false;
        }

        if (category == KeyFrameCategory.CAMERA && type == KeyFrameType.CAMERA)
        {
            return true;
        }

        return false;
    }
}