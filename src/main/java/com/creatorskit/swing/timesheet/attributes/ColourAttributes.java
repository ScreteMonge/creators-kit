package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.ColourBlendMode;
import com.creatorskit.swing.timesheet.keyframe.ColourKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the {@link ColourKeyFrame} card. Owns:
 * <ul>
 *   <li>a colour-picker JButton (same pattern as ShieldAttributes' fill picker)
 *       backed by a separately-tracked packed RGB int so the
 *       keyframe-state tint can recolour the button border without
 *       clobbering the selected fill;</li>
 *   <li>three tick-valued JSpinners for fadeIn / hold / fadeOut;</li>
 *   <li>a {@link ColourBlendMode} combo (Add / Replace / Multiply) with
 *       Add as the default;</li>
 *   <li>an easing toggle (linear vs ease-in-out smoothstep);</li>
 *   <li>an "affect spotanims" toggle so the tint can extend to the
 *       Character's SpotAnim 1 / 2 CKObjects.</li>
 * </ul>
 */
@Getter
public class ColourAttributes extends Attributes
{
    public static final int DEFAULT_RGB = 0xFFB060;  // warm peach -- reads on most skintones
    public static final double DEFAULT_FADE_IN = 1.0;
    public static final double DEFAULT_HOLD = 1.0;
    public static final double DEFAULT_FADE_OUT = 1.0;
    public static final ColourBlendMode DEFAULT_BLEND_MODE = ColourBlendMode.ADD;

    private final JButton colour = new JButton();
    private final JSpinner fadeInTicks = new JSpinner();
    private final JSpinner holdTicks = new JSpinner();
    private final JSpinner fadeOutTicks = new JSpinner();
    private final JComboBox<ColourBlendMode> blendMode = new JComboBox<>();
    /** Toggle: ENABLE = smoothstep ramps, DISABLE = linear ramps. */
    private final JComboBox<Toggle> easeInOut = new JComboBox<>();
    /** Toggle: ENABLE = tint also applied to spotanim 1 / 2 CKObjects. */
    private final JComboBox<Toggle> affectSpotAnims = new JComboBox<>();

    private int rgb = DEFAULT_RGB;

    public ColourAttributes()
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
        ColourKeyFrame kf = (ColourKeyFrame) keyFrame;
        setRgb(kf.getColorRgb());
        fadeInTicks.setValue(kf.getFadeInTicks());
        holdTicks.setValue(kf.getHoldTicks());
        fadeOutTicks.setValue(kf.getFadeOutTicks());
        blendMode.setSelectedItem(kf.getBlendMode() == null ? DEFAULT_BLEND_MODE : kf.getBlendMode());
        easeInOut.setSelectedItem(kf.isEaseInOut() ? Toggle.ENABLE : Toggle.DISABLE);
        affectSpotAnims.setSelectedItem(kf.isAffectSpotAnims() ? Toggle.ENABLE : Toggle.DISABLE);
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        // Colour swatch: border carries the keyframe-state tint, button face stays the chosen fill.
        colour.setBorder(BorderFactory.createLineBorder(color, 2));
        colour.setBackground(new Color(rgb));
        fadeInTicks.setBackground(color);
        holdTicks.setBackground(color);
        fadeOutTicks.setBackground(color);
        blendMode.setBackground(color);
        easeInOut.setBackground(color);
        affectSpotAnims.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        colour,
                        fadeInTicks,
                        holdTicks,
                        fadeOutTicks,
                        blendMode,
                        easeInOut,
                        affectSpotAnims
                };
    }

    @Override
    public void addChangeListeners()
    {
        fadeInTicks.addChangeListener(e -> fadeInTicks.setBackground(getRed()));
        holdTicks.addChangeListener(e -> holdTicks.setBackground(getRed()));
        fadeOutTicks.addChangeListener(e -> fadeOutTicks.setBackground(getRed()));
        blendMode.addItemListener(e -> blendMode.setBackground(getRed()));
        easeInOut.addItemListener(e -> easeInOut.setBackground(getRed()));
        affectSpotAnims.addItemListener(e -> affectSpotAnims.setBackground(getRed()));
        // colour button is wired in the card setup -- it needs the JColorChooser dialog.
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        setRgb(DEFAULT_RGB);
        fadeInTicks.setValue(DEFAULT_FADE_IN);
        holdTicks.setValue(DEFAULT_HOLD);
        fadeOutTicks.setValue(DEFAULT_FADE_OUT);
        blendMode.setSelectedItem(DEFAULT_BLEND_MODE);
        easeInOut.setSelectedItem(Toggle.DISABLE);
        affectSpotAnims.setSelectedItem(Toggle.DISABLE);
        super.resetAttributes(resetBackground);
    }
}
