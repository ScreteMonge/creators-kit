package com.creatorskit.swing.timesheet;

import com.creatorskit.Character;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
@AllArgsConstructor
public class TimeSheetComp
{
    private final JPanel panel;
    private final JLabel label;
    private Color color;

    public TimeSheetComp createEmptyComp(Character character)
    {
        return new TimeSheetComp(
                new JPanel(),
                new JLabel(character.getName()),
                Color.RED);
    }
}
