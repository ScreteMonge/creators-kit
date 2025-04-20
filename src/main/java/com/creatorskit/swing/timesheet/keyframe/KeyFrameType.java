package com.creatorskit.swing.timesheet.keyframe;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum KeyFrameType
{
    NULL("Null", "N"),
    MOVEMENT("Movement","M"),
    ANIMATION("Animation","A"),
    ORIENTATION("Orientation","O"),
    SPAWN("Spawn","S"),
    MODEL("Model","MD"),
    SPOTANIM("SpotAnim 1","S1"),
    SPOTANIM2("SpotAnim 2","S2"),
    TEXT("Text","T"),
    OVERHEAD("Overhead","OH"),
    HEALTH("Health","H"),
    HITSPLAT_1("Hitsplat 1","H1"),
    HITSPLAT_2("Hitsplat 2","H2"),
    HITSPLAT_3("Hitsplat 3","H3"),
    HITSPLAT_4("Hitsplat 4","H4");

    private final String name;
    private final String shortHand;

    public String toString()
    {
        return name;
    }

    public static final KeyFrameType[] ALL_KEYFRAME_TYPES = new KeyFrameType[]{
            MOVEMENT,
            ANIMATION,
            ORIENTATION,
            SPAWN,
            MODEL,
            SPOTANIM,
            SPOTANIM2,
            TEXT,
            OVERHEAD,
            HEALTH,
            HITSPLAT_1,
            HITSPLAT_2,
            HITSPLAT_3,
            HITSPLAT_4
    };

    public static final KeyFrameType[] HITSPLAT_TYPES = new KeyFrameType[]{KeyFrameType.HITSPLAT_1, KeyFrameType.HITSPLAT_2, KeyFrameType.HITSPLAT_3, KeyFrameType.HITSPLAT_4};
    public static final KeyFrameType[] SPOTANIM_TYPES = new KeyFrameType[]{KeyFrameType.SPOTANIM, KeyFrameType.SPOTANIM2};

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
            case SPOTANIM:
                return 5;
            case SPOTANIM2:
                return 6;
            case TEXT:
                return 7;
            case OVERHEAD:
                return 8;
            case HEALTH:
                return 9;
            case HITSPLAT_1:
                return 10;
            case HITSPLAT_2:
                return 11;
            case HITSPLAT_3:
                return 12;
            case HITSPLAT_4:
                return 13;
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
                return SPOTANIM;
            case 6:
                return SPOTANIM2;
            case 7:
                return TEXT;
            case 8:
                return OVERHEAD;
            case 9:
                return HEALTH;
            case 10:
                return HITSPLAT_1;
            case 11:
                return HITSPLAT_2;
            case 12:
                return HITSPLAT_3;
            case 13:
                return HITSPLAT_4;
        }
    }

    public static int getTotalFrameTypes()
    {
        return 14;
    }

    public static KeyFrameType[] createDefaultSummary()
    {
        return new KeyFrameType[]{MOVEMENT, ANIMATION, ORIENTATION};
    }
}
