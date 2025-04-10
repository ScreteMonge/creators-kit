package com.creatorskit.swing.searchabletable;

import lombok.Setter;

import javax.swing.table.AbstractTableModel;

@Setter
public class DataTableModel extends AbstractTableModel
{
    private Object[] data;

    public DataTableModel(Object[] data)
    {
        super();
        this.data = data;
    }

    public int getColumnCount()
    {
        return 1;
    }

    public int getRowCount()
    {
        return data.length;
    }

    public Object getValueAt(int row, int col)
    {
        return data[row];
    }

}
