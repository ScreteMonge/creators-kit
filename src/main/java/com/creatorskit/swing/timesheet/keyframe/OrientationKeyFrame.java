package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.programming.orientation.OrientationGoal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrientationKeyFrame extends KeyFrame
{
    public static final int TURN_RATE = 32; //In JUnits/clientTick

    private OrientationGoal goal;
    private int start;
    private int end;
    private double duration;
    private int turnRate;
    private String targetCharacterName;

    public OrientationKeyFrame(double tick, OrientationGoal goal, int start, int end, double duration, int turnRate)
    {
        this(tick, goal, start, end, duration, turnRate, null);
    }

    public OrientationKeyFrame(double tick, OrientationGoal goal, int start, int end, double duration, int turnRate, String targetCharacterName)
    {
        super(KeyFrameType.ORIENTATION, tick);
        this.goal = goal;
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.turnRate = turnRate;
        this.targetCharacterName = targetCharacterName;
    }
}
