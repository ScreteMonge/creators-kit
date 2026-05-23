package com.creatorskit.saves;

import com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Top-level storage for the three global keyframe types -- Camera, Screen Fade,
 * Screen Shake -- that have a global rendering effect rather than a per-Character
 * one. Pulled out of Character storage in the Phase 2 camera refactor so the
 * user can author global timeline effects without first picking a "scene
 * controller" Character to dump them on.
 *
 * <p>Saved as a top-level field on {@link SetupSave}. Old saves (pre-Phase 2)
 * have a null GlobalKeyFrames field and the per-Character globals fields
 * populated; the load path migrates them in via
 * {@code CreatorsPanel.migrateLegacyGlobalsIfNeeded}.
 *
 * <p>The arrays are nullable; consumers treat null and empty the same way.
 * Gson defaults missing fields to null which is what older saves get.
 */
@Getter
@Setter
public class GlobalKeyFrames
{
    private CameraKeyFrame[] cameraKeyFrames;
    private ScreenFadeKeyFrame[] screenFadeKeyFrames;
    private ScreenShakeKeyFrame[] screenShakeKeyFrames;
    /** 4 parallel sound slots so multiple sounds can play layered at the same tick. */
    private SoundKeyFrame[] sound1KeyFrames;
    private SoundKeyFrame[] sound2KeyFrames;
    private SoundKeyFrame[] sound3KeyFrames;
    private SoundKeyFrame[] sound4KeyFrames;

    public GlobalKeyFrames()
    {
        this.cameraKeyFrames = new CameraKeyFrame[0];
        this.screenFadeKeyFrames = new ScreenFadeKeyFrame[0];
        this.screenShakeKeyFrames = new ScreenShakeKeyFrame[0];
        this.sound1KeyFrames = new SoundKeyFrame[0];
        this.sound2KeyFrames = new SoundKeyFrame[0];
        this.sound3KeyFrames = new SoundKeyFrame[0];
        this.sound4KeyFrames = new SoundKeyFrame[0];
    }

    public CameraKeyFrame[] getCameraKeyFramesSafe()
    {
        return cameraKeyFrames == null ? new CameraKeyFrame[0] : cameraKeyFrames;
    }

    public ScreenFadeKeyFrame[] getScreenFadeKeyFramesSafe()
    {
        return screenFadeKeyFrames == null ? new ScreenFadeKeyFrame[0] : screenFadeKeyFrames;
    }

    public ScreenShakeKeyFrame[] getScreenShakeKeyFramesSafe()
    {
        return screenShakeKeyFrames == null ? new ScreenShakeKeyFrame[0] : screenShakeKeyFrames;
    }

    public SoundKeyFrame[] getSound1KeyFramesSafe() { return sound1KeyFrames == null ? new SoundKeyFrame[0] : sound1KeyFrames; }
    public SoundKeyFrame[] getSound2KeyFramesSafe() { return sound2KeyFrames == null ? new SoundKeyFrame[0] : sound2KeyFrames; }
    public SoundKeyFrame[] getSound3KeyFramesSafe() { return sound3KeyFrames == null ? new SoundKeyFrame[0] : sound3KeyFrames; }
    public SoundKeyFrame[] getSound4KeyFramesSafe() { return sound4KeyFrames == null ? new SoundKeyFrame[0] : sound4KeyFrames; }

    /** Returns the safe array for the matching SOUND_x type, or null if the type isn't a sound. */
    public SoundKeyFrame[] getSoundKeyFramesSafe(KeyFrameType type)
    {
        switch (type)
        {
            case SOUND_1: return getSound1KeyFramesSafe();
            case SOUND_2: return getSound2KeyFramesSafe();
            case SOUND_3: return getSound3KeyFramesSafe();
            case SOUND_4: return getSound4KeyFramesSafe();
            default: return null;
        }
    }

    public void setSoundKeyFrames(KeyFrameType type, SoundKeyFrame[] arr)
    {
        switch (type)
        {
            case SOUND_1: sound1KeyFrames = arr; break;
            case SOUND_2: sound2KeyFrames = arr; break;
            case SOUND_3: sound3KeyFrames = arr; break;
            case SOUND_4: sound4KeyFrames = arr; break;
            default: break;
        }
    }

    /**
     * Inserts {@code kf} into the appropriate array, replacing any existing
     * keyframe at the same tick. Returns the replaced keyframe so the caller
     * can record it on the undo stack, or null if nothing was displaced.
     *
     * <p>Used by the TimeSheetPanel when the user adds a global keyframe with
     * no Character selected -- the central store becomes the direct write
     * target instead of routing through Character.addKeyFrame.
     */
    public KeyFrame add(KeyFrame kf)
    {
        if (kf == null) return null;
        switch (kf.getKeyFrameType())
        {
            case CAMERA:
            {
                CameraKeyFrame[] existing = getCameraKeyFramesSafe();
                CameraKeyFrame replaced = (CameraKeyFrame) findAtTick(existing, kf.getTick());
                cameraKeyFrames = insertSorted(existing, (CameraKeyFrame) kf, new CameraKeyFrame[0]);
                return replaced;
            }
            case SCREEN_FADE:
            {
                ScreenFadeKeyFrame[] existing = getScreenFadeKeyFramesSafe();
                ScreenFadeKeyFrame replaced = (ScreenFadeKeyFrame) findAtTick(existing, kf.getTick());
                screenFadeKeyFrames = insertSorted(existing, (ScreenFadeKeyFrame) kf, new ScreenFadeKeyFrame[0]);
                return replaced;
            }
            case SCREEN_SHAKE:
            {
                ScreenShakeKeyFrame[] existing = getScreenShakeKeyFramesSafe();
                ScreenShakeKeyFrame replaced = (ScreenShakeKeyFrame) findAtTick(existing, kf.getTick());
                screenShakeKeyFrames = insertSorted(existing, (ScreenShakeKeyFrame) kf, new ScreenShakeKeyFrame[0]);
                return replaced;
            }
            case SOUND_1:
            case SOUND_2:
            case SOUND_3:
            case SOUND_4:
            {
                SoundKeyFrame[] existing = getSoundKeyFramesSafe(kf.getKeyFrameType());
                SoundKeyFrame replaced = (SoundKeyFrame) findAtTick(existing, kf.getTick());
                setSoundKeyFrames(kf.getKeyFrameType(),
                        insertSorted(existing, (SoundKeyFrame) kf, new SoundKeyFrame[0]));
                return replaced;
            }
            default:
                return null;
        }
    }

    /**
     * Removes a specific keyframe instance from the appropriate array.
     * No-op if the keyframe isn't in the store.
     */
    public void remove(KeyFrame kf)
    {
        if (kf == null) return;
        switch (kf.getKeyFrameType())
        {
            case CAMERA:
                cameraKeyFrames = ArrayUtils.removeElement(getCameraKeyFramesSafe(), kf);
                break;
            case SCREEN_FADE:
                screenFadeKeyFrames = ArrayUtils.removeElement(getScreenFadeKeyFramesSafe(), kf);
                break;
            case SCREEN_SHAKE:
                screenShakeKeyFrames = ArrayUtils.removeElement(getScreenShakeKeyFramesSafe(), kf);
                break;
            case SOUND_1:
            case SOUND_2:
            case SOUND_3:
            case SOUND_4:
                setSoundKeyFrames(kf.getKeyFrameType(),
                        ArrayUtils.removeElement(getSoundKeyFramesSafe(kf.getKeyFrameType()), kf));
                break;
            default:
                break;
        }
    }

    /** Scan helper: returns the existing keyframe whose tick matches, or null. */
    private static KeyFrame findAtTick(KeyFrame[] arr, double tick)
    {
        if (arr == null) return null;
        for (KeyFrame k : arr)
        {
            if (k != null && k.getTick() == tick) return k;
        }
        return null;
    }

    /**
     * Inserts {@code kf} into {@code existing} sorted by tick. If a keyframe
     * already sits at the same tick, that slot is overwritten so each (type,
     * tick) pair stays unique -- matches Character.addKeyFrame semantics.
     * The {@code zeroLengthMarker} is only used to recover the component
     * type when {@code existing} is null/empty.
     */
    @SuppressWarnings("unchecked")
    private static <T extends KeyFrame> T[] insertSorted(T[] existing, T kf, T[] zeroLengthMarker)
    {
        if (existing == null || existing.length == 0)
        {
            T[] out = (T[]) java.lang.reflect.Array.newInstance(
                    zeroLengthMarker.getClass().getComponentType(), 1);
            out[0] = kf;
            return out;
        }
        int sameTickIdx = -1;
        int insertIdx = existing.length;
        for (int i = 0; i < existing.length; i++)
        {
            if (existing[i] == null) continue;
            if (existing[i].getTick() == kf.getTick())
            {
                sameTickIdx = i;
                break;
            }
            if (existing[i].getTick() > kf.getTick())
            {
                insertIdx = i;
                break;
            }
        }
        if (sameTickIdx >= 0)
        {
            T[] out = existing.clone();
            out[sameTickIdx] = kf;
            return out;
        }
        return ArrayUtils.insert(insertIdx, existing, kf);
    }
}
