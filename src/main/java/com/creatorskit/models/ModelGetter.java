package com.creatorskit.models;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.ModelOrganizer;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;

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

    public void storeNPC(int index, String target, NPC npc, String option, boolean setTransmog)
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
                            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, npc.getId(), modelStats, null, null, null, LightingStyle.ACTOR, false, npc.getName());
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

    public void addSpotAnimGetter(int index, String target, String option, Player player, boolean sendToAnvil)
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
                        for (ActorSpotAnim spotAnim : player.getSpotAnims())
                        {
                            Thread thread = new Thread(() ->
                            {
                                ModelStats[] modelStats = modelFinder.findSpotAnim(spotAnim.getId());
                                clientThread.invokeLater(() ->
                                {
                                    plugin.cacheToAnvil(modelStats, new int[0], false);
                                    plugin.sendChatMessage("Model sent to Anvil: " + spotAnim.getId() + "; Animation: " + modelFinder.getLastAnim());
                                });
                            });
                            thread.start();
                        }
                        return;
                    }

                    //For "Store" option on SpotAnims
                    for (ActorSpotAnim spotAnim : player.getSpotAnims())
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findSpotAnim(spotAnim.getId());
                            clientThread.invokeLater(() ->
                            {
                                String name = "SpotAnim " + spotAnim.getId();
                                CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_OBJECT, spotAnim.getId(), modelStats, null, null, null, LightingStyle.ACTOR, false, name);

                                ModelData modelData = client.loadModelData(modelStats[0].getModelId()).cloneColors().cloneVertices();
                                short[] recolFrom = modelStats[0].getRecolourFrom();
                                short[] recolTo = modelStats[0].getRecolourTo();
                                for (int i = 0; i < recolFrom.length; i++)
                                    modelData.recolor(recolFrom[i], recolTo[i]);
                                modelData.scale(modelStats[0].getResizeX(), modelStats[0].getResizeZ(), modelStats[0].getResizeY());

                                Lighting lighting = modelStats[0].getLighting();
                                Model model = modelData.light(lighting.getAmbient(), lighting.getContrast(), lighting.getX(), lighting.getY(), lighting.getZ());
                                CustomModel customModel = new CustomModel(model, comp);
                                plugin.addCustomModel(customModel, false);
                                plugin.sendChatMessage("Model stored: " + name + "; Animation: " + modelFinder.getLastAnim());
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
                            CustomModelComp composition = new CustomModelComp(0, CustomModelType.CACHE_PLAYER, -1, modelStats, comp.getColors(), null, null, LightingStyle.ACTOR, false, finalName);
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
                        CustomModelComp comp = new CustomModelComp(0, type, objectId, modelStats, null, null, null, LightingStyle.DEFAULT, false, name);
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
