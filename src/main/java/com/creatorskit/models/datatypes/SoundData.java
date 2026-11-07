package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SoundData
{
    private final int id;
    private final String name;

    @Override
    public String toString()
    {
        return name + " (" + id + ")";
    }

    public String createLookupName()
    {
        if (name.isEmpty())
        {
            return "" + id;
        }

        String s = name.replaceAll(" ", "_");
        return id + "%20" + s;
    }
}
