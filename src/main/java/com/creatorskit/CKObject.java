package com.creatorskit;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;

@Getter
@Setter
public class CKObject extends RuneLiteObjectController
{
    private final Client client;
    private Model baseModel;
    private boolean freeze;
    private boolean playing;
    private boolean loop = true;
    private AnimationController animationController;
    private AnimationController poseAnimationController;

    public CKObject(Client client)
    {
        this.client = client;
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

    private void setupAnimController(int animId)
    {
        animationController = new AnimationController(client, animId);
        poseAnimationController = new AnimationController(client, -1);

        animationController.setOnFinished(e ->
        {
            if (loop)
            {
                setActive(true);
                animationController.loop();
            }
            else
            {
                setActive(false);
                animationController.reset();
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

    @Override
    public void tick(int ticksSinceLastFrame)
    {
        if (freeze)
        {
            return;
        }

        if (!playing)
        {
            //return;
        }

        if (animationController == null)
        {
            setupAnimController(-1);
        }

        animationController.tick(ticksSinceLastFrame);
    }

    @Override
    public Model getModel()
    {
        if (animationController != null)
        {
            return animationController.animate(this.baseModel, this.poseAnimationController);
        }
        else if (poseAnimationController != null)
        {
            return poseAnimationController.animate(this.baseModel);
        }
        else
        {
            return baseModel;
        }
    }

    public void setAnimation(int animId)
    {
        if (animationController == null)
        {
            setupAnimController(animId);
            return;
        }

        animationController.setAnimation(client.loadAnimation(animId));
    }

    public Animation getAnimation()
    {
        if (animationController == null)
        {
            setupAnimController(-1);
        }

        return animationController.getAnimation();
    }

    public void setAnimationFrame(int animFrame, boolean allowFreeze)
    {
        if (animationController == null)
        {
            return;
        }

        Animation animation = animationController.getAnimation();
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
                animationController.setFrame(0);
                return;
            }

            freeze = true;
            animationController.setFrame(animFrame);
            return;
        }

        freeze = false;
        animationController.setFrame(animFrame);
    }

    public int getAnimationFrame()
    {
        if (animationController == null)
        {
            return 0;
        }

        Animation animation = animationController.getAnimation();
        if (animation == null)
        {
            return 0;
        }

        return animationController.getFrame();
    }

    public int getMaxAnimFrames()
    {
        if (animationController == null)
        {
            return 0;
        }

        Animation animation = animationController.getAnimation();
        if (animation == null)
        {
            return 0;
        }

        return animationController.getAnimation().getDuration();
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
}
