package com.creatorskit.swing.timesheet.keyframe;

public enum KeyFrameType
{
    NULL,
    MOVEMENT,
    ANIMATION,
    SPAWN,
    MODEL,
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
            case MOVEMENT:
                return 0;
            case ANIMATION:
                return 1;
            case ORIENTATION:
                return 2;
            case SPAWN:
                return 3;
            case MODEL:
                return 4;
            case TEXT:
                return 5;
            case OVERHEAD:
                return 6;
            case HITSPLAT:
                return 7;
            case HEALTHBAR:
                return 8;
        }
    }

    public static int getTotalFrameTypes()
    {
        return 9;
    }
}
