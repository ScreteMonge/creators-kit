package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.models.CustomModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpotAnimKeyFrame extends KeyFrame
{
    /**
     * Default animation-speed multiplier ({@code 1.0} = play at the spotanim's
     * baked-in cache rate). Old saves that predate the field deserialize as
     * {@code 0.0} (Gson's default for doubles); the playback path treats
     * {@code <= 0} as "use this default" so legacy scenes don't suddenly
     * play frozen at 0x speed.
     */
    public static final double DEFAULT_ANIMATION_SPEED = 1.0;
    /** Default custom-model animation id. -1 = play no animation (static model). */
    public static final int DEFAULT_ANIMATION_ID = -1;

    private KeyFrameType spotAnimType;
    private int spotAnimId;
    private boolean loop;
    private int height;
    /**
     * Render radius (1/128 tile units), same semantics as {@link com.creatorskit.Character}'s
     * radiusSpinner -- controls how far the spotanim model is allowed to render before
     * being clipped by surrounding scene tiles. Matches the underlying
     * {@link net.runelite.api.RuneLiteObjectController#getRadius()} default of 60.
     * Old saves without this field deserialize as 0 (Gson default); same back-compat
     * behaviour as ModelKeyFrame's pre-existing radius field.
     */
    private int radius;
    /**
     * Per-keyframe animation-speed multiplier applied on top of the spotanim's
     * baked cache animation. {@code 1.0} plays at the cache's authored rate;
     * {@code 2.0} plays twice as fast; {@code 0.5} plays half-speed. Plumbed
     * into the {@code setAnimationSpeed} field on the underlying CKObject's
     * animation-tick accumulator -- the same lever the Character animation
     * card already uses. Old saves deserialize as {@code 0.0}; the playback
     * code falls back to {@link #DEFAULT_ANIMATION_SPEED} in that case.
     */
    private double animationSpeed;
    /**
     * When true the spotanim renders a {@link #customModel} instead of the
     * cache SpotAnim graphic identified by {@link #spotAnimId}. Mirrors
     * {@link ModelKeyFrame#isUseCustomModel()}. Old saves predate the field and
     * Gson defaults it to {@code false}, so they keep their SpotAnim look.
     */
    private boolean useCustomModel;
    /**
     * Live custom-model reference used when {@link #useCustomModel} is true.
     * <b>transient</b>: a {@link CustomModel} wraps a native {@code Model} that
     * can't be Gson-serialized. The persistent identity is {@link #customModelName};
     * this live ref is re-resolved from the global model list on first render
     * after a load (see {@code Programmer.resolveSpotAnimCustomModel}).
     */
    private transient CustomModel customModel;
    /**
     * Serializable identity of {@link #customModel} -- the model's
     * {@code CustomModelComp} name. Survives save/load and is resolved back to a
     * live {@link CustomModel} at render time. Null when no custom model is set.
     */
    private String customModelName;
    /**
     * Cache animation id played on the custom model. Only consulted when
     * {@link #useCustomModel} is true -- the cache SpotAnim path uses the
     * spotanim's own baked animation. -1 = no animation (static model).
     */
    private int animationId;

    /**
     * Back-compat constructor for call sites that don't carry a user-set
     * animation speed (search-drop quick-add, actor-spotanim snapshots).
     * Defaults to {@link #DEFAULT_ANIMATION_SPEED} so new kfs feel like
     * the pre-feature behaviour.
     */
    public SpotAnimKeyFrame(double tick, KeyFrameType spotAnimType, int spotAnimId, boolean loop, int height, int radius)
    {
        this(tick, spotAnimType, spotAnimId, loop, height, radius, DEFAULT_ANIMATION_SPEED);
    }

    public SpotAnimKeyFrame(double tick, KeyFrameType spotAnimType, int spotAnimId, boolean loop, int height, int radius, double animationSpeed)
    {
        this(tick, spotAnimType, spotAnimId, loop, height, radius, animationSpeed, false, null, null, DEFAULT_ANIMATION_ID);
    }

    public SpotAnimKeyFrame(double tick, KeyFrameType spotAnimType, int spotAnimId, boolean loop, int height, int radius, double animationSpeed, boolean useCustomModel, CustomModel customModel, String customModelName, int animationId)
    {
        super(spotAnimType, tick);
        this.spotAnimId = spotAnimId;
        this.loop = loop;
        this.height = height;
        this.radius = radius;
        this.animationSpeed = animationSpeed;
        this.useCustomModel = useCustomModel;
        this.customModel = customModel;
        // Prefer the live model's comp name as the persistent identity; fall
        // back to the explicitly-passed name (createCopy of a just-loaded kf
        // whose live ref hasn't been resolved). Gson bypasses this constructor
        // on load, so a loaded kf's customModelName comes straight from JSON.
        this.customModelName = customModel != null && customModel.getComp() != null
                ? customModel.getComp().getName()
                : customModelName;
        this.animationId = animationId;
    }
}
