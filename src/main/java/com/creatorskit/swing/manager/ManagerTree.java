package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.selection.SelectionOrigin;
import com.creatorskit.swing.*;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private final SelectionManager selectionManager;
    private final JPanel objectHolder;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode sidePanelNode;
    private final DefaultMutableTreeNode managerNode;
    private final ManagerTreeModel treeModel;
    private final GridBagConstraints c = new GridBagConstraints();
    private List<Folder> selectedFolders = new ArrayList<>();
    private final int ROW_HEIGHT = 28;

    private final BufferedImage FOLDER_OPEN = ImageUtil.loadImageResource(getClass(), "/Folder_Open.png");
    private final BufferedImage FOLDER_CLOSED = ImageUtil.loadImageResource(getClass(), "/Folder_Closed.png");
    private final BufferedImage OBJECT = ImageUtil.loadImageResource(getClass(), "/Object.png");

    private final Color backgroundSelectionColor = ColorScheme.MEDIUM_GRAY_COLOR;
    private final Color backgroundNonSelectionColor1 = ColorScheme.DARKER_GRAY_COLOR;
    private final Color backgroundNonSelectionColor2 = new Color(33, 33, 33);

    @Inject
    public ManagerTree(ToolBoxFrame toolBox, CreatorsPlugin plugin, SelectionManager selectionManager, JPanel objectHolder, DefaultMutableTreeNode rootNode, DefaultMutableTreeNode sidePanelNode, DefaultMutableTreeNode managerNode)
    {
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.selectionManager = selectionManager;
        this.objectHolder = objectHolder;
        this.rootNode = rootNode;
        this.sidePanelNode = sidePanelNode;
        this.managerNode = managerNode;
        selectionManager.addListener((manager, origin) ->
        {
            if (origin == SelectionOrigin.MANAGER_TREE)
            {
                return;
            }

            setTreeSelection(manager.getSelected());
        });

        setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));

        rootNode.add(sidePanelNode);
        rootNode.add(managerNode);

        treeModel = new ManagerTreeModel(rootNode, sidePanelNode, managerNode, plugin);
        setModel(treeModel);
        expandRow(0);
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setOpaque(false);
        setRowHeight(ROW_HEIGHT);
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
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
        actionMap.put("toggleAndAnchor", null);
        actionMap.put("selectAll", null);
        actionMap.getParent().put("cut", null);
        actionMap.getParent().put("copy", null);
        actionMap.getParent().put("paste", null);
        actionMap.getParent().put("toggleAndAnchor", null);
        actionMap.getParent().put("selectAll", null);

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
        return addFolderNode(getParentFolderNode(parentPanel, false), name, parentPanel, true);
    }

    public DefaultMutableTreeNode addFolderNode(DefaultMutableTreeNode parent, ParentPanel parentPanel, String name)
    {
        return addFolderNode(parent, name, parentPanel, false);
    }

    public DefaultMutableTreeNode addFolderNode(DefaultMutableTreeNode managerParent, String name, ParentPanel parentPanel, boolean shouldBeVisible)
    {
        if (managerParent == rootNode)
        {
            managerParent = managerNode;
        }

        Folder folder = new Folder(name, FolderType.STANDARD, parentPanel, null, managerParent);
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
        removeNodes(treePaths, false);

        plugin.getCreatorsPanel().updateLoadedFile(null);
        SwingUtilities.invokeLater(() ->
        {
            toolBox.repaint();
            toolBox.revalidate();
        });
    }

    public void removeNodes(TreePath[] paths, boolean showWarning)
    {
        ArrayList<DefaultMutableTreeNode> charactersToRemove = new ArrayList<>();
        ArrayList<DefaultMutableTreeNode> foldersToRemove = new ArrayList<>();

        for (TreePath path : paths)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof Character)
            {
                if (!charactersToRemove.contains(node))
                {
                    charactersToRemove.add(node);
                }
                continue;
            }

            Folder folder = (Folder) node.getUserObject();
            {
                FolderType type = folder.getFolderType();
                if (type == FolderType.STANDARD && !foldersToRemove.contains(node))
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

        if (showWarning && foldersToRemove.size() + charactersToRemove.size() > 1)
        {
            int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete all the selected Folders and their Objects?");
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

            if (cellEditor != null)
            {
                getCellEditor().cancelCellEditing();
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
                if (!panelsToRemove.contains(node))
                {
                    panelsToRemove.add(node);
                }
                continue;
            }

            Folder folder = (Folder) node.getUserObject();
            {
                FolderType type = folder.getFolderType();
                if (type == FolderType.STANDARD && !foldersToRemove.contains(node))
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

    public void getObjectPanelChildren(DefaultMutableTreeNode parent, ArrayList<Character> characters, @Nullable ParentPanel parentPanel)
    {
        if (parent.getUserObject() instanceof Character)
        {
            Character character = (Character) parent.getUserObject();
            if (parentPanel == null || parentPanel == character.getParentPanel())
            {
                characters.add(character);
            }
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
                {
                    if (parentPanel == null || parentPanel == character.getParentPanel())
                    {
                        characters.add(character);
                    }
                }
            }

            if (!node.isLeaf())
                getObjectPanelChildren(node, characters, parentPanel);
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
                selectionManager.clear(SelectionOrigin.MANAGER_TREE);
                return;
            }

            updateTreeSelectionIndex();

            ArrayList<Character> characters = new ArrayList<>();
            ArrayList<Folder> folders = new ArrayList<>();

            for (TreePath path : treePaths)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                if (node.getUserObject() instanceof Character)
                {
                    characters.add((Character) node.getUserObject());
                }
            }

            ArrayList<Character> panelsToAdd = new ArrayList<>();
            getPanelsToShow(panelsToAdd, folders, ParentPanel.MANAGER);

            selectedFolders = folders;
            selectionManager.selectAll(characters, SelectionOrigin.MANAGER_TREE);

            resetObjectHolder(panelsToAdd);
        }
    }

    public void getPanelsToShow(ArrayList<Character> characters, ArrayList<Folder> folders, ParentPanel parentPanel)
    {
        TreePath[] treePaths = getSelectionPaths();
        if (treePaths == null)
        {
            return;
        }

        for (TreePath treePath : treePaths)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            if (node.getUserObject() instanceof Folder)
            {
                Folder folder = (Folder) node.getUserObject();
                if (folder.getParentPanel() == parentPanel)
                {
                    folders.add(folder);
                }
            }
        }

        if (!folders.isEmpty())
        {
            for (TreePath treePath : treePaths)
            {
                getObjectPanelChildren((DefaultMutableTreeNode) treePath.getLastPathComponent(), characters,  parentPanel);
            }
        }
        else
        {
            TreePath parentPath = treePaths[0].getParentPath();
            if (parentPath == null)
                return;

            DefaultMutableTreeNode folderNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
            getObjectPanelChildren(folderNode, characters,  parentPanel);
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

    public void resetObjectHolder()
    {
        ArrayList<Character> list = new ArrayList<>();
        for (Folder folder : selectedFolders)
        {
            DefaultMutableTreeNode node = folder.getLinkedManagerNode();
            getCharacterNodeChildren(node, list);
        }

        JPanel[] objectPanels = new JPanel[0];
        for (Character character : list)
        {
            if (character.getParentPanel() == ParentPanel.MANAGER)
            {
                objectPanels = ArrayUtils.add(objectPanels, character.getObjectPanel());
            }
        }

        resetObjectHolder(objectPanels);
    }

    public void resetObjectHolder(ArrayList<Character> charactersToAdd)
    {
        resetObjectHolder(charactersToAdd.toArray(new Character[charactersToAdd.size()]));
    }

    public void resetObjectHolder(Character[] charactersToAdd)
    {
        JPanel[] panelsToAdd = new JPanel[charactersToAdd.length];

        for (int i = 0; i < charactersToAdd.length; i++)
        {
            panelsToAdd[i] = charactersToAdd[i].getObjectPanel();
        }
        resetObjectHolder(panelsToAdd);
    }

    public void resetObjectHolder(JPanel[] panelsToAdd)
    {
        JPanel managerHolder = toolBox.getManagerPanel().getObjectHolder();
        managerHolder.removeAll();

        for (JPanel objectPanel : panelsToAdd)
        {
            managerHolder.add(objectPanel);
        }

        toolBox.getManagerPanel().revalidate();
        toolBox.getManagerPanel().repaint();
    }

    public void onPanelScrolled(int scroll)
    {
        toolBox.getTimeSheetPanel().getSummarySheet().onVerticalScrollEvent(scroll);
    }

    public void setTreeSelection(Set<Character> selected)
    {
        List<Character> characters = new ArrayList<>(selected);
        if (characters.isEmpty())
        {
            setSelectionPath(null);
            updateTreeSelectionIndex();
            return;
        }

        TreePath[] paths = new TreePath[characters.size()];
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            TreePath path = new TreePath(character.getLinkedManagerNode().getPath());
            paths[i] = path;
        }

        for (int i = 0; i < selectedFolders.size(); i++)
        {
            Folder folder = selectedFolders.get(i);
            paths = ArrayUtils.add(paths, new TreePath(folder.getLinkedManagerNode().getPath()));
        }

        setSelectionPaths(paths);
        updateTreeSelectionIndex();
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
        updateTreeSelectionIndex();
    }

    public void updateTreeSelectionIndex()
    {
        int[] rows = getSelectionRows();
        int current = getLeadSelectionRow();
        if (current == -1)
        {
            toolBox.getTimeSheetPanel().getSummarySheet().setSelectedIndex(-1);
            return;
        }

        int row = 0;
        if (rows != null && rows.length > 0)
        {
            row = rows[0];
        }

        toolBox.getTimeSheetPanel().getSummarySheet().setSelectedIndex(row);
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

        Rectangle rect = getRowBounds(row);

        if (!rect.contains(rect.getX(), y))
        {
            return;
        }

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

        Rectangle rect = getRowBounds(row);

        if (!rect.contains(rect.getX(), y))
        {
            return;
        }

        TreePath path = getPathForRow(row);
        if (path == null)
        {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (node.getUserObject() instanceof Character)
        {
            Character character = (Character) node.getUserObject();
            showCharacterContextMenu(character, x, y);
            return;
        }

        if (node.getUserObject() instanceof Folder)
        {
            Folder folder = (Folder) node.getUserObject();
            showFolderContextMenu(folder, x, y);
        }
    }

    private void showCharacterContextMenu(Character character, int x, int y)
    {
        int count = selectionManager.getSelectionSize();
        JPopupMenu popup = new JPopupMenu();

        JLabel title = new JLabel(count > 1 ? count + " Characters selected" : character.getName());
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setBorder(new EmptyBorder(2, 6, 2, 6));
        popup.add(title);
        popup.addSeparator();

        JMenuItem rename = new JMenuItem("Rename");
        rename.addActionListener(e -> showRenamePopup(character.getLinkedManagerNode(), x, y));
        popup.add(rename);

        JMenuItem recolour = new JMenuItem("Recolour");
        recolour.addActionListener(e ->
                plugin.getCreatorsPanel().showColorPickerAt(this, x, y, character));
        popup.add(recolour);

        JMenuItem keyframes = new JMenuItem("Change Summary Preview");
        keyframes.addActionListener(e ->
                toolBox.getTimeSheetPanel().getSummarySheet().showSummaryPopup(this, character, x, y));
        popup.add(keyframes);

        popup.show(this, x, y);
    }

    private void showFolderContextMenu(Folder folder, int x, int y)
    {
        JPopupMenu popup = new JPopupMenu();

        JLabel title = new JLabel(folder.getName());
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setBorder(new EmptyBorder(2, 6, 2, 6));
        popup.add(title);
        popup.addSeparator();

        JMenuItem selectAll = new JMenuItem("Select All");
        selectAll.addActionListener(e ->
        {
            ArrayList<DefaultMutableTreeNode> list = new ArrayList<>();
            DefaultMutableTreeNode parent = folder.getLinkedManagerNode();
            getAllNodes(parent, list);
            list.add(parent);

            List<Character> characters = new ArrayList<>();
            List<Folder> folders = new ArrayList<>();

            for (DefaultMutableTreeNode node : list)
            {
                Object o = node.getUserObject();
                if (o instanceof Character)
                {
                    characters.add((Character) o);
                }

                if (o instanceof Folder)
                {
                    folders.add((Folder) o);
                }
            }

            selectedFolders = folders;
            plugin.getSelectionManager().selectAll(characters, SelectionOrigin.MANAGER_TREE);
        });
        popup.add(selectAll);

        FolderType type = folder.getFolderType();
        if (type != FolderType.MASTER
                && type != FolderType.SIDE_PANEL
                && type != FolderType.MANAGER)
        {
            JMenuItem rename = new JMenuItem("Rename");
            rename.addActionListener(e -> showRenamePopup(folder.getLinkedManagerNode(), x, y));
            popup.add(rename);
        }

        popup.show(this, x, y);
    }

    private void showRenamePopup(DefaultMutableTreeNode node, int x, int y)
    {
        JPopupMenu popup = new JPopupMenu();

        JLabel title = new JLabel("Rename");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setBorder(new EmptyBorder(2, 2, 2, 2));
        popup.add(title);
        popup.addSeparator();

        Object o = node.getUserObject();

        JTextField textField = new JTextField();
        textField.setColumns(25);

        textField.addActionListener(e ->
        {
            String text = textField.getText();
            text = StringHandler.cleanString(text);

            if (o instanceof Character)
            {
                Character character = (Character) o;
                character.getNameField().setText(text);
                plugin.getCreatorsPanel().onNameTextFieldChanged(character);
            }

            if (o instanceof Folder)
            {
                Folder folder = (Folder) o;
                folder.setName(text);
            }

            treeModel.nodeChanged(node);
        });

        popup.add(textField);
        popup.show(this, x, y);
    }
}

