package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpotAnimKeyFrame extends KeyFrame
{
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

    public SpotAnimKeyFrame(double tick, KeyFrameType spotAnimType, int spotAnimId, boolean loop, int height, int radius)
    {
        super(spotAnimType, tick);
        this.spotAnimId = spotAnimId;
        this.loop = loop;
        this.height = height;
        this.radius = radius;
    }
}
