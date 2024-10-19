package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.*;
import com.creatorskit.swing.timesheet.TimeTree;
import com.formdev.flatlaf.FlatClientProperties;
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
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

@Getter
public class ManagerTree extends JScrollPane
{
    private final ToolBoxFrame toolBox;
    private final CreatorsPlugin plugin;
    private final JPanel objectHolder;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode sidePanelNode;
    private final DefaultMutableTreeNode managerNode;
    private final ManagerTreeModel treeModel;
    private final TimeTree timeTree;
    private final JButton[] headerButtons = new JButton[3];
    private final JTree tree;
    private final GridBagConstraints c = new GridBagConstraints();
    private Folder[] selectedFolders = new Folder[0];

    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage ADD = ImageUtil.loadImageResource(getClass(), "/Add.png");
    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");
    private final BufferedImage FOLDER_OPEN = ImageUtil.loadImageResource(getClass(), "/Folder_Open.png");
    private final BufferedImage FOLDER_CLOSED = ImageUtil.loadImageResource(getClass(), "/Folder_Closed.png");
    private final BufferedImage OBJECT = ImageUtil.loadImageResource(getClass(), "/Object.png");

    @Inject
    public ManagerTree(ToolBoxFrame toolBox, CreatorsPlugin plugin, JPanel objectHolder, DefaultMutableTreeNode rootNode, DefaultMutableTreeNode sidePanelNode, DefaultMutableTreeNode managerNode, TimeTree timeTree)
    {
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.objectHolder = objectHolder;
        this.rootNode = rootNode;
        this.sidePanelNode = sidePanelNode;
        this.managerNode = managerNode;
        this.timeTree = timeTree;

        setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        setPreferredSize(new Dimension(300, 0));

        rootNode.add(sidePanelNode);
        rootNode.add(managerNode);

        treeModel = new ManagerTreeModel(rootNode, sidePanelNode, managerNode, plugin);
        tree = new JTree(treeModel);
        tree.expandRow(0);
        tree.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tree.setEditable(true);
        tree.setRowHeight(20);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new ManagerTreeTransferHandler(plugin, toolBox, timeTree));
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
        folderLabel.setFont(FontManager.getRunescapeBoldFont());
        folderHeader.add(folderLabel, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JButton addFolderButton = new JButton(new ImageIcon(ADD));
        addFolderButton.setToolTipText("Add a new Folder to the currently selected Folder");
        addFolderButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        addFolderButton.addActionListener(e ->
        {
            TreePath path = tree.getSelectionPath();
            DefaultMutableTreeNode folderNode;
            if (path == null)
            {
                folderNode = addFolderNode("New Folder", ParentPanel.MANAGER);
            }
            else
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                ParentPanel parentPanel = treeContainsSidePanel(node) ? ParentPanel.SIDE_PANEL : ParentPanel.MANAGER;
                folderNode = addFolderNode("New Folder", parentPanel);
            }

            TreePath treePath = new TreePath(folderNode.getPath());
            int row = tree.getRowForPath(new TreePath(folderNode.getPath()));
            tree.expandRow(row);
            tree.setSelectionPath(treePath);
        });
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
        clearFolderButton.addActionListener(e ->
        {
            Thread thread = new Thread(this::removeAllNodes);
            thread.start();
        });
        folderHeader.add(clearFolderButton, c);

        headerButtons[0] = addFolderButton;
        headerButtons[1] = removeFolderButton;
        headerButtons[2] = clearFolderButton;

        return folderHeader;
    }

    public DefaultMutableTreeNode getParentNode(ParentPanel parentPanel)
    {
        DefaultMutableTreeNode defaultNode = parentPanel == ParentPanel.MANAGER ? managerNode : sidePanelNode;
        DefaultMutableTreeNode parentNode;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null)
        {
            parentNode = defaultNode;
        }
        else
        {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
            if (parentNode.getUserObject() instanceof Character)
            {
                Character character = (Character) parentNode.getUserObject();
                parentNode = character.getParentManagerNode();
            }

            if (parentNode == null)
            {
                parentNode = defaultNode;
            }
        }

        return parentNode;
    }

    public DefaultMutableTreeNode addFolderNode(String name, ParentPanel parentPanel)
    {
        return addFolderNode(getParentNode(parentPanel), name, true);
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

        Folder parentFolder = (Folder) managerParent.getUserObject();
        DefaultMutableTreeNode timeTreeParent = parentFolder.getLinkedTimeSheetNode();

        Folder folder = new Folder(name, FolderType.STANDARD, null, null, managerParent, timeTreeParent);
        DefaultMutableTreeNode linkedManagerNode = new DefaultMutableTreeNode(folder);
        DefaultMutableTreeNode linkedTimeTreeNode = new DefaultMutableTreeNode(folder);
        folder.setLinkedManagerNode(linkedManagerNode);
        folder.setLinkedTimeSheetNode(linkedTimeTreeNode);

        treeModel.insertNodeInto(linkedManagerNode, managerParent, managerParent.getChildCount());
        timeTree.getTreeModel().insertNodeInto(linkedTimeTreeNode, timeTreeParent, timeTreeParent.getChildCount());

        if (shouldBeVisible)
            tree.scrollPathToVisible(new TreePath(linkedManagerNode.getPath()));

        return linkedManagerNode;
    }

    /**
     * Adds a character node to the currently selected ManagerTree node. Creates a linked ManagerTree and TimeTree node and remembers the parent node of each
     * @param character the character to add to the selected node
     * @param parentPanel the ParentPanel to which the character will be added (SidePanel or ManagerPanel)
     * @param shouldBeVisible visibility
     * @return the linked ManagerTree node
     */
    public DefaultMutableTreeNode addCharacterNode(Character character, ParentPanel parentPanel, boolean shouldBeVisible)
    {
        if (parentPanel == ParentPanel.SIDE_PANEL)
        {
            return addCharacterNode(sidePanelNode, character, parentPanel, shouldBeVisible);
        }

        return addCharacterNode(getParentNode(parentPanel), character, parentPanel, shouldBeVisible);
    }

    // Adds the character to a previously indicated parent
    public DefaultMutableTreeNode addCharacterNode(DefaultMutableTreeNode parent, Character character, ParentPanel parentPanel)
    {
        return addCharacterNode(parent, character, parentPanel, false);
    }

    /**
     * Adds a character node to the indicated ManagerTree node. Creates a linked ManagerTree and TimeTree node and remembers the parent node of each
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

        Folder parentFolder = (Folder) managerParent.getUserObject();
        DefaultMutableTreeNode timeTreeParent = parentFolder.getParentTimeSheetNode();

        DefaultMutableTreeNode linkedManagerNode = new DefaultMutableTreeNode(character);
        DefaultMutableTreeNode linkedTimeTreeNode = new DefaultMutableTreeNode(character);
        character.setLinkedManagerNode(linkedManagerNode);
        character.setParentManagerNode(managerParent);
        character.setLinkedTimeSheetNode(linkedTimeTreeNode);
        character.setParentTimeSheetNode(timeTreeParent);

        treeModel.insertNodeInto(linkedManagerNode, managerParent, managerParent.getChildCount());

        if (shouldBeVisible)
            tree.scrollPathToVisible(new TreePath(linkedManagerNode.getPath()));

        return linkedManagerNode;
    }

    public void switchFolders(Character character)
    {
        DefaultMutableTreeNode newParent;

        if (character.getParentPanel() == ParentPanel.MANAGER)
        {
            newParent = sidePanelNode;
        }
        else
        {
            TreePath treePath = tree.getSelectionPath();
            if (treePath == null)
            {
                newParent = managerNode;
            }
            else
            {
                newParent = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                if (treeContainsSidePanel(newParent))
                {
                    newParent = managerNode;
                }
                else if (newParent.getUserObject() instanceof Character)
                {
                    TreePath parentPath = treePath.getParentPath();
                    if (parentPath == null)
                    {
                        return;
                    }

                    newParent = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                }
            }
        }

        DefaultMutableTreeNode linkedManagerNode = character.getLinkedManagerNode();

        ArrayList<Character> listFrom;
        ParentPanel parentPanelFrom = character.getParentPanel();
        if (parentPanelFrom == ParentPanel.MANAGER)
        {
            listFrom = toolBox.getManagerPanel().getManagerCharacters();
        }
        else
        {
            listFrom = plugin.getCreatorsPanel().getSidePanelCharacters();
        }

        ParentPanel parentPanelTo = treeContainsSidePanel(newParent) ? ParentPanel.SIDE_PANEL : ParentPanel.MANAGER;

        ArrayList<Character> listTo;
        if (parentPanelTo == ParentPanel.MANAGER)
        {
            listTo = toolBox.getManagerPanel().getManagerCharacters();
        }
        else
        {
            listTo = plugin.getCreatorsPanel().getSidePanelCharacters();
        }

        listFrom.remove(character);
        listTo.add(character);

        character.setParentPanel(parentPanelTo);
        character.setParentManagerNode(newParent);
        treeModel.removeNodeFromParent(linkedManagerNode);
        treeModel.insertNodeInto(linkedManagerNode, newParent, linkedManagerNode.getChildCount());
    }

    public void switchFolders(Character[] characters, ParentPanel parentPanelFrom)
    {
        DefaultMutableTreeNode newParent;

        if (parentPanelFrom == ParentPanel.MANAGER)
        {
            newParent = sidePanelNode;
        }
        else
        {
            TreePath treePath = tree.getSelectionPath();
            if (treePath == null)
            {
                newParent = managerNode;
            }
            else
            {
                newParent = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                if (treeContainsSidePanel(newParent))
                {
                    newParent = managerNode;
                }
                else if (newParent.getUserObject() instanceof Character)
                {
                    TreePath parentPath = treePath.getParentPath();
                    if (parentPath == null)
                    {
                        return;
                    }

                    newParent = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                }
            }
        }


        ArrayList<Character> listFrom;
        if (parentPanelFrom == ParentPanel.MANAGER)
        {
            listFrom = toolBox.getManagerPanel().getManagerCharacters();
        }
        else
        {
            listFrom = plugin.getCreatorsPanel().getSidePanelCharacters();
        }

        ParentPanel parentPanelTo = treeContainsSidePanel(newParent) ? ParentPanel.SIDE_PANEL : ParentPanel.MANAGER;
        ArrayList<Character> listTo;
        if (parentPanelTo == ParentPanel.MANAGER)
        {
            listTo = toolBox.getManagerPanel().getManagerCharacters();
        }
        else
        {
            listTo = plugin.getCreatorsPanel().getSidePanelCharacters();
        }

        DefaultMutableTreeNode finalNewParent = newParent;
        Thread thread = new Thread(() ->
        {
            for (int i = characters.length - 1; i >= 0; i--)
            {
                Character character = characters[i];
                DefaultMutableTreeNode linkedManagerNode = character.getLinkedManagerNode();

                listFrom.remove(character);
                listTo.add(character);

                character.setParentPanel(parentPanelTo);
                character.setParentManagerNode(finalNewParent);
                treeModel.removeNodeFromParent(linkedManagerNode);
                treeModel.insertNodeInto(linkedManagerNode, finalNewParent, linkedManagerNode.getChildCount());
            }

            toolBox.getManagerPanel().getManagerTree().resetObjectHolder();
            plugin.getCreatorsPanel().resetSidePanel();
        });
        thread.start();
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
        timeTree.getTreeModel().removeNodeFromParent(child.getLinkedTimeSheetNode());
    }

    public void removeFolderNode(DefaultMutableTreeNode folderNode)
    {
        treeModel.removeNodeFromParent(folderNode);

        Folder folder = (Folder) folderNode.getUserObject();
        timeTree.getTreeModel().removeNodeFromParent(folder.getLinkedTimeSheetNode());
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

    private void getCharacterNodeChildren(DefaultMutableTreeNode parent, ArrayList<Character> characters)
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
            TreePath[] treePaths = tree.getSelectionPaths();
            if (treePaths == null)
                return;

            JPanel objectHolder = toolBox.getManagerPanel().getObjectHolder();

            for (TreePath treePath : treePaths)
            {
                if (treePath.getLastPathComponent() == rootNode)
                {
                    objectHolder.removeAll();
                    objectHolder.revalidate();
                    return;
                }

                boolean containsSidePanel = treeContainsSidePanel((TreeNode) treePath.getLastPathComponent());
                if (containsSidePanel)
                {
                    objectHolder.removeAll();
                    objectHolder.revalidate();
                    return;
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
            resetObjectHolder(panelsToAdd);
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

        resetObjectHolder(objectPanels);
    }

    public void resetObjectHolder(ArrayList<Character> charactersToAdd)
    {
        resetObjectHolder(charactersToAdd.toArray(new Character[charactersToAdd.size()]));
    }

    public void resetObjectHolder(Character[] charactersToAdd)
    {
        ObjectPanel[] panelsToAdd = new ObjectPanel[charactersToAdd.length];

        for (int i = 0; i < charactersToAdd.length; i++)
        {
            panelsToAdd[i] = charactersToAdd[i].getObjectPanel();
        }
        resetObjectHolder(panelsToAdd);
    }

    public void resetObjectHolder(ObjectPanel[] panelsToAdd)
    {
        JPanel[] programPanels = new JPanel[0];

        for (ObjectPanel objectPanel : panelsToAdd)
            programPanels = ArrayUtils.add(programPanels, objectPanel.getProgramPanel());

        resetProgramHolder(programPanels);

        JPanel objectHolder = toolBox.getManagerPanel().getObjectHolder();
        objectHolder.removeAll();

        for (ObjectPanel objectPanel : panelsToAdd)
        {
            objectHolder.add(objectPanel);
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
}

