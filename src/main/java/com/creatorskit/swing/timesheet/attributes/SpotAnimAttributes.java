package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.models.CustomModel;
import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.SpotAnimKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.SpotAnimToggle;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class SpotAnimAttributes extends Attributes
{
    private final JSpinner spotAnimId = new JSpinner();
    /** SpotAnim ID vs Custom Model chooser -- mirrors the Model/Projectile cards. */
    private final JComboBox<SpotAnimToggle> modelSource = new JComboBox<>();
    /** Custom model picked when modelSource == CUSTOM_MODEL. */
    private final JComboBox<CustomModel> customModel = new JComboBox<>();
    /** Cache animation id played on the custom model (-1 = none). */
    private final JSpinner animationId = new JSpinner();
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
        modelSource.setSelectedItem(kf.isUseCustomModel() ? SpotAnimToggle.CUSTOM_MODEL : SpotAnimToggle.SPOTANIM_ID);
        // Resolve the custom-model selection for display. After a save reload
        // the live ref is null -- the persistent identity is the comp name, so
        // match it against the combo's items (which ARE the global CustomModel
        // instances) to highlight the right one.
        CustomModel cm = kf.getCustomModel();
        if (cm == null && kf.getCustomModelName() != null && !kf.getCustomModelName().isEmpty())
        {
            for (int i = 0; i < customModel.getItemCount(); i++)
            {
                CustomModel item = customModel.getItemAt(i);
                if (item != null && item.getComp() != null && kf.getCustomModelName().equals(item.getComp().getName()))
                {
                    cm = item;
                    break;
                }
            }
        }
        customModel.setSelectedItem(cm);
        animationId.setValue(kf.getAnimationId());
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
        modelSource.setBackground(color);
        customModel.setBackground(color);
        animationId.setBackground(color);
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
                        modelSource,
                        customModel,
                        animationId,
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

        modelSource.addItemListener(e ->
        {
            modelSource.setBackground(getRed());
        });

        customModel.addItemListener(e ->
        {
            customModel.setBackground(getRed());
        });

        animationId.addChangeListener(e ->
        {
            animationId.setBackground(getRed());
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
        modelSource.setSelectedItem(SpotAnimToggle.SPOTANIM_ID);
        customModel.setSelectedItem(null);
        animationId.setValue(SpotAnimKeyFrame.DEFAULT_ANIMATION_ID);
        loop.setSelectedItem(Toggle.DISABLE);
        height.setValue(92);
        radius.setValue(65);
        animationSpeed.setValue(SpotAnimKeyFrame.DEFAULT_ANIMATION_SPEED);
        super.resetAttributes(resetBackground);
    }
}
