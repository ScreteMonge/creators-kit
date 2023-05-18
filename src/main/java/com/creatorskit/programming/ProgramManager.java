package com.creatorskit.programming;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.NPCCharacter;
import net.runelite.api.Client;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;

import javax.inject.Inject;

public class ProgramManager
{
    @Inject
    private CreatorsPlugin plugin;

    @Inject
    private Client client;

    public void moveObject(NPCCharacter npcCharacter)
    {
        Program program = npcCharacter.getProgram();
        if (program == null || !npcCharacter.isMoving())
        {
            return;
        }

        LocalPoint endTile = program.getEndLocation();
        double speed = 128 / program.getSpeed();
        RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
        LocalPoint lp = runeLiteObject.getLocation();

        if (lp.distanceTo(endTile) <= speed)
        {
            runeLiteObject.setLocation(endTile, client.getPlane());
            return;
        }

        LocalPoint finalPoint = new LocalPoint(lp.getX() + (int) speed, lp.getY() + (int) speed);
        runeLiteObject.setLocation(finalPoint, client.getPlane());
    }
}
