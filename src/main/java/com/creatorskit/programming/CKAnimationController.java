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
}
