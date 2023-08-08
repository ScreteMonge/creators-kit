package com.creatorskit.programming;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.util.ArrayList;

public class PathFinder
{
    @Inject
    private CreatorsPlugin plugin;

    @Inject
    private Client client;

    @Inject
    private CreatorsConfig config;

    private final int SCENE_SIZE = 104;
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

    public Coordinate[] getPath(LocalPoint startLoc, LocalPoint destLoc, boolean waterWalk)
    {
        final ArrayList<Integer> rowQueue = new ArrayList<>();
        final ArrayList<Integer> columnQueue = new ArrayList<>();
        final boolean[][] visited = new boolean[104][104];
        final Coordinate[][] path = new Coordinate[104][104];

        boolean reachedEnd = false;
        int startX = startLoc.getSceneX();
        int startY = startLoc.getSceneY();
        System.out.println("StartX: " + startX + ", StartY: " + startY);
        int endX = destLoc.getSceneX();
        int endY = destLoc.getSceneY();
        System.out.println("EndX: " + endX + ", EndY: " + endY);
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

            exploreNeighbours(row, column, data, overlays, visited, columnQueue, rowQueue, path, waterWalk);
        }

        if (reachedEnd)
        {
            System.out.println("End Found!");
            return reconstructPath(endX, endY, path);
        }

        System.out.println("end unfindable");
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

    private void exploreNeighbours(int row, int column, CollisionData data, short[][] overlays, boolean[][] visited, ArrayList<Integer> columnQueue, ArrayList<Integer> rowQueue, Coordinate[][] path, boolean waterWalk)
    {
        for (int i = 0; i < 8; i++)
        {
            int testRow = row + directionRow[i];
            int testColumn = column + directionColumn[i];
            //System.out.println("X: " + testColumn + ", Y: " + testRow);

            if (testRow > SCENE_SIZE || testRow < 0 || testColumn > SCENE_SIZE || testColumn < 0)
            {
                continue;
            }

            int setting = data.getFlags()[testColumn][testRow];
            int currentSetting = data.getFlags()[column][row];

            if ((setting & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 && (!waterWalk || !(overlays[testColumn][testRow] == WATER_OVERLAY) || (setting & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0))
            {
                continue;
            }

            if (waterWalk && (setting & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0)
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

                if (((settingEW & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 && (!waterWalk || !(overlays[testColumn][row] == WATER_OVERLAY) || (settingEW & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0))
                        || ((settingNS & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 && (!waterWalk || !(overlays[column][testRow] == WATER_OVERLAY) || (settingNS & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0)))
                {
                    continue;
                }

                if (waterWalk && ((settingEW & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 || (settingNS & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0))
                    continue;

                if ((settingEW & blocks[1]) != 0 || (settingNS & blocks[2]) != 0)
                    continue;

                if ((currentSetting & blocks[1]) != 0 || (currentSetting & blocks[2]) != 0)
                    continue;
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
}
