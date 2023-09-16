package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;

@AllArgsConstructor
@Getter
@Setter
public class ProgramComp
{
    private WorldPoint[] steps;
    private WorldPoint[] path;
    private Coordinate[] coordinates;
    private int currentStep;
    private double speed;
    private int turnSpeed;
    private int idleAnim;
    private int walkAnim;
    private MovementType movementType;
    private Color color;
    private boolean loop;
    private boolean programActive;
}
