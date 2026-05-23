package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Schedules an OSRS projectile firing during playback. When this keyframe's
 * tick is crossed, the Programmer calls {@code client.createProjectile(...)}
 * once per resolved target — the game engine then handles arc, height,
 * trajectory, and despawn natively, so the visual is indistinguishable from
 * a real spell or arrow in-game.
 *
 * <p>The keyframe lives on the "caster" Character — its world position at the
 * keyframe's tick becomes the projectile's source point.
 */
@Getter
@Setter
public class ProjectileKeyFrame extends KeyFrame
{
    public static final int DEFAULT_PROJECTILE_ID = 9; // SpotanimID.IRON_ARROW_TRAVEL — verified visible arrow projectile
    public static final int DEFAULT_START_HEIGHT = 80;
    public static final int DEFAULT_END_HEIGHT = 80;
    public static final int DEFAULT_SLOPE = 15;
    public static final double DEFAULT_DURATION = 2.0;
    public static final boolean DEFAULT_FACE_TRAJECTORY = false;

    /** Spotanim / projectile gfx id used by client.createProjectile. */
    private int projectileId;

    /**
     * Target specifier. Accepts:
     *   - a single Character name ("Player")
     *   - a comma-separated list ("Player, NPC1, NPC2")
     *   - "folder:Foldername" to fan out to every Character under that folder
     * Empty string means no targets — no projectile fires.
     */
    private String target;

    private int startHeight;
    private int endHeight;
    private int slope;
    /** Game ticks the projectile takes to traverse from source to target. */
    private double durationTicks;
    /**
     * When true, the model is pitched each frame so its forward axis aligns with the
     * trajectory direction (nose-up when ascending, nose-down when crashing back to
     * earth). Useful for high-slope projectiles like Yama's overhead barrage where a
     * fixed-pitch model looks unnatural at the top of the arc.
     */
    private boolean faceTrajectory;

    public ProjectileKeyFrame(double tick, int projectileId, String target, int startHeight, int endHeight, int slope, double durationTicks, boolean faceTrajectory)
    {
        super(KeyFrameType.PROJECTILE, tick);
        this.projectileId = projectileId;
        this.target = target;
        this.startHeight = startHeight;
        this.endHeight = endHeight;
        this.slope = slope;
        this.durationTicks = durationTicks;
        this.faceTrajectory = faceTrajectory;
    }
}
