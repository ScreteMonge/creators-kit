package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class HealthAttributes extends Attributes
{
    private final JSpinner duration = new JSpinner();
    private final JComboBox<HealthbarSprite> healthbarSprite = new JComboBox<>();
    private final JSpinner maxHealth = new JSpinner();
    private final JSpinner currentHealth = new JSpinner();
    /** Stack position relative to Shield/Special. Default 0 = topmost. */
    private final JSpinner order = new JSpinner();
    /** Pixel-width override. 0 = auto-scale from maxHealth. */
    private final JSpinner width = new JSpinner();
    /**
     * "Sync hitsplats" toggle. Defaults to ON. Drives whether hitsplats at
     * later ticks auto-create a follow-up Health keyframe via the
     * hitsplat -> bar sync pipeline. Stored on the keyframe so each bar
     * keyframe can opt in/out independently.
     */
    private final JCheckBox syncHitsplats = new JCheckBox("", true);
    /** Fade-in / fade-out durations in ticks. Only meaningful for the
     *  BOSS_HEALTH sprite -- the Boss healthbar overlay reads them at the
     *  edges of the bar's lifecycle. 0 = no fade (snap in / snap out). */
    private final JSpinner fadeInTicks = new JSpinner();
    private final JSpinner fadeOutTicks = new JSpinner();

    public HealthAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        HealthKeyFrame kf = (HealthKeyFrame) keyFrame;
        duration.setValue(kf.getDuration());
        healthbarSprite.setSelectedItem(kf.getHealthbarSprite());
        maxHealth.setValue(kf.getMaxHealth());
        currentHealth.setValue(kf.getCurrentHealth());
        order.setValue(kf.getOrder());
        width.setValue(kf.getWidth());
        syncHitsplats.setSelected(kf.isSyncHitsplats());
        fadeInTicks.setValue(kf.getFadeInTicks());
        fadeOutTicks.setValue(kf.getFadeOutTicks());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        duration.setBackground(color);
        healthbarSprite.setBackground(color);
        maxHealth.setBackground(color);
        currentHealth.setBackground(color);
        order.setBackground(color);
        width.setBackground(color);
        syncHitsplats.setBackground(color);
        fadeInTicks.setBackground(color);
        fadeOutTicks.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        duration,
                        healthbarSprite,
                        maxHealth,
                        currentHealth,
                        order,
                        width,
                        syncHitsplats,
                        fadeInTicks,
                        fadeOutTicks
                };
    }

    @Override
    public void addChangeListeners()
    {
        duration.addChangeListener(e ->
        {
            duration.setBackground(getRed());
        });

        healthbarSprite.addItemListener(e ->
        {
            healthbarSprite.setBackground(getRed());
        });

        maxHealth.addChangeListener(e ->
        {
            maxHealth.setBackground(getRed());
        });

        currentHealth.addChangeListener(e ->
        {
            currentHealth.setBackground(getRed());
        });

        order.addChangeListener(e ->
        {
            order.setBackground(getRed());
        });

        width.addChangeListener(e ->
        {
            width.setBackground(getRed());
        });

        syncHitsplats.addActionListener(e -> syncHitsplats.setBackground(getRed()));

        fadeInTicks.addChangeListener(e -> fadeInTicks.setBackground(getRed()));
        fadeOutTicks.addChangeListener(e -> fadeOutTicks.setBackground(getRed()));
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        duration.setValue(5.0);
        healthbarSprite.setSelectedItem(HealthbarSprite.DEFAULT);
        maxHealth.setValue(99);
        currentHealth.setValue(99);
        order.setValue(HealthKeyFrame.DEFAULT_ORDER);
        width.setValue(HealthKeyFrame.AUTO_WIDTH);
        syncHitsplats.setSelected(true);
        fadeInTicks.setValue(0.0);
        fadeOutTicks.setValue(0.0);
        super.resetAttributes(resetBackground);
    }
}