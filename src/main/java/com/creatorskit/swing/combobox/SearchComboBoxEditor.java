package com.creatorskit.swing.combobox;

import javax.swing.plaf.basic.BasicComboBoxEditor;

//Code adapted from trashgod, see: https://stackoverflow.com/a/7605780
public class SearchComboBoxEditor extends BasicComboBoxEditor
{
    public SearchComboBoxEditor()
    {
        super();
    }

    @Override
    public void setItem(Object anObject)
    {
        if (anObject == null)
        {
            super.setItem(anObject);
        }
        else
        {
            Object[] o = (Object[]) anObject;
            super.setItem(o[0]);
        }
    }

    @Override
    public Object getItem()
    {
        return new Object[]{super.getItem(), super.getItem(), 0};
    }
}
