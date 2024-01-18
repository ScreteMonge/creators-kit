package com.creatorskit.swing.jtree;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.ManagerPanel;
import com.creatorskit.swing.ObjectPanel;
import com.creatorskit.swing.ProgrammerPanel;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

@Getter
public class FolderTree extends JScrollPane
{
    private final CreatorsPlugin plugin;
    private final ManagerPanel managerPanel;
    private final DefaultMutableTreeNode rootNode;
    private final CreatorTreeModel treeModel;
    private final JButton[] headerButtons = new JButton[3];
    private final JTree tree;
    private final GridBagConstraints c = new GridBagConstraints();

    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage ADD = ImageUtil.loadImageResource(getClass(), "/Add.png");
    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");
    private final BufferedImage FOLDER_OPEN = ImageUtil.loadImageResource(getClass(), "/Folder_Open.png");
    private final BufferedImage FOLDER_CLOSED = ImageUtil.loadImageResource(getClass(), "/Folder_Closed.png");
    private final BufferedImage OBJECT = ImageUtil.loadImageResource(getClass(), "/Object.png");

    @Inject
    public FolderTree(ManagerPanel managerPanel, CreatorsPlugin plugin)
    {
        this.plugin = plugin;
        this.managerPanel = managerPanel;
        setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        setMinimumSize(new Dimension(250, 0));
        rootNode = new DefaultMutableTreeNode("Master Folder                                           ");
        treeModel = new CreatorTreeModel(rootNode, plugin);
        tree = new JTree(treeModel);
        tree.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        tree.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setEditable(true);
        tree.setRowHeight(20);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new CreatorTreeTransferHandler());
        tree.addTreeSelectionListener(new MyTreeSelectionListener());
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(new ImageIcon(FOLDER_OPEN));
        renderer.setClosedIcon(new ImageIcon(FOLDER_CLOSED));
        renderer.setLeafIcon(new ImageIcon(OBJECT));
        renderer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        tree.setCellRenderer(renderer);

        setViewportView(tree);
        JPanel folderHeader = setupFolderHeader();
        setColumnHeaderView(folderHeader);
    }

    private JPanel setupFolderHeader()
    {
        JPanel folderHeader = new JPanel();
        folderHeader.setLayout(new GridBagLayout());
        folderHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.weightx = 1;
        c.weighty = 0;
        JLabel folderLabel = new JLabel("Folders");
        folderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        folderLabel.setVerticalAlignment(SwingConstants.CENTER);
        folderLabel.setPreferredSize(new Dimension(50, 25));
        folderLabel.setFont(FontManager.getRunescapeBoldFont());
        folderHeader.add(folderLabel, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JButton addFolderButton = new JButton(new ImageIcon(ADD));
        addFolderButton.setToolTipText("Add a new Folder to the currently selected Folder");
        addFolderButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        addFolderButton.addActionListener(e -> addNode("New Folder"));
        folderHeader.add(addFolderButton, c);

        c.gridx = 1;
        c.gridy = 1;
        JButton removeFolderButton = new JButton(new ImageIcon(CLOSE));
        removeFolderButton.setToolTipText("Remove the currently selected Object or Folder and all children");
        removeFolderButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        removeFolderButton.addActionListener(e ->
        {
            TreePath[] treePaths = tree.getSelectionPaths();
            if (treePaths == null)
                return;

            removeNodes(treePaths);
        });
        folderHeader.add(removeFolderButton, c);

        c.gridx = 2;
        c.gridy = 1;
        JButton clearFolderButton = new JButton(new ImageIcon(CLEAR));
        clearFolderButton.setToolTipText("Remove all Folders and all Objects in them");
        clearFolderButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        clearFolderButton.addActionListener(e -> removeAllNodes());
        folderHeader.add(clearFolderButton, c);

        headerButtons[0] = addFolderButton;
        headerButtons[1] = removeFolderButton;
        headerButtons[2] = clearFolderButton;

        return folderHeader;
    }

    public DefaultMutableTreeNode addNode(Object child)
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
            if (parentNode.getUserObject() instanceof ObjectPanel)
            {
                parentNode = (DefaultMutableTreeNode) parentPath.getParentPath().getLastPathComponent();
                if (parentNode == null)
                    parentNode = rootNode;
            }
        }

        return addNode(parentNode, child, true);
    }

    public DefaultMutableTreeNode addNode(DefaultMutableTreeNode parent, Object child)
    {
        return addNode(parent, child, false);
    }

    public DefaultMutableTreeNode addNode(DefaultMutableTreeNode parent, Object child, boolean shouldBeVisible)
    {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

        if (parent == null)
            parent = rootNode;

        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        if (shouldBeVisible)
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));

        return childNode;
    }

    public void removeAllNodes()
    {
        TreePath[] treePaths = new TreePath[]{tree.getPathForRow(0)};
        removeNodes(treePaths);
    }

    public void removeNodes(TreePath[] paths)
    {
        ArrayList<DefaultMutableTreeNode> panelsToRemove = new ArrayList<>();
        ArrayList<DefaultMutableTreeNode> foldersToRemove = new ArrayList<>();

        for (TreePath path : paths)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof ObjectPanel)
            {
                panelsToRemove.add(node);
                continue;
            }

            foldersToRemove.add(node);
            getNodeChildren(node, panelsToRemove, foldersToRemove);
        }

        if (foldersToRemove.isEmpty() && panelsToRemove.isEmpty())
            return;

        if (!foldersToRemove.isEmpty() && !panelsToRemove.isEmpty())
        {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all the selected Folders and their Objects?");
            if (result != JOptionPane.YES_OPTION)
                return;
        }

        for (DefaultMutableTreeNode node : panelsToRemove)
            plugin.getCreatorsPanel().onDeleteButtonPressed((ObjectPanel) node.getUserObject());

        for (DefaultMutableTreeNode node : foldersToRemove)
            removeNode(node);
    }

    public void removeNode(ObjectPanel child)
    {
        DefaultMutableTreeNode node = findNode(child);
        if (node == null)
            return;

        MutableTreeNode parent = (MutableTreeNode) node.getParent();
        if (parent != null)
            treeModel.removeNodeFromParent(node);
    }

    public void removeNode(DefaultMutableTreeNode folderNode)
    {
        MutableTreeNode parent = (MutableTreeNode) folderNode.getParent();
        if (parent != null)
            treeModel.removeNodeFromParent(folderNode);
    }

    public void getNodeChildren(DefaultMutableTreeNode parent, ArrayList<DefaultMutableTreeNode> panelsToRemove, ArrayList<DefaultMutableTreeNode> foldersToRemove)
    {
        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            if (node.getUserObject() instanceof ObjectPanel)
            {
                panelsToRemove.add(node);
                continue;
            }

            foldersToRemove.add(node);
            if (!node.isLeaf())
                getNodeChildren(node, panelsToRemove, foldersToRemove);
        }
    }

    public void getObjectPanelChildren(DefaultMutableTreeNode parent, ArrayList<ObjectPanel> objectPanels)
    {
        if (parent.getUserObject() instanceof ObjectPanel)
        {
            objectPanels.add((ObjectPanel) parent.getUserObject());
            return;
        }

        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            Object object = node.getUserObject();
            if (object instanceof ObjectPanel)
            {
                ObjectPanel objectPanel = (ObjectPanel) object;
                if (!objectPanels.contains(objectPanel))
                    objectPanels.add(objectPanel);
            }

            if (!node.isLeaf())
                getObjectPanelChildren(node, objectPanels);
        }
    }

    public void clearObjects()
    {
        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all Folders and Objects from the Manager?");
        if (result != JOptionPane.YES_OPTION)
            return;

        ArrayList<DefaultMutableTreeNode> panelsToRemove = new ArrayList<>();
        ArrayList<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();

        getNodeChildren(rootNode, panelsToRemove, nodesToRemove);

        for (DefaultMutableTreeNode node : panelsToRemove)
            managerPanel.removeObject(node);

        for (DefaultMutableTreeNode node : nodesToRemove)
        {
            MutableTreeNode parent = (MutableTreeNode) (node.getParent());
            if (parent != null)
                treeModel.removeNodeFromParent(node);
        }
        treeModel.reload();
    }

    public DefaultMutableTreeNode findNode(Object child)
    {
        Enumeration<?> e = rootNode.preorderEnumeration();
        while(e.hasMoreElements())
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() == child)
                return node;
        }
        return null;
    }

    class MyTreeSelectionListener implements TreeSelectionListener
    {
        @Override
        public void valueChanged(TreeSelectionEvent event)
        {
            TreePath[] treePaths = tree.getSelectionPaths();
            if (treePaths == null)
                return;

            ArrayList<ObjectPanel> panelsToAdd = new ArrayList<>();

            boolean noFolderSelected = true;
            String folderName = "";
            for (TreePath treePath : treePaths)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                if (node.getUserObject() instanceof String)
                {
                    if (folderName.isEmpty())
                        folderName = (String) node.getUserObject();
                    noFolderSelected = false;
                }
            }

            if (noFolderSelected)
            {
                TreePath parentPath = treePaths[0].getParentPath();
                if (parentPath == null)
                    return;

                DefaultMutableTreeNode folder = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                folderName = (String) folder.getUserObject();
                getObjectPanelChildren(folder, panelsToAdd);
            }
            else
            {
                for (TreePath treePath : treePaths)
                {
                    getObjectPanelChildren((DefaultMutableTreeNode) treePath.getLastPathComponent(), panelsToAdd);
                }
            }

            managerPanel.getObjectLabel().setText("Current Folder: " + folderName);
            resetObjectHolder(panelsToAdd);
        }
    }

    public void resetObjectHolder()
    {
        ObjectPanel[] objectPanels = new ObjectPanel[0];
        JPanel objectHolder = managerPanel.getObjectHolder();
        for (Component component : objectHolder.getComponents())
        {
            if (component instanceof ObjectPanel)
            {
                ObjectPanel objectPanel = (ObjectPanel) component;
                objectPanels = ArrayUtils.add(objectPanels, objectPanel);
            }
        }

        resetObjectHolder(objectPanels);
    }

    public void resetObjectHolder(ArrayList<ObjectPanel> panelsToAdd)
    {
        resetObjectHolder(panelsToAdd.toArray(new ObjectPanel[panelsToAdd.size()]));
    }

    public void resetObjectHolder(ObjectPanel[] panelsToAdd)
    {
        JPanel[] programPanels = new JPanel[0];
        for (ObjectPanel objectPanel : panelsToAdd)
            programPanels = ArrayUtils.add(programPanels, objectPanel.getProgramPanel());

        resetProgramHolder(programPanels);

        GridBagConstraints cManager = plugin.getCreatorsPanel().getCManager();
        cManager.fill = GridBagConstraints.NONE;
        cManager.insets = new Insets(2, 2, 2, 2);
        cManager.gridx = 0;
        cManager.gridy = 0;
        cManager.weightx = 0;
        cManager.weighty = 0;
        cManager.gridheight = 1;
        cManager.gridwidth = 1;

        JPanel objectHolder = managerPanel.getObjectHolder();
        objectHolder.removeAll();
        int rows = panelsToAdd.length / 5;

        for (int i = 0; i < panelsToAdd.length; i++)
        {
            ObjectPanel objectPanel = panelsToAdd[i];

            cManager.weightx = cManager.gridx == 4 ? 1 : 0;
            if (cManager.gridx == panelsToAdd.length - 1)
                cManager.weightx = 1;

            cManager.weighty = cManager.gridy == rows ? 1 : 0;
            if (i == panelsToAdd.length - 1)
                cManager.weighty = 1;

            objectHolder.add(objectPanel, cManager);
            cManager.gridx++;
            if (cManager.gridx > 4)
            {
                cManager.gridx = 0;
                cManager.gridy++;
            }
        }

        managerPanel.revalidate();
        managerPanel.repaint();
    }

    public void resetProgramHolder(JPanel[] panelsToAdd)
    {
        ProgrammerPanel programmerPanel = plugin.getCreatorsPanel().getProgrammerPanel();
        GridBagConstraints cManager = programmerPanel.getC();
        cManager.fill = GridBagConstraints.NONE;
        cManager.insets = new Insets(2, 2, 2, 2);
        cManager.gridx = 0;
        cManager.gridy = 0;
        cManager.weightx = 0;
        cManager.weighty = 0;
        cManager.gridheight = 1;
        cManager.gridwidth = 1;
        cManager.anchor = GridBagConstraints.FIRST_LINE_START;

        JPanel programHolder = programmerPanel.getManagerProgramHolder();
        programHolder.removeAll();
        int rows = panelsToAdd.length / 5;

        for (int i = 0; i < panelsToAdd.length; i++)
        {
            JPanel panel = panelsToAdd[i];

            cManager.weightx = cManager.gridx == 4 ? 1 : 0;
            if (cManager.gridx == panelsToAdd.length - 1)
                cManager.weightx = 1;

            cManager.weighty = cManager.gridy == rows ? 1 : 0;
            if (i == panelsToAdd.length - 1)
                cManager.weighty = 1;

            programHolder.add(panel, cManager);
            cManager.gridx++;
            if (cManager.gridx > 4)
            {
                cManager.gridx = 0;
                cManager.gridy++;
            }
        }

        programmerPanel.revalidate();
        programmerPanel.repaint();
    }
}

