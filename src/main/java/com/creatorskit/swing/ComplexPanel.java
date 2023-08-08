package com.creatorskit.swing;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;

@Getter
@AllArgsConstructor
public class ComplexPanel extends JPanel
{
    private final JSpinner modelIdSpinner;
    private final JSpinner groupSpinner;
    private final JTextField nameField;
    private final JTextField colourNewField;
    private final JTextField colourOldField;
    private final JSpinner xSpinner;
    private final JSpinner ySpinner;
    private final JSpinner zSpinner;
    private final JSpinner xTileSpinner;
    private final JSpinner yTileSpinner;
    private final JSpinner zTileSpinner;
    private final JSpinner xScaleSpinner;
    private final JSpinner yScaleSpinner;
    private final JSpinner zScaleSpinner;
    private final JCheckBox check90;
    private final JCheckBox check180;
    private final JCheckBox check270;
}
