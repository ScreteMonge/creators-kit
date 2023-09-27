package com.creatorskit.models;

import com.creatorskit.CreatorsPlugin;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;

import javax.inject.Inject;

public class ModelGetter
{
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private CreatorsPlugin plugin;

    public void storeNPC(String target, NPC npc)
    {
        client.createMenuEntry(-1)
                .setOption("Store")
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = ModelFinder.findModelsForNPC(npc.getId());
                        clientThread.invokeLater(() ->
                        {
                            Model model = plugin.constructModelFromCache(modelStats, new int[0], false, true);
                            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, npc.getId(), modelStats, null, null, LightingStyle.ACTOR, false, npc.getName());
                            CustomModel customModel = new CustomModel(model, comp);
                            plugin.addCustomModel(customModel, false);
                            plugin.sendChatMessage("Model stored: " + npc.getName());
                        });
                    });
                    thread.start();
                });
    }

    public void sendToAnvilNPC(String target, NPC npc)
    {
        client.createMenuEntry(-2)
                .setOption("Anvil")
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = ModelFinder.findModelsForNPC(npc.getId());
                        clientThread.invokeLater(() ->
                        {
                            plugin.cacheToAnvil(modelStats, new int[0], false);
                            plugin.sendChatMessage("Model sent to Anvil: " + npc.getName());
                        });
                    });
                    thread.start();

                });
    }

    public void addPlayerGetter(int index, String target, String option, Player player, boolean sendToAnvil)
    {
        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    PlayerComposition comp = player.getPlayerComposition();
                    int[] items = comp.getEquipmentIds();

                    /*
                    ColorTextureOverride[] textures = comp.getColorTextureOverrides();
                    if (textures != null)
                    {
                        for (ColorTextureOverride colorTextureOverride : textures)
                        {
                            short[] replaceWith = colorTextureOverride.getColorToReplaceWith();
                            for (short s : replaceWith)
                            {
                                System.out.println("ColOverride: " + s);
                            }

                            short[] textureWith = colorTextureOverride.getTextureToReplaceWith();
                            for (short s : textureWith)
                            {
                                System.out.println("TextOverride: " + s);
                            }
                        }
                    }
                     */

                    //For "Anvil" option on players
                    if (sendToAnvil)
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = ModelFinder.findModelsForPlayer(false, comp.getGender() == 0, items);
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
                        ModelStats[] modelStats = ModelFinder.findModelsForPlayer(false, comp.getGender() == 0, items);
                        clientThread.invokeLater(() ->
                        {
                            Model model = plugin.constructModelFromCache(modelStats, comp.getColors(), true, true);
                            CustomModelComp composition = new CustomModelComp(0, CustomModelType.CACHE_PLAYER, -1, modelStats, comp.getColors(), null, LightingStyle.ACTOR, false, player.getName());
                            CustomModel customModel = new CustomModel(model, composition);
                            plugin.addCustomModel(customModel, false);
                            plugin.sendChatMessage("Model stored: " + Text.removeTags(target));
                        });
                    });
                    thread.start();
                });
    }

    public void addGameObjectGetter(String target, String name, Model model, int objectId, CustomModelType type)
    {
        client.createMenuEntry(-1)
                .setOption("Store")
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = ModelFinder.findModelsForObject(objectId);
                        CustomModelComp comp = new CustomModelComp(0, type, objectId, modelStats, null, null, LightingStyle.DEFAULT, false, name);
                        CustomModel customModel = new CustomModel(model, comp);
                        plugin.addCustomModel(customModel, false);
                        plugin.sendChatMessage("Model stored: " + name);
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
                        ModelStats[] modelStats = ModelFinder.findModelsForObject(objectId);
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
