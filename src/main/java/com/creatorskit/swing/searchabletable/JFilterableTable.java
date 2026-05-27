package com.creatorskit.swing.searchabletable;

import com.creatorskit.cache.metadata.CacheMetadataStore;
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
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    /**
     * Metadata store backing the rename / tags. Optional -- set via
     * {@link #setMetadataStore} after construction. When null the
     * right-click menu hides (display-only mode for the mini-searcher
     * popups inside keyframe cards).
     */
    private CacheMetadataStore metadataStore;
    /** The renderer instance so we can pass it the store reference for italic-yellow rendering. */
    private final JFilterableRenderer renderer;

    public CacheType getCacheType() { return cacheType; }

    public JFilterableTable(String name)
    {
        this(name, null);
    }

    public JFilterableTable(String name, CacheType cacheType)
    {
        super();
        this.setName(name);
        this.cacheType = cacheType;
        this.renderer = new JFilterableRenderer();
        this.setDefaultRenderer(Object.class, renderer);

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
     * JTable defers to the underlying model for editability; we want
     * column 0 (Name) to be editable in cache mode when a metadata
     * store is attached. Returning true here doesn't auto-start an
     * editor on click -- {@link #attachRenameEditor} configures the
     * editor with click-count-to-start = MAX_VALUE so only explicit
     * {@code editCellAt(row, 0)} calls (from the Rename context-menu
     * action) fire it.
     */
    @Override
    public boolean isCellEditable(int row, int column)
    {
        return metadataStore != null && cacheType != null && column == 0;
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

    /**
     * Enables the right-click context menu (Rename / Set Tag(s) /
     * Remove Tag(s) / Filter by tag(s)) for this table. Only meaningful
     * in cache mode. The store reference is also handed to the renderer
     * so italic-yellow renames + tag bullets pick up store updates.
     *
     * <p>Mini-searcher popups inside keyframe cards skip this call --
     * they're display-only.
     */
    public void setMetadataStore(CacheMetadataStore store)
    {
        if (cacheType == null) return;
        this.metadataStore = store;
        renderer.setMetadataStore(store, cacheType);
        attachRightClickHandler();
        attachRenameEditor();
        // Repaint whenever the store changes -- catches renames + tag
        // updates initiated from any other panel.
        store.addListener(() -> SwingUtilities.invokeLater(this::repaint));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Re-attaches the rename editor after any model swap (each
     * search rebuilds the model). Without this the editor binding on
     * column 0 vanishes the first time the user filters the table.
     */
    @Override
    public void setModel(javax.swing.table.TableModel dataModel)
    {
        super.setModel(dataModel);
        if (metadataStore != null && cacheType != null) attachRenameEditor();
    }

    /**
     * Adds a mouse listener that opens the row-context popup on
     * right-click. Right-click also "implicitly selects" the row under
     * the cursor (matching typical OS file-manager behaviour), so the
     * user doesn't have to left-click first to target a row.
     */
    private void attachRightClickHandler()
    {
        addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }

            private void maybeShow(MouseEvent e)
            {
                if (!e.isPopupTrigger()) return;
                int viewRow = rowAtPoint(e.getPoint());
                if (viewRow < 0) return;
                // Implicit row selection: if the clicked row isn't part
                // of the existing selection, replace selection with it.
                if (!isRowSelected(viewRow)) setRowSelectionInterval(viewRow, viewRow);
                showContextMenu(e);
            }
        });
    }

    /**
     * Builds + shows the right-click context menu. The Rename action
     * targets only the currently focused row (in-place edit doesn't
     * meaningfully multi-select); Set / Remove tags and Filter apply
     * to the whole selection.
     */
    private void showContextMenu(MouseEvent e)
    {
        if (metadataStore == null || cacheType == null) return;
        JPopupMenu menu = new JPopupMenu();

        JMenuItem rename = new JMenuItem("Rename");
        rename.addActionListener(ev ->
        {
            int viewRow = getSelectedRow();
            if (viewRow < 0) return;
            // editCellAt routes through the cell editor registered on
            // column 0 (attachRenameEditor wires it). Editing finishes
            // on Enter or focus-lost; the editor's stopCellEditing
            // pushes the new name into the metadata store.
            editCellAt(viewRow, 0);
            // Pull keyboard focus into the editor so the user can type
            // without an additional click.
            java.awt.Component ed = getEditorComponent();
            if (ed != null) ed.requestFocusInWindow();
        });
        menu.add(rename);

        // Set Tag(s) / Remove Tag(s) / Filter by tag(s) ship in phase 4b.
        // Stubs are added here so users can see the eventual layout but
        // they immediately close without firing yet.
        JMenuItem setTag = new JMenuItem("Set Tag(s)...");
        setTag.setEnabled(false);
        menu.add(setTag);

        JMenuItem removeTag = new JMenuItem("Remove Tag(s)...");
        removeTag.setEnabled(false);
        menu.add(removeTag);

        menu.addSeparator();
        JMenuItem filter = new JMenuItem("Filter by tag(s)...");
        filter.setEnabled(false);
        menu.add(filter);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Wires a cell editor for column 0 (Name). Cells are otherwise
     * non-editable -- the editor only fires when {@code editCellAt} is
     * called explicitly from the Rename context-menu action. On
     * commit we push the new name to {@link CacheMetadataStore#rename}
     * instead of mutating the table model (the model is rebuilt from
     * the store on the next reload anyway; the renderer reads renames
     * directly from the store).
     */
    private void attachRenameEditor()
    {
        if (cacheType == null) return;
        JTextField field = new JTextField();
        DefaultCellEditor editor = new DefaultCellEditor(field)
        {
            @Override public boolean isCellEditable(java.util.EventObject e)
            {
                // Suppress the default "double-click to edit" path. Only
                // explicit editCellAt() from the Rename menu starts editing.
                return e == null;
            }

            @Override public boolean stopCellEditing()
            {
                String newName = field.getText();
                int viewRow = getEditingRow();
                if (viewRow >= 0 && metadataStore != null)
                {
                    int modelRow = (getRowSorter() != null) ? getRowSorter().convertRowIndexToModel(viewRow) : viewRow;
                    Object raw = getModel().getValueAt(modelRow, 0);
                    if (raw instanceof CacheRow)
                    {
                        Object item = ((CacheRow) raw).getItem();
                        int id = idOf(item);
                        // Empty / unchanged input clears the rename (revert
                        // to the cache-default name). Otherwise stores the
                        // user's custom name.
                        if (newName == null || newName.trim().isEmpty())
                        {
                            metadataStore.rename(cacheType, id, null);
                        }
                        else
                        {
                            metadataStore.rename(cacheType, id, newName);
                        }
                    }
                }
                return super.stopCellEditing();
            }
        };
        // Pre-fill the editor with the current display name (rename if
        // present, otherwise the underlying item's name).
        editor.setClickCountToStart(Integer.MAX_VALUE);  // never start on click
        TableCellEditor wrapped = new TableCellEditor()
        {
            @Override public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
            {
                String current = "";
                if (value instanceof CacheRow)
                {
                    Object item = ((CacheRow) value).getItem();
                    int id = idOf(item);
                    String renamed = metadataStore != null ? metadataStore.getRename(cacheType, id) : null;
                    if (renamed != null) current = renamed;
                    else current = stripIdSuffix(item.toString());
                }
                field.setText(current);
                field.selectAll();
                return field;
            }
            @Override public Object getCellEditorValue() { return editor.getCellEditorValue(); }
            @Override public boolean isCellEditable(java.util.EventObject e) { return editor.isCellEditable(e); }
            @Override public boolean shouldSelectCell(java.util.EventObject e) { return editor.shouldSelectCell(e); }
            @Override public boolean stopCellEditing() { return editor.stopCellEditing(); }
            @Override public void cancelCellEditing() { editor.cancelCellEditing(); }
            @Override public void addCellEditorListener(javax.swing.event.CellEditorListener l) { editor.addCellEditorListener(l); }
            @Override public void removeCellEditorListener(javax.swing.event.CellEditorListener l) { editor.removeCellEditorListener(l); }
        };
        getColumnModel().getColumn(0).setCellEditor(wrapped);
    }

    /**
     * Strips the trailing " (id)" suffix from an XData toString so the
     * rename editor pre-fills with just the name. Falls back to the
     * full string if no " (" is found.
     */
    private static String stripIdSuffix(String s)
    {
        int idx = s.lastIndexOf(" (");
        return idx > 0 ? s.substring(0, idx) : s;
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
