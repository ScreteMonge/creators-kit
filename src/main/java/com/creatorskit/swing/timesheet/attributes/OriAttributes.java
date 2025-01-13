package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.OrientationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.OrientationToggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class OriAttributes extends Attributes
{
    private final JSpinner manual = new JSpinner();
    private final JComboBox<OrientationToggle> manualOverride = new JComboBox<>();

    public OriAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        OrientationKeyFrame kf = (OrientationKeyFrame) keyFrame;
        manual.setValue(kf.getManualOrientation());
        manualOverride.setSelectedItem(kf.isManualOverride() ? OrientationToggle.MANUAL_ORIENTATION : OrientationToggle.SMART_ORIENTATION);
    }

    public void setBackgroundColours(Color color)
    {
        manual.setBackground(color);
        manualOverride.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        manual,
                        manualOverride
                };
    }

    @Override
    public void addChangeListeners()
    {
        manual.addChangeListener(e ->
        {
            manual.setBackground(getRed());
        });

        manualOverride.addItemListener(e ->
        {
            manualOverride.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        manual.setValue(0);
        manualOverride.setSelectedItem(OrientationToggle.SMART_ORIENTATION);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
