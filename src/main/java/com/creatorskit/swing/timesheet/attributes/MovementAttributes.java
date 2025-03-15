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
    private final JComboBox<MovementType> movementType = new JComboBox<>();

    public MovementAttributes()
    {
        addChangeListeners();
        loop.setOpaque(true);
        movementType.setOpaque(true);
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        MovementKeyFrame kf = (MovementKeyFrame) keyFrame;
        loop.setSelectedItem(kf.isLoop() ? Toggle.ENABLE : Toggle.DISABLE);
        speed.setValue(kf.getSpeed());
        movementType.setSelectedItem(kf.getMovementType());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        loop.setBackground(color);
        speed.setBackground(color);
        movementType.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        loop,
                        speed,
                        movementType
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

        movementType.addItemListener(e ->
        {
            movementType.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        loop.setSelectedItem(Toggle.DISABLE);
        speed.setValue(1);
        movementType.setSelectedItem(MovementType.NORMAL);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
