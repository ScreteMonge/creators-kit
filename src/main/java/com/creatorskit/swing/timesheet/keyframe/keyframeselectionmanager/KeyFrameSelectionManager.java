package com.creatorskit.swing.timesheet.keyframe.keyframeselectionmanager;

import com.creatorskit.Character;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameCategory;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameTarget;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

public class KeyFrameSelectionManager
{
    @Getter
    private KeyFrame primary;
    private final LinkedHashMap<KeyFrameTarget, KeyFrame[]> selectedKeyFrames = new LinkedHashMap<>();
    private final List<KeyFrameSelectionManager.SelectionListener> listeners = new ArrayList<>();

    public LinkedHashMap<KeyFrameTarget, KeyFrame[]> getSelected()
    {
        return selectedKeyFrames;
    }

    public boolean containsKeyFrame(KeyFrame[] keyFrames)
    {
        return selectedKeyFrames.values().stream()
                .flatMap(Arrays::stream)
                .anyMatch(kf -> Arrays.asList(keyFrames).contains(kf));
    }

    public boolean isEmpty()
    {
        return selectedKeyFrames.isEmpty();
    }

    public void select(KeyFrameTarget target, KeyFrame primaryKeyFrame)
    {
        clear();
        selectedKeyFrames.put(target, new KeyFrame[]{primaryKeyFrame});
        primary = primaryKeyFrame;
    }

    public void add(KeyFrameTarget target, KeyFrame primaryKeyFrame)
    {
        add(target, new KeyFrame[]{primaryKeyFrame}, primaryKeyFrame);
    }

    public void add(KeyFrameTarget target, KeyFrame[] keyFrames, KeyFrame primaryKeyFrame)
    {
        if (selectedKeyFrames.containsKey(target))
        {
            KeyFrame[] previouslySelected = selectedKeyFrames.get(target);
            Set<KeyFrame> toSelect = new HashSet<>();
            Collections.addAll(toSelect, previouslySelected);
            Collections.addAll(toSelect, keyFrames);

            selectedKeyFrames.put(target, toSelect.toArray(new KeyFrame[0]));
        }
        else
        {
            selectedKeyFrames.put(target, keyFrames);
        }

        postAddGroups(primaryKeyFrame);
    }

    public void addCameraGroups(KeyFrame[] cameraKeyFrames, KeyFrame primaryKeyFrame)
    {
        if (selectedKeyFrames.containsKey(new KeyFrameTarget(KeyFrameCategory.CAMERA, null)))
        {
            KeyFrame[] previouslySelected = selectedKeyFrames.get(new KeyFrameTarget(KeyFrameCategory.CAMERA, null));
            Set<KeyFrame> toAdd = new HashSet<>();
            Collections.addAll(toAdd, previouslySelected);
            Collections.addAll(toAdd, cameraKeyFrames);

            selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), toAdd.toArray(new KeyFrame[0]));
        }
        else
        {
            selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), cameraKeyFrames);
        }

        postAddGroups(primaryKeyFrame);
    }

    public void addCharacterGroups(LinkedHashMap<Character, KeyFrame[]> groupsToAdd, KeyFrame primaryKeyFrame)
    {
        groupsToAdd.forEach((character, keyFrames) ->
        {
            if (selectedKeyFrames.containsKey(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character)))
            {
                KeyFrame[] previouslySelected = selectedKeyFrames.get(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character));
                Set<KeyFrame> toSelect = new HashSet<>();
                Collections.addAll(toSelect, previouslySelected);
                Collections.addAll(toSelect, keyFrames);

                selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character), toSelect.toArray(new KeyFrame[0]));
            }
            else
            {
                selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character), keyFrames);
            }
        });

        postAddGroups(primaryKeyFrame);
    }

    public void addGroups(KeyFrame[] cameraKeyFrames, LinkedHashMap<Character, KeyFrame[]> groupsToAdd, KeyFrame primaryKeyFrame)
    {
        if (selectedKeyFrames.containsKey(new KeyFrameTarget(KeyFrameCategory.CAMERA, null)))
        {
            KeyFrame[] previouslySelected = selectedKeyFrames.get(new KeyFrameTarget(KeyFrameCategory.CAMERA, null));
            Set<KeyFrame> toAdd = new HashSet<>();
            Collections.addAll(toAdd, previouslySelected);
            Collections.addAll(toAdd, cameraKeyFrames);

            selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), toAdd.toArray(new KeyFrame[0]));
        }
        else
        {
            selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), cameraKeyFrames);
        }

        groupsToAdd.forEach((character, keyFrames) ->
        {
            if (selectedKeyFrames.containsKey(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character)))
            {
                KeyFrame[] previouslySelected = selectedKeyFrames.get(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character));
                Set<KeyFrame> toSelect = new HashSet<>();
                Collections.addAll(toSelect, previouslySelected);
                Collections.addAll(toSelect, keyFrames);

                selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character), toSelect.toArray(new KeyFrame[0]));
            }
            else
            {
                selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character), keyFrames);
            }
        });

        postAddGroups(primaryKeyFrame);
    }

    private void postAddGroups(KeyFrame primaryKeyFrame)
    {
        primary = primaryKeyFrame;
        fireChanged();
    }

    public void remove(KeyFrameTarget target, KeyFrame toRemove)
    {
        KeyFrame[] keyFrames = selectedKeyFrames.get(target);
        if (keyFrames != null)
        {
            keyFrames = ArrayUtils.removeElement(keyFrames, toRemove);

            if (keyFrames.length == 0)
            {
                selectedKeyFrames.remove(target);
            }
            else
            {
                selectedKeyFrames.put(target, keyFrames);
            }
        }

        if (primary == toRemove)
        {
            primary = null;
        }

        fireChanged();
    }

    public void clear()
    {
        selectedKeyFrames.clear();
        primary = null;
        fireChanged();
    }

    public void addListener(KeyFrameSelectionManager.SelectionListener listener)
    {
        listeners.add(listener);
    }

    private void fireChanged()
    {
        for (KeyFrameSelectionManager.SelectionListener l : new ArrayList<>(listeners))
        {
            l.selectionChanged(this);
        }
    }

    public interface SelectionListener
    {
        void selectionChanged(KeyFrameSelectionManager manager);
    }
}
