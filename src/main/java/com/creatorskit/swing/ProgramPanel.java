package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.Character;
import com.creatorskit.programming.MovementType;
import com.creatorskit.programming.Program;
import com.creatorskit.programming.ProgramComp;
import lombok.Getter;
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
import java.util.Random;

public class ProgramPanel extends JFrame
{
    @Inject
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private final GridBagConstraints c = new GridBagConstraints();
    @Getter
    private final JPanel allPanel = new JPanel();
    private final Random random = new Random();

    @Inject
    public ProgramPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setTitle("Creators Kit Programmer");
        setIconImage(icon);
        setPreferredSize(new Dimension(300, 300));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(allPanel);
        add(scrollPane);

        allPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        allPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        allPanel.setLayout(new GridLayout(0, 7));
        pack();
    }

    public void createProgramPanel(Character character, JPanel programPanel, JLabel nameLabel, JSpinner idleAnimSpinner)
    {
        programPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        programPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        programPanel.setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 0;
        c.weighty = 0;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        nameLabel.setText(character.getName());
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        programPanel.add(nameLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridLayout(0, 1));
        textPanel.setBorder(new EmptyBorder(2, 2, 2, 0));
        textPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        programPanel.add(textPanel, c);

        JLabel idleAnimLabel = new JLabel("Idle animation:");
        idleAnimLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        idleAnimLabel.setToolTipText("Set the animation for when the object isn't moving");

        JLabel walkAnimLabel = new JLabel("Active animation:");
        walkAnimLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        walkAnimLabel.setToolTipText("Set the animation for when the object is moving");

        JLabel speedLabel = new JLabel("Speed:");
        speedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        speedLabel.setToolTipText("Set how fast the object moves");

        JLabel turnSpeedLabel = new JLabel("Turn speed:");
        turnSpeedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        turnSpeedLabel.setToolTipText("Set how fast the object turns. Set to 0 for no turning");

        JLabel movementTypeLabel = new JLabel("Movement Type:");
        movementTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        movementTypeLabel.setToolTipText("Determines the terrain on which the object can travel");

        JLabel loopLabel = new JLabel("Loop:");
        loopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        loopLabel.setToolTipText("Set whether this program should loop once finished");

        textPanel.add(idleAnimLabel);
        textPanel.add(walkAnimLabel);
        textPanel.add(speedLabel);
        textPanel.add(turnSpeedLabel);
        textPanel.add(movementTypeLabel);
        textPanel.add(loopLabel);

        c.gridx = 1;
        c.gridy = 1;
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(0, 1));
        optionsPanel.setBorder(new EmptyBorder(2, 0, 2, 2));
        optionsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        programPanel.add(optionsPanel, c);

        Program program = character.getProgram();
        ProgramComp comp = program.getComp();

        idleAnimSpinner.setModel(new SpinnerNumberModel(comp.getIdleAnim(), -1, 9999, 1));
        idleAnimSpinner.addChangeListener(e ->
        {
            int idleAnim = (int) idleAnimSpinner.getValue();
            comp.setIdleAnim(idleAnim);
            character.getAnimationSpinner().setValue(idleAnim);
        });
        optionsPanel.add(idleAnimSpinner);

        JSpinner walkAnimSpinner = new JSpinner(new SpinnerNumberModel(comp.getWalkAnim(), -1, 9999, 1));
        walkAnimSpinner.addChangeListener(e ->
                comp.setWalkAnim((int) walkAnimSpinner.getValue()));
        optionsPanel.add(walkAnimSpinner);

        JSpinner speedSpinner = new JSpinner(new SpinnerNumberModel(comp.getSpeed(), 0, 2, 1));
        speedSpinner.addChangeListener(e ->
                comp.setSpeed((double) speedSpinner.getValue()));
        optionsPanel.add(speedSpinner);

        JSpinner turnSpeedSpinner = new JSpinner();
        turnSpeedSpinner.setValue(comp.getTurnSpeed());
        turnSpeedSpinner.addChangeListener(e ->
                comp.setTurnSpeed((int) turnSpeedSpinner.getValue()));
        optionsPanel.add(turnSpeedSpinner);

        JComboBox<String> movementBox = new JComboBox<>();
        movementBox.addItem("Normal");
        movementBox.addItem("Waterborne");
        movementBox.addItem("Ghost");
        movementBox.addItemListener(e ->
        {
            switch ((String) e.getItem())
            {
                case "Normal":
                    comp.setMovementType(MovementType.NORMAL);
                    break;
                case "Waterborne":
                    comp.setMovementType(MovementType.WATERBORNE);
                    break;
                case "Ghost":
                    comp.setMovementType(MovementType.GHOST);
            }

            plugin.updateProgramPath(program, false);
        });
        optionsPanel.add(movementBox);

        JCheckBox loopCheckBox = new JCheckBox();
        loopCheckBox.setFocusable(false);
        loopCheckBox.addActionListener(e ->
                comp.setLoop(!comp.isLoop()));
        optionsPanel.add(loopCheckBox);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        JButton colourButton = new JButton();
        colourButton.setText("Reroll Colour");
        colourButton.setToolTipText("Rerolls a new random colour for the path");
        colourButton.setFocusable(false);
        colourButton.addActionListener(e ->
                comp.setColor(getRandomColor()));
        programPanel.add(colourButton, c);

        allPanel.add(programPanel);
        repaint();
        revalidate();
    }

    public void removeProgramPanel(JPanel panel)
    {
        allPanel.remove(panel);
        repaint();
        revalidate();
    }

    private Color getRandomColor()
    {
        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();
        return new Color(r, g, b);
    }
}
