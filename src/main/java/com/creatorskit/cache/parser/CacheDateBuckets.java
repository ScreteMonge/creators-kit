package com.creatorskit.cache.parser;

import com.creatorskit.cache.metadata.CacheType;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coarse "approximate year added" lookup for cache entries. Each
 * {@link CacheType} carries a list of {@code (idThreshold, yearLabel)}
 * anchors sorted by ascending id. {@link #yearFor(CacheType, int)}
 * picks the first anchor whose threshold is &gt;= the queried id, so
 * any id that's beyond all anchors falls into the most-recent year.
 *
 * <p>Anchors are calibrated against well-known OSRS content drops
 * (Vorkath = NPC ~8060 in 2018, Akkha/TOA = NPC ~11772 in 2022,
 * etc.) rather than a per-id revision diff. Resolution is roughly
 * one-year buckets; an entry tagged "2022" might have actually been
 * added late 2021 or early 2023. Good enough to spot recent vs
 * legacy content; not good enough for "exactly when did Jagex ship
 * this NPC" precision.
 *
 * <p>Bump the anchor lists when new game updates push the live max
 * id past the last entry. The {@link #sortKeyFor} method returns the
 * anchor's bucket INDEX (not the year string) so column sorting on
 * the cache searcher is stable across years that share a label.
 */
public final class CacheDateBuckets
{
    private CacheDateBuckets() {}

    /**
     * Anchor entry: every id LESS THAN {@link #idMax} (and greater
     * than the previous anchor's idMax) falls into this bucket.
     */
    public static final class Anchor
    {
        public final int idMax;
        public final String yearLabel;
        public Anchor(int idMax, String yearLabel) { this.idMax = idMax; this.yearLabel = yearLabel; }
    }

    /** Per-type anchor tables. Iterate in order; first idMax &gt;= id wins. */
    private static final Map<CacheType, List<Anchor>> ANCHORS = buildAnchorTable();

    private static Map<CacheType, List<Anchor>> buildAnchorTable()
    {
        Map<CacheType, List<Anchor>> m = new LinkedHashMap<>();

        // NPCs: ~1000 ids/year cadence in modern OSRS. Anchor points
        // from major content drops:
        //   Vorkath          (2018-07)  NPC ~8060
        //   Theatre of Blood (2018-10)  NPC ~8359
        //   Nightmare        (2020-01)  NPC ~9415
        //   Trailblazer L1   (2020-08)  NPC ~9700
        //   TOA              (2022-08)  NPC ~11724
        //   DT2              (2023-07)  NPC ~12200
        //   Varlamore P1     (2024-01)  NPC ~13000
        //   Varlamore P2     (2024-07)  NPC ~13800
        //   Sailing prep     (2025+)    NPC ~14500+
        m.put(CacheType.NPC, Arrays.asList(
                new Anchor(2000,  "Legacy (RS2)"),
                new Anchor(4000,  "2013-2014"),
                new Anchor(6000,  "2015-2016"),
                new Anchor(8000,  "2017-2018"),
                new Anchor(10000, "2019-2020"),
                new Anchor(12000, "2021-2022"),
                new Anchor(13500, "2023"),
                new Anchor(14500, "2024"),
                new Anchor(Integer.MAX_VALUE, "2025+")
        ));

        // Items: ~2000 ids/year (higher cadence than NPCs; lots of
        // cosmetic / quest items per update).
        m.put(CacheType.ITEM, Arrays.asList(
                new Anchor(5000,  "Legacy (RS2)"),
                new Anchor(10000, "2013-2016"),
                new Anchor(15000, "2017-2018"),
                new Anchor(20000, "2019-2020"),
                new Anchor(25000, "2021-2022"),
                new Anchor(28000, "2023"),
                new Anchor(30000, "2024"),
                new Anchor(Integer.MAX_VALUE, "2025+")
        ));

        // Objects: ~3000-5000 ids/year (huge content drops add lots of
        // scenery / decorative props).
        m.put(CacheType.OBJECT, Arrays.asList(
                new Anchor(10000, "Legacy (RS2)"),
                new Anchor(25000, "2013-2017"),
                new Anchor(35000, "2018-2020"),
                new Anchor(45000, "2021-2022"),
                new Anchor(52000, "2023"),
                new Anchor(58000, "2024"),
                new Anchor(Integer.MAX_VALUE, "2025+")
        ));

        // SpotAnims: very slow cadence (~100-200 ids/year).
        m.put(CacheType.SPOTANIM, Arrays.asList(
                new Anchor(1000, "Legacy (RS2)"),
                new Anchor(1700, "2013-2018"),
                new Anchor(2200, "2019-2021"),
                new Anchor(2700, "2022-2023"),
                new Anchor(3100, "2024"),
                new Anchor(Integer.MAX_VALUE, "2025+")
        ));

        // Anims: ~800-1000 ids/year.
        m.put(CacheType.ANIM, Arrays.asList(
                new Anchor(5000,  "Legacy (RS2)"),
                new Anchor(7000,  "2013-2017"),
                new Anchor(9000,  "2018-2020"),
                new Anchor(11000, "2021-2022"),
                new Anchor(12500, "2023-2024"),
                new Anchor(Integer.MAX_VALUE, "2025+")
        ));

        // Sounds: named ids stop at 10200 (26 Feb 2025 leak boundary).
        // Past that they're unnamed but live in the cache.
        m.put(CacheType.SOUND, Arrays.asList(
                new Anchor(3500,  "Legacy (RS2)"),
                new Anchor(5500,  "2013-2017"),
                new Anchor(7500,  "2018-2020"),
                new Anchor(9000,  "2021-2022"),
                new Anchor(10000, "2023"),
                new Anchor(10200, "2024-early"),
                new Anchor(Integer.MAX_VALUE, "2024+ (unnamed)")
        ));

        return Collections.unmodifiableMap(m);
    }

    /**
     * Returns the year label for the cache entry. Returns "?" when no
     * anchors exist for the type (shouldn't happen given the table
     * above is complete, but guards against future type additions).
     */
    public static String yearFor(CacheType type, int id)
    {
        List<Anchor> list = ANCHORS.get(type);
        if (list == null) return "?";
        for (Anchor a : list) if (id < a.idMax) return a.yearLabel;
        return list.get(list.size() - 1).yearLabel;
    }

    /**
     * Returns the bucket INDEX so the cache searcher's date column
     * sorts stably even within a shared year label (e.g. two "2022"
     * entries sort by id within the bucket). Higher index = newer.
     */
    public static int sortKeyFor(CacheType type, int id)
    {
        List<Anchor> list = ANCHORS.get(type);
        if (list == null) return 0;
        for (int i = 0; i < list.size(); i++) if (id < list.get(i).idMax) return i;
        return list.size() - 1;
    }
}
