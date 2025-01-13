package com.creatorskit.swing.timesheet.keyframe.settings;

import lombok.AllArgsConstructor;
import net.runelite.api.HitsplatID;

@AllArgsConstructor
public enum HitsplatType
{
    DAMAGE("Damage", HitsplatID.DAMAGE_ME),
    BLOCK("Block", HitsplatID.BLOCK_ME)
    ;

    private final String name;
    private final int hitsplatID;

    @Override
    public String toString()
    {
        return name;
    }
}
