package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
@AllArgsConstructor
public class Program
{
    private ProgramComp comp;
    private JPanel programPanel;
    private JLabel nameLabel;
    private JSpinner idleAnimSpinner;
    private Color color;
}
