package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.OrientationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.OrientationToggle;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;

@Getter
public class OriAttributes
{
    private final Color green = new Color(42, 77, 26);
    private final Color yellow = new Color(91, 80, 29);
    private final Color red = new Color(101, 66, 29);

    private final JSpinner manual = new JSpinner();
    private final JComboBox<OrientationToggle> manualOverride = new JComboBox<>();

    public OriAttributes()
    {
        addChangeListeners();
    }

    public void setAttributes(OrientationKeyFrame kf)
    {
        manual.setValue(kf.getManualOrientation());
        manualOverride.setSelectedItem(kf.isManualOverride() ? OrientationToggle.MANUAL_ORIENTATION : OrientationToggle.SMART_ORIENTATION);
    }

    public void setBackgroundColours(KeyFrameState keyFrameState)
    {
        Color color;

        switch (keyFrameState)
        {
            default:
            case EMPTY:
                color = ColorScheme.DARKER_GRAY_COLOR;
                break;
            case ON_KEYFRAME:
                color = yellow;
                break;
            case OFF_KEYFRAME:
                color = green;
        }

        manual.setBackground(color);
        manualOverride.setBackground(color);
    }

    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        manual,
                        manualOverride
                };
    }

    public void addChangeListeners()
    {
        manual.addChangeListener(e ->
        {
            manual.setBackground(red);
        });

        manualOverride.addItemListener(e ->
        {
            manualOverride.setBackground(red);
        });
    }
}
