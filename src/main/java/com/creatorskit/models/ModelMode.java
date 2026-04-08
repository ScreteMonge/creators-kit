package com.creatorskit.models;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ModelMode
{
    ID("ID"),
    CK_MODEL("Custom");

    private String name;

    @Override
    public String toString()
    {
        return name;
    }
}
