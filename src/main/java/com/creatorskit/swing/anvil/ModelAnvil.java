package com.creatorskit.swing.anvil;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.*;
import com.creatorskit.swing.StringHandler;
import com.creatorskit.swing.colours.ColourSwapPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

@Slf4j
public class ModelAnvil extends JPanel
{
    private ClientThread clientThread;
    private final Client client;
    private final CreatorsPlugin plugin;
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
    private final Dimension LIGHT_DIMENSION = new Dimension(70, 25);
    @Getter
    private final JSpinner[] lightingSpinners = new JSpinner[5];
    private final JComboBox<LightingStyle> presetComboBox = new JComboBox<>();
    private final JSpinner ambSpinner = new JSpinner(new SpinnerNumberModel(LightingStyle.DEFAULT.getAmbient(), -1000, 1000, 1));
    private final JSpinner conSpinner = new JSpinner(new SpinnerNumberModel(LightingStyle.DEFAULT.getContrast(), 100, 9999, 1));
    private final JSpinner lightXSpinner = new JSpinner(new SpinnerNumberModel(LightingStyle.DEFAULT.getX(), -1000, 1000, 1));
    private final JSpinner lightYSpinner = new JSpinner((new SpinnerNumberModel(LightingStyle.DEFAULT.getY(), -1000, 1000, 1)));
    private final JSpinner lightZSpinner = new JSpinner((new SpinnerNumberModel(LightingStyle.DEFAULT.getZ(), -1000, 1000, 1)));
    private final Dimension BUTTON_DIMENSION = new Dimension(85, 25);
    @Getter
    private final ArrayList<ComplexPanel> complexPanels = new ArrayList<>();
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    private final JPanel complexMode = new JPanel();
    private final JScrollPane scrollPane = new JScrollPane();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    @Getter
    private final JCheckBox priorityCheckBox = new JCheckBox("Priority");
    @Getter
    private final JTextField nameField = new JTextField();
    private final ColourSwapPanel colourSwapPanel;
    private final GridBagConstraints c = new GridBagConstraints();
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
        nameField.addActionListener(e -> nameField.setText(StringHandler.cleanString(nameField.getText())));
        nameField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                nameField.setText(StringHandler.cleanString(nameField.getText()));
            }
        });
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
        tabbedPane.setBorder(new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        add(tabbedPane, c);

        JPanel viewport = new JPanel();
        viewport.setLayout(new GridBagLayout());
        viewport.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setViewportView(viewport);
        scrollPane.setViewportBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 8));
        tabbedPane.addTab("Model Anvil", scrollPane);

        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        complexMode.setLayout(new GridLayout(0, COMPLEX_GRID_COLUMNS, 8, 8));
        complexMode.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        viewport.add(complexMode, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        JLabel emptyLabel = new JLabel("");
        viewport.add(emptyLabel, c);

        colourSwapPanel = new ColourSwapPanel(client, clientThread, complexPanels);
        tabbedPane.addTab("Colour/Texture Swapper", colourSwapPanel);

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
                colourSwapPanel.removeAllComplexPanelOptions();
            }

            repaint();
            revalidate();
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

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new GridBagLayout());
        scrollPane.setRowHeaderView(sidePanel);

        JPanel lightPanel = new JPanel();
        lightPanel.setLayout(new GridBagLayout());
        lightPanel.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));

        lightingSpinners[0] = ambSpinner;
        lightingSpinners[1] = conSpinner;
        lightingSpinners[2] = lightXSpinner;
        lightingSpinners[3] = lightYSpinner;
        lightingSpinners[4] = lightZSpinner;

        ambSpinner.setToolTipText("<html>Set the ambient lighting level<br>Range: -1000 to 1000</html>");
        conSpinner.setToolTipText("<html>Set the lighting contrast (higher is lower)<br>Range: 100 to 9999</html>");
        lightXSpinner.setToolTipText("<html>Set the sun's x coordinate relative to the player<br>Range: -1000 to 1000</html>");
        lightYSpinner.setToolTipText("<html>Set the sun's y coordinate relative to the player<br>Range: -1000 to 1000</html>");
        lightZSpinner.setToolTipText("<html>Set the sun's z coordinate relative to the player<br>Range: -1000 to 1000</html>");

        presetComboBox.addItem(LightingStyle.DEFAULT);
        presetComboBox.addItem(LightingStyle.ACTOR);
        presetComboBox.addItem(LightingStyle.SPOTANIM);
        presetComboBox.addItem(LightingStyle.DYNAMIC);
        presetComboBox.addItem(LightingStyle.NONE);
        presetComboBox.setFocusable(false);
        presetComboBox.setToolTipText("Quick lighting presets for common cases. Actor = NPCs/Players, SpotAnim = Spells/Effects");

        presetComboBox.addItemListener(e ->
        {
            LightingStyle preset = (LightingStyle) presetComboBox.getSelectedItem();
            if (preset == null)
                return;

            lightingSpinners[0].setValue(preset.getAmbient());
            lightingSpinners[1].setValue(preset.getContrast());
            lightingSpinners[2].setValue(preset.getX());
            lightingSpinners[3].setValue(preset.getY());
            lightingSpinners[4].setValue(preset.getZ());
        });

        c.weightx = 1;
        c.weighty = 0;
        c.insets = new Insets(4, 0, 4, 0);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        JLabel lightingLabel = new JLabel("Lighting Settings", SwingConstants.CENTER);
        lightingLabel.setFont(FontManager.getRunescapeBoldFont());
        lightPanel.add(lightingLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        lightPanel.add(new JLabel("Pre ", SwingConstants.RIGHT), c);

        c.gridx = 1;
        c.gridy = 1;
        presetComboBox.setPreferredSize(LIGHT_DIMENSION);
        lightPanel.add(presetComboBox, c);

        c.gridx = 0;
        c.gridy = 2;
        lightPanel.add(new JLabel("Amb ", SwingConstants.RIGHT), c);

        c.gridx = 0;
        c.gridy = 3;
        lightPanel.add(new JLabel("Con ", SwingConstants.RIGHT), c);

        c.gridx = 1;
        c.gridy = 2;
        ambSpinner.setPreferredSize(LIGHT_DIMENSION);
        lightPanel.add(ambSpinner, c);

        c.gridx = 1;
        c.gridy = 3;
        conSpinner.setPreferredSize(LIGHT_DIMENSION);
        lightPanel.add(conSpinner, c);

        c.gridx = 2;
        c.gridy = 1;
        lightPanel.add(new JLabel("x ", SwingConstants.RIGHT), c);

        c.gridx = 2;
        c.gridy = 2;
        lightPanel.add(new JLabel("y ", SwingConstants.RIGHT), c);

        c.gridx = 2;
        c.gridy = 3;
        lightPanel.add(new JLabel("z ", SwingConstants.RIGHT), c);

        c.gridx = 3;
        c.gridy = 1;
        lightXSpinner.setPreferredSize(LIGHT_DIMENSION);
        lightPanel.add(lightXSpinner, c);

        c.gridx = 3;
        c.gridy = 2;
        lightYSpinner.setPreferredSize(LIGHT_DIMENSION);
        lightPanel.add(lightYSpinner, c);

        c.gridx = 3;
        c.gridy = 3;
        lightZSpinner.setPreferredSize(LIGHT_DIMENSION);
        lightPanel.add(lightZSpinner, c);

        JPanel groupPanel = new GroupPanel(client, plugin, clientThread);

        c.insets = new Insets(8, 8, 8, 8);

        c.gridx = 0;
        c.gridy = 0;
        sidePanel.add(lightPanel, c);

        c.gridx = 0;
        c.gridy = 1;
        sidePanel.add(groupPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weighty = 1;
        sidePanel.add(new JLabel(""), c);

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

        revalidate();
    }

    public void createComplexPanel()
    {
        createComplexPanel(
                "Name",
                -1,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                128,
                128,
                128,
                0,
                "",
                "",
                new short[0],
                new short[0],
                new short[0],
                new short[0],
                false);
    }

    public void createComplexPanel(DetailedModel dm)
    {
        createComplexPanel(
                dm.getName(),
                dm.getModelId(),
                dm.getGroup(),
                dm.getXTile(),
                dm.getYTile(),
                dm.getZTile(),
                dm.getXTranslate(),
                dm.getYTranslate(),
                dm.getZTranslate(),
                dm.getXScale(),
                dm.getYScale(),
                dm.getZScale(),
                dm.getRotate(),
                dm.getRecolourNew(),
                dm.getRecolourOld(),
                dm.getColoursFrom(),
                dm.getColoursTo(),
                dm.getTexturesFrom(),
                dm.getTexturesTo(),
                dm.isInvertFaces());
    }

    public void createComplexPanel(
            String name,
            int modelId,
            int group,
            int xTile,
            int yTile,
            int zTile,
            int xTranslate,
            int yTranslate,
            int zTranslate,
            int scaleX,
            int scaleY,
            int scaleZ,
            int rotate,
            String newColours,
            String oldColours,
            short[] coloursFrom,
            short[] coloursTo,
            short[] texturesFrom,
            short[] texturesTo,
            boolean invertFaces)
    {
        JSpinner modelIdSpinner = new JSpinner();
        JSpinner groupSpinner = new JSpinner();
        JTextField nameField = new JTextField(name);
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

        if (coloursFrom == null)
            coloursFrom = stringToShort(oldColours);

        if (coloursTo == null)
            coloursTo = stringToShort(newColours);

        if (texturesFrom == null)
            texturesFrom = new short[0];

        if (texturesTo == null)
            texturesTo = new short[0];

        ComplexPanel complexModePanel = new ComplexPanel(
                modelIdSpinner,
                groupSpinner,
                nameField,
                coloursFrom, coloursTo,
                texturesFrom, texturesTo,
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
        modelIdSpinner.setBackground((modelId == -1) ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.DARK_GRAY_COLOR);
        modelIdSpinner.setToolTipText("Set the id of the model you want to draw from the cache");
        modelIdSpinner.addChangeListener(e ->
        {
            int newValue = (int) modelIdSpinner.getValue();
            complexModePanel.setBorder(new LineBorder(getBorderColour(newValue), 1));
            modelIdSpinner.setBackground((newValue == -1) ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.DARK_GRAY_COLOR);
            if (colourSwapPanel.getComboBox().getSelectedItem() == complexModePanel)
                colourSwapPanel.onSwapperPressed(complexModePanel);
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

        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 2;
        JButton swapperButton = new JButton("Swap");
        swapperButton.setFocusable(false);
        swapperButton.setToolTipText("Opens this model in the Colour/Texture Swapper tab");
        swapperButton.setPreferredSize(BUTTON_DIMENSION);
        complexModePanel.add(swapperButton, c);
        swapperButton.addActionListener(e ->
        {
            colourSwapPanel.setComboBox(complexModePanel);
            tabbedPane.setSelectedIndex(1);
        });

        c.gridx = 4;
        c.gridy = 3;
        c.gridwidth = 1;
        JButton copyButton = new JButton(new ImageIcon(COPY_COLOURS));
        copyButton.setFocusable(false);
        copyButton.setToolTipText("Copy all swapped colours & textures");
        copyButton.addActionListener(e -> colourSwapPanel.copyColoursTextures(complexModePanel));
        complexModePanel.add(copyButton, c);

        c.gridx = 5;
        c.gridy = 3;
        c.gridwidth = 1;
        JButton pasteButton = new JButton(new ImageIcon(PASTE_COLOURS));
        pasteButton.setFocusable(false);
        pasteButton.setToolTipText("Paste all copied colours & textures");
        complexModePanel.add(pasteButton, c);
        pasteButton.addActionListener(e -> colourSwapPanel.pasteColoursTextures(complexModePanel));

        c.gridx = 6;
        c.gridy = 3;
        c.gridwidth = 2;
        JButton clearColoursTextures = new JButton("Clear");
        clearColoursTextures.setFocusable(false);
        clearColoursTextures.setToolTipText("Clears all swapped colours & textures");
        clearColoursTextures.setPreferredSize(BUTTON_DIMENSION);
        complexModePanel.add(clearColoursTextures, c);
        clearColoursTextures.addActionListener(e -> colourSwapPanel.clearColoursTextures(complexModePanel));
        colourSwapPanel.addComplexPanelOption(complexModePanel);

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

            createComplexPanel(
                    nameField.getText(),
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
                    "",
                    "",
                    complexModePanel.getColoursFrom(),
                    complexModePanel.getColoursTo(),
                    complexModePanel.getTexturesFrom(),
                    complexModePanel.getTexturesTo(),
                    checkInvertFaces.isSelected());
        });

        removeButton.addActionListener(e ->
        {
            colourSwapPanel.removeComplexPanelOption(complexModePanel);
            complexMode.remove(complexModePanel);
            complexPanels.remove(complexModePanel);
            repaint();
            revalidate();
        });

        nameField.addActionListener(e ->
        {
            String text = StringHandler.cleanString(nameField.getText());
            nameField.setText(text.replaceAll("=", ""));
        });

        nameField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                String text = StringHandler.cleanString(nameField.getText());
                nameField.setText(text.replaceAll("=", ""));
            }
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
        return plugin.createComplexModel(detailedModels, setPriority, lightingStyle, lighting, true);
    }

    private DetailedModel[] panelsToDetailedModels()
    {
        DetailedModel[] detailedModels = new DetailedModel[0];

        for (int i = 0; i < complexPanels.size(); i++)
        {
            ComplexPanel complexModePanel = complexPanels.get(i);
            String name = complexModePanel.getNameField().getText();
            int modelId = (int) complexModePanel.getModelIdSpinner().getValue();
            if (modelId == -1)
            {
                continue;
            }

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
            short[] coloursFrom = complexModePanel.getColoursFrom();
            short[] coloursTo = complexModePanel.getColoursTo();
            short[] texturesFrom = complexModePanel.getTexturesFrom();
            short[] texturesTo = complexModePanel.getTexturesTo();
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

            DetailedModel detailedModel = new DetailedModel(
                    name,
                    modelId,
                    group,
                    xTile, yTile, zTile,
                    xTranslate, yTranslate, zTranslate,
                    xScale, yScale, zScale, rotate,
                    "", "",
                    coloursFrom, coloursTo,
                    texturesFrom, texturesTo,
                    invertFaces);
            detailedModels = ArrayUtils.add(detailedModels, detailedModel);
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

            plugin.loadCustomModelToAnvil(selectedFile);
        }
    }

    public static short[] stringToShort(String string)
    {
        if (string.isEmpty())
            return new short[0];

        String[] split = string.split(",");
        short[] array = new short[0];
        try
        {
            for (String s : split)
                array = ArrayUtils.add(array, Short.parseShort(s));
        }
        catch (Exception e)
        {
            return new short[0];
        }

        return array;
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

    public void setLightingSettings(LightingStyle preset, int ambience, int contrast, int x, int y, int z)
    {
        if (preset == LightingStyle.CUSTOM)
            preset = LightingStyle.DEFAULT;

        presetComboBox.setSelectedItem(preset);
        ambSpinner.setValue(ambience);
        conSpinner.setValue(contrast);
        lightXSpinner.setValue(x);
        lightYSpinner.setValue(y);
        lightZSpinner.setValue(z);
    }
}
