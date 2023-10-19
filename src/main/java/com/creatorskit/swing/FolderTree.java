package com.creatorskit.swing;

import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

@Getter
public class FolderTree extends JPanel
{
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final JTree tree;
    private final GridBagConstraints c = new GridBagConstraints();

    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage ADD = ImageUtil.loadImageResource(getClass(), "/Add.png");
    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");
    private final BufferedImage FOLDER_OPEN = ImageUtil.loadImageResource(getClass(), "/Folder_Open.png");
    private final BufferedImage FOLDER_CLOSED = ImageUtil.loadImageResource(getClass(), "/Folder_Closed.png");
    private final BufferedImage OBJECT = ImageUtil.loadImageResource(getClass(), "/Object.png");

    public FolderTree()
    {
        super(new GridLayout(1,0));

        rootNode = new DefaultMutableTreeNode("All Folders");
        treeModel = new DefaultTreeModel(rootNode);
        treeModel.addTreeModelListener(new MyTreeModelListener());
        tree = new JTree(treeModel);
        tree.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        tree.setBackground(Color.BLACK);
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(new ImageIcon(FOLDER_OPEN));
        renderer.setClosedIcon(new ImageIcon(FOLDER_CLOSED));
        renderer.setLeafIcon(new ImageIcon(OBJECT));
        renderer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        tree.setCellRenderer(renderer);

        JScrollPane scrollPane = new JScrollPane(tree);
        JPanel folderHeader = setupFolderHeader();
        scrollPane.setColumnHeaderView(folderHeader);
        add(scrollPane);
    }

    private JPanel setupFolderHeader()
    {
        JPanel folderHeader = new JPanel();
        folderHeader.setLayout(new GridBagLayout());
        folderHeader.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.weightx = 1;
        c.weighty = 0;
        JLabel folderLabel = new JLabel("Folders");
        folderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        folderLabel.setFont(FontManager.getRunescapeBoldFont());
        folderHeader.add(folderLabel, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JButton addFolderButton = new JButton(new ImageIcon(ADD));
        addFolderButton.setToolTipText("Add a new Folder to the currently selected Folder");
        addFolderButton.addActionListener(e ->
        {
            addObject("New Folder");
        });
        folderHeader.add(addFolderButton, c);

        c.gridx = 1;
        c.gridy = 1;
        JButton removeFolderButton = new JButton(new ImageIcon(CLOSE));
        removeFolderButton.setToolTipText("Remove the currently selected Folder and all its Objects");
        removeFolderButton.addActionListener(e -> removeObject());
        folderHeader.add(removeFolderButton, c);

        c.gridx = 2;
        c.gridy = 1;
        JButton clearFolderButton = new JButton(new ImageIcon(CLEAR));
        clearFolderButton.setToolTipText("Remove all Folders and all Objects in them");
        clearFolderButton.addActionListener(e -> clearObjects());
        folderHeader.add(clearFolderButton, c);

        return folderHeader;
    }

    public DefaultMutableTreeNode addObject(Object child)
    {
        DefaultMutableTreeNode parentNode;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null)
        {
            parentNode = rootNode;
        }
        else
        {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }

        return addObject(parentNode, child, true);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Object child)
    {
        return addObject(parent, child, false);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Object child, boolean shouldBeVisible)
    {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

        if (parent == null)
            parent = rootNode;

        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        if (shouldBeVisible)
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));

        return childNode;
    }

    public void removeObject()
    {
        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection == null)
            return;

        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
        MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
        if (parent != null)
            treeModel.removeNodeFromParent(currentNode);
    }

    public void clearObjects()
    {
        rootNode.removeAllChildren();
        treeModel.reload();
    }

    class MyTreeModelListener implements TreeModelListener {
        public void treeNodesChanged(TreeModelEvent e) {
            DefaultMutableTreeNode node;
            node = (DefaultMutableTreeNode)(e.getTreePath().getLastPathComponent());

            /*
             * If the event lists children, then the changed
             * node is the child of the node we've already
             * gotten.  Otherwise, the changed node and the
             * specified node are the same.
             */

            int index = e.getChildIndices()[0];
            node = (DefaultMutableTreeNode)(node.getChildAt(index));

            System.out.println("The user has finished editing the node.");
            System.out.println("New value: " + node.getUserObject());
        }
        public void treeNodesInserted(TreeModelEvent e) {
        }
        public void treeNodesRemoved(TreeModelEvent e) {
        }
        public void treeStructureChanged(TreeModelEvent e) {
        }
    }
}

