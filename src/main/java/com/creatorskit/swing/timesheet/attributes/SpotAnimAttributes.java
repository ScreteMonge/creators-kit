package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.SpotAnimKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class SpotAnimAttributes extends Attributes
{
    private final JSpinner spotAnimId = new JSpinner();
    private final JComboBox<Toggle> loop = new JComboBox<>();

    public SpotAnimAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        SpotAnimKeyFrame kf = (SpotAnimKeyFrame) keyFrame;
        spotAnimId.setValue(kf.getSpotAnimId());
        loop.setSelectedItem(kf.isLoop() ? Toggle.ENABLE : Toggle.DISABLE);
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        spotAnimId.setBackground(color);
        loop.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        spotAnimId,
                        loop
                };
    }

    @Override
    public void addChangeListeners()
    {
        spotAnimId.addChangeListener(e ->
        {
            spotAnimId.setBackground(getRed());
        });

        loop.addItemListener(e ->
        {
            loop.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        spotAnimId.setValue(-1);
        loop.setSelectedItem(Toggle.DISABLE);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
