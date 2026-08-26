package com.creatorskit.programming.camera;

public enum CameraMotionType
{
    TILE_TRACKING("Track Tile"),
    OBJECT_TRACKING("Track Object");

    private final String name;

    CameraMotionType(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
