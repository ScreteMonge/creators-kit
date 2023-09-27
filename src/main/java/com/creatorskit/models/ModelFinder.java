package com.creatorskit.models;

import net.runelite.client.util.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelFinder
{
    public static ModelStats[] findModelsForPlayer(boolean groundItem, boolean maleItem, int[] items)
    {
        //Convert equipmentId to itemId or kitId as appropriate
        int[] ids = new int[items.length];
        boolean baldHead = false;
        for (int i = 0; i < ids.length; i++)
        {
            int item = items[i];

            if (item == 256)
                baldHead = true;

            if (item >= 256 && item <= 512)
            {
                ids[i] = item - 256;
                continue;
            }

            if (item > 512)
            {
                ids[i] = item - 512;
            }
        }

        Pattern recolFrom = Pattern.compile("recol\\ds=.+");
        Pattern recolTo = Pattern.compile("recol\\dd=.+");
        ArrayList<Integer> itemList = new ArrayList<>();
        ArrayList<Integer> kitList = new ArrayList<>();

        for (int i : ids)
        {
            if (i == 0)
                continue;

            if (i < 256)
            {
                kitList.add(i);
                continue;
            }

            itemList.add(i);
        }

        if (baldHead)
        {
            kitList.add(0);
        }

        ArrayList<ModelStats> modelStatsItems = new ArrayList<>();

        //for modelIds
        int[] itemId = new int[itemList.size()];
        for (int i = 0; i < itemList.size(); i++)
        {
            itemId[i] = itemList.get(i);
        }

        try
        {
            URL url = new URL("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.obj");
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String s = "";
            Pattern[] patterns = new Pattern[itemId.length];

            for (int i = 0; i < itemId.length; i++)
            {
                int item = itemId[i];
                Pattern itemPattern = Pattern.compile("\\[.+_" + item + "]");
                patterns[i] = itemPattern;
            }

            while ((s = br.readLine()) != null)
            {
                for (int i = 0; i < patterns.length; i++)
                {
                    Pattern pattern = patterns[i];
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.matches())
                    {
                        int[] modelIds = new int[3];
                        ArrayList<Integer> recolourFrom = new ArrayList<>();
                        ArrayList<Integer> recolourTo = new ArrayList<>();


                        String string = "";
                        while (!(string = br.readLine()).equals(""))
                        {
                            if (groundItem && string.startsWith("model="))
                            {
                                String[] split = string.split("_");
                                modelIds[0] = Integer.parseInt(split[split.length - 1]);
                            }
                            else
                            {
                                if (maleItem && string.startsWith("manwear="))
                                {
                                    String replaced = string.replace(",", "_");
                                    String[] split = replaced.split("_");
                                    modelIds[0] = Integer.parseInt(split[split.length - 2]);
                                    continue;
                                }

                                if (maleItem && string.startsWith("manwear2="))
                                {
                                    String[] split = string.split("_");
                                    modelIds[1] = Integer.parseInt(split[split.length - 1]);
                                    continue;
                                }

                                if (maleItem && string.startsWith("manwear3="))
                                {
                                    String[] split = string.split("_");
                                    modelIds[2] = Integer.parseInt(split[split.length - 1]);
                                    continue;
                                }

                                if (!maleItem && string.startsWith("womanwear="))
                                {
                                    String replaced = string.replace(",", "_");
                                    String[] split = replaced.split("_");
                                    modelIds[0] = Integer.parseInt(split[split.length - 2]);
                                    continue;
                                }

                                if (!maleItem && string.startsWith("womanwear2="))
                                {
                                    String[] split = string.split("_");
                                    modelIds[1] = Integer.parseInt(split[split.length - 1]);
                                    continue;
                                }

                                if (!maleItem && string.startsWith("womanwear3="))
                                {
                                    String[] split = string.split("_");
                                    modelIds[2] = Integer.parseInt(split[split.length - 1]);
                                    continue;
                                }
                            }

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
                                modelStatsItems.add(new ModelStats(id, BodyPart.NA, recolourFromArray, recolourToArray));
                            }
                        }

                        if (i == patterns.length - 1)
                        {
                            break;
                        }
                    }
                }
            }
            br.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }


        //for KitIds
        int[] kitId = new int[kitList.size()];
        for (int i = 0; i < kitList.size(); i++)
        {
            kitId[i] = kitList.get(i);
        }

        try
        {
            URL url = new URL("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.idk");
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String s = "";
            Pattern[] patterns = new Pattern[kitId.length];

            for (int i = 0; i < kitId.length; i++)
            {
                //Create a pattern to match the format [idk_XXX] per kitId
                int item = kitId[i];
                Pattern kitPattern = Pattern.compile("\\[.+_" + item + "]");
                patterns[i] = kitPattern;
            }

            while ((s = br.readLine()) != null)
            {
                for (int i = 0; i < patterns.length; i++)
                {
                    Pattern pattern = patterns[i];
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.matches())
                    {
                        int[] modelIds = new int[2];
                        ArrayList<Integer> recolourFrom = new ArrayList<>();
                        ArrayList<Integer> recolourTo = new ArrayList<>();
                        BodyPart bodyPart = BodyPart.NA;


                        String string = "";
                        while (!(string = br.readLine()).equals(""))
                        {
                            if (string.startsWith("model1="))
                            {
                                String[] split = string.split("_");
                                modelIds[0] = Integer.parseInt(split[split.length - 1]);
                                continue;
                            }

                            if (string.startsWith("model2="))
                            {
                                String[] split = string.split("_");
                                modelIds[1] = Integer.parseInt(split[split.length - 1]);
                                continue;
                            }

                            if (string.endsWith("hair"))
                                bodyPart = BodyPart.HAIR;

                            if (string.endsWith("jaw"))
                                bodyPart = BodyPart.JAW;

                            if (string.endsWith("torso"))
                                bodyPart = BodyPart.TORSO;

                            if (string.endsWith("arms"))
                                bodyPart = BodyPart.ARMS;

                            if (string.endsWith("hands"))
                                bodyPart = BodyPart.HANDS;

                            if (string.endsWith("legs"))
                                bodyPart = BodyPart.LEGS;

                            if (string.endsWith("feet"))
                                bodyPart = BodyPart.FEET;

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
                                modelStatsItems.add(new ModelStats(id, bodyPart, recolourFromArray, recolourToArray));
                            }
                        }

                        if (i == patterns.length - 1)
                        {
                            break;
                        }
                    }
                }
            }
            br.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return modelStatsItems.toArray(new ModelStats[0]);
    }

    public static ModelStats[] findModelsForNPC(int npcId)
    {
        Pattern recolFrom = Pattern.compile("recol\\ds=.+");
        Pattern recolTo = Pattern.compile("recol\\dd=.+");

        ArrayList<Integer> modelIds = new ArrayList<>();
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();

        try
        {
            URL url = new URL("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.npc");
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String s = "";

            Pattern npcPattern = Pattern.compile("\\[.+_" + npcId + "]");

            while ((s = br.readLine()) != null)
            {
                Matcher match = npcPattern.matcher(s);
                if (match.matches())
                {
                    String string = "";
                    while (!(string = br.readLine()).equals(""))
                    {
                        if (string.startsWith("model"))
                        {
                            String[] split = string.split("_");
                            modelIds.add(Integer.parseInt(split[split.length - 1]));
                        }

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
            br.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        ModelStats[] modelStats = new ModelStats[modelIds.size()];
        for (int i = 0; i < modelIds.size(); i++)
            modelStats[i] = new ModelStats(modelIds.get(i), BodyPart.NA, rf, rt);

        return modelStats;
    }

    public static String findNameForNPC(int npcId)
    {
        try
        {
            URL url = new URL("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.npc");
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String s = "";

            Pattern npcPattern = Pattern.compile("\\[.+_" + npcId + "]");

            while ((s = br.readLine()) != null)
            {
                Matcher match = npcPattern.matcher(s);
                if (match.matches())
                {
                    String name = br.readLine();
                    return Text.removeTags(name.replaceAll("name=", ""));
                }
            }
            br.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return "Null";
    }

    public static ModelStats[] findModelsForObject(int objectId)
    {
        Pattern recolFrom = Pattern.compile("recol\\ds=.+");
        Pattern recolTo = Pattern.compile("recol\\dd=.+");

        ArrayList<Integer> modelIds = new ArrayList<>();
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();

        try
        {
            URL url = new URL("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.loc");
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String s = "";

            Pattern npcPattern = Pattern.compile("\\[.+_" + objectId + "]");

            while ((s = br.readLine()) != null)
            {
                Matcher match = npcPattern.matcher(s);
                if (match.matches())
                {
                    String string = "";
                    while (!(string = br.readLine()).equals(""))
                    {
                        if (string.startsWith("model"))
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
            br.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        ModelStats[] modelStats = new ModelStats[modelIds.size()];
        for (int i = 0; i < modelIds.size(); i++)
            modelStats[i] = new ModelStats(modelIds.get(i), BodyPart.NA, rf, rt);

        return modelStats;
    }

    public static String findNameForObject(int objectId)
    {
        try
        {
            URL url = new URL("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.loc");
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String s = "";

            Pattern npcPattern = Pattern.compile("\\[.+_" + objectId + "]");

            while ((s = br.readLine()) != null)
            {
                Matcher match = npcPattern.matcher(s);
                if (match.matches())
                {
                    String name = br.readLine();
                    return Text.removeTags(name.replaceAll("name=", ""));
                }
            }
            br.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return "Null";
    }

    public static String shortArrayToString(short[] array)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < array.length; i++)
        {
            short s = array[i];
            stringBuilder.append(s);
            if (i < array.length - 1)
                stringBuilder.append(",");
        }

        return stringBuilder.toString();
    }
}
