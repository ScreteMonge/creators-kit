package com.creatorskit.swing.timesheet.keyframe;

import java.awt.Color;

/**
 * Fixed 12-colour palette the block creation / recolour dialog presents to
 * the user. No RGB / HSV / hex pickers per the spec -- just a swatch grid.
 * Indexes are stable, so future code can reference colours by ordinal if a
 * "next colour" rotation is ever added.
 */
public final class BlockPalette
{
    private BlockPalette() {}

    /**
     * 4x3 grid layout source. Reasonably distinct hues, mid-saturation so
     * the white name text overlay reads on every swatch.
     */
    public static final int[] COLOURS = new int[]
    {
            0xE53935, // red
            0xFB8C00, // orange
            0xFDD835, // yellow
            0xC0CA33, // lime
            0x43A047, // green
            0x00897B, // teal
            0x00ACC1, // cyan
            0x1E88E5, // blue
            0x3949AB, // indigo
            0x8E24AA, // purple
            0xD81B60, // magenta
            0xEC407A, // pink
    };

    /** Fallback when an old / missing colour value is read from a save. */
    public static final int DEFAULT_RGB = COLOURS[7]; // blue

    public static Color asColor(int rgb)
    {
        return new Color(rgb);
    }
}
