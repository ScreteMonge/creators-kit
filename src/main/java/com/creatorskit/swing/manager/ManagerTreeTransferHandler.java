package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.*;
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
class ManagerTreeTransferHandler extends TransferHandler
{
    private final CreatorsPlugin plugin;
    private final ToolBoxFrame toolBox;
    private DataFlavor nodesFlavor;
    private DataFlavor[] flavors = new DataFlavor[1];
    private DefaultMutableTreeNode[] nodesToRemove;

    public ManagerTreeTransferHandler(CreatorsPlugin plugin, ToolBoxFrame toolBox)
    {
        this.plugin = plugin;
        this.toolBox = toolBox;
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
        if (childIndex == -1 && parentNode.getUserObject() instanceof Character)
            return false;

        if (parentNode.getUserObject() instanceof Folder)
        {
            Folder folder = (Folder) parentNode.getUserObject();
            if (folder.getFolderType() == FolderType.MASTER)
            {
                return false;
            }
        }

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
            for (int i = 1; i < paths.length; i++)
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
            return new ManagerTreeTransferHandler.NodesTransferable(nodes);
        }
        return null;
    }

    private DefaultMutableTreeNode copy(DefaultMutableTreeNode node, HashSet<TreeNode> doneItems, JTree tree)
    {
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node.getUserObject());
        if (node.getUserObject() instanceof Folder)
        {
            ((Folder) node.getUserObject()).setLinkedManagerNode(copy);
        }

        if (node.getUserObject() instanceof Character)
        {
            ((Character) node.getUserObject()).setLinkedManagerNode(copy);
        }

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

        plugin.getCreatorsPanel().resetSidePanel();
        toolBox.getManagerPanel().getManagerTree().resetObjectHolder();
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

        ManagerPanel managerPanel = toolBox.getManagerPanel();
        boolean sendToSidePanel = managerPanel.getManagerTree().treeContainsSidePanel(parentNode);
        ParentPanel newParent = sendToSidePanel ? ParentPanel.SIDE_PANEL : ParentPanel.MANAGER;
        ArrayList<Character> sidePanelCharacters = plugin.getCreatorsPanel().getSidePanelCharacters();
        ArrayList<Character> managerCharacters = managerPanel.getManagerCharacters();

        ArrayList<Character> arrayTo;
        if (sendToSidePanel)
        {
            arrayTo = sidePanelCharacters;
        }
        else
        {
            arrayTo = managerCharacters;
        }

        // Prevent Protected folders from being transferred
        for (DefaultMutableTreeNode node : nodes)
        {
            if (node.getUserObject() instanceof Folder)
            {
                Folder folder = (Folder) node.getUserObject();
                FolderType type = folder.getFolderType();
                if (type == FolderType.MANAGER
                        || type == FolderType.MASTER
                        || type == FolderType.SIDE_PANEL)
                {
                    return false;
                }
            }
        }

        JTree tree = (JTree) support.getComponent();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        // Configure for drop mode.
        int index = childIndex;    // DropMode.INSERT
        if (childIndex == -1)     // DropMode.ON
        {
            index = parentNode.getChildCount();
        }

        ArrayList<Character> characters = new ArrayList<>();

        // Add data to model.
        for (int i = 0; i < nodes.length; i++)
        {
            DefaultMutableTreeNode node = nodes[i];
            model.insertNodeInto(node, parentNode, index++);

            if (node.getUserObject() instanceof Folder)
            {
                Folder folder = (Folder) node.getUserObject();
                folder.setParentManagerNode(parentNode);
                getCharacterNodeChildren(node, characters);
            }

            if (node.getUserObject() instanceof Character)
            {
                characters.add((Character) node.getUserObject());
            }
        }

        for (Character character : characters)
        {
            ParentPanel oldParent = character.getParentPanel();
            character.setParentPanel(newParent);
            character.setParentManagerNode(parentNode);

            ArrayList<Character> arrayFrom;
            if (oldParent == ParentPanel.SIDE_PANEL)
            {
                arrayFrom = sidePanelCharacters;
            }
            else
            {
                arrayFrom = managerCharacters;
            }

            arrayFrom.remove(character);
            arrayTo.add(character);
        }

        tree.setSelectionPath(dest);
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

    public void getCharacterNodeChildren(DefaultMutableTreeNode parent, ArrayList<Character> characters)
    {
        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            if (node.getUserObject() instanceof Character)
            {
                characters.add((Character) node.getUserObject());
                continue;
            }

            if (!node.isLeaf())
                getCharacterNodeChildren(node, characters);
        }
    }
}
