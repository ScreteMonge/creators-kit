package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class PathFinder
{
    @Inject
    private CreatorsPlugin plugin;

    @Inject
    private Client client;

    @Inject
    private CreatorsConfig config;

    private final int WATER_OVERLAY = 6;
    private final int REGION_ID_X = 256;
    private final int[] directionColumn = new int[]{1, -1, 0, 0, -1, 1, -1, 1};
    private final int[] directionRow = new int[]{0, 0, -1, 1, -1, -1, 1, 1};
    private final int[][] directionBlocks = new int[][]
            {
                    {CollisionDataFlag.BLOCK_MOVEMENT_EAST},
                    {CollisionDataFlag.BLOCK_MOVEMENT_WEST},
                    {CollisionDataFlag.BLOCK_MOVEMENT_SOUTH},
                    {CollisionDataFlag.BLOCK_MOVEMENT_NORTH},
                    {CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH, CollisionDataFlag.BLOCK_MOVEMENT_WEST},
                    {CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH, CollisionDataFlag.BLOCK_MOVEMENT_EAST},
                    {CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST, CollisionDataFlag.BLOCK_MOVEMENT_NORTH, CollisionDataFlag.BLOCK_MOVEMENT_WEST},
                    {CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST, CollisionDataFlag.BLOCK_MOVEMENT_NORTH, CollisionDataFlag.BLOCK_MOVEMENT_EAST}
            };

    public Coordinate[] getPath(LocalPoint startLocation, LocalPoint destLocation, MovementType movementType)
    {
        final ArrayList<Integer> rowQueue = new ArrayList<>();
        final ArrayList<Integer> columnQueue = new ArrayList<>();
        final boolean[][] visited = new boolean[104][104];
        final Coordinate[][] path = new Coordinate[104][104];

        boolean reachedEnd = false;

        if (startLocation == null || destLocation == null)
        {
            return null;
        }

        int startX = startLocation.getSceneX();
        int startY = startLocation.getSceneY();
        int endX = destLocation.getSceneX();
        int endY = destLocation.getSceneY();

        rowQueue.add(startY);
        columnQueue.add(startX);
        visited[startX][startY] = true;
        if (client.getCollisionMaps() == null)
        {
            return null;
        }

        CollisionData data = client.getCollisionMaps()[client.getPlane()];
        short[][] overlays = client.getScene().getOverlayIds()[client.getPlane()];

        while (!rowQueue.isEmpty() && !columnQueue.isEmpty())
        {
            int row = rowQueue.get(0);
            int column = columnQueue.get(0);
            rowQueue.remove(0);
            columnQueue.remove(0);
            if (row == endY && column == endX)
            {
                reachedEnd = true;
                break;
            }

            exploreNeighbours(row, column, data, overlays, visited, columnQueue, rowQueue, path, movementType);
        }

        if (reachedEnd)
        {
            return reconstructPath(endX, endY, path);
        }

        return null;
    }

    public Coordinate[] getPath(WorldPoint startLocation, WorldPoint destLocation, MovementType movementType)
    {
        final ArrayList<Integer> rowQueue = new ArrayList<>();
        final ArrayList<Integer> columnQueue = new ArrayList<>();
        final boolean[][] visited = new boolean[104][104];
        final Coordinate[][] path = new Coordinate[104][104];

        boolean reachedEnd = false;
        Collection<WorldPoint> startPoints = WorldPoint.toLocalInstance(client, startLocation);
        WorldPoint startPoint = startPoints.iterator().next();

        Collection<WorldPoint> destPoints = WorldPoint.toLocalInstance(client, destLocation);
        WorldPoint destPoint = destPoints.iterator().next();

        LocalPoint startLoc = LocalPoint.fromWorld(client, startPoint);
        LocalPoint destLoc = LocalPoint.fromWorld(client, destPoint);

        if (startLoc == null || destLoc == null)
        {
            return null;
        }

        int startX = startLoc.getSceneX();
        int startY = startLoc.getSceneY();
        int endX = destLoc.getSceneX();
        int endY = destLoc.getSceneY();
        rowQueue.add(startY);
        columnQueue.add(startX);
        visited[startX][startY] = true;
        if (client.getCollisionMaps() == null)
        {
            return null;
        }

        CollisionData data = client.getCollisionMaps()[client.getPlane()];
        short[][] overlays = client.getScene().getOverlayIds()[client.getPlane()];

        while (!rowQueue.isEmpty() && !columnQueue.isEmpty())
        {
            int row = rowQueue.get(0);
            int column = columnQueue.get(0);
            rowQueue.remove(0);
            columnQueue.remove(0);
            if (row == endY && column == endX)
            {
                reachedEnd = true;
                break;
            }

            exploreNeighbours(row, column, data, overlays, visited, columnQueue, rowQueue, path, movementType);
        }

        if (reachedEnd)
        {
            return reconstructPath(endX, endY, path);
        }

        return null;
    }

    private Coordinate[] reconstructPath(int endX, int endY, Coordinate[][] coordinates)
    {
        ArrayList<Coordinate> list = new ArrayList<>();
        Coordinate endCoordinate = new Coordinate(endX, endY);
        for (Coordinate coordinate = endCoordinate; coordinate != null; coordinate = coordinates[coordinate.getColumn()][coordinate.getRow()])
        {
            list.add(coordinate);
        }

        Coordinate[] path = new Coordinate[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            path[i] = list.get(i);
        }

        ArrayUtils.reverse(path);

        return path;
    }

    private void exploreNeighbours(int row, int column, CollisionData data, short[][] overlays, boolean[][] visited, ArrayList<Integer> columnQueue, ArrayList<Integer> rowQueue, Coordinate[][] path, MovementType movementType)
    {
        for (int i = 0; i < 8; i++)
        {
            int testRow = row + directionRow[i];
            int testColumn = column + directionColumn[i];

            if (testRow > Constants.SCENE_SIZE || testRow < 0 || testColumn > Constants.SCENE_SIZE || testColumn < 0)
            {
                continue;
            }

            if (movementType != MovementType.GHOST)
            {
                int setting = data.getFlags()[testColumn][testRow];
                int currentSetting = data.getFlags()[column][row];

                if ((setting & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 && (movementType != MovementType.WATERBORNE || !(overlays[testColumn][testRow] == WATER_OVERLAY) || (setting & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0))
                {
                    continue;
                }

                if (movementType == MovementType.WATERBORNE && (setting & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0)
                {
                    continue;
                }

                int[] blocks = directionBlocks[i];
                if ((currentSetting & blocks[0]) != 0)
                {
                    continue;
                }

                if (i >= 4)
                {
                    int settingEW = data.getFlags()[testColumn][row];
                    int settingNS = data.getFlags()[column][testRow];

                    if (((settingEW & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 && (movementType != MovementType.WATERBORNE || !(overlays[testColumn][row] == WATER_OVERLAY) || (settingEW & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0))
                            || ((settingNS & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 && (movementType != MovementType.WATERBORNE || !(overlays[column][testRow] == WATER_OVERLAY) || (settingNS & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0)))
                    {
                        continue;
                    }

                    if (movementType == MovementType.WATERBORNE && ((settingEW & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 || (settingNS & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0))
                        continue;

                    if ((settingEW & blocks[1]) != 0 || (settingNS & blocks[2]) != 0)
                        continue;

                    if ((currentSetting & blocks[1]) != 0 || (currentSetting & blocks[2]) != 0)
                        continue;
                }
            }

            if (visited[testColumn][testRow])
            {
                continue;
            }

            columnQueue.add(testColumn);
            rowQueue.add(testRow);
            visited[testColumn][testRow] = true;
            path[testColumn][testRow] = new Coordinate(column, row);
        }
    }

    public void transplantSteps(Character character, int newX, int newY, boolean fromInstance, boolean toInstance)
    {
        if (toInstance)
        {
            transplantInstancedSteps(character, newX, newY, fromInstance);
            return;
        }

        transplantNonInstancedSteps(character, newX, newY, fromInstance);
    }

    public void transplantNonInstancedSteps(Character character, int newX, int newY, boolean fromInstance)
    {
        Program program = character.getProgram();
        ProgramComp comp = program.getComp();

        if (!fromInstance)
        {
            WorldPoint[] steps = comp.getStepsWP();
            if (steps.length == 0)
                return;

            int changeX = newX - steps[0].getX();
            int changeY = newY - steps[0].getY();

            WorldPoint[] newSteps = new WorldPoint[steps.length];
            for (int i = 0; i < steps.length; i++)
            {
                WorldPoint wp = steps[i];
                WorldPoint point = new WorldPoint(wp.getX() + changeX, wp.getY() + changeY, client.getPlane());
                newSteps[i] = point;
            }

            comp.setStepsWP(newSteps);
            comp.setCurrentStep(0);
            return;
        }

        //Translate from LocalPoints to WorldPoints
        LocalPoint[] steps = comp.getStepsLP();
        if (steps.length == 0)
            return;

        WorldPoint[] newSteps = new WorldPoint[steps.length];
        newSteps[0] = new WorldPoint(newX, newY, client.getPlane());

        int[] changeXArray = new int[steps.length];
        int[] changeYArray = new int[steps.length];
        changeXArray[0] = 0;
        changeYArray[0] = 0;

        if (steps.length > 1)
        {
            for (int i = 1; i < steps.length; i++)
            {
                changeXArray[i] = changeXArray[i - 1] + steps[i].getSceneX() - steps[i - 1].getSceneX();
                changeYArray[i] = changeYArray[i - 1] + steps[i].getSceneY() - steps[i - 1].getSceneY();
            }

            for (int i = 1; i < steps.length; i++)
            {
                WorldPoint point = new WorldPoint(newX + changeXArray[i], newY + changeYArray[i], client.getPlane());
                newSteps[i] = point;
            }
        }

        comp.setStepsWP(newSteps);
        comp.setCurrentStep(0);

    }

    public void transplantInstancedSteps(Character character, int newX, int newY, boolean fromInstance)
    {
        Program program = character.getProgram();
        ProgramComp comp = program.getComp();

        if (fromInstance)
        {
            LocalPoint[] steps = comp.getStepsLP();
            if (steps.length == 0)
                return;

            int changeX = newX - steps[0].getSceneX();
            int changeY = newY - steps[0].getSceneY();

            LocalPoint[] newSteps = new LocalPoint[steps.length];
            for (int i = 0; i < steps.length; i++)
            {
                LocalPoint lp = steps[i];
                LocalPoint point = LocalPoint.fromScene(lp.getSceneX() + changeX, lp.getSceneY() + changeY);
                newSteps[i] = point;
            }

            comp.setStepsLP(newSteps);
            comp.setCurrentStep(0);
            return;
        }

        //Translate from WorldPoints to LocalPoints
        WorldPoint[] steps = comp.getStepsWP();
        if (steps.length == 0)
            return;

        LocalPoint[] newSteps = new LocalPoint[steps.length];
        newSteps[0] = LocalPoint.fromScene(newX, newY);

        int[] changeXArray = new int[steps.length];
        int[] changeYArray = new int[steps.length];
        changeXArray[0] = 0;
        changeYArray[0] = 0;

        if (steps.length > 1)
        {
            for (int i = 1; i < steps.length; i++)
            {
                changeXArray[i] = changeXArray[i - 1] + steps[i].getX() - steps[i - 1].getX();
                changeYArray[i] = changeYArray[i - 1] + steps[i].getY() - steps[i - 1].getY();
            }

            for (int i = 1; i < steps.length; i++)
            {
                LocalPoint point = LocalPoint.fromScene(newX + changeXArray[i], newY + changeYArray[i]);
                newSteps[i] = point;
            }
        }

        comp.setStepsLP(newSteps);
        comp.setCurrentStep(0);
    }

    /*
    private int[] calibrateForRegions(Character character)
    {
        int[] mapRegions = client.getMapRegions();
        Object[] mapRegionsObject = {mapRegions};

        int[] savedRegions = character.getLocalPointRegions();
        Object[] savedRegionsObject = {savedRegions};

        if (Arrays.deepEquals(mapRegionsObject, savedRegionsObject))
            return new int[]{0, 0};

        int mapRegionCorner = mapRegions[0];
        int savedRegionCorner = savedRegions[0];
        int difference = savedRegionCorner - mapRegionCorner;

        int yChange = 0;
        int xChange = 0;

        while (difference != 0)
        {
            if (difference >= REGION_ID_X)
            {
                difference -= REGION_ID_X;
                xChange++;
            }
            else if (difference <= -REGION_ID_X)
            {
                difference += REGION_ID_X;
                xChange--;
            }
            else
            {
                yChange = difference;
                break;
            }
        }

        return new int[]{xChange * Constants.REGION_SIZE, yChange * Constants.REGION_SIZE};
    }

     */
}
