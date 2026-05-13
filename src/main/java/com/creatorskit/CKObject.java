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

    public CKObject(Client client)
    {
        this.client = client;
    }

    private int startCycle;

    public void setModel(Model baseModel)
    {
        this.baseModel = baseModel;
        // Invalidate the previous snapshot -- a new model needs a fresh baseline before
        // the render-fix pass can shrink + restore it without compounding drift.
        this.baseVerticesSnapshot = null;
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
            animationController.tick(ticksSinceLastFrame);
            poseAnimationController.tick(ticksSinceLastFrame);
        }
    }

    @Override
    public Model getModel()
    {
        boolean fix = renderFix
                && baseModel != null
                && (widthScale != RIGGING_SCALE || heightScale != RIGGING_SCALE)
                && widthScale > 0
                && heightScale > 0
                && (animationController != null || poseAnimationController != null);

        if (fix)
        {
            shrinkBaseModelForAnimation();
        }

        Model model;
        if (animationController != null)
        {
            model = animationController.animate(this.baseModel, this.poseAnimationController);
        }
        else if (poseAnimationController != null)
        {
            model = poseAnimationController.animate(this.baseModel);
        }
        else
        {
            model = baseModel;
        }

        if (fix && model != null)
        {
            expandAnimatedModel(model);
        }

        return model;
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
        if (baseVerticesSnapshot == null
                || baseVerticesSnapshot[0].length != vx.length)
        {
            baseVerticesSnapshot = new float[][]{vx.clone(), vy.clone(), vz.clone()};
        }
        else
        {
            // Restore from snapshot so the next scale() starts from the original size.
            System.arraycopy(baseVerticesSnapshot[0], 0, vx, 0, vx.length);
            System.arraycopy(baseVerticesSnapshot[1], 0, vy, 0, vy.length);
            System.arraycopy(baseVerticesSnapshot[2], 0, vz, 0, vz.length);
        }
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
     */
    private void expandAnimatedModel(Model animated)
    {
        animated.scale(widthScale, heightScale, widthScale);
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
