package com.creatorskit.programming;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;

/**
 * Per-slot sound-keyframe driver. Owns the "did I already fire this kf
 * during the current play-through?" bookkeeping so {@code playSoundEffect}
 * runs exactly once per kf-activation, not repeatedly per tick within
 * the kf's window.
 *
 * <p>Scrub never triggers sounds -- the user would hear chirps every
 * time they dragged the playhead, which is intolerable. The
 * Programmer's update loop only calls {@link #onPlayTick} from the
 * play path; scrub uses {@link #onScrub} which only resets the
 * "already-fired" markers so the next play-through re-triggers from
 * wherever the user scrubbed to.
 *
 * <p>Multiple sound kfs at the same tick across slots all play in
 * parallel because {@code Client.playSoundEffect} returns immediately
 * and the engine layers the samples.
 */
@Singleton
public class SoundController
{
    private final Client client;
    private final ClientThread clientThread;
    /** Last kf we fired per slot, so we don't re-trigger within its window. */
    private final Map<KeyFrameType, SoundKeyFrame> lastFired = new EnumMap<>(KeyFrameType.class);

    @Inject
    public SoundController(Client client, ClientThread clientThread)
    {
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Called from Programmer.updateProgramsOnTick once per tick during
     * play. For each of the 4 sound slots: find the kf at or before
     * currentTick; if it's a NEW one (different from last fired), play
     * the sound effect and remember it.
     */
    public synchronized void onPlayTick(double currentTick, com.creatorskit.saves.GlobalKeyFrames store)
    {
        if (store == null) return;
        for (KeyFrameType slot : KeyFrameType.SOUND_TYPES)
        {
            SoundKeyFrame[] kfs = store.getSoundKeyFramesSafe(slot);
            SoundKeyFrame active = findActiveAtOrBefore(kfs, currentTick);
            if (active == null) continue;
            SoundKeyFrame prev = lastFired.get(slot);
            if (prev == active) continue;
            lastFired.put(slot, active);
            final int id = active.getSoundId();
            final int vol = active.getVolume();
            if (id < 0) continue;  // -1 = silence
            // playSoundEffect must run on the client thread. The volume
            // overload plays the sound even when the player has muted SFX
            // -- desirable for cinematics where the timeline asks for a
            // sound regardless of the user's normal mute preference.
            clientThread.invoke(() -> client.playSoundEffect(id, vol));
        }
    }

    /**
     * Called from any scrub / setCurrentTime path. Clears the "already
     * fired" markers for any slot whose new active kf is different from
     * (or none, when the playhead is before all sound kfs in that slot)
     * the one we last fired. The next play-tick after this scrub will
     * re-trigger as if entering the slot fresh.
     */
    public synchronized void onScrub(double currentTick, com.creatorskit.saves.GlobalKeyFrames store)
    {
        if (store == null) return;
        for (KeyFrameType slot : KeyFrameType.SOUND_TYPES)
        {
            SoundKeyFrame[] kfs = store.getSoundKeyFramesSafe(slot);
            SoundKeyFrame active = findActiveAtOrBefore(kfs, currentTick);
            // Park lastFired AT the current active so the next play-tick
            // sees previous == current and DOESN'T fire on the scrub
            // landing tick. The user just stopped scrubbing; they don't
            // want a sound chirp the moment they hit play. The next
            // FORWARD transition (entering the next sound kf after the
            // current one) will fire normally.
            lastFired.put(slot, active);
        }
    }

    /**
     * Called when play stops, so on next play-tick after a restart we
     * start fresh. (Soft reset; scrub also clears via {@link #onScrub}.)
     */
    public synchronized void onPlayStopped()
    {
        // Intentionally no-op -- onScrub-on-stop or the next setCurrentTime
        // will handle reseeding lastFired. Defined for symmetry with
        // a possible future "reset on play start" hook.
    }

    private static SoundKeyFrame findActiveAtOrBefore(SoundKeyFrame[] arr, double tick)
    {
        if (arr == null) return null;
        SoundKeyFrame best = null;
        for (SoundKeyFrame kf : arr)
        {
            if (kf == null) continue;
            if (kf.getTick() > tick + 1e-9) continue;
            if (best == null || kf.getTick() > best.getTick()) best = kf;
        }
        return best;
    }
}
