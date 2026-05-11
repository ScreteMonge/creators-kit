package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.OrientationKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class OriAttributes extends Attributes
{
    private final JSpinner start = new JSpinner();
    private final JSpinner end = new JSpinner();
    private final JSpinner duration = new JSpinner();
    private final JSpinner turnRate = new JSpinner();
    private final JTextField targetCharacterName = new JTextField();

    public OriAttributes()
    {
        addChangeListeners();
        targetCharacterName.setToolTipText("<html>Optional Character name to face while this orientation keyframe is active."
                + "<br>The Object snaps to face that Character every tick."
                + "<br>Leave empty to use the start/end/duration angles below.</html>");
    }

    /**
     * Returns the target Character name as stored in the keyframe — null if blank
     * so we don't roundtrip empty strings.
     */
    public String getTargetCharacterNameValue()
    {
        String text = targetCharacterName.getText();
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
        OrientationKeyFrame kf = (OrientationKeyFrame) keyFrame;
        start.setValue(kf.getStart());
        end.setValue(kf.getEnd());
        duration.setValue(kf.getDuration());
        turnRate.setValue(kf.getTurnRate());
        targetCharacterName.setText(kf.getTargetCharacterName() == null ? "" : kf.getTargetCharacterName());
    }

    public void setBackgroundColours(Color color)
    {
        start.setBackground(color);
        end.setBackground(color);
        duration.setBackground(color);
        turnRate.setBackground(color);
        targetCharacterName.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        start,
                        end,
                        duration,
                        turnRate,
                        targetCharacterName
                };
    }

    @Override
    public void addChangeListeners()
    {
        start.addChangeListener(e ->
        {
            start.setBackground(getRed());
        });

        end.addChangeListener(e ->
        {
            end.setBackground(getRed());
        });

        duration.addChangeListener(e ->
        {
            duration.setBackground(getRed());
        });

        turnRate.addChangeListener(e ->
        {
            turnRate.setBackground(getRed());
        });

        targetCharacterName.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
        {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { targetCharacterName.setBackground(getRed()); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { targetCharacterName.setBackground(getRed()); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { targetCharacterName.setBackground(getRed()); }
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        start.setValue(0);
        end.setValue(0);
        duration.setValue(1.0);
        turnRate.setValue(OrientationKeyFrame.TURN_RATE);
        targetCharacterName.setText("");
        super.resetAttributes(resetBackground);
    }
}
