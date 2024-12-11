package com.creatorskit.models;

import com.creatorskit.models.datatypes.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPCComposition;
import net.runelite.api.NpcOverrides;
import net.runelite.api.PlayerComposition;
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
import java.util.concurrent.CountDownLatch;

@Slf4j
@Getter
public class DataFinder
{
    private Gson gson;
    OkHttpClient httpClient;

    private String lastFound;
    private int lastAnim;

    private final List<NPCData> npcData = new ArrayList<>();
    private final List<ObjectData> objectData = new ArrayList<>();
    private final List<SpotanimData> spotanimData = new ArrayList<>();
    private final List<ItemData> itemData = new ArrayList<>();
    private final List<KitData> kitData = new ArrayList<>();
    private final List<SeqData> seqData = new ArrayList<>();

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
    public DataFinder(Gson gson, OkHttpClient httpClient)
    {
        this.gson = gson;
        this.httpClient = httpClient;

        lookupNPCData();
        lookupObjectData();
        lookupSpotAnimData();
        lookupItemData();
        lookupKitData();
        lookupSeqData();
    }

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
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                Type listType = new TypeToken<List<KitData>>(){}.getType();
                List<KitData> list = gson.fromJson(reader, listType);
                kitData.addAll(list);

                response.body().close();
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
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                Type listType = new TypeToken<List<SeqData>>(){}.getType();
                List<SeqData> list = gson.fromJson(reader, listType);
                seqData.addAll(list);

                response.body().close();
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

                    for (int id : modelIds)
                    {
                        if (id != -1)
                        {
                            modelStats.add(new ModelStats(
                                    id,
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

                    modelStats.add(new ModelStats(
                            modelId,
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
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                Type listType = new TypeToken<List<SpotanimData>>(){}.getType();
                List<SpotanimData> list = gson.fromJson(reader, listType);

                spotanimData.addAll(list);
                response.body().close();
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

                lastFound = "SpotAnim " + spotAnimId;
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

                modelStats.add(new ModelStats(
                        modelId,
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
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStreamReader reader = new InputStreamReader(response.body().byteStream());

                Type listType = new TypeToken<List<NPCData>>(){}.getType();
                List<NPCData> list = gson.fromJson(reader, listType);

                npcData.addAll(list);
                npcData.sort(Comparator.comparing(NPCData::getName));
                response.body().close();
            }
        });
    }

    public ModelStats[] findModelsForNPC(int npcId)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        for (NPCData npcData : npcData)
        {
            if (npcData.getId() == npcId)
            {
                lastFound = npcData.getName();
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
                lastFound = npcData.getName();
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
                lastFound = npcData.getName();
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
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                //create a reader to read the URL
                InputStreamReader reader = new InputStreamReader(response.body().byteStream());

                Type listType = new TypeToken<List<ObjectData>>(){}.getType();
                List<ObjectData> list = gson.fromJson(reader, listType);

                objectData.addAll(list);
                objectData.sort(Comparator.comparing(ObjectData::getName));
                response.body().close();
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
                lastFound = objectData.getName();
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

                if (objectData.getTextureToReplace() != null)
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

                for (int i : modelIds)
                {
                    modelStats.add(new ModelStats(
                            i,
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
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                Type listType = new TypeToken<List<ItemData>>(){}.getType();
                List<ItemData> list = gson.fromJson(reader, listType);
                itemData.addAll(list);
                itemData.sort(Comparator.comparing(ItemData::getName));

                response.body().close();
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
                lastFound = item.getName();
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

                for (int id : modelIds)
                {
                    if (id != -1)
                    {
                        modelStats.add(new ModelStats(
                                id,
                                BodyPart.NA,
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

        return new ModelStats[]{modelStats.get(0)};
    }
}
