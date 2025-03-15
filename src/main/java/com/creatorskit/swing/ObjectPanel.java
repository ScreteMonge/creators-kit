package com.creatorskit.swing;

import com.creatorskit.Character;
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
    private Character character;

    @Override
    public String toString()
    {
        return name;
    }
}
