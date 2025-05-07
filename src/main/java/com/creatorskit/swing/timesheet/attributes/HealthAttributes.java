package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
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
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        duration.setBackground(color);
        healthbarSprite.setBackground(color);
        maxHealth.setBackground(color);
        currentHealth.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        duration,
                        healthbarSprite,
                        maxHealth,
                        currentHealth
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
    }

    @Override
    public void resetAttributes()
    {
        duration.setValue(5.0);
        healthbarSprite.setSelectedItem(HealthbarSprite.DEFAULT);
        maxHealth.setValue(99);
        currentHealth.setValue(99);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
