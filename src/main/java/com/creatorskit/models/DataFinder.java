package com.creatorskit.models;

import com.creatorskit.models.dataloaders.*;
import com.creatorskit.models.datatypes.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Slf4j
@Getter
public class DataFinder
{
    public enum DataType
    {
        NPC,
        OBJECT,
        SPOTANIM,
        ITEM,
        KIT,
        SEQ,
        ANIM,
        WEAPON_ANIM,
        SOUND
    }

    @Data
    private static class LoadCallback
    {
        private final Runnable callback;
        private boolean done = false;
        public void run() { if (!done) { done = true; callback.run(); } }
    }

    private final ConcurrentHashMap<DataType, List<LoadCallback>> loadCallbacks = new ConcurrentHashMap<>(){{
        Arrays.stream(DataType.values()).forEach(d -> this.put(d, new ArrayList<>()));
    }};
    private final ConcurrentHashMap<DataType, Boolean> loadState = new ConcurrentHashMap<>(){{
        Arrays.stream(DataType.values()).forEach(d -> this.put(d, false));
    }};

    private final Client client;
    private Gson gson;
    OkHttpClient httpClient;
    private final NpcLoader npcLoader;
    private final ObjectLoader objectLoader;
    private final ItemLoader itemLoader;
    private final KitLoader kitLoader;
    private final SpotAnimLoader spotAnimLoader;

    private int lastAnim;
    private static final String DEFAULT_NAME = "Name";

    private final List<WeaponAnimData> weaponAnimData = new ArrayList<>();

    private static final BodyPart[] bodyParts = new BodyPart[]{
            BodyPart.HEAD,
            BodyPart.CAPE,
            BodyPart.AMULET,
            BodyPart.WEAPON,
            BodyPart.TORSO,
            BodyPart.SHIELD,
            BodyPart.ARMS,
            BodyPart.LEGS,
            BodyPart.HAIR,
            BodyPart.HANDS,
            BodyPart.FEET,
            BodyPart.JAW,
            BodyPart.SPOTANIM};
    private static final int WEAPON_IDX = 3;
    private static final int SHIELD_IDX = 5;

    @Inject
    public DataFinder(Client client, Gson gson, OkHttpClient httpClient, NpcLoader npcLoader, ObjectLoader objectLoader, ItemLoader itemLoader, KitLoader kitLoader, SpotAnimLoader spotAnimLoader)
    {
        this.client = client;
        this.gson = gson;
        this.httpClient = httpClient;
        this.npcLoader = npcLoader;
        this.objectLoader = objectLoader;
        this.itemLoader = itemLoader;
        this.kitLoader = kitLoader;
        this.spotAnimLoader = spotAnimLoader;
    }

    public void loadDataBase()
    {
        if (client == null)
        {
            return;
        }

        lookupWeaponAnimationData();
    }

    public void clearDataBase()
    {
        Arrays.stream(DataType.values()).forEach(d -> loadState.put(d, false));
        Arrays.stream(DataType.values()).forEach(d -> loadCallbacks.put(d, new ArrayList<>()));
        weaponAnimData.clear();
    }

    private void executeCallbacks(DataType dataType)
    {
        List<LoadCallback> callbacksToExecute;
        synchronized (dataType)
        {
            loadState.put(dataType, true);
            callbacksToExecute = new ArrayList<>(loadCallbacks.get(dataType));
            loadCallbacks.get(dataType).clear();
        }
        callbacksToExecute.forEach(LoadCallback::run);
    }

    public KitDefinition[] findKitData(int[] ids)
    {
        KitDefinition[] items = new KitDefinition[ids.length];

        if (client == null || client.getIndexConfig() == null)
        {
            return items;
        }

        final int KIT_CONFIG = 3;

        for (int i = 0; i < ids.length; i++)
        {
            int id = ids[i];
            byte[] data = client.getIndex(2).loadData(KIT_CONFIG, id);
            if (data == null)
            {
                items[i] = null;
                continue;
            }

            items[i] = kitLoader.load(id, data);
        }

        return items;
    }

    public CompletableFuture<List<AnimData>> filterAnimData(String entry)
    {
        CompletableFuture<List<AnimData>> future = new CompletableFuture<>();

        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/anims.json")
                .build();

        Call call = httpClient.newCall(request);

        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/anims.json");
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (ResponseBody body = response.body())
                {
                    if (!response.isSuccessful() || body == null)
                    {
                        future.completeExceptionally(new IOException("HTTP error: " + response.code()));
                        return;
                    }

                    InputStreamReader reader = new InputStreamReader(body.byteStream());

                    Type listType = new TypeToken<List<AnimData>>() {}.getType();
                    List<AnimData> list = gson.fromJson(reader, listType);

                    List<AnimData> filtered = new ArrayList<>();

                    for (AnimData animData : list)
                    {
                        if (animData.toString().contains(entry))
                        {
                            filtered.add(animData);
                        }
                    }

                    future.complete(filtered);
                }
                catch (Exception e)
                {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    public ModelStats[] findModelsForPlayer(boolean groundItem, boolean maleItem, int[] items, int animId, int leftHandItem, int rightHandItem, int[] spotAnimIds)
    {
        //Convert equipmentId to itemId or kitId as appropriate
        int[] ids = new int[items.length];

        int[] itemShortList = new int[items.length];
        int[] kitShortList = new int[items.length];

        for (int i = 0; i < ids.length; i++)
        {
            int item = items[i];

            if (item >= PlayerComposition.KIT_OFFSET && item <= PlayerComposition.ITEM_OFFSET)
            {
                kitShortList[i] = item - PlayerComposition.KIT_OFFSET;
            }
            else
            {
                kitShortList[i] = -1;
            }

            if (item > PlayerComposition.ITEM_OFFSET)
            {
                itemShortList[i] = item - PlayerComposition.ITEM_OFFSET;
            }
            else
            {
                itemShortList[i] = -1;
            }
        }

        AnimSequence animSequence = new AnimSequence(
                AnimSequenceData.UNALTERED,
                AnimSequenceData.UNALTERED,
                -1,
                -1);

        if (animId != -1)
        {
            removePlayerItems(animSequence, leftHandItem, rightHandItem);
        }

        //for ItemIds
        ArrayList<ModelStats> itemArray = new ArrayList<>();
        getPlayerItems(itemArray, groundItem, maleItem, itemShortList, animSequence);

        //for KitIds
        ArrayList<ModelStats> kitArray = new ArrayList<>();
        getPlayerKit(kitArray, kitShortList);

        ModelStats[] spotAnim = findModelsForSpotAnims(spotAnimIds);

        itemArray.addAll(kitArray);
        itemArray.addAll(List.of(spotAnim));
        ArrayList<ModelStats> orderedItems = new ArrayList<>();
        for (int e = 0; e < bodyParts.length; e++)
        {
            for (int i = 0; i < itemArray.size(); i++)
            {
                ModelStats modelStats = itemArray.get(i);
                if (modelStats.getBodyPart() == bodyParts[e])
                {
                    if (!orderedItems.contains(modelStats))
                    {
                        orderedItems.add(modelStats);
                    }
                }
            }
        }

        return orderedItems.toArray(new ModelStats[0]);
    }

    public void removePlayerItems(AnimSequence animSequence, int leftHandItem, int rightHandItem)
    {
        switch (leftHandItem)
        {
            case -1:
                break;
            case 0:
                animSequence.setOffHandData(AnimSequenceData.HIDE);
                break;
            default:
                animSequence.setOffHandItemId(leftHandItem - 512);
                animSequence.setOffHandData(AnimSequenceData.SWAP);
        }

        switch (rightHandItem)
        {
            case -1:
                break;
            case 0:
                animSequence.setMainHandData(AnimSequenceData.HIDE);
                break;
            default:
                animSequence.setMainHandItemId(rightHandItem - 512);
                animSequence.setMainHandData(AnimSequenceData.SWAP);
        }
    }

    public void getPlayerItems(ArrayList<ModelStats> modelStats, boolean groundItem, boolean maleItem, int[] itemId, AnimSequence animSequence)
    {
        AnimSequenceData mainHand = animSequence.getMainHandData();
        AnimSequenceData offHand = animSequence.getOffHandData();

        int[] updatedItemIds = Arrays.copyOf(itemId, itemId.length);

        switch (mainHand)
        {
            case UNALTERED:
                switch (offHand)
                {
                    case UNALTERED:
                        break;
                    case HIDE:
                        updatedItemIds[SHIELD_IDX] = -1;
                        break;
                    case SWAP:
                        updatedItemIds[SHIELD_IDX] = animSequence.getOffHandItemId();
                }
                break;
            case SWAP:
                switch (offHand)
                {
                    case UNALTERED:
                        updatedItemIds[WEAPON_IDX] = animSequence.getMainHandItemId();
                        break;
                    case HIDE:
                        updatedItemIds[WEAPON_IDX] = -1;
                        updatedItemIds[SHIELD_IDX] = animSequence.getMainHandItemId();
                        break;
                    case SWAP:
                        updatedItemIds[SHIELD_IDX] = animSequence.getMainHandItemId();
                        updatedItemIds[WEAPON_IDX] = animSequence.getOffHandItemId();
                }
                break;
            case HIDE:
                switch (offHand)
                {
                    case UNALTERED:
                        updatedItemIds[WEAPON_IDX] = -1;
                        break;
                    case HIDE:
                        updatedItemIds[WEAPON_IDX] = -1;
                        updatedItemIds[SHIELD_IDX] = -1;
                        break;
                    case SWAP:
                        updatedItemIds[WEAPON_IDX] = animSequence.getOffHandItemId();
                        updatedItemIds[SHIELD_IDX] = -1;
                }
                break;
        }

        ItemDefinition[] items = findItemData(updatedItemIds);
        for (int i = 0; i < items.length; i++)
        {
            ItemDefinition itemDatum = items[i];
            if (itemDatum == null)
            {
                continue;
            }

            int[] modelIds = new int[0];
            int offset = 0;

            if (groundItem)
            {
                modelIds = ArrayUtils.add(modelIds, itemDatum.getInventoryModel());
            }
            else if (maleItem)
            {
                modelIds = ArrayUtils.addAll(modelIds, itemDatum.getMaleModel0(), itemDatum.getMaleModel1(), itemDatum.getMaleModel2());
                offset = itemDatum.getMaleOffset();
            }
            else
            {
                modelIds = ArrayUtils.addAll(modelIds, itemDatum.getFemaleModel0(), itemDatum.getFemaleModel1(), itemDatum.getFemaleModel2());
                offset = itemDatum.getFemaleOffset();
            }

            if (modelIds == null || modelIds.length == 0)
            {
                continue;
            }

            short[] rf = itemDatum.getColorFind();
            short[] rt = itemDatum.getColorReplace();
            short[] rtFrom = itemDatum.getTextureFind();
            short[] rtTo = itemDatum.getTextureReplace();

            LightingStyle ls = LightingStyle.ACTOR;
            CustomLighting customLighting = new CustomLighting(
                    ls.getAmbient(),
                    ls.getContrast(),
                    ls.getX(),
                    ls.getY(),
                    ls.getZ());

            String name = itemDatum.getName();
            if (name.equals("null") || name.isEmpty())
            {
                name = DEFAULT_NAME;
            }

            for (int id : modelIds)
            {
                if (id != -1)
                {
                    modelStats.add(new ModelStats(
                            id,
                            name,
                            bodyParts[i],
                            rf,
                            rt,
                            rtFrom,
                            rtTo,
                            itemDatum.getResizeX(),
                            itemDatum.getResizeZ(),
                            itemDatum.getResizeY(),
                            offset * -1,
                            customLighting
                    ));
                }
            }
        }
    }

    public void getPlayerKit(ArrayList<ModelStats> modelStats, int[] kitId)
    {
        KitDefinition[] items = findKitData(kitId);
        for (int i = 0; i < items.length; i++)
        {
            KitDefinition kitData = items[i];
            if (kitData == null)
            {
                continue;
            }

            int[] modelIds = kitData.getModels();
            if (modelIds == null || modelIds.length == 0)
            {
                continue;
            }

            short[] rf = kitData.getRecolorToFind();
            short[] rt = kitData.getRecolorToReplace();
            short[] rtf = kitData.getRetextureToFind();
            short[] rtt = kitData.getRetextureToReplace();

            LightingStyle ls = LightingStyle.ACTOR;
            CustomLighting customLighting = new CustomLighting(
                    ls.getAmbient(),
                    ls.getContrast(),
                    ls.getX(),
                    ls.getY(),
                    ls.getZ());

            for (int id : modelIds)
            {
                if (id != -1)
                {
                    modelStats.add(new ModelStats(
                            id,
                            bodyParts[i].getName(),
                            bodyParts[i],
                            rf,
                            rt,
                            rtf,
                            rtt,
                            128,
                            128,
                            128,
                            0,
                            customLighting
                    ));
                }
            }
        }
    }

    public ModelStats[] findModelsForSpotAnims(int[] ids)
    {
        ModelStats[] modelStats = new ModelStats[0];
        SpotAnimDefinition[] spotAnims = findSpotAnimData(ids);
        for (int i = 0; i < spotAnims.length; i++)
        {
            SpotAnimDefinition spotAnim = spotAnims[i];
            if (spotAnim == null)
            {
                continue;
            }

            int modelId = spotAnim.getModelId();

            short[] rf = spotAnim.getRecolorToFind();
            short[] rt = spotAnim.getRecolorToReplace();

            int ambient = spotAnim.getAmbient();
            int contrast = spotAnim.getContrast();

            LightingStyle ls = LightingStyle.SPOTANIM;
            CustomLighting customLighting = new CustomLighting(
                    ls.getAmbient() + ambient,
                    ls.getContrast() + contrast,
                    ls.getX(),
                    ls.getY(),
                    ls.getZ());

            String name = spotAnim.getName();
            if (name == null || name.equals("null") || name.isEmpty())
            {
                name = DEFAULT_NAME;
            }

            modelStats = ArrayUtils.add(modelStats, new ModelStats(
                    modelId,
                    name,
                    BodyPart.SPOTANIM,
                    rf,
                    rt,
                    new short[0],
                    new short[0],
                    spotAnim.getResizeX(),
                    spotAnim.getResizeX(),
                    spotAnim.getResizeY(),
                    0,
                    customLighting
            ));
        }

        return modelStats;
    }

    public SpotAnimDefinition findSpotAnimData(int id)
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return null;
        }

        final int SPOTANIM_CONFIG = 13;
        Set<Integer> unknownOpcodes = new HashSet<>();

        byte[] data = client.getIndex(2).loadData(SPOTANIM_CONFIG, id);
        if (data == null)
        {
            return null;
        }

        return spotAnimLoader.load(unknownOpcodes, id, data);
    }

    public SpotAnimDefinition[] findSpotAnimData(int[] ids)
    {
        SpotAnimDefinition[] spotAnims = new SpotAnimDefinition[ids.length];

        if (client == null || client.getIndexConfig() == null)
        {
            return spotAnims;
        }

        final int SPOTANIM_CONFIG = 13;
        Set<Integer> unknownOpcodes = new HashSet<>();

        for (int i = 0; i < ids.length; i++)
        {
            int id = ids[i];
            byte[] data = client.getIndex(2).loadData(SPOTANIM_CONFIG, id);
            if (data == null)
            {
                spotAnims[i] = null;
                continue;
            }

            spotAnims[i] = spotAnimLoader.load(unknownOpcodes, id, data);
        }

        return spotAnims;
    }

    public CompletableFuture<List<SpotAnimDefinition>> filterSpotAnimNames(String entry)
    {
        CompletableFuture<List<SpotAnimDefinition>> future = new CompletableFuture<>();

        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/spotanims.json")
                .build();

        Call call = httpClient.newCall(request);

        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/spotanims.json");
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (ResponseBody body = response.body())
                {
                    if (!response.isSuccessful() || body == null)
                    {
                        future.completeExceptionally(new IOException("HTTP error: " + response.code()));
                        return;
                    }

                    InputStreamReader reader = new InputStreamReader(body.byteStream());

                    Type listType = new TypeToken<List<SpotanimData>>() {}.getType();
                    List<SpotanimData> list = gson.fromJson(reader, listType);

                    List<SpotanimData> filtered = new ArrayList<>();

                    for (SpotanimData spotanimData : list)
                    {
                        if (spotanimData.toString().contains(entry))
                        {
                            filtered.add(spotanimData);
                        }
                    }

                    List<SpotAnimDefinition> definitions = new ArrayList<>();
                    for (SpotanimData spotanimData : filtered)
                    {
                        SpotAnimDefinition def = findSpotAnimData(spotanimData.getId());
                        def.setName(spotanimData.getName());
                        definitions.add(def);
                    }

                    future.complete(definitions);
                }
                catch (Exception e)
                {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    public NpcDefinition findNPCData(int id)
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return null;
        }

        final int NPC_CONFIG = 9;
        Set<Integer> unknownOpcodes = new HashSet<>();

        byte[] data = client.getIndex(2).loadData(NPC_CONFIG, id);
        if (data == null)
        {
            return null;
        }

        return npcLoader.load(unknownOpcodes, id, data);
    }

    public ModelStats[] findModelsForNPC(NPC npc)
    {
        NPCComposition composition = npc.getTransformedComposition();
        NpcOverrides overrides = npc.getModelOverrides();

        if (overrides != null && overrides.getModelIds() != null)
        {
            return findModelsForNPC(composition, overrides.getModelIds());
        }

        if (composition != null)
        {
            return findModelsForNPC(composition, composition.getModels());
        }

        composition = npc.getComposition();
        return findModelsForNPC(composition, composition.getModels());
    }

    public ModelStats[] findModelsForNPC(NPCComposition comp, int[] modelIds)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        short[] recolorToReplace = comp.getColorToReplace();
        short[] recolorToFind = comp.getColorToReplaceWith();

        if (recolorToReplace == null || recolorToFind == null)
        {
            recolorToReplace = new short[0];
            recolorToFind = new short[0];
        }

        LightingStyle ls = LightingStyle.ACTOR;
        CustomLighting customLighting = new CustomLighting(
                ls.getAmbient(),
                ls.getContrast(),
                ls.getX(),
                ls.getY(),
                ls.getZ());

        for (int i : modelIds)
        {
            modelStats.add(new ModelStats(
                    i,
                    comp.getName(),
                    BodyPart.NA,
                    recolorToReplace,
                    recolorToFind,
                    new short[0],
                    new short[0],
                    128,
                    128,
                    128,
                    0,
                    customLighting
            ));
        }

        ModelStats[] stats = new ModelStats[modelStats.size()];
        for (int i = 0; i < modelStats.size(); i++)
        {
            stats[i] = modelStats.get(i);
        }

        return stats;
    }

    public Map.Entry<int[], ModelStats[]> findModelsForNPC(int npcId)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        int widthScale = 128;
        int heightScale = 128;
        NpcDefinition npcData = findNPCData(npcId);
        if (npcData == null)
        {
            return null;
        }

        lastAnim = npcData.getStandingAnimation();
        widthScale = npcData.getWidthScale();
        heightScale = npcData.getHeightScale();

        int[] modelIds = npcData.getModels();
        if (modelIds == null || modelIds.length == 0)
        {
            return null;
        }

        short[] recolorToFind = npcData.getRecolorToFind();
        short[] recolorToReplace = npcData.getRecolorToReplace();

        LightingStyle ls = LightingStyle.ACTOR;
        CustomLighting customLighting = new CustomLighting(
                ls.getAmbient(),
                ls.getContrast(),
                ls.getX(),
                ls.getY(),
                ls.getZ());

        for (int i : modelIds)
        {
            modelStats.add(new ModelStats(
                    i,
                    npcData.getName(),
                    BodyPart.NA,
                    recolorToFind,
                    recolorToReplace,
                    new short[0],
                    new short[0],
                    128,
                    128,
                    128,
                    0,
                    customLighting
            ));
        }

        ModelStats[] stats = new ModelStats[modelStats.size()];
        for (int i = 0; i < modelStats.size(); i++)
        {
            stats[i] = modelStats.get(i);
        }

        return new AbstractMap.SimpleEntry<>(new int[]{widthScale, heightScale}, stats);
    }

    public List<NpcDefinition> filterNPCs(String entry)
    {
        ArrayList<NpcDefinition> list = new ArrayList<>();

        if (client == null || client.getIndexConfig() == null)
        {
            return list;
        }

        final int NPC_CONFIG = 9;
        int[] ids = client.getIndexConfig().getFileIds(NPC_CONFIG);
        Set<Integer> unknownOpcodes = new HashSet<>();

        for (int i : ids)
        {
            byte[] data = client.getIndex(2).loadData(NPC_CONFIG, i);
            if (data == null)
            {
                continue;
            }

            NpcDefinition def = npcLoader.load(unknownOpcodes, i, data);
            if (def != null)
            {
                if (def.toString().contains(entry))
                {
                    list.add(def);
                }
            }
        }

        return list;
    }

    public ObjectDefinition findObjectData(int id)
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return null;
        }

        final int OBJECT_CONFIG = 6;
        Set<Integer> unknownOpcodes = new HashSet<>();

        byte[] data = client.getIndex(2).loadData(OBJECT_CONFIG, id);
        if (data == null)
        {
            return null;
        }

        return objectLoader.load(unknownOpcodes, id, data);
    }

    public ModelStats[] findModelsForObject(int objectId, int modelType, LightingStyle ls, boolean firstModelType)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        ObjectDefinition objectData = findObjectData(objectId);
        if (objectData == null)
        {
            return null;
        }

        int[] modelIds = objectData.getObjectModels();
        if (modelIds == null)
        {
            return new ModelStats[0];
        }

        int[] objectTypes = objectData.getObjectTypes();
        if (objectTypes != null && objectTypes.length > 0)
        {
            if (firstModelType)
            {
                int modelId = modelIds[0];
                modelIds = new int[]{modelId};
            }
            else
            {
                for (int i = 0; i < objectTypes.length; i++)
                {
                    if (objectTypes[i] == modelType)
                    {
                        int modelId = modelIds[i];
                        modelIds = new int[]{modelId};
                        break;
                    }
                }
            }
        }

        short[] rf = objectData.getRecolorToFind();
        short[] rt = objectData.getRecolorToReplace();
        short[] rtFrom = objectData.getRetextureToFind();
        short[] rtTo = objectData.getTextureToReplace();

        int ambient = objectData.getAmbient();
        int contrast = objectData.getContrast();
        CustomLighting customLighting = new CustomLighting(
                ls.getAmbient() + ambient,
                ls.getContrast() + contrast,
                ls.getX(),
                ls.getY(),
                ls.getZ());

        String name = objectData.getName();
        if (name.equals("null") || name.isEmpty())
        {
            name = DEFAULT_NAME;
        }

        for (int i : modelIds)
        {
            modelStats.add(new ModelStats(
                    i,
                    name,
                    BodyPart.NA,
                    rf,
                    rt,
                    rtFrom,
                    rtTo,
                    objectData.getModelSizeX(),
                    objectData.getModelSizeY(),
                    objectData.getModelSizeHeight(),
                    0,
                    customLighting
            ));
        }

        ModelStats[] stats = new ModelStats[modelStats.size()];
        for (int i = 0; i < modelStats.size(); i++)
        {
            stats[i] = modelStats.get(i);
        }

        return stats;
    }

    public List<ObjectDefinition> filterObjects(String entry)
    {
        ArrayList<ObjectDefinition> list = new ArrayList<>();

        if (client == null || client.getIndexConfig() == null)
        {
            return list;
        }

        final int OBJECT_CONFIG = 6;
        int[] ids = client.getIndexConfig().getFileIds(OBJECT_CONFIG);
        Set<Integer> unknownOpcodes = new HashSet<>();

        for (int i : ids)
        {
            byte[] data = client.getIndex(2).loadData(OBJECT_CONFIG, i);
            if (data == null)
            {
                continue;
            }

            ObjectDefinition def = objectLoader.load(unknownOpcodes, i, data);
            if (def != null)
            {
                if (def.toString().contains(entry))
                {
                    list.add(def);
                }
            }
        }

        return list;
    }

    public ItemDefinition findItemData(int id)
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return null;
        }

        final int ITEM_CONFIG = 10;
        Set<Integer> unknownOpcodes = new HashSet<>();

        byte[] data = client.getIndex(2).loadData(ITEM_CONFIG, id);
        if (data == null)
        {
            return null;
        }

        return itemLoader.load(unknownOpcodes, id, data);
    }

    public ItemDefinition[] findItemData(int[] ids)
    {
        ItemDefinition[] items = new ItemDefinition[ids.length];

        if (client == null || client.getIndexConfig() == null)
        {
            return items;
        }

        final int ITEM_CONFIG = 10;
        Set<Integer> unknownOpcodes = new HashSet<>();

        for (int i = 0; i < ids.length; i++)
        {
            int id = ids[i];
            byte[] data = client.getIndex(2).loadData(ITEM_CONFIG, id);
            if (data == null)
            {
                items[i] = null;
                continue;
            }

            items[i] = itemLoader.load(unknownOpcodes, id, data);
        }

        return items;
    }

    public ModelStats[] findModelsForGroundItem(int itemId, CustomModelType modelType)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        ItemDefinition item = findItemData(itemId);
        if (item == null)
        {
            return null;
        }

        int[] modelIds = new int[0];

        switch (modelType)
        {
            default:
            case CACHE_GROUND_ITEM:
                modelIds = ArrayUtils.add(modelIds, item.getInventoryModel());
                break;
            case CACHE_MAN_WEAR:
                modelIds = ArrayUtils.addAll(modelIds, item.getMaleModel0(), item.getMaleModel1(), item.getMaleModel2());
                break;
            case CACHE_WOMAN_WEAR:
                modelIds = ArrayUtils.addAll(modelIds, item.getFemaleModel0(), item.getFemaleModel1(), item.getFemaleModel2());
        }

        short[] rf = item.getColorFind();
        short[] rt = item.getColorReplace();
        short[] rtFrom = item.getTextureFind();
        short[] rtTo = item.getTextureReplace();

        LightingStyle ls;

        switch (modelType)
        {
            default:
            case CACHE_GROUND_ITEM:
                ls = LightingStyle.DEFAULT;
                break;
            case CACHE_MAN_WEAR:
            case CACHE_WOMAN_WEAR:
                ls = LightingStyle.ACTOR;
        }

        CustomLighting customLighting = new CustomLighting(
                ls.getAmbient(),
                ls.getContrast(),
                ls.getX(),
                ls.getY(),
                ls.getZ());

        String name = item.getName();
        if (name.equals("null") || name.isEmpty())
        {
            name = DEFAULT_NAME;
        }

        for (int i = 0; i < modelIds.length; i++)
        {
            int id = modelIds[i];
            int wearPos;
            switch (i)
            {
                default:
                case 0:
                    wearPos = item.getWearPos1();
                    break;
                case 1:
                    wearPos = item.getWearPos2();
                    break;
                case 2:
                    wearPos = item.getWearPos3();
            }

            if (id != -1)
            {
                modelStats.add(new ModelStats(
                        id,
                        name,
                        BodyPart.wearPosToBodyPart(wearPos),
                        rf,
                        rt,
                        rtFrom,
                        rtTo,
                        item.getResizeX(),
                        item.getResizeZ(),
                        item.getResizeY(),
                        0,
                        customLighting
                ));
            }
        }

        if (modelStats.isEmpty())
        {
            return null;
        }

        ModelStats[] stats = new ModelStats[modelStats.size()];
        for (int i = 0; i < modelStats.size(); i++)
        {
            stats[i] = modelStats.get(i);
        }

        return stats;
    }

    public List<ItemDefinition> filterItems(String entry)
    {
        ArrayList<ItemDefinition> list = new ArrayList<>();

        if (client == null || client.getIndexConfig() == null)
        {
            return list;
        }

        final int ITEM_CONFIG = 10;
        int[] ids = client.getIndexConfig().getFileIds(ITEM_CONFIG);
        Set<Integer> unknownOpcodes = new HashSet<>();

        for (int i : ids)
        {
            byte[] data = client.getIndex(2).loadData(ITEM_CONFIG, i);
            if (data == null)
            {
                continue;
            }

            ItemDefinition def = itemLoader.load(unknownOpcodes, i, data);
            if (def != null)
            {
                if (def.toString().contains(entry))
                {
                    list.add(def);
                }
            }
        }

        return list;
    }

    private void lookupWeaponAnimationData()
    {
        Request request = new Request.Builder().url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/refs/heads/master/.venv/weapon_animations.json").build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/refs/heads/master/.venv/weapon_animations.json");
                executeCallbacks(DataType.WEAPON_ANIM);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    //create a reader to read the URL
                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());

                    Type listType = new TypeToken<List<WeaponAnimData>>() {}.getType();
                    List<WeaponAnimData> list = gson.fromJson(reader, listType);

                    weaponAnimData.addAll(list);
                    response.body().close();
                }
                executeCallbacks(DataType.WEAPON_ANIM);
            }
        });
    }

    public WeaponAnimData findWeaponAnimData(int itemId)
    {
        for (WeaponAnimData weaponAnim : weaponAnimData)
        {
            int[] ids = weaponAnim.getId();
            if (ids == null || ids.length == 0)
            {
                continue;
            }

            for (int i : ids)
            {
                if (i == itemId)
                {
                    return weaponAnim;
                }
            }
        }

        return null;
    }

    public CompletableFuture<List<SoundData>> filterSoundData(String entry)
    {
        CompletableFuture<List<SoundData>> future = new CompletableFuture<>();

        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/refs/heads/master/.venv/sounds.json")
                .build();

        Call call = httpClient.newCall(request);

        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/refs/heads/master/.venv/sounds.json");
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (ResponseBody body = response.body())
                {
                    if (!response.isSuccessful() || body == null)
                    {
                        future.completeExceptionally(new IOException("HTTP error: " + response.code()));
                        return;
                    }

                    InputStreamReader reader = new InputStreamReader(body.byteStream());

                    Type listType = new TypeToken<List<SoundData>>() {}.getType();
                    List<SoundData> list = gson.fromJson(reader, listType);

                    List<SoundData> filtered = new ArrayList<>();

                    for (SoundData soundData : list)
                    {
                        if (soundData.toString().contains(entry))
                        {
                            filtered.add(soundData);
                        }
                    }

                    future.complete(filtered);
                }
                catch (Exception e)
                {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    public CompletableFuture<String> generateNameFromModel(int id)
    {
        CompletableFuture<String> future = new CompletableFuture<>();
        String name = DEFAULT_NAME;

        if (id == -1)
        {
            future.complete(name);
            return future;
        }

        if (client == null || client.getIndexConfig() == null)
        {
            future.complete(name);
            return future;
        }

        final int KIT_CONFIG = 3;
        int[] kitIds = client.getIndexConfig().getFileIds(KIT_CONFIG);
        for (int i : kitIds)
        {
            byte[] data = client.getIndex(2).loadData(KIT_CONFIG, i);
            if (data == null)
            {
                continue;
            }

            KitDefinition def = kitLoader.load(i, data);
            if (def == null)
            {
                continue;
            }

            int[] models = def.getModels();
            if (models != null)
            {
                if (Arrays.stream(models).anyMatch(e -> e == id))
                {
                    name = BodyPart.bodyPartIdToBodyPart(def.getBodyPartId()).getName();
                    future.complete(name);
                    return future;
                }
            }

            int[] chatheadModels = def.getChatheadModels();
            if (chatheadModels != null)
            {
                if (Arrays.stream(chatheadModels).anyMatch(e -> e == id))
                {
                    name = BodyPart.bodyPartIdToBodyPart(def.getBodyPartId()).getName();
                    future.complete(name);
                    return future;
                }
            }
        }

        List<ObjectDefinition> objects = filterObjects("");
        for (ObjectDefinition data : objects)
        {
            if (data.getObjectModels() == null)
            {
                continue;
            }

            if (Arrays.stream(data.getObjectModels()).anyMatch(e -> e == id))
            {
                future.complete(data.getName());
                return future;
            }
        }

        List<ItemDefinition> items = filterItems("");
        for (ItemDefinition data : items)
        {
            int[] itemModels = new int[]{
                    data.getFemaleModel0(),
                    data.getFemaleModel1(),
                    data.getFemaleModel2(),
                    data.getFemaleHeadModel(),
                    data.getFemaleHeadModel2(),
                    data.getMaleModel0(),
                    data.getMaleModel1(),
                    data.getMaleModel2(),
                    data.getMaleHeadModel(),
                    data.getMaleHeadModel2()};

            if (Arrays.stream(itemModels).anyMatch(e -> e == id))
            {
                future.complete(data.getName());
                return future;
            }
        }

        final int SPOTANIM_CONFIG = 13;
        int[] spotAnimIds = client.getIndexConfig().getFileIds(SPOTANIM_CONFIG);
        Set<Integer> unknownOpcodes = new HashSet<>();

        for (int i : spotAnimIds)
        {
            byte[] data = client.getIndex(2).loadData(SPOTANIM_CONFIG, i);
            if (data == null)
            {
                continue;
            }

            SpotAnimDefinition def = spotAnimLoader.load(unknownOpcodes, i, data);
            if (def == null)
            {
                continue;
            }

            int model = def.getModelId();
            if (model == id)
            {
                Request request = new Request.Builder()
                        .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/spotanims.json")
                        .build();

                Call call = httpClient.newCall(request);

                call.enqueue(new Callback()
                {
                    @Override
                    public void onFailure(Call call, IOException e)
                    {
                        log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/spotanims.json");
                        future.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response)
                    {
                        try (ResponseBody body = response.body())
                        {
                            if (!response.isSuccessful() || body == null)
                            {
                                future.completeExceptionally(new IOException("HTTP error: " + response.code()));
                                return;
                            }

                            InputStreamReader reader = new InputStreamReader(body.byteStream());

                            Type listType = new TypeToken<List<SpotanimData>>() {}.getType();
                            List<SpotanimData> list = gson.fromJson(reader, listType);

                            for (SpotanimData spotanimData : list)
                            {
                                if (spotanimData.getId() == def.getId())
                                {
                                    future.complete(spotanimData.getName());
                                    return;
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            future.completeExceptionally(e);
                        }
                    }
                });
            }
        }

        return future;
    }
}
