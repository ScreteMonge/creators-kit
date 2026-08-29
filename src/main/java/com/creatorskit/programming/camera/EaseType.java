package com.creatorskit.programming.camera;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum EaseType
{
    LINEAR("Linear"),
    SINE("Ease-In-Out Sine"),
    QUAD("Ease-In-Out Quad"),
    CUBIC("Ease-In-Out Cubic"),
    QUART("Ease-In-Out Quart"),
    QUINT("Ease-In-Out Quint"),
    EXPO("Ease-In-Out Expo"),
    EASE_IN_CUBIC("Ease-In Cubic"),
    EASE_IN_QUAD("Ease-In Quad"),
    EASE_IN_QUART("Ease-In Quart"),
    EASE_IN_QUINT("Ease-In Quint"),
    EASE_OUT_CUBIC("Ease-Out Cubic"),
    EASE_OUT_QUAD("Ease-Out Quad"),
    EASE_OUT_QUART("Ease-Out Quart"),
    EASE_OUT_QUINT("Ease-Out Quint"),
    ;

    private final String name;

    @Override
    public String toString()
    {
        return name;
    }
}