package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.NPCCharacter;
import com.creatorskit.models.CustomModel;
import com.creatorskit.programming.Program;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

@Slf4j
public class CreatorsPanel extends PluginPanel
{
    @Inject
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;

    @Getter
    private final ModelAnvil modelAnvil;
    private final ModelOrganizer modelOrganizer;
    private final ProgramPanel programPanel;

    private final JButton createNPCButton = new JButton();
    private final JPanel mainPanel = new JPanel();
    private GridBagConstraints cNPC = new GridBagConstraints();
    private int npcPanels = 0;
    @Getter
    private final ArrayList<JPanel> objectPanels = new ArrayList<>();
    @Getter
    private final ArrayList<JComboBox<CustomModel>> comboBoxes = new ArrayList<>();
    private final Dimension spinnerSize = new Dimension(72, 30);

    LineBorder defaultBorder = new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1);
    LineBorder hoveredBorder = new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1);
    LineBorder selectedBorder = new LineBorder(Color.WHITE, 1);

    private final BufferedImage MAXIMIZE = ImageUtil.loadImageResource(getClass(), "/Maximize.png");
    private final BufferedImage MINIMIZE = ImageUtil.loadImageResource(getClass(), "/Minimize.png");
    private final BufferedImage DUPLICATE = ImageUtil.loadImageResource(getClass(), "/Duplicate.png");
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");

    @Inject
    public CreatorsPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin, ModelOrganizer modelOrganizer, ProgramPanel programPanel, ModelAnvil modelAnvil)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.modelOrganizer = modelOrganizer;
        this.programPanel = programPanel;
        this.modelAnvil = modelAnvil;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        JButton organizerButton = new JButton("Organizer");
        organizerButton.setToolTipText("Opens an interface for managing custom models");
        organizerButton.setFocusable(false);
        organizerButton.addActionListener(e ->
        {
            modelOrganizer.setVisible(!modelOrganizer.isVisible());
            revalidate();
            repaint();
        });
        add(organizerButton, c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        JButton anvilButton = new JButton("Anvil");
        anvilButton.setToolTipText("Opens an interface for creating custom models");
        anvilButton.setFocusable(false);
        anvilButton.addActionListener(e ->
        {
            modelAnvil.setVisible(!modelAnvil.isVisible());
            revalidate();
            repaint();
        });
        add(anvilButton, c);

        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 1;
        JButton programmerButton = new JButton("Programmer");
        programmerButton.setToolTipText("Opens an interface for programming object actions");
        programmerButton.setFocusable(false);
        programmerButton.addActionListener(e ->
        {
            programPanel.setVisible(!programPanel.isVisible());
            revalidate();
            repaint();
        });
        add(programmerButton, c);


        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        c.weightx = 1;
        c.ipady = 5;
        createNPCButton.setText("Add Object");
        createNPCButton.setToolTipText("Add an new Object to the palette");
        createNPCButton.setFocusable(false);
        createNPCButton.addActionListener(e ->
                {
                    createPanel();
                }
                );
        add(createNPCButton, c);


        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 2;
        c.weighty = 1;
        c.ipady = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(mainPanel, c);

        cNPC.fill = GridBagConstraints.HORIZONTAL;
        cNPC.insets = new Insets(2, 2, 2, 2);
        cNPC.gridx = 0;
        cNPC.gridy = 0;
        cNPC.weightx = 1;
        cNPC.weighty = 1;
    }

    public JPanel createPanel()
    {
        return createPanel("Object " + npcPanels, 7699, null, false, 0,  -1, 60);
    }

    public JPanel createPanel(String name, int modelId, CustomModel customModel, boolean customModeActive, int orientation, int animationId, int radius)
    {
        JPanel masterPanel = new JPanel();
        masterPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        masterPanel.setBorder(defaultBorder);
        masterPanel.setLayout(new GridBagLayout());
        masterPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int y = e.getPoint().y;
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        JTextField textField = new JTextField(name);
        masterPanel.add(textField, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        JPanel topButtonsPanel = new JPanel();
        Dimension topButtonsPanelSize = new Dimension(81, 30);
        topButtonsPanel.setMaximumSize(topButtonsPanelSize);
        topButtonsPanel.setPreferredSize(topButtonsPanelSize);
        topButtonsPanel.setMinimumSize(topButtonsPanelSize);
        topButtonsPanel.setLayout(new GridLayout(1, 3, 1, 0));
        masterPanel.add(topButtonsPanel, c);

        JButton duplicateButton = new JButton(new ImageIcon(DUPLICATE));
        duplicateButton.setToolTipText("Duplicate object");
        duplicateButton.setFocusable(false);
        topButtonsPanel.add(duplicateButton);

        final boolean[] minimized = {false};
        ImageIcon minimize = new ImageIcon(MINIMIZE);
        ImageIcon maximize = new ImageIcon(MAXIMIZE);
        JButton minimizeButton = new JButton(minimize);
        minimizeButton.setToolTipText("Minimize");
        minimizeButton.setFocusable(false);
        topButtonsPanel.add(minimizeButton);

        JButton deleteButton = new JButton(new ImageIcon(CLOSE));
        deleteButton.setToolTipText("Delete object");
        deleteButton.setFocusable(false);
        topButtonsPanel.add(deleteButton);

        //Buttons

        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.CENTER;
        c.ipadx = 0;
        c.ipady = 5;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JButton modelButton = new JButton();
        modelButton.setFont(FontManager.getRunescapeFont());
        modelButton.setText("Custom");
        modelButton.setToolTipText("Toggle between Custom Model and Model ID");
        modelButton.setFocusable(false);
        masterPanel.add(modelButton, c);

        c.gridy++;
        JButton spawnButton = new JButton();
        spawnButton.setFont(FontManager.getRunescapeFont());
        spawnButton.setText("Spawn");
        spawnButton.setToolTipText("Toggle the NPC on or off");
        spawnButton.setFocusable(false);
        masterPanel.add(spawnButton, c);

        c.gridy++;
        JButton relocateButton = new JButton();
        relocateButton.setFont(FontManager.getRunescapeFont());
        relocateButton.setText("Relocate");
        relocateButton.setToolTipText("Set the object's location to the selected tile");
        relocateButton.setFocusable(false);
        masterPanel.add(relocateButton, c);

        c.gridy++;
        JButton animationButton = new JButton();
        animationButton.setFont(FontManager.getRunescapeFont());
        animationButton.setText("Anim Off");
        animationButton.setToolTipText("Toggle the playing animation");
        animationButton.setFocusable(false);
        masterPanel.add(animationButton, c);

        c.gridy++;
        JButton setEndButton = new JButton();
        setEndButton.setFont(FontManager.getRunescapeFont());
        setEndButton.setText("Set End");
        setEndButton.setFocusable(false);
        masterPanel.add(setEndButton, c);


        //Labels

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JLabel modelLabel = new JLabel("Model ID:");
        modelLabel.setToolTipText("The ID number of the model to spawn");
        modelLabel.setFont(FontManager.getRunescapeSmallFont());
        masterPanel.add(modelLabel, c);

        c.gridy++;
        JLabel orientationLabel = new JLabel("Rotation:");
        orientationLabel.setToolTipText("0 = South, 512 = West, 1024 = North, 1736 = East, 2048 = Max");
        orientationLabel.setFont(FontManager.getRunescapeSmallFont());
        masterPanel.add(orientationLabel, c);

        c.gridy++;
        JLabel radiusLabel = new JLabel("Radius:");
        radiusLabel.setToolTipText("Increasing the radius may prevent clipping issues with the ground");
        radiusLabel.setFont(FontManager.getRunescapeSmallFont());
        masterPanel.add(radiusLabel, c);

        c.gridy++;
        JLabel animationLabel = new JLabel("Anim ID:");
        animationLabel.setToolTipText("The animation ID number. -1 gives no animation");
        animationLabel.setFont(FontManager.getRunescapeSmallFont());
        masterPanel.add(animationLabel, c);

        //Spinners

        final boolean[] customMode = {customModeActive};
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 1;
        JSpinner modelSpinner = new JSpinner();
        modelSpinner.setValue(modelId);
        modelSpinner.setVisible(!customMode[0]);
        modelSpinner.setMaximumSize(spinnerSize);
        modelSpinner.setPreferredSize(spinnerSize);
        modelSpinner.setMinimumSize(spinnerSize);
        masterPanel.add(modelSpinner, c);

        JComboBox<CustomModel> modelComboBox = new JComboBox<>();
        modelComboBox.setFont(FontManager.getRunescapeFont());
        modelComboBox.setVisible(customMode[0]);
        modelComboBox.setFocusable(false);
        modelComboBox.setName("modelComboBox");
        modelComboBox.setMaximumSize(spinnerSize);
        modelComboBox.setPreferredSize(spinnerSize);
        modelComboBox.setMinimumSize(spinnerSize);
        for (CustomModel model : plugin.getStoredModels())
        {
            modelComboBox.addItem(model);
        }
        modelComboBox.setSelectedItem(customModel);
        masterPanel.add(modelComboBox, c);

        c.gridy++;
        SpinnerModel orientationRange = new SpinnerNumberModel(orientation, 0, 2048, 1);
        JSpinner orientationSpinner = new JSpinner(orientationRange);
        orientationSpinner.setName("orientationSpinner");
        orientationSpinner.setMaximumSize(spinnerSize);
        orientationSpinner.setPreferredSize(spinnerSize);
        orientationSpinner.setMinimumSize(spinnerSize);
        masterPanel.add(orientationSpinner, c);

        c.gridy++;
        JSpinner radiusSpinner = new JSpinner();
        radiusSpinner.setValue(radius);
        radiusSpinner.setMaximumSize(spinnerSize);
        radiusSpinner.setPreferredSize(spinnerSize);
        radiusSpinner.setMinimumSize(spinnerSize);
        masterPanel.add(radiusSpinner, c);

        c.gridy++;
        JSpinner animationSpinner = new JSpinner();
        animationSpinner.setValue(animationId);
        animationSpinner.setMaximumSize(spinnerSize);
        animationSpinner.setPreferredSize(spinnerSize);
        animationSpinner.setMinimumSize(spinnerSize);
        masterPanel.add(animationSpinner, c);

        c.gridy++;
        JButton goButton = new JButton();
        goButton.setFont(FontManager.getRunescapeFont());
        goButton.setText("Go!");
        goButton.setFocusable(false);
        masterPanel.add(goButton, c);


        minimizeButton.addActionListener(e ->
        {
            if (!minimized[0])
            {
                customMode[0] = modelComboBox.isVisible();
                relocateButton.setVisible(false);
                modelButton.setVisible(false);
                modelLabel.setVisible(false);
                modelSpinner.setVisible(false);
                modelComboBox.setVisible(false);
                spawnButton.setVisible(false);
                orientationLabel.setVisible(false);
                orientationSpinner.setVisible(false);
                animationButton.setVisible(false);
                animationLabel.setVisible(false);
                animationSpinner.setVisible(false);
                radiusLabel.setVisible(false);
                radiusSpinner.setVisible(false);
                minimizeButton.setIcon(maximize);
                minimized[0] = true;
                minimizeButton.setToolTipText("Maximize");
                masterPanel.updateUI();
                return;
            }

            relocateButton.setVisible(true);
            modelButton.setVisible(true);
            modelLabel.setVisible(true);
            if (customMode[0])
            {
                modelComboBox.setVisible(true);
            }
            else
            {
                modelSpinner.setVisible(true);
            }

            spawnButton.setVisible(true);
            orientationLabel.setVisible(true);
            orientationSpinner.setVisible(true);
            animationButton.setVisible(true);
            animationLabel.setVisible(true);
            animationSpinner.setVisible(true);
            radiusLabel.setVisible(true);
            radiusSpinner.setVisible(true);
            minimizeButton.setIcon(minimize);
            minimized[0] = false;
            minimizeButton.setToolTipText("Minimize");
            masterPanel.updateUI();
        });


        clientThread.invokeLater(() -> {
            NPCCharacter npcCharacter = plugin.buildNPC(
                    textField.getText(),
                    npcPanels,
                    masterPanel,
                    textField,
                    modelComboBox,
                    relocateButton,
                    spawnButton,
                    animationButton,
                    modelButton,
                    modelId,
                    modelSpinner,
                    modelComboBox,
                    customMode[0],
                    orientation,
                    orientationSpinner,
                    radius,
                    radiusSpinner,
                    animationId,
                    animationSpinner);

            setEndButton.addActionListener(e ->
            {
                if (plugin.getSelectedTile() != null)
                {
                    LocalPoint lp = plugin.getSelectedTile().getLocalLocation();
                    Program program = new Program(plugin.getSpeed(), lp);
                    npcCharacter.setProgram(program);
                }
            });

            goButton.addActionListener(e ->
            {
                npcCharacter.setMoving(!npcCharacter.isMoving());
            });

            textField.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered (MouseEvent e)
                {
                    setHoveredCharacter(npcCharacter, masterPanel);
                }

                @Override
                public void mouseExited (MouseEvent e)
                {
                    unsetHoveredCharacter(npcCharacter, masterPanel);
                }
            });

            deleteButton.addActionListener(e ->
            {
                mainPanel.remove(masterPanel);
                objectPanels.remove(masterPanel);

                ArrayList<NPCCharacter> npcCharacters = plugin.getNpcCharacters();
                for (NPCCharacter npc : npcCharacters)
                {
                    if (npc.getPanel() == masterPanel)
                    {
                        clientThread.invokeLater(() ->
                        {
                            npc.getRuneLiteObject().setActive(false);
                        });
                        npcCharacters.remove(npc);
                        mainPanel.updateUI();
                        return;
                    }
                }
            });

            duplicateButton.addActionListener(e ->
            {
                boolean customModelMode = modelComboBox.isVisible();
                createPanel(textField.getText() + " Dupe", (int) modelSpinner.getValue(), (CustomModel) modelComboBox.getSelectedItem(), customModelMode, (int) orientationSpinner.getValue(), (int) animationSpinner.getValue(), (int) radiusSpinner.getValue());
            });

            masterPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    setSelectedCharacter(npcCharacter, masterPanel);
                }
            });

            for (Component component : masterPanel.getComponents())
            {
                component.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        setSelectedCharacter(npcCharacter, masterPanel);
                    }
                });
            }

            setSelectedCharacter(npcCharacter, masterPanel);
        });

        mainPanel.add(masterPanel, cNPC);
        mainPanel.updateUI();
        objectPanels.add(masterPanel);
        comboBoxes.add(modelComboBox);
        npcPanels++;
        cNPC.gridy++;

        return masterPanel;
    }

    public void setSelectedCharacter(NPCCharacter selected, JPanel jPanel)
    {
        for (JPanel panel : objectPanels)
        {
            panel.setBorder(defaultBorder);
        }

        jPanel.setBorder(selectedBorder);
        plugin.setSelectedNPC(selected);
    }

    public void setHoveredCharacter(NPCCharacter hovered, JPanel jPanel)
    {
        if (plugin.getSelectedNPC() == hovered)
        {
            return;
        }

        jPanel.setBorder(hoveredBorder);
        plugin.setHoveredNPC(hovered);
    }

    public void unsetHoveredCharacter(NPCCharacter hoverRemoved, JPanel jPanel)
    {
        plugin.setHoveredNPC(null);

        if (plugin.getSelectedNPC() == hoverRemoved)
        {
            return;
        }

        jPanel.setBorder(defaultBorder);
    }

    public void addModelOption(CustomModel model, boolean setComboBox)
    {
        for (JComboBox<CustomModel> comboBox : comboBoxes)
        {
            comboBox.addItem(model);
            if (setComboBox && plugin.getSelectedNPC() != null)
            {
                JComboBox<CustomModel> selectedBox = plugin.getSelectedNPC().getComboBox();
                if (comboBox == selectedBox)
                {
                    comboBox.setSelectedItem(model);
                }
            }
        }
        modelOrganizer.createModelPanel(model);
    }

    public void removeModelOption(CustomModel model)
    {
        for (JComboBox<CustomModel> comboBox : comboBoxes)
        {
            comboBox.removeItem(model);
        }
        modelOrganizer.removeModelPanel(model);
    }
}
