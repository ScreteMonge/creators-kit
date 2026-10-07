package com.creatorskit.models.datatypes;

import lombok.Data;

@Data
public class SpotanimData
{
    private final String name;
    private final int id;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }
}
