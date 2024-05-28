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
import net.runelite.api.Animation;
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
import net.runelite.client.util.LinkBrowser;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
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
    private final ModelImporter modelImporter;

    private final JButton addObjectButton = new JButton();
    private final JPanel sidePanel = new JPanel();
    private final GridBagConstraints cNPC = new GridBagConstraints();
    private final GridBagConstraints cManager = new GridBagConstraints();
    private final Random random = new Random();
    public static final File SETUP_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/setups");
    public static final File CREATORS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    private final Pattern pattern = Pattern.compile("\\(\\d+\\)\\Z");
    private int npcPanels = 0;
    private final ArrayList<ObjectPanel> allObjectPanels = new ArrayList<>();
    private final ArrayList<ObjectPanel> sideObjectPanels = new ArrayList<>();
    private final ArrayList<JComboBox<CustomModel>> comboBoxes = new ArrayList<>();
    private final Dimension spinnerSize = new Dimension(72, 30);
    private final Dimension BUTTON_SIZE = new Dimension(25, 25);
    private final int DEFAULT_TURN_SPEED = 40;
    private final BufferedImage MAXIMIZE = ImageUtil.loadImageResource(getClass(), "/Maximize.png");
    private final BufferedImage MINIMIZE = ImageUtil.loadImageResource(getClass(), "/Minimize.png");
    private final BufferedImage DUPLICATE = ImageUtil.loadImageResource(getClass(), "/Duplicate.png");
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage SWITCH = ImageUtil.loadImageResource(getClass(), "/Switch.png");
    private final BufferedImage SWITCH_ALL = ImageUtil.loadImageResource(getClass(), "/Switch All.png");
    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");
    private final BufferedImage LOAD = ImageUtil.loadImageResource(getClass(), "/Load.png");
    private final BufferedImage SAVE = ImageUtil.loadImageResource(getClass(), "/Save.png");
    private final BufferedImage FIND = ImageUtil.loadImageResource(getClass(), "/Find.png");
    private final BufferedImage CUSTOM_MODEL = ImageUtil.loadImageResource(getClass(), "/Custom model.png");
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    private final LineBorder defaultBorder = new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1);
    private final LineBorder hoveredBorder = new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1);
    private final LineBorder selectedBorder = new LineBorder(Color.WHITE, 1);

    @Inject
    public CreatorsPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin, ToolBoxFrame toolBox, ModelImporter modelImporter)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.toolBox = toolBox;
        this.modelOrganizer = toolBox.getModelOrganizer();
        this.programmerPanel = toolBox.getProgramPanel();
        this.modelAnvil = toolBox.getModelAnvil();
        this.transmogPanel = toolBox.getTransmogPanel();
        this.modelImporter = modelImporter;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 3;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
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


        c.gridwidth = 1;
        c.gridheight = 2;
        c.weightx = 5;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 1;
        addObjectButton.setFocusable(false);
        addObjectButton.setText("Add Object");
        addObjectButton.setToolTipText("Add an new Object to the palette");
        addObjectButton.addActionListener(e ->
        {
            ObjectPanel panel = createPanel(sidePanel);
            SwingUtilities.invokeLater(() -> addPanel(sideObjectPanels, sidePanel, panel));
        });
        add(addObjectButton, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 2;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 1;
        JButton findButton = new JButton(new ImageIcon(FIND));
        findButton.setFocusable(false);
        findButton.setToolTipText("Open the File Explorer and navigate to the Creator's Kit directory");
        add(findButton, c);
        findButton.addActionListener(e ->
        {
            try
            {
                LinkBrowser.open(CREATORS_DIR.getAbsolutePath());
            }
            catch (Exception exception)
            {
            }
        });

        c.gridx = 2;
        c.gridy = 1;
        JButton switchAllButton = new JButton(new ImageIcon(SWITCH_ALL));
        switchAllButton.setToolTipText("Send all Objects from this Side Panel to the currently open folder in the Manager");
        switchAllButton.setFocusable(false);
        add(switchAllButton, c);
        switchAllButton.addActionListener(e ->
        {
            ObjectPanel[] objectPanels = sideObjectPanels.toArray(new ObjectPanel[sideObjectPanels.size()]);
            switchPanels(sidePanel, objectPanels);
        });

        c.gridx = 1;
        c.gridy = 2;
        JButton loadCustomModelButton = new JButton(new ImageIcon(CUSTOM_MODEL));
        loadCustomModelButton.setFocusable(false);
        loadCustomModelButton.setToolTipText("Load a previously saved Custom Model");
        add(loadCustomModelButton, c);
        loadCustomModelButton.addActionListener(e -> openLoadCustomModelDialog());

        c.gridx = 2;
        c.gridy = 2;
        JButton clearButton = new JButton(new ImageIcon(CLEAR));
        clearButton.setFocusable(false);
        clearButton.setToolTipText("Clears all Objects");
        add(clearButton, c);
        clearButton.addActionListener(e -> clearSidePanels(true));

        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 0;
        sidePanel.setLayout(new GridBagLayout());
        sidePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(sidePanel, c);

        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 1;
        c.weighty = 1;
        JLabel emptyLabel = new JLabel("");
        add(emptyLabel, c);

        cNPC.insets = new Insets(1, 2, 1, 2);
        cNPC.gridx = 0;
        cNPC.gridy = GridBagConstraints.RELATIVE;
        cNPC.weightx = 1;
        cNPC.weighty = 1;

        cManager.insets = new Insets(1, 2, 1, 2);
        cManager.gridx = 0;
        cManager.gridy = 0;
        cManager.anchor = GridBagConstraints.FIRST_LINE_START;
        cManager.weightx = 0;
        cManager.weighty = 0;
    }

    public ObjectPanel createPanel(JPanel parentPanel)
    {
        return createPanel(parentPanel, "Object (" + npcPanels + ")", 7699, null, false, false, 0,  -1, 60, createEmptyProgram(-1, -1), false, null, null, new int[0], -1, false, false);
    }

    public ObjectPanel createPanel(
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
                              boolean inInstance,
                              boolean setHoveredLocation)
    {
        JPanel programPanel = program.getProgramPanel();
        ObjectPanel objectPanel = new ObjectPanel(name, programPanel, parentPanel);
        objectPanel.setLayout(new GridBagLayout());

        JTextField textField = new JTextField(name);
        Dimension textDimension = new Dimension(140, 30);
        textField.setMaximumSize(textDimension);
        textField.setPreferredSize(textDimension);
        textField.setMinimumSize(textDimension);

        JPanel topButtonsPanel = new JPanel();
        Dimension topButtonsPanelSize = new Dimension(81, 30);
        topButtonsPanel.setMaximumSize(topButtonsPanelSize);
        topButtonsPanel.setPreferredSize(topButtonsPanelSize);
        topButtonsPanel.setMinimumSize(topButtonsPanelSize);
        topButtonsPanel.setLayout(new GridLayout(1, 3, 0, 0));

        JButton switchButton = new JButton(new ImageIcon(SWITCH));
        switchButton.setToolTipText("Switch this Object between the Manager and Side Panel");
        switchButton.setFocusable(false);

        JButton duplicateButton = new JButton(new ImageIcon(DUPLICATE));
        duplicateButton.setToolTipText("Duplicate object");
        duplicateButton.setFocusable(false);

        ImageIcon minimize = new ImageIcon(MINIMIZE);
        ImageIcon maximize = new ImageIcon(MAXIMIZE);
        JButton minimizeButton = new JButton(minimize);
        minimizeButton.setToolTipText("Minimize");
        minimizeButton.setFocusable(false);

        JButton deleteButton = new JButton(new ImageIcon(CLOSE));
        deleteButton.setToolTipText("Delete object");
        deleteButton.setFocusable(false);

        //Buttons
        JButton modelButton = new JButton();
        modelButton.setFont(FontManager.getRunescapeFont());
        String modelButtonText = customModeActive ? "Custom" : "Id";
        modelButton.setText(modelButtonText);
        modelButton.setToolTipText("Toggle between Custom Model and Model ID");
        modelButton.setFocusable(false);

        JButton spawnButton = new JButton();
        spawnButton.setFont(FontManager.getRunescapeFont());
        spawnButton.setText(active ? "Spawn" : "Despawn");
        spawnButton.setToolTipText("Toggle the NPC on or off");
        spawnButton.setFocusable(false);

        JButton relocateButton = new JButton();
        relocateButton.setFont(FontManager.getRunescapeFont());
        relocateButton.setText("Relocate");
        relocateButton.setToolTipText("Set the object's location to the selected tile");
        relocateButton.setFocusable(false);

        JButton animationButton = new JButton();
        animationButton.setFont(FontManager.getRunescapeFont());
        animationButton.setText("Anim Off");
        animationButton.setToolTipText("Toggle the playing animation");
        animationButton.setPreferredSize(new Dimension(90, 25));
        animationButton.setFocusable(false);

        //Labels
        JLabel modelLabel = new JLabel("Model ID:");
        modelLabel.setToolTipText("The ID number of the model to spawn");
        modelLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel orientationLabel = new JLabel("Rotation:");
        orientationLabel.setToolTipText("0 = South, 512 = West, 1024 = North, 1536 = East, 2048 = Max");
        orientationLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel radiusLabel = new JLabel("Radius:");
        radiusLabel.setToolTipText("Increasing the radius may prevent clipping issues with the ground");
        radiusLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel animationLabel = new JLabel("Anim ID:");
        animationLabel.setToolTipText("The animation ID number. -1 gives no animation");
        animationLabel.setFont(FontManager.getRunescapeSmallFont());

        //Spinners
        JSpinner modelSpinner = new JSpinner();
        modelSpinner.setPreferredSize(topButtonsPanelSize);
        modelSpinner.setMaximumSize(topButtonsPanelSize);
        modelSpinner.setMinimumSize(topButtonsPanelSize);
        modelSpinner.setValue(modelId);
        modelSpinner.setVisible(!customModeActive);

        JComboBox<CustomModel> modelComboBox = new JComboBox<>();
        modelComboBox.setPreferredSize(topButtonsPanelSize);
        modelComboBox.setMaximumSize(topButtonsPanelSize);
        modelComboBox.setMinimumSize(topButtonsPanelSize);
        modelComboBox.setFont(FontManager.getRunescapeFont());
        modelComboBox.setVisible(customModeActive);
        for (CustomModel model : plugin.getStoredModels())
        {
            modelComboBox.addItem(model);
        }
        if (customModel != null)
            modelComboBox.setSelectedItem(customModel);

        SpinnerModel orientationRange = new SpinnerNumberModel(orientation, 0, 2048, 1);
        JSpinner orientationSpinner = new JSpinner(orientationRange);

        JSpinner radiusSpinner = new JSpinner();
        radiusSpinner.setValue(radius);

        JSpinner animationSpinner = new JSpinner();
        animationSpinner.setValue(animationId);

        if (setMinimized)
        {
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
        }

        SwingUtilities.invokeLater(() ->
        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(1, 1, 1, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            objectPanel.add(textField, c);

            c.gridwidth = 1;
            c.gridx = 2;
            c.gridy = 0;
            c.weightx = 0;
            objectPanel.add(topButtonsPanel, c);
            topButtonsPanel.add(switchButton);
            topButtonsPanel.add(duplicateButton);
            topButtonsPanel.add(minimizeButton);
            topButtonsPanel.add(deleteButton);

            c.ipadx = 0;
            c.ipady = 5;
            c.gridwidth = 1;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 1;
            objectPanel.add(modelButton, c);

            c.gridy++;
            objectPanel.add(spawnButton, c);

            c.gridy++;
            objectPanel.add(relocateButton, c);

            c.gridy++;
            objectPanel.add(animationButton, c);

            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.LINE_END;
            c.ipadx = 0;
            c.ipady = 0;
            c.gridwidth = 1;
            c.weightx = 0;
            c.gridx = 1;
            c.gridy = 1;
            objectPanel.add(modelLabel, c);

            c.gridy++;
            objectPanel.add(orientationLabel, c);

            c.gridy++;
            objectPanel.add(radiusLabel, c);

            c.gridy++;
            objectPanel.add(animationLabel, c);

            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.CENTER;
            c.gridwidth = 1;
            c.gridx = 2;
            c.gridy = 1;
            objectPanel.add(modelSpinner, c);
            objectPanel.add(modelComboBox, c);

            c.gridy++;
            objectPanel.add(orientationSpinner, c);

            c.gridy++;
            objectPanel.add(radiusSpinner, c);

            c.gridy++;
            objectPanel.add(animationSpinner, c);
        });

        JLabel programmerNameLabel = program.getNameLabel();
        programmerNameLabel.setText(name);
        JSpinner programmerIdleSpinner = program.getIdleAnimSpinner();

        Character character = new Character(
                textField.getText(),
                active,
                worldPoint != null || localPoint != null,
                setMinimized,
                program,
                worldPoint,
                localPoint,
                localPointRegion,
                localPointPlane,
                inInstance,
                (CustomModel) modelComboBox.getSelectedItem(),
                objectPanel,
                customModeActive,
                textField,
                modelComboBox,
                spawnButton,
                modelButton,
                modelSpinner,
                animationSpinner,
                orientationSpinner,
                radiusSpinner,
                programmerNameLabel,
                programmerIdleSpinner,
                null,
                0);

        SwingUtilities.invokeLater(() -> programmerPanel.createProgramPanel(character, programPanel, programmerNameLabel, programmerIdleSpinner));

        textField.addActionListener(e ->
        {
            String text = StringHandler.cleanString(textField.getText());
            textField.setText(text);
            character.setName(text);
            objectPanel.setName(text);
            character.getProgram().getNameLabel().setText(text);
            toolBox.getManagerPanel().revalidate();
            toolBox.getManagerPanel().repaint();
        });

        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}

            @Override
            public void focusLost(FocusEvent e)
            {
                String text = StringHandler.cleanString(textField.getText());
                textField.setText(text);
                objectPanel.setName(text);
                character.setName(text);
                programmerNameLabel.setText(text);
                toolBox.getManagerPanel().revalidate();
                toolBox.getManagerPanel().repaint();
            }
        });

        switchButton.addActionListener(e -> switchPanel(objectPanel));

        deleteButton.addActionListener(e ->
        {
            onDeleteButtonPressed(objectPanel);
            objectPanel.getParentPanel().repaint();
            objectPanel.getParentPanel().revalidate();
        });

        duplicateButton.addActionListener(e -> onDuplicatePressed(character, false));

        minimizeButton.addActionListener(e ->
        {
            if (!character.isMinimized())
            {
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
            if (character.isCustomMode())
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

        relocateButton.addActionListener(e -> plugin.setLocation(character, !character.isLocationSet(), false, false, false));

        spawnButton.addActionListener(e -> plugin.toggleSpawn(spawnButton, character));

        animationButton.addActionListener(e ->
        {
            Animation anim = character.getRuneLiteObject().getAnimation();

            if (anim == null)
            {
                animationButton.setText("Anim Off");
                programmerIdleSpinner.setValue((int) animationSpinner.getValue());
                plugin.setAnimation(character, (int) animationSpinner.getValue());
                return;
            }

            int animId = anim.getId();

            if (animId == -1)
            {
                animationButton.setText("Anim Off");
                programmerIdleSpinner.setValue((int) animationSpinner.getValue());
                plugin.setAnimation(character, (int) animationSpinner.getValue());
                return;
            }

            animationButton.setText("Anim On");
            plugin.unsetAnimation(character);
        });

        modelButton.addActionListener(e ->
        {
            if (character.isCustomMode())
            {
                character.setCustomMode(false);
                modelButton.setText("Id");
                modelSpinner.setVisible(true);
                modelComboBox.setVisible(false);
                plugin.setModel(character, false, (int) modelSpinner.getValue());
            }
            else
            {
                character.setCustomMode(true);
                modelButton.setText("Custom");
                modelSpinner.setVisible(false);
                modelComboBox.setVisible(true);
                plugin.setModel(character, true, -1);
            }
        });

        modelSpinner.addChangeListener(e ->
        {
            int modelNumber = (int) modelSpinner.getValue();
            plugin.setModel(character, false, modelNumber);
        });

        modelComboBox.addItemListener(e ->
        {
            CustomModel m = (CustomModel) modelComboBox.getSelectedItem();
            character.setStoredModel(m);
            if (modelComboBox.isVisible() && character == plugin.getSelectedCharacter())
                plugin.setModel(character, true, -1);
        });

        orientationSpinner.addChangeListener(e ->
        {
            int orient = (int) orientationSpinner.getValue();
            plugin.setOrientation(character, orient);
        });

        animationSpinner.addChangeListener(e ->
        {
            animationButton.setText("Anim Off");
            int animationNumber = (int) animationSpinner.getValue();
            plugin.setAnimation(character, animationNumber);
            programmerIdleSpinner.setValue(animationNumber);
        });

        radiusSpinner.addChangeListener(e ->
        {
            int rad = (int) radiusSpinner.getValue();
            plugin.setRadius(character, rad);
        });

        addAllSelectListeners(
                character,
                objectPanel,
                textField,
                switchButton,
                topButtonsPanel,
                duplicateButton,
                minimizeButton,
                deleteButton,
                modelButton,
                spawnButton,
                relocateButton,
                animationButton,
                modelLabel,
                orientationLabel,
                radiusLabel,
                animationLabel,
                modelSpinner,
                modelComboBox,
                orientationSpinner,
                radiusSpinner,
                animationSpinner
        );

        setSelectedCharacter(character, objectPanel);

        plugin.setupRLObject(character, setHoveredLocation);
        plugin.getCharacters().add(character);

        comboBoxes.add(modelComboBox);
        return objectPanel;
    }

    private void addAllSelectListeners(
            Character character,
            ObjectPanel objectPanel,
            JTextField textField,
            JButton switchButton,
            JPanel topButtonsPanel,
            JButton duplicateButton,
            JButton minimizeButton,
            JButton deleteButton,
            JButton modelButton,
            JButton spawnButton,
            JButton relocateButton,
            JButton animationButton,
            JLabel modelLabel,
            JLabel orientationLabel,
            JLabel radiusLabel,
            JLabel animationLabel,
            JSpinner modelSpinner,
            JComboBox<CustomModel> modelComboBox,
            JSpinner orientationSpinner,
            JSpinner radiusSpinner,
            JSpinner animationSpinner)
    {
        addSelectListeners(objectPanel, character, objectPanel, true);
        addSelectListeners(textField, character, objectPanel, true);
        addSelectListeners(switchButton, character, objectPanel, true);
        addSelectListeners(topButtonsPanel, character, objectPanel, true);
        addSelectListeners(duplicateButton, character, objectPanel, false);
        addSelectListeners(minimizeButton, character, objectPanel, true);
        addSelectListeners(deleteButton, character, objectPanel, false);
        addSelectListeners(modelButton, character, objectPanel, true);
        addSelectListeners(spawnButton, character, objectPanel, true);
        addSelectListeners(relocateButton, character, objectPanel, true);
        addSelectListeners(animationButton, character, objectPanel, true);
        addSelectListeners(modelLabel, character, objectPanel, true);
        addSelectListeners(orientationLabel, character, objectPanel, true);
        addSelectListeners(radiusLabel, character, objectPanel, true);
        addSelectListeners(animationLabel, character, objectPanel, true);
        addSelectListeners(modelComboBox, character, objectPanel, true);

        for (Component c : modelSpinner.getEditor().getComponents())
        {
            addSelectListeners(c, character, objectPanel, true);
        }

        for (Component c : orientationSpinner.getEditor().getComponents())
        {
            addSelectListeners(c, character, objectPanel, true);
        }

        for (Component c : radiusSpinner.getEditor().getComponents())
        {
            addSelectListeners(c, character, objectPanel, true);
        }

        for (Component c : animationSpinner.getEditor().getComponents())
        {
            addSelectListeners(c, character, objectPanel, true);
        }
    }

    private void addSelectListeners(Component component, Character character, ObjectPanel objectPanel, boolean pressedListener)
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

        if (pressedListener)
        {
            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    setSelectedCharacter(character, objectPanel);
                }
            });
        }
    }

    public void addPanel(ArrayList<ObjectPanel> panelArray, JPanel parentPanel, ObjectPanel childPanel)
    {
        addPanel(panelArray, parentPanel, null, childPanel);
    }

    public void addPanel(ArrayList<ObjectPanel> panelArray, JPanel parentPanel, DefaultMutableTreeNode parentNode, ObjectPanel childPanel)
    {
        panelArray.add(childPanel);
        allObjectPanels.add(childPanel);

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

    public void onDuplicatePressed(Character character, boolean setLocation)
    {
        ProgramComp comp = character.getProgram().getComp();

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
                comp.isPathFound(),
                0,
                comp.getSpeed(),
                comp.getTurnSpeed(),
                comp.getIdleAnim(),
                comp.getWalkAnim(),
                comp.getMovementType(),
                newColor.getRGB(),
                comp.isLoop(),
                comp.isProgramActive());

        String newName = character.getName();
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

        JPanel parentPanel = character.getObjectPanel().getParentPanel();
        ArrayList<ObjectPanel> panelArray;
        if (parentPanel == sidePanel)
        {
            panelArray = sideObjectPanels;
        }
        else
        {
            panelArray = toolBox.getManagerPanel().getManagerObjectPanels();
        }

        String finalNewName = newName;
        Thread thread = new Thread(() ->
        {
            ObjectPanel panel = createPanel(
                    character.getObjectPanel().getParentPanel(),
                    finalNewName,
                    (int) character.getModelSpinner().getValue(),
                    (CustomModel) character.getComboBox().getSelectedItem(),
                    character.isCustomMode(), character.isMinimized(),
                    (int) character.getOrientationSpinner().getValue(),
                    (int) character.getAnimationSpinner().getValue(),
                    (int) character.getRadiusSpinner().getValue(),
                    newProgram,
                    character.getRuneLiteObject().isActive(),
                    character.getNonInstancedPoint(),
                    character.getInstancedPoint(),
                    character.getInstancedRegions(),
                    character.getInstancedPlane(),
                    character.isInInstance(),
                    setLocation);

            SwingUtilities.invokeLater(() -> addPanel(panelArray, parentPanel, panel));
        });
        thread.start();
    }

    public void onDeleteButtonPressed(ObjectPanel objectPanel)
    {
        removePanel(objectPanel);

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
                return;
            }
        }
    }

    public void deletePanels(ObjectPanel[] objectPanels)
    {
        Thread thread = new Thread(() ->
        {
            removePanels(objectPanels);

            ArrayList<Character> characters = plugin.getCharacters();
            for (ObjectPanel objectPanel : objectPanels)
            {
                for (int i = 0; i < characters.size(); i++)
                {
                    Character character = characters.get(i);
                    if (character.getObjectPanel() == objectPanel)
                    {
                        clientThread.invokeLater(() -> character.getRuneLiteObject().setActive(false));
                        characters.remove(character);
                        plugin.setSelectedCharacter(null);
                    }
                }
            }
        });

        thread.start();
    }

    public void removePanels(ObjectPanel[] objectPanels)
    {
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        JPanel objectHolder = managerPanel.getObjectHolder();
        ArrayList<ObjectPanel> managerObjectPanels = managerPanel.getManagerObjectPanels();
        FolderTree folderTree = managerPanel.getFolderTree();

        for (ObjectPanel objectPanel : objectPanels)
        {
            allObjectPanels.remove(objectPanel);
            JPanel parentPanel = objectPanel.getParentPanel();
            if (objectPanel.getParentPanel() == sidePanel)
            {
                parentPanel.remove(objectPanel);
                programmerPanel.removeSideProgram(objectPanel.getProgramPanel());
                sideObjectPanels.remove(objectPanel);
            }

            if (parentPanel == objectHolder)
            {
                parentPanel.remove(objectPanel);
                folderTree.removeNode(objectPanel);
                managerObjectPanels.remove(objectPanel);
            }
        }

        sidePanel.repaint();
        sidePanel.revalidate();
        objectHolder.repaint();
        objectHolder.revalidate();
        folderTree.resetObjectHolder();
    }

    public void removePanel(ObjectPanel objectPanel)
    {
        allObjectPanels.remove(objectPanel);
        JPanel parentPanel = objectPanel.getParentPanel();
        if (objectPanel.getParentPanel() == sidePanel)
        {
            parentPanel.remove(objectPanel);
            programmerPanel.removeSideProgram(objectPanel.getProgramPanel());
            sideObjectPanels.remove(objectPanel);
        }

        ManagerPanel managerPanel = toolBox.getManagerPanel();
        JPanel objectHolder = managerPanel.getObjectHolder();
        if (parentPanel == objectHolder)
        {
            parentPanel.remove(objectPanel);
            FolderTree folderTree = managerPanel.getFolderTree();
            folderTree.removeNode(objectPanel);
            folderTree.resetObjectHolder();
            managerPanel.getManagerObjectPanels().remove(objectPanel);
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

    public void switchPanels(JPanel switchFrom, ObjectPanel[] objectPanels)
    {
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        FolderTree folderTree = managerPanel.getFolderTree();
        JPanel objectHolder = toolBox.getManagerPanel().getObjectHolder();

        JPanel newParent;
        ArrayList<ObjectPanel> arrayFrom;
        ArrayList<ObjectPanel> arrayTo;
        if (switchFrom == sidePanel)
        {
            newParent = managerPanel.getObjectHolder();
            arrayFrom = sideObjectPanels;
            arrayTo = managerPanel.getManagerObjectPanels();
        }
        else
        {
            newParent = sidePanel;
            arrayFrom = managerPanel.getManagerObjectPanels();
            arrayTo = sideObjectPanels;
        }

        Thread thread = new Thread(() ->
        {
            for (ObjectPanel objectPanel : objectPanels)
            {
                //remove
                switchFrom.remove(objectPanel);
                if (switchFrom == sidePanel)
                {
                    programmerPanel.removeSideProgram(objectPanel.getProgramPanel());
                }
                else
                {
                    folderTree.removeNode(objectPanel);
                }

                arrayFrom.remove(objectPanel);
                objectPanel.setParentPanel(newParent);

                //re-add
                if (newParent == sidePanel)
                {
                    sidePanel.add(objectPanel, cNPC);
                    programmerPanel.addSideProgram(objectPanel.getProgramPanel());
                }
                else
                {
                    objectHolder.add(objectPanel, cManager);
                    folderTree.addNode(objectPanel);
                }

                arrayTo.add(objectPanel);
            }

            folderTree.resetObjectHolder();
            sidePanel.revalidate();
            objectHolder.revalidate();
        });
        thread.start();
    }

    public void switchPanel(ObjectPanel objectPanel)
    {
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        JPanel objectHolder = managerPanel.getObjectHolder();
        ArrayList<ObjectPanel> managerObjectPanels = managerPanel.getManagerObjectPanels();
        FolderTree folderTree = managerPanel.getFolderTree();

        JPanel parentPanel = objectPanel.getParentPanel();

        parentPanel.remove(objectPanel);
        if (parentPanel == sidePanel)
        {
            programmerPanel.removeSideProgram(objectPanel.getProgramPanel());
            sideObjectPanels.remove(objectPanel);
        }
        else
        {
            folderTree.removeNode(objectPanel);
            managerObjectPanels.remove(objectPanel);
        }

        JPanel newParent = parentPanel == sidePanel ? objectHolder : sidePanel;
        objectPanel.setParentPanel(newParent);

        if (newParent == sidePanel)
        {
            sidePanel.add(objectPanel, cNPC);
            programmerPanel.addSideProgram(objectPanel.getProgramPanel());
            sideObjectPanels.add(objectPanel);
        }
        else
        {
            objectHolder.add(objectPanel, cManager);
            folderTree.addNode(objectPanel);
            managerObjectPanels.add(objectPanel);
        }

        folderTree.resetObjectHolder();
        sidePanel.revalidate();
        objectHolder.revalidate();
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

    public void scrollSelectedCharacter(Character selected, int clicks)
    {
        int sidePanelSize = sideObjectPanels.size();
        ObjectPanel[] managerArray = toolBox.getManagerPanel().getShownObjectPanels();
        int managerSize = managerArray.length;

        boolean selectionFound = false;

        if (selected != null)
        {
            JPanel selectedParent = selected.getObjectPanel().getParentPanel();
            ObjectPanel[] list = selectedParent == sidePanel ? sideObjectPanels.toArray(new ObjectPanel[sideObjectPanels.size()]) : managerArray;
            int length = list.length;
            for (int i = 0; i < length; i++)
            {
                if (selected.getObjectPanel() == list[i])
                {
                    int index = clicks + i;
                    while (index >= length)
                    {
                        index -= length;
                    }

                    while (index < 0)
                    {
                        index += length;
                    }

                    unsetSelectedCharacter();
                    setSelectedCharacter(list[index]);
                    selectionFound = true;
                    break;
                }
            }
        }

        if (!selectionFound)
        {
            if (toolBox.isFocused())
            {
                if (managerSize > 0)
                {
                    unsetSelectedCharacter();
                    setSelectedCharacter(managerArray[0]);
                    return;
                }
            }

            if (sidePanelSize > 0)
            {
                unsetSelectedCharacter();
                setSelectedCharacter(sideObjectPanels.get(0));
            }
        }
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

    public Program createEmptyProgram(int poseAnim, int walkAnim)
    {
        Color color = getRandomColor();
        ProgramComp comp = new ProgramComp(new WorldPoint[0], new WorldPoint[0], new LocalPoint[0], new LocalPoint[0], new Coordinate[0], false, 0, 1, DEFAULT_TURN_SPEED, poseAnim, walkAnim, MovementType.NORMAL, color.getRGB(), false, false);
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
        fileChooser.setFileFilter(new FileFilter()
        {
            @Override
            public String getDescription()
            {
                return "Json File (*.json)";
            }

            @Override
            public boolean accept(File f)
            {
                if (f.isDirectory())
                {
                    return true;
                }
                else
                {
                    String filename = f.getName().toLowerCase();
                    return filename.endsWith(".json");
                }
            }
        });

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
            e.printStackTrace();
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
                    model = plugin.createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle(), comp.getCustomLighting());
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
                case CACHE_MAN_WEAR:
                case CACHE_WOMAN_WEAR:
                    modelStats = comp.getModelStats();
                    model = plugin.constructModelFromCache(modelStats, null, false, false);
                    customModel = new CustomModel(model, comp);
                    break;
                case BLENDER:
                    model = modelImporter.createModel(comp.getBlenderModel(), comp.getLightingStyle());
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

        Thread thread = new Thread(() ->
        {
            for (CharacterSave save : characterSaves)
            {

                Color color = new Color(save.getProgramComp().getRgb());
                Program program = new Program(save.getProgramComp(), new JPanel(), new JLabel(), new JSpinner(), color);
                ObjectPanel objectPanel;

                if (customModels.length == 0)
                {
                    objectPanel = createPanel(sidePanel, save.getName(), save.getModelId(), null, save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance(), false);
                }
                else
                {
                    objectPanel = createPanel(sidePanel, save.getName(), save.getModelId(), customModels[save.getCompId()], save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance(), false);
                }

                SwingUtilities.invokeLater(() -> addPanel(sideObjectPanels, sidePanel, objectPanel));
            }
        });
        thread.start();

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
                objectPanel = createPanel(objectHolder, save.getName(), save.getModelId(), null, save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance(), false);
            }
            else
            {
                objectPanel = createPanel(objectHolder, save.getName(), save.getModelId(), customModels[save.getCompId()], save.isCustomMode(), save.isMinimized(), save.getRotation(), save.getAnimationId(), save.getRadius(), program, save.isActive(), save.getNonInstancedPoint(), save.getInstancedPoint(), save.getInstancedRegions(), save.getInstancedPlane(), save.isInInstance(), false);
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

        JPanel accessory = new JPanel();
        accessory.setLayout(new GridLayout(0, 1));
        accessory.add(priorityCheckbox);

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

            plugin.loadCustomModel(selectedFile, priorityCheckbox.isSelected(), name);
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