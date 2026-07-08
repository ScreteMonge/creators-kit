package com.creatorskit.swing;

import com.creatorskit.CKObject;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.hotkeymanager.LocationOption;
import com.creatorskit.programming.AnimationType;
import com.creatorskit.programming.orientation.Orientation;
import com.creatorskit.saves.CharacterSave;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.Character;
import com.creatorskit.saves.FolderNodeSave;
import com.creatorskit.saves.ModelKeyFrameSave;
import com.creatorskit.saves.SetupSave;
import com.creatorskit.models.*;
import com.creatorskit.selection.SelectionCommand;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.selection.SelectionOrigin;
import com.creatorskit.swing.anvil.ModelAnvil;
import com.creatorskit.swing.manager.Folder;
import com.creatorskit.swing.manager.FolderType;
import com.creatorskit.swing.manager.ManagerPanel;
import com.creatorskit.swing.manager.ManagerTree;
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
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class CreatorsPanel extends PluginPanel
{
    private ClientThread clientThread;
    private final Client client;
    private CreatorsConfig config;
    private final CreatorsPlugin plugin;
    private final ToolBoxFrame toolBox;
    private final ModelAnvil modelAnvil;
    private final ModelOrganizer modelOrganizer;
    private final DataFinder dataFinder;
    private final ModelImporter modelImporter;
    private final SelectionManager selectionManager;

    private final JButton addObjectButton = new JButton();
    private final JPanel sidePanel = new JPanel();
    private final Random random = new Random();

    public static final File SETUP_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/setups");
    public static final File CREATORS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    private File lastFileLoaded;

    private final Pattern pattern = Pattern.compile("\\(\\d+\\)\\Z");
    private int npcPanels = 0;
    private ArrayList<Character> sidePanelCharacters = new ArrayList<>();
    private final ArrayList<JComboBox<CustomModel>> comboBoxes = new ArrayList<>();
    private final BufferedImage SWITCH = ImageUtil.loadImageResource(getClass(), "/Switch.png");
    private final BufferedImage DUPLICATE = ImageUtil.loadImageResource(getClass(), "/Duplicate.png");
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");
    private final BufferedImage NEW = ImageUtil.loadImageResource(getClass(), "/New.png");
    private final BufferedImage FIND = ImageUtil.loadImageResource(getClass(), "/Find.png");
    private final BufferedImage CUSTOM_MODEL = ImageUtil.loadImageResource(getClass(), "/Custom model.png");
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");

    private final LineBorder defaultBorder = new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1);
    private final LineBorder hoveredBorder = new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1);
    private final LineBorder selectedBorder = new LineBorder(ColorScheme.BRAND_ORANGE, 1);
    private final LineBorder firstBorder = new LineBorder(Color.WHITE, 1);

    private boolean bulkEditing = false;

    @Inject
    public CreatorsPanel(@Nullable Client client, CreatorsConfig config, ClientThread clientThread, CreatorsPlugin plugin, ToolBoxFrame toolBox, DataFinder dataFinder, ModelImporter modelImporter, SelectionManager selectionManager)
    {
        this.clientThread = clientThread;
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        this.toolBox = toolBox;
        this.modelOrganizer = toolBox.getModelOrganizer();
        this.modelAnvil = toolBox.getModelAnvil();
        this.dataFinder = dataFinder;
        this.modelImporter = modelImporter;
        this.selectionManager = selectionManager;
        selectionManager.addListener((manager, origin) -> refreshSelectionBorders());

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
            SwingUtilities.invokeLater(() ->
            {
                toolBox.setVisible(!toolBox.isVisible());
                revalidate();
                repaint();
            });
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
            if (client == null)
            {
                return;
            }

            Character character = createCharacter(ParentPanel.SIDE_PANEL);
            SwingUtilities.invokeLater(() -> addPanel(ParentPanel.SIDE_PANEL, character, true, false, SelectionCommand.SELECT_ONLY));
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
        helpButton.setToolTipText("Open a YouTube tutorial for help with using this plugin");
        helpButton.setFocusable(false);
        add(helpButton, c);
        helpButton.addActionListener(e -> toolBox.openLink("https://www.youtube.com/playlist?list=PL5-mTiHdZKNgcEbhEdadHzX-F4VNE0G9O"));

        c.gridx = 1;
        c.gridy = 2;
        JButton loadCustomModelButton = new JButton(new ImageIcon(CUSTOM_MODEL));
        loadCustomModelButton.setFocusable(false);
        loadCustomModelButton.setToolTipText("Load a previously Saved Model");
        add(loadCustomModelButton, c);
        loadCustomModelButton.addActionListener(e -> openLoadCustomModelDialog());

        c.gridx = 2;
        c.gridy = 2;
        JButton newSetupButton = new JButton(new ImageIcon(NEW));
        newSetupButton.setFocusable(false);
        newSetupButton.setToolTipText("Create a new Setup file");
        add(newSetupButton, c);
        newSetupButton.addActionListener(e -> toolBox.createNewSetup(true, false));

        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 0;
        sidePanel.setLayout(new GridLayout(0, 1, 4, 4));
        sidePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(sidePanel, c);

        setKeyBindings();

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
                getRandomColor(),
                false, null, null, -1, false, false, LocationOption.TO_SAVED_LOCATION, new int[]{0, 0});
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
                              Color color,
                              boolean active,
                              WorldPoint worldPoint,
                              LocalPoint localPoint,
                              int plane,
                              boolean inPOH,
                              boolean transplant,
                              LocationOption locationOption,
                              int[] diff)
    {
        ObjectPanel objectPanel = new ObjectPanel();
        objectPanel.setLayout(new GridBagLayout());

        JTextField textField = new JTextField(name);
        Dimension textDimension = new Dimension(140, 30);
        textField.setMaximumSize(textDimension);
        textField.setPreferredSize(textDimension);
        textField.setMinimumSize(textDimension);

        final Border textFieldInnerBorder = textField.getBorder();
        textField.setBorder(buildNameFieldBorder(textFieldInnerBorder, color));

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
        duplicateButton.setToolTipText("Duplicate Object");
        duplicateButton.setFocusable(false);

        JButton deleteButton = new JButton(new ImageIcon(CLOSE));
        deleteButton.setToolTipText("Delete Object");
        deleteButton.setFocusable(false);

        //Buttons
        JButton modelButton = new JButton();
        modelButton.setFont(FontManager.getRunescapeFont());
        String modelButtonText = customModeActive ? "Custom" : "Id";
        modelButton.setText(modelButtonText);
        modelButton.setToolTipText("Toggle between Custom Model and Model ID");
        modelButton.setFocusable(false);

        JCheckBox spawnCheckBox = new JCheckBox();
        spawnCheckBox.setText("Spawn");
        spawnCheckBox.setToolTipText("Toggle the Object on or off");
        spawnCheckBox.setFocusable(false);

        JButton colourButton = new JButton();
        colourButton.setFont(FontManager.getRunescapeFont());
        colourButton.setText("Recolour");
        colourButton.setToolTipText("Rerolls the Object's colour overlays");
        colourButton.setPreferredSize(new Dimension(90, 25));
        colourButton.setFocusable(false);

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
            objectPanel.add(spawnCheckBox, c);

            c.gridy++;
            objectPanel.add(colourButton, c);

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

        Character character = new Character(
                textField.getText(),
                active,
                false,
                keyFrames,
                new KeyFrame[KeyFrameType.getTotalFrameTypes()],
                summary,
                null,
                null,
                color,
                worldPoint,
                localPoint,
                plane,
                inPOH,
                (CustomModel) modelComboBox.getSelectedItem(),
                parentPanel,
                objectPanel,
                customModeActive,
                textField,
                modelComboBox,
                spawnCheckBox,
                modelButton,
                modelSpinner,
                animationSpinner,
                animationFrameSpinner,
                orientationSpinner,
                radiusSpinner,
                new CKObject(client),
                null,
                null,
                0);

        textField.addActionListener(e -> onNameTextFieldChanged(character));

        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}

            @Override
            public void focusLost(FocusEvent e)
            {
                onNameTextFieldChanged(character);
            }
        });

        switchButton.addActionListener(e -> onSwitchButtonPressed((e.getModifiers() & ActionEvent.CTRL_MASK) != 0 && selectionManager.contains(character), character));

        deleteButton.addActionListener(e -> onDeleteButtonPressed((e.getModifiers() & ActionEvent.CTRL_MASK) != 0 && selectionManager.contains(character), character));

        duplicateButton.addActionListener(e ->
        {
            if ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0 && selectionManager.contains(character))
            {
                getPlugin().getHotKeyManager().onDuplicate(LocationOption.TO_SAVED_LOCATION);
                return;
            }

            onDuplicatePressed(character, LocationOption.TO_SAVED_LOCATION, new int[]{0, 0}, SelectionCommand.SELECT_ONLY);
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

        spawnCheckBox.addActionListener(e ->
        {
            character.toggleActive(clientThread);
            propagateActive(character, character.isActive());
        });

        colourButton.addActionListener(e -> showColorPickerFor(character, colourButton));

        modelButton.addActionListener(e ->
        {
            boolean newCustomMode = !character.isCustomMode();
            applyModelMode(character, newCustomMode, modelButton, modelSpinner, modelComboBox);
            propagateModelMode(character, newCustomMode);
        });

        modelSpinner.addChangeListener(e ->
        {
            int modelNumber = (int) modelSpinner.getValue();
            character.setToBaseModel(client, clientThread, false, modelNumber);
            propagateSpinner(character, modelNumber, Character::getModelSpinner);
        });

        modelComboBox.addActionListener(e ->
        {
            CustomModel m = (CustomModel) modelComboBox.getSelectedItem();
            character.setStoredModel(m);
            character.setToBaseModel(client, clientThread, true, -1);
            propagateModelComboBox(character, m);
        });

        orientationSpinner.addChangeListener(e ->
        {
            int orient = Orientation.boundOrientation((int) orientationSpinner.getValue());
            character.updateCKOOrientation(orient);
            propagateSpinner(character, orient, Character::getOrientationSpinner);
        });

        animationSpinner.addChangeListener(e ->
        {
            character.setAnimation(clientThread, client, plugin.getRandom(), AnimationType.ACTIVE, (int) animationSpinner.getValue(), (int) animationFrameSpinner.getValue(), config.randomizeStartFrame(), true);
            propagateSpinner(character, (int) animationSpinner.getValue(), Character::getAnimationSpinner);
        });

        animationFrameSpinner.addChangeListener(e ->
        {
            character.setAnimation(clientThread, client, plugin.getRandom(), AnimationType.ACTIVE, (int) animationSpinner.getValue(), (int) animationFrameSpinner.getValue(), config.randomizeStartFrame(), true);
            propagateSpinner(character, (int) animationFrameSpinner.getValue(), Character::getAnimationFrameSpinner);
        });

        radiusSpinner.addChangeListener(e ->
        {
            int rad = (int) radiusSpinner.getValue();
            character.setRadius(clientThread, rad);
            propagateSpinner(character, rad, Character::getRadiusSpinner);
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
                spawnCheckBox,
                colourButton,
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

        character.setupRLObject(client, clientThread, getToolBox().getProgrammer(), random, config.randomizeStartFrame(), locationOption, transplant, diff);
        plugin.getCharacters().add(character);

        comboBoxes.add(modelComboBox);
        return character;
    }

    private void addAllSelectListeners(
            Character character,
            JPanel objectPanel,
            JTextField textField,
            JPanel topButtonsPanel,
            JButton duplicateButton,
            JButton switchButton,
            JButton deleteButton,
            JButton modelButton,
            JCheckBox spawnCheckBox,
            JButton colourButton,
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
        addSelectListeners(switchButton, character, objectPanel, false);
        addSelectListeners(duplicateButton, character, objectPanel, false);
        addSelectListeners(deleteButton, character, objectPanel, false);
        addSelectListeners(modelButton, character, objectPanel, true);
        addSelectListeners(spawnCheckBox, character, objectPanel, true);
        addSelectListeners(colourButton, character, objectPanel, true);
        addSelectListeners(animationFrameSpinner, character, objectPanel, true);
        addSelectListeners(frameLabel, character, objectPanel, true);
        addSelectListeners(modelLabel, character, objectPanel, true);
        addSelectListeners(orientationLabel, character, objectPanel, true);
        addSelectListeners(radiusLabel, character, objectPanel, true);
        addSelectListeners(animationLabel, character, objectPanel, true);
        addSelectListeners(modelComboBox, character, objectPanel, true);

        for (Component c : modelComboBox.getComponents())
        {
            addSelectListeners(c, character, objectPanel, true);
        }

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

    private void addSelectListeners(Component component, Character character, JPanel objectPanel, boolean pressedListener)
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
                    onObjectPanelClicked(character, e);
                }
            });
        }
    }

    public void addPanel(ParentPanel parentPanel, Character character, boolean revalidate, boolean switching, SelectionCommand selectionCommand)
    {
        addPanel(parentPanel, character, null, revalidate, switching, selectionCommand);
    }

    public void addPanel(ParentPanel parentPanel, Character character, DefaultMutableTreeNode parentNode, boolean revalidate, boolean switching, SelectionCommand selectionCommand)
    {
        JPanel childPanel = character.getObjectPanel();
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

        if (selectionCommand == SelectionCommand.ADD)
        {
            selectionManager.add(character, SelectionOrigin.AUTOMATED);
        }
        else
        {
            selectionManager.select(character, SelectionOrigin.AUTOMATED);
        }
    }

    public void onSwitchButtonPressed(boolean propagate, Character character)
    {
        if (propagate)
        {
            Set<Character> characters = new HashSet<>(selectionManager.getSelected());
            for (Character c : characters)
            {
                switchCharacter(c);
            }

            return;
        }

        switchCharacter(character);
    }

    private void switchCharacter(Character character)
    {
        ParentPanel parentPanel = character.getParentPanel();
        removePanel(character);

        if (parentPanel == ParentPanel.SIDE_PANEL)
        {
            addPanel(ParentPanel.MANAGER, character, false, true, SelectionCommand.SELECT_ONLY);
        }
        else
        {
            addPanel(ParentPanel.SIDE_PANEL, character, false, true, SelectionCommand.SELECT_ONLY);
        }
    }

    public void onDuplicatePressed(Character character, LocationOption locationOption, int[] diff, SelectionCommand selectionCommand)
    {
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

        KeyFrameType[] keyFrameTypes = character.getSummary();
        KeyFrameType[] summary = new KeyFrameType[]{keyFrameTypes[0], keyFrameTypes[1], keyFrameTypes[2]};

        ParentPanel parentPanel = character.getParentPanel();

        String finalNewName = newName;
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
                duplicateKeyFrames(character),
                summary,
                getRandomColor(),
                character.isActive(),
                character.getNonInstancedPoint(),
                character.getInstancedPoint(),
                character.getInstancedPlane(),
                character.isInPOH(),
                true,
                locationOption,
                diff);

        SwingUtilities.invokeLater(() -> addPanel(parentPanel, c, true, false, selectionCommand));
    }

    private KeyFrame[][] duplicateKeyFrames(Character character)
    {
        KeyFrame[][] duplicatesArrays = new KeyFrame[KeyFrameType.getTotalFrameTypes()][];

        KeyFrame[][] originalArrays = character.getFrames();
        for (int i = 0; i < originalArrays.length; i++)
        {
            KeyFrame[] originalArray = originalArrays[i];
            if (originalArray == null || originalArray.length == 0)
            {
                duplicatesArrays[i] = new KeyFrame[0];
                continue;
            }

            KeyFrame[] duplicateArray = new KeyFrame[originalArray.length];
            for (int e = 0; e < originalArray.length; e++)
            {
                KeyFrame original = originalArray[e];
                KeyFrame duplicate = KeyFrame.createCopy(original, original.getTick());
                duplicateArray[e] = duplicate;
            }

            duplicatesArrays[i] = duplicateArray;
        }

        return duplicatesArrays;
    }

    public void onNameTextFieldChanged(Character character)
    {
        JTextField textField = character.getNameField();
        String text = StringHandler.cleanString(textField.getText());
        textField.setText(text);
        character.setName(text);

        DefaultMutableTreeNode node = character.getLinkedManagerNode();
        if (node == null)
        {
            return;
        }

        toolBox.getManagerPanel().getManagerTree().getTreeModel().nodeChanged(node);
        toolBox.getTimeSheetPanel().getAttributePanel().updateObjectLabel(character);

        toolBox.revalidate();
        toolBox.repaint();
    }

    public void onDeleteButtonPressed(boolean propagate, Character character)
    {
        if (propagate)
        {
            Character[] characters = selectionManager.getSelected().toArray(new Character[0]);
            deleteCharacters(characters);
            return;
        }

        deleteCharacters(new Character[]{character});
    }

    public void deleteCharacters(Character[] charactersToRemove)
    {
        removePanels(charactersToRemove);

        ArrayList<Character> characters = plugin.getCharacters();

        for (Character c : charactersToRemove)
        {
            clientThread.invokeLater(() ->
            {
                c.getCkObject().setActive(false);

                CKObject sp1 = c.getSpotAnim1();
                if (sp1 != null)
                {
                    sp1.setActive(false);
                }

                CKObject sp2 = c.getSpotAnim2();
                if (sp2 != null)
                {
                    sp2.setActive(false);
                }
            });
            characters.remove(c);

            selectionManager.remove(c, SelectionOrigin.AUTOMATED);
            toolBox.getTimeSheetPanel().unstackKeyFrameActions(c);
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
            JPanel objectPanel = character.getObjectPanel();
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
        managerTree.updateTreeSelectionIndex();
        managerTree.resetObjectHolder();
    }

    public void removePanel(Character character)
    {
        ManagerPanel managerPanel = toolBox.getManagerPanel();
        ManagerTree managerTree = managerPanel.getManagerTree();

        JPanel objectPanel = character.getObjectPanel();
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
        managerTree.updateTreeSelectionIndex();
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

    public void onObjectPanelClicked(Character character, MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON3)
        {
            if (e.isControlDown())
            {
                selectionManager.remove(character, SelectionOrigin.DIRECT);
                return;
            }

            selectionManager.clear(SelectionOrigin.DIRECT);
            return;
        }

        if (e.isShiftDown())
        {
            Character primary = selectionManager.getPrimary();
            if (primary == null)
            {
                return;
            }

            ManagerTree tree = toolBox.getManagerPanel().getManagerTree();
            Set<Character> characters = tree.getCharactersBetween(primary, character);
            selectionManager.selectAll(characters, SelectionOrigin.DIRECT);
            selectionManager.setPrimary(primary, SelectionOrigin.DIRECT);
            return;
        }

        if (e.isControlDown())
        {
            selectionManager.add(character, SelectionOrigin.DIRECT);
            return;
        }

        selectionManager.select(character, SelectionOrigin.DIRECT);
    }

    private static Border buildNameFieldBorder(Border inner, Color accent)
    {
        Color safe = accent == null ? ColorScheme.MEDIUM_GRAY_COLOR : accent;
        Border swatch = BorderFactory.createMatteBorder(0, 6, 0, 0, safe);
        return BorderFactory.createCompoundBorder(swatch, inner);
    }

    private static final Color[] COLOUR_PALETTE = new Color[]{
            new Color(255, 99, 99),    // red
            new Color(255, 165, 60),   // orange
            new Color(255, 220, 60),   // yellow
            new Color(80, 220, 100),   // green
            new Color(80, 160, 255),   // blue
            new Color(190, 110, 240),  // purple
    };

    private void showColorPickerFor(Character character, JButton anchor)
    {
        showColorPickerAt(anchor, 0, anchor.getHeight(), character);
    }

    public void showColorPickerAt(Component invoker, int x, int y, Character target)
    {
        JPopupMenu popup = new JPopupMenu();
        JPanel row = new JPanel(new GridLayout(1, 0, 2, 0));
        row.setBorder(new EmptyBorder(2, 2, 2, 2));

        Dimension swatchSize = new Dimension(22, 22);
        for (Color c : COLOUR_PALETTE)
        {
            JButton swatch = new JButton();
            swatch.setBackground(c);
            swatch.setPreferredSize(swatchSize);
            swatch.addActionListener(ev ->
            {
                applyColorToTargets(target, c);
                popup.setVisible(false);
            });
            row.add(swatch);
        }

        JButton random = new JButton("?");
        random.setPreferredSize(swatchSize);
        random.setToolTipText("Random colour");
        random.addActionListener(ev ->
        {
            applyColorToTargets(target, getRandomColor());
            popup.setVisible(false);
        });
        row.add(random);

        popup.add(row);
        popup.show(invoker, x, y);
    }

    private Set<Character> getBulkTargets(Character source)
    {
        if (selectionManager.getSelectionSize() > 1 && selectionManager.contains(source))
        {
            return selectionManager.getSelected();
        }
        return Collections.singleton(source);
    }

    private void propagateSpinner(Character source, Object value, Function<Character, JSpinner> getter)
    {
        if (bulkEditing)
        {
            return;
        }

        Set<Character> targets = getBulkTargets(source);
        if (targets.size() == 1)
        {
            return;
        }

        bulkEditing = true;
        for (Character c : targets)
        {
            if (c == source)
            {
                continue;
            }

            JSpinner s = getter.apply(c);
            if (!Objects.equals(s.getValue(), value))
            {
                s.setValue(value);
            }
        }

        bulkEditing = false;
    }

    private void applyModelMode(Character c, boolean customMode, JButton modelBtn, JSpinner modelSp, JComboBox<CustomModel> modelCb)
    {
        c.setCustomMode(customMode);
        modelBtn.setText(customMode ? "Custom" : "Id");
        modelSp.setVisible(!customMode);
        modelCb.setVisible(customMode);

        if (customMode)
        {
            c.setToBaseModel(client, clientThread, true, -1);
        }
        else
        {
            c.setToBaseModel(client, clientThread, false, (int) modelSp.getValue());
        }
    }

    private void propagateModelMode(Character source, boolean newCustomMode)
    {
        if (bulkEditing)
        {
            return;
        }

        Set<Character> targets = getBulkTargets(source);
        if (targets.size() == 1)
        {
            return;
        }

        bulkEditing = true;
        for (Character c : targets)
        {
            if (c == source || c.isCustomMode() == newCustomMode)
            {
                continue;
            }
            applyModelMode(c, newCustomMode, c.getModelButton(), c.getModelSpinner(), c.getComboBox());
        }
        bulkEditing = false;
    }

    private void propagateModelComboBox(Character source, CustomModel m)
    {
        if (bulkEditing)
        {
            return;
        }

        Set<Character> targets = getBulkTargets(source);
        if (targets.size() == 1)
        {
            return;
        }

        bulkEditing = true;
        for (Character c : targets)
        {
            if (c == source)
            {
                continue;
            }

            c.setStoredModel(m);
            JComboBox<CustomModel> cb = c.getComboBox();

            if (cb.getSelectedItem() != m)
            {
                bulkEditing = false;
                cb.setSelectedItem(m);
                bulkEditing = true;
            }

            if (c.isCustomMode())
            {
                c.setToBaseModel(client, clientThread, true, -1);
            }
        }

        bulkEditing = false;
    }

    private void propagateActive(Character source, boolean active)
    {
        if (bulkEditing)
        {
            return;
        }

        Set<Character> targets = getBulkTargets(source);
        if (targets.size() == 1)
        {
            return;
        }

        bulkEditing = true;
        for (Character c : targets)
        {
            if (c == source)
            {
                continue;
            }

            if (c.isActive() != active)
            {
                JCheckBox cb = c.getSpawnCheckBox();
                cb.setSelected(active);
                c.setActive(active, active, true, clientThread);
            }
        }

        bulkEditing = false;
    }

    public void applyColorToTargets(Character clicked, Color color)
    {
        Set<Character> selected = selectionManager.getSelected();
        if (selected.size() > 1 && selected.contains(clicked))
        {
            for (Character c : selected)
            {
                applyCharacterColor(c, color);
            }
        }
        else
        {
            applyCharacterColor(clicked, color);
        }
    }

    private void applyCharacterColor(Character c, Color color)
    {
        c.setColor(color);
        JTextField nameField = c.getNameField();
        CompoundBorder current = (CompoundBorder) nameField.getBorder();
        Border inner = current.getInsideBorder();
        nameField.setBorder(buildNameFieldBorder(inner, color));
    }

    public void refreshSelectionBorders()
    {
        ArrayList<Character> characters = plugin.getCharacters();
        Character hovered = plugin.getHoveredCharacter();
        Character first = selectionManager.getPrimary();

        for (int i = 0; i < characters.size(); i++)
        {
            Character c = characters.get(i);
            JPanel panel = c.getObjectPanel();

            if (c == first)
            {
                panel.setBackground(ColorScheme.DARK_GRAY_COLOR.brighter());
            }
            else
            {
                panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            }

            if (c == first)
            {
                panel.setBorder(firstBorder);
                continue;
            }

            if (selectionManager.contains(c))
            {
                panel.setBorder(selectedBorder);
                continue;
            }

            if (c == hovered)
            {
                panel.setBorder(hoveredBorder);
                continue;
            }

            panel.setBorder(defaultBorder);
        }
    }

    public void scrollSelectedCharacter(int clicks)
    {
        toolBox.getManagerPanel().
                getManagerTree().
                scrollSelectedIndex(clicks);
    }

    public void setHoveredCharacter(Character hovered, JPanel jPanel)
    {
        if (selectionManager.contains(hovered))
        {
            return;
        }

        jPanel.setBorder(hoveredBorder);
        plugin.setHoveredCharacter(hovered);
    }

    public void unsetHoveredCharacter(Character hoverRemoved, JPanel jPanel)
    {
        plugin.setHoveredCharacter(null);

        if (selectionManager.contains(hoverRemoved))
        {
            return;
        }

        jPanel.setBorder(defaultBorder);
    }

    public void addModelOptions(CustomModel[] models, boolean setComboBox)
    {
        modelOrganizer.addModels(models);
        Character selectedCharacter = selectionManager.getPrimary();

        JComboBox<CustomModel> modelAttributesBox = toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getModelAttributes()
                .getCustomModel();

        for (int i = 0; i < models.length; i++)
        {
            CustomModel model = models[i];
            modelAttributesBox.addItem(model);

            for (int e = 0; e < comboBoxes.size(); e++)
            {
                JComboBox<CustomModel> comboBox = comboBoxes.get(e);
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
                selectedCharacter.setToBaseModel(client, clientThread, true, -1);
            }
        }
    }

    public void removeModelOptions(CustomModel[] models)
    {
        JComboBox<CustomModel> modelAttributesBox = toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getModelAttributes()
                .getCustomModel();

        for (CustomModel model : models)
        {
            modelAttributesBox.removeItem(model);

            for (JComboBox<CustomModel> comboBox : comboBoxes)
            {
                comboBox.removeItem(model);
            }
        }

        modelOrganizer.removeModels(models);
    }

    public Color getRandomColor()
    {
        int max = 90;
        int min = 35;
        float r = (float) (random.nextInt(max - min + 1) + min) / 100;
        float g = (float) (random.nextInt(max - min + 1) + min) / 100;
        float b = (float) (random.nextInt(max - min + 1) + min) / 100;
        return new Color(r, g, b);
    }

    public void updateLoadedFile(File file)
    {
        lastFileLoaded = file;
        String fileName = "";
        if (file != null)
        {
            fileName = " - " + getFileName(lastFileLoaded);
        }

        toolBox.setTitle("Creator's Kit Toolbox" + fileName);
    }

    private String getFileName(File file)
    {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1)
        {
            return fileName.substring(0, lastDotIndex);
        }

        return fileName;
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

    public void quickSaveToFile()
    {
        if (lastFileLoaded == null)
        {
            SwingUtilities.invokeLater(this::openSaveDialog);
            return;
        }

        saveToFile(lastFileLoaded);
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

        SetupSave saveFile = new SetupSave(CreatorsPlugin.getPluginVersion(), comps, folderNodeSave, new CharacterSave[0]);

        try
        {
            FileWriter writer = new FileWriter(file, false);
            String string = plugin.getGson().toJson(saveFile);
            writer.write(string);
            writer.close();
            updateLoadedFile(file);
            LocalTime time = LocalTime.now();
            plugin.sendChatMessage("[" + time.getHour() + ":" + time.getMinute() + "] Saved successfully to: " + getFileName(file));
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

            if (storedModel == null)
            {
                saves[i] = new ModelKeyFrameSave(keyFrame.getTick(), false, keyFrame.getModelId(), 0, keyFrame.getRadius());
                continue;
            }

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

            saves[i] = new ModelKeyFrameSave(keyFrame.getTick(), keyFrame.isUseCustomModel(), keyFrame.getModelId(), compId, keyFrame.getRadius());
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

            keyFrames[i] = new ModelKeyFrame(save.getTick(), save.isUseCustomModel(), save.getModelId(), customModel, save.getRadius());
        }

        return keyFrames;
    }

    private CharacterSave createCharacterSave(Character character, CustomModelComp[] comps)
    {
        String name = character.getName();
        WorldPoint savedWorldPoint = character.getNonInstancedPoint();
        LocalPoint savedLocalPoint = character.getInstancedPoint();
        int localPointPlane = character.getInstancedPlane();
        boolean inPOH = character.isInPOH();
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
        int rgb = character.getColor().getRGB();
        ModelKeyFrameSave[] modelKeyFrameSaves = saveModelKeyFrames(character.getModelKeyFrames(), comps);

        SpotAnimKeyFrame[][] spotAnimKeyFrames = new SpotAnimKeyFrame[][]
                {
                        character.getSpotAnimKeyFrames(KeyFrameType.SPOTANIM),
                        character.getSpotAnimKeyFrames(KeyFrameType.SPOTANIM2)
                };

        HitsplatKeyFrame[][] hitsplatKeyFrames = new HitsplatKeyFrame[][]
                {
                        character.getHitsplatKeyFrames(KeyFrameType.HITSPLAT_1),
                        character.getHitsplatKeyFrames(KeyFrameType.HITSPLAT_2),
                        character.getHitsplatKeyFrames(KeyFrameType.HITSPLAT_3),
                        character.getHitsplatKeyFrames(KeyFrameType.HITSPLAT_4)
                };

        return new CharacterSave(
                name,
                savedWorldPoint,
                savedLocalPoint,
                localPointPlane,
                inPOH,
                compId,
                customMode,
                modelId,
                active,
                radius,
                rotation,
                animationId,
                frame,
                rgb,
                character.getMovementKeyFrames(),
                character.getAnimationKeyFrames(),
                character.getSpawnKeyFrames(),
                modelKeyFrameSaves,
                character.getOrientationKeyFrames(),
                character.getTextKeyFrames(),
                character.getOverheadKeyFrames(),
                character.getHealthKeyFrames(),
                spotAnimKeyFrames,
                hitsplatKeyFrames,
                character.getSummary());
    }

    public void openLoadSetupDialog(boolean updateLastLoadedFile)
    {
        SwingUtilities.invokeLater(() ->
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

            int option = fileChooser.showOpenDialog(null);
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
                    clientThread.invokeLater(() -> loadSetup(finalSelectedFile, saveFile, updateLastLoadedFile));
                    reader.close();
                    LocalTime time = LocalTime.now();
                    plugin.sendChatMessage("[" + time.getHour() + ":" + time.getMinute() + "] Loaded file: " + getFileName(finalSelectedFile));
                }
                catch (Exception e)
                {
                    plugin.sendChatMessage("An error occurred while attempting to read this file.");
                }
            }
        });
    }

    public void loadSetup(File file, boolean updateLastLoadedFile)
    {
        try
        {
            Reader reader = Files.newBufferedReader(file.toPath());
            SetupSave saveFile = plugin.getGson().fromJson(reader, SetupSave.class);
            clientThread.invokeLater(() -> loadSetup(file, saveFile, updateLastLoadedFile));
            reader.close();
            LocalTime time = LocalTime.now();
            plugin.sendChatMessage("[" + time.getHour() + ":" + time.getMinute() + "] Loaded file: " + getFileName(file));
        }
        catch (Exception e)
        {
            plugin.sendChatMessage("An error occurred while attempting to read this file.");
        }
    }

    private void loadSetup(File file, SetupSave saveFile, boolean updateLastLoadedFile)
    {
        ModelUtilities modelUtilities = toolBox.getModelUtilities();

        if (updateLastLoadedFile || lastFileLoaded == null)
        {
            updateLoadedFile(file);
        }

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
                    model = modelUtilities.createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getCustomLighting(), false);
                    customModel = new CustomModel(model, comp);
                    break;
                case CACHE_NPC:
                    modelStats = comp.getModelStats();
                    model = modelUtilities.constructModelFromCache(modelStats, new int[0], false, CustomLighting.fromLightingStyle(LightingStyle.ACTOR));
                    customModel = new CustomModel(model, comp);
                    break;
                case CACHE_PLAYER:
                    modelStats = comp.getModelStats();
                    model = modelUtilities.constructModelFromCache(modelStats, comp.getKitRecolours(), true, CustomLighting.fromLightingStyle(LightingStyle.ACTOR));
                    customModel = new CustomModel(model, comp);
                    break;
                default:
                case CACHE_OBJECT:
                case CACHE_SPOTANIM:
                case CACHE_GROUND_ITEM:
                case CACHE_MAN_WEAR:
                case CACHE_WOMAN_WEAR:
                    modelStats = comp.getModelStats();
                    model = modelUtilities.constructModelFromCache(modelStats, null, false, CustomLighting.fromLightingStyle(LightingStyle.DEFAULT));
                    customModel = new CustomModel(model, comp);
                    break;
                case BLENDER:
                    model = modelImporter.createModel(comp.getBlenderModel(), comp.getCustomLighting());
                    customModel = new CustomModel(model, comp);
            }

            customModels[i] = customModel;
        }

        modelUtilities.addCustomModels(customModels, false);

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
                    boolean resetAnimFrames = isVersionLessThan(version, "1.5.12");

                    for (CharacterSave save : characterSaves)
                    {
                        Character character;

                        CustomModel customModel = null;
                        if (customModels.length > 0)
                        {
                            customModel = customModels[save.getCompId()];
                        }

                        int animFrame = save.getFrame();
                        if (resetAnimFrames)
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
                                new KeyFrameType[]{KeyFrameType.MOVEMENT, KeyFrameType.ANIMATION, KeyFrameType.ORIENTATION},
                                new Color(save.getRgb()),
                                save.isActive(),
                                save.getNonInstancedPoint(),
                                save.getInstancedPoint(),
                                save.getInstancedPlane(),
                                save.isInInstance(),
                                false,
                                LocationOption.TO_SAVED_LOCATION,
                                new int[]{0, 0});

                        SwingUtilities.invokeLater(() -> addPanel(ParentPanel.SIDE_PANEL, character, true, false, SelectionCommand.SELECT_ONLY));
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
            node = managerTree.addFolderNode(parentNode, ParentPanel.MANAGER, name);
        }
        else
        {
            switch (folderType)
            {
                default:
                case STANDARD:
                    node = managerTree.addFolderNode(parentNode, ParentPanel.MANAGER, name);
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

        boolean resetAnimFrame = isVersionLessThan(fileVersion, "1.5.12");
        boolean resetTurnRate = isVersionLessThan(fileVersion, "2.0.1");

        for (CharacterSave save : folderNodeSave.getCharacterSaves())
        {
            Character character;
            CustomModel customModel = null;
            if (customModels.length > 0)
            {
                customModel = customModels[save.getCompId()];
            }

            int animFrame = save.getFrame();
            if (resetAnimFrame)
            {
                animFrame = -1;
            }

            KeyFrame[][] frames = new KeyFrame[KeyFrameType.getTotalFrameTypes()][];
            if (save.getMovementKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.MOVEMENT)] = save.getMovementKeyFrames();

                if (resetTurnRate)
                {
                    for (KeyFrame kf : frames[KeyFrameType.getIndex(KeyFrameType.MOVEMENT)])
                    {
                        MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
                        if (keyFrame.getTurnRate() == -1)
                        {
                            keyFrame.setTurnRate(OrientationKeyFrame.TURN_RATE);
                        }
                    }
                }
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

                if (resetTurnRate)
                {
                    for (KeyFrame kf : frames[KeyFrameType.getIndex(KeyFrameType.ORIENTATION)])
                    {
                        OrientationKeyFrame keyFrame = (OrientationKeyFrame) kf;
                        if (keyFrame.getTurnRate() == -1)
                        {
                            keyFrame.setTurnRate(OrientationKeyFrame.TURN_RATE);
                        }
                    }
                }
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

            SpotAnimKeyFrame[][] spotAnimKeyFrames = save.getSpotanimKeyFrames();
            if (save.getSpotanimKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.SPOTANIM)] = spotAnimKeyFrames[0];
                frames[KeyFrameType.getIndex(KeyFrameType.SPOTANIM2)] = spotAnimKeyFrames[1];
            }

            HitsplatKeyFrame[][] hitsplatKeyFrames = save.getHitsplatKeyFrames();
            if (hitsplatKeyFrames != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.HITSPLAT_1)] = hitsplatKeyFrames[0];
                frames[KeyFrameType.getIndex(KeyFrameType.HITSPLAT_2)] = hitsplatKeyFrames[1];
                frames[KeyFrameType.getIndex(KeyFrameType.HITSPLAT_3)] = hitsplatKeyFrames[2];
                frames[KeyFrameType.getIndex(KeyFrameType.HITSPLAT_4)] = hitsplatKeyFrames[3];
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
                    new Color(save.getRgb()),
                    save.isActive(),
                    save.getNonInstancedPoint(),
                    save.getInstancedPoint(),
                    save.getInstancedPlane(),
                    save.isInInstance(),
                    false,
                    LocationOption.TO_SAVED_LOCATION,
                    new int[]{0, 0});

            addPanel(parentPanel, character, node, false, false, SelectionCommand.SELECT_ONLY);
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
            toolBox.getModelUtilities().loadCustomModel(selectedFile);
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

    private void setKeyBindings()
    {
        ActionMap actionMap = getActionMap();
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "VK_S");
        actionMap.put("VK_S", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                quickSaveToFile();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "VK_O");
        actionMap.put("VK_O", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                toolBox.createNewSetup(false, true);
            }
        });
    }
}