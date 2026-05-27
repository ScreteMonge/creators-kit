package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.SpotAnimKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class SpotAnimAttributes extends Attributes
{
    private final JSpinner spotAnimId = new JSpinner();
    private final JComboBox<Toggle> loop = new JComboBox<>();
    private final JSpinner height = new JSpinner();
    private final JSpinner radius = new JSpinner();
    /**
     * Animation-speed multiplier over the spotanim's baked cache rate.
     * 1.0 = unchanged; 2.0 = double-time; 0.5 = half-speed. Plumbed into
     * CKObject.setAnimationSpeed via the SpotAnim playback path.
     */
    private final JSpinner animationSpeed = new JSpinner();

    public SpotAnimAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        SpotAnimKeyFrame kf = (SpotAnimKeyFrame) keyFrame;
        spotAnimId.setValue(kf.getSpotAnimId());
        loop.setSelectedItem(kf.isLoop() ? Toggle.ENABLE : Toggle.DISABLE);
        height.setValue(kf.getHeight());
        radius.setValue(kf.getRadius());
        // Old saves predating the field deserialize as 0.0; surface the
        // default (1.0) in the spinner instead so the user sees the
        // effective speed rather than a misleading "0".
        double speed = kf.getAnimationSpeed();
        animationSpeed.setValue(speed > 0 ? speed : SpotAnimKeyFrame.DEFAULT_ANIMATION_SPEED);
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        spotAnimId.setBackground(color);
        loop.setBackground(color);
        height.setBackground(color);
        radius.setBackground(color);
        animationSpeed.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        spotAnimId,
                        loop,
                        height,
                        radius,
                        animationSpeed
                };
    }

    @Override
    public void addChangeListeners()
    {
        spotAnimId.addChangeListener(e ->
        {
            spotAnimId.setBackground(getRed());
        });

        loop.addItemListener(e ->
        {
            loop.setBackground(getRed());
        });

        height.addChangeListener(e ->
        {
            height.setBackground(getRed());
        });

        radius.addChangeListener(e ->
        {
            radius.setBackground(getRed());
        });

        animationSpeed.addChangeListener(e ->
        {
            animationSpeed.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        spotAnimId.setValue(-1);
        loop.setSelectedItem(Toggle.DISABLE);
        height.setValue(92);
        radius.setValue(65);
        animationSpeed.setValue(SpotAnimKeyFrame.DEFAULT_ANIMATION_SPEED);
        super.resetAttributes(resetBackground);
    }
}
