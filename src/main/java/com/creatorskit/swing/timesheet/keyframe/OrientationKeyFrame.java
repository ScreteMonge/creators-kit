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
    private double turnRate;
    private String targetCharacterName;
    /**
     * Explicit turn direction (CW / CCW) or AUTO for legacy "shortest
     * path" behaviour. Null in saves predating this field; the playback
     * path treats null as AUTO so old saves load with no behaviour change.
     */
    private TurnDirection turnDirection;

    public OrientationKeyFrame(double tick, OrientationGoal goal, int start, int end, double duration, double turnRate)
    {
        this(tick, goal, start, end, duration, turnRate, null);
    }

    public OrientationKeyFrame(double tick, OrientationGoal goal, int start, int end, double duration, double turnRate, String targetCharacterName)
    {
        this(tick, goal, start, end, duration, turnRate, targetCharacterName, TurnDirection.AUTO);
    }

    public OrientationKeyFrame(double tick, OrientationGoal goal, int start, int end, double duration, double turnRate, String targetCharacterName, TurnDirection turnDirection)
    {
        super(KeyFrameType.ORIENTATION, tick);
        this.goal = goal;
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.turnRate = turnRate;
        this.targetCharacterName = targetCharacterName;
        this.turnDirection = turnDirection;
    }
}
