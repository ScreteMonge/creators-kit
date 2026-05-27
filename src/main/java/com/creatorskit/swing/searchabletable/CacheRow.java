package com.creatorskit.swing.searchabletable;

import lombok.Getter;

/**
 * Row holder for the cache-searcher tables. Wraps the underlying
 * data object (NPCData / ObjectData / etc.) with precomputed
 * presentation fields:
 *
 * <ul>
 *   <li>{@code dateLabel} -- coarse year-bucket text shown in the
 *       Date Added column.</li>
 *   <li>{@code sortKey} -- bucket INDEX so the sortable column orders
 *       oldest-to-newest stably across rows that share the same
 *       year label.</li>
 *   <li>{@code hasModels} -- false when the entry's model array is
 *       null or empty (some cache definitions ship without models --
 *       map markers, area placeholders, etc.). Renderer dims these
 *       rows so the user can spot which Unnamed entries are usable.</li>
 * </ul>
 *
 * <p>The {@code searchTerm} is set per-search by the filter pass,
 * not at construction -- the row instance survives across many
 * searches.
 */
@Getter
public final class CacheRow
{
    private final Object item;
    private final String dateLabel;
    private final int sortKey;
    private final boolean hasModels;
    private String searchTerm = "";

    public CacheRow(Object item, String dateLabel, int sortKey, boolean hasModels)
    {
        this.item = item;
        this.dateLabel = dateLabel;
        this.sortKey = sortKey;
        this.hasModels = hasModels;
    }

    public void setSearchTerm(String s) { this.searchTerm = s == null ? "" : s; }

    @Override
    public String toString()
    {
        // Used by the filter pass for name-match -- delegates to the
        // underlying item's toString so existing filter logic ("NPC
        // name (id)" substring) works unchanged.
        return item.toString();
    }
}
