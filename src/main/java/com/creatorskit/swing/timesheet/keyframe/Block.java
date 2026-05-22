package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.Character;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A user-defined time-range grouping of keyframes on a single Character --
 * the Premiere nested-clip equivalent.
 *
 * <p>"Dumb" range-based model: a block stores its own name, colour, and
 * an absolute tick range {@code [startTick, endTick]}. Its <b>members</b>
 * are derived on demand at any time as "every keyframe on the owning
 * Character whose tick falls inside that range". There is no separate
 * member list to keep in sync -- moving a keyframe into or out of the
 * range is the same as adding / removing it from the block.
 *
 * <p>Trade-off vs the previous per-keyframe membership model: the user
 * can't keep a kf inside the range OUT of the block. If you don't want
 * it in the block, move it out of the range. In exchange the block
 * renders as a simple full-height time-range strip with no "did the
 * rect cover a row I didn't include?" confusion.
 *
 * <p>Two blocks on the same Character may not overlap in tick range
 * (validated at creation -- see {@link BlockValidator#overlapsExistingBlock}).
 */
@Getter
@Setter
public class Block
{
    private String name;
    /** Packed RGB int (Gson-friendly, no awt.Color save adapter required). */
    private int colorRgb;
    /** Inclusive left bound of the block's tick range. */
    private double startTick;
    /** Inclusive right bound of the block's tick range. */
    private double endTick;

    public Block()
    {
        // Default constructor for future Gson use.
    }

    public Block(String name, int colorRgb, double startTick, double endTick)
    {
        this.name = name;
        this.colorRgb = colorRgb;
        this.startTick = startTick;
        this.endTick = endTick;
    }

    /** True if {@code tick} is inside the block's closed range. */
    public boolean containsTick(double tick)
    {
        return tick >= startTick && tick <= endTick;
    }

    /**
     * Walks {@code character}'s frame matrix and returns every keyframe whose
     * tick falls inside the block's range. Used by click-to-select-members,
     * delete-block-with-keyframes, and any other operation that needs the
     * live member set. Cheap enough to call repeatedly -- the matrix is
     * small (20 type rows, typically &lt;100 kfs total per Character).
     */
    public List<KeyFrame> resolveMembers(Character character)
    {
        List<KeyFrame> out = new ArrayList<>();
        if (character == null) return out;
        KeyFrame[][] frames = character.getFrames();
        if (frames == null) return out;
        for (KeyFrame[] row : frames)
        {
            if (row == null) continue;
            for (KeyFrame kf : row)
            {
                if (kf == null) continue;
                if (containsTick(kf.getTick())) out.add(kf);
            }
        }
        return out;
    }
}
