package com.creatorskit.models;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.swing.ParentPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

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

    public void storeNPC(int index, String target, String option, NPC npc, ModelMenuOption menuOption)
    {
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
                            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();

                            if (menuOption == ModelMenuOption.TRANSMOG)
                            {
                                creatorsPanel.getModelOrganizer().setTransmog(customModel);
                            }

                            if (menuOption == ModelMenuOption.STORE_AND_ADD)
                            {
                                Character character = creatorsPanel.createCharacter(
                                        ParentPanel.SIDE_PANEL,
                                        npc.getName(),
                                        7699,
                                        customModel,
                                        true,
                                        false,
                                        npc.getOrientation(),
                                        npc.getPoseAnimation(),
                                        60,
                                        new KeyFrame[0][],
                                        creatorsPanel.createEmptyProgram(npc.getPoseAnimation(), npc.getWalkAnimation()),
                                        false,
                                        null,
                                        null,
                                        new int[0],
                                        -1,
                                        false,
                                        false);

                                SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character));
                            }
                        });
                    });
                    thread.start();
                });
    }

    public void sendToAnvilNPC(int index, String target, NPC npc)
    {
        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag("Anvil", Color.ORANGE))
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

    public void addSpotAnimGetter(int index, String target, String option, IterableHashTable<ActorSpotAnim> spotAnims, ModelMenuOption menuOption)
    {
        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    //For "Anvil" option on SpotAnims
                    if (menuOption == ModelMenuOption.ANVIL)
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

                                int anim = modelFinder.getLastAnim();
                                Model model = modelData.light(lighting.getAmbient(), lighting.getContrast(), lighting.getX(), lighting.getZ() * -1, lighting.getY());
                                CustomModel customModel = new CustomModel(model, comp);
                                plugin.addCustomModel(customModel, false);
                                plugin.sendChatMessage("Model stored: " + name + "; Anim: " + anim + "; Ambient/Contrast: " + lighting.getAmbient() + "/" + lighting.getContrast());

                                if (menuOption == ModelMenuOption.STORE_AND_ADD)
                                {
                                    CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();

                                    Character character = creatorsPanel.createCharacter(
                                            ParentPanel.SIDE_PANEL,
                                            name,
                                            7699,
                                            customModel,
                                            true,
                                            false,
                                            0,
                                            anim,
                                            60,
                                            new KeyFrame[0][],
                                            creatorsPanel.createEmptyProgram(anim, anim),
                                            false,
                                            null,
                                            null,
                                            new int[0],
                                            -1,
                                            false,
                                            false);

                                    SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character));
                                }
                            });
                        });
                        thread.start();
                    }
                });
    }

    public void addPlayerGetter(int index, String target, String option, Player player, ModelMenuOption menuOption)
    {
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
                    if (menuOption == ModelMenuOption.ANVIL)
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

                            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
                            if (menuOption == ModelMenuOption.TRANSMOG)
                            {
                                creatorsPanel.getModelOrganizer().setTransmog(customModel);
                            }

                            if (menuOption == ModelMenuOption.STORE_AND_ADD)
                            {
                                Character character = creatorsPanel.createCharacter(
                                        ParentPanel.SIDE_PANEL,
                                        finalName,
                                        7699,
                                        customModel,
                                        true,
                                        false,
                                        player.getCurrentOrientation(),
                                        player.getPoseAnimation(),
                                        60,
                                        new KeyFrame[0][],
                                        creatorsPanel.createEmptyProgram(player.getPoseAnimation(), player.getWalkAnimation()),
                                        false,
                                        null,
                                        null,
                                        new int[0],
                                        -1,
                                        false,
                                        false);

                                SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character));
                            }
                        });
                    });
                    thread.start();
                });
    }

    public void addGameObjectGetter(int index, String option, String target, String name, Model model, int objectId, CustomModelType type, int animationId, int orientation, ModelMenuOption menuOption)
    {
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

                        CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
                        if (menuOption == ModelMenuOption.TRANSMOG)
                        {
                            creatorsPanel.getModelOrganizer().setTransmog(customModel);
                        }

                        if (menuOption == ModelMenuOption.STORE_AND_ADD)
                        {
                            Character character = creatorsPanel.createCharacter(
                                    ParentPanel.SIDE_PANEL,
                                    name,
                                    7699,
                                    customModel,
                                    true,
                                    false,
                                    orientation,
                                    animationId,
                                    60,
                                    new KeyFrame[0][],
                                    creatorsPanel.createEmptyProgram(animationId, animationId),
                                    false,
                                    null,
                                    null,
                                    new int[0],
                                    -1,
                                    false,
                                    false);

                            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character));
                        }
                    });
                    thread.start();
                });
    }

    public void addObjectGetterToAnvil(String target, String name, int objectId)
    {
        client.createMenuEntry(-2)
                .setOption(ColorUtil.prependColorTag("Anvil", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
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
