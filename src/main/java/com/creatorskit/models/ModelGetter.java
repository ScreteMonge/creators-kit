package com.creatorskit.models;

import com.creatorskit.CreatorsPlugin;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;

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
                            CustomModel customModel = new CustomModel(model, npc.getName());
                            plugin.getStoredModels().add(customModel);
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
                            plugin.sendChatMessage("Model sent to Anvil Complex Mode: " + npc.getName());
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

                    //Convert equipmentId to itemId or kitId as appropriate
                    int[] ids = new int[items.length];
                    boolean baldHead = false;
                    for (int i = 0; i < ids.length; i++)
                    {
                        int item = items[i];

                        if (item == 256)
                            baldHead = true;

                        if (item >= 256 && item <= 512)
                        {
                            ids[i] = item - 256;
                            continue;
                        }

                        if (item > 512)
                        {
                            ids[i] = item - 512;
                        }
                    }

                    boolean finalBaldHead = baldHead;
                    //For "Anvil" option on players
                    if (sendToAnvil)
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = ModelFinder.findModelsForPlayer(false, comp.getGender() == 0, finalBaldHead, ids);
                            clientThread.invokeLater(() ->
                            {
                                plugin.cacheToAnvil(modelStats, comp.getColors(), true);
                                plugin.sendChatMessage("Model sent to Anvil: " + player.getName());
                            });
                        });
                        thread.start();
                        return;
                    }

                    //For "Store" option on players
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = ModelFinder.findModelsForPlayer(false, comp.getGender() == 0, finalBaldHead, ids);
                        clientThread.invokeLater(() ->
                        {
                            Model model = plugin.constructModelFromCache(modelStats, comp.getColors(), true, true);
                            CustomModel customModel = new CustomModel(model, player.getName());
                            plugin.addCustomModel(customModel, false);
                        });
                    });
                    thread.start();
                });
    }

    public void addGameObjectGetter(String target, String name, Model model)
    {
        client.createMenuEntry(-1)
                .setOption("Store")
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    CustomModel customModel = new CustomModel(model, name);
                    plugin.addCustomModel(customModel, false);
                    plugin.sendChatMessage("Model stored: " + name);
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
