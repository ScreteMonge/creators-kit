package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.SpawnKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.Toggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class SpawnAttributes extends Attributes
{
    private final JComboBox<Toggle> spawn = new JComboBox<>();

    public SpawnAttributes()
    {
        addChangeListeners();
        spawn.setOpaque(true);
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        SpawnKeyFrame kf = (SpawnKeyFrame) keyFrame;
        spawn.setSelectedItem(kf.isSpawnActive() ? Toggle.ENABLE : Toggle.DISABLE);
    }

    @Override
    public void setBackgroundColours(Color color)
    {
       spawn.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        spawn
                };
    }

    @Override
    public void addChangeListeners()
    {
        spawn.addItemListener(e ->
        {
            spawn.setBackground(getRed().brighter());
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        spawn.setSelectedItem(Toggle.ENABLE);
        super.resetAttributes(resetBackground);
    }
}
