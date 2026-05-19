package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimationKeyFrame extends KeyFrame
{
    /** Multiplier on the animation frame-advance rate. 1.0 = native speed. */
    public static final double DEFAULT_SPEED = 1.0;
    /** Sentinel for {@link #lastFrame}: 0 means "play through the last frame of the animation". */
    public static final int LAST_FRAME_DISABLED = 0;

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
    /**
     * Added 2.3.x. Animation playback-rate multiplier (1.0 = native, 2.0 = double
     * speed, 0.5 = half). Applied to BOTH the scrub-time frame computation (via
     * Programmer.getAnimFrame) AND the per-tick playback advance (via
     * CKObject.tick's fractional accumulator). Pre-2.3 saves get Gson's default
     * of 0.0 for missing doubles; the load path treats {@code <= 0} as
     * "no value set" and uses {@link #DEFAULT_SPEED}.
     */
    private double speed = DEFAULT_SPEED;
    /**
     * Added 2.3.x. Upper bound of the playback range. {@link #LAST_FRAME_DISABLED}
     * (0, Gson's default) means "use the animation's natural last frame". When
     * set together with {@link #startFrame} defines [startFrame, lastFrame] as the
     * play range -- looping wraps back to startFrame instead of frame 0.
     */
    private int lastFrame = LAST_FRAME_DISABLED;
    /**
     * Added 2.3.x. Number of client ticks to dwell on the last frame before
     * looping back. Only meaningful when {@link #loop} is on. Defaults to 0
     * (no pause). Pre-2.3 saves get Gson's 0 which matches the no-pause default.
     */
    private int pauseTicks = 0;

    public AnimationKeyFrame(double tick, boolean stall, int active, int startFrame, boolean loop, boolean freeze, int idle, int walk, int run, int walk180, int walkRight, int walkLeft, int idleRight, int idleLeft)
    {
        this(tick, stall, active, startFrame, loop, freeze, idle, walk, run, walk180, walkRight, walkLeft, idleRight, idleLeft, DEFAULT_SPEED, LAST_FRAME_DISABLED, 0);
    }

    public AnimationKeyFrame(double tick, boolean stall, int active, int startFrame, boolean loop, boolean freeze, int idle, int walk, int run, int walk180, int walkRight, int walkLeft, int idleRight, int idleLeft, double speed)
    {
        this(tick, stall, active, startFrame, loop, freeze, idle, walk, run, walk180, walkRight, walkLeft, idleRight, idleLeft, speed, LAST_FRAME_DISABLED, 0);
    }

    public AnimationKeyFrame(double tick, boolean stall, int active, int startFrame, boolean loop, boolean freeze, int idle, int walk, int run, int walk180, int walkRight, int walkLeft, int idleRight, int idleLeft, double speed, int lastFrame, int pauseTicks)
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
        this.speed = speed;
        this.lastFrame = lastFrame;
        this.pauseTicks = pauseTicks;
    }
}
