package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatType;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class HealthAttributes extends Attributes
{
    private final JComboBox<Toggle> enableBox = new JComboBox<>();
    private final JComboBox<HitsplatType> hitsplatType = new JComboBox<>();
    private final JSpinner hitsplatHeight = new JSpinner();
    private final JSpinner maxHealth = new JSpinner();
    private final JSpinner currentHealth = new JSpinner();
    private final JSpinner healthbarHeight = new JSpinner();

    public HealthAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        HealthKeyFrame kf = (HealthKeyFrame) keyFrame;
        enableBox.setSelectedItem(kf.isEnabled() ? Toggle.ENABLE : Toggle.DISABLE);
        hitsplatType.setSelectedItem(kf.getHitsplatType());
        hitsplatHeight.setValue(kf.getHitsplatHeight());
        maxHealth.setValue(kf.getMaxHealth());
        currentHealth.setValue(kf.getCurrentHealth());
        healthbarHeight.setValue(kf.getHealthbarHeight());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        enableBox.setBackground(color);
        hitsplatType.setBackground(color);
        hitsplatHeight.setBackground(color);
        maxHealth.setBackground(color);
        currentHealth.setBackground(color);
        healthbarHeight.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        enableBox,
                        hitsplatType,
                        hitsplatHeight,
                        maxHealth,
                        currentHealth,
                        healthbarHeight
                };
    }

    @Override
    public void addChangeListeners()
    {
        enableBox.addItemListener(e ->
        {
            enableBox.setBackground(getRed());
        });

        hitsplatType.addItemListener(e ->
        {
            hitsplatType.setBackground(getRed());
        });

        hitsplatHeight.addChangeListener(e ->
        {
            hitsplatHeight.setBackground(getRed());
        });

        maxHealth.addChangeListener(e ->
        {
            maxHealth.setBackground(getRed());
        });

        currentHealth.addChangeListener(e ->
        {
            currentHealth.setBackground(getRed());
        });

        healthbarHeight.addChangeListener(e ->
        {
            healthbarHeight.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        enableBox.setSelectedItem(Toggle.DISABLE);
        hitsplatType.setSelectedItem(HitsplatType.DAMAGE);
        hitsplatHeight.setValue(30);
        maxHealth.setValue(99);
        currentHealth.setValue(99);
        healthbarHeight.setValue(60);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
