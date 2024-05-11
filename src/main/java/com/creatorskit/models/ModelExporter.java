package com.creatorskit.models;

import com.creatorskit.CreatorsPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.client.RuneLite;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class ModelExporter
{
    private final Client client;
    private final CreatorsPlugin plugin;
    public final File BLENDER_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/blender-models");

    @Inject
    public ModelExporter(Client client, CreatorsPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
    }

    public void saveToFile(String name, BlenderModel blenderModel)
    {
        try {
            BLENDER_DIR.mkdirs();

            String path = BLENDER_DIR.getPath() + "\\\\" + name;
            String ext = ".json";
            int iteration = 0;
            File file = new File(path + ext);
            while (file.exists())
            {
                iteration++;
                file = new File(path + " (" + iteration + ")" + ext);
            }

            FileWriter writer = new FileWriter(file, false);
            String string = plugin.getGson().toJson(blenderModel);
            writer.write(string);
            writer.close();
        }
        catch (IOException e)
        {
            log.debug("Error when exporting model to file");
        }
    }

    public BlenderModel blenderModelFromCache(ModelStats[] modelStatsArray, int[] kitRecolours, boolean player, byte[] renderPriorities)
    {
        ModelData[] mds = new ModelData[modelStatsArray.length];

        for (int i = 0; i < modelStatsArray.length; i++)
        {
            ModelStats modelStats = modelStatsArray[i];
            ModelData modelData = client.loadModelData(modelStats.getModelId());

            if (modelData == null)
                continue;

            modelData.cloneColors().cloneVertices();

            for (short s = 0; s < modelStats.getRecolourFrom().length; s++)
                modelData.recolor(modelStats.getRecolourFrom()[s], modelStats.getRecolourTo()[s]);

            if (player)
                KitRecolourer.recolourKitModel(modelData, modelStats.getBodyPart(), kitRecolours);

            short[] textureFrom = modelStats.getTextureFrom();
            short[] textureTo = modelStats.getTextureTo();

            if (textureFrom == null || textureTo == null)
            {
                modelStats.setTextureFrom(new short[0]);
                modelStats.setTextureTo(new short[0]);
            }

            textureFrom = modelStats.getTextureFrom();
            textureTo = modelStats.getTextureTo();

            if (textureFrom.length > 0 && textureTo.length > 0)
            {
                for (int e = 0; e < textureFrom.length; e++)
                {
                    modelData.retexture(textureFrom[e], textureTo[e]);
                }
            }

            if (modelStats.getResizeX() == 0 && modelStats.getResizeY() == 0 && modelStats.getResizeZ() == 0)
            {
                modelStats.setResizeX(128);
                modelStats.setResizeY(128);
                modelStats.setResizeZ(128);
            }

            modelData.scale(modelStats.getResizeX(), modelStats.getResizeZ(), modelStats.getResizeY());

            mds[i] = modelData;
        }

        ModelData md = client.mergeModels(mds);

        int[][] verts = new int[md.getVerticesCount()][3];
        int[] vX = md.getVerticesX();
        int[] vY = md.getVerticesY();
        int[] vZ = md.getVerticesZ();

        for (int i = 0; i < verts.length; i++)
        {
            int[] v = verts[i];
            v[0] = vX[i];
            v[1] = vY[i];
            v[2] = vZ[i];
        }

        int[][] faces = new int[md.getFaceCount()][3];
        int[] fX = md.getFaceIndices1();
        int[] fY = md.getFaceIndices2();
        int[] fZ = md.getFaceIndices3();

        for (int i = 0; i < faces.length; i++)
        {
            int[] f = faces[i];
            f[0] = fX[i];
            f[1] = fY[i];
            f[2] = fZ[i];
        }

        double[][] colours = new double[md.getFaceCount()][3];
        short[] faceColours = md.getFaceColors();

        for (int i = 0; i < md.getFaceCount(); i++)
        {
            short col = faceColours[i];
            double h = (double) (63 - JagexColor.unpackHue(col)) / 63;
            double l = (double) JagexColor.unpackLuminance(col) / 127;
            double s = (double) JagexColor.unpackSaturation(col) / 7;
            double[] array = colours[i];
            array[0] = h;
            array[1] = l;
            array[2] = s;
        }

        byte[] transparencies = new byte[md.getFaceCount()];
        if (md.getFaceTransparencies() == null)
        {
            Arrays.fill(transparencies, (byte) 0);
        }
        else
        {
            transparencies = md.getFaceTransparencies();
        }

        if (renderPriorities == null)
        {
            renderPriorities = new byte[md.getFaceCount()];
            Arrays.fill(renderPriorities, (byte) 0);
        }

        return new BlenderModel(
                verts,
                faces,
                colours,
                transparencies,
                renderPriorities
        );
    }

    public BlenderModel blenderModelFromGame(ModelStats[] modelStatsArray, int[] kitRecolours, boolean player, byte[] renderPriorities, Model model)
    {
        Model copy = model.getModel();
        ModelData[] mds = new ModelData[modelStatsArray.length];

        for (int i = 0; i < modelStatsArray.length; i++)
        {
            ModelStats modelStats = modelStatsArray[i];
            ModelData modelData = client.loadModelData(modelStats.getModelId());

            if (modelData == null)
                continue;

            modelData.cloneColors().cloneVertices();

            for (short s = 0; s < modelStats.getRecolourFrom().length; s++)
                modelData.recolor(modelStats.getRecolourFrom()[s], modelStats.getRecolourTo()[s]);

            if (player)
                KitRecolourer.recolourKitModel(modelData, modelStats.getBodyPart(), kitRecolours);

            short[] textureFrom = modelStats.getTextureFrom();
            short[] textureTo = modelStats.getTextureTo();

            if (textureFrom == null || textureTo == null)
            {
                modelStats.setTextureFrom(new short[0]);
                modelStats.setTextureTo(new short[0]);
            }

            textureFrom = modelStats.getTextureFrom();
            textureTo = modelStats.getTextureTo();

            if (textureFrom.length > 0 && textureTo.length > 0)
            {
                for (int e = 0; e < textureFrom.length; e++)
                {
                    modelData.retexture(textureFrom[e], textureTo[e]);
                }
            }

            if (modelStats.getResizeX() == 0 && modelStats.getResizeY() == 0 && modelStats.getResizeZ() == 0)
            {
                modelStats.setResizeX(128);
                modelStats.setResizeY(128);
                modelStats.setResizeZ(128);
            }

            modelData.scale(modelStats.getResizeX(), modelStats.getResizeZ(), modelStats.getResizeY());

            mds[i] = modelData;
        }

        ModelData md = client.mergeModels(mds);

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

        int[][] faces = new int[model.getFaceCount()][3];
        int[] fX = model.getFaceIndices1();
        int[] fY = model.getFaceIndices2();
        int[] fZ = model.getFaceIndices3();

        for (int i = 0; i < faces.length; i++)
        {
            int[] f = faces[i];
            f[0] = fX[i];
            f[1] = fY[i];
            f[2] = fZ[i];
        }

        double[][] colours = new double[md.getFaceCount()][3];
        short[] faceColours = md.getFaceColors();

        for (int i = 0; i < md.getFaceCount(); i++)
        {
            short col = faceColours[i];
            double h = (double) (63 - JagexColor.unpackHue(col)) / 63;
            double l = (double) JagexColor.unpackLuminance(col) / 127;
            double s = (double) JagexColor.unpackSaturation(col) / 7;
            double[] array = colours[i];
            array[0] = h;
            array[1] = l;
            array[2] = s;
        }

        byte[] transparencies = new byte[md.getFaceCount()];
        if (md.getFaceTransparencies() == null)
        {
            Arrays.fill(transparencies, (byte) 0);
        }
        else
        {
            transparencies = md.getFaceTransparencies();
        }

        if (renderPriorities == null)
        {
            renderPriorities = new byte[model.getFaceCount()];
            Arrays.fill(renderPriorities, (byte) 0);
        }

        return new BlenderModel(
                verts,
                faces,
                colours,
                transparencies,
                renderPriorities
        );
    }
}
