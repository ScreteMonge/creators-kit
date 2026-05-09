package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimationKeyFrame extends KeyFrame
{
    private boolean stall;
    private int active;
    private int startFrame;
    private boolean loop;
    private boolean freeze;
    private int idle;
    private int walk;
    private int run;
    private int walk180;
    private int walkRight;
    private int walkLeft;
    private int idleRight;
    private int idleLeft;
    private String targetCharacterName;

    public AnimationKeyFrame(double tick, boolean stall, int active, int startFrame, boolean loop, boolean freeze, int idle, int walk, int run, int walk180, int walkRight, int walkLeft, int idleRight, int idleLeft)
    {
        this(tick, stall, active, startFrame, loop, freeze, idle, walk, run, walk180, walkRight, walkLeft, idleRight, idleLeft, null);
    }

    public AnimationKeyFrame(double tick, boolean stall, int active, int startFrame, boolean loop, boolean freeze, int idle, int walk, int run, int walk180, int walkRight, int walkLeft, int idleRight, int idleLeft, String targetCharacterName)
    {
        super(KeyFrameType.ANIMATION, tick);
        this.stall = stall;
        this.active = active;
        this.startFrame = startFrame;
        this.loop = loop;
        this.freeze = freeze;
        this.idle = idle;
        this.walk = walk;
        this.run = run;
        this.walk180 = walk180;
        this.walkRight = walkRight;
        this.walkLeft = walkLeft;
        this.idleRight = idleRight;
        this.idleLeft = idleLeft;
        this.targetCharacterName = targetCharacterName;
    }
}
