package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimationKeyFrame extends KeyFrame
{
    private int manualAnim;
    private boolean manualOverride;
    private int idleAnim;
    private int walkAnim;
    private int runAnim;
    private int walk180Anim;
    private int walkRAnim;
    private int walkLAnim;
    private int idleRAnim;
    private int idleLAnim;

    public AnimationKeyFrame(double tick, int manualAnim, boolean manualOverride, int idleAnim, int walkAnim, int runAnim, int walk180Anim, int walkRAnim, int walkLAnim, int idleRAnim, int idleLAnim)
    {
        super(KeyFrameType.ANIMATION, tick);
        this.manualAnim = manualAnim;
        this.manualOverride = manualOverride;
        this.idleAnim = idleAnim;
        this.walkAnim = walkAnim;
        this.runAnim = runAnim;
        this.walk180Anim = walk180Anim;
        this.walkRAnim = walkRAnim;
        this.walkLAnim = walkLAnim;
        this.idleRAnim = idleRAnim;
        this.idleLAnim = idleLAnim;
    }
}
