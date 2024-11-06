package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.SpawnKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.SpawnToggle;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;

@Getter
public class SpawnAttributes
{
    private final Color green = new Color(42, 77, 26);
    private final Color yellow = new Color(91, 80, 29);
    private final Color red = new Color(101, 66, 29);

    private final JComboBox<SpawnToggle> spawn = new JComboBox<>();

    public SpawnAttributes()
    {
        addChangeListeners();
        spawn.setOpaque(true);
    }

    public void setAttributes(SpawnKeyFrame kf)
    {
        spawn.setSelectedItem(kf.isSpawnActive() ? SpawnToggle.SPAWN_ACTIVE : SpawnToggle.SPAWN_INACTIVE);
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
                color = yellow;
                break;
            case OFF_KEYFRAME:
                color = green;
        }

        spawn.setBackground(color);
    }

    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        spawn
                };
    }

    public void addChangeListeners()
    {
        spawn.addItemListener(e ->
        {
            spawn.setBackground(red.brighter());
        });
    }
}
