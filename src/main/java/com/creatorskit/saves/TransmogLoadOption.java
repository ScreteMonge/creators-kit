package com.creatorskit.saves;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TransmogLoadOption
{
    BOTH("Both"),
    CUSTOM_MODEL("Custom Model"),
    ANIMATIONS("Animations")
    ;

    private String string;

    @Override
    public String toString()
    {
        return string;
    }
}
