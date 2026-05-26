package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.CameraEaseType;
import com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.CustomEasingCurve;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the Camera keyframe. Mirrors mlgudi/keyframe-camera's per-
 * keyframe edit panel: focal X/Y/Z, pitch + yaw (degrees in the UI so users
 * don't reason in radians; converted at apply time), zoom scale, easing curve,
 * and duration to the next keyframe.
 */
@Getter
public class CameraAttributes extends Attributes
{
    private final JSpinner focalX = new JSpinner();
    private final JSpinner focalY = new JSpinner();
    private final JSpinner focalZ = new JSpinner();
    /** Pitch in DEGREES for the UI (-90..90). Converted to radians on save. */
    private final JSpinner pitchDeg = new JSpinner();
    /** Yaw in DEGREES for the UI (0..360). Converted to radians on save. */
    private final JSpinner yawDeg = new JSpinner();
    private final JSpinner scale = new JSpinner();
    private final JSpinner durationTicks = new JSpinner();
    private final JComboBox<CameraEaseType> ease = new JComboBox<>(CameraEaseType.values());

    /**
     * "Capture from current camera" button. Wired up in AttributePanel.setupCameraCard
     * because it needs Client + the live camera state.
     */
    private final JButton capture = new JButton("Capture from camera");

    /**
     * In-flight curve produced by the CurveEditorDialog when the user picks
     * "Custom..." and clicks Select. The CAMERA branch of applyEditsTo reads
     * this when ease == CUSTOM and writes it into the new kf's customCurve,
     * then clears it on the next setAttributes (= panel reload). Lets the
     * dialog's output flow through the existing edit-commit pipeline without
     * inventing a parallel write path.
     */
    @Setter
    private CustomEasingCurve pendingCustomCurve;

    /**
     * Last ease the user actually committed (non-CUSTOM). The CUSTOM combo
     * pick opens a dialog; clicking Discard there needs to revert the combo
     * to whatever was selected before, which is this value. Updated each
     * time setAttributes loads a non-CUSTOM kf or the user picks a non-
     * CUSTOM item from the combo.
     */
    @Setter
    private CameraEaseType lastCommittedEase = CameraEaseType.LINEAR;

    public CameraAttributes()
    {
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        CameraKeyFrame kf = (CameraKeyFrame) keyFrame;
        focalX.setValue(kf.getFocalX());
        focalY.setValue(kf.getFocalY());
        focalZ.setValue(kf.getFocalZ());
        pitchDeg.setValue(Math.toDegrees(kf.getPitch()));
        yawDeg.setValue(Math.toDegrees(kf.getYaw()));
        scale.setValue(kf.getScale());
        durationTicks.setValue(kf.getDurationTicks());
        ease.setSelectedItem(kf.getEase());
        // Loading a fresh kf == start of a new edit batch. The pending
        // curve from a prior dialog session shouldn't bleed into the kf
        // being shown now.
        pendingCustomCurve = null;
        if (kf.getEase() != CameraEaseType.CUSTOM)
        {
            lastCommittedEase = kf.getEase();
        }
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        focalX.setBackground(color);
        focalY.setBackground(color);
        focalZ.setBackground(color);
        pitchDeg.setBackground(color);
        yawDeg.setBackground(color);
        scale.setBackground(color);
        durationTicks.setBackground(color);
        ease.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]{focalX, focalY, focalZ, pitchDeg, yawDeg, scale, durationTicks, ease};
    }

    @Override
    public void addChangeListeners()
    {
        focalX.addChangeListener(e -> focalX.setBackground(getRed()));
        focalY.addChangeListener(e -> focalY.setBackground(getRed()));
        focalZ.addChangeListener(e -> focalZ.setBackground(getRed()));
        pitchDeg.addChangeListener(e -> pitchDeg.setBackground(getRed()));
        yawDeg.addChangeListener(e -> yawDeg.setBackground(getRed()));
        scale.addChangeListener(e -> scale.setBackground(getRed()));
        durationTicks.addChangeListener(e -> durationTicks.setBackground(getRed()));
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        focalX.setValue(CameraKeyFrame.DEFAULT_FOCAL_X);
        focalY.setValue(CameraKeyFrame.DEFAULT_FOCAL_Y);
        focalZ.setValue(CameraKeyFrame.DEFAULT_FOCAL_Z);
        pitchDeg.setValue(Math.toDegrees(CameraKeyFrame.DEFAULT_PITCH));
        yawDeg.setValue(Math.toDegrees(CameraKeyFrame.DEFAULT_YAW));
        scale.setValue(CameraKeyFrame.DEFAULT_SCALE);
        durationTicks.setValue(CameraKeyFrame.DEFAULT_DURATION);
        ease.setSelectedItem(CameraKeyFrame.DEFAULT_EASE);
        super.resetAttributes(resetBackground);
    }
}
