package com.creatorskit.cache.parser;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.cache.ItemManager;
import net.runelite.cache.NpcManager;
import net.runelite.cache.ObjectManager;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.definitions.SpotAnimDefinition;
import net.runelite.cache.definitions.loaders.SpotAnimLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;
import net.runelite.client.RuneLite;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One-shot reader for the user's local OSRS cache
 * ({@code ~/.runelite/jagexcache/oldschool/LIVE/}). Drives RuneLite's
 * own {@code net.runelite:cache} module to parse definitions for NPCs,
 * Objects, Items, and SpotAnims directly from
 * {@code main_file_cache.dat2} -- bypassing the
 * {@code cache-converter} JSON dumps that cap at the named-range
 * boundary (26 Feb 2025). The local cache always reflects whatever
 * Jagex shipped this week, including unnamed entries past that
 * boundary that still carry valid model IDs.
 *
 * <p>{@link #load()} is synchronous and blocks ~500ms-2s on a cold
 * disk; callers should run it on a background thread. Idempotent --
 * calling twice is a no-op after the first successful load.
 *
 * <p>If the cache directory isn't present (RuneLite not run yet, or
 * non-standard install) all four collections stay empty and
 * {@link #isLoaded()} returns false. Callers fall back to the URL
 * dumps in that case.
 */
@Slf4j
@Singleton
public class CacheLoader
{
    /** Default OSRS cache location -- mirrors what RuneLite's downloader writes to. */
    public static final File DEFAULT_CACHE_DIR = new File(RuneLite.RUNELITE_DIR, "jagexcache/oldschool/LIVE");

    @Getter
    private boolean loaded = false;
    @Getter
    private boolean loadFailed = false;
    @Getter
    private String loadError;

    /** id -> definition. Maps preserve insertion order? No, use HashMap and let callers sort. */
    @Getter
    private final Map<Integer, NpcDefinition> npcs = new HashMap<>();
    @Getter
    private final Map<Integer, ObjectDefinition> objects = new HashMap<>();
    @Getter
    private final Map<Integer, ItemDefinition> items = new HashMap<>();
    @Getter
    private final Map<Integer, SpotAnimDefinition> spotAnims = new HashMap<>();

    @Inject
    public CacheLoader()
    {
        // No work in ctor -- caller decides when (and on which thread) to
        // call load(). DataFinder kicks it off from its async pool.
    }

    /**
     * Opens the cache dir, runs every manager + spotanim loader, and
     * fills the four maps. Sets {@link #loaded} on success or
     * {@link #loadFailed} on any IO error. Subsequent calls return
     * immediately.
     */
    public synchronized void load()
    {
        load(DEFAULT_CACHE_DIR);
    }

    public synchronized void load(File cacheDir)
    {
        if (loaded || loadFailed) return;
        if (!cacheDir.exists() || !cacheDir.isDirectory())
        {
            loadFailed = true;
            loadError = "Cache directory not found: " + cacheDir.getAbsolutePath();
            log.debug(loadError);
            return;
        }

        long t0 = System.currentTimeMillis();
        try (Store store = new Store(cacheDir))
        {
            store.load();

            // NPCs / Objects / Items each have a dedicated manager that
            // walks the relevant ConfigType archive and parses every file.
            NpcManager npcMgr = new NpcManager(store);
            npcMgr.load();
            for (NpcDefinition d : npcMgr.getNpcs()) npcs.put(d.id, d);

            ObjectManager objMgr = new ObjectManager(store);
            objMgr.load();
            for (ObjectDefinition d : objMgr.getObjects()) objects.put(d.getId(), d);

            ItemManager itemMgr = new ItemManager(store);
            itemMgr.load();
            for (ItemDefinition d : itemMgr.getItems()) items.put(d.id, d);

            // SpotAnims don't have a Manager class; parse the archive
            // manually with SpotAnimLoader (same pattern NpcManager.load
            // uses internally).
            Storage storage = store.getStorage();
            Index configIdx = store.getIndex(IndexType.CONFIGS);
            Archive spotArchive = configIdx.getArchive(ConfigType.SPOTANIM.getId());
            if (spotArchive != null)
            {
                byte[] archiveData = storage.loadArchive(spotArchive);
                ArchiveFiles spotFiles = spotArchive.getFiles(archiveData);
                SpotAnimLoader spotLoader = new SpotAnimLoader();
                for (FSFile f : spotFiles.getFiles())
                {
                    SpotAnimDefinition def = spotLoader.load(f.getFileId(), f.getContents());
                    spotAnims.put(def.id, def);
                }
            }

            loaded = true;
            log.debug("CacheLoader: parsed {} npcs / {} objs / {} items / {} spotanims in {}ms",
                    npcs.size(), objects.size(), items.size(), spotAnims.size(),
                    System.currentTimeMillis() - t0);
        }
        catch (Throwable t)
        {
            loadFailed = true;
            loadError = t.toString();
            log.warn("CacheLoader: failed to parse cache from {} -- falling back to URL dumps", cacheDir, t);
        }
    }

    /** Returns the highest id present in the npcs map, or -1 when empty. */
    public synchronized int getMaxNpcId() { return maxKey(npcs); }
    public synchronized int getMaxObjectId() { return maxKey(objects); }
    public synchronized int getMaxItemId() { return maxKey(items); }
    public synchronized int getMaxSpotAnimId() { return maxKey(spotAnims); }

    private static int maxKey(Map<Integer, ?> m)
    {
        int max = -1;
        for (Integer k : m.keySet()) if (k != null && k > max) max = k;
        return max;
    }

    /** Returns a sorted (by id ascending) snapshot of all NPC definitions. */
    public synchronized List<NpcDefinition> getNpcDefinitionsSorted()
    {
        List<NpcDefinition> out = new ArrayList<>(npcs.values());
        out.sort((a, b) -> Integer.compare(a.id, b.id));
        return out;
    }

    public synchronized List<ObjectDefinition> getObjectDefinitionsSorted()
    {
        List<ObjectDefinition> out = new ArrayList<>(objects.values());
        out.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        return out;
    }

    public synchronized List<ItemDefinition> getItemDefinitionsSorted()
    {
        List<ItemDefinition> out = new ArrayList<>(items.values());
        out.sort((a, b) -> Integer.compare(a.id, b.id));
        return out;
    }

    public synchronized List<SpotAnimDefinition> getSpotAnimDefinitionsSorted()
    {
        List<SpotAnimDefinition> out = new ArrayList<>(spotAnims.values());
        out.sort((a, b) -> Integer.compare(a.id, b.id));
        return out;
    }
}
