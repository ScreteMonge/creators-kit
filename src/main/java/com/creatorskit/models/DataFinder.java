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

    private final List<NpcDefinition> npcData = new ArrayList<>();
    private final List<ObjectDefinition> objectData = new ArrayList<>();
    private final List<SpotAnimDefinition> spotanimData = new ArrayList<>();
    private final List<ItemDefinition> itemData = new ArrayList<>();
    private final List<KitDefinition> kitData = new ArrayList<>();
    private final List<AnimData> animData = new ArrayList<>();
    private final List<WeaponAnimData> weaponAnimData = new ArrayList<>();
    private final List<SoundData> soundData = new ArrayList<>();

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

        lookupNPCData();
        lookupObjectData();
        lookupSpotAnimData();
        lookupItemData();
        lookupKitData();
        lookupAnimData();
        lookupWeaponAnimationData();
        lookupSoundData();
    }

    public void clearDataBase()
    {
        Arrays.stream(DataType.values()).forEach(d -> loadState.put(d, false));
        Arrays.stream(DataType.values()).forEach(d -> loadCallbacks.put(d, new ArrayList<>()));
        npcData.clear();
        objectData.clear();
        spotanimData.clear();
        itemData.clear();
        kitData.clear();
        animData.clear();
        weaponAnimData.clear();
        soundData.clear();
    }

    /**
     * <p>Adds a callback to be executed once the specified data type has been loaded.</p>
     * <p>The callback will run on the same thread that executes the load operation.</p>
     * <p>If thread-specific execution is needed, it should be handled within the callback.</p>
     * @param dataType The DataType for which to add the callback
     * @param callback The Runnable to execute once the data has been loaded
     */
    public void addLoadCallback(DataType dataType, Runnable callback)
    {
        LoadCallback cbe = new LoadCallback(callback);
        boolean shouldRun;
        synchronized (dataType)
        {
            shouldRun = loadState.get(dataType);
            if(!shouldRun) loadCallbacks.get(dataType).add(cbe);
        }
        if (shouldRun) cbe.run();
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

    public boolean isDataLoaded(DataType dataType) { return loadState.get(dataType); }

    private void lookupKitData()
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return;
        }

        final int KIT_CONFIG = 3;
        int[] ids = client.getIndexConfig().getFileIds(KIT_CONFIG);

        for (int i : ids)
        {
            byte[] data = client.getIndex(2).loadData(KIT_CONFIG, i);
            if (data == null)
            {
                continue;
            }

            KitDefinition def = kitLoader.load(i, data);
            kitData.add(def);
        }

        executeCallbacks(DataType.KIT);
    }

    private void lookupAnimData()
    {
        Request animRequest = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/anims.json")
                .build();
        Call call = httpClient.newCall(animRequest);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/anims.json");
                executeCallbacks(DataType.ANIM);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                    Type listType = new TypeToken<List<AnimData>>() {}.getType();
                    List<AnimData> list = gson.fromJson(reader, listType);
                    animData.addAll(list);

                    response.body().close();
                }
                executeCallbacks(DataType.ANIM);
            }
        });
    }

    public ModelStats[] findModelsForPlayer(boolean groundItem, boolean maleItem, int[] items, int animId, int leftHandItem, int rightHandItem, int[] spotAnims)
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

        ArrayList<ModelStats> spotAnimArray = new ArrayList<>();
        if (spotAnims.length > 0)
        {
            getPlayerSpotAnims(spotAnims, spotAnimArray);
        }

        itemArray.addAll(kitArray);
        itemArray.addAll(spotAnimArray);
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

        int itemsToComplete = updatedItemIds.length;
        for (int i : updatedItemIds)
        {
            if (i == -1)
            {
                itemsToComplete--;
            }
        }

        for (ItemDefinition itemDatum : itemData)
        {
            if (itemsToComplete == 0)
            {
                break;
            }

            for (int i = 0; i < updatedItemIds.length; i++)
            {
                int item = updatedItemIds[i];
                if (item == -1)
                {
                    continue;
                }

                if (itemDatum.getId() == item)
                {
                    itemsToComplete--;
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

                    break;
                }
            }
        }
    }

    public void getPlayerKit(ArrayList<ModelStats> modelStats, int[] kitId)
    {
        int itemsToComplete = kitId.length;
        for (int i : kitId)
        {
            if (i == -1)
            {
                itemsToComplete--;
            }
        }

        for (KitDefinition kitData : kitData)
        {
            if (itemsToComplete == 0)
            {
                break;
            }

            for (int i = 0; i < kitId.length; i++)
            {
                int item = kitId[i];
                if (item == -1)
                {
                    continue;
                }

                if (kitData.getId() == item)
                {
                    itemsToComplete--;
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

                    break;
                }
            }
        }
    }

    public void getPlayerSpotAnims(int[] spotAnims, ArrayList<ModelStats> modelStats)
    {
        int itemsToComplete = spotAnims.length;

        for (SpotAnimDefinition spotanimData : spotanimData)
        {
            if (itemsToComplete == 0)
            {
                break;
            }

            for (int i : spotAnims)
            {
                if (spotanimData.getId() == i)
                {
                    itemsToComplete--;
                    int modelId = spotanimData.getModelId();

                    short[] rf = spotanimData.getRecolorToFind();
                    short[] rt = spotanimData.getRecolorToReplace();

                    int ambient = spotanimData.getAmbient();
                    int contrast = spotanimData.getContrast();

                    LightingStyle ls = LightingStyle.SPOTANIM;
                    CustomLighting customLighting = new CustomLighting(
                            ls.getAmbient() + ambient,
                            ls.getContrast() + contrast,
                            ls.getX(),
                            ls.getY(),
                            ls.getZ());

                    String name = spotanimData.getName();
                    if (name.equals("null") || name.isEmpty())
                    {
                        name = DEFAULT_NAME;
                    }

                    modelStats.add(new ModelStats(
                            modelId,
                            name,
                            BodyPart.SPOTANIM,
                            rf,
                            rt,
                            new short[0],
                            new short[0],
                            spotanimData.getResizeX(),
                            spotanimData.getResizeX(),
                            spotanimData.getResizeY(),
                            0,
                            customLighting
                    ));

                    break;
                }
            }
        }
    }

    private void lookupSpotAnimData()
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return;
        }

        final int SPOTANIM_CONFIG = 13;
        int[] ids = client.getIndexConfig().getFileIds(SPOTANIM_CONFIG);
        Set<Integer> unknownOpcodes = new HashSet<>();

        for (int i : ids)
        {
            byte[] data = client.getIndex(2).loadData(SPOTANIM_CONFIG, i);
            if (data == null)
            {
                continue;
            }

            SpotAnimDefinition def = spotAnimLoader.load(unknownOpcodes, i, data);
            if (def != null)
            {
                spotanimData.add(def);
            }
        }

        Request spotanimRequest = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/spotanims.json")
                .build();
        Call call = httpClient.newCall(spotanimRequest);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/spotanims.json");
                executeCallbacks(DataType.SPOTANIM);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {

                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                    Type listType = new TypeToken<List<SpotanimData>>() {}.getType();
                    List<SpotanimData> list = gson.fromJson(reader, listType);
                    response.body().close();

                    for (SpotAnimDefinition def : spotanimData)
                    {
                        for (SpotanimData data : list)
                        {
                            if (def.getId() == data.getId())
                            {
                                def.setName(data.getName());
                                break;
                            }
                        }
                    }
                }
                executeCallbacks(DataType.SPOTANIM);
            }
        });
    }

    public ModelStats[] findSpotAnim(int spotAnimId)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        for (SpotAnimDefinition spotanimData : spotanimData)
        {
            if (spotanimData.getId() == spotAnimId)
            {
                int modelId = spotanimData.getModelId();

                lastAnim = spotanimData.getAnimationId();

                short[] rf = spotanimData.getRecolorToFind();
                short[] rt = spotanimData.getRecolorToReplace();

                int ambient = spotanimData.getAmbient();
                int contrast = spotanimData.getContrast();

                LightingStyle ls = LightingStyle.SPOTANIM;
                CustomLighting customLighting = new CustomLighting(
                        ls.getAmbient() + ambient,
                        ls.getContrast() + contrast,
                        ls.getX(),
                        ls.getY(),
                        ls.getZ());

                String name = spotanimData.getName();
                if (name.equals("null") || name.isEmpty())
                {
                    name = DEFAULT_NAME;
                }

                modelStats.add(new ModelStats(
                        modelId,
                        name,
                        BodyPart.SPOTANIM,
                        rf,
                        rt,
                        new short[0],
                        new short[0],
                        spotanimData.getResizeX(),
                        spotanimData.getResizeX(),
                        spotanimData.getResizeY(),
                        0,
                        customLighting));
            }
        }

        if (modelStats.isEmpty())
        {
            return null;
        }

        return new ModelStats[]{modelStats.get(0)};
    }

    public ModelStats[] findSpotAnim(SpotAnimDefinition spotanimData)
    {
        if (spotanimData == null)
        {
            return null;
        }

        ArrayList<ModelStats> modelStats = new ArrayList<>();
        int modelId = spotanimData.getModelId();

        lastAnim = spotanimData.getAnimationId();

        short[] rf = spotanimData.getRecolorToFind();
        short[] rt = spotanimData.getRecolorToReplace();

        int ambient = spotanimData.getAmbient();
        int contrast = spotanimData.getContrast();

        LightingStyle ls = LightingStyle.SPOTANIM;
        CustomLighting customLighting = new CustomLighting(
                ls.getAmbient() + ambient,
                ls.getContrast() + contrast,
                ls.getX(),
                ls.getY(),
                ls.getZ());

        String name = spotanimData.getName();
        if (name.equals("null") || name.isEmpty())
        {
            name = DEFAULT_NAME;
        }

        modelStats.add(new ModelStats(
                modelId,
                name,
                BodyPart.SPOTANIM,
                rf,
                rt,
                new short[0],
                new short[0],
                spotanimData.getResizeX(),
                spotanimData.getResizeX(),
                spotanimData.getResizeY(),
                0,
                customLighting));

        return new ModelStats[]{modelStats.get(0)};
    }

    public SpotAnimDefinition getSpotAnimData(int spotAnimId)
    {
        for (SpotAnimDefinition data : spotanimData)
        {
            if (data.getId() == spotAnimId)
            {
                return data;
            }
        }

        return null;
    }

    public void lookupNPCData()
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return;
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
                npcData.add(def);
            }
        }

        executeCallbacks(DataType.NPC);
    }

    public NpcDefinition findNPCData(NPC npc)
    {
        for (NpcDefinition npcData : npcData)
        {
            if (npcData.getId() == npc.getId())
            {
                return npcData;
            }
        }

        return null;
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
                    comp.getWidthScale(),
                    comp.getWidthScale(),
                    comp.getHeightScale(),
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

    public ModelStats[] findModelsForNPC(int npcId)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        for (NpcDefinition npcData : npcData)
        {
            if (npcData.getId() == npcId)
            {
                lastAnim = npcData.getStandingAnimation();

                int[] modelIds = npcData.getModels();
                if (modelIds == null || modelIds.length == 0)
                {
                    return new ModelStats[0];
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
                            npcData.getWidthScale(),
                            npcData.getWidthScale(),
                            npcData.getHeightScale(),
                            0,
                            customLighting
                    ));
                }

                break;
            }
        }

        ModelStats[] stats = new ModelStats[modelStats.size()];
        for (int i = 0; i < modelStats.size(); i++)
        {
            stats[i] = modelStats.get(i);
        }

        return stats;
    }

    private void lookupObjectData()
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return;
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
                objectData.add(def);
            }
        }

        objectData.sort(Comparator.comparing(ObjectDefinition::getName));
        executeCallbacks(DataType.OBJECT);
    }

    public ModelStats[] findModelsForObject(int objectId, int modelType, LightingStyle ls, boolean firstModelType)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        for (ObjectDefinition objectData : objectData)
        {
            if (objectData.getId() == objectId)
            {
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

                break;
            }
        }

        ModelStats[] stats = new ModelStats[modelStats.size()];
        for (int i = 0; i < modelStats.size(); i++)
        {
            stats[i] = modelStats.get(i);
        }

        return stats;
    }

    private void lookupItemData()
    {
        if (client == null || client.getIndexConfig() == null)
        {
            return;
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
                itemData.add(def);
            }
        }

        itemData.sort(Comparator.comparing(ItemDefinition::getName));
        executeCallbacks(DataType.ITEM);
    }

    public ModelStats[] findModelsForGroundItem(int itemId, CustomModelType modelType)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        for (ItemDefinition item : itemData)
        {
            if (item.getId() == itemId)
            {
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

                break;
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

    private void lookupSoundData()
    {
        Request request = new Request.Builder().url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/refs/heads/master/.venv/sounds.json").build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/refs/heads/master/.venv/sounds.json");
                executeCallbacks(DataType.SOUND);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    //create a reader to read the URL
                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());

                    Type listType = new TypeToken<List<SoundData>>() {}.getType();
                    List<SoundData> list = gson.fromJson(reader, listType);

                    soundData.addAll(list);
                    response.body().close();
                }
                executeCallbacks(DataType.SOUND);
            }
        });
    }

    public String generateNameFromModel(int id)
    {
        if (id == -1)
        {
            return DEFAULT_NAME;
        }

        for (KitDefinition data : kitData)
        {
            if (data.getModels() != null && Arrays.stream(data.getModels()).anyMatch(e -> e == id))
            {
                return BodyPart.bodyPartIdToBodyPart(data.getBodyPartId()).getName();
            }

            if (data.getChatheadModels() != null && Arrays.stream(data.getChatheadModels()).anyMatch(e -> e == id))
            {
                return BodyPart.bodyPartIdToBodyPart(data.getBodyPartId()).getName();
            }
        }

        for (ObjectDefinition data : objectData)
        {
            if (data.getObjectModels() == null)
            {
                continue;
            }

            if (Arrays.stream(data.getObjectModels()).anyMatch(e -> e == id))
            {
                return data.getName();
            }
        }

        for (ItemDefinition data : itemData)
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
                return data.getName();
            }
        }

        for (SpotAnimDefinition data : spotanimData)
        {
            if (data.getModelId() == id)
            {
                return data.getName();
            }
        }

        return DEFAULT_NAME;
    }
}
