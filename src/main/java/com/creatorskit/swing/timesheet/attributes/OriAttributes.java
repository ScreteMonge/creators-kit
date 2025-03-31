package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.programming.OrientationType;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.OrientationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class OriAttributes extends Attributes
{
    private final JSpinner manual = new JSpinner();
    private final JComboBox<OrientationType> type = new JComboBox<>();
    private final JComboBox<Toggle> override = new JComboBox<>();

    public OriAttributes()
    {
        addChangeListeners();
        type.setOpaque(true);
        override.setOpaque(true);
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        OrientationKeyFrame kf = (OrientationKeyFrame) keyFrame;
        manual.setValue(kf.getManualOrientation());
        type.setSelectedItem(kf.getType());
        override.setSelectedItem(kf.isOverride() ? Toggle.ENABLE : Toggle.DISABLE);
    }

    public void setBackgroundColours(Color color)
    {
        manual.setBackground(color);
        type.setBackground(color);
        override.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        manual,
                        type,
                        override
                };
    }

    @Override
    public void addChangeListeners()
    {
        manual.addChangeListener(e ->
        {
            manual.setBackground(getRed());
        });

        type.addItemListener(e ->
        {
            type.setBackground(getRed());
        });

        override.addItemListener(e ->
        {
            override.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        manual.setValue(0);
        type.setSelectedItem(OrientationType.GRADUAL);
        override.setSelectedItem(Toggle.DISABLE);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
