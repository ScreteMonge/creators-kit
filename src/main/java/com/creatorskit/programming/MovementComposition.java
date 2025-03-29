package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.LocalPoint;

@AllArgsConstructor
@Getter
public class MovementComposition
{
    private boolean moving;
    private LocalPoint localPoint;
    private OrientationAction orientationAction;
    private int orientationGoal;
}
