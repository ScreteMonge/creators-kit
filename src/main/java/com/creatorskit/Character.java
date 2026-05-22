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
     * User-defined block groupings for this Character's keyframes. Empty
     * (not null) for fresh Characters and for pre-blocks-feature saves. See
     * {@link com.creatorskit.swing.timesheet.keyframe.Block} for the data
     * model and {@link com.creatorskit.swing.timesheet.keyframe.BlockValidator}
     * for the no-gaps + no-overlap rules a candidate selection must satisfy.
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
        currentFrames[KeyFrameType.getIndex(type)] = keyFrame;
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

    private static boolean isGlobalType(KeyFrameType type)
    {
        return type == KeyFrameType.CAMERA
                || type == KeyFrameType.SCREEN_FADE
                || type == KeyFrameType.SCREEN_SHAKE;
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
