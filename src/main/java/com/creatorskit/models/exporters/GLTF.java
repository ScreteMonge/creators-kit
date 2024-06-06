package com.creatorskit.models.exporters;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

public class GLTF
{
    public int scene = 0;
    public Scene[] scenes = new Scene[]{new Scene()};
    public Node[] nodes = new Node[]{new Node()};
    public BaseSampler[] samplers = new BaseSampler[]{new BaseSampler()};
    public List<Mesh> meshes = new ArrayList<>();
    public List<Buffer> buffers = new ArrayList<>();
    public List<BufferView> bufferViews = new ArrayList<>();
    public List<Accessor> accessors = new ArrayList<>();
    public Asset asset = new Asset();

    public Material[] materials = new Material[]{new Material()};
    public List<Animation> animations = new ArrayList<>();

    enum AccessorType {
        SCALAR,
        VEC3,
        VEC4
    }

    public static class Scene {
        int[] nodes = new int[]{0};
    }

    public static class Node {
        int mesh = 0;
    }

    @Data
    public static class Mesh {
        List<primitive> primitives = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int[] materials = new int[]{0};
        String name = "mesh";
    }

    @Data
    public static class primitive {
        int indices;
        Attribute attributes;
        List<Target> targets = new ArrayList<>();
        int material = 0;
    }

    @Data
    public static class Target {
        int POSITION;
    }

    @Data
    public static class Attribute {
        int POSITION;
        int COLOR_0;
    }

    public static class Material
    {
        String name = "vertexColors";
        float[] baseColorFactor = new float[]{1.0f, 1.0f, 1.0f};
        MetallicRoughness pbrMetallicRoughness = new MetallicRoughness();
    }

    public static class MetallicRoughness
    {
        float[] baseColorFactor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        float metallicFactor = 0.0f;
        float roughnessFactor = 1.0f;
    }

    @Data
    public static class Animation {
        List<Channel> channels = new ArrayList<>();
        List<Sampler> samplers = new ArrayList<>();
    }

    public static class BaseSampler {
        int magFilter = 9729;
        int minFilter = 9987;
        int wrapS = 33648;
        int wrapT = 33648;
    }

    @Data
    public static class Sampler {
        int input;
        String interpolation = "STEP";
        int output;
    }

    @Data
    public static class Channel {
        int sampler;
        ChannelTarget target;
    }

    public static class ChannelTarget {
        int node = 0;
        String path = "weights";
    }

    @Data
    public static class Buffer {
        String uri;
        int byteLength;
    }

    @Data
    public static class BufferView {
        int buffer;
        int byteOffset;
        int byteLength;
        int target;
    }

    @Data
    public static class Accessor {
        int bufferView;
        int byteOffset;
        int componentType;
        boolean normalized;
        int count;
        AccessorType type;
        List<Number> max = new ArrayList<>();
        List<Number> min = new ArrayList<>();
    }

    public static class Asset {
        String version = "2.0";
        String generator = "CreatorsKit";
    }
}
