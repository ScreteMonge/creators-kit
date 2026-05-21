package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;

@Getter
public class Attributes
{
    private final Color green = new Color(42, 77, 26);
    private final Color yellow = new Color(91, 80, 29);
    private final Color red = new Color(101, 66, 29);

    public void setAttributes(KeyFrame kf)
    {

    }

    public void setBackgroundColours(KeyFrameState keyFrameState)
    {
        // Every state collapses to DARKER_GRAY now -- the old yellow (on
        // keyframe) / green (off keyframe) distinction was confusing because
        // a field could be one of three colours for reasons unrelated to the
        // user's most recent action. Edits surface via a transient green
        // fade-back driven from AttributePanel.wireAutoUpdate instead, which
        // is the cue the user actually wants: "did my change land?"
        setBackgroundColours(ColorScheme.DARKER_GRAY_COLOR);
    }

    public void setBackgroundColours(Color color)
    {

    }

    public JComponent[] getAllComponents()
    {
        return new JComponent[0];
    }

    public void addChangeListeners()
    {

    }

    public void resetAttributes(boolean resetBackground)
    {
        if (resetBackground)
        {
            setBackgroundColours(KeyFrameState.EMPTY);
        }
    }
}