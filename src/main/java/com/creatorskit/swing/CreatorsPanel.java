package com.creatorskit.swing;

import com.creatorskit.saves.CharacterSave;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.Character;
import com.creatorskit.saves.SetupSave;
import com.creatorskit.models.*;
import com.creatorskit.programming.Coordinate;
import com.creatorskit.programming.MovementType;
import com.creatorskit.programming.Program;
import com.creatorskit.programming.ProgramComp;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class CreatorsPanel extends PluginPanel
{
    @Inject
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;

    private final ToolBoxFrame toolBox;
    private final ModelAnvil modelAnvil;
    private final ModelOrganizer modelOrganizer;
    private final ProgramPanel programPanel;
    private final TransmogPanel transmogPanel;
    private final JButton addObjectButton = new JButton();
    private final JPanel mainPanel = new JPanel();
    private final GridBagConstraints cNPC = new GridBagConstraints();
    private final Random random = new Random();
    public static final File SETUP_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit-setups");
    private final Pattern pattern = Pattern.compile("\\(\\d+\\)\\Z");
    private int npcPanels = 0;
    private final ArrayList<ObjectPanel> allObjectPanels = new ArrayList<>();
    private final ArrayList<ObjectPanel> sideObjectPanels = new ArrayList<>();
    private final ArrayList<JComboBox<CustomModel>> comboBoxes = new ArrayList<>();
    private final Dimension spinnerSize = new Dimension(72, 30);
    private final int DEFAULT_TURN_SPEED = 68;
    private final BufferedImage MAXIMIZE = ImageUtil.loadImageResource(getClass(), "/Maximize.png");
    private final BufferedImage MINIMIZE = ImageUtil.loadImageResource(getClass(), "/Minimize.png");
    private final BufferedImage DUPLICATE = ImageUtil.loadImageResource(getClass(), "/Duplicate.png");
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");
    private final BufferedImage LOAD = ImageUtil.loadImageResource(getClass(), "/Load.png");
    private final BufferedImage SAVE = ImageUtil.loadImageResource(getClass(), "/Save.png");
    private final BufferedImage CUSTOM_MODEL = ImageUtil.loadImageResource(getClass(), "/Custom model.png");
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    private final LineBorder defaultBorder = new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1);
    private final LineBorder hoveredBorder = new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1);
    private final LineBorder selectedBorder = new LineBorder(Color.WHITE, 1);

    @Inject
    public CreatorsPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin, ToolBoxFrame toolBox)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.toolBox = toolBox;
        this.modelOrganizer = toolBox.getModelOrganizer();
        this.programPanel = toolBox.getProgramPanel();
        this.modelAnvil = toolBox.getModelAnvil();
        this.transmogPanel = toolBox.getTransmogPanel();

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.gridwidth = 5;
        JButton toolBoxButton = new JButton("Toolbox");
        toolBoxButton.setToolTipText("Opens an interface for organizing Objects, creating Custom Models, programming, and more");
        toolBoxButton.setFocusable(false);
        toolBoxButton.addActionListener(e ->
        {
            toolBox.setVisible(!toolBox.isVisible());
            revalidate();
            repaint();
        });
        add(toolBoxButton, c);


        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        c.gridheight = 2;
        c.weightx = 3;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        addObjectButton.setText("Add Object");
        addObjectButton.setToolTipText("Add an new Object to the palette");
        addObjectButton.setFocusable(false);
        addObjectButton.addActionListener(e ->
                {
                    ObjectPanel panel = createPanel(sideObjectPanels, mainPanel);
                    addPanel(sideObjectPanels, mainPanel, panel);
                });
        add(addObjectButton, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        JButton saveButton = new JButton(new ImageIcon(SAVE));
        saveButton.setFocusable(false);
        saveButton.setToolTipText("Save this setup");
        add(saveButton, c);
        saveButton.addActionListener(this::actionPerformed);

        c.gridx = 4;
        c.gridy = 1;
        JButton loadButton = new JButton(new ImageIcon(LOAD));
        loadButton.setFocusable(false);
        loadButton.setToolTipText("Load a previously saved setup");
        add(loadButton, c);
        loadButton.addActionListener(e -> openLoadSetupDialog());

        c.gridx = 3;
        c.gridy = 2;
        JButton loadCustomModelButton = new JButton(new ImageIcon(CUSTOM_MODEL));
        loadCustomModelButton.setFocusable(false);
        loadCustomModelButton.setToolTipText("Load a previously saved Custom Model");
        add(loadCustomModelButton, c);
        loadCustomModelButton.addActionListener(e -> openLoadCustomModelDialog());

        c.gridx = 4;
        c.gridy = 2;
        JButton clearButton = new JButton(new ImageIcon(CLEAR));
        clearButton.setFocusable(false);
        clearButton.setToolTipText("Clears all Objects");
        add(clearButton, c);
        clearButton.addActionListener(e -> clearPanels());

        c.gridwidth = 5;
        c.gridx = 0;
        c.gridy = 3;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(mainPanel, c);

        cNPC.fill = GridBagConstraints.HORIZONTAL;
        cNPC.insets = new Insets(2, 2, 2, 2);
        cNPC.gridx = 0;
        cNPC.gridy = GridBagConstraints.RELATIVE;
        cNPC.weightx = 1;
        cNPC.weighty = 1;
    }

    public ObjectPanel createPanel(ArrayList<ObjectPanel> panelArray, JPanel parentPanel)
    {
        return createPanel(panelArray, parentPanel, "Object (" + npcPanels + ")", 7699, null, false, false, 0,  -1, 60, createEmptyProgram(), false, null, null, new int[0], -1, false);
    }

    public ObjectPanel createPanel(ArrayList<ObjectPanel> panelArray,
                              JPanel parentPanel,
                              String name,
                              int modelId,
                              CustomModel customModel,
                              boolean customModeActive,
                              boolean setMinimized,
                              int orientation,
                              int animationId,
                              int radius,
                              Program program,
                              boolean active,
                              WorldPoint worldPoint,
                              LocalPoint localPoint,
                              int[] localPointRegion,
                              int localPointPlane,
                              boolean inInstance)
    {
        ObjectPanel objectPanel = new ObjectPanel(name);
        objectPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        objectPanel.setBorder(defaultBorder);
        objectPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        JTextField textField = new JTextField(name);
        objectPanel.add(textField, c);

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
        objectPanel.add(topButtonsPanel, c);

        JButton duplicateButton = new JButton(new ImageIcon(DUPLICATE));
        duplicateButton.setName("Duplicate");
        duplicateButton.setToolTipText("Duplicate object");
        duplicateButton.setFocusable(false);
        topButtonsPanel.add(duplicateButton);

        final boolean[] minimized = {setMinimized};
        ImageIcon minimize = new ImageIcon(MINIMIZE);
        ImageIcon maximize = new ImageIcon(MAXIMIZE);
        JButton minimizeButton = new JButton(minimize);
        minimizeButton.setToolTipText("Minimize");
        minimizeButton.setFocusable(false);
        topButtonsPanel.add(minimizeButton);

        JButton deleteButton = new JButton(new ImageIcon(CLOSE));
        deleteButton.setName("Delete");
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
        String modelButtonText = customModeActive ? "Custom" : "Id";
        modelButton.setText(modelButtonText);
        modelButton.setText("Custom");
        modelButton.setToolTipText("Toggle between Custom Model and Model ID");
        modelButton.setFocusable(false);
        objectPanel.add(modelButton, c);

        c.gridy++;
        JButton spawnButton = new JButton();
        spawnButton.setFont(FontManager.getRunescapeFont());
        spawnButton.setText("Spawn");
        spawnButton.setToolTipText("Toggle the NPC on or off");
        spawnButton.setFocusable(false);
        objectPanel.add(spawnButton, c);

        c.gridy++;
        JButton relocateButton = new JButton();
        relocateButton.setFont(FontManager.getRunescapeFont());
        relocateButton.setText("Relocate");
        relocateButton.setToolTipText("Set the object's location to the selected tile");
        relocateButton.setFocusable(false);
        objectPanel.add(relocateButton, c);

        c.gridy++;
        JButton animationButton = new JButton();
        animationButton.setFont(FontManager.getRunescapeFont());
        animationButton.setText("Anim Off");
        animationButton.setToolTipText("Toggle the playing animation");
        animationButton.setFocusable(false);
        objectPanel.add(animationButton, c);

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
        objectPanel.add(modelLabel, c);

        c.gridy++;
        JLabel orientationLabel = new JLabel("Rotation:");
        orientationLabel.setToolTipText("0 = South, 512 = West, 1024 = North, 1736 = East, 2048 = Max");
        orientationLabel.setFont(FontManager.getRunescapeSmallFont());
        objectPanel.add(orientationLabel, c);

        c.gridy++;
        JLabel radiusLabel = new JLabel("Radius:");
        radiusLabel.setToolTipText("Increasing the radius may prevent clipping issues with the ground");
        radiusLabel.setFont(FontManager.getRunescapeSmallFont());
        objectPanel.add(radiusLabel, c);

        c.gridy++;
        JLabel animationLabel = new JLabel("Anim ID:");
        animationLabel.setToolTipText("The animation ID number. -1 gives no animation");
        animationLabel.setFont(FontManager.getRunescapeSmallFont());
        objectPanel.add(animationLabel, c);

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
        objectPanel.add(modelSpinner, c);

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
        if (customModel != null)
            modelComboBox.setSelectedItem(customModel);
        objectPanel.add(modelComboBox, c);

        c.gridy++;
        SpinnerModel orientationRange = new SpinnerNumberModel(orientation, 0, 2048, 1);
        JSpinner orientationSpinner = new JSpinner(orientationRange);
        orientationSpinner.setName("orientationSpinner");
        orientationSpinner.setMaximumSize(spinnerSize);
        orientationSpinner.setPreferredSize(spinnerSize);
        orientationSpinner.setMinimumSize(spinnerSize);
        objectPanel.add(orientationSpinner, c);

        c.gridy++;
        JSpinner radiusSpinner = new JSpinner();
        radiusSpinner.setValue(radius);
        radiusSpinner.setMaximumSize(spinnerSize);
        radiusSpinner.setPreferredSize(spinnerSize);
        radiusSpinner.setMinimumSize(spinnerSize);
        objectPanel.add(radiusSpinner, c);

        c.gridy++;
        JSpinner animationSpinner = new JSpinner();
        animationSpinner.setValue(animationId);
        animationSpinner.setMaximumSize(spinnerSize);
        animationSpinner.setPreferredSize(spinnerSize);
        animationSpinner.setMinimumSize(spinnerSize);
        objectPanel.add(animationSpinner, c);

        if (setMinimized)
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
            minimizeButton.setToolTipText("Maximize");
            objectPanel.updateUI();
        }

        JLabel programmerNameLabel = program.getNameLabel();
        programmerNameLabel.setText(name);
        JSpinner programmerIdleSpinner = program.getIdleAnimSpinner();
        JPanel programJPanel = program.getProgramPanel();

        clientThread.invokeLater(() ->
        {
            Character character = plugin.buildCharacter(
                    textField.getText(),
                    objectPanel,
                    textField,
                    relocateButton,
                    spawnButton,
                    animationButton,
                    modelButton,
                    modelId,
                    modelSpinner,
                    modelComboBox,
                    customMode[0],
                    minimized[0],
                    orientation,
                    orientationSpinner,
                    radius,
                    radiusSpinner,
                    animationId,
                    animationSpinner,
                    program,
                    programmerNameLabel,
                    programmerIdleSpinner,
                    active,
                    worldPoint,
                    localPoint,
                    localPointRegion,
                    localPointPlane,
                    inInstance);

            SwingUtilities.invokeLater(() ->
                    programPanel.createProgramPanel(character, programJPanel, programmerNameLabel, programmerIdleSpinner));

            textField.addActionListener(e ->
            {
                character.setName(textField.getText());
                objectPanel.setName(textField.getText());
                toolBox.getManagerPanel().revalidate();
                toolBox.getManagerPanel().repaint();
            });

            deleteButton.addActionListener(e ->
            {
                removePanel(panelArray, parentPanel, objectPanel);
                sideObjectPanels.remove(objectPanel);
                allObjectPanels.remove(objectPanel);
                SwingUtilities.invokeLater(() ->
                        programPanel.removeProgramPanel(programJPanel));

                ArrayList<Character> characters = plugin.getCharacters();
                for (Character npc : characters)
                {
                    if (npc.getObjectPanel() == objectPanel)
                    {
                        clientThread.invokeLater(() ->
                                npc.getRuneLiteObject().setActive(false));
                        characters.remove(npc);
                        plugin.setSelectedCharacter(null);
                        parentPanel.repaint();
                        parentPanel.revalidate();
                        return;
                    }
                }
            });

            duplicateButton.addActionListener(e ->
            {
                ProgramComp comp = program.getComp();

                WorldPoint[] newSteps = ArrayUtils.clone(comp.getStepsWP());
                WorldPoint[] newPath = ArrayUtils.clone(comp.getPathWP());
                LocalPoint[] newStepsLP = ArrayUtils.clone(comp.getStepsLP());
                LocalPoint[] newPathLP = ArrayUtils.clone(comp.getPathLP());
                Coordinate[] newCoordinates = ArrayUtils.clone(comp.getCoordinates());
                ProgramComp newComp = new ProgramComp(newSteps, newPath, newStepsLP, newPathLP, newCoordinates, 0, comp.getSpeed(), comp.getTurnSpeed(), comp.getIdleAnim(), comp.getWalkAnim(), comp.getMovementType(), getRandomColor(), comp.isLoop(), comp.isProgramActive());

                String newName = textField.getText();
                Matcher matcher = pattern.matcher(newName);
                if (matcher.find())
                {
                    String duplicate = matcher.group();
                    duplicate = duplicate.replace("(", "");
                    duplicate = duplicate.replace(")", "");

                    int duplicateNumber = Integer.parseInt(duplicate) + 1;
                    newName = newName.replaceFirst("(?s)" + duplicate + "(?!.*?" + duplicate + ")", "" + duplicateNumber);
                }
                else
                {
                    newName = newName + " (1)";
                }

                Program newProgram = new Program(newComp, new JPanel(), new JLabel(), new JSpinner());
                ObjectPanel panel = createPanel(
                        panelArray,
                        parentPanel,
                        newName,
                        (int) modelSpinner.getValue(),
                        (CustomModel) modelComboBox.getSelectedItem(),
                        character.isCustomMode(), minimized[0],
                        (int) orientationSpinner.getValue(),
                        (int) animationSpinner.getValue(),
                        (int) radiusSpinner.getValue(),
                        newProgram,
                        character.getRuneLiteObject().isActive(),
                        character.getNonInstancedPoint(),
                        character.getInstancedPoint(),
                        character.getInstancedRegions(),
                        character.getInstancedPlane(),
                        character.isInInstance());
                addPanel(panelArray, parentPanel, panel);
            });

            minimizeButton.addActionListener(e ->
            {
                if (!character.isMinimized())
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
                    character.setMinimized(true);
                    minimizeButton.setToolTipText("Maximize");
                    objectPanel.updateUI();
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
                character.setMinimized(false);
                minimizeButton.setToolTipText("Minimize");
                objectPanel.updateUI();
            });

            objectPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    setSelectedCharacter(character, objectPanel);
                }
            });

            objectPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered (MouseEvent e)
                {
                    setHoveredCharacter(character, objectPanel);
                }

                @Override
                public void mouseExited (MouseEvent e)
                {
                    unsetHoveredCharacter(character, objectPanel);
                }
            });

            ArrayList<Component> list = new ArrayList<>();
            for (Component component : objectPanel.getComponents())
            {
                list.add(component);

                if (component instanceof Container)
                {
                    Container container = (Container) component;
                    list.addAll(List.of(container.getComponents()));

                    for (Component comp : container.getComponents())
                    {
                        if (comp instanceof Container)
                        {
                            Container container1 = (Container) comp;
                            list.addAll(List.of(container1.getComponents()));
                        }
                    }
                }
            }

            for (Component component : list)
            {
                component.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered (MouseEvent e)
                    {
                        setHoveredCharacter(character, objectPanel);
                    }
                    @Override
                    public void mouseExited (MouseEvent e)
                    {
                        unsetHoveredCharacter(character, objectPanel);
                    }
                });

                String compName = component.getName();
                if (compName == null || (!compName.equals("Duplicate") && !compName.equals("Delete")))
                {
                    component.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            setSelectedCharacter(character, objectPanel);
                        }
                    });
                }
            }

            setSelectedCharacter(character, objectPanel);
        });

        allObjectPanels.add(objectPanel);
        comboBoxes.add(modelComboBox);
        return objectPanel;
    }

    public void addPanel(ArrayList<ObjectPanel> panelArray, JPanel parentPanel, ObjectPanel childPanel)
    {
        panelArray.add(childPanel);
        parentPanel.add(childPanel, cNPC);
        if (parentPanel != mainPanel)
            toolBox.getManagerPanel().getFolderTree().addNode(childPanel);

        parentPanel.repaint();
        parentPanel.revalidate();
        npcPanels++;
    }

    public void removePanel(ArrayList<ObjectPanel> panelArray, JPanel parentPanel, ObjectPanel childPanel)
    {
        panelArray.remove(childPanel);
        parentPanel.remove(childPanel);
        if (parentPanel != mainPanel)
            toolBox.getManagerPanel().getFolderTree().removeNode(childPanel);

        parentPanel.repaint();
        parentPanel.revalidate();
    }

    public void clearPanels()
    {
        Character[] characters = new Character[0];
        JPanel[] programPanels = new JPanel[0];
        for (Character character : plugin.getCharacters())
        {
            if (sideObjectPanels.contains(character.getObjectPanel()))
            {
                if (character == plugin.getSelectedCharacter())
                    unsetSelectedCharacter();

                characters = ArrayUtils.add(characters, character);
                programPanels = ArrayUtils.add(programPanels, character.getProgram().getProgramPanel());
                allObjectPanels.remove(character.getObjectPanel());
            }
        }

        sideObjectPanels.clear();
        plugin.clearCharacters(characters);
        mainPanel.removeAll();
        mainPanel.updateUI();

        JPanel programmerAllPanel = programPanel.getAllPanel();
        Component[] components = programmerAllPanel.getComponents();
        for (int i = 0; i < components.length; i++)
        {
            Component component = components[i];
            if (component instanceof JPanel)
            {
                JPanel program = (JPanel) component;
                if (Arrays.stream(programPanels).anyMatch(n -> n == program))
                    programmerAllPanel.remove(program);
            }
        }

        programPanel.revalidate();
        programPanel.repaint();
    }

    public void setSelectedCharacter(Character selected, JPanel jPanel)
    {
        for (JPanel panel : allObjectPanels)
            panel.setBorder(defaultBorder);

        jPanel.setBorder(selectedBorder);
        plugin.setSelectedCharacter(selected);
    }

    public void unsetSelectedCharacter()
    {
        for (JPanel panel : allObjectPanels)
            panel.setBorder(defaultBorder);

        plugin.setSelectedCharacter(null);
    }

    public void setHoveredCharacter(Character hovered, JPanel jPanel)
    {
        if (plugin.getSelectedCharacter() == hovered)
        {
            return;
        }

        jPanel.setBorder(hoveredBorder);
        plugin.setHoveredCharacter(hovered);
    }

    public void unsetHoveredCharacter(Character hoverRemoved, JPanel jPanel)
    {
        plugin.setHoveredCharacter(null);

        if (plugin.getSelectedCharacter() == hoverRemoved)
        {
            return;
        }

        jPanel.setBorder(defaultBorder);
    }

    public void addModelOption(CustomModel model, boolean setComboBox)
    {
        modelOrganizer.createModelPanel(model);
        Character selectedNPC = plugin.getSelectedCharacter();

        for (JComboBox<CustomModel> comboBox : comboBoxes)
        {
            comboBox.addItem(model);
            if (!setComboBox || selectedNPC == null)
                continue;

            JComboBox<CustomModel> selectedBox = selectedNPC.getComboBox();
            if (comboBox == selectedBox)
            {
                comboBox.setSelectedItem(model);
                selectedNPC.setCustomMode(true);
                selectedNPC.getModelButton().setText("Custom");

                if (selectedNPC.getModelSpinner().isVisible() || comboBox.isVisible())
                {
                    comboBox.setVisible(true);
                    selectedNPC.getModelSpinner().setVisible(false);
                }
            }

            selectedNPC.setStoredModel(model);
            plugin.setModel(selectedNPC, true, -1);
        }
    }

    public void removeModelOption(CustomModel model)
    {
        for (JComboBox<CustomModel> comboBox : comboBoxes)
        {
            comboBox.removeItem(model);
        }
        modelOrganizer.removeModelPanel(model);
    }

    private Color getRandomColor()
    {
        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();
        return new Color(r, g, b);
    }

    private Program createEmptyProgram()
    {
        ProgramComp comp = new ProgramComp(new WorldPoint[0], new WorldPoint[0], new LocalPoint[0], new LocalPoint[0], new Coordinate[0], 0, 1, DEFAULT_TURN_SPEED, -1, -1, MovementType.NORMAL, getRandomColor(), false, false);
        return new Program(comp, new JPanel(), new JLabel(), new JSpinner());
    }

    private void openSaveDialog()
    {
        File outputDir = SETUP_DIR;
        outputDir.mkdirs();

        JFileChooser fileChooser = new JFileChooser(outputDir)
        {
            @Override
            public void approveSelection()
            {
                File f = getSelectedFile();
                if (!f.getName().endsWith(".json"))
                {
                    f = new File(f.getPath() + ".json");
                }
                if (f.exists() && getDialogType() == SAVE_DIALOG)
                {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            "File already exists, overwrite?",
                            "Warning",
                            JOptionPane.YES_NO_CANCEL_OPTION
                    );
                    switch (result)
                    {
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                        case JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }
        };
        fileChooser.setSelectedFile(new File("setup"));
        fileChooser.setDialogTitle("Save current setup");

        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().endsWith(".json"))
            {
                selectedFile = new File(selectedFile.getPath() + ".json");
            }
            saveToFile(selectedFile);
        }
    }

    public void saveToFile(File file)
    {
        ArrayList<Character> characters = plugin.getCharacters();
        CharacterSave[] characterSaves = new CharacterSave[characters.size()];

        ArrayList<CustomModel> customModels = plugin.getStoredModels();
        CustomModelComp[] comps = new CustomModelComp[customModels.size()];

        for (int i = 0; i < comps.length; i++)
        {
            comps[i] = customModels.get(i).getComp();
        }

        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            String name = character.getName();
            boolean locationSet = character.isLocationSet();
            WorldPoint savedWorldPoint = character.getNonInstancedPoint();
            LocalPoint savedLocalPoint = character.getInstancedPoint();
            int[] localPointRegion = character.getInstancedRegions();
            int localPointPlane = character.getInstancedPlane();
            boolean inInstance = character.isInInstance();
            int compId = 0;
            CustomModel storedModel = character.getStoredModel();
            if (storedModel != null)
            {
                for (int e = 0; e < comps.length; e++)
                {
                    CustomModelComp comp = comps[e];
                    if (storedModel.getComp() == comp)
                    {
                        compId = e;
                        break;
                    }
                }
            }

            boolean customMode = character.isCustomMode();
            boolean minimized = character.isMinimized();
            int modelId = (int) character.getModelSpinner().getValue();
            boolean active = character.getRuneLiteObject().isActive();
            int radius = character.getRuneLiteObject().getRadius();
            int rotation = (int) character.getOrientationSpinner().getValue();
            int animationId = (int) character.getAnimationSpinner().getValue();
            ProgramComp programComp = character.getProgram().getComp();

            characterSaves[i] = new CharacterSave(name, locationSet, savedWorldPoint, savedLocalPoint, localPointRegion, localPointPlane, inInstance, compId, customMode, minimized, modelId, active, radius, rotation, animationId, programComp);
        }

        SetupSave saveFile = new SetupSave(comps, characterSaves);

        try {
            FileWriter writer = new FileWriter(file, false);
            String string = plugin.gson.toJson(saveFile);
            writer.write(string);

            writer.close();
        }
        catch (IOException e)
        {
            System.out.println("Error occurred while writing to file.");
        }
    }

    private void openLoadCustomModelDialog()
    {
        MODELS_DIR.mkdirs();

        JFileChooser fileChooser = new JFileChooser(MODELS_DIR);
        fileChooser.setDialogTitle("Choose a model to load");

        JCheckBox priorityCheckbox = new JCheckBox("Set Priority?");
        priorityCheckbox.setToolTipText("May resolve some rendering issues by setting all faces to the same priority. Leave off if you're unsure");

        JComboBox<LightingStyle> comboBox = new JComboBox<>();
        comboBox.setToolTipText("Sets the lighting style");
        comboBox.addItem(LightingStyle.DEFAULT);
        comboBox.addItem(LightingStyle.ACTOR);
        comboBox.addItem(LightingStyle.NONE);
        comboBox.setFocusable(false);

        JPanel accessory = new JPanel();
        accessory.setLayout(new GridLayout(0, 1));
        accessory.add(priorityCheckbox);
        accessory.add(comboBox);

        fileChooser.setAccessory(accessory);

        int option = fileChooser.showOpenDialog(fileChooser);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            String name = selectedFile.getName();
            if (name.endsWith(".json"))
                name = replaceLast(name, ".json");

            if (name.endsWith(".txt"))
                name = replaceLast(name, ".txt");

            plugin.loadCustomModel(selectedFile, priorityCheckbox.isSelected(), (LightingStyle) comboBox.getSelectedItem(), name);
        }
    }

    private String replaceLast(String string, String from)
    {
        int lastIndex = string.lastIndexOf(from);
        if (lastIndex < 0)
            return string;
        String tail = string.substring(lastIndex).replaceFirst(from, "");
        return string.substring(0, lastIndex) + tail;
    }

    private void openLoadSetupDialog()
    {
        File outputDir = SETUP_DIR;
        outputDir.mkdirs();

        JFileChooser fileChooser = new JFileChooser(outputDir);
        fileChooser.setDialogTitle("Choose a setup to load");

        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            try
            {
                Reader reader = Files.newBufferedReader(selectedFile.toPath());
                SetupSave saveFile = plugin.gson.fromJson(reader, SetupSave.class);
                clientThread.invokeLater(() -> loadSetup(saveFile));
                reader.close();
            }
            catch (Exception e)
            {
                plugin.sendChatMessage("An error occurred while attempting to read this file.");
            }
        }
    }

    public void loadSetup(File file)
    {
        try
        {
            Reader reader = Files.newBufferedReader(file.toPath());
            SetupSave saveFile = plugin.gson.fromJson(reader, SetupSave.class);
            clientThread.invokeLater(() -> loadSetup(saveFile));
            reader.close();
        }
        catch (Exception e)
        {
            plugin.sendChatMessage("An error occurred while attempting to read this file.");
        }
    }

    private void loadSetup(SetupSave saveFile)
    {
        CustomModelComp[] comps = saveFile.getComps();
        CharacterSave[] characterSaves = saveFile.getSaves();
        CustomModel[] customModels = new CustomModel[comps.length];

        if (comps.length == 0)
        {
            for (CharacterSave save : characterSaves)
            {
                SwingUtilities.invokeLater(() ->
                {
                    Program program = new Program(save.getProgramComp(), new JPanel(), new JLabel(), new JSpinner());
                    ObjectPanel panel = createPanel(sideObjectPanels, mainPanel, save.getName(), save.getModelId(), null, save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance());
                    addPanel(sideObjectPanels, mainPanel, panel);
                });
            }
            return;
        }

        for (int i = 0; i < comps.length; i++)
        {
            CustomModelComp comp = comps[i];
            Model model;
            CustomModel customModel;
            ModelStats[] modelStats;

            switch (comp.getType())
            {
                case FORGED:
                    model = plugin.createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle());
                    customModel = new CustomModel(model, comp);
                    break;
                case CACHE_NPC:
                    modelStats = comp.getModelStats();
                    model = plugin.constructModelFromCache(modelStats, new int[0], false, true);
                    customModel = new CustomModel(model, comp);
                    break;
                case CACHE_PLAYER:
                    modelStats = comp.getModelStats();
                    model = plugin.constructModelFromCache(modelStats, comp.getKitRecolours(), true, true);
                    customModel = new CustomModel(model, comp);
                    break;
                default:
                case CACHE_OBJECT:
                case CACHE_GROUND_ITEM:
                    modelStats = comp.getModelStats();
                    model = plugin.constructModelFromCache(modelStats, null, false, false);
                    customModel = new CustomModel(model, comp);
            }

            plugin.addCustomModel(customModel, false);
            customModels[i] = customModel;
        }

        for (CharacterSave save : characterSaves)
        {
            SwingUtilities.invokeLater(() ->
            {
                Program program = new Program(save.getProgramComp(), new JPanel(), new JLabel(), new JSpinner());
                ObjectPanel panel = createPanel(sideObjectPanels, mainPanel, save.getName(), save.getModelId(), customModels[save.getCompId()], save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance());
                addPanel(sideObjectPanels, mainPanel, panel);
            });
        }
    }

    private void actionPerformed(ActionEvent e) {
        openSaveDialog();
    }
}