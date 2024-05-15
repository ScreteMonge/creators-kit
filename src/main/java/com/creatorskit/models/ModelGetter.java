package com.creatorskit.models;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.swing.ObjectPanel;
import net.runelite.api.*;
import net.runelite.api.kit.KitType;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

public class ModelGetter
{
    private final Client client;
    private final ClientThread clientThread;
    private final CreatorsConfig config;
    private final CreatorsPlugin plugin;
    private final ModelFinder modelFinder;
    private final ModelExporter modelExporter;
    public final File BLENDER_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/blender-models");

    @Inject
    public ModelGetter(Client client, ClientThread clientThread, CreatorsConfig config, CreatorsPlugin plugin, ModelFinder modelFinder, ModelExporter modelExporter)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.plugin = plugin;
        this.modelFinder = modelFinder;
        this.modelExporter = modelExporter;
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
                                ObjectPanel objectPanel = creatorsPanel.createPanel(
                                        creatorsPanel.getSidePanel(),
                                        npc.getName(),
                                        7699,
                                        customModel,
                                        true,
                                        false,
                                        npc.getOrientation(),
                                        npc.getPoseAnimation(),
                                        60,
                                        creatorsPanel.createEmptyProgram(npc.getPoseAnimation(), npc.getWalkAnimation()),
                                        false,
                                        null,
                                        null,
                                        new int[0],
                                        -1,
                                        false,
                                        false);

                                SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(creatorsPanel.getSideObjectPanels(), creatorsPanel.getSidePanel(), objectPanel));
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

    public void addNPCExporter(int index, String target, NPC npc)
    {
        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag("Export", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    if (config.vertexColours())
                    {
                        BlenderModel blenderModel = modelExporter.bmVertexColours(npc.getModel());
                        modelExporter.saveToFile(npc.getName(), blenderModel);
                        plugin.sendChatMessage("Exported " + npc.getName() + " to " + BLENDER_DIR.getAbsolutePath() + ".");
                    }
                    else
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForNPC(npc.getId());

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel blenderModel = modelExporter.bmFaceColours(modelStats, null, false, npc.getModel());
                                modelExporter.saveToFile(npc.getName(), blenderModel);
                                plugin.sendChatMessage("Exported " + npc.getName() + " to " + BLENDER_DIR.getAbsolutePath() + ".");
                            });
                        });
                        thread.start();
                    }
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

                                    ObjectPanel objectPanel = creatorsPanel.createPanel(
                                            creatorsPanel.getSidePanel(),
                                            name,
                                            7699,
                                            customModel,
                                            true,
                                            false,
                                            0,
                                            anim,
                                            60,
                                            creatorsPanel.createEmptyProgram(anim, anim),
                                            false,
                                            null,
                                            null,
                                            new int[0],
                                            -1,
                                            false,
                                            false);

                                    SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(creatorsPanel.getSideObjectPanels(), creatorsPanel.getSidePanel(), objectPanel));
                                }
                            });
                        });
                        thread.start();
                    }
                });
    }

    public void addSpotAnimExporter(int index, String target, IterableHashTable<ActorSpotAnim> spotAnims)
    {
        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag("Export SpotAnims", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        for (ActorSpotAnim spotAnim : spotAnims) {

                            ModelStats[] modelStats = modelFinder.findSpotAnim(spotAnim.getId());

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel blenderModel = modelExporter.bmFaceColours(modelStats, null, false, null);
                                String name = "SpotAnim " + spotAnim.getId();
                                modelExporter.saveToFile(name, blenderModel);
                                plugin.sendChatMessage("Exported " + name + " to " + BLENDER_DIR.getAbsolutePath() + ".");
                            });
                        }
                    });
                    thread.start();
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
                            ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items, player.getAnimation());
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
                        ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items, player.getAnimation());
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
                                ObjectPanel objectPanel = creatorsPanel.createPanel(
                                        creatorsPanel.getSidePanel(),
                                        finalName,
                                        7699,
                                        customModel,
                                        true,
                                        false,
                                        player.getCurrentOrientation(),
                                        player.getPoseAnimation(),
                                        60,
                                        creatorsPanel.createEmptyProgram(player.getPoseAnimation(), player.getWalkAnimation()),
                                        false,
                                        null,
                                        null,
                                        new int[0],
                                        -1,
                                        false,
                                        false);

                                SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(creatorsPanel.getSideObjectPanels(), creatorsPanel.getSidePanel(), objectPanel));
                            }
                        });
                    });
                    thread.start();
                });
    }

    public void addPlayerExporter(int index, String target, Player player)
    {
        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag("Export", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    PlayerComposition comp = player.getPlayerComposition();
/*
                    int amulet = comp.getEquipmentId(KitType.AMULET);
                    int amuletKit = comp.getKitId(KitType.AMULET);
                    int arms = comp.getEquipmentId(KitType.ARMS);
                    int armsKit = comp.getKitId(KitType.ARMS);
                    int cape = comp.getEquipmentId(KitType.CAPE);
                    int capeKit = comp.getKitId(KitType.CAPE);
                    int boots = comp.getEquipmentId(KitType.BOOTS);
                    int bootsKit = comp.getKitId(KitType.BOOTS);
                    int hair = comp.getEquipmentId(KitType.HAIR);
                    int hairKit = comp.getKitId(KitType.HAIR);
                    int jaw = comp.getEquipmentId(KitType.JAW);
                    int jawKit = comp.getKitId(KitType.JAW);
                    int legs = comp.getEquipmentId(KitType.LEGS);
                    int legsKit = comp.getKitId(KitType.LEGS);
                    int shield = comp.getEquipmentId(KitType.SHIELD);
                    int shieldKit = comp.getKitId(KitType.SHIELD);
                    int weapon = comp.getEquipmentId(KitType.WEAPON);
                    int weaponKit = comp.getKitId(KitType.WEAPON);
                    int hands = comp.getEquipmentId(KitType.HANDS);
                    int handsKit = comp.getKitId(KitType.HANDS);
                    int head = comp.getEquipmentId(KitType.HEAD);
                    int headKit = comp.getKitId(KitType.HEAD);
                    int torso = comp.getEquipmentId(KitType.TORSO);
                    int torsoKit = comp.getKitId(KitType.TORSO);
                    System.out.println("Ammy: " + amulet);
                    System.out.println("AmmyKit: " + amuletKit);
                    System.out.println("Arms: " + arms);
                    System.out.println("ArmsKit: " + armsKit);
                    System.out.println("Cape: " + cape);
                    System.out.println("CapeKit: " + capeKit);
                    System.out.println("Boots: " + boots);
                    System.out.println("BootsKit: " + bootsKit);
                    System.out.println("Hair: " + hair);
                    System.out.println("HairKit: " + hairKit);
                    System.out.println("Jaw: " + jaw);
                    System.out.println("JawKit: " + jawKit);
                    System.out.println("Legs: " + legs);
                    System.out.println("LegsKit: " + legsKit);
                    System.out.println("Shield: " + shield);
                    System.out.println("ShieldKit: " + shieldKit);
                    System.out.println("Weapon: " + weapon);
                    System.out.println("WeaponKit: " + weaponKit);
                    System.out.println("Hands: " + hands);
                    System.out.println("HandsKit: " + handsKit);
                    System.out.println("Head: " + head);
                    System.out.println("HeadKit: " + headKit);
                    System.out.println("Torso: " + torso);
                    System.out.println("TorsoKit: " + torsoKit);


 */
                    int[] items = comp.getEquipmentIds();
                    for (int i : items)
                    {
                        System.out.println("Item: " + i);
                    }

                    Model model = player.getModel();
                    int vCount = model.getVerticesCount();
                    int fCount = model.getFaceCount();
                    int[] vX = Arrays.copyOf(model.getVerticesX(), vCount);
                    int[] vY = Arrays.copyOf(model.getVerticesY(), vCount);
                    int[] vZ = Arrays.copyOf(model.getVerticesZ(), vCount);
                    int[] f1 = Arrays.copyOf(model.getFaceIndices1(), fCount);
                    int[] f2 = Arrays.copyOf(model.getFaceIndices2(), fCount);
                    int[] f3 = Arrays.copyOf(model.getFaceIndices3(), fCount);
                    byte[] renderPriorities;
                    if (model.getFaceRenderPriorities() == null)
                    {
                        renderPriorities = new byte[fCount];
                        Arrays.fill(renderPriorities, (byte) 0);
                    }
                    else
                    {
                        renderPriorities = model.getFaceRenderPriorities();
                    }
                    int animId = player.getAnimation();

                    System.out.println("True model FaceCount: " + model.getFaceCount());

                    String name = player.getName();
                    if (player == client.getLocalPlayer())
                        name = "Local Player";
                    String finalName = name;

                    if (config.vertexColours())
                    {
                        BlenderModel blenderModel = modelExporter.bmVertexColours(model);
                        modelExporter.saveToFile(finalName, blenderModel);
                        plugin.sendChatMessage("Exported " + finalName + " to " + BLENDER_DIR.getAbsolutePath() + ".");
                    }
                    else
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items, animId);

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel bm = modelExporter.bmFaceColoursForPlayer(
                                        modelStats,
                                        comp.getColors(),
                                        true,
                                        vX,
                                        vY,
                                        vZ,
                                        f1,
                                        f2,
                                        f3,
                                        renderPriorities);
                                modelExporter.saveToFile(finalName, bm);
                                plugin.sendChatMessage("Exported " + finalName + " to " + BLENDER_DIR.getAbsolutePath() + ".");
                            });
                        });
                        thread.start();
                    }
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
                            ObjectPanel objectPanel = creatorsPanel.createPanel(
                                    creatorsPanel.getSidePanel(),
                                    name,
                                    7699,
                                    customModel,
                                    true,
                                    false,
                                    orientation,
                                    animationId,
                                    60,
                                    creatorsPanel.createEmptyProgram(animationId, animationId),
                                    false,
                                    null,
                                    null,
                                    new int[0],
                                    -1,
                                    false,
                                    false);

                            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(creatorsPanel.getSideObjectPanels(), creatorsPanel.getSidePanel(), objectPanel));
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

    public void addObjectExporter(int index, String target, String name, int objectId, Model model)
    {
        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag("Export", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    if (config.vertexColours())
                    {
                        BlenderModel blenderModel = modelExporter.bmVertexColours(model);
                        modelExporter.saveToFile(name, blenderModel);
                        plugin.sendChatMessage("Exported " + name + " to " + BLENDER_DIR.getAbsolutePath() + ".");
                    }
                    else
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForObject(objectId);

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel blenderModel = modelExporter.bmFaceColours(modelStats, null, false, model);
                                modelExporter.saveToFile(name, blenderModel);
                                plugin.sendChatMessage("Exported " + name + " to " + BLENDER_DIR.getAbsolutePath() + ".");
                            });
                        });
                        thread.start();

                    }
                });
    }
}
