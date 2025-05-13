package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.OverheadKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.OverheadSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class OverheadAttributes extends Attributes
{
    private final JComboBox<OverheadSprite> skullSprite = new JComboBox<>();
    private final JComboBox<OverheadSprite> prayerSprite = new JComboBox<>();

    public OverheadAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        OverheadKeyFrame kf = (OverheadKeyFrame) keyFrame;
        skullSprite.setSelectedItem(kf.getSkullSprite());
        prayerSprite.setSelectedItem(kf.getPrayerSprite());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        skullSprite.setBackground(color);
        prayerSprite.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        skullSprite,
                        prayerSprite
                };
    }

    @Override
    public void addChangeListeners()
    {
        skullSprite.addItemListener(e ->
        {
            skullSprite.setBackground(getRed());
        });

        prayerSprite.addItemListener(e ->
        {
            prayerSprite.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        skullSprite.setSelectedItem(OverheadSprite.NONE);
        prayerSprite.setSelectedItem(OverheadSprite.NONE);
        super.resetAttributes(resetBackground);
    }
}