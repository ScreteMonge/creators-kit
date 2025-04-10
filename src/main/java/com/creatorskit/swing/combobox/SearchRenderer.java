package com.creatorskit.swing.combobox;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.util.ArrayList;

//Code adapted from trashgod, see: https://stackoverflow.com/a/7605780
public class SearchRenderer extends BasicComboBoxRenderer
{
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        if (index == 0)
        {
            setText("");
            return this;
        }

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
        return this;
    }
}
