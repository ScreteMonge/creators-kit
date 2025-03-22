package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.programming.MovementType;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.MovementKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class MovementAttributes extends Attributes
{
    private final JComboBox<Toggle> loop = new JComboBox<>();
    private final JSpinner speed = new JSpinner();

    public MovementAttributes()
    {
        addChangeListeners();
        loop.setOpaque(true);
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        MovementKeyFrame kf = (MovementKeyFrame) keyFrame;
        loop.setSelectedItem(kf.isLoop() ? Toggle.ENABLE : Toggle.DISABLE);
        speed.setValue(kf.getSpeed());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        loop.setBackground(color);
        speed.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        loop,
                        speed
                };
    }

    @Override
    public void addChangeListeners()
    {
        loop.addItemListener(e ->
        {
            loop.setBackground(getRed());
        });

        speed.addChangeListener(e ->
        {
            speed.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        loop.setSelectedItem(Toggle.DISABLE);
        speed.setValue(1);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
