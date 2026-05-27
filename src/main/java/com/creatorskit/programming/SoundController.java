package com.creatorskit.programming;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumMap;
import java.util.IdentityHashMap;
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
    /** Last kf we fired per global slot, so we don't re-trigger within its window. */
    private final Map<KeyFrameType, SoundKeyFrame> lastFired = new EnumMap<>(KeyFrameType.class);
    /**
     * Per-Character "already fired" tracker for the four local SOUND slots.
     * Nested: outer key is the Character (identity-keyed so renames /
     * mutated copies don't shadow each other); inner key is the slot
     * type (SOUND / SOUND_LOCAL_2 / 3 / 4). Each slot tracks its own
     * lastFired independently so layered sounds at the same tick across
     * slots all play once.
     */
    private final Map<com.creatorskit.Character, EnumMap<KeyFrameType, SoundKeyFrame>> lastFiredLocal = new IdentityHashMap<>();

    private SoundKeyFrame getLastFiredLocal(com.creatorskit.Character ch, KeyFrameType slot)
    {
        EnumMap<KeyFrameType, SoundKeyFrame> m = lastFiredLocal.get(ch);
        return m == null ? null : m.get(slot);
    }

    private void putLastFiredLocal(com.creatorskit.Character ch, KeyFrameType slot, SoundKeyFrame kf)
    {
        lastFiredLocal.computeIfAbsent(ch, k -> new EnumMap<>(KeyFrameType.class)).put(slot, kf);
    }

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
            // Compare by TICK rather than object identity. The previous
            // `prev == active` check broke whenever the kf-array contents
            // got swapped for fresh instances even though the authored
            // kf was logically the same -- the most common offender being
            // save reload (the GlobalKeyFrames load path replaces the kf
            // arrays wholesale, so lastFired keeps pointing at orphan
            // objects). Symptom the user saw: long area sound replayed
            // twice on press-play, the first cut off when the second
            // started. Ticks are unique per slot (the GlobalKeyFrames.add
            // / insertSorted path overwrites same-tick entries), so tick
            // equality safely captures "we already fired this kf."
            SoundKeyFrame prev = lastFired.get(slot);
            if (prev != null && Double.compare(prev.getTick(), active.getTick()) == 0) continue;
            lastFired.put(slot, active);
            final int id = active.getSoundId();
            final int vol = active.getVolume();
            if (id < 0) continue;  // -1 = silence
            // Per-kf volume is a multiplier over the in-game Area Sound
            // preference -- the in-game slider is the master, so muting
            // SFX in-game silences the kf regardless of its stored
            // volume. Effective = kfVol * areaSoundPref / 127.
            clientThread.invoke(() -> playScaled(id, vol, areaSoundPreferenceVolume()));
        }
    }

    /**
     * Reads the user's in-game Area Sound preference (0-127) so per-kf
     * volume can be scaled. Defaults to max if the preferences object
     * is unavailable, matching the legacy "play at the kf's stored
     * volume" behaviour rather than going silent on a null read.
     */
    private int areaSoundPreferenceVolume()
    {
        try
        {
            return client.getPreferences().getAreaSoundEffectVolume();
        }
        catch (Throwable t) { return 127; }
    }

    /**
     * Reads the user's in-game Sound Effects preference (0-127) for the
     * per-Character (local) sound playback path. Falls back to max if
     * unreadable.
     */
    private int sfxPreferenceVolume()
    {
        try
        {
            return client.getPreferences().getSoundEffectVolume();
        }
        catch (Throwable t) { return 127; }
    }

    /**
     * Computes {@code (kfVolume * inGameVolume) / 127} and fires
     * playSoundEffect with the resulting volume. Skips the call when
     * the scaled volume is 0 -- that's the in-game-muted case, where
     * we want full silence rather than a zero-volume API ping.
     */
    private void playScaled(int id, int kfVolume, int inGameVolume)
    {
        int effective = (int) Math.round(kfVolume * (inGameVolume / 127.0));
        if (effective <= 0) return;
        if (effective > 127) effective = 127;
        client.playSoundEffect(id, effective);
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

    /**
     * Per-Character SOUND playback. Iterates every active Character once per
     * play-tick: for each, find the latest SOUND kf at-or-before {@code
     * currentTick}; if it's a new one (different identity from the last we
     * fired for that Character), play it and remember it.
     *
     * <p>Uses the one-arg {@code Client.playSoundEffect(int id)} so the
     * user's in-game SFX volume applies. The volume field on the kf is
     * ignored here -- for an override that bypasses the user's volume,
     * authors should use a global Area Sound 1/2/3/4 slot instead.
     */
    public synchronized void onPlayTickLocal(double currentTick, java.util.List<com.creatorskit.Character> characters)
    {
        if (characters == null) return;
        // Snapshot before iterating -- the caller's list (plugin.getCharacters())
        // can mutate mid-walk when a tick callback fires while a Character is
        // being constructed (e.g. toolbox-open, setup-load). ArrayList's
        // iterator throws ConcurrentModificationException on detection;
        // copying once per call is cheap and removes the race.
        for (com.creatorskit.Character ch : new java.util.ArrayList<>(characters))
        {
            if (ch == null) continue;
            // Walk all 4 local sound slots so layered sounds (same tick,
            // different slot) all play. Each slot tracks its own
            // lastFired so re-entering the slot's window doesn't re-fire.
            for (KeyFrameType slot : KeyFrameType.LOCAL_SOUND_TYPES)
            {
                KeyFrame[] kfs = ch.getKeyFrames(slot);
                SoundKeyFrame active = findActiveAtOrBefore(kfs, currentTick);
                if (active == null) continue;
                // Tick-based dedup, same rationale as onPlayTick: object
                // identity breaks when the per-Character SOUND array gets
                // rebuilt by save-load (CreatorsPanel's slot-routing pass)
                // or any other path that materialises fresh kf instances.
                SoundKeyFrame prev = getLastFiredLocal(ch, slot);
                if (prev != null && Double.compare(prev.getTick(), active.getTick()) == 0) continue;
                putLastFiredLocal(ch, slot, active);
                final int id = active.getSoundId();
                final int vol = active.getVolume();
                if (id < 0) continue;  // -1 = silence
                // Per-kf volume scales the user's in-game SFX preference.
                // The in-game slider is master, so a muted SFX setting
                // silences this kf regardless of its stored volume.
                clientThread.invoke(() -> playScaled(id, vol, sfxPreferenceVolume()));
            }
        }
    }

    /**
     * Mirror of {@link #onScrub} for per-Character SOUND. Parks each
     * (Character, slot) pair's lastFired at the current active kf so
     * the next play-tick doesn't chirp on the scrub-landing tick.
     */
    public synchronized void onScrubLocal(double currentTick, java.util.List<com.creatorskit.Character> characters)
    {
        if (characters == null) return;
        // Snapshot before iterating; see onPlayTickLocal for the race.
        for (com.creatorskit.Character ch : new java.util.ArrayList<>(characters))
        {
            if (ch == null) continue;
            for (KeyFrameType slot : KeyFrameType.LOCAL_SOUND_TYPES)
            {
                KeyFrame[] kfs = ch.getKeyFrames(slot);
                SoundKeyFrame active = findActiveAtOrBefore(kfs, currentTick);
                putLastFiredLocal(ch, slot, active);
            }
        }
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

    /**
     * Local-Sound variant of {@link #findActiveAtOrBefore} that accepts the
     * generic {@code KeyFrame[]} returned by {@code Character.getKeyFrames}.
     * Same scan, just with one cast at the end -- the per-Character SOUND
     * track is always a SoundKeyFrame[] in practice.
     */
    private static SoundKeyFrame findActiveAtOrBefore(KeyFrame[] arr, double tick)
    {
        if (arr == null) return null;
        SoundKeyFrame best = null;
        for (KeyFrame kf : arr)
        {
            if (kf == null) continue;
            if (kf.getTick() > tick + 1e-9) continue;
            if (best == null || kf.getTick() > best.getTick()) best = (SoundKeyFrame) kf;
        }
        return best;
    }
}
