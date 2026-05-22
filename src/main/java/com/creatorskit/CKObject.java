package com.creatorskit;

import com.creatorskit.programming.AnimationType;
import com.creatorskit.programming.CKAnimationController;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;

import java.util.Random;

@Getter
@Setter
public class CKObject extends RuneLiteObjectController
{
    private final Client client;
    private Model baseModel;
    private boolean freeze;
    private boolean playing;
    private boolean hasAnimKeyFrame;
    private boolean despawnOnFinish;
    private Animation activeAnimation;
    private CKAnimationController animationController;
    private CKAnimationController poseAnimationController;

    /**
     * When true, getModel() bracket-scales the model around the animation call so the
     * mesh is at the rigging's canonical 128 scale while animation runs, then scaled
     * back up to its display size for rendering. Fixes the "warped / drifting parts"
     * artifact on models whose cache resizeX/Y/Z pushes them above 128 (e.g. Sol
     * Heredit at 300) -- bones are stored at the 128 rigging scale, so vertices at
     * 300 swing in arcs ~2.3x the rigging's intended radius and the animation looks
     * exaggerated / torn. Running the animation against a temporarily-shrunk mesh
     * matches bone radii to vertex distances, then post-animation upscale restores
     * the visual size. Toggleable per-Character because well-behaved models that
     * already match the rigging don't need (and would round-trip lossily through)
     * the bracket scale.
     */
    private boolean renderFix;

    /**
     * Horizontal display scale of this model in 1/128 units, as passed to
     * {@code ModelData.scale} at construction (and as exposed by
     * {@code NPCComposition.getWidthScale()}). 128 means "matches the rigging's
     * canonical 1 tile = 128 units"; values like 300 (Sol Heredit) or 200 mean the
     * cache shipped the model pre-scaled. The render-fix bracket uses
     * {@code 128 / widthScale} as the pre-animation shrink factor on X and Z, then
     * {@code widthScale / 128} as the post-animation expand factor.
     */
    private int widthScale = 128;

    /** Vertical companion to {@link #widthScale}; matches NPCComposition.getHeightScale(). */
    private int heightScale = 128;

    /**
     * User-controlled uniform extra scale multiplier applied on top of widthScale /
     * heightScale. 1.0 = no change. Driven by the ALT+Scroll hotkey; persists in
     * {@code CharacterSave}. Multiplied into the render-fix bracket's post-animation
     * expand step so animation still runs at the canonical rigging scale and only
     * the final display size changes -- works whether or not the cache-shipped
     * scales differ from 128.
     */
    private double extraScale = 1.0;

    /**
     * Animation playback-rate multiplier. 1.0 = native. Set by Programmer when an
     * AnimationKeyFrame activates (so different keyframes can run at different
     * speeds). Drives the fractional accumulator in {@link #tick} so even
     * sub-integer speeds (e.g. 0.5) advance correctly over multiple frames
     * instead of rounding to zero and freezing.
     */
    private double animationSpeed = 1.0;
    /** Fractional carryover for sub-integer animationSpeed scaling. */
    private double animationTickAccumulator = 0;

    /** Canonical rigging scale baked into OSRS animation data (1/128 units = 1 tile). */
    private static final int RIGGING_SCALE = 128;

    /**
     * Snapshot of baseModel's vertices captured when {@link #setModel(Model)} is called,
     * so each frame's render-fix pass can start from a clean baseline instead of
     * compounding scale operations across frames (Mesh.scale's int truncation
     * accumulates 0.7%/frame at 300:128 -- after a few seconds baseModel would drift
     * far from its original size). Null until renderFix actually engages.
     */
    private float[][] baseVerticesSnapshot;

    /**
     * User-set sub-tile position offsets in scene units, applied after the tile-
     * aligned base position in {@link #setLocation(LocalPoint, int)}. Driven by the
     * Character's ALT+WASD/R/F nudge hotkeys; mirrored from Character.offsetX/Y/Z
     * so the values stay in sync. Z is "user-positive = up"; subtracted when applied
     * because OSRS render space treats negative z as up.
     */
    private int offsetX;
    private int offsetY;
    private int offsetZ;

    public CKObject(Client client)
    {
        this.client = client;
    }

    private int startCycle;

    public void setModel(Model baseModel)
    {
        this.baseModel = baseModel;
        if (baseModel == null)
        {
            this.baseVerticesSnapshot = null;
            return;
        }
        // Synchronize on baseModel itself so other ckObjects sharing this same
        // Model reference (e.g. multi-selected Characters all pointing at the
        // same CustomModel.getModel()) can't have their render-fix bracket
        // running mid-mutation on baseModel.vertices while we snapshot here.
        // Without this, EDT setModel could clone the shrunken vertices that the
        // render thread is in the middle of using -- the snapshot would then
        // capture the shrunken state, and subsequent brackets restore-from-
        // snapshot would re-shrink before scaling again, compounding the
        // distortion every time Update / scrub fires setModel.
        synchronized (baseModel)
        {
            float[] vx = baseModel.getVerticesX();
            float[] vy = baseModel.getVerticesY();
            float[] vz = baseModel.getVerticesZ();
            if (vx == null || vy == null || vz == null)
            {
                this.baseVerticesSnapshot = null;
                return;
            }
            this.baseVerticesSnapshot = new float[][]{vx.clone(), vy.clone(), vz.clone()};
        }
    }

    @Override
    public void setLocation(LocalPoint point, int level)
    {
        if (point == null)
        {
            return;
        }

        super.setLocation(point, level);
        setZ(Perspective.getTileHeight(client, point, level));

        // Apply user-set sub-tile offsets AFTER the tile-aligned base position. Done
        // here (in the central setLocation override) so the offsets follow the
        // Character through every code path that re-positions it -- static placement,
        // movement keyframes, animation transforms -- rather than detaching when one
        // of those paths runs and silently overwrites x/y/z.
        if (offsetX != 0)
        {
            setX(getX() + offsetX);
        }
        if (offsetY != 0)
        {
            setY(getY() + offsetY);
        }
        if (offsetZ != 0)
        {
            // OSRS render space has negative z = up; the user-facing offsetZ is
            // "positive = up" for intuitive hotkey direction (R = up, F = down).
            setZ(getZ() - offsetZ);
        }

        /*
        boolean needReregister = isActive() && point.getWorldView() != getWorldView();
        if (needReregister)
        {
            setActive(false);
        }

        super.setLocation(point, level);
        setZ(Perspective.getTileHeight(client, point, level));

        if (needReregister)
        {
            setActive(true);
        }

         */
    }

    public void setupAnimController(AnimationType type, int animId)
    {
        if (type == AnimationType.ACTIVE)
        {
            animationController = new CKAnimationController(client, animId, false);
            setOnFinished(AnimationType.ACTIVE);
        }
        else
        {
            poseAnimationController = new CKAnimationController(client, animId, true);
            setOnFinished(AnimationType.POSE);
        }
    }

    /**
     * Pushes the per-keyframe play-range overrides into the Active animation
     * controller (only -- pose plays naturally regardless of range). Resets the
     * range playback clock so the first tick after the activation lands on
     * {@code firstFrame}.
     *
     * <p>Called from Programmer.registerActiveAnimationChanges whenever the
     * active AnimationKeyFrame changes, and called with 0/0/0 from the no-
     * keyframe branch so a previously-active range doesn't leak into the
     * spinner-driven default playback.
     */
    public void setAnimationRange(int firstFrame, int lastFrame, int pauseTicks)
    {
        if (animationController != null)
        {
            animationController.setFirstFrameOverride(firstFrame);
            animationController.setLastFrameOverride(lastFrame);
            animationController.setPauseTicks(pauseTicks);
            animationController.resetRangeClock();
        }
    }

    public void setupAnimController(AnimationType type, Animation animation)
    {
        if (type == AnimationType.ACTIVE)
        {
            animationController = new CKAnimationController(client, animation, false);
            setOnFinished(AnimationType.ACTIVE);
        }
        else
        {
            poseAnimationController = new CKAnimationController(client, animation, true);
            setOnFinished(AnimationType.POSE);
        }
    }

    private void setOnFinished(AnimationType type)
    {
        CKAnimationController ac = getController(type);
        ac.setOnFinished(e ->
        {
            if (ac.isLoop())
            {
                setActive(true);
                ac.setFinished(false);
                ac.loop();
            }
            else
            {
                ac.setFinished(true);
                setAnimation(type, -1);
            }

            if (ac.isFinished() && despawnOnFinish)
            {
                setActive(false);
            }
        });
    }

    public void setActive(boolean active)
    {
        if (active)
        {
            client.registerRuneLiteObject(this);
        }
        else
        {
            client.removeRuneLiteObject(this);
        }
    }

    public boolean isActive()
    {
        return client.isRuneLiteObjectRegistered(this);
    }

    public void setLoop(boolean loop)
    {
        if (animationController == null)
        {
            return;
        }

        animationController.setLoop(loop);
    }

    @Override
    public void tick(int ticksSinceLastFrame)
    {
        if (!playing && hasAnimKeyFrame)
        {
            return;
        }

        if (animationController == null)
        {
            setupAnimController(AnimationType.ACTIVE, -1);
        }

        if (poseAnimationController == null)
        {
            setupAnimController(AnimationType.POSE, -1);
        }

        if (!freeze)
        {
            // animationSpeed scales the tick count so animations can run faster /
            // slower than native. Accumulator captures the fractional remainder so
            // sub-integer speeds (e.g. 0.5) don't floor-to-zero and freeze the
            // animation -- with speed=0.5 we tick by 0 then 1 then 0 then 1 etc.
            int scaledTicks;
            if (animationSpeed == 1.0)
            {
                scaledTicks = ticksSinceLastFrame;
            }
            else
            {
                animationTickAccumulator += ticksSinceLastFrame * animationSpeed;
                scaledTicks = (int) animationTickAccumulator;
                animationTickAccumulator -= scaledTicks;
            }
            if (scaledTicks > 0)
            {
                animationController.tick(scaledTicks);
                poseAnimationController.tick(scaledTicks);
            }
        }
    }

    @Override
    public Model getModel()
    {
        // Bracket engages whenever EITHER (a) renderFix is on AND the cache shipped a
        // non-canonical scale (the original rigging-correction use-case) OR (b) the
        // user has set a non-1.0 extraScale -- the bracket is also how we apply
        // user up/down-scaling, since baking it into the post-animation expand step
        // means animation still runs against the canonical rigging mesh.
        boolean fix = baseModel != null
                && (animationController != null || poseAnimationController != null)
                && widthScale > 0
                && heightScale > 0
                && (
                    (renderFix && (widthScale != RIGGING_SCALE || heightScale != RIGGING_SCALE))
                    || extraScale != 1.0
                );

        if (!fix)
        {
            // Fast path: no shrink/restore needed, no mutation of shared baseModel.
            if (animationController != null)
            {
                return applyColourTintIfAny(animationController.animate(this.baseModel, this.poseAnimationController));
            }
            if (poseAnimationController != null)
            {
                return applyColourTintIfAny(poseAnimationController.animate(this.baseModel));
            }
            return applyColourTintIfAny(baseModel);
        }

        // Synchronize the entire bracket on baseModel so another ckObject sharing
        // this Model reference can't snapshot or render while baseModel is mid-
        // shrink. Without this lock, multi-selected Characters with the same
        // CustomModel would race: one ckObject's bracket leaves baseModel in the
        // shrunken state for a few cpu cycles, and the other ckObject's setModel
        // (EDT) or getModel (render) catches that shrunken state -- the snapshot
        // then captures a shrunken baseline, the next bracket restore-from-
        // snapshot re-shrinks, and the expand step compounds the distortion
        // every time Update / scrub fires setModel.
        synchronized (baseModel)
        {
            shrinkBaseModelForAnimation();
            Model model;
            if (animationController != null)
            {
                model = animationController.animate(this.baseModel, this.poseAnimationController);
            }
            else
            {
                model = poseAnimationController.animate(this.baseModel);
            }
            // Restore baseModel BEFORE expanding the animated output. The animated
            // model is a fresh copy from applyTransformations, so expanding it doesn't
            // depend on baseModel anymore -- but baseModel needs to be back to its
            // original size before this method returns. Otherwise anything else that
            // shares the same Model instance (e.g. the Ctrl-hover preview, which
            // points at the same CustomModel.getModel() reference) reads the shrunken
            // state between frames and renders at the rigging size instead of the
            // intended display size.
            restoreBaseModelToSnapshot();
            if (model != null && model != baseModel)
            {
                expandAnimatedModel(model);
            }
            return applyColourTintIfAny(model);
        }
    }

    /**
     * Per-CKObject colour tint, set by ColourController during a Colour
     * keyframe envelope and cleared on envelope end. When non-null,
     * {@link #getModel} routes its post-animation output through the
     * tint -- the tint clones the animated frame via {@code mergeModels}
     * and writes a per-face HSL blend into the clone, so the mutation
     * doesn't leak to other Characters sharing the same baseModel.
     *
     * <p>Same idea as ProjectileCKObject's pitch hook (which deep-copies
     * the animated frame to apply pitch without disturbing the shared
     * baseModel) -- just for colour instead of geometry.
     */
    @Setter
    @Getter
    private com.creatorskit.programming.ColourTint colourTint;

    /**
     * Returns {@code raw} unchanged when no tint is active, otherwise the
     * tint's per-frame deep-copy with the lerped face colours applied.
     * Always safe to call -- null tint or null model both short-circuit
     * to the raw input.
     */
    private Model applyColourTintIfAny(Model raw)
    {
        com.creatorskit.programming.ColourTint tint = this.colourTint;
        if (tint == null || raw == null) return raw;
        Model tinted = tint.applyToFrame(client, raw);
        return tinted == null ? raw : tinted;
    }

    /**
     * Resets baseModel to its snapshotted vertices, then scales it per-axis so each
     * dimension matches the bones' canonical 128-scale distances when the animation
     * pipeline runs against it. Mesh.scale(x, y, z) multiplies vertex i by
     * {@code (x/128, y/128, z/128)}, so passing {@code 128 * 128 / widthScale} (etc.)
     * achieves a factor of {@code 128 / widthScale}. The reset step is critical:
     * Mesh.scale's int truncation compounds ~0.7%/frame at 300:128, so without
     * restoring from snapshot baseModel would drift far from its original size
     * within a few seconds of playback.
     */
    private void shrinkBaseModelForAnimation()
    {
        float[] vx = baseModel.getVerticesX();
        float[] vy = baseModel.getVerticesY();
        float[] vz = baseModel.getVerticesZ();
        if (vx == null || vy == null || vz == null)
        {
            return;
        }
        // Snapshot is captured eagerly in setModel now -- if it's missing here it
        // means baseModel was swapped out from under us by something that bypassed
        // setModel (shouldn't happen). Skip rather than capturing the current state,
        // which might be mid-mutation from another ckObject sharing the same Model
        // reference (the original lazy-capture was the source of the multi-select
        // upscale-on-scrub compounding bug).
        if (baseVerticesSnapshot == null
                || baseVerticesSnapshot[0].length != vx.length)
        {
            return;
        }
        // Restore from snapshot so the next scale() starts from the original size.
        System.arraycopy(baseVerticesSnapshot[0], 0, vx, 0, vx.length);
        System.arraycopy(baseVerticesSnapshot[1], 0, vy, 0, vy.length);
        System.arraycopy(baseVerticesSnapshot[2], 0, vz, 0, vz.length);
        int sx = Math.max(1, (128 * RIGGING_SCALE) / widthScale);
        int sy = Math.max(1, (128 * RIGGING_SCALE) / heightScale);
        int sz = sx; // Z mirrors X by OSRS convention (NPCComposition has no z-axis scale).
        baseModel.scale(sx, sy, sz);
    }

    /**
     * Inverse of {@link #shrinkBaseModelForAnimation}: scales the just-animated mesh by
     * {@code widthScale / 128} on X/Z and {@code heightScale / 128} on Y, restoring the
     * model to its display size while preserving the animation that was just computed
     * at the rigging-correct smaller scale. Mesh.scale(s) multiplies by s/128, so
     * passing {@code widthScale} directly gives the right factor.
     *
     * <p>{@link #extraScale} is multiplied in here so user up/down-scaling composes
     * cleanly with the rigging correction -- the rigging still ran at canonical 128,
     * we just expand to (display * userScale) instead of plain display.
     */
    private void expandAnimatedModel(Model animated)
    {
        int sx = Math.max(1, (int) Math.round(widthScale * extraScale));
        int sy = Math.max(1, (int) Math.round(heightScale * extraScale));
        animated.scale(sx, sy, sx);
    }

    /**
     * Copies the snapshotted vertices back into baseModel without rescaling. Called
     * after the animation pipeline runs so baseModel returns to its original display-
     * scale state before this getModel() invocation returns -- otherwise other readers
     * of the same Model instance (sharing via CustomModel.getModel()) would observe
     * the shrunken vertices we left behind for the animate() call. Cheap: just three
     * System.arraycopy operations on already-sized vertex arrays.
     */
    private void restoreBaseModelToSnapshot()
    {
        if (baseVerticesSnapshot == null || baseModel == null)
        {
            return;
        }
        float[] vx = baseModel.getVerticesX();
        float[] vy = baseModel.getVerticesY();
        float[] vz = baseModel.getVerticesZ();
        if (vx == null || vy == null || vz == null
                || vx.length != baseVerticesSnapshot[0].length)
        {
            return;
        }
        System.arraycopy(baseVerticesSnapshot[0], 0, vx, 0, vx.length);
        System.arraycopy(baseVerticesSnapshot[1], 0, vy, 0, vy.length);
        System.arraycopy(baseVerticesSnapshot[2], 0, vz, 0, vz.length);
    }

    public boolean isFinished()
    {
        if (animationController == null)
        {
            return true;
        }

        return animationController.isFinished();
    }

    public void setFinished(boolean finished)
    {
        if (animationController == null)
        {
            return;
        }

        animationController.setFinished(finished);
    }

    public void unsetAnimation(AnimationType type)
    {
        CKAnimationController ac = getController(type);
        if (ac == null)
        {
            setupAnimController(type, -1);
            return;
        }

        ac.setAnimation(client.loadAnimation(-1));
    }

    public void setAnimation(AnimationType type, Animation animation)
    {
        CKAnimationController ac = getController(type);
        if (ac == null)
        {
            setupAnimController(type, animation);
            return;
        }

        ac.setAnimation(animation);
    }

    public void setAnimation(AnimationType type, int animId)
    {
        CKAnimationController ac = getController(type);
        if (ac == null)
        {
            setupAnimController(type, animId);
            return;
        }

        ac.setAnimation(client.loadAnimation(animId));
    }

    public Animation[] getAnimations()
    {
        if (animationController == null)
        {
            setupAnimController(AnimationType.ACTIVE, -1);
        }

        if (poseAnimationController == null)
        {
            setupAnimController(AnimationType.POSE, -1);
        }

        return new Animation[]{animationController.getAnimation(), poseAnimationController.getAnimation()};
    }

    public void setAnimationFrame(AnimationType type, int animFrame, Random random, boolean randomizeStartFrame, boolean allowFreeze)
    {
        CKAnimationController ac = getController(type);
        if (ac == null)
        {
            setupAnimController(type, -1);
        }

        Animation animation = ac.getAnimation();
        if (animation == null)
        {
            return;
        }

        if (randomizeStartFrame)
        {
            animFrame = random.nextInt(animation.getNumFrames());
        }

        if (animFrame >= animation.getDuration())
        {
            animFrame = animation.getDuration() - 1;
        }

        if (allowFreeze)
        {
            if (animFrame == -1)
            {
                freeze = false;
                ac.setFrame(0);
                return;
            }

            freeze = true;
            ac.setFrame(animFrame);
            return;
        }

        freeze = false;

        if (animFrame == -1)
        {
            return;
        }

        ac.setFrame(animFrame);
    }

    public int getAnimationFrame(AnimationType type)
    {
        CKAnimationController ac = getController(type);
        if (ac == null)
        {
            return 0;
        }

        Animation animation = ac.getAnimation();
        if (animation == null)
        {
            return 0;
        }

        return animationController.getFrame();
    }

    public int getMaxAnimFrames(AnimationType type)
    {
        CKAnimationController ac = getController(type);
        if (ac == null)
        {
            return 0;
        }

        Animation animation = ac.getAnimation();
        if (animation == null)
        {
            return 0;
        }

        return animation.getDuration();
    }

    public int getAnimationId()
    {
        if (animationController != null)
        {
            Animation animation = animationController.getAnimation();
            if (animation != null)
            {
                return animation.getId();
            }
        }

        if (poseAnimationController != null)
        {
            Animation animation = poseAnimationController.getAnimation();
            if (animation != null)
            {
                return animation.getId();
            }
        }

        return -1;
    }

    private CKAnimationController getController(AnimationType type)
    {
        if (type == AnimationType.ACTIVE)
        {
            return animationController;
        }

        return poseAnimationController;
    }
}
