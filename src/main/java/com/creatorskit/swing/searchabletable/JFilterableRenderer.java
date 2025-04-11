package com.creatorskit.swing.searchabletable;

import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

public class JFilterableRenderer extends JLabel implements TableCellRenderer
{
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        Object[] v = (Object[]) value;
        String s = v[0].toString();
        String lowerS = s.toLowerCase();
        String sf = v[1].toString();
        String lowerSf = sf.toLowerCase();
        ArrayList<String> notMatching = new ArrayList<>();

        if (!sf.equals(""))
        {
            int fs = -1;
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

        if (html.isEmpty())
        {
            html = s;
        }

        this.setText("<html><head></head><body style=\"color: gray;\">" + html + "</body></head>");

        if (isSelected)
        {
            setOpaque(true);
            setBackground(Color.DARK_GRAY);
        }
        else
        {
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
        }
        return this;
    }
}
