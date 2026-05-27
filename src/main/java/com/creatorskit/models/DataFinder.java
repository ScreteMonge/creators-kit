package com.creatorskit.models;

import com.creatorskit.cache.parser.CacheLoader;
import com.creatorskit.models.datatypes.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.kit.KitType;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

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

    private Gson gson;
    OkHttpClient httpClient;

    private int lastAnim;
    private static final String DEFAULT_NAME = "Name";

    private final List<NPCData> npcData = new ArrayList<>();
    private final List<ObjectData> objectData = new ArrayList<>();
    private final List<SpotanimData> spotanimData = new ArrayList<>();
    private final List<ItemData> itemData = new ArrayList<>();
    private final List<KitData> kitData = new ArrayList<>();
    private final List<SeqData> seqData = new ArrayList<>();
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

    private final CacheLoader cacheLoader;

    /**
     * Reload-callbacks fire after CacheLoader replaces the URL-derived
     * list with live-cache-derived entries. CacheSearcherTab listens
     * here to re-initialise its JFilterableTable with the freshly
     * complete dataset. Separate from {@link #loadCallbacks} because
     * those clear themselves after the first fire, but reload may
     * fire after that initial run.
     */
    private final ConcurrentHashMap<DataType, List<Runnable>> reloadListeners = new ConcurrentHashMap<>(){{
        Arrays.stream(DataType.values()).forEach(d -> this.put(d, new ArrayList<>()));
    }};

    @Inject
    public DataFinder(Gson gson, OkHttpClient httpClient, CacheLoader cacheLoader)
    {
        this.gson = gson;
        this.httpClient = httpClient;
        this.cacheLoader = cacheLoader;

        lookupNPCData();
        lookupObjectData();
        lookupSpotAnimData();
        lookupItemData();
        lookupKitData();
        lookupSeqData();
        lookupAnimData();
        lookupWeaponAnimationData();
        lookupSoundData();

        // Kick off the live-cache hydration on a background thread.
        // CacheLoader.load() does the heavy lifting (~500ms-2s); when it
        // finishes, we replace npcData / objectData / itemData /
        // spotanimData with cache-derived entries and fire reload
        // listeners so the searcher tables re-initialise. URL data
        // stays as the fallback for anyone whose cache dir is missing.
        Thread t = new Thread(this::hydrateFromLocalCache, "creatorskit-cache-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Subscribe to "data list was replaced with cache-derived entries"
     * events. Listener runs on the cache-loader background thread; UI
     * subscribers should marshal back to the EDT internally.
     */
    public void addReloadListener(DataType dataType, Runnable listener)
    {
        if (listener != null) reloadListeners.get(dataType).add(listener);
    }

    private void fireReload(DataType dataType)
    {
        for (Runnable l : new ArrayList<>(reloadListeners.get(dataType)))
        {
            try { l.run(); }
            catch (Throwable t) { log.warn("DataFinder reload listener threw", t); }
        }
    }

    /**
     * Background-thread entry point. Loads the local cache via
     * {@link CacheLoader}, converts each definition to its searcher
     * data-type, REPLACES the URL-derived list, sorts by name, and
     * fires the reload listeners.
     */
    private void hydrateFromLocalCache()
    {
        cacheLoader.load();
        if (!cacheLoader.isLoaded())
        {
            log.debug("CacheLoader didn't load; cache searcher stays on URL data");
            return;
        }

        // Block until the URL fetches finish (or hit a safety cap). The
        // rebuild path reads existing entries from each XData list to
        // preserve URL-supplied names through the cache replacement --
        // particularly important for SpotAnims, where
        // SpotAnimDefinition.debugName is null for the vast majority of
        // entries even though the upstream JSON dump did name them.
        // 30s is just a deadlock safety -- in practice the URL fetches
        // resolve in well under a second on a normal connection.
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(4);
        addLoadCallback(DataType.NPC, latch::countDown);
        addLoadCallback(DataType.OBJECT, latch::countDown);
        addLoadCallback(DataType.ITEM, latch::countDown);
        addLoadCallback(DataType.SPOTANIM, latch::countDown);
        try { latch.await(30, java.util.concurrent.TimeUnit.SECONDS); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }

        rebuildNpcDataFromCache();
        rebuildObjectDataFromCache();
        rebuildItemDataFromCache();
        rebuildSpotAnimDataFromCache();
    }

    private void rebuildNpcDataFromCache()
    {
        List<NPCData> rebuilt = new ArrayList<>(cacheLoader.getNpcs().size());
        for (net.runelite.cache.definitions.NpcDefinition def : cacheLoader.getNpcs().values())
        {
            String name = (def.name == null || def.name.equalsIgnoreCase("null") || def.name.isEmpty())
                    ? "Unnamed" : def.name;
            rebuilt.add(new NPCData(
                    def.id,
                    name,
                    def.models == null ? new int[0] : def.models,
                    def.size,
                    def.standingAnimation,
                    def.walkingAnimation,
                    def.runAnimation,
                    def.idleRotateLeftAnimation,
                    def.idleRotateRightAnimation,
                    def.rotate180Animation,
                    def.rotateLeftAnimation,
                    def.rotateRightAnimation,
                    def.widthScale,
                    def.heightScale,
                    toIntArray(def.recolorToReplace),
                    toIntArray(def.recolorToFind)));
        }
        rebuilt.sort(Comparator.comparing(NPCData::getName));
        synchronized (npcData) { npcData.clear(); npcData.addAll(rebuilt); }
        fireReload(DataType.NPC);
    }

    private void rebuildObjectDataFromCache()
    {
        List<ObjectData> rebuilt = new ArrayList<>(cacheLoader.getObjects().size());
        for (net.runelite.cache.definitions.ObjectDefinition def : cacheLoader.getObjects().values())
        {
            String n = def.getName();
            String name = (n == null || n.equalsIgnoreCase("null") || n.isEmpty()) ? "Unnamed" : n;
            rebuilt.add(new ObjectData(
                    def.getId(),
                    name,
                    def.getAnimationID(),
                    def.getObjectModels() == null ? new int[0] : def.getObjectModels(),
                    def.getObjectTypes() == null ? new int[0] : def.getObjectTypes(),
                    def.getModelSizeX(),
                    def.getModelSizeY(),
                    def.getModelSizeHeight(),
                    def.getAmbient(),
                    def.getContrast(),
                    toIntArray(def.getRecolorToReplace()),
                    toIntArray(def.getRecolorToFind()),
                    toIntArray(def.getTextureToReplace()),
                    toIntArray(def.getRetextureToFind())));
        }
        rebuilt.sort(Comparator.comparing(ObjectData::getName));
        synchronized (objectData) { objectData.clear(); objectData.addAll(rebuilt); }
        fireReload(DataType.OBJECT);
    }

    private void rebuildItemDataFromCache()
    {
        List<ItemData> rebuilt = new ArrayList<>(cacheLoader.getItems().size());
        for (net.runelite.cache.definitions.ItemDefinition def : cacheLoader.getItems().values())
        {
            String name = (def.name == null || def.name.equalsIgnoreCase("null") || def.name.isEmpty())
                    ? "Unnamed" : def.name;
            rebuilt.add(new ItemData(
                    def.id,
                    name,
                    def.inventoryModel,
                    def.maleModel0,
                    def.maleModel1,
                    def.maleModel2,
                    def.maleOffset,
                    def.femaleModel0,
                    def.femaleModel1,
                    def.femaleModel2,
                    0, 0, 0,  // wearPos0/1/2 -- not exposed on ItemDefinition
                    def.femaleOffset,
                    def.maleHeadModel,
                    def.maleHeadModel2,
                    def.femaleHeadModel,
                    def.femaleHeadModel2,
                    def.resizeX,
                    def.resizeY,
                    def.resizeZ,
                    toIntArray(def.colorReplace),
                    toIntArray(def.colorFind),
                    toIntArray(def.textureReplace),
                    toIntArray(def.textureFind)));
        }
        rebuilt.sort(Comparator.comparing(ItemData::getName));
        synchronized (itemData) { itemData.clear(); itemData.addAll(rebuilt); }
        fireReload(DataType.ITEM);
    }

    private void rebuildSpotAnimDataFromCache()
    {
        // Snapshot the URL-loaded names BEFORE we clear and rebuild.
        // SpotAnimDefinition.debugName is null for the vast majority of
        // cache entries -- Jagex barely populated it. Without this
        // pass the cache replacement would wipe every name supplied by
        // the upstream JSON dump and the entire SpotAnim searcher
        // would read as "Unnamed".
        java.util.Map<Integer, String> urlNames = new java.util.HashMap<>();
        synchronized (spotanimData)
        {
            for (SpotanimData d : spotanimData)
            {
                String n = d.getName();
                if (n != null && !n.isEmpty() && !n.equalsIgnoreCase("Unnamed"))
                {
                    urlNames.put(d.getId(), n);
                }
            }
        }

        List<SpotanimData> rebuilt = new ArrayList<>(cacheLoader.getSpotAnims().size());
        for (net.runelite.cache.definitions.SpotAnimDefinition def : cacheLoader.getSpotAnims().values())
        {
            // Name priority: upstream JSON > cache debugName > "Unnamed".
            String name = urlNames.get(def.id);
            if (name == null)
            {
                name = (def.debugName == null || def.debugName.isEmpty()) ? "Unnamed" : def.debugName;
            }
            rebuilt.add(new SpotanimData(
                    name,
                    def.id,
                    def.modelId,
                    def.animationId,
                    def.resizeX,
                    def.resizeY,
                    def.ambient,
                    def.contrast,
                    toIntArray(def.recolorToReplace),
                    toIntArray(def.recolorToFind)));
        }
        rebuilt.sort(Comparator.comparing(SpotanimData::getName));
        synchronized (spotanimData) { spotanimData.clear(); spotanimData.addAll(rebuilt); }
        fireReload(DataType.SPOTANIM);
    }

    /**
     * Convert a {@code short[]} (Jagex colour / texture id storage) to
     * the {@code int[]} our XData classes carry. Caller treats null as
     * "no recolours". Sign-extends naturally since short -> int is the
     * standard widening conversion -- the colour rebuild paths in
     * findModelsForX downstream re-narrow to short and handle the
     * 0xFFFF wraparound.
     */
    private static int[] toIntArray(short[] arr)
    {
        if (arr == null) return null;
        int[] out = new int[arr.length];
        for (int i = 0; i < arr.length; i++) out[i] = arr[i] & 0xFFFF;
        return out;
    }

    private static int[] toIntArray(int[] arr)
    {
        return arr;  // already the right shape
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
        Request kitRequest = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/kit.json")
                .build();
        Call kitCall = httpClient.newCall(kitRequest);
        kitCall.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/kit.json");
                executeCallbacks(DataType.KIT);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                    Type listType = new TypeToken<List<KitData>>() {}.getType();
                    List<KitData> list = gson.fromJson(reader, listType);
                    kitData.addAll(list);

                    response.body().close();
                }
                executeCallbacks(DataType.KIT);
            }
        });
    }

    private void lookupSeqData()
    {
        Request seqRequest = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/sequences.json")
                .build();
        Call call = httpClient.newCall(seqRequest);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/sequences.json");
                executeCallbacks(DataType.SEQ);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                    Type listType = new TypeToken<List<SeqData>>() {}.getType();
                    List<SeqData> list = gson.fromJson(reader, listType);
                    seqData.addAll(list);

                    response.body().close();
                }
                executeCallbacks(DataType.SEQ);
            }
        });
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
                appendUnnamedAnims();
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
                appendUnnamedAnims();  // extend past URL max using live idx0 size
                executeCallbacks(DataType.ANIM);
            }
        });
    }

    public ModelStats[] findModelsForPlayer(boolean groundItem, boolean maleItem, int[] items, int animId, int[] spotAnims)
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
            removePlayerItems(animSequence, animId);
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

    public void removePlayerItems(AnimSequence animSequence, int animId)
    {
        for (SeqData seqDatum : seqData)
        {
            if (seqDatum.getId() == animId)
            {
                int offHandItem = seqDatum.getLeftHandItem();
                switch (offHandItem)
                {
                    case -1:
                        break;
                    case 0:
                        animSequence.setOffHandData(AnimSequenceData.HIDE);
                        break;
                    default:
                        animSequence.setOffHandItemId(offHandItem - 512);
                        animSequence.setOffHandData(AnimSequenceData.SWAP);
                }

                int mainHandItem = seqDatum.getRightHandItem();
                switch (mainHandItem)
                {
                    case -1:
                        break;
                    case 0:
                        animSequence.setMainHandData(AnimSequenceData.HIDE);
                        break;
                    default:
                        animSequence.setMainHandItemId(mainHandItem - 512);
                        animSequence.setMainHandData(AnimSequenceData.SWAP);
                }
                break;
            }
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

        for (ItemData itemDatum : itemData)
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

                    short[] rf = new short[0];
                    short[] rt = new short[0];

                    if (itemDatum.getColorReplace() != null)
                    {
                        int[] recolorToReplace = itemDatum.getColorReplace();
                        int[] recolorToFind = itemDatum.getColorFind();
                        rf = new short[recolorToReplace.length];
                        rt = new short[recolorToReplace.length];

                        for (int e = 0; e < rf.length; e++)
                        {
                            int rfi = recolorToFind[e];
                            if (rfi > 32767)
                            {
                                rfi -= 65536;
                            }
                            rf[e] = (short) rfi;

                            int rti = recolorToReplace[e];
                            if (rti > 32767)
                            {
                                rti -= 65536;
                            }
                            rt[e] = (short) rti;
                        }
                    }

                    short[] rtFrom = new short[0];
                    short[] rtTo = new short[0];

                    if (itemDatum.getTextureReplace() != null)
                    {
                        int[] textureToReplace = itemDatum.getTextureReplace();
                        int[] retextureToFind = itemDatum.getTextureFind();
                        rtFrom = new short[textureToReplace.length];
                        rtTo = new short[textureToReplace.length];

                        for (int e = 0; e < rtFrom.length; e++)
                        {
                            rtFrom[e] = (short) retextureToFind[e];
                            rtTo[e] = (short) textureToReplace[e];
                        }
                    }

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

        for (KitData kitData : kitData)
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

                    short[] rf = new short[0];
                    short[] rt = new short[0];

                    if (kitData.getRecolorToReplace() != null)
                    {
                        int[] recolorToReplace = kitData.getRecolorToReplace();
                        int[] recolorToFind = kitData.getRecolorToFind();
                        rf = new short[recolorToReplace.length];
                        rt = new short[recolorToReplace.length];

                        for (int e = 0; e < rf.length; e++)
                        {
                            int rfi = recolorToFind[e];
                            if (rfi > 32767)
                            {
                                rfi -= 65536;
                            }
                            rf[e] = (short) rfi;

                            int rti = recolorToReplace[e];
                            if (rti > 32767)
                            {
                                rti -= 65536;
                            }
                            rt[e] = (short) rti;
                        }
                    }

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
                                    new short[0],
                                    new short[0],
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

        for (SpotanimData spotanimData : spotanimData)
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

                    short[] rf = new short[0];
                    short[] rt = new short[0];
                    if (spotanimData.getRecolorToReplace() != null)
                    {
                        int[] recolorToReplace = spotanimData.getRecolorToReplace();
                        int[] recolorToFind = spotanimData.getRecolorToFind();
                        rf = new short[recolorToReplace.length];
                        rt = new short[recolorToReplace.length];

                        for (int e = 0; e < rf.length; e++)
                        {
                            int rfi = recolorToFind[e];
                            if (rfi > 32767)
                            {
                                rfi -= 65536;
                            }
                            rf[e] = (short) rfi;

                            int rti = recolorToReplace[e];
                            if (rti > 32767)
                            {
                                rti -= 65536;
                            }
                            rt[e] = (short) rti;
                        }
                    }

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

                    spotanimData.addAll(list);
                    response.body().close();
                }
                executeCallbacks(DataType.SPOTANIM);
            }
        });
    }

    public ModelStats[] findSpotAnim(int spotAnimId)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        for (SpotanimData spotanimData : spotanimData)
        {
            if (spotanimData.getId() == spotAnimId)
            {
                int modelId = spotanimData.getModelId();

                lastAnim = spotanimData.getAnimationId();

                short[] rf = new short[0];
                short[] rt = new short[0];
                if (spotanimData.getRecolorToReplace() != null)
                {
                    int[] recolorToReplace = spotanimData.getRecolorToReplace();
                    int[] recolorToFind = spotanimData.getRecolorToFind();
                    rf = new short[recolorToReplace.length];
                    rt = new short[recolorToReplace.length];

                    for (int e = 0; e < rf.length; e++)
                    {
                        int rfi = recolorToFind[e];
                        if (rfi > 32767)
                        {
                            rfi -= 65536;
                        }
                        rf[e] = (short) rfi;

                        int rti = recolorToReplace[e];
                        if (rti > 32767)
                        {
                            rti -= 65536;
                        }
                        rt[e] = (short) rti;
                    }
                }

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

    public ModelStats[] findSpotAnim(SpotanimData spotanimData)
    {
        if (spotanimData == null)
        {
            return null;
        }

        ArrayList<ModelStats> modelStats = new ArrayList<>();
        int modelId = spotanimData.getModelId();

        lastAnim = spotanimData.getAnimationId();

        short[] rf = new short[0];
        short[] rt = new short[0];
        if (spotanimData.getRecolorToReplace() != null)
        {
            int[] recolorToReplace = spotanimData.getRecolorToReplace();
            int[] recolorToFind = spotanimData.getRecolorToFind();
            rf = new short[recolorToReplace.length];
            rt = new short[recolorToReplace.length];

            for (int e = 0; e < rf.length; e++)
            {
                int rfi = recolorToFind[e];
                if (rfi > 32767)
                {
                    rfi -= 65536;
                }
                rf[e] = (short) rfi;

                int rti = recolorToReplace[e];
                if (rti > 32767)
                {
                    rti -= 65536;
                }
                rt[e] = (short) rti;
            }
        }

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

    public SpotanimData getSpotAnimData(int spotAnimId)
    {
        for (SpotanimData data : spotanimData)
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
        Request request = new Request.Builder().url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/npc_defs.json").build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/npc_defs.json");
                executeCallbacks(DataType.NPC);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());

                    Type listType = new TypeToken<List<NPCData>>() {}.getType();
                    List<NPCData> list = gson.fromJson(reader, listType);

                    npcData.addAll(list);
                    npcData.sort(Comparator.comparing(NPCData::getName));
                    response.body().close();
                }
                executeCallbacks(DataType.NPC);
            }
        });
    }

    public NPCData findNPCData(NPC npc)
    {
        for (NPCData npcData : npcData)
        {
            if (npcData.getId() == npc.getId())
            {
                return npcData;
            }
        }

        return null;
    }

    public ModelStats[] findModelsForNPC(int npcId)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        for (NPCData npcData : npcData)
        {
            if (npcData.getId() == npcId)
            {
                lastAnim = npcData.getStandingAnimation();

                int[] modelIds = npcData.getModels();

                short[] rf = new short[0];
                short[] rt = new short[0];

                if (npcData.getRecolorToReplace() != null)
                {
                    int[] recolorToReplace = npcData.getRecolorToReplace();
                    int[] recolorToFind = npcData.getRecolorToFind();
                    rf = new short[recolorToReplace.length];
                    rt = new short[recolorToReplace.length];

                    for (int i = 0; i < rf.length; i++)
                    {
                        int rfi = recolorToFind[i];
                        if (rfi > 32767)
                        {
                            rfi -= 65536;
                        }
                        rf[i] = (short) rfi;

                        int rti = recolorToReplace[i];
                        if (rti > 32767)
                        {
                            rti -= 65536;
                        }
                        rt[i] = (short) rti;
                    }
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
                            npcData.getName(),
                            BodyPart.NA,
                            rf,
                            rt,
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

    public ModelStats[] findModelsForNPC(int npcId, NpcOverrides overrides)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        for (NPCData npcData : npcData)
        {
            if (npcData.getId() == npcId)
            {
                lastAnim = npcData.getStandingAnimation();

                int[] modelIds = overrides.getModelIds();

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
                            new short[0],
                            new short[0],
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

    public ModelStats[] findModelsForNPC(int npcId, NPCComposition composition)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        for (NPCData npcData : npcData)
        {
            if (npcData.getId() == npcId)
            {
                lastAnim = npcData.getStandingAnimation();

                int[] modelIds = composition.getModels();
                short[] colourToReplace = composition.getColorToReplace();
                short[] colourToReplaceWith = composition.getColorToReplaceWith();

                if (colourToReplace == null || colourToReplaceWith == null)
                {
                    colourToReplace = new short[0];
                    colourToReplaceWith = new short[0];
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
                            npcData.getName(),
                            BodyPart.NA,
                            colourToReplace,
                            colourToReplaceWith,
                            new short[0],
                            new short[0],
                            composition.getWidthScale(),
                            composition.getWidthScale(),
                            composition.getHeightScale(),
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
        Request request = new Request.Builder().url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/object_defs.json").build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/object_defs.json");
                executeCallbacks(DataType.OBJECT);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    //create a reader to read the URL
                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());

                    Type listType = new TypeToken<List<ObjectData>>() {}.getType();
                    List<ObjectData> list = gson.fromJson(reader, listType);

                    objectData.addAll(list);
                    objectData.sort(Comparator.comparing(ObjectData::getName));
                    response.body().close();
                }
                executeCallbacks(DataType.OBJECT);
            }
        });
    }

    public ModelStats[] findModelsForObject(int objectId, int modelType, LightingStyle ls, boolean firstModelType)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        for (ObjectData objectData : objectData)
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

                short[] rf = new short[0];
                short[] rt = new short[0];
                if (objectData.getRecolorToReplace() != null)
                {
                    int[] recolorToReplace = objectData.getRecolorToReplace();
                    int[] recolorToFind = objectData.getRecolorToFind();
                    rf = new short[recolorToReplace.length];
                    rt = new short[recolorToReplace.length];

                    for (int i = 0; i < rf.length; i++)
                    {
                        int rfi = recolorToFind[i];
                        if (rfi > 32767)
                        {
                            rfi -= 65536;
                        }
                        rf[i] = (short) rfi;

                        int rti = recolorToReplace[i];
                        if (rti > 32767)
                        {
                            rti -= 65536;
                        }
                        rt[i] = (short) rti;
                    }
                }

                short[] rtFrom = new short[0];
                short[] rtTo = new short[0];

                if (objectData.getTextureToReplace() != null && objectData.getRetextureToFind() != null)
                {
                    int[] textureToReplace = objectData.getTextureToReplace();
                    int[] retextureToFind = objectData.getRetextureToFind();
                    rtFrom = new short[textureToReplace.length];
                    rtTo = new short[textureToReplace.length];

                    for (int i = 0; i < rtFrom.length; i++)
                    {
                        rtFrom[i] = (short) retextureToFind[i];
                        rtTo[i] = (short) textureToReplace[i];
                    }
                }

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
                            objectData.getModelSizeZ(),
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
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Request itemRequest = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/item_defs.json")
                .build();
        Call itemCall = httpClient.newCall(itemRequest);
        itemCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/item_defs.json");
                countDownLatch.countDown();
                executeCallbacks(DataType.ITEM);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                    Type listType = new TypeToken<List<ItemData>>() {}.getType();
                    List<ItemData> list = gson.fromJson(reader, listType);
                    itemData.addAll(list);
                    itemData.sort(Comparator.comparing(ItemData::getName));

                    response.body().close();
                }
                executeCallbacks(DataType.ITEM);
            }
        });
    }

    public ModelStats[] findModelsForGroundItem(int itemId, CustomModelType modelType)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        for (ItemData item : itemData)
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

                short[] rf = new short[0];
                short[] rt = new short[0];

                if (item.getColorReplace() != null)
                {
                    int[] recolorToReplace = item.getColorReplace();
                    int[] recolorToFind = item.getColorFind();
                    rf = new short[recolorToReplace.length];
                    rt = new short[recolorToReplace.length];

                    for (int e = 0; e < rf.length; e++)
                    {
                        int rfi = recolorToFind[e];
                        if (rfi > 32767)
                        {
                            rfi -= 65536;
                        }
                        rf[e] = (short) rfi;

                        int rti = recolorToReplace[e];
                        if (rti > 32767)
                        {
                            rti -= 65536;
                        }
                        rt[e] = (short) rti;
                    }
                }

                short[] rtFrom = new short[0];
                short[] rtTo = new short[0];

                if (item.getTextureReplace() != null)
                {
                    int[] textureToReplace = item.getTextureReplace();
                    int[] retextureToFind = item.getTextureFind();
                    rtFrom = new short[textureToReplace.length];
                    rtTo = new short[textureToReplace.length];

                    for (int e = 0; e < rtFrom.length; e++)
                    {
                        rtFrom[e] = (short) retextureToFind[e];
                        rtTo[e] = (short) textureToReplace[e];
                    }
                }

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
                            wearPos = item.getWearPos0();
                            break;
                        case 1:
                            wearPos = item.getWearPos1();
                            break;
                        case 2:
                            wearPos = item.getWearPos2();
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

    // Placeholder caps for the Sound + Anim searchers. These two indices
    // are read directly from main_file_cache.idx{N} at runtime, so the
    // cap reflects whatever the user's live cache actually holds rather
    // than a hardcoded guess. Falls back to a defensive cap of 0 if the
    // idx file can't be opened (in which case no placeholders are added
    // and the URL dump's range is all the user sees).

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
                // Still emit the unnamed-tail entries so the searcher
                // isn't completely empty when the URL fetch fails (e.g.
                // offline). The user gets a usable picker even if it's
                // only placeholders.
                appendUnnamedSounds();
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
                appendUnnamedSounds();
                executeCallbacks(DataType.SOUND);
            }
        });
    }

    /**
     * Reads {@code main_file_cache.idx<n>}'s entry count to determine
     * the live cache's max id for indices that map 1:1 (id -> file).
     * Each idx entry is 6 bytes (3-byte size + 3-byte first sector);
     * dividing the file's length gives the slot count, and (count - 1)
     * is the highest valid id. Returns -1 when the file can't be
     * opened so callers fall back to URL-only data.
     *
     * <p>This works directly only for "flat" indices like sounds (idx4)
     * and anims (idx0). Configs (NPCs/Objects/Items/SpotAnims) live
     * inside compressed archives in idx2 and require a cache parser
     * -- those are NOT extended past the URL range here.
     */
    private static int readLiveMaxIdFromIdx(int indexNumber)
    {
        try
        {
            java.io.File idx = new java.io.File(net.runelite.client.RuneLite.RUNELITE_DIR,
                    "jagexcache/oldschool/LIVE/main_file_cache.idx" + indexNumber);
            if (!idx.exists()) return -1;
            long len = idx.length();
            if (len <= 0) return -1;
            return (int) (len / 6) - 1;  // count - 1 = max valid id
        }
        catch (Exception e)
        {
            log.debug("Failed to read idx{} size for live cap", indexNumber, e);
            return -1;
        }
    }

    /**
     * Appends {@code "Unnamed"} placeholders to {@link #soundData} for
     * every id between the URL dump's max + 1 and the live cache's idx4
     * max (read from disk). Items already present at the same id in the
     * URL dump are skipped. Sound entries past id 10200 lost their
     * config names in the 26 Feb 2025 update -- this lets users still
     * pick those modern sounds by id from the searcher.
     */
    private void appendUnnamedSounds()
    {
        int liveMax = readLiveMaxIdFromIdx(4);
        if (liveMax < 0) return;
        int maxNamed = -1;
        java.util.Set<Integer> have = new java.util.HashSet<>();
        for (SoundData d : soundData) { have.add(d.getId()); if (d.getId() > maxNamed) maxNamed = d.getId(); }
        int start = maxNamed < 0 ? 0 : maxNamed + 1;
        for (int id = start; id <= liveMax; id++)
        {
            if (have.contains(id)) continue;
            soundData.add(new SoundData(id, "Unnamed"));
        }
    }

    /**
     * Anim siblings to {@link #appendUnnamedSounds}. idx0 also maps
     * id -> file 1:1 so the size-based extraction works directly.
     * Anim's only display fields are (id, name) so no model hydration
     * is needed -- the placeholder data is complete on its own.
     */
    private void appendUnnamedAnims()
    {
        int liveMax = readLiveMaxIdFromIdx(0);
        if (liveMax < 0) return;
        int maxNamed = -1;
        java.util.Set<Integer> have = new java.util.HashSet<>();
        for (AnimData d : animData) { have.add(d.getId()); if (d.getId() > maxNamed) maxNamed = d.getId(); }
        int start = maxNamed < 0 ? 0 : maxNamed + 1;
        for (int id = start; id <= liveMax; id++)
        {
            if (have.contains(id)) continue;
            animData.add(new AnimData(id, "Unnamed"));
        }
    }

    public String generateNameFromModel(int id)
    {
        if (id == -1)
        {
            return DEFAULT_NAME;
        }

        for (KitData data : kitData)
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

        for (ObjectData data : objectData)
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

        for (ItemData data : itemData)
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

        for (SpotanimData data : spotanimData)
        {
            if (data.getModelId() == id)
            {
                return data.getName();
            }
        }

        return DEFAULT_NAME;
    }
}
