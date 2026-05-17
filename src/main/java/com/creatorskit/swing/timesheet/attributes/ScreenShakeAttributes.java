package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the global camera-shake keyframe. Mirrors
 * {@link ScreenFadeAttributes}: each axis amplitude + frequency are paired
 * with the standard fade-in / hold / fade-out envelope so a shake feels like
 * an in-game boss slam (snap up, sustain, ease out).
 */
@Getter
public class ScreenShakeAttributes extends Attributes
{
    private final JSpinner amplitudeX = new JSpinner();
    private final JSpinner amplitudeY = new JSpinner();
    private final JSpinner amplitudeZ = new JSpinner();
    private final JSpinner frequency = new JSpinner();
    private final JSpinner fadeInTicks = new JSpinner();
    private final JSpinner holdTicks = new JSpinner();
    private final JSpinner fadeOutTicks = new JSpinner();

    public ScreenShakeAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        ScreenShakeKeyFrame kf = (ScreenShakeKeyFrame) keyFrame;
        amplitudeX.setValue(kf.getAmplitudeX());
        amplitudeY.setValue(kf.getAmplitudeY());
        amplitudeZ.setValue(kf.getAmplitudeZ());
        frequency.setValue(kf.getFrequency());
        fadeInTicks.setValue(kf.getFadeInTicks());
        holdTicks.setValue(kf.getHoldTicks());
        fadeOutTicks.setValue(kf.getFadeOutTicks());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        amplitudeX.setBackground(color);
        amplitudeY.setBackground(color);
        amplitudeZ.setBackground(color);
        frequency.setBackground(color);
        fadeInTicks.setBackground(color);
        holdTicks.setBackground(color);
        fadeOutTicks.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        amplitudeX,
                        amplitudeY,
                        amplitudeZ,
                        frequency,
                        fadeInTicks,
                        holdTicks,
                        fadeOutTicks
                };
    }

    @Override
    public void addChangeListeners()
    {
        amplitudeX.addChangeListener(e -> amplitudeX.setBackground(getRed()));
        amplitudeY.addChangeListener(e -> amplitudeY.setBackground(getRed()));
        amplitudeZ.addChangeListener(e -> amplitudeZ.setBackground(getRed()));
        frequency.addChangeListener(e -> frequency.setBackground(getRed()));
        fadeInTicks.addChangeListener(e -> fadeInTicks.setBackground(getRed()));
        holdTicks.addChangeListener(e -> holdTicks.setBackground(getRed()));
        fadeOutTicks.addChangeListener(e -> fadeOutTicks.setBackground(getRed()));
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        amplitudeX.setValue(ScreenShakeKeyFrame.DEFAULT_AMPLITUDE_X);
        amplitudeY.setValue(ScreenShakeKeyFrame.DEFAULT_AMPLITUDE_Y);
        amplitudeZ.setValue(ScreenShakeKeyFrame.DEFAULT_AMPLITUDE_Z);
        frequency.setValue(ScreenShakeKeyFrame.DEFAULT_FREQUENCY);
        fadeInTicks.setValue(ScreenShakeKeyFrame.DEFAULT_FADE_IN);
        holdTicks.setValue(ScreenShakeKeyFrame.DEFAULT_HOLD);
        fadeOutTicks.setValue(ScreenShakeKeyFrame.DEFAULT_FADE_OUT);
        super.resetAttributes(resetBackground);
    }
}
