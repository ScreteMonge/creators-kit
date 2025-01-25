package com.creatorskit.swing.timesheet.attributes;

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
    private final JComboBox<Toggle> toggleSkull = new JComboBox<>();
    private final JComboBox<OverheadSprite> prayerSprite = new JComboBox<>();

    public OverheadAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        OverheadKeyFrame kf = (OverheadKeyFrame) keyFrame;
        toggleSkull.setSelectedItem(kf.isToggleSkull() ? Toggle.ENABLE : Toggle.DISABLE);
        prayerSprite.setSelectedItem(kf.getOverheadSprite());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        toggleSkull.setBackground(color);
        prayerSprite.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        toggleSkull,
                        prayerSprite
                };
    }

    @Override
    public void addChangeListeners()
    {
        toggleSkull.addItemListener(e ->
        {
            toggleSkull.setBackground(getRed());
        });

        prayerSprite.addItemListener(e ->
        {
            prayerSprite.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        toggleSkull.setSelectedItem(Toggle.DISABLE);
        prayerSprite.setSelectedItem(OverheadSprite.NONE);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
