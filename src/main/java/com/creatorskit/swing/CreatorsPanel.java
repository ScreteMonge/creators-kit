package com.creatorskit.swing;

import com.creatorskit.saves.CharacterSave;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.Character;
import com.creatorskit.saves.FolderNodeSave;
import com.creatorskit.saves.SetupSave;
import com.creatorskit.models.*;
import com.creatorskit.programming.Coordinate;
import com.creatorskit.programming.MovementType;
import com.creatorskit.programming.Program;
import com.creatorskit.programming.ProgramComp;
import com.creatorskit.swing.jtree.FolderTree;
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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class CreatorsPanel extends PluginPanel
{
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final ToolBoxFrame toolBox;
    private final ModelAnvil modelAnvil;
    private final ModelOrganizer modelOrganizer;
    private final ProgrammerPanel programmerPanel;
    private final TransmogPanel transmogPanel;
    private final JButton addObjectButton = new JButton();
    private final JPanel sidePanel = new JPanel();
    private final GridBagConstraints cNPC = new GridBagConstraints();
    private final GridBagConstraints cManager = new GridBagConstraints();
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
    private final BufferedImage SWITCH = ImageUtil.loadImageResource(getClass(), "/Switch.png");
    private final BufferedImage SWITCH_ALL = ImageUtil.loadImageResource(getClass(), "/Switch All.png");
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
        this.programmerPanel = toolBox.getProgramPanel();
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
                    ObjectPanel panel = createPanel(sideObjectPanels, sidePanel);
                    addPanel(sideObjectPanels, sidePanel, panel);
                });
        add(addObjectButton, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        /*
        JButton saveButton = new JButton(new ImageIcon(SAVE));
        saveButton.setFocusable(false);
        saveButton.setToolTipText("Save this setup");
        add(saveButton, c);
        saveButton.addActionListener(this::actionPerformed);

         */

        c.gridx = 4;
        c.gridy = 1;
        JButton switchAllButton = new JButton(new ImageIcon(SWITCH_ALL));
        switchAllButton.setFocusable(false);
        switchAllButton.setToolTipText("Send all Objects from this Side Panel to the currently open folder in the Manager");
        add(switchAllButton, c);
        switchAllButton.addActionListener(e -> {
            ObjectPanel[] objectPanels = sideObjectPanels.toArray(new ObjectPanel[sideObjectPanels.size()]);
            for (ObjectPanel objectPanel : objectPanels)
            {
                switchPanel(objectPanel);
            }
        });

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
        clearButton.addActionListener(e -> clearSidePanels(true));

        c.gridwidth = 5;
        c.gridx = 0;
        c.gridy = 3;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        sidePanel.setLayout(new GridBagLayout());
        sidePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(sidePanel, c);

        cNPC.fill = GridBagConstraints.HORIZONTAL;
        cNPC.insets = new Insets(2, 2, 2, 2);
        cNPC.gridx = 0;
        cNPC.gridy = GridBagConstraints.RELATIVE;
        cNPC.weightx = 1;
        cNPC.weighty = 1;

        cManager.fill = GridBagConstraints.NONE;
        cManager.insets = new Insets(2, 2, 2, 2);
        cManager.gridx = 0;
        cManager.gridy = 0;
        cManager.anchor = GridBagConstraints.FIRST_LINE_START;
        cManager.weightx = 0;
        cManager.weighty = 0;
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
        JPanel programPanel = program.getProgramPanel();
        ObjectPanel objectPanel = new ObjectPanel(name, programPanel, parentPanel);
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
        topButtonsPanel.setLayout(new GridLayout(1, 3, 0, 0));
        objectPanel.add(topButtonsPanel, c);

        JButton switchButton = new JButton(new ImageIcon(SWITCH));
        switchButton.setName("Switch");
        switchButton.setToolTipText("Switch this Object between the Manager and Side Panel");
        switchButton.setFocusable(false);
        topButtonsPanel.add(switchButton);

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
        orientationLabel.setToolTipText("0 = South, 512 = West, 1024 = North, 1536 = East, 2048 = Max");
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
                    programmerPanel.createProgramPanel(character, programPanel, programmerNameLabel, programmerIdleSpinner));

            textField.addActionListener(e ->
            {
                character.setName(textField.getText());
                objectPanel.setName(textField.getText());
                character.getProgram().getNameLabel().setText(textField.getText());
                toolBox.getManagerPanel().revalidate();
                toolBox.getManagerPanel().repaint();
            });

            textField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {}

                @Override
                public void focusLost(FocusEvent e)
                {
                    character.setName(textField.getText());
                    objectPanel.setName(textField.getText());
                    character.getProgram().getNameLabel().setText(textField.getText());
                    toolBox.getManagerPanel().revalidate();
                    toolBox.getManagerPanel().repaint();
                }
            });


            switchButton.addActionListener(e ->
            {
                switchPanel(objectPanel);
            });

            deleteButton.addActionListener(e ->
            {
                onDeleteButtonPressed(objectPanel);
            });

            duplicateButton.addActionListener(e ->
            {
                ProgramComp comp = program.getComp();

                WorldPoint[] newSteps = ArrayUtils.clone(comp.getStepsWP());
                WorldPoint[] newPath = ArrayUtils.clone(comp.getPathWP());
                LocalPoint[] newStepsLP = ArrayUtils.clone(comp.getStepsLP());
                LocalPoint[] newPathLP = ArrayUtils.clone(comp.getPathLP());
                Coordinate[] newCoordinates = ArrayUtils.clone(comp.getCoordinates());
                Color newColor = getRandomColor();
                ProgramComp newComp = new ProgramComp(
                        newSteps,
                        newPath,
                        newStepsLP,
                        newPathLP,
                        newCoordinates,
                        0,
                        comp.getSpeed(),
                        comp.getTurnSpeed(),
                        comp.getIdleAnim(),
                        comp.getWalkAnim(),
                        comp.getMovementType(),
                        newColor.getRGB(),
                        comp.isLoop(),
                        comp.isProgramActive());

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

                Program newProgram = new Program(newComp, new JPanel(), new JLabel(), new JSpinner(), newColor);
                ObjectPanel panel = createPanel(
                        panelArray,
                        objectPanel.getParentPanel(),
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
                addPanel(panelArray, objectPanel.getParentPanel(), panel);
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
        addPanel(panelArray, parentPanel, null, childPanel);
    }

    public void addPanel(ArrayList<ObjectPanel> panelArray, JPanel parentPanel, DefaultMutableTreeNode parentNode, ObjectPanel childPanel)
    {
        panelArray.add(childPanel);

        if (parentPanel == sidePanel)
        {
            parentPanel.add(childPanel, cNPC);
            programmerPanel.addSideProgram(childPanel.getProgramPanel());
        }

        JPanel objectHolder = toolBox.getManagerPanel().getObjectHolder();
        if (parentPanel == objectHolder)
        {
            parentPanel.add(childPanel, cManager);
            FolderTree folderTree = toolBox.getManagerPanel().getFolderTree();
            if (parentNode == null)
            {
                folderTree.addNode(childPanel);
            }
            else
            {
                folderTree.addNode(parentNode, childPanel);
            }

            folderTree.resetObjectHolder();
        }

        parentPanel.repaint();
        parentPanel.revalidate();
        npcPanels++;
    }

    public void onDeleteButtonPressed(ObjectPanel objectPanel)
    {
        removePanel(objectPanel.getParentPanel(), objectPanel);
        allObjectPanels.remove(objectPanel);
        SwingUtilities.invokeLater(() ->
                programmerPanel.removeProgramPanel(objectPanel.getProgramPanel()));

        ArrayList<Character> characters = plugin.getCharacters();
        for (Character character : characters)
        {
            if (character.getObjectPanel() == objectPanel)
            {
                clientThread.invokeLater(() ->
                        character.getRuneLiteObject().setActive(false));
                characters.remove(character);
                if (plugin.getSelectedCharacter() == character)
                    plugin.setSelectedCharacter(null);
                objectPanel.getParentPanel().repaint();
                objectPanel.getParentPanel().revalidate();
                return;
            }
        }
    }

    public void removePanel(JPanel parentPanel, ObjectPanel childPanel)
    {
        if (parentPanel == sidePanel)
        {
            parentPanel.remove(childPanel);
            programmerPanel.removeSideProgram(childPanel.getProgramPanel());
            sideObjectPanels.remove(childPanel);
        }

        ManagerPanel managerPanel = toolBox.getManagerPanel();
        JPanel objectHolder = managerPanel.getObjectHolder();
        if (parentPanel == objectHolder)
        {
            parentPanel.remove(childPanel);
            FolderTree folderTree = managerPanel.getFolderTree();
            folderTree.removeNode(childPanel);
            folderTree.resetObjectHolder();
            managerPanel.getManagerObjectPanels().remove(childPanel);
        }

        parentPanel.repaint();
        parentPanel.revalidate();
    }

    public void clearSidePanels(boolean warning)
    {
        if (warning)
        {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all Objects from the Side Panel?");
            if (result != JOptionPane.YES_OPTION)
                return;
        }

        Character[] characters = new Character[0];

        for (Character character : plugin.getCharacters())
        {
            if (sideObjectPanels.contains(character.getObjectPanel()))
            {
                if (character == plugin.getSelectedCharacter())
                    unsetSelectedCharacter();

                characters = ArrayUtils.add(characters, character);
                allObjectPanels.remove(character.getObjectPanel());
            }
        }

        sideObjectPanels.clear();
        programmerPanel.clearSidePrograms();
        plugin.removeCharacters(characters);
        sidePanel.removeAll();
        sidePanel.repaint();
        sidePanel.revalidate();
        programmerPanel.repaint();
        programmerPanel.revalidate();
    }

    public void clearManagerPanels()
    {
        Character[] characters = new Character[0];
        ArrayList<ObjectPanel> managerObjectPanels = toolBox.getManagerPanel().getManagerObjectPanels();

        for (Character character : plugin.getCharacters())
        {
            if (managerObjectPanels.contains(character.getObjectPanel()))
            {
                if (character == plugin.getSelectedCharacter())
                    unsetSelectedCharacter();

                characters = ArrayUtils.add(characters, character);
                allObjectPanels.remove(character.getObjectPanel());
            }
        }

        allObjectPanels.clear();
        toolBox.getManagerPanel().getObjectHolder().removeAll();
        toolBox.getManagerPanel().getFolderTree().resetObjectHolder();
        plugin.removeCharacters(characters);
        programmerPanel.repaint();
        programmerPanel.revalidate();
    }

    public void switchPanel(ObjectPanel objectPanel)
    {
        npcPanels--;
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        JPanel objectHolder = managerPanel.getObjectHolder();
        ArrayList<ObjectPanel> managerObjectPanels = managerPanel.getManagerObjectPanels();

        if (objectPanel.getParentPanel() == sidePanel)
        {
            removePanel(sidePanel, objectPanel);
            objectPanel.setParentPanel(objectHolder);
            addPanel(managerObjectPanels, objectHolder, objectPanel);
            return;
        }

        if (objectPanel.getParentPanel() == objectHolder)
        {
            removePanel(objectHolder, objectPanel);
            objectPanel.setParentPanel(sidePanel);
            addPanel(sideObjectPanels, sidePanel, objectPanel);
        }
    }

    public void setSelectedCharacter(ObjectPanel objectPanel)
    {
        for (Character character : plugin.getCharacters())
        {
            if (character.getObjectPanel() == objectPanel)
            {
                setSelectedCharacter(character, objectPanel);
                return;
            }
        }
    }

    public void setSelectedCharacter(Character selected, ObjectPanel objectPanel)
    {
        for (int i = 0; i < allObjectPanels.size(); i++)
        {
            ObjectPanel panel = allObjectPanels.get(i);
            panel.setBorder(defaultBorder);
        }

        objectPanel.setBorder(selectedBorder);
        plugin.setSelectedCharacter(selected);
    }

    public void unsetSelectedCharacter()
    {
        for (int i = 0; i < allObjectPanels.size(); i++)
        {
            ObjectPanel panel = allObjectPanels.get(i);
            panel.setBorder(defaultBorder);
        }

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
        Color color = getRandomColor();
        ProgramComp comp = new ProgramComp(new WorldPoint[0], new WorldPoint[0], new LocalPoint[0], new LocalPoint[0], new Coordinate[0], 0, 1, DEFAULT_TURN_SPEED, -1, -1, MovementType.NORMAL, color.getRGB(), false, false);
        return new Program(comp, new JPanel(), new JLabel(), new JSpinner(), color);
    }

    public void openSaveDialog()
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
        CharacterSave[] characterSaves = new CharacterSave[0];

        ArrayList<CustomModel> customModels = plugin.getStoredModels();
        CustomModelComp[] comps = new CustomModelComp[customModels.size()];

        for (int i = 0; i < comps.length; i++)
        {
            comps[i] = customModels.get(i).getComp();
        }

        //Get Folder structure and all characters contained within
        FolderNodeSave folderNodeSave = getFolders(comps);

        //Get all characters in side panel
        for (Character character : characters)
        {
            if (!sideObjectPanels.contains(character.getObjectPanel()))
                continue;

            characterSaves = ArrayUtils.add(characterSaves, createCharacterSave(character, comps));
        }

        SetupSave saveFile = new SetupSave(comps, folderNodeSave, characterSaves);

        try
        {
            FileWriter writer = new FileWriter(file, false);
            String string = plugin.getGson().toJson(saveFile);
            writer.write(string);
            writer.close();
        }
        catch (IOException e)
        {
            plugin.sendChatMessage("An error occurred while writing to file.");
        }
    }

    public FolderNodeSave getFolders(CustomModelComp[] comps)
    {
        FolderNodeSave folderNodeSave = new FolderNodeSave(true, "Master Panel", new CharacterSave[0], new FolderNodeSave[0]);
        getFolderChildren(folderNodeSave, toolBox.getManagerPanel().getFolderTree().getRootNode(), comps);
        return folderNodeSave;
    }

    public void getFolderChildren(FolderNodeSave parentNodeSave, DefaultMutableTreeNode parent, CustomModelComp[] comps)
    {
        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            CharacterSave[] characterSaves = parentNodeSave.getCharacterSaves();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            if (node.getUserObject() instanceof ObjectPanel)
            {
                ObjectPanel objectPanel = (ObjectPanel) node.getUserObject();
                for (Character character : plugin.getCharacters())
                {
                    if (character.getObjectPanel() == objectPanel)
                    {
                        CharacterSave characterSave = createCharacterSave(character, comps);
                        parentNodeSave.setCharacterSaves(ArrayUtils.add(characterSaves, characterSave));
                        break;
                    }
                }
            }

            if (node.getUserObject() instanceof String)
            {
                FolderNodeSave folderNodeSave = new FolderNodeSave(false, (String) node.getUserObject(), new CharacterSave[0], new FolderNodeSave[0]);
                parentNodeSave.setFolderSaves(ArrayUtils.add(parentNodeSave.getFolderSaves(), folderNodeSave));

                if (!node.isLeaf())
                    getFolderChildren(folderNodeSave, node, comps);
            }
        }
    }

    private CharacterSave createCharacterSave(Character character, CustomModelComp[] comps)
    {
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

        return new CharacterSave(name, locationSet, savedWorldPoint, savedLocalPoint, localPointRegion, localPointPlane, inInstance, compId, customMode, minimized, modelId, active, radius, rotation, animationId, programComp);

    }

    public void openLoadSetupDialog()
    {
        File outputDir = SETUP_DIR;
        outputDir.mkdirs();

        JFileChooser fileChooser = new JFileChooser(outputDir);
        fileChooser.setDialogTitle("Choose a setup to load");

        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.exists())
            {
                selectedFile = new File(selectedFile.getPath() + ".json");
                if (!selectedFile.exists())
                {
                    plugin.sendChatMessage("Could not find the requested Setup file.");
                    return;
                }
            }

            try
            {
                Reader reader = Files.newBufferedReader(selectedFile.toPath());
                SetupSave saveFile = plugin.getGson().fromJson(reader, SetupSave.class);
                File finalSelectedFile = selectedFile;
                clientThread.invokeLater(() -> loadSetup(finalSelectedFile, saveFile));
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
            SetupSave saveFile = plugin.getGson().fromJson(reader, SetupSave.class);
            clientThread.invokeLater(() -> loadSetup(file, saveFile));
            reader.close();
        }
        catch (Exception e)
        {
            plugin.sendChatMessage("An error occurred while attempting to read this file.");
        }
    }

    private void loadSetup(File file, SetupSave saveFile)
    {
        CustomModelComp[] comps = saveFile.getComps();
        FolderNodeSave folderNodeSave = saveFile.getMasterFolderNode();
        CharacterSave[] characterSaves = saveFile.getSaves();
        CustomModel[] customModels = new CustomModel[comps.length];

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

        FolderTree folderTree = toolBox.getManagerPanel().getFolderTree();
        DefaultMutableTreeNode rootNode = folderTree.getRootNode();
        boolean v1_2Save = folderNodeSave == null;

        SwingUtilities.invokeLater(() ->
        {
            if (folderNodeSave != null)
                openFolderNodeSave(folderTree, rootNode, folderNodeSave, customModels);
        });


        for (CharacterSave save : characterSaves)
        {
            SwingUtilities.invokeLater(() ->
            {
                Color color = new Color(save.getProgramComp().getRgb());
                Program program = new Program(save.getProgramComp(), new JPanel(), new JLabel(), new JSpinner(), color);
                ObjectPanel objectPanel;

                if (customModels.length == 0)
                {
                    objectPanel = createPanel(sideObjectPanels, sidePanel, save.getName(), save.getModelId(), null, save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance());
                }
                else
                {
                    objectPanel = createPanel(sideObjectPanels, sidePanel, save.getName(), save.getModelId(), customModels[save.getCompId()], save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance());
                }

                addPanel(sideObjectPanels, sidePanel, objectPanel);
            });
        }

        if (v1_2Save)
        {
            plugin.sendChatMessage("The Setup you loaded appears to be an older version 1.2 Setup");
            plugin.sendChatMessage("Please re-save this Setup to update it to a newer 1.3 version");
        }
    }

    private void openFolderNodeSave(FolderTree folderTree, DefaultMutableTreeNode parentNode, FolderNodeSave folderNodeSave, CustomModel[] customModels)
    {
        String name = folderNodeSave.getName();
        DefaultMutableTreeNode node;
        if (folderNodeSave.isMasterFolder())
        {
            node = folderTree.getRootNode();
        }
        else
        {
            node = folderTree.addNode(parentNode, name);
        }

        ArrayList<ObjectPanel> managerObjectPanels = toolBox.getManagerPanel().getManagerObjectPanels();
        JPanel objectHolder = toolBox.getManagerPanel().getObjectHolder();
        for (CharacterSave save : folderNodeSave.getCharacterSaves())
        {
            Color color = new Color(save.getProgramComp().getRgb());
            Program program = new Program(save.getProgramComp(), new JPanel(), new JLabel(), new JSpinner(), color);
            ObjectPanel objectPanel;

            if (customModels.length == 0)
            {
                objectPanel = createPanel(managerObjectPanels, objectHolder, save.getName(), save.getModelId(), null, save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance());
            }
            else
            {
                objectPanel = createPanel(managerObjectPanels, objectHolder, save.getName(), save.getModelId(), customModels[save.getCompId()], save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance());
            }

            addPanel(managerObjectPanels, objectHolder, node, objectPanel);
        }

        FolderNodeSave[] folderNodeSaves = folderNodeSave.getFolderSaves();
        for (FolderNodeSave fns : folderNodeSaves)
        {
            openFolderNodeSave(folderTree, node, fns, customModels);
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
}