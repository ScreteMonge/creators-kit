package com.creatorskit.swing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.tree.DefaultMutableTreeNode;

@Getter
@Setter
@AllArgsConstructor
public class Folder
{
    private String name;
    private DefaultMutableTreeNode linkedManagerNode;
    private DefaultMutableTreeNode linkedTimeSheetNode;
    private DefaultMutableTreeNode parentManagerNode;
    private DefaultMutableTreeNode parentTimeSheetNode;

    @Override
    public String toString()
    {
        return name;
    }
}
