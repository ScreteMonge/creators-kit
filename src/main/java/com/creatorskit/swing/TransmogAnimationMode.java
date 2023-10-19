package com.creatorskit.swing;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TransmogAnimationMode
{
    PLAYER("Player"),
    MODIFIED("Modified"),
    CUSTOM("Custom"),
    NONE("None")
    ;

    private final String string;

    @Override
    public String toString()
    {
        return string;
    }
}
