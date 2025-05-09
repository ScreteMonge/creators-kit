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
        Color color;

        switch (keyFrameState)
        {
            default:
            case EMPTY:
                color = ColorScheme.DARKER_GRAY_COLOR;
                break;
            case ON_KEYFRAME:
                color = getYellow();
                break;
            case OFF_KEYFRAME:
                color = getGreen();
        }

        setBackgroundColours(color);
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
