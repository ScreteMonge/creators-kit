package com.creatorskit;

import com.creatorskit.models.CustomModel;
import com.creatorskit.programming.Program;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.swing.*;

@Getter
@Setter
@AllArgsConstructor
public class Character
{
    private String name;
    private boolean active;
    private boolean locationSet;
    private boolean minimized;
    private Program program;
    private WorldPoint savedLocation;
    private CustomModel storedModel;
    private JPanel masterPanel;
    private boolean customMode;
    private JComboBox<CustomModel> comboBox;
    private JButton spawnButton;
    private JButton modelButton;
    private JSpinner modelSpinner;
    private JSpinner animationSpinner;
    private JSpinner orientationSpinner;
    private JLabel programmerLabel;
    private JSpinner programmerIdleSpinner;
    private RuneLiteObject runeLiteObject;
    private int targetOrientation;
}
