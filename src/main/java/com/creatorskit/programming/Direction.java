package com.creatorskit.programming;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Direction
{
    UNSET,
    NORTH,
    SOUTH,
    EAST,
    WEST,
    NORTHEAST,
    NORTHWEST,
    SOUTHEAST,
    SOUTHWEST
    ;

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
}
