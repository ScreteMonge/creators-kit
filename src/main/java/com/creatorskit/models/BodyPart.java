package com.creatorskit.models;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BodyPart
{
    NA("Na"),
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

    @Override
    public String toString()
    {
        return name;
    }
}
