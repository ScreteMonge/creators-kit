package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.TextKeyFrame;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

@Getter
public class TextAttributes extends Attributes
{
    private final JSpinner duration = new JSpinner();
    private final JTextArea text = new JTextArea("");

    public TextAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        TextKeyFrame kf = (TextKeyFrame) keyFrame;
        duration.setValue(kf.getDuration());
        text.setText(kf.getText());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        duration.setBackground(color);
        text.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        duration,
                        text
                };
    }

    @Override
    public void addChangeListeners()
    {
        duration.addChangeListener(e ->
        {
            duration.setBackground(getRed());
        });

        text.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                text.setBackground(getRed());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                text.setBackground(getRed());
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {

            }
        });
    }

    @Override
    public void resetAttributes()
    {
        duration.setValue(5);
        text.setText("");
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
