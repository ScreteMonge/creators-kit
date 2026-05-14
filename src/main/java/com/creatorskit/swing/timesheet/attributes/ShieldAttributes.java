package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ShieldKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the Shield overhead-bar keyframe. Mirrors {@link HealthAttributes}
 * apart from the colour picker -- HP uses a fixed red/green sprite but Shield and
 * Special bars pick their fill colour from a {@link JColorChooser} button.
 *
 * <p>The colour is stored as a packed RGB int alongside the button so that
 * {@link #createKeyFrame}-style code can read it back without re-deriving from
 * the button's background.
 */
@Getter
public class ShieldAttributes extends Attributes
{
    public static final int DEFAULT_RGB = 0x29A2FF;  // OSRS shield-blue
    public static final int DEFAULT_MAX = 100;
    public static final double DEFAULT_DURATION = 5.0;

    private final JSpinner duration = new JSpinner();
    private final JButton colour = new JButton();
    private final JSpinner maxValue = new JSpinner();
    private final JSpinner currentValue = new JSpinner();

    /**
     * Packed RGB of the currently-selected fill colour. Tracked separately from
     * the button background because the background gets recoloured to the
     * green/yellow/red keyframe-state tint by {@link #setBackgroundColours}.
     */
    private int rgb = DEFAULT_RGB;

    public ShieldAttributes()
    {
        addChangeListeners();
    }

    public void setRgb(int rgb)
    {
        this.rgb = rgb;
        // Light/dark text contrast based on perceived luminance.
        Color c = new Color(rgb);
        int luminance = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
        colour.setForeground(luminance > 140 ? Color.BLACK : Color.WHITE);
        colour.setText(String.format("#%06X", rgb & 0xFFFFFF));
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        ShieldKeyFrame kf = (ShieldKeyFrame) keyFrame;
        duration.setValue(kf.getDuration());
        setRgb(kf.getRgb());
        maxValue.setValue(kf.getMaxValue());
        currentValue.setValue(kf.getCurrentValue());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        duration.setBackground(color);
        // Colour picker keeps its own fill -- only its border carries the keyframe-state tint.
        colour.setBorder(BorderFactory.createLineBorder(color, 2));
        // But keep the button face as the selected fill so the colour is visible.
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
        // colour button is handled by the card setup since it needs the JColorChooser dialog.
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
