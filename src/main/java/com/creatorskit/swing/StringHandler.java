package com.creatorskit.swing;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StringHandler
{
    public static String cleanString(String string)
    {
        return string.replaceAll("\\P{Print}", "?");
    }
}
