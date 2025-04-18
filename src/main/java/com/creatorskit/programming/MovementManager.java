package com.creatorskit.programming;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.swing.timesheet.keyframe.MovementKeyFrame;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.util.Arrays;

public class MovementManager
{
    private Client client;
    private CreatorsConfig config;
    private PathFinder pathFinder;
    private static final int[] POH_REGIONS = new int[]{7257, 7513, 7514, 7769, 7770, 8025, 8026};
    private static final int[] GAUNTLET_REGIONS = new int[]{7512, 7768};

    @Inject
    public MovementManager(Client client, CreatorsConfig config, PathFinder pathFinder)
    {
        this.client = client;
        this.config = config;
        this.pathFinder = pathFinder;
    }

    public void addProgramStep(MovementKeyFrame keyFrame, WorldView worldView, LocalPoint localPoint)
    {
        if (localPoint == null)
        {
            return;
        }

        if (useLocalLocations(worldView))
        {
            addPOHStep(keyFrame, worldView, localPoint);
            return;
        }

        addWorldStep(keyFrame, worldView, WorldPoint.fromLocalInstance(client, localPoint));
    }

    public void addWorldStep(MovementKeyFrame keyFrame, WorldView worldView, WorldPoint worldPoint)
    {
        int[][] path = keyFrame.getPath();
        if (path.length == 0)
        {
            path = new int[][]{new int[]{worldPoint.getX(), worldPoint.getY()}};
            keyFrame.setPath(path);
            return;
        }

        int[] lastStep = path[path.length - 1];
        int[][] stepsToAdd = new int[0][]; //pathFinder.findPath(worldView, new WorldPoint(lastStep[0], lastStep[1], worldView.getPlane()), worldPoint, config.movementAlgorithm());

        path = ArrayUtils.addAll(path, stepsToAdd);
        keyFrame.setPath(path);
    }

    public void addPOHStep(MovementKeyFrame keyFrame, WorldView worldView, LocalPoint localPoint)
    {
        int[][] path = keyFrame.getPath();
        if (path.length == 0)
        {
            path = new int[][]{new int[]{localPoint.getSceneX(), localPoint.getSceneY()}};
            keyFrame.setPath(path);
            return;
        }

        int[] lastStep = path[path.length - 1];
        int[][] stepsToAdd = new int[0][]; //pathFinder.findPath(LocalPoint.fromScene(lastStep[0], lastStep[1], worldView), localPoint, config.movementAlgorithm());

        path = ArrayUtils.addAll(path, stepsToAdd);
        keyFrame.setPath(path);
    }

    public static boolean useLocalLocations(WorldView worldView)
    {
        if (Arrays.stream(worldView.getMapRegions()).anyMatch(x -> Arrays.stream(POH_REGIONS).anyMatch(y -> y == x)))
        {
            return true;
        }

        if (Arrays.stream(worldView.getMapRegions()).anyMatch(x -> Arrays.stream(GAUNTLET_REGIONS).anyMatch(y -> y == x)))
        {
            return true;
        }

        return false;
    }
}
