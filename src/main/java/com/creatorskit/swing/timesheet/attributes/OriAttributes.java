package com.creatorskit.swing.timesheet.attributes;

import lombok.Getter;

import javax.swing.*;

@Getter
public class OriAttributes
{
    private final JSpinner manual = new JSpinner();
    private final JCheckBox manualOverride = new JCheckBox();
}
