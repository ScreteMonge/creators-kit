package com.creatorskit.programming.camera;

import com.creatorskit.programming.MovementManager;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarClientID;

import java.util.Collection;

public class CameraUtilities
{
    public static CameraScript readCameraScript(boolean inPOH, WorldView worldView, CameraScript script)
    {
        if (inPOH)
        {
            return script;
        }

        return readWorldScript(worldView, script);
    }

    private static CameraScript readWorldScript(WorldView worldView, CameraScript script)
    {
        WorldPoint worldPoint = new WorldPoint((int) script.getFocalX(), (int) script.getFocalZ(), worldView.getPlane());
        LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldPoint);
        if (localPoint == null)
        {
            return null;
        }

        return new CameraScript(
                script.isInPOH(),
                localPoint.getX(),
                script.getFocalY(),
                localPoint.getY(),
                script.getPitch(),
                script.getYaw(),
                script.getScale()
        );
    }

    public static CameraScript writeCameraScript(Client client, WorldView worldView, boolean inPOH)
    {
        if (inPOH)
        {
            return writePOHScript(client, inPOH);
        }

        return writeWorldScript(client, worldView, inPOH);
    }

    private static CameraScript writePOHScript(Client client, boolean inPOH)
    {
        return new CameraScript(
                inPOH,
                client.getCameraFocalPointX(),
                client.getCameraFocalPointY(),
                client.getCameraFocalPointZ(),
                client.getCameraPitch(),
                client.getCameraYaw(),
                client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL)
        );
    }

    private static CameraScript writeWorldScript(Client client, WorldView worldView, boolean inPOH)
    {
        LocalPoint lp = new LocalPoint((int) client.getCameraFocalPointX(), (int) client.getCameraFocalPointZ(), worldView);
        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, lp);

        Collection<WorldPoint> wps = WorldPoint.toLocalInstance(worldView, worldPoint);
        if (wps.isEmpty())
        {
            return null;
        }

        WorldPoint wp = wps.iterator().next();

        return new CameraScript(
                inPOH,
                wp.getX(),
                client.getCameraFocalPointY(),
                wp.getY(),
                client.getCameraPitch(),
                client.getCameraYaw(),
                client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL)
        );
    }
}
