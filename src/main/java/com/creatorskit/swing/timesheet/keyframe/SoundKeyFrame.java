package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Global sound-effect keyframe. Triggers a {@code client.playSoundEffect}
 * call on activation during playback. Lives in {@link com.creatorskit.saves.GlobalKeyFrames}
 * (one of 4 parallel slots: SOUND_1..SOUND_4) so multiple sounds can be
 * layered at the same tick across slots without cutting each other off.
 *
 * <p>Sounds are point-in-time events -- they don't carry a duration field
 * because RuneLite's sound API doesn't expose the cached sample length and
 * the engine plays asynchronously (we hand the id to playSoundEffect and
 * the audio system manages playback). Multiple kfs at the same tick across
 * different slots simply layer because each playSoundEffect call returns
 * immediately.
 */
@Getter
@Setter
public class SoundKeyFrame extends KeyFrame
{
    /** Default sound effect volume. {@link net.runelite.api.SoundEffectVolume#HIGH} = 127. */
    public static final int DEFAULT_VOLUME = 127;

    /** Cache sound effect id. -1 = silence (kf does nothing on activation). */
    private int soundId;
    /** 0-127 sound volume (SoundEffectVolume scale). */
    private int volume;

    public SoundKeyFrame(double tick, KeyFrameType slot, int soundId, int volume)
    {
        super(slot, tick);
        this.soundId = soundId;
        this.volume = volume;
    }
}
