package com.creatorskit.swing.timesheet.keyframe.subtypes;

import com.creatorskit.programming.orientation.OrientationGoal;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
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

    public OrientationKeyFrame(double tick, OrientationGoal goal, int start, int end, double duration, int turnRate)
    {
        super(KeyFrameType.ORIENTATION, tick);
        this.goal = goal;
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.turnRate = turnRate;
    }
}
