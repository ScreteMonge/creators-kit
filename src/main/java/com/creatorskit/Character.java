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
import net.runelite.api.RuneLiteObject;
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
    private boolean minimized;
    private KeyFrame[][] frames;
    private DefaultMutableTreeNode linkedManagerNode;
    private DefaultMutableTreeNode linkedTimeSheetNode;
    private DefaultMutableTreeNode parentManagerNode;
    private DefaultMutableTreeNode parentTimeSheetNode;
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
    private JSpinner orientationSpinner;
    private JSpinner radiusSpinner;
    private JLabel programmerLabel;
    private JSpinner programmerIdleSpinner;
    private RuneLiteObject runeLiteObject;
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

    public KeyFrame findKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] frames = getKeyFrames(type);
        for (KeyFrame keyFrame : frames)
        {
            if (keyFrame.getTick() == tick)
            {
                return keyFrame;
            }
        }

        return null;
    }

    public KeyFrame findNextKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] keyFrames = getKeyFrames(type);
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
     * Finds the last KeyFrame of the given type relative to the current time
     * @param type the type of KeyFrame to search for
     * @param tick the current time indicator
     * @param includeCurrentKeyFrame whether to include the keyframe at the current time (if any), or to skip and find the keyframe before
     * @return the last keyframe relative to the current time indicator
     */
    public KeyFrame findPreviousKeyFrame(KeyFrameType type, double tick, boolean includeCurrentKeyFrame)
    {
        KeyFrame[] keyFrames = getKeyFrames(type);
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
