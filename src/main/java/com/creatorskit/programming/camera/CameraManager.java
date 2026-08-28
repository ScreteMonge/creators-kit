package com.creatorskit.programming.camera;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.saves.CameraScriptSave;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.subtypes.CameraKeyFrame;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.util.List;

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
            handleCameraScript(true, 0);
            return;
        }

        handleCameraScript(true, 0);
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
    public void updateProgram(double currentTick, boolean playing)
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
            handleCameraScript(playing, currentTick);
            return;
        }

        nextKeyFrame = (CameraKeyFrame) next;
        clientTicksPassed = (int) ((currentTick - currentKeyFrame.getTick()) * CLIENT_TO_GAME_TICK_RATIO);
        handleCameraScript(playing, currentTick);
    }

    private void handleCameraScript(boolean playing, double currentTick)
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
        if (currentScript.getType() == CameraMotionType.TILE_TRACKING)
        {
            CameraDirectionalScript currentDirectionalScript = (CameraDirectionalScript) currentScript;
            handleDirectionalScript(currentDirectionalScript);
            return;
        }

        CameraTrackingScript currentTrackingScript = (CameraTrackingScript) currentScript;
        handleTrackingScript(currentTrackingScript, playing, currentTick);
    }

    private void handleDirectionalScript(CameraDirectionalScript currentDirectionalScript)
    {
        boolean inPOH = MovementManager.useLocalLocations(client.getTopLevelWorldView());
        if (!inPOH && currentDirectionalScript.isInPOH() || inPOH && !currentDirectionalScript.isInPOH())
        {
            return;
        }

        CameraDirectionalScript convertedCurrent = CameraUtilities.readCameraScript(inPOH, client.getTopLevelWorldView(), currentDirectionalScript);
        if (convertedCurrent == null)
        {
            return;
        }

        CameraDirectionalScript nextScript;
        if (nextKeyFrame == null || nextKeyFrame.getScript().getType() == CameraMotionType.OBJECT_TRACKING)
        {
            setCamera(convertedCurrent.getFocalX(), convertedCurrent.getFocalY(), convertedCurrent.getFocalZ(), (int) convertedCurrent.getPitch(), (int) convertedCurrent.getYaw(), convertedCurrent.getScale());
            return;
        }
        else
        {
            nextScript = (CameraDirectionalScript) nextKeyFrame.getScript();
        }

        if (!inPOH && nextScript.isInPOH() || inPOH && !nextScript.isInPOH())
        {
            setCamera(convertedCurrent.getFocalX(), convertedCurrent.getFocalY(), convertedCurrent.getFocalZ(), (int) convertedCurrent.getPitch(), (int) convertedCurrent.getYaw(), convertedCurrent.getScale());
            return;
        }

        CameraDirectionalScript convertedNext = CameraUtilities.readCameraScript(inPOH, client.getTopLevelWorldView(), nextScript);
        if (convertedNext == null)
        {
            setCamera(convertedCurrent.getFocalX(), convertedCurrent.getFocalY(), convertedCurrent.getFocalZ(), (int) convertedCurrent.getPitch(), (int) convertedCurrent.getYaw(), convertedCurrent.getScale());
            return;
        }

        double ratio = ((double) clientTicksPassed / CLIENT_TO_GAME_TICK_RATIO) / (nextKeyFrame.getTick() - currentKeyFrame.getTick());
        CameraDirectionalScript script = Ease.interpolateTileTracking(MovementManager.useLocalLocations(client.getTopLevelWorldView()), ratio, convertedCurrent.getEase(), convertedCurrent, convertedNext);
        setCamera(script.getFocalX(), script.getFocalY(), script.getFocalZ(), (int) script.getPitch(), (int) script.getYaw(), script.getScale());
    }

    private void handleTrackingScript(CameraTrackingScript currentTrackingScript, boolean playing, double currentTick)
    {
        Character character = currentTrackingScript.getCharacter();
        if (character == null || !character.isInScene())
        {
            return;
        }

        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        LocalPoint lp = ckObject.getLocation();

        Model model = ckObject.getBaseModel();
        if (model == null)
        {
            return;
        }

        model.calculateBoundsCylinder();
        Float height = Ease.interpolateObjectTrackingHeight(client, currentTick, playing, character, model.getModelHeight());
        if (height == null)
        {
            return;
        }

        CameraTrackingScript nextScript;
        if (nextKeyFrame == null || nextKeyFrame.getScript().getType() == CameraMotionType.TILE_TRACKING)
        {
            setCamera(
                    lp.getX(),
                    height,
                    lp.getY(),
                    (int) currentTrackingScript.getPitch(),
                    (int) currentTrackingScript.getYaw(),
                    currentTrackingScript.getScale());
            return;
        }
        else
        {
            nextScript = (CameraTrackingScript) nextKeyFrame.getScript();
        }

        double ratio = ((double) clientTicksPassed / CLIENT_TO_GAME_TICK_RATIO) / (nextKeyFrame.getTick() - currentKeyFrame.getTick());
        CameraTrackingScript script = Ease.interpolateObjectTracking(ratio, currentTrackingScript.getEase(), character, currentTrackingScript, nextScript);
        setCamera(
                lp.getX(),
                height,
                lp.getY(),
                (int) script.getPitch(),
                (int) script.getYaw(),
                script.getScale());
    }

    private void setCamera(float x, float y, float z, int pitch, int yaw, int scale)
    {
        client.setCameraFocalPointX(x);
        client.setCameraFocalPointY(y);
        client.setCameraFocalPointZ(z);
        client.setCameraPitchTarget(pitch);
        client.setCameraYawTarget(yaw);
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, scale, scale));
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

    public void clearKeyFrames()
    {
        keyFrames = new KeyFrame[0];
        currentKeyFrame = null;
        nextKeyFrame = null;
    }

    public CameraScriptSave[] packCameraKeyFrames()
    {
        CameraScriptSave[] scriptSaves = new CameraScriptSave[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            KeyFrame keyFrame = keyFrames[i];
            CameraKeyFrame kf = (CameraKeyFrame) keyFrame;
            if (kf.getScript().getType() == CameraMotionType.TILE_TRACKING)
            {
                CameraDirectionalScript script = (CameraDirectionalScript) kf.getScript();
                CameraScriptSave save = new CameraScriptSave(
                        CameraMotionType.TILE_TRACKING,
                        script.getEase(),
                        script.getPitch(),
                        script.getYaw(),
                        script.getScale(),
                        keyFrame.getTick(),
                        script.isInPOH(),
                        script.getFocalX(),
                        script.getOffsetX(),
                        script.getFocalY(),
                        script.getFocalZ(),
                        script.getOffsetZ(),
                        null
                );
                scriptSaves[i] = save;
            }
            else
            {
                CameraTrackingScript script = (CameraTrackingScript) kf.getScript();
                Character character = script.getCharacter();
                String id = character == null ? "" : character.getId();
                CameraScriptSave save = new CameraScriptSave(
                        CameraMotionType.OBJECT_TRACKING,
                        script.getEase(),
                        script.getPitch(),
                        script.getYaw(),
                        script.getScale(),
                        keyFrame.getTick(),
                        false,
                        0,
                        0,
                        0,
                        0,
                        0,
                        id
                );

                scriptSaves[i] = save;
            }
        }

        return scriptSaves;
    }

    public void unpackCameraKeyFrames(CameraScriptSave[] scriptSaves, List<Character> characters)
    {
        if (scriptSaves == null)
        {
            return;
        }

        for (int i = 0; i < scriptSaves.length; i++)
        {
            CameraScriptSave scriptSave = scriptSaves[i];
            if (scriptSave.getType() == CameraMotionType.TILE_TRACKING)
            {
                CameraDirectionalScript script = new CameraDirectionalScript(
                        CameraMotionType.TILE_TRACKING,
                        scriptSave.getEase(),
                        scriptSave.getPitch(),
                        scriptSave.getYaw(),
                        scriptSave.getScale(),
                        scriptSave.isInPOH(),
                        scriptSave.getFocalX(),
                        scriptSave.getOffsetX(),
                        scriptSave.getFocalY(),
                        scriptSave.getFocalZ(),
                        scriptSave.getOffsetZ()
                );

                addKeyFrame(new CameraKeyFrame(
                        scriptSave.getTick(),
                        script)
                );
            }
            else
            {
                Character character = null;
                String scriptId = scriptSave.getId();
                for (Character c : characters)
                {
                    if (c.getId().equals(scriptId))
                    {
                        character = c;
                        break;
                    }
                }

                CameraTrackingScript script = new CameraTrackingScript(
                        CameraMotionType.OBJECT_TRACKING,
                        scriptSave.getEase(),
                        scriptSave.getPitch(),
                        scriptSave.getYaw(),
                        scriptSave.getScale(),
                        character
                );

                addKeyFrame(new CameraKeyFrame(
                        scriptSave.getTick(),
                        script)
                );
            }
        }
    }
}
