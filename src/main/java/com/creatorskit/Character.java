package com.creatorskit;

import com.creatorskit.models.CustomModel;
import com.creatorskit.programming.Program;
import com.creatorskit.swing.ObjectPanel;
import com.creatorskit.swing.ParentPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

@Getter
@Setter
@AllArgsConstructor
public class Character
{
    private String name;
    private boolean active;
    private boolean locationSet;
    private KeyFrame[][] frames;
    private DefaultMutableTreeNode linkedManagerNode;
    private DefaultMutableTreeNode parentManagerNode;
    private Program program;
    private WorldPoint nonInstancedPoint;
    private LocalPoint instancedPoint;
    private int[] instancedRegions;
    private int instancedPlane;
    private boolean inInstance;
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
    private JLabel programmerLabel;
    private JSpinner programmerIdleSpinner;
    private CKObject ckObject;
    private int targetOrientation;

    @Override
    public String toString()
    {
        return name;
    }

    public KeyFrame[] getKeyFrames(KeyFrameType type)
    {
        return frames[KeyFrameType.getIndex(type)];
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
     * Finds the last KeyFrame of the given type relative to the current time
     * @param type the type of KeyFrame to search for
     * @param tick the current time indicator
     * @param includeCurrentKeyFrame whether to include the keyframe at the current time (if any), or to skip and find the keyframe before
     * @return the last keyframe relative to the current time indicator
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
     */
    public void addKeyFrame(KeyFrame keyFrame)
    {
        KeyFrameType type = keyFrame.getKeyFrameType();
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            keyFrames = new KeyFrame[]{keyFrame};
            setKeyFrames(keyFrames, type);
            return;
        }

        int[] framePosition = getFramePosition(keyFrames, keyFrame.getTick());

        // Check first if the new keyframe is replacing a previous one
        if (framePosition[1] == 1)
        {
            keyFrames[framePosition[0]] = keyFrame;
        }
        else
        {
            keyFrames = ArrayUtils.insert(framePosition[0], keyFrames, keyFrame);
        }

        setKeyFrames(keyFrames, type);
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
    }

    /**
     * Removes a keyframe from the character (if it exists) of the chosen type, at the chosen tick
     * @param type the KeyFrameType to remove
     * @param tick the tick at which to find the keyframe
     */
    public void removeKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] keyFrames = getKeyFrames(type);
        if (keyFrames == null)
        {
            return;
        }

        for (int i = 0; i < keyFrames.length; i++)
        {
            KeyFrame keyFrame = keyFrames[i];
            if (keyFrame.getTick() == tick)
            {
                removeKeyFrame(keyFrame);
                return;
            }
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
}
