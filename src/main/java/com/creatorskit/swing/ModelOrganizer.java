package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.ModelImporter;
import com.creatorskit.models.*;
import com.creatorskit.models.exporters.ModelExporter;
import com.creatorskit.swing.renderer.RenderPanel;
import com.creatorskit.swing.searchabletable.JFilterableTable;
import com.creatorskit.swing.searchabletable.TableRenderStyle;
import com.creatorskit.swing.timesheet.keyframe.ModelKeyFrame;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ModelOrganizer extends JPanel
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final ClientThread clientThread;
    private final ModelImporter modelImporter;
    private final ModelExporter modelExporter;
    private final ModelUtilities modelUtilities;

    private final JFilterableTable table = new JFilterableTable("Custom Models", TableRenderStyle.DEFAULT);
    private List<CustomModel> customModels = new ArrayList<>();
    private RenderPanel renderPanel;

    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");
    private final BufferedImage ANVIL = ImageUtil.loadImageResource(getClass(), "/Anvil.png");
    private final BufferedImage SAVE = ImageUtil.loadImageResource(getClass(), "/Save.png");
    private final BufferedImage TRANSMOG = ImageUtil.loadImageResource(getClass(), "/Transmog.png");
    private final BufferedImage EXPORT = ImageUtil.loadImageResource(getClass(), "/Export.png");
    private final GridBagConstraints c = new GridBagConstraints();
    public static final File MODELS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");

    @Inject
    public ModelOrganizer(Client client, CreatorsPlugin plugin, ClientThread clientThread, ModelImporter modelImporter, ModelExporter modelExporter, ModelUtilities modelUtilities)
    {
        this.client = client;
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.modelImporter = modelImporter;
        this.modelExporter = modelExporter;
        this.modelUtilities = modelUtilities;

        setupLayout();
    }

    private void setupLayout()
    {
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.gridheight = 3;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel organizerLabel = new JLabel("Custom Models");
        organizerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        organizerLabel.setFont(FontManager.getRunescapeBoldFont());
        add(organizerLabel, c);

        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 2;
        c.gridy = 0;
        JButton loadCustomButton = new JButton("Load Saved Model");
        loadCustomButton.setToolTipText("Loads a previously forged and saved Custom Model");
        add(loadCustomButton, c);
        loadCustomButton.addActionListener(e -> openLoadDialog());

        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 1;
        JButton loadBlenderButton = new JButton("Load Blender Model");
        loadBlenderButton.setToolTipText("Loads a model exported from Blender");
        add(loadBlenderButton, c);
        loadBlenderButton.addActionListener(e -> modelImporter.openLoadDialog());

        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 1;
        JButton quickLoadBlenderButton = new JButton("Quick");
        quickLoadBlenderButton.setToolTipText("Loads the latest model exported from Blender in the " + RuneLite.RUNELITE_DIR + "\\creatorskit\\blender-models folder");
        add(quickLoadBlenderButton, c);
        quickLoadBlenderButton.addActionListener(e -> modelImporter.openLatestFile());

        c.gridheight = 1;
        c.gridwidth = 2;
        c.gridx = 2;
        c.gridy = 2;
        JButton clearButton = new JButton("Clear Unused Models");
        clearButton.setToolTipText("Clears all unused models from Custom Model dropdown menus");
        add(clearButton, c);
        clearButton.addActionListener(e -> onClearButtonPressed());

        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 4;
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(new LineBorder(ColorScheme.DARK_GRAY_COLOR));
        table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.setViewportView(table);

        table.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = table.getSelectedObject();
            if (o instanceof CustomModel)
            {
                CustomModel model = (CustomModel) o;
                clientThread.invokeLater(() -> renderPanel.updateModel(model.getModel()));
            }
        });
        add(scrollPane, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 5;
        c.weightx = 1;
        c.weighty = 0;
        JLabel tools = new JLabel("Tools");
        tools.setHorizontalAlignment(SwingConstants.CENTER);
        tools.setFont(FontManager.getRunescapeBoldFont());
        add(tools, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 1;
        c.gridy = 5;
        c.weightx = 0;
        c.weighty = 0;
        JPanel buttons = new JPanel();
        setupButtons(buttons);
        add(buttons, c);

        c.gridwidth = 2;
        c.gridheight = 2;
        c.weightx = 1;
        c.gridx = 2;
        c.gridy = 4;
        JPanel previewPanel = new JPanel();
        setupRenderPanel(previewPanel);
        add(previewPanel, c);

        revalidate();
    }

    private CustomModel getSelectedCustomModel()
    {
        Object o = table.getSelectedObject();
        if (o instanceof CustomModel)
        {
            return (CustomModel) o;
        }

        return null;
    }

    public void setupButtons(JPanel master)
    {
        master.setLayout(new BorderLayout());

        JPanel buttons =  new JPanel();
        buttons.setLayout(new GridLayout(0, 3, 4, 4));
        master.add(buttons, BorderLayout.CENTER);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        JLabel textHeader = new JLabel("Rename:");
        textPanel.add(textHeader, BorderLayout.LINE_START);

        JTextField textField = new JTextField();
        textPanel.add(textField, BorderLayout.CENTER);
        master.add(textPanel, BorderLayout.PAGE_END);

        textField.addActionListener(e ->
        {
            String text = StringHandler.cleanString(textField.getText());
            textField.setText(text);

            CustomModel model = getSelectedCustomModel();
            if (model == null)
            {
                return;
            }

            model.getComp().setName(text);
            modelUtilities.updatePanelComboBoxes();

            TransmogPanel transmogPanel = plugin.getCreatorsPanel().getToolBox().getTransmogPanel();
            transmogPanel.getTransmogLabel().setText(text);
            repaint();
        });

        JButton deleteButton = new JButton(new ImageIcon(CLEAR));
        deleteButton.setText("Delete");
        deleteButton.setToolTipText("Remove this model from all Objects and dropdown menus");
        deleteButton.addActionListener(e ->
        {
            CustomModel model = getSelectedCustomModel();
            if (model == null)
            {
                return;
            }

            modelUtilities.removeCustomModels(new CustomModel[]{model});
        });
        buttons.add(deleteButton);

        JButton anvilButton = new JButton(new ImageIcon(ANVIL));
        anvilButton.setText("Send to Anvil");
        anvilButton.setToolTipText("Send this model to the Anvil");
        buttons.add(anvilButton);
        anvilButton.addActionListener(e ->
        {
            CustomModel model = getSelectedCustomModel();
            if (model == null)
            {
                return;
            }

            modelUtilities.customModelToAnvil(model);
        });

        JButton saveButton = new JButton(new ImageIcon(SAVE));
        saveButton.setText("Save");
        saveButton.setToolTipText("Save this model for future use");
        buttons.add(saveButton);
        saveButton.addActionListener(e ->
        {
            CustomModel model = getSelectedCustomModel();
            if (model == null)
            {
                return;
            }

            openSaveDialog(model, model.getComp().getName());
        });

        JButton transmogButton = new JButton(new ImageIcon(TRANSMOG));
        transmogButton.setText("Set as Transmog");
        transmogButton.setToolTipText("Set this as your player transmog");
        buttons.add(transmogButton);
        transmogButton.addActionListener(e ->
        {
            CustomModel model = getSelectedCustomModel();
            if (model == null)
            {
                return;
            }

            TransmogPanel transmogPanel = plugin.getCreatorsPanel().getToolBox().getTransmogPanel();
            transmogPanel.setTransmog(model);
        });

        JButton exportButton = new JButton(new ImageIcon(EXPORT));
        exportButton.setText("Export 3D Model");
        exportButton.setToolTipText("Export this model to a 3D format based on the Model Exporter settings in the config");
        buttons.add(exportButton);
        exportButton.addActionListener(e ->
        {
            CustomModel model = getSelectedCustomModel();
            if (model == null)
            {
                return;
            }

            clientThread.invokeLater(() ->
            {
                String name = model.getComp().getName();
                BlenderModel blenderModel = modelExporter.bmFromCustomModel(model);
                modelExporter.saveToFile(name, blenderModel);
            });
        });
    }

    private void onClearButtonPressed()
    {
        CustomModel[] unusedModels = new CustomModel[0];
        CustomModel transmogModel = plugin.getTransmogModel();
        ArrayList<Character> characters = plugin.getCharacters();

        ArrayList<CustomModel> storedModels = plugin.getStoredModels();
        for (int i = 0; i < storedModels.size(); i++)
        {
            CustomModel customModel = storedModels.get(i);

            if (customModel == transmogModel)
                continue;

            boolean isBeingUsed = false;
            for (int x = 0; x < characters.size(); x++)
            {
                Character character = characters.get(x);
                if (character.getStoredModel() == customModel)
                {
                    isBeingUsed = true;
                    break;
                }

                ModelKeyFrame[] modelKeyFrames = character.getModelKeyFrames();
                if (modelKeyFrames == null || modelKeyFrames.length == 0)
                {
                    continue;
                }

                for (int f = 0; f < modelKeyFrames.length; f++)
                {
                    ModelKeyFrame keyFrame = modelKeyFrames[f];
                    if (keyFrame.isUseCustomModel() && keyFrame.getCustomModel() == customModel)
                    {
                        isBeingUsed = true;
                        break;
                    }
                }

                if (isBeingUsed)
                {
                    break;
                }
            }

            if (!isBeingUsed)
            {
                unusedModels = ArrayUtils.add(unusedModels, customModel);
            }
        }

        modelUtilities.removeCustomModels(unusedModels);
    }

    private void setupRenderPanel(JPanel previewPanel)
    {
        previewPanel.setLayout(new BorderLayout());
        previewPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        previewPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        JSlider fovSlider = new JSlider(1, 179, RenderPanel.FOV_DEFAULT);
        previewPanel.add(fovSlider, BorderLayout.NORTH);

        renderPanel = new RenderPanel(client, clientThread, fovSlider);
        previewPanel.add(renderPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BorderLayout());
        previewPanel.add(footer, BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel(new FlowLayout());
        footer.add(controlPanel, BorderLayout.NORTH);

        JButton resetButton = new JButton("Reset Camera View");
        resetButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        resetButton.addActionListener(e -> renderPanel.resetCameraView());
        controlPanel.add(resetButton);

        JCheckBox animate = new JCheckBox("Enable Animations");
        animate.setSelected(true);
        animate.addActionListener(e -> clientThread.invokeLater(() -> renderPanel.toggleAnimations(animate.isSelected())));
        controlPanel.add(animate);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 3, 3, 3));
        footer.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void addModels(CustomModel[] models)
    {
        for (CustomModel customModel : models)
        {
            customModels.add(customModel);
        }

        List<Object> list = new ArrayList<>(customModels);
        table.initialize(list);
        table.resetView();
    }

    public void removeModels(CustomModel[] models)
    {
        for (CustomModel customModel : models)
        {
            customModels.remove(customModel);
        }

        List<Object> list = new ArrayList<>(customModels);
        table.initialize(list);
        table.resetView();
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

                modelUtilities.loadCustomModel(selectedFile);
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
}
