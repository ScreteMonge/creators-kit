package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Card-state for the per-Character Sound keyframe (slot {@code KeyFrameType.SOUND}).
 * One field only: the cache sound effect id. Playback uses the one-arg
 * {@code Client.playSoundEffect(id)} so the user's in-game SFX volume is
 * respected -- there is no per-kf volume override the way global
 * {@link SoundAttributes} carries.
 *
 * <p>The kf still serializes through {@link SoundKeyFrame}; the volume
 * field on the underlying kf is parked at {@link SoundKeyFrame#DEFAULT_VOLUME}
 * and ignored on read.
 */
@Getter
public class LocalSoundAttributes extends Attributes
{
    private final JSpinner soundId = new JSpinner();

    public LocalSoundAttributes()
    {
        addChangeListeners();
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        SoundKeyFrame kf = (SoundKeyFrame) keyFrame;
        soundId.setValue(kf.getSoundId());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        soundId.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]{soundId};
    }

    @Override
    public void addChangeListeners()
    {
        soundId.addChangeListener(e -> soundId.setBackground(getRed()));
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        soundId.setValue(-1);
        super.resetAttributes(resetBackground);
    }
}
