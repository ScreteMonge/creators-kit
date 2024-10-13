package com.creatorskit.swing.timesheet;

import javax.inject.Inject;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class TimeTreeModel extends DefaultTreeModel
{
    @Inject
    public TimeTreeModel(TreeNode root)
    {
        super(root);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        node.setUserObject(newValue);
    }
}

