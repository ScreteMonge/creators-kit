package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Direction
{
    UNSET("Unset", 0),
    NORTH("North", 1024),
    SOUTH("South", 0),
    EAST("East", 1536),
    WEST("West", 512),
    NORTHEAST("North-East", 1280),
    NORTHWEST("North-West", 768),
    SOUTHEAST("South-East", 1792),
    SOUTHWEST("South_West", 256)
    ;

    private String name;
    private int jUnit;

    public static Direction getDirection(int changeX, int changeY)
    {
        switch (changeX)
        {
            case -1:
                switch (changeY)
                {
                    case -1:
                        return SOUTHWEST;
                    case 0:
                        return WEST;
                    case 1:
                        return NORTHWEST;
                }
            case 0:
                switch (changeY)
                {
                    case -1:
                        return SOUTH;
                    case 1:
                        return NORTH;
                }
            case 1:
                switch (changeY)
                {
                    case -1:
                        return SOUTHEAST;
                    case 0:
                        return EAST;
                    case 1:
                        return NORTHEAST;
                }
        }

        return UNSET;
    }

    public static Direction[] getAllDirections()
    {
        return new Direction[]{NORTH, EAST, SOUTH, WEST, NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST};
    }

    @Override
    public String toString()
    {
        return name;
    }
}
