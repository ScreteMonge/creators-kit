package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.Character;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Pure-function helpers that decide whether a candidate keyframe selection
 * forms a valid {@link Block} on a given Character.
 *
 * <p>Two checks the creator runs before instantiating a Block:
 * <ul>
 *   <li>{@link #isValidSelection} -- no gaps in any included property over
 *   the selection's [min, max] tick range.</li>
 *   <li>{@link #overlapsExistingBlock} -- proposed range doesn't intersect
 *   an existing block on the same Character.</li>
 * </ul>
 *
 * <p>Kept as static helpers (not methods on Block / Character) so the
 * TimeSheetPanel's create-block flow can validate independently of mutation
 * paths and produce a single batched "X of Y selections are valid" message.
 */
public final class BlockValidator
{
    private BlockValidator() {}

    /**
     * Returns true when {@code selection} on {@code character} can become a
     * Block: at least 2 keyframes, and for every property type that's
     * represented, every other keyframe of that type whose tick falls
     * inside [min, max] of the selection is also in the selection.
     */
    public static boolean isValidSelection(Character character, Collection<KeyFrame> selection)
    {
        if (character == null || selection == null || selection.size() < 2) return false;

        double minTick = Double.POSITIVE_INFINITY;
        double maxTick = Double.NEGATIVE_INFINITY;
        java.util.EnumSet<KeyFrameType> types = java.util.EnumSet.noneOf(KeyFrameType.class);
        for (KeyFrame kf : selection)
        {
            if (kf == null) continue;
            if (kf.getTick() < minTick) minTick = kf.getTick();
            if (kf.getTick() > maxTick) maxTick = kf.getTick();
            types.add(kf.getKeyFrameType());
        }
        if (minTick == Double.POSITIVE_INFINITY) return false;

        // No-gaps: for each included property, every keyframe whose tick is
        // in the closed range [minTick, maxTick] must be a member by ref.
        for (KeyFrameType type : types)
        {
            KeyFrame[] row = character.getKeyFrames(type);
            if (row == null) continue;
            for (KeyFrame kf : row)
            {
                if (kf == null) continue;
                if (kf.getTick() < minTick || kf.getTick() > maxTick) continue;
                if (!containsByRef(selection, kf)) return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the proposed [minTick, maxTick] range intersects any
     * existing block's [start, end] range on the same Character. Used by
     * the creator to enforce the disjoint-in-time rule.
     */
    public static boolean overlapsExistingBlock(Character character, double minTick, double maxTick)
    {
        if (character == null) return false;
        List<Block> blocks = character.getBlocks();
        if (blocks == null) return false;
        for (Block b : blocks)
        {
            if (b == null || b.isEmpty()) continue;
            double bStart = b.getStartTick();
            double bEnd = b.getEndTick();
            // Closed intervals overlap when neither ends strictly before the
            // other starts. Endpoints touching counts as overlap (the user's
            // "disjoint" rule rejects shared ticks).
            if (bEnd < minTick || bStart > maxTick) continue;
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

    /** Set of property types touched by a selection. */
    public static Set<KeyFrameType> includedTypes(Collection<KeyFrame> selection)
    {
        java.util.EnumSet<KeyFrameType> types = java.util.EnumSet.noneOf(KeyFrameType.class);
        for (KeyFrame kf : selection)
        {
            if (kf != null) types.add(kf.getKeyFrameType());
        }
        return types;
    }

    private static boolean containsByRef(Collection<KeyFrame> coll, KeyFrame needle)
    {
        for (KeyFrame kf : coll)
        {
            if (kf == needle) return true;
        }
        return false;
    }
}
