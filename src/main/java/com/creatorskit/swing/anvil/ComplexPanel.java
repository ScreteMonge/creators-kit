package com.creatorskit.swing.anvil;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@Getter
@Setter
@AllArgsConstructor
public class ComplexPanel extends JPanel
{
    private final JSpinner modelIdSpinner;
    private final JSpinner groupSpinner;
    private final JTextField nameField;
    private short[] coloursFrom;
    private short[] coloursTo;
    private short[] texturesFrom;
    private short[] texturesTo;
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
    private final JCheckBox invertFaces;

    @Override
    public String toString()
    {
        return nameField.getText() + " (" + modelIdSpinner.getValue() + (")");
    }
}
