package com.creatorskit.swing.searchabletable;

import com.creatorskit.cache.metadata.CacheMetadataStore;
import com.creatorskit.cache.metadata.CacheType;
import com.creatorskit.models.datatypes.AnimData;
import com.creatorskit.models.datatypes.ItemData;
import com.creatorskit.models.datatypes.NPCData;
import com.creatorskit.models.datatypes.ObjectData;
import com.creatorskit.models.datatypes.SoundData;
import com.creatorskit.models.datatypes.SpotanimData;
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
    /** Italic yellow used for user-renamed entries -- mirrors the spec. */
    private static final String RENAME_FG = "#f1c40f";

    private CacheMetadataStore metadataStore;
    private CacheType cacheType;

    /**
     * Lets the rename / tag display read its data straight from the
     * store on each render pass. Called once by JFilterableTable when
     * setMetadataStore is invoked.
     */
    public void setMetadataStore(CacheMetadataStore store, CacheType type)
    {
        this.metadataStore = store;
        this.cacheType = type;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        // Cache mode column 0: value is CacheRow.
        if (value instanceof CacheRow)
        {
            CacheRow r = (CacheRow) value;
            String displayName = r.getItem().toString();
            boolean renamed = false;
            if (metadataStore != null && cacheType != null)
            {
                String custom = metadataStore.getRename(cacheType, idOf(r.getItem()));
                if (custom != null)
                {
                    // Keep the "(id)" suffix so users can still see which
                    // entry they renamed -- "Custom (8060)" instead of
                    // just "Custom".
                    displayName = custom + idSuffix(r.getItem());
                    renamed = true;
                }
            }
            renderNameCell(displayName, r.getSearchTerm(), r.isHasModels(), renamed);
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
        renderNameCell(v[0].toString(), v[1].toString(), true, false);
        paintSelection(isSelected);
        return this;
    }

    /**
     * Renders the Name column text with search-term highlighting.
     * When {@code hasModels} is false the whole label dims to
     * {@link #DIM_FG} + italic so the user can spot unrenderable
     * entries at a glance. When {@code renamed} is true the label
     * paints in italic {@link #RENAME_FG} (yellow) -- the convention
     * is "user authored this name".
     */
    private void renderNameCell(String s, String sf, boolean hasModels, boolean renamed)
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

        // Rename overrides the dim treatment -- a renamed entry is by
        // definition user-curated, so it reads as italic yellow even if
        // its underlying model array is empty.
        String color;
        String style;
        if (renamed)
        {
            color = RENAME_FG;
            style = " font-style: italic;";
        }
        else if (!hasModels)
        {
            color = DIM_FG;
            style = " font-style: italic;";
        }
        else
        {
            color = NORMAL_FG;
            style = "";
        }
        setText("<html><head></head><body style=\"color: " + color + ";" + style + "\">" + html + "</body></html>");
    }

    /** Per-type id extraction for the rename lookup. Mirrors JFilterableTable's idOf. */
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

    /** Extracts the trailing " (id)" suffix from an XData toString so renamed labels keep it. */
    private static String idSuffix(Object item)
    {
        String full = item.toString();
        int idx = full.lastIndexOf(" (");
        return idx > 0 ? full.substring(idx) : "";
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
