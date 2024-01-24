package com.creatorskit.models;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CustomModelType
{
    FORGED("Forged"),
    CACHE_NPC("NPC"),
    CACHE_OBJECT("Object"),
    CACHE_PLAYER("Player"),
    CACHE_GROUND_ITEM("Ground Item"),
    BLENDER("Blender")

    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}
