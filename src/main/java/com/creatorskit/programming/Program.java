package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.LocalPoint;

import javax.swing.*;

@Getter
@Setter
@AllArgsConstructor
public class Program
{
    private LocalPoint[] steps;
    private LocalPoint[] path;
    private Coordinate[] coordinates;
    private int currentStep;
    private double speed;
    private int turnSpeed;
    private int idleAnim;
    private int walkAnim;
    private boolean waterWalk;
    private JLabel nameLabel;
    private JSpinner idleAnimSpinner;
}
