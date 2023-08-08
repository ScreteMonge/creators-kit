package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.Character;
import com.creatorskit.programming.Program;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ProgramPanel extends JFrame
{
    @Inject
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private GridBagConstraints c = new GridBagConstraints();

    @Inject
    public ProgramPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setTitle("Creators Kit Programmer");
        setIconImage(icon);


        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        pack();
    }

    public void createProgramPanel(Character character, JPanel programPanel)
    {
        programPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        programPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        programPanel.setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;

        JLabel nameLabel = new JLabel(character.getName());
        programPanel.add(nameLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridLayout(0, 1));
        textPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        programPanel.add(textPanel, c);

        JLabel idleAnimLabel = new JLabel("Idle animation: ");
        JLabel walkAnimLabel = new JLabel("Active animation: ");
        JLabel speedLabel = new JLabel("Speed: ");
        JLabel turnSpeedLabel = new JLabel("Turn speed: ");
        JLabel waterWalkLabel = new JLabel("Watercraft? ");

        textPanel.add(idleAnimLabel);
        textPanel.add(walkAnimLabel);
        textPanel.add(speedLabel);
        textPanel.add(turnSpeedLabel);
        textPanel.add(waterWalkLabel);


        c.gridx = 1;
        c.gridy = 1;
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(0, 1));
        optionsPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        programPanel.add(optionsPanel, c);

        Program program = character.getProgram();

        JSpinner idleAnimSpinner = new JSpinner(new SpinnerNumberModel(program.getIdleAnim(), -1, 9999, 1));
        idleAnimSpinner.addChangeListener(e ->
        {
            int idleAnim = (int) idleAnimSpinner.getValue();
            program.setIdleAnim(idleAnim);
            character.getAnimationSpinner().setValue(idleAnim);
        });
        optionsPanel.add(idleAnimSpinner);

        JSpinner walkAnimSpinner = new JSpinner(new SpinnerNumberModel(program.getWalkAnim(), -1, 9999, 1));
        walkAnimSpinner.addChangeListener(e ->
        {
            program.setWalkAnim((int) walkAnimSpinner.getValue());
        });
        optionsPanel.add(walkAnimSpinner);

        JSpinner speedSpinner = new JSpinner(new SpinnerNumberModel(program.getSpeed(), 0, 10, 1));
        speedSpinner.addChangeListener(e ->
        {
            program.setSpeed((double) speedSpinner.getValue());
        });
        optionsPanel.add(speedSpinner);

        JSpinner turnSpeedSpinner = new JSpinner();
        turnSpeedSpinner.setValue(program.getTurnSpeed());
        turnSpeedSpinner.addChangeListener(e ->
        {
            program.setTurnSpeed((int) turnSpeedSpinner.getValue());
        });
        optionsPanel.add(turnSpeedSpinner);

        JCheckBox waterWalkCheckBox = new JCheckBox();
        waterWalkCheckBox.setSelected(program.isWaterWalk());
        waterWalkCheckBox.addChangeListener(e ->
        {
            program.setWaterWalk(waterWalkCheckBox.isSelected());
        });
        optionsPanel.add(waterWalkCheckBox);

        add(programPanel);
        repaint();
        revalidate();
        pack();
    }

    public void removeProgramPanel(JPanel panel)
    {
        remove(panel);
        repaint();
        revalidate();
        pack();
    }
}
