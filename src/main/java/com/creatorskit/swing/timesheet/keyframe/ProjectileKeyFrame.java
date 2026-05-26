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
    public static final int DEFAULT_START_X = 0;
    public static final int DEFAULT_START_Y = 0;
    public static final int DEFAULT_START_HEIGHT = 80;
    public static final int DEFAULT_END_HEIGHT = 80;
    public static final int DEFAULT_SLOPE = 15;
    public static final double DEFAULT_DURATION = 2.0;
    public static final boolean DEFAULT_FACE_TRAJECTORY = false;
    /**
     * Render-radius default, in 1/128-tile units. Matches {@link
     * com.creatorskit.swing.timesheet.keyframe.SpotAnimKeyFrame}'s default
     * (60) and the underlying {@code RuneLiteObjectController.getRadius()}
     * convention. Old saves predating the field deserialize as 0; the
     * renderer treats 0 as "use default" so existing scenes don't suddenly
     * clip to a 0-tile box on load.
     */
    public static final int DEFAULT_RADIUS = 60;

    /** Spotanim / projectile gfx id used by client.createProjectile. */
    private int projectileId;

    /**
     * Target specifier. Accepts:
     *   - a single Character name ("Player")
     *   - a comma-separated list ("Player, NPC1, NPC2")
     *   - "folder:Foldername" to fan out to every Character under that folder
     *   - "f[Folder1, Folder2, ...]" multi-folder shorthand (same fan-out, just
     *     less typing than chaining several "folder:" entries)
     * Empty string means no targets — no projectile fires.
     */
    private String target;

    /**
     * Horizontal X offset (1/128-tile units) added to the source
     * Character's X when computing the projectile's spawn point. 0 =
     * spawn at the source tile centre; 128 = one tile east; -128 = one
     * tile west. Pairs with {@link #startY} so the spawn can be nudged
     * off-centre (e.g. to fire from a character's hand instead of their
     * model origin).
     */
    private int startX;
    /**
     * Horizontal Y offset (1/128-tile units) added to the source
     * Character's Y when computing the projectile's spawn point. 128 =
     * one tile north; -128 = one tile south. Pre-existing saves load
     * with 0 (Gson default) so behaviour is unchanged.
     */
    private int startY;
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
    /**
     * Per-keyframe render radius (1/128 tile units). 0 = "use renderer default"
     * (treated as {@link #DEFAULT_RADIUS} so pre-existing saves without the
     * field deserialize cleanly via Gson's int-default). Scales the projectile
     * model's visible size; intentionally per-kf so different shots in the
     * same scene can be sized differently (e.g. a wave of small fireballs
     * followed by a single boss-size impactor).
     */
    private int radius;

    public ProjectileKeyFrame(double tick, int projectileId, String target, int startHeight, int endHeight, int slope, double durationTicks, boolean faceTrajectory)
    {
        this(tick, projectileId, target, startHeight, endHeight, slope, durationTicks, faceTrajectory, DEFAULT_RADIUS);
    }

    public ProjectileKeyFrame(double tick, int projectileId, String target, int startHeight, int endHeight, int slope, double durationTicks, boolean faceTrajectory, int radius)
    {
        this(tick, projectileId, target, DEFAULT_START_X, DEFAULT_START_Y, startHeight, endHeight, slope, durationTicks, faceTrajectory, radius);
    }

    public ProjectileKeyFrame(double tick, int projectileId, String target, int startX, int startY, int startHeight, int endHeight, int slope, double durationTicks, boolean faceTrajectory, int radius)
    {
        super(KeyFrameType.PROJECTILE, tick);
        this.projectileId = projectileId;
        this.target = target;
        this.startX = startX;
        this.startY = startY;
        this.startHeight = startHeight;
        this.endHeight = endHeight;
        this.slope = slope;
        this.durationTicks = durationTicks;
        this.faceTrajectory = faceTrajectory;
        this.radius = radius;
    }
}
