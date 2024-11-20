package com.creatorskit.swing.timesheet.keyframe;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum KeyFrameType
{
    NULL("Null"),
    SUMMARY("Summary"),
    MOVEMENT("Movement"),
    ANIMATION("Animation"),
    SPAWN("Spawn"),
    MODEL("Model"),
    ORIENTATION("Orientation"),
    TEXT("Text"),
    OVERHEAD("Overhead"),
    HITSPLAT("Hitsplat"),
    HEALTHBAR("Healthbar");

    private String name;

    public String toString()
    {
        return name;
    }

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

    public static KeyFrameType getKeyFrameType(int index)
    {
        switch (index)
        {
            default:
            case 0:
                return MOVEMENT;
            case 1:
                return ANIMATION;
            case 2:
                return ORIENTATION;
            case 3:
                return SPAWN;
            case 4:
                return MODEL;
            case 5:
                return TEXT;
            case 6:
                return OVERHEAD;
            case 7:
                return HITSPLAT;
            case 8:
                return HEALTHBAR;
        }
    }

    public static int getTotalFrameTypes()
    {
        return 9;
    }
}
