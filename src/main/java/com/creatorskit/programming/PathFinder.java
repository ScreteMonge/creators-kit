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

    public void transplantPath(Character character, WorldPoint newLocation)
    {
        Program program = character.getProgram();
        ProgramComp comp = program.getComp();
        WorldPoint[] steps = comp.getSteps();
        if (steps.length == 0)
            return;

        int changeX = newLocation.getX() - steps[0].getX();
        int changeY = newLocation.getY() - steps[0].getY();

        WorldPoint[] newSteps = new WorldPoint[steps.length];
        for (int i = 0; i < steps.length; i++)
        {
            WorldPoint wp = steps[i];
            WorldPoint point = new WorldPoint(wp.getX() + changeX, wp.getY() + changeY, newLocation.getPlane());
            newSteps[i] = point;
        }

        comp.setSteps(newSteps);
        comp.setCurrentStep(0);
    }
}
