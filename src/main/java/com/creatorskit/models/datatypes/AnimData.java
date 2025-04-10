package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnimData
{
    private final int id;
    private final String name;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}
