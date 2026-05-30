package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.models.CustomModel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ProjectileKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.ProjectileToggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class ProjectileAttributes extends Attributes
{
    private final JSpinner projectileId = new JSpinner();
    /** SpotAnim ID vs Custom Model chooser -- mirrors the Model card's modelOverride. */
    private final JComboBox<ProjectileToggle> projectileType = new JComboBox<>();
    /** Custom model picked when projectileType == CUSTOM_MODEL. */
    private final JComboBox<CustomModel> customModel = new JComboBox<>();
    /** Cache animation id played on the custom model (-1 = none). */
    private final JSpinner animationId = new JSpinner();
    private final JTextField target = new JTextField();
    private final JSpinner startX = new JSpinner();
    private final JSpinner startY = new JSpinner();
    private final JSpinner startHeight = new JSpinner();
    private final JSpinner endHeight = new JSpinner();
    private final JSpinner slope = new JSpinner();
    private final JSpinner durationTicks = new JSpinner();
    private final JCheckBox faceTrajectory = new JCheckBox("Face trajectory");
    private final JSpinner radius = new JSpinner();

    public ProjectileAttributes()
    {
        addChangeListeners();
        target.setToolTipText("<html>Target(s) the projectile flies to.<br>"
                + "Accepts a name, comma-separated names, <b>folder:Name</b>, or <b>f[F1, F2, ...]</b>.</html>");
    }

    /**
     * Returns the target string, trimmed; null if blank so we don't roundtrip empty.
     */
    public String getTargetValue()
    {
        String text = target.getText();
        if (text == null)
        {
            return null;
        }
        text = text.trim();
        return text.isEmpty() ? null : text;
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        ProjectileKeyFrame kf = (ProjectileKeyFrame) keyFrame;
        projectileId.setValue(kf.getProjectileId());
        projectileType.setSelectedItem(kf.isUseCustomModel() ? ProjectileToggle.CUSTOM_MODEL : ProjectileToggle.SPOTANIM_ID);
        // Show the right custom-model selection. After a save reload the live
        // ref (kf.getCustomModel()) is null -- the persistent identity is the
        // comp name, so resolve it against the combo's items (which ARE the
        // global CustomModel instances). Selecting by name gives the exact
        // instance so the combo highlights correctly.
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
        target.setText(kf.getTarget() == null ? "" : kf.getTarget());
        startX.setValue(kf.getStartX());
        startY.setValue(kf.getStartY());
        startHeight.setValue(kf.getStartHeight());
        endHeight.setValue(kf.getEndHeight());
        slope.setValue(kf.getSlope());
        durationTicks.setValue(kf.getDurationTicks());
        faceTrajectory.setSelected(kf.isFaceTrajectory());
        // Old saves predate the field -> Gson fills it with 0; show the
        // default radius value instead of "0" so the spinner reads
        // truthfully ("60 = renderer default") and an inadvertent edit
        // away from 0 doesn't visually look like a different value.
        radius.setValue(kf.getRadius() > 0 ? kf.getRadius() : ProjectileKeyFrame.DEFAULT_RADIUS);
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        projectileId.setBackground(color);
        projectileType.setBackground(color);
        customModel.setBackground(color);
        animationId.setBackground(color);
        target.setBackground(color);
        startX.setBackground(color);
        startY.setBackground(color);
        startHeight.setBackground(color);
        endHeight.setBackground(color);
        slope.setBackground(color);
        durationTicks.setBackground(color);
        faceTrajectory.setBackground(color);
        radius.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        projectileId,
                        projectileType,
                        customModel,
                        animationId,
                        target,
                        startX,
                        startY,
                        startHeight,
                        endHeight,
                        slope,
                        durationTicks,
                        faceTrajectory,
                        radius
                };
    }

    @Override
    public void addChangeListeners()
    {
        projectileId.addChangeListener(e -> projectileId.setBackground(getRed()));
        projectileType.addItemListener(e -> projectileType.setBackground(getRed()));
        customModel.addItemListener(e -> customModel.setBackground(getRed()));
        animationId.addChangeListener(e -> animationId.setBackground(getRed()));
        startX.addChangeListener(e -> startX.setBackground(getRed()));
        startY.addChangeListener(e -> startY.setBackground(getRed()));
        startHeight.addChangeListener(e -> startHeight.setBackground(getRed()));
        endHeight.addChangeListener(e -> endHeight.setBackground(getRed()));
        slope.addChangeListener(e -> slope.setBackground(getRed()));
        durationTicks.addChangeListener(e -> durationTicks.setBackground(getRed()));
        faceTrajectory.addItemListener(e -> faceTrajectory.setBackground(getRed()));
        radius.addChangeListener(e -> radius.setBackground(getRed()));

        target.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
        {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { target.setBackground(getRed()); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { target.setBackground(getRed()); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { target.setBackground(getRed()); }
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        projectileId.setValue(ProjectileKeyFrame.DEFAULT_PROJECTILE_ID);
        projectileType.setSelectedItem(ProjectileToggle.SPOTANIM_ID);
        customModel.setSelectedItem(null);
        animationId.setValue(ProjectileKeyFrame.DEFAULT_ANIMATION_ID);
        target.setText("");
        startX.setValue(ProjectileKeyFrame.DEFAULT_START_X);
        startY.setValue(ProjectileKeyFrame.DEFAULT_START_Y);
        startHeight.setValue(ProjectileKeyFrame.DEFAULT_START_HEIGHT);
        endHeight.setValue(ProjectileKeyFrame.DEFAULT_END_HEIGHT);
        slope.setValue(ProjectileKeyFrame.DEFAULT_SLOPE);
        durationTicks.setValue(ProjectileKeyFrame.DEFAULT_DURATION);
        faceTrajectory.setSelected(ProjectileKeyFrame.DEFAULT_FACE_TRAJECTORY);
        radius.setValue(ProjectileKeyFrame.DEFAULT_RADIUS);
        super.resetAttributes(resetBackground);
    }
}
