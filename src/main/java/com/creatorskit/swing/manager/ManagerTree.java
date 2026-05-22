package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.swing.*;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final SelectionManager selectionManager;
    private final JPanel objectHolder;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode sidePanelNode;
    private final DefaultMutableTreeNode managerNode;
    private final ManagerTreeModel treeModel;
    private final GridBagConstraints c = new GridBagConstraints();
    private Folder[] selectedFolders = new Folder[0];
    private boolean syncingFromSelectionManager = false;
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

        setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));

        rootNode.add(sidePanelNode);
        rootNode.add(managerNode);

        treeModel = new ManagerTreeModel(rootNode, sidePanelNode, managerNode, plugin);
        setModel(treeModel);
        expandRow(0);
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setEditable(false);
        setOpaque(false);
        setRowHeight(ROW_HEIGHT);
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        setShowsRootHandles(true);
        setRootVisible(false);
        setDragEnabled(false);
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
        int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to create a new Setup file? All unsaved changes will be lost");
        if (result != JOptionPane.YES_OPTION)
        {
            return;
        }

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

            // JTree.getCellEditor() returns null when reorder/inline-rename mode
            // is off (the common case). Without the null guard this lambda NPEs
            // after every Delete -- the rest of the tree-mutation work
            // succeeded so the error message wasn't obviously fatal, but it
            // bubbled up and short-circuited any downstream listeners.
            javax.swing.tree.TreeCellEditor editor = getCellEditor();
            if (editor != null)
            {
                editor.cancelCellEditing();
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

    /**
     * Walks every descendant of {@code parent} and appends each child's TreePath to
     * {@code out}. Used to visually highlight every subfolder + Character node in
     * the tree when the user clicks a parent folder. Returns true if at least one
     * descendant was added.
     */
    private boolean collectDescendantPaths(DefaultMutableTreeNode parent, ArrayList<TreePath> out)
    {
        boolean added = false;
        Enumeration<TreeNode> children = parent.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            TreePath childPath = new TreePath(child.getPath());
            if (!out.contains(childPath))
            {
                out.add(childPath);
                added = true;
            }
            if (!child.isLeaf())
            {
                added |= collectDescendantPaths(child, out);
            }
        }
        return added;
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
                if (!syncingFromSelectionManager)
                {
                    selectionManager.clear();
                }
                return;
            }

            updateTreeSelectionIndex();

            if (!syncingFromSelectionManager)
            {
                ArrayList<Character> selectedChars = new ArrayList<>();
                for (TreePath treePath : treePaths)
                {
                    Object component = treePath.getLastPathComponent();
                    if (!(component instanceof DefaultMutableTreeNode))
                    {
                        continue;
                    }
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) component;
                    Object user = node.getUserObject();
                    if (user instanceof Character)
                    {
                        Character character = (Character) user;
                        if (!selectedChars.contains(character))
                        {
                            selectedChars.add(character);
                        }
                    }
                    else if (user instanceof Folder)
                    {
                        // Folder selection = select every Character recursively under it
                        // (drives selectionManager / keyframe fan-out), but DON'T
                        // expand the folder visually. Previously the listener also
                        // collected every descendant TreePath and called
                        // setSelectionPaths() which has the side effect of expanding
                        // each path -- folder click silently exploded the tree open.
                        // The user can still expand manually via the disclosure
                        // triangle or right-click > Expand.
                        getObjectPanelChildren(node, selectedChars);
                    }
                }
                selectionManager.selectAll(selectedChars);
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

        resetObjectHolder(objectPanels, false);
    }

    public void resetObjectHolder(ArrayList<Character> charactersToAdd, boolean sidePanel)
    {
        resetObjectHolder(charactersToAdd.toArray(new Character[charactersToAdd.size()]), sidePanel);
    }

    public void resetObjectHolder(Character[] charactersToAdd, boolean sidePanel)
    {
        JPanel[] panelsToAdd = new JPanel[charactersToAdd.length];

        for (int i = 0; i < charactersToAdd.length; i++)
        {
            panelsToAdd[i] = charactersToAdd[i].getObjectPanel();
        }
        resetObjectHolder(panelsToAdd, sidePanel);
    }

    public void resetObjectHolder(JPanel[] panelsToAdd, boolean sidePanel)
    {
        if (sidePanel)
        {
            objectHolder.removeAll();
            objectHolder.revalidate();
            return;
        }

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

    public void setTreeSelection(Character character)
    {
        if (character == null)
        {
            updateTreeSelectionIndex();
            return;
        }

        TreePath treePath = new TreePath(character.getLinkedManagerNode().getPath());
        setSelectionPath(treePath);
        scrollPathToVisible(treePath);
        updateTreeSelectionIndex();
    }

    /**
     * Reorder mode toggles drag-to-reorder and inline-rename together. Disabled by
     * default so misclicks between rows don't accidentally reorder or rename Characters.
     */
    public void setReorderMode(boolean enabled)
    {
        setEditable(enabled);
        setDragEnabled(enabled);
    }

    public boolean isReorderMode()
    {
        return isEditable();
    }

    /**
     * Updates the tree's visual selection to match SelectionManager. Called from sidebar
     * click handlers so the tree reflects multi-selection done elsewhere. The
     * syncingFromSelectionManager flag prevents the resulting TreeSelectionEvent from
     * pushing the same state back into SelectionManager.
     */
    public void syncTreeFromSelection()
    {
        syncingFromSelectionManager = true;
        try
        {
            java.util.Set<Character> selected = selectionManager.getSelected();
            if (selected.isEmpty())
            {
                clearSelection();
                return;
            }

            TreePath[] paths = new TreePath[selected.size()];
            int i = 0;
            for (Character c : selected)
            {
                DefaultMutableTreeNode node = c.getLinkedManagerNode();
                if (node != null)
                {
                    paths[i++] = new TreePath(node.getPath());
                }
            }
            if (i < paths.length)
            {
                paths = Arrays.copyOf(paths, i);
            }
            setSelectionPaths(paths);
            if (paths.length > 0)
            {
                scrollPathToVisible(paths[paths.length - 1]);
            }
            updateTreeSelectionIndex();
        }
        finally
        {
            syncingFromSelectionManager = false;
        }
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
        Object user = node.getUserObject();

        if (user instanceof Character)
        {
            showCharacterContextMenu(node, (Character) user, x, y);
            return;
        }

        if (user instanceof Folder)
        {
            showFolderContextMenu(node, (Folder) user, x, y);
        }
    }

    /**
     * Right-click context menu for a Character node in the tree. Shows the existing
     * keyframe-summary submenu plus a "Recolour" submenu that operates on the current
     * SelectionManager state (so multi-select recolour works from the tree).
     */
    private void showCharacterContextMenu(DefaultMutableTreeNode characterNode, Character character, int x, int y)
    {
        if (!selectionManager.isSelected(character))
        {
            selectionManager.select(character);
            syncTreeFromSelection();
        }

        int count = selectionManager.size();
        JPopupMenu popup = new JPopupMenu();

        JLabel title = new JLabel(count > 1
                ? count + " Characters selected"
                : character.getName());
        title.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());
        title.setBorder(new javax.swing.border.EmptyBorder(2, 6, 2, 6));
        popup.add(title);
        popup.addSeparator();

        JMenuItem rename = new JMenuItem("Rename...");
        rename.setToolTipText("Edit this Character's display name.");
        rename.addActionListener(e -> promptRenameCharacter(character));
        popup.add(rename);

        JMenuItem recolour = new JMenuItem("Recolour...");
        recolour.addActionListener(e ->
                plugin.getCreatorsPanel().showColorPickerAt(this, x, y, character));
        popup.add(recolour);

        JMenuItem keyframes = new JMenuItem("Show keyframes...");
        keyframes.addActionListener(e ->
                toolBox.getTimeSheetPanel().getSummarySheet().showSummaryPopup(this, character, x, y));
        popup.add(keyframes);

        // Camera lock toggle. Checked state reflects whatever the plugin currently has
        // locked, and the action just delegates to the plugin which centralises the
        // mutual-exclusion + sidebar-checkbox sync.
        JCheckBoxMenuItem cameraLock = new JCheckBoxMenuItem("Lock camera to this character");
        cameraLock.setSelected(plugin.getCameraLockedCharacter() == character);
        cameraLock.setToolTipText("<html>Locks the camera onto this Character so it follows them"
                + " through movement keyframes,<br>just like the default game camera follows the local"
                + " player. Re-click to release.</html>");
        cameraLock.addActionListener(e -> plugin.setCameraLockedCharacter(character));
        popup.add(cameraLock);

        popup.addSeparator();

        // Collapse the Character's immediate parent folder. Only enabled
        // when the parent is actually a Folder node (Characters sitting at
        // the root have no folder to collapse). Useful after finishing a
        // pass over one folder's Characters and wanting to fold the whole
        // group away without right-clicking the folder node itself.
        DefaultMutableTreeNode parent = characterNode == null ? null
                : (DefaultMutableTreeNode) characterNode.getParent();
        boolean parentIsFolder = parent != null && parent.getUserObject() instanceof Folder;
        JMenuItem collapseParent = new JMenuItem("Collapse parent folder");
        collapseParent.setToolTipText(parentIsFolder
                ? "Close \"" + ((Folder) parent.getUserObject()).getName()
                        + "\" and every nested subfolder."
                : "This Character sits at the tree root -- no parent folder to collapse.");
        collapseParent.setEnabled(parentIsFolder);
        if (parentIsFolder)
        {
            final DefaultMutableTreeNode folderToCollapse = parent;
            collapseParent.addActionListener(e -> setFolderExpanded(folderToCollapse, false));
        }
        popup.add(collapseParent);

        JMenuItem deselect = new JMenuItem("Deselect");
        deselect.setToolTipText("Clear the current selection. Useful for stepping out of multi-select without picking another Character.");
        deselect.addActionListener(e ->
        {
            selectionManager.clear();
            // Drop the tree's own selection too so the row's blue background goes
            // away alongside the SelectionManager state. Without this the JTree
            // would still show the previous selection highlight even though our
            // logical selection is empty.
            clearSelection();
        });
        popup.add(deselect);

        popup.show(this, x, y);
    }

    /**
     * Pops a small input dialog prefilled with the Character's current name,
     * then routes the entered value through the same path the sidebar name
     * textfield uses ({@code onNameTextFieldChanged}) so the tree node label,
     * AttributePanel header, and sidebar input all refresh consistently. Null
     * dialog return = user cancelled; empty/whitespace input is rejected so a
     * fat-fingered Enter doesn't wipe the name.
     */
    private void promptRenameCharacter(Character character)
    {
        if (character == null) return;
        String current = character.getName();
        String entered = (String) JOptionPane.showInputDialog(
                this,
                "New name for this Character:",
                "Rename",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                current);

        if (entered == null) return; // cancelled
        String trimmed = entered.trim();
        if (trimmed.isEmpty()) return;
        if (trimmed.equals(current)) return; // no-op

        javax.swing.JTextField nameField = character.getNameField();
        if (nameField == null)
        {
            // No sidebar textfield (e.g. Character displayed only in manager
            // tree): set the model directly and nudge dependent UI manually.
            character.setName(trimmed);
            treeModel.nodeChanged(character.getLinkedManagerNode());
            toolBox.getTimeSheetPanel().getAttributePanel().updateObjectLabel(character);
            return;
        }
        // Going through the textfield path lets CreatorsPanel run its existing
        // cleanString / setName / tree-node / AttributePanel refresh chain.
        nameField.setText(trimmed);
        plugin.getCreatorsPanel().onNameTextFieldChanged(character);
    }

    /**
     * Folder analog of {@link #promptRenameCharacter}. Folders have no sidebar
     * textfield to route through, so we set the model directly and ask the tree
     * model to refresh the node label -- the renderer just re-reads
     * Folder.toString() (= name) on the next paint.
     */
    private void promptRenameFolder(DefaultMutableTreeNode folderNode, Folder folder)
    {
        if (folder == null) return;
        String current = folder.getName();
        String entered = (String) JOptionPane.showInputDialog(
                this,
                "New name for this Folder:",
                "Rename",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                current);

        if (entered == null) return; // cancelled
        String trimmed = entered.trim();
        if (trimmed.isEmpty()) return;
        if (trimmed.equals(current)) return; // no-op

        folder.setName(trimmed);
        treeModel.nodeChanged(folderNode);
    }

    /**
     * Right-click context menu for a Folder node. Currently just exposes the
     * "Select N random Characters..." action, which picks N direct child
     * Characters at random (no recursion into subfolders) and replaces the
     * current selection with them. Useful for sampling a subset out of a large
     * folder when fanning out keyframes -- pick the folder, choose N, then
     * any subsequent add / paste / Update applies to those N via
     * resolveSelectionTargets.
     */
    private void showFolderContextMenu(DefaultMutableTreeNode folderNode, Folder folder, int x, int y)
    {
        java.util.List<Character> directChildren = new ArrayList<>();
        Enumeration<TreeNode> children = folderNode.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (child.getUserObject() instanceof Character)
            {
                directChildren.add((Character) child.getUserObject());
            }
        }

        JPopupMenu popup = new JPopupMenu();

        JLabel title = new JLabel(folder.getName() + "  (" + directChildren.size() + " direct)");
        title.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());
        title.setBorder(new javax.swing.border.EmptyBorder(2, 6, 2, 6));
        popup.add(title);
        popup.addSeparator();

        JMenuItem rename = new JMenuItem("Rename...");
        rename.setToolTipText("Edit this folder's display name.");
        rename.addActionListener(e -> promptRenameFolder(folderNode, folder));
        popup.add(rename);

        popup.addSeparator();

        JMenuItem randomSelect = new JMenuItem("Select N random Characters...");
        randomSelect.setToolTipText("<html>Replace current selection with N randomly-picked direct children<br>"
                + "of this folder (subfolders ignored). Useful for sampling a subset<br>"
                + "before fanning out a keyframe edit.</html>");
        if (directChildren.isEmpty())
        {
            randomSelect.setEnabled(false);
        }
        else
        {
            randomSelect.addActionListener(e -> promptAndRandomSelect(directChildren));
        }
        popup.add(randomSelect);

        popup.addSeparator();

        // Collapse / Expand operate on this folder + every descendant folder.
        // Collapse closes the clicked folder so its children disappear from view
        // (useful for huge folders the user just finished editing). Expand opens
        // the clicked folder and every subfolder beneath it (useful when
        // exploring a deeply-nested setup). Uses the JTree expand/collapse API
        // directly so the visual state matches what right-click-then-arrow-keys
        // would produce.
        JMenuItem collapse = new JMenuItem("Collapse");
        collapse.setToolTipText("Close this folder and every nested subfolder.");
        collapse.addActionListener(e -> setFolderExpanded(folderNode, false));
        popup.add(collapse);

        JMenuItem expand = new JMenuItem("Expand");
        expand.setToolTipText("Open this folder and every nested subfolder.");
        expand.addActionListener(e -> setFolderExpanded(folderNode, true));
        popup.add(expand);

        popup.show(this, x, y);
    }

    /**
     * Collapses every Folder node under the manager / side-panel roots. Used by
     * the load path when the user has the collapse-on-load config toggle on so
     * big scenes don't open with the whole hierarchy exploded.
     */
    public void collapseAllFolders()
    {
        setFolderExpanded(managerNode, false);
        setFolderExpanded(sidePanelNode, false);
    }

    /**
     * Recursively expand or collapse a folder subtree. JTree.expandPath /
     * collapsePath only operate on the path itself, so we walk descendant
     * folder nodes ourselves. For expand we traverse top-down (parent before
     * children) so each child's path resolves; for collapse we traverse
     * bottom-up so we don't try to collapse a child whose parent we already
     * collapsed (no-op but cleaner).
     */
    private void setFolderExpanded(DefaultMutableTreeNode node, boolean expand)
    {
        if (node == null)
        {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        if (expand)
        {
            expandPath(path);
            Enumeration<TreeNode> children = node.children();
            while (children.hasMoreElements())
            {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                if (child.getUserObject() instanceof Folder)
                {
                    setFolderExpanded(child, true);
                }
            }
        }
        else
        {
            Enumeration<TreeNode> children = node.children();
            while (children.hasMoreElements())
            {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                if (child.getUserObject() instanceof Folder)
                {
                    setFolderExpanded(child, false);
                }
            }
            collapsePath(path);
        }
    }

    /**
     * Menu-driven entry point (Tools > Random > Random Select) into the same
     * flow as the folder right-click. Uses the first currently-selected folder
     * as the pool. If no folder is selected, shows a one-line warning and
     * returns -- the user just needs to click a folder in the tree first.
     */
    public void selectRandomFromActiveFolder()
    {
        if (selectedFolders == null || selectedFolders.length == 0)
        {
            JOptionPane.showMessageDialog(this,
                    "Click a folder in the Manager Tree first.",
                    "Random Select",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Folder folder = selectedFolders[0];
        DefaultMutableTreeNode folderNode = folder.getLinkedManagerNode();
        if (folderNode == null)
        {
            return;
        }

        java.util.List<Character> directChildren = new ArrayList<>();
        Enumeration<TreeNode> children = folderNode.children();
        while (children.hasMoreElements())
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (child.getUserObject() instanceof Character)
            {
                directChildren.add((Character) child.getUserObject());
            }
        }

        if (directChildren.isEmpty())
        {
            JOptionPane.showMessageDialog(this,
                    "Folder '" + folder.getName() + "' has no direct Characters.",
                    "Random Select",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        promptAndRandomSelect(directChildren);
    }

    /**
     * Pops up a small input dialog asking how many Characters to pick, then
     * replaces the SelectionManager with that many random entries from
     * {@code pool}. Clamps the requested count to the pool size and rejects
     * non-positive / non-numeric input silently (dialog stays cancelable).
     */
    private void promptAndRandomSelect(java.util.List<Character> pool)
    {
        if (pool.isEmpty())
        {
            return;
        }

        String input = (String) JOptionPane.showInputDialog(
                this,
                "How many Characters to randomly select?\n(folder has " + pool.size() + ")",
                "Random select",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                Integer.toString(Math.min(5, pool.size())));

        if (input == null)
        {
            return; // cancelled
        }

        int n;
        try
        {
            n = Integer.parseInt(input.trim());
        }
        catch (NumberFormatException ex)
        {
            return;
        }

        if (n <= 0)
        {
            return;
        }
        if (n > pool.size())
        {
            n = pool.size();
        }

        // Fisher-Yates shuffle on a local copy, then take the first n. Avoids
        // the Collections.shuffle-then-sublist allocation churn and gives an
        // unbiased uniform sample. Random is freshly seeded per call so two
        // consecutive picks on the same folder produce different subsets.
        java.util.List<Character> shuffled = new ArrayList<>(pool);
        java.util.Random rng = new java.util.Random();
        for (int i = shuffled.size() - 1; i > 0; i--)
        {
            int j = rng.nextInt(i + 1);
            Character tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        selectionManager.selectAll(shuffled.subList(0, n));
        syncTreeFromSelection();
    }
}

