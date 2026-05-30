package com.creatorskit.swing;

import com.creatorskit.CKObject;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.programming.AnimationType;
import com.creatorskit.saves.CharacterSave;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.Character;
import com.creatorskit.ActiveOption;
import com.creatorskit.LocationOption;
import com.creatorskit.saves.FolderNodeSave;
import com.creatorskit.saves.ModelKeyFrameSave;
import com.creatorskit.saves.SetupSave;
import com.creatorskit.models.*;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.swing.anvil.ModelAnvil;
import com.creatorskit.swing.manager.Folder;
import com.creatorskit.swing.manager.FolderType;
import com.creatorskit.swing.manager.ManagerPanel;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.WorldView;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.*;
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
    private boolean bulkEditing = false;

    private final JButton addObjectButton = new JButton();
    private final JPanel sidePanel = new JPanel();
    private final Random random = new Random();

    public static final File SETUP_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/setups");
    public static final File SETUP_VERSIONS_DIR = new File(SETUP_DIR, ".versions");
    private static final java.time.format.DateTimeFormatter VERSION_STAMP =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    public static final File CREATORS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    public File lastFileLoaded;

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
    private final LineBorder hoveredBorder = new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 2);
    private final LineBorder selectedBorder = new LineBorder(Color.WHITE, 2);
    private final JLabel stepSpeedLabel = new JLabel("Step speed: 1.0");

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
        selectionManager.addListener(mgr -> refreshSelectionBorders());

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
        newSetupButton.addActionListener(e ->
        {
            Thread thread = new Thread(() -> toolBox.getManagerPanel().getManagerTree().removeAllNodes());
            thread.start();
        });

        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 1;
        c.weighty = 0;
        JButton deselectButton = new JButton("Deselect All");
        deselectButton.setFocusable(false);
        deselectButton.setToolTipText("Clear the current Character selection");
        deselectButton.addActionListener(e -> selectionManager.clear());
        add(deselectButton, c);

        // Global Unlock Camera button. Sits right under Deselect All so it's visible
        // and one click away when the user wants to escape the lock without having to
        // hunt down whichever Character has it in the manager tree. Delegates to the
        // plugin (passing null releases via the same code path manual toggle uses).
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy++;
        JButton unlockCameraButton = new JButton("Unlock Camera");
        unlockCameraButton.setFocusable(false);
        unlockCameraButton.setToolTipText("Release the camera lock on whichever Character currently has it. "
                + "Restores the camera mode and Oculus Orb state that were active before the lock engaged.");
        unlockCameraButton.addActionListener(e -> plugin.setCameraLockedCharacter(null));
        add(unlockCameraButton, c);

        // Live indicator of the Add-Program-Step speed; updated when the user holds
        // the hotkey and scrolls the mouse wheel.
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy++;
        stepSpeedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        stepSpeedLabel.setToolTipText("<html>Speed for new movement steps. Hold the Add Program Step hotkey and scroll to adjust.</html>");
        add(stepSpeedLabel, c);

        // Switched from hardcoded gridy values to c.gridy++ so subsequent additions
        // (e.g. Unlock Camera between Deselect All and stepSpeedLabel) don't collide
        // with rows below. Earlier hardcoded gridy=5 for sidePanel overlapped with
        // stepSpeedLabel once the Unlock button pushed everything down a row.
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        sidePanel.setLayout(new GridLayout(0, 1, 4, 4));
        sidePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(sidePanel, c);

        setKeyBindings();

        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        JLabel emptyLabel = new JLabel("");
        add(emptyLabel, c);
    }

    public void updateStepSpeedLabel(double speed)
    {
        stepSpeedLabel.setText(String.format("Step speed: %.1f", speed));
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
                65,
                new KeyFrame[KeyFrameType.getTotalFrameTypes()][0],
                KeyFrameType.createDefaultSummary(),
                getRandomColor(),
                false, null, null, -1, false, false, false);
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
                              boolean setHoveredLocation)
    {
        JPanel objectPanel = new JPanel();
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

        JCheckBox renderFixCheckBox = new JCheckBox();
        renderFixCheckBox.setText("Render fix");
        renderFixCheckBox.setToolTipText("Toggle on if the cache model's animation looks broken.");
        renderFixCheckBox.setFocusable(false);

        JCheckBox cameraLockCheckBox = new JCheckBox();
        cameraLockCheckBox.setText("Camera lock");
        cameraLockCheckBox.setToolTipText("Lock the camera to this Character. Only one Character at a time.");
        cameraLockCheckBox.setFocusable(false);

        JButton colourButton = new JButton();
        colourButton.setFont(FontManager.getRunescapeFont());
        colourButton.setText("Recolour");
        colourButton.setToolTipText("Rerolls the Object's colour overlays");
        colourButton.setPreferredSize(new Dimension(90, 25));
        colourButton.setFocusable(false);
        colourButton.setForeground(color);

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
            objectPanel.add(renderFixCheckBox, c);

            c.gridy++;
            objectPanel.add(cameraLockCheckBox, c);

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
                renderFixCheckBox,
                cameraLockCheckBox,
                modelButton,
                null,
                modelSpinner,
                animationSpinner,
                animationFrameSpinner,
                orientationSpinner,
                radiusSpinner,
                new CKObject(client),
                null,
                null,
                0,
                false,
                128,
                128,
                0,
                0,
                0,
                1.0,
                new java.util.ArrayList<>());

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

        switchButton.addActionListener(e -> onSwitchButtonPressed(character));

        deleteButton.addActionListener(e -> onDeleteButtonPressed(character));

        duplicateButton.addActionListener(e -> onDuplicatePressed(character, false));

        objectPanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                onCharacterPanelClicked(character, e);
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

        spawnCheckBox.addActionListener(e ->
        {
            character.toggleActive(clientThread);
            propagateActive(character, character.isActive());
        });

        // Mirror the checkbox into the Character's renderFix flag. The setter pushes the
        // value down to the live CKObject so the renderer picks it up immediately on the
        // next frame -- no rebuild needed.
        renderFixCheckBox.addActionListener(e -> character.setRenderFix(renderFixCheckBox.isSelected()));

        // Camera lock toggles via the plugin so the mutual-exclusion (only one Character
        // locked at a time) is centrally enforced and the manager-tree right-click menu
        // for any other Character can see the current locked state.
        cameraLockCheckBox.addActionListener(e -> plugin.setCameraLockedCharacter(character));

        character.setColourButton(colourButton);
        colourButton.addActionListener(e -> showColorPickerFor(character, colourButton));

        modelButton.addActionListener(e ->
        {
            boolean newCustomMode = !character.isCustomMode();
            applyModelMode(character, newCustomMode, modelButton, modelSpinner, modelComboBox);
            if (bulkEditing)
            {
                return;
            }
            java.util.Set<Character> targets = getBulkTargets(character);
            if (targets.size() == 1)
            {
                return;
            }
            bulkEditing = true;
            try
            {
                for (Character c : targets)
                {
                    if (c == character || c.isCustomMode() == newCustomMode)
                    {
                        continue;
                    }
                    applyModelMode(c, newCustomMode, c.getModelButton(), c.getModelSpinner(), c.getComboBox());
                }
            }
            finally
            {
                bulkEditing = false;
            }
        });

        modelSpinner.addChangeListener(e ->
        {
            int modelNumber = (int) modelSpinner.getValue();
            plugin.setModel(character, false, modelNumber);
            propagateSpinner(character, modelNumber, Character::getModelSpinner);
        });

        modelComboBox.addItemListener(e ->
        {
            CustomModel m = (CustomModel) modelComboBox.getSelectedItem();
            character.setStoredModel(m);
            if (modelComboBox.isVisible() && character == plugin.getSelectedCharacter())
                plugin.setModel(character, true, -1);
            propagateCustomModel(character, m);
        });

        orientationSpinner.addChangeListener(e ->
        {
            int orient = (int) orientationSpinner.getValue();
            plugin.setOrientation(character, orient);
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
            plugin.setRadius(character, rad);
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

        plugin.setupRLObject(character, transplant, setHoveredLocation);
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
        addSelectListeners(switchButton, character, objectPanel, true);
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
                    onCharacterPanelClicked(character, e);
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
                    duplicateKeyFrames(character),
                    summary,
                    getRandomColor(),
                    character.isActive(),
                    character.getNonInstancedPoint(),
                    character.getInstancedPoint(),
                    character.getInstancedPlane(),
                    character.isInPOH(),
                    true,
                    setLocation);

            // Copy the Alt-driven visual state: WASD/R/F sub-tile nudges and the
            // Alt+Scroll uniform extra scale. The render-fix toggle + per-axis scales
            // come along too, since they're locked to the underlying model rather
            // than the keyframe data the createCharacter call already handles.
            c.setRenderFixWidth(character.getRenderFixWidth());
            c.setRenderFixHeight(character.getRenderFixHeight());
            c.setRenderFix(character.isRenderFix());
            c.setOffsetX(character.getOffsetX());
            c.setOffsetY(character.getOffsetY());
            c.setOffsetZ(character.getOffsetZ());
            c.setExtraScale(character.getExtraScale());

            SwingUtilities.invokeLater(() -> addPanel(parentPanel, c, true, false));
        });
        thread.start();
    }

    private KeyFrame[][] duplicateKeyFrames(Character character)
    {
        return duplicateKeyFrames(character, 0.0, 0.0);
    }

    /**
     * Variant of {@link #duplicateKeyFrames(Character)} that lets the caller
     * shift each duplicated keyframe's tick by {@code timeOffset}, with an
     * opt-in floor: keyframes at tick &lt; {@code shiftThreshold} stay at
     * their original tick. Used by the Random Hazard Grid so each stamp can
     * advance its "effect" keyframes by the step tick while preserving an
     * earlier baseline (e.g. a tick-0 spawn-Disable to keep the Character
     * invisible until the effect fires).
     *
     * <p>When called with {@code timeOffset == 0} this is a no-op offset
     * and matches the original behaviour exactly -- callers that don't
     * need the shift go through the zero-arg overload.
     */
    private KeyFrame[][] duplicateKeyFrames(Character character, double timeOffset, double shiftThreshold)
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
                double newTick = original.getTick() >= shiftThreshold
                        ? original.getTick() + timeOffset
                        : original.getTick();
                KeyFrame duplicate = KeyFrame.createCopy(original, newTick);
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
            // Drop any Colour-kf snapshot we may be holding -- the controller's
            // IdentityHashMap entry would otherwise pin the Character and its
            // Model references after the visual is gone.
            toolBox.getProgrammer().getColourController().release(c);

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

                // Projectile CKObjects are lazily allocated per target inside
                // Programmer.ensureProjectileSlot and stored on the Character.
                // Without this loop they'd stay registered with the scene after
                // the owning Character is gone, leaving an orphaned spinning
                // model wherever the last projectile frame landed.
                for (CKObject proj : c.getProjectileObjects())
                {
                    if (proj != null && proj.isActive())
                    {
                        proj.setActive(false);
                    }
                }
            });
            characters.remove(c);
            if (c == selectedCharacter)
            {
                plugin.setSelectedCharacter(null);
            }

            if (c == tspSelectedCharacter)
            {
                timeSheetPanel.setSelectedCharacter(null);
            }

            toolBox.getTimeSheetPanel().removeKeyFrameActions(c);
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
        plugin.setSelectedCharacter(selected);

        if (updateManagerTree)
        {
            toolBox.getManagerPanel().getManagerTree().setTreeSelection(selected);
        }
    }

    /**
     * Modifier-aware click router for Character panels.
     * Plain click   = replace selection. Ctrl+Click = toggle. Shift+Click = range.
     * If the Character is already part of an active multi-selection, a plain click
     * preserves the selection (so clicks on inner buttons don't collapse the group).
     */
    public void onCharacterPanelClicked(Character character, MouseEvent e)
    {
        if (character == null)
        {
            return;
        }

        if (e.isShiftDown())
        {
            extendSelectionTo(character);
            return;
        }

        if (e.isControlDown())
        {
            selectionManager.toggle(character);
            toolBox.getManagerPanel().getManagerTree().syncTreeFromSelection();
            return;
        }

        if (selectionManager.size() > 1 && selectionManager.isSelected(character))
        {
            return;
        }

        setSelectedCharacter(character);
    }

    private void extendSelectionTo(Character character)
    {
        ArrayList<Character> all = plugin.getCharacters();
        Character primary = selectionManager.getPrimary();
        int target = all.indexOf(character);
        if (target < 0)
        {
            setSelectedCharacter(character);
            return;
        }
        int anchor = primary == null ? target : all.indexOf(primary);
        if (anchor < 0)
        {
            anchor = target;
        }
        int start = Math.min(anchor, target);
        int end = Math.max(anchor, target);
        ArrayList<Character> range = new ArrayList<>(end - start + 1);
        for (int i = start; i <= end; i++)
        {
            range.add(all.get(i));
        }
        selectionManager.selectAll(range);
        toolBox.getManagerPanel().getManagerTree().syncTreeFromSelection();
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

    /**
     * Tools &gt; Layout &gt; Fill Rectangle. With exactly 2 Characters selected as
     * opposite corners on the same plane / instance, duplicates them across
     * every tile in the bounding rectangle (alternating checkerboard by tile
     * parity). All new Characters land in a fresh folder named "Fill: A &amp; B"
     * so the user can collapse / select / delete them as a group.
     *
     * <p>Stride parameter lets the user skip tiles for sparse fills (every
     * Nth tile in each direction). Stride=1 fills every tile; stride=2
     * gives a quarter-density "every other" pattern, etc.
     *
     * <p>Use case: rain on a 10x10 area -- place a raindrop on the NW corner,
     * a different (or identical) raindrop on the SE corner, select both, run
     * Fill Rectangle. Now there's a raindrop on every tile in the area, all
     * grouped in one folder ready for Tools &gt; Random &gt; Scatter.
     */
    /**
     * Undo stack for spatial "layout" operations (Rotate, multi-move) that the
     * keyframe undo system doesn't cover. Each entry is a Runnable that fully
     * reverts one operation (restores every affected Character's position +
     * orientation). LIFO; capped so it can't grow unbounded. Driven by CTRL+Z
     * when the toolbox is on a non-timesheet tab (the timesheet tab keeps its
     * own keyframe undo).
     */
    private final java.util.Deque<Runnable> layoutUndoStack = new java.util.ArrayDeque<>();
    private final Object layoutUndoLock = new Object();
    private static final int LAYOUT_UNDO_LIMIT = 25;

    /** The open modeless Rotate dialog, or null. Tracked so we don't stack duplicates. */
    private JDialog rotateDialog;

    private void pushLayoutUndo(Runnable revert)
    {
        if (revert == null)
        {
            return;
        }
        // Pushed from the EDT (Rotate dialog) AND the client thread (CTRL+click
        // multi-move), so guard the deque -- ArrayDeque isn't thread-safe.
        synchronized (layoutUndoLock)
        {
            layoutUndoStack.push(revert);
            while (layoutUndoStack.size() > LAYOUT_UNDO_LIMIT)
            {
                layoutUndoStack.removeLast();
            }
        }
    }

    /** True when there's a layout operation that CTRL+Z can revert. */
    public boolean hasLayoutUndo()
    {
        synchronized (layoutUndoLock)
        {
            return !layoutUndoStack.isEmpty();
        }
    }

    /** Reverts the most recent layout operation (Rotate / multi-move), if any. */
    public void undoLayout()
    {
        Runnable revert;
        synchronized (layoutUndoLock)
        {
            revert = layoutUndoStack.poll();
        }
        // Run the revert OUTSIDE the lock -- it touches the client thread and
        // shouldn't hold the undo lock while doing engine work.
        if (revert != null)
        {
            revert.run();
        }
    }

    /**
     * Public hook so spatial ops outside this class (e.g. the plugin's
     * multi-Character CTRL+click move) can register a single-CTRL+Z revert on
     * the shared layout-undo stack.
     */
    public void recordLayoutUndo(Runnable revert)
    {
        pushLayoutUndo(revert);
    }

    /**
     * Confirmation dialog for the Rotate layout tool. Names the pivot (the
     * primary / last-clicked selection) so there's no ambiguity about the
     * centre of rotation, and lets the user pick direction + angle before
     * committing. Delegates to {@link #rotateSelectionAroundPivot(int)} with
     * the clockwise-equivalent angle.
     */
    /**
     * Opens the Rotate tool as a MODELESS dialog so the user can still click in
     * the Manager / scene while it's up. The group to rotate is snapshotted at
     * open time (so picking the pivot can't shrink it); the pivot starts as
     * "None" and OK stays disabled until the user explicitly clicks one of the
     * group's Characters to designate it. Fixes the old flaw where the pivot
     * was silently the last-clicked selection.
     */
    public void showRotateDialog()
    {
        java.util.Collection<Character> current = selectionManager.getSelected();
        if (current.size() < 2)
        {
            JOptionPane.showMessageDialog(this,
                    "Select at least 2 Characters first -- the group you want to rotate.\n"
                            + "You'll pick which one is the pivot inside the dialog.",
                    "Rotate", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (rotateDialog != null && rotateDialog.isShowing())
        {
            rotateDialog.toFront();
            return;
        }

        // Snapshot the group NOW. The user designates the pivot by clicking a
        // Character, which replaces the live selection -- but we rotate this
        // fixed snapshot, so picking the pivot never shrinks the group.
        final java.util.List<Character> rotateSet = new java.util.ArrayList<>(current);

        Window owner = SwingUtilities.getWindowAncestor(this);
        final JDialog dialog = new JDialog(owner, "Rotate selection", Dialog.ModalityType.MODELESS);
        rotateDialog = dialog;

        final Character[] pivot = new Character[]{null};

        JComboBox<String> directionBox = new JComboBox<>(new String[]{"Clockwise", "Counter-clockwise"});
        JComboBox<String> angleBox = new JComboBox<>(new String[]{"90°", "180°", "270°"});
        JLabel pivotValue = new JLabel("None");
        pivotValue.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        JButton okButton = new JButton("Rotate");
        okButton.setEnabled(false);
        JButton cancelButton = new JButton("Cancel");

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Group to rotate:"));
        form.add(new JLabel(rotateSet.size() + " Characters"));
        form.add(new JLabel("Pivot (stays put):"));
        form.add(pivotValue);
        form.add(new JLabel("Direction:"));
        form.add(directionBox);
        form.add(new JLabel("Angle:"));
        form.add(angleBox);

        JLabel hint = new JLabel("<html><body style='width:240px'>Click one of the selected Characters (in the Manager,"
                + " or right-click &rarr; Select in the scene) to set it as the pivot.</body></html>");
        hint.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okButton);
        buttons.add(cancelButton);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        content.add(hint, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(content);

        // While open, adopt any clicked group member as the pivot. Clicks that
        // land outside the group are ignored (pivot unchanged), so the user
        // can't accidentally pick a Character that isn't part of the rotation.
        final SelectionManager.SelectionListener listener = mgr ->
        {
            Character p = mgr.getPrimary();
            if (p != null && rotateSet.contains(p))
            {
                pivot[0] = p;
                pivotValue.setText(p.getName());
                pivotValue.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
                okButton.setEnabled(true);
            }
        };
        selectionManager.addListener(listener);

        Runnable cleanup = () ->
        {
            selectionManager.removeListener(listener);
            if (rotateDialog == dialog)
            {
                rotateDialog = null;
            }
            dialog.dispose();
        };

        okButton.addActionListener(e ->
        {
            Character chosenPivot = pivot[0];
            if (chosenPivot == null)
            {
                return;
            }
            int angle = (angleBox.getSelectedIndex() + 1) * 90; // 90 / 180 / 270
            boolean clockwise = directionBox.getSelectedIndex() == 0;
            // The core rotates clockwise; a counter-clockwise turn equals the
            // complementary clockwise one (CCW 90 == CW 270, etc.; 180 mirrors).
            int cwDegrees = clockwise ? angle : (360 - angle) % 360;
            if (cwDegrees == 0)
            {
                cwDegrees = 180; // defensive; angle is always 90/180/270
            }
            cleanup.run();
            rotateSelectionAroundPivot(rotateSet, chosenPivot, cwDegrees);
            // Re-select the group so a follow-up rotate starts from the same set.
            selectionManager.selectAll(rotateSet);
        });

        cancelButton.addActionListener(e -> cleanup.run());
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                cleanup.run();
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * Rotates {@code rotateSet} around {@code pivot} by {@code degrees} (90 /
     * 180 / 270, clockwise viewed top-down). The pivot is the centre of
     * rotation and stays put; every other Character in the set has BOTH its
     * tile orbited around the pivot AND its facing turned by the same angle, so
     * the formation rotates as a rigid body.
     *
     * <p>All Characters in the set must share the pivot's instance context (all
     * overworld, or all inside a POH/instance) and plane -- the coordinate
     * systems don't line up otherwise (same constraint as Fill Rectangle).
     * 90/180/270 keep every tile on the integer grid, so there's no rounding.
     */
    private void rotateSelectionAroundPivot(java.util.List<Character> rotateSet, Character pivot, int degrees)
    {
        if (rotateSet == null || rotateSet.size() < 2 || pivot == null)
        {
            return;
        }

        boolean inPOH = pivot.isInPOH();

        // Pivot anchor coords + plane, in this context's native units:
        // overworld = WorldPoint tile units; POH = LocalPoint 1/128 units.
        // Rotating in native units keeps sub-tile POH placement intact and
        // is identical math either way.
        int px, py, plane;
        if (inPOH)
        {
            LocalPoint plp = pivot.getInstancedPoint();
            if (plp == null)
            {
                JOptionPane.showMessageDialog(this, "Couldn't read the pivot's instance position.", "Rotate", JOptionPane.WARNING_MESSAGE);
                return;
            }
            px = plp.getX();
            py = plp.getY();
            plane = pivot.getInstancedPlane();
        }
        else
        {
            WorldPoint pwp = pivot.getNonInstancedPoint();
            if (pwp == null)
            {
                JOptionPane.showMessageDialog(this, "Couldn't read the pivot's world position.", "Rotate", JOptionPane.WARNING_MESSAGE);
                return;
            }
            px = pwp.getX();
            py = pwp.getY();
            plane = pwp.getPlane();
        }

        // Uniform-context guard (mirrors Fill Rectangle): every Character in the
        // group must share the pivot's POH/overworld state and plane.
        for (Character c : rotateSet)
        {
            if (c.isInPOH() != inPOH)
            {
                JOptionPane.showMessageDialog(this,
                        "All selected Characters must share the pivot's instance state\n(all overworld, or all inside a POH/instance).",
                        "Rotate", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int cPlane;
            if (inPOH)
            {
                cPlane = c.getInstancedPlane();
            }
            else
            {
                WorldPoint cwp = c.getNonInstancedPoint();
                cPlane = cwp != null ? cwp.getPlane() : plane;
            }
            if (cPlane != plane)
            {
                JOptionPane.showMessageDialog(this,
                        "All selected Characters must be on the pivot's plane.",
                        "Rotate", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // JAU increases clockwise (north-up): S=0, W=512, N=1024, E=1536, and
        // 90 deg = 512 JAU. So a clockwise position rotation pairs with +512
        // per 90 deg of facing, keeping each Character pointing the same way
        // relative to the rotated formation.
        int oriDelta = 512 * (degrees / 90);
        WorldView wv = client.getTopLevelWorldView();

        // Per-Character revert closures captured BEFORE mutation. WorldPoint /
        // LocalPoint are immutable, so the references we grab here stay valid
        // snapshots after we overwrite the fields. Bundled into one undo entry
        // so a single CTRL+Z reverses the whole rotate.
        java.util.List<Runnable> reverts = new java.util.ArrayList<>();

        int moved = 0;
        for (Character c : rotateSet)
        {
            if (c == pivot)
            {
                continue;
            }

            int x, y;
            if (inPOH)
            {
                LocalPoint lp = c.getInstancedPoint();
                if (lp == null) continue;
                x = lp.getX();
                y = lp.getY();
            }
            else
            {
                WorldPoint wp = c.getNonInstancedPoint();
                if (wp == null) continue;
                x = wp.getX();
                y = wp.getY();
            }

            // Snapshot pre-rotation state for undo.
            final Character fc = c;
            final WorldPoint oldWp = inPOH ? null : c.getNonInstancedPoint();
            final LocalPoint oldLp = inPOH ? c.getInstancedPoint() : null;
            final int oldPlane = c.getInstancedPlane();
            final int prevOri = ((Number) c.getOrientationSpinner().getValue()).intValue();
            final boolean fInPOH = inPOH;
            reverts.add(() ->
            {
                if (fInPOH)
                {
                    fc.setInstancedPoint(oldLp);
                    fc.setInstancedPlane(oldPlane);
                }
                else
                {
                    fc.setNonInstancedPoint(oldWp);
                }
                plugin.setOrientation(fc, prevOri);
                plugin.setLocation(fc, false, false, ActiveOption.UNCHANGED, LocationOption.TO_SAVED_LOCATION);
            });

            // Rotate (dx, dy) about the pivot. OSRS +x = east, +y = north;
            // 90 deg CW sends east -> south, i.e. (dx,dy) -> (dy,-dx).
            int dx = x - px;
            int dy = y - py;
            int ndx, ndy;
            switch (degrees)
            {
                case 90:  ndx =  dy; ndy = -dx; break;
                case 180: ndx = -dx; ndy = -dy; break;
                default:  ndx = -dy; ndy =  dx; break; // 270
            }
            int nx = px + ndx;
            int ny = py + ndy;

            if (inPOH)
            {
                c.setInstancedPoint(new LocalPoint(nx, ny, wv));
            }
            else
            {
                c.setNonInstancedPoint(new WorldPoint(nx, ny, plane));
            }

            int newOri = (((prevOri + oriDelta) % 2048) + 2048) % 2048;
            plugin.setOrientation(c, newOri);

            // Re-render at the just-updated saved point (no active-state change).
            plugin.setLocation(c, false, false, ActiveOption.UNCHANGED, LocationOption.TO_SAVED_LOCATION);
            moved++;
        }

        if (!reverts.isEmpty())
        {
            final int undoneCount = reverts.size();
            pushLayoutUndo(() ->
            {
                for (Runnable r : reverts)
                {
                    r.run();
                }
                plugin.sendChatMessage("Undid rotate (" + undoneCount + " Character" + (undoneCount == 1 ? "" : "s") + " restored).");
            });
        }

        plugin.sendChatMessage("Rotated " + moved + " Character" + (moved == 1 ? "" : "s")
                + " " + degrees + "° around pivot '" + pivot.getName() + "'. (CTRL+Z to undo)");
    }

    public void showFillRectangleDialog()
    {
        java.util.Collection<Character> selected = selectionManager.getSelected();
        if (selected.size() != 2)
        {
            JOptionPane.showMessageDialog(this,
                    "Fill Rectangle requires exactly 2 Characters selected as opposite corners. Currently " + selected.size() + " selected.",
                    "Fill Rectangle", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.Iterator<Character> it = selected.iterator();
        Character cornerA = it.next();
        Character cornerB = it.next();

        // Both corners need to share the instance context -- you can't fill
        // between a POH tile and an overworld tile, the coordinate systems don't
        // line up. Same plane is required for the same reason (a Z-traversing
        // fill would need a totally different semantic).
        if (cornerA.isInPOH() != cornerB.isInPOH())
        {
            JOptionPane.showMessageDialog(this,
                    "Both corner Characters must be in the same instance state (both inside POH/instance or both on the overworld).",
                    "Fill Rectangle", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cornerA.getInstancedPlane() != cornerB.getInstancedPlane())
        {
            JOptionPane.showMessageDialog(this,
                    "Both corner Characters must be on the same plane.",
                    "Fill Rectangle", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean inPOH = cornerA.isInPOH();
        // Plane source depends on the placement context:
        //   - POH/instance characters: instancedPlane is set by setLocationPOH.
        //   - Overworld characters: instancedPlane stays at the constructor
        //     default (-1) because setLocationWorld never writes it. The real
        //     plane lives on nonInstancedPoint.getPlane(). Using -1 here would
        //     produce new WorldPoint(tx, ty, -1) for every copy, which then
        //     fails LocalPoint.fromWorld's "wv.plane != world.plane" guard --
        //     fromWorld returns null, setLocationWorld can't position the
        //     character, and the engine renders at a fallback (visible as
        //     every copy stacked on the same wrong tile).
        int plane;
        if (inPOH)
        {
            plane = cornerA.getInstancedPlane();
        }
        else
        {
            WorldPoint wpForPlane = cornerA.getNonInstancedPoint();
            plane = wpForPlane != null ? wpForPlane.getPlane() : 0;
        }

        // Pull tile coordinates from whichever point applies for this context.
        // Overworld uses WorldPoint (already in tile units). POH/instance uses
        // LocalPoint (1/128 units, divide by 128 for tile grid).
        int aTileX, aTileY, bTileX, bTileY;
        if (inPOH)
        {
            LocalPoint a = cornerA.getInstancedPoint();
            LocalPoint b = cornerB.getInstancedPoint();
            if (a == null || b == null)
            {
                JOptionPane.showMessageDialog(this,
                        "Couldn't read instance positions for one of the corners.",
                        "Fill Rectangle", JOptionPane.WARNING_MESSAGE);
                return;
            }
            aTileX = a.getX() / 128;
            aTileY = a.getY() / 128;
            bTileX = b.getX() / 128;
            bTileY = b.getY() / 128;
        }
        else
        {
            WorldPoint a = cornerA.getNonInstancedPoint();
            WorldPoint b = cornerB.getNonInstancedPoint();
            if (a == null || b == null)
            {
                JOptionPane.showMessageDialog(this,
                        "Couldn't read world positions for one of the corners.",
                        "Fill Rectangle", JOptionPane.WARNING_MESSAGE);
                return;
            }
            aTileX = a.getX();
            aTileY = a.getY();
            bTileX = b.getX();
            bTileY = b.getY();
        }

        int minX = Math.min(aTileX, bTileX);
        int maxX = Math.max(aTileX, bTileX);
        int minY = Math.min(aTileY, bTileY);
        int maxY = Math.max(aTileY, bTileY);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int totalTiles = width * height;

        JSpinner strideSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Stride (tile spacing):"));
        panel.add(strideSpinner);
        panel.add(new JLabel("Rectangle size:"));
        panel.add(new JLabel(width + " x " + height + " = " + totalTiles + " tiles"));

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Fill Rectangle (" + cornerA.getName() + " & " + cornerB.getName() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        final int stride = Math.max(1, ((Number) strideSpinner.getValue()).intValue());

        // Find or create the destination folder. Reusing an existing folder of
        // the same name (instead of always creating a new one) means running
        // Fill Rectangle multiple times with the same two corners appends to
        // one folder rather than littering the tree with duplicates.
        ParentPanel parentPanel = cornerA.getParentPanel();
        ManagerTree managerTree = toolBox.getManagerPanel().getManagerTree();
        DefaultMutableTreeNode parentFolderNode = managerTree.getParentFolderNode(parentPanel, false);
        String folderName = "Fill: " + cornerA.getName() + " & " + cornerB.getName();
        final DefaultMutableTreeNode fillFolderNode = findOrCreateChildFolder(managerTree, parentFolderNode, folderName);

        // Iterative naming: single global counter so copies get strictly
        // sequential names regardless of which corner templated them. Earlier
        // attempt used per-source counters but when both corners shared a base
        // name (very common -- duplicating a Character via the side-panel
        // button keeps the same stem) the two counters produced duplicate
        // names like "Foo (1)", "Foo (1)" instead of iterating. Use cornerA's
        // stripped name as the base (the user can rename later if cornerB had
        // a meaningfully different label).
        final String baseName = stripTrailingNumber(cornerA.getName());
        final int[] globalCounter = {1};

        // Spawn copies on a background thread so the (potentially large) fill
        // doesn't freeze the EDT. createCharacter does its own clientThread
        // hops internally, so we just need to keep the iteration off the EDT.
        final WorldView worldView = client != null ? client.getTopLevelWorldView() : null;
        final int aTileXFinal = aTileX;
        final int aTileYFinal = aTileY;
        final int bTileXFinal = bTileX;
        final int bTileYFinal = bTileY;
        final int minXFinal = minX;
        final int maxXFinal = maxX;
        final int minYFinal = minY;
        final int maxYFinal = maxY;
        final boolean inPOHFinal = inPOH;
        final int planeFinal = plane;
        Thread thread = new Thread(() ->
        {
            int spawned = 0;
            for (int dy = 0; minYFinal + dy <= maxYFinal; dy += stride)
            {
                for (int dx = 0; minXFinal + dx <= maxXFinal; dx += stride)
                {
                    int tx = minXFinal + dx;
                    int ty = minYFinal + dy;

                    // Skip the original corners -- they already exist; reusing
                    // their tiles would create a duplicate on top of them.
                    if ((tx == aTileXFinal && ty == aTileYFinal) || (tx == bTileXFinal && ty == bTileYFinal))
                    {
                        continue;
                    }

                    // Checkerboard alternation by stride-step parity so the
                    // pattern is the same regardless of stride.
                    int parity = ((dx / stride) + (dy / stride)) % 2;
                    Character source = parity == 0 ? cornerA : cornerB;
                    String name = baseName + " (" + (globalCounter[0]++) + ")";

                    spawnFillCopy(source, name, tx, ty, planeFinal, inPOHFinal, worldView, fillFolderNode, parentPanel);
                    spawned++;
                }
            }

            final int totalSpawned = spawned;
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Filled " + totalSpawned + " tiles into folder '" + folderName + "'.",
                            "Fill Rectangle", JOptionPane.INFORMATION_MESSAGE));
        });
        thread.start();
    }

    /**
     * Find an existing direct-child folder of {@code parent} with the given
     * {@code name}, or create one if it doesn't exist. Used by Fill Rectangle
     * so repeated fills with the same corners append to one folder.
     */
    private DefaultMutableTreeNode findOrCreateChildFolder(ManagerTree managerTree, DefaultMutableTreeNode parent, String name)
    {
        java.util.Enumeration<javax.swing.tree.TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (child.getUserObject() instanceof com.creatorskit.swing.manager.Folder)
            {
                com.creatorskit.swing.manager.Folder f = (com.creatorskit.swing.manager.Folder) child.getUserObject();
                if (name.equals(f.getName()))
                {
                    return child;
                }
            }
        }
        return managerTree.addFolderNode(parent, name);
    }

    /**
     * Strip a trailing " (N)" suffix from a name so we can re-attach a fresh
     * incrementing counter. "Foo" stays "Foo"; "Foo (3)" becomes "Foo".
     */
    private static String stripTrailingNumber(String name)
    {
        if (name == null) return "";
        return name.replaceFirst("\\s*\\(\\d+\\)\\s*$", "");
    }

    /**
     * Drops a duplicate of {@code source} onto an explicit overworld tile.
     * Public entry point for the paste-at-cursor / paint-drag hotkey -- single
     * call sites don't need the bookkeeping (folder, counter, summary) that
     * Fill Rectangle bundles into spawnFillCopy, but the underlying placement
     * + arg-swap workaround is identical. Names the copy "{source} (N)" with
     * the next available index based on what already exists so paint-drag
     * doesn't pile up duplicate "{source} (1)" entries.
     *
     * <p>Lands the copy under the source's panel root (no special folder) so
     * the user can immediately see + drag it where existing duplicates live.
     */
    public void pasteCharacterAtTile(Character source, int tileX, int tileY, int plane, boolean inPOH, WorldView worldView)
    {
        if (source == null) return;
        String name = stripTrailingNumber(source.getName()) + " (" + nextPasteCounter(source) + ")";
        spawnFillCopy(source, name, tileX, tileY, plane, inPOH, worldView, null, source.getParentPanel());
    }

    // ----- Folder stamp -----------------------------------------------------
    //
    // A "stamp" duplicates an entire folder + its descendant Characters as
    // a new sibling folder, with each Character placed at a tile offset
    // from the first descendant (the "pivot"). Internal layout is preserved:
    // Fire 2 tiles east of Ring in the source folder stays 2 east of Ring
    // in every stamp. Optional time offset shifts each new Character's
    // keyframes -- used by Random Hazard Grid to give every stamp a
    // staggered start tick in one atomic operation.

    /**
     * First descendant {@link Character} of {@code folder} in DFS tree order,
     * or null if the folder is empty. Used as the pivot for folder stamps --
     * its tile becomes the anchor that every other descendant offsets from.
     */
    public Character findFolderPivot(Folder folder)
    {
        if (folder == null || folder.getLinkedManagerNode() == null) return null;
        ArrayList<Character> chars = new ArrayList<>();
        toolBox.getManagerPanel().getManagerTree().getCharacterNodeChildren(folder.getLinkedManagerNode(), chars);
        return chars.isEmpty() ? null : chars.get(0);
    }

    /**
     * Next "(N)" suffix for sibling-folder naming. Mirrors
     * {@link #nextPasteCounter(Character)} but scoped to the source folder's
     * parent (so two siblings named "Set 1 (2)" and "Set 1 (3)" already in
     * the parent both bump the counter).
     */
    private int nextFolderPasteCounter(Folder source)
    {
        if (source == null) return 1;
        DefaultMutableTreeNode parent = source.getParentManagerNode();
        if (parent == null) return 1;
        String base = stripTrailingNumber(source.getName());
        int max = 0;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\((\\d+)\\)$");
        java.util.Enumeration<javax.swing.tree.TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (!(child.getUserObject() instanceof Folder)) continue;
            Folder f = (Folder) child.getUserObject();
            String n = f.getName();
            if (n == null) continue;
            if (!stripTrailingNumber(n).equals(base)) continue;
            java.util.regex.Matcher m = p.matcher(n);
            if (m.find())
            {
                try
                {
                    int v = Integer.parseInt(m.group(1));
                    if (v > max) max = v;
                }
                catch (NumberFormatException ignored) {}
            }
        }
        return max + 1;
    }

    /**
     * Convenience overload: stamp at the source folder's own tiles (no offset
     * + no time shift). Used by CTRL+D and CTRL+V-in-tree on folders.
     */
    public Folder stampFolderInPlace(Folder source)
    {
        if (source == null) return null;
        Character pivot = findFolderPivot(source);
        if (pivot == null) return null;
        WorldPoint pivotTile = pivot.getNonInstancedPoint();
        if (pivotTile == null) return null;
        return stampFolderAtTile(source, pivotTile, 0.0, 0.0, client.getTopLevelWorldView());
    }

    /**
     * Stamps a copy of {@code source} at {@code targetTile}, using the first
     * descendant Character as the pivot. Every descendant Character is
     * placed at (its own tile + (targetTile - pivotTile)), preserving the
     * source folder's internal positional layout.
     *
     * <p>{@code timeOffset} shifts every cloned keyframe whose tick is at
     * or above {@code shiftThreshold}. The default (0, 0) values clone
     * verbatim. Random Hazard Grid passes the step tick as timeOffset and
     * lets the user set the threshold to preserve early baseline kfs.
     *
     * <p>Returns the new sibling folder, or null if the source had no
     * descendant Characters or no pivot tile.
     */
    public Folder stampFolderAtTile(Folder source, WorldPoint targetTile, double timeOffset, double shiftThreshold, WorldView worldView)
    {
        if (source == null || targetTile == null) return null;
        DefaultMutableTreeNode srcNode = source.getLinkedManagerNode();
        if (srcNode == null) return null;
        Character pivot = findFolderPivot(source);
        if (pivot == null) return null;
        WorldPoint pivotTile = pivot.getNonInstancedPoint();
        if (pivotTile == null) return null;

        final int dx = targetTile.getX() - pivotTile.getX();
        final int dy = targetTile.getY() - pivotTile.getY();
        final int newPlane = targetTile.getPlane();

        DefaultMutableTreeNode parentNode = source.getParentManagerNode();
        if (parentNode == null) parentNode = (DefaultMutableTreeNode) srcNode.getParent();
        if (parentNode == null) return null;

        ManagerTree managerTree = toolBox.getManagerPanel().getManagerTree();
        ParentPanel parentPanel = managerTree.treeContainsSidePanel(parentNode) ? ParentPanel.SIDE_PANEL : ParentPanel.MANAGER;

        String newName = stripTrailingNumber(source.getName()) + " (" + nextFolderPasteCounter(source) + ")";
        DefaultMutableTreeNode dstFolderNode = managerTree.addFolderNode(parentNode, newName);

        cloneFolderContents(srcNode, dstFolderNode, dx, dy, newPlane, timeOffset, shiftThreshold, parentPanel, worldView);

        return (Folder) dstFolderNode.getUserObject();
    }

    /**
     * Tools &gt; Random Hazard Grid... -- spreads N stamps of a source
     * folder across a rectangular tile area, advancing the timeline step
     * by step. At each step tick the planner rolls K stamps; each stamp
     * lands at a random tile in the rectangle and clones the source folder
     * with its keyframes shifted by the step tick.
     *
     * <p>The source folder is whichever folder the user has DIRECTLY
     * clicked in the manager tree -- explicit so the user can't trigger
     * the grid by accident with a stray multi-selection. Refuses to open
     * without one.
     *
     * <p>Parameters: tile-area corners (captured live from the hovered
     * scene tile), time window [from, to], step min/max (ticks between
     * grid rows), count per step min/max (stamps per row), shift threshold
     * (kfs at tick &lt; threshold stay at their original tick, useful for
     * preserving a tick-0 baseline disable on every stamp).
     */
    /**
     * Hazard-grid state owned by the open dialog. The plugin reads this
     * via {@link #getHazardGridState()} to:
     *   - inject "Set as Corner A/B" entries on tile right-clicks
     *   - drive CTRL+I / CTRL+E paint-drag that adds / removes individual
     *     tiles from the candidate pool
     *   - drive the pink overlay that paints the live pool on the game
     *     canvas
     *
     * <p>Effective pool = (rectangle from cornerA to cornerB if both set)
     * ∪ {@code includedTiles} − {@code excludedTiles}. Resolved on demand
     * via {@link #resolvePool()} since corner edits + CTRL+I/E paints are
     * all mutations that should be reflected immediately in the overlay.
     */
    public static final class HazardGridState
    {
        public WorldPoint cornerA;
        public WorldPoint cornerB;
        /** Extra tiles added via CTRL+I; outside the rect or as overrides for excluded. */
        public final java.util.Set<WorldPoint> includedTiles = new java.util.LinkedHashSet<>();
        /** Cutouts subtracted from (rect ∪ includedTiles) via CTRL+E. */
        public final java.util.Set<WorldPoint> excludedTiles = new java.util.LinkedHashSet<>();
        public Runnable onChange;

        public void setCornerA(WorldPoint wp) { cornerA = wp; fire(); }
        public void setCornerB(WorldPoint wp) { cornerB = wp; fire(); }

        /** CTRL+I action: tile becomes part of the pool. Beats any prior exclude. */
        public void includeTile(WorldPoint wp)
        {
            if (wp == null) return;
            excludedTiles.remove(wp);
            includedTiles.add(wp);
            fire();
        }

        /** CTRL+E action: tile is carved out. Beats any prior include. */
        public void excludeTile(WorldPoint wp)
        {
            if (wp == null) return;
            includedTiles.remove(wp);
            excludedTiles.add(wp);
            fire();
        }

        /**
         * Resolves the live candidate pool. Walks the rect once + applies
         * include/exclude on top. LinkedHashSet preserves deterministic
         * iteration order if the caller wants to (the Run loop doesn't
         * need it, but the overlay benefits from stable layering).
         */
        public java.util.Set<WorldPoint> resolvePool()
        {
            java.util.LinkedHashSet<WorldPoint> pool = new java.util.LinkedHashSet<>();
            if (cornerA != null && cornerB != null)
            {
                int minX = Math.min(cornerA.getX(), cornerB.getX());
                int maxX = Math.max(cornerA.getX(), cornerB.getX());
                int minY = Math.min(cornerA.getY(), cornerB.getY());
                int maxY = Math.max(cornerA.getY(), cornerB.getY());
                int plane = cornerA.getPlane();
                for (int x = minX; x <= maxX; x++)
                {
                    for (int y = minY; y <= maxY; y++)
                    {
                        pool.add(new WorldPoint(x, y, plane));
                    }
                }
            }
            pool.addAll(includedTiles);
            pool.removeAll(excludedTiles);
            return pool;
        }

        private void fire() { if (onChange != null) onChange.run(); }
    }

    private HazardGridState hazardGridState = null;

    /**
     * Returns the live hazard-grid dialog state, or null when no Random
     * Hazard Grid window is open. Plugin reads this from onPostMenuSort
     * to gate the corner-capture menu entries.
     */
    public HazardGridState getHazardGridState()
    {
        return hazardGridState;
    }

    public void showRandomHazardGridDialog()
    {
        ManagerTree managerTree = toolBox.getManagerPanel().getManagerTree();
        Folder[] directFolders = managerTree.getDirectlySelectedFolders();
        if (directFolders.length == 0)
        {
            JOptionPane.showMessageDialog(this,
                    "Select a folder in the manager tree first -- it'll be the source template for each stamp.",
                    "Random Hazard Grid",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        final Folder source = directFolders[0];

        // Publish state so CreatorsPlugin.addMenuEntries can wire the
        // "Set as Corner A/B" right-click entries while this window is up.
        final HazardGridState state = new HazardGridState();
        hazardGridState = state;

        JLabel cornerALabel = new JLabel();
        JLabel cornerBLabel = new JLabel();
        Runnable refreshCornerLabels = () ->
        {
            cornerALabel.setText(state.cornerA == null
                    ? "<not set>"
                    : state.cornerA.getX() + "," + state.cornerA.getY() + " (p" + state.cornerA.getPlane() + ")");
            cornerBLabel.setText(state.cornerB == null
                    ? "<not set>"
                    : state.cornerB.getX() + "," + state.cornerB.getY() + " (p" + state.cornerB.getPlane() + ")");
        };
        JLabel poolSizeLabel = new JLabel();
        Runnable refreshPoolSize = () ->
        {
            int n = state.resolvePool().size();
            poolSizeLabel.setText(n + " candidate tile" + (n == 1 ? "" : "s"));
        };
        // Chain refreshCornerLabels with the pool-size refresh so any state
        // mutation hits both. Wrapping rather than replacing because the
        // right-click menu still calls onChange directly.
        Runnable everyRefresh = () -> { refreshCornerLabels.run(); refreshPoolSize.run(); };
        state.onChange = everyRefresh;
        everyRefresh.run();

        JSpinner timeFromSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100000.0, 1.0));
        JSpinner timeToSpinner = new JSpinner(new SpinnerNumberModel(100.0, 0.0, 100000.0, 1.0));
        JSpinner stepMinSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 1000, 1));
        JSpinner stepMaxSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 1000, 1));
        JSpinner countMinSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        JSpinner countMaxSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 100, 1));
        JSpinner shiftThresholdSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 100000.0, 1.0));
        JCheckBox collisionCheck = new JCheckBox("Collision check (no two stamps on the same tile)", true);
        collisionCheck.setToolTipText("Prevent two stamps from landing on the same tile. Uncheck to allow stacking.");

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Source folder:"));
        form.add(new JLabel(source.getName()));
        form.add(new JLabel("Corner A:"));
        form.add(cornerALabel);
        form.add(new JLabel("Corner B:"));
        form.add(cornerBLabel);
        form.add(new JLabel("Candidate pool:"));
        form.add(poolSizeLabel);
        form.add(new JLabel("Time window from (tick):"));
        form.add(timeFromSpinner);
        form.add(new JLabel("Time window to (tick):"));
        form.add(timeToSpinner);
        form.add(new JLabel("Step ticks (min):"));
        form.add(stepMinSpinner);
        form.add(new JLabel("Step ticks (max):"));
        form.add(stepMaxSpinner);
        form.add(new JLabel("Count per step (min):"));
        form.add(countMinSpinner);
        form.add(new JLabel("Count per step (max):"));
        form.add(countMaxSpinner);
        form.add(new JLabel("Shift only kfs at tick >="));
        form.add(shiftThresholdSpinner);
        form.add(new JLabel("Collision check:"));
        form.add(collisionCheck);

        JLabel hint = new JLabel("<html><i>Pink tiles in the world preview the candidate pool. Refine with:<br>"
                + "<b>Hold CTRL+I + drag</b> to <i>include</i> tiles, <b>CTRL+E + drag</b> to <i>exclude</i>.<br>"
                + "Keyframes at tick &gt;= threshold get shifted by the step tick; set the<br>"
                + "threshold to 1 to preserve a tick-0 baseline across every stamp.</i></html>");
        hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));

        JButton runBtn = new JButton("Run");
        JButton closeBtn = new JButton("Close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(runBtn);
        buttons.add(closeBtn);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        content.add(form, BorderLayout.NORTH);
        content.add(hint, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        // Non-modal JFrame so the user can hover game tiles BETWEEN clicks
        // -- modal dialogs (JOptionPane) steal focus and prevent the game
        // canvas from receiving the mouse-move events that update
        // worldView.getSelectedSceneTile(). With a non-modal frame the user
        // can: focus the game, hover the tile they want, click back on the
        // capture button (the most recently selected scene tile is what the
        // button reads). Run stays available so the user can stamp, tweak
        // params, and stamp again without reopening.
        final javax.swing.JFrame frame = new javax.swing.JFrame("Random Hazard Grid -- source: " + source.getName());
        frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(content);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);  // keep visible above the game canvas

        // Clear hazardGridState on close so the plugin's menu hook stops
        // injecting the "Set as Corner" entries once the window is gone.
        frame.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { hazardGridState = null; }
        });

        runBtn.addActionListener(e ->
        {
            java.util.Set<WorldPoint> pool = state.resolvePool();
            if (pool.isEmpty())
            {
                JOptionPane.showMessageDialog(frame,
                        "Candidate pool is empty. Set corners A and B (right-click a tile), or CTRL+I-drag tiles in manually.",
                        "Random Hazard Grid", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double timeFrom = ((Number) timeFromSpinner.getValue()).doubleValue();
            double timeTo = ((Number) timeToSpinner.getValue()).doubleValue();
            int stepMin = ((Number) stepMinSpinner.getValue()).intValue();
            int stepMax = ((Number) stepMaxSpinner.getValue()).intValue();
            int countMin = ((Number) countMinSpinner.getValue()).intValue();
            int countMax = ((Number) countMaxSpinner.getValue()).intValue();
            double shiftThreshold = ((Number) shiftThresholdSpinner.getValue()).doubleValue();
            boolean collisionOn = collisionCheck.isSelected();

            if (timeTo < timeFrom) { double t = timeFrom; timeFrom = timeTo; timeTo = t; }
            if (stepMin > stepMax) { int t = stepMin; stepMin = stepMax; stepMax = t; }
            if (countMin > countMax) { int t = countMin; countMin = countMax; countMax = t; }

            runRandomHazardGrid(source, pool,
                    timeFrom, timeTo, stepMin, stepMax, countMin, countMax, shiftThreshold, collisionOn);
        });
        closeBtn.addActionListener(e -> frame.dispose());

        frame.setVisible(true);
    }

    /**
     * Executor for {@link #showRandomHazardGridDialog}. Walks the time
     * axis from {@code timeFrom} to {@code timeTo}, at each tick rolling
     * a random count K in [countMin, countMax] and stamping the folder K
     * times at random tiles drawn from {@code pool}. Each stamp's
     * keyframes shift by the current step tick.
     *
     * <p>When {@code collisionCheckOn} is true the planner samples without
     * replacement across the entire Run -- no two stamps ever share a tile.
     * If the pool is exhausted before the time window ends the Run stops
     * gracefully. With collision off, every step picks fresh randoms and
     * stacking is allowed.
     */
    private void runRandomHazardGrid(Folder source,
                                     java.util.Set<WorldPoint> pool,
                                     double timeFrom, double timeTo,
                                     int stepMin, int stepMax,
                                     int countMin, int countMax,
                                     double shiftThreshold,
                                     boolean collisionCheckOn)
    {
        // Plane mismatch sanity: warn in chat if the pool spans multiple
        // planes (e.g. user CTRL+I'd tiles on a different floor). Stamps
        // still use each tile's own plane.
        java.util.Set<Integer> planes = new java.util.HashSet<>();
        for (WorldPoint wp : pool) planes.add(wp.getPlane());
        if (planes.size() > 1)
        {
            plugin.sendChatMessage("Random Hazard Grid: candidate tiles span planes " + planes + ". Stamps use each tile's own plane.");
        }

        // Working copy: when collision is on we sample without replacement
        // by removing picked tiles. When off, we sample with replacement
        // (picking a fresh random tile each time) so the original pool is
        // untouched -- we use the list view directly.
        java.util.List<WorldPoint> remaining = new java.util.ArrayList<>(pool);

        WorldView worldView = client.getTopLevelWorldView();
        java.util.Random rng = new java.util.Random();
        double tick = timeFrom;
        int totalStamps = 0;
        // Hard safety bound -- if step rolls 0 (shouldn't, min is 1) we'd
        // loop forever; cap total stamps at 10k to protect against pathological
        // params even at minute tick windows.
        final int SAFETY = 10000;
        boolean exhausted = false;
        outer:
        while (tick <= timeTo && totalStamps < SAFETY)
        {
            int k = countMin == countMax ? countMin : countMin + rng.nextInt(countMax - countMin + 1);
            for (int i = 0; i < k && totalStamps < SAFETY; i++)
            {
                WorldPoint target;
                if (collisionCheckOn)
                {
                    if (remaining.isEmpty())
                    {
                        exhausted = true;
                        break outer;
                    }
                    int idx = rng.nextInt(remaining.size());
                    target = remaining.remove(idx);
                }
                else
                {
                    target = remaining.get(rng.nextInt(remaining.size()));
                }
                Folder stamp = stampFolderAtTile(source, target, tick, shiftThreshold, worldView);
                if (stamp != null) totalStamps++;
            }
            int step = stepMin == stepMax ? stepMin : stepMin + rng.nextInt(stepMax - stepMin + 1);
            if (step <= 0) step = 1;
            tick += step;
        }

        StringBuilder summary = new StringBuilder("Random Hazard Grid: stamped " + totalStamps + " copies of folder '" + source.getName() + "'");
        if (exhausted) summary.append(" (pool exhausted at tick ").append(tick).append(")");
        summary.append(".");
        plugin.sendChatMessage(summary.toString());
        if (totalStamps >= SAFETY)
        {
            JOptionPane.showMessageDialog(this,
                    "Hit the " + SAFETY + "-stamp safety cap. Try smaller count-per-step or a shorter time window.",
                    "Random Hazard Grid", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Recursive worker behind {@link #stampFolderAtTile}. Walks every child
     * of {@code srcNode}: Characters land in {@code dstFolderNode} at the
     * tile offset, subfolders create matching sub-destination folders and
     * recurse.
     */
    private void cloneFolderContents(DefaultMutableTreeNode srcNode, DefaultMutableTreeNode dstFolderNode,
                                     int dx, int dy, int newPlane,
                                     double timeOffset, double shiftThreshold,
                                     ParentPanel parentPanel, WorldView worldView)
    {
        ManagerTree managerTree = toolBox.getManagerPanel().getManagerTree();
        java.util.Enumeration<javax.swing.tree.TreeNode> children = srcNode.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            Object user = child.getUserObject();
            if (user instanceof Character)
            {
                Character srcChar = (Character) user;
                WorldPoint srcTile = srcChar.getNonInstancedPoint();
                if (srcTile == null) continue;
                int tx = srcTile.getX() + dx;
                int ty = srcTile.getY() + dy;
                String name = stripTrailingNumber(srcChar.getName()) + " (" + nextPasteCounter(srcChar) + ")";
                spawnFillCopy(srcChar, name, tx, ty, newPlane, srcChar.isInPOH(),
                        worldView, dstFolderNode, parentPanel, timeOffset, shiftThreshold);
            }
            else if (user instanceof Folder)
            {
                Folder srcSub = (Folder) user;
                DefaultMutableTreeNode dstSubFolder = managerTree.addFolderNode(dstFolderNode, srcSub.getName());
                cloneFolderContents(child, dstSubFolder, dx, dy, newPlane, timeOffset, shiftThreshold, parentPanel, worldView);
            }
        }
    }

    /**
     * Returns the next available "(N)" index for paste-at-cursor naming, based
     * on existing Characters whose names already match the {base} (N) pattern.
     * Cheap linear scan -- at the scale of "a few hundred Characters" it's well
     * under a frame's worth of work even when called every mouse-move during
     * paint-drag.
     */
    private int nextPasteCounter(Character source)
    {
        String base = stripTrailingNumber(source.getName());
        int max = 0;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\((\\d+)\\)$");
        for (Character c : plugin.getCharacters())
        {
            String n = c.getName();
            if (n == null) continue;
            String stripped = stripTrailingNumber(n);
            if (!stripped.equals(base)) continue;
            java.util.regex.Matcher m = p.matcher(n);
            if (m.find())
            {
                try
                {
                    int v = Integer.parseInt(m.group(1));
                    if (v > max) max = v;
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
        return max + 1;
    }

    /**
     * Duplicates {@code source} at its OWN world position, parented under its
     * OWN existing folder. Entry point for ManagerTree's CTRL+D / CTRL+V /
     * "Duplicate" context-menu item -- the user picked the source in the
     * tree, not on the world, so we keep both the tile and the folder placement
     * stable instead of routing through tree-selection-based parent inference.
     *
     * <p>Returns the new Character (synchronously; the actual addPanel UI
     * wiring runs through invokeLater inside spawnFillCopy but the model
     * object is fully built by the time we return).
     */
    public Character duplicateCharacterInPlace(Character source)
    {
        if (source == null) return null;
        String name = stripTrailingNumber(source.getName()) + " (" + nextPasteCounter(source) + ")";
        WorldPoint wp = source.getNonInstancedPoint();
        int tileX = wp != null ? wp.getX() : 0;
        int tileY = wp != null ? wp.getY() : 0;
        int plane = wp != null ? wp.getPlane() : source.getInstancedPlane();
        WorldView wv = client.getTopLevelWorldView();
        // Use source.getParentManagerNode() as fillFolderNode so the copy lands
        // directly under the source's folder, regardless of what's currently
        // highlighted in the tree (matches "same parent as source" semantics
        // the user asked for in the tree copy/paste/duplicate question).
        return spawnFillCopy(source, name, tileX, tileY, plane, source.isInPOH(), wv,
                source.getParentManagerNode(), source.getParentPanel());
    }

    /**
     * Multi-source variant of {@link #duplicateCharacterInPlace}. After each
     * source is cloned, the new copies replace the selection so the user can
     * immediately chain another action (move, recolour, another CTRL+D)
     * against the duplicates rather than the originals.
     *
     * <p>Filters out null / already-removed sources defensively (the clipboard
     * for CTRL+C / CTRL+V holds Character references; a paste after the
     * source was deleted shouldn't NPE, it should just skip).
     */
    public java.util.List<Character> duplicateCharactersInPlace(java.util.Collection<Character> sources)
    {
        java.util.List<Character> copies = new java.util.ArrayList<>();
        if (sources == null || sources.isEmpty()) return copies;
        for (Character src : sources)
        {
            if (src == null) continue;
            Character copy = duplicateCharacterInPlace(src);
            if (copy != null) copies.add(copy);
        }
        if (!copies.isEmpty())
        {
            selectionManager.selectAll(copies);
        }
        return copies;
    }

    /**
     * Duplicates {@code source} onto a target tile (POH-aware) and parents the
     * new Character under {@code fillFolderNode}. Reuses createCharacter's full
     * arg form so keyframes, render-fix state, offsets, and extraScale all
     * carry over -- the only thing the fill overrides is the position and name.
     *
     * <p>Both targetWorld AND targetLocal are computed per-tile so that the
     * downstream setLocation path resolves to the right tile regardless of
     * whether the player is currently in an instance: setLocationWorld
     * computes a LocalPoint from getNonInstancedPoint via LocalPoint.fromWorld,
     * but setLocationPOH reads getInstancedPoint directly. If we shared the
     * source's getInstancedPoint across copies (the previous version), every
     * copy landed at the same tile when MovementManager.useLocalLocations
     * returned true -- visible as "all copies stacked in the middle".
     */
    private Character spawnFillCopy(Character source, String name, int targetTileX, int targetTileY, int plane, boolean inPOH,
                               @Nullable WorldView worldView, DefaultMutableTreeNode fillFolderNode, ParentPanel parentPanel)
    {
        return spawnFillCopy(source, name, targetTileX, targetTileY, plane, inPOH, worldView, fillFolderNode, parentPanel, 0.0, 0.0);
    }

    /**
     * Variant of spawnFillCopy that accepts a per-keyframe time shift.
     * Used by the folder-stamp + Random Hazard Grid path so each stamped
     * copy can advance its effect keyframes by the step tick atomically
     * (one createCharacter call = one undo step). Pass timeOffset = 0 for
     * the regular fill / paste-at-tile behaviour.
     */
    private Character spawnFillCopy(Character source, String name, int targetTileX, int targetTileY, int plane, boolean inPOH,
                               @Nullable WorldView worldView, DefaultMutableTreeNode fillFolderNode, ParentPanel parentPanel,
                               double timeOffset, double shiftThreshold)
    {
        WorldPoint targetWorld;
        LocalPoint targetLocal;
        if (inPOH)
        {
            if (worldView == null) return null;
            targetLocal = new LocalPoint(targetTileX * 128, targetTileY * 128, worldView);
            targetWorld = source.getNonInstancedPoint();
        }
        else
        {
            targetWorld = new WorldPoint(targetTileX, targetTileY, plane);
            // Compute per-tile LocalPoint so setLocationPOH would also place
            // the copy at the right spot if the player is currently inside an
            // instance even though the character's logical context is overworld.
            // Returns null if the target tile is outside the loaded scene --
            // that's OK, character will simply have setInScene(false) until the
            // scene shifts to include it.
            targetLocal = worldView != null ? LocalPoint.fromWorld(worldView, targetWorld) : null;
        }

        KeyFrameType[] summary = source.getSummary();
        KeyFrameType[] summaryCopy = summary == null ? null : new KeyFrameType[]{summary[0], summary[1], summary[2]};

        // CAUTION: the last two args ('transplant' and 'setHoveredLocation' per
        // the createCharacter signature) are silently SWAPPED on the way to
        // plugin.setupRLObject -- createCharacter line 727 calls
        // setupRLObject(character, transplant, setHoveredLocation) but
        // setupRLObject's signature is (character, setHoveredTile, transplant).
        // Passing (true, false) here would arrive as
        // (setHoveredTile=true, transplant=false), which makes setLocationWorld
        // take the TO_HOVERED_TILE branch and overwrite every copy's position
        // to the user's cursor -- visible as every fill copy stacked on the
        // user's hovered tile. The earlier rounds of Fill Rectangle fixes
        // missed this because the symptom (stacking) looked identical to a
        // shared-LocalPoint bug.
        //
        // We want setHoveredTile=false (don't use cursor) and transplant=true
        // (relocate any movement keyframes to the target tile so the copy
        // stays at its tile instead of wandering along the source's path).
        // To achieve that through the swap, pass (transplant=false,
        // setHoveredLocation=true).
        Character copy = createCharacter(
                parentPanel,
                name,
                (int) source.getModelSpinner().getValue(),
                (CustomModel) source.getComboBox().getSelectedItem(),
                source.isCustomMode(),
                (int) source.getOrientationSpinner().getValue(),
                (int) source.getAnimationSpinner().getValue(),
                (int) source.getAnimationFrameSpinner().getValue(),
                (int) source.getRadiusSpinner().getValue(),
                duplicateKeyFrames(source, timeOffset, shiftThreshold),
                summaryCopy,
                getRandomColor(),
                source.isActive(),
                targetWorld,
                targetLocal,
                plane,
                inPOH,
                /* createCharacter's 'transplant' (becomes setHoveredTile) */ false,
                /* createCharacter's 'setHoveredLocation' (becomes transplant) */ true);

        // Copy visual state, same as onDuplicatePressed.
        copy.setRenderFixWidth(source.getRenderFixWidth());
        copy.setRenderFixHeight(source.getRenderFixHeight());
        copy.setRenderFix(source.isRenderFix());
        copy.setOffsetX(source.getOffsetX());
        copy.setOffsetY(source.getOffsetY());
        copy.setOffsetZ(source.getOffsetZ());
        copy.setExtraScale(source.getExtraScale());

        SwingUtilities.invokeLater(() -> addPanel(parentPanel, copy, fillFolderNode, false, false));
        return copy;
    }

    /**
     * Show the swatch picker rooted at (x, y) within {@code invoker}. The picked
     * colour is applied to every selected Character (when {@code target} is part of
     * the multi-selection) or to just {@code target} otherwise.
     */
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
            swatch.setOpaque(true);
            swatch.setBorderPainted(false);
            swatch.setPreferredSize(swatchSize);
            swatch.setFocusable(false);
            swatch.addActionListener(ev ->
            {
                applyColorToTargets(target, c);
                popup.setVisible(false);
            });
            row.add(swatch);
        }

        JButton random = new JButton("?");
        random.setPreferredSize(swatchSize);
        random.setFocusable(false);
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

    /**
     * Returns the Characters a per-Character UI edit should fan out to. If {@code source}
     * is part of an active multi-selection, the full selected set is returned; otherwise
     * just {@code source}.
     */
    private java.util.Set<Character> getBulkTargets(Character source)
    {
        if (selectionManager.size() > 1 && selectionManager.isSelected(source))
        {
            return selectionManager.getSelected();
        }
        return java.util.Collections.singleton(source);
    }

    /**
     * Propagate a spinner value to every other selected Character's spinner. Setting the
     * spinner value re-fires its ChangeListener, which applies to its own Character;
     * the {@code bulkEditing} flag prevents that nested call from re-propagating.
     */
    private void propagateSpinner(Character source, Object value, java.util.function.Function<Character, JSpinner> getter)
    {
        if (bulkEditing)
        {
            return;
        }
        java.util.Set<Character> targets = getBulkTargets(source);
        if (targets.size() == 1)
        {
            return;
        }
        bulkEditing = true;
        try
        {
            for (Character c : targets)
            {
                if (c == source)
                {
                    continue;
                }
                JSpinner s = getter.apply(c);
                if (s != null && !java.util.Objects.equals(s.getValue(), value))
                {
                    s.setValue(value);
                }
            }
        }
        finally
        {
            bulkEditing = false;
        }
    }

    /**
     * Apply the Id/Custom model toggle to a single Character (used both for the
     * source click and for fan-out to selected siblings).
     */
    private void applyModelMode(Character c, boolean customMode, JButton modelBtn, JSpinner modelSp, JComboBox<CustomModel> modelCb)
    {
        c.setCustomMode(customMode);
        if (modelBtn != null)
        {
            modelBtn.setText(customMode ? "Custom" : "Id");
        }
        if (modelSp != null)
        {
            modelSp.setVisible(!customMode);
        }
        if (modelCb != null)
        {
            modelCb.setVisible(customMode);
        }
        if (customMode)
        {
            plugin.setModel(c, true, -1);
        }
        else
        {
            int idValue = modelSp != null ? (int) modelSp.getValue() : 0;
            plugin.setModel(c, false, idValue);
        }
    }

    /**
     * Propagate a custom-model selection to every other selected Character.
     */
    private void propagateCustomModel(Character source, CustomModel m)
    {
        if (bulkEditing)
        {
            return;
        }
        java.util.Set<Character> targets = getBulkTargets(source);
        if (targets.size() == 1)
        {
            return;
        }
        bulkEditing = true;
        try
        {
            for (Character c : targets)
            {
                if (c == source)
                {
                    continue;
                }
                c.setStoredModel(m);
                JComboBox<CustomModel> cb = c.getComboBox();
                if (cb != null && cb.getSelectedItem() != m)
                {
                    cb.setSelectedItem(m);
                }
                if (c.isCustomMode())
                {
                    plugin.setModel(c, true, -1);
                }
            }
        }
        finally
        {
            bulkEditing = false;
        }
    }

    /**
     * Propagate the spawn checkbox state to every other selected Character.
     */
    private void propagateActive(Character source, boolean active)
    {
        if (bulkEditing)
        {
            return;
        }
        java.util.Set<Character> targets = getBulkTargets(source);
        if (targets.size() == 1)
        {
            return;
        }
        bulkEditing = true;
        try
        {
            for (Character c : targets)
            {
                if (c == source)
                {
                    continue;
                }
                if (c.isActive() != active)
                {
                    JCheckBox cb = c.getSpawnCheckBox();
                    if (cb != null)
                    {
                        cb.setSelected(active);
                    }
                    c.setActive(active, active, true, clientThread);
                }
            }
        }
        finally
        {
            bulkEditing = false;
        }
    }

    /**
     * Apply the chosen color to either every selected Character (when the clicked
     * Character is part of the multi-selection) or to just the clicked Character.
     */
    public void applyColorToTargets(Character clicked, Color color)
    {
        java.util.Set<Character> selected = selectionManager.getSelected();
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
        JButton btn = c.getColourButton();
        if (btn != null)
        {
            btn.setForeground(color);
        }
        JTextField nameField = c.getNameField();
        if (nameField != null)
        {
            Border current = nameField.getBorder();
            Border inner = current instanceof CompoundBorder
                    ? ((CompoundBorder) current).getInsideBorder()
                    : current;
            nameField.setBorder(buildNameFieldBorder(inner, color));
        }
    }

    /**
     * Iterates every Character panel and applies the correct border based on
     * whether it's selected (per SelectionManager) or hovered.
     */
    public void refreshSelectionBorders()
    {
        ArrayList<Character> characters = plugin.getCharacters();
        Character hovered = plugin.getHoveredCharacter();
        for (int i = 0; i < characters.size(); i++)
        {
            Character c = characters.get(i);
            JPanel panel = c.getObjectPanel();
            if (panel == null)
            {
                continue;
            }
            if (selectionManager.isSelected(c))
            {
                panel.setBorder(selectedBorder);
            }
            else if (c == hovered)
            {
                panel.setBorder(hoveredBorder);
            }
            else
            {
                panel.setBorder(defaultBorder);
            }
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
        plugin.setSelectedCharacter(null);
    }

    public void setHoveredCharacter(Character hovered, JPanel jPanel)
    {
        if (selectionManager.isSelected(hovered))
        {
            return;
        }

        jPanel.setBorder(hoveredBorder);
        plugin.setHoveredCharacter(hovered);
    }

    public void unsetHoveredCharacter(Character hoverRemoved, JPanel jPanel)
    {
        plugin.setHoveredCharacter(null);

        if (selectionManager.isSelected(hoverRemoved))
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

        // Projectile + SpotAnim 1/2 cards each mirror the Model card's custom-
        // model combo -- keep them populated with the same models so a projectile
        // or spotanim can render as any custom model the user has created.
        toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getProjectileAttributes()
                .getCustomModel()
                .addItem(model);
        toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getSpotAnimAttributes()
                .getCustomModel()
                .addItem(model);
        toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getSpotAnim2Attributes()
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

        toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getProjectileAttributes()
                .getCustomModel()
                .removeItem(model);
        toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getSpotAnimAttributes()
                .getCustomModel()
                .removeItem(model);
        toolBox.getTimeSheetPanel()
                .getAttributePanel()
                .getSpotAnim2Attributes()
                .getCustomModel()
                .removeItem(model);

        for (JComboBox<CustomModel> comboBox : comboBoxes)
        {
            comboBox.removeItem(model);
        }
        modelOrganizer.removeModelPanel(model);
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

    /**
     * Copies an about-to-be-overwritten setup file into the rotating versions
     * directory, then prunes to the configured retention count. Called from
     * {@link #saveToFile(File)} before every manual or quick save.
     */
    public void snapshotExistingFile(File file)
    {
        if (file == null || !file.exists())
        {
            return;
        }

        try
        {
            File charDir = new File(SETUP_VERSIONS_DIR, file.getName());
            if (!charDir.exists() && !charDir.mkdirs())
            {
                return;
            }

            String stamp = java.time.LocalDateTime.now().format(VERSION_STAMP);
            File snapshot = new File(charDir, stamp + ".json");
            java.nio.file.Files.copy(
                    file.toPath(),
                    snapshot.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            pruneOldVersions(charDir);
        }
        catch (Exception ex)
        {
            log.warn("Failed to snapshot setup {}: {}", file.getName(), ex.toString());
        }
    }

    /**
     * Snapshot the currently-loaded Setup's on-disk file into the versions dir.
     * Invoked by the timer in CreatorsPlugin at the user-configured cadence.
     * No-op when no setup is loaded or the source file no longer exists.
     */
    public void periodicSetupSnapshot()
    {
        snapshotExistingFile(lastFileLoaded);
    }

    private void pruneOldVersions(File charDir)
    {
        int keep = Math.max(1, config.setupVersionKeep());
        File[] versions = charDir.listFiles((d, name) -> name.endsWith(".json"));
        if (versions == null || versions.length <= keep)
        {
            return;
        }
        java.util.Arrays.sort(versions, java.util.Comparator.comparing(File::getName));
        int toDelete = versions.length - keep;
        for (int i = 0; i < toDelete; i++)
        {
            // Best-effort delete; ignore failures.
            //noinspection ResultOfMethodCallIgnored
            versions[i].delete();
        }
    }

    public void saveToFile(File file)
    {
        // Snapshot the existing file into the rotating versions dir before we
        // overwrite. Best-effort; doesn't block save on failure.
        snapshotExistingFile(file);

        ArrayList<CustomModel> customModels = plugin.getStoredModels();
        CustomModelComp[] comps = new CustomModelComp[customModels.size()];

        for (int i = 0; i < comps.length; i++)
        {
            comps[i] = customModels.get(i).getComp();
        }

        //Get Folder structure and all characters contained within
        FolderNodeSave folderNodeSave = getFolders(comps);

        // Phase 2: pull the central GlobalKeyFrames out of the plugin and bake
        // it into the SetupSave so future loads source globals from the
        // top-level field rather than walking each CharacterSave.
        SetupSave saveFile = new SetupSave(
                getPluginVersion(),
                comps,
                folderNodeSave,
                new CharacterSave[0],
                plugin.getGlobalKeyFrames());

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
                character.getSummary(),
                character.getProjectileKeyFrames(),
                character.isRenderFix(),
                character.getRenderFixWidth(),
                character.getRenderFixHeight(),
                character.getOffsetX(),
                character.getOffsetY(),
                character.getOffsetZ(),
                character.getShieldKeyFrames(),
                character.getSpecialKeyFrames(),
                // Phase 2: the three global keyframe types live in SetupSave's
                // top-level GlobalKeyFrames now. Write nulls into the per-
                // Character fields so we don't duplicate the data across N
                // CharacterSaves. Pre-Phase-2 saves still have these populated
                // and the load path migrates them; new saves keep them null.
                null,
                null,
                character.getExtraScale(),
                null,
                character.getColourKeyFrames(),
                character.getSoundKeyFrames());
    }

    public void openLoadSetupDialog()
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
                    rememberLastSetupPath(finalSelectedFile);
                    clientThread.invokeLater(() -> loadSetup(finalSelectedFile, saveFile));
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

    /**
     * Persists the path of the most recently-loaded setup to the creatorssuite
     * config group so File > Load last setup can one-click reload it across
     * sessions. Best-effort -- if persistence fails (rare; config manager not
     * ready), the menu item just stays disabled until the next successful load.
     */
    private void rememberLastSetupPath(File file)
    {
        if (file == null || plugin.getConfigManager() == null) return;
        try
        {
            plugin.getConfigManager().setConfiguration("creatorssuite", "lastSetupPath", file.getAbsolutePath());
        }
        catch (Exception ignored)
        {
        }
    }

    public void loadSetup(File file)
    {
        try
        {
            Reader reader = Files.newBufferedReader(file.toPath());
            SetupSave saveFile = plugin.getGson().fromJson(reader, SetupSave.class);
            rememberLastSetupPath(file);
            clientThread.invokeLater(() -> loadSetup(file, saveFile));
            reader.close();
            LocalTime time = LocalTime.now();
            plugin.sendChatMessage("[" + time.getHour() + ":" + time.getMinute() + "] Loaded file: " + getFileName(file));
        }
        catch (Exception e)
        {
            plugin.sendChatMessage("An error occurred while attempting to read this file.");
        }
    }

    private void loadSetup(File file, SetupSave saveFile)
    {
        ModelUtilities modelUtilities = toolBox.getModelUtilities();
        updateLoadedFile(file);
        CustomModelComp[] comps = saveFile.getComps();
        FolderNodeSave folderNodeSave = saveFile.getMasterFolderNode();
        CustomModel[] customModels = new CustomModel[comps.length];
        String fileVersion = saveFile.getVersion();
        if (fileVersion == null || fileVersion.isEmpty())
        {
            fileVersion = "1.5.0";
        }

        // Phase 2 migration: pull the global keyframes either from the new
        // top-level field or, if absent (pre-Phase-2 file), aggregate them
        // out of every CharacterSave's per-Character globals fields. Installed
        // BEFORE the character/folder walk so any code that reads the central
        // store during load sees the migrated values.
        installGlobalKeyFramesFromSave(saveFile, folderNodeSave);

        for (int i = 0; i < comps.length; i++)
        {
            CustomModelComp comp = comps[i];
            Model model;
            CustomModel customModel;
            ModelStats[] modelStats;

            switch (comp.getType())
            {
                case FORGED:
                    model = modelUtilities.createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle(), comp.getCustomLighting(), false);
                    customModel = new CustomModel(model, comp);
                    break;
                case CACHE_NPC:
                    modelStats = comp.getModelStats();
                    model = modelUtilities.constructModelFromCache(modelStats, new int[0], false, LightingStyle.ACTOR, null);
                    customModel = new CustomModel(model, comp);
                    break;
                case CACHE_PLAYER:
                    modelStats = comp.getModelStats();
                    model = modelUtilities.constructModelFromCache(modelStats, comp.getKitRecolours(), true, LightingStyle.ACTOR, null);
                    customModel = new CustomModel(model, comp);
                    break;
                default:
                case CACHE_OBJECT:
                case CACHE_SPOTANIM:
                case CACHE_GROUND_ITEM:
                case CACHE_MAN_WEAR:
                case CACHE_WOMAN_WEAR:
                    modelStats = comp.getModelStats();
                    model = modelUtilities.constructModelFromCache(modelStats, null, false, LightingStyle.DEFAULT, null);
                    customModel = new CustomModel(model, comp);
                    break;
                case BLENDER:
                    model = modelImporter.createModel(comp.getBlenderModel(), comp.getLightingStyle());
                    customModel = new CustomModel(model, comp);
            }

            modelUtilities.addCustomModel(customModel, false);
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

                // Post-load housekeeping. (1) Clear the selection -- after a fresh
                // load nothing should be highlighted so the user starts from a
                // blank slate instead of inheriting whichever Character happened
                // to be selected before. (2) If the user has the collapse-on-load
                // config toggle on, walk the tree and collapse every folder so a
                // big scene doesn't open with the whole hierarchy exploded.
                selectionManager.clear();
                managerTree.clearSelection();
                if (config.collapseFoldersOnLoad())
                {
                    managerTree.collapseAllFolders();
                }
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
                                false);

                        SwingUtilities.invokeLater(() -> addPanel(ParentPanel.SIDE_PANEL, character, true, false));
                    }
                });
                thread.start();
            }
        }
    }

    /**
     * Phase 2 migration. If the save has a top-level GlobalKeyFrames field
     * (post-Phase-2 saves), install it directly. Otherwise scan every
     * CharacterSave in the file's folder tree, collect any non-empty per-
     * Character globals fields (camera / screen fade / screen shake) into a
     * fresh GlobalKeyFrames, and install that.
     *
     * <p>Pre-Phase-2 saves frequently put globals on a single "scene
     * controller" Character, but nothing prevented them being scattered across
     * several; concat-by-tick is the right consolidation either way. Per-
     * Character fields stay populated on the CharacterSaves (read-only) so a
     * downgraded plugin would still see them. New saves emit empty/zero
     * length arrays on each CharacterSave plus the top-level store.
     */
    private void installGlobalKeyFramesFromSave(SetupSave saveFile, FolderNodeSave folderNodeSave)
    {
        // Always mutate the plugin's existing GlobalKeyFrames instance rather
        // than replacing it -- Character.globalKeyFramesStore is a static
        // reference captured at startUp(); swapping the instance would orphan
        // it. (We could re-set the static, but mutating preserves invariants
        // simpler.)
        com.creatorskit.saves.GlobalKeyFrames target = plugin.getGlobalKeyFrames();
        com.creatorskit.saves.GlobalKeyFrames topLevel = saveFile.getGlobalKeyFrames();
        if (topLevel != null)
        {
            target.setCameraKeyFrames(topLevel.getCameraKeyFramesSafe());
            target.setScreenFadeKeyFrames(topLevel.getScreenFadeKeyFramesSafe());
            target.setScreenShakeKeyFrames(topLevel.getScreenShakeKeyFramesSafe());
            // Restore the four Area Sound slots. The save path serializes these
            // alongside camera / fade / shake via Gson on the GlobalKeyFrames
            // object, but the load path used to forget them -- the user would
            // place Area Sound 1..4 kfs, save, reload, and find them all gone.
            // Pre-4-slot saves only carried sound1KeyFrames (the others were
            // null), and the *Safe getters normalise null to an empty array,
            // so this is also forward-compatible for older save files.
            target.setSound1KeyFrames(topLevel.getSound1KeyFramesSafe());
            target.setSound2KeyFrames(topLevel.getSound2KeyFramesSafe());
            target.setSound3KeyFrames(topLevel.getSound3KeyFramesSafe());
            target.setSound4KeyFrames(topLevel.getSound4KeyFramesSafe());
            return;
        }

        java.util.ArrayList<com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame> cams = new java.util.ArrayList<>();
        java.util.ArrayList<com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame> fades = new java.util.ArrayList<>();
        java.util.ArrayList<com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame> shakes = new java.util.ArrayList<>();

        if (folderNodeSave != null)
        {
            collectLegacyGlobalsFromFolder(folderNodeSave, cams, fades, shakes);
        }
        // Older flat-list save format (no folder structure) -- everything sat
        // in saveFile.getSaves(). Still walk it to catch any leftover globals.
        CharacterSave[] flat = saveFile.getSaves();
        if (flat != null)
        {
            for (CharacterSave cs : flat)
            {
                accumulateLegacyGlobals(cs, cams, fades, shakes);
            }
        }

        target.setCameraKeyFrames(cams.toArray(new com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame[0]));
        target.setScreenFadeKeyFrames(fades.toArray(new com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame[0]));
        target.setScreenShakeKeyFrames(shakes.toArray(new com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame[0]));
    }

    private void collectLegacyGlobalsFromFolder(
            FolderNodeSave folderNodeSave,
            java.util.List<com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame> cams,
            java.util.List<com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame> fades,
            java.util.List<com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame> shakes)
    {
        CharacterSave[] saves = folderNodeSave.getCharacterSaves();
        if (saves != null)
        {
            for (CharacterSave cs : saves)
            {
                accumulateLegacyGlobals(cs, cams, fades, shakes);
            }
        }
        FolderNodeSave[] childFolders = folderNodeSave.getFolderSaves();
        if (childFolders != null)
        {
            for (FolderNodeSave child : childFolders)
            {
                collectLegacyGlobalsFromFolder(child, cams, fades, shakes);
            }
        }
    }

    private void accumulateLegacyGlobals(
            CharacterSave cs,
            java.util.List<com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame> cams,
            java.util.List<com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame> fades,
            java.util.List<com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame> shakes)
    {
        if (cs == null) return;
        com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame[] csCams = cs.getCameraKeyFrames();
        if (csCams != null)
        {
            for (com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame kf : csCams)
            {
                if (kf != null) cams.add(kf);
            }
        }
        com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame[] csFades = cs.getScreenFadeKeyFrames();
        if (csFades != null)
        {
            for (com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame kf : csFades)
            {
                if (kf != null) fades.add(kf);
            }
        }
        com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame[] csShakes = cs.getScreenShakeKeyFrames();
        if (csShakes != null)
        {
            for (com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame kf : csShakes)
            {
                if (kf != null) shakes.add(kf);
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
                // Pass MovementKeyFrames through unchanged. An earlier load
                // path ran splitMultiTileMovementKeyFrames here on the premise
                // that "path.length > 2 means old format" -- but the chained-
                // step writer in TimeSheetPanel.onAddMovementKeyPressed builds
                // multi-tile path keyframes on purpose (full pathfinder output
                // for far-away clicks), so the splitter was actively
                // corrupting valid new-format saves into per-tile sub-kfs at
                // fractional ticks -- the visible "teleporting on every tile"
                // bug. The truly-old format that splitter was designed for
                // hasn't been written by this codebase for long enough that
                // there's no real fleet to migrate; if a save genuinely
                // pre-dates the change, the user can re-author the path.
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

            // Null in pre-2.2 saves (field didn't exist) -- Gson defaults to null for
            // missing object arrays. Empty array also valid for a Character that had no
            // projectile keyframes at save time.
            if (save.getProjectileKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.PROJECTILE)] = save.getProjectileKeyFrames();
            }

            // Null in pre-2.3 saves -- same Gson-default behaviour for the three
            // bar/fade keyframe types added in 2.3.
            if (save.getShieldKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.SHIELD)] = save.getShieldKeyFrames();
            }
            if (save.getSpecialKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.SPECIAL)] = save.getSpecialKeyFrames();
            }
            if (save.getScreenFadeKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.SCREEN_FADE)] = save.getScreenFadeKeyFrames();
            }
            if (save.getScreenShakeKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.SCREEN_SHAKE)] = save.getScreenShakeKeyFrames();
            }
            if (save.getCameraKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.CAMERA)] = save.getCameraKeyFrames();
            }
            if (save.getColourKeyFrames() != null)
            {
                frames[KeyFrameType.getIndex(KeyFrameType.COLOUR)] = save.getColourKeyFrames();
            }
            // Null in saves predating the per-Character Sound keyframe (any
            // build before 2.4). Gson defaults the missing field to null
            // and the load path simply leaves the SOUND slot empty -- no
            // migration warning, the user sees an empty Sound track and
            // can add kfs as needed.
            if (save.getSoundKeyFrames() != null)
            {
                // The save's soundKeyFrames was a flat single-slot array in
                // the original ship (everything was SOUND). Saves authored
                // after the Sound 1..4 split mix SOUND / SOUND_LOCAL_2/3/4
                // entries in the same array; route each by its slot field
                // into the matching frames[] index. Entries without a
                // slot field (old saves) default to SOUND -- behaviour
                // identical to the previous single-array storage.
                java.util.Map<KeyFrameType, java.util.List<com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame>> bySlot =
                        new java.util.EnumMap<>(KeyFrameType.class);
                for (com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame sk : save.getSoundKeyFrames())
                {
                    if (sk == null) continue;
                    KeyFrameType slot = sk.getKeyFrameType();
                    if (slot != KeyFrameType.SOUND && slot != KeyFrameType.SOUND_LOCAL_2
                            && slot != KeyFrameType.SOUND_LOCAL_3 && slot != KeyFrameType.SOUND_LOCAL_4)
                    {
                        slot = KeyFrameType.SOUND;  // legacy single-slot
                    }
                    bySlot.computeIfAbsent(slot, s -> new java.util.ArrayList<>()).add(sk);
                }
                for (java.util.Map.Entry<KeyFrameType, java.util.List<com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame>> e : bySlot.entrySet())
                {
                    frames[KeyFrameType.getIndex(e.getKey())] =
                            e.getValue().toArray(new com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame[0]);
                }
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
                    false);

            // Restore both the toggle and the per-axis scales. Pre-2.2 saves have
            // these fields serialized as 0; the setters treat 0 as "no value, keep the
            // class default of 128" so old saves still render correctly.
            if (save.getRenderFixWidth() > 0)
            {
                character.setRenderFixWidth(save.getRenderFixWidth());
            }
            if (save.getRenderFixHeight() > 0)
            {
                character.setRenderFixHeight(save.getRenderFixHeight());
            }
            character.setRenderFix(save.isRenderFix());

            // Restore sub-tile offsets. Zero is the no-offset default in both the
            // save (Gson default for missing ints) and on the Character, so no
            // additional guard is needed here.
            character.setOffsetX(save.getOffsetX());
            character.setOffsetY(save.getOffsetY());
            character.setOffsetZ(save.getOffsetZ());

            // Pre-2.3 saves predate extraScale -- Gson defaults missing doubles to 0.0,
            // so treat that as "no scaling set" and use the natural 1.0.
            double savedExtraScale = save.getExtraScale();
            character.setExtraScale(savedExtraScale > 0 ? savedExtraScale : 1.0);

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
            toolBox.getModelUtilities().loadCustomModel(selectedFile);
        }
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

    // splitMultiTileMovementKeyFrames removed: its "path.length > 2 means
    // old format" heuristic was false because the chained-step writer in
    // TimeSheetPanel intentionally builds multi-tile path keyframes (full
    // pathfinder output for far-away clicks). The migration was corrupting
    // valid new-format saves into per-tile sub-kfs at fractional ticks --
    // visible as "teleporting on every tile" after a save / load round-trip.

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
                plugin.getCreatorsPanel().quickSaveToFile();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "VK_O");
        actionMap.put("VK_O", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                plugin.getCreatorsPanel().openLoadSetupDialog();
            }
        });
    }
}