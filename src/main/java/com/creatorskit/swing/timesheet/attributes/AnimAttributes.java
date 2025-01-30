package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.settings.AnimationToggle;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;

@Getter
public class AnimAttributes extends Attributes
{
    private final JSpinner manual = new JSpinner();
    private final JComboBox<AnimationToggle> manualOverride = new JComboBox<>();

    private final JSpinner idle = new JSpinner();
    private final JSpinner walk = new JSpinner();
    private final JSpinner run = new JSpinner();
    private final JSpinner walk180 = new JSpinner();
    private final JSpinner walkRight = new JSpinner();
    private final JSpinner walkLeft = new JSpinner();
    private final JSpinner idleRight = new JSpinner();
    private final JSpinner idleLeft = new JSpinner();

    public AnimAttributes()
    {
        addChangeListeners();
        manualOverride.setOpaque(true);
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        AnimationKeyFrame kf = (AnimationKeyFrame) keyFrame;
        manual.setValue(kf.getManualAnim());
        manualOverride.setSelectedItem(kf.isManualOverride() ? AnimationToggle.MANUAL_ANIMATION : AnimationToggle.SMART_ANIMATION);
        idle.setValue(kf.getIdleAnim());
        walk.setValue(kf.getWalkAnim());
        run.setValue(kf.getRunAnim());
        walk180.setValue(kf.getWalk180Anim());
        walkRight.setValue(kf.getWalkRAnim());
        walkLeft.setValue(kf.getWalkLAnim());
        idleRight.setValue(kf.getIdleRAnim());
        idleLeft.setValue(kf.getIdleLAnim());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        manual.setBackground(color);
        manualOverride.setBackground(color);
        idle.setBackground(color);
        walk.setBackground(color);
        run.setBackground(color);
        walk180.setBackground(color);
        walkRight.setBackground(color);
        walkLeft.setBackground(color);
        idleRight.setBackground(color);
        idleLeft.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        manual,
                        manualOverride,
                        idle,
                        walk,
                        run,
                        walk180,
                        walkRight,
                        walkLeft,
                        idleRight,
                        idleLeft
                };
    }

    @Override
    public void addChangeListeners()
    {
        manual.addChangeListener(e ->
        {
            manual.setBackground(getRed());
        });

        manualOverride.addItemListener(e ->
        {
            manualOverride.setBackground(getRed());
        });

        idle.addChangeListener(e ->
        {
            idle.setBackground(getRed());
        });

        walk.addChangeListener(e ->
        {
            walk.setBackground(getRed());
        });

        run.addChangeListener(e ->
        {
            run.setBackground(getRed());
        });

        walk180.addChangeListener(e ->
        {
            walk180.setBackground(getRed());
        });

        walkRight.addChangeListener(e ->
        {
            walkRight.setBackground(getRed());
        });

        walkLeft.addChangeListener(e ->
        {
            walkLeft.setBackground(getRed());
        });

        idleRight.addChangeListener(e ->
        {
            idleRight.setBackground(getRed());
        });

        idleLeft.addChangeListener(e ->
        {
            idleLeft.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        manual.setValue(-1);
        manualOverride.setSelectedItem(AnimationToggle.SMART_ANIMATION);
        idle.setValue(-1);
        walk.setValue(-1);
        run.setValue(-1);
        walk180.setValue(-1);
        walkRight.setValue(-1);
        walkLeft.setValue(-1);
        idleRight.setValue(-1);
        idleLeft.setValue(-1);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
