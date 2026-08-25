package com.creatorskit.programming.camera;

import com.creatorskit.Character;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarClientID;

import java.util.Collection;

public class CameraUtilities
{
    public static CameraDirectionalScript readCameraScript(boolean inPOH, WorldView worldView, CameraDirectionalScript script)
    {
        if (inPOH)
        {
            return script;
        }

        return readWorldScript(worldView, script);
    }

    private static CameraDirectionalScript readWorldScript(WorldView worldView, CameraDirectionalScript script)
    {
        WorldPoint worldPoint = new WorldPoint((int) script.getFocalX(), (int) script.getFocalZ(), worldView.getPlane());
        LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldPoint);
        if (localPoint == null)
        {
            return null;
        }

        int baseX = localPoint.getX() - Perspective.LOCAL_TILE_SIZE / 2;
        int baseY = localPoint.getY() - Perspective.LOCAL_TILE_SIZE / 2;

        return new CameraDirectionalScript(
                CameraMotionType.DIRECTIONAL,
                script.getEase(),
                script.getPitch(),
                script.getYaw(),
                script.getScale(),
                script.isInPOH(),
                baseX + script.getOffsetX(),
                0,
                script.getFocalY(),
                baseY + script.getOffsetZ(),
                0
        );
    }

    public static CameraTrackingScript writeTrackingScript(Client client, EaseType easeType, Character character)
    {
        return new CameraTrackingScript(
                CameraMotionType.TRACKING,
                easeType,
                client.getCameraPitch(),
                client.getCameraYaw(),
                client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL),
                character
        );
    }

    public static CameraDirectionalScript writeDirectionalScript(Client client, WorldView worldView, EaseType easeType, boolean inPOH)
    {
        if (inPOH)
        {
            return writeDirecationalPOHScript(client, easeType, inPOH);
        }

        return writeDirectionalWorldScript(client, worldView, easeType, inPOH);
    }

    private static CameraDirectionalScript writeDirecationalPOHScript(Client client, EaseType easeType, boolean inPOH)
    {
        return new CameraDirectionalScript(
                CameraMotionType.DIRECTIONAL,
                easeType,
                client.getCameraPitch(),
                client.getCameraYaw(),
                client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL),
                inPOH,
                client.getCameraFocalPointX(),
                0,
                client.getCameraFocalPointY(),
                client.getCameraFocalPointZ(),
                0
        );
    }

    private static CameraDirectionalScript writeDirectionalWorldScript(Client client, WorldView worldView, EaseType easeType, boolean inPOH)
    {
        LocalPoint lp = new LocalPoint((int) client.getCameraFocalPointX(), (int) client.getCameraFocalPointZ(), worldView);
        int offsetX = lp.getX() & 127;
        int offsetY = lp.getY() & 127;
        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, lp);

        Collection<WorldPoint> wps = WorldPoint.toLocalInstance(worldView, worldPoint);
        if (wps.isEmpty())
        {
            return null;
        }

        WorldPoint wp = wps.iterator().next();

        return new CameraDirectionalScript(
                CameraMotionType.DIRECTIONAL,
                easeType,
                client.getCameraPitch(),
                client.getCameraYaw(),
                client.getVarcIntValue(VarClientID.CAMERA_ZOOM_SMALL),
                inPOH,
                wp.getX(),
                offsetX,
                client.getCameraFocalPointY(),
                wp.getY(),
                offsetY
        );
    }
}
