package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.SpecialKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the Special overhead-bar keyframe. Field-for-field mirror of
 * {@link ShieldAttributes} with a different default fill colour (OSRS yellow
 * vs. blue) -- the split keeps the two cards independent so the user can
 * animate HP, Shield, and Special on three separate timelines.
 */
@Getter
public class SpecialAttributes extends Attributes
{
    public static final int DEFAULT_RGB = 0xFFD333;  // OSRS special-bar yellow
    public static final int DEFAULT_MAX = 100;
    public static final double DEFAULT_DURATION = 5.0;

    private final JSpinner duration = new JSpinner();
    private final JButton colour = new JButton();
    private final JSpinner maxValue = new JSpinner();
    private final JSpinner currentValue = new JSpinner();

    private int rgb = DEFAULT_RGB;

    public SpecialAttributes()
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
        SpecialKeyFrame kf = (SpecialKeyFrame) keyFrame;
        duration.setValue(kf.getDuration());
        setRgb(kf.getRgb());
        maxValue.setValue(kf.getMaxValue());
        currentValue.setValue(kf.getCurrentValue());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        duration.setBackground(color);
        colour.setBorder(BorderFactory.createLineBorder(color, 2));
        colour.setBackground(new Color(rgb));
        maxValue.setBackground(color);
        currentValue.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        duration,
                        colour,
                        maxValue,
                        currentValue
                };
    }

    @Override
    public void addChangeListeners()
    {
        duration.addChangeListener(e -> duration.setBackground(getRed()));
        maxValue.addChangeListener(e -> maxValue.setBackground(getRed()));
        currentValue.addChangeListener(e -> currentValue.setBackground(getRed()));
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        duration.setValue(DEFAULT_DURATION);
        setRgb(DEFAULT_RGB);
        maxValue.setValue(DEFAULT_MAX);
        currentValue.setValue(DEFAULT_MAX);
        super.resetAttributes(resetBackground);
    }
}
