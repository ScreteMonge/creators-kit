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
    HITSPLAT_4("Hitsplat 4","H4"),
    PROJECTILE("Projectile","P"),
    SHIELD("Shield","SH"),
    SPECIAL("Special","SP"),
    SCREEN_FADE("Screen Fade","SF"),
    SCREEN_SHAKE("Screen Shake","SK"),
    CAMERA("Camera","CM");

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
            HITSPLAT_4,
            PROJECTILE,
            SHIELD,
            SPECIAL,
            SCREEN_FADE,
            SCREEN_SHAKE,
            CAMERA
    };

    /**
     * Display-order array used by the timeline UI -- types are listed by
     * {@link #getName()} alphabetically rather than the historical "movement
     * first" arrangement. Persistence (CharacterSave field names) is unchanged;
     * only the rendered row order shifts. Use {@link #getDisplayIndex(KeyFrameType)}
     * to look up a type's position here.
     */
    public static final KeyFrameType[] ALL_KEYFRAME_TYPES_ALPHABETICAL = new KeyFrameType[]{
            ANIMATION,
            CAMERA,
            HEALTH,
            HITSPLAT_1,
            HITSPLAT_2,
            HITSPLAT_3,
            HITSPLAT_4,
            MODEL,
            MOVEMENT,
            ORIENTATION,
            OVERHEAD,
            PROJECTILE,
            SCREEN_FADE,
            SCREEN_SHAKE,
            SHIELD,
            SPAWN,
            SPECIAL,
            SPOTANIM,
            SPOTANIM2,
            TEXT
    };

    /**
     * Position of {@code type} in the alphabetical display array, or -1 if not
     * present. Use this for row-y calculations in the timeline sheet so types
     * land under their alphabetical label.
     */
    public static int getDisplayIndex(KeyFrameType type)
    {
        for (int i = 0; i < ALL_KEYFRAME_TYPES_ALPHABETICAL.length; i++)
        {
            if (ALL_KEYFRAME_TYPES_ALPHABETICAL[i] == type)
            {
                return i;
            }
        }
        return -1;
    }

    public static final KeyFrameType[] HITSPLAT_TYPES = new KeyFrameType[]{KeyFrameType.HITSPLAT_1, KeyFrameType.HITSPLAT_2, KeyFrameType.HITSPLAT_3, KeyFrameType.HITSPLAT_4};
    public static final KeyFrameType[] SPOTANIM_TYPES = new KeyFrameType[]{KeyFrameType.SPOTANIM, KeyFrameType.SPOTANIM2};
    /** Bar keyframes that share the overhead-bar rendering pipeline (HP / shield / special). */
    public static final KeyFrameType[] BAR_TYPES = new KeyFrameType[]{KeyFrameType.HEALTH, KeyFrameType.SHIELD, KeyFrameType.SPECIAL};

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
            case PROJECTILE:
                return 14;
            case SHIELD:
                return 15;
            case SPECIAL:
                return 16;
            case SCREEN_FADE:
                return 17;
            case SCREEN_SHAKE:
                return 18;
            case CAMERA:
                return 19;
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
            case 14:
                return PROJECTILE;
            case 15:
                return SHIELD;
            case 16:
                return SPECIAL;
            case 17:
                return SCREEN_FADE;
            case 18:
                return SCREEN_SHAKE;
            case 19:
                return CAMERA;
        }
    }

    public static int getTotalFrameTypes()
    {
        return 20;
    }

    public static KeyFrameType[] createDefaultSummary()
    {
        return new KeyFrameType[]{MOVEMENT, ANIMATION, ORIENTATION};
    }
}
