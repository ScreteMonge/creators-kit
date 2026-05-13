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
     * The display scale of this model in 1/128 units, as passed to ModelData.scale at
     * construction. 128 means "no fix needed" (model already matches rigging). 300
     * (Sol Heredit's cache value) means "shrink to 128 for animation, expand to 300
     * for display". Captured by callers that build the model (the projectile loader,
     * the spotanim loader, etc.) when they know the scale; defaults to 128 so the
     * renderFix path is a no-op until a real scale is set.
     */
    private int displayScale = 128;

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
                && displayScale != RIGGING_SCALE
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
     * Resets baseModel to its snapshotted original vertices, then uniformly scales it
     * down so the mesh radii match the bones' canonical 128-scale distances when the
     * animation pipeline runs against it. Mesh.scale(s) multiplies vertices by s/128,
     * so we pass {@code (128 * RIGGING_SCALE) / displayScale} to get a factor of
     * {@code RIGGING_SCALE / displayScale}. The reset step is critical: without it,
     * Mesh.scale's int truncation compounds ~0.7%/frame at 300:128 and baseModel
     * would drift far from its original size after a few seconds of playback.
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
        if (displayScale <= 0)
        {
            return;
        }
        int s = Math.max(1, (128 * RIGGING_SCALE) / displayScale);
        baseModel.scale(s, s, s);
    }

    /**
     * Inverse of {@link #shrinkBaseModelForAnimation}: scales the just-animated mesh by
     * {@code displayScale / RIGGING_SCALE} so the rendered size matches the user's
     * intended model size while the animation was computed at the rigging-correct
     * smaller scale. Mesh.scale(s) multiplies by s/128, so passing {@code displayScale}
     * gives a factor of {@code displayScale / 128} -- exactly the inverse of the
     * shrink, because the model's "natural" display scale is also normalized against
     * the 128 reference.
     */
    private void expandAnimatedModel(Model animated)
    {
        animated.scale(displayScale, displayScale, displayScale);
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
