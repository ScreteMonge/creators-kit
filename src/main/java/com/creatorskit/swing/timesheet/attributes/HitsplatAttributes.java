package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.HitsplatKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class HitsplatAttributes extends Attributes
{
    private final JSpinner duration = new JSpinner();
    private final JComboBox<HitsplatSprite> sprite = new JComboBox<>();
    private final JSpinner damage = new JSpinner();

    public HitsplatAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        HitsplatKeyFrame kf = (HitsplatKeyFrame) keyFrame;
        duration.setValue(kf.getDuration());
        sprite.setSelectedItem(kf.getSprite());
        damage.setValue(kf.getDamage());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        duration.setBackground(color);
        sprite.setBackground(color);
        damage.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        duration,
                        sprite,
                        damage
                };
    }

    @Override
    public void addChangeListeners()
    {
        duration.addChangeListener(e ->
        {
            duration.setBackground(getRed());
        });

        sprite.addItemListener(e ->
        {
            sprite.setBackground(getRed());
        });

        damage.addChangeListener(e ->
        {
            damage.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        duration.setValue(-1);
        sprite.setSelectedItem(HitsplatSprite.NONE);
        damage.setValue(0);
        super.resetAttributes(resetBackground);
    }
}