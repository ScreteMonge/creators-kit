package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpawnKeyFrame extends KeyFrame
{
    private boolean spawnActive;

    public SpawnKeyFrame(double tick, boolean spawnActive)
    {
        super(KeyFrameType.SPAWN, tick);
        this.spawnActive = spawnActive;
    }
}
