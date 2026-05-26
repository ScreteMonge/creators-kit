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
    COLOUR("Colour","CL"),
    SCREEN_FADE("Screen Fade","SF"),
    SCREEN_SHAKE("Screen Shake","SK"),
    CAMERA("Camera","CM"),
    // Renamed Sound 1..4 -> Area Sound 1..4 to clarify that these
    // globals are area-positioned ambient sources, separate from the
    // per-Character SOUND row below. Enum value names stay SOUND_1..4
    // so persistence + every code reference holds.
    SOUND_1("Area Sound 1","SD1"),
    SOUND_2("Area Sound 2","SD2"),
    SOUND_3("Area Sound 3","SD3"),
    SOUND_4("Area Sound 4","SD4"),
    /**
     * Per-Character sound. Plays through {@code client.playSoundEffect(id)}
     * (one-arg overload that respects the player's in-game volume setting)
     * when the playhead crosses the kf's tick during play.
     */
    SOUND("Sound","SO");

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
            COLOUR,
            SCREEN_FADE,
            SCREEN_SHAKE,
            CAMERA,
            SOUND_1,
            SOUND_2,
            SOUND_3,
            SOUND_4,
            SOUND
    };

    /**
     * Display-order array used by the timeline UI -- types are listed by
     * {@link #getName()} alphabetically rather than the historical "movement
     * first" arrangement. Persistence (CharacterSave field names) is unchanged;
     * only the rendered row order shifts. Use {@link #getDisplayIndex(KeyFrameType)}
     * to look up a type's position here. Contains all 20 types -- kept for
     * paths that iterate everything (e.g. CTRL+A, save/load summary).
     */
    public static final KeyFrameType[] ALL_KEYFRAME_TYPES_ALPHABETICAL = new KeyFrameType[]{
            ANIMATION,
            // Area Sound 1..4 sort under 'A' now that the display name
            // changed; the underlying enum values stay SOUND_1..4.
            SOUND_1,
            SOUND_2,
            SOUND_3,
            SOUND_4,
            CAMERA,
            COLOUR,
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
            // Per-Character "Sound" sits between Shield and Spawn (S-H,
            // S-O, S-P alphabetical).
            SOUND,
            SPAWN,
            SPECIAL,
            SPOTANIM,
            SPOTANIM2,
            TEXT
    };

    /**
     * Per-Character (local) types in alphabetical display order. Drives the
     * row layout of the main local AttributeSheet -- globals are excluded
     * here because they live in their own dedicated view now.
     */
    public static final KeyFrameType[] LOCAL_KEYFRAME_TYPES_ALPHABETICAL = new KeyFrameType[]{
            ANIMATION,
            COLOUR,
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
            SHIELD,
            // Per-Character "Sound" sits between Shield and Spawn
            // (S-H, S-O, S-P alphabetical).
            SOUND,
            SPAWN,
            SPECIAL,
            SPOTANIM,
            SPOTANIM2,
            TEXT
    };

    /**
     * Global types in alphabetical display order. Drives the row layout of
     * the dedicated global AttributeSheet so the user can author and
     * marquee Camera / Fade / Shake without contaminating per-Object data.
     */
    public static final KeyFrameType[] GLOBAL_KEYFRAME_TYPES_ALPHABETICAL = new KeyFrameType[]{
            CAMERA,
            SCREEN_FADE,
            SCREEN_SHAKE,
            SOUND_1,
            SOUND_2,
            SOUND_3,
            SOUND_4
    };

    /**
     * Position of {@code type} in {@link #ALL_KEYFRAME_TYPES_ALPHABETICAL},
     * or -1 if not present. Kept for full-array callers; per-view UIs should
     * prefer {@link #getLocalDisplayIndex} / {@link #getGlobalDisplayIndex}.
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

    /** Position of {@code type} in the local-view row order, or -1 if it's a global. */
    public static int getLocalDisplayIndex(KeyFrameType type)
    {
        for (int i = 0; i < LOCAL_KEYFRAME_TYPES_ALPHABETICAL.length; i++)
        {
            if (LOCAL_KEYFRAME_TYPES_ALPHABETICAL[i] == type) return i;
        }
        return -1;
    }

    /** Position of {@code type} in the global-view row order, or -1 if it's local. */
    public static int getGlobalDisplayIndex(KeyFrameType type)
    {
        for (int i = 0; i < GLOBAL_KEYFRAME_TYPES_ALPHABETICAL.length; i++)
        {
            if (GLOBAL_KEYFRAME_TYPES_ALPHABETICAL[i] == type) return i;
        }
        return -1;
    }

    public static boolean isGlobal(KeyFrameType type)
    {
        return type == CAMERA || type == SCREEN_FADE || type == SCREEN_SHAKE
                || type == SOUND_1 || type == SOUND_2 || type == SOUND_3 || type == SOUND_4;
    }

    public static final KeyFrameType[] HITSPLAT_TYPES = new KeyFrameType[]{KeyFrameType.HITSPLAT_1, KeyFrameType.HITSPLAT_2, KeyFrameType.HITSPLAT_3, KeyFrameType.HITSPLAT_4};
    public static final KeyFrameType[] SPOTANIM_TYPES = new KeyFrameType[]{KeyFrameType.SPOTANIM, KeyFrameType.SPOTANIM2};
    /** Bar keyframes that share the overhead-bar rendering pipeline (HP / shield / special). */
    public static final KeyFrameType[] BAR_TYPES = new KeyFrameType[]{KeyFrameType.HEALTH, KeyFrameType.SHIELD, KeyFrameType.SPECIAL};
    /** All 4 sound kf slots in order. */
    public static final KeyFrameType[] SOUND_TYPES = new KeyFrameType[]{KeyFrameType.SOUND_1, KeyFrameType.SOUND_2, KeyFrameType.SOUND_3, KeyFrameType.SOUND_4};

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
            case COLOUR:
                return 20;
            case SOUND_1:
                return 21;
            case SOUND_2:
                return 22;
            case SOUND_3:
                return 23;
            case SOUND_4:
                return 24;
            case SOUND:
                return 25;
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
            case 20:
                return COLOUR;
            case 21:
                return SOUND_1;
            case 22:
                return SOUND_2;
            case 23:
                return SOUND_3;
            case 24:
                return SOUND_4;
            case 25:
                return SOUND;
        }
    }

    public static int getTotalFrameTypes()
    {
        return 26;
    }

    public static KeyFrameType[] createDefaultSummary()
    {
        return new KeyFrameType[]{MOVEMENT, ANIMATION, ORIENTATION};
    }
}
