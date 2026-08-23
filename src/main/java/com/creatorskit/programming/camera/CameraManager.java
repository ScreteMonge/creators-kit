package com.creatorskit.programming.camera;

import com.creatorskit.programming.MovementManager;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.subtypes.CameraKeyFrame;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;

public class CameraManager
{
    private final Client client;
    private ClientThread clientThread;

    private CameraKeyFrame currentKeyFrame;
    private CameraKeyFrame nextKeyFrame;
    private int clientTicksPassed = 0;
    private final static int CLIENT_TO_GAME_TICK_RATIO = 30;

    @Getter
    private KeyFrame[] keyFrames = new CameraKeyFrame[0];
    @Setter
    private boolean enabled = true;
    @Setter
    private boolean cancelled = false;

    @Inject
    public CameraManager(Client client, ClientThread clientThread)
    {
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Updates the active camera keyframe. Intended for use when playing the program
     */
    public void updateProgramOnTick(double currentTick)
    {
        KeyFrame current = getCurrentKeyFrame(currentTick);
        if (current == null)
        {
            clientTicksPassed = 0;
            currentKeyFrame = null;
            nextKeyFrame = null;
            return;
        }

        if (current != currentKeyFrame)
        {
            clientTicksPassed = 0;
            currentKeyFrame = (CameraKeyFrame) current;
        }

        KeyFrame next = getNextKeyFrame(currentTick);
        if (next == null)
        {
            nextKeyFrame = null;
            return;
        }

        nextKeyFrame = (CameraKeyFrame) next;
    }

    /**
     * Updates the active camera keyframe. Intended for use when manually setting the time
     */
    public void updateProgram(double currentTick)
    {
        cancelled = false;
        KeyFrame current = getCurrentKeyFrame(currentTick);
        if (current == null)
        {
            clientTicksPassed = 0;
            currentKeyFrame = null;
            nextKeyFrame = null;
            return;
        }

        if (current != currentKeyFrame)
        {
            clientTicksPassed = 0;
            currentKeyFrame = (CameraKeyFrame) current;
        }

        KeyFrame next = getNextKeyFrame(currentTick);
        if (next == null)
        {
            nextKeyFrame = null;
            handleCameraScript();
            return;
        }

        nextKeyFrame = (CameraKeyFrame) next;
        clientTicksPassed = (int) ((currentTick - currentKeyFrame.getTick()) * CLIENT_TO_GAME_TICK_RATIO);
        handleCameraScript();
    }

    private void handleCameraScript()
    {
        if (!enabled || cancelled || client.getCameraMode() != 1)
        {
            return;
        }

        if (currentKeyFrame == null)
        {
            return;
        }

        CameraScript currentScript = currentKeyFrame.getScript();

        boolean inPOH = MovementManager.useLocalLocations(client.getTopLevelWorldView());
        if (!inPOH && currentScript.isInPOH() || inPOH && !currentScript.isInPOH())
        {
            return;
        }

        CameraScript convertedCurrent = CameraUtilities.readCameraScript(inPOH, client.getTopLevelWorldView(), currentScript);
        if (convertedCurrent == null)
        {
            return;
        }

        CameraScript nextScript;
        if (nextKeyFrame == null)
        {
            setCamera(convertedCurrent);
            return;
        }
        else
        {
            nextScript = nextKeyFrame.getScript();
        }

        if (!inPOH && nextScript.isInPOH() || inPOH && !nextScript.isInPOH())
        {
            setCamera(convertedCurrent);
            return;
        }

        CameraScript convertedNext = CameraUtilities.readCameraScript(inPOH, client.getTopLevelWorldView(), nextScript);
        if (convertedNext == null)
        {
            setCamera(convertedCurrent);
            return;
        }

        double ratio = ((double) clientTicksPassed / CLIENT_TO_GAME_TICK_RATIO) / (nextKeyFrame.getTick() - currentKeyFrame.getTick());
        CameraScript script = Ease.interpolate(MovementManager.useLocalLocations(client.getTopLevelWorldView()), ratio, currentKeyFrame.getEase(), convertedCurrent, convertedNext);
        setCamera(script);
    }

    /**
     * Ticks the camera timer for interpolating between GameTicks. Intended for use when Playing the programmer, not when manually setting the time
     */
    public void tick()
    {
        clientTicksPassed++;

        if (currentKeyFrame == null)
        {
            return;
        }

        if (nextKeyFrame == null)
        {
            handleCameraScript();
            return;
        }

        handleCameraScript();
    }

    private void setCamera(CameraScript script)
    {
        client.setCameraFocalPointX(script.getFocalX());
        client.setCameraFocalPointY(script.getFocalY());
        client.setCameraFocalPointZ(script.getFocalZ());
        client.setCameraPitchTarget((int) script.getPitch());
        client.setCameraYawTarget((int) script.getYaw());
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, script.getScale(), script.getScale()));
    }

    public KeyFrame getCurrentKeyFrame(double currentTick)
    {
        if (keyFrames.length == 0)
        {
            return null;
        }

        if (keyFrames.length == 1)
        {
            return keyFrames[0];
        }

        for (int i = 0; i < keyFrames.length; i++)
        {
            KeyFrame keyFrame = keyFrames[i];

            if (keyFrame.getTick() == currentTick)
            {
                return keyFrame;
            }

            if (keyFrame.getTick() > currentTick)
            {
                if (i == 0)
                {
                    return keyFrame;
                }

                return keyFrames[i - 1];
            }
        }

        return keyFrames[keyFrames.length - 1];
    }

    public KeyFrame getNextKeyFrame(double currentTick)
    {
        for (int i = 0; i < keyFrames.length; i++)
        {
            KeyFrame keyFrame = keyFrames[i];

            if (keyFrame.getTick() > currentTick)
            {
                return keyFrame;
            }
        }

        return null;
    }

    public KeyFrame addKeyFrame(KeyFrame keyFrame)
    {
        double tick = keyFrame.getTick();

        int index = 0;
        while (index < keyFrames.length && keyFrames[index].getTick() < tick)
        {
            index++;
        }

        if (index < keyFrames.length && keyFrames[index].getTick() == tick)
        {
            KeyFrame toRemove = keyFrames[index];
            keyFrames[index] = keyFrame;
            return toRemove;
        }

        keyFrames = ArrayUtils.insert(index, keyFrames, keyFrame);
        return null;
    }

    public void removeKeyFrame(KeyFrame keyFrame)
    {
        keyFrames = ArrayUtils.removeElement(keyFrames, keyFrame);
    }
}
