package com.creatorskit.swing.timesheet.sheets;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TimelineUnits
{
    GAMETICKS("Gameticks", 1),
    SECONDS("Seconds", 0.6)
    ;

    private final String name;
    private final double multiplier;

    @Override
    public String toString()
    {
        return name;
    }
}
