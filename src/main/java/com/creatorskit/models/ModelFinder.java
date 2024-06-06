package com.creatorskit.models;

import com.creatorskit.models.datatypes.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ModelFinder
{
    @Inject
    private Gson gson;
    @Inject
    OkHttpClient httpClient;
    @Getter
    private String lastFound;
    @Getter
    private int lastAnim;
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

    public ModelStats[] findModelsForPlayer(boolean groundItem, boolean maleItem, int[] items, int animId, int[] spotAnims)
    {
        //Convert equipmentId to itemId or kitId as appropriate
        int[] ids = new int[items.length];

        int[] itemShortList = new int[items.length];
        int[] kitShortList = new int[items.length];

        for (int i = 0; i < ids.length; i++)
        {
            int item = items[i];

            if (item >= 256 && item <= 512)
            {
                kitShortList[i] = item - 256;
            }
            else
            {
                kitShortList[i] = -1;
            }

            if (item > 512)
            {
                itemShortList[i] = item - 512;
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
            CountDownLatch countDownLatch1 = new CountDownLatch(1);

            Request seqRequest = new Request.Builder()
                    .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/sequences.json")
                    .build();
            Call call = httpClient.newCall(seqRequest);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/sequences.json");
                    countDownLatch1.countDown();
                }

                @Override
                public void onResponse(Call call, Response response)
                {
                    if (!response.isSuccessful() || response.body() == null)
                        return;

                    removePlayerItems(response, animSequence, animId);
                    countDownLatch1.countDown();
                    response.body().close();
                }
            });

            try
            {
                countDownLatch1.await();
            }
            catch (Exception e)
            {
                log.debug("CountDownLatch failed to wait at findModelsForPlayers, AnimSeq");
            }
        }

        CountDownLatch countDownLatch2 = new CountDownLatch(3);

        ArrayList<ModelStats> itemArray = new ArrayList<>();

        Request itemRequest = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/item_defs.json")
                .build();
        Call itemCall = httpClient.newCall(itemRequest);
        itemCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/item_defs.json");
                countDownLatch2.countDown();
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                getPlayerItems(response, itemArray, groundItem, maleItem, itemShortList, animSequence);
                countDownLatch2.countDown();
                response.body().close();
            }
        });

        ArrayList<ModelStats> kitArray = new ArrayList<>();

        //for KitIds

        Request kitRequest = new Request.Builder()
                .url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/kit.json")
                .build();
        Call kitCall = httpClient.newCall(kitRequest);
        kitCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/kit.json");
                countDownLatch2.countDown();
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                getPlayerKit(response, kitArray, kitShortList);
                countDownLatch2.countDown();
                response.body().close();
            }
        });

        ArrayList<ModelStats> spotAnimArray = new ArrayList<>();

        if (spotAnims.length == 0)
        {
            countDownLatch2.countDown();
        }
        else
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
                    countDownLatch2.countDown();
                }

                @Override
                public void onResponse(Call call, Response response)
                {
                    if (!response.isSuccessful() || response.body() == null)
                        return;

                    getPlayerSpotAnims(response, spotAnims, spotAnimArray);
                    countDownLatch2.countDown();
                    response.body().close();
                }
            });
        }

        try
        {
            countDownLatch2.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to wait at findModelsForPlayers, Item/Kits");
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

    public void removePlayerItems(Response response, AnimSequence animSequence, int animId)
    {
        InputStreamReader reader = new InputStreamReader(response.body().byteStream());
        Type listType = new TypeToken<List<SeqData>>(){}.getType();
        List<SeqData> posts = gson.fromJson(reader, listType);

        for (SeqData seqData : posts)
        {
            if (seqData.getId() == animId)
            {
                int offHandItem = seqData.getLeftHandItem();
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

                int mainHandItem = seqData.getRightHandItem();
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

    public void getPlayerItems(Response response, ArrayList<ModelStats> modelStats, boolean groundItem, boolean maleItem, int[] itemId, AnimSequence animSequence)
    {
        InputStreamReader reader = new InputStreamReader(response.body().byteStream());
        Type listType = new TypeToken<List<ItemData>>(){}.getType();
        List<ItemData> posts = gson.fromJson(reader, listType);

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

        for (ItemData itemData : posts)
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

                if (itemData.getId() == item)
                {
                    itemsToComplete--;
                    int[] modelIds = new int[0];
                    int offset = 0;

                    if (groundItem)
                    {
                        modelIds = ArrayUtils.add(modelIds, itemData.getInventoryModel());
                    }
                    else if (maleItem)
                    {
                        modelIds = ArrayUtils.addAll(modelIds, itemData.getMaleModel0(), itemData.getMaleModel1(), itemData.getMaleModel2());
                        offset = itemData.getMaleOffset();
                    }
                    else
                    {
                        modelIds = ArrayUtils.addAll(modelIds, itemData.getFemaleModel0(), itemData.getFemaleModel1(), itemData.getFemaleModel2());
                        offset = itemData.getFemaleOffset();
                    }

                    short[] rf = new short[0];
                    short[] rt = new short[0];

                    if (itemData.getColorReplace() != null)
                    {
                        int[] recolorToReplace = itemData.getColorReplace();
                        int[] recolorToFind = itemData.getColorFind();
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

                    if (itemData.getTextureReplace() != null)
                    {
                        int[] textureToReplace = itemData.getTextureReplace();
                        int[] retextureToFind = itemData.getTextureFind();
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
                                    itemData.getResizeX(),
                                    itemData.getResizeZ(),
                                    itemData.getResizeY(),
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

    public void getPlayerKit(Response response, ArrayList<ModelStats> modelStats, int[] kitId)
    {
        InputStreamReader reader = new InputStreamReader(response.body().byteStream());
        Type listType = new TypeToken<List<KitData>>(){}.getType();
        List<KitData> posts = gson.fromJson(reader, listType);

        int itemsToComplete = kitId.length;
        for (int i : kitId)
        {
            if (i == -1)
            {
                itemsToComplete--;
            }
        }

        for (KitData kitData : posts)
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

    public void getPlayerSpotAnims(Response response, int[] spotAnims, ArrayList<ModelStats> modelStats)
    {
        InputStreamReader reader = new InputStreamReader(response.body().byteStream());
        Type listType = new TypeToken<List<SpotanimData>>(){}.getType();
        List<SpotanimData> posts = gson.fromJson(reader, listType);

        int itemsToComplete = spotAnims.length;

        for (SpotanimData spotanimData : posts)
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

    public ModelStats[] findSpotAnim(int spotAnimId)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);

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
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStreamReader reader = new InputStreamReader(response.body().byteStream());
                Type listType = new TypeToken<List<SpotanimData>>(){}.getType();
                List<SpotanimData> posts = gson.fromJson(reader, listType);

                for (SpotanimData spotanimData : posts)
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

                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to await at findSpotAnim");
        }

        if (modelStats.isEmpty())
        {
            return null;
        }

        return new ModelStats[]{modelStats.get(0)};
    }

    public ModelStats[] findModelsForNPC(int npcId)
    {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        Request request = new Request.Builder().url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/npc_defs.json").build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/npc_defs.json");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStreamReader reader = new InputStreamReader(response.body().byteStream());

                Type listType = new TypeToken<List<NPCData>>(){}.getType();
                List<NPCData> posts = gson.fromJson(reader, listType);
                for (NPCData npcData : posts)
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

                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to await at findModelsForNPC");
        }

        ModelStats[] stats = new ModelStats[modelStats.size()];
        for (int i = 0; i < modelStats.size(); i++)
        {
            stats[i] = modelStats.get(i);
        }

        return stats;
    }

    public ModelStats[] findModelsForObject(int objectId, int modelType, LightingStyle ls, boolean firstModelType)
    {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ArrayList<ModelStats> modelStats = new ArrayList<>();

        Request request = new Request.Builder().url("https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/object_defs.json").build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://raw.githubusercontent.com/ScreteMonge/cache-converter/master/.venv/object_defs.json");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                //create a reader to read the URL
                InputStreamReader reader = new InputStreamReader(response.body().byteStream());

                Type listType = new TypeToken<List<ObjectData>>(){}.getType();
                List<ObjectData> posts = gson.fromJson(reader, listType);
                for (ObjectData objectData : posts)
                {
                    if (objectData.getId() == objectId)
                    {
                        lastFound = objectData.getName();
                        int[] modelIds = objectData.getObjectModels();

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

                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to await at findModelsForObject");
        }

        ModelStats[] stats = new ModelStats[modelStats.size()];
        for (int i = 0; i < modelStats.size(); i++)
        {
            stats[i] = modelStats.get(i);
        }

        return stats;
    }

    public ModelStats[] findModelsForGroundItem(int itemId, CustomModelType modelType)
    {
        ArrayList<ModelStats> modelStats = new ArrayList<>();

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
                List<ItemData> posts = gson.fromJson(reader, listType);

                for (ItemData itemData : posts)
                {
                    if (itemData.getId() == itemId)
                    {
                        lastFound = itemData.getName();
                        int[] modelIds = new int[0];

                        switch (modelType)
                        {
                            default:
                            case CACHE_GROUND_ITEM:
                                modelIds = ArrayUtils.add(modelIds, itemData.getInventoryModel());
                                break;
                            case CACHE_MAN_WEAR:
                                modelIds = ArrayUtils.addAll(modelIds, itemData.getMaleModel0(), itemData.getMaleModel1(), itemData.getMaleModel2());
                                break;
                            case CACHE_WOMAN_WEAR:
                                modelIds = ArrayUtils.addAll(modelIds, itemData.getFemaleModel0(), itemData.getFemaleModel1(), itemData.getFemaleModel2());
                        }

                        short[] rf = new short[0];
                        short[] rt = new short[0];

                        if (itemData.getColorReplace() != null)
                        {
                            int[] recolorToReplace = itemData.getColorReplace();
                            int[] recolorToFind = itemData.getColorFind();
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

                        if (itemData.getTextureReplace() != null)
                        {
                            int[] textureToReplace = itemData.getTextureReplace();
                            int[] retextureToFind = itemData.getTextureFind();
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
                                        itemData.getResizeX(),
                                        itemData.getResizeZ(),
                                        itemData.getResizeY(),
                                        0,
                                        customLighting
                                ));
                            }
                        }

                        break;
                    }
                }

                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to await at findModelsForGroundItem");
        }

        if (modelStats.isEmpty())
        {
            return null;
        }

        return new ModelStats[]{modelStats.get(0)};
    }
}
