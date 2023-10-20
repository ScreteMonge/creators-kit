package com.creatorskit.swing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@AllArgsConstructor
@Getter
@Setter
public class ObjectPanel extends JPanel
{
    private String name;

    @Override
    public String toString()
    {
        return name;
    }
}
