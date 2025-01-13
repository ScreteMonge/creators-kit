package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.TextKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

@Getter
public class TextAttributes extends Attributes
{
    private final JComboBox<Toggle> enableBox = new JComboBox<>();
    private final JTextArea text = new JTextArea("");
    private final JSpinner height = new JSpinner();

    public TextAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        TextKeyFrame kf = (TextKeyFrame) keyFrame;
        enableBox.setSelectedItem(kf.isEnabled() ? Toggle.ENABLE : Toggle.DISABLE);
        text.setText(kf.getText());
        height.setValue(kf.getHeight());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        enableBox.setBackground(color);
        text.setBackground(color);
        height.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        enableBox,
                        text,
                        height
                };
    }

    @Override
    public void addChangeListeners()
    {
        enableBox.addItemListener(e ->
        {
            enableBox.setBackground(getRed());
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

        height.addChangeListener(e ->
        {
            height.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes()
    {
        enableBox.setSelectedItem(Toggle.DISABLE);
        text.setText("");
        height.setValue(60);
        setBackgroundColours(KeyFrameState.EMPTY);
    }
}
