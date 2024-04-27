package com.creatorskit.models;

import com.creatorskit.CreatorsPlugin;
import net.runelite.api.*;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;

public class ModelImporter
{
    @Inject
    private CreatorsPlugin plugin;

    @Inject
    private ClientThread clientThread;

    @Inject
    private Client client;

    public static final File BLENDER_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/blender-models");
    private final int BASIC_MODEL = 823;
    private final int TRANSPARENT_MODEL = 18871;
    private final int PRIORITY_MODEL = 6733;
    private final int BASIC_MODEL_2_4 = 15166;
    private final int BASIC_MODEL_3_7 = 21140;
    private final int ANIMATED_MODEL_2_5 = 21201;
    private final int ANIMATED_MODEL_13_13 = 80;
    private final int TEST_MODEL = 23901;

    public void openLoadDialog()
    {
        BLENDER_DIR.mkdirs();

        JFileChooser fileChooser = new JFileChooser(BLENDER_DIR);
        fileChooser.setDialogTitle("Choose a Blender model to load");

        JComboBox<LightingStyle> comboBox = new JComboBox<>();
        comboBox.setToolTipText("Sets the lighting style");
        comboBox.addItem(LightingStyle.ACTOR);
        comboBox.addItem(LightingStyle.DEFAULT);
        comboBox.addItem(LightingStyle.NONE);
        comboBox.setFocusable(false);

        JPanel accessory = new JPanel();
        accessory.setLayout(new GridLayout(0, 1));
        accessory.add(comboBox);

        fileChooser.setAccessory(accessory);
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
                loadBlenderModel(selectedFile, (LightingStyle) comboBox.getSelectedItem());
            }
        }
    }

    public void openLatestFile()
    {
        File directory = new File(BLENDER_DIR.getPath());
        File[] files = directory.listFiles(File::isFile);

        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;
        if (files != null)
        {
            for (File file : files)
            {
                if (!file.getName().endsWith(".json"))
                {
                    continue;
                }

                if (file.lastModified() > lastModifiedTime)
                {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }

        if (chosenFile == null)
        {
            plugin.sendChatMessage("Could not find the latest .json file in the " + RuneLite.RUNELITE_DIR + "\\creatorskit\\blender-models folder.");
            return;
        }

        loadBlenderModel(chosenFile, LightingStyle.ACTOR);
    }

    private void loadBlenderModel(File file, LightingStyle lightingStyle)
    {
        if (!file.exists())
        {
            file = new File(file.getPath() + ".json");
            if (!file.exists())
            {
                file = new File(file.getPath().replaceAll("/", "\\\\"));
                if (!file.exists())
                {
                    file = new File(file.getPath().replaceAll("/", "\\\\") + ".json");
                }
            }
        }

        if (!file.exists())
        {
            plugin.sendChatMessage("Failed to find file.");
            return;
        }

        String name = file.getName();
        if (name.endsWith(".json"))
            name = replaceLast(name);

        String finalName = name;
        File finalSelectedFile = file;
        clientThread.invokeLater(() ->
        {
            BlenderModel blenderModel;

            try
            {
                Reader reader = Files.newBufferedReader(finalSelectedFile.toPath());
                blenderModel = plugin.getGson().fromJson(reader, BlenderModel.class);
                reader.close();
            }
            catch (Exception e)
            {
                plugin.sendChatMessage("The file found was unreadable as a BlenderModel.");
                return;
            }

            if (blenderModel == null)
            {
                plugin.sendChatMessage("File was found but is incompatible or empty.");
                return;
            }

            addModel(blenderModel, lightingStyle, finalName);
        });
    }

    private String replaceLast(String string)
    {
        int lastIndex = string.lastIndexOf(".json");
        if (lastIndex < 0)
            return string;
        String tail = string.substring(lastIndex).replaceFirst(".json", "");
        return string.substring(0, lastIndex) + tail;
    }

    public void addModel(BlenderModel blenderModel, LightingStyle lightingStyle, String name)
    {
        Model model = createModel(blenderModel, lightingStyle);
        if (model == null)
            return;

        CustomLighting lighting = new CustomLighting(lightingStyle.getAmbient(), lightingStyle.getContrast(), lightingStyle.getX(), lightingStyle.getY(), lightingStyle.getZ());

        CustomModelComp comp = new CustomModelComp(0, CustomModelType.BLENDER, -1, null, null, null, blenderModel, lightingStyle, lighting, false, name);
        CustomModel customModel = new CustomModel(model, comp);
        plugin.addCustomModel(customModel, false);
    }

    public Model createModel(BlenderModel blenderModel, LightingStyle lightingStyle)
    {
        int vertexCount = blenderModel.getVertices().length;
        int[] verticesX = new int[vertexCount];
        int[] verticesY = new int[vertexCount];
        int[] verticesZ = new int[vertexCount];

        for (int i = 0; i < vertexCount; i++)
        {
            int[] v = blenderModel.getVertices()[i];
            verticesX[i] = v[0];
            verticesY[i] = v[1];
            verticesZ[i] = v[2];
        }

        int faceCount = blenderModel.getFaces().length;
        int[] faces1 = new int[faceCount];
        int[] faces2 = new int[faceCount];
        int[] faces3 = new int[faceCount];

        for (int i = 0; i < faceCount; i++)
        {
            int[] f = blenderModel.getFaces()[i];
            faces1[i] = f[0];
            faces2[i] = f[1];
            faces3[i] = f[2];
        }

        double[][] blenderColours = blenderModel.getColours();
        short[] colours = new short[0];
        for (double[] colour : blenderColours)
        {
            int colorHInt = (int) (63 - (colour[0] * 63));
            int colorLInt = (int) (colour[1] * 127);
            int colorSInt = (int) (colour[2] * 7);
            short jagexColor = JagexColor.packHSL(colorHInt, colorSInt, colorLInt);
            colours = ArrayUtils.add(colours, jagexColor);
        }

        //{2_6 priority, 1_3, 1_2, 1_0, 1_3 transparent}
        int[] polys = new int[]{1, 0, 0, 0, 1};
        try
        {
            polys = getPolyCount(polys, faces1.length - 3, verticesX.length - 3);
        }
        catch (Exception e)
        {
            plugin.sendChatMessage("Could not import this Blender Model. It's likely that it contains too few faces or vertices.");
            return null;
        }

        //int[] polys = new int[]{0, 1, 0, 0, 0};
        //polys = getPolyCountAnimated(polys, faces1.length - 2, verticesX.length - 4);
        //System.out.println(polys[0] + "," + polys[1] + "," + polys[2] + "," + polys[3] + "," + polys[4]);
        ModelData modelData = constructModel(polys);
        if (modelData == null)
            return null;

        //System.out.println(faceCount + ", " + modelData.getFaceCount());
        //System.out.println(vertexCount + ", " + modelData.getVerticesCount());

        int[] vX = modelData.getVerticesX();
        int[] vY = modelData.getVerticesY();
        int[] vZ = modelData.getVerticesZ();
        int[] f1 = modelData.getFaceIndices1();
        int[] f2 = modelData.getFaceIndices2();
        int[] f3 = modelData.getFaceIndices3();
        short[] cols = modelData.getFaceColors();

        for (int i = 0; i < modelData.getVerticesCount(); i++)
        {
            vX[i] = verticesX[i];
            vY[i] = verticesY[i];
            vZ[i] = verticesZ[i];
        }

        for (int i = 0; i < modelData.getFaceCount(); i++)
        {
            f1[i] = faces1[i];
            f2[i] = faces2[i];
            f3[i] = faces3[i];
            cols[i] = colours[i];
        }

        byte[] transparencies = blenderModel.getTransparencies();
        byte[] tp = modelData.getFaceTransparencies();
        for (int i = 0; i < modelData.getFaceCount(); i++)
        {
            tp[i] = transparencies[i];
        }

        Model model;
        switch (lightingStyle)
        {
            default:
            case DEFAULT:
                model = modelData.light();
                break;
            case ACTOR:
                model = modelData.light(64, 850, -30, -50, -30);
                break;
            case NONE:
                model = modelData.getModel();
        }

        byte[] priorities = blenderModel.getPriorities();
        byte[] facePriorities = model.getFaceRenderPriorities();
        for (int i = 0; i < model.getFaceCount(); i++)
        {
            facePriorities[i] = priorities[i];
        }

        return model;
    }

    public int[] getPolyCount(int[] polys, int facesRemaining, int verticesRemaining)
    {
        //int[2_6 priority, 1_3, 1_2, 1_0, 1_3 transparent] for [face_vertices]
        //System.out.println("FacesRemaining " + facesRemaining + ", verticesRemaining: " + verticesRemaining);
        if (facesRemaining == 0 && verticesRemaining == 0)
            return polys;

        double divisor = (double) verticesRemaining / facesRemaining;
        // if there's a 1:3 ratio of faces:vertices, close out the function
        if (divisor == 3)
        {
            polys[1] += facesRemaining;
            return polys;
        }

        // if there's a 1:2 ratio of faces:vertices, close out the function
        if (divisor == 2)
        {
            polys[2] += facesRemaining;
            return polys;
        }

        //Effectively while-loop till face count drops below vertex count
        if (facesRemaining + 2 > verticesRemaining)
        {
            int change = facesRemaining + 2 - verticesRemaining;
            polys[3] += change;
            facesRemaining -= change;
            return getPolyCount(polys, facesRemaining, verticesRemaining);
        }

        //if remaining vertices is even, keep even. If odd, change to even
        if (verticesRemaining % 2 == 0)
        {
            polys[2] += 1;
            facesRemaining--;
            verticesRemaining -= 2;
        }
        else
        {
            polys[1] += 1;
            facesRemaining--;
            verticesRemaining -= 3;
        }

        return getPolyCount(polys, facesRemaining, verticesRemaining);
    }

    private ModelData constructModel(int[] polys)
    {
        ModelData[] modelsToMerge = new ModelData[]{};

        ModelData polyPriority = client.loadModelData(PRIORITY_MODEL);
        if (polyPriority != null)
        {
            polyPriority.cloneColors().cloneVertices();
            int[] vX = polyPriority.getVerticesX();
            int[] vY = polyPriority.getVerticesY();
            int[] vZ = polyPriority.getVerticesZ();

            vX[0] = vX[1] = 64;
            vY[0] = vY[1] = 0;
            vZ[0] = vZ[1] = -64;

            vX[2] = vX[3] = -64;
            vY[2] = vY[3] = 0;
            vZ[2] = vZ[3] = 64;

            vX[4] = vX[5] = -64;
            vY[4] = vY[5] = 0;
            vZ[4] = vZ[5] = -64;

            modelsToMerge = ArrayUtils.add(modelsToMerge, polyPriority);
        }
        else
        {
            return null;
        }

        ModelData poly_transparent = client.loadModelData(TRANSPARENT_MODEL);
        if (poly_transparent != null)
        {
            poly_transparent.cloneColors().cloneVertices().cloneTransparencies();
            int[] vX = poly_transparent.getVerticesX();
            int[] vY = poly_transparent.getVerticesY();
            int[] vZ = poly_transparent.getVerticesZ();

            vX[0] = 64;
            vY[0] = 0;
            vZ[0] = -64;

            vX[1] = -64;
            vY[1] = 0;
            vZ[1] = 64;

            vX[2] = -64;
            vY[2] = 0;
            vZ[2] = -64;

            modelsToMerge = ArrayUtils.add(modelsToMerge, poly_transparent);
        }
        else
        {
            return null;
        }

        for (int i = 0; i < polys[1]; i++)
        {
            ModelData poly_1_3 = client.loadModelData(BASIC_MODEL);
            if (poly_1_3 == null)
                return null;

            poly_1_3.cloneColors().cloneVertices().translate(i + 1, i + 1, i + 1);
            modelsToMerge = ArrayUtils.add(modelsToMerge, poly_1_3);
        }

        int translateBy = 128;
        for (int i = 0; i < polys[2]; i++)
        {
            ModelData poly_1_2 = client.loadModelData(BASIC_MODEL);
            if (poly_1_2 == null)
                return null;

            poly_1_2.cloneColors().cloneVertices().translate((i + 1) * translateBy, 0, 0);
            modelsToMerge = ArrayUtils.add(modelsToMerge, poly_1_2);
        }

        for (int i = 0; i < polys[3]; i++)
        {
            ModelData poly_1_0 = client.loadModelData(BASIC_MODEL);
            if (poly_1_0 == null)
                return null;

            poly_1_0.cloneColors().cloneVertices();
            modelsToMerge = ArrayUtils.add(modelsToMerge, poly_1_0);
        }

        return client.mergeModels(modelsToMerge);
    }
}
