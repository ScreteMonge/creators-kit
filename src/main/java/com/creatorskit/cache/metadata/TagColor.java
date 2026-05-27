package com.creatorskit.cache.metadata;

import java.awt.Color;

/**
 * Six fixed swatches the Tag Manager exposes. Picked to be distinct
 * at small sizes (the colored bullet glyph rendered next to tagged
 * cache entries) and reasonably accessible on the dark theme. Stored
 * as the enum's {@code name()} when serialised so future palette
 * tweaks (different hex values) don't break saved tag definitions.
 *
 * <p>To add a 7th colour, append at the end and bump the swatch grid
 * in the Tag Manager panel. To replace a colour, swap its hex --
 * tags pre-existing in the metadata file stay bound to the same enum
 * value and just render with the new hex.
 */
public enum TagColor
{
    RED("#E74C3C"),
    ORANGE("#E67E22"),
    YELLOW("#F1C40F"),
    GREEN("#27AE60"),
    BLUE("#3498DB"),
    PURPLE("#9B59B6");

    public final String hex;
    public final Color awt;

    TagColor(String hex)
    {
        this.hex = hex;
        this.awt = Color.decode(hex);
    }
}
