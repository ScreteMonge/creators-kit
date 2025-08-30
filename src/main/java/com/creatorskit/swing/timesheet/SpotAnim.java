package com.creatorskit.swing.timesheet;

import com.creatorskit.programming.CKAnimationController;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;

@Getter
@Setter
public class SpotAnim
{
    private int id = -1;
    private Model baseModel;
    private CKAnimationController ckAnimationController;
    private boolean animationSetup = false;

    public SpotAnim(Client client)
    {
        setupAnimationController(client);
    }

    public void setupAnimationController(Client client)
    {
        ckAnimationController = new CKAnimationController(client, -1, false);
        ckAnimationController.setOnFinished(e -> ckAnimationController.setFinished(true));
        animationSetup = true;
    }

    public void tick(int ticksSinceLastFrame)
    {
        if (id == -1)
        {
            return;
        }

        ckAnimationController.tick(ticksSinceLastFrame);
    }

    public boolean isFinished()
    {
        return ckAnimationController.isFinished();
    }

    public Model getModel()
    {
        return ckAnimationController.animate(this.baseModel);
    }
}
