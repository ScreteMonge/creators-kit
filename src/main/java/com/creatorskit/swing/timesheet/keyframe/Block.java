package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A user-defined grouping of keyframes on a single Character -- equivalent to
 * a "nested clip" in Premiere. The block has a name and colour the user
 * chooses on creation, plus a list of keyframe references that make up its
 * members.
 *
 * <p>Two structural invariants the creator enforces (and that drag /
 * keyframe-edit code is expected to maintain):
 * <ul>
 *   <li><b>No gaps in included properties.</b> For every property type that
 *   has at least one member keyframe in the block, every other keyframe of
 *   that type whose tick falls in [startTick, endTick] must also be a
 *   member. A block can span any subset of properties, but within each
 *   included property it owns the whole time slice.</li>
 *   <li><b>Disjoint in time on the same Character.</b> Two blocks on the
 *   same Character can never overlap in tick range. Validated at creation.</li>
 * </ul>
 *
 * <p>{@link #keyFrames} stores live references to the Character's keyframe
 * instances -- block membership is by object identity. Saves are not yet
 * wired (Phase 1 ships in-memory only); when added, persistence will use a
 * separate (type, tick) ref list and resolve at load time.
 */
@Getter
@Setter
public class Block
{
    private String name;
    /**
     * Packed RGB colour for the block rectangle on the timeline. Stored as an
     * int (not {@link java.awt.Color}) so Gson can save it without custom
     * adapters when persistence is added.
     */
    private int colorRgb;
    /**
     * Member keyframes by reference. The block's {@link #getStartTick} /
     * {@link #getEndTick} bounds are derived from these. Mutated by the
     * Block-maintenance hooks when keyframes are added / moved / removed on
     * the owning Character so the block stays in sync.
     */
    private List<KeyFrame> keyFrames = new ArrayList<>();

    public Block()
    {
        // Default constructor for future Gson use.
    }

    public Block(String name, int colorRgb, List<KeyFrame> keyFrames)
    {
        this.name = name;
        this.colorRgb = colorRgb;
        this.keyFrames = new ArrayList<>(keyFrames);
    }

    /** Earliest member keyframe tick, or 0 if the block is empty. */
    public double getStartTick()
    {
        double min = Double.POSITIVE_INFINITY;
        for (KeyFrame kf : keyFrames)
        {
            if (kf == null) continue;
            if (kf.getTick() < min) min = kf.getTick();
        }
        return min == Double.POSITIVE_INFINITY ? 0 : min;
    }

    /** Latest member keyframe tick, or 0 if the block is empty. */
    public double getEndTick()
    {
        double max = Double.NEGATIVE_INFINITY;
        for (KeyFrame kf : keyFrames)
        {
            if (kf == null) continue;
            if (kf.getTick() > max) max = kf.getTick();
        }
        return max == Double.NEGATIVE_INFINITY ? 0 : max;
    }

    /** Set of {@link KeyFrameType}s that have at least one member keyframe.
     *  Drives the row-span of the rendered rectangle and the no-gaps validation. */
    public java.util.Set<KeyFrameType> getIncludedTypes()
    {
        java.util.EnumSet<KeyFrameType> types = java.util.EnumSet.noneOf(KeyFrameType.class);
        for (KeyFrame kf : keyFrames)
        {
            if (kf != null) types.add(kf.getKeyFrameType());
        }
        return types;
    }

    /** True if {@code kf} is a member by reference identity. */
    public boolean contains(KeyFrame kf)
    {
        for (KeyFrame member : keyFrames)
        {
            if (member == kf) return true;
        }
        return false;
    }

    /** True if this block has no remaining members (e.g. all member keyframes
     *  have been deleted). Used by the maintenance pass to prune empty blocks. */
    public boolean isEmpty()
    {
        for (KeyFrame kf : keyFrames)
        {
            if (kf != null) return false;
        }
        return true;
    }
}
