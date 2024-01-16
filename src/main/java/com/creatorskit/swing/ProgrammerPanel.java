package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.Character;
import com.creatorskit.programming.MovementType;
import com.creatorskit.programming.Program;
import com.creatorskit.programming.ProgramComp;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Random;

@Getter
public class ProgrammerPanel extends JPanel
{
    @Inject
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel managerProgramHolder = new JPanel();
    private final JPanel sideProgramHolder = new JPanel();
    private JPanel[] sidePrograms = new JPanel[0];
    private final Random random = new Random();


    @Inject
    public ProgrammerPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(headerPanel, c);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.gridwidth = 1;
        JLabel titleLabel = new JLabel("Programmer");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        JButton syncAllButton = new JButton("Sync All Idles");
        syncAllButton.setToolTipText("Synchronizes the idle animations of all current Objects");
        syncAllButton.setPreferredSize(new Dimension(150, 30));
        headerPanel.add(syncAllButton, c);
        syncAllButton.addActionListener(e ->
        {
            for (Character character : plugin.getCharacters())
            {
                plugin.setAnimation(character, (int) character.getAnimationSpinner().getValue());
            }
        });

        JTabbedPane tabbedPane = new JTabbedPane();
        JScrollPane managerScrollPane = new JScrollPane();
        JScrollPane sideScrollPane = new JScrollPane();

        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        JButton syncShownButton = new JButton("Sync Shown Idles");
        syncShownButton.setToolTipText("Synchronizes the idle animations of all currently shown Objects");
        syncShownButton.setPreferredSize(new Dimension(150, 30));
        headerPanel.add(syncShownButton, c);
        syncShownButton.addActionListener(e ->
        {
            Component component = tabbedPane.getSelectedComponent();
            if (component == sideScrollPane)
            {
                for (ObjectPanel objectPanel : plugin.getCreatorsPanel().getSideObjectPanels())
                {
                    for (Character character : plugin.getCharacters())
                    {
                        if (character.getObjectPanel() == objectPanel)
                        {
                            plugin.setAnimation(character, (int) character.getAnimationSpinner().getValue());
                        }
                    }
                }
            }

            if (component == managerScrollPane)
            {
                ObjectPanel[] objectPanels = plugin.getCreatorsPanel().getToolBox().getManagerPanel().getShownObjectPanels();
                for (ObjectPanel objectPanel : objectPanels)
                {
                    for (Character character : plugin.getCharacters())
                    {
                        if (character.getObjectPanel() == objectPanel)
                        {
                            plugin.setAnimation(character, (int) character.getAnimationSpinner().getValue());
                        }
                    }
                }
            }
        });

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.8;
        c.weighty = 1;
        c.gridwidth = 2;
        tabbedPane.addTab("Side Panel Objects", sideScrollPane);
        tabbedPane.addTab("Manager Objects", managerScrollPane);
        tabbedPane.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        add(tabbedPane, c);

        managerProgramHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        managerProgramHolder.setBorder(new EmptyBorder(4, 4, 4, 4));
        managerProgramHolder.setLayout(new GridBagLayout());
        managerScrollPane.setViewportView(managerProgramHolder);

        sideProgramHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        sideProgramHolder.setBorder(new EmptyBorder(4, 4, 4, 4));
        sideProgramHolder.setLayout(new GridBagLayout());
        sideScrollPane.setViewportView(sideProgramHolder);

        repaint();
        revalidate();
    }

    public void createProgramPanel(Character character, JPanel programPanel, JLabel nameLabel, JSpinner idleAnimSpinner)
    {
        Program program = character.getProgram();
        ProgramComp comp = program.getComp();

        programPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        programPanel.setBorder(new LineBorder(program.getColor(), 1));
        programPanel.setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0;
        c.weighty = 0;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        nameLabel.setText(character.getName());
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setBackground(ColorScheme.DARK_GRAY_COLOR);
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

        JComboBox<MovementType> movementBox = new JComboBox<>();
        movementBox.addItem(MovementType.NORMAL);
        movementBox.addItem(MovementType.WATERBORNE);
        movementBox.addItem(MovementType.GHOST);
        movementBox.addItemListener(e ->
        {
            comp.setMovementType((MovementType) movementBox.getSelectedItem());
            plugin.updateProgramPath(program, false, character.isInInstance());
        });
        movementBox.setSelectedItem(comp.getMovementType());
        optionsPanel.add(movementBox);

        JCheckBox loopCheckBox = new JCheckBox();
        loopCheckBox.setFocusable(false);
        loopCheckBox.setSelected(comp.isLoop());
        loopCheckBox.addActionListener(e ->
                comp.setLoop(loopCheckBox.isSelected()));
        optionsPanel.add(loopCheckBox);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        JButton colourButton = new JButton();
        colourButton.setText("Reroll Colour");
        colourButton.setToolTipText("Rerolls a new random colour for the path");
        colourButton.setFocusable(false);
        colourButton.addActionListener(e ->
                {
                    Color color = getRandomColor();
                    program.setColor(color);
                    program.getComp().setRgb(color.getRGB());
                    programPanel.setBorder(new LineBorder(color, 1));
                });
        programPanel.add(colourButton, c);

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

    public void addSideProgram(JPanel programPanel)
    {
        sidePrograms = ArrayUtils.add(sidePrograms, programPanel);
        resetProgramSidePanel();
    }

    public void removeSideProgram(JPanel programPanel)
    {
        sidePrograms = ArrayUtils.removeElement(sidePrograms, programPanel);
        resetProgramSidePanel();
    }

    public void clearSidePrograms()
    {
        sidePrograms = new JPanel[0];
        sideProgramHolder.removeAll();
    }

    public void resetProgramSidePanel()
    {
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.FIRST_LINE_START;

        sideProgramHolder.removeAll();
        int rows = sidePrograms.length / 5;

        for (int i = 0; i < sidePrograms.length; i++)
        {
            JPanel panel = sidePrograms[i];

            c.weightx = c.gridx == 4 ? 1 : 0;
            if (c.gridx == sidePrograms.length - 1)
                c.weightx = 1;

            c.weighty = c.gridy == rows ? 1 : 0;
            if (i == sidePrograms.length - 1)
                c.weighty = 1;

            sideProgramHolder.add(panel, c);
            c.gridx++;
            if (c.gridx > 4)
            {
                c.gridx = 0;
                c.gridy++;
            }
        }

        sideProgramHolder.revalidate();
        sideProgramHolder.repaint();
    }
}
