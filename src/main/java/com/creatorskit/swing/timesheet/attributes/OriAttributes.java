package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.OrientationKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class OriAttributes extends Attributes
{
    private final JSpinner start = new JSpinner();
    private final JSpinner end = new JSpinner();
    private final JSpinner duration = new JSpinner();
    private final JSpinner turnRate = new JSpinner();

    public OriAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        OrientationKeyFrame kf = (OrientationKeyFrame) keyFrame;
        start.setValue(kf.getStart());
        end.setValue(kf.getEnd());
        duration.setValue(kf.getDuration());
        turnRate.setValue(kf.getTurnRate());
    }

    public void setBackgroundColours(Color color)
    {
        start.setBackground(color);
        end.setBackground(color);
        duration.setBackground(color);
        turnRate.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        start,
                        end,
                        duration,
                        turnRate
                };
    }

    @Override
    public void addChangeListeners()
    {
        start.addChangeListener(e ->
        {
            start.setBackground(getRed());
        });

        end.addChangeListener(e ->
        {
            end.setBackground(getRed());
        });

        duration.addChangeListener(e ->
        {
            duration.setBackground(getRed());
        });

        turnRate.addChangeListener(e ->
        {
            turnRate.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        start.setValue(0);
        end.setValue(0);
        duration.setValue(2);
        turnRate.setValue(-1);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
