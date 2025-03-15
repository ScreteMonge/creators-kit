package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.MovementKeyFrame;
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
    private Client client;

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

    public int[][] findPath(LocalPoint startLocation, LocalPoint destLocation, MovementType movementType)
    {
        Coordinate[] coordinates = getPath(startLocation, destLocation, movementType);
        if (coordinates == null || coordinates.length == 0)
        {
            return null;
        }

        int[][] path = new int[coordinates.length][2];

        for (int i = 0; i < coordinates.length; i++)
        {
            Coordinate c = coordinates[i];
            path[i] = new int[]{c.getColumn(), c.getRow()};
        }

        return path;
    }

    public Coordinate[] getPath(LocalPoint startLocation, LocalPoint destLocation, MovementType movementType)
    {
        WorldView worldView = client.getTopLevelWorldView();
        Scene scene = worldView.getScene();
        final ArrayList<Integer> rowQueue = new ArrayList<>();
        final ArrayList<Integer> columnQueue = new ArrayList<>();
        final boolean[][] visited = new boolean[Constants.SCENE_SIZE][Constants.SCENE_SIZE];
        final Coordinate[][] path = new Coordinate[Constants.SCENE_SIZE][Constants.SCENE_SIZE];

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
        if (worldView.getCollisionMaps() == null)
        {
            return null;
        }

        CollisionData data = worldView.getCollisionMaps()[worldView.getPlane()];
        short[][] overlays = scene.getOverlayIds()[worldView.getPlane()];

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

    public int[][] findPath(WorldView worldView, WorldPoint startLocation, WorldPoint destLocation, MovementType movementType)
    {
        Coordinate[] coordinates = getPath(startLocation, destLocation, movementType);
        if (coordinates == null || coordinates.length == 0)
        {
            return null;
        }

        int[][] path = new int[coordinates.length][2];

        for (int i = 0; i < coordinates.length; i++)
        {
            Coordinate c = coordinates[i];
            LocalPoint lp = LocalPoint.fromScene(c.getColumn(), c.getRow(), worldView);
            WorldPoint wp = WorldPoint.fromLocalInstance(client, lp, worldView.getPlane());
            path[i] = new int[]{wp.getX(), wp.getY()};
        }

        return path;
    }

    public Coordinate[] getPath(WorldPoint startLocation, WorldPoint destLocation, MovementType movementType)
    {
        WorldView worldView = client.getTopLevelWorldView();
        Scene scene = worldView.getScene();
        final ArrayList<Integer> rowQueue = new ArrayList<>();
        final ArrayList<Integer> columnQueue = new ArrayList<>();
        final boolean[][] visited = new boolean[Constants.SCENE_SIZE][Constants.SCENE_SIZE];
        final Coordinate[][] path = new Coordinate[Constants.SCENE_SIZE][Constants.SCENE_SIZE];

        boolean reachedEnd = false;
        Collection<WorldPoint> startPoints = WorldPoint.toLocalInstance(scene, startLocation);
        WorldPoint startPoint = startPoints.iterator().next();

        Collection<WorldPoint> destPoints = WorldPoint.toLocalInstance(scene, destLocation);
        WorldPoint destPoint = destPoints.iterator().next();

        LocalPoint startLoc = LocalPoint.fromWorld(worldView, startPoint);
        LocalPoint destLoc = LocalPoint.fromWorld(worldView, destPoint);

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
        if (worldView.getCollisionMaps() == null)
        {
            return null;
        }

        CollisionData data = worldView.getCollisionMaps()[worldView.getPlane()];
        short[][] overlays = scene.getOverlayIds()[worldView.getPlane()];

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
        path = ArrayUtils.remove(path, 0);

        return path;
    }

    private void exploreNeighbours(int row, int column, CollisionData data, short[][] overlays, boolean[][] visited, ArrayList<Integer> columnQueue, ArrayList<Integer> rowQueue, Coordinate[][] path, MovementType movementType)
    {
        for (int i = 0; i < 8; i++)
        {
            int testRow = row + directionRow[i];
            int testColumn = column + directionColumn[i];

            if (testRow >= Constants.SCENE_SIZE || testRow < 0 || testColumn >= Constants.SCENE_SIZE || testColumn < 0)
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

    public void transplantSteps(Character character, WorldView worldView, int newX, int newY)
    {
        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        if (kf == null)
        {
            return;
        }

        MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
        int[][] path = keyFrame.getPath();
        if (path.length == 0)
        {
            return;
        }

        int[] start = path[0];
        int changeX = newX - start[0];
        int changeY = newY - start[1];

        keyFrame.setPlane(worldView.getPlane());
        boolean poh = MovementManager.isInPOH(worldView);
        keyFrame.setPoh(poh);

        for (int[] coordinates : path)
        {
            coordinates[0] = coordinates[0] + changeX;
            coordinates[1] = coordinates[1] + changeY;
        }
    }
}
