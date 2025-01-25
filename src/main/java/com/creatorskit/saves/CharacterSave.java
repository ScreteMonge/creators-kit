package com.creatorskit.saves;

import com.creatorskit.programming.ProgramComp;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
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
    private int[] instancedRegions;
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
    private ProgramComp programComp;
    private KeyFrame[][] keyFrames;
    private KeyFrame[] currentFrames;
}
