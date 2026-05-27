package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the per-Character Sound keyframe slots (SOUND /
 * SOUND_LOCAL_2 / 3 / 4). Two fields: cache sound id, per-kf volume.
 *
 * <p>Per-kf volume is treated as a MULTIPLIER over the user's in-game
 * SFX preference -- {@code SoundController} scales the kf's
 * {@code volume} field by the live {@code SoundEffectVolume} setting
 * before passing the result to {@code Client.playSoundEffect(id, vol)}.
 * Default is {@link SoundKeyFrame#DEFAULT_VOLUME} (127 = max), so a
 * fresh kf plays at exactly the user's in-game volume.
 */
@Getter
public class LocalSoundAttributes extends Attributes
{
    private final JSpinner soundId = new JSpinner();
    private final JSpinner volume = new JSpinner();

    public LocalSoundAttributes()
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
