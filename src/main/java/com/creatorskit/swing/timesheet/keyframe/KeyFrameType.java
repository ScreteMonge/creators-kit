package com.creatorskit.swing.timesheet.keyframe;

public enum KeyFrameType
{
    LOCATION,
    ANIMATION,
    SPAWN,
    ORIENTATION,
    TEXT,
    OVERHEAD,
    HITSPLAT,
    HEALTHBAR;

    public static int getIndex(KeyFrameType type)
    {
        switch (type)
        {
            default:
            case LOCATION:
                return 0;
            case ANIMATION:
                return 1;
            case SPAWN:
                return 2;
            case ORIENTATION:
                return 3;
            case TEXT:
                return 4;
            case OVERHEAD:
                return 5;
            case HITSPLAT:
                return 6;
            case HEALTHBAR:
                return 7;
        }
    }
}
