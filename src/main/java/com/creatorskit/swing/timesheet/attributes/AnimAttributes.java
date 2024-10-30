package com.creatorskit.swing.timesheet.attributes;

import lombok.Getter;

import javax.swing.*;

@Getter
public class AnimAttributes
{
    private final JSpinner override = new JSpinner();
    private final JCheckBox checkBox = new JCheckBox();

    private final JSpinner idle = new JSpinner();
    private final JSpinner walk = new JSpinner();
    private final JSpinner run = new JSpinner();
    private final JSpinner backwards = new JSpinner();
    private final JSpinner idleRight = new JSpinner();
    private final JSpinner idleLeft = new JSpinner();
    private final JSpinner walk180 = new JSpinner();
    private final JSpinner walkRight = new JSpinner();
    private final JSpinner walkLeft = new JSpinner();
}
