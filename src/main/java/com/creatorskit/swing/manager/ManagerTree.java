package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.*;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

@Getter
public class ManagerTree extends JTree
{
    private final ToolBoxFrame toolBox;
    private final CreatorsPlugin plugin;
    private final JPanel objectHolder;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode sidePanelNode;
    private final DefaultMutableTreeNode managerNode;
    private final ManagerTreeModel treeModel;
    private final GridBagConstraints c = new GridBagConstraints();
    private Folder[] selectedFolders = new Folder[0];
    private final int ROW_HEIGHT = 28;

    private final BufferedImage FOLDER_OPEN = ImageUtil.loadImageResource(getClass(), "/Folder_Open.png");
    private final BufferedImage FOLDER_CLOSED = ImageUtil.loadImageResource(getClass(), "/Folder_Closed.png");
    private final BufferedImage OBJECT = ImageUtil.loadImageResource(getClass(), "/Object.png");

    private final Color backgroundSelectionColor = ColorScheme.MEDIUM_GRAY_COLOR;
    private final Color backgroundNonSelectionColor1 = ColorScheme.DARKER_GRAY_COLOR;
    private final Color backgroundNonSelectionColor2 = new Color(33, 33, 33);

    @Inject
    public ManagerTree(ToolBoxFrame toolBox, CreatorsPlugin plugin, JPanel objectHolder, DefaultMutableTreeNode rootNode, DefaultMutableTreeNode sidePanelNode, DefaultMutableTreeNode managerNode)
    {
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.objectHolder = objectHolder;
        this.rootNode = rootNode;
        this.sidePanelNode = sidePanelNode;
        this.managerNode = managerNode;

        setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));

        rootNode.add(sidePanelNode);
        rootNode.add(managerNode);

        treeModel = new ManagerTreeModel(rootNode, sidePanelNode, managerNode, plugin);
        setModel(treeModel);
        expandRow(0);
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setEditable(true);
        setOpaque(false);
        setRowHeight(ROW_HEIGHT);
        getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        setShowsRootHandles(true);
        setRootVisible(false);
        setDragEnabled(true);
        setDropMode(DropMode.ON_OR_INSERT);
        setTransferHandler(new ManagerTreeTransferHandler(plugin, toolBox));
        addTreeSelectionListener(new MyTreeSelectionListener());
        addTreeExpansionListener(new TreeExpansionListener()
        {
            @Override
            public void treeExpanded(TreeExpansionEvent event)
            {
                updateTreeSelectionIndex();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event)
            {
                updateTreeSelectionIndex();
            }
        });

        ActionMap actionMap = getActionMap();
        actionMap.put("cut", null);
        actionMap.put("copy", null);
        actionMap.put("paste", null);
        actionMap.getParent().put("cut", null);
        actionMap.getParent().put("copy", null);
        actionMap.getParent().put("paste", null);

        ManagerTreeCellRenderer renderer = new ManagerTreeCellRenderer();
        setCellRenderer(renderer);

        setMouseListeners();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        g.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());

        boolean colour1 = true;
        for (int i = 0; i < getRowCount(); i++)
        {
            Rectangle r = getRowBounds(i);
            g.setColor(colour1 ? backgroundNonSelectionColor1 : backgroundNonSelectionColor2);
            g.fillRect(0, r.y, getWidth(), r.height);
            colour1 = !colour1;
        }

        super.paintComponent(g);
    }

    public DefaultMutableTreeNode getParentFolderNode(ParentPanel parentPanel, boolean switching)
    {
        DefaultMutableTreeNode defaultNode = parentPanel == ParentPanel.MANAGER ? managerNode : sidePanelNode;
        DefaultMutableTreeNode parentFolderNode;
        TreePath parentPath = getSelectionPath();

        if (parentPath == null)
        {
            return defaultNode;
        }

        parentFolderNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        if (parentFolderNode.getUserObject() instanceof Character)
        {
            Character character = (Character) parentFolderNode.getUserObject();
            parentFolderNode = character.getParentManagerNode();
        }

        if (parentFolderNode == null)
        {
            parentFolderNode = defaultNode;
        }

        if (switching)
        {
            if (parentPanel == ParentPanel.SIDE_PANEL && !treeContainsSidePanel(parentFolderNode))
            {
                parentFolderNode = sidePanelNode;
            }

            if (parentPanel == ParentPanel.MANAGER && treeContainsSidePanel(parentFolderNode))
            {
                parentFolderNode = managerNode;
            }
        }

        return parentFolderNode;
    }

    public DefaultMutableTreeNode addFolderNode(String name, ParentPanel parentPanel)
    {
        return addFolderNode(getParentFolderNode(parentPanel, false), name, true);
    }

    public DefaultMutableTreeNode addFolderNode(DefaultMutableTreeNode parent, String name)
    {
        return addFolderNode(parent, name, false);
    }

    public DefaultMutableTreeNode addFolderNode(DefaultMutableTreeNode managerParent, String name, boolean shouldBeVisible)
    {
        if (managerParent == rootNode)
        {
            managerParent = managerNode;
        }

        Folder folder = new Folder(name, FolderType.STANDARD, null, managerParent);
        DefaultMutableTreeNode linkedManagerNode = new DefaultMutableTreeNode(folder);
        folder.setLinkedManagerNode(linkedManagerNode);

        treeModel.insertNodeInto(linkedManagerNode, managerParent, managerParent.getChildCount());

        if (shouldBeVisible)
            scrollPathToVisible(new TreePath(linkedManagerNode.getPath()));

        return linkedManagerNode;
    }

    /**
     * Adds a character node to the currently selected ManagerTree node. Creates a linked ManagerTree node and remembers the selected Parent node
     * @param character the character to add to the selected node
     * @param parentPanel the ParentPanel to which the character will be added (SidePanel or ManagerPanel)
     * @param shouldBeVisible visibility
     * @return the linked ManagerTree node
     */
    public DefaultMutableTreeNode addCharacterNode(Character character, ParentPanel parentPanel, boolean shouldBeVisible, boolean switching)
    {
        return addCharacterNode(getParentFolderNode(parentPanel, switching), character, parentPanel, shouldBeVisible);
    }

    // Adds the character to a previously indicated parent
    public DefaultMutableTreeNode addCharacterNode(DefaultMutableTreeNode parent, Character character, ParentPanel parentPanel)
    {
        return addCharacterNode(parent, character, parentPanel, false);
    }

    /**
     * Adds a character node to the indicated ManagerTree node. Creates a linked ManagerTree node and remembers the indicated Parent node
     * @param managerParent the parent node in the ManagerTree to which to attach the new node
     * @param character the character to add to the selected node
     * @param parentPanel the ParentPanel to which the character will be added (SidePanel or ManagerPanel)
     * @param shouldBeVisible visibility
     * @return the linked ManagerTree node
     */
    public DefaultMutableTreeNode addCharacterNode(DefaultMutableTreeNode managerParent, Character character, ParentPanel parentPanel, boolean shouldBeVisible)
    {
        if (managerParent == rootNode)
        {
            managerParent = parentPanel == ParentPanel.MANAGER ? managerNode : sidePanelNode;
        }

        if (managerParent == managerNode && parentPanel == ParentPanel.SIDE_PANEL)
        {
            managerParent = sidePanelNode;
        }

        if (managerParent == sidePanelNode && parentPanel == ParentPanel.MANAGER)
        {
            managerParent = managerNode;
        }

        DefaultMutableTreeNode linkedManagerNode = new DefaultMutableTreeNode(character);
        character.setParentPanel(parentPanel);
        character.setLinkedManagerNode(linkedManagerNode);
        character.setParentManagerNode(managerParent);

        treeModel.insertNodeInto(linkedManagerNode, managerParent, managerParent.getChildCount());

        if (shouldBeVisible)
            scrollPathToVisible(new TreePath(linkedManagerNode.getPath()));

        return linkedManagerNode;
    }

    public void removeAllNodes()
    {
        TreePath[] treePaths = new TreePath[]{new TreePath(rootNode.getPath())};
        removeNodes(treePaths);
    }

    public void removeNodes(TreePath[] paths)
    {
        ArrayList<DefaultMutableTreeNode> charactersToRemove = new ArrayList<>();
        ArrayList<DefaultMutableTreeNode> foldersToRemove = new ArrayList<>();

        for (TreePath path : paths)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof Character)
            {
                charactersToRemove.add(node);
                continue;
            }

            Folder folder = (Folder) node.getUserObject();
            {
                FolderType type = folder.getFolderType();
                if (type == FolderType.STANDARD)
                {
                    foldersToRemove.add(node);
                }
            }

            getNodeChildren(node, charactersToRemove, foldersToRemove);
        }

        if (foldersToRemove.isEmpty() && charactersToRemove.isEmpty())
        {
            return;
        }

        if (foldersToRemove.size() + charactersToRemove.size() > 1)
        {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all the selected Folders and their Objects?");
            if (result != JOptionPane.YES_OPTION)
            {
                return;
            }
        }

        Character[] characters = new Character[charactersToRemove.size()];
        for (int i = 0; i < characters.length; i++)
        {
            Character character = (Character) charactersToRemove.get(i).getUserObject();
            characters[i] = character;
        }

        Thread thread = new Thread(() ->
        {
            plugin.getCreatorsPanel().deleteCharacters(characters);
            for (DefaultMutableTreeNode node : foldersToRemove)
            {
                removeFolderNode(node);
            }
        });
        thread.start();
    }

    public void removeCharacterNode(Character child)
    {
        treeModel.removeNodeFromParent(child.getLinkedManagerNode());
    }

    public void removeFolderNode(DefaultMutableTreeNode folderNode)
    {
        treeModel.removeNodeFromParent(folderNode);
    }

    public void getNodeChildren(DefaultMutableTreeNode parent, ArrayList<DefaultMutableTreeNode> panelsToRemove, ArrayList<DefaultMutableTreeNode> foldersToRemove)
    {
        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            if (node.getUserObject() instanceof Character)
            {
                panelsToRemove.add(node);
                continue;
            }

            Folder folder = (Folder) node.getUserObject();
            {
                FolderType type = folder.getFolderType();
                if (type == FolderType.STANDARD)
                {
                    foldersToRemove.add(node);
                }
            }

            if (!node.isLeaf())
                getNodeChildren(node, panelsToRemove, foldersToRemove);
        }
    }

    public void getSidePanelChildren(ArrayList<Character> characters)
    {
        getCharacterNodeChildren(sidePanelNode, characters);
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

    public void getAllNodes(DefaultMutableTreeNode parent, ArrayList<DefaultMutableTreeNode> nodes)
    {
        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            nodes.add(node);
            if (!node.isLeaf())
                getAllNodes(node, nodes);
        }
    }

    public void getObjectPanelChildren(DefaultMutableTreeNode parent, ArrayList<Character> characters)
    {
        if (parent.getUserObject() instanceof Character)
        {
            characters.add((Character) parent.getUserObject());
            return;
        }

        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            Object object = node.getUserObject();
            if (object instanceof Character)
            {
                Character character = (Character) object;
                if (!characters.contains(character))
                    characters.add(character);
            }

            if (!node.isLeaf())
                getObjectPanelChildren(node, characters);
        }
    }

    class MyTreeSelectionListener implements TreeSelectionListener
    {
        @Override
        public void valueChanged(TreeSelectionEvent event)
        {
            TreePath[] treePaths = getSelectionPaths();
            if (treePaths == null)
            {
                return;
            }

            updateTreeSelectionIndex();

            DefaultMutableTreeNode first = (DefaultMutableTreeNode) (treePaths[0].getLastPathComponent());
            if (first.getUserObject() instanceof Character)
            {
                plugin.getCreatorsPanel().setSelectedCharacter((Character) first.getUserObject(), false);
            }
            else
            {
                plugin.getCreatorsPanel().setSelectedCharacter(null, false);
            }

            JPanel objectHolder = toolBox.getManagerPanel().getObjectHolder();

            boolean containsSidePanel = false;
            for (TreePath treePath : treePaths)
            {
                if (treePath.getLastPathComponent() == rootNode)
                {
                    objectHolder.removeAll();
                    objectHolder.revalidate();
                    return;
                }

                if (treeContainsSidePanel((TreeNode) treePath.getLastPathComponent()))
                {
                    containsSidePanel = true;
                }
            }

            ArrayList<Character> panelsToAdd = new ArrayList<>();
            Folder[] folders = new Folder[0];

            boolean folderSelected = false;
            String folderName = "";
            for (TreePath treePath : treePaths)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                if (node.getUserObject() instanceof Folder)
                {
                    Folder folder = (Folder) node.getUserObject();
                    if (folderName.isEmpty())
                    {
                        folderName = folder.getName();
                    }

                    folderSelected = true;
                    folders = ArrayUtils.add(folders, folder);
                }
            }

            if (folderSelected)
            {
                for (TreePath treePath : treePaths)
                {
                    getObjectPanelChildren((DefaultMutableTreeNode) treePath.getLastPathComponent(), panelsToAdd);
                }
            }
            else
            {
                TreePath parentPath = treePaths[0].getParentPath();
                if (parentPath == null)
                    return;

                DefaultMutableTreeNode folderNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                Folder folder = (Folder) folderNode.getUserObject();
                folderName = folder.getName();
                getObjectPanelChildren(folderNode, panelsToAdd);
                folders = ArrayUtils.add(folders, folder);
            }

            selectedFolders = folders;

            toolBox.getManagerPanel().getObjectLabel().setText("Current Folder: " + folderName);
            resetObjectHolder(panelsToAdd, containsSidePanel);
        }
    }

    public boolean treeContainsSidePanel(TreeNode node)
    {
        if (node == sidePanelNode)
        {
            return true;
        }

        TreeNode parent = node.getParent();
        if (parent == null || parent == managerNode || parent == rootNode)
        {
            return false;
        }

        if (parent == sidePanelNode)
        {
            return true;
        }

        return treeContainsSidePanel(parent);
    }

    public Character[] getShownCharacters()
    {
        Character[] characters = new Character[0];
        JPanel objectHolder = toolBox.getManagerPanel().getObjectHolder();
        for (Component component : objectHolder.getComponents())
        {
            if (component instanceof ObjectPanel)
            {
                ObjectPanel objectPanel = (ObjectPanel) component;
                characters = ArrayUtils.add(characters, objectPanel.getCharacter());
            }
        }

        return characters;
    }

    public void resetObjectHolder()
    {
        ArrayList<Character> list = new ArrayList<>();
        for (Folder folder : selectedFolders)
        {
            DefaultMutableTreeNode node = folder.getLinkedManagerNode();
            getCharacterNodeChildren(node, list);
        }

        ObjectPanel[] objectPanels = new ObjectPanel[0];
        for (Character character : list)
        {
            if (character.getParentPanel() == ParentPanel.MANAGER)
            {
                objectPanels = ArrayUtils.add(objectPanels, character.getObjectPanel());
            }
        }

        resetObjectHolder(objectPanels, false);
    }

    public void resetObjectHolder(ArrayList<Character> charactersToAdd, boolean sidePanel)
    {
        resetObjectHolder(charactersToAdd.toArray(new Character[charactersToAdd.size()]), sidePanel);
    }

    public void resetObjectHolder(Character[] charactersToAdd, boolean sidePanel)
    {
        ObjectPanel[] panelsToAdd = new ObjectPanel[charactersToAdd.length];

        for (int i = 0; i < charactersToAdd.length; i++)
        {
            panelsToAdd[i] = charactersToAdd[i].getObjectPanel();
        }
        resetObjectHolder(panelsToAdd, sidePanel);
    }

    public void resetObjectHolder(ObjectPanel[] panelsToAdd, boolean sidePanel)
    {
        JPanel[] programPanels = new JPanel[0];

        for (ObjectPanel objectPanel : panelsToAdd)
            programPanels = ArrayUtils.add(programPanels, objectPanel.getProgramPanel());

        resetProgramHolder(programPanels);

        if (sidePanel)
        {
            objectHolder.removeAll();
            objectHolder.revalidate();
            return;
        }

        JPanel managerHolder = toolBox.getManagerPanel().getObjectHolder();
        managerHolder.removeAll();

        for (ObjectPanel objectPanel : panelsToAdd)
        {
            managerHolder.add(objectPanel);
        }

        toolBox.getManagerPanel().revalidate();
        toolBox.getManagerPanel().repaint();
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

    public void onPanelScrolled(int scroll)
    {
        toolBox.getTimeSheetPanel().getSummarySheet().onVerticalScrollEvent(scroll);
    }

    public void setTreeSelection(Character character)
    {
        if (character == null)
        {
            return;
        }

        TreePath treePath = new TreePath(character.getLinkedManagerNode().getPath());
        setSelectionPath(treePath);
        scrollPathToVisible(treePath);
    }

    public void scrollSelectedIndex(int direction)
    {
        int current = getLeadSelectionRow();
        int rows = getRowCount();

        int index = current + direction;
        if (current == -1)
        {
            if (direction >= 0)
            {
                index = 0;
            }

            if (direction < 0)
            {
                index = rows - 1;
            }
        }

        if (index > rows - 1)
        {
            index = 0;
        }

        if (index < 0)
        {
            index = rows - 1;
        }

        setSelectionRow(index);
    }

    private void updateTreeSelectionIndex()
    {
        int[] rows = getSelectionRows();
        {
            if (rows == null || rows.length == 0)
            {
                return;
            }
            toolBox.getTimeSheetPanel().getSummarySheet().setSelectedIndex(rows[0]);
        }
    }

    private void setMouseListeners()
    {
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                {
                    onMouseButton3Pressed(e.getPoint());
                }
            }
        });
    }

    public void setRowSelection(Point p)
    {
        int x = (int) p.getX();
        int y = (int) p.getY();
        int row = getClosestRowForLocation(x, y);

        TreePath path = getPathForRow(row);
        if (path == null)
        {
            return;
        }

        setSelectionRow(row);

    }

    private void onMouseButton3Pressed(Point p)
    {
        int x = (int) p.getX();
        int y = (int) p.getY();
        int row = getClosestRowForLocation(x, y);

        TreePath path = getPathForRow(row);
        if (path == null)
        {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(node.getUserObject() instanceof Character))
        {
            return;
        }

        Character character = (Character) node.getUserObject();
        toolBox.getTimeSheetPanel().getSummarySheet().showSummaryPopup(this, character, x, y);
    }
}

