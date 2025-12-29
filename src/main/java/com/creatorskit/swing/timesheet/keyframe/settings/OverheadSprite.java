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
    SKULL_HIGH_RISK("High Risk", 525, 0),
    SKULL_FIGHT_PITS("Fight Pits", 524, 0),
    SKULL_BH_1("Bounty Hunter 1", 6316, 0),
    SKULL_BH_2("Bounty Hunter 2", 6333, 0),
    SKULL_BH_3("Bounty Hunter 3", 6334, 0),
    SKULL_BH_4("Bounty Hunter 4", 6335, 0),
    SKULL_BH_5("Bounty Hunter 5", 6336, 0),
    SKULL_FORINTHRY("Forinthry", 526, 0),
    SKULL_FORINTHRY_1("Forinthry 1", 2271, 0),
    SKULL_FORINTHRY_2("Forinthry 2", 2501, 0),
    SKULL_FORINTHRY_3("Forinthry 3", 2502, 0),
    SKULL_FORINTHRY_4("Forinthry 4", 2503, 0),
    SKULL_FORINTHRY_5("Forinthry 5", 2504, 0),
    SKULL_DEADMAN_1("Deadman 1", 1097, 0),
    SKULL_DEADMAN_2("Deadman 2", 1221, 0),
    SKULL_DEADMAN_3("Deadman 3", 1222, 0),
    SKULL_DEADMAN_4("Deadman 4", 1223, 0),
    SKULL_DEADMAN_5("Deadman 5", 1224, 0),
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
