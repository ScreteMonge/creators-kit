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
    private FolderType folderType;
    private DefaultMutableTreeNode linkedManagerNode;
    private DefaultMutableTreeNode parentManagerNode;

    @Override
    public String toString()
    {
        return name;
    }
}
