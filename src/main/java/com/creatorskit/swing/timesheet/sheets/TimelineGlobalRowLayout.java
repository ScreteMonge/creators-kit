package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Row layout for the GLOBAL timeline view. Currently has no group
 * concept (unlike {@link TimelineLocalRowLayout}'s Hitsplats grouping)
 * -- just hides filtered-out types from the row order. Exists so the
 * Filters dialog can read / write a single "hidden set" symmetrically
 * for both views.
 */
public class TimelineGlobalRowLayout
{
    private final Set<KeyFrameType> hidden = new HashSet<>();

    /**
     * Returns the types to render, in
     * {@link KeyFrameType#GLOBAL_KEYFRAME_TYPES_ALPHABETICAL} order, minus
     * any in the hidden set.
     */
    public List<KeyFrameType> visibleTypes()
    {
        List<KeyFrameType> out = new ArrayList<>();
        for (KeyFrameType t : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
        {
            if (!hidden.contains(t)) out.add(t);
        }
        return out;
    }

    /**
     * Position of {@code type} among the currently-visible types, or -1
     * if hidden / not a global. Mirrors {@link TimelineLocalRowLayout#rowIndexOf}.
     */
    public int rowIndexOf(KeyFrameType type)
    {
        List<KeyFrameType> v = visibleTypes();
        for (int i = 0; i < v.size(); i++)
        {
            if (v.get(i) == type) return i;
        }
        return -1;
    }

    public Set<KeyFrameType> getHidden()
    {
        return hidden;
    }

    public void setHidden(Set<KeyFrameType> newHidden)
    {
        hidden.clear();
        if (newHidden != null) hidden.addAll(newHidden);
    }
}
