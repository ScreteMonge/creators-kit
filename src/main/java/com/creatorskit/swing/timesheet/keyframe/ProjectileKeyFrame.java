package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.models.CustomModel;
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
    /** Default custom-model animation id. -1 = play no animation (static model). */
    public static final int DEFAULT_ANIMATION_ID = -1;
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
    /**
     * When true the projectile renders a {@link #customModel} instead of the
     * SpotAnim graphic identified by {@link #projectileId}. Mirrors
     * {@link ModelKeyFrame#isUseCustomModel()}. Old saves predate the field and
     * Gson defaults it to {@code false}, so they keep their SpotAnim look.
     */
    private boolean useCustomModel;
    /**
     * Live custom-model reference used when {@link #useCustomModel} is true.
     * <b>transient</b> on purpose: a {@link CustomModel} wraps a native
     * {@code Model} that can't be Gson-serialized. The persistent identity is
     * {@link #customModelName}; this live ref is re-resolved from the global
     * model list on first render after a load (see
     * {@code Programmer.resolveProjectileCustomModel}).
     */
    private transient CustomModel customModel;
    /**
     * Serializable identity of {@link #customModel} -- the model's
     * {@code CustomModelComp} name. Survives save/load (it's a plain String)
     * and is resolved back to a live {@link CustomModel} at render time.
     * Null when no custom model is selected.
     */
    private String customModelName;
    /**
     * Cache animation id played on the custom model. Only consulted when
     * {@link #useCustomModel} is true -- the SpotAnim path uses the spotanim's
     * own baked animation. -1 = no animation (static model). Old saves default
     * to 0 via Gson; the render path treats anything {@code < 0} as "none" and
     * 0 is a valid (if rarely-used) animation id, so no migration is needed --
     * old saves never set useCustomModel so this field is never read for them.
     */
    private int animationId;

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
        this(tick, projectileId, target, startX, startY, startHeight, endHeight, slope, durationTicks, faceTrajectory, radius, false, null, null, DEFAULT_ANIMATION_ID);
    }

    public ProjectileKeyFrame(double tick, int projectileId, String target, int startX, int startY, int startHeight, int endHeight, int slope, double durationTicks, boolean faceTrajectory, int radius, boolean useCustomModel, CustomModel customModel, String customModelName, int animationId)
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
        this.useCustomModel = useCustomModel;
        this.customModel = customModel;
        // Prefer the live model's comp name as the persistent identity; fall
        // back to the explicitly-passed name (used when re-creating a kf whose
        // live ref hasn't been resolved yet -- e.g. createCopy of a just-loaded
        // kf). Gson bypasses this constructor entirely on load, so a loaded
        // kf's customModelName comes straight from JSON.
        this.customModelName = customModel != null && customModel.getComp() != null
                ? customModel.getComp().getName()
                : customModelName;
        this.animationId = animationId;
    }
}
