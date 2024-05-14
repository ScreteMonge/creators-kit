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

    public BlenderModel bmVertexColours(Model model)
    {
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

        byte[] transparencies = new byte[model.getFaceCount()];
        if (model.getFaceTransparencies() == null)
        {
            Arrays.fill(transparencies, (byte) 0);
        }
        else
        {
            transparencies = model.getFaceTransparencies();
        }

        // Each face has 3 vertices, each vertex has a h, l, s, and alpha value
        byte[] vertTransparencies = Arrays.copyOf(transparencies, transparencies.length);
        double[][] colours = new double[model.getFaceCount() * 3][4];
        int[] fc1 = model.getFaceColors1();
        int[] fc2 = model.getFaceColors2();
        int[] fc3 = model.getFaceColors3();

        for (int i = 0; i < model.getFaceCount(); i++)
        {
            //System.out.println(faceColours[i] + " - " + fc1[i] + ", " + fc2[i] + ", " + fc3[i]);
            double tp = vertTransparencies[i];
            if (tp < 0)
            {
                tp += 256;
            }
            tp = -1 * (tp - 255) / 255;

            int fc1i = fc1[i];
            if (fc1i > 32767)
            {
                fc1i -= 65536;
            }
            short fc1s = (short) fc1i;

            double h0 = (double) (63 - JagexColor.unpackHue(fc1s)) / 63;
            double l0 = (double) JagexColor.unpackLuminance(fc1s) / 127;
            double s0 = (double) JagexColor.unpackSaturation(fc1s) / 7;
            colours[i * 3][0] = h0;
            colours[i * 3][1] = l0;
            colours[i * 3][2] = s0;
            colours[i * 3][3] = tp;

            int fc2i = fc2[i];
            if (fc2i > 32767)
            {
                fc2i -= 65536;
            }
            short fc2s = (short) fc2i;

            double h1 = (double) (63 - JagexColor.unpackHue(fc2s)) / 63;
            double l1 = (double) JagexColor.unpackLuminance(fc2s) / 127;
            double s1 = (double) JagexColor.unpackSaturation(fc2s) / 7;
            colours[i * 3 + 1][0] = h1;
            colours[i * 3 + 1][1] = l1;
            colours[i * 3 + 1][2] = s1;
            colours[i * 3 + 1][3] = tp;

            int fc3i = fc3[i];
            if (fc3i > 32767)
            {
                fc3i -= 65536;
            }
            short fc3s = (short) fc3i;

            double h2 = (double) (63 - JagexColor.unpackHue(fc3s)) / 63;
            double l2 = (double) JagexColor.unpackLuminance(fc3s) / 127;
            double s2 = (double) JagexColor.unpackSaturation(fc3s) / 7;
            colours[i * 3 + 2][0] = h2;
            colours[i * 3 + 2][1] = l2;
            colours[i * 3 + 2][2] = s2;
            colours[i * 3 + 2][3] = tp;
        }

        byte[] renderPriorities = model.getFaceRenderPriorities();
        if (model.getFaceRenderPriorities() == null)
        {
            renderPriorities = new byte[model.getFaceCount()];
            Arrays.fill(renderPriorities, (byte) 0);
        }

        return new BlenderModel(
                true,
                verts,
                faces,
                colours,
                transparencies,
                renderPriorities
        );
    }

    public BlenderModel bmFaceColours(ModelStats[] modelStatsArray, int[] kitRecolours, boolean player, Model model)
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

        byte[] renderPriorities;
        if (model == null)
        {
            renderPriorities = new byte[md.getFaceCount()];
            Arrays.fill(renderPriorities, (byte) 0);
        }
        else if (model.getFaceRenderPriorities() == null)
        {
            renderPriorities = new byte[md.getFaceCount()];
            Arrays.fill(renderPriorities, (byte) 0);
        }
        else
        {
            renderPriorities = model.getFaceRenderPriorities();
        }

        return new BlenderModel(
                false,
                verts,
                faces,
                colours,
                transparencies,
                renderPriorities
        );
    }
}
