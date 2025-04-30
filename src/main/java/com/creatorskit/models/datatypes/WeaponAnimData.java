package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeaponAnimData
{
    private final int[] id;
    private final int[] animations;

    public static int getAnimation(WeaponAnimData weaponAnimData, PlayerAnimationType type)
    {
        int[] animations = weaponAnimData.getAnimations();
        switch (type)
        {
            default:
            case IDLE:
                return animations[0];
            case WALK:
                return animations[1];
            case RUN:
                return animations[2];
            case IDLE_ROTATE_LEFT:
            case IDLE_ROTATE_RIGHT:
                return animations[6];
            case ROTATE_180:
                return animations[3];
            case ROTATE_LEFT:
                return animations[4];
            case ROTATE_RIGHT:
                return animations[5];
            case SPECIAL:
                return animations[10];
            case STAB:
                return animations[7];
            case SLASH:
                return animations[8];
            case SLASH_2:
                return animations[12];
            case CRUSH:
                return animations[9];
            case CRUSH_2:
                return animations[13];
            case DEFEND:
                return animations[11];
        }
    }
}
