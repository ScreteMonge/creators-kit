package com.creatorskit;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Rotation
{
    _45_DEGREES("45 Degrees", 256),
    _90_DEGREES("90 Degrees", 512),
    _180_DEGREES("180 Degrees", 1024)

    ;
    public final String name;
    public final int degrees;

    @Override
    public String toString()
    {
        return name;
    }

    public static int convertRotation(double x, double y, double angle)
    {
        int jagexDegree = 0;
        if (x >= 0 && y >= 0)
        {
            if (angle <= 30)
            {
                jagexDegree = 1536;
            }
            else if (angle > 30 && angle < 60)
            {
                jagexDegree = 1280;
            }
            else if (angle >= 60)
            {
                jagexDegree = 1024;
            }
        }
        else if (x >= 0 && y <= 0)
        {
            if (angle <= 30)
            {
                jagexDegree = 1536;
            }
            else if (angle > 30 && angle < 60)
            {
                jagexDegree = 1792;
            }
            else if (angle >= 60)
            {
                jagexDegree = 0;
            }
        }
        else if (x <=0 && y >= 0)
        {
            if (angle <= 30)
            {
                jagexDegree = 512;
            }
            else if (angle > 30 && angle < 60)
            {
                jagexDegree = 768;
            }
            else if (angle >= 60)
            {
                jagexDegree = 1024;
            }
        }
        else if (x <=0 && y <= 0)
        {
            if (angle <= 30)
            {
                jagexDegree = 512;
            }
            else if (angle > 30 && angle < 60)
            {
                jagexDegree = 256;
            }
            else if (angle >= 60)
            {
                jagexDegree = 0;
            }
        }

        return jagexDegree;
    }

    public static int roundRotation(int rotation)
    {
        if ((rotation > -128 && rotation <= 128) || (rotation > 1920 && rotation <= 2176))
        {
            return 0;
        }

        if ((rotation > 128 && rotation <= 384) || (rotation > 2176 && rotation <= 2432))
        {
            return 256;
        }

        if (rotation > 384 && rotation <= 640)
        {
            return 512;
        }

        if (rotation > 640 && rotation <= 896)
        {
            return 768;
        }

        if (rotation > 896 && rotation <= 1152)
        {
            return 1024;
        }

        if (rotation > 1152 && rotation <= 1408)
        {
            return 1280;
        }

        if (rotation > 1408 && rotation <= 1664)
        {
            return 1536;
        }

        if ((rotation > 1664 && rotation <= 1920) || (rotation > -384 && rotation <= -128))
        {
            return 1792;
        }

        return 0;
    }

    public static int getJagexDegrees(double x, double y, int yaw, double pitch)
    {
        double degrees = Math.abs((Math.atan(y / x)) * 180 / Math.PI);
        int jagexDegree = Rotation.convertRotation(x, y, degrees) - (Rotation.roundRotation(yaw));
        while (jagexDegree >= 2048)
        {
            jagexDegree -= 2048;
        }

        while (jagexDegree < 0)
        {
            jagexDegree += 2048;
        }

        return jagexDegree;
    }
}
