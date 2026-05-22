package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.Character;

import java.util.Collection;
import java.util.List;

/**
 * Pure-function helpers that decide whether a candidate keyframe selection
 * can become a {@link Block} on a given Character. With the range-based
 * Block model the rules are simple:
 *
 * <ul>
 *   <li>{@link #isValidSelection} -- selection has at least 2 keyframes
 *   (a single-tick "range" is meaningless as a block).</li>
 *   <li>{@link #overlapsExistingBlock} -- the proposed range
 *   {@code [min(tick), max(tick)]} doesn't intersect any existing block
 *   on the same Character. Disjoint ranges, per spec.</li>
 * </ul>
 *
 * <p>No "no gaps in included properties" rule anymore -- the range-based
 * model includes EVERY keyframe in the time range, so gaps are
 * structurally impossible.
 */
public final class BlockValidator
{
    private BlockValidator() {}

    /**
     * Returns true when {@code selection} on {@code character} can become a
     * Block: at least 2 keyframes and the implied range doesn't overlap an
     * existing block. The overlap check is decoupled into
     * {@link #overlapsExistingBlock} so the create-flow can report it as a
     * distinct "skipped" reason in the multi-Character warning.
     */
    public static boolean isValidSelection(Character character, Collection<KeyFrame> selection)
    {
        if (character == null || selection == null) return false;
        int real = 0;
        for (KeyFrame kf : selection)
        {
            if (kf != null) real++;
            if (real >= 2) return true;
        }
        return false;
    }

    /**
     * Returns true if the proposed [minTick, maxTick] range intersects any
     * existing block's range on the same Character. Used by the creator to
     * enforce the disjoint-in-time rule. Endpoints touching counts as
     * overlap (the user's "disjoint" rule rejects shared ticks).
     */
    public static boolean overlapsExistingBlock(Character character, double minTick, double maxTick)
    {
        if (character == null) return false;
        List<Block> blocks = character.getBlocks();
        if (blocks == null) return false;
        for (Block b : blocks)
        {
            if (b == null) continue;
            if (b.getEndTick() < minTick || b.getStartTick() > maxTick) continue;
            return true;
        }
        return false;
    }

    /** Computes [min, max] tick range of a keyframe selection in a single pass. */
    public static double[] tickRange(Collection<KeyFrame> selection)
    {
        double minTick = Double.POSITIVE_INFINITY;
        double maxTick = Double.NEGATIVE_INFINITY;
        for (KeyFrame kf : selection)
        {
            if (kf == null) continue;
            if (kf.getTick() < minTick) minTick = kf.getTick();
            if (kf.getTick() > maxTick) maxTick = kf.getTick();
        }
        if (minTick == Double.POSITIVE_INFINITY) return new double[]{0, 0};
        return new double[]{minTick, maxTick};
    }
}
