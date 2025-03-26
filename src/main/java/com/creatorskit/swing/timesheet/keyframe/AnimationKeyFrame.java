package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimationKeyFrame extends KeyFrame
{
    private boolean freeze;
    private int active;
    private int startFrame;
    private boolean loop;
    private int idle;
    private int walk;
    private int run;
    private int walk180;
    private int walkRight;
    private int walkLeft;
    private int idleRight;
    private int idleLeft;

    public AnimationKeyFrame(double tick, boolean freeze, int active, int startFrame, boolean loop, int idle, int walk, int run, int walk180, int walkRight, int walkLeft, int idleRight, int idleLeft)
    {
        super(KeyFrameType.ANIMATION, tick);
        this.freeze = freeze;
        this.active = active;
        this.startFrame = startFrame;
        this.loop = loop;
        this.idle = idle;
        this.walk = walk;
        this.run = run;
        this.walk180 = walk180;
        this.walkRight = walkRight;
        this.walkLeft = walkLeft;
        this.idleRight = idleRight;
        this.idleLeft = idleLeft;
    }
}
