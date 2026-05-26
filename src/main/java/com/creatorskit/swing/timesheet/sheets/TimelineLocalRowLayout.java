package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Row layout for the LOCAL timeline view. v1 introduces a single
 * collapsible group: <b>Hitsplats</b>, wrapping HITSPLAT_1..4.
 *
 * <p>The underlying row order is still
 * {@link KeyFrameType#LOCAL_KEYFRAME_TYPES_ALPHABETICAL}; the layout
 * just inserts a synthetic parent row at the alphabetical position of
 * the group's name ("Hitsplats" sits between "Health" and "Model").
 * When the parent is collapsed, its child rows are hidden -- the
 * canvas's {@code displayRowIndex(type)} returns -1 for collapsed
 * children, so their keyframes don't render.
 *
 * <p>State is per-instance; persistence (config roundtrip of the
 * collapsed-set) is owned by the caller.
 */
public class TimelineLocalRowLayout
{
    /** Display name shown in the label column for the group parent row. */
    public static final String HITSPLATS_GROUP_NAME = "Hitsplats";

    private final List<Row> baseRows;
    /** Set of group names currently collapsed. */
    private final Set<String> collapsed = new java.util.HashSet<>();

    public TimelineLocalRowLayout()
    {
        this.baseRows = buildBaseRows();
    }

    /**
     * Constructs the canonical row order, inserting the group parent at
     * the alphabetical slot of its name ("Hitsplats" lands between
     * "Health" and "Model"). Children follow immediately after the
     * parent in their existing order.
     */
    private static List<Row> buildBaseRows()
    {
        // The 4 hitsplat types as a Set for membership checks.
        Set<KeyFrameType> hitsplatChildren = new LinkedHashSet<>(Arrays.asList(KeyFrameType.HITSPLAT_TYPES));

        // Walk the alphabetical list. When we'd output the first hitsplat
        // child, output the parent group first. Subsequent hitsplat
        // children are still emitted as LEAF rows -- the rendering layer
        // hides them when the parent is collapsed.
        List<Row> rows = new ArrayList<>();
        boolean emittedHitsplatsParent = false;
        for (KeyFrameType t : KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL)
        {
            if (hitsplatChildren.contains(t) && !emittedHitsplatsParent)
            {
                rows.add(Row.parent(HITSPLATS_GROUP_NAME, hitsplatChildren));
                emittedHitsplatsParent = true;
            }
            rows.add(Row.leaf(t));
        }
        return Collections.unmodifiableList(rows);
    }

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

    /**
     * Visible row list, respecting current collapse state. Group parent
     * rows are always visible; their children are hidden when the parent
     * is collapsed.
     */
    public List<Row> visibleRows()
    {
        List<Row> out = new ArrayList<>();
        Row currentGroup = null;
        for (Row r : baseRows)
        {
            if (r.kind == Row.Kind.GROUP_PARENT)
            {
                currentGroup = r;
                out.add(r);
                continue;
            }
            // LEAF: include unless its owning group is collapsed.
            if (currentGroup != null && currentGroup.childTypes.contains(r.type) && collapsed.contains(currentGroup.groupName))
            {
                continue;
            }
            out.add(r);
        }
        return out;
    }

    /**
     * Returns the visible-row index for {@code type}, or -1 if its
     * owning group is collapsed (rendering should skip it) OR the type
     * isn't in the local view at all (e.g. a global).
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
     * returns its group name; otherwise null. Used by the label column
     * to render the chevron + click handler.
     */
    public String groupNameAt(int visibleIdx)
    {
        List<Row> v = visibleRows();
        if (visibleIdx < 0 || visibleIdx >= v.size()) return null;
        Row r = v.get(visibleIdx);
        return r.kind == Row.Kind.GROUP_PARENT ? r.groupName : null;
    }

    /**
     * Returns the group name owning {@code type}, or null if the type is
     * not part of any group.
     */
    public String groupOf(KeyFrameType type)
    {
        for (Row r : baseRows)
        {
            if (r.kind == Row.Kind.GROUP_PARENT && r.childTypes.contains(type))
            {
                return r.groupName;
            }
        }
        return null;
    }

    /**
     * Single row definition. Leaf rows are backed by a single KeyFrameType;
     * parent rows carry a group name + the set of child types they hide
     * when collapsed.
     */
    public static final class Row
    {
        public enum Kind { LEAF, GROUP_PARENT }

        public final Kind kind;
        public final KeyFrameType type;        // non-null for LEAF
        public final String groupName;          // non-null for GROUP_PARENT
        public final Set<KeyFrameType> childTypes; // non-null for GROUP_PARENT
        public final boolean isGroupChild;      // true if this LEAF belongs to a group

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
            boolean inGroup = Arrays.asList(KeyFrameType.HITSPLAT_TYPES).contains(type);
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
