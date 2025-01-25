package com.creatorskit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;

import javax.annotation.Nullable;

@Getter
@Setter
@RequiredArgsConstructor
public class CKObject extends RuneLiteObjectController
{
    private final Client client;
    private Model baseModel;
    private boolean pause;

    @Nullable
    private AnimationController animationController;

    @Nullable
    private AnimationController poseAnimationController;

    private int startCycle;

    public void setModel(Model baseModel)
    {
        this.baseModel = baseModel;
    }

    @Override
    public void setLocation(LocalPoint point, int level)
    {
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
    }

    public void setAnimationController(@Nullable AnimationController animationController)
    {
        this.animationController = animationController;
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
        if (pause)
        {
            return;
        }

        if (animationController != null)
        {
            animationController.tick(ticksSinceLastFrame);
        }
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
            animationController = new AnimationController(client, animId);
            return;
        }

        animationController.setAnimation(client.loadAnimation(animId));
    }

    public void setAnimationFrame(int animFrame, boolean allowPause)
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

        if (allowPause)
        {
            if (animFrame == -1)
            {
                pause = false;
                animationController.setFrame(0);
                return;
            }

            pause = true;
            animationController.setFrame(animFrame);
            return;
        }

        pause = false;
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
