package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.CustomModel;
import com.creatorskit.models.DetailedModel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModelAnvil extends JFrame
{
    private ClientThread clientThread;
    private final Client client;
    private final CreatorsPlugin plugin;
    private final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private final BufferedImage DUPLICATE = ImageUtil.loadImageResource(getClass(), "/Duplicate.png");
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage ARROW_LEFT = ImageUtil.loadImageResource(getClass(), "/Arrow_Left.png");
    private final BufferedImage ARROW_RIGHT = ImageUtil.loadImageResource(getClass(), "/Arrow_Right.png");
    private final BufferedImage TRANSLATE = ImageUtil.loadImageResource(getClass(), "/Translate.png");
    private final BufferedImage ROTATE = ImageUtil.loadImageResource(getClass(), "/Rotate.png");
    private final BufferedImage TRANSLATE_SUBTILE = ImageUtil.loadImageResource(getClass(), "/Translate subtile.png");
    private final BufferedImage SCALE = ImageUtil.loadImageResource(getClass(), "/Scale.png");
    private final Dimension spinnerDimension = new Dimension(65, 25);
    private final ArrayList<JPanel> complexPanels = new ArrayList<>();
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    JPanel complexMode = new JPanel();
    JScrollPane scrollPane = new JScrollPane();
    GridBagConstraints c = new GridBagConstraints();
    Pattern pattern = Pattern.compile("\\s|[^\\d,]");

    @Inject
    public ModelAnvil(Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setTitle("RuneLite Model Anvil");
        setIconImage(icon);


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

        JTextField nameField = new JTextField();
        nameField.setText("Name");
        nameField.setHorizontalAlignment(JTextField.CENTER);
        buttonsPanel.add(nameField);

        JButton forgeButton = new JButton("Forge");
        forgeButton.setFocusable(false);
        buttonsPanel.add(forgeButton);

        JButton forgeSetButton = new JButton("Forge & Set");
        forgeSetButton.setFocusable(false);
        buttonsPanel.add(forgeSetButton);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.ipady = 0;
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        add(tabbedPane, c);

        JPanel simpleMode = new JPanel();
        simpleMode.setLayout(new GridBagLayout());
        tabbedPane.addTab("Simple Mode", simpleMode);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        JLabel modelLoadLabel = new JLabel("Models to load:");
        modelLoadLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        simpleMode.add(modelLoadLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        JLabel newColourLabel = new JLabel("New colours:");
        newColourLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        simpleMode.add(newColourLabel, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel oldColourLabel = new JLabel("Old colours:");
        oldColourLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        simpleMode.add(oldColourLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        JTextField modelLoadField = new JTextField();
        simpleMode.add(modelLoadField, c);

        c.gridx = 1;
        c.gridy = 1;
        JTextField newColourField = new JTextField();
        simpleMode.add(newColourField, c);

        c.gridx = 1;
        c.gridy = 2;
        JTextField oldColourField = new JTextField();
        simpleMode.add(oldColourField, c);

        scrollPane.setViewportView(complexMode);
        complexMode.setLayout(new GridLayout(0, 4, 8, 8));
        complexMode.setBackground(Color.BLACK);
        createComplexPanel();
        tabbedPane.addTab("Complex Mode", scrollPane);

        JPanel headerPanel = new JPanel();
        scrollPane.setColumnHeaderView(headerPanel);

        JButton addButton = new JButton("Add");
        addButton.setFocusable(false);
        addButton.addActionListener(e -> { createComplexPanel(); });
        headerPanel.add(addButton);

        JPopupMenu loadPopupMenu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Load");
        menuItem.addActionListener(e -> LinkBrowser.open(MODELS_DIR.toString()));
        loadPopupMenu.add(menuItem);

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> openLoadDialog());
        loadButton.setFocusable(false);
        loadButton.setComponentPopupMenu(loadPopupMenu);
        headerPanel.add(loadButton);

        JButton saveButton = new JButton("Save");
        saveButton.setFocusable(false);
        saveButton.addActionListener(e -> openSaveDialog());
        headerPanel.add(saveButton);


        forgeButton.addActionListener(e ->
        {
            boolean simpleModeActive = false;
            if (tabbedPane.getSelectedComponent() == simpleMode)
                simpleModeActive = true;

            forgeModel(client, simpleModeActive, modelLoadField, oldColourField, newColourField, nameField, false);
        });

        forgeSetButton.addActionListener(e ->
        {
            boolean simpleModeActive = false;
            if (tabbedPane.getSelectedComponent() == simpleMode)
                simpleModeActive = true;

            forgeModel(client, simpleModeActive, modelLoadField, oldColourField, newColourField, nameField, true);
        });

        System.out.println("Model anvil created");
        validate();
        pack();
    }

    public void createComplexPanel()
    {
        createComplexPanel(0, 0, 0, 0, 0, 0, 0, 128, 128, 128, 0, "", "");
    }

    public void createComplexPanel(int modelId, int xTile, int yTile, int zTile, int xTranslate, int yTranslate, int zTranslate, int scaleX, int scaleY, int scaleZ, int rotate, String newColours, String oldColours)
    {
        JPanel complexModePanel = new JPanel();
        complexModePanel.setLayout(new GridBagLayout());
        complexModePanel.setBorder(new LineBorder(getBorderColour(modelId), 1));

        c.insets = new Insets(2, 4, 2, 4);
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        JLabel modelIdLabel = new JLabel("Model Id:");
        modelIdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        modelIdLabel.setVerticalAlignment(SwingConstants.CENTER);
        complexModePanel.add(modelIdLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        JLabel emptyLabel = new JLabel("");
        complexModePanel.add(emptyLabel, c);

        c.gridx = 0;
        c.gridy = 2;
        JPanel xyzPanel = new JPanel();
        xyzPanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(xyzPanel, c);

        JLabel xLabel = new JLabel("x:");
        xLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(xLabel);

        JLabel yLabel = new JLabel("y:");
        yLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(yLabel);

        JLabel zLabel = new JLabel("z:");
        zLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(zLabel);

        c.gridx = 0;
        c.gridy = 3;
        JLabel colourNewLabel = new JLabel("New Colours:");
        colourNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        complexModePanel.add(colourNewLabel, c);

        c.gridx = 0;
        c.gridy = 4;
        JLabel colourOldLabel = new JLabel("Old Colours:");
        colourOldLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        complexModePanel.add(colourOldLabel, c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 4;
        JSpinner modelIdSpinner = new JSpinner();
        modelIdSpinner.setValue(modelId);
        modelIdSpinner.setToolTipText("Set the Model Id");
        modelIdSpinner.setName("modelIdSpinner");
        modelIdSpinner.addChangeListener(e ->
        {
            complexModePanel.setBorder(new LineBorder(getBorderColour((int) modelIdSpinner.getValue()), 1));
        });
        complexModePanel.add(modelIdSpinner, c);

        c.gridx = 5;
        c.gridy = 0;
        c.gridwidth = 1;
        JButton arrowLeftButton = new JButton(new ImageIcon(ARROW_LEFT));
        arrowLeftButton.setFocusable(false);
        arrowLeftButton.addActionListener(e -> {setPanelIndex(complexModePanel, -1);});
        complexModePanel.add(arrowLeftButton, c);

        c.gridx = 6;
        c.gridy = 0;
        c.gridwidth = 1;
        JButton arrowRightButton = new JButton(new ImageIcon(ARROW_RIGHT));
        arrowRightButton.setFocusable(false);
        arrowRightButton.addActionListener(e -> {setPanelIndex(complexModePanel, 1);});
        complexModePanel.add(arrowRightButton, c);

        c.gridx = 7;
        c.gridy = 0;
        c.gridwidth = 1;
        JButton duplicateButton = new JButton(new ImageIcon(DUPLICATE));
        duplicateButton.setFocusable(false);
        complexModePanel.add(duplicateButton, c);

        c.gridx = 8;
        c.gridy = 0;
        c.gridwidth = 1;
        JButton removeButton = new JButton(new ImageIcon(CLOSE));
        removeButton.setFocusable(false);
        removeButton.addActionListener(e ->
        {
            if (complexPanels.size() > 1)
            {
                complexMode.remove(complexModePanel);
                complexPanels.remove(complexModePanel);
                repaint();
                revalidate();
            }
        });
        complexModePanel.add(removeButton, c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel tileLabel = new JLabel(new ImageIcon(TRANSLATE));
        tileLabel.setToolTipText("Translate by unit tile");
        tileLabel.setBackground(Color.BLACK);
        complexModePanel.add(tileLabel, c);

        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel translateLabel = new JLabel(new ImageIcon(TRANSLATE_SUBTILE));
        translateLabel.setToolTipText("Translate by unit sub-tile (1/128 of a tile)");
        translateLabel.setBackground(Color.BLACK);
        complexModePanel.add(translateLabel, c);

        c.gridx = 5;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel scaleLabel = new JLabel(new ImageIcon(SCALE));
        scaleLabel.setToolTipText("Scale (128 is default scale)");
        scaleLabel.setBackground(Color.BLACK);
        complexModePanel.add(scaleLabel, c);

        c.gridx = 7;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel rotateLabel = new JLabel(new ImageIcon(ROTATE));
        rotateLabel.setToolTipText("Rotate");
        rotateLabel.setBackground(Color.BLACK);
        complexModePanel.add(rotateLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(tilePanel, c);

        JSpinner xTileSpinner = new JSpinner();
        xTileSpinner.setValue(xTile);
        xTileSpinner.setPreferredSize(spinnerDimension);
        xTileSpinner.setName("xTileSpinner");
        tilePanel.add(xTileSpinner);

        JSpinner yTileSpinner = new JSpinner();
        yTileSpinner.setValue(yTile);
        yTileSpinner.setName("yTileSpinner");
        tilePanel.add(yTileSpinner);

        JSpinner zTileSpinner = new JSpinner();
        zTileSpinner.setValue(zTile);
        zTileSpinner.setName("zTileSpinner");
        tilePanel.add(zTileSpinner);

        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 2;
        JPanel translatePanel = new JPanel();
        translatePanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(translatePanel, c);

        JSpinner xSpinner = new JSpinner();
        xSpinner.setValue(xTranslate);
        xSpinner.setPreferredSize(spinnerDimension);
        xSpinner.setName("xSpinner");
        translatePanel.add(xSpinner);

        JSpinner ySpinner = new JSpinner();
        ySpinner.setValue(yTranslate);
        ySpinner.setName("ySpinner");
        translatePanel.add(ySpinner);

        JSpinner zSpinner = new JSpinner();
        zSpinner.setValue(zTranslate);
        zSpinner.setName("zSpinner");
        translatePanel.add(zSpinner);

        c.gridx = 5;
        c.gridy = 2;
        c.gridwidth = 2;
        JPanel scalePanel = new JPanel();
        scalePanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(scalePanel, c);

        JSpinner xScaleSpinner = new JSpinner();
        xScaleSpinner.setValue(scaleX);
        xScaleSpinner.setPreferredSize(spinnerDimension);
        xScaleSpinner.setToolTipText("Scale x. 128 is default scale");
        xScaleSpinner.setName("xScaleSpinner");
        scalePanel.add(xScaleSpinner);

        JSpinner yScaleSpinner = new JSpinner();
        yScaleSpinner.setValue(scaleY);
        yScaleSpinner.setToolTipText("Scale y. 128 is default scale");
        yScaleSpinner.setName("yScaleSpinner");
        scalePanel.add(yScaleSpinner);

        JSpinner zScaleSpinner = new JSpinner();
        zScaleSpinner.setValue(scaleZ);
        zScaleSpinner.setToolTipText("Scale z. 128 is default scale");
        zScaleSpinner.setName("zScaleSpinner");
        scalePanel.add(zScaleSpinner);

        c.gridx = 7;
        c.gridy = 2;
        c.gridwidth = 2;
        JPanel rotatePanel = new JPanel();
        rotatePanel.setBackground(Color.BLACK);
        rotatePanel.setLayout(new GridLayout(3, 0));
        complexModePanel.add(rotatePanel, c);

        JCheckBox check90 = new JCheckBox();
        check90.setText("90°");
        check90.setName("check90");
        check90.setHorizontalAlignment(SwingConstants.LEFT);
        check90.setFocusable(false);
        rotatePanel.add(check90);

        JCheckBox check180 = new JCheckBox();
        check180.setText("180°");
        check180.setName("check180");
        check180.setHorizontalAlignment(SwingConstants.LEFT);
        check180.setFocusable(false);
        rotatePanel.add(check180);

        JCheckBox check270 = new JCheckBox();
        check270.setText("270°");
        check270.setName("check270");
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

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 8;
        JTextField colourNewField = new JTextField();
        colourNewField.setText(newColours);
        colourNewField.setName("colourNewField");
        colourNewField.addActionListener(e -> { stringToCSV(pattern, colourNewField); });
        colourNewField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                stringToCSV(pattern, colourNewField);
            }
        });
        complexModePanel.add(colourNewField, c);

        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 8;
        JTextField colourOldField = new JTextField();
        colourOldField.setText(oldColours);
        colourOldField.setName("colourOldField");
        colourOldField.addActionListener(e -> { stringToCSV(pattern, colourOldField); });
        colourOldField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                stringToCSV(pattern, colourOldField);
            }
        });
        complexModePanel.add(colourOldField, c);

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

            createComplexPanel
                    ((int) modelIdSpinner.getValue(),
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
                            colourNewField.getText(),
                            colourOldField.getText()
                    );
        });

        System.out.println("Success");
        complexMode.add(complexModePanel);
        complexModePanel.setEnabled(true);
        complexModePanel.setVisible(true);
        complexPanels.add(complexModePanel);
        System.out.println(complexPanels.size());

        revalidate();
        repaint();
    }

    private void setPanelIndex(JPanel panel, int change)
    {
        int newPosition = complexMode.getComponentZOrder(panel) + change;
        if (newPosition >= 0 && newPosition < complexPanels.size())
        {
            complexMode.setComponentZOrder(panel, newPosition);
            repaint();
            revalidate();
        }
    }

    private void forgeModel(Client client, boolean simpleModeActive, JTextField modelLoadField, JTextField oldColourField, JTextField newColourField, JTextField nameField, boolean forgeAndSet)
    {
        if (client == null)
        {
            return;
        }

        if (simpleModeActive)
        {
            if (modelLoadField.getText().isEmpty())
            {
                clientThread.invokeLater(() ->
                {
                    plugin.sendChatMessage("You must enter at least one ModelId to Forge a new model.");
                });
                return;
            }

            String[] modelIdString = modelLoadField.getText().split(",");
            int[] modelIds = new int[modelIdString.length];

            String[] oldColourStringArray = oldColourField.getText().split(",");
            short[] oldColours = new short[oldColourStringArray.length];
            String[] newColourStringArray = newColourField.getText().split(",");
            short[] newColours = new short[newColourStringArray.length];

            if (!oldColourField.getText().isEmpty() && !newColourField.getText().isEmpty())
            {
                if (oldColourStringArray.length != newColourStringArray.length)
                {
                    clientThread.invokeLater(() -> plugin.sendChatMessage("There must be the same number of New and Old colours."));
                    return;
                }

                try
                {
                    for (int i = 0; i < oldColourStringArray.length; i++)
                    {
                        oldColours[i] = Short.parseShort(oldColourStringArray[i]);
                        newColours[i] = Short.parseShort(newColourStringArray[i]);
                    }
                }
                catch (Exception exception)
                {
                    clientThread.invokeLater(() -> plugin.sendChatMessage("Please reformat your Colours by separating each Colour with a comma and removing excess characters."));
                    return;
                }
            }

            try
            {
                for (int i = 0; i < modelIdString.length; i++)
                {
                    modelIds[i] = Integer.parseInt(modelIdString[i]);
                }
            }
            catch (Exception exception)
            {
                clientThread.invokeLater(() -> plugin.sendChatMessage("Please reformat your ModelIds by separating each ModelId with a comma and removing excess characters."));
                return;
            }

            clientThread.invokeLater(() ->
            {
                Model model = plugin.constructSimpleModel(modelIds, oldColours, newColours);
                CustomModel customModel = new CustomModel(model, nameField.getText());
                plugin.addCustomModel(customModel, forgeAndSet);
            });
            return;
        }

        clientThread.invokeLater(() ->
        {
            Model model = forgeComplexModel();
            if (model == null)
            {
                return;
            }

            CustomModel customModel = new CustomModel(model, nameField.getText());
            plugin.addCustomModel(customModel, forgeAndSet);
        });
    }

    private Model forgeComplexModel()
    {
        return plugin.createComplexModel(panelsToDetailedModels());
    }

    private DetailedModel[] panelsToDetailedModels()
    {
        DetailedModel[] detailedModels = new DetailedModel[complexPanels.size()];
        for (int i = 0; i < complexPanels.size(); i++) {
            JPanel complexModePanel = complexPanels.get(i);
            int modelId = 0;
            int xTile = 0;
            int yTile = 0;
            int zTile = 0;
            int xTranslate = 0;
            int yTranslate = 0;
            int zTranslate = 0;
            int xScale = 128;
            int yScale = 128;
            int zScale = 128;
            int rotate = 0;
            String recolourNew = "";
            String recolourOld = "";

            for (Component component : complexModePanel.getComponents())
            {
                String primaryComponentName = component.getName();
                if (component instanceof JSpinner)
                {
                    if (primaryComponentName.equals("modelIdSpinner"))
                    {
                        modelId = (int) ((JSpinner) component).getValue();
                        continue;
                    }
                }

                if (component instanceof JTextField) {
                    if (primaryComponentName.equals("colourNewField")) {
                        recolourNew = ((JTextField) component).getText();
                        continue;
                    }

                    if (primaryComponentName.equals("colourOldField")) {
                        recolourOld = ((JTextField) component).getText();
                    }
                }

                if (component instanceof JPanel)
                {
                    JPanel componentPanel = (JPanel) component;
                    for (Component comp : componentPanel.getComponents())
                    {
                        String name = comp.getName();
                        if (comp instanceof JSpinner)
                        {
                            if (name.equals("xSpinner")) {
                                xTranslate = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            if (name.equals("ySpinner")) {
                                yTranslate = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            if (name.equals("zSpinner")) {
                                zTranslate = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            if (name.equals("xTileSpinner")) {
                                xTile = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            if (name.equals("yTileSpinner")) {
                                yTile = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            if (name.equals("zTileSpinner")) {
                                zTile = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            if (name.equals("xScaleSpinner")) {
                                xScale = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            if (name.equals("yScaleSpinner")) {
                                yScale = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            if (name.equals("zScaleSpinner")) {
                                zScale = (int) ((JSpinner) comp).getValue();
                                continue;
                            }

                            continue;
                        }

                        if (comp instanceof JCheckBox) {
                            JCheckBox checkBox = (JCheckBox) comp;

                            if (name.equals("check90") && checkBox.isSelected()) {
                                rotate = 1;
                                continue;
                            }

                            if (name.equals("check180") && checkBox.isSelected()) {
                                rotate = 2;
                                continue;
                            }

                            if (name.equals("check270") && checkBox.isSelected()) {
                                rotate = 3;
                            }
                        }
                    }
                }
            }

            DetailedModel detailedModel = new DetailedModel(modelId, xTile, yTile, zTile, xTranslate, yTranslate, zTranslate, xScale, yScale, zScale, rotate, recolourNew, recolourOld);
            detailedModels[i] = detailedModel;
        }

        return detailedModels;
    }

    private void openSaveDialog()
    {
        File outputDir = MODELS_DIR;
        outputDir.mkdirs();

        JFileChooser fileChooser = new JFileChooser(outputDir)
        {
            @Override
            public void approveSelection()
            {
                File f = getSelectedFile();
                if (!f.getName().endsWith(".txt"))
                {
                    f = new File(f.getPath() + ".txt");
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
        fileChooser.setSelectedFile(new File("model.txt"));
        fileChooser.setDialogTitle("Save current model collection");

        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().endsWith(".txt"))
            {
                selectedFile = new File(selectedFile.getPath() + ".txt");
            }
            saveToFile(selectedFile);
        }
    }

    public void saveToFile(File file)
    {
        try {
            FileWriter writer = new FileWriter(file, false);

            for (DetailedModel detailedModel : panelsToDetailedModels())
            {
                writer.write("\n\n" + "modelid=" + detailedModel.getModelId() + "\n" + detailedModel.getXTile() + "\n" + detailedModel.getYTile() + "\n" + detailedModel.getZTile() + "\n" + "xt=" + detailedModel.getXTranslate() + "\n" + "yt=" + detailedModel.getYTranslate() + "\n" + "zt=" + detailedModel.getZTranslate() + "\n" + "xs=" + detailedModel.getXScale() + "\n" + "ys=" + detailedModel.getYScale() + "\n" + "zs=" + detailedModel.getZScale() + "\n" + "r=" + detailedModel.getRotate() + "\n" + "n=" + detailedModel.getRecolourNew() + "\n" + "o=" + detailedModel.getRecolourOld() + "\n" + "");
            }

            System.out.println("Written");
            writer.close();
        }
        catch (IOException e)
        {
            System.out.println("Error occurred while writing to file.");
        }
    }

    private void openLoadDialog()
    {
        File outputDir = MODELS_DIR;
        outputDir.mkdirs();

        JFileChooser fileChooser = new JFileChooser(outputDir);
        fileChooser.setDialogTitle("Choose a model collection to load");

        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            plugin.loadCustomModel(selectedFile);
        }
    }

    private void stringToCSV(Pattern pattern, JTextField jTextField)
    {
        jTextField.setText(pattern.matcher(jTextField.getText()).replaceAll(""));
    }

    private Color getBorderColour(int modelId)
    {
        if (modelId == 0)
        {
            return ColorScheme.MEDIUM_GRAY_COLOR;
        }

        float hue = (float) modelId / 50;
        return Color.getHSBColor(hue, 1, (float) 0.7);
    }
}
