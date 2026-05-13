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
}
