package com.creatorskit;

import com.creatorskit.models.CustomModel;
import com.creatorskit.programming.AnimationType;
import com.creatorskit.swing.ParentPanel;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

@Getter
@Setter
@AllArgsConstructor
public class Character
{
    private String name;
    private boolean active;
    private boolean inScene;
    private KeyFrame[][] frames;
    private KeyFrame[] currentFrames;
    private KeyFrameType[] summary;
    private DefaultMutableTreeNode linkedManagerNode;
    private DefaultMutableTreeNode parentManagerNode;
    private Color color;
    private WorldPoint nonInstancedPoint;
    private LocalPoint instancedPoint;
    private int instancedPlane;
    private boolean inPOH;
    private CustomModel storedModel;
    private ParentPanel parentPanel;
    private JPanel objectPanel;
    private boolean customMode;
    private JTextField nameField;
    private JComboBox<CustomModel> comboBox;
    private JCheckBox spawnCheckBox;
    /** Side-panel checkbox mirroring {@link #renderFix}; lives next to spawnCheckBox. */
    private JCheckBox renderFixCheckBox;
    /**
     * Side-panel checkbox for the per-Character camera lock. Mutually exclusive across
     * Characters -- the plugin's setCameraLockedCharacter clears the previously-locked
     * Character's checkbox before flipping this one on. Click-driven, and stays in
     * sync with the manager-tree right-click toggle since both call into the plugin.
     */
    private JCheckBox cameraLockCheckBox;
    private JButton modelButton;
    private JButton colourButton;
    private JSpinner modelSpinner;
    private JSpinner animationSpinner;
    private JSpinner animationFrameSpinner;
    private JSpinner orientationSpinner;
    private JSpinner radiusSpinner;
    private CKObject ckObject;
    private CKObject spotAnim1;
    private CKObject spotAnim2;
    /** One CKObject per active projectile-target instance; lazily allocated by the Programmer. */
    private final java.util.List<CKObject> projectileObjects = new java.util.ArrayList<>();
    private int targetOrientation;
    /**
     * When true, this Character's CKObject runs its model through a bracket-scaled
     * animation pass: vertices are shrunk to the rigging's canonical 128 scale before
     * the animation pipeline runs, then expanded back to {@link #renderFixScale} for
     * rendering. Fixes the "warped / drifting animations" some newer cache models
     * exhibit when their cache-resize value pushes vertex radii above 128 -- bones are
     * stored at 128 scale, so vertex distances of e.g. 300 swing in arcs ~2.3x the
     * rigging's intended radius. Off by default since well-behaved 128-scale models
     * already match the rigging.
     */
    private boolean renderFix;

    /**
     * Horizontal display scale of this Character's model in OSRS 1/128 units. Mirrors
     * NPCComposition.getWidthScale(); captured at NPC-import time (auto-detect path)
     * or settable manually for custom models. 128 = no scaling needed (matches the
     * canonical rigging size); other values trigger the render-fix bracket when the
     * toggle is on.
     */
    private int renderFixWidth = 128;

    /** Vertical companion to {@link #renderFixWidth}; matches NPCComposition.getHeightScale(). */
    private int renderFixHeight = 128;

    /**
     * User-set sub-tile position offsets applied on top of whatever tile-aligned
     * position the Character would otherwise have. X/Y are scene-space (positive X =
     * east, positive Y = north, same as LocalPoint). Z is conceptually "up", positive
     * = higher; internally subtracted from CKObject's z when applied because OSRS
     * render space treats negative z as up.
     *
     * <p>Driven by the ALT+WASD/R/F hotkeys for in-place nudging without going
     * through the Model Anvil. Each press of {@link #nudgeOffset(int, int, int)}
     * accumulates a delta of 5 units; the offset is applied per-frame via
     * {@link CKObject#setLocation(LocalPoint, int)} so it follows the Character
     * through movement keyframes and animations rather than detaching at any point.
     */
    private int offsetX;
    private int offsetY;
    private int offsetZ;

    /**
     * User-controlled uniform scale multiplier applied on top of widthScale /
     * heightScale, propagated to the live CKObject. 1.0 = no change. Driven by the
     * ALT+Scroll hotkey. Saved + loaded via {@code CharacterSave#extraScale};
     * pre-2.3 saves (and the Lombok no-args ctor path) default to 1.0.
     */
    private double extraScale = 1.0;
    /**
     * User-defined timeline <b>Labels</b> for this Character -- colored, named
     * time-range markers shown in the top row of the timeline, purely for
     * organization (no interactivity). Empty (not null) for fresh Characters
     * and for old saves. See {@link com.creatorskit.swing.timesheet.keyframe.Block}
     * (the legacy class name) for the data model.
     *
     * <p>Positioned at the END of the field list so Lombok's
     * {@code @AllArgsConstructor} adds the new arg as the LAST parameter --
     * existing positional {@code new Character(...)} callsites just append
     * one more arg (empty list or null) instead of having to renumber
     * everything in between.
     */
    private java.util.List<com.creatorskit.swing.timesheet.keyframe.Block> blocks = new java.util.ArrayList<>();

    /**
     * Overrides Lombok's auto-generated setter so toggling renderFix on the Character
     * also (a) pushes the flag + per-axis scales to the live CKObject, so the renderer
     * picks up the change next frame, and (b) syncs the side-panel checkbox so
     * programmatic toggles (e.g. loading a save) visibly reflect on the UI.
     */
    public void setRenderFix(boolean renderFix)
    {
        this.renderFix = renderFix;
        if (ckObject != null)
        {
            ckObject.setRenderFix(renderFix);
            // Scales only matter when the fix is active; when off, force CKObject back
            // to 128/128 so getModel() short-circuits even if scales are non-default.
            ckObject.setWidthScale(renderFix ? renderFixWidth : 128);
            ckObject.setHeightScale(renderFix ? renderFixHeight : 128);
        }
        if (renderFixCheckBox != null && renderFixCheckBox.isSelected() != renderFix)
        {
            renderFixCheckBox.setSelected(renderFix);
        }
    }

    /**
     * Overrides Lombok's auto-generated setter so changing the horizontal scale also
     * pushes it down to the live CKObject when the fix is currently active.
     */
    public void setRenderFixWidth(int renderFixWidth)
    {
        this.renderFixWidth = renderFixWidth;
        if (ckObject != null && renderFix)
        {
            ckObject.setWidthScale(renderFixWidth);
        }
    }

    /** @see #setRenderFixWidth(int) -- vertical companion. */
    public void setRenderFixHeight(int renderFixHeight)
    {
        this.renderFixHeight = renderFixHeight;
        if (ckObject != null && renderFix)
        {
            ckObject.setHeightScale(renderFixHeight);
        }
    }

    /**
     * Overrides for the Lombok offset setters so changing offsets at runtime (e.g.
     * during save load) also pushes the values down to the live CKObject. CKObject's
     * own setLocation applies them per-frame, so the values stay in sync without
     * any further triggering.
     */
    public void setOffsetX(int offsetX)
    {
        this.offsetX = offsetX;
        if (ckObject != null)
        {
            ckObject.setOffsetX(offsetX);
        }
        // SpotAnims piggyback on the model's sub-tile offsets so projectile-style
        // effects, beams and impact glows ride along with an ALT+WASD nudge
        // instead of detaching to the tile-aligned base position.
        if (spotAnim1 != null) spotAnim1.setOffsetX(offsetX);
        if (spotAnim2 != null) spotAnim2.setOffsetX(offsetX);
    }

    public void setOffsetY(int offsetY)
    {
        this.offsetY = offsetY;
        if (ckObject != null)
        {
            ckObject.setOffsetY(offsetY);
        }
        if (spotAnim1 != null) spotAnim1.setOffsetY(offsetY);
        if (spotAnim2 != null) spotAnim2.setOffsetY(offsetY);
    }

    public void setOffsetZ(int offsetZ)
    {
        this.offsetZ = offsetZ;
        if (ckObject != null)
        {
            ckObject.setOffsetZ(offsetZ);
        }
        if (spotAnim1 != null) spotAnim1.setOffsetZ(offsetZ);
        if (spotAnim2 != null) spotAnim2.setOffsetZ(offsetZ);
    }

    /**
     * Overrides Lombok's auto-generated setter so the value reaches the live CKObject's
     * getModel() bracket. Clamps to a sane range so a runaway scroll can't drive the
     * model into a numerically broken size (Mesh.scale uses int factors -- huge scales
     * overflow, sub-pixel scales collapse).
     */
    public void setExtraScale(double extraScale)
    {
        // Clamp matches Mesh.scale's safe range -- below 0.05 the int factor floors to
        // zero and the model disappears; above 20 we hit perf cliffs and silly artefacts.
        if (extraScale < 0.05) extraScale = 0.05;
        if (extraScale > 20.0) extraScale = 20.0;
        this.extraScale = extraScale;
        if (ckObject != null)
        {
            ckObject.setExtraScale(extraScale);
        }
        // Spotanims scale with the model so impact glows and projectile effects
        // stay proportional when the user ALT+Scrolls the parent up/down.
        if (spotAnim1 != null) spotAnim1.setExtraScale(extraScale);
        if (spotAnim2 != null) spotAnim2.setExtraScale(extraScale);
    }

    /**
     * Multiplies the current extraScale by {@code factor} and applies. Helper for the
     * ALT+Scroll hotkey -- one scroll tick passes (1 + scaleStep) for upscale,
     * (1 / (1 + scaleStep)) for downscale, giving symmetric / reversible scaling
     * (3 ups then 3 downs returns to the original size).
     */
    public void scaleBy(double factor)
    {
        setExtraScale(extraScale * factor);
    }

    /**
     * Accumulates a delta into the user-set position offsets and immediately applies
     * the same delta to the live CKObject's rendered position so the ALT+WASD/R/F
     * hotkeys feel responsive (otherwise the user would have to wait for the next
     * setLocation call to see the shift). Z is negated when applied to the CKObject
     * because OSRS render space has -z = up while we expose +offsetZ = up.
     */
    public void nudgeOffset(int dx, int dy, int dz)
    {
        this.offsetX += dx;
        this.offsetY += dy;
        this.offsetZ += dz;
        if (ckObject != null)
        {
            ckObject.setOffsetX(this.offsetX);
            ckObject.setOffsetY(this.offsetY);
            ckObject.setOffsetZ(this.offsetZ);
            ckObject.setX(ckObject.getX() + dx);
            ckObject.setY(ckObject.getY() + dy);
            ckObject.setZ(ckObject.getZ() - dz);
        }
        // Mirror the nudge live onto the spotanim CKObjects -- without this,
        // hits / glows / cast effects detach from the model on every ALT+WASD
        // press and the user has to re-fire the spotanim to resync.
        nudgeSpotAnim(spotAnim1, dx, dy, dz);
        nudgeSpotAnim(spotAnim2, dx, dy, dz);
    }

    private void nudgeSpotAnim(CKObject sa, int dx, int dy, int dz)
    {
        if (sa == null) return;
        sa.setOffsetX(this.offsetX);
        sa.setOffsetY(this.offsetY);
        sa.setOffsetZ(this.offsetZ);
        sa.setX(sa.getX() + dx);
        sa.setY(sa.getY() + dy);
        sa.setZ(sa.getZ() - dz);
    }

    @Override
    public String toString()
    {
        return name;
    }

    public void setPlaying(boolean playing)
    {
        if (playing)
        {
            play();
            return;
        }

        pause();
    }

    public void play()
    {
        ckObject.setPlaying(true);
        if (spotAnim1 != null)
        {
            spotAnim1.setPlaying(true);
        }

        if (spotAnim2 != null)
        {
            spotAnim2.setPlaying(true);
        }
    }

    public void pause()
    {
        ckObject.setPlaying(false);
        if (spotAnim1 != null)
        {
            spotAnim1.setPlaying(false);
        }

        if (spotAnim2 != null)
        {
            spotAnim2.setPlaying(false);
        }
    }

    /**
     * Updates the Character's current KeyFrame for the given time
     * @param tick the tick at which to look for KeyFrames
     */
    public void updateProgram(double tick)
    {
        for (KeyFrameType type : KeyFrameType.ALL_KEYFRAME_TYPES)
        {
            KeyFrame current = findPreviousKeyFrame(type, tick, true);
            setCurrentKeyFrame(current, type);
        }
    }

    public void setOrientation(int orientation)
    {
        if (ckObject != null)
        {
            ckObject.setOrientation(orientation);
        }

        if (spotAnim1 != null)
        {
            spotAnim1.setOrientation(orientation);
        }

        if (spotAnim2 != null)
        {
            spotAnim2.setOrientation(orientation);
        }
    }

    public void setLocation(LocalPoint lp, int plane)
    {
        if (ckObject != null)
        {
            ckObject.setLocation(lp, plane);
        }

        if (spotAnim1 != null)
        {
            spotAnim1.setLocation(lp, plane);
        }

        if (spotAnim2 != null)
        {
            spotAnim2.setLocation(lp, plane);
        }
    }

    public void setAnimation(ClientThread clientThread, Client client, Random random, AnimationType type, int animId, int animFrame, boolean randomizeStartFrame, boolean allowPause)
    {
        clientThread.invokeLater(() -> setAnimation(client, random, type, animId, animFrame, randomizeStartFrame, allowPause));
    }

    /**
     * Plays {@code animId} on this Character's active animation slot ONCE,
     * without touching {@link #animationSpinner}. Used by the Cache Searcher
     * Animation Searcher double-click handler so users can audition an
     * animation visually without committing it to the Character's recorded
     * state. The default on-finished handler resets to anim -1 when the play
     * completes, so the Character ends up visually idle -- the next timeline
     * scrub / play tick / explicit "Add keyframe" cleanly re-derives the
     * correct state from the (unchanged) spinner + keyframe data.
     *
     * <p>Crucially: NO {@code propagateSpinner} call, NO spinner.setValue.
     * Multi-select Characters keep their own animations.
     */
    public void previewAnimation(ClientThread clientThread, Client client, Random random, int animId)
    {
        clientThread.invokeLater(() -> {
            Animation animation = client.loadAnimation(animId);
            ckObject.setAnimation(AnimationType.ACTIVE, animation);
            ckObject.setAnimationFrame(AnimationType.ACTIVE, 0, random, false, false);
            ckObject.setPlaying(true);
            // Reset every playback modifier that a prior keyframe might
            // have left lingering on the ckObject. Preview is always:
            // 1x speed, frame 0 -> natural end, no loop, no pause-on-end,
            // no freeze. Without this reset a Character that last ran a
            // 0.5x looping anim with a clamped last-frame would preview
            // the new id under those stale settings.
            ckObject.setAnimationSpeed(1.0);
            ckObject.setAnimationRange(0, 0, 0);  // first=0 + last=0 = "use natural range"; pauseTicks=0
            ckObject.setFreeze(false);
            // loop=false is the whole point -- the default onFinished handler
            // in CKObject.setOnFinished() will reset animation to -1 when the
            // cycle ends, so preview is truly transient.
            ckObject.setLoop(false);
            ckObject.setHasAnimKeyFrame(false);
        });
    }

    public void setAnimation(Client client, Random random, AnimationType type, int animId, int animFrame, boolean randomizeStartFrame, boolean allowPause)
    {
        Animation animation = client.loadAnimation(animId);
        ckObject.setAnimation(type, animation);

        int frame = animFrame;
        boolean pause = allowPause;

        if (frame == -1)
        {
            pause = false;
        }

        ckObject.setAnimationFrame(type, frame, random, randomizeStartFrame, pause);
        KeyFrame kf = getCurrentKeyFrame(KeyFrameType.ANIMATION);
        if (kf == null)
        {
            ckObject.setPlaying(true);
            ckObject.setLoop(true);
            ckObject.setHasAnimKeyFrame(false);
        }
        else
        {
            pause();
            ckObject.setHasAnimKeyFrame(true);
        }
    }

    public void setSpotAnim(CKObject spotAnim, KeyFrameType spotAnimType)
    {
        if (spotAnimType == KeyFrameType.SPOTANIM)
        {
            setSpotAnim1(spotAnim);
        }

        if (spotAnimType == KeyFrameType.SPOTANIM2)
        {
            setSpotAnim2(spotAnim);
        }
    }

    public KeyFrame getCurrentKeyFrame(KeyFrameType type)
    {
        return currentFrames[KeyFrameType.getIndex(type)];
    }

    public void setCurrentKeyFrame(KeyFrame keyFrame, KeyFrameType type)
    {
        int idx = KeyFrameType.getIndex(type);
        KeyFrame previous = currentFrames[idx];

        // Orientation snapshot-at-activation: when an OrientationKeyFrame
        // becomes the current kf (transition from a different value), write
        // its start angle so {@code setOrientationStatic} / {@code getOrientation}
        // interpolate from a sensible "before this kf" value. start is a
        // runtime cache, not user input.
        //
        // Source is the IMMEDIATELY-PRECEDING orientation kf's end value
        // (or the character's idle spinner if there isn't one) -- NOT
        // ckObject.getOrientation(). Reading the live ckObject orientation
        // would include any rotation a Movement keyframe's face-trajectory
        // had just applied, so the snapshot would capture a movement-
        // influenced angle. Then setOrientationStatic interps from that
        // angle to kf.end -- visibly wrong, because the user's intent for
        // an orientation kf is to override movement face-trajectory
        // entirely.
        //
        // Gates:
        //  (1) previous != keyFrame -- skip when the same kf is re-set
        //      within its window (every-tick play loop), so start isn't
        //      rewritten with the same value redundantly.
        //  (2) !ckObject.isPlaying() -- only refresh on scrub / edit, not
        //      mid-play. Play uses whatever the most recent scrub
        //      computed, keeping per-play orientation deterministic.
        // Re-snapshot on every scrub-onto-this-kf, not just when it
        // first becomes current. Previously the `keyFrame != previous`
        // guard skipped re-snapshot when the SAME kf was activated
        // again -- so editing the PRIOR ori kf's end didn't propagate
        // to this kf's Start, and the compass showed the stale value
        // until the user scrubbed away and back to force a fresh
        // activation. The !isPlaying() gate still prevents per-tick
        // churn during playback (where edits aren't allowed anyway
        // because the play-lock disables the UI).
        if (type == KeyFrameType.ORIENTATION
                && keyFrame instanceof OrientationKeyFrame
                && ckObject != null
                && !ckObject.isPlaying())
        {
            OrientationKeyFrame newKf = (OrientationKeyFrame) keyFrame;
            int newStart = computeOrientationStartFor(newKf);
            if (newStart != newKf.getStart())
            {
                newKf.setStart(newStart);
            }
        }

        currentFrames[idx] = keyFrame;
    }

    /**
     * "Start" angle snapshot for an OrientationKeyFrame's interpolation.
     * Picks whichever of (latest prior orientation kf, latest prior movement
     * kf) ENDED LATER as the reference -- because that's whatever was last
     * actively driving the character's rotation, so it's where the character
     * is sitting now.
     *
     * <p>"Ended last" vs "started last": two sources can start at the same
     * tick (e.g. a 5-tick movement and a 3-tick orientation kf both at tick
     * 0). "Started last" is ambiguous there. "Ended last" picks Movement
     * (ends at tick 5) over the prior orientation (ends at tick 3) -- and
     * Movement's auto-orient is the more recent driver of the character's
     * rotation at any tick after 3, so its final-segment direction is the
     * honest snapshot.
     *
     * <p>Reference value:
     * <ul>
     *   <li>Prior orientation kf wins -&gt; its {@code end} angle.</li>
     *   <li>Prior movement kf wins -&gt; the angle of its last path segment
     *     (path[length-2] -&gt; path[length-1]), which is what Movement's
     *     auto-orient settled on when the path completed.</li>
     *   <li>Neither exists -&gt; the Character's idle orientation spinner.</li>
     * </ul>
     *
     * <p>Never reads ckObject.getOrientation() -- the live ckObject value
     * can be mid-interpolation, mid-face-target snap, or otherwise
     * transient, so going to the keyframe data directly keeps the start
     * stable across scrub / pause / play.
     */
    private int computeOrientationStartFor(OrientationKeyFrame newKf)
    {
        double newKfTick = newKf.getTick();

        OrientationKeyFrame priorOri = null;
        KeyFrame[] oris = getKeyFrames(KeyFrameType.ORIENTATION);
        if (oris != null)
        {
            for (KeyFrame kf : oris)
            {
                if (kf == null || kf == newKf) continue;
                if (kf.getTick() < newKfTick
                        && (priorOri == null || kf.getTick() > priorOri.getTick()))
                {
                    priorOri = (OrientationKeyFrame) kf;
                }
            }
        }

        MovementKeyFrame priorMkf = null;
        KeyFrame[] mkfs = getKeyFrames(KeyFrameType.MOVEMENT);
        if (mkfs != null)
        {
            for (KeyFrame kf : mkfs)
            {
                if (kf == null) continue;
                if (kf.getTick() < newKfTick
                        && (priorMkf == null || kf.getTick() > priorMkf.getTick()))
                {
                    priorMkf = (MovementKeyFrame) kf;
                }
            }
        }

        double oriEnd = priorOri == null
                ? Double.NEGATIVE_INFINITY
                : priorOri.getTick() + priorOri.getDuration();
        double mkfEnd = Double.NEGATIVE_INFINITY;
        boolean mkfUsable = priorMkf != null
                && priorMkf.getPath() != null
                && priorMkf.getPath().length >= 2
                && priorMkf.getSpeed() > 0;
        if (mkfUsable)
        {
            // Match Programmer.findLastOrientation's pathDuration formula
            // (Math.ceil((length - 1) / speed)) so the "end tick" computed
            // here lines up exactly with the play-loop's notion of when
            // movement finishes. Without ceil, fractional speeds make this
            // value slightly less than the play loop's, so the tie check
            // below can fire "true tie" when one side thinks they're
            // tied and the other thinks movement ends later.
            double pathDur = Math.ceil((priorMkf.getPath().length - 1) / priorMkf.getSpeed());
            mkfEnd = priorMkf.getTick() + pathDur;
        }

        // No candidates -> idle spinner.
        if (priorOri == null && !mkfUsable)
        {
            int v = idleOrientationFromSpinner();
            debug("computeOrientationStartFor newKfTick=%.2f: no prior ori or movement -> spinner=%d", newKfTick, v);
            return v;
        }
        // Only one candidate -> use it.
        if (!mkfUsable)
        {
            int v = orientationOfOriAt(priorOri, newKfTick);
            debug("computeOrientationStartFor newKfTick=%.2f: only priorOri (ends %.2f) -> resolved=%d (goal end=%d)",
                    newKfTick, oriEnd, v, priorOri.getEnd());
            return v;
        }
        if (priorOri == null)
        {
            int v = movementFinalDirection(priorMkf);
            debug("computeOrientationStartFor newKfTick=%.2f: only priorMkf (ends %.2f) -> movement final dir=%d",
                    newKfTick, mkfEnd, v);
            return v;
        }
        // Both candidates exist. Mirror Programmer.findLastOrientation's
        // arbitration, in priority order:
        //
        //   1. newKfTick <= oriEnd -> ORI wins. The new kf sits within
        //      (or exactly at the boundary of) the prior orientation kf's
        //      effect window, so the prior ori was still in control at
        //      newKfTick -- its end angle is what the character is
        //      sitting at. This is the case the user's "Moon Shield
        //      (Outer)" repro hit: second kf at tick 41, prior ori ends
        //      at tick 41, movement extended further; without this check
        //      Movement won the ended-last comparison and snapshot
        //      grabbed the path's final segment direction instead of
        //      the ori's end angle.
        //
        //   2. Else (newKfTick > oriEnd, prior ori has already expired):
        //      compare end ticks. If oriEnd >= mkfEnd, ori was the
        //      more recent driver; otherwise movement is.
        //
        // TIE-BREAKER inside rule 2: when end ticks are equal, ori wins
        // (mkfEnd > oriEnd is strict).
        if (newKfTick <= oriEnd)
        {
            int v = orientationOfOriAt(priorOri, newKfTick);
            debug("computeOrientationStartFor newKfTick=%.2f: priorOri ends %.2f (within window) -> ORI wins (resolved=%d, goal end=%d)",
                    newKfTick, oriEnd, v, priorOri.getEnd());
            return v;
        }
        if (mkfEnd > oriEnd)
        {
            int v = movementFinalDirection(priorMkf);
            debug("computeOrientationStartFor newKfTick=%.2f: priorOri ended %.2f, priorMkf ends %.2f (later) -> MOVEMENT wins (final dir=%d)",
                    newKfTick, oriEnd, mkfEnd, v);
            return v;
        }
        int v = orientationOfOriAt(priorOri, newKfTick);
        debug("computeOrientationStartFor newKfTick=%.2f: priorOri ended %.2f, priorMkf ends %.2f (oriEnd >= mkfEnd) -> ORI wins (resolved=%d, goal end=%d)%s",
                newKfTick, oriEnd, mkfEnd, v, priorOri.getEnd(), (mkfEnd == oriEnd ? " [TIE]" : ""));
        return v;
    }

    /**
     * The orientation a prior OrientationKeyFrame has ACTUALLY produced by the
     * time {@code atTick} arrives: it interpolates from {@code okf}'s own
     * (recursively resolved) start toward its end at its turn rate, clamped to
     * {@code okf}'s duration. Unlike {@code okf.getEnd()}, this reflects an
     * INCOMPLETE turn -- when the keyframe's duration was too short for its turn
     * rate to reach the goal, the character only got part-way, and a FOLLOWING
     * keyframe must start from where the character really is, not from a goal it
     * never reached. (That mismatch was the "uses last-known orientation, not
     * current orientation" bug.)
     *
     * <p>Recursion is bounded: {@link #computeOrientationStartFor} only ever
     * looks at keyframes with a strictly smaller tick, so the chain terminates
     * at the first orientation keyframe (whose start falls back to a movement
     * segment or the idle spinner). The rotation math mirrors
     * {@code Programmer.getOrientationStatic} so scrub and play agree.
     */
    private int orientationOfOriAt(OrientationKeyFrame okf, double atTick)
    {
        // Face Target (FOLLOW) keyframes don't turn toward a fixed end -- they
        // track a moving target, so the start->end clamp below is meaningless.
        // Keep the stored end angle for them (the prior behaviour). The
        // incomplete-turn resolution is only meaningful for fixed-angle (POINT)
        // turns. (A FOLLOW kf's OWN start is still resolved correctly -- that
        // path runs computeOrientationStartFor on ITS prior, not this branch.)
        String targetName = okf.getTargetCharacterName();
        if (targetName != null && !targetName.trim().isEmpty())
        {
            return okf.getEnd();
        }

        double ticksPassed = atTick - okf.getTick();
        if (ticksPassed < 0) ticksPassed = 0;
        if (ticksPassed > okf.getDuration()) ticksPassed = okf.getDuration();

        int start = computeOrientationStartFor(okf);
        int end = okf.getEnd();
        int difference = com.creatorskit.programming.orientation.Orientation
                .directionalDifference(start, end, okf.getTurnDirection());
        double rotation = okf.getTurnRate() * ticksPassed
                * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH;

        if (difference > -rotation && difference < rotation)
        {
            return end;
        }
        if (difference > 0)
        {
            return com.creatorskit.programming.orientation.Orientation.boundOrientation((int) (start + rotation));
        }
        return com.creatorskit.programming.orientation.Orientation.boundOrientation((int) (start - rotation));
    }

    /**
     * Direction angle of the movement kf's final segment -- where its
     * auto-orient leaves the character pointing once the path is fully
     * walked. Returns the idle spinner value when the path is too short
     * to define a direction (length &lt; 2).
     */
    private int movementFinalDirection(MovementKeyFrame mkf)
    {
        int[][] path = mkf.getPath();
        if (path == null || path.length < 2) return idleOrientationFromSpinner();
        int[] last = path[path.length - 1];
        int[] secondLast = path[path.length - 2];
        if (last == null || secondLast == null || last.length < 2 || secondLast.length < 2)
        {
            return idleOrientationFromSpinner();
        }
        double dx = last[0] - secondLast[0];
        double dy = last[1] - secondLast[1];
        if (dx == 0 && dy == 0) return idleOrientationFromSpinner();
        return (int) com.creatorskit.programming.orientation.Orientation
                .radiansToJAngle(Math.atan(dy / dx), dx, dy);
    }

    private int idleOrientationFromSpinner()
    {
        if (orientationSpinner != null)
        {
            Object v = orientationSpinner.getValue();
            if (v instanceof Number) return ((Number) v).intValue();
        }
        return 0;
    }

    public void resetMovementKeyFrame(int clientTick, double currentTime)
    {
        KeyFrame kf = getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        if (kf == null)
        {
            return;
        }

        MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
        double diff = TimeSheetPanel.round(currentTime - keyFrame.getTick());
        int cTickDiff = (int) (diff * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH);
        int stepCTick = clientTick - cTickDiff;
        keyFrame.setStepClientTick(stepCTick);
    }

    /**
     * Static handle to the central GlobalKeyFrames store, injected by the
     * plugin at boot via {@link #setGlobalKeyFramesStore}. Phase 2: the three
     * "global" keyframe types (Camera / Screen Fade / Screen Shake) no longer
     * live in per-Character {@link #frames} -- {@link #getKeyFrames} and
     * {@link #setKeyFrames} transparently delegate them to this store so every
     * existing code path (find/add/remove keyframe) keeps working while reads
     * and writes hit one canonical location.
     *
     * <p>Static rather than instance-level because every Character shares the
     * same global store anyway; injecting it through every Character
     * constructor would require touching every {@code new Character(...)}
     * callsite for no behavioural gain.
     */
    private static com.creatorskit.saves.GlobalKeyFrames globalKeyFramesStore;

    public static void setGlobalKeyFramesStore(com.creatorskit.saves.GlobalKeyFrames store)
    {
        globalKeyFramesStore = store;
    }

    /**
     * Static debug hook installed by the plugin so Character can log
     * diagnostic messages without needing access to CreatorsConfig /
     * Programmer / sendChatMessage from inside its model code. Set to a
     * BiConsumer that takes the Character and a message; the consumer
     * decides whether to actually log based on its own config (e.g. the
     * debugCharacterName setting in Programmer.debugCharacter).
     */
    private static java.util.function.BiConsumer<Character, String> debugHook;

    public static void setDebugHook(java.util.function.BiConsumer<Character, String> hook)
    {
        debugHook = hook;
    }

    private void debug(String fmt, Object... args)
    {
        if (debugHook == null) return;
        debugHook.accept(this, String.format(fmt, args));
    }

    private static boolean isGlobalType(KeyFrameType type)
    {
        // Mirror the canonical predicate so SOUND_1..4 route to the global
        // store too -- otherwise adding an Area Sound while a Character is
        // selected would write the kf to that Character's per-type frames
        // array, where the global timeline never looks for it.
        return KeyFrameType.isGlobal(type);
    }

    public KeyFrame[] getKeyFrames(KeyFrameType type)
    {
        if (globalKeyFramesStore != null && isGlobalType(type))
        {
            switch (type)
            {
                case CAMERA:       return globalKeyFramesStore.getCameraKeyFramesSafe();
                case SCREEN_FADE:  return globalKeyFramesStore.getScreenFadeKeyFramesSafe();
                case SCREEN_SHAKE: return globalKeyFramesStore.getScreenShakeKeyFramesSafe();
                case SOUND_1:
                case SOUND_2:
                case SOUND_3:
                case SOUND_4:      return globalKeyFramesStore.getSoundKeyFramesSafe(type);
                default:           break;
            }
        }
        return frames[KeyFrameType.getIndex(type)];
    }

    public KeyFrame[] getAllKeyFrames()
    {
        KeyFrame[] keyFrames = new KeyFrame[0];
        for (KeyFrame[] kfs : frames)
        {
            keyFrames = ArrayUtils.addAll(keyFrames, kfs);
        }

        return keyFrames;
    }

    public void setKeyFrames(KeyFrame[] keyFrames, KeyFrameType type)
    {
        if (globalKeyFramesStore != null && isGlobalType(type))
        {
            // Cast is safe because Character.addKeyFrame only builds typed arrays
            // for one type at a time -- the array's element type matches `type`.
            switch (type)
            {
                case CAMERA:
                    globalKeyFramesStore.setCameraKeyFrames(
                            toTypedArray(keyFrames, com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame.class));
                    return;
                case SCREEN_FADE:
                    globalKeyFramesStore.setScreenFadeKeyFrames(
                            toTypedArray(keyFrames, com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame.class));
                    return;
                case SCREEN_SHAKE:
                    globalKeyFramesStore.setScreenShakeKeyFrames(
                            toTypedArray(keyFrames, com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame.class));
                    return;
                case SOUND_1:
                case SOUND_2:
                case SOUND_3:
                case SOUND_4:
                    globalKeyFramesStore.setSoundKeyFrames(type,
                            toTypedArray(keyFrames, com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame.class));
                    return;
                default: break;
            }
        }
        frames[KeyFrameType.getIndex(type)] = keyFrames;
    }

    /**
     * Copies a generic {@code KeyFrame[]} into a typed array of {@code T[]}.
     * The existing addKeyFrame path manipulates the array as {@code KeyFrame[]}
     * (uses ArrayUtils.insert / removeElement), so by the time we forward to
     * the typed setter we have a {@code KeyFrame[]} whose elements are all of
     * the right concrete subtype. Direct cast would ClassCast at runtime
     * (Java arrays are reified by element type); copy element-by-element
     * instead.
     */
    @SuppressWarnings("unchecked")
    private static <T extends KeyFrame> T[] toTypedArray(KeyFrame[] src, Class<T> elementType)
    {
        if (src == null)
        {
            return (T[]) java.lang.reflect.Array.newInstance(elementType, 0);
        }
        T[] out = (T[]) java.lang.reflect.Array.newInstance(elementType, src.length);
        for (int i = 0; i < src.length; i++)
        {
            out[i] = (T) src[i];
        }
        return out;
    }

    /**
     * find a keyframe of the given type at the given tick
     * @param type the type of keyframe to look for
     * @param tick the tick at which the requested keyframe should exist
     * @return the keyframe, if one is found; otherwise, returns null
     */
    public KeyFrame findKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] frames = getKeyFrames(type);
        if (frames == null)
        {
            return null;
        }

        for (KeyFrame keyFrame : frames)
        {
            if (keyFrame.getTick() == tick)
            {
                return keyFrame;
            }
        }

        return null;
    }

    public KeyFrame findFirstKeyFrame()
    {
        KeyFrame firstFrame = null;

        for (KeyFrame[] keyFrames : frames)
        {
            if (keyFrames == null)
            {
                continue;
            }

            for (KeyFrame keyFrame : keyFrames)
            {
                if (firstFrame == null)
                {
                    firstFrame = keyFrame;
                }

                if (keyFrame.getTick() < firstFrame.getTick())
                {
                    firstFrame = keyFrame;
                }
            }
        }

        return firstFrame;
    }

    public KeyFrame findLastKeyFrame()
    {
        KeyFrame lastFrame = null;

        for (KeyFrame[] keyFrames : frames)
        {
            if (keyFrames == null)
            {
                continue;
            }

            for (KeyFrame keyFrame : keyFrames)
            {
                if (lastFrame == null)
                {
                    lastFrame = keyFrame;
                }

                if (keyFrame.getTick() > lastFrame.getTick())
                {
                    lastFrame = keyFrame;
                }
            }
        }

        return lastFrame;
    }

    /**
     * Finds the next keyframe for this character of any given KeyFrameType, excluding any keyframes on the current tick
     * @param tick the tick at which to start searching
     * @return the next keyframe
     */
    public KeyFrame findNextKeyFrame(double tick)
    {
        KeyFrame nextFrame = null;

        for (KeyFrame[] keyFrames : frames)
        {
            if (keyFrames == null)
            {
                continue;
            }

            if (keyFrames.length == 0)
            {
                continue;
            }

            for (int i = keyFrames.length - 1; i >= 0; i--)
            {
                KeyFrame keyFrame = keyFrames[i];

                if (nextFrame == null)
                {
                    nextFrame = keyFrame;
                    continue;
                }

                if (nextFrame.getTick() <= tick)
                {
                    nextFrame = keyFrame;
                    continue;
                }

                double test = keyFrame.getTick();
                if (test <= tick)
                {
                    continue;
                }

                if (test < nextFrame.getTick())
                {
                    nextFrame = keyFrame;
                }
            }
        }

        if (nextFrame == null)
        {
            return null;
        }

        if (nextFrame.getTick() < tick)
        {
            return null;
        }

        return nextFrame;
    }

    /**
     * Finds the next keyframe for this character of the given KeyFrameType, excluding any keyframes on the current tick
     * @param type the KeyFrameType to look for
     * @param tick the tick at which to start looking
     * @return the next Keyframe of the given KeyFrameType
     */
    public KeyFrame findNextKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            return null;
        }

        if (keyFrames.length == 0)
        {
            return null;
        }

        for (int i = 0; i < keyFrames.length; i++)
        {
            KeyFrame keyFrame = keyFrames[i];
            if (keyFrame.getTick() > tick)
            {
                return keyFrames[i];
            }
        }

        return null;
    }

    /**
     * Finds the previous keyframe for this character of any given KeyFrameType, excluding any keyframes on the current tick
     * @param tick the tick at which to start searching
     * @return the previous keyframe
     */
    public KeyFrame findPreviousKeyFrame(double tick)
    {
        KeyFrame nextFrame = null;

        for (KeyFrame[] keyFrames : frames)
        {
            if (keyFrames == null)
            {
                continue;
            }

            if (keyFrames.length == 0)
            {
                continue;
            }

            for (int i = 0; i < keyFrames.length; i++)
            {
                KeyFrame keyFrame = keyFrames[i];

                double test = keyFrame.getTick();
                if (test >= tick)
                {
                    continue;
                }

                if (nextFrame == null)
                {
                    nextFrame = keyFrame;
                }

                if (test > nextFrame.getTick())
                {
                    nextFrame = keyFrame;
                }
            }
        }

        if (nextFrame == null)
        {
            return null;
        }

        if (nextFrame.getTick() > tick)
        {
            return null;
        }

        return nextFrame;
    }

    /**
     * Finds the last KeyFrame of the given type relative to the given time
     * @param type the type of KeyFrame to search for
     * @param tick the given time
     * @param includeCurrentKeyFrame whether to include the keyframe at the given time (if any), or to skip and find the keyframe before
     * @return the last keyframe relative to the given time
     */
    public KeyFrame findPreviousKeyFrame(KeyFrameType type, double tick, boolean includeCurrentKeyFrame)
    {
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            return null;
        }

        if (keyFrames.length == 0)
        {
            return null;
        }

        for (int i = 0; i < keyFrames.length; i++)
        {
            KeyFrame keyFrame = keyFrames[i];

            if (!includeCurrentKeyFrame && keyFrame.getTick() == tick)
            {
                if (i == 0)
                {
                    return null;
                }

                return keyFrames[i - 1];
            }

            if (keyFrame.getTick() > tick)
            {
                if (i == 0)
                {
                    return null;
                }

                return keyFrames[i - 1];
            }
        }

        return keyFrames[keyFrames.length - 1];
    }

    /**
     * Adds the keyframe to a specific character, or replaces a keyframe if the tick matches exactly
     * @param keyFrame the keyframe to add or modify for the character
     * @return the keyframe that is being replaced; null if there is no keyframe being replaced
     */
    public KeyFrame addKeyFrame(KeyFrame keyFrame, double currentTime)
    {
        KeyFrameType type = keyFrame.getKeyFrameType();
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            keyFrames = new KeyFrame[]{keyFrame};
            setKeyFrames(keyFrames, type);

            KeyFrame currentKeyFrame = findPreviousKeyFrame(type, currentTime, true);
            setCurrentKeyFrame(currentKeyFrame, type);
            return null;
        }

        int[] framePosition = getFramePosition(keyFrames, keyFrame.getTick());
        KeyFrame keyFrameToReplace = null;

        // Check first if the new keyframe is replacing a previous one
        if (framePosition[1] == 1)
        {
            keyFrameToReplace = keyFrames[framePosition[0]];
            keyFrames[framePosition[0]] = keyFrame;
        }
        else
        {
            keyFrames = ArrayUtils.insert(framePosition[0], keyFrames, keyFrame);
        }

        setKeyFrames(keyFrames, type);

        KeyFrame currentKeyFrame = findPreviousKeyFrame(type, currentTime, true);
        setCurrentKeyFrame(currentKeyFrame, type);
        return keyFrameToReplace;
    }

    /**
     * Removes the indicated keyframe from the character
     * @param keyFrame the keyframe to remove
     */
    public void removeKeyFrame(KeyFrame keyFrame)
    {
        KeyFrameType type = keyFrame.getKeyFrameType();
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            return;
        }

        keyFrames = ArrayUtils.removeElement(keyFrames, keyFrame);
        setKeyFrames(keyFrames, type);

        if (getCurrentKeyFrame(type) == keyFrame)
        {
            setCurrentKeyFrame(null, type);
        }
    }

    /**
     * Gets the new position of the keyframe to add as an int[] of {index, boolean}
     * @param keyFrames the keyframe array to add to
     * @param newTick the tick of the new keyframe to be added
     * @return an int[] of {index, boolean}. The boolean determines whether the new keyframe will replace a previously existing keyframe of the exact same tick
     */
    private int[] getFramePosition(KeyFrame[] keyFrames, double newTick)
    {
        if (keyFrames == null)
        {
            return new int[]{0, 0};
        }

        int frameIndex = 0;
        for (int i = 0; i < keyFrames.length; i++)
        {
            if (keyFrames[i].getTick() == newTick)
            {
                return new int[]{i, 1};
            }

            if (keyFrames[i].getTick() > newTick)
            {
                if (i == 0)
                {
                    return new int[]{0, 0};
                }

                return new int[]{i, 0};
            }

            frameIndex++;
        }

        return new int[]{frameIndex, 0};
    }

    public MovementKeyFrame[] getMovementKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.MOVEMENT);
        if (keyFrames == null)
        {
            return null;
        }

        MovementKeyFrame[] keyFrame = new MovementKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (MovementKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public ProjectileKeyFrame[] getProjectileKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.PROJECTILE);
        if (keyFrames == null)
        {
            return null;
        }

        ProjectileKeyFrame[] keyFrame = new ProjectileKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (ProjectileKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public AnimationKeyFrame[] getAnimationKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.ANIMATION);
        if (keyFrames == null)
        {
            return null;
        }

        AnimationKeyFrame[] keyFrame = new AnimationKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (AnimationKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public SpawnKeyFrame[] getSpawnKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.SPAWN);
        if (keyFrames == null)
        {
            return null;
        }

        SpawnKeyFrame[] keyFrame = new SpawnKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (SpawnKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public ModelKeyFrame[] getModelKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.MODEL);
        if (keyFrames == null)
        {
            return null;
        }

        ModelKeyFrame[] keyFrame = new ModelKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (ModelKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public OrientationKeyFrame[] getOrientationKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.ORIENTATION);
        if (keyFrames == null)
        {
            return null;
        }

        OrientationKeyFrame[] keyFrame = new OrientationKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (OrientationKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public TextKeyFrame[] getTextKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.TEXT);
        if (keyFrames == null)
        {
            return null;
        }

        TextKeyFrame[] keyFrame = new TextKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (TextKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public OverheadKeyFrame[] getOverheadKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.OVERHEAD);
        if (keyFrames == null)
        {
            return null;
        }

        OverheadKeyFrame[] keyFrame = new OverheadKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (OverheadKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public HealthKeyFrame[] getHealthKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.HEALTH);
        if (keyFrames == null)
        {
            return null;
        }

        HealthKeyFrame[] keyFrame = new HealthKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (HealthKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    public com.creatorskit.swing.timesheet.keyframe.ColourKeyFrame[] getColourKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.COLOUR);
        if (keyFrames == null)
        {
            return null;
        }

        com.creatorskit.swing.timesheet.keyframe.ColourKeyFrame[] keyFrame =
                new com.creatorskit.swing.timesheet.keyframe.ColourKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (com.creatorskit.swing.timesheet.keyframe.ColourKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }

    /**
     * Per-Character sound kfs (slot {@link KeyFrameType#SOUND}). Same shape
     * as the other per-type getters: returns null when the underlying array
     * is missing OR when every slot is null, so the save layer can write
     * {@code null} into pre-existing saves without bloating the JSON.
     */
    public SoundKeyFrame[] getSoundKeyFrames()
    {
        // Collect kfs from all 4 per-Character sound slots into a single
        // flat array (the save format stays 1D; each kf's slot field
        // tells the load path which frames[] index to restore it into).
        java.util.List<SoundKeyFrame> out = new java.util.ArrayList<>();
        for (KeyFrameType slot : KeyFrameType.LOCAL_SOUND_TYPES)
        {
            KeyFrame[] keyFrames = getKeyFrames(slot);
            if (keyFrames == null) continue;
            for (KeyFrame kf : keyFrames)
            {
                if (kf != null) out.add((SoundKeyFrame) kf);
            }
        }
        if (out.isEmpty()) return null;
        return out.toArray(new SoundKeyFrame[0]);
    }

    public ShieldKeyFrame[] getShieldKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.SHIELD);
        if (keyFrames == null)
        {
            return null;
        }
        ShieldKeyFrame[] out = new ShieldKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            out[i] = (ShieldKeyFrame) keyFrames[i];
        }
        if (Arrays.stream(out).allMatch(Objects::isNull))
        {
            return null;
        }
        return out;
    }

    public SpecialKeyFrame[] getSpecialKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.SPECIAL);
        if (keyFrames == null)
        {
            return null;
        }
        SpecialKeyFrame[] out = new SpecialKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            out[i] = (SpecialKeyFrame) keyFrames[i];
        }
        if (Arrays.stream(out).allMatch(Objects::isNull))
        {
            return null;
        }
        return out;
    }

    public ScreenFadeKeyFrame[] getScreenFadeKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.SCREEN_FADE);
        if (keyFrames == null)
        {
            return null;
        }
        ScreenFadeKeyFrame[] out = new ScreenFadeKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            out[i] = (ScreenFadeKeyFrame) keyFrames[i];
        }
        if (Arrays.stream(out).allMatch(Objects::isNull))
        {
            return null;
        }
        return out;
    }

    public ScreenShakeKeyFrame[] getScreenShakeKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.SCREEN_SHAKE);
        if (keyFrames == null)
        {
            return null;
        }
        ScreenShakeKeyFrame[] out = new ScreenShakeKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            out[i] = (ScreenShakeKeyFrame) keyFrames[i];
        }
        if (Arrays.stream(out).allMatch(Objects::isNull))
        {
            return null;
        }
        return out;
    }

    public CameraKeyFrame[] getCameraKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.CAMERA);
        if (keyFrames == null)
        {
            return null;
        }
        CameraKeyFrame[] out = new CameraKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            out[i] = (CameraKeyFrame) keyFrames[i];
        }
        if (Arrays.stream(out).allMatch(Objects::isNull))
        {
            return null;
        }
        return out;
    }

    public SpotAnimKeyFrame[] getSpotAnimKeyFrames(KeyFrameType spotAnimNumber)
    {
        KeyFrame[] keyFrames = getKeyFrames(spotAnimNumber);
        if (keyFrames == null)
        {
            return null;
        }

        SpotAnimKeyFrame[] keyFrame = new SpotAnimKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (SpotAnimKeyFrame) keyFrames[i];
        }

        if (Arrays.stream(keyFrame).allMatch(Objects::isNull))
        {
            return null;
        }

        return keyFrame;
    }


    public void toggleActive(ClientThread clientThread)
    {
        setActive(!active, !active, true, clientThread);
    }

    public void setVisible(boolean visible, ClientThread clientThread)
    {
        clientThread.invokeLater(() -> ckObject.setActive(visible));
    }

    public void resetActive(ClientThread clientThread)
    {
        setActive(active, active, active, clientThread);
    }

    public void setActive(boolean setActive, boolean shouldBeActive, boolean reset, ClientThread clientThread)
    {
        active = shouldBeActive;

        clientThread.invokeLater(() ->
        {
            if (setActive)
            {
                if (reset)
                {
                    ckObject.setActive(false);
                }
                ckObject.setActive(true);
                spawnCheckBox.setSelected(true);
                return;
            }

            ckObject.setActive(false);
            spawnCheckBox.setSelected(false);
        });
    }

    public HitsplatKeyFrame[] getHitsplatKeyFrames(KeyFrameType hitsplatType)
    {
        KeyFrame[] keyFrames = getKeyFrames(hitsplatType);
        if (keyFrames == null)
        {
            return null;
        }

        HitsplatKeyFrame[] keyFrame = new HitsplatKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (HitsplatKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }
}
