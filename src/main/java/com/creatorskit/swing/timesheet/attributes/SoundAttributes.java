package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for a Sound 1..4 keyframe. Two fields: the cache sound effect
 * id and a 0-127 volume. RuneLite's Client.playSoundEffect plays
 * asynchronously, so a "duration" field isn't meaningful here -- multiple
 * sound kfs at the same tick across the 4 parallel slots layer naturally.
 */
@Getter
public class SoundAttributes extends Attributes
{
    private final JSpinner soundId = new JSpinner();
    private final JSpinner volume = new JSpinner();

    public SoundAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        SoundKeyFrame kf = (SoundKeyFrame) keyFrame;
        soundId.setValue(kf.getSoundId());
        volume.setValue(kf.getVolume());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        soundId.setBackground(color);
        volume.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]{soundId, volume};
    }

    @Override
    public void addChangeListeners()
    {
        soundId.addChangeListener(e -> soundId.setBackground(getRed()));
        volume.addChangeListener(e -> volume.setBackground(getRed()));
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        soundId.setValue(-1);
        volume.setValue(SoundKeyFrame.DEFAULT_VOLUME);
        super.resetAttributes(resetBackground);
    }
}
