package com.creatorskit.models;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.IterableHashTable;
import net.runelite.api.ModelData;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ModelFinder
{
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
    private static final Pattern recolFrom = Pattern.compile("recol\\ds=.+");
    private static final Pattern recolTo = Pattern.compile("recol\\dd=.+");
    private static final Pattern retexFrom = Pattern.compile("retex\\ds=.+");
    private static final Pattern retexTo = Pattern.compile("retex\\dd=.+");
    private final Request objRequest = new Request.Builder().url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.obj").build();
    private final Request spotAnimRequest = new Request.Builder().url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.spotanim?ref_type=heads").build();
    private final Request npcRequest = new Request.Builder().url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.npc").build();
    private final Request locRequest = new Request.Builder().url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.loc").build();
    private final Request seqRequest = new Request.Builder().url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.seq?ref_type=heads").build();

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

            Call call = httpClient.newCall(seqRequest);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.seq?ref_type=heads");
                    countDownLatch1.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException
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

        Call itemCall = httpClient.newCall(objRequest);
        itemCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.obj");
                countDownLatch2.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
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
                .url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.idk")
                .build();
        Call kitCall = httpClient.newCall(kitRequest);
        kitCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.idk");
                countDownLatch2.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
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
            Call call = httpClient.newCall(spotAnimRequest);
            call.enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.spotanim?ref_type=heads");
                    countDownLatch2.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException
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
        InputStream inputStream = response.body().byteStream();
        Scanner scanner = new Scanner(inputStream);
        Pattern seqPattern = Pattern.compile("\\[.+_" + animId + "]");

        while (scanner.hasNextLine())
        {
            String string = scanner.nextLine();
            Matcher match = seqPattern.matcher(string);
            if (match.matches())
            {
                while (!string.isEmpty())
                {
                    string = scanner.nextLine();
                    if (string.startsWith("mainhand"))
                    {
                        String[] split = string.split("=");
                        if (split[1].equals("hide"))
                        {
                            animSequence.setMainHandData(AnimSequenceData.HIDE);
                        }
                        else
                        {
                            animSequence.setMainHandData(AnimSequenceData.SWAP);
                            String[] modelSplit = split[1].split("_");
                            animSequence.setMainHandItemId(Integer.parseInt(modelSplit[modelSplit.length - 1]));
                        }
                    }
                    else if (string.startsWith("offhand"))
                    {
                        String[] split = string.split("=");
                        if (split[1].equals("hide"))
                        {
                            animSequence.setOffHandData(AnimSequenceData.HIDE);
                        }
                        else
                        {
                            animSequence.setOffHandData(AnimSequenceData.SWAP);
                            String[] modelSplit = split[1].split("_");
                            animSequence.setOffHandItemId(Integer.parseInt(modelSplit[modelSplit.length - 1]));
                        }
                    }
                }
                return;
            }
        }
    }

    public static void getPlayerItems(Response response, ArrayList<ModelStats> modelStatsArray, boolean groundItem, boolean maleItem, int[] itemId, AnimSequence animSequence)
    {
        InputStream inputStream = response.body().byteStream();
        Scanner scanner = new Scanner(inputStream);
        Pattern[] patterns = new Pattern[itemId.length];

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

        for (int i = 0; i < updatedItemIds.length; i++)
        {
            int item = updatedItemIds[i];
            if (item == -1)
            {
                continue;
            }

            Pattern itemPattern = Pattern.compile("\\[.+_" + item + "]");
            patterns[i] = itemPattern;
        }

        while (scanner.hasNextLine())
        {
            String string = scanner.nextLine();
            for (int i = 0; i < patterns.length; i++)
            {
                if (updatedItemIds[i] == -1)
                {
                    continue;
                }

                Pattern pattern = patterns[i];
                Matcher matcher = pattern.matcher(string);
                if (matcher.matches())
                {
                    int[] modelIds = new int[3];
                    ArrayList<Integer> recolourFrom = new ArrayList<>();
                    ArrayList<Integer> recolourTo = new ArrayList<>();
                    short[] retextureFrom = new short[0];
                    short[] retextureTo = new short[0];

                    while (!string.isEmpty())
                    {
                        string = scanner.nextLine();
                        if (groundItem && string.startsWith("model="))
                        {
                            String[] split = string.split("_");
                            modelIds[0] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (maleItem && string.startsWith("manwear="))
                        {
                            String replaced = string.replace(",", "_");
                            String[] split = replaced.split("_");
                            modelIds[0] = Integer.parseInt(split[split.length - 2]);
                        }
                        else if (maleItem && string.startsWith("manwear2="))
                        {
                            String[] split = string.split("_");
                            modelIds[1] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (maleItem && string.startsWith("manwear3="))
                        {
                            String[] split = string.split("_");
                            modelIds[2] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (!maleItem && string.startsWith("womanwear="))
                        {
                            String replaced = string.replace(",", "_");
                            String[] split = replaced.split("_");
                            modelIds[0] = Integer.parseInt(split[split.length - 2]);
                        }
                        else if (!maleItem && string.startsWith("womanwear2="))
                        {
                            String[] split = string.split("_");
                            modelIds[1] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (!maleItem && string.startsWith("womanwear3="))
                        {
                            String[] split = string.split("_");
                            modelIds[2] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (string.startsWith("recol"))
                        {
                            matcher = recolFrom.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                recolourFrom.add(Integer.parseInt(split[1]));
                                continue;
                            }

                            matcher = recolTo.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                recolourTo.add(Integer.parseInt(split[1]));
                            }
                        }
                        else if (string.startsWith("retex"))
                        {
                            matcher = retexFrom.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("_");
                                retextureFrom = ArrayUtils.add(retextureFrom, Short.parseShort(split[1]));
                                continue;
                            }

                            matcher = retexTo.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("_");
                                retextureTo = ArrayUtils.add(retextureTo, Short.parseShort(split[1]));
                            }
                        }
                    }

                    int size = recolourFrom.size();
                    short[] recolourFromArray = new short[size];
                    short[] recolourToArray = new short[size];
                    for (int e = 0; e < size; e++)
                    {
                        int from = recolourFrom.get(e);
                        if (from > 32767)
                            from -= 65536;
                        recolourFromArray[e] = (short) from;

                        int to = recolourTo.get(e);
                        if (to > 32767)
                            to -= 65536;
                        recolourToArray[e] = (short) to;
                    }

                    for (int id : modelIds)
                    {
                        if (id > 0)
                        {
                            modelStatsArray.add(new ModelStats(
                                    id,
                                    bodyParts[i],
                                    recolourFromArray,
                                    recolourToArray,
                                    retextureFrom,
                                    retextureTo,
                                    128,
                                    128,
                                    128,
                                    new CustomLighting(64, 850, -30, -50, -30)));
                        }
                    }

                    if (i == patterns.length - 1)
                    {
                        break;
                    }
                }
            }
        }
    }

    public static void getPlayerKit(Response response, ArrayList<ModelStats> modelStatsArray, int[] kitId)
    {
        InputStream inputStream = response.body().byteStream();
        Scanner scanner = new Scanner(inputStream);
        Pattern[] patterns = new Pattern[kitId.length];

        for (int i = 0; i < kitId.length; i++)
        {
            //Create a pattern to match the format [idk_XXX] per kitId
            int item = kitId[i];
            if (item != -1)
            {
                Pattern kitPattern = Pattern.compile("\\[.+_" + item + "]");
                patterns[i] = kitPattern;
            }
        }

        while (scanner.hasNextLine())
        {
            String string = scanner.nextLine();
            for (int i = 0; i < patterns.length; i++)
            {
                if (kitId[i] == -1)
                {
                    continue;
                }

                Pattern pattern = patterns[i];
                Matcher matcher = pattern.matcher(string);
                if (matcher.matches())
                {
                    int[] modelIds = new int[2];
                    ArrayList<Integer> recolourFrom = new ArrayList<>();
                    ArrayList<Integer> recolourTo = new ArrayList<>();

                    while (!string.isEmpty())
                    {
                        string = scanner.nextLine();
                        if (string.startsWith("model1="))
                        {
                            String[] split = string.split("_");
                            modelIds[0] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (string.startsWith("model2="))
                        {
                            String[] split = string.split("_");
                            modelIds[1] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (string.startsWith("recol"))
                        {
                            //recolour if a recol1s and recol1d are present with the kitId
                            matcher = recolFrom.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                recolourFrom.add(Integer.parseInt(split[1]));
                                continue;
                            }

                            matcher = recolTo.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                recolourTo.add(Integer.parseInt(split[1]));
                            }
                        }
                    }

                    int size = recolourFrom.size();
                    short[] recolourFromArray = new short[size];
                    short[] recolourToArray = new short[size];
                    for (int e = 0; e < size; e++)
                    {
                        int from = recolourFrom.get(e);
                        if (from > 32767)
                            from -= 65536;
                        recolourFromArray[e] = (short) from;

                        int to = recolourTo.get(e);
                        if (to > 32767)
                            to -= 65536;
                        recolourToArray[e] = (short) to;
                    }

                    for (int id : modelIds)
                    {
                        if (id > 0)
                        {
                            modelStatsArray.add(new ModelStats(
                                    id,
                                    bodyParts[i],
                                    recolourFromArray,
                                    recolourToArray,
                                    new short[0],
                                    new short[0],
                                    128,
                                    128,
                                    128,
                                    new CustomLighting(64, 850, -30, -50, -30)));
                        }
                    }

                    if (i == patterns.length - 1)
                    {
                        break;
                    }
                }
            }
        }
    }

    public static void getPlayerSpotAnims(Response response, int[] spotAnims, ArrayList<ModelStats> spotAnimList)
    {
        InputStream inputStream = response.body().byteStream();
        Scanner scanner = new Scanner(inputStream);
        Pattern[] patterns = new Pattern[spotAnims.length];

        for (int i = 0; i < spotAnims.length; i++)
        {
            //Create a pattern to match the format [idk_XXX] per kitId
            int spotAnimId = spotAnims[i];
            Pattern kitPattern = Pattern.compile("\\[.+_" + spotAnimId + "]");
            patterns[i] = kitPattern;
        }

        while (scanner.hasNextLine())
        {
            String string = scanner.nextLine();
            for (int i = 0; i < patterns.length; i++)
            {
                Pattern pattern = patterns[i];
                Matcher matcher = pattern.matcher(string);
                if (matcher.matches())
                {
                    int modelId = 0;
                    CustomLighting lighting = new CustomLighting(64, 850, -50, -50, 75);
                    int[] resize = new int[]{128, 128, 128};
                    ArrayList<Integer> recolourFrom = new ArrayList<>();
                    ArrayList<Integer> recolourTo = new ArrayList<>();

                    while (!string.isEmpty())
                    {
                        string = scanner.nextLine();
                        if (string.startsWith("model"))
                        {
                            String[] split = string.split("_");
                            modelId = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (string.startsWith("amb"))
                        {
                            String[] split = string.split("=");
                            int ambient = Integer.parseInt(split[split.length - 1]);
                            if (ambient >= 128)
                                ambient -= 256;

                            lighting.setAmbient(64 + ambient);
                        }
                        else if (string.startsWith("con"))
                        {
                            String[] split = string.split("=");
                            int contrast = Integer.parseInt(split[split.length - 1]);
                            if (contrast >= 128)
                                contrast -= 128;

                            lighting.setContrast(768 + contrast);
                        }
                        else if (string.startsWith("resizeh"))
                        {
                            String[] split = string.split("=");
                            int resizeH = Integer.parseInt(split[split.length - 1]);
                            resize[0] = resizeH;
                            resize[1] = resizeH;
                        }
                        else if (string.startsWith("resizev"))
                        {
                            String[] split = string.split("=");
                            resize[2] = Integer.parseInt(split[split.length - 1]);
                        }
                        else
                        {
                            matcher = recolFrom.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                int e = Integer.parseInt(split[1]);
                                recolourFrom.add(e);
                            }

                            matcher = recolTo.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                int e = Integer.parseInt(split[1]);
                                recolourTo.add(e);
                            }
                        }
                    }

                    int size = recolourFrom.size();
                    short[] recolourFromArray = new short[size];
                    short[] recolourToArray = new short[size];
                    for (int e = 0; e < size; e++)
                    {
                        int from = recolourFrom.get(e);
                        if (from > 32767)
                            from -= 65536;
                        recolourFromArray[e] = (short) from;

                        int to = recolourTo.get(e);
                        if (to > 32767)
                            to -= 65536;
                        recolourToArray[e] = (short) to;
                    }

                    spotAnimList.add(new ModelStats(
                            modelId,
                            BodyPart.SPOTANIM,
                            recolourFromArray,
                            recolourToArray,
                            new short[0],
                            new short[0],
                            resize[0],
                            resize[1],
                            resize[2],
                            lighting));

                    if (i == patterns.length - 1)
                    {
                        break;
                    }
                }
            }
        }
    }

    public ModelStats[] findSpotAnim(int spotAnimId)
    {
        ArrayList<Integer> modelIds = new ArrayList<>();
        final int[] resize = new int[]{128, 128, 128};
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();
        CustomLighting lighting = new CustomLighting(64, 850, -50, -50, 75);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Call call = httpClient.newCall(spotAnimRequest);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.spotanim?ref_type=heads");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream);
                Pattern npcPattern = Pattern.compile("\\[.+_" + spotAnimId + "]");

                while (scanner.hasNextLine())
                {
                    String string = scanner.nextLine();
                    Matcher match = npcPattern.matcher(string);
                    if (match.matches())
                    {
                        lastFound = "SpotAnim " + spotAnimId;
                        lastAnim = -1;
                        while (!string.isEmpty())
                        {
                            string = scanner.nextLine();
                            if (string.startsWith("model"))
                            {
                                String[] split = string.split("_");
                                modelIds.add(Integer.parseInt(split[split.length - 1]));
                            }
                            else if (string.startsWith("anim"))
                            {
                                String[] split = string.split("_");
                                lastAnim = Integer.parseInt(split[split.length - 1]);
                            }
                            else if (string.startsWith("amb"))
                            {
                                String[] split = string.split("=");
                                int ambient = Integer.parseInt(split[split.length - 1]);
                                if (ambient >= 128)
                                    ambient -= 256;

                                lighting.setAmbient(64 + ambient);
                            }
                            else if (string.startsWith("con"))
                            {
                                String[] split = string.split("=");
                                int contrast = Integer.parseInt(split[split.length - 1]);
                                if (contrast >= 128)
                                    contrast -= 128;

                                lighting.setContrast(768 + contrast);
                            }
                            else if (string.startsWith("resizeh"))
                            {
                                String[] split = string.split("=");
                                int resizeH = Integer.parseInt(split[split.length - 1]);
                                resize[0] = resizeH;
                                resize[1] = resizeH;
                            }
                            else if (string.startsWith("resizev"))
                            {
                                String[] split = string.split("=");
                                resize[2] = Integer.parseInt(split[split.length - 1]);
                            }
                            else
                            {
                                match = recolFrom.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourFrom.add((short) i);
                                }

                                match = recolTo.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourTo.add((short) i);
                                }
                            }
                        }
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
            log.debug("CountDownLatch failed to await at findModelsForNPCs");
        }

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        return new ModelStats[]{new ModelStats(
                modelIds.get(0),
                BodyPart.SPOTANIM,
                rf,
                rt,
                new short[0],
                new short[0],
                resize[0],
                resize[1],
                resize[2],
                lighting)};
    }

    public ModelStats[] findModelsForNPC(int npcId)
    {
        ArrayList<Integer> modelIds = new ArrayList<>();
        final int[] resize = new int[]{128, 128, 128};
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();
        short[] retextureFrom = new short[0];
        short[] retextureTo = new short[0];

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Call call = httpClient.newCall(npcRequest);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.npc");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream);
                Pattern npcPattern = Pattern.compile("\\[.+_" + npcId + "]");

                while (scanner.hasNextLine())
                {
                    String string = scanner.nextLine();
                    Matcher match = npcPattern.matcher(string);
                    if (match.matches())
                    {
                        lastFound = string;
                        while (!string.isEmpty())
                        {
                            string = scanner.nextLine();

                            if (string.startsWith("name="))
                            {
                                lastFound = string.replaceAll("name=", "");
                            }
                            else if (string.startsWith("model"))
                            {
                                String[] split = string.split("_");
                                modelIds.add(Integer.parseInt(split[split.length - 1]));
                            }
                            else if (string.startsWith("recol"))
                            {
                                match = recolFrom.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourFrom.add((short) i);
                                }

                                match = recolTo.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourTo.add((short) i);
                                }
                            }
                            else if (string.startsWith("resizeh"))
                            {
                                String[] split = string.split("=");
                                int resizeH = Integer.parseInt(split[1]);
                                resize[0] = resizeH;
                                resize[2] = resizeH;
                            }
                            else if (string.startsWith("resizev"))
                            {
                                String[] split = string.split("=");
                                resize[1] = Integer.parseInt(split[1]);
                            }
                        }
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
            log.debug("CountDownLatch failed to await at findModelsForNPCs");
        }

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        //Currently the only npc in dump.npc that has a retexture
        //Less costly (unless more retextured npcs are added) to manually enter that npc in rather than search for retex on every npc
        if (npcId == 2702)
        {
            retextureFrom = ArrayUtils.add(retextureFrom, (short) 2);
            retextureTo = ArrayUtils.add(retextureTo, (short) 0);
        }

        ModelStats[] modelStats = new ModelStats[modelIds.size()];
        for (int i = 0; i < modelIds.size(); i++)
            modelStats[i] = new ModelStats(
                    modelIds.get(i),
                    BodyPart.NA,
                    rf,
                    rt,
                    retextureFrom,
                    retextureTo,
                    resize[0],
                    resize[1],
                    resize[2],
                    new CustomLighting(64, 850, -30, -50, -30));

        return modelStats;
    }

    public ModelStats[] findModelsForObject(int objectId, int modelType, LightingStyle ls)
    {
        ArrayList<Integer> modelIds = new ArrayList<>();
        final int[] resize = new int[]{128, 128, 128};
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();
        ArrayList<Short> retextureFrom = new ArrayList<>();
        ArrayList<Short> retextureTo = new ArrayList<>();
        CustomLighting lighting = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        final char cacheValueToFind = ModelType.findCacheValue(modelType);

        Call call = httpClient.newCall(locRequest);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.loc");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream);
                Pattern npcPattern = Pattern.compile("\\[.+_" + objectId + "]");

                boolean cacheValueFound = false;

                //In the case where Cache Searcher is used and the objectId has multiple ModelType variants, get the first variant only
                boolean getFirstModelType = false;
                if (cacheValueToFind == 'o')
                {
                    getFirstModelType = true;
                }

                while (scanner.hasNextLine())
                {
                    String string = scanner.nextLine();
                    Matcher matcher = npcPattern.matcher(string);
                    if (matcher.matches())
                    {
                        lastFound = string;
                        while (!string.isEmpty())
                        {
                            string = scanner.nextLine();
                            if (string.startsWith("name"))
                            {
                                lastFound = string.replaceAll("name=", "");
                            }
                            else if (!cacheValueFound && string.startsWith("model"))
                            {
                                String[] split = string.split("_");
                                if (split[split.length - 1].contains(","))
                                {
                                    String split2 = split[split.length - 1];
                                    String[] splitCacheValue = split2.split(",");
                                    char cacheValue = splitCacheValue[1].charAt(0);
                                    if (cacheValueToFind == cacheValue)
                                    {
                                        modelIds.add(Integer.parseInt(splitCacheValue[0]));
                                        cacheValueFound = true;
                                    }

                                    if (getFirstModelType)
                                    {
                                        modelIds.add(Integer.parseInt(splitCacheValue[0]));
                                        cacheValueFound = true;
                                    }
                                }
                                else
                                {
                                    modelIds.add(Integer.parseInt(split[split.length - 1]));
                                }
                            }
                            else if (string.startsWith("amb"))
                            {
                                String[] split = string.split("=");
                                int ambient = Integer.parseInt(split[split.length - 1]);
                                if (ambient >= 128)
                                    ambient -= 256;

                                lighting.setAmbient(LightingStyle.DEFAULT.getAmbient() + ambient);
                            }
                            else if (string.startsWith("con"))
                            {
                                String[] split = string.split("=");
                                int contrast = Integer.parseInt(split[split.length - 1]);
                                if (contrast >= 128)
                                    contrast -= 128;

                                lighting.setContrast(LightingStyle.DEFAULT.getContrast() + contrast);
                            }
                            else if (string.startsWith("recol"))
                            {
                                matcher = recolFrom.matcher(string);

                                if (matcher.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourFrom.add((short) i);
                                }

                                matcher = recolTo.matcher(string);

                                if (matcher.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourTo.add((short) i);
                                }
                            }
                            else if (string.startsWith("retex"))
                            {
                                matcher = retexFrom.matcher(string);

                                if (matcher.matches())
                                {
                                    String[] split = string.split("_");
                                    retextureFrom.add(Short.parseShort(split[1]));
                                    continue;
                                }

                                matcher = retexTo.matcher(string);

                                if (matcher.matches())
                                {
                                    String[] split = string.split("_");
                                    retextureTo.add(Short.parseShort(split[1]));
                                }
                            }
                            else if (string.startsWith("resizex"))
                            {
                                String[] split = string.split("=");
                                resize[0] = Integer.parseInt(split[1]);
                            }
                            else if (string.startsWith("resizey"))
                            {
                                String[] split = string.split("=");
                                resize[2] = Integer.parseInt(split[1]);
                            }
                            else if (string.startsWith("resizez"))
                            {
                                String[] split = string.split("=");
                                resize[1] = Integer.parseInt(split[1]);
                            }
                        }
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

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        short[] rtFrom = new short[retextureFrom.size()];
        short[] rtTo = new short[retextureTo.size()];
        for (int i = 0; i < retextureFrom.size(); i++)
        {
            rtFrom[i] = retextureFrom.get(i);
            rtTo[i] = retextureTo.get(i);
        }

        ModelStats[] modelStats = new ModelStats[modelIds.size()];
        for (int i = 0; i < modelIds.size(); i++)
            modelStats[i] = new ModelStats(
                    modelIds.get(i),
                    BodyPart.NA,
                    rf,
                    rt,
                    rtFrom,
                    rtTo,
                    resize[0],
                    resize[1],
                    resize[2],
                    lighting);

        return modelStats;
    }

    public ModelStats[] findModelsForGroundItem(int itemId, CustomModelType modelType)
    {
        ArrayList<Integer> modelIds = new ArrayList<>();
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();
        ArrayList<Short> retextureFrom = new ArrayList<>();
        ArrayList<Short> retextureTo = new ArrayList<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Call call = httpClient.newCall(objRequest);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.obj");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream);
                Pattern npcPattern = Pattern.compile("\\[.+_" + itemId + "]");

                while (scanner.hasNextLine())
                {
                    String string = scanner.nextLine();
                    Matcher matcher = npcPattern.matcher(string);
                    if (matcher.matches())
                    {
                        lastFound = string;
                        while (!string.isEmpty())
                        {
                            string = scanner.nextLine();
                            String searchName;
                            switch (modelType)
                            {
                                default:
                                case CACHE_GROUND_ITEM:
                                    searchName = "model";
                                    break;
                                case CACHE_MAN_WEAR:
                                    searchName = "manwear";
                                    break;
                                case CACHE_WOMAN_WEAR:
                                    searchName = "womanwear";
                            }

                            if (string.startsWith("name="))
                            {
                                lastFound = string.replaceAll("name=", "");
                            }
                            else if (string.startsWith(searchName))
                            {
                                String[] split = string.split("_");
                                if (split[split.length - 1].contains(","))
                                {
                                    String split2 = split[split.length - 1].split(",")[0];
                                    modelIds.add(Integer.parseInt(split2));
                                }
                                else
                                {
                                    modelIds.add(Integer.parseInt(split[split.length - 1]));
                                }
                            }
                            else if (string.startsWith("recol"))
                            {
                                matcher = recolFrom.matcher(string);

                                if (matcher.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourFrom.add((short) i);
                                }

                                matcher = recolTo.matcher(string);

                                if (matcher.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourTo.add((short) i);
                                }
                            }
                            else if (string.startsWith("retex"))
                            {
                                matcher = retexFrom.matcher(string);

                                if (matcher.matches())
                                {
                                    String[] split = string.split("_");
                                    retextureFrom.add(Short.parseShort(split[1]));
                                    continue;
                                }

                                matcher = retexTo.matcher(string);

                                if (matcher.matches())
                                {
                                    String[] split = string.split("_");
                                    retextureTo.add(Short.parseShort(split[1]));
                                }
                            }
                        }
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

        if (modelIds.isEmpty())
            return null;

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        short[] rtFrom = new short[retextureFrom.size()];
        short[] rtTo = new short[retextureTo.size()];
        for (int i = 0; i < retextureFrom.size(); i++)
        {
            rtFrom[i] = retextureFrom.get(i);
            rtTo[i] = retextureTo.get(i);
        }

        ModelStats[] modelStats = new ModelStats[modelIds.size()];
        for (int i = 0; i < modelIds.size(); i++)
            modelStats[i] = new ModelStats(
                    modelIds.get(i),
                    BodyPart.NA,
                    rf,
                    rt,
                    rtFrom,
                    rtTo,
                    128,
                    128,
                    128,
                    new CustomLighting(ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z));

        return modelStats;
    }
}
