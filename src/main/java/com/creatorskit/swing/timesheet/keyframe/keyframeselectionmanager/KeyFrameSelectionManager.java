package com.creatorskit.swing.timesheet.keyframe.keyframeselectionmanager;

import com.creatorskit.Character;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.stream.Collectors;

public class KeyFrameSelectionManager
{
    @Getter
    private KeyFrame primary;
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

    public void select(Character character, KeyFrame primaryKeyFrame)
    {
        clear();
        selectedKeyFrames.put(character, new KeyFrame[]{primaryKeyFrame});
        primary = primaryKeyFrame;
    }

    public void add(Character character, KeyFrame primaryKeyFrame)
    {
        LinkedHashMap<Character, KeyFrame[]> toAdd = new LinkedHashMap<>();
        toAdd.put(character, new KeyFrame[]{primaryKeyFrame});
        addAll(character, new KeyFrame[]{primaryKeyFrame}, primaryKeyFrame);
    }

    public void addAll(Character character, KeyFrame[] keyFrames, KeyFrame primaryKeyFrame)
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

        primary = primaryKeyFrame;
        fireChanged();
    }

    public void addAll(LinkedHashMap<Character, KeyFrame[]> groupsToAdd, KeyFrame primaryKeyFrame)
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

        primary = primaryKeyFrame;
        fireChanged();
    }

    public void remove(Character c, KeyFrame toRemove)
    {
        KeyFrame[] keyFrames = selectedKeyFrames.get(c);

        if (keyFrames != null)
        {
            keyFrames = ArrayUtils.removeElement(keyFrames, toRemove);

            if (keyFrames.length == 0)
            {
                selectedKeyFrames.remove(c);
            }
            else
            {
                selectedKeyFrames.put(c, keyFrames);
            }
        }

        if (primary == toRemove)
        {
            primary = null;
        }

        fireChanged();
    }

    public void removeAll(Character c, KeyFrame[] toRemove)
    {
        boolean containsPrimary = Arrays.stream(toRemove).anyMatch(keyFrame -> keyFrame.equals(primary));

        KeyFrame[] keyFrames = selectedKeyFrames.get(c);
        List<KeyFrame> list = new ArrayList<>(Arrays.asList(keyFrames));
        list.removeAll(Arrays.asList(toRemove));

        keyFrames = list.toArray(new KeyFrame[0]);

        if (keyFrames.length == 0)
        {
            selectedKeyFrames.remove(c);
        }
        else
        {
            selectedKeyFrames.put(c, keyFrames);
        }

        if (containsPrimary)
        {
            primary = null;
        }

        fireChanged();
    }

    public void removeAll(LinkedHashMap<Character, KeyFrame[]> groupsToRemove)
    {
        Map<Character, KeyFrame[]> pairingsToRemove = new HashMap<>();
        Map<Character, KeyFrame[]> pairingsToReplace = new HashMap<>();

        selectedKeyFrames.forEach((Character character, KeyFrame[] keyFrames) ->
        {
            KeyFrame[] toRemove = groupsToRemove.get(character);
            keyFrames = ArrayUtils.removeElement(keyFrames, toRemove);

            for (KeyFrame keyFrame : keyFrames)
            {
                if (keyFrame == primary)
                {
                    primary = null;
                    break;
                }
            }

            if (keyFrames.length == 0)
            {
                pairingsToRemove.put(character, keyFrames);
            }
            else
            {
                pairingsToReplace.put(character, keyFrames);
            }
        });

        for (Map.Entry<Character, KeyFrame[]> entry : pairingsToRemove.entrySet())
        {
            selectedKeyFrames.remove(entry.getKey());
        }

        selectedKeyFrames.putAll(pairingsToReplace);
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
