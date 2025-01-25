package com.creatorskit.swing.timesheet.attributes;

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
    private final JSpinner spotAnimId1 = new JSpinner();
    private final JSpinner spotAnimId2 = new JSpinner();
    private final JComboBox<Toggle> loop1 = new JComboBox<>();
    private final JComboBox<Toggle> loop2 = new JComboBox<>();

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
        loop1.setSelectedItem(kf.isLoop1() ? Toggle.ENABLE : Toggle.DISABLE);
        loop2.setSelectedItem(kf.isLoop2() ? Toggle.ENABLE : Toggle.DISABLE);
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        spotAnimId1.setBackground(color);
        spotAnimId2.setBackground(color);
        loop1.setBackground(color);
        loop2.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        spotAnimId1,
                        spotAnimId2,
                        loop1,
                        loop2
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

        loop1.addItemListener(e ->
        {
            loop1.setBackground(getRed());
        });

        loop2.addItemListener(e ->
        {
            loop2.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        spotAnimId1.setValue(-1);
        spotAnimId2.setValue(-1);
        loop1.setSelectedItem(Toggle.DISABLE);
        loop2.setSelectedItem(Toggle.DISABLE);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
