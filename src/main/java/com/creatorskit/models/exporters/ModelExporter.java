package com.creatorskit.models.exporters;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import java.nio.file.Paths;

import com.creatorskit.models.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.RuneLite;
import org.apache.commons.lang3.ArrayUtils;

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
    private final CreatorsConfig config;
    public final File BLENDER_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/blender-models");

    @Inject
    public ModelExporter(Client client, CreatorsPlugin plugin, CreatorsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    public void saveToFile(String name, BlenderModel blenderModel)
    {
        boolean success;
        switch (config.exportFileFormat())
        {
            case OBJ:
                if (blenderModel.getAnimVertices().length > 0)
                {
                    plugin.sendChatMessage("You cannot export animations to OBJ format. Falling back to Blender format.");
                    success = saveBlender(name, blenderModel);
                } else {
                    success = OBJExporter.saveOBJ(name, blenderModel);
                }
                break;
            case GLTF:
                GLTFExporter gltfExporter = new GLTFExporter(name, blenderModel);
                success = gltfExporter.saveGLTF(plugin.getGson(), name);
                break;
            default:
            case BLENDER:
                success = saveBlender(name, blenderModel);
                break;
        }

        if (success)
            plugin.sendChatMessage("Exported " + name + " to your /.runelite/creatorskit/blender-models directory.");
    }

    private boolean saveBlender(String name, BlenderModel blenderModel)
    {
        try
        {
            BLENDER_DIR.mkdirs();

            String path = Paths.get(BLENDER_DIR.getPath(), name).toString();
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
            return false;
        }

        return true;
    }

    public BlenderModel bmVertexColours(Renderable renderable)
    {
        Model model;
        if (renderable instanceof Model)
        {
            model = (Model) renderable;
        }
        else
        {
            model = renderable.getModel();
        }

        int[][] verts = new int[model.getVerticesCount()][3];
        float[] vX = model.getVerticesX();
        float[] vY = model.getVerticesY();
        float[] vZ = model.getVerticesZ();

        for (int i = 0; i < verts.length; i++)
        {
            int[] v = verts[i];
            v[0] = (int) vX[i];
            v[1] = (int) vY[i];
            v[2] = (int) vZ[i];
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
        int[] vt = new int[vertTransparencies.length];
        for (int i = 0; i< model.getFaceCount(); i++)
        {
            int tp = vertTransparencies[i];
            if (tp < 0)
            {
                tp += 256;
            }
            vt[i] = tp;
        }

        int[] fc1 = model.getFaceColors1();
        int[] fc2 = model.getFaceColors2();
        int[] fc3 = model.getFaceColors3();
        int[][] shortList = new int[0][2];
        int[] vertexColourIndex = new int[0];

        for (int i = 0; i < model.getFaceCount(); i++)
        {
            int tp = vt[i];

            int fc1i = fc1[i];
            int fc2i = fc2[i];
            int fc3i = fc3[i];
            boolean shadeFlat = fc3i == -1;
            if (shadeFlat)
            {
                fc3i = fc2i = fc1i;
            }

            boolean alreadyContains1 = false;
            for (int e = 0; e < shortList.length; e++)
            {
                if (shortList[e][0] == fc1i && shortList[e][1] == tp)
                {
                    alreadyContains1 = true;
                    vertexColourIndex = ArrayUtils.add(vertexColourIndex, e);
                    break;
                }
            }

            if (!alreadyContains1)
            {
                int[] colourTransparency = new int[]{fc1i, tp};
                shortList = ArrayUtils.add(shortList, colourTransparency);
                vertexColourIndex = ArrayUtils.add(vertexColourIndex, shortList.length - 1);
            }


            boolean alreadyContains2 = false;
            for (int e = 0; e < shortList.length; e++)
            {
                if (shortList[e][0] == fc2i && shortList[e][1] == tp)
                {
                    alreadyContains2 = true;
                    vertexColourIndex = ArrayUtils.add(vertexColourIndex, e);
                    break;
                }
            }

            if (!alreadyContains2)
            {
                int[] colourTransparency = new int[]{fc2i, tp};
                shortList = ArrayUtils.add(shortList, colourTransparency);
                vertexColourIndex = ArrayUtils.add(vertexColourIndex, shortList.length - 1);
            }

            boolean alreadyContains3 = false;
            for (int e = 0; e < shortList.length; e++)
            {
                if (shortList[e][0] == fc3i && shortList[e][1] == tp)
                {
                    alreadyContains3 = true;
                    vertexColourIndex = ArrayUtils.add(vertexColourIndex, e);
                    break;
                }
            }

            if (!alreadyContains3)
            {
                int[] colourTransparency = new int[]{fc3i, tp};
                shortList = ArrayUtils.add(shortList, colourTransparency);
                vertexColourIndex = ArrayUtils.add(vertexColourIndex, shortList.length - 1);
            }
        }

        /*
        System.out.println("Shortlist length: " + shortList.length);
        System.out.println("index length: " + vertexColourIndex.length);
        System.out.println("Verts: " + model.getVerticesCount());
        System.out.println("Faces: " + model.getFaceCount());

         */

        double[][] vertexColours = new double[shortList.length][4];
        for (int i = 0; i < shortList.length; i++)
        {
            double tp = shortList[i][1];
            tp = -1 * (tp - 255) / 255;

            int intCol = shortList[i][0];
            if (intCol > 32767)
            {
                intCol -= 65536;
            }

            short col = (short) intCol;
            double h0 = (double) (63 - JagexColor.unpackHue(col)) / 63;
            double l0 = (double) JagexColor.unpackLuminance(col) / 127;
            double s0 = (double) JagexColor.unpackSaturation(col) / 7;
            vertexColours[i][0] = h0;
            vertexColours[i][1] = l0;
            vertexColours[i][2] = s0;
            vertexColours[i][3] = tp;
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
                vertexColours,
                vertexColourIndex,
                new double[0][],
                new int[0],
                renderPriorities,
                new int[0],
                new int[0][][]
        );
    }

    public BlenderModel bmFaceColours(
            ModelStats[] modelStatsArray,
            boolean object,
            int[] kitRecolours,
            boolean player,
            int[] vX,
            int[] vY,
            int[] vZ,
            int[] f1,
            int[] f2,
            int[] f3,
            byte[] transparencies,
            byte[] renderPriorities)
    {
        ModelData[] mds = new ModelData[modelStatsArray.length];

        for (int i = 0; i < modelStatsArray.length; i++)
        {
            ModelStats modelStats = modelStatsArray[i];
            ModelData modelData = client.loadModelData(modelStats.getModelId());

            if (modelData == null)
                continue;

            modelData.cloneColors().cloneVertices();
            if (modelData.getFaceTransparencies() != null)
            {
                modelData.cloneTransparencies();
            }

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

            if (!object)
            {
                modelData.scale(modelStats.getResizeX(), modelStats.getResizeZ(), modelStats.getResizeY());
            }
            modelData.translate(0, -1 * modelStats.getTranslateZ(), 0);

            mds[i] = modelData;
        }

        ModelData md = client.mergeModels(mds);
        if (object)
        {
            ModelStats ms0 = modelStatsArray[0];
            if (ms0 == null)
            {
                return null;
            }

            md.scale(ms0.getResizeX(), ms0.getResizeZ(), ms0.getResizeY());
        }

        /*
        System.out.println("ModelData FC: " + md.getFaceCount());
        System.out.println(f1.length + "," + f2.length + "," + f3.length);
        System.out.println("ModelData VC: " + md.getVerticesCount());
        System.out.println(vX.length + "," + vY.length + "," + vZ.length);
         */

        int[][] verts = new int[md.getVerticesCount()][3];
        for (int i = 0; i < verts.length; i++)
        {
            int[] v = verts[i];
            v[0] = vX[i];
            v[1] = vY[i];
            v[2] = vZ[i];
        }

        int[][] faces = new int[md.getFaceCount()][3];

        for (int i = 0; i < faces.length; i++)
        {
            int[] f = faces[i];
            f[0] = f1[i];
            f[1] = f2[i];
            f[2] = f3[i];
        }

        short[] mdFaceColours = md.getFaceColors();
        int[] faceColourIndex = new int[mdFaceColours.length];
        short[][] fcShortList = new short[0][2];
        for (int i = 0; i < md.getFaceCount(); i++)
        {
            short col = mdFaceColours[i];
            short tp = transparencies[i];
            boolean alreadyContains = false;
            for (int e = 0; e < fcShortList.length; e++)
            {
                if (col == fcShortList[e][0] && tp == fcShortList[e][1])
                {
                    faceColourIndex[i] = e;
                    alreadyContains = true;
                    break;
                }
            }

            if (alreadyContains)
            {
                continue;
            }

            fcShortList = ArrayUtils.add(fcShortList, new short[]{col, tp});
            faceColourIndex[i] = fcShortList.length - 1;
        }

        int shortListSize = fcShortList.length;
        double[][] faceColours = new double[shortListSize][4];
        for (int i = 0; i < shortListSize; i++)
        {
            short col = fcShortList[i][0];
            double h = (double) (63 - JagexColor.unpackHue(col)) / 63;
            double l = (double) JagexColor.unpackLuminance(col) / 127;
            double s = (double) JagexColor.unpackSaturation(col) / 7;

            double a = (fcShortList[i][1]);
            if (a < 0)
            {
                a += 256;
            }
            a = (255 - a) / 255;

            double[] array = faceColours[i];
            array[0] = h;
            array[1] = l;
            array[2] = s;
            array[3] = a;
        }

        return new BlenderModel(
                false,
                verts,
                faces,
                new double[0][],
                new int[0],
                faceColours,
                faceColourIndex,
                renderPriorities,
                new int[0],
                new int[0][][]

        );
    }

    public BlenderModel bmFaceColoursForForgedModel(
            ModelData md,
            int[] vX,
            int[] vY,
            int[] vZ,
            int[] f1,
            int[] f2,
            int[] f3,
            byte[] transparencies,
            byte[] renderPriorities)
    {
        /*
        System.out.println("ModelData FC: " + md.getFaceCount());
        System.out.println(f1.length + "," + f2.length + "," + f3.length);
         */

        int[][] verts = new int[md.getVerticesCount()][3];
        for (int i = 0; i < verts.length; i++)
        {
            int[] v = verts[i];
            v[0] = vX[i];
            v[1] = vY[i];
            v[2] = vZ[i];
        }

        int[][] faces = new int[md.getFaceCount()][3];

        for (int i = 0; i < faces.length; i++)
        {
            int[] f = faces[i];
            f[0] = f1[i];
            f[1] = f2[i];
            f[2] = f3[i];
        }

        short[] mdFaceColours = md.getFaceColors();
        int[] faceColourIndex = new int[mdFaceColours.length];
        short[][] fcShortList = new short[0][2];
        for (int i = 0; i < md.getFaceCount(); i++)
        {
            short col = mdFaceColours[i];
            short tp = transparencies[i];
            boolean alreadyContains = false;
            for (int e = 0; e < fcShortList.length; e++)
            {
                if (col == fcShortList[e][0] && tp == fcShortList[e][1])
                {
                    faceColourIndex[i] = e;
                    alreadyContains = true;
                    break;
                }
            }

            if (alreadyContains)
            {
                continue;
            }

            fcShortList = ArrayUtils.add(fcShortList, new short[]{col, tp});
            faceColourIndex[i] = fcShortList.length - 1;
        }

        int shortListSize = fcShortList.length;
        double[][] faceColours = new double[shortListSize][4];
        for (int i = 0; i < shortListSize; i++)
        {
            short col = fcShortList[i][0];
            double h = (double) (63 - JagexColor.unpackHue(col)) / 63;
            double l = (double) JagexColor.unpackLuminance(col) / 127;
            double s = (double) JagexColor.unpackSaturation(col) / 7;

            double a = (fcShortList[i][1]);
            if (a < 0)
            {
                a += 256;
            }
            a = (255 - a) / 255;

            double[] array = faceColours[i];
            array[0] = h;
            array[1] = l;
            array[2] = s;
            array[3] = a;
        }

        return new BlenderModel(
                false,
                verts,
                faces,
                new double[0][],
                new int[0],
                faceColours,
                faceColourIndex,
                renderPriorities,
                new int[0],
                new int[0][][]
        );
    }

    public BlenderModel bmFromCustomModel(CustomModel customModel)
    {
        CustomModelComp comp = customModel.getComp();
        Model model = customModel.getModel();

        BlenderModel blenderModel;
        ModelData md;
        int fCount;
        byte[] renderPriorities;
        byte[] transparencies;
        switch (comp.getType())
        {
            case FORGED:
                md = plugin.createComplexModelData(comp.getDetailedModels());
                fCount = md.getFaceCount();

                if (model.getFaceRenderPriorities() == null)
                {
                    renderPriorities = new byte[fCount];
                    Arrays.fill(renderPriorities, (byte) 0);
                }
                else
                {
                    renderPriorities = model.getFaceRenderPriorities();
                }

                if (model.getFaceTransparencies() == null)
                {
                    transparencies = new byte[fCount];
                    Arrays.fill(transparencies, (byte) 0);
                }
                else
                {
                    transparencies = model.getFaceTransparencies();
                }

                float[] fvx = md.getVerticesX();
                float[] fvy = md.getVerticesY();
                float[] fvz = md.getVerticesZ();

                int vCount = md.getVerticesCount();
                int[] vX = new int[vCount];
                int[] vY = new int[vCount];
                int[] vZ = new int[vCount];

                for (int i = 0; i < md.getVerticesCount(); i++)
                {
                    vX[i] = (int) fvx[i];
                    vY[i] = (int) fvy[i];
                    vZ[i] = (int) fvz[i];
                }

                blenderModel = bmFaceColoursForForgedModel(
                        md,
                        vX,
                        vY,
                        vZ,
                        md.getFaceIndices1(),
                        md.getFaceIndices2(),
                        md.getFaceIndices3(),
                        transparencies,
                        renderPriorities);
                break;
            default:
            case CACHE_NPC:
            case CACHE_OBJECT:
            case CACHE_GROUND_ITEM:
            case CACHE_MAN_WEAR:
            case CACHE_WOMAN_WEAR:
                md = plugin.constructModelDataFromCache(comp.getModelStats(), new int[0], false);
                fCount = md.getFaceCount();

                if (model.getFaceRenderPriorities() == null)
                {
                    renderPriorities = new byte[fCount];
                    Arrays.fill(renderPriorities, (byte) 0);
                }
                else
                {
                    renderPriorities = model.getFaceRenderPriorities();
                }

                if (model.getFaceTransparencies() == null)
                {
                    transparencies = new byte[fCount];
                    Arrays.fill(transparencies, (byte) 0);
                }
                else
                {
                    transparencies = model.getFaceTransparencies();
                }

                float[] fvx1 = md.getVerticesX();
                float[] fvy1 = md.getVerticesY();
                float[] fvz1 = md.getVerticesZ();

                int vCount1 = md.getVerticesCount();
                int[] vX1 = new int[vCount1];
                int[] vY1 = new int[vCount1];
                int[] vZ1 = new int[vCount1];

                for (int i = 0; i < md.getVerticesCount(); i++)
                {
                    vX1[i] = (int) fvx1[i];
                    vY1[i] = (int) fvy1[i];
                    vZ1[i] = (int) fvz1[i];
                }

                blenderModel = bmFaceColours(
                        comp.getModelStats(),
                        comp.getType() == CustomModelType.CACHE_OBJECT,
                        new int[0],
                        false,
                        vX1,
                        vY1,
                        vZ1,
                        md.getFaceIndices1(),
                        md.getFaceIndices2(),
                        md.getFaceIndices3(),
                        transparencies,
                        renderPriorities);
                break;
            case CACHE_PLAYER:
                md = plugin.constructModelDataFromCache(comp.getModelStats(), comp.getKitRecolours(), true);
                fCount = md.getFaceCount();

                if (model.getFaceRenderPriorities() == null)
                {
                    renderPriorities = new byte[fCount];
                    Arrays.fill(renderPriorities, (byte) 0);
                }
                else
                {
                    renderPriorities = model.getFaceRenderPriorities();
                }

                if (model.getFaceTransparencies() == null)
                {
                    transparencies = new byte[fCount];
                    Arrays.fill(transparencies, (byte) 0);
                }
                else
                {
                    transparencies = model.getFaceTransparencies();
                }

                float[] fvx2 = md.getVerticesX();
                float[] fvy2 = md.getVerticesY();
                float[] fvz2 = md.getVerticesZ();

                int vCount2 = md.getVerticesCount();
                int[] vX2 = new int[vCount2];
                int[] vY2 = new int[vCount2];
                int[] vZ2 = new int[vCount2];

                for (int i = 0; i < md.getVerticesCount(); i++)
                {
                    vX2[i] = (int) fvx2[i];
                    vY2[i] = (int) fvy2[i];
                    vZ2[i] = (int) fvz2[i];
                }

                blenderModel = bmFaceColours(
                        comp.getModelStats(),
                        false,
                        comp.getKitRecolours(),
                        true,
                        vX2,
                        vY2,
                        vZ2,
                        md.getFaceIndices1(),
                        md.getFaceIndices2(),
                        md.getFaceIndices3(),
                        transparencies,
                        renderPriorities);
                break;
            case BLENDER:
                blenderModel = comp.getBlenderModel();
        }

        return blenderModel;
    }

    public BlenderModel bmSpotAnimFromCache(ModelStats[] modelStatsArray)
    {
        ModelData[] mds = new ModelData[modelStatsArray.length];

        for (int i = 0; i < modelStatsArray.length; i++)
        {
            ModelStats modelStats = modelStatsArray[i];
            ModelData modelData = client.loadModelData(modelStats.getModelId());

            if (modelData == null)
                continue;

            modelData.cloneColors().cloneVertices();
            if (modelData.getFaceTransparencies() != null)
            {
                modelData.cloneTransparencies();
            }

            for (short s = 0; s < modelStats.getRecolourFrom().length; s++)
                modelData.recolor(modelStats.getRecolourFrom()[s], modelStats.getRecolourTo()[s]);

            if (modelStats.getResizeX() == 0 && modelStats.getResizeY() == 0 && modelStats.getResizeZ() == 0)
            {
                modelStats.setResizeX(128);
                modelStats.setResizeY(128);
                modelStats.setResizeZ(128);
            }

            modelData.scale(modelStats.getResizeX(), modelStats.getResizeZ(), modelStats.getResizeY());
            modelData.translate(0, -1 * modelStats.getTranslateZ(), 0);

            mds[i] = modelData;
        }

        ModelData md = client.mergeModels(mds);

        int[][] verts = new int[md.getVerticesCount()][3];
        float[] vX = md.getVerticesX();
        float[] vY = md.getVerticesY();
        float[] vZ = md.getVerticesZ();

        for (int i = 0; i < verts.length; i++)
        {
            int[] v = verts[i];
            v[0] = (int) vX[i];
            v[1] = (int) vY[i];
            v[2] = (int) vZ[i];
        }

        int[][] faces = new int[md.getFaceCount()][3];
        int[] f1 = md.getFaceIndices1();
        int[] f2 = md.getFaceIndices2();
        int[] f3 = md.getFaceIndices3();

        for (int i = 0; i < faces.length; i++)
        {
            int[] f = faces[i];
            f[0] = f1[i];
            f[1] = f2[i];
            f[2] = f3[i];
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

        short[] mdFaceColours = md.getFaceColors();
        int[] faceColourIndex = new int[mdFaceColours.length];
        short[][] fcShortList = new short[0][2];
        for (int i = 0; i < md.getFaceCount(); i++)
        {
            short col = mdFaceColours[i];
            short tp = transparencies[i];
            boolean alreadyContains = false;
            for (int e = 0; e < fcShortList.length; e++)
            {
                if (col == fcShortList[e][0] && tp == fcShortList[e][1])
                {
                    faceColourIndex[i] = e;
                    alreadyContains = true;
                    break;
                }
            }

            if (alreadyContains)
            {
                continue;
            }

            fcShortList = ArrayUtils.add(fcShortList, new short[]{col, tp});
            faceColourIndex[i] = fcShortList.length - 1;
        }

        int shortListSize = fcShortList.length;
        double[][] faceColours = new double[shortListSize][4];
        for (int i = 0; i < shortListSize; i++)
        {
            short col = fcShortList[i][0];
            double h = (double) (63 - JagexColor.unpackHue(col)) / 63;
            double l = (double) JagexColor.unpackLuminance(col) / 127;
            double s = (double) JagexColor.unpackSaturation(col) / 7;

            double a = (fcShortList[i][1]);
            if (a < 0)
            {
                a += 256;
            }
            a = (255 - a) / 255;

            double[] array = faceColours[i];
            array[0] = h;
            array[1] = l;
            array[2] = s;
            array[3] = a;
        }

        byte[] renderPriorities = new byte[md.getFaceCount()];

        return new BlenderModel(
                false,
                verts,
                faces,
                new double[0][],
                new int[0],
                faceColours,
                faceColourIndex,
                renderPriorities,
                new int[0],
                new int[0][][]
        );
    }
}
