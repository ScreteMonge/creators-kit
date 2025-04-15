package com.creatorskit.models;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.models.exporters.ModelExporter;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.swing.ParentPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import net.runelite.api.*;
import net.runelite.api.Menu;
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
    private final ClientThread clientThread;
    private final CreatorsConfig config;
    private final CreatorsPlugin plugin;
    private final DataFinder dataFinder;
    private final ModelExporter modelExporter;
    private CKObject exportObject;

    @Inject
    public ModelGetter(Client client, ClientThread clientThread, CreatorsConfig config, CreatorsPlugin plugin, DataFinder dataFinder, ModelExporter modelExporter)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.plugin = plugin;
        this.dataFinder = dataFinder;
        this.modelExporter = modelExporter;
    }

    public void addCharacterMenuEntries(Tile tile)
    {
        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            CKObject ckObject = character.getCkObject();
            if (character.isActive() && ckObject != null)
            {
                LocalPoint localPoint = ckObject.getLocation();
                if (localPoint != null && localPoint.equals(tile.getLocalLocation()))
                {
                    MenuEntry menuEntry = client.getMenu().createMenuEntry(1)
                            .setOption(ColorUtil.prependColorTag("Select", Color.ORANGE))
                            .setTarget(ColorUtil.colorTag(Color.GREEN) + character.getName())
                            .setType(MenuAction.RUNELITE)
                            .onClick(e -> plugin.getCreatorsPanel().setSelectedCharacter(character));

                    Menu menu = menuEntry.createSubMenu();

                    menu.createMenuEntry(0)
                            .setOption(ColorUtil.prependColorTag("Export 3D", Color.WHITE))
                            .setType(MenuAction.RUNELITE)
                            .onClick(e -> exportRLObject(character, false));

                    menu.createMenuEntry(0)
                            .setOption(ColorUtil.prependColorTag("Export Animation", Color.WHITE))
                            .setType(MenuAction.RUNELITE)
                            .onClick(e -> exportRLObject(character, true));
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
                addPlayerMenuEntries(target, localPlayer);
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
                addGroundItemMenuEntries(name, model, itemId);
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
            int orientation = gameObject.getOrientation();

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
                addGameObjectMenuEntries(name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, orientation, LightingStyle.DEFAULT, false);
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
                addGameObjectMenuEntries(name, dynamicObject.getModel(), objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, orientation, LightingStyle.DYNAMIC, true);
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
                addGameObjectMenuEntries(name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, LightingStyle.DEFAULT, false);
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
                addGameObjectMenuEntries(name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, LightingStyle.DEFAULT, false);
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
                addGameObjectMenuEntries(name, model, objectId, modelType, CustomModelType.CACHE_OBJECT, animationId, 0, LightingStyle.DEFAULT, false);
            }
        }
    }

    public void addNPCMenuEntries(String target, NPC npc)
    {
        MenuEntry menuEntry = client.getMenu().createMenuEntry(1)
                .setOption(ColorUtil.prependColorTag("Store-Add", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeNPC(npc, ModelMenuOption.STORE_AND_ADD));

        Menu menu = menuEntry.createSubMenu();

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Store-Only", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeNPC(npc, ModelMenuOption.STORE));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Anvil", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeNPC(npc, ModelMenuOption.ANVIL));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Transmog", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeNPC(npc, ModelMenuOption.TRANSMOG));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Export 3D", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> exportNPC(npc, false));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Export Animation", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> exportNPC(npc, true));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Store-Add Spotanims", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeSpotAnims(npc.getSpotAnims()));
    }

    public void storeNPC(NPC npc, ModelMenuOption menuOption)
    {
        NPCComposition composition = npc.getTransformedComposition();
        NpcOverrides overrides = npc.getModelOverrides();

        ModelStats[] modelStats;
        if (overrides != null)
        {
            modelStats = dataFinder.findModelsForNPC(npc.getId(), overrides);
        }
        else if (composition != null)
        {
            modelStats = dataFinder.findModelsForNPC(npc.getId(), composition);
        }
        else
        {
            modelStats = dataFinder.findModelsForNPC(npc.getId());
        }

        if (modelStats == null || modelStats.length == 0)
        {
            plugin.sendChatMessage("Could not find this NPC in the cache.");
            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
            return;
        }

        String name = npc.getName();

        if (menuOption == ModelMenuOption.ANVIL)
        {
            handleAnvilOption(modelStats, new int[0], false, name);
            return;
        }

        handleStoreOptions(modelStats, menuOption, CustomModelType.CACHE_NPC, name, new int[0], false, LightingStyle.ACTOR, npc.getOrientation(), npc.getPoseAnimation(), npc.getWalkAnimation());
    }

    public void exportNPC(NPC npc, boolean exportAnimation)
    {
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
                    NPCComposition composition = npc.getTransformedComposition();

                    ModelStats[] modelStats;
                    if (composition == null)
                    {
                        modelStats = dataFinder.findModelsForNPC(npc.getId());
                    }
                    else
                    {
                        modelStats = dataFinder.findModelsForNPC(npc.getId(), composition);
                    }

                    if (modelStats == null || modelStats.length == 0)
                    {
                        plugin.sendChatMessage("Could not find this NPC in the cache.");
                        plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                        return;
                    }

                    clientThread.invokeLater(() ->
                    {
                        initiateAnimationExport(finalAnimId, name, bm, modelStats, new int[0], false, LightingStyle.ACTOR, null);
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
            float[] fvX = Arrays.copyOf(model.getVerticesX(), vCount);
            float[] fvY = Arrays.copyOf(model.getVerticesY(), vCount);
            float[] fvZ = Arrays.copyOf(model.getVerticesZ(), vCount);

            int[] vX = new int[vCount];
            int[] vY = new int[vCount];
            int[] vZ = new int[vCount];

            for (int i = 0; i < vCount; i++)
            {
                vX[i] = (int) fvX[i];
                vY[i] = (int) fvY[i];
                vZ[i] = (int) fvZ[i];
            }

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
                ModelStats[] modelStats = dataFinder.findModelsForNPC(npcId);
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
                        initiateAnimationExport(finalAnimId, name, bm, modelStats, new int[0], false, LightingStyle.ACTOR, null);
                    }
                    else
                    {
                        modelExporter.saveToFile(name, bm);
                    }
                });
            });
            thread.start();
        }
    }

    public void storeSpotAnims(IterableHashTable<ActorSpotAnim> spotAnims)
    {
        for (ActorSpotAnim spotAnim : spotAnims)
        {
            ModelStats[] modelStats = dataFinder.findSpotAnim(spotAnim.getId());
            if (modelStats == null || modelStats.length == 0)
            {
                plugin.sendChatMessage("Could not find this Spotanim in the cache.");
                plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                continue;
            }

            String name = "SpotAnim " + spotAnim.getId();
            clientThread.invokeLater(() ->
            {
                CustomLighting lighting = modelStats[0].getLighting();
                CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_SPOTANIM, spotAnim.getId(), modelStats, null, null, null, LightingStyle.CUSTOM, lighting, false, name);

                ModelData modelData = client.loadModelData(modelStats[0].getModelId()).cloneColors().cloneVertices();
                short[] recolFrom = modelStats[0].getRecolourFrom();
                short[] recolTo = modelStats[0].getRecolourTo();
                for (int i = 0; i < recolFrom.length; i++)
                    modelData.recolor(recolFrom[i], recolTo[i]);
                modelData.scale(modelStats[0].getResizeX(), modelStats[0].getResizeZ(), modelStats[0].getResizeY());

                int anim = dataFinder.getLastAnim();
                Model model = modelData.light(lighting.getAmbient(), lighting.getContrast(), lighting.getX(), lighting.getZ() * -1, lighting.getY());
                CustomModel customModel = new CustomModel(model, comp);
                plugin.addCustomModel(customModel, false);
                plugin.sendChatMessage("Model stored: " + name + "; Anim: " + anim + "; Ambient/Contrast: " + lighting.getAmbient() + "/" + lighting.getContrast());
                CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();

                Character character = creatorsPanel.createCharacter(
                        ParentPanel.SIDE_PANEL,
                        name,
                        7699,
                        customModel,
                        true,
                        0,
                        anim,
                        -1,
                        60,
                        new KeyFrame[KeyFrameType.getTotalFrameTypes()][],
                        KeyFrameType.createDefaultSummary(),
                        creatorsPanel.createEmptyProgram(anim, anim),
                        false,
                        null,
                        null,
                        new int[0],
                        -1,
                        false,
                        false);

                SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));
            });
        }
    }

    public void addPlayerMenuEntries(String target, Player player)
    {
        MenuEntry menuEntry = client.getMenu().createMenuEntry(1)
                .setOption(ColorUtil.prependColorTag("Store-Add", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storePlayer(player, ModelMenuOption.STORE_AND_ADD));

        Menu menu = menuEntry.createSubMenu();

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Store-Only", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storePlayer(player, ModelMenuOption.STORE));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Anvil", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storePlayer(player, ModelMenuOption.ANVIL));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Transmog", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storePlayer(player, ModelMenuOption.TRANSMOG));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Export 3D", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> exportPlayer(player, false));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Export Animation", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> exportPlayer(player, true));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Store-Add Spotanims", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeSpotAnims(player.getSpotAnims()));
    }

    public void storePlayer(Player player, ModelMenuOption menuOption)
    {
        PlayerComposition comp = player.getPlayerComposition();
        final int[] items = comp.getEquipmentIds();
        final int[] colours = comp.getColors().clone();

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

        String name = player.getName();
        if (player == client.getLocalPlayer())
        {
            name = "Local Player";
        }

        ModelStats[] modelStats = dataFinder.findModelsForPlayer(false, comp.getGender() == 0, items, animId, fSpotAnims);

        if (menuOption == ModelMenuOption.ANVIL)
        {
            handleAnvilOption(modelStats, colours, true, name);
            return;
        }

        handleStoreOptions(modelStats, menuOption, CustomModelType.CACHE_PLAYER, name, colours, true, LightingStyle.ACTOR, player.getOrientation(), animId, player.getWalkAnimation());
    }

    public void exportPlayer(Player player, boolean exportAnimation)
    {
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
        float[] fvX = Arrays.copyOf(model.getVerticesX(), vCount);
        float[] fvY = Arrays.copyOf(model.getVerticesY(), vCount);
        float[] fvZ = Arrays.copyOf(model.getVerticesZ(), vCount);

        int[] vX = new int[vCount];
        int[] vY = new int[vCount];
        int[] vZ = new int[vCount];

        for (int i = 0; i < vCount; i++)
        {
            vX[i] = (int) fvX[i];
            vY[i] = (int) fvY[i];
            vZ[i] = (int) fvZ[i];
        }

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
                    ModelStats[] modelStats = dataFinder.findModelsForPlayer(false, comp.getGender() == 0, items, finalAnimId, fSpotAnims);
                    clientThread.invokeLater(() ->
                    {
                        initiateAnimationExport(finalAnimId, finalName, bm, modelStats, comp.getColors(), true, LightingStyle.ACTOR, null);
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
                ModelStats[] modelStats = dataFinder.findModelsForPlayer(false, comp.getGender() == 0, items, finalAnimId, fSpotAnims);

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
                        initiateAnimationExport(finalAnimId, finalName, bm, modelStats, comp.getColors(), true, LightingStyle.ACTOR, null);
                    }
                    else
                    {
                        modelExporter.saveToFile(finalName, bm);
                    }
                });
            });
            thread.start();
        }
    }

    public void addGameObjectMenuEntries(String name, Model model, int objectId, int modelType, CustomModelType type, int animationId, int orientation, LightingStyle ls, boolean dynamicObject)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);

        MenuEntry menuEntry = client.getMenu().createMenuEntry(1)
                .setOption(ColorUtil.prependColorTag("Store-Add", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeGameObject(name, model, objectId, modelType, type, animationId, orientation, ls, dynamicObject, ModelMenuOption.STORE_AND_ADD));

        Menu menu = menuEntry.createSubMenu();

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Store-Only", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeGameObject(name, model, objectId, modelType, type, animationId, orientation, ls, dynamicObject, ModelMenuOption.STORE));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Anvil", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeGameObject(name, model, objectId, modelType, type, animationId, orientation, ls, dynamicObject, ModelMenuOption.ANVIL));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Transmog", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeGameObject(name, model, objectId, modelType, type, animationId, orientation, ls, dynamicObject, ModelMenuOption.TRANSMOG));

        if (dynamicObject)
        {
            addDynamicObjectExporter(menu, name, objectId, modelType, animationId, model, false);
            addDynamicObjectExporter(menu, name, objectId, modelType, animationId, model, true);
        }
        else
        {
            menu.createMenuEntry(0)
                    .setOption(ColorUtil.prependColorTag("Export 3D", Color.WHITE))
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> exportObject(name, objectId, modelType, model));
        }
    }

    public void storeGameObject(String name, Model model, int objectId, int modelType, CustomModelType type, int animationId, int orientation, LightingStyle ls, boolean dynamicObject, ModelMenuOption menuOption)
    {
        ModelStats[] modelStats = dataFinder.findModelsForObject(objectId, modelType, ls, false);
        if (modelStats == null || modelStats.length == 0)
        {
            plugin.sendChatMessage("Could not find this Object in the cache.");
            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
            return;
        }

        if (menuOption == ModelMenuOption.ANVIL)
        {
            handleAnvilOption(modelStats, new int[0], false, name);
            return;
        }

        if (dynamicObject)
        {
            handleStoreOptions(modelStats, menuOption, type, name, new int[0], false, ls, orientation, animationId, -1);
            return;
        }

        handleStoreOptions(model, modelStats, menuOption, type, name, new int[0], false, ls, orientation, animationId, -1);
    }

    public void exportObject(String name, int objectId, int modelType, Model model)
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
            float[] fvX = Arrays.copyOf(model.getVerticesX(), vCount);
            float[] fvY = Arrays.copyOf(model.getVerticesY(), vCount);
            float[] fvZ = Arrays.copyOf(model.getVerticesZ(), vCount);

            int[] vX = new int[vCount];
            int[] vY = new int[vCount];
            int[] vZ = new int[vCount];

            for (int i = 0; i < vCount; i++)
            {
                vX[i] = (int) fvX[i];
                vY[i] = (int) fvY[i];
                vZ[i] = (int) fvZ[i];
            }

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
                ModelStats[] modelStats = dataFinder.findModelsForObject(objectId, modelType, LightingStyle.DEFAULT, false);
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
    }

    public void addDynamicObjectExporter(Menu menu, String name, int objectId, int modelType, int animId, Model model, boolean exportAnimation)
    {
        int vCount = model.getVerticesCount();
        int fCount = model.getFaceCount();
        float[] fvX = Arrays.copyOf(model.getVerticesX(), vCount);
        float[] fvY = Arrays.copyOf(model.getVerticesY(), vCount);
        float[] fvZ = Arrays.copyOf(model.getVerticesZ(), vCount);

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

        menu.createMenuEntry(0)
            .setOption(ColorUtil.prependColorTag(option, Color.WHITE))
            .setType(MenuAction.RUNELITE)
            .onClick(e ->
                {
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
                                ModelStats[] modelStats = dataFinder.findModelsForObject(objectId, modelType, LightingStyle.DYNAMIC, false);
                                if (modelStats == null || modelStats.length == 0)
                                {
                                    plugin.sendChatMessage("Could not find this Object in the cache.");
                                    plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                    return;
                                }

                                clientThread.invokeLater(() ->
                                {
                                    initiateAnimationExport(animId, name, bm, modelStats, new int[0], false, LightingStyle.DYNAMIC, null);
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
                            ModelStats[] modelStats = dataFinder.findModelsForObject(objectId, modelType, LightingStyle.DYNAMIC, false);
                            if (modelStats == null || modelStats.length == 0)
                            {
                                plugin.sendChatMessage("Could not find this Object in the cache.");
                                plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
                                return;
                            }

                            int[] vX = new int[vCount];
                            int[] vY = new int[vCount];
                            int[] vZ = new int[vCount];

                            for (int i = 0; i < vCount; i++)
                            {
                                vX[i] = (int) fvX[i];
                                vY[i] = (int) fvY[i];
                                vZ[i] = (int) fvZ[i];
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
                                    initiateAnimationExport(animId, name, bm, modelStats, new int[0], false, LightingStyle.DYNAMIC, null);
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

    public void exportRLObject(Character character, boolean exportAnimation)
    {
        String name = character.getName();
        int animId = (int) character.getAnimationSpinner().getValue();

        if (exportAnimation && animId == -1)
        {
            plugin.sendChatMessage("There is no animation currently playing to export.");
            return;
        }

        final Model model = character.getCkObject().getModel();
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
                            case CACHE_SPOTANIM:
                            case CACHE_GROUND_ITEM:
                            case CACHE_MAN_WEAR:
                            case CACHE_WOMAN_WEAR:
                                initiateAnimationExport(animId, name, bm, comp.getModelStats(), new int[0], false, comp.getLightingStyle(), comp.getCustomLighting());
                                break;
                            case CACHE_PLAYER:
                                initiateAnimationExport(animId, name, bm, comp.getModelStats(), comp.getKitRecolours(), true, comp.getLightingStyle(), comp.getCustomLighting());
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
                        initiateAnimationExport(animId, name, bm, modelStats, new int[0], false, LightingStyle.DEFAULT, null);
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
            float[] fvX = Arrays.copyOf(model.getVerticesX(), vCount);
            float[] fvY = Arrays.copyOf(model.getVerticesY(), vCount);
            float[] fvZ = Arrays.copyOf(model.getVerticesZ(), vCount);

            int[] vX = new int[vCount];
            int[] vY = new int[vCount];
            int[] vZ = new int[vCount];

            for (int i = 0; i < vCount; i++)
            {
                vX[i] = (int) fvX[i];
                vY[i] = (int) fvY[i];
                vZ[i] = (int) fvZ[i];
            }

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
                        case CACHE_SPOTANIM:
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
                            case CACHE_SPOTANIM:
                            case CACHE_GROUND_ITEM:
                            case CACHE_MAN_WEAR:
                            case CACHE_WOMAN_WEAR:
                                initiateAnimationExport(animId, name, bm, comp.getModelStats(), new int[0], false, comp.getLightingStyle(), comp.getCustomLighting());
                                break;
                            case CACHE_PLAYER:
                                initiateAnimationExport(animId, name, bm, comp.getModelStats(), comp.getKitRecolours(), true, comp.getLightingStyle(), comp.getCustomLighting());
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
                        initiateAnimationExport(animId, name, bm, modelStats, new int[0], false, LightingStyle.DEFAULT, null);
                    }
                    else
                    {
                        modelExporter.saveToFile(name, bm);
                    }
                });
            }
        }
    }

    public void addGroundItemMenuEntries(String name, Model model, int itemId)
    {
        String target = ColorUtil.prependColorTag(name, Color.CYAN);

        MenuEntry menuEntry = client.getMenu().createMenuEntry(1)
                .setOption(ColorUtil.prependColorTag("Store-Add", Color.ORANGE))
                .setTarget(target)
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeGroundItem(name, model, itemId, ModelMenuOption.STORE_AND_ADD));

        Menu menu = menuEntry.createSubMenu();

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Store-Only", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeGroundItem(name, model, itemId, ModelMenuOption.STORE));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Anvil", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeGroundItem(name, model, itemId, ModelMenuOption.ANVIL));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Transmog", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> storeGroundItem(name, model, itemId, ModelMenuOption.TRANSMOG));

        menu.createMenuEntry(0)
                .setOption(ColorUtil.prependColorTag("Export 3D", Color.WHITE))
                .setType(MenuAction.RUNELITE)
                .onClick(e -> exportGroundItem(name, itemId, model));
    }

    public void storeGroundItem(String name, Model model, int itemId, ModelMenuOption menuOption)
    {
        ModelStats[] modelStats = dataFinder.findModelsForGroundItem(itemId, CustomModelType.CACHE_GROUND_ITEM);
        if (modelStats == null || modelStats.length == 0)
        {
            plugin.sendChatMessage("Could not find this Item in the cache.");
            plugin.sendChatMessage("This may be because Creator's Kit's cache dumps have not yet been updated to the latest game update.");
            return;
        }

        if (menuOption == ModelMenuOption.ANVIL)
        {
            handleAnvilOption(modelStats, new int[0], false, name);
            return;
        }

        handleStoreOptions(model, modelStats, menuOption, CustomModelType.CACHE_GROUND_ITEM, name, new int[0], false, LightingStyle.DEFAULT, 0, -1, -1);
    }

    public void exportGroundItem(String name, int itemId, Model model)
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

            float[] fvX = Arrays.copyOf(model.getVerticesX(), vCount);
            float[] fvY = Arrays.copyOf(model.getVerticesY(), vCount);
            float[] fvZ = Arrays.copyOf(model.getVerticesZ(), vCount);

            int[] vX = new int[vCount];
            int[] vY = new int[vCount];
            int[] vZ = new int[vCount];

            for (int i = 0; i < vCount; i++)
            {
                vX[i] = (int) fvX[i];
                vY[i] = (int) fvY[i];
                vZ[i] = (int) fvZ[i];
            }

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
                ModelStats[] modelStats = dataFinder.findModelsForGroundItem(itemId, CustomModelType.CACHE_GROUND_ITEM);
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
    }

    private void handleAnvilOption(ModelStats[] modelStats, int[] kitRecolours, boolean player, String name)
    {
        clientThread.invokeLater(() ->
        {
            plugin.cacheToAnvil(modelStats, kitRecolours, player);
            plugin.sendChatMessage("Model sent to Anvil: " + name);
        });
    }

    private void handleStoreOptions(ModelStats[] modelStats, ModelMenuOption menuOption, CustomModelType customModelType, String name, int[] kitRecolours, boolean player, LightingStyle ls, int orientation, int poseAnimation, int walkAnimation)
    {
        clientThread.invokeLater(() ->
        {
            Model model = plugin.constructModelFromCache(modelStats, kitRecolours, player, ls, null);
            store(model, modelStats, menuOption, customModelType, name, kitRecolours, ls, orientation, poseAnimation, walkAnimation);
        });
    }

    private void handleStoreOptions(Model model, ModelStats[] modelStats, ModelMenuOption menuOption, CustomModelType customModelType, String name, int[] kitRecolours, boolean player, LightingStyle ls, int orientation, int poseAnimation, int walkAnimation)
    {
        Thread thread = new Thread(() ->
        {
            store(model, modelStats, menuOption, customModelType, name, kitRecolours, ls, orientation, poseAnimation, walkAnimation);
        });
        thread.start();
    }

    private void store(Model model, ModelStats[] modelStats, ModelMenuOption menuOption, CustomModelType customModelType, String name, int[] kitRecolours, LightingStyle ls, int orientation, int poseAnimation, int walkAnimation)
    {
        CustomLighting lighting = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());
        CustomModelComp comp = new CustomModelComp(0, customModelType, 7699, modelStats, kitRecolours, null, null, ls, lighting, false, name);
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
                    orientation,
                    poseAnimation,
                    -1,
                    60,
                    new KeyFrame[KeyFrameType.getTotalFrameTypes()][],
                    KeyFrameType.createDefaultSummary(),
                    creatorsPanel.createEmptyProgram(poseAnimation, walkAnimation),
                    false,
                    null,
                    null,
                    new int[0],
                    -1,
                    false,
                    false);

            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));
        }
    }

    private void initiateAnimationExport(int animId, String name, BlenderModel bm, ModelStats[] modelStats, int[] kitRecolours, boolean player, LightingStyle ls, CustomLighting cl)
    {
        Model model = plugin.constructModelFromCache(modelStats, kitRecolours, player, ls, cl);
        initiateAnimationExport(animId, name, model, bm);
    }

    private void initiateAnimationExport(int animId, String name, Model model, BlenderModel bm)
    {
        exportObject = new CKObject(client);
        client.registerRuneLiteObject(exportObject);

        exportObject.setAnimation(animId);
        exportObject.setModel(model);
        exportObject.setActive(true);
        exportObject.setLocation(client.getLocalPlayer().getLocalLocation(), client.getTopLevelWorldView().getPlane());

        AnimationController ac = exportObject.getAnimationController();
        if (ac == null)
        {
            return;
        }

        Animation animation = ac.getAnimation();
        if (animation == null)
        {
            return;
        }

        int vCount = model.getVerticesCount();

        int[] clientTicks;
        if (animation.isMayaAnim())
        {
            int frames = animation.getNumFrames();
            clientTicks = new int[frames];

            for (int i = 0; i < frames; i++)
            {
                clientTicks[i] = i;
            }
        }
        else
        {
            int[] frameLengths = animation.getFrameLengths();
            int frames = frameLengths.length;
            clientTicks = new int[frames];

            for (int i = 0; i < frameLengths.length; i++)
            {
                int length = 0;
                for (int e = 0; e <= i; e++)
                {
                    length += frameLengths[e];
                }

                clientTicks[i] = length;
            }
        }

        int maxAnimFrames = exportObject.getMaxAnimFrames();
        int[][][] animVerts = new int[maxAnimFrames][model.getVerticesCount()][3];

        for (int e = 0; e < maxAnimFrames; e++)
        {
            exportObject.setAnimationFrame(e, true);
            Model m = exportObject.getModel();

            int[][] verts = animVerts[e];

            float[] fvX = m.getVerticesX();
            float[] fvY = m.getVerticesY();
            float[] fvZ = m.getVerticesZ();

            int[] vX = new int[vCount];
            int[] vY = new int[vCount];
            int[] vZ = new int[vCount];

            for (int i = 0; i < vCount; i++)
            {
                vX[i] = (int) fvX[i];
                vY[i] = (int) fvY[i];
                vZ[i] = (int) fvZ[i];
            }

            for (int i = 0; i < verts.length; i++)
            {
                int[] v = verts[i];
                v[0] = vX[i];
                v[1] = vY[i];
                v[2] = vZ[i];
            }
        }

        exportObject.setActive(false);

        bm.setClientTicks(clientTicks);
        bm.setAnimVertices(animVerts);
        modelExporter.saveToFile(name, bm);
    }
}
