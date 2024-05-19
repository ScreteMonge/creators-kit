package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelType
{
    WALL_FLAT(0, '1'),
    WALL_CORNER_TRI(1, '2'),
    WALL_CORNER_L(2, '3'),
    WALL_POST(3, '4'),
    DECORATIVE_OBJECT(4, 'q'),
    WALL_DIAGONAL(9, '5'),
    GAME_OBJECT(10, '8'),
    GAME_OBJECT_DIAGONAL(11, '0'),
    ROOF_SLANT(12, 'a'),
    ROOF_TRI(13, 's'),
    ROOF_RHOMBUS_BOWED(14, 'd'),
    ROOF_RHOMBUS_WINGS_HIGH(15, 'f'),
    ROOF_RHOMBUS_WINGS_LOW(16, 'g'),
    ROOF_FLAT(17, 'h'),
    ROOF_EDGE_FLAT(18, 'z'),
    ROOF_EDGE_CORNER_TRI(19, 'x'),
    ROOF_EDGE_CORNER_L(20, 'c'),
    ROOF_EDGE_CORNER_SQUARE(21, 'v'),
    GROUND_OBJECT(22, '0')
    ;

    private final int modelType;
    private final char cacheValue;

    public static char findCacheValue(int modelType)
    {
        for (ModelType mt : ModelType.values())
        {
            if (modelType == mt.getModelType())
            {
                return mt.getCacheValue();
            }
        }

        return 'o';
    }
}
