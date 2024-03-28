package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ModelAnvil extends JPanel
{
    private ClientThread clientThread;
    private final Client client;
    private final CreatorsPlugin plugin;
    private final BufferedImage ICON = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private final BufferedImage DUPLICATE = ImageUtil.loadImageResource(getClass(), "/Duplicate.png");
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage ARROW_LEFT = ImageUtil.loadImageResource(getClass(), "/Arrow_Left.png");
    private final BufferedImage ARROW_RIGHT = ImageUtil.loadImageResource(getClass(), "/Arrow_Right.png");
    private final BufferedImage ARROW_DOWN = ImageUtil.loadImageResource(getClass(), "/Arrow_Down.png");
    private final BufferedImage ARROW_UP = ImageUtil.loadImageResource(getClass(), "/Arrow_Up.png");
    private final BufferedImage TRANSLATE = ImageUtil.loadImageResource(getClass(), "/Translate.png");
    private final BufferedImage ROTATE = ImageUtil.loadImageResource(getClass(), "/Rotate.png");
    private final BufferedImage TRANSLATE_SUBTILE = ImageUtil.loadImageResource(getClass(), "/Translate subtile.png");
    private final BufferedImage SCALE = ImageUtil.loadImageResource(getClass(), "/Scale.png");
    private final BufferedImage COPY_COLOURS = ImageUtil.loadImageResource(getClass(), "/Copy_Colours.png");
    private final BufferedImage PASTE_COLOURS = ImageUtil.loadImageResource(getClass(), "/Paste_Colours.png");
    private final Dimension SPINNER_DIMENSION = new Dimension(65, 25);
    private final Dimension BUTTON_DIMENSION = new Dimension(85, 25);
    @Getter
    private final ArrayList<ComplexPanel> complexPanels = new ArrayList<>();
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    private final JPanel complexMode = new JPanel();
    private final JScrollPane scrollPane = new JScrollPane();
    @Getter
    private final JCheckBox priorityCheckBox = new JCheckBox("Priority");
    @Getter
    private final JComboBox<LightingStyle> lightingComboBox = new JComboBox<>();
    @Getter
    private final JSpinner[] lightingSpinners = new JSpinner[5];
    @Getter
    private final JTextField nameField = new JTextField();
    private final GridBagConstraints c = new GridBagConstraints();
    private HashMap<Short, Short> copiedColourMap = new HashMap<>();
    private final int COMPLEX_GRID_COLUMNS = 3;

    @Inject
    public ModelAnvil(Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);

        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel modelForgeLabel = new JLabel("Model Anvil");
        modelForgeLabel.setFont(FontManager.getRunescapeBoldFont());
        modelForgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        modelForgeLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(modelForgeLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.ipady = 10;
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 0, 10, 0));
        add(buttonsPanel, c);

        nameField.setText("Name");
        nameField.setHorizontalAlignment(JTextField.CENTER);
        buttonsPanel.add(nameField);

        JButton forgeButton = new JButton("Forge");
        buttonsPanel.add(forgeButton);

        JButton forgeSetButton = new JButton("Forge & Set");
        buttonsPanel.add(forgeSetButton);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.ipady = 0;
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        add(tabbedPane, c);

        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setViewportView(complexMode);
        complexMode.setLayout(new GridLayout(0, COMPLEX_GRID_COLUMNS, 8, 8));
        complexMode.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        complexMode.setBorder(new LineBorder(ColorScheme.DARK_GRAY_COLOR, 8));
        tabbedPane.addTab("Anvil", scrollPane);

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setColumnHeaderView(headerPanel);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> createComplexPanel());
        headerPanel.add(addButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e ->
        {
            for (JPanel complexModePanel : complexPanels)
            {
                complexMode.remove(complexModePanel);
                repaint();
                revalidate();
            }

            complexPanels.clear();
        });
        headerPanel.add(clearButton);

        JPopupMenu loadPopupMenu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Load");
        menuItem.addActionListener(e -> LinkBrowser.open(MODELS_DIR.toString()));
        loadPopupMenu.add(menuItem);

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> openLoadDialog());
        loadButton.setComponentPopupMenu(loadPopupMenu);
        headerPanel.add(loadButton);

        JButton saveButton = new JButton("Save");

        headerPanel.add(saveButton);

        priorityCheckBox.setToolTipText("Use an oversimplified method of resolving render order issues (can be useful when merging many models)");
        priorityCheckBox.setFocusable(false);
        headerPanel.add(priorityCheckBox);

        JPanel lightPanel = new JPanel();
        lightPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        headerPanel.add(lightPanel);

        JSpinner ambSpinner = new JSpinner(new SpinnerNumberModel(LightingStyle.DEFAULT.getAmbient(), -1000, 1000, 1));
        JSpinner conSpinner = new JSpinner(new SpinnerNumberModel(LightingStyle.DEFAULT.getContrast(), 100, 9999, 1));
        JSpinner lightXSpinner = new JSpinner(new SpinnerNumberModel(LightingStyle.DEFAULT.getX(), -1000, 1000, 1));
        JSpinner lightYSpinner = new JSpinner((new SpinnerNumberModel(LightingStyle.DEFAULT.getY(), -1000, 1000, 1)));
        JSpinner lightZSpinner = new JSpinner((new SpinnerNumberModel(LightingStyle.DEFAULT.getZ(), -1000, 1000, 1)));
        lightingSpinners[0] = ambSpinner;
        lightingSpinners[1] = conSpinner;
        lightingSpinners[2] = lightXSpinner;
        lightingSpinners[3] = lightYSpinner;
        lightingSpinners[4] = lightZSpinner;

        ambSpinner.setToolTipText("Set the ambient lighting level");
        conSpinner.setToolTipText("Set the lighting contrast (higher is lower)");
        lightXSpinner.setToolTipText("Set the sun's x coordinate relative to the player");
        lightYSpinner.setToolTipText("Set the sun's y coordinate relative to the player");
        lightZSpinner.setToolTipText("Set the sun's z coordinate relative to the player");

        lightPanel.add(new JLabel("Ambient"));
        lightPanel.add(ambSpinner);
        lightPanel.add(new JLabel("Contrast"));
        lightPanel.add(conSpinner);

        lightPanel.add(new JLabel("x/y/z"));
        lightPanel.add(lightXSpinner);
        lightPanel.add(lightYSpinner);
        lightPanel.add(lightZSpinner);

        JComboBox<LightingStyle> presetComboBox = new JComboBox<>();
        presetComboBox.addItem(LightingStyle.PRESET);
        presetComboBox.addItem(LightingStyle.DEFAULT);
        presetComboBox.addItem(LightingStyle.ACTOR);
        presetComboBox.addItem(LightingStyle.SPOTANIM);
        presetComboBox.addItem(LightingStyle.NONE);
        presetComboBox.setFocusable(false);
        presetComboBox.setToolTipText("Quick lighting presets for common cases. Actor = NPCs/Players, SpotAnim = Spells/Effects");
        lightPanel.add(presetComboBox);

        presetComboBox.addItemListener(e ->
        {
            LightingStyle preset = (LightingStyle) presetComboBox.getSelectedItem();
            if (preset == null)
                return;

            if (preset == LightingStyle.PRESET)
                return;

            lightingSpinners[0].setValue(preset.getAmbient());
            lightingSpinners[1].setValue(preset.getContrast());
            lightingSpinners[2].setValue(preset.getX());
            lightingSpinners[3].setValue(preset.getY());
            lightingSpinners[4].setValue(preset.getZ());
        });

        forgeButton.addActionListener(e -> onForgeButtonPressed(client, nameField, false));
        forgeSetButton.addActionListener(e -> onForgeButtonPressed(client, nameField, true));

        saveButton.addActionListener(e ->
        {
            CustomLighting lighting = new CustomLighting(
                    (int) lightingSpinners[0].getValue(),
                    (int) lightingSpinners[1].getValue(),
                    (int) lightingSpinners[2].getValue(),
                    (int) lightingSpinners[3].getValue(),
                    (int) lightingSpinners[4].getValue());

            openSaveDialog(nameField.getText(), priorityCheckBox.isSelected(), LightingStyle.CUSTOM, lighting);
        });

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BorderLayout());
        sidePanel.setBorder(new EmptyBorder(0, 7, 0, 0));
        scrollPane.setRowHeaderView(sidePanel);

        JPanel cacheSearcherPanel = new JPanel();
        cacheSearcherPanel.setLayout(new GridLayout(0, 1, 4, 4));
        cacheSearcherPanel.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        cacheSearcherPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        sidePanel.add(cacheSearcherPanel, BorderLayout.NORTH);

        JPanel groupPanel = new GroupPanel(client, plugin, clientThread);
        sidePanel.add(groupPanel, BorderLayout.CENTER);

        JLabel searcherLabel = new JLabel("Cache Searcher");
        searcherLabel.setFont(FontManager.getRunescapeBoldFont());
        searcherLabel.setHorizontalAlignment(SwingConstants.CENTER);
        searcherLabel.setVerticalAlignment(SwingConstants.CENTER);
        cacheSearcherPanel.add(searcherLabel);

        JSpinner idSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
        idSpinner.setToolTipText("Choose the NPC, Object, or Item Id to find all its associated models");
        cacheSearcherPanel.add(idSpinner);

        JComboBox<CustomModelType> modelTypeComboBox = new JComboBox<>();
        modelTypeComboBox.addItem(CustomModelType.CACHE_NPC);
        modelTypeComboBox.addItem(CustomModelType.CACHE_OBJECT);
        modelTypeComboBox.addItem(CustomModelType.CACHE_GROUND_ITEM);
        modelTypeComboBox.addItem(CustomModelType.CACHE_MAN_WEAR);
        modelTypeComboBox.addItem(CustomModelType.CACHE_WOMAN_WEAR);
        modelTypeComboBox.setFocusable(false);
        modelTypeComboBox.setToolTipText("Pick which part of the cache to search");
        cacheSearcherPanel.add(modelTypeComboBox);

        JButton addModelsButton = new JButton("Add Models");
        addModelsButton.setToolTipText("Add the chosen NPC, Object, or Item as a Custom Model");
        cacheSearcherPanel.add(addModelsButton);
        addModelsButton.addActionListener(e ->
        {
            CustomModelType type = (CustomModelType) modelTypeComboBox.getSelectedItem();
            if (type == null)
                return;

            int id = (int) idSpinner.getValue();
            plugin.cacheToAnvil(type, id);
        });

        revalidate();
    }

    public void createComplexPanel()
    {
        createComplexPanel("Name", -1, 1, 0, 0, 0, 0, 0, 0, 128, 128, 128, 0, "", "", false);
    }

    public void createComplexPanel(DetailedModel dm)
    {
        createComplexPanel(dm.getName(), dm.getModelId(), dm.getGroup(), dm.getXTile(), dm.getYTile(), dm.getZTile(), dm.getXTranslate(), dm.getYTranslate(), dm.getZTranslate(), dm.getXScale(), dm.getYScale(), dm.getZScale(), dm.getRotate(), dm.getRecolourNew(), dm.getRecolourOld(), dm.isInvertFaces());
    }

    public void createComplexPanel(String name, int modelId, int group, int xTile, int yTile, int zTile, int xTranslate, int yTranslate, int zTranslate, int scaleX, int scaleY, int scaleZ, int rotate, String newColours, String oldColours, boolean invertFaces)
    {
        JSpinner modelIdSpinner = new JSpinner();
        JSpinner groupSpinner = new JSpinner();
        JTextField nameField = new JTextField(name);
        JTextField colourNewField = new JTextField();
        JTextField colourOldField = new JTextField();
        JSpinner xTileSpinner = new JSpinner();
        JSpinner yTileSpinner = new JSpinner();
        JSpinner zTileSpinner = new JSpinner();
        JSpinner xSpinner = new JSpinner();
        JSpinner ySpinner = new JSpinner();
        JSpinner zSpinner = new JSpinner();
        JSpinner xScaleSpinner = new JSpinner();
        JSpinner yScaleSpinner = new JSpinner();
        JSpinner zScaleSpinner = new JSpinner();
        JCheckBox check90 = new JCheckBox();
        JCheckBox check180 = new JCheckBox();
        JCheckBox check270 = new JCheckBox();
        JCheckBox checkInvertFaces = new JCheckBox();

        ComplexPanel complexModePanel = new ComplexPanel(
                modelIdSpinner,
                groupSpinner,
                nameField,
                colourNewField,
                colourOldField,
                xSpinner, ySpinner, zSpinner,
                xTileSpinner, yTileSpinner, zTileSpinner,
                xScaleSpinner, yScaleSpinner, zScaleSpinner,
                check90, check180, check270,
                checkInvertFaces);

        complexModePanel.setLayout(new GridBagLayout());
        complexModePanel.setBorder(new LineBorder(getBorderColour(modelId), 1));

        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1;
        c.weighty = 0;

        c.gridwidth = 4;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        nameField.setToolTipText("Name for organizational purposes only");
        complexModePanel.add(nameField, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 2;
        JPanel arrowPanel = new JPanel();
        arrowPanel.setLayout(new BorderLayout());
        complexModePanel.add(arrowPanel, c);

        JButton arrowUpButton = new JButton(new ImageIcon(ARROW_UP));
        arrowUpButton.setFocusable(false);
        arrowUpButton.setToolTipText("Move panel up");
        arrowUpButton.addActionListener(e -> setPanelIndex(complexModePanel, -COMPLEX_GRID_COLUMNS));
        arrowPanel.add(arrowUpButton, BorderLayout.PAGE_START);

        JButton arrowLeftButton = new JButton(new ImageIcon(ARROW_LEFT));
        arrowLeftButton.setFocusable(false);
        arrowLeftButton.setToolTipText("Move panel left");
        arrowLeftButton.addActionListener(e -> setPanelIndex(complexModePanel, -1));
        arrowPanel.add(arrowLeftButton, BorderLayout.LINE_START);

        JButton arrowRightButton = new JButton(new ImageIcon(ARROW_RIGHT));
        arrowRightButton.setFocusable(false);
        arrowRightButton.setToolTipText("Move panel right");
        arrowRightButton.addActionListener(e -> setPanelIndex(complexModePanel, 1));
        arrowPanel.add(arrowRightButton, BorderLayout.LINE_END);

        JButton arrowDownButton = new JButton(new ImageIcon(ARROW_DOWN));
        arrowDownButton.setFocusable(false);
        arrowDownButton.setToolTipText("Move panel up");
        arrowDownButton.addActionListener(e -> setPanelIndex(complexModePanel, COMPLEX_GRID_COLUMNS));
        arrowPanel.add(arrowDownButton, BorderLayout.PAGE_END);

        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 2;
        JPanel xyzPanel = new JPanel();
        xyzPanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(xyzPanel, c);

        JLabel xLabel = new JLabel("x:");
        xLabel.setToolTipText("East/West");
        xLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(xLabel);

        JLabel yLabel = new JLabel("y:");
        yLabel.setToolTipText("North/South");
        yLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(yLabel);

        JLabel zLabel = new JLabel("z:");
        zLabel.setToolTipText("Up/Down");
        zLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(zLabel);

        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 3;
        JLabel colourLabel = new JLabel("Colours:");
        colourLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        colourLabel.setToolTipText("Choose new Colours to replace default Colours");
        complexModePanel.add(colourLabel, c);

        c.gridwidth = 2;
        c.gridx = 4;
        c.gridy = 0;
        SpinnerNumberModel modelIdModel = new SpinnerNumberModel(modelId, -1, 99999, 1);
        modelIdSpinner.setModel(modelIdModel);
        modelIdSpinner.setBackground((modelId == -1) ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
        modelIdSpinner.setToolTipText("Set the id of the model you want to draw from the cache");
        modelIdSpinner.addChangeListener(e ->
        {
            complexModePanel.setBorder(new LineBorder(getBorderColour((int) modelIdSpinner.getValue()), 1));
            modelIdSpinner.setBackground(((int) modelIdSpinner.getValue() == -1) ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
        });
        complexModePanel.add(modelIdSpinner, c);

        c.gridwidth = 2;
        c.gridx = 6;
        c.gridy = 0;
        SpinnerNumberModel groupSpinnerNumberModel = new SpinnerNumberModel(group, 1, 99, 1);
        groupSpinner.setModel(groupSpinnerNumberModel);
        groupSpinner.setToolTipText("Set the group for the purpose of Group Transforms");
        groupSpinner.setBackground(getBorderColour(group * 6));
        complexModePanel.add(groupSpinner, c);
        groupSpinner.addChangeListener(e ->
        {
            int colourPick = ((int) groupSpinner.getValue()) * 6;
            groupSpinner.setBackground(getBorderColour(colourPick));
        });

        c.gridx = 8;
        c.gridy = 0;
        c.gridwidth = 1;
        JButton duplicateButton = new JButton(new ImageIcon(DUPLICATE));
        duplicateButton.setFocusable(false);
        duplicateButton.setToolTipText("Duplicate panel");
        duplicateButton.setPreferredSize(new Dimension(30, 25));
        complexModePanel.add(duplicateButton, c);

        c.gridx = 9;
        c.gridy = 0;
        c.gridwidth = 1;
        JButton removeButton = new JButton(new ImageIcon(CLOSE));
        removeButton.setFocusable(false);
        removeButton.setToolTipText("Remove panel");
        removeButton.setPreferredSize(new Dimension(30, 25));
        complexModePanel.add(removeButton, c);

        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel tileLabel = new JLabel(new ImageIcon(TRANSLATE));
        tileLabel.setToolTipText("Translate by full tile");
        tileLabel.setBackground(Color.BLACK);
        complexModePanel.add(tileLabel, c);

        c.gridx = 4;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel translateLabel = new JLabel(new ImageIcon(TRANSLATE_SUBTILE));
        translateLabel.setToolTipText("Translate by sub-tile (1/128 of a tile)");
        translateLabel.setBackground(Color.BLACK);
        complexModePanel.add(translateLabel, c);

        c.gridx = 6;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel scaleLabel = new JLabel(new ImageIcon(SCALE));
        scaleLabel.setToolTipText("Scale. 128 is default scale");
        scaleLabel.setBackground(Color.BLACK);
        complexModePanel.add(scaleLabel, c);

        c.gridx = 8;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel rotateLabel = new JLabel(new ImageIcon(ROTATE));
        rotateLabel.setToolTipText("Rotate");
        rotateLabel.setBackground(Color.BLACK);
        complexModePanel.add(rotateLabel, c);

        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 2;
        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(tilePanel, c);

        xTileSpinner.setValue(xTile);
        xTileSpinner.setToolTipText("E/W");
        xTileSpinner.setPreferredSize(SPINNER_DIMENSION);
        tilePanel.add(xTileSpinner);

        yTileSpinner.setValue(yTile);
        yTileSpinner.setToolTipText("N/S");
        tilePanel.add(yTileSpinner);

        zTileSpinner.setValue(zTile);
        zTileSpinner.setToolTipText("U/D");
        tilePanel.add(zTileSpinner);

        c.gridx = 4;
        c.gridy = 2;
        c.gridwidth = 2;
        JPanel translatePanel = new JPanel();
        translatePanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(translatePanel, c);

        xSpinner.setValue(xTranslate);
        xSpinner.setToolTipText("E/W");
        xSpinner.setPreferredSize(SPINNER_DIMENSION);
        translatePanel.add(xSpinner);

        ySpinner.setValue(yTranslate);
        ySpinner.setToolTipText("N/S");
        translatePanel.add(ySpinner);

        zSpinner.setValue(zTranslate);
        zSpinner.setToolTipText("U/D");
        translatePanel.add(zSpinner);

        c.gridx = 6;
        c.gridy = 2;
        c.gridwidth = 2;
        JPanel scalePanel = new JPanel();
        scalePanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(scalePanel, c);

        xScaleSpinner.setValue(scaleX);
        xScaleSpinner.setPreferredSize(SPINNER_DIMENSION);
        xScaleSpinner.setToolTipText("E/W");
        scalePanel.add(xScaleSpinner);

        yScaleSpinner.setValue(scaleY);
        yScaleSpinner.setToolTipText("N/S");
        scalePanel.add(yScaleSpinner);

        zScaleSpinner.setValue(scaleZ);
        zScaleSpinner.setToolTipText("U/D");
        scalePanel.add(zScaleSpinner);

        c.gridx = 8;
        c.gridy = 2;
        c.gridwidth = 2;
        JPanel rotatePanel = new JPanel();
        rotatePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        rotatePanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(rotatePanel, c);

        check90.setText("90°");
        check90.setHorizontalAlignment(SwingConstants.LEFT);
        check90.setFocusable(false);
        rotatePanel.add(check90);

        check180.setText("180°");
        check180.setHorizontalAlignment(SwingConstants.LEFT);
        check180.setFocusable(false);
        rotatePanel.add(check180);

        check270.setText("270°");
        check270.setHorizontalAlignment(SwingConstants.LEFT);
        check270.setFocusable(false);
        rotatePanel.add(check270);

        check90.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
                check180.setSelected(false);
                check270.setSelected(false);
            }
        });

        check180.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
                check90.setSelected(false);
                check270.setSelected(false);
            }
        });

        check270.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
                check180.setSelected(false);
                check90.setSelected(false);
            }
        });

        switch (rotate)
        {
            case 0:
                break;
            case 1:
                check270.setSelected(true);
                break;
            case 2:
                check180.setSelected(true);
                break;
            case 3:
                check90.setSelected(true);
        }

        HashMap<Short, Short> colourMap = new HashMap<>();
        if (!newColours.equals("") && !oldColours.equals(""))
        {
            try
            {
                String[] newCols = newColours.split(",");
                String[] oldCols = oldColours.split(",");

                for (int i = 0; i < newCols.length; i++)
                {
                    Short newCol = Short.parseShort(newCols[i]);
                    Short oldCol = Short.parseShort(oldCols[i]);
                    colourMap.put(oldCol, newCol);
                }
            }
            catch (Exception e)
            {
            }
        }

        colourNewField.setText(newColours);
        colourNewField.setVisible(false);
        complexModePanel.add(colourNewField);

        colourOldField.setText(oldColours);
        colourOldField.setVisible(false);
        complexModePanel.add(colourOldField);

        JButton clearColoursButton = new JButton("Clear (" + colourMap.size() + ")");

        JFrame swapperFrame = new JFrame("Colour Swapper: " + nameField.getText());
        swapperFrame.setVisible(false);
        swapperFrame.setEnabled(false);
        swapperFrame.setIconImage(ICON);
        swapperFrame.setLayout(new BorderLayout());

        JScrollPane colourScrollPane = new JScrollPane();
        swapperFrame.add(colourScrollPane, BorderLayout.LINE_START);

        JPanel gridMenu = new JPanel();
        gridMenu.setLayout(new GridLayout(0, 2, 2, 2));
        colourScrollPane.setViewportView(gridMenu);

        JPanel colourButtons = new JPanel();
        colourScrollPane.setColumnHeaderView(colourButtons);

        JButton copyColourButton = new JButton("Copy");
        copyColourButton.setFocusable(false);
        copyColourButton.setToolTipText("Copy New Colours");
        copyColourButton.addActionListener(e -> copiedColourMap = colourMap);
        colourButtons.add(copyColourButton);

        JButton pasteColourButton = new JButton("Paste");
        pasteColourButton.setFocusable(false);
        pasteColourButton.setToolTipText("Paste copied New Colours");
        colourButtons.add(pasteColourButton);

        JColorChooser colorChooser = new JColorChooser();
        swapperFrame.add(colorChooser, BorderLayout.LINE_END);

        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 2;
        JButton colourSwapper = new JButton("Swap");
        colourSwapper.setFocusable(false);
        colourSwapper.setToolTipText("Opens an interface to swap colours on this model");
        colourSwapper.setPreferredSize(BUTTON_DIMENSION);
        complexModePanel.add(colourSwapper, c);
        colourSwapper.addActionListener(e ->
                setupColourSwapper(swapperFrame, gridMenu, nameField, modelIdSpinner, colourMap, colorChooser, colourNewField, colourOldField, clearColoursButton));

        pasteColourButton.addActionListener(e ->
        {
            colourMap.clear();
            colourMap.putAll(copiedColourMap);
            String[] mapToCSV = hashmapToCSV(colourMap);
            colourNewField.setText(mapToCSV[1]);
            colourOldField.setText(mapToCSV[0]);
            setupColourSwapper(swapperFrame, gridMenu, nameField, modelIdSpinner, colourMap, colorChooser, colourNewField, colourOldField, clearColoursButton);
        });

        c.gridx = 4;
        c.gridy = 3;
        c.gridwidth = 1;
        JButton copyColourButtonMain = new JButton(new ImageIcon(COPY_COLOURS));
        copyColourButtonMain.setFocusable(false);
        copyColourButtonMain.setToolTipText("Copy New Colours");
        copyColourButtonMain.addActionListener(e -> copiedColourMap = colourMap);
        complexModePanel.add(copyColourButtonMain, c);

        c.gridx = 5;
        c.gridy = 3;
        c.gridwidth = 1;
        JButton pasteColourButtonMain = new JButton(new ImageIcon(PASTE_COLOURS));
        pasteColourButtonMain.setFocusable(false);
        pasteColourButtonMain.setToolTipText("Paste copied New Colours");
        complexModePanel.add(pasteColourButtonMain, c);

        c.gridx = 6;
        c.gridy = 3;
        c.gridwidth = 2;
        clearColoursButton.setFocusable(false);
        clearColoursButton.setToolTipText("Clears all swapped colours");
        clearColoursButton.setPreferredSize(BUTTON_DIMENSION);
        complexModePanel.add(clearColoursButton, c);
        clearColoursButton.addActionListener(e ->
        {
            for (Component component : gridMenu.getComponents())
            {
                if (component instanceof JButton)
                {
                    JButton button = (JButton) component;
                    if (button.getText().equals("Unset"))
                    {
                        button.setBorder(new EmptyBorder(4, 4, 4, 4));
                        button.setText("Set");
                    }
                }
            }

            colourMap.clear();
            colourNewField.setText("");
            colourOldField.setText("");
            clearColoursButton.setText("Clear (0)");

            swapperFrame.revalidate();
            swapperFrame.repaint();
            swapperFrame.pack();
            revalidate();
            repaint();
        });

        pasteColourButtonMain.addActionListener(e ->
        {
            colourMap.clear();
            colourMap.putAll(copiedColourMap);
            String[] mapToCSV = hashmapToCSV(colourMap);
            colourNewField.setText(mapToCSV[1]);
            colourOldField.setText(mapToCSV[0]);
            clearColoursButton.setText("Clear (" + colourMap.size() + ")");
            if (swapperFrame.isVisible())
                setupColourSwapper(swapperFrame, gridMenu, nameField, modelIdSpinner, colourMap, colorChooser, colourNewField, colourOldField, clearColoursButton);
        });

        c.gridx = 8;
        c.gridy = 3;
        c.gridwidth = 1;
        checkInvertFaces.setText("Invert");
        checkInvertFaces.setToolTipText("Inverts all faces. Should be used with scaling the x or y dimensions in the negative direction");
        checkInvertFaces.setSelected(invertFaces);
        checkInvertFaces.setHorizontalAlignment(SwingConstants.LEFT);
        checkInvertFaces.setFocusable(false);
        complexModePanel.add(checkInvertFaces, c);

        duplicateButton.addActionListener(e ->
        {
            int rotation = 0;
            if (check270.isSelected())
            {
                rotation = 1;
            }

            if (check180.isSelected())
            {
                rotation = 2;
            }

            if (check90.isSelected())
            {
                rotation = 3;
            }

            String[] colourSwaps = hashmapToCSV(colourMap);

            createComplexPanel
                    (nameField.getText(),
                            (int) modelIdSpinner.getValue(),
                            (int) groupSpinner.getValue(),
                            (int) xTileSpinner.getValue(),
                            (int) yTileSpinner.getValue(),
                            (int) zTileSpinner.getValue(),
                            (int) xSpinner.getValue(),
                            (int) ySpinner.getValue(),
                            (int) zSpinner.getValue(),
                            (int) xScaleSpinner.getValue(),
                            (int) yScaleSpinner.getValue(),
                            (int) zScaleSpinner.getValue(),
                            rotation,
                            colourSwaps[1],
                            colourSwaps[0],
                            checkInvertFaces.isSelected()
                    );
        });

        removeButton.addActionListener(e ->
        {
            swapperFrame.setVisible(false);
            swapperFrame.setEnabled(false);
            complexMode.remove(complexModePanel);
            complexPanels.remove(complexModePanel);
            repaint();
            revalidate();
        });

        nameField.addActionListener(e ->
        {
            nameField.setText(nameField.getText().replaceAll("=", ""));
            swapperFrame.setTitle("Colour Swapper: " + nameField.getText());
        });

        JSpinner[] spinners = new JSpinner[]{xSpinner, ySpinner, zSpinner, xScaleSpinner, yScaleSpinner, zScaleSpinner, xTileSpinner, yTileSpinner, zTileSpinner};
        for (JSpinner spinner : spinners)
        {
            JSpinner.DefaultEditor defaultEditor = (JSpinner.DefaultEditor) spinner.getEditor();
            defaultEditor.getTextField().addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (SwingUtilities.isRightMouseButton(e))
                    {
                        int i = 0;
                        if (spinner == xScaleSpinner || spinner == yScaleSpinner || spinner == zScaleSpinner)
                            i = 128;
                        spinner.setValue(i);
                    }
                }
            });
        }

        complexMode.add(complexModePanel);
        complexModePanel.setEnabled(true);
        complexModePanel.setVisible(true);
        complexPanels.add(complexModePanel);

        revalidate();
        repaint();
    }

    private void setPanelIndex(ComplexPanel panel, int change)
    {
        int newPosition = complexMode.getComponentZOrder(panel) + change;
        if (newPosition < 0)
            newPosition = 0;

        if (newPosition >= complexPanels.size())
            newPosition = complexPanels.size() - 1;

        complexMode.setComponentZOrder(panel, newPosition);
        complexPanels.remove(panel);
        complexPanels.add(newPosition, panel);

        repaint();
        revalidate();
    }

    private void onForgeButtonPressed(Client client, JTextField nameField, boolean forgeAndSet)
    {
        CustomLighting lighting = new CustomLighting(
                (int) lightingSpinners[0].getValue(),
                (int) lightingSpinners[1].getValue(),
                (int) lightingSpinners[2].getValue(),
                (int) lightingSpinners[3].getValue(),
                (int) lightingSpinners[4].getValue());

        if (lighting.getX() == 0 && lighting.getY() == 0 && lighting.getZ() == 0)
            lighting.setZ(1);

        forgeModel(client, nameField, priorityCheckBox.isSelected(), lighting, forgeAndSet);
    }

    private void forgeModel(Client client, JTextField nameField, boolean setPriority, CustomLighting lighting, boolean forgeAndSet)
    {
        if (client == null)
        {
            return;
        }

        if (complexPanels.isEmpty())
            return;

        clientThread.invokeLater(() ->
        {
            DetailedModel[] detailedModels = panelsToDetailedModels();
            Model model = forgeComplexModel(setPriority, detailedModels, LightingStyle.CUSTOM, lighting);
            if (model == null)
            {
                return;
            }

            CustomModelComp comp = new CustomModelComp(plugin.getStoredModels().size(), CustomModelType.FORGED, -1, null, null, detailedModels, null, LightingStyle.CUSTOM, lighting, setPriority, nameField.getText());
            CustomModel customModel = new CustomModel(model, comp);
            plugin.addCustomModel(customModel, forgeAndSet);
        });
    }

    public Model forgeComplexModel(boolean setPriority, DetailedModel[] detailedModels, LightingStyle lightingStyle, CustomLighting lighting)
    {
        return plugin.createComplexModel(detailedModels, setPriority, lightingStyle, lighting);
    }

    private DetailedModel[] panelsToDetailedModels()
    {
        DetailedModel[] detailedModels = new DetailedModel[complexPanels.size()];

        for (int i = 0; i < complexPanels.size(); i++) {
            ComplexPanel complexModePanel = complexPanels.get(i);
            String name = complexModePanel.getNameField().getText();
            int modelId = (int) complexModePanel.getModelIdSpinner().getValue();
            int group = (int) complexModePanel.getGroupSpinner().getValue();
            int xTile = (int) complexModePanel.getXTileSpinner().getValue();
            int yTile = (int) complexModePanel.getYTileSpinner().getValue();
            int zTile = (int) complexModePanel.getZTileSpinner().getValue();
            int xTranslate = (int) complexModePanel.getXSpinner().getValue();
            int yTranslate = (int) complexModePanel.getYSpinner().getValue();
            int zTranslate = (int) complexModePanel.getZSpinner().getValue();
            int xScale = (int) complexModePanel.getXScaleSpinner().getValue();
            int yScale = (int) complexModePanel.getYScaleSpinner().getValue();
            int zScale = (int) complexModePanel.getZScaleSpinner().getValue();
            int rotate = 0;
            String recolourNew = complexModePanel.getColourNewField().getText();
            String recolourOld = complexModePanel.getColourOldField().getText();
            boolean invertFaces = complexModePanel.getInvertFaces().isSelected();

            JCheckBox check90 = complexModePanel.getCheck90();
            JCheckBox check180 = complexModePanel.getCheck180();
            JCheckBox check270 = complexModePanel.getCheck270();

            if (check90.isSelected())
                rotate = 3;

            if (check180.isSelected())
                rotate = 2;

            if (check270.isSelected())
                rotate = 1;

            DetailedModel detailedModel = new DetailedModel(name, modelId, group, xTile, yTile, zTile, xTranslate, yTranslate, zTranslate, xScale, yScale, zScale, rotate, recolourNew, recolourOld, invertFaces);
            detailedModels[i] = detailedModel;
        }

        return detailedModels;
    }

    private void openSaveDialog(String name, boolean priority, LightingStyle lightingStyle, CustomLighting lighting)
    {
        File outputDir = MODELS_DIR;
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
        fileChooser.setSelectedFile(new File(name));
        fileChooser.setDialogTitle("Save current model collection");

        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().endsWith(".json"))
            {
                selectedFile = new File(selectedFile.getPath() + ".json");
            }
            saveToFile(selectedFile, name, priority, lightingStyle, lighting);
        }
    }

    public void saveToFile(File file, String name, boolean priority, LightingStyle lightingStyle, CustomLighting lighting)
    {
        try {
            FileWriter writer = new FileWriter(file, false);

            DetailedModel[] detailedModels = panelsToDetailedModels();
            CustomModelComp comp = new CustomModelComp(0, CustomModelType.FORGED, -1, null, null, detailedModels, null, lightingStyle, lighting, priority, name);
            String string = plugin.getGson().toJson(comp);
            writer.write(string);
            writer.close();
        }
        catch (IOException e)
        {
            log.debug("Error when saving Forge model to file");
        }
    }

    private void openLoadDialog()
    {
        MODELS_DIR.mkdirs();

        JFileChooser fileChooser = new JFileChooser(MODELS_DIR);
        fileChooser.setDialogTitle("Choose a model to load");

        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.exists())
            {
                selectedFile = new File(selectedFile.getPath() + ".json");
                if (!selectedFile.exists())
                {
                    plugin.sendChatMessage("Could not find the requested Custom Model file.");
                    return;
                }
            }

            plugin.loadCustomModelToAnvil(selectedFile, priorityCheckBox.isSelected(), nameField.getText());
        }
    }

    private void setupColourSwapper(JFrame swapperFrame, JPanel gridMenu, JTextField nameField, JSpinner modelIdSpinner, HashMap<Short, Short> colourMap, JColorChooser colorChooser, JTextField colourNewField, JTextField colourOldField, JButton clearColoursButton)
    {
        swapperFrame.setVisible(true);
        swapperFrame.setEnabled(true);
        swapperFrame.setTitle("Colour Swapper: " + nameField.getText());
        gridMenu.removeAll();

        clientThread.invokeLater(() ->
        {
            ModelData md = client.loadModelData((int) modelIdSpinner.getValue());
            if (md == null)
            {
                return;
            }

            ArrayList<Short> list = new ArrayList<>();
            for (short s : md.getFaceColors())
            {
                if (!list.contains(s))
                {
                    list.add(s);
                }
            }

            SwingUtilities.invokeLater(() ->
            {
                JLabel oldColourTitle = new JLabel("Old Colours");
                oldColourTitle.setFont(FontManager.getRunescapeBoldFont());
                oldColourTitle.setToolTipText("Default colours of the model");
                oldColourTitle.setHorizontalAlignment(SwingConstants.CENTER);
                oldColourTitle.setVerticalAlignment(SwingConstants.BOTTOM);
                gridMenu.add(oldColourTitle);

                JLabel newColourTitle = new JLabel("New Colours");
                newColourTitle.setToolTipText("Pick colours to replace Old Colours");
                newColourTitle.setFont(FontManager.getRunescapeBoldFont());
                newColourTitle.setHorizontalAlignment(SwingConstants.CENTER);
                newColourTitle.setVerticalAlignment(SwingConstants.BOTTOM);
                gridMenu.add(newColourTitle);

                for (int i = 0; i < list.size(); i++)
                {
                    short s = list.get(i);
                    JLabel oldColour = new JLabel(s + "", JLabel.CENTER);
                    oldColour.setFont(FontManager.getRunescapeBoldFont());
                    oldColour.setOpaque(true);
                    oldColour.setBackground(Color.BLACK);
                    Color color = colorFromShort(s);
                    oldColour.setBorder(new LineBorder(color, 12));
                    gridMenu.add(oldColour);

                    JButton newColour = new JButton();
                    newColour.setName("newColour");
                    newColour.setFocusable(false);
                    newColour.setFont(FontManager.getRunescapeBoldFont());
                    newColour.setText("Set");
                    gridMenu.add(newColour);
                    if (colourMap.containsKey(s))
                    {
                        newColour.setBorder(new LineBorder(colorFromShort(colourMap.get(s)), 12));
                        newColour.setText("Unset");
                    }

                    newColour.addActionListener(f ->
                    {
                        if (newColour.getText().equals("Unset"))
                        {
                            colourMap.remove(s);
                            newColour.setBorder(new EmptyBorder(4, 4, 4, 4));
                            newColour.setText("Set");
                        }
                        else
                        {
                            Color colourToAdd = colorChooser.getColor();
                            colourMap.put(s, shortFromColour(colourToAdd));
                            newColour.setBorder(new LineBorder(colourToAdd, 12));
                            newColour.setText("Unset");
                        }

                        String[] mapToCSV = hashmapToCSV(colourMap);
                        colourNewField.setText(mapToCSV[1]);
                        colourOldField.setText(mapToCSV[0]);
                        clearColoursButton.setText("Clear (" + colourMap.size() + ")");

                        swapperFrame.revalidate();
                        swapperFrame.repaint();
                        revalidate();
                        repaint();
                    });
                }

                swapperFrame.revalidate();
                swapperFrame.repaint();
                swapperFrame.pack();
                revalidate();
                repaint();
            });
        });

        clearColoursButton.setText("Clear (" + colourMap.size() + ")");
        swapperFrame.revalidate();
        swapperFrame.repaint();
        swapperFrame.pack();
        revalidate();
        repaint();
    }

    public static String[] hashmapToCSV(HashMap<Short, Short> map)
    {
        if (map.isEmpty())
        {
            return new String[]{"", ""};
        }

        StringBuilder oldBuilder = new StringBuilder();
        StringBuilder newBuilder = new StringBuilder();
        for (Map.Entry<Short, Short> entry : map.entrySet())
        {
            short oldColour = entry.getKey();
            short newColour = entry.getValue();

            oldBuilder.append(oldColour).append(",");
            newBuilder.append(newColour).append(",");
        }

        //remove final comma
        oldBuilder.deleteCharAt(oldBuilder.length() - 1);
        newBuilder.deleteCharAt(newBuilder.length() - 1);
        return new String[]{oldBuilder.toString(), newBuilder.toString()};
    }

    public static Color colorFromShort(short s)
    {
        float hue = (float) JagexColor.unpackHue(s) / JagexColor.HUE_MAX;
        float sat = (float) JagexColor.unpackSaturation(s) / JagexColor.SATURATION_MAX;
        float lum = (float) JagexColor.unpackLuminance(s) / JagexColor.LUMINANCE_MAX;
        int[] rgb = hslToRgb(hue, sat, lum);
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    public static short shortFromColour(Color color)
    {
        float[] col = rgbToHsl(color.getRed(), color.getGreen(), color.getBlue());
        int hue = (int) (col[0] * JagexColor.HUE_MAX);
        int sat = (int) (col[1] * JagexColor.SATURATION_MAX);
        int lum = (int) (col[2] * JagexColor.LUMINANCE_MAX);
        return JagexColor.packHSL(hue, sat, lum);
    }

    public static Color getBorderColour(int i)
    {
        if (i == -1)
        {
            return ColorScheme.MEDIUM_GRAY_COLOR;
        }

        float hue = ((float) i - 25) / 50;
        return Color.getHSBColor(hue, 1, (float) 0.7);
    }

    public static int[] hslToRgb(float h, float s, float l){
        float r, g, b;

        if (s == 0f) {
            r = g = b = l; // achromatic
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hueToRgb(p, q, h + 1f/3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f/3f);
        }
        return new int[]{to255(r), to255(g), to255(b)};
    }

    public static float[] rgbToHsl(int red, int green, int blue) {
        float r = red / 255f;
        float g = green / 255f;
        float b = blue / 255f;

        float max = (r > g && r > b) ? r : (g > b) ? g : b;
        float min = (r < g && r < b) ? r : (g < b) ? g : b;

        float h, s, l;
        l = (max + min) / 2.0f;

        if (max == min) {
            h = s = 0.0f;
        } else {
            float d = max - min;
            s = (l > 0.5f) ? d / (2.0f - max - min) : d / (max + min);

            if (r > g && r > b)
                h = (g - b) / d + (g < b ? 6.0f : 0.0f);

            else if (g > b)
                h = (b - r) / d + 2.0f;

            else
                h = (r - g) / d + 4.0f;

            h /= 6.0f;
        }
        return new float[]{h, s, l};
    }

    public static int to255(float v) { return (int)Math.min(255,256*v); }

    public static float hueToRgb(float p, float q, float t) {
        if (t < 0f)
            t += 1f;
        if (t > 1f)
            t -= 1f;
        if (t < 1f/6f)
            return p + (q - p) * 6f * t;
        if (t < 1f/2f)
            return q;
        if (t < 2f/3f)
            return p + (q - p) * (2f/3f - t) * 6f;
        return p;
    }
}
