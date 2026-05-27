package com.creatorskit.swing.searchabletable;

import com.creatorskit.cache.metadata.CacheMetadataStore;
import com.creatorskit.cache.metadata.CacheType;
import com.creatorskit.cache.metadata.Tag;
import com.creatorskit.cache.parser.CacheDateBuckets;
import com.creatorskit.models.datatypes.AnimData;
import com.creatorskit.models.datatypes.ItemData;
import com.creatorskit.models.datatypes.NPCData;
import com.creatorskit.models.datatypes.ObjectData;
import com.creatorskit.models.datatypes.SoundData;
import com.creatorskit.models.datatypes.SpotanimData;
import com.creatorskit.swing.tags.TagFilterDialog;
import com.creatorskit.swing.tags.TagPickerPopup;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    /** Names of tags currently used as a row filter, in left-to-right order from the dialog. */
    private final Set<String> activeFilterTags = new LinkedHashSet<>();
    /** AND -> entry must have every active filter tag; OR -> any one of them. */
    private TagFilterDialog.Mode filterMode = TagFilterDialog.Mode.AND;
    /** Listener fired on filter-state changes so the cache panel can update its "(filtered)" title suffix. */
    private Runnable filterChangedListener;
    /** When true, rows whose model array is null / empty are dropped entirely instead of just dimmed. */
    private boolean hideUnavailable = false;

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
        // Make the table draw through the full viewport even when it
        // has zero rows. Without this an empty filtered list collapses
        // to 0px and right-clicks in the visually-empty viewport area
        // go to the scrollpane instead of the table -- so the
        // "right-click empty list to clear filters" path wouldn't fire.
        this.setFillsViewportHeight(true);

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
     * {@link CacheRow}s in this mode), keeps rows that pass BOTH the
     * search-text substring check AND the active-tag filter, then
     * rebuilds the model with the matched rows + their search term
     * so the renderer can do its highlight pass.
     *
     * <p>Tag filter semantics: AND keeps an entry only if it has every
     * active filter tag; OR keeps it if it has any one. Empty filter
     * set (no tags chosen yet) is a pass-through.
     */
    private void searchAndListCacheMode(Object searchFor)
    {
        String search = searchFor instanceof String ? (String) searchFor : "";
        String searchLower = search.toLowerCase();
        List<Object> found = new ArrayList<>();
        for (Object o : this.itemBackup)
        {
            if (!(o instanceof CacheRow)) continue;
            CacheRow row = (CacheRow) o;
            boolean textMatch = search.isEmpty() || rowMatches(row, searchLower);
            if (!textMatch) continue;
            if (!passesTagFilter(row)) continue;
            // Hide-unavailable filter: drop empty-model rows entirely
            // instead of just dimming them. Toggled via the right-click
            // menu item; off by default so users see the full set.
            if (hideUnavailable && !row.isHasModels()) continue;
            row.setSearchTerm(search);
            found.add(row);
        }
        setModel(new DataTableModel(found.toArray(), true));
        installRowSorter();
    }

    /**
     * Substring match against the row's displayed text. Checks both the
     * underlying item.toString (the cache-default "Name (id)") AND the
     * user-supplied rename if one exists -- so searching for a custom
     * name like "Test" finds the entry even when the cache-default
     * would have read "Unnamed (12345)". Tag names aren't matched here
     * intentionally; the tag-filter dialog covers that case explicitly.
     */
    private boolean rowMatches(CacheRow row, String searchLower)
    {
        if (row.toString().toLowerCase().contains(searchLower)) return true;
        if (metadataStore == null || cacheType == null) return false;
        String custom = metadataStore.getRename(cacheType, idOf(row.getItem()));
        return custom != null && custom.toLowerCase().contains(searchLower);
    }

    /**
     * Tag-filter predicate per {@link #filterMode}. Pulls the entry's
     * assigned tags from the store on each call -- cheap enough at
     * searcher-list size, avoids stale data when the user mutates
     * assignments while a filter is active.
     */
    private boolean passesTagFilter(CacheRow row)
    {
        if (activeFilterTags.isEmpty()) return true;
        if (metadataStore == null || cacheType == null) return true;
        Set<String> rowTags = metadataStore.getTagsFor(cacheType, idOf(row.getItem()));
        if (filterMode == TagFilterDialog.Mode.AND)
        {
            return rowTags.containsAll(activeFilterTags);
        }
        // OR
        for (String f : activeFilterTags) if (rowTags.contains(f)) return true;
        return false;
    }

    /** True when the cache panel's title should append "(filtered)". */
    public boolean isFilterActive() { return !activeFilterTags.isEmpty(); }

    /** Lets the cache panel listen for filter-state changes so the title can be updated. */
    public void setFilterChangedListener(Runnable listener) { this.filterChangedListener = listener; }

    /**
     * Drops every active tag filter and re-runs the search pass.
     * Called from the title-row "Clear filters" button + the right-click
     * menu's "Clear filters" item. No-op when no filter is active.
     */
    public void clearFilters()
    {
        if (activeFilterTags.isEmpty()) return;
        activeFilterTags.clear();
        searchAndListEntries("");
        if (filterChangedListener != null) filterChangedListener.run();
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
     * Display-only variant for the mini-searcher popups inside
     * keyframe cards. Renames render in italic yellow, tag bullets
     * paint after the name, but right-click does nothing -- per the
     * spec, the mini popups don't host rename / tag / filter actions
     * (those live exclusively in the main Cache Searcher tab).
     */
    public void setMetadataStoreDisplayOnly(CacheMetadataStore store)
    {
        if (cacheType == null) return;
        renderer.setMetadataStore(store, cacheType);
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
                if (viewRow >= 0)
                {
                    // Implicit row selection: clicking a row that isn't
                    // already part of the selection replaces selection with
                    // it. Matches OS file-manager conventions.
                    if (!isRowSelected(viewRow)) setRowSelectionInterval(viewRow, viewRow);
                }
                else
                {
                    // Empty-area right-click. Clear any prior selection so
                    // Rename / Set Tag / Remove Tag correctly grey out --
                    // they need a target row to operate on. Filter / Hide /
                    // Clear filters still apply, which is the main reason
                    // to expose this menu on empty space (e.g. clearing an
                    // active tag filter that left zero matching rows).
                    clearSelection();
                }
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
        // Rename only meaningful when a single row is selected -- the
        // editor binds to one cell at a time. Empty-area right-click
        // greys this out (we cleared selection in attachRightClickHandler).
        rename.setEnabled(getSelectedRow() >= 0);
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

        // Set / Remove tag use the persistent TagPickerPopup -- not a
        // JPopupMenu, because the popup needs to stay open while the
        // user clicks multiple tag rows. Pre-compute the union of tags
        // already assigned across the selection so the checkmarks read
        // correctly even when the user right-clicked multiple rows.
        List<Integer> selectedIds = getSelectedIds();
        Set<String> unionAssigned = unionAssignedFor(selectedIds);

        JMenuItem setTag = new JMenuItem("Set Tag(s)...");
        setTag.setEnabled(!metadataStore.getTags().isEmpty() && !selectedIds.isEmpty());
        setTag.addActionListener(ev -> openSetTagPopup(selectedIds, unionAssigned, e));
        menu.add(setTag);

        JMenuItem removeTag = new JMenuItem("Remove Tag(s)...");
        removeTag.setEnabled(!unionAssigned.isEmpty() && !selectedIds.isEmpty());
        removeTag.addActionListener(ev -> openRemoveTagPopup(selectedIds, unionAssigned, e));
        menu.add(removeTag);

        menu.addSeparator();
        JMenuItem filter = new JMenuItem("Filter by tag(s)...");
        filter.setEnabled(!metadataStore.getTags().isEmpty());
        filter.addActionListener(ev -> openFilterDialog());
        menu.add(filter);

        // Always available when at least one tag filter is currently
        // active. Useful when the filter empties the visible list and
        // the user wants to recover without re-opening the dialog.
        JMenuItem clearFilters = new JMenuItem("Clear filters");
        clearFilters.setEnabled(isFilterActive());
        clearFilters.addActionListener(ev -> clearFilters());
        menu.add(clearFilters);

        // Toggle: dim VS drop empty-model rows entirely. Checkbox menu
        // item so the current state reads at a glance. Re-runs the
        // filter pass on change.
        JCheckBoxMenuItem hideUnav = new JCheckBoxMenuItem("Hide unavailable", hideUnavailable);
        hideUnav.setToolTipText("Drop rows with no models (map markers, area placeholders) from the list entirely instead of just dimming them.");
        hideUnav.addActionListener(ev ->
        {
            hideUnavailable = !hideUnavailable;
            searchAndListEntries("");
        });
        menu.add(hideUnav);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Opens the two-column tag filter dialog. Caller's state is the
     * source of truth -- the dialog only mutates {@link #activeFilterTags}
     * + {@link #filterMode} via the change callback, and after every
     * mutation we re-run the search pass so the visible rows reflect
     * the new filter.
     */
    private void openFilterDialog()
    {
        List<Tag> all = metadataStore.getTagsAlphabetical();
        TagFilterDialog.show(this, all, activeFilterTags, filterMode, state ->
        {
            activeFilterTags.clear();
            activeFilterTags.addAll(state.activeFilterNames);
            filterMode = state.mode;
            // Re-run the current search through the new filter.
            searchAndListEntries("");
            if (filterChangedListener != null) filterChangedListener.run();
        });
    }

    /**
     * Walks the current view-selection, converts each view row to a
     * model row, extracts the item id. Used by the tag actions to
     * decide which entries the user is operating on.
     */
    private List<Integer> getSelectedIds()
    {
        int[] viewRows = getSelectedRows();
        List<Integer> ids = new ArrayList<>(viewRows.length);
        for (int viewRow : viewRows)
        {
            int modelRow = (getRowSorter() != null) ? getRowSorter().convertRowIndexToModel(viewRow) : viewRow;
            Object raw = getModel().getValueAt(modelRow, 0);
            if (raw instanceof CacheRow) ids.add(idOf(((CacheRow) raw).getItem()));
        }
        return ids;
    }

    /**
     * Returns every tag name that's already applied to ANY of the
     * selected ids. Used to seed the picker's checkmark column. We
     * deliberately use union semantics (not intersection) because the
     * Set/Remove menus operate per-entry, not per-cell -- the
     * checkmark reads as "at least one selected entry has this tag".
     */
    private Set<String> unionAssignedFor(List<Integer> ids)
    {
        if (metadataStore == null || cacheType == null) return new HashSet<>();
        Set<String> out = new LinkedHashSet<>();
        for (Integer id : ids) out.addAll(metadataStore.getTagsFor(cacheType, id));
        return out;
    }

    /**
     * Builds + shows the Set Tag(s) popup. Clicking a tag row toggles
     * the assignment ON every selected entry that doesn't have it yet
     * (when isNowChecked is true), or OFF every selected entry that
     * does (false). Empty case (no tags created yet) is guarded at
     * menu-build time so we don't reach here.
     */
    private void openSetTagPopup(List<Integer> ids, Set<String> seedAssigned, MouseEvent srcEvent)
    {
        List<Tag> all = metadataStore.getTagsAlphabetical();
        Point loc = screenLocationFor(srcEvent);
        TagPickerPopup popup = new TagPickerPopup(this, "Set Tags", false, all, new LinkedHashSet<>(seedAssigned), (tag, isNowChecked) ->
        {
            for (Integer id : ids)
            {
                if (isNowChecked) metadataStore.addTagToEntry(cacheType, id, tag.getName());
                else metadataStore.removeTagFromEntry(cacheType, id, tag.getName());
            }
        });
        popup.showAt(loc);
    }

    /**
     * Set Tag's removal-mode sibling. Lists only tags currently
     * assigned to any of the selected entries; clicking a row strips
     * the tag from every selected entry that holds it.
     */
    private void openRemoveTagPopup(List<Integer> ids, Set<String> seedAssigned, MouseEvent srcEvent)
    {
        List<Tag> assigned = new ArrayList<>();
        for (Tag t : metadataStore.getTagsAlphabetical())
        {
            if (seedAssigned.contains(t.getName())) assigned.add(t);
        }
        Point loc = screenLocationFor(srcEvent);
        TagPickerPopup popup = new TagPickerPopup(this, "Remove Tags", true, assigned, new LinkedHashSet<>(seedAssigned), (tag, isNowChecked) ->
        {
            // Remove mode: isNowChecked is always false from the picker.
            for (Integer id : ids)
            {
                metadataStore.removeTagFromEntry(cacheType, id, tag.getName());
            }
        });
        popup.showAt(loc);
    }

    /** Converts a table-relative mouse event to a screen point so the popup lands under the cursor. */
    private Point screenLocationFor(MouseEvent e)
    {
        Point screen = new Point(e.getX(), e.getY());
        SwingUtilities.convertPointToScreen(screen, e.getComponent());
        return screen;
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
