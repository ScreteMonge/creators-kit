package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ProjectileKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class ProjectileAttributes extends Attributes
{
    private final JSpinner projectileId = new JSpinner();
    private final JTextField target = new JTextField();
    private final JSpinner startHeight = new JSpinner();
    private final JSpinner endHeight = new JSpinner();
    private final JSpinner slope = new JSpinner();
    private final JSpinner durationTicks = new JSpinner();
    private final JCheckBox faceTrajectory = new JCheckBox("Face trajectory");

    public ProjectileAttributes()
    {
        addChangeListeners();
        target.setToolTipText("<html>Target(s) the projectile flies to. Accepts:"
                + "<br>- A single Character name (\"Player\")"
                + "<br>- A comma-separated list (\"Player, NPC1, NPC2\")"
                + "<br>- \"folder:Foldername\" to fan out to every Character under that folder"
                + "<br>- \"f[Folder1, Folder2, ...]\" to fan out to multiple folders at once"
                + "<br>Empty = no projectile fires.</html>");
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
        target.setText(kf.getTarget() == null ? "" : kf.getTarget());
        startHeight.setValue(kf.getStartHeight());
        endHeight.setValue(kf.getEndHeight());
        slope.setValue(kf.getSlope());
        durationTicks.setValue(kf.getDurationTicks());
        faceTrajectory.setSelected(kf.isFaceTrajectory());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        projectileId.setBackground(color);
        target.setBackground(color);
        startHeight.setBackground(color);
        endHeight.setBackground(color);
        slope.setBackground(color);
        durationTicks.setBackground(color);
        faceTrajectory.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        projectileId,
                        target,
                        startHeight,
                        endHeight,
                        slope,
                        durationTicks,
                        faceTrajectory
                };
    }

    @Override
    public void addChangeListeners()
    {
        projectileId.addChangeListener(e -> projectileId.setBackground(getRed()));
        startHeight.addChangeListener(e -> startHeight.setBackground(getRed()));
        endHeight.addChangeListener(e -> endHeight.setBackground(getRed()));
        slope.addChangeListener(e -> slope.setBackground(getRed()));
        durationTicks.addChangeListener(e -> durationTicks.setBackground(getRed()));
        faceTrajectory.addItemListener(e -> faceTrajectory.setBackground(getRed()));

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
        target.setText("");
        startHeight.setValue(ProjectileKeyFrame.DEFAULT_START_HEIGHT);
        endHeight.setValue(ProjectileKeyFrame.DEFAULT_END_HEIGHT);
        slope.setValue(ProjectileKeyFrame.DEFAULT_SLOPE);
        durationTicks.setValue(ProjectileKeyFrame.DEFAULT_DURATION);
        faceTrajectory.setSelected(ProjectileKeyFrame.DEFAULT_FACE_TRAJECTORY);
        super.resetAttributes(resetBackground);
    }
}
