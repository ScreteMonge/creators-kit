package com.creatorskit.panels;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.CustomModel;
import com.creatorskit.models.DetailedModel;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class ModelManager extends JFrame
{
    @Inject
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private final HashMap<CustomModel, JPanel> panelMap = new HashMap<>();
    private final ArrayList<JPanel> complexPanels = new ArrayList<>();
    JPanel modelFrame = new JPanel();
    JScrollPane scrollPane = new JScrollPane();
    JPanel scrollPanel = new JPanel();
    GridBagConstraints sC = new GridBagConstraints();
    Pattern pattern = Pattern.compile("\\s|[^\\d,]");

    @Inject
    public ModelManager(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setTitle("RuneLite Model Organizer");
        setIconImage(icon);

        GridBagConstraints initial = new GridBagConstraints();
        initial.fill = GridBagConstraints.BOTH;
        initial.gridx = 0;
        initial.gridy = 0;
        initial.weightx = 1;
        initial.weighty = 0;
        initial.anchor = GridBagConstraints.PAGE_START;
        JPanel anvilMenu = new JPanel();
        anvilMenu.setBackground(ColorScheme.DARK_GRAY_COLOR);
        anvilMenu.setLayout(new GridBagLayout());
        add(anvilMenu, initial);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(4, 4, 4, 4);

        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 6;
        c.gridheight = 2;
        JLabel anvilLabel = new JLabel("Model Anvil", SwingConstants.CENTER);
        anvilLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        anvilLabel.setFont(FontManager.getRunescapeBoldFont());
        anvilLabel.setToolTipText("Use to create new models");
        anvilLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        anvilMenu.add(anvilLabel, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        JTextField nameTextField = new JTextField();
        nameTextField.setText("Name");
        nameTextField.setToolTipText("Set the name of this model");
        nameTextField.setHorizontalAlignment(JTextField.CENTER);
        anvilMenu.add(nameTextField, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        final boolean[] simpleModeActive = {true};
        JButton modeButton = new JButton("Mode");
        modeButton.setToolTipText("Switch Anvil modes");
        modeButton.setFocusable(false);
        anvilMenu.add(modeButton, c);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        JButton forgeButton = new JButton();
        forgeButton.setText("Forge");
        forgeButton.setToolTipText("Creates a custom model from the input");
        forgeButton.setFocusable(false);
        anvilMenu.add(forgeButton, c);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        JButton forgeAndSetButton = new JButton();
        forgeAndSetButton.setText("Forge & Set");
        forgeAndSetButton.setToolTipText("Create a custom model from the input and set it to selected object");
        forgeAndSetButton.setFocusable(false);
        anvilMenu.add(forgeAndSetButton, c);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 5;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 5;
        c.gridheight = 4;
        JPanel entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        anvilMenu.add(entriesPanel, c);

        JPanel simpleMode = new JPanel();
        simpleMode.setLayout(new BorderLayout());
        simpleMode.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        simpleMode.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        entriesPanel.add(simpleMode);

        JLabel simpleLabel = new JLabel("- Simple Mode -");
        simpleLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        simpleLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        simpleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        simpleLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        simpleMode.add(simpleLabel, BorderLayout.PAGE_START);

        JPanel simpleModePanel = new JPanel();
        simpleModePanel.setLayout(new GridBagLayout());
        simpleMode.add(simpleModePanel);

        sC.fill = GridBagConstraints.NONE;
        sC.insets = new Insets(4, 4, 4, 4);

        sC.gridx = 0;
        sC.gridy = 0;
        sC.anchor = GridBagConstraints.LINE_END;
        sC.weightx = 0;
        sC.weighty = 1;
        JLabel sModelsLabel = new JLabel("Models to load:");
        sModelsLabel.setToolTipText("Enter the models to load (separated by commas)");
        simpleModePanel.add(sModelsLabel, sC);

        sC.gridx = 0;
        sC.gridy = 1;
        sC.anchor = GridBagConstraints.LINE_END;
        sC.weightx = 0;
        JLabel sNewColourLabel = new JLabel("New colours:");
        sNewColourLabel.setToolTipText("Colours to replace the old colours (separate by commas)");
        simpleModePanel.add(sNewColourLabel, sC);

        sC.gridx = 0;
        sC.gridy = 2;
        sC.anchor = GridBagConstraints.LINE_END;
        sC.weightx = 0;
        JLabel sOldColourLabel = new JLabel("Old colours:");
        sOldColourLabel.setToolTipText("Colours to replace with the new colours (separated by commas)");
        simpleModePanel.add(sOldColourLabel, sC);

        sC.fill = GridBagConstraints.BOTH;
        sC.anchor = GridBagConstraints.LINE_START;
        sC.gridx = 1;
        sC.gridy = 0;
        sC.weightx = 1;
        JTextField sModelsField = new JTextField();
        sModelsField.addActionListener(e -> { stringToCSV(pattern, sModelsField); });
        sModelsField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                stringToCSV(pattern, sModelsField);
            }
        });
        sModelsField.setPreferredSize(new Dimension(250, 25));
        simpleModePanel.add(sModelsField, sC);

        sC.gridx = 1;
        sC.gridy = 1;
        sC.weightx = 1;
        JTextField sNewColoursField = new JTextField();
        sNewColoursField.addActionListener(e -> { stringToCSV(pattern, sNewColoursField); });
        sNewColoursField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                stringToCSV(pattern, sNewColoursField);
            }
        });
        sNewColoursField.setPreferredSize(new Dimension(250, 25));
        simpleModePanel.add(sNewColoursField, sC);

        sC.gridx = 1;
        sC.gridy = 2;
        sC.weightx = 1;
        JTextField sOldColourField = new JTextField();
        sOldColourField.addActionListener(e -> { stringToCSV(pattern, sOldColourField); });
        sOldColourField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) { }

            @Override
            public void focusLost(FocusEvent e) {
                stringToCSV(pattern, sOldColourField);
            }
        });
        sOldColourField.setPreferredSize(new Dimension(250, 25));
        simpleModePanel.add(sOldColourField, sC);

        JPanel complexMode = new JPanel();
        complexMode.setLayout(new BorderLayout());
        complexMode.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        complexMode.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        complexMode.setVisible(false);
        complexMode.setMinimumSize(new Dimension(1000, 800));
        complexMode.setMaximumSize(new Dimension(1500, 800));
        entriesPanel.add(complexMode);

        JLabel complexLabel = new JLabel("- Complex Mode -");
        complexLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        complexLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        complexLabel.setHorizontalAlignment(SwingConstants.CENTER);
        complexLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        complexMode.add(complexLabel, BorderLayout.PAGE_START);

        complexMode.add(scrollPane, BorderLayout.CENTER);
        scrollPanel.setLayout(new GridLayout(0, 3, 6, 6));
        scrollPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.getViewport().add(scrollPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setViewportView(scrollPanel);

        createComplexPanel();

        forgeButton.addActionListener(e ->
        {
            forgeModel(client, simpleModeActive, sModelsField, sOldColourField, sNewColoursField, nameTextField, false);
        });

        forgeAndSetButton.addActionListener(e ->
        {
            forgeModel(client, simpleModeActive, sModelsField, sOldColourField, sNewColoursField, nameTextField, true);
        });

        modeButton.addActionListener(e ->
        {
            if (simpleModeActive[0])
            {
                simpleModeActive[0] = false;
                simpleMode.setVisible(false);
                complexMode.setVisible(true);
                simpleMode.revalidate();
                anvilMenu.revalidate();
                entriesPanel.revalidate();
                scrollPanel.revalidate();
                repaint();
                revalidate();
                return;
            }

            simpleModeActive[0] = true;
            simpleMode.setVisible(true);
            complexMode.setVisible(false);
            simpleMode.revalidate();
            anvilMenu.revalidate();
            entriesPanel.revalidate();
            scrollPanel.revalidate();
            repaint();
            revalidate();
        });


        initial.gridx = 0;
        initial.gridy = 1;
        JPanel modelOrganizer = new JPanel();
        modelOrganizer.setBorder(new EmptyBorder(8, 8, 8, 8));
        modelOrganizer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        modelOrganizer.setLayout(new BorderLayout());
        add(modelOrganizer, initial);

        JLabel organizerLabel = new JLabel("Model Organizer");
        organizerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        organizerLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        organizerLabel.setFont(FontManager.getRunescapeBoldFont());
        modelOrganizer.add(organizerLabel, BorderLayout.PAGE_START);

        modelFrame.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        modelFrame.setLayout(new GridLayout(0, 7, 8, 8));
        modelOrganizer.add(modelFrame, BorderLayout.CENTER);

        revalidate();
        pack();
    }

    public void createComplexPanel()
    {
        createComplexPanel(0, 0, 0, 0, 128, 128, 128, 0, "", "");
    }

    public void createComplexPanel(int modelId, int xTranslate, int yTranslate, int zTranslate, int scaleX, int scaleY, int scaleZ, int rotate, String newColours, String oldColours)
    {
        JPanel complexModePanel = new JPanel();
        complexModePanel.setLayout(new GridBagLayout());

        sC.fill = GridBagConstraints.BOTH;
        sC.insets = new Insets(4, 4, 4, 4);

        sC.gridx = 0;
        sC.gridy = 0;
        sC.weightx = 1;
        sC.weighty = 0;
        sC.gridwidth = 1;
        sC.gridheight = 1;
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1, 4, 4));
        buttonPanel.setName("buttonPanel");
        complexModePanel.add(buttonPanel, sC);

        JLabel modelIdLabel = new JLabel("Model Id");
        modelIdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        modelIdLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        buttonPanel.add(modelIdLabel);

        JSpinner modelIdSpinner = new JSpinner();
        modelIdSpinner.setValue(modelId);
        modelIdSpinner.setToolTipText("Set the Model Id");
        modelIdSpinner.setName("modelIdSpinner");
        buttonPanel.add(modelIdSpinner);

        JButton addButton = new JButton("Add");
        addButton.setFocusable(false);
        addButton.addActionListener(e ->
        {
            createComplexPanel();
        });
        buttonPanel.add(addButton);

        JButton removeButton = new JButton("Remove");
        removeButton.setFocusable(false);
        removeButton.addActionListener(e ->
        {
            if (complexPanels.size() > 1)
            {
                scrollPanel.remove(complexModePanel);
                complexPanels.remove(complexModePanel);
                scrollPanel.revalidate();
                scrollPanel.updateUI();
                scrollPanel.repaint();
            }
        });
        buttonPanel.add(removeButton);

        JButton duplicateButton = new JButton("Duplicate");
        duplicateButton.setFocusable(false);
        buttonPanel.add(duplicateButton);


        sC.gridx = 1;
        sC.gridy = 0;
        sC.weightx = 1;
        sC.weighty = 1;
        JPanel titlesPanel = new JPanel();
        titlesPanel.setLayout(new GridLayout(0, 1));
        complexModePanel.add(titlesPanel, sC);

        JLabel translateLabel = new JLabel("Translate:");
        translateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        titlesPanel.add(translateLabel);

        JLabel scaleLabel = new JLabel("Scale:");
        scaleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        titlesPanel.add(scaleLabel);

        JLabel rotateLabel = new JLabel("Rotate:");
        rotateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        titlesPanel.add(rotateLabel);

        JLabel colourNewLabel = new JLabel("New Colours:");
        colourNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        titlesPanel.add(colourNewLabel);

        JLabel colourOldLabel = new JLabel("Old Colours:");
        colourOldLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        titlesPanel.add(colourOldLabel);

        sC.gridx = 2;
        sC.gridy = 0;
        sC.weightx = 5;
        sC.weighty = 5;
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridBagLayout());
        settingsPanel.setName("settingsPanel");
        complexModePanel.add(settingsPanel, sC);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        JLabel x = new JLabel("x:");
        x.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(x, c);

        c.gridx = 1;
        c.gridy = 0;
        JSpinner xSpinner = new JSpinner();
        xSpinner.setValue(xTranslate);
        xSpinner.setName("xSpinner");
        settingsPanel.add(xSpinner, c);

        c.gridx = 2;
        c.gridy = 0;
        JLabel y = new JLabel("y:");
        y.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(y, c);

        c.gridx = 3;
        c.gridy = 0;
        JSpinner ySpinner = new JSpinner();
        ySpinner.setValue(yTranslate);
        ySpinner.setName("ySpinner");
        settingsPanel.add(ySpinner, c);

        c.gridx = 4;
        c.gridy = 0;
        JLabel z = new JLabel("z:");
        z.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(z, c);

        c.gridx = 5;
        c.gridy = 0;
        JSpinner zSpinner = new JSpinner();
        zSpinner.setValue(zTranslate);
        zSpinner.setName("zSpinner");
        settingsPanel.add(zSpinner, c);

        c.gridx = 0;
        c.gridy = 1;
        JLabel xScale = new JLabel("x:");
        xScale.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(xScale, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner xScaleSpinner = new JSpinner();
        xScaleSpinner.setValue(scaleX);
        xScaleSpinner.setToolTipText("Scale x. 128 is default scale");
        xScaleSpinner.setName("xScaleSpinner");
        settingsPanel.add(xScaleSpinner, c);

        c.gridx = 2;
        c.gridy = 1;
        JLabel yScale = new JLabel("y:");
        yScale.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(yScale, c);

        c.gridx = 3;
        c.gridy = 1;
        JSpinner yScaleSpinner = new JSpinner();
        yScaleSpinner.setValue(scaleY);
        yScaleSpinner.setToolTipText("Scale y. 128 is default scale");
        yScaleSpinner.setName("yScaleSpinner");
        settingsPanel.add(yScaleSpinner, c);

        c.gridx = 4;
        c.gridy = 1;
        JLabel zScale = new JLabel("z:");;
        zScale.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(zScale, c);

        c.gridx = 5;
        c.gridy = 1;
        JSpinner zScaleSpinner = new JSpinner();
        zScaleSpinner.setValue(scaleZ);
        zScaleSpinner.setToolTipText("Scale z. 128 is default scale");
        zScaleSpinner.setName("zScaleSpinner");
        settingsPanel.add(zScaleSpinner, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel rotate90 = new JLabel("90°");
        rotate90.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(rotate90, c);

        c.gridx = 1;
        c.gridy = 2;
        JCheckBox check90 = new JCheckBox();
        check90.setName("check90");
        check90.setHorizontalAlignment(SwingConstants.LEFT);
        check90.setFocusable(false);
        settingsPanel.add(check90, c);

        c.gridx = 2;
        c.gridy = 2;
        JLabel rotate180 = new JLabel("180°");
        rotate180.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(rotate180, c);

        c.gridx = 3;
        c.gridy = 2;
        JCheckBox check180 = new JCheckBox();
        check180.setName("check180");
        check180.setHorizontalAlignment(SwingConstants.LEFT);
        check180.setFocusable(false);
        settingsPanel.add(check180, c);

        c.gridx = 4;
        c.gridy = 2;
        JLabel rotate270 = new JLabel("270°");
        rotate270.setHorizontalAlignment(SwingConstants.RIGHT);
        settingsPanel.add(rotate270, c);

        c.gridx = 5;
        c.gridy = 2;
        JCheckBox check270 = new JCheckBox();
        check270.setName("check270");
        check270.setHorizontalAlignment(SwingConstants.LEFT);
        check270.setFocusable(false);
        settingsPanel.add(check270, c);

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

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 6;
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
        settingsPanel.add(colourNewField, c);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 6;
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
        settingsPanel.add(colourOldField, c);

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

        scrollPanel.add(complexModePanel);
        scrollPanel.updateUI();
        scrollPanel.revalidate();
        scrollPanel.repaint();
        scrollPane.updateUI();
        scrollPane.revalidate();
        scrollPane.repaint();
        complexPanels.add(complexModePanel);
    }

    private void forgeModel(Client client, boolean[] simpleModeActive, JTextField sModelsField, JTextField sOldColourField, JTextField sNewColoursField, JTextField nameTextField, boolean forgeAndSet)
    {
        if (client == null)
        {
            return;
        }

        if (simpleModeActive[0])
        {
            if (sModelsField.getText().isEmpty())
            {
                clientThread.invokeLater(() ->
                {
                    plugin.sendChatMessage("You must enter at least one ModelId to Forge a new model.");
                });
                return;
            }

            String[] modelIdString = sModelsField.getText().split(",");
            int[] modelIds = new int[modelIdString.length];

            String[] oldColourStringArray = sOldColourField.getText().split(",");
            short[] oldColours = new short[oldColourStringArray.length];
            String[] newColourStringArray = sNewColoursField.getText().split(",");
            short[] newColours = new short[newColourStringArray.length];

            if (!sOldColourField.getText().isEmpty() && !sNewColoursField.getText().isEmpty())
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
                CustomModel customModel = new CustomModel(model, nameTextField.getText());
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

            CustomModel customModel = new CustomModel(model, nameTextField.getText());
            plugin.addCustomModel(customModel, forgeAndSet);
        });
    }

    private Model forgeComplexModel()
    {
        DetailedModel[] detailedModels = new DetailedModel[complexPanels.size()];
        for (int i = 0; i < complexPanels.size(); i++) {
            JPanel complexModePanel = complexPanels.get(i);
            int modelId = 0;
            int xTranslate = 0;
            int yTranslate = 0;
            int zTranslate = 0;
            int xScale = 128;
            int yScale = 128;
            int zScale = 128;
            int rotate = 0;
            String recolourNew = "";
            String recolourOld = "";

            for (Component component : complexModePanel.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel componentPanel = (JPanel) component;
                    for (Component comp : componentPanel.getComponents()) {
                        String name = comp.getName();
                        if (comp instanceof JSpinner) {
                            if (name.equals("modelIdSpinner")) {
                                modelId = (int) ((JSpinner) comp).getValue();
                                continue;
                            }
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
                                continue;
                            }

                            continue;
                        }

                        if (comp instanceof JTextField) {
                            if (name.equals("colourNewField")) {
                                recolourNew = ((JTextField) comp).getText();
                                continue;
                            }

                            if (name.equals("colourOldField")) {
                                recolourOld = ((JTextField) comp).getText();
                            }
                        }
                    }
                }
            }

            String[] newColoursArray = recolourNew.split(",");
            short[] newColours = new short[newColoursArray.length];
            String[] oldColoursArray = recolourOld.split(",");
            short[] oldColours = new short[oldColoursArray.length];

            if (!recolourNew.isEmpty() && !recolourOld.isEmpty())
            {
                if (newColoursArray.length != oldColoursArray.length)
                {
                    clientThread.invokeLater(() -> plugin.sendChatMessage("Please ensure that each model has the same number of New Colours as Old Colours"));
                    return null;
                }

                try
                {
                    for (int e = 0; e < oldColours.length; e++)
                    {
                        oldColours[e] = Short.parseShort(oldColoursArray[e]);
                        newColours[e] = Short.parseShort(newColoursArray[e]);
                    }
                }
                catch (Exception e)
                {
                    clientThread.invokeLater(() -> plugin.sendChatMessage("Please reformat your colour entry to CSV format (ex. 123,987,456"));
                    return null;
                }
            }

            DetailedModel detailedModel = new DetailedModel(modelId, xTranslate, yTranslate, zTranslate, xScale, yScale, zScale, rotate, newColours, oldColours);
            detailedModels[i] = detailedModel;

            //Extraneous
            StringBuilder newC = new StringBuilder();
            StringBuilder oldC = new StringBuilder();
            for (int s = 0; s < newColours.length; s++)
            {
                newC.append(newColours[s]);
                oldC.append(oldColours[s]);

                if (s != newColours.length -1)
                {
                    newC.append(",");
                    oldC.append(",");
                }
            }

            String newer = newC.toString();
            String old = oldC.toString();
            System.out.println();

            try {
                FileWriter writer = new FileWriter("info.txt", true);

                writer.write("\n\n" + "modelid=" + modelId + "\n" + "xt=" + xTranslate + "\n" + "yt=" + yTranslate + "\n" + "zt=" + zTranslate + "\n" + "xs=" + xScale + "\n" + "ys=" + yScale + "\n" + "zs=" + zScale + "\n" + "r=" + rotate + "\n" + "n=" + newer + "\n" + "o=" + old + "\n" + "");
                System.out.println("Written");
                writer.close();
            }
            catch (IOException e)
            {
                System.out.println("io");
            }

        }

        return plugin.createComplexModel(detailedModels);
    }

    public void createModelPanel(CustomModel model)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JTextField textField = new JTextField();
        textField.setText(model.toString());
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(textField, BorderLayout.PAGE_START);
        panelMap.put(model, panel);
        textField.addActionListener(e ->
        {
            model.setString(textField.getText());
            plugin.updatePanelComboBoxes();
        });

        JButton deleteButton = new JButton();
        deleteButton.setText("Delete");
        deleteButton.setFocusable(false);
        deleteButton.addActionListener(e ->
        {
            plugin.removeCustomModel(model);
        });
        panel.add(deleteButton, BorderLayout.PAGE_END);

        modelFrame.add(panel);
        modelFrame.updateUI();
        revalidate();
    }

    public void removeModelPanel(CustomModel model)
    {
        JPanel panel = panelMap.get(model);
        modelFrame.remove(panel);
        modelFrame.updateUI();
        panelMap.remove(model);
        revalidate();
    }

    private void stringToCSV(Pattern pattern, JTextField jTextField)
    {
        jTextField.setText(pattern.matcher(jTextField.getText()).replaceAll(""));
    }
}
