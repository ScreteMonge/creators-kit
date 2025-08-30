package com.creatorskit;

import com.creatorskit.programming.AnimationType;
import com.creatorskit.programming.CKAnimationController;
import com.creatorskit.swing.timesheet.SpotAnim;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import org.apache.commons.lang3.ArrayUtils;

@Getter
@Setter
public class CKObject extends RuneLiteObjectController
{
    private final Client client;
    private Model baseModel;
    private SpotAnim spotAnim1;
    private SpotAnim spotAnim2;
    private boolean freeze;
    private boolean playing;
    private boolean hasAnimKeyFrame;
    private boolean despawnOnFinish;
    private Animation activeAnimation;
    private CKAnimationController animationController;
    private CKAnimationController poseAnimationController;

    public CKObject(Client client)
    {
        this.client = client;
        spotAnim1 = new SpotAnim(client);
        spotAnim2 = new SpotAnim(client);
    }

    private int startCycle;

    public void setModel(Model baseModel)
    {
        this.baseModel = baseModel;
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

        if (!spotAnim1.isAnimationSetup())
        {
            spotAnim1.setupAnimationController(client);
        }

        if (!spotAnim2.isAnimationSetup())
        {
            spotAnim2.setupAnimationController(client);
        }

        if (!freeze)
        {
            animationController.tick(ticksSinceLastFrame);
            poseAnimationController.tick(ticksSinceLastFrame);
        }

        if (playing)
        {
            spotAnim1.tick(ticksSinceLastFrame);
            spotAnim2.tick(ticksSinceLastFrame);
        }
    }

    @Override
    public Model getModel()
    {
        Model[] models = new Model[1];

        if (animationController != null)
        {
            models[0] = animationController.animate(this.baseModel, this.poseAnimationController);
        }
        else if (poseAnimationController != null)
        {
            models[0] = poseAnimationController.animate(this.baseModel);
        }

        if (spotAnim1.getId() != -1 && !spotAnim1.isFinished())
        {
            models = ArrayUtils.add(models, spotAnim1.getModel());
        }

        if (spotAnim2.getId() != -1 && !spotAnim2.isFinished())
        {
            models = ArrayUtils.add(models, spotAnim2.getModel());
        }

        return client.mergeModels(models, models.length);
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

    public void setAnimationFrame(AnimationType type, int animFrame, boolean allowFreeze)
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
