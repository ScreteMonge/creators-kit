package com.creatorskit.swing.searchabletable;

import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

/**
 * Cell renderer for {@link JFilterableTable}. Two modes:
 *
 * <ul>
 *   <li><b>Legacy</b>: value is {@code Object[2]} {@code [item, searchTerm]} --
 *       renders the item's toString with the search-term substring
 *       highlighted in orange.</li>
 *   <li><b>Cache</b>: value is a {@link CacheRow} for column 0 (renderer
 *       extracts item + searchTerm), or a String for column 1
 *       (date label). Rows whose {@code hasModels} is false render in
 *       darker / italic text so the user can spot unrenderable entries
 *       like map markers and area placeholders.</li>
 * </ul>
 */
public class JFilterableRenderer extends JLabel implements TableCellRenderer
{
    /** Text color for normal rows. The HTML pre-wrap uses this. */
    private static final String NORMAL_FG = "#bfbfbf";
    /** Dimmer text color used when the row has no renderable models. */
    private static final String DIM_FG = "#5e5e5e";

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        // Cache mode column 0: value is CacheRow.
        if (value instanceof CacheRow)
        {
            CacheRow r = (CacheRow) value;
            renderNameCell(r.getItem().toString(), r.getSearchTerm(), r.isHasModels());
            paintSelection(isSelected);
            return this;
        }
        // Cache mode column 1: value is the date label String. We can't
        // see the row's hasModels from here without the row index, so
        // pull it via the table's model side-channel.
        if (table.getColumnCount() > 1 && column == 1)
        {
            boolean has = true;
            if (table.getModel() instanceof DataTableModel)
            {
                int modelRow = table.convertRowIndexToModel(row);
                Object raw = table.getModel().getValueAt(modelRow, 0);
                if (raw instanceof CacheRow) has = ((CacheRow) raw).isHasModels();
            }
            String text = String.valueOf(value);
            String color = has ? NORMAL_FG : DIM_FG;
            String style = has ? "" : " font-style: italic;";
            setText("<html><head></head><body style=\"color: " + color + ";" + style + "\">" + text + "</body></html>");
            paintSelection(isSelected);
            return this;
        }

        // Legacy mode: value is Object[2] {item, searchTerm}.
        Object[] v = (Object[]) value;
        renderNameCell(v[0].toString(), v[1].toString(), true);
        paintSelection(isSelected);
        return this;
    }

    /**
     * Renders the Name column text with search-term highlighting.
     * When {@code hasModels} is false the whole label dims to
     * {@link #DIM_FG} + italic so the user can spot unrenderable
     * entries at a glance.
     */
    private void renderNameCell(String s, String sf, boolean hasModels)
    {
        String lowerS = s.toLowerCase();
        String lowerSf = sf == null ? "" : sf.toLowerCase();
        ArrayList<String> notMatching = new ArrayList<>();

        if (!lowerSf.isEmpty())
        {
            int fs;
            int lastFs = 0;
            while ((fs = lowerS.indexOf(lowerSf, (lastFs == 0) ? -1 : lastFs)) > -1)
            {
                notMatching.add(s.substring(lastFs, fs));
                lastFs = fs + sf.length();
            }
            notMatching.add(s.substring(lastFs));
        }

        String html = "";
        if (notMatching.size() > 1)
        {
            html = notMatching.get(0);
            int start = html.length();
            int sfl = sf.length();
            for (int i = 1; i < notMatching.size(); i++)
            {
                String t = notMatching.get(i);
                html += "<b style=\"color: orange;\">" + s.substring(start, start + sfl) + "</b>" + t;
                start += sfl + t.length();
            }
        }
        if (html.isEmpty()) html = s;

        String color = hasModels ? NORMAL_FG : DIM_FG;
        String style = hasModels ? "" : " font-style: italic;";
        setText("<html><head></head><body style=\"color: " + color + ";" + style + "\">" + html + "</body></html>");
    }

    private void paintSelection(boolean isSelected)
    {
        if (isSelected)
        {
            setOpaque(true);
            setBackground(Color.DARK_GRAY);
        }
        else
        {
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
        }
    }
}
