package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the Whisperer-style screen-fade keyframe. The fade is global
 * (covers the canvas regardless of which Character "owns" the keyframe) so
 * this card exposes the fade's full timing curve in addition to colour and
 * the centre ring's geometry.
 */
@Getter
public class ScreenFadeAttributes extends Attributes
{
    private final JButton colour = new JButton();
    private final JSpinner peakAlpha = new JSpinner();
    private final JSpinner ringRadius = new JSpinner();
    private final JSpinner ringFeather = new JSpinner();
    private final JSpinner fadeInTicks = new JSpinner();
    private final JSpinner holdTicks = new JSpinner();
    private final JSpinner fadeOutTicks = new JSpinner();

    private int rgb = ScreenFadeKeyFrame.DEFAULT_RGB;

    public ScreenFadeAttributes()
    {
        addChangeListeners();
    }

    public void setRgb(int rgb)
    {
        this.rgb = rgb;
        Color c = new Color(rgb);
        int luminance = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
        colour.setForeground(luminance > 140 ? Color.BLACK : Color.WHITE);
        colour.setText(String.format("#%06X", rgb & 0xFFFFFF));
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        ScreenFadeKeyFrame kf = (ScreenFadeKeyFrame) keyFrame;
        setRgb(kf.getRgb());
        peakAlpha.setValue(kf.getPeakAlpha());
        ringRadius.setValue(kf.getRingRadius());
        ringFeather.setValue(kf.getRingFeather());
        fadeInTicks.setValue(kf.getFadeInTicks());
        holdTicks.setValue(kf.getHoldTicks());
        fadeOutTicks.setValue(kf.getFadeOutTicks());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        colour.setBorder(BorderFactory.createLineBorder(color, 2));
        colour.setBackground(new Color(rgb));
        peakAlpha.setBackground(color);
        ringRadius.setBackground(color);
        ringFeather.setBackground(color);
        fadeInTicks.setBackground(color);
        holdTicks.setBackground(color);
        fadeOutTicks.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        colour,
                        peakAlpha,
                        ringRadius,
                        ringFeather,
                        fadeInTicks,
                        holdTicks,
                        fadeOutTicks
                };
    }

    @Override
    public void addChangeListeners()
    {
        peakAlpha.addChangeListener(e -> peakAlpha.setBackground(getRed()));
        ringRadius.addChangeListener(e -> ringRadius.setBackground(getRed()));
        ringFeather.addChangeListener(e -> ringFeather.setBackground(getRed()));
        fadeInTicks.addChangeListener(e -> fadeInTicks.setBackground(getRed()));
        holdTicks.addChangeListener(e -> holdTicks.setBackground(getRed()));
        fadeOutTicks.addChangeListener(e -> fadeOutTicks.setBackground(getRed()));
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        setRgb(ScreenFadeKeyFrame.DEFAULT_RGB);
        peakAlpha.setValue(ScreenFadeKeyFrame.DEFAULT_PEAK_ALPHA);
        ringRadius.setValue(ScreenFadeKeyFrame.DEFAULT_RING_RADIUS);
        ringFeather.setValue(ScreenFadeKeyFrame.DEFAULT_RING_FEATHER);
        fadeInTicks.setValue(ScreenFadeKeyFrame.DEFAULT_FADE_IN);
        holdTicks.setValue(ScreenFadeKeyFrame.DEFAULT_HOLD);
        fadeOutTicks.setValue(ScreenFadeKeyFrame.DEFAULT_FADE_OUT);
        super.resetAttributes(resetBackground);
    }
}
