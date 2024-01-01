package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.RuneLiteObject;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@Slf4j
public class ModelOrganizer extends JPanel
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final ClientThread clientThread;
    private final CreatorsConfig config;
    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");
    private final BufferedImage ANVIL = ImageUtil.loadImageResource(getClass(), "/Anvil.png");
    private final BufferedImage SAVE = ImageUtil.loadImageResource(getClass(), "/Save.png");
    private final BufferedImage TRANSMOG = ImageUtil.loadImageResource(getClass(), "/Transmog.png");
    private final HashMap<CustomModel, JPanel> panelMap = new HashMap<>();
    private final JPanel modelPanel = new JPanel();
    private final GridBagConstraints c = new GridBagConstraints();
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");

    @Inject
    public ModelOrganizer(Client client, CreatorsPlugin plugin, ClientThread clientThread, CreatorsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.config = config;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane();
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.setColumnHeaderView(headerPanel);

        modelPanel.setBackground(Color.BLACK);
        modelPanel.setLayout(new GridLayout(0, 8, 8, 8));
        modelPanel.setBorder(new EmptyBorder(6, 4, 6, 4));
        scrollPane.setViewportView(modelPanel);
        add(scrollPane);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1;
        c.weighty = 0;

        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 4;
        JLabel organizerLabel = new JLabel("Model Organizer");
        organizerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        organizerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerPanel.add(organizerLabel, c);

        c.weightx = 0.1;
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 1;
        JLabel searcherLabel = new JLabel("Cache Searcher");
        searcherLabel.setHorizontalAlignment(SwingConstants.CENTER);
        searcherLabel.setFont(FontManager.getRunescapeBoldFont());
        headerPanel.add(searcherLabel, c);

        c.gridx = 2;
        c.gridy = 1;
        JSpinner idSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
        idSpinner.setToolTipText("Choose the NPC or Object Id to find");
        headerPanel.add(idSpinner, c);

        c.gridx = 2;
        c.gridy = 2;
        JComboBox<CustomModelType> modelTypeComboBox = new JComboBox<>();
        modelTypeComboBox.addItem(CustomModelType.CACHE_NPC);
        modelTypeComboBox.addItem(CustomModelType.CACHE_OBJECT);
        modelTypeComboBox.setFocusable(false);
        modelTypeComboBox.setToolTipText("Pick which part of the cache to search");
        headerPanel.add(modelTypeComboBox, c);

        c.gridx = 2;
        c.gridy = 3;
        JButton addCustomModelButton = new JButton("Add Custom Model");
        addCustomModelButton.setToolTipText("Add the chosen NPC or Object as a Custom Model");
        headerPanel.add(addCustomModelButton, c);
        addCustomModelButton.addActionListener(e ->
        {
            CustomModelType type = (CustomModelType) modelTypeComboBox.getSelectedItem();
            if (type == null)
                return;

            int id = (int) idSpinner.getValue();
            plugin.cacheToCustomModel(type, id);
        });

        c.gridx = 3;
        c.gridy = 0;
        c.gridheight = 2;
        JButton loadButton = new JButton("Load Custom Model");
        loadButton.setToolTipText("Loads a previously forged and saved Custom Model");
        headerPanel.add(loadButton, c);
        loadButton.addActionListener(e ->
                openLoadDialog());

        c.gridx = 3;
        c.gridy = 2;
        c.gridheight = 2;
        JButton clearButton = new JButton("Clear Unused Models");
        clearButton.setToolTipText("Clears all unused models from Custom Model dropdown menus");
        headerPanel.add(clearButton, c);
        clearButton.addActionListener(e ->
        {
            ArrayList<CustomModel> unusedModels = new ArrayList<>();
            CustomModel transmogModel = plugin.getTransmogModel();

            for (int i = 0; i < plugin.getStoredModels().size(); i++)
            {
                CustomModel customModel = plugin.getStoredModels().get(i);

                if (customModel == transmogModel)
                    continue;

                boolean isBeingUsed = false;
                for (Character character : plugin.getCharacters())
                {
                    if (character.getStoredModel() == customModel)
                    {
                        isBeingUsed = true;
                        break;
                    }
                }

                if (!isBeingUsed)
                    unusedModels.add(customModel);
            }

            for (CustomModel customModel : unusedModels)
                plugin.removeCustomModel(customModel);
        });

        revalidate();
    }

    public void createModelPanel(CustomModel model)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        modelPanel.add(panel);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridx = 0;
        c.gridy = 0;
        JTextField textField = new JTextField();
        textField.setText(model.getComp().getName());
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(textField, c);
        panelMap.put(model, panel);
        textField.addActionListener(e ->
        {
            String text = textField.getText();
            model.getComp().setName(text);
            plugin.updatePanelComboBoxes();
            plugin.getCreatorsPanel().getTransmogPanel().getTransmogLabel().setText(text);
        });

        c.gridx = 0;
        c.gridy = 1;
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 0, 4, 4));
        panel.add(buttonsPanel, c);

        JButton deleteButton = new JButton(new ImageIcon(CLEAR));
        deleteButton.setFocusable(false);
        deleteButton.setToolTipText("Remove this model from all Objects and dropdown menus");
        deleteButton.addActionListener(e ->
                plugin.removeCustomModel(model));
        buttonsPanel.add(deleteButton);

        JButton anvilButton = new JButton(new ImageIcon(ANVIL));
        anvilButton.setFocusable(false);
        anvilButton.setToolTipText("Send this model to the Anvil");
        buttonsPanel.add(anvilButton);
        anvilButton.addActionListener(e ->
                plugin.customModelToAnvil(model));

        JButton saveButton = new JButton(new ImageIcon(SAVE));
        saveButton.setFocusable(false);
        saveButton.setToolTipText("Save this model for future use");
        buttonsPanel.add(saveButton);
        saveButton.addActionListener(e ->
                openSaveDialog(model, textField.getText()));

        JButton transmogButton = new JButton(new ImageIcon(TRANSMOG));
        transmogButton.setFocusable(false);
        transmogButton.setToolTipText("Set this as your player transmog");
        buttonsPanel.add(transmogButton);
        transmogButton.addActionListener(e ->
                setTransmog(model));

        revalidate();
        repaint();
    }

    public void removeModelPanel(CustomModel model)
    {
        JPanel panel = panelMap.get(model);
        modelPanel.remove(panel);
        modelPanel.updateUI();
        panelMap.remove(model);
        revalidate();
        repaint();
    }

    private void openLoadDialog()
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

            if (!selectedFile.exists())
            {
                selectedFile = new File(selectedFile.getPath() + ".json");
                if (!selectedFile.exists())
                {
                    plugin.sendChatMessage("Could not find the requested Custom Model file.");
                    return;
                }
            }

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

    private void openSaveDialog(CustomModel customModel, String name)
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
        fileChooser.setDialogTitle("Save Custom Model");

        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().endsWith(".json"))
            {
                selectedFile = new File(selectedFile.getPath() + ".json");
            }
            saveToFile(selectedFile, customModel);
        }
    }

    public void saveToFile(File file, CustomModel customModel)
    {
        try {
            FileWriter writer = new FileWriter(file, false);

            CustomModelComp comp = customModel.getComp();
            DetailedModel[] detailedModels = comp.getDetailedModels();
            if (detailedModels == null)
            {
                detailedModels = modelToDetailedPanels(customModel);
                comp.setDetailedModels(detailedModels);
            }

            String string = plugin.getGson().toJson(comp);
            writer.write(string);
            writer.close();
        }
        catch (IOException e)
        {
            log.debug("Error when saving Custom Model to file");
        }
    }

    public DetailedModel[] modelToDetailedPanels(CustomModel customModel)
    {
        return modelToDetailedPanels(customModel.getComp());
    }

    public DetailedModel[] modelToDetailedPanels(CustomModelComp comp)
    {
        CustomModelType type = comp.getType();

        ModelStats[] modelStats = comp.getModelStats();
        DetailedModel[] detailedModels = new DetailedModel[modelStats.length];
        int group = 1;
        if (type == CustomModelType.CACHE_NPC)
            group = 8;

        if (type == CustomModelType.CACHE_PLAYER)
            group = 9;

        for (int i = 0; i < modelStats.length; i++)
        {
            ModelStats modelStat = modelStats[i];

            String recolFrom;
            String recolTo;
            if (type == CustomModelType.CACHE_PLAYER && modelStat.getBodyPart() != BodyPart.NA)
            {
                String modelStatFrom = ModelFinder.shortArrayToString(modelStat.getRecolourFrom());
                String modelStatTo = ModelFinder.shortArrayToString(modelStat.getRecolourTo());
                String kitRecolourOld = KitRecolourer.getKitRecolourOld(modelStat.getBodyPart());
                String kitRecolourNew = KitRecolourer.getKitRecolourNew(modelStat.getBodyPart(), comp.getKitRecolours());

                if (modelStatFrom.isEmpty() || modelStatTo.isEmpty())
                {
                    recolFrom = KitRecolourer.getKitRecolourOld(modelStat.getBodyPart());
                    recolTo = KitRecolourer.getKitRecolourNew(modelStat.getBodyPart(), comp.getKitRecolours());
                }
                else if (kitRecolourOld.isEmpty() || kitRecolourNew.isEmpty())
                {
                    recolFrom = modelStatFrom;
                    recolTo = modelStatTo;
                }
                else
                {
                    recolFrom = modelStatFrom + "," + kitRecolourOld;
                    recolTo = modelStatTo + "," + kitRecolourNew;
                }
            }
            else
            {
                recolFrom = ModelFinder.shortArrayToString(modelStat.getRecolourFrom());
                recolTo = ModelFinder.shortArrayToString(modelStat.getRecolourTo());
            }

            String bodyPart = "Name";
            if (type == CustomModelType.CACHE_PLAYER)
                bodyPart = "Item";

            if (modelStat.getBodyPart() != BodyPart.NA)
                bodyPart = modelStat.getBodyPart().toString();

            DetailedModel detailedModel = new DetailedModel(bodyPart, modelStat.getModelId(), group, 0, 0, 0, 0, 0, 0, 128, 128, 128, 0, recolTo, recolFrom, false);
            detailedModels[i] = detailedModel;
        }

        return detailedModels;
    }

    public void setTransmog(CustomModel customModel)
    {
        RuneLiteObject transmog = plugin.getTransmog();
        if (transmog == null)
        {
            transmog = client.createRuneLiteObject();
            plugin.setTransmog(transmog);
        }

        plugin.setTransmogModel(customModel);
        plugin.getCreatorsPanel().getTransmogPanel().getTransmogLabel().setText(customModel.getComp().getName());
        transmog.setModel(customModel.getModel());
        transmog.setShouldLoop(true);
        transmog.setRadius(plugin.getCreatorsPanel().getTransmogPanel().getRadius());
    }
}
