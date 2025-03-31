package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.LocalPoint;

@AllArgsConstructor
@Getter
@Setter
public class MovementComposition
{
    private boolean moving;
    private LocalPoint localPoint;
    private OrientationAction orientationAction;
    private int orientationGoal;
}
