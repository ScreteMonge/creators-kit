package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.settings.AnimationToggle;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;

@Getter
public class AnimAttributes
{
    private final Color green = new Color(42, 77, 26);
    private final Color yellow = new Color(91, 80, 29);
    private final Color red = new Color(101, 66, 29);

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

    public void setAttributes(AnimationKeyFrame kf)
    {
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

    public void setBackgroundColours(KeyFrameState keyFrameState)
    {
        Color color;

        switch (keyFrameState)
        {
            default:
            case EMPTY:
                color = ColorScheme.DARKER_GRAY_COLOR;
                break;
            case ON_KEYFRAME:
                color = yellow;
                break;
            case OFF_KEYFRAME:
                color = green;
        }

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

    public void addChangeListeners()
    {
        manual.addChangeListener(e ->
        {
            manual.setBackground(red);
        });

        manualOverride.addItemListener(e ->
        {
            manualOverride.setBackground(red);
        });

        idle.addChangeListener(e ->
        {
            idle.setBackground(red);
        });

        walk.addChangeListener(e ->
        {
            walk.setBackground(red);
        });

        run.addChangeListener(e ->
        {
            run.setBackground(red);
        });

        walk180.addChangeListener(e ->
        {
            walk180.setBackground(red);
        });

        walkRight.addChangeListener(e ->
        {
            walkRight.setBackground(red);
        });

        walkLeft.addChangeListener(e ->
        {
            walkLeft.setBackground(red);
        });

        idleRight.addChangeListener(e ->
        {
            idleRight.setBackground(red);
        });

        idleLeft.addChangeListener(e ->
        {
            idleLeft.setBackground(red);
        });
    }
}
