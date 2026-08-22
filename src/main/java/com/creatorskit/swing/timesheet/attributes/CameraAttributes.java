package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.programming.camera.EaseType;
import com.creatorskit.swing.timesheet.keyframe.subtypes.CameraKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class CameraAttributes extends Attributes
{
    private final JComboBox<EaseType> easeType = new JComboBox<>();

    public CameraAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        if (keyFrame == null)
        {
            resetAttributes(true);
            return;
        }

        CameraKeyFrame kf = (CameraKeyFrame) keyFrame;
        easeType.setSelectedItem(kf.getEase());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        easeType.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        easeType
                };
    }

    @Override
    public void addChangeListeners()
    {
        easeType.addItemListener(e ->
        {
            easeType.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        easeType.setSelectedItem(EaseType.SINE);
        super.resetAttributes(resetBackground);
    }
}
