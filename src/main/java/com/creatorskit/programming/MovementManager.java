package com.creatorskit.programming;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
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

    public int[][] addProgramStep(MovementKeyFrame keyFrame, WorldView worldView, LocalPoint localPoint)
    {
        if (localPoint == null)
        {
            return new int[0][2];
        }

        if (useLocalLocations(worldView))
        {
            return addPOHStep(keyFrame, worldView, localPoint);
        }

        return addWorldStep(keyFrame, worldView, WorldPoint.fromLocalInstance(client, localPoint));
    }

    public int[][] addWorldStep(MovementKeyFrame keyFrame, WorldView worldView, WorldPoint worldPoint)
    {
        int[][] path = keyFrame.getPath();
        if (path.length == 0)
        {
            return new int[][]{new int[]{worldPoint.getX(), worldPoint.getY()}};
        }

        int[] lastStep = path[path.length - 1];
        int[][] stepsToAdd = new int[0][]; //pathFinder.findPath(worldView, new WorldPoint(lastStep[0], lastStep[1], worldView.getPlane()), worldPoint, config.movementAlgorithm());

        return ArrayUtils.addAll(path, stepsToAdd);
    }

    public int[][] addPOHStep(MovementKeyFrame keyFrame, WorldView worldView, LocalPoint localPoint)
    {
        int[][] path = keyFrame.getPath();
        if (path.length == 0)
        {
            return new int[][]{new int[]{localPoint.getSceneX(), localPoint.getSceneY()}};
        }

        int[] lastStep = path[path.length - 1];
        int[][] stepsToAdd = new int[0][]; //pathFinder.findPath(LocalPoint.fromScene(lastStep[0], lastStep[1], worldView), localPoint, config.movementAlgorithm());

        return ArrayUtils.addAll(path, stepsToAdd);
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
