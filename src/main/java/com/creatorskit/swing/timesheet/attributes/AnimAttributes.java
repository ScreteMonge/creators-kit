package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class AnimAttributes extends Attributes
{
    private final JComboBox<Toggle> freeze = new JComboBox<>();
    private final JSpinner active = new JSpinner();
    private final JSpinner startFrame = new JSpinner();
    private final JComboBox<Toggle> loop = new JComboBox<>();

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
        active.setOpaque(true);
        loop.setOpaque(true);
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        AnimationKeyFrame kf = (AnimationKeyFrame) keyFrame;
        freeze.setSelectedItem(kf.isFreeze() ? Toggle.ENABLE : Toggle.DISABLE);
        active.setValue(kf.getActive());
        startFrame.setValue(kf.getStartFrame());
        loop.setSelectedItem(kf.isLoop() ? Toggle.ENABLE : Toggle.DISABLE);
        idle.setValue(kf.getIdle());
        walk.setValue(kf.getWalk());
        run.setValue(kf.getRun());
        walk180.setValue(kf.getWalk180());
        walkRight.setValue(kf.getWalkRight());
        walkLeft.setValue(kf.getWalkLeft());
        idleRight.setValue(kf.getIdleRight());
        idleLeft.setValue(kf.getIdleLeft());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        freeze.setBackground(color);
        active.setBackground(color);
        startFrame.setBackground(color);
        loop.setBackground(color);
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
                        freeze,
                        active,
                        startFrame,
                        loop,
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
        freeze.addItemListener(e ->
        {
            freeze.setBackground(getRed());
        });

        active.addChangeListener(e ->
        {
            active.setBackground(getRed());
        });

        startFrame.addChangeListener(e ->
        {
            startFrame.setBackground(getRed());
        });

        loop.addItemListener(e ->
        {
            loop.setBackground(getRed());
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
        freeze.setSelectedItem(Toggle.DISABLE);
        active.setValue(-1);
        startFrame.setValue(0);
        loop.setSelectedItem(Toggle.DISABLE);
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
