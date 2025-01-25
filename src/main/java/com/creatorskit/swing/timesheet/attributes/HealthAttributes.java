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
    private final JComboBox<HitsplatSprite> hitsplat1Sprite = new JComboBox<>();
    private final JComboBox<HitsplatSprite> hitsplat2Sprite = new JComboBox<>();
    private final JComboBox<HitsplatSprite> hitsplat3Sprite = new JComboBox<>();
    private final JComboBox<HitsplatSprite> hitsplat4Sprite = new JComboBox<>();
    private final JSpinner hitsplat1 = new JSpinner();
    private final JSpinner hitsplat2 = new JSpinner();
    private final JSpinner hitsplat3 = new JSpinner();
    private final JSpinner hitsplat4 = new JSpinner();


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
        hitsplat1Sprite.setSelectedItem(kf.getHitsplat1Sprite());
        hitsplat2Sprite.setSelectedItem(kf.getHitsplat2Sprite());
        hitsplat3Sprite.setSelectedItem(kf.getHitsplat3Sprite());
        hitsplat4Sprite.setSelectedItem(kf.getHitsplat4Sprite());
        hitsplat1.setValue(kf.getHitsplat1());
        hitsplat2.setValue(kf.getHitsplat2());
        hitsplat3.setValue(kf.getHitsplat3());
        hitsplat4.setValue(kf.getHitsplat4());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        enableBox.setBackground(color);
        healthbarSprite.setBackground(color);
        maxHealth.setBackground(color);
        currentHealth.setBackground(color);
        hitsplat1Sprite.setBackground(color);
        hitsplat2Sprite.setBackground(color);
        hitsplat3Sprite.setBackground(color);
        hitsplat4Sprite.setBackground(color);
        hitsplat1.setBackground(color);
        hitsplat2.setBackground(color);
        hitsplat3.setBackground(color);
        hitsplat4.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        enableBox,
                        healthbarSprite,
                        maxHealth,
                        currentHealth,
                        hitsplat1Sprite,
                        hitsplat2Sprite,
                        hitsplat3Sprite,
                        hitsplat4Sprite,
                        hitsplat1,
                        hitsplat2,
                        hitsplat3,
                        hitsplat4
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

        hitsplat1Sprite.addItemListener(e ->
        {
            hitsplat1Sprite.setBackground(getRed());
        });

        hitsplat2Sprite.addItemListener(e ->
        {
            hitsplat2Sprite.setBackground(getRed());
        });

        hitsplat3Sprite.addItemListener(e ->
        {
            hitsplat3Sprite.setBackground(getRed());
        });

        hitsplat4Sprite.addItemListener(e ->
        {
            hitsplat4Sprite.setBackground(getRed());
        });

        hitsplat1.addChangeListener(e ->
        {
            hitsplat1.setBackground(getRed());
        });

        hitsplat2.addChangeListener(e ->
        {
            hitsplat2.setBackground(getRed());
        });

        hitsplat3.addChangeListener(e ->
        {
            hitsplat3.setBackground(getRed());
        });

        hitsplat4.addChangeListener(e ->
        {
            hitsplat4.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        enableBox.setSelectedItem(Toggle.ENABLE);
        healthbarSprite.setSelectedItem(HealthbarSprite.DEFAULT);
        maxHealth.setValue(99);
        currentHealth.setValue(99);
        hitsplat1Sprite.setSelectedItem(HitsplatSprite.NONE);
        hitsplat2Sprite.setSelectedItem(HitsplatSprite.NONE);
        hitsplat3Sprite.setSelectedItem(HitsplatSprite.NONE);
        hitsplat4Sprite.setSelectedItem(HitsplatSprite.NONE);
        hitsplat1.setValue(1);
        hitsplat2.setValue(1);
        hitsplat3.setValue(1);
        hitsplat4.setValue(1);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
