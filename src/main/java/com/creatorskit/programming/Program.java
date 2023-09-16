package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@Getter
@Setter
@AllArgsConstructor
public class Program
{
    private ProgramComp comp;
    private JLabel nameLabel;
    private JSpinner idleAnimSpinner;
}
