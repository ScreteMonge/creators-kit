package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.OverheadKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;
import net.runelite.api.HeadIcon;

import javax.swing.*;
import java.awt.*;

@Getter
public class OverheadAttributes extends Attributes
{
    private final JComboBox<Toggle> enableBox = new JComboBox<>();
    private final JComboBox<HeadIcon> headIcon = new JComboBox<>();
    private final JSpinner height = new JSpinner();

    public OverheadAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        OverheadKeyFrame kf = (OverheadKeyFrame) keyFrame;
        enableBox.setSelectedItem(kf.isEnabled() ? Toggle.ENABLE : Toggle.DISABLE);
        headIcon.setSelectedItem(kf.getHeadIcon());
        height.setValue(kf.getHeight());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        enableBox.setBackground(color);
        headIcon.setBackground(color);
        height.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        enableBox,
                        headIcon,
                        height
                };
    }

    @Override
    public void addChangeListeners()
    {
        enableBox.addItemListener(e ->
        {
            enableBox.setBackground(getRed());
        });

        headIcon.addItemListener(e ->
        {
            headIcon.setBackground(getRed());
        });

        height.addChangeListener(e ->
        {
            height.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        enableBox.setSelectedItem(Toggle.DISABLE);
        headIcon.setSelectedItem(HeadIcon.MAGIC);
        height.setValue(60);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
