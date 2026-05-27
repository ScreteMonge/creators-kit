package com.creatorskit.swing.searchabletable;

import lombok.Setter;

import javax.swing.table.AbstractTableModel;

/**
 * Backing model for {@link JFilterableTable}. Used in two modes:
 *
 * <ul>
 *   <li><b>Legacy (1 column)</b>: each row in {@code data} is an
 *       {@code Object[2]} of {@code [item, searchTerm]} -- the
 *       renderer reads both to draw the search-term highlight.
 *       Used by the model-id breakdown table and any other places
 *       that aren't cache searchers.</li>
 *   <li><b>Cache (2 columns)</b>: each row is a {@link CacheRow} that
 *       carries the item + searchTerm + precomputed date bucket +
 *       hasModels flag. Column 0 returns the CacheRow itself
 *       (renderer extracts item / searchTerm / hasModels); column 1
 *       returns the date label string.</li>
 * </ul>
 *
 * <p>The {@code hasDateColumn} flag tells JTable how many columns
 * to expose and the renderer how to interpret cell values.
 */
@Setter
public class DataTableModel extends AbstractTableModel
{
    private Object[] data;
    private final boolean hasDateColumn;

    public DataTableModel(Object[] data)
    {
        this(data, false);
    }

    public DataTableModel(Object[] data, boolean hasDateColumn)
    {
        super();
        this.data = data;
        this.hasDateColumn = hasDateColumn;
    }

    public int getColumnCount()
    {
        return hasDateColumn ? 2 : 1;
    }

    public int getRowCount()
    {
        return data.length;
    }

    public Object getValueAt(int row, int col)
    {
        Object rowData = data[row];
        if (!hasDateColumn) return rowData;  // legacy Object[2] path
        // Cache mode: rowData is a CacheRow.
        if (col == 0) return rowData;
        // Column 1 = date label. Renderer reads it as a String.
        return rowData instanceof CacheRow ? ((CacheRow) rowData).getDateLabel() : "";
    }

    @Override
    public String getColumnName(int col)
    {
        if (!hasDateColumn) return "";
        return col == 0 ? "Name" : "Date Added";
    }

    /** Sort key the row sorter reads for the Date Added column. */
    public Integer getDateSortKey(int row)
    {
        Object rowData = data[row];
        return rowData instanceof CacheRow ? ((CacheRow) rowData).getSortKey() : 0;
    }
}
