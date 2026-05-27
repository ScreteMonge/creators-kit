package com.creatorskit.swing.searchabletable;

import com.creatorskit.cache.metadata.CacheType;
import com.creatorskit.cache.parser.CacheDateBuckets;
import com.creatorskit.models.datatypes.AnimData;
import com.creatorskit.models.datatypes.ItemData;
import com.creatorskit.models.datatypes.NPCData;
import com.creatorskit.models.datatypes.ObjectData;
import com.creatorskit.models.datatypes.SoundData;
import com.creatorskit.models.datatypes.SpotanimData;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filterable single-column or two-column table for the cache
 * searchers + the model-id breakdown.
 *
 * <p><b>Legacy mode</b> (default ctor, {@code cacheType == null}):
 * one column, rows are {@code Object[2]} {@code [item, searchTerm]}.
 * Used by the model-id breakdown table.
 *
 * <p><b>Cache mode</b> (ctor with {@link CacheType}): two columns
 * (Name + Date Added). Rows are {@link CacheRow}s carrying
 * precomputed year bucket + hasModels flag. Header click sorts;
 * empty-model rows render dimmed by {@link JFilterableRenderer}.
 */
public class JFilterableTable extends JTable
{
    private final CacheType cacheType;
    /** In cache mode this is List&lt;CacheRow&gt;; in legacy mode it stays as the original input list. */
    private List<Object> itemBackup;

    public JFilterableTable(String name)
    {
        this(name, null);
    }

    public JFilterableTable(String name, CacheType cacheType)
    {
        super();
        this.setName(name);
        this.cacheType = cacheType;
        this.setDefaultRenderer(Object.class, new JFilterableRenderer());

        if (cacheType != null)
        {
            // Cache mode: show the column headers + wire sortable col 0
            // (Name) + col 1 (Date bucket index). Without the explicit
            // header construct the table renders headerless.
            this.setTableHeader(new JTableHeader(this.getColumnModel()));
            setModel(new DataTableModel(new Object[0], true));
            installRowSorter();
        }
        else
        {
            // Legacy: no headers, single column.
            this.setTableHeader(new JTableHeader());
            setModel(new DataTableModel(new Object[0]));
        }
    }

    /**
     * Initialises with a list of cache data objects (NPCData /
     * ObjectData / etc.). In cache mode each item is wrapped in a
     * {@link CacheRow} carrying precomputed presentation fields. In
     * legacy mode the list is stored as-is.
     */
    public void initialize(List<Object> list)
    {
        if (cacheType != null)
        {
            List<Object> rows = new ArrayList<>(list.size());
            for (Object item : list)
            {
                int id = idOf(item);
                String label = CacheDateBuckets.yearFor(cacheType, id);
                int sortKey = CacheDateBuckets.sortKeyFor(cacheType, id);
                rows.add(new CacheRow(item, label, sortKey, hasModelsFor(item)));
            }
            this.itemBackup = rows;
        }
        else
        {
            this.itemBackup = list;
        }
        // Push an empty-search initial view so the table isn't blank.
        searchAndListEntries("");
    }

    public void searchAndListEntries(Object searchFor)
    {
        if (this.itemBackup == null) return;

        if (cacheType != null)
        {
            searchAndListCacheMode(searchFor);
        }
        else
        {
            searchAndListLegacy(searchFor);
        }
    }

    /**
     * Cache-mode filter pass. Walks {@link #itemBackup} (which holds
     * {@link CacheRow}s in this mode), keeps rows whose item.toString
     * contains the search substring, and rebuilds the model with the
     * matched rows + their search term so the renderer can do its
     * highlight pass.
     */
    private void searchAndListCacheMode(Object searchFor)
    {
        String search = searchFor instanceof String ? (String) searchFor : "";
        List<Object> found = new ArrayList<>();
        for (Object o : this.itemBackup)
        {
            if (!(o instanceof CacheRow)) continue;
            CacheRow row = (CacheRow) o;
            if (search.isEmpty()
                    || row.toString().toLowerCase().matches("(?i).*" + Pattern.quote(search.toLowerCase()) + ".*"))
            {
                row.setSearchTerm(search);
                found.add(row);
            }
        }
        setModel(new DataTableModel(found.toArray(), true));
        installRowSorter();
    }

    /** Legacy single-column path: matches the pre-refactor behaviour exactly. */
    private void searchAndListLegacy(Object searchFor)
    {
        List<Object> found = new ArrayList<>();
        for (int i = 0; i < this.itemBackup.size(); i++)
        {
            Object tmp = this.itemBackup.get(i);
            if (tmp == null || searchFor == null) continue;
            String s = tmp.toString();
            if (searchFor instanceof String)
            {
                String search = (String) searchFor;
                if (s.matches("(?i).*" + Pattern.quote(search) + ".*"))
                {
                    found.add(new Object[]{tmp, searchFor});
                }
            }
        }
        setModel(new DataTableModel(found.toArray()));
    }

    /**
     * Wires a sort comparator for the Date Added column so header
     * clicks order rows by bucket index (oldest -&gt; newest)
     * regardless of the year label string. Name column uses default
     * string sort.
     *
     * <p>RowSorter's comparator gets passed cell VALUES (not row
     * indices), so we can't read each row's {@code sortKey}
     * directly. Workaround: build a one-shot label -&gt; bucketIndex
     * lookup from the current model rows, then compare by that
     * index. Labels are unique per bucket so the lookup is
     * collision-free.
     */
    private void installRowSorter()
    {
        DataTableModel m = (DataTableModel) getModel();
        TableRowSorter<DataTableModel> sorter = new TableRowSorter<>(m);
        sorter.setComparator(1, dateLabelComparator(m));
        sorter.setSortable(0, true);
        sorter.setSortable(1, true);
        setRowSorter(sorter);
    }

    /** See {@link #installRowSorter} for the label-index trick. */
    private static Comparator<Object> dateLabelComparator(DataTableModel model)
    {
        java.util.Map<String, Integer> labelIndex = new java.util.HashMap<>();
        for (int r = 0; r < model.getRowCount(); r++)
        {
            String l = String.valueOf(model.getValueAt(r, 1));
            Integer k = model.getDateSortKey(r);
            labelIndex.putIfAbsent(l, k);
        }
        return (a, b) ->
        {
            Integer ai = labelIndex.getOrDefault(String.valueOf(a), 0);
            Integer bi = labelIndex.getOrDefault(String.valueOf(b), 0);
            return Integer.compare(ai, bi);
        };
    }

    public Object getSelectedObject()
    {
        int viewRow = getSelectedRow();
        if (viewRow == -1) return null;
        int modelRow = (getRowSorter() != null) ? getRowSorter().convertRowIndexToModel(viewRow) : viewRow;
        Object raw = this.getModel().getValueAt(modelRow, 0);
        if (raw instanceof CacheRow) return ((CacheRow) raw).getItem();
        if (raw instanceof Object[]) return ((Object[]) raw)[0];
        return raw;
    }

    // ----- Per-type "does this entry render a model" check -----

    private static int idOf(Object item)
    {
        if (item instanceof NPCData) return ((NPCData) item).getId();
        if (item instanceof ObjectData) return ((ObjectData) item).getId();
        if (item instanceof ItemData) return ((ItemData) item).getId();
        if (item instanceof SpotanimData) return ((SpotanimData) item).getId();
        if (item instanceof AnimData) return ((AnimData) item).getId();
        if (item instanceof SoundData) return ((SoundData) item).getId();
        return 0;
    }

    /**
     * Returns true when the cache entry will actually render something
     * if the user picks it. Per type:
     * <ul>
     *   <li>NPC: at least one non-(-1) model id.</li>
     *   <li>Object: at least one non-(-1) objectModel id.</li>
     *   <li>Item: inventoryModel or any wear model is &gt; 0.</li>
     *   <li>SpotAnim: modelId &gt; 0.</li>
     *   <li>Anim / Sound: no model concept -- always true.</li>
     * </ul>
     */
    private static boolean hasModelsFor(Object item)
    {
        if (item instanceof NPCData)
        {
            int[] m = ((NPCData) item).getModels();
            return m != null && containsNonNegative(m);
        }
        if (item instanceof ObjectData)
        {
            int[] m = ((ObjectData) item).getObjectModels();
            return m != null && containsNonNegative(m);
        }
        if (item instanceof ItemData)
        {
            ItemData d = (ItemData) item;
            return d.getInventoryModel() > 0
                    || d.getMaleModel0() > 0 || d.getMaleModel1() > 0 || d.getMaleModel2() > 0
                    || d.getFemaleModel0() > 0 || d.getFemaleModel1() > 0 || d.getFemaleModel2() > 0;
        }
        if (item instanceof SpotanimData)
        {
            return ((SpotanimData) item).getModelId() > 0;
        }
        return true;  // Anim / Sound: no model concept, treat as valid
    }

    private static boolean containsNonNegative(int[] arr)
    {
        for (int v : arr) if (v > 0) return true;
        return false;
    }
}
