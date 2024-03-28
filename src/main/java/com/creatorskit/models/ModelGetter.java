package com.creatorskit.models;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;

public class ModelGetter
{
    private final Client client;
    private final ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final ModelFinder modelFinder;
    private final CreatorsConfig config;

    @Inject
    public ModelGetter(Client client, ClientThread clientThread, CreatorsPlugin plugin, ModelFinder modelFinder, CreatorsConfig config)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.modelFinder = modelFinder;
        this.config = config;
    }

    public void storeNPC(int index, String target, String option, NPC npc, boolean setTransmog)
    {
        if (setTransmog && !config.transmogRightClick())
            return;

        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForNPC(npc.getId());
                        clientThread.invokeLater(() ->
                        {
                            Model model = plugin.constructModelFromCache(modelStats, new int[0], false, true);
                            CustomLighting lighting = new CustomLighting(64, 850, -30, -30, 50);
                            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, npc.getId(), modelStats, null, null, null, LightingStyle.ACTOR, lighting, false, npc.getName());
                            CustomModel customModel = new CustomModel(model, comp);
                            plugin.addCustomModel(customModel, false);
                            plugin.sendChatMessage("Model stored: " + npc.getName());
                            if (setTransmog)
                                plugin.getCreatorsPanel().getModelOrganizer().setTransmog(customModel);
                        });
                    });
                    thread.start();
                });
    }

    public void sendToAnvilNPC(int index, String target, NPC npc)
    {
        client.createMenuEntry(index)
                .setOption("Anvil")
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForNPC(npc.getId());
                        clientThread.invokeLater(() ->
                        {
                            plugin.cacheToAnvil(modelStats, new int[0], false);
                            plugin.sendChatMessage("Model sent to Anvil: " + npc.getName());
                        });
                    });
                    thread.start();

                });
    }

    public void addSpotAnimGetter(int index, String target, String option, IterableHashTable<ActorSpotAnim> spotAnims, boolean sendToAnvil)
    {
        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    //For "Anvil" option on SpotAnims
                    if (sendToAnvil)
                    {
                        for (ActorSpotAnim spotAnim : spotAnims)
                        {
                            Thread thread = new Thread(() ->
                            {
                                ModelStats[] modelStats = modelFinder.findSpotAnim(spotAnim.getId());
                                clientThread.invokeLater(() ->
                                {
                                    CustomLighting lighting = modelStats[0].getLighting();
                                    plugin.cacheToAnvil(modelStats, new int[0], false);
                                    plugin.sendChatMessage("Model sent to Anvil: " + spotAnim.getId() + "; Anim: " + modelFinder.getLastAnim() + "; Ambient/Contrast: " + lighting.getAmbient() + "/" + lighting.getContrast());
                                });
                            });
                            thread.start();
                        }
                        return;
                    }

                    //For "Store" option on SpotAnims
                    for (ActorSpotAnim spotAnim : spotAnims)
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findSpotAnim(spotAnim.getId());
                            clientThread.invokeLater(() ->
                            {
                                String name = "SpotAnim " + spotAnim.getId();
                                CustomLighting lighting = modelStats[0].getLighting();
                                CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_OBJECT, spotAnim.getId(), modelStats, null, null, null, LightingStyle.CUSTOM, lighting, false, name);

                                ModelData modelData = client.loadModelData(modelStats[0].getModelId()).cloneColors().cloneVertices();
                                short[] recolFrom = modelStats[0].getRecolourFrom();
                                short[] recolTo = modelStats[0].getRecolourTo();
                                for (int i = 0; i < recolFrom.length; i++)
                                    modelData.recolor(recolFrom[i], recolTo[i]);
                                modelData.scale(modelStats[0].getResizeX(), modelStats[0].getResizeZ(), modelStats[0].getResizeY());

                                Model model = modelData.light(lighting.getAmbient(), lighting.getContrast(), lighting.getX(), lighting.getZ() * -1, lighting.getY());
                                CustomModel customModel = new CustomModel(model, comp);
                                plugin.addCustomModel(customModel, false);
                                plugin.sendChatMessage("Model stored: " + name + "; Anim: " + modelFinder.getLastAnim() + "; Ambient/Contrast: " + lighting.getAmbient() + "/" + lighting.getContrast());
                            });
                        });
                        thread.start();
                    }
                });
    }

    public void addPlayerGetter(int index, String target, String option, Player player, boolean sendToAnvil, boolean setTransmog)
    {
        if (setTransmog && !config.transmogRightClick())
            return;

        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    PlayerComposition comp = player.getPlayerComposition();
                    int[] items = comp.getEquipmentIds();
                    String name = player.getName();
                    if (player == client.getLocalPlayer())
                        name = "Local Player";
                    String finalName = name;

                    //For "Anvil" option on players
                    if (sendToAnvil)
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items);
                            clientThread.invokeLater(() ->
                            {
                                plugin.cacheToAnvil(modelStats, comp.getColors(), true);
                                plugin.sendChatMessage("Model sent to Anvil: " + finalName);
                            });
                        });
                        thread.start();
                        return;
                    }

                    //For "Store" option on players
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items);
                        clientThread.invokeLater(() ->
                        {
                            Model model = plugin.constructModelFromCache(modelStats, comp.getColors(), true, true);
                            CustomLighting lighting = new CustomLighting(64, 850, -30, -30, 50);
                            CustomModelComp composition = new CustomModelComp(0, CustomModelType.CACHE_PLAYER, -1, modelStats, comp.getColors(), null, null, LightingStyle.ACTOR, lighting, false, finalName);
                            CustomModel customModel = new CustomModel(model, composition);
                            plugin.addCustomModel(customModel, false);
                            plugin.sendChatMessage("Model stored: " + finalName);
                            if (setTransmog)
                                plugin.getCreatorsPanel().getModelOrganizer().setTransmog(customModel);
                        });
                    });
                    thread.start();
                });
    }

    public void addGameObjectGetter(int index, String option, String target, String name, Model model, int objectId, CustomModelType type, boolean setTransmog)
    {
        if (setTransmog && !config.transmogRightClick())
            return;

        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForObject(objectId);
                        CustomLighting lighting = new CustomLighting(64, 768, -50, -50, 10);
                        CustomModelComp comp = new CustomModelComp(0, type, objectId, modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, name);
                        CustomModel customModel = new CustomModel(model, comp);
                        plugin.addCustomModel(customModel, false);
                        plugin.sendChatMessage("Model stored: " + name);
                        if (setTransmog)
                            plugin.getCreatorsPanel().getModelOrganizer().setTransmog(customModel);
                    });
                    thread.start();
                });
    }

    public void addObjectGetterToAnvil(String target, String name, int objectId)
    {
        client.createMenuEntry(-2)
                .setOption("Anvil")
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        CountDownLatch countDownLatch = new CountDownLatch(1);
                        ModelStats[] modelStats = modelFinder.findModelsForObject(objectId);
                        clientThread.invokeLater(() ->
                        {
                            plugin.cacheToAnvil(modelStats, new int[0], false);
                            plugin.sendChatMessage("Model sent to Anvil: " + name);
                        });
                    });
                    thread.start();
                });
    }
}
