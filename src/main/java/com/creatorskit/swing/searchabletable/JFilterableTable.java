package com.creatorskit.swing.searchabletable;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class JFilterableTable extends JTable
{
    private List<Object> itemBackup;

    public JFilterableTable(String name)
    {
        super();
        this.setName(name);
        this.setDefaultRenderer(Object.class, new JFilterableRenderer());
        this.setTableHeader(new JTableHeader());
        setModel(new DataTableModel(new Object[0]));
    }

    public void initialize(List<Object> list)
    {
        itemBackup = list;
    }

    public void searchAndListEntries(Object searchFor)
    {
        List<Object> found = new ArrayList<>();

        //showingAll = false;
        for (int i = 0; i < this.itemBackup.size(); i++)
        {
            Object tmp = this.itemBackup.get(i);
            if (tmp == null || searchFor == null)
            {
                continue;
            }

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

    public Object getSelectedObject()
    {
        int row = getSelectedRow();
        if (row == -1)
        {
            return null;
        }

        Object[] o = (Object[]) this.getModel().getValueAt(row, 0);
        return o[0];
    }
}
