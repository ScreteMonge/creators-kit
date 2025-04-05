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
    private final JSpinner height = new JSpinner();

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
        height.setValue(kf.getHeight());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        spotAnimId.setBackground(color);
        loop.setBackground(color);
        height.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        spotAnimId,
                        loop,
                        height
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

        height.addChangeListener(e ->
        {
            height.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        spotAnimId.setValue(-1);
        loop.setSelectedItem(Toggle.DISABLE);
        height.setValue(92);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
