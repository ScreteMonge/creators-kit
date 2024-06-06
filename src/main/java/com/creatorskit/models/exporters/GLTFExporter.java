package com.creatorskit.models.exporters;

import com.creatorskit.models.BlenderModel;
import com.creatorskit.models.exporters.GLTF.*;
import com.creatorskit.swing.colours.HSLColor;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashFunction;
import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static com.creatorskit.models.ModelImporter.BLENDER_DIR;

@Slf4j
public class GLTFExporter {

    static int UNSIGNED_SHORT = 5123;
    static int FLOAT = 5126;
    static int ARRAY_BUFFER = 34962;
    static int ELEMENT_ARRAY_BUFFER = 34963;

    @Getter
    GLTF gltf;
    BlenderModel blenderModel;

    HashMap<Integer, FaceMap> faceMaps = new HashMap<>();
    HashMap<Integer, float[]> vertexColours = new HashMap<>();

    List<Vertex> uVerts = new ArrayList<>();
    HashMap<Integer, Integer> hashToIndex = new HashMap<>();

    HashMap<Integer, Vertex[]> animVerts = new HashMap<>();
    HashMap<Integer, Vertex> hashVertexMap = new HashMap<>();

    public GLTFExporter(String name, BlenderModel blenderModel) {
        this.blenderModel = blenderModel;

        Vertex v1 = new Vertex();
        v1.setXyz(new float[]{1.0f, 2.0f, 3.0f});
        v1.setColour(new float[]{1.0f, 0.0f, 0.0f, 1.0f});

        Vertex v2 = new Vertex();
        v2.setXyz(new float[]{4.0f, 5.0f, 6.0f});
        v2.setColour(new float[]{1.0f, 0.0f, 0.0f, 1.0f});

        gltf = new GLTF();

        addMesh();
        refineVertices();
        addBasis();

        if (blenderModel.getClientTicks().length > 0)
        {
            createAnimVerts();
            addAnimation();
        }

        gltf.meshes.get(0).setName(name);
    }

    public boolean saveGLTF(Gson gson, String name)
    {
        try
        {
            BLENDER_DIR.mkdirs();

            String path = Paths.get(BLENDER_DIR.getPath(), name).toString();
            String ext = ".gltf";
            int iteration = 0;
            File file = new File(path + ext);
            while (file.exists())
            {
                iteration++;
                file = new File(path + " (" + iteration + ")" + ext);
            }

            FileWriter writer = new FileWriter(file, false);
            String string = gson.toJson(gltf);
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

    private void addMesh() {
        Mesh m = new Mesh();
        gltf.meshes.add(m);
        primitive prim = new primitive();
        prim.setAttributes(new Attribute());
        m.primitives.add(prim);
    }

    private float[] convertCoords(int[] xyz)
    {
        float[] xyzf = new float[3];
        for (int i = 0; i < 3; i++)
        {
            int s = i == 0 ? 1 : -1; // y/z are inverted
            xyzf[i] = xyz[i] * s / 128f;
        }
        return xyzf;
    }

    private void refineVertices()
    {
        // To avoid faces of the model being totally disjointed, while preserving vertex colours,
        // limit vert duplication to identical verts with different colours.

        for (int fi = 0; fi < blenderModel.getFaces().length; fi++)
        {
            int[] face = blenderModel.getFaces()[fi];
            FaceMap fm = new FaceMap();

            for (int vi = 0; vi < 3; vi++)
            {
                int vertIndex = face[vi];
                Vertex v = new Vertex();
                v.setXyz(convertCoords(blenderModel.getVertices()[vertIndex]));
                v.setColour(getColours(fi, vi));

                if (!hashVertexMap.containsKey(v.hashCode()))
                {
                    hashToIndex.put(v.hashCode(), uVerts.size());
                    vertexColours.put(uVerts.size(), v.getColour());
                    hashVertexMap.put(v.hashCode(), v);
                    uVerts.add(v);
                }

                fm.addIndex(vi, hashToIndex.get(v.hashCode()));
            }

            faceMaps.put(fi, fm);
        }
    }

    private float[] getColours(int fi, int vi)
    {
        double[] cols;
        if (blenderModel.isUseVertexColours())
        {
            cols = blenderModel.getVertexColours()[blenderModel.getVertexColourIndex()[fi * 3 + vi]];
        } else {
            cols = blenderModel.getFaceColours()[blenderModel.getFaceColourIndex()[fi]];
        }

        Color rgbColor = HSLColor.toRGB(new float[] {
                (float) cols[0] * 360f,
                (float) cols[2] * 100f,
                (float) cols[1] * 100f
        });

        return new float[] {
                (float) Math.pow(rgbColor.getRed() / 255f, 2.2f),
                (float) Math.pow(rgbColor.getBlue() / 255f, 2.2f),
                (float) Math.pow(rgbColor.getGreen() / 255f, 2.2f),
                (float) cols[3]
        };
    }

    private void addBasis() {

        // Verts and colours
        int vertexCount = uVerts.size();

        float[] vertexMin = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] vertexMax = new float[]{Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};

        float[] colourMin = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] colourMax = new float[]{Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};

        ByteBuffer vertices = ByteBuffer.allocateDirect(vertexCount * 3 * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer vertColours = ByteBuffer.allocateDirect(vertexCount * 4 * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (Vertex vert : uVerts) {
            for (int i = 0; i < 3; i++)
            {
                float s = vert.getXyz()[i];
                vertices.putFloat(s);
                vertexMin[i] = Math.min(vertexMin[i], s);
                vertexMax[i] = Math.max(vertexMax[i], s);
            }

            for (int l = 0; l < 4; l++)
            {
                vertColours.putFloat(vert.getColour()[l]);
                colourMin[l] = Math.min(colourMin[l], vert.getColour()[l]);
                colourMax[l] = Math.max(colourMax[l], vert.getColour()[l]);
            }
        }

        int vertAccessorIdx = addBuffer(vertices, vertexCount, ARRAY_BUFFER, FLOAT, AccessorType.VEC3);
        Accessor vertexAccessor = gltf.accessors.get(vertAccessorIdx);

        for (int i = 0; i < 3; i++) {
            vertexAccessor.min.add(vertexMin[i]);
            vertexAccessor.max.add(vertexMax[i]);
        }

        gltf.meshes.get(0).primitives.get(0).attributes.setPOSITION(vertAccessorIdx);

        int colourAccessorIdx = addBuffer(vertColours, vertexCount, ARRAY_BUFFER, FLOAT, AccessorType.VEC4);
        gltf.meshes.get(0).primitives.get(0).attributes.setCOLOR_0(colourAccessorIdx);

        // Faces
        ByteBuffer faces = ByteBuffer.allocateDirect(faceMaps.size() * 3 * 2)
                .order(ByteOrder.LITTLE_ENDIAN);

        short faceMin = Short.MAX_VALUE;
        short faceMax = Short.MIN_VALUE;

        for (int i = 0; i < faceMaps.size(); i++)
        {
            FaceMap fm = faceMaps.get(i);
            for (int j = 0; j < 3; j++)
            {
                faceMin = (short) Math.min(faceMin, fm.getIndices()[j]);
                faceMax = (short) Math.max(faceMax, fm.getIndices()[j]);
                faces.putShort((short) fm.getIndices()[j]);
            }
        }

        int faceAccessorIdx = addBuffer(
                faces, faceMaps.size() * 3, ELEMENT_ARRAY_BUFFER, UNSIGNED_SHORT, AccessorType.SCALAR
        );
        Accessor faceAccessor = gltf.accessors.get(faceAccessorIdx);
        faceAccessor.min.add(faceMin);
        faceAccessor.max.add(faceMax);
        gltf.meshes.get(0).primitives.get(0).setIndices(faceAccessorIdx);
    }

    private void createAnimVerts()
    {
        for (int frame = 0; frame < blenderModel.getAnimVertices().length; frame++)
        {
            int[][] anim = blenderModel.getAnimVertices()[frame];
            Vertex[] verts = new Vertex[uVerts.size()];

            for (int fi = 0; fi < blenderModel.getFaces().length; fi++)
            {
                int[] face = blenderModel.getFaces()[fi];
                for (int vi = 0; vi < 3; vi++)
                {
                    int index = faceMaps.get(fi).getIndices()[vi];
                    Vertex baseV = uVerts.get(index);

                    float[] newPosition = convertCoords(anim[face[vi]]);
                    float[] displacement = new float[3];
                    for (int i = 0; i < 3; i++)
                    {
                        displacement[i] = newPosition[i] - baseV.getXyz()[i];
                    }

                    Vertex v = new Vertex();
                    v.setXyz(displacement);

                    verts[index] = v;
                }
            }

            animVerts.put(frame, verts);
        }
    }

    private void addAnimation()
    {
        // Add morph targets
        for (int frame = 0; frame < blenderModel.getAnimVertices().length; frame++)
        {
            Vertex[] verts = animVerts.get(frame);
            int morphAccessor = addMorphTarget(verts);

            gltf.meshes.get(0).weights.add(frame == 0 ? 1 : 0);

            Target t = new Target();
            t.setPOSITION(morphAccessor);
            gltf.meshes.get(0).primitives.get(0).targets.add(t);
        }

        // Add the weights and keyframes
        int weightsAccessor = addWeights();
        int keyframesAccessor = addKeyframes();

        // Add the animation
        Animation anim = new Animation();

        Sampler sampler = new Sampler();
        sampler.input = keyframesAccessor;
        sampler.output = weightsAccessor;

        Channel channel = new Channel();
        channel.sampler = 0;
        channel.target = new ChannelTarget();

        anim.samplers.add(sampler);
        anim.channels.add(channel);

        gltf.animations.add(anim);
    }

    private int addMorphTarget(Vertex[] verts)
    {
        int vertexCount = verts.length;

        float[] morphMin = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] morphMax = new float[]{Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};

        ByteBuffer morph = ByteBuffer.allocateDirect(vertexCount * 3 * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

        // Add the displacements to the buffer
        for (Vertex vert : verts) {
            for (int i = 0; i < 3; i++)
            {
                float displacement = vert.getXyz()[i];
                morph.putFloat(displacement);
                morphMin[i] = Math.min(morphMin[i], displacement);
                morphMax[i] = Math.max(morphMax[i], displacement);
            }
        }

        int idx = addBuffer(morph, vertexCount, ARRAY_BUFFER, FLOAT, AccessorType.VEC3);
        Accessor a = gltf.accessors.get(idx);
        for (int i = 0; i < 3; i++)
        {
            a.min.add(morphMin[i]);
            a.max.add(morphMax[i]);
        }

        return idx;
    }

    private int addWeights()
    {
        int frameCount = blenderModel.getClientTicks().length;
        int capacity = frameCount * frameCount * 4;

        ByteBuffer weights = ByteBuffer.allocateDirect(capacity)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (int frame = 0; frame < frameCount; frame++)
        {
            for (int morph = 0; morph < frameCount; morph++)
            {
                weights.putFloat(morph == frame ? 1.0f : 0.0f);
            }
        }

        return addBuffer(weights, frameCount * frameCount, ARRAY_BUFFER, FLOAT, AccessorType.SCALAR);
    }

    private int addKeyframes()
    {
        int frameCount = blenderModel.getClientTicks().length;
        int capacity = frameCount * 4;

        ByteBuffer keyframes = ByteBuffer.allocateDirect(capacity)
                .order(ByteOrder.LITTLE_ENDIAN);

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (int i = 0; i < frameCount; i++)
        {
            float secs = i == 0 ? 0f : blenderModel.getClientTicks()[i] * 20 / 1000f;
            min = Math.min(min, secs);
            max = Math.max(max, secs);
            keyframes.putFloat(secs);
        }

        int idx = addBuffer(keyframes, frameCount, ARRAY_BUFFER, FLOAT, AccessorType.SCALAR);
        Accessor a = gltf.accessors.get(idx);
        a.min.add(min);
        a.max.add(max);

        return idx;
    }

    private int addBuffer(
            ByteBuffer buf,
            int count,
            int target,
            int componentType,
            AccessorType type
    )
    {
        Buffer b = new Buffer();
        BufferView bv = new BufferView();
        Accessor a = new Accessor();

        gltf.buffers.add(b);
        gltf.bufferViews.add(bv);
        gltf.accessors.add(a);

        b.setUri("data:application/octet-stream;base64," + bytesToBase64(buf));
        b.setByteLength(buf.capacity());

        bv.setBuffer(gltf.buffers.size() - 1);
        bv.setByteOffset(0);
        bv.setByteLength(buf.capacity());
        bv.setTarget(target);

        a.setBufferView(gltf.bufferViews.size() - 1);
        a.setByteOffset(0);
        a.setComponentType(componentType);
        a.setNormalized(false);
        a.setCount(count);
        a.setType(type);

        return gltf.accessors.size() - 1;
    }

    private String bytesToBase64(ByteBuffer buffer) {
        buffer.rewind();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    @Data
    private static class Vertex
    {
        float[] xyz;
        float[] colour;
        int index;

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof Vertex))
            {
                return false;
            }

            Vertex v = (Vertex) obj;
            return Arrays.equals(xyz, v.getXyz()) && Arrays.equals(colour, v.getColour());
        }

        @Override
        public int hashCode() {
            HashFunction hashFunction = Hashing.murmur3_32();
            Hasher hasher = hashFunction.newHasher();

            for (float value : xyz) {
                hasher.putFloat(value);
            }

            for (float value : colour) {
                hasher.putFloat(value);
            }

            return hasher.hash().asInt();
        }
    }

    @Data
    static class FaceMap
    {
        int[] indices;
        public void addIndex(int pos, int vertIndex)
        {
            if (indices == null)
            {
                indices = new int[3];
            }

            indices[pos] = vertIndex;
        }

        public int get(int i) { return indices[i]; }
    }
}
