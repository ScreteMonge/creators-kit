package com.creatorskit.swing.jtree;

import com.creatorskit.swing.ObjectPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.*;

@Slf4j
class CreatorTreeTransferHandler extends TransferHandler
{
    DataFlavor nodesFlavor;
    DataFlavor[] flavors = new DataFlavor[1];
    DefaultMutableTreeNode[] nodesToRemove;

    public CreatorTreeTransferHandler()
    {
        try
        {
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                    ";class=\"" +
                    javax.swing.tree.DefaultMutableTreeNode[].class.getName() +
                    "\"";
            nodesFlavor = new DataFlavor(mimeType);
            flavors[0] = nodesFlavor;
        }
        catch(ClassNotFoundException e)
        {
            log.info("ClassNotFound: " + e.getMessage());
        }
    }

    public boolean canImport(TransferHandler.TransferSupport support)
    {
        if (!support.isDrop())
            return false;

        support.setShowDropLocation(true);
        if (!support.isDataFlavorSupported(nodesFlavor))
            return false;

        // Do not allow a drop on the drag source selections.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        JTree tree = (JTree) support.getComponent();
        int dropRow = tree.getRowForPath(dl.getPath());
        int[] selRows = tree.getSelectionRows();
        if (selRows == null || selRows.length == 0)
            return false;

        for (int i = 0; i < selRows.length; i++)
        {
            if (selRows[i] == dropRow)
                return false;

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)tree.getPathForRow(selRows[i]).getLastPathComponent();
            for (TreeNode offspring: Collections.list(treeNode.depthFirstEnumeration()))
            {
                if (tree.getRowForPath(new TreePath(((DefaultMutableTreeNode) offspring).getPath())) == dropRow)
                    return false;
            }
        }

        // If trying to insert into an ObjectPanel, cancel
        int childIndex = dl.getChildIndex();
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) dest.getLastPathComponent();
        if (childIndex == -1 && parentNode.getUserObject() instanceof ObjectPanel)
            return false;

        return true;
    }

    protected Transferable createTransferable(JComponent c)
    {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null)
        {
            // Make up a node array of copies for transfer and
            // another for/of the nodes that will be removed in
            // exportDone after a successful drop.
            ArrayList<DefaultMutableTreeNode> copies = new ArrayList<>();
            ArrayList<DefaultMutableTreeNode> toRemove = new ArrayList<>();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
            HashSet<TreeNode> doneItems = new LinkedHashSet<>(paths.length);
            DefaultMutableTreeNode copy = copy(node, doneItems, tree);
            copies.add(copy);
            toRemove.add(node);
            for(int i = 1; i < paths.length; i++)
            {
                DefaultMutableTreeNode next = (DefaultMutableTreeNode)paths[i].getLastPathComponent();
                if (doneItems.contains(next))
                    continue;

                // Do not allow higher level nodes to be added to list.
                if (next.getLevel() < node.getLevel())
                {
                    break;
                }
                else if (next.getLevel() > node.getLevel())  // child node
                {
                    copy.add(copy(next, doneItems, tree));
                    // node already contains child
                }
                else
                {  // sibling
                    copies.add(copy(next, doneItems, tree));
                    toRemove.add(next);
                }
                doneItems.add(next);
            }
            DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
            nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
            return new CreatorTreeTransferHandler.NodesTransferable(nodes);
        }
        return null;
    }

    private DefaultMutableTreeNode copy(DefaultMutableTreeNode node, HashSet<TreeNode> doneItems, JTree tree)
    {
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node.getUserObject());
        doneItems.add(node);
        for (int i = 0; i < node.getChildCount(); i++)
        {
            copy.add(copy((DefaultMutableTreeNode)((TreeNode)node).getChildAt(i), doneItems, tree));
        }
        int row = tree.getRowForPath(new TreePath(copy.getPath()));
        tree.expandRow(row);
        return copy;
    }

    protected void exportDone(JComponent source, Transferable data, int action)
    {
        if ((action & MOVE) == MOVE)
        {
            JTree tree = (JTree)source;
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            // Remove nodes saved in nodesToRemove in createTransferable.
            for (DefaultMutableTreeNode node : nodesToRemove)
                model.removeNodeFromParent(node);
        }
    }

    public int getSourceActions(JComponent c)
    {
        return COPY_OR_MOVE;
    }

    public boolean importData(TransferHandler.TransferSupport support)
    {
        if (!canImport(support))
            return false;

        // Extract transfer data.
        DefaultMutableTreeNode[] nodes = null;
        try
        {
            Transferable t = support.getTransferable();
            nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
        }
        catch(UnsupportedFlavorException ufe)
        {
            log.info("UnsupportedFlavor: " + ufe.getMessage());
        }
        catch(java.io.IOException ioe)
        {
            log.info("I/O error: " + ioe.getMessage());
        }

        // Get drop location info.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        int childIndex = dl.getChildIndex();
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) (dest.getLastPathComponent());

        JTree tree = (JTree) support.getComponent();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        // Configure for drop mode.
        int index = childIndex;    // DropMode.INSERT
        if (childIndex == -1)     // DropMode.ON
        {
            index = parentNode.getChildCount();
        }

        // Add data to model.
        for(int i = 0; i < nodes.length; i++)
        {
            model.insertNodeInto(nodes[i], parentNode, index++);
        }

        tree.expandPath(dest);
        return true;
    }

    public String toString()
    {
        return getClass().getName();
    }

    public class NodesTransferable implements Transferable
    {
        DefaultMutableTreeNode[] nodes;

        public NodesTransferable(DefaultMutableTreeNode[] nodes)
        {
            this.nodes = nodes;
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
        {
            if (!isDataFlavorSupported(flavor))
                throw new UnsupportedFlavorException(flavor);
            return nodes;
        }

        public DataFlavor[] getTransferDataFlavors()
        {
            return flavors;
        }

        public boolean isDataFlavorSupported (DataFlavor flavor)
        {
            return nodesFlavor.equals(flavor);
        }
    }
}
