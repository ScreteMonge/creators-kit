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
    private CreatorsPlugin plugin;
    private CreatorsConfig config;
    private PathFinder pathFinder;
    private static final int[] POH_REGIONS = new int[]{7257, 7513, 7514, 7769, 7770, 8025, 8026};
    private static final int[] GAUNTLET_REGIONS = new int[]{7512, 7768};

    @Inject
    public MovementManager(Client client, CreatorsPlugin plugin, CreatorsConfig config, PathFinder pathFinder)
    {
        this.client = client;
        this.plugin = plugin;
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

            System.out.println("Checking path");
            for (int[] i : path)
            {
                System.out.println("Path: " + i[0] + "," + i[1]);
            }
            System.out.println("Unchecking path");
            return;
        }

        int[] lastStep = path[path.length - 1];
        int[][] stepsToAdd = pathFinder.findPath(worldView, new WorldPoint(lastStep[0], lastStep[1], worldView.getPlane()), worldPoint, config.movementAlgorithm());

        path = ArrayUtils.addAll(path, stepsToAdd);
        keyFrame.setPath(path);

        System.out.println("Checking path");
        for (int[] i : path)
        {
            System.out.println("Path: " + i[0] + "," + i[1]);
        }
        System.out.println("Unchecking path");
    }

    public void addPOHStep(MovementKeyFrame keyFrame, WorldView worldView, LocalPoint localPoint)
    {
        int[][] path = keyFrame.getPath();
        if (path.length == 0)
        {
            path = new int[][]{new int[]{localPoint.getSceneX(), localPoint.getSceneY()}};
            keyFrame.setPath(path);
            System.out.println("Checking path");
            for (int[] i : path)
            {
                System.out.println("Path: " + i[0] + "," + i[1]);
            }
            System.out.println("Unchecking path");
            return;
        }

        int[] lastStep = path[path.length - 1];
        int[][] stepsToAdd = pathFinder.findPath(LocalPoint.fromScene(lastStep[0], lastStep[1], worldView), localPoint, config.movementAlgorithm());

        path = ArrayUtils.addAll(path, stepsToAdd);
        keyFrame.setPath(path);

        System.out.println("Checking path");
        for (int[] i : path)
        {
            System.out.println("Path: " + i[0] + "," + i[1]);
        }
        System.out.println("Unchecking path");
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

    /*

    public boolean isInScene(MovementKeyFrame keyFrame)
    {
        if (keyFrame.isInstanced())
        {
            return (isInNonInstanceScene(keyFrame));
        }

        return (isInSceneInstance(keyFrame));
    }

    public boolean isInSceneInstance(MovementKeyFrame keyFrame)
    {
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView.getScene().isInstance())
        {
            return false;
        }

        if (worldView.getPlane() != keyFrame.getPlane())
        {
            return false;
        }

        WorldPoint wp = keyFrame.getLocation();
        if (wp == null)
        {
            return false;
        }

        return WorldPoint.isInScene(worldView, wp.getX(), wp.getY());
    }

    public boolean isInNonInstanceScene(MovementKeyFrame keyFrame)
    {
        WorldView worldView = client.getTopLevelWorldView();
        if (!worldView.getScene().isInstance())
        {
            return false;
        }

        if (worldView.getPlane() != program.getPlane())
        {
            return false;
        }

        int[] mapRegions = client.getMapRegions();

        //This function is finicky with larger instances, in that only an exact region:region map will load
        //The alternative of finding any match will otherwise make spawns off if the regions don't match because the scenes won't exactly match
        Object[] mapRegionsObjects = {mapRegions};
        Object[] instancedRegionObjects = {program.getRegions()};
        return Arrays.deepEquals(mapRegionsObjects, instancedRegionObjects);
    }


     */
    /*
    if (keyFrame == null)
        {
            if (instance)
            {
                keyFrame = new MovementKeyFrame(
                        currentTick,
                        new InstancedProgram(true, worldView.getPlane(), localPoint, client.getMapRegions()),
                        new int[][]{new int[]{0, 0}},
                        false,
                        1);
            }
            else
            {
                WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
                keyFrame = new MovementKeyFrame(
                        currentTick,
                        new NonInstancedProgram(false, worldView.getPlane(), worldPoint),
                        new int[][]{new int[]{0, 0}},
                        false,
                        1);
            }
            return;
        }
     */

    /*
    public void addProgramSteps(Character character, MovementKeyFrame keyFrame)
    {
        if (character == null)
        {
            return;
        }
        WorldView worldView = client.getTopLevelWorldView();
        boolean instance = worldView.getScene().isInstance();

        Tile tile = worldView.getSelectedSceneTile();
        if (tile == null)
        {
            return;
        }

        LocalPoint localPoint = tile.getLocalLocation();
        if (localPoint == null)
        {
            return;
        }

        Program program = character.getProgram();
        boolean inScene = plugin.isInScene(character);

        if (inScene && instance)
        {
            LocalPoint[] steps = program.getComp().getStepsLP();

            if (steps.length == 0)
            {
                setLocation(character, true, false, true, false);
            }

            if (steps.length > 0)
            {
                Coordinate[] coordinates = pathFinder.getPath(steps[steps.length - 1], localPoint, program.getComp().getMovementType());
                if (coordinates == null)
                {
                    sendChatMessage("A path could not be found to this tile");
                    return;
                }
            }

            steps = ArrayUtils.add(steps, localPoint);
            program.getComp().setStepsLP(steps);
            updateProgramPath(program, false, character.isInInstance());
            return;
        }

        if (!inScene && instance)
        {
            program.getComp().setStepsLP(new LocalPoint[]{localPoint});
            program.getComp().setStepsWP(new WorldPoint[0]);
            setLocation(character, true, false, true, false);
            updateProgramPath(program, false, character.isInInstance());
            return;
        }

        if (inScene && !instance)
        {
            WorldPoint[] steps = program.getComp().getStepsWP();

            if (steps.length == 0)
            {
                setLocation(character, true, false, true, false);
            }

            WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

            if (steps.length > 0)
            {
                Coordinate[] coordinates = pathFinder.getPath(steps[steps.length - 1], worldPoint, program.getComp().getMovementType());
                if (coordinates == null)
                {
                    sendChatMessage("A path could not be found to this tile");
                    return;
                }
            }

            steps = ArrayUtils.add(steps, worldPoint);
            program.getComp().setStepsWP(steps);
            updateProgramPath(program, false, character.isInInstance());
            return;
        }

        if (!inScene && !instance)
        {
            WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
            program.getComp().setStepsWP(new WorldPoint[]{worldPoint});
            program.getComp().setStepsLP(new LocalPoint[0]);
            setLocation(character, true, false, true, false);
            updateProgramPath(program, false, character.isInInstance());
        }


    }

     */
}
