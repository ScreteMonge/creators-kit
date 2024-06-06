package com.creatorskit.models.exporters;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class GLTF
{
    public int scene = 0;
    public scene[] scenes = new scene[]{new scene()};
    public node[] nodes = new node[]{new node()};
    public baseSampler[] samplers = new baseSampler[]{new baseSampler()};
    public List<mesh> meshes = new ArrayList<>();
    public List<buffer> buffers = new ArrayList<>();
    public List<bufferView> bufferViews = new ArrayList<>();
    public List<accessor> accessors = new ArrayList<>();
    public asset asset = new asset();

    public material[] materials = new material[]{new material()};
    public List<animation> animations = new ArrayList<>();

    enum AccessorType {
        SCALAR,
        VEC3,
        VEC4
    }

    public static class scene {
        int[] nodes = new int[]{0};
    }

    public static class node {
        int mesh = 0;
    }

    @Data
    public static class mesh {
        List<primitive> primitives = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int[] materials = new int[]{0};
        String name = "mesh";
    }

    @Data
    public static class primitive {
        int indices;
        attribute attributes;
        List<target> targets = new ArrayList<>();
        int material = 0;
    }

    @Data
    public static class target {
        int POSITION;
    }

    @Data
    public static class attribute {
        int POSITION;
        int COLOR_0;
    }

    public static class material
    {
        String name = "vertexColors";
        float[] baseColorFactor = new float[]{1.0f, 1.0f, 1.0f};
        pbrMetallicRoughness pbrMetallicRoughness = new pbrMetallicRoughness();
    }

    public static class pbrMetallicRoughness
    {
        float[] baseColorFactor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        float metallicFactor = 0.0f;
        float roughnessFactor = 1.0f;
    }

    @Data
    public static class animation {
        List<channel> channels = new ArrayList<>();
        List<sampler> samplers = new ArrayList<>();
    }

    public static class baseSampler {
        int magFilter = 9729;
        int minFilter = 9987;
        int wrapS = 33648;
        int wrapT = 33648;
    }

    @Data
    public static class sampler {
        int input;
        String interpolation = "STEP";
        int output;
    }

    @Data
    public static class channel {
        int sampler;
        channelTarget target;
    }

    public static class channelTarget {
        int node = 0;
        String path = "weights";
    }

    @Data
    public static class buffer {
        String uri;
        int byteLength;
    }

    @Data
    public static class bufferView {
        int buffer;
        int byteOffset;
        int byteLength;
        int target;
    }

    @Data
    public static class accessor {
        int bufferView;
        int byteOffset;
        int componentType;
        boolean normalized;
        int count;
        AccessorType type;
        List<Number> max = new ArrayList<>();
        List<Number> min = new ArrayList<>();
    }

    public static class asset {
        String version = "2.0";
        String generator = "CreatorsKit";
    }
}
