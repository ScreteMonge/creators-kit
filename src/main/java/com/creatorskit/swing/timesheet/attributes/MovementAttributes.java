package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.programming.MovementType;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.MovementKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.OrientationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class MovementAttributes extends Attributes
{
    private final JComboBox<Toggle> loop = new JComboBox<>();
    private final JSpinner speed = new JSpinner();
    private final JSpinner turnRate = new JSpinner();
    private final JCheckBox autoFaceMovement = new JCheckBox("Auto-face movement");

    public MovementAttributes()
    {
        addChangeListeners();
        loop.setOpaque(true);
        autoFaceMovement.setToolTipText("<html>When on, this Object faces the next path tile each tick like an OSRS NPC."
                + "<br>Overrides any Orientation keyframes that overlap this Movement."
                + "<br>Look-ahead = ceil(speed) tiles (1 walking, 2 running).</html>");
        autoFaceMovement.setFocusable(false);
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        MovementKeyFrame kf = (MovementKeyFrame) keyFrame;
        loop.setSelectedItem(kf.isLoop() ? Toggle.ENABLE : Toggle.DISABLE);
        speed.setValue(kf.getSpeed());
        turnRate.setValue(kf.getTurnRate());
        autoFaceMovement.setSelected(kf.isAutoFaceMovement());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        loop.setBackground(color);
        speed.setBackground(color);
        turnRate.setBackground(color);
        autoFaceMovement.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        loop,
                        speed,
                        turnRate,
                        autoFaceMovement
                };
    }

    @Override
    public void addChangeListeners()
    {
        loop.addItemListener(e ->
        {
            loop.setBackground(getRed());
        });

        speed.addChangeListener(e ->
        {
            speed.setBackground(getRed());
        });

        turnRate.addChangeListener(e ->
        {
            turnRate.setBackground(getRed());
        });

        autoFaceMovement.addActionListener(e ->
        {
            autoFaceMovement.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        loop.setSelectedItem(Toggle.DISABLE);
        speed.setValue(1.0);
        turnRate.setValue(OrientationKeyFrame.TURN_RATE);
        autoFaceMovement.setSelected(false);
        super.resetAttributes(resetBackground);
    }
}