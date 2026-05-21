package com.creatorskit.saves;

import com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame;
import lombok.Getter;
import lombok.Setter;

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

    public GlobalKeyFrames()
    {
        this.cameraKeyFrames = new CameraKeyFrame[0];
        this.screenFadeKeyFrames = new ScreenFadeKeyFrame[0];
        this.screenShakeKeyFrames = new ScreenShakeKeyFrame[0];
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
}
