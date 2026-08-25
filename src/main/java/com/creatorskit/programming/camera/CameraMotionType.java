package com.creatorskit.programming.camera;

public enum CameraMotionType
{
    DIRECTIONAL("Move to Tile"),
    TRACKING("Track Object");

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
