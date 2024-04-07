package com.creatorskit.swing.colours;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@AllArgsConstructor
@Getter
@Setter
public class ColourPanel extends JPanel
{
    private boolean colourSet;
    private short oldColour;
    private short newColour;
    private JLabel oldColourLabel;
    private JButton newColourButton;
    private JSpinner spinner;
}
