package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BodyPart
{
    NA("Name"),
    HEAD("Head"),
    CAPE("Cape"),
    AMULET("Amulet"),
    WEAPON("Weapon"),
    TORSO("Torso"),
    SHIELD("Shield"),
    ARMS("Arms"),
    LEGS("Legs"),
    HAIR("Hair"),
    HANDS("Hands"),
    FEET("Feet"),
    JAW("Jaw"),
    SPOTANIM("SpotAnim");

    private final String name;

    public static BodyPart wearPosToBodyPart(int wearPos)
    {
        switch (wearPos)
        {
            case 0:
                return HEAD;
            case 1:
                return CAPE;
            case 2:
                return AMULET;
            case 3:
                return WEAPON;
            case 4:
                return TORSO;
            case 5:
                return SHIELD;
            case 6:
                return ARMS;
            case 7:
                return LEGS;
            case 8:
                return HAIR;
            case 9:
                return HANDS;
            case 10:
                return FEET;
            case 11:
                return JAW;
            case 12:
                return SPOTANIM;
            case -1:
            default:
                return NA;
        }
    }

    @Override
    public String toString()
    {
        return name;
    }
}
