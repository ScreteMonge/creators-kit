package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.SpriteID;

@Getter
@AllArgsConstructor
public enum HitsplatSprite
{
    NONE("None", SpriteID.HITSPLAT_BLUE_MISS, 0),
    BLOCK("Block", SpriteID.HITSPLAT_BLUE_MISS, 0),
    DAMAGE("Damage", 1359, 0),
    POISON("Poison", SpriteID.HITSPLAT_GREEN_POISON, 0),
    VENOM("Venom", SpriteID.HITSPLAT_DARK_GREEN_VENOM, 0),
    DISEASE("Disease", 1361, 0),
    HEAL("Heal", 1629, 0)
    ;

    private final String name;
    private final int spriteID;
    private final int file;

    @Override
    public String toString()
    {
        return name;
    }
}
