package com.creatorskit.swing;

import com.creatorskit.CKObject;
import com.creatorskit.saves.CharacterSave;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.Character;
import com.creatorskit.saves.FolderNodeSave;
import com.creatorskit.saves.ModelKeyFrameSave;
import com.creatorskit.saves.SetupSave;
import com.creatorskit.models.*;
import com.creatorskit.programming.Coordinate;
import com.creatorskit.programming.MovementType;
import com.creatorskit.programming.Program;
import com.creatorskit.programming.ProgramComp;
import com.creatorskit.swing.manager.ManagerPanel;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class CreatorsPanel extends PluginPanel
{
    private ClientThread clientThread;
    private final Client client;
    private final CreatorsPlugin plugin;
    private final ToolBoxFrame toolBox;
    private final ModelAnvil modelAnvil;
    private final ModelOrganizer modelOrganizer;
    private final DataFinder dataFinder;
    private final ProgrammerPanel programmerPanel;
    private final TransmogPanel transmogPanel;
    private final ModelImporter modelImporter;

    private final JButton addObjectButton = new JButton();
    private final JPanel sidePanel = new JPanel();
    private final Random random = new Random();
    public static final File SETUP_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/setups");
    public static final File CREATORS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    private final Pattern pattern = Pattern.compile("\\(\\d+\\)\\Z");
    private int npcPanels = 0;
    private ArrayList<Character> sidePanelCharacters = new ArrayList<>();
    private final ArrayList<JComboBox<CustomModel>> comboBoxes = new ArrayList<>();
    private final Dimension spinnerSize = new Dimension(72, 30);
    private final Dimension BUTTON_SIZE = new Dimension(25, 25);
    private final int DEFAULT_TURN_SPEED = 40;
    private final BufferedImage SWITCH = ImageUtil.loadImageResource(getClass(), "/Switch.png");
    private final BufferedImage DUPLICATE = ImageUtil.loadImageResource(getClass(), "/Duplicate.png");
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");
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
    public CreatorsPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin, ToolBoxFrame toolBox, DataFinder dataFinder, ModelImporter modelImporter)
    {
        this.clientThread = clientThread;
        this.client = client;
        this.plugin = plugin;
        this.toolBox = toolBox;
        this.modelOrganizer = toolBox.getModelOrganizer();
        this.programmerPanel = toolBox.getProgramPanel();
        this.modelAnvil = toolBox.getModelAnvil();
        this.transmogPanel = toolBox.getTransmogPanel();
        this.dataFinder = dataFinder;
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
            Character character = createCharacter(ParentPanel.SIDE_PANEL);
            SwingUtilities.invokeLater(() -> addPanel(ParentPanel.SIDE_PANEL, character, true, false));
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
        JButton helpButton = new JButton(new ImageIcon(HELP));
        helpButton.setToolTipText("Open at YouTube tutorial for help with using this plugin");
        helpButton.setFocusable(false);
        add(helpButton, c);
        helpButton.addActionListener(e -> toolBox.openLink("https://www.youtube.com/watch?v=E_9c-LwDRRY"));

        c.gridx = 1;
        c.gridy = 2;
        JButton loadCustomModelButton = new JButton(new ImageIcon(CUSTOM_MODEL));
        loadCustomModelButton.setFocusable(false);
        loadCustomModelButton.setToolTipText("Load a previously Saved Model");
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
        sidePanel.setLayout(new GridLayout(0, 1, 4, 4));
        sidePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(sidePanel, c);

        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 1;
        c.weighty = 1;
        JLabel emptyLabel = new JLabel("");
        add(emptyLabel, c);
    }

    public Character createCharacter(ParentPanel parentPanel)
    {
        return createCharacter(
                parentPanel,
                "Object (" + npcPanels + ")",
                7699,
                null,
                false,
                0,
                -1,
                -1,
                60,
                new KeyFrame[KeyFrameType.getTotalFrameTypes()][0],
                KeyFrameType.createDefaultSummary(),
                createEmptyProgram(-1, -1),
                false, null, null, new int[0], -1, false, false);
    }

    public Character createCharacter(
                              ParentPanel parentPanel,
                              String name,
                              int modelId,
                              CustomModel customModel,
                              boolean customModeActive,
                              int orientation,
                              int animationId,
                              int frame,
                              int radius,
                              KeyFrame[][] keyFrames,
                              KeyFrameType[] summary,
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
        ObjectPanel objectPanel = new ObjectPanel(name, null, programPanel);
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

        JButton animationButton = new JButton();
        animationButton.setFont(FontManager.getRunescapeFont());
        animationButton.setText("Anim Off");
        animationButton.setToolTipText("Toggle the playing animation");
        animationButton.setPreferredSize(new Dimension(90, 25));
        animationButton.setFocusable(false);

        JPanel framePanel = new JPanel();
        framePanel.setLayout(new BorderLayout());

        JLabel frameLabel = new JLabel(" Frame: ");
        frameLabel.setToolTipText("The animation frame to freeze on");
        frameLabel.setFont(FontManager.getRunescapeSmallFont());
        framePanel.add(frameLabel, BorderLayout.LINE_START);

        JSpinner animationFrameSpinner = new JSpinner(new SpinnerNumberModel(frame, -1, 999, 1));
        animationFrameSpinner.setPreferredSize(new Dimension(60, 25));
        framePanel.add(animationFrameSpinner, BorderLayout.CENTER);

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
            objectPanel.add(animationButton, c);

            c.gridy++;
            objectPanel.add(framePanel, c);

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

            objectPanel.repaint();
            objectPanel.revalidate();
        });

        JLabel programmerNameLabel = program.getNameLabel();
        programmerNameLabel.setText(name);
        JSpinner programmerIdleSpinner = program.getIdleAnimSpinner();

        Character character = new Character(
                textField.getText(),
                active,
                worldPoint != null || localPoint != null,
                keyFrames,
                new KeyFrame[KeyFrameType.getTotalFrameTypes()],
                summary,
                null,
                null,
                program,
                worldPoint,
                localPoint,
                localPointRegion,
                localPointPlane,
                inInstance,
                (CustomModel) modelComboBox.getSelectedItem(),
                parentPanel,
                objectPanel,
                customModeActive,
                textField,
                modelComboBox,
                spawnButton,
                modelButton,
                modelSpinner,
                animationSpinner,
                animationFrameSpinner,
                orientationSpinner,
                radiusSpinner,
                programmerNameLabel,
                programmerIdleSpinner,
                new CKObject(client),
                null,
                null,
                0);

        objectPanel.setCharacter(character);
        ManagerPanel managerPanel = toolBox.getManagerPanel();

        SwingUtilities.invokeLater(() -> programmerPanel.createProgramPanel(character, programPanel, programmerNameLabel, programmerIdleSpinner));

        textField.addActionListener(e ->
        {
            String text = StringHandler.cleanString(textField.getText());
            textField.setText(text);
            character.setName(text);
            objectPanel.setName(text);
            character.getProgram().getNameLabel().setText(text);
            managerPanel.revalidate();
            managerPanel.repaint();
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
                managerPanel.revalidate();
                managerPanel.repaint();
            }
        });

        switchButton.addActionListener(e -> onSwitchButtonPressed(character));

        deleteButton.addActionListener(e -> onDeleteButtonPressed(character));

        duplicateButton.addActionListener(e -> onDuplicatePressed(character, false));

        objectPanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                setSelectedCharacter(character);
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

        spawnButton.addActionListener(e -> plugin.toggleSpawn(spawnButton, character));

        animationButton.addActionListener(e ->
        {
            int animId = character.getCkObject().getAnimationId();
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
            plugin.setAnimationFrame(character, (int) animationFrameSpinner.getValue(), true);
            programmerIdleSpinner.setValue(animationNumber);
        });

        animationFrameSpinner.addChangeListener(e ->
        {
            int animFrame = (int) animationFrameSpinner.getValue();
            plugin.setAnimationFrame(character, animFrame, true);
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
                topButtonsPanel,
                duplicateButton,
                switchButton,
                deleteButton,
                modelButton,
                spawnButton,
                animationButton,
                animationFrameSpinner,
                frameLabel,
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

        plugin.setupRLObject(character, setHoveredLocation);
        plugin.getCharacters().add(character);

        comboBoxes.add(modelComboBox);
        return character;
    }

    private void addAllSelectListeners(
            Character character,
            ObjectPanel objectPanel,
            JTextField textField,
            JPanel topButtonsPanel,
            JButton duplicateButton,
            JButton switchButton,
            JButton deleteButton,
            JButton modelButton,
            JButton spawnButton,
            JButton animationButton,
            JSpinner animationFrameSpinner,
            JLabel frameLabel,
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
        addSelectListeners(topButtonsPanel, character, objectPanel, true);
        addSelectListeners(switchButton, character, objectPanel, true);
        addSelectListeners(duplicateButton, character, objectPanel, false);
        addSelectListeners(deleteButton, character, objectPanel, false);
        addSelectListeners(modelButton, character, objectPanel, true);
        addSelectListeners(spawnButton, character, objectPanel, true);
        addSelectListeners(animationButton, character, objectPanel, true);
        addSelectListeners(animationFrameSpinner, character, objectPanel, true);
        addSelectListeners(frameLabel, character, objectPanel, true);
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
                    setSelectedCharacter(character);
                }
            });
        }
    }

    public void addPanel(ParentPanel parentPanel, Character character, boolean revalidate, boolean switching)
    {
        addPanel(parentPanel, character, null, revalidate, switching);
    }

    public void addPanel(ParentPanel parentPanel, Character character, DefaultMutableTreeNode parentNode, boolean revalidate, boolean switching)
    {
        ObjectPanel childPanel = character.getObjectPanel();
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        ManagerTree managerTree = managerPanel.getManagerTree();

        if (parentPanel == ParentPanel.SIDE_PANEL)
        {
            sidePanelCharacters.add(character);
            sidePanel.add(childPanel);
            if (parentNode == null)
            {
                managerTree.addCharacterNode(character, ParentPanel.SIDE_PANEL, true, switching);
            }
            else
            {
                managerTree.addCharacterNode(parentNode, character, ParentPanel.SIDE_PANEL, true);
            }

            if (revalidate)
            {
                sidePanel.repaint();
                sidePanel.revalidate();
            }
        }

        if (parentPanel == ParentPanel.MANAGER)
        {
            managerPanel.getManagerCharacters().add(character);

            if (parentNode == null)
            {
                managerTree.addCharacterNode(character, ParentPanel.MANAGER, true, switching);
            }
            else
            {
                managerTree.addCharacterNode(parentNode, character, ParentPanel.MANAGER, true);
            }

            if (revalidate)
            {
                managerTree.resetObjectHolder();
            }
        }

        if (!switching)
        {
            npcPanels++;
        }

        setSelectedCharacter(character);
    }

    public void onSwitchButtonPressed(Character character)
    {
        ParentPanel parentPanel = character.getParentPanel();
        removePanel(character);

        if (parentPanel == ParentPanel.SIDE_PANEL)
        {
            addPanel(ParentPanel.MANAGER, character, true, true);
        }
        else
        {
            addPanel(ParentPanel.SIDE_PANEL, character, true, true);
        }
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

        KeyFrameType[] keyFrameTypes = character.getSummary();
        KeyFrameType[] summary = new KeyFrameType[]{keyFrameTypes[0], keyFrameTypes[1], keyFrameTypes[2]};

        ParentPanel parentPanel = character.getParentPanel();

        String finalNewName = newName;
        Thread thread = new Thread(() ->
        {
            Character c = createCharacter(
                    character.getParentPanel(),
                    finalNewName,
                    (int) character.getModelSpinner().getValue(),
                    (CustomModel) character.getComboBox().getSelectedItem(),
                    character.isCustomMode(),
                    (int) character.getOrientationSpinner().getValue(),
                    (int) character.getAnimationSpinner().getValue(),
                    (int) character.getAnimationFrameSpinner().getValue(),
                    (int) character.getRadiusSpinner().getValue(),
                    Arrays.copyOf(character.getFrames(), character.getFrames().length),
                    summary,
                    newProgram,
                    character.isActive(),
                    character.getNonInstancedPoint(),
                    character.getInstancedPoint(),
                    character.getInstancedRegions(),
                    character.getInstancedPlane(),
                    character.isInInstance(),
                    setLocation);

            SwingUtilities.invokeLater(() -> addPanel(parentPanel, c, true, false));
        });
        thread.start();
    }

    public void onDeleteButtonPressed(Character character)
    {
        deleteCharacters(new Character[]{character});
    }

    public void deleteCharacters(Character[] charactersToRemove)
    {
        removePanels(charactersToRemove);
        TimeSheetPanel timeSheetPanel = toolBox.getTimeSheetPanel();

        ArrayList<Character> characters = plugin.getCharacters();
        Character selectedCharacter = plugin.getSelectedCharacter();
        Character tspSelectedCharacter = timeSheetPanel.getSelectedCharacter();

        for (Character c : charactersToRemove)
        {
            clientThread.invokeLater(() -> c.getCkObject().setActive(false));
            characters.remove(c);
            if (c == selectedCharacter)
            {
                plugin.setSelectedCharacter(null);
            }

            if (c == tspSelectedCharacter)
            {
                timeSheetPanel.setSelectedCharacter(null);
            }
        }
    }

    public void removePanels(Character[] characters)
    {
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        JPanel objectHolder = managerPanel.getObjectHolder();
        ArrayList<Character> managerCharacters = managerPanel.getManagerCharacters();
        ManagerTree managerTree = managerPanel.getManagerTree();

        for (Character character : characters)
        {
            ObjectPanel objectPanel = character.getObjectPanel();
            ParentPanel parentPanel = character.getParentPanel();
            if (parentPanel == ParentPanel.SIDE_PANEL)
            {
                sidePanel.remove(objectPanel);
                sidePanelCharacters.remove(character);
                managerTree.removeCharacterNode(character);
            }

            if (parentPanel == ParentPanel.MANAGER)
            {
                objectHolder.remove(objectPanel);
                managerTree.removeCharacterNode(character);
                managerCharacters.remove(character);
            }
        }

        sidePanel.repaint();
        sidePanel.revalidate();
        objectHolder.repaint();
        objectHolder.revalidate();
        managerTree.resetObjectHolder();
    }

    public void removePanel(Character character)
    {
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        ManagerTree managerTree = managerPanel.getManagerTree();

        ObjectPanel objectPanel = character.getObjectPanel();
        ParentPanel parentPanel = character.getParentPanel();
        if (parentPanel == ParentPanel.SIDE_PANEL)
        {
            sidePanel.remove(objectPanel);
            managerTree.resetObjectHolder();
            sidePanelCharacters.remove(character);
            managerTree.removeCharacterNode(character);
        }

        JPanel objectHolder = managerPanel.getObjectHolder();
        if (parentPanel == ParentPanel.MANAGER)
        {
            objectHolder.remove(objectPanel);
            managerTree.removeCharacterNode(character);
            managerTree.resetObjectHolder();
            managerPanel.getManagerCharacters().remove(character);
        }

        sidePanel.repaint();
        sidePanel.revalidate();
        objectHolder.repaint();
        objectHolder.revalidate();
    }

    public void clearSidePanels(boolean warning)
    {
        if (warning)
        {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all Objects from the Side Panel?");
            if (result != JOptionPane.YES_OPTION)
                return;
        }

        for (Character character : sidePanelCharacters)
        {
            if (character == plugin.getSelectedCharacter())
            {
                unsetSelectedCharacter();
                break;
            }
        }

        Character[] charactersToRemove = sidePanelCharacters.toArray(new Character[sidePanelCharacters.size()]);

        sidePanelCharacters.clear();
        Thread thread = new Thread(() -> deleteCharacters(charactersToRemove));
        thread.start();
        sidePanel.removeAll();
        sidePanel.repaint();
        sidePanel.revalidate();
        programmerPanel.repaint();
        programmerPanel.revalidate();
    }

    public void clearManagerPanels()
    {
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        JPanel objectHolder = managerPanel.getObjectHolder();
        ArrayList<Character> managerCharacters = managerPanel.getManagerCharacters();

        for (Character character : managerCharacters)
        {
            if (character == plugin.getSelectedCharacter())
            {
                unsetSelectedCharacter();
                break;
            }
        }

        Character[] charactersToRemove = managerCharacters.toArray(new Character[managerCharacters.size()]);

        objectHolder.removeAll();
        managerPanel.getManagerTree().resetObjectHolder();
        Thread thread = new Thread(() -> deleteCharacters(charactersToRemove));
        thread.start();
        programmerPanel.repaint();
        programmerPanel.revalidate();
    }

    public void resetSidePanel()
    {
        sidePanel.removeAll();
        ArrayList<Character> characters = new ArrayList<>();
        toolBox.getManagerPanel().getManagerTree().getSidePanelChildren(characters);
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            sidePanel.add(character.getObjectPanel());
        }

        sidePanelCharacters = characters;

        sidePanel.repaint();
        sidePanel.revalidate();
    }

    public void setSelectedCharacter(Character selected)
    {
        setSelectedCharacter(selected, true);
    }

    public void setSelectedCharacter(Character selected, boolean updateManagerTree)
    {
        ArrayList<Character> characters = plugin.getCharacters();

        for (int i = 0; i < characters.size(); i++)
        {
            ObjectPanel panel = characters.get(i).getObjectPanel();
            panel.setBorder(defaultBorder);
        }

        plugin.setSelectedCharacter(selected);

        if (updateManagerTree)
        {
            toolBox.getManagerPanel().getManagerTree().setTreeSelection(selected);
        }

        if (selected != null)
        {
            selected.getObjectPanel().setBorder(selectedBorder);
        }
    }

    public void scrollSelectedCharacter(int clicks)
    {
        toolBox.getManagerPanel().
                getManagerTree().
                scrollSelectedIndex(clicks);
    }

    public void unsetSelectedCharacter()
    {
        ArrayList<Character> characters = plugin.getCharacters();

        for (int i = 0; i < characters.size(); i++)
        {
            ObjectPanel panel = characters.get(i).getObjectPanel();
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
        Character selectedCharacter = plugin.getSelectedCharacter();

        toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getModelAttributes()
                .getCustomModel()
                .addItem(model);

        for (JComboBox<CustomModel> comboBox : comboBoxes)
        {
            comboBox.addItem(model);
            if (!setComboBox || selectedCharacter == null)
            {
                continue;
            }

            JComboBox<CustomModel> selectedBox = selectedCharacter.getComboBox();
            if (comboBox == selectedBox)
            {
                comboBox.setSelectedItem(model);
                selectedCharacter.setCustomMode(true);
                selectedCharacter.getModelButton().setText("Custom");

                if (selectedCharacter.getModelSpinner().isVisible() || comboBox.isVisible())
                {
                    comboBox.setVisible(true);
                    selectedCharacter.getModelSpinner().setVisible(false);
                }
            }

            selectedCharacter.setStoredModel(model);
            plugin.setModel(selectedCharacter, true, -1);
        }
    }

    public void removeModelOption(CustomModel model)
    {
        toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getModelAttributes()
                .getCustomModel()
                .removeItem(model);

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
        ArrayList<CustomModel> customModels = plugin.getStoredModels();
        CustomModelComp[] comps = new CustomModelComp[customModels.size()];

        for (int i = 0; i < comps.length; i++)
        {
            comps[i] = customModels.get(i).getComp();
        }

        //Get Folder structure and all characters contained within
        FolderNodeSave folderNodeSave = getFolders(comps);

        SetupSave saveFile = new SetupSave(getPluginVersion(), comps, folderNodeSave, new CharacterSave[0]);

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
        FolderNodeSave folderNodeSave = new FolderNodeSave(FolderType.MASTER, "Master Panel", new CharacterSave[0], new FolderNodeSave[0]);
        getFolderChildren(folderNodeSave, toolBox.getManagerPanel().getManagerTree().getRootNode(), comps);
        return folderNodeSave;
    }

    public void getFolderChildren(FolderNodeSave parentNodeSave, DefaultMutableTreeNode parent, CustomModelComp[] comps)
    {
        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            CharacterSave[] characterSaves = parentNodeSave.getCharacterSaves();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            if (node.getUserObject() instanceof Character)
            {
                Character character = (Character) node.getUserObject();
                CharacterSave characterSave = createCharacterSave(character, comps);
                parentNodeSave.setCharacterSaves(ArrayUtils.add(characterSaves, characterSave));
            }

            if (node.getUserObject() instanceof Folder)
            {
                Folder folder = (Folder) node.getUserObject();
                String name = (folder.getName());
                FolderNodeSave folderNodeSave = new FolderNodeSave(folder.getFolderType(), name, new CharacterSave[0], new FolderNodeSave[0]);
                parentNodeSave.setFolderSaves(ArrayUtils.add(parentNodeSave.getFolderSaves(), folderNodeSave));

                if (!node.isLeaf())
                    getFolderChildren(folderNodeSave, node, comps);
            }
        }
    }

    private ModelKeyFrameSave[] saveModelKeyFrames(ModelKeyFrame[] keyFrames, CustomModelComp[] comps)
    {
        if (keyFrames == null)
        {
            return null;
        }

        ModelKeyFrameSave[] saves = new ModelKeyFrameSave[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            ModelKeyFrame keyFrame = keyFrames[i];
            CustomModel storedModel = keyFrame.getCustomModel();
            int compId = 0;

            for (int e = 0; e < comps.length; e++)
            {
                CustomModelComp comp = comps[e];
                if (storedModel.getComp() == comp)
                {
                    compId = e;
                    break;
                }
            }

            saves[i] = new ModelKeyFrameSave(keyFrame.getTick(), keyFrame.isUseCustomModel(), keyFrame.getModelId(), compId);
        }

        return saves;
    }

    private ModelKeyFrame[] loadModelKeyFrames(ModelKeyFrameSave[] saves, CustomModel[] customModels)
    {
        ModelKeyFrame[] keyFrames = new ModelKeyFrame[saves.length];

        for (int i = 0; i < saves.length; i++)
        {
            ModelKeyFrameSave save = saves[i];
            CustomModel customModel = null;
            if (customModels.length > 0)
            {
                customModel = customModels[save.getCustomModel()];
            }

            keyFrames[i] = new ModelKeyFrame(save.getTick(), save.isUseCustomModel(), save.getModelId(), customModel);
        }

        return keyFrames;
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
        int modelId = (int) character.getModelSpinner().getValue();
        boolean active = character.isActive();
        int radius = character.getCkObject().getRadius();
        int rotation = (int) character.getOrientationSpinner().getValue();
        int animationId = (int) character.getAnimationSpinner().getValue();
        int frame = (int) character.getAnimationFrameSpinner().getValue();
        ProgramComp programComp = character.getProgram().getComp();
        ModelKeyFrameSave[] modelKeyFrameSaves = saveModelKeyFrames(character.getModelKeyFrames(), comps);

        return new CharacterSave(
                name,
                locationSet,
                savedWorldPoint,
                savedLocalPoint,
                localPointRegion,
                localPointPlane,
                inInstance,
                compId,
                customMode,
                modelId,
                active,
                radius,
                rotation,
                animationId,
                frame,
                programComp,
                character.getMovementKeyFrames(),
                character.getAnimationKeyFrames(),
                character.getSpawnKeyFrames(),
                modelKeyFrameSaves,
                character.getOrientationKeyFrames(),
                character.getTextKeyFrames(),
                character.getOverheadKeyFrames(),
                character.getHealthKeyFrames(),
                character.getSpotAnimKeyFrames(KeyFrameType.SPOTANIM),
                character.getSpotAnimKeyFrames(KeyFrameType.SPOTANIM2),
                character.getSummary());
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
            plugin.sendChatMessage("An error occurred while attempting to read this file.");
        }
    }

    private void loadSetup(File file, SetupSave saveFile)
    {
        CustomModelComp[] comps = saveFile.getComps();
        FolderNodeSave folderNodeSave = saveFile.getMasterFolderNode();
        CustomModel[] customModels = new CustomModel[comps.length];
        String fileVersion = saveFile.getVersion();
        if (fileVersion == null || fileVersion.isEmpty())
        {
            fileVersion = "1.5.0";
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
                    model = plugin.createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle(), comp.getCustomLighting());
                    customModel = new CustomModel(model, comp);
                    break;
                case CACHE_NPC:
                    modelStats = comp.getModelStats();
                    model = plugin.constructModelFromCache(modelStats, new int[0], false, LightingStyle.ACTOR, null);
                    customModel = new CustomModel(model, comp);
                    break;
                case CACHE_PLAYER:
                    modelStats = comp.getModelStats();
                    model = plugin.constructModelFromCache(modelStats, comp.getKitRecolours(), true, LightingStyle.ACTOR, null);
                    customModel = new CustomModel(model, comp);
                    break;
                default:
                case CACHE_OBJECT:
                case CACHE_SPOTANIM:
                case CACHE_GROUND_ITEM:
                case CACHE_MAN_WEAR:
                case CACHE_WOMAN_WEAR:
                    modelStats = comp.getModelStats();
                    model = plugin.constructModelFromCache(modelStats, null, false, LightingStyle.DEFAULT, null);
                    customModel = new CustomModel(model, comp);
                    break;
                case BLENDER:
                    model = modelImporter.createModel(comp.getBlenderModel(), comp.getLightingStyle());
                    customModel = new CustomModel(model, comp);
            }

            plugin.addCustomModel(customModel, false);
            customModels[i] = customModel;
        }

        ManagerTree managerTree = toolBox.getManagerPanel().getManagerTree();
        DefaultMutableTreeNode rootNode = managerTree.getRootNode();

        final String version = fileVersion;

        SwingUtilities.invokeLater(() ->
        {
            if (folderNodeSave != null)
            {
                openFolderNodeSave(version, managerTree, rootNode, folderNodeSave, customModels);
                toolBox.repaint();
                toolBox.revalidate();
            }
        });

        if (isVersionLessThan(fileVersion, "1.5.4"))
        {
            CharacterSave[] characterSaves = saveFile.getSaves();
            if (characterSaves.length > 0)
            {
                Thread thread = new Thread(() ->
                {
                    for (CharacterSave save : characterSaves)
                    {
                        Color color = new Color(save.getProgramComp().getRgb());
                        Program program = new Program(save.getProgramComp(), new JPanel(), new JLabel(), new JSpinner(), color);
                        Character character;

                        CustomModel customModel = null;
                        if (customModels.length > 0)
                        {
                            customModel = customModels[save.getCompId()];
                        }

                        int animFrame = save.getFrame();
                        if (isVersionLessThan(version, "1.5.12"))
                        {
                            animFrame = -1;
                        }

                        KeyFrame[][] frames = new KeyFrame[KeyFrameType.getTotalFrameTypes()][];

                        character = createCharacter(
                                ParentPanel.SIDE_PANEL,
                                save.getName(),
                                save.getModelId(),
                                customModel,
                                save.isCustomMode(),
                                save.getRotation(),
                                save.getAnimationId(),
                                animFrame,
                                save.getRadius(),
                                frames,
                                save.getSummary(),
                                program,
                                save.isActive(),
                                save.getNonInstancedPoint(),
                                save.getInstancedPoint(),
                                save.getInstancedRegions(),
                                save.getInstancedPlane(),
                                save.isInInstance(),
                                false);

                        SwingUtilities.invokeLater(() -> addPanel(ParentPanel.SIDE_PANEL, character, true, false));
                    }
                });
                thread.start();
            }
        }
    }

    private void openFolderNodeSave(String fileVersion, ManagerTree managerTree, DefaultMutableTreeNode parentNode, FolderNodeSave folderNodeSave, CustomModel[] customModels)
    {
        String name = folderNodeSave.getName();
        DefaultMutableTreeNode node;
        FolderType folderType = folderNodeSave.getFolderType();

        if (isVersionLessThan(fileVersion, "1.5.4"))
        {
            node = managerTree.addFolderNode(parentNode, name);
        }
        else
        {
            switch (folderType)
            {
                default:
                case STANDARD:
                    node = managerTree.addFolderNode(parentNode, name);
                    break;
                case MASTER:
                case MANAGER:
                    node = managerTree.getManagerNode();
                    break;
                case SIDE_PANEL:
                    node = managerTree.getSidePanelNode();
            }
        }

        ParentPanel parentPanel;
        if (folderType == FolderType.SIDE_PANEL)
        {
            parentPanel = ParentPanel.SIDE_PANEL;
        }
        else
        {
            parentPanel = ParentPanel.MANAGER;
        }

        for (CharacterSave save : folderNodeSave.getCharacterSaves())
        {
            Color color = new Color(save.getProgramComp().getRgb());
            Program program = new Program(save.getProgramComp(), new JPanel(), new JLabel(), new JSpinner(), color);
            Character character;
            CustomModel customModel = null;
            if (customModels.length > 0)
            {
                customModel = customModels[save.getCompId()];
            }

            int animFrame = save.getFrame();
            if (isVersionLessThan(fileVersion, "1.5.12"))
            {
                animFrame = -1;
            }

            KeyFrame[][] frames = new KeyFrame[KeyFrameType.getTotalFrameTypes()][];
            if (save.getMovementKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.MOVEMENT)] = save.getMovementKeyFrames();
            }

            if (save.getAnimationKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.ANIMATION)] = save.getAnimationKeyFrames();
            }

            if (save.getSpawnKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.SPAWN)] = save.getSpawnKeyFrames();
            }

            ModelKeyFrameSave[] modelKeyFrameSaves = save.getModelKeyFrameSaves();
            if (modelKeyFrameSaves != null)
            {
                ModelKeyFrame[] modelKeyFrames = loadModelKeyFrames(modelKeyFrameSaves, customModels);
                frames[KeyFrameType.getIndex(KeyFrameType.MODEL)] = modelKeyFrames;
            }

            if (save.getOrientationKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.ORIENTATION)] = save.getOrientationKeyFrames();
            }

            if (save.getTextKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.TEXT)] = save.getTextKeyFrames();
            }

            if (save.getOverheadKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.OVERHEAD)] = save.getOverheadKeyFrames();
            }

            if (save.getHealthKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.HEALTH)] = save.getHealthKeyFrames();
            }

            if (save.getSpotAnimKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.SPOTANIM)] = save.getSpotAnimKeyFrames();
            }

            if (save.getSpotAnim2KeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.SPOTANIM2)] = save.getSpotAnim2KeyFrames();
            }

            KeyFrameType[] summary;
            if (save.getSummary() == null)
            {
                summary = KeyFrameType.createDefaultSummary();
            }
            else
            {
                summary = save.getSummary();
            }

            character = createCharacter(
                    parentPanel,
                    save.getName(),
                    save.getModelId(),
                    customModel,
                    save.isCustomMode(),
                    save.getRotation(),
                    save.getAnimationId(),
                    animFrame,
                    save.getRadius(),
                    frames,
                    summary,
                    program,
                    save.isActive(),
                    save.getNonInstancedPoint(),
                    save.getInstancedPoint(),
                    save.getInstancedRegions(),
                    save.getInstancedPlane(),
                    save.isInInstance(),
                    false);

            addPanel(parentPanel, character, node, false, false);
        }

        FolderNodeSave[] folderNodeSaves = folderNodeSave.getFolderSaves();
        for (FolderNodeSave fns : folderNodeSaves)
        {
            openFolderNodeSave(fileVersion, managerTree, node, fns, customModels);
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
            plugin.loadCustomModel(selectedFile);
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

    private String getPluginVersion()
    {
        try (InputStream is = CreatorsPlugin.class.getResourceAsStream("/version.txt"))
        {
            if (is == null)
            {
                return "0.0.0";
            }

            String text = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).readLine();
            String version = text.split("=")[1];
            is.close();
            return version;
        }
        catch (IOException e)
        {
            return "0.0.0";
        }
    }

    private boolean isVersionLessThan(String version1, String version2)
    {
        String[] split1 = version1.split("\\.");
        int first1 = Integer.parseInt(split1[0]);
        int second1 = Integer.parseInt(split1[1]);
        int third1 = Integer.parseInt(split1[2]);

        String[] split2 = version2.split("\\.");
        int first2 = Integer.parseInt(split2[0]);
        int second2 = Integer.parseInt(split2[1]);
        int third2 = Integer.parseInt(split2[2]);

        if (first1 < first2)
        {
            return true;
        }

        if (first1 > first2)
        {
            return false;
        }

        if (second1 < second2)
        {
            return true;
        }

        if (second1 > second2)
        {
            return false;
        }

        if (third1 < third2)
        {
            return true;
        }

        if (third1 > third2)
        {
            return false;
        }

        return false;
    }
}