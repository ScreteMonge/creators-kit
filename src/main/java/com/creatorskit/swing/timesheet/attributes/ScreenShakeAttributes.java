package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the global camera-shake keyframe. Four knobs only:
 * horizontal amplitude, vertical amplitude, base frequency, and duration.
 * No fade envelope -- the shake snaps in at the keyframe tick and snaps out
 * when its duration elapses.
 */
@Getter
public class ScreenShakeAttributes extends Attributes
{
    private final JSpinner amplitudeHorizontal = new JSpinner();
    private final JSpinner amplitudeVertical = new JSpinner();
    private final JSpinner frequency = new JSpinner();
    private final JSpinner durationTicks = new JSpinner();

    public ScreenShakeAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        ScreenShakeKeyFrame kf = (ScreenShakeKeyFrame) keyFrame;
        amplitudeHorizontal.setValue(kf.getAmplitudeHorizontal());
        amplitudeVertical.setValue(kf.getAmplitudeVertical());
        frequency.setValue(kf.getFrequency());
        durationTicks.setValue(kf.getDurationTicks());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        amplitudeHorizontal.setBackground(color);
        amplitudeVertical.setBackground(color);
        frequency.setBackground(color);
        durationTicks.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        amplitudeHorizontal,
                        amplitudeVertical,
                        frequency,
                        durationTicks
                };
    }

    @Override
    public void addChangeListeners()
    {
        amplitudeHorizontal.addChangeListener(e -> amplitudeHorizontal.setBackground(getRed()));
        amplitudeVertical.addChangeListener(e -> amplitudeVertical.setBackground(getRed()));
        frequency.addChangeListener(e -> frequency.setBackground(getRed()));
        durationTicks.addChangeListener(e -> durationTicks.setBackground(getRed()));
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        amplitudeHorizontal.setValue(ScreenShakeKeyFrame.DEFAULT_AMPLITUDE_HORIZONTAL);
        amplitudeVertical.setValue(ScreenShakeKeyFrame.DEFAULT_AMPLITUDE_VERTICAL);
        frequency.setValue(ScreenShakeKeyFrame.DEFAULT_FREQUENCY);
        durationTicks.setValue(ScreenShakeKeyFrame.DEFAULT_DURATION);
        super.resetAttributes(resetBackground);
    }
}
