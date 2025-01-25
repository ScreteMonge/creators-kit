package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.SpriteID;

@Getter
@AllArgsConstructor
public enum OverheadSprite
{
    NONE("None", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 0),
    SKULL("Skull", SpriteID.PLAYER_KILLER_SKULL, 0),
    PROTECT_MAGIC("Protect Magic", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 2),
    PROTECT_RANGED("Protect Ranged", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 1),
    PROTECT_MELEE("Protect Melee", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 0),
    REDEMPTION("Redemption", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 5),
    RETRIBUTION("Retribution", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 3),
    SMITE("Smite", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 4),
    PROTECT_RANGE_MAGE("Protect Ranged + Mage", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 6),
    PROTECT_RANGE_MELEE("Protect Ranged + Melee", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 7),
    PROTECT_MAGE_MELEE("Protect Mage + Melee", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 8),
    PROTECT_RANGE_MAGE_MELEE("Protect All", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 9),
    DEFLECT_MAGE("Deflect Magic", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 14),
    DEFLECT_RANGE("Deflect Ranged", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 13),
    DEFLECT_MELEE("Deflect Melee", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 12),
    SOUL_SPLIT("Soul Split", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 11),
    WRATH("Wrath", SpriteID.OVERHEAD_PROTECT_FROM_MELEE, 10),
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
