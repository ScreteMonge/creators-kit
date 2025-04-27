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
    private boolean locationSet;
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
}
