package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class HealthAttributes extends Attributes
{
    private final JComboBox<Toggle> enableBox = new JComboBox<>();
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
        enableBox.setSelectedItem(kf.isEnabled() ? Toggle.ENABLE : Toggle.DISABLE);
        healthbarSprite.setSelectedItem(kf.getHealthbarSprite());
        maxHealth.setValue(kf.getMaxHealth());
        currentHealth.setValue(kf.getCurrentHealth());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        enableBox.setBackground(color);
        healthbarSprite.setBackground(color);
        maxHealth.setBackground(color);
        currentHealth.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        enableBox,
                        healthbarSprite,
                        maxHealth,
                        currentHealth
                };
    }

    @Override
    public void addChangeListeners()
    {
        enableBox.addItemListener(e ->
        {
            enableBox.setBackground(getRed());
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
        enableBox.setSelectedItem(Toggle.ENABLE);
        healthbarSprite.setSelectedItem(HealthbarSprite.DEFAULT);
        maxHealth.setValue(99);
        currentHealth.setValue(99);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
