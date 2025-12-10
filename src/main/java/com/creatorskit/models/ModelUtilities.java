package com.creatorskit.models;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.saves.TransmogLoadOption;
import com.creatorskit.saves.TransmogSave;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.swing.TransmogPanel;
import com.creatorskit.swing.anvil.ModelAnvil;
import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.swing.*;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Arrays;

public class ModelUtilities
{
    private final Client client;
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final Gson gson;

    @Inject
    public ModelUtilities(Client client, ClientThread clientThread, CreatorsPlugin plugin, Gson gson)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.gson = gson;
    }

    public Model createComplexModel(DetailedModel[] detailedModels, boolean setPriority, LightingStyle lightingStyle, CustomLighting cl, boolean sendModelStats)
    {
        ModelData modelData = createComplexModelData(detailedModels);

        if (cl == null)
        {
            cl = new CustomLighting(lightingStyle.getAmbient(), lightingStyle.getContrast(), lightingStyle.getX(), lightingStyle.getY(), lightingStyle.getZ());
        }

        CustomLighting finalLighting;
        if (lightingStyle == LightingStyle.CUSTOM)
        {
            finalLighting = cl;
        }
        else
        {
            finalLighting = new CustomLighting(lightingStyle.getAmbient(), lightingStyle.getContrast(), lightingStyle.getX(), lightingStyle.getY(), lightingStyle.getZ());
        }

        Model model;
        try
        {
            model = modelData.light(
                    finalLighting.getAmbient(),
                    finalLighting.getContrast(),
                    finalLighting.getX(),
                    finalLighting.getZ() * -1,
                    finalLighting.getY());
        }
        catch (Exception e)
        {
            sendChatMessage("Could not Forge this model with the chosen Lighting Settings. Please adjust them and try again.");
            return null;
        }

        if (model == null)
            return null;

        if (setPriority)
        {
            byte[] renderPriorities = model.getFaceRenderPriorities();
            if (renderPriorities != null && renderPriorities.length > 0)
                Arrays.fill(renderPriorities, (byte) 0);
        }

        if (sendModelStats)
        {
            sendChatMessage("Model forged. Faces: " + model.getFaceCount() + ", Vertices: " + model.getVerticesCount());
        }

        if (model.getFaceCount() >= 6200 && model.getVerticesCount() >= 3900)
            sendChatMessage("You've exceeded the max face count of 6200 or vertex count of 3900 in this model; any additional faces or vertices will not render");

        return model;
    }

    public ModelData createComplexModelData(DetailedModel[] detailedModels)
    {
        ModelData[] models = new ModelData[detailedModels.length];
        boolean[] facesToInvert = new boolean[0];

        for (int e = 0; e < detailedModels.length; e++)
        {
            DetailedModel detailedModel = detailedModels[e];
            ModelData modelData = client.loadModelData(detailedModel.getModelId());
            if (modelData == null)
                return null;

            modelData.cloneVertices().cloneColors();

            switch(detailedModel.getRotate())
            {
                case 0:
                    break;
                case 1:
                    modelData.rotateY270Ccw();
                    break;
                case 2:
                    modelData.rotateY180Ccw();
                    break;
                case 3:
                    modelData.rotateY90Ccw();
            }

            //swapping y and z, making y positive to align with traditional axes
            modelData.scale(detailedModel.getXScale(), detailedModel.getZScale(), detailedModel.getYScale());
            modelData.translate(detailedModel.getXTranslate() + detailedModel.getXTile() * 128, -1 * (detailedModel.getZTranslate() + detailedModel.getZTile() * 128), detailedModel.getYTranslate() + detailedModel.getYTile() * 128);

            boolean[] faceInvert = new boolean[modelData.getFaceCount()];
            Arrays.fill(faceInvert, detailedModel.isInvertFaces());
            facesToInvert = ArrayUtils.addAll(facesToInvert, faceInvert);

            short[] coloursFrom = detailedModel.getColoursFrom();
            short[] coloursTo = detailedModel.getColoursTo();

            if (coloursFrom == null || coloursTo == null)
            {
                if (!detailedModel.getRecolourNew().isEmpty() && !detailedModel.getRecolourOld().isEmpty())
                {
                    String[] newColoursArray = detailedModel.getRecolourNew().split(",");
                    coloursTo = new short[newColoursArray.length];
                    String[] oldColoursArray = detailedModel.getRecolourOld().split(",");
                    coloursFrom = new short[oldColoursArray.length];

                    for (int i = 0; i < coloursFrom.length; i++)
                    {
                        coloursFrom[i] = Short.parseShort(oldColoursArray[i]);
                        coloursTo[i] = Short.parseShort(newColoursArray[i]);
                    }
                }
                else
                {
                    coloursFrom = new short[0];
                    coloursTo = new short[0];
                }

                detailedModel.setColoursFrom(coloursFrom);
                detailedModel.setColoursTo(coloursTo);
            }

            for (int i = 0; i < coloursTo.length; i++)
            {
                modelData.recolor(coloursFrom[i], coloursTo[i]);
            }

            short[] texturesFrom = detailedModel.getTexturesFrom();
            short[] texturesTo = detailedModel.getTexturesTo();
            if (texturesFrom != null && texturesTo != null)
            {
                try
                {
                    modelData.cloneTextures();
                    for (int i = 0; i < texturesTo.length; i++)
                    {
                        modelData.retexture(texturesFrom[i], texturesTo[i]);
                    }
                }
                catch (Exception f)
                {
                }
            }
            else
            {
                detailedModel.setTexturesFrom(new short[0]);
                detailedModel.setTexturesFrom(new short[0]);
            }

            models[e] = modelData;
        }

        ModelData modelData = client.mergeModels(models);

        int[] faces2 = modelData.getFaceIndices2();
        int[] faces3 = modelData.getFaceIndices3();
        int[] faces2Copy = Arrays.copyOf(faces2, faces2.length);
        for (int i = 0; i < modelData.getFaceCount(); i++)
        {
            if (facesToInvert[i])
            {
                faces2[i] = faces3[i];
                faces3[i] = faces2Copy[i];
            }
        }

        return modelData;
    }

    public void cacheToAnvil(CustomModelType type, int id)
    {
        ModelStats[] modelStats;
        DataFinder dataFinder = plugin.getDataFinder();

        switch (type)
        {
            case CACHE_NPC:
                modelStats = dataFinder.findModelsForNPC(id);
                break;
            default:
            case CACHE_OBJECT:
                modelStats = dataFinder.findModelsForObject(id, -1, LightingStyle.DEFAULT, true);
                break;
            case CACHE_GROUND_ITEM:
                modelStats = dataFinder.findModelsForGroundItem(id, CustomModelType.CACHE_GROUND_ITEM);
                break;
            case CACHE_MAN_WEAR:
                modelStats = dataFinder.findModelsForGroundItem(id, CustomModelType.CACHE_MAN_WEAR);
                break;
            case CACHE_WOMAN_WEAR:
                modelStats = dataFinder.findModelsForGroundItem(id, CustomModelType.CACHE_WOMAN_WEAR);
                break;
            case CACHE_SPOTANIM:
                modelStats = dataFinder.findSpotAnim(id);
        }

        if (modelStats == null || modelStats.length == 0)
        {
            sendChatMessage("Could not find the " + type + " you were looking for in the cache.");
            return;
        }

        cacheToAnvil(modelStats, new int[0], type);
        sendChatMessage("Model sent to Anvil: " + modelStats[0].getName());
    }

    public void cacheToAnvil(ModelStats[] modelStatsArray, int[] kitRecolours, CustomModelType type)
    {
        SwingUtilities.invokeLater(() ->
        {
            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();

            for (ModelStats modelStats : modelStatsArray)
            {
                int id = modelStats.getModelId();
                String name;

                switch (type)
                {
                    default:
                    case CACHE_NPC:
                        name = plugin.getDataFinder().generateNameFromModel(id);
                        break;
                    case CACHE_PLAYER:
                    case CACHE_MAN_WEAR:
                    case CACHE_WOMAN_WEAR:
                    case CACHE_GROUND_ITEM:
                        name = modelStats.getName();
                }

                int group = type == CustomModelType.CACHE_PLAYER ? 9 : 8;
                int tz = modelStats.getTranslateZ();
                int rx = modelStats.getResizeX();
                int ry = modelStats.getResizeY();
                int rz = modelStats.getResizeZ();
                short[] textFrom = modelStats.getTextureFrom();
                short[] textTo = modelStats.getTextureTo();
                short[] itemRecolourTo = modelStats.getRecolourTo();
                short[] itemRecolourFrom = modelStats.getRecolourFrom();

                if (type == CustomModelType.CACHE_PLAYER)
                {
                    short[] kitRecolourTo = KitRecolourer.getKitRecolourTo(modelStats.getBodyPart(), kitRecolours);
                    short[] kitRecolourFrom = KitRecolourer.getKitRecolourFrom(modelStats.getBodyPart());

                    itemRecolourTo = ArrayUtils.addAll(itemRecolourTo, kitRecolourTo);
                    itemRecolourFrom = ArrayUtils.addAll(itemRecolourFrom, kitRecolourFrom);
                }

                creatorsPanel.getModelAnvil().createComplexPanel(
                        name,
                        id,
                        group,
                        0, 0, 0,
                        0, 0, tz,
                        rx, ry, rz,
                        0,
                        "", "",
                        itemRecolourFrom, itemRecolourTo,
                        textFrom, textTo,
                        false);
            }

            ModelAnvil modelAnvil = creatorsPanel.getModelAnvil();
            modelAnvil.generateNames();
            modelAnvil.updateRenderPanel();
        });
    }

    public void cacheToCustomModel(CustomModelType type, int id, int modelType)
    {
        DataFinder dataFinder = plugin.getDataFinder();
        Thread thread = new Thread(() ->
        {
            ModelStats[] modelStats;
            CustomModelComp comp;
            CustomLighting lighting;

            switch (type)
            {
                case CACHE_NPC:
                    modelStats = dataFinder.findModelsForNPC(id);
                    break;
                default:
                case CACHE_OBJECT:
                    modelStats = dataFinder.findModelsForObject(id, modelType, LightingStyle.DEFAULT, false);
                    break;
                case CACHE_GROUND_ITEM:
                    modelStats = dataFinder.findModelsForGroundItem(id, CustomModelType.CACHE_GROUND_ITEM);
                    break;
                case CACHE_MAN_WEAR:
                    modelStats = dataFinder.findModelsForGroundItem(id, CustomModelType.CACHE_MAN_WEAR);
                    break;
                case CACHE_WOMAN_WEAR:
                    modelStats = dataFinder.findModelsForGroundItem(id, CustomModelType.CACHE_WOMAN_WEAR);
                    break;
                case CACHE_SPOTANIM:
                    modelStats = dataFinder.findSpotAnim(id);
            }

            if (modelStats == null || modelStats.length == 0)
            {
                sendChatMessage("Could not find the " + type + " you were looking for in the cache.");
                return;
            }

            String name = modelStats[0].getName();

            switch (type)
            {
                case CACHE_NPC:
                    lighting = new CustomLighting(64, 850, -30, -30, 50);
                    comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, id, modelStats, null, null, null, LightingStyle.ACTOR, lighting, false, name);
                    break;
                default:
                case CACHE_OBJECT:
                    lighting = modelStats[0].getLighting();
                    comp = new CustomModelComp(0, CustomModelType.CACHE_OBJECT, id, modelStats, null, null, null, LightingStyle.CUSTOM, lighting, false, name);
                    break;
                case CACHE_GROUND_ITEM:
                    lighting = new CustomLighting(64, 768, -50, -50, 10);
                    comp = new CustomModelComp(0, CustomModelType.CACHE_GROUND_ITEM, id, modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, name);
                    break;
                case CACHE_MAN_WEAR:
                    lighting = new CustomLighting(64, 768, -50, -50, 10);
                    comp = new CustomModelComp(0, CustomModelType.CACHE_MAN_WEAR, id, modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, name);
                    break;
                case CACHE_WOMAN_WEAR:
                    lighting = new CustomLighting(64, 768, -50, -50, 10);
                    comp = new CustomModelComp(0, CustomModelType.CACHE_WOMAN_WEAR, id, modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, name);
                    break;
                case CACHE_SPOTANIM:
                    lighting = modelStats[0].getLighting();
                    comp = new CustomModelComp(0, CustomModelType.CACHE_SPOTANIM, id, modelStats, null, null, null, LightingStyle.CUSTOM, lighting, false, name);
                    break;
            }

            clientThread.invokeLater(() ->
            {
                Model model = constructModelFromCache(modelStats, new int[0], false, LightingStyle.CUSTOM, lighting);
                CustomModel customModel = new CustomModel(model, comp);
                addCustomModel(customModel, false);
                sendChatMessage("Model stored: " + name);
            });
        });
        thread.start();
    }

    public Model constructModelFromCache(ModelStats[] modelStatsArray, int[] kitRecolours, boolean player, LightingStyle ls, CustomLighting cl)
    {
        ModelData md = constructModelDataFromCache(modelStatsArray, kitRecolours, player);
        if (ls == LightingStyle.CUSTOM)
        {
            return client.mergeModels(md).light(cl.getAmbient(), cl.getContrast(), cl.getX(), -cl.getZ(), cl.getY());
        }

        return client.mergeModels(md).light(ls.getAmbient(), ls.getContrast(), ls.getX(), -ls.getZ(), ls.getY());
    }

    public ModelData constructModelDataFromCache(ModelStats[] modelStatsArray, int[] kitRecolours, boolean player)
    {
        ModelData[] mds = new ModelData[modelStatsArray.length];

        for (int i = 0; i < modelStatsArray.length; i++)
        {
            ModelStats modelStats = modelStatsArray[i];
            ModelData modelData = client.loadModelData(modelStats.getModelId());

            if (modelData == null)
                continue;

            modelData.cloneColors().cloneVertices();

            for (short s = 0; s < modelStats.getRecolourFrom().length; s++)
                modelData.recolor(modelStats.getRecolourFrom()[s], modelStats.getRecolourTo()[s]);

            if (player)
                KitRecolourer.recolourKitModel(modelData, modelStats.getBodyPart(), kitRecolours);

            short[] textureFrom = modelStats.getTextureFrom();
            short[] textureTo = modelStats.getTextureTo();

            if (textureFrom == null || textureTo == null)
            {
                modelStats.setTextureFrom(new short[0]);
                modelStats.setTextureTo(new short[0]);
            }

            textureFrom = modelStats.getTextureFrom();
            textureTo = modelStats.getTextureTo();

            if (textureFrom.length > 0 && textureTo.length > 0)
            {
                for (int e = 0; e < textureFrom.length; e++)
                {
                    modelData.retexture(textureFrom[e], textureTo[e]);
                }
            }

            if (modelStats.getResizeX() == 0 && modelStats.getResizeY() == 0 && modelStats.getResizeZ() == 0)
            {
                modelStats.setResizeX(128);
                modelStats.setResizeY(128);
                modelStats.setResizeZ(128);
            }

            modelData.scale(modelStats.getResizeX(), modelStats.getResizeZ(), modelStats.getResizeY());

            modelData.translate(0, -1 * modelStats.getTranslateZ(), 0);

            mds[i] = modelData;
        }

        return client.mergeModels(mds);
    }

    public void customModelToAnvil(CustomModel customModel)
    {
        if (customModel.getComp().getType() == CustomModelType.BLENDER)
        {
            sendChatMessage("Blender models cannot currently be used in the Anvil.");
            return;
        }

        ModelAnvil modelAnvil = plugin.getCreatorsPanel().getModelAnvil();

        SwingUtilities.invokeLater(() ->
        {
            CustomModelComp comp = customModel.getComp();
            sendChatMessage("Model sent to Anvil: " + comp.getName());

            CustomLighting cl;
            LightingStyle lightingStyle = comp.getLightingStyle();
            if (lightingStyle == LightingStyle.CUSTOM)
            {
                cl = comp.getCustomLighting();
            }
            else
            {
                cl = new CustomLighting(
                        lightingStyle.getAmbient(),
                        lightingStyle.getContrast(),
                        lightingStyle.getX(),
                        lightingStyle.getY(),
                        lightingStyle.getZ());
            }

            modelAnvil.setLightingSettings(
                    comp.getLightingStyle(),
                    cl.getAmbient(),
                    cl.getContrast(),
                    cl.getX(),
                    cl.getY(),
                    cl.getZ());

            modelAnvil.getPriorityCheckBox().setSelected(comp.isPriority());
            modelAnvil.getNameField().setText(comp.getName());

            if (comp.getModelStats() == null)
            {
                DetailedModel[] detailedModels = comp.getDetailedModels();
                for (DetailedModel detailedModel : detailedModels)
                    modelAnvil.createComplexPanel(detailedModel);

                return;
            }

            cacheToAnvil(comp.getModelStats(), comp.getKitRecolours(), comp.getType());
        });
    }

    public void loadCustomModelToAnvil(File file)
    {
        ModelAnvil modelAnvil = plugin.getCreatorsPanel().getModelAnvil();

        try
        {
            Reader reader = Files.newBufferedReader(file.toPath());
            CustomModelComp comp = gson.fromJson(reader, CustomModelComp.class);

            SwingUtilities.invokeLater(() ->
            {
                for (DetailedModel detailedModel : comp.getDetailedModels())
                {
                    modelAnvil.createComplexPanel(detailedModel);
                }

                modelAnvil.updateRenderPanel();
            });

            LightingStyle ls = comp.getLightingStyle();
            CustomLighting cl = comp.getCustomLighting();
            if (cl == null)
                cl = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());

            modelAnvil.setLightingSettings(
                    comp.getLightingStyle(),
                    cl.getAmbient(),
                    cl.getContrast(),
                    cl.getX(),
                    cl.getY(),
                    cl.getZ());

            modelAnvil.getPriorityCheckBox().setSelected(comp.isPriority());
            modelAnvil.getNameField().setText(comp.getName());
            reader.close();
        }
        catch (Exception e)
        {
            sendChatMessage("Failed to load this Saved Model file.");
        }
    }

    public void loadCustomModel(File file)
    {
        try
        {
            Reader reader = Files.newBufferedReader(file.toPath());
            CustomModelComp comp = gson.fromJson(reader, CustomModelComp.class);
            clientThread.invokeLater(() ->
            {
                LightingStyle ls = comp.getLightingStyle();
                CustomLighting cl = comp.getCustomLighting();
                if (cl == null)
                    cl = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());

                Model model = createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle(), cl, true);
                CustomModel customModel = new CustomModel(model, comp);
                addCustomModel(customModel, false);
            });
            reader.close();
        }
        catch (Exception e)
        {
            sendChatMessage("Failed to load this Saved Model file.");
        }
    }

    public void loadTransmog(File file, TransmogLoadOption transmogLoadOption)
    {
        CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
        TransmogPanel transmogPanel = creatorsPanel.getToolBox().getTransmogPanel();

        try
        {
            Reader reader = Files.newBufferedReader(file.toPath());
            TransmogSave transmogSave = gson.fromJson(reader, TransmogSave.class);
            CustomModelComp comp = transmogSave.getCustomModelComp();
            if (comp != null)
            {
                DetailedModel[] detailedModels = comp.getDetailedModels();
                if (detailedModels == null)
                {
                    detailedModels = creatorsPanel.getModelOrganizer().modelToDetailedPanels(comp);
                    comp.setDetailedModels(detailedModels);
                }
            }

            reader.close();

            boolean loadCustomModel = false;
            switch (transmogLoadOption)
            {
                case ANIMATIONS:
                    transmogPanel.loadTransmog(transmogSave);
                    break;
                case CUSTOM_MODEL:
                    if (comp != null)
                        loadCustomModel = true;
                    break;
                case BOTH:
                    transmogPanel.loadTransmog(transmogSave);
                    if (comp != null)
                        loadCustomModel = true;
            }

            if (loadCustomModel)
            {
                clientThread.invokeLater(() ->
                {
                    LightingStyle ls = comp.getLightingStyle();
                    CustomLighting cl = comp.getCustomLighting();
                    if (cl == null)
                        cl = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());
                    Model model = createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle(), cl, false);
                    CustomModel customModel = new CustomModel(model, comp);
                    addCustomModel(customModel, false);
                    transmogPanel.setTransmog(customModel);
                });
            }
        }
        catch (Exception e)
        {
            sendChatMessage("Failed to load the selected Transmog. Make sure you selected an appropriate transmog file.");
        }
    }

    public void addCustomModel(CustomModel customModel, boolean setComboBox)
    {
        SwingUtilities.invokeLater(() -> plugin.getCreatorsPanel().addModelOption(customModel, setComboBox));
        plugin.getStoredModels().add(customModel);
    }

    public void removeCustomModel(CustomModel customModel)
    {
        plugin.getCreatorsPanel().removeModelOption(customModel);
        plugin.getStoredModels().remove(customModel);
    }

    public void updatePanelComboBoxes()
    {
        SwingUtilities.invokeLater(() ->
        {
            for (JComboBox<CustomModel> comboBox : plugin.getCreatorsPanel().getComboBoxes())
                comboBox.updateUI();
        });
    }

    private void sendChatMessage(String message)
    {
        plugin.sendChatMessage(message);
    }
}
