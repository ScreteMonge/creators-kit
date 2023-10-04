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
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private CreatorsPlugin plugin;
    @Inject
    private ModelFinder modelFinder;
    @Inject
    private ModelOrganizer modelOrganizer;
    @Inject
    private CreatorsConfig config;

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
                            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, npc.getId(), modelStats, null, null, LightingStyle.ACTOR, false, npc.getName());
                            CustomModel customModel = new CustomModel(model, comp);
                            plugin.addCustomModel(customModel, false);
                            plugin.sendChatMessage("Model stored: " + npc.getName());
                            if (setTransmog)
                                modelOrganizer.setTransmog(customModel);
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

                    //For "Anvil" option on players
                    if (sendToAnvil)
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items);
                            clientThread.invokeLater(() ->
                            {
                                plugin.cacheToAnvil(modelStats, comp.getColors(), true);
                                plugin.sendChatMessage("Model sent to Anvil: " + Text.removeTags(target));
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
                            CustomModelComp composition = new CustomModelComp(0, CustomModelType.CACHE_PLAYER, -1, modelStats, comp.getColors(), null, LightingStyle.ACTOR, false, player.getName());
                            CustomModel customModel = new CustomModel(model, composition);
                            plugin.addCustomModel(customModel, false);
                            plugin.sendChatMessage("Model stored: " + Text.removeTags(target));
                            if (setTransmog)
                                modelOrganizer.setTransmog(customModel);
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
                        CustomModelComp comp = new CustomModelComp(0, type, objectId, modelStats, null, null, LightingStyle.DEFAULT, false, name);
                        CustomModel customModel = new CustomModel(model, comp);
                        plugin.addCustomModel(customModel, false);
                        plugin.sendChatMessage("Model stored: " + name);
                        if (setTransmog)
                            modelOrganizer.setTransmog(customModel);
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
