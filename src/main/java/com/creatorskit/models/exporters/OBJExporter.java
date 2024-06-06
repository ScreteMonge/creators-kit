package com.creatorskit.models.exporters;

import com.creatorskit.models.BlenderModel;
import com.creatorskit.swing.colours.HSLColor;
import com.creatorskit.models.exporters.GLTFExporter.Vertex;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.creatorskit.models.ModelImporter.BLENDER_DIR;

@Slf4j
public class OBJExporter
{
    final static String HEADER = "# Made with Creator's Kit";
    final static String VERT = "v %f %f %f";
    final static String VERT_COLOUR = "v %f %f %f %f %f %f";
    final static String FACE = "f %d %d %d";
    final static String NEW_MTL = "newmtl c%d";
    final static String KD = "Kd %f %f %f";

    public static boolean saveOBJ(String name, BlenderModel blenderModel)
    {
        if (blenderModel.isUseVertexColours())
        {
            return saveVertColours(name, blenderModel);
        }
        else
        {
            return saveFaceColours(name, blenderModel);
        }
    }

    public static boolean saveVertColours(String name, BlenderModel blenderModel)
    {
        StringBuilder obj = new StringBuilder();
        obj.append(HEADER).append("\n");
        obj.append("o ").append(name).append("\n");

        HashMap<Integer, GLTFExporter.FaceMap> faceMaps = new HashMap<>();

        List<Vertex> uVerts = new ArrayList<>();
        HashMap<Integer, Integer> hashToIndex = new HashMap<>();
        HashMap<Integer, Vertex> hashVertexMap = new HashMap<>();

        // To avoid faces of the model being totally disjointed, while preserving vertex colours,
        // limit vert duplication to identical verts with different colours.
        for (int fi = 0; fi < blenderModel.getFaces().length; fi++)
        {
            int[] face = blenderModel.getFaces()[fi];
            GLTFExporter.FaceMap fm = new GLTFExporter.FaceMap();

            for (int vi = 0; vi < 3; vi++)
            {
                int vertIndex = face[vi];
                Vertex v = new Vertex();

                float[] xyz = new float[3];
                xyz[0] = blenderModel.getVertices()[vertIndex][0] / 128f;
                xyz[1] = -blenderModel.getVertices()[vertIndex][1] / 128f;
                xyz[2] = -blenderModel.getVertices()[vertIndex][2] / 128f;

                v.setXyz(xyz);
                v.setColour(getRGB(blenderModel.getVertexColours()[blenderModel.getVertexColourIndex()[fi * 3 + vi]]));

                if (!hashVertexMap.containsKey(v.hashCode()))
                {
                    hashToIndex.put(v.hashCode(), uVerts.size());
                    hashVertexMap.put(v.hashCode(), v);
                    uVerts.add(v);
                }

                fm.addIndex(vi, hashToIndex.get(v.hashCode()));
            }

            faceMaps.put(fi, fm);
        }

        for (Vertex v : uVerts)
        {
            obj.append(
                    String.format(
                            VERT_COLOUR,
                            v.getXyz()[0],
                            v.getXyz()[1],
                            v.getXyz()[2],
                            v.getColour()[0],
                            v.getColour()[1],
                            v.getColour()[2]
                    )
            ).append("\n");
        }

        for (int fi = 0; fi < faceMaps.size(); fi++)
        {
            GLTFExporter.FaceMap fm = faceMaps.get(fi);
            obj.append(
                    String.format(
                            FACE,
                            fm.getIndices()[0] + 1,
                            fm.getIndices()[1] + 1,
                            fm.getIndices()[2] + 1
                    )
            ).append("\n");
        }

        try
        {
            BLENDER_DIR.mkdirs();

            String path = Paths.get(BLENDER_DIR.getPath(), name).toString();
            String ext = ".obj";
            int iteration = 0;
            File file = new File(path + ext);
            while (file.exists())
            {
                iteration++;
                file = new File(path + " (" + iteration + ")" + ext);
            }

            FileWriter writer = new FileWriter(file, false);
            writer.write(obj.toString());
            writer.close();
        }
        catch (IOException e)
        {
            log.debug("Error when exporting model to file");
            return false;
        }

        return true;
    }

    public static boolean saveFaceColours(String name, BlenderModel blenderModel)
    {
        StringBuilder obj = new StringBuilder();
        StringBuilder mtl = new StringBuilder();

        for (int ci = 0; ci < blenderModel.getFaceColours().length; ci++)
        {
            float[] colour = getRGB(blenderModel.getFaceColours()[ci]);
            mtl.append(String.format(NEW_MTL, ci)).append("\n");
            mtl.append(String.format(KD, colour[0], colour[1], colour[2])).append("\n");
            if (colour[3] != 1.0f)
            {
                mtl.append(String.format("d %f", colour[3])).append("\n");
            }
        }

        obj.append(HEADER).append("\n");
        obj.append("o ").append(name).append("\n");

        for (int vi = 0; vi < blenderModel.getVertices().length; vi++)
        {
            obj.append(
                    String.format(
                            VERT,
                            blenderModel.getVertices()[vi][0] / 128f,
                            -blenderModel.getVertices()[vi][1] / 128f,
                            -blenderModel.getVertices()[vi][2] / 128f
                    )
            ).append("\n");
        }

        int currentMaterial = -1;

        for (int fi = 0; fi < blenderModel.getFaces().length; fi++)
        {
            int[] face = blenderModel.getFaces()[fi];
            if (blenderModel.getFaceColourIndex()[fi] != currentMaterial)
            {
                currentMaterial = blenderModel.getFaceColourIndex()[fi];
                obj.append("usemtl ").append(String.format("c%d", currentMaterial)).append("\n");
            }

            obj.append(
                    String.format(
                            FACE,
                            face[0] + 1,
                            face[1] + 1,
                            face[2] + 1
                    )
            ).append("\n");
        }

        try
        {
            BLENDER_DIR.mkdirs();

            String path = Paths.get(BLENDER_DIR.getPath(), name).toString();
            String ext = ".obj";
            int iteration = 0;
            File file = new File(path + ext);
            while (file.exists())
            {
                iteration++;
                file = new File(path + " (" + iteration + ")" + ext);
            }

            File fileMtl = new File(file.getAbsolutePath().replace(".obj", ".mtl"));

            FileWriter writer = new FileWriter(file, false);
            writer.write(obj.toString());
            writer.close();

            writer = new FileWriter(fileMtl, false);
            writer.write(mtl.toString());
            writer.close();
        }
        catch (IOException e)
        {
            log.debug("Error when exporting model to file");
            return false;
        }

        return true;
    }

    private static float[] getRGB(double[] colour)
    {
        Color rgb = HSLColor.toRGB(new float[] {
                (float) colour[0] * 360f,
                (float) colour[2] * 100f,
                (float) colour[1] * 100f
        });

        return new float[] {
                rgb.getRed() / 255f,
                rgb.getBlue() / 255f,
                rgb.getGreen() / 255f,
                (float) colour[3]
        };
    }

}
