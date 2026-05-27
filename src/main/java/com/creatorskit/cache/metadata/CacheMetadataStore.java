package com.creatorskit.cache.metadata;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Singleton store for user-authored cache metadata: renames, tags,
 * and tag assignments. Backed by
 * {@code ~/.runelite/creatorskit/cache-metadata.json}.
 *
 * <p><b>Tags</b> are stored as a flat list to preserve creation order
 * (the Tag Manager dropdown surfaces the latest-created tag in its
 * collapsed state, then sorts alphabetically when expanded).
 *
 * <p><b>Renames</b> map {@code "TYPE:id"} composite keys to the
 * user's custom name. Renamed entries render in italic yellow in the
 * Cache Searcher; the original name is still searchable.
 *
 * <p><b>Assignments</b> map {@code "TYPE:id"} composite keys to a
 * Set of tag names. A set per entry (vs. a list) prevents duplicate
 * tag entries when the user re-applies a tag they've already set.
 *
 * <p>Thread-safety: every mutation method is {@code synchronized}.
 * The store is read from the EDT (Cache Searcher renderers) and
 * written from the EDT (right-click menus + Tag Manager panel) so
 * contention is minimal -- the synchronisation just guards against
 * the load-on-construction running concurrently with a paint pass on
 * a slow disk.
 *
 * <p>Listeners are notified after every mutation so the UI can repaint
 * without each panel having to subscribe to every individual change
 * type.
 */
@Slf4j
@Singleton
public class CacheMetadataStore
{
    private static final File DATA_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit");
    private static final File DATA_FILE = new File(Paths.get(DATA_DIR.getPath(), "cache-metadata.json").toString());

    /** Max length of a tag name as enforced by addTag. Mirror in UI input field. */
    public static final int MAX_TAG_NAME_LENGTH = 8;

    private final Gson gson = new Gson();
    /** Preserves creation order; used by the Tag Manager's "latest-created" field. */
    private final List<Tag> tags = new ArrayList<>();
    /** "TYPE:id" -> custom name */
    private final Map<String, String> renames = new HashMap<>();
    /** "TYPE:id" -> set of tag names */
    private final Map<String, Set<String>> assignments = new HashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    @Inject
    public CacheMetadataStore()
    {
        load();
    }

    // ---------- composite key helpers ----------

    /** Composite "TYPE:id" key used by renames + assignments maps. */
    public static String key(CacheType type, int id)
    {
        return type.name() + ":" + id;
    }

    // ---------- tags ----------

    public synchronized List<Tag> getTags()
    {
        return new ArrayList<>(tags);
    }

    /**
     * Returns tags sorted alphabetically by name. Used by the Tag
     * Manager dropdown when expanded (creation-order is shown in the
     * collapsed main field by reading {@link #getLatestTag()}).
     */
    public synchronized List<Tag> getTagsAlphabetical()
    {
        List<Tag> out = new ArrayList<>(tags);
        out.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return out;
    }

    /** Most recently created tag, or null if no tags exist. */
    public synchronized Tag getLatestTag()
    {
        return tags.isEmpty() ? null : tags.get(tags.size() - 1);
    }

    public synchronized Tag findTag(String name)
    {
        for (Tag t : tags) if (t.getName().equals(name)) return t;
        return null;
    }

    /**
     * Adds a tag. Names are truncated to {@link #MAX_TAG_NAME_LENGTH}
     * and trimmed of surrounding whitespace; empty names are rejected.
     * Duplicate names (case-sensitive) are also rejected.
     * @return the created tag, or null if the input was rejected.
     */
    public synchronized Tag addTag(String name, TagColor color)
    {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > MAX_TAG_NAME_LENGTH) trimmed = trimmed.substring(0, MAX_TAG_NAME_LENGTH);
        if (findTag(trimmed) != null) return null;
        Tag tag = new Tag(trimmed, color);
        tags.add(tag);
        save();
        fire();
        return tag;
    }

    /**
     * Deletes a tag by name. Also removes the tag from every entry's
     * assignments so the JSON file doesn't accumulate dangling
     * references to deleted tag names.
     */
    public synchronized void deleteTag(String name)
    {
        if (name == null) return;
        Tag tag = findTag(name);
        if (tag == null) return;
        tags.remove(tag);
        // Strip the tag name from every entry's assignment set.
        for (Set<String> set : assignments.values()) set.remove(name);
        // Drop any now-empty assignment entries entirely.
        assignments.values().removeIf(Set::isEmpty);
        save();
        fire();
    }

    // ---------- renames ----------

    public synchronized String getRename(CacheType type, int id)
    {
        return renames.get(key(type, id));
    }

    /**
     * Sets a custom display name. Empty / null name clears the rename
     * (entry reverts to its cache-default name). The cache searcher's
     * renderer paints renamed entries in italic yellow.
     */
    public synchronized void rename(CacheType type, int id, String newName)
    {
        String k = key(type, id);
        if (newName == null || newName.trim().isEmpty())
        {
            if (renames.remove(k) != null) { save(); fire(); }
            return;
        }
        renames.put(k, newName.trim());
        save();
        fire();
    }

    // ---------- assignments ----------

    public synchronized Set<String> getTagsFor(CacheType type, int id)
    {
        Set<String> set = assignments.get(key(type, id));
        return set == null ? Collections.emptySet() : new LinkedHashSet<>(set);
    }

    /**
     * Returns tags applied to {@code (type, id)} as resolved {@link Tag}
     * objects, in the order they were added. Tag names that no longer
     * exist (because the user deleted them) are dropped on read; the
     * underlying assignment set isn't mutated here so the saved file
     * keeps the historical reference until the next explicit edit
     * (matches the dangling-reference cleanup deleteTag also performs).
     */
    public synchronized List<Tag> resolveTagsFor(CacheType type, int id)
    {
        Set<String> names = getTagsFor(type, id);
        if (names.isEmpty()) return Collections.emptyList();
        List<Tag> out = new ArrayList<>(names.size());
        for (String n : names)
        {
            Tag t = findTag(n);
            if (t != null) out.add(t);
        }
        return out;
    }

    public synchronized void addTagToEntry(CacheType type, int id, String tagName)
    {
        if (findTag(tagName) == null) return;
        String k = key(type, id);
        Set<String> set = assignments.computeIfAbsent(k, kk -> new LinkedHashSet<>());
        if (set.add(tagName)) { save(); fire(); }
    }

    public synchronized void removeTagFromEntry(CacheType type, int id, String tagName)
    {
        String k = key(type, id);
        Set<String> set = assignments.get(k);
        if (set == null) return;
        if (set.remove(tagName))
        {
            if (set.isEmpty()) assignments.remove(k);
            save();
            fire();
        }
    }

    /**
     * Returns every unique tag name currently in use across all entries.
     * Used by the "Remove Tag(s)" right-click dropdown to populate only
     * tags that are actually applied to at least one entry.
     */
    public synchronized Set<String> getAllAssignedTagNames()
    {
        Set<String> out = new HashSet<>();
        for (Set<String> set : assignments.values()) out.addAll(set);
        return out;
    }

    // ---------- change-listener fan-out ----------

    public synchronized void addListener(Runnable l)
    {
        if (l != null) listeners.add(l);
    }

    public synchronized void removeListener(Runnable l)
    {
        listeners.remove(l);
    }

    private void fire()
    {
        // Copy first so a listener can remove itself during dispatch
        // without ConcurrentModificationException.
        List<Runnable> snap = new ArrayList<>(listeners);
        for (Runnable l : snap)
        {
            try { l.run(); }
            catch (Throwable t) { log.warn("Cache-metadata listener threw", t); }
        }
    }

    // ---------- load / save ----------

    private void load()
    {
        if (!DATA_FILE.exists()) return;
        try (FileReader r = new FileReader(DATA_FILE))
        {
            // Use the instance API (older Gson on the RuneLite classpath
            // doesn't ship the static parseReader). Behaviour is identical.
            @SuppressWarnings("deprecation")
            JsonElement root = new JsonParser().parse(r);
            if (!root.isJsonObject()) return;
            JsonObject obj = root.getAsJsonObject();

            // Tags
            if (obj.has("tags") && obj.get("tags").isJsonArray())
            {
                JsonArray arr = obj.getAsJsonArray("tags");
                for (JsonElement el : arr)
                {
                    if (!el.isJsonObject()) continue;
                    JsonObject t = el.getAsJsonObject();
                    String name = t.has("name") ? t.get("name").getAsString() : null;
                    String colorName = t.has("color") ? t.get("color").getAsString() : null;
                    if (name == null || colorName == null) continue;
                    try
                    {
                        TagColor color = TagColor.valueOf(colorName);
                        tags.add(new Tag(name, color));
                    }
                    catch (IllegalArgumentException ignored)
                    {
                        // Unknown colour from a future build -- skip the tag
                        // rather than abort the whole load.
                    }
                }
            }

            // Renames
            if (obj.has("renames") && obj.get("renames").isJsonObject())
            {
                JsonObject rmap = obj.getAsJsonObject("renames");
                for (Map.Entry<String, JsonElement> e : rmap.entrySet())
                {
                    if (!e.getValue().isJsonPrimitive()) continue;
                    renames.put(e.getKey(), e.getValue().getAsString());
                }
            }

            // Assignments
            if (obj.has("assignments") && obj.get("assignments").isJsonObject())
            {
                JsonObject amap = obj.getAsJsonObject("assignments");
                for (Map.Entry<String, JsonElement> e : amap.entrySet())
                {
                    if (!e.getValue().isJsonArray()) continue;
                    Set<String> set = new LinkedHashSet<>();
                    for (JsonElement v : e.getValue().getAsJsonArray())
                    {
                        if (v.isJsonPrimitive()) set.add(v.getAsString());
                    }
                    if (!set.isEmpty()) assignments.put(e.getKey(), set);
                }
            }
        }
        catch (Exception ex)
        {
            log.warn("Failed to load cache-metadata.json; starting fresh", ex);
        }
    }

    private void save()
    {
        try
        {
            if (!DATA_DIR.exists()) DATA_DIR.mkdirs();

            JsonObject root = new JsonObject();

            JsonArray tagsArr = new JsonArray();
            for (Tag t : tags)
            {
                JsonObject o = new JsonObject();
                o.addProperty("name", t.getName());
                o.addProperty("color", t.getColor().name());
                tagsArr.add(o);
            }
            root.add("tags", tagsArr);

            JsonObject rmap = new JsonObject();
            // Iterate sorted so the file diffs cleanly across versions.
            List<String> rKeys = new ArrayList<>(renames.keySet());
            Collections.sort(rKeys);
            for (String k : rKeys) rmap.addProperty(k, renames.get(k));
            root.add("renames", rmap);

            JsonObject amap = new JsonObject();
            List<String> aKeys = new ArrayList<>(assignments.keySet());
            Collections.sort(aKeys);
            for (String k : aKeys)
            {
                JsonArray arr = new JsonArray();
                for (String name : assignments.get(k)) arr.add(name);
                amap.add(k, arr);
            }
            root.add("assignments", amap);

            try (FileWriter w = new FileWriter(DATA_FILE))
            {
                gson.toJson(root, w);
            }
        }
        catch (Exception ex)
        {
            log.warn("Failed to save cache-metadata.json", ex);
        }
    }
}
