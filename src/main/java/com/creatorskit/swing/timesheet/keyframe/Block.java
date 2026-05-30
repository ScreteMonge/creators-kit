package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;
import lombok.Setter;

/**
 * Data model for a timeline <b>Label</b>: a colored, named time-range marker
 * shown in the top row of the timeline purely for organization -- it makes
 * clear to the viewer what a slice of time represents. Labels have NO
 * interactivity (clicking one does nothing) and no relationship to the
 * keyframes underneath them; they're a per-Character annotation
 * ({@code Character.blocks}).
 *
 * <p>The class is still named {@code Block} for historical reasons (it
 * replaced an earlier interactive "Block" grouping feature); {@code name} is
 * the label text and {@code startTick}/{@code endTick} are the From/To range.
 */
@Getter
@Setter
public class Block
{
    /** Label text shown on the marker. */
    private String name;
    /** Packed RGB int (Gson-friendly, no awt.Color save adapter required). */
    private int colorRgb;
    /** "From" tick -- inclusive left bound of the label's range. */
    private double startTick;
    /** "To" tick -- inclusive right bound of the label's range. */
    private double endTick;

    public Block()
    {
        // Default constructor for Gson.
    }

    public Block(String name, int colorRgb, double startTick, double endTick)
    {
        this.name = name;
        this.colorRgb = colorRgb;
        this.startTick = startTick;
        this.endTick = endTick;
    }
}
