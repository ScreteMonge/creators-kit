package com.creatorssuite.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.LocalPoint;

@Getter
@Setter
@AllArgsConstructor
public class Program
{
    private double speed;
    private LocalPoint endLocation;
}
