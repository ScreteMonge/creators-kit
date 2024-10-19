package com.creatorskit;

import com.creatorskit.models.CustomModel;
import com.creatorskit.programming.Program;
import com.creatorskit.swing.ObjectPanel;
import com.creatorskit.swing.ParentPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

@Getter
@Setter
@AllArgsConstructor
public class Character
{
    private String name;
    private boolean active;
    private boolean locationSet;
    private boolean minimized;
    private KeyFrame[][] frames;
    private DefaultMutableTreeNode linkedManagerNode;
    private DefaultMutableTreeNode linkedTimeSheetNode;
    private DefaultMutableTreeNode parentManagerNode;
    private DefaultMutableTreeNode parentTimeSheetNode;
    private Program program;
    private WorldPoint nonInstancedPoint;
    private LocalPoint instancedPoint;
    private int[] instancedRegions;
    private int instancedPlane;
    private boolean inInstance;
    private CustomModel storedModel;
    private ParentPanel parentPanel;
    private ObjectPanel objectPanel;
    private boolean customMode;
    private JTextField nameField;
    private JComboBox<CustomModel> comboBox;
    private JButton spawnButton;
    private JButton modelButton;
    private JSpinner modelSpinner;
    private JSpinner animationSpinner;
    private JSpinner orientationSpinner;
    private JSpinner radiusSpinner;
    private JLabel programmerLabel;
    private JSpinner programmerIdleSpinner;
    private RuneLiteObject runeLiteObject;
    private int targetOrientation;

    @Override
    public String toString()
    {
        return name;
    }
}
