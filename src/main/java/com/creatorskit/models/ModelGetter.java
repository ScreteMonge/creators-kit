package com.creatorskit.models;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.exporters.ModelExporter;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.swing.ObjectPanel;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ColorUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelGetter
{
    private final Client client;
    private final CreatorsPanel creatorsPanel;
    private final ClientThread clientThread;
    private final CreatorsConfig config;
    private final CreatorsPlugin plugin;
    private final ModelFinder modelFinder;
    private final ModelExporter modelExporter;
    private boolean initiateExport = false;
    private boolean continueAnimExport = false;
    private RuneLiteObject exportObject;
    private String exportName;
    private BlenderModel blenderModelExport;
    private int[] animFrames;
    private int[] clientTicks;
    private int[][][] animVerts;

    @Inject
    public ModelGetter(Client client, CreatorsPanel creatorsPanel, ClientThread clientThread, CreatorsConfig config, CreatorsPlugin plugin, ModelFinder modelFinder, ModelExporter modelExporter)
    {
        this.client = client;
        this.creatorsPanel = creatorsPanel;
        this.clientThread = clientThread;
        this.config = config;
        this.plugin = plugin;
        this.modelFinder = modelFinder;
        this.modelExporter = modelExporter;
    }

    public void addCharacterMenuEntries(Tile tile)
    {
        if (!config.rightSelect() && !config.exportRightClick())
        {
            return;
        }

        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            RuneLiteObject runeLiteObject = character.getRuneLiteObject();
            if (character.isActive() && runeLiteObject != null)
            {
                LocalPoint localPoint = runeLiteObject.getLocation();
                if (localPoint != null && localPoint.equals(tile.getLocalLocation()))
                {
                    if (config.rightSelect())
                    {
                        client.createMenuEntry(1)
                                .setOption(ColorUtil.prependColorTag("Select", Color.ORANGE))
                                .setTarget(ColorUtil.colorTag(Color.GREEN) + character.getName())
                                .setType(MenuAction.RUNELITE)
                                .onClick(e -> creatorsPanel.setSelectedCharacter(character, character.getObjectPanel()));
                    }

                    if (config.exportRightClick())
                    {
                        addRLObjectExporter(1, character, false);
                        addRLObjectExporter(1, character, true);
                    }
                }
            }
        }
    }

    public void addLocalPlayerMenuEntries(Tile tile)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null)
        {
            if (tile.getLocalLocation().equals(localPlayer.getLocalLocation()))
            {
                String target = ColorUtil.prependColorTag("Local Player", Color.GREEN);
                if (config.rightClick())
                {
                    if (client.isKeyPressed(KeyCode.KC_CONTROL))
                    {
                        addPlayerGetter(1, target, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), localPlayer, ModelMenuOption.STORE_AND_ADD);
                    }
                    else
                    {
                        addPlayerGetter(1, target, ColorUtil.prependColorTag("Store", Color.ORANGE), localPlayer, ModelMenuOption.STORE);
                    }
                    addPlayerGetter(1, target, ColorUtil.prependColorTag("Anvil", Color.ORANGE), localPlayer, ModelMenuOption.ANVIL);
                }

                if (config.rightSpotAnim())
                {
                    if (client.isKeyPressed(KeyCode.KC_CONTROL))
                    {
                        addSpotAnimGetter(1, target, ColorUtil.prependColorTag("SpotAnim-Store-Add", Color.ORANGE), localPlayer.getSpotAnims(), ModelMenuOption.STORE_AND_ADD);
                    }
                    else
                    {
                        addSpotAnimGetter(1, target, ColorUtil.prependColorTag("SpotAnim-Store", Color.ORANGE), localPlayer.getSpotAnims(), ModelMenuOption.STORE);
                    }
                    addSpotAnimGetter(1, target, ColorUtil.prependColorTag("SpotAnim-Anvil", Color.ORANGE), localPlayer.getSpotAnims(), ModelMenuOption.ANVIL);
                }

                if (config.exportRightClick())
                {
                    addPlayerExporter(1, target, localPlayer, false);
                    addPlayerExporter(1, target, localPlayer, true);
                }
            }
        }
    }

    public void addTileItemMenuEntries(Tile tile)
    {
        List<TileItem> tileItems = tile.getGroundItems();
        if (tileItems != null)
        {
            for (TileItem tileItem : tileItems)
            {
                int itemId = tileItem.getId();
                String name = client.getItemDefinition(itemId).getName();
                if (name.equals("null"))
                {
                    name = "Item";
                }

                Model model = tileItem.getModel();
                if (config.rightClick())
                {
                    if (client.isKeyPressed(KeyCode.KC_CONTROL))
                    {
                        addGroundItemGetter(1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, itemId, 0, ModelMenuOption.STORE_AND_ADD);
                    }
                    else
                    {
                        addGroundItemGetter(1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, itemId, 0, ModelMenuOption.STORE);
                    }

                    addGroundItemGetterToAnvil(1, name, itemId);
                }

                if (config.transmogRightClick())
                {
                    addGroundItemGetter(1, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, itemId, 0, ModelMenuOption.TRANSMOG);
                }

                if (config.exportRightClick())
                {
                    addGroundItemExporter(1, name, itemId, model);
                }
            }
        }
    }

    public void addTileObjectMenuEntries(Tile tile)
    {
        GameObject[] gameObjects = tile.getGameObjects();
        for (GameObject gameObject : gameObjects)
        {
            if (gameObject == null)
                continue;

            Renderable renderable = gameObject.getRenderable();
            if (renderable == null)
                continue;

            int objectId = gameObject.getId();
            int modelType = gameObject.getConfig() & 31;

            if (renderable instanceof Model)
            {
                ObjectComposition comp = client.getObjectDefinition(objectId);
                String name = comp.getName();
                if (name.equals("null"))
                {
                    name = "GameObj";
                }
                int animationId = -1;

                Model model = (Model) renderable;
                if (config.rightClick())
                {
                    if (client.isKeyPressed(KeyCode.KC_CONTROL))
                    {
                        addGameObjectGetter(1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.STORE_AND_ADD);
                    }
                    else
                    {
                        addGameObjectGetter(1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.STORE);
                    }
                    addObjectGetterToAnvil(1, name, gameObject.getId(), modelType, LightingStyle.DEFAULT);
                }

                if (config.transmogRightClick())
                {
                    addGameObjectGetter(1, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.TRANSMOG);
                }

                if (config.exportRightClick())
                {
                    addObjectExporter(1, name, objectId, modelType, model);
                }
            }

            if (renderable instanceof DynamicObject)
            {
                ObjectComposition comp = client.getObjectDefinition(objectId);
                DynamicObject dynamicObject = (DynamicObject) renderable;
                if (comp.getImpostorIds() != null)
                {
                    if (comp.getImpostor() != null)
                    {
                        comp = comp.getImpostor();
                    }
                }

                objectId = comp.getId();
                Animation animation = dynamicObject.getAnimation();
                int animationId = -1;
                if (animation != null)
                {
                    animationId = animation.getId();
                }

                String name = comp.getName();
                if (name.equals("null"))
                {
                    name = "GameObj";
                }

                if (config.rightClick())
                {
                    if (client.isKeyPressed(KeyCode.KC_CONTROL))
                    {
                        addDynamicObjectGetter(1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.STORE_AND_ADD);
                    }
                    else
                    {
                        addDynamicObjectGetter(1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.STORE);
                    }

                    addObjectGetterToAnvil(1, name, objectId, modelType, LightingStyle.DYNAMIC);
                }

                if (config.transmogRightClick())
                {
                    addDynamicObjectGetter(1, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.TRANSMOG);
                }

                if (config.exportRightClick())
                {
                    Model model = dynamicObject.getModel();
                    if (model != null)
                    {
                        addDynamicObjectExporter(1, name, objectId, modelType, animationId, dynamicObject.getModel(), false);
                        addDynamicObjectExporter(1, name, objectId, modelType, animationId, dynamicObject.getModel(), true);
                    }
                }
            }
        }

        GroundObject groundObject = tile.getGroundObject();
        if (groundObject != null)
        {
            Renderable renderable = groundObject.getRenderable();
            if (renderable instanceof Model)
            {
                Model model = (Model) groundObject.getRenderable();

                int objectId = groundObject.getId();
                int animationId = -1;
                int modelType = 22;
                String name = client.getObjectDefinition(objectId).getName();
                if (name.equals("null"))
                {
                    name = "GroundObj";
                }

                if (config.rightClick())
                {
                    if (client.isKeyPressed(KeyCode.KC_CONTROL))
                    {
                        addGameObjectGetter(1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE_AND_ADD);
                    }
                    else
                    {
                        addGameObjectGetter(1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE);

                    }
                    addObjectGetterToAnvil(1, name, objectId, modelType, LightingStyle.DEFAULT);
                }

                if (config.transmogRightClick())
                {
                    addGameObjectGetter(1, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.TRANSMOG);
                }

                if (config.exportRightClick())
                {
                    addObjectExporter(1, name, objectId, modelType,  model);
                }
            }
        }

        DecorativeObject decorativeObject = tile.getDecorativeObject();
        if (decorativeObject != null)
        {
            Renderable renderable = decorativeObject.getRenderable();
            if (renderable instanceof Model)
            {
                Model model = (Model) decorativeObject.getRenderable();

                int objectId = decorativeObject.getId();
                int animationId = -1;
                int modelType = 4;
                String name = client.getObjectDefinition(objectId).getName();
                if (name.equals("null"))
                {
                    name = "DecorativeObj";
                }

                if (config.rightClick())
                {
                    if (client.isKeyPressed(KeyCode.KC_CONTROL))
                    {
                        addGameObjectGetter(1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE_AND_ADD);
                    }
                    else
                    {
                        addGameObjectGetter(1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE);

                    }
                    addObjectGetterToAnvil(1, name, objectId, modelType, LightingStyle.DEFAULT);
                }

                if (config.transmogRightClick())
                {
                    addGameObjectGetter(1, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.TRANSMOG);
                }

                if (config.exportRightClick())
                {
                    addObjectExporter(1, name, objectId, modelType, model);
                }
            }
        }

        WallObject wallObject = tile.getWallObject();
        if (wallObject != null)
        {
            Renderable renderable = wallObject.getRenderable1();
            if (renderable instanceof Model)
            {
                Model model = (Model) renderable;

                int objectId = wallObject.getId();
                int animationId = -1;
                int modelType = tile.getWallObject().getConfig() & 31;
                String name = client.getObjectDefinition(objectId).getName();
                if (name.equals("null"))
                {
                    name = "WallObj";
                }

                if (config.rightClick())
                {
                    if (client.isKeyPressed(KeyCode.KC_CONTROL))
                    {
                        addGameObjectGetter(1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE_AND_ADD);
                    }
                    else
                    {
                        addGameObjectGetter(1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE);
                    }
                    addObjectGetterToAnvil(1, name, objectId, modelType, LightingStyle.DEFAULT);
                }

                if (config.transmogRightClick())
                {
                    addGameObjectGetter(1, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.TRANSMOG);
                }

                if (config.exportRightClick())
                {
                    addObjectExporter(1, name, objectId, modelType, model);
                }
            }
        }
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
                        if (modelStats == null || modelStats.length == 0)
                        {
                            plugin.sendChatMessage("Could not find this NPC in the cache.");
                            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                            return;
                        }

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
                        if (modelStats == null || modelStats.length == 0)
                        {
                            plugin.sendChatMessage("Could not find this NPC in the cache.");
                            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                            return;
                        }

                        clientThread.invokeLater(() ->
                        {
                            plugin.cacheToAnvil(modelStats, new int[0], false);
                            plugin.sendChatMessage("Model sent to Anvil: " + npc.getName());
                        });
                    });
                    thread.start();

                });
    }

    public void addNPCExporter(int index, String target, NPC npc, boolean exportAnimation)
    {
        String option = "Export Model";
        if (exportAnimation)
        {
            option = "Export Animation";
        }

        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag(option, Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    if (exportAnimation && continueAnimExport)
                    {
                        plugin.sendChatMessage("Please wait for the current animation export to finish before trying again.");
                        return;
                    }

                    int animId = npc.getAnimation();
                    int poseAnimId = npc.getPoseAnimation();
                    if (animId == -1)
                    {
                        animId = poseAnimId;
                    }
                    int finalAnimId = animId;

                    if (exportAnimation && animId == -1)
                    {
                        plugin.sendChatMessage("There is no animation currently playing to export.");
                        return;
                    }

                    int npcId = npc.getId();

                    String name = npc.getName();

                    if (config.vertexColours())
                    {
                        BlenderModel bm = modelExporter.bmVertexColours(npc.getModel());

                        if (exportAnimation)
                        {
                            Thread thread = new Thread(() ->
                            {
                                ModelStats[] modelStats = modelFinder.findModelsForNPC(npcId);
                                if (modelStats == null || modelStats.length == 0)
                                {
                                    plugin.sendChatMessage("Could not find this NPC in the cache.");
                                    plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                    return;
                                }

                                clientThread.invokeLater(() ->
                                {
                                    initiateAnimationExport(finalAnimId, name, bm, modelStats, new int[0], false, true);
                                });
                            });
                            thread.start();
                        }
                        else
                        {
                            if (config.exportTPose())
                            {
                                npc.setAnimation(-1);
                                npc.setPoseAnimation(-1);
                            }

                            modelExporter.saveToFile(name, bm);
                        }
                    }
                    else
                    {
                        Model model = npc.getModel();
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

                        byte[] transparencies;
                        if (model.getFaceTransparencies() == null)
                        {
                            transparencies = new byte[fCount];
                            Arrays.fill(transparencies, (byte) 0);
                        }
                        else
                        {
                            transparencies = model.getFaceTransparencies();
                        }

                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForNPC(npcId);
                            if (modelStats == null || modelStats.length == 0)
                            {
                                plugin.sendChatMessage("Could not find this NPC in the cache.");
                                plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                return;
                            }

                            clientThread.invokeLater(() ->
                            {
                                if (config.exportTPose() && !exportAnimation)
                                {
                                    npc.setAnimation(-1);
                                    npc.setPoseAnimation(-1);
                                }

                                BlenderModel bm = modelExporter.bmFaceColours(
                                        modelStats,
                                        false,
                                        new int[0],
                                        false,
                                        vX,
                                        vY,
                                        vZ,
                                        f1,
                                        f2,
                                        f3,
                                        transparencies,
                                        renderPriorities);

                                if (exportAnimation)
                                {
                                    initiateAnimationExport(finalAnimId, name, bm, modelStats, new int[0], false, true);
                                }
                                else
                                {
                                    modelExporter.saveToFile(name, bm);
                                }
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
                                if (modelStats == null || modelStats.length == 0)
                                {
                                    plugin.sendChatMessage("Could not find this Spotanim in the cache.");
                                    plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                    return;
                                }

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
                            if (modelStats == null || modelStats.length == 0)
                            {
                                plugin.sendChatMessage("Could not find this Spotanim in the cache.");
                                plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                return;
                            }

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
                        for (ActorSpotAnim spotAnim : spotAnims)
                        {
                            ModelStats[] modelStats = modelFinder.findSpotAnim(spotAnim.getId());
                            if (modelStats == null || modelStats.length == 0)
                            {
                                plugin.sendChatMessage("Could not find this Spotanim in the cache.");
                                plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                return;
                            }

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel blenderModel = modelExporter.bmSpotAnimFromCache(modelStats);
                                String name = "SpotAnim " + spotAnim.getId();
                                modelExporter.saveToFile(name, blenderModel);
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

                    IterableHashTable<ActorSpotAnim> actorSpotAnims = player.getSpotAnims();
                    int[] spotAnims = new int[0];
                    for (ActorSpotAnim actorSpotAnim : actorSpotAnims)
                    {
                        spotAnims = ArrayUtils.add(spotAnims, actorSpotAnim.getId());
                    }
                    final int[] fSpotAnims = Arrays.copyOf(spotAnims, spotAnims.length);

                    int animId = player.getAnimation();
                    if (animId == -1)
                    {
                        animId = player.getPoseAnimation();
                    }
                    final int anim = animId;

                    String name = player.getName();
                    if (player == client.getLocalPlayer())
                        name = "Local Player";
                    String finalName = name;

                    //For "Anvil" option on players
                    if (menuOption == ModelMenuOption.ANVIL)
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items, anim, fSpotAnims);

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
                        ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items, anim, fSpotAnims);
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
                                        anim,
                                        60,
                                        creatorsPanel.createEmptyProgram(anim, player.getWalkAnimation()),
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

    public void addPlayerExporter(int index, String target, Player player, boolean exportAnimation)
    {
        String option = "Export Model";
        if (exportAnimation)
        {
            option = "Export Animation";
        }

        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag(option, Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    if (exportAnimation && continueAnimExport)
                    {
                        plugin.sendChatMessage("Please wait for the current animation export to finish before trying again.");
                        return;
                    }

                    int animId = player.getAnimation();
                    int poseAnimId = player.getPoseAnimation();
                    if (animId == -1)
                    {
                        animId = poseAnimId;
                    }
                    int finalAnimId = animId;

                    if (exportAnimation && animId == -1)
                    {
                        plugin.sendChatMessage("There is no animation currently playing to export.");
                        return;
                    }

                    PlayerComposition comp = player.getPlayerComposition();
                    int[] items = comp.getEquipmentIds();

                    if (config.exportTPose() && !exportAnimation)
                    {
                        player.setAnimation(-1);
                        player.setPoseAnimation(-1);
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

                    byte[] transparencies;
                    if (model.getFaceTransparencies() == null)
                    {
                        transparencies = new byte[fCount];
                        Arrays.fill(transparencies, (byte) 0);
                    }
                    else
                    {
                        transparencies = model.getFaceTransparencies();
                    }

                    IterableHashTable<ActorSpotAnim> actorSpotAnims = player.getSpotAnims();
                    int[] spotAnims = new int[0];
                    for (ActorSpotAnim actorSpotAnim : actorSpotAnims)
                    {
                        spotAnims = ArrayUtils.add(spotAnims, actorSpotAnim.getId());
                    }
                    final int[] fSpotAnims = Arrays.copyOf(spotAnims, spotAnims.length);

                    String name = player.getName();
                    if (player == client.getLocalPlayer())
                        name = "Local Player";
                    String finalName = name;

                    if (config.vertexColours())
                    {
                        BlenderModel bm = modelExporter.bmVertexColours(model);
                        if (exportAnimation)
                        {
                            Thread thread = new Thread(() ->
                            {
                                ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items, finalAnimId, fSpotAnims);
                                clientThread.invokeLater(() ->
                                {
                                    initiateAnimationExport(finalAnimId, finalName, bm, modelStats, comp.getColors(), true, true);
                                });
                            });
                            thread.start();
                        }
                        else
                        {
                            modelExporter.saveToFile(finalName, bm);
                        }
                    }
                    else
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForPlayer(false, comp.getGender() == 0, items, finalAnimId, fSpotAnims);

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel bm = modelExporter.bmFaceColours(
                                        modelStats,
                                        false,
                                        comp.getColors(),
                                        true,
                                        vX,
                                        vY,
                                        vZ,
                                        f1,
                                        f2,
                                        f3,
                                        transparencies,
                                        renderPriorities);

                                if (exportAnimation)
                                {
                                    initiateAnimationExport(finalAnimId, finalName, bm, modelStats, comp.getColors(), true, true);
                                }
                                else
                                {
                                    modelExporter.saveToFile(finalName, bm);
                                }
                            });
                        });
                        thread.start();
                    }
                });
    }

    public void addGameObjectGetter(int index, String option, String name, Model model, int objectId, int modelType, CustomModelType type, int animationId, int orientation, ModelMenuOption menuOption)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);
        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForObject(objectId, modelType, LightingStyle.DEFAULT, false);
                        if (modelStats == null || modelStats.length == 0)
                        {
                            plugin.sendChatMessage("Could not find this Object in the cache.");
                            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                            return;
                        }

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

    public void addDynamicObjectGetter(int index, String option, String name, int objectId, int modelType, CustomModelType type, int animationId, int orientation, ModelMenuOption menuOption)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);
        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForObject(objectId, modelType, LightingStyle.DYNAMIC, false);
                        if (modelStats == null || modelStats.length == 0)
                        {
                            plugin.sendChatMessage("Could not find this Object in the cache.");
                            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                            return;
                        }

                        LightingStyle ls = LightingStyle.DYNAMIC;
                        CustomLighting lighting = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());
                        CustomModelComp comp = new CustomModelComp(0, type, objectId, modelStats, null, null, null, ls, lighting, false, name);
                        clientThread.invokeLater(() ->
                        {
                            Model model = plugin.constructModelFromCache(modelStats, new int[0], false, false);
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
                    });
                    thread.start();
                });
    }

    public void addObjectGetterToAnvil(int index, String name, int objectId, int modelType, LightingStyle lightingStyle)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);
        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag("Anvil", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForObject(objectId, modelType, lightingStyle, false);
                        if (modelStats == null || modelStats.length == 0)
                        {
                            plugin.sendChatMessage("Could not find this Object in the cache.");
                            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                            return;
                        }

                        clientThread.invokeLater(() ->
                        {
                            plugin.cacheToAnvil(modelStats, new int[0], false);
                            plugin.sendChatMessage("Model sent to Anvil: " + name);
                        });
                    });
                    thread.start();
                });
    }

    public void addObjectExporter(int index, String name, int objectId, int modelType, Model model)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);
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
                    }
                    else
                    {
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

                        byte[] transparencies;
                        if (model.getFaceTransparencies() == null)
                        {
                            transparencies = new byte[fCount];
                            Arrays.fill(transparencies, (byte) 0);
                        }
                        else
                        {
                            transparencies = model.getFaceTransparencies();
                        }

                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForObject(objectId, modelType, LightingStyle.DEFAULT, false);
                            if (modelStats == null || modelStats.length == 0)
                            {
                                plugin.sendChatMessage("Could not find this Object in the cache.");
                                plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                return;
                            }

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel blenderModel = modelExporter.bmFaceColours(
                                        modelStats,
                                        true,
                                        new int[0],
                                        false,
                                        vX,
                                        vY,
                                        vZ,
                                        f1,
                                        f2,
                                        f3,
                                        transparencies,
                                        renderPriorities);
                                modelExporter.saveToFile(name, blenderModel);
                            });
                        });
                        thread.start();

                    }
                });
    }

    public void addDynamicObjectExporter(int index, String name, int objectId, int modelType, int animId, Model model, boolean exportAnimation)
    {
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

        byte[] transparencies;
        if (model.getFaceTransparencies() == null)
        {
            transparencies = new byte[fCount];
            Arrays.fill(transparencies, (byte) 0);
        }
        else
        {
            transparencies = model.getFaceTransparencies();
        }

        String option = "Export Model";
        if (exportAnimation)
        {
            option = "Export Animation";
        }

        String target = ColorUtil.prependColorTag(name, Color.CYAN);
        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag(option, Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    if (exportAnimation && continueAnimExport)
                    {
                        plugin.sendChatMessage("Please wait for the current animation export to finish before trying again.");
                        return;
                    }

                    if (exportAnimation && animId == -1)
                    {
                        plugin.sendChatMessage("There is no animation currently playing to export.");
                        return;
                    }

                    if (config.vertexColours())
                    {
                        BlenderModel bm = modelExporter.bmVertexColours(model);

                        if (exportAnimation)
                        {
                            Thread thread = new Thread(() ->
                            {
                                ModelStats[] modelStats = modelFinder.findModelsForObject(objectId, modelType, LightingStyle.DYNAMIC, false);
                                if (modelStats == null || modelStats.length == 0)
                                {
                                    plugin.sendChatMessage("Could not find this Object in the cache.");
                                    plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                    return;
                                }

                                clientThread.invokeLater(() ->
                                {
                                    initiateAnimationExport(animId, name, bm, modelStats, new int[0], false, false);
                                });
                            });
                            thread.start();
                        }
                        else
                        {
                            modelExporter.saveToFile(name, bm);
                        }
                    }
                    else
                    {
                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForObject(objectId, modelType, LightingStyle.DYNAMIC, false);
                            if (modelStats == null || modelStats.length == 0)
                            {
                                plugin.sendChatMessage("Could not find this Object in the cache.");
                                plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                return;
                            }

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel bm = modelExporter.bmFaceColours(
                                        modelStats,
                                        true,
                                        new int[0],
                                        false,
                                        vX,
                                        vY,
                                        vZ,
                                        f1,
                                        f2,
                                        f3,
                                        transparencies,
                                        renderPriorities);

                                if (exportAnimation)
                                {
                                    initiateAnimationExport(animId, name, bm, modelStats, new int[0], false, false);
                                }
                                else
                                {
                                    modelExporter.saveToFile(name, bm);
                                }
                            });
                        });
                        thread.start();

                    }
                });
    }

    public void addRLObjectExporter(int index, Character character, boolean exportAnimation)
    {
        String name = character.getName();
        String target = ColorUtil.prependColorTag(name, Color.GREEN);
        String option = "Export Model";
        if (exportAnimation)
        {
            option = "Export Animation";
        }

        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag(option, Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    if (exportAnimation && continueAnimExport)
                    {
                        plugin.sendChatMessage("Please wait for the current animation export to finish before trying again.");
                        return;
                    }

                    int animId = (int) character.getAnimationSpinner().getValue();

                    if (exportAnimation && animId == -1)
                    {
                        plugin.sendChatMessage("There is no animation currently playing to export.");
                        return;
                    }

                    final Model model = character.getRuneLiteObject().getModel();
                    if (config.vertexColours())
                    {
                        BlenderModel bm = modelExporter.bmVertexColours(model);

                        if (exportAnimation)
                        {
                            if (character.isCustomMode())
                            {
                                clientThread.invokeLater(() ->
                                {
                                    CustomModelComp comp = character.getStoredModel().getComp();
                                    switch (comp.getType())
                                    {
                                        case FORGED:
                                            ModelData modelData = plugin.createComplexModelData(comp.getDetailedModels());
                                            initiateAnimationExport(animId, name, modelData.light(), bm);
                                            break;
                                        default:
                                        case CACHE_NPC:
                                        case CACHE_OBJECT:
                                        case CACHE_GROUND_ITEM:
                                        case CACHE_MAN_WEAR:
                                        case CACHE_WOMAN_WEAR:
                                            initiateAnimationExport(animId, name, bm, comp.getModelStats(), new int[0], false, false);
                                            break;
                                        case CACHE_PLAYER:
                                            initiateAnimationExport(animId, name, bm, comp.getModelStats(), comp.getKitRecolours(), true, true);
                                            break;
                                        case BLENDER:
                                            plugin.sendChatMessage("This model already came from Blender.");
                                            break;
                                    }
                                });
                            }
                            else
                            {
                                int modelId = (int) character.getModelSpinner().getValue();
                                ModelStats[] modelStats = new ModelStats[]{new ModelStats(
                                        modelId,
                                        BodyPart.NA,
                                        new short[0],
                                        new short[0],
                                        new short[0],
                                        new short[0],
                                        128,
                                        128,
                                        128,
                                        0,
                                        new CustomLighting(64, 768, -50, -50, 10))};

                                clientThread.invokeLater(() ->
                                {
                                    initiateAnimationExport(animId, name, bm, modelStats, new int[0], false, false);
                                });
                            }
                        }
                        else
                        {
                            modelExporter.saveToFile(name, bm);
                        }
                    }
                    else
                    {
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

                        byte[] transparencies;
                        if (model.getFaceTransparencies() == null)
                        {
                            transparencies = new byte[fCount];
                            Arrays.fill(transparencies, (byte) 0);
                        }
                        else
                        {
                            transparencies = model.getFaceTransparencies();
                        }

                        if (character.isCustomMode())
                        {
                            clientThread.invokeLater(() ->
                            {
                                CustomModelComp comp = character.getStoredModel().getComp();
                                BlenderModel bm;
                                ModelData modelData = null;
                                switch (comp.getType())
                                {
                                    case FORGED:
                                        modelData = plugin.createComplexModelData(comp.getDetailedModels());
                                        bm = modelExporter.bmFaceColoursForForgedModel(
                                                modelData,
                                                vX,
                                                vY,
                                                vZ,
                                                f1,
                                                f2,
                                                f3,
                                                transparencies,
                                                renderPriorities);
                                        break;
                                    default:
                                    case CACHE_NPC:
                                    case CACHE_OBJECT:
                                    case CACHE_GROUND_ITEM:
                                    case CACHE_MAN_WEAR:
                                    case CACHE_WOMAN_WEAR:
                                        bm = modelExporter.bmFaceColours(
                                                comp.getModelStats(),
                                                comp.getType() == CustomModelType.CACHE_OBJECT,
                                                new int[0],
                                                false,
                                                vX,
                                                vY,
                                                vZ,
                                                f1,
                                                f2,
                                                f3,
                                                transparencies,
                                                renderPriorities);
                                        break;
                                    case CACHE_PLAYER:
                                        bm = modelExporter.bmFaceColours(
                                                comp.getModelStats(),
                                                false,
                                                comp.getKitRecolours(),
                                                true,
                                                vX,
                                                vY,
                                                vZ,
                                                f1,
                                                f2,
                                                f3,
                                                transparencies,
                                                renderPriorities);
                                        break;
                                    case BLENDER:
                                        bm = comp.getBlenderModel();
                                }

                                if (exportAnimation)
                                {
                                    switch (comp.getType())
                                    {
                                        case FORGED:
                                            if (modelData != null)
                                            {
                                                initiateAnimationExport(animId, name, modelData.light(), bm);
                                            }
                                            break;
                                        default:
                                        case CACHE_NPC:
                                        case CACHE_OBJECT:
                                        case CACHE_GROUND_ITEM:
                                        case CACHE_MAN_WEAR:
                                        case CACHE_WOMAN_WEAR:
                                            initiateAnimationExport(animId, name, bm, comp.getModelStats(), new int[0], false, false);
                                            break;
                                        case CACHE_PLAYER:
                                            initiateAnimationExport(animId, name, bm, comp.getModelStats(), comp.getKitRecolours(), true, true);
                                            break;
                                        case BLENDER:
                                            plugin.sendChatMessage("This model already came from Blender.");
                                            break;
                                    }
                                }
                                else
                                {
                                    modelExporter.saveToFile(name, bm);
                                }

                            });

                        }
                        else
                        {
                            int modelId = (int) character.getModelSpinner().getValue();
                            ModelStats[] modelStats = new ModelStats[]{new ModelStats(
                                    modelId,
                                    BodyPart.NA,
                                    new short[0],
                                    new short[0],
                                    new short[0],
                                    new short[0],
                                    128,
                                    128,
                                    128,
                                    0,
                                    new CustomLighting(64, 768, -50, -50, 10))};

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel bm = modelExporter.bmFaceColours(
                                        modelStats,
                                        false,
                                        new int[0],
                                        false,
                                        vX,
                                        vY,
                                        vZ,
                                        f1,
                                        f2,
                                        f3,
                                        transparencies,
                                        renderPriorities);

                                if (exportAnimation)
                                {
                                    initiateAnimationExport(animId, name, bm, modelStats, new int[0], false, false);
                                }
                                else
                                {
                                    modelExporter.saveToFile(name, bm);
                                }
                            });
                        }
                    }
                });
    }

    public void addGroundItemGetter(int index, String option, String name, Model model, int itemId, int orientation, ModelMenuOption menuOption)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);
        client.createMenuEntry(index)
                .setOption(option)
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForGroundItem(itemId, CustomModelType.CACHE_GROUND_ITEM);
                        if (modelStats == null || modelStats.length == 0)
                        {
                            plugin.sendChatMessage("Could not find this Item in the cache.");
                            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                            return;
                        }

                        CustomLighting lighting = new CustomLighting(64, 768, -50, -50, 10);
                        CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_GROUND_ITEM, itemId, modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, name);
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
                                    -1,
                                    60,
                                    creatorsPanel.createEmptyProgram(-1, -1),
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

    public void addGroundItemGetterToAnvil(int index, String name, int itemId)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);
        client.createMenuEntry(index)
                .setOption(ColorUtil.prependColorTag("Anvil", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    Thread thread = new Thread(() ->
                    {
                        ModelStats[] modelStats = modelFinder.findModelsForGroundItem(itemId, CustomModelType.CACHE_GROUND_ITEM);
                        if (modelStats == null || modelStats.length == 0)
                        {
                            plugin.sendChatMessage("Could not find this Item in the cache.");
                            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                            return;
                        }

                        clientThread.invokeLater(() ->
                        {
                            plugin.cacheToAnvil(modelStats, new int[0], false);
                            plugin.sendChatMessage("Model sent to Anvil: " + name);
                        });
                    });
                    thread.start();
                });
    }

    public void addGroundItemExporter(int index, String name, int itemId, Model model)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);
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
                    }
                    else
                    {
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

                        byte[] transparencies;
                        if (model.getFaceTransparencies() == null)
                        {
                            transparencies = new byte[fCount];
                            Arrays.fill(transparencies, (byte) 0);
                        }
                        else
                        {
                            transparencies = model.getFaceTransparencies();
                        }

                        Thread thread = new Thread(() ->
                        {
                            ModelStats[] modelStats = modelFinder.findModelsForGroundItem(itemId, CustomModelType.CACHE_GROUND_ITEM);
                            if (modelStats == null || modelStats.length == 0)
                            {
                                plugin.sendChatMessage("Could not find this Item in the cache.");
                                plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                return;
                            }

                            clientThread.invokeLater(() ->
                            {
                                BlenderModel blenderModel = modelExporter.bmFaceColours(
                                        modelStats,
                                        false,
                                        new int[0],
                                        false,
                                        vX,
                                        vY,
                                        vZ,
                                        f1,
                                        f2,
                                        f3,
                                        transparencies,
                                        renderPriorities);
                                modelExporter.saveToFile(name, blenderModel);
                            });
                        });
                        thread.start();

                    }
                });
    }

    private void initiateAnimationExport(int animId, String name, BlenderModel bm, ModelStats[] modelStats, int[] kitRecolours, boolean player, boolean actorLighting)
    {
        Model model = plugin.constructModelFromCache(modelStats, kitRecolours, player, actorLighting);
        initiateAnimationExport(animId, name, model, bm);
    }

    private void initiateAnimationExport(int animId, String name, Model model, BlenderModel bm)
    {
        plugin.sendChatMessage("Exporting " + name + ", please stand by as the animation plays...");
        blenderModelExport = bm;
        exportName = name;

        exportObject = client.createRuneLiteObject();
        exportObject.setAnimation(client.loadAnimation(animId));
        exportObject.setModel(model);
        exportObject.setLocation(client.getLocalPlayer().getLocalLocation(), client.getPlane());
        exportObject.setActive(true);
        continueAnimExport = true;
        initiateExport = true;
    }

    public void handleAnimationExport(int clientTick)
    {
        if (!continueAnimExport)
        {
            initiateExport = false;
            return;
        }

        if (blenderModelExport == null || exportObject == null)
        {
            initiateExport = false;
            continueAnimExport = false;
            return;
        }

        if (!exportObject.isActive())
        {
            int[] ticks = new int[clientTicks.length];
            int baseLine = clientTicks[0];
            for (int i = 0; i < clientTicks.length; i++)
            {
                ticks[i] = clientTicks[i] - baseLine;
            }

            blenderModelExport.setClientTicks(ticks);
            blenderModelExport.setAnimVertices(animVerts);
            continueAnimExport = false;
            modelExporter.saveToFile(exportName, blenderModelExport);
            return;
        }

        if (exportObject.getAnimation() == null)
        {
            initiateExport = false;
            continueAnimExport = false;
            return;
        }

        Animation animation = exportObject.getAnimation();
        if (animation.getId() == -1)
        {
            initiateExport = false;
            continueAnimExport = false;
            return;
        }

        Model model = exportObject.getModel();
        int frame = exportObject.getAnimationFrame();

        if (initiateExport)
        {
            initiateExport = false;
            animFrames = new int[]{0};
            clientTicks = new int[]{clientTick};

            animVerts = new int[1][model.getVerticesCount()][3];
            int[][] verts = animVerts[0];
            int[] vX = model.getVerticesX();
            int[] vY = model.getVerticesY();
            int[] vZ = model.getVerticesZ();

            for (int i = 0; i < verts.length; i++)
            {
                int[] v = verts[i];
                v[0] = vX[i];
                v[1] = vY[i];
                v[2] = vZ[i];
            }

            return;
        }

        if (frame == animFrames[animFrames.length - 1])
        {
            return;
        }

        int[][] verts = new int[model.getVerticesCount()][3];
        int[] vX = model.getVerticesX();
        int[] vY = model.getVerticesY();
        int[] vZ = model.getVerticesZ();

        for (int i = 0; i < verts.length; i++)
        {
            int[] v = verts[i];
            v[0] = vX[i];
            v[1] = vY[i];
            v[2] = vZ[i];
        }

        animFrames = ArrayUtils.add(animFrames, frame);
        clientTicks = ArrayUtils.add(clientTicks, clientTick);
        animVerts = ArrayUtils.add(animVerts, verts);
    }
}
