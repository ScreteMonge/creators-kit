package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.SpriteID;

@Getter
@AllArgsConstructor
public enum HitsplatSprite
{
    ARMOUR("Armour", 1628, 0),
    ARMOUR_MAX("Armour Max", 4557, 0),
    BURN("Burn", 4767, 0),
    BLEED("Bleed", 4564, 0),
    BLOCK("Block", SpriteID.HITSPLAT_BLUE_MISS, 0),
    BLOCK_OTHER("Block Other", 1630, 0),
    CHARGE_UP("Charge Up", 3519, 0),
    CHARGE_UP_OTHER("Charge Up Other", 3569, 0),
    CHARGE_DOWN("Charge Down", 3520, 0),
    CHARGE_DOWN_OTHER("Charge Down Other", 3570, 0),
    CORRUPTION("Corruption", 2270, 0),
    DAMAGE("Damage", 1359, 0),
    DAMAGE_OTHER("Damage Other", 1631, 0),
    DAMAGE_MAX("Max", 3571, 0),
    DISEASE("Disease", 1361, 0),
    DOOM("Doom", 4766, 0),
    FREEZE("Freeze", 4768, 0),
    FREEZE_OTHER("Freeze Other", 4769, 0),
    NO_KILL_CREDIT("No Kill Credit", 3521, 0),
    HEAL("Heal", 1629, 0),
    NONE("None", SpriteID.HITSPLAT_BLUE_MISS, 0),
    POISE("Poise", 4558, 0),
    POISE_OTHER("Poise Other", 4559, 0),
    POISE_MAX("Poise Max", 4560, 0),
    POISON("Poison", SpriteID.HITSPLAT_GREEN_POISON, 0),
    POISON_OTHER("Poison Other", 1632, 0),
    POISON_MAX("Poison Max", 4763, 0),
    PRAYER_DRAIN("Prayer Drain", 4561, 0),
    PRAYER_DRAIN_OTHER("Prayer Drain Other", 4562, 0),
    PRAYER_DRAIN_MAX("Prayer Drain Max", 4563, 0),
    SANITY_DRAIN("Sanity Drain", 4764, 0),
    SANITY_RESTORE("Sanity Restore", 4765, 0),
    SHIELD("Shield", 1419, 0),
    SHIELD_OTHER("Shield Other", 1339, 0),
    SHIELD_MAX("Shield Max", 4556, 0),
    TOTEM_UP_OTHER("Totem Up Other", 1634, 0),
    TOTEM_UP_MAX("Totem Up Max", 3572, 0),
    TOTEM_DOWN_MAX("Totem Down Max", 3573, 0),
    VENOM("Venom", SpriteID.HITSPLAT_DARK_GREEN_VENOM, 0),
    VENOM_OTHER("Venom Other", 2245, 0),
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
