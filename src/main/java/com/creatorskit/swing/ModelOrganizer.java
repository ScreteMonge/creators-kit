package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.models.ModelImporter;
import com.creatorskit.models.*;
import com.creatorskit.models.exporters.ModelExporter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
public class ModelOrganizer extends JPanel
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final ClientThread clientThread;
    private final ModelImporter modelImporter;
    private final ModelExporter modelExporter;
    private final CreatorsConfig config;
    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");
    private final BufferedImage ANVIL = ImageUtil.loadImageResource(getClass(), "/Anvil.png");
    private final BufferedImage SAVE = ImageUtil.loadImageResource(getClass(), "/Save.png");
    private final BufferedImage TRANSMOG = ImageUtil.loadImageResource(getClass(), "/Transmog.png");
    private final BufferedImage EXPORT = ImageUtil.loadImageResource(getClass(), "/Export.png");
    private final Dimension buttonDimension = new Dimension(30, 25);
    private final HashMap<CustomModel, JPanel> panelMap = new HashMap<>();
    private final JPanel modelPanel = new JPanel();
    private final GridBagConstraints c = new GridBagConstraints();
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    public final File BLENDER_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/blender-models");

    @Inject
    public ModelOrganizer(Client client, CreatorsPlugin plugin, ClientThread clientThread, ModelImporter modelImporter, ModelExporter modelExporter, CreatorsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.modelImporter = modelImporter;
        this.modelExporter = modelExporter;
        this.config = config;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane();
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setColumnHeaderView(headerPanel);
        add(scrollPane);

        JPanel viewport = new JPanel();
        viewport.setBorder(new EmptyBorder(6, 4, 6, 4));
        viewport.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        viewport.setLayout(new GridBagLayout());
        scrollPane.setViewportView(viewport);

        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        modelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        modelPanel.setLayout(new GridLayout(0, 8, 8, 8));
        viewport.add(modelPanel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        JLabel emptyLabel = new JLabel("");
        viewport.add(emptyLabel, c);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weighty = 0;

        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 4;
        c.weightx = 10;
        JLabel organizerLabel = new JLabel("Model Organizer");
        organizerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        organizerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerPanel.add(organizerLabel, c);

        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 4;
        c.weightx = 1;
        JPanel cacheSearcherPanel = new JPanel();
        cacheSearcherPanel.setLayout(new GridLayout(0, 1, 4, 4));
        cacheSearcherPanel.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        headerPanel.add(cacheSearcherPanel, c);

        JLabel searcherLabel = new JLabel("Cache Searcher");
        searcherLabel.setHorizontalAlignment(SwingConstants.CENTER);
        searcherLabel.setFont(FontManager.getRunescapeBoldFont());
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

        JButton addCustomModelButton = new JButton("Add Custom Model");
        addCustomModelButton.setToolTipText("Add the chosen NPC, Object, or Item as a Custom Model");
        cacheSearcherPanel.add(addCustomModelButton);
        addCustomModelButton.addActionListener(e ->
        {
            CustomModelType type = (CustomModelType) modelTypeComboBox.getSelectedItem();
            if (type == null)
                return;

            int id = (int) idSpinner.getValue();
            plugin.cacheToCustomModel(type, id, -1);
        });

        c.gridx = 3;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 2;
        c.weightx = 0.5;
        JButton loadCustomButton = new JButton("Load Custom Model");
        loadCustomButton.setToolTipText("Loads a previously forged and saved Custom Model");
        headerPanel.add(loadCustomButton, c);
        loadCustomButton.addActionListener(e -> openLoadDialog());

        c.gridx = 3;
        c.gridy = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 0.5;
        JButton loadBlenderButton = new JButton("Load Blender Model");
        loadBlenderButton.setToolTipText("Loads a model exported from Blender");
        headerPanel.add(loadBlenderButton, c);
        loadBlenderButton.addActionListener(e -> modelImporter.openLoadDialog());

        c.gridx = 4;
        c.gridy = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        JButton quickLoadBlenderButton = new JButton("Quick");
        quickLoadBlenderButton.setToolTipText("Loads the latest model exported from Blender in the " + RuneLite.RUNELITE_DIR + "\\creatorskit\\blender-models folder");
        headerPanel.add(quickLoadBlenderButton, c);
        quickLoadBlenderButton.addActionListener(e -> modelImporter.openLatestFile());

        c.gridx = 3;
        c.gridy = 2;
        c.gridheight = 2;
        c.gridwidth = 2;
        c.weightx = 0.5;
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
            String text = StringHandler.cleanString(textField.getText());
            textField.setText(text);
            model.getComp().setName(text);
            plugin.updatePanelComboBoxes();
            plugin.getCreatorsPanel().getTransmogPanel().getTransmogLabel().setText(text);
        });
        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                String text = StringHandler.cleanString(textField.getText());
                textField.setText(text);
                model.getComp().setName(text);
            }
        });

        c.gridx = 0;
        c.gridy = 1;
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 0, 4, 4));
        panel.add(buttonsPanel, c);

        JButton deleteButton = new JButton(new ImageIcon(CLEAR));
        deleteButton.setPreferredSize(buttonDimension);
        deleteButton.setToolTipText("Remove this model from all Objects and dropdown menus");
        deleteButton.addActionListener(e -> plugin.removeCustomModel(model));
        buttonsPanel.add(deleteButton);

        JButton anvilButton = new JButton(new ImageIcon(ANVIL));
        anvilButton.setPreferredSize(buttonDimension);
        anvilButton.setToolTipText("Send this model to the Anvil");
        buttonsPanel.add(anvilButton);
        anvilButton.addActionListener(e -> plugin.customModelToAnvil(model));

        JButton saveButton = new JButton(new ImageIcon(SAVE));
        saveButton.setPreferredSize(buttonDimension);
        saveButton.setToolTipText("Save this model for future use");
        buttonsPanel.add(saveButton);
        saveButton.addActionListener(e -> openSaveDialog(model, textField.getText()));

        JButton transmogButton = new JButton(new ImageIcon(TRANSMOG));
        transmogButton.setPreferredSize(buttonDimension);
        transmogButton.setToolTipText("Set this as your player transmog");
        buttonsPanel.add(transmogButton);
        transmogButton.addActionListener(e -> setTransmog(model));

        JButton exportButton = new JButton(new ImageIcon(EXPORT));
        exportButton.setPreferredSize(buttonDimension);
        exportButton.setToolTipText("Export this model to a 3D format based on the Model Exporter settings in the config");
        buttonsPanel.add(exportButton);
        exportButton.addActionListener(e ->
        {
            clientThread.invokeLater(() ->
            {
                String name = model.getComp().getName();
                BlenderModel blenderModel = modelExporter.bmFromCustomModel(model);
                modelExporter.saveToFile(name, blenderModel);
            });
        });

        revalidate();
        repaint();
    }

    public void removeModelPanel(CustomModel model)
    {
        JPanel panel = panelMap.get(model);
        modelPanel.remove(panel);
        panelMap.remove(model);
        revalidate();
        repaint();
    }

    private void openLoadDialog()
    {
        MODELS_DIR.mkdirs();

        JFileChooser fileChooser = new JFileChooser(MODELS_DIR);
        fileChooser.setDialogTitle("Choose a model to load");
        fileChooser.setMultiSelectionEnabled(true);
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

        int option = fileChooser.showOpenDialog(fileChooser);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File[] files = fileChooser.getSelectedFiles();
            for (File selectedFile : files)
            {
                String name = selectedFile.getName();
                if (name.endsWith(".json"))
                    name = replaceLast(name, ".json");

                if (!selectedFile.exists())
                {
                    selectedFile = new File(selectedFile.getPath() + ".json");
                    if (!selectedFile.exists())
                    {
                        plugin.sendChatMessage("Could not find the requested Custom Model file.");
                        continue;
                    }
                }

                plugin.loadCustomModel(selectedFile, false, name);
            }
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

            short[] colourFrom = modelStat.getRecolourFrom();
            short[] colourTo = modelStat.getRecolourTo();

            BodyPart bp = modelStat.getBodyPart();
            if (type == CustomModelType.CACHE_PLAYER && modelStat.getBodyPart() != BodyPart.NA)
            {
                colourFrom = ArrayUtils.addAll(colourFrom, KitRecolourer.getKitRecolourFrom(modelStat.getBodyPart()));
                colourTo = ArrayUtils.addAll(colourTo, KitRecolourer.getKitRecolourTo(modelStat.getBodyPart(), comp.getKitRecolours()));
            }

            String bodyPart = "Name";
            if (type == CustomModelType.CACHE_PLAYER)
                bodyPart = "Item";

            if (bp != BodyPart.NA)
                bodyPart = modelStat.getBodyPart().toString();

            DetailedModel detailedModel = new DetailedModel(
                    bodyPart,
                    modelStat.getModelId(),
                    group,
                    0, 0, 0,
                    0, 0, 0,
                    modelStat.getResizeX(),
                    modelStat.getResizeY(),
                    modelStat.getResizeZ(),
                    0,
                    "", "",
                    colourFrom, colourTo,
                    modelStat.getTextureFrom(), modelStat.getTextureTo(),
                    false);
            detailedModels[i] = detailedModel;
        }

        return detailedModels;
    }

    public void setTransmog(CustomModel customModel)
    {
        CKObject transmog = plugin.getTransmog();
        if (transmog == null)
        {
            transmog = new CKObject(client);
            client.registerRuneLiteObject(transmog);
            plugin.setTransmog(transmog);
        }

        plugin.setTransmogModel(customModel);
        plugin.getCreatorsPanel().getTransmogPanel().getTransmogLabel().setText(customModel.getComp().getName());
        transmog.setModel(customModel.getModel());
        transmog.setRadius(plugin.getCreatorsPanel().getTransmogPanel().getRadius());
    }
}
