package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Row layout for the LOCAL timeline view. Supports collapsible groups
 * that wrap a set of related rows under a single parent. Current
 * groups: <b>Hitsplats</b> (HITSPLAT_1..4) and <b>SpotAnims</b>
 * (SPOTANIM, SPOTANIM2).
 *
 * <p>The underlying row order is still
 * {@link KeyFrameType#LOCAL_KEYFRAME_TYPES_ALPHABETICAL}; the layout
 * inserts a synthetic parent row at the alphabetical position of each
 * group's display name (e.g. "Hitsplats" between "Health" and "Model",
 * "SpotAnims" between "Spawn" and "Special"). When a parent is
 * collapsed, its child rows are hidden -- the canvas's
 * {@code displayRowIndex(type)} returns -1 for collapsed children, so
 * their keyframes don't render.
 *
 * <p>State is per-instance; persistence (config roundtrip of the
 * collapsed-set) is owned by the caller.
 */
public class TimelineLocalRowLayout
{
    /** Display name shown in the label column for the Hitsplats parent row. */
    public static final String HITSPLATS_GROUP_NAME = "Hitsplats";
    /** Display name shown in the label column for the SpotAnims parent row. */
    public static final String SPOTANIMS_GROUP_NAME = "SpotAnims";

    /**
     * Declarative table of the groups this view supports. Each entry maps a
     * display name to the set of child types that belong under it. Adding a
     * new group is a one-line append here -- {@link #buildBaseRows} and
     * {@link Row#leaf} both read from this table, so they pick it up
     * automatically. Order matters only for tie-breaks within a single
     * alphabetical slot; in practice each parent inserts at its own unique
     * slot in the type list so collisions don't happen.
     */
    private static final java.util.LinkedHashMap<String, Set<KeyFrameType>> GROUPS = buildGroupTable();

    private static java.util.LinkedHashMap<String, Set<KeyFrameType>> buildGroupTable()
    {
        java.util.LinkedHashMap<String, Set<KeyFrameType>> m = new java.util.LinkedHashMap<>();
        m.put(HITSPLATS_GROUP_NAME, new LinkedHashSet<>(Arrays.asList(KeyFrameType.HITSPLAT_TYPES)));
        m.put(SPOTANIMS_GROUP_NAME, new LinkedHashSet<>(Arrays.asList(KeyFrameType.SPOTANIM_TYPES)));
        return m;
    }

    /** All types that belong to any group; cached so leaf-construction can check membership without walking GROUPS each call. */
    private static final Set<KeyFrameType> ALL_GROUPED_TYPES = buildAllGroupedTypes();

    private static Set<KeyFrameType> buildAllGroupedTypes()
    {
        Set<KeyFrameType> out = new java.util.HashSet<>();
        for (Set<KeyFrameType> children : GROUPS.values()) out.addAll(children);
        return out;
    }

    private final List<Row> baseRows;
    /** Set of group names currently collapsed. */
    private final Set<String> collapsed = new java.util.HashSet<>();
    /**
     * Types currently hidden via the Filters dialog. Filtering is
     * orthogonal to collapsing: a HITSPLAT type can be hidden even when
     * its Hitsplats group is expanded; a hidden type just doesn't paint
     * a row regardless of group state. Group parent rows themselves
     * collapse to nothing when ALL their child types are hidden -- they
     * stay visible if at least one child is still showing.
     */
    private final Set<KeyFrameType> hidden = new java.util.HashSet<>();

    public TimelineLocalRowLayout()
    {
        this.baseRows = buildBaseRows();
    }

    /**
     * Constructs the canonical row order, inserting each declared group's
     * parent row at the alphabetical slot of its first child. Children
     * follow immediately after their parent in their existing order; the
     * rendering layer hides them when the parent is collapsed.
     */
    private static List<Row> buildBaseRows()
    {
        // Track which group parents have already been emitted so we don't
        // re-emit them on subsequent children of the same group.
        Set<String> emittedParents = new java.util.HashSet<>();
        List<Row> rows = new ArrayList<>();
        for (KeyFrameType t : KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL)
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

    /** Returns the group name owning {@code type} from {@link #GROUPS}, or null. */
    private static String groupNameFor(KeyFrameType type)
    {
        for (java.util.Map.Entry<String, Set<KeyFrameType>> e : GROUPS.entrySet())
        {
            if (e.getValue().contains(type)) return e.getKey();
        }
        return null;
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
     * Visible row list, respecting current collapse + filter state.
     * Hidden leaf types are dropped entirely. Group parent rows are
     * visible if any of their children would still render -- a parent
     * with every child hidden disappears too.
     */
    public List<Row> visibleRows()
    {
        // Pass 1: collect leaves we'd emit, accounting for filter +
        // collapse state. Pass 2: emit each leaf, prefixed by its
        // group parent (once) if it has one and the group hasn't been
        // emitted yet.
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
            // LEAF. Skip if filtered out.
            if (hidden.contains(r.type)) continue;
            // Skip if its group exists and is collapsed.
            if (currentGroup != null
                    && currentGroup.childTypes.contains(r.type)
                    && collapsed.contains(currentGroup.groupName))
            {
                // Still emit the parent ONCE so the user can re-expand.
                if (!groupParentEmitted)
                {
                    out.add(currentGroup);
                    groupParentEmitted = true;
                }
                continue;
            }
            // Emit parent the first time we hit a visible child.
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

    public Set<KeyFrameType> getHidden()
    {
        return hidden;
    }

    /** Replace the hidden set in one shot (used by the filters dialog on Close). */
    public void setHidden(Set<KeyFrameType> newHidden)
    {
        hidden.clear();
        if (newHidden != null) hidden.addAll(newHidden);
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
            // Membership reads from the centralised GROUPS table so adding a
            // new group anywhere automatically flags its children for the
            // indent treatment.
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
