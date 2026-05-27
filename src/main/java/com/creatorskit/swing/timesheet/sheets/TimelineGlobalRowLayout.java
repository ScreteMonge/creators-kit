package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Row layout for the GLOBAL timeline view. Carries both a hidden-set
 * (driven by the global Filters dialog) and collapsible groups (driven
 * by chevron clicks on the group parent rows) -- same shape as
 * {@link TimelineLocalRowLayout} so the label column and renderer can
 * use either symmetrically.
 *
 * <p>Currently exposes one group: <b>Area Sounds</b> wrapping
 * {@code SOUND_1..4}. Adding a group is a one-line append to
 * {@link #GROUPS}.
 */
public class TimelineGlobalRowLayout
{
    /** Display name shown in the label column for the Area Sounds parent row. */
    public static final String AREA_SOUNDS_GROUP_NAME = "Area Sounds";

    /** Declarative table of groups for the global view. See local-layout sibling. */
    private static final LinkedHashMap<String, Set<KeyFrameType>> GROUPS = buildGroupTable();

    private static LinkedHashMap<String, Set<KeyFrameType>> buildGroupTable()
    {
        LinkedHashMap<String, Set<KeyFrameType>> m = new LinkedHashMap<>();
        // Area Sound 1..4 share the SOUND_TYPES enum array (the local /
        // global distinction lives in KeyFrameType.isGlobal, not in the
        // membership set).
        m.put(AREA_SOUNDS_GROUP_NAME, new LinkedHashSet<>(Arrays.asList(KeyFrameType.SOUND_TYPES)));
        return m;
    }

    private static final Set<KeyFrameType> ALL_GROUPED_TYPES = buildAllGroupedTypes();

    private static Set<KeyFrameType> buildAllGroupedTypes()
    {
        Set<KeyFrameType> out = new HashSet<>();
        for (Set<KeyFrameType> children : GROUPS.values()) out.addAll(children);
        return out;
    }

    private final List<Row> baseRows = buildBaseRows();
    private final Set<String> collapsed = new HashSet<>();
    private final Set<KeyFrameType> hidden = new HashSet<>();

    /**
     * Walks {@link KeyFrameType#GLOBAL_KEYFRAME_TYPES_ALPHABETICAL} and
     * inserts a group parent row at the alphabetical slot of the first
     * child encountered for each group. Same shape as
     * {@link TimelineLocalRowLayout#buildBaseRows}.
     */
    private static List<Row> buildBaseRows()
    {
        Set<String> emittedParents = new HashSet<>();
        List<Row> rows = new ArrayList<>();
        for (KeyFrameType t : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
        {
            String owningGroup = groupNameFor(t);
            if (owningGroup != null && !emittedParents.contains(owningGroup))
            {
                rows.add(Row.parent(owningGroup, GROUPS.get(owningGroup)));
                emittedParents.add(owningGroup);
            }
            rows.add(Row.leaf(t));
        }
        return Collections.unmodifiableList(rows);
    }

    private static String groupNameFor(KeyFrameType type)
    {
        for (Map.Entry<String, Set<KeyFrameType>> e : GROUPS.entrySet())
        {
            if (e.getValue().contains(type)) return e.getKey();
        }
        return null;
    }

    // ---------- collapse state ----------

    public Set<String> getCollapsed()
    {
        return Collections.unmodifiableSet(collapsed);
    }

    public void setCollapsed(String groupName, boolean isCollapsed)
    {
        if (isCollapsed) collapsed.add(groupName);
        else collapsed.remove(groupName);
    }

    public boolean isCollapsed(String groupName)
    {
        return collapsed.contains(groupName);
    }

    public void toggleCollapsed(String groupName)
    {
        if (collapsed.contains(groupName)) collapsed.remove(groupName);
        else collapsed.add(groupName);
    }

    // ---------- visible-row computation ----------

    /**
     * Returns the rendered row list honoring both the hidden set and
     * the collapse state. Identical algorithm to the local-layout
     * sibling -- group parents are emitted when at least one child
     * would render or when the group is collapsed (so the user can
     * re-expand).
     */
    public List<Row> visibleRows()
    {
        List<Row> out = new ArrayList<>();
        Row currentGroup = null;
        boolean groupParentEmitted = false;
        for (Row r : baseRows)
        {
            if (r.kind == Row.Kind.GROUP_PARENT)
            {
                currentGroup = r;
                groupParentEmitted = false;
                continue;
            }
            if (hidden.contains(r.type)) continue;
            if (currentGroup != null
                    && currentGroup.childTypes.contains(r.type)
                    && collapsed.contains(currentGroup.groupName))
            {
                if (!groupParentEmitted)
                {
                    out.add(currentGroup);
                    groupParentEmitted = true;
                }
                continue;
            }
            if (currentGroup != null
                    && currentGroup.childTypes.contains(r.type)
                    && !groupParentEmitted)
            {
                out.add(currentGroup);
                groupParentEmitted = true;
            }
            out.add(r);
        }
        return out;
    }

    /**
     * Legacy method preserved for callers that just want a flat
     * type list. Skips group parents.
     */
    public List<KeyFrameType> visibleTypes()
    {
        List<KeyFrameType> out = new ArrayList<>();
        for (Row r : visibleRows())
        {
            if (r.kind == Row.Kind.LEAF) out.add(r.type);
        }
        return out;
    }

    /**
     * Visible-row index of {@code type}, or -1 when its group is
     * collapsed or the type is filtered out. Mirrors
     * {@link TimelineLocalRowLayout#rowIndexOf}.
     */
    public int rowIndexOf(KeyFrameType type)
    {
        List<Row> visible = visibleRows();
        for (int i = 0; i < visible.size(); i++)
        {
            Row r = visible.get(i);
            if (r.kind == Row.Kind.LEAF && r.type == type) return i;
        }
        return -1;
    }

    /**
     * If the row at visible index {@code visibleIdx} is a group parent,
     * returns its group name; otherwise null. Drives the chevron
     * rendering in the label column.
     */
    public String groupNameAt(int visibleIdx)
    {
        List<Row> v = visibleRows();
        if (visibleIdx < 0 || visibleIdx >= v.size()) return null;
        Row r = v.get(visibleIdx);
        return r.kind == Row.Kind.GROUP_PARENT ? r.groupName : null;
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

    // ---------- row holder ----------

    /**
     * Mirror of {@link TimelineLocalRowLayout.Row}; kept as a separate
     * class so the two views can evolve independently without breaking
     * each other's consumers.
     */
    public static final class Row
    {
        public enum Kind { LEAF, GROUP_PARENT }

        public final Kind kind;
        public final KeyFrameType type;
        public final String groupName;
        public final Set<KeyFrameType> childTypes;
        public final boolean isGroupChild;

        private Row(Kind kind, KeyFrameType type, String groupName, Set<KeyFrameType> childTypes, boolean isGroupChild)
        {
            this.kind = kind;
            this.type = type;
            this.groupName = groupName;
            this.childTypes = childTypes;
            this.isGroupChild = isGroupChild;
        }

        static Row leaf(KeyFrameType type)
        {
            boolean inGroup = ALL_GROUPED_TYPES.contains(type);
            return new Row(Kind.LEAF, type, null, null, inGroup);
        }

        static Row parent(String groupName, Set<KeyFrameType> childTypes)
        {
            return new Row(Kind.GROUP_PARENT, null, groupName, childTypes, false);
        }

        public String displayName()
        {
            return kind == Kind.GROUP_PARENT ? groupName : type.getName();
        }
    }
}
