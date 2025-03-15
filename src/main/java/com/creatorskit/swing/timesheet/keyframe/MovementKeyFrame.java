package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.programming.MovementType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovementKeyFrame extends KeyFrame
{
    private int plane;
    private boolean poh;
    private int[][] path;
    private int currentStep;
    private int stepClientTick;
    private boolean loop;
    private double speed;
    private MovementType movementType;

    public MovementKeyFrame(double tick, int plane, boolean poh, int[][] path, int currentStep, int stepClientTick, boolean loop, double speed, MovementType movementType)
    {
        super(KeyFrameType.MOVEMENT, tick);
        this.plane = plane;
        this.poh = poh;
        this.path = path;
        this.currentStep = currentStep;
        this.stepClientTick = stepClientTick;
        this.loop = loop;
        this.speed = speed;
        this.movementType = movementType;
    }
}
