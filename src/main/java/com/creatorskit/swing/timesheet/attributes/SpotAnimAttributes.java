package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.SpotAnimKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class SpotAnimAttributes extends Attributes
{
    private final JSpinner spotAnimId1 = new JSpinner();
    private final JSpinner spotAnimId2 = new JSpinner();

    public SpotAnimAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        SpotAnimKeyFrame kf = (SpotAnimKeyFrame) keyFrame;
        spotAnimId1.setValue(kf.getSpotAnimId1());
        spotAnimId2.setValue(kf.getSpotAnimId2());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        spotAnimId1.setBackground(color);
        spotAnimId2.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        spotAnimId1,
                        spotAnimId2
                };
    }

    @Override
    public void addChangeListeners()
    {
        spotAnimId1.addChangeListener(e ->
        {
            spotAnimId1.setBackground(getRed());
        });

        spotAnimId2.addChangeListener(e ->
        {
            spotAnimId2.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        spotAnimId1.setValue(-1);
        spotAnimId2.setValue(-1);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
