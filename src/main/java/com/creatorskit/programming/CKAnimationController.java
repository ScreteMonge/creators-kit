package com.creatorskit.programming;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;

public class CKAnimationController extends AnimationController
{
    @Getter
    @Setter
    private boolean loop;

    @Getter
    @Setter
    private boolean finished;

    /**
     * Lower bound of the playback range. {@code 0} = animation's natural start.
     * Honored by {@link #tick} -- looping wraps back to this frame instead of frame 0,
     * and elapsed-tick accounting is anchored to the range's cumulative length.
     */
    @Getter
    @Setter
    private int firstFrameOverride = 0;

    /**
     * Upper bound of the playback range. {@code 0} (or out-of-range) = "use the
     * animation's natural last frame". When set, playback stops at this frame
     * (or loops back to {@link #firstFrameOverride} after dwelling for
     * {@link #pauseTicks} client ticks if loop is on).
     */
    @Getter
    @Setter
    private int lastFrameOverride = 0;

    /**
     * Number of client ticks to dwell on the last frame before looping back to
     * {@link #firstFrameOverride}. Only meaningful when {@link #loop} is on.
     * Zero = no pause (loop immediately after the last frame).
     */
    @Getter
    @Setter
    private int pauseTicks = 0;

    /**
     * Authoritative playback clock when range / pause are active. Mirrors the
     * parent's per-frame {@code elapsedTicks} accounting but tracks total ticks
     * since the range was last reset, so we can compute the current frame
     * deterministically from this single counter every tick.
     */
    private long rangePlaybackTicks = 0;

    public CKAnimationController(Client client, int animationID, boolean loop)
    {
        super(client, animationID);
        this.loop = loop;
    }

    public CKAnimationController(Client client, Animation animation, boolean loop)
    {
        super(client, animation);
        this.loop = loop;
    }

    /** Returns true when any range/pause override is in effect for this controller. */
    private boolean rangeActive()
    {
        return lastFrameOverride > 0 || pauseTicks > 0 || firstFrameOverride > 0;
    }

    /**
     * Resets the range playback clock to zero. Call when (re-)activating an
     * animation keyframe so the first frame after activation is the range's
     * starting frame regardless of prior playback state.
     */
    public void resetRangeClock()
    {
        rangePlaybackTicks = 0;
    }

    @Override
    public void tick(int ticks)
    {
        Animation animation = getAnimation();
        if (animation == null || !rangeActive() || animation.isMayaAnim())
        {
            // No range -- fall through to the engine's standard per-frame advance.
            super.tick(ticks);
            return;
        }

        int[] frameLengths = animation.getFrameLengths();
        if (frameLengths == null || frameLengths.length == 0)
        {
            super.tick(ticks);
            return;
        }

        rangePlaybackTicks += ticks;

        int totalFrames = frameLengths.length;
        int effectiveLast = (lastFrameOverride > 0 && lastFrameOverride < totalFrames)
                ? lastFrameOverride
                : totalFrames - 1;
        int effectiveFirst = Math.max(0, Math.min(firstFrameOverride, effectiveLast));

        int rangeDuration = 0;
        for (int i = effectiveFirst; i <= effectiveLast; i++)
        {
            rangeDuration += frameLengths[i];
        }
        int safePause = Math.max(0, pauseTicks);

        if (loop)
        {
            int loopLength = rangeDuration + safePause;
            if (loopLength <= 0)
            {
                loopLength = 1;
            }
            int timeIntoLoop = (int) (rangePlaybackTicks % loopLength);
            if (timeIntoLoop >= rangeDuration)
            {
                // Pause phase: hold the last frame.
                setFrame(effectiveLast);
                return;
            }
            int accumulated = 0;
            for (int i = effectiveFirst; i <= effectiveLast; i++)
            {
                int frameLength = frameLengths[i];
                if (accumulated + frameLength > timeIntoLoop)
                {
                    setFrame(i);
                    return;
                }
                accumulated += frameLength;
            }
            setFrame(effectiveLast);
            return;
        }

        // Non-loop: play the range once then hold the last frame. The engine's
        // onFinished callback (used by the parent's super.tick) isn't fired here --
        // for the cinematic-capture use case the user can simply not enable loop
        // and the animation will freeze at the last frame, which is the desired
        // behaviour. If a future caller needs the despawn-on-finish hook this can
        // be revisited (the field is package-private on AnimationController so it
        // needs reflection or an API change to invoke).
        if (rangePlaybackTicks >= rangeDuration)
        {
            setFrame(effectiveLast);
            return;
        }

        int accumulated = 0;
        for (int i = effectiveFirst; i <= effectiveLast; i++)
        {
            int frameLength = frameLengths[i];
            if (accumulated + frameLength > rangePlaybackTicks)
            {
                setFrame(i);
                return;
            }
            accumulated += frameLength;
        }
        setFrame(effectiveLast);
    }
}
