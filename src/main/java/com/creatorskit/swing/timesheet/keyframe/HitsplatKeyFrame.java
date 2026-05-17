package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatVariant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HitsplatKeyFrame extends KeyFrame
{
    public static final double DEFAULT_DURATION = (double) 5 / 3;

    /**
     * Game ticks the hitsplat stays on screen. Fractional values allowed
     * (0.1-tick increments in the spinner) so users can match the in-game
     * 5/3-tick default exactly, plus shorter / longer effects. Sentinel -1
     * means "use {@link #DEFAULT_DURATION}" -- see HitsplatOverlay /
     * SummarySheet / AttributeSheet for the substitution.
     */
    private double duration;
    private HitsplatSprite sprite;
    private HitsplatVariant variant;
    private int damage;

    public HitsplatKeyFrame(double tick, KeyFrameType hitsplatType, double duration, HitsplatSprite sprite, HitsplatVariant variant, int damage)
    {
        super(hitsplatType, tick);
        this.duration = duration;
        this.sprite = sprite;
        this.variant = variant;
        this.damage = damage;
    }
}
