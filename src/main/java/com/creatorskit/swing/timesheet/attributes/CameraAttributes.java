package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.Character;
import com.creatorskit.programming.camera.*;
import com.creatorskit.swing.timesheet.keyframe.subtypes.CameraKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

@Getter
public class CameraAttributes extends Attributes
{
    private final JComboBox<CameraMotionType> motionType = new JComboBox<>();
    private final JComboBox<EaseType> easeType = new JComboBox<>();
    private final JTextField trackingTarget = new JTextField();

    @Setter
    private Character character = null;

    public CameraAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        if (keyFrame == null)
        {
            resetAttributes(true);
            return;
        }

        CameraKeyFrame kf = (CameraKeyFrame) keyFrame;
        CameraScript script = kf.getScript();
        CameraMotionType type = script.getType();
        motionType.setSelectedItem(type);
        easeType.setSelectedItem(script.getEase());

        if (type == CameraMotionType.OBJECT_TRACKING)
        {
            CameraTrackingScript trackingScript = (CameraTrackingScript) script;
            character = trackingScript.getCharacter();
            String name = character == null ? "" : character.getName();
            trackingTarget.setText(name);
        }
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        motionType.setBackground(color);
        easeType.setBackground(color);
        trackingTarget.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        motionType,
                        easeType,
                        trackingTarget
                };
    }

    @Override
    public void addChangeListeners()
    {
        motionType.addItemListener(e ->
        {
            motionType.setBackground(getRed());
        });

        easeType.addItemListener(e ->
        {
            easeType.setBackground(getRed());
        });

        trackingTarget.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                trackingTarget.setBackground(getRed());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                trackingTarget.setBackground(getRed());
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {

            }
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        motionType.setSelectedItem(CameraMotionType.TILE_TRACKING);
        easeType.setSelectedItem(EaseType.SINE);
        character = null;
        trackingTarget.setText("");
        super.resetAttributes(resetBackground);
    }
}
