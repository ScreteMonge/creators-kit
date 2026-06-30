package com.creatorskit.swing.timesheet.keyframe.keyframeselectionmanager;

import com.creatorskit.Character;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

public class KeyFrameSelectionManager
{
    private final LinkedHashMap<Character, KeyFrame[]> selectedKeyFrames = new LinkedHashMap<>();
    private final List<KeyFrameSelectionManager.SelectionListener> listeners = new ArrayList<>();

    public LinkedHashMap<Character, KeyFrame[]> getSelected()
    {
        return selectedKeyFrames;
    }

    public boolean containsCharacter(Character c)
    {
        return selectedKeyFrames.containsKey(c);
    }

    public boolean containsKeyFrame(KeyFrame[] keyFrames)
    {
        return selectedKeyFrames.values().stream()
                .flatMap(Arrays::stream)
                .anyMatch(kf -> Arrays.asList(keyFrames).contains(kf));
    }

    public boolean containsKeyFrame(KeyFrame kf)
    {
        return selectedKeyFrames.values().stream()
                .flatMap(Arrays::stream)
                .anyMatch(keyFrame -> keyFrame.equals(kf));
    }

    public boolean isEmpty()
    {
        return selectedKeyFrames.isEmpty();
    }

    public int getSelectionSize()
    {
        return selectedKeyFrames.size();
    }

    public void add(Character character, KeyFrame keyFrame)
    {
        LinkedHashMap<Character, KeyFrame[]> toAdd = new LinkedHashMap<>();
        toAdd.put(character, new KeyFrame[]{keyFrame});
        add(toAdd);
    }

    public void add(Character character, KeyFrame[] keyFrames)
    {
        LinkedHashMap<Character, KeyFrame[]> toAdd = new LinkedHashMap<>();
        toAdd.put(character, keyFrames);
        add(toAdd);
    }

    public void add(LinkedHashMap<Character, KeyFrame[]> groupsToAdd)
    {
        groupsToAdd.forEach((character, keyFrames) ->
        {
            if (selectedKeyFrames.containsKey(character))
            {
                KeyFrame[] previouslySelected = selectedKeyFrames.get(character);
                Set<KeyFrame> toSelect = new HashSet<>();
                Collections.addAll(toSelect, previouslySelected);
                Collections.addAll(toSelect, keyFrames);

                selectedKeyFrames.put(character, toSelect.toArray(new KeyFrame[0]));
            }
            else
            {
                selectedKeyFrames.put(character, keyFrames);
            }
        });

        fireChanged();
    }

    public void remove(LinkedHashMap<Character, KeyFrame[]> groupsToRemove)
    {
        selectedKeyFrames.forEach((Character character, KeyFrame[] keyFrames) ->
        {
            KeyFrame[] toRemove = groupsToRemove.get(character);
            keyFrames = ArrayUtils.removeElement(keyFrames, toRemove);
            if (keyFrames.length == 0)
            {
                selectedKeyFrames.remove(character);
            }
            else
            {
                selectedKeyFrames.put(character, keyFrames);
            }
        });

        fireChanged();
    }

    public void clear()
    {
        selectedKeyFrames.clear();
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
