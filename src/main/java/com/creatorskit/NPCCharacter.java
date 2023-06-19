package com.creatorskit;

import com.creatorskit.models.CustomModel;
import com.creatorskit.programming.Program;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;

import javax.swing.*;

@Getter
@Setter
@AllArgsConstructor
public class NPCCharacter
{
    private String name;
    private int id;
    private boolean moving;
    private boolean locationSet;
    private Program program;
    private LocalPoint savedLocation;
    private CustomModel storedModel;
    private JPanel panel;
    private JComboBox<CustomModel> comboBox;
    private JButton spawnButton;
    private JButton modelButton;
    private JSpinner modelSpinner;
    private RuneLiteObject runeLiteObject;
}
