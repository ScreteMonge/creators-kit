package com.creatorskit;

import com.creatorskit.models.CustomModel;
import com.creatorskit.swing.ObjectPanel;
import com.creatorskit.swing.ParentPanel;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Constants;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

@Getter
@Setter
@AllArgsConstructor
public class Character
{
    private String name;
    private boolean active;
    private boolean locationSet;
    private KeyFrame[][] frames;
    private KeyFrame[] currentFrames;
    private DefaultMutableTreeNode linkedManagerNode;
    private DefaultMutableTreeNode parentManagerNode;
    private Color color;
    private WorldPoint nonInstancedPoint;
    private LocalPoint instancedPoint;
    private int instancedPlane;
    private boolean inPOH;
    private CustomModel storedModel;
    private ParentPanel parentPanel;
    private ObjectPanel objectPanel;
    private boolean customMode;
    private JTextField nameField;
    private JComboBox<CustomModel> comboBox;
    private JButton spawnButton;
    private JButton modelButton;
    private JSpinner modelSpinner;
    private JSpinner animationSpinner;
    private JSpinner animationFrameSpinner;
    private JSpinner orientationSpinner;
    private JSpinner radiusSpinner;
    private CKObject ckObject;
    private CKObject spotAnim1;
    private CKObject spotAnim2;
    private int targetOrientation;

    @Override
    public String toString()
    {
        return name;
    }

    public void play()
    {
        ckObject.setPlaying(true);
        if (spotAnim1 != null)
        {
            spotAnim1.setPlaying(true);
        }

        if (spotAnim2 != null)
        {
            spotAnim2.setPlaying(true);
        }
    }

    public void pause()
    {
        ckObject.setPlaying(false);
        if (spotAnim1 != null)
        {
            spotAnim1.setPlaying(false);
        }

        if (spotAnim2 != null)
        {
            spotAnim2.setPlaying(false);
        }
    }

    /**
     * Updates the Character's current KeyFrame for the given time
     * @param tick the tick at which to look for KeyFrames
     */
    public void updateProgram(double tick)
    {
        KeyFrame currentMovement = findPreviousKeyFrame(KeyFrameType.MOVEMENT, tick, true);
        setCurrentKeyFrame(currentMovement, KeyFrameType.MOVEMENT);

        KeyFrame currentAnimation = findPreviousKeyFrame(KeyFrameType.ANIMATION, tick, true);
        setCurrentKeyFrame(currentAnimation, KeyFrameType.ANIMATION);

        KeyFrame currentSpawn = findPreviousKeyFrame(KeyFrameType.SPAWN, tick, true);
        setCurrentKeyFrame(currentSpawn, KeyFrameType.SPAWN);

        KeyFrame currentModel = findPreviousKeyFrame(KeyFrameType.MODEL, tick, true);
        setCurrentKeyFrame(currentModel, KeyFrameType.MODEL);

        KeyFrame currentOrientation = findPreviousKeyFrame(KeyFrameType.ORIENTATION, tick, true);
        setCurrentKeyFrame(currentOrientation, KeyFrameType.ORIENTATION);

        KeyFrame currentText = findPreviousKeyFrame(KeyFrameType.TEXT, tick, true);
        setCurrentKeyFrame(currentText, KeyFrameType.TEXT);

        KeyFrame currentOverhead = findPreviousKeyFrame(KeyFrameType.OVERHEAD, tick, true);
        setCurrentKeyFrame(currentOverhead, KeyFrameType.OVERHEAD);

        KeyFrame currentHealth = findPreviousKeyFrame(KeyFrameType.HEALTH, tick, true);
        setCurrentKeyFrame(currentHealth, KeyFrameType.HEALTH);

        KeyFrame currentSpotAnim = findPreviousKeyFrame(KeyFrameType.SPOTANIM, tick, true);
        setCurrentKeyFrame(currentSpotAnim, KeyFrameType.SPOTANIM);

        KeyFrame currentSpotAnim2 = findPreviousKeyFrame(KeyFrameType.SPOTANIM2, tick, true);
        setCurrentKeyFrame(currentSpotAnim2, KeyFrameType.SPOTANIM2);
    }

    public void setSpotAnim(CKObject spotAnim, KeyFrameType spotAnimType)
    {
        if (spotAnimType == KeyFrameType.SPOTANIM)
        {
            setSpotAnim1(spotAnim);
        }

        if (spotAnimType == KeyFrameType.SPOTANIM2)
        {
            setSpotAnim2(spotAnim);
        }
    }

    public KeyFrame getCurrentKeyFrame(KeyFrameType type)
    {
        return currentFrames[KeyFrameType.getIndex(type)];
    }

    public void setCurrentKeyFrame(KeyFrame keyFrame, KeyFrameType type)
    {
        currentFrames[KeyFrameType.getIndex(type)] = keyFrame;
    }

    public void resetMovementKeyFrame(int clientTick, double currentTime)
    {
        KeyFrame kf = getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        if (kf == null)
        {
            return;
        }

        MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
        double diff = TimeSheetPanel.round(currentTime - keyFrame.getTick());
        int cTickDiff = (int) (diff * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH);
        int stepCTick = clientTick - cTickDiff;
        keyFrame.setStepClientTick(stepCTick);
    }

    public KeyFrame[] getKeyFrames(KeyFrameType type)
    {
        return frames[KeyFrameType.getIndex(type)];
    }

    public KeyFrame[] getAllKeyFrames()
    {
        KeyFrame[] keyFrames = new KeyFrame[0];
        for (KeyFrame[] kfs : frames)
        {
            keyFrames = ArrayUtils.addAll(keyFrames, kfs);
        }

        return keyFrames;
    }

    public void setKeyFrames(KeyFrame[] keyFrames, KeyFrameType type)
    {
        frames[KeyFrameType.getIndex(type)] = keyFrames;
    }

    /**
     * find a keyframe of the given type at the given tick
     * @param type the type of keyframe to look for
     * @param tick the tick at which the requested keyframe should exist
     * @return the keyframe, if one is found; otherwise, returns null
     */
    public KeyFrame findKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] frames = getKeyFrames(type);
        if (frames == null)
        {
            return null;
        }

        for (KeyFrame keyFrame : frames)
        {
            if (keyFrame.getTick() == tick)
            {
                return keyFrame;
            }
        }

        return null;
    }

    public KeyFrame findFirstKeyFrame()
    {
        KeyFrame firstFrame = null;

        for (KeyFrame[] keyFrames : frames)
        {
            if (keyFrames == null)
            {
                continue;
            }

            for (KeyFrame keyFrame : keyFrames)
            {
                if (firstFrame == null)
                {
                    firstFrame = keyFrame;
                }

                if (keyFrame.getTick() < firstFrame.getTick())
                {
                    firstFrame = keyFrame;
                }
            }
        }

        return firstFrame;
    }

    public KeyFrame findLastKeyFrame()
    {
        KeyFrame lastFrame = null;

        for (KeyFrame[] keyFrames : frames)
        {
            if (keyFrames == null)
            {
                continue;
            }

            for (KeyFrame keyFrame : keyFrames)
            {
                if (lastFrame == null)
                {
                    lastFrame = keyFrame;
                }

                if (keyFrame.getTick() > lastFrame.getTick())
                {
                    lastFrame = keyFrame;
                }
            }
        }

        return lastFrame;
    }

    /**
     * Finds the next keyframe for this character of any given KeyFrameType, excluding any keyframes on the current tick
     * @param tick the tick at which to start searching
     * @return the next keyframe
     */
    public KeyFrame findNextKeyFrame(double tick)
    {
        KeyFrame nextFrame = null;

        for (KeyFrame[] keyFrames : frames)
        {
            if (keyFrames == null)
            {
                continue;
            }

            if (keyFrames.length == 0)
            {
                continue;
            }

            for (int i = keyFrames.length - 1; i >= 0; i--)
            {
                KeyFrame keyFrame = keyFrames[i];

                if (nextFrame == null)
                {
                    nextFrame = keyFrame;
                    continue;
                }

                if (nextFrame.getTick() <= tick)
                {
                    nextFrame = keyFrame;
                    continue;
                }

                double test = keyFrame.getTick();
                if (test <= tick)
                {
                    continue;
                }

                if (test < nextFrame.getTick())
                {
                    nextFrame = keyFrame;
                }
            }
        }

        if (nextFrame == null)
        {
            return null;
        }

        if (nextFrame.getTick() < tick)
        {
            return null;
        }

        return nextFrame;
    }

    /**
     * Finds the next keyframe for this character of the given KeyFrameType, excluding any keyframes on the current tick
     * @param type the KeyFrameType to look for
     * @param tick the tick at which to start looking
     * @return the next Keyframe of the given KeyFrameType
     */
    public KeyFrame findNextKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            return null;
        }

        if (keyFrames.length == 0)
        {
            return null;
        }

        for (int i = 0; i < keyFrames.length; i++)
        {
            KeyFrame keyFrame = keyFrames[i];
            if (keyFrame.getTick() > tick)
            {
                return keyFrames[i];
            }
        }

        return null;
    }

    /**
     * Finds the previous keyframe for this character of any given KeyFrameType, excluding any keyframes on the current tick
     * @param tick the tick at which to start searching
     * @return the previous keyframe
     */
    public KeyFrame findPreviousKeyFrame(double tick)
    {
        KeyFrame nextFrame = null;

        for (KeyFrame[] keyFrames : frames)
        {
            if (keyFrames == null)
            {
                continue;
            }

            if (keyFrames.length == 0)
            {
                continue;
            }

            for (int i = 0; i < keyFrames.length; i++)
            {
                KeyFrame keyFrame = keyFrames[i];

                double test = keyFrame.getTick();
                if (test >= tick)
                {
                    continue;
                }

                if (nextFrame == null)
                {
                    nextFrame = keyFrame;
                }

                if (test > nextFrame.getTick())
                {
                    nextFrame = keyFrame;
                }
            }
        }

        if (nextFrame == null)
        {
            return null;
        }

        if (nextFrame.getTick() > tick)
        {
            return null;
        }

        return nextFrame;
    }

    /**
     * Finds the last KeyFrame of the given type relative to the given time
     * @param type the type of KeyFrame to search for
     * @param tick the given time
     * @param includeCurrentKeyFrame whether to include the keyframe at the given time (if any), or to skip and find the keyframe before
     * @return the last keyframe relative to the given time
     */
    public KeyFrame findPreviousKeyFrame(KeyFrameType type, double tick, boolean includeCurrentKeyFrame)
    {
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            return null;
        }

        if (keyFrames.length == 0)
        {
            return null;
        }

        for (int i = 0; i < keyFrames.length; i++)
        {
            KeyFrame keyFrame = keyFrames[i];

            if (!includeCurrentKeyFrame && keyFrame.getTick() == tick)
            {
                if (i == 0)
                {
                    return null;
                }

                return keyFrames[i - 1];
            }

            if (keyFrame.getTick() > tick)
            {
                if (i == 0)
                {
                    return null;
                }

                return keyFrames[i - 1];
            }
        }

        return keyFrames[keyFrames.length - 1];
    }

    /**
     * Adds the keyframe to a specific character, or replaces a keyframe if the tick matches exactly
     * @param keyFrame the keyframe to add or modify for the character
     * @return the keyframe that is being replaced; null if there is no keyframe being replaced
     */
    public KeyFrame addKeyFrame(KeyFrame keyFrame, double currentTime)
    {
        KeyFrameType type = keyFrame.getKeyFrameType();
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            keyFrames = new KeyFrame[]{keyFrame};
            setKeyFrames(keyFrames, type);

            KeyFrame currentKeyFrame = findPreviousKeyFrame(type, currentTime, true);
            setCurrentKeyFrame(currentKeyFrame, type);
            return null;
        }

        int[] framePosition = getFramePosition(keyFrames, keyFrame.getTick());
        KeyFrame keyFrameToReplace = null;

        // Check first if the new keyframe is replacing a previous one
        if (framePosition[1] == 1)
        {
            keyFrameToReplace = keyFrames[framePosition[0]];
            keyFrames[framePosition[0]] = keyFrame;
        }
        else
        {
            keyFrames = ArrayUtils.insert(framePosition[0], keyFrames, keyFrame);
        }

        setKeyFrames(keyFrames, type);

        KeyFrame currentKeyFrame = findPreviousKeyFrame(type, currentTime, true);
        setCurrentKeyFrame(currentKeyFrame, type);
        return keyFrameToReplace;
    }

    /**
     * Removes the indicated keyframe from the character
     * @param keyFrame the keyframe to remove
     */
    public void removeKeyFrame(KeyFrame keyFrame)
    {
        KeyFrameType type = keyFrame.getKeyFrameType();
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            return;
        }

        keyFrames = ArrayUtils.removeElement(keyFrames, keyFrame);
        setKeyFrames(keyFrames, type);

        if (getCurrentKeyFrame(type) == keyFrame)
        {
            setCurrentKeyFrame(null, type);
        }
    }

    /**
     * Gets the new position of the keyframe to add as an int[] of {index, boolean}
     * @param keyFrames the keyframe array to add to
     * @param newTick the tick of the new keyframe to be added
     * @return an int[] of {index, boolean}. The boolean determines whether the new keyframe will replace a previously existing keyframe of the exact same tick
     */
    private int[] getFramePosition(KeyFrame[] keyFrames, double newTick)
    {
        if (keyFrames == null)
        {
            return new int[]{0, 0};
        }

        int frameIndex = 0;
        for (int i = 0; i < keyFrames.length; i++)
        {
            if (keyFrames[i].getTick() == newTick)
            {
                return new int[]{i, 1};
            }

            if (keyFrames[i].getTick() > newTick)
            {
                if (i == 0)
                {
                    return new int[]{0, 0};
                }

                return new int[]{i, 0};
            }

            frameIndex++;
        }

        return new int[]{frameIndex, 0};
    }

    public MovementKeyFrame[] getMovementKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.MOVEMENT);
        if (keyFrames == null)
        {
            return null;
        }

        MovementKeyFrame[] keyFrame = new MovementKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (MovementKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }

    public AnimationKeyFrame[] getAnimationKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.ANIMATION);
        if (keyFrames == null)
        {
            return null;
        }

        AnimationKeyFrame[] keyFrame = new AnimationKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (AnimationKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }

    public SpawnKeyFrame[] getSpawnKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.SPAWN);
        if (keyFrames == null)
        {
            return null;
        }

        SpawnKeyFrame[] keyFrame = new SpawnKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (SpawnKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }

    public ModelKeyFrame[] getModelKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.MODEL);
        if (keyFrames == null)
        {
            return null;
        }

        ModelKeyFrame[] keyFrame = new ModelKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (ModelKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }

    public OrientationKeyFrame[] getOrientationKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.ORIENTATION);
        if (keyFrames == null)
        {
            return null;
        }

        OrientationKeyFrame[] keyFrame = new OrientationKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (OrientationKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }

    public TextKeyFrame[] getTextKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.TEXT);
        if (keyFrames == null)
        {
            return null;
        }

        TextKeyFrame[] keyFrame = new TextKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (TextKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }

    public OverheadKeyFrame[] getOverheadKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.OVERHEAD);
        if (keyFrames == null)
        {
            return null;
        }

        OverheadKeyFrame[] keyFrame = new OverheadKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (OverheadKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }

    public HealthKeyFrame[] getHealthKeyFrames()
    {
        KeyFrame[] keyFrames = getKeyFrames(KeyFrameType.HEALTH);
        if (keyFrames == null)
        {
            return null;
        }

        HealthKeyFrame[] keyFrame = new HealthKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (HealthKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }

    public SpotAnimKeyFrame[] getSpotAnimKeyFrames(KeyFrameType spotAnimNumber)
    {
        KeyFrame[] keyFrames = getKeyFrames(spotAnimNumber);
        if (keyFrames == null)
        {
            return null;
        }

        SpotAnimKeyFrame[] keyFrame = new SpotAnimKeyFrame[keyFrames.length];
        for (int i = 0; i < keyFrames.length; i++)
        {
            keyFrame[i] = (SpotAnimKeyFrame) keyFrames[i];
        }
        return keyFrame;
    }


    public void toggleActive(ClientThread clientThread)
    {
        setActive(!active, true, clientThread);
    }

    public void setVisible(boolean visible, ClientThread clientThread)
    {
        clientThread.invokeLater(() -> ckObject.setActive(visible));
    }

    public void setActive(boolean setActive, boolean reset, ClientThread clientThread)
    {
        clientThread.invokeLater(() ->
        {
            if (setActive)
            {
                active = true;
                if (reset)
                {
                    ckObject.setActive(false);
                }
                ckObject.setActive(true);
                spawnButton.setText("Spawn");
                return;
            }

            active = false;
            ckObject.setActive(false);
            spawnButton.setText("Despawn");
        });
    }
}
