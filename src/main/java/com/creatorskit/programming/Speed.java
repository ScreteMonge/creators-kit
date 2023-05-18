package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Speed
{
    WALK(1),
    RUN(2),
    SPRINT(3);

    private final int tilesPerTick;
}
