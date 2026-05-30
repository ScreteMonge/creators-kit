package com.creatorskit.saves;

import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
@Getter
@Setter
public class CharacterSave
{
    private String name;
    private WorldPoint nonInstancedPoint;
    private LocalPoint instancedPoint;
    private int instancedPlane;
    private boolean inInstance;
    private int compId;
    private boolean customMode;
    private int modelId;
    private boolean active;
    private int radius;
    private int rotation;
    private int animationId;
    private int frame;
    private int rgb;
    private MovementKeyFrame[] movementKeyFrames;
    private AnimationKeyFrame[] animationKeyFrames;
    private SpawnKeyFrame[] spawnKeyFrames;
    private ModelKeyFrameSave[] modelKeyFrameSaves;
    private OrientationKeyFrame[] orientationKeyFrames;
    private TextKeyFrame[] textKeyFrames;
    private OverheadKeyFrame[] overheadKeyFrames;
    private HealthKeyFrame[] healthKeyFrames;
    private SpotAnimKeyFrame[][] spotanimKeyFrames;
    private HitsplatKeyFrame[][] hitsplatKeyFrames;
    private KeyFrameType[] summary;
    /**
     * Added 2.2.x. Null in pre-2.2 saves — Gson defaults missing fields, the load path
     * treats null as "no projectile keyframes" so old saves load without migration.
     */
    private ProjectileKeyFrame[] projectileKeyFrames;
    /**
     * Added 2.2.x. Per-Character toggle for the bracket-scaled animation pass. Gson
     * defaults missing boolean fields to false, so old saves preserve default rendering.
     */
    private boolean renderFix;
    /**
     * Added 2.2.x. Horizontal model scale in 1/128 units (mirror of
     * NPCComposition.getWidthScale). Gson defaults missing ints to 0; the load path
     * treats {@code <= 0} as "use the runtime default of 128" so old saves keep working.
     */
    private int renderFixWidth;
    /** Vertical companion to {@link #renderFixWidth}. */
    private int renderFixHeight;
    /**
     * Added 2.2.x. User-set sub-tile position offsets (scene units; +Z = up). Gson
     * defaults missing ints to 0 which is exactly the no-offset case, so pre-2.2 saves
     * load cleanly without a migration step.
     */
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    /**
     * Added 2.3.x. Doom-of-Mokhaiotl-style secondary bars (Shield + Special) and the
     * Whisperer-style screen fade. Each is null in pre-2.3 saves; the load path treats
     * null as "no such keyframes" so old saves load cleanly without a migration step.
     */
    private ShieldKeyFrame[] shieldKeyFrames;
    private SpecialKeyFrame[] specialKeyFrames;
    private ScreenFadeKeyFrame[] screenFadeKeyFrames;
    /**
     * Added 2.3.x. Sol-Heredit-style global camera shake. Null in pre-2.3 saves --
     * Gson defaults missing fields, load treats null as "no shake keyframes".
     */
    private ScreenShakeKeyFrame[] screenShakeKeyFrames;
    /**
     * Added 2.3.x. User-set uniform extra scale (ALT+Scroll). Pre-2.3 saves get
     * Gson's default of 0.0 for missing doubles -- the load path treats {@code <= 0}
     * as "use default 1.0" so old saves keep rendering at their original size.
     */
    private double extraScale;
    /**
     * Camera keyframes for the timeline-driven free-cam (mlgudi/keyframe-camera
     * port). Null in saves predating this field; the load path treats null as
     * "no camera keyframes" so old saves load cleanly. Stored on Character even
     * though the effect is global (matches the Screen Fade / Shake pattern --
     * Phase 2 refactor will move all three to a central global-keyframes store).
     */
    private CameraKeyFrame[] cameraKeyFrames;
    /**
     * Colour keyframes (temporary model recolour with fade-in / hold / fade-out
     * envelope). Null in saves predating this field; the load path treats null
     * as "no Colour keyframes" so old saves load cleanly without a migration step.
     *
     * <p>Originally shipped as {@code pulseKeyFrames} -- renamed pre-release
     * before any user-authored saves existed, so no migration shim is needed.
     */
    private ColourKeyFrame[] colourKeyFrames;
    /**
     * Per-Character sound keyframes. Null in saves predating this field --
     * the load path treats null as "no Sound keyframes" so old saves load
     * with an empty Sound track and no warning. Playback fires through the
     * one-arg {@code Client.playSoundEffect(id)} so the user's in-game
     * SFX volume applies (no per-kf volume override here; for that, use
     * the global Area Sound 1/2/3/4 slots).
     */
    private SoundKeyFrame[] soundKeyFrames;
    /**
     * Per-Character timeline Labels (colored, named time-range markers shown
     * in the top row, organizational only). Null in saves predating the
     * feature -- the load path treats null as "no labels". Last field so
     * Lombok's @AllArgsConstructor appends it as the final positional arg.
     */
    private java.util.List<Block> blocks;
}

