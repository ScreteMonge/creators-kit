package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
@Getter
@Setter
public class ProgramComp
{
    private WorldPoint[] stepsWP;
    private WorldPoint[] pathWP;
    private LocalPoint[] stepsLP;
    private LocalPoint[] pathLP;
    private Coordinate[] coordinates;
    private int currentStep;
    private double speed;
    private int turnSpeed;
    private int idleAnim;
    private int walkAnim;
    private MovementType movementType;
    private int rgb;
    private boolean loop;
    private boolean programActive;
}
