package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.*;
import com.creatorskit.swing.timesheet.TimeTree;
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
    private final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode sidePanelNode;
    private final DefaultMutableTreeNode managerNode;
    private final ManagerTreeModel treeModel;
    private final TimeTree timeTree;
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
    public ManagerTree(ToolBoxFrame toolBox, CreatorsPlugin plugin, DefaultMutableTreeNode rootNode, DefaultMutableTreeNode sidePanelNode, DefaultMutableTreeNode managerNode, TimeTree timeTree)
    {
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.rootNode = rootNode;
        this.sidePanelNode = sidePanelNode;
        this.managerNode = managerNode;
        this.timeTree = timeTree;

        setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        setPreferredSize(new Dimension(300, 0));

        treeModel = new ManagerTreeModel(rootNode, sidePanelNode, managerNode, plugin);
        tree = new JTree(treeModel);
        tree.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setEditable(true);
        tree.setRowHeight(20);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new ManagerTreeTransferHandler(timeTree));
        tree.addTreeSelectionListener(new MyTreeSelectionListener());
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(new ImageIcon(FOLDER_OPEN));
        renderer.setClosedIcon(new ImageIcon(FOLDER_CLOSED));
        renderer.setLeafIcon(new ImageIcon(OBJECT));
        renderer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        tree.setCellRenderer(renderer);

        rootNode.add(sidePanelNode);
        rootNode.add(managerNode);

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
        addFolderButton.addActionListener(e -> addFolderNode("New Folder"));
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

    public DefaultMutableTreeNode getParentNode()
    {
        DefaultMutableTreeNode parentNode;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null)
        {
            parentNode = managerNode;
        }
        else
        {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
            if (parentNode.getUserObject() instanceof Character)
            {
                Character character = (Character) parentNode.getUserObject();
                parentNode = character.getParentManagerNode();
                System.out.println("Character");
            }

            if (parentNode == null)
            {
                parentNode = managerNode;
            }
        }

        return parentNode;
    }

    public DefaultMutableTreeNode addFolderNode(String name)
    {
        return addFolderNode(getParentNode(), name, true);
    }

    public DefaultMutableTreeNode addFolderNode(DefaultMutableTreeNode parent, String name)
    {
        return addFolderNode(parent, name, false);
    }

    public DefaultMutableTreeNode addFolderNode(DefaultMutableTreeNode managerParent, String name, boolean shouldBeVisible)
    {
        if (managerParent == rootNode || managerParent == sidePanelNode)
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

    public DefaultMutableTreeNode addCharacterNode(Character character)
    {
        return addCharacterNode(getParentNode(), character, true);
    }

    public DefaultMutableTreeNode addCharacterNode(DefaultMutableTreeNode parent, Character character)
    {
        return addCharacterNode(parent, character, false);
    }

    public DefaultMutableTreeNode addCharacterNode(DefaultMutableTreeNode managerParent, Character character, boolean shouldBeVisible)
    {
        if (managerParent == rootNode || managerParent == sidePanelNode)
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

    public void removeAllNodes()
    {
        TreePath[] treePaths = new TreePath[]{tree.getPathForRow(0)};
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
            return;

        if (!foldersToRemove.isEmpty() && !charactersToRemove.isEmpty())
        {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all the selected Folders and their Objects?");
            if (result != JOptionPane.YES_OPTION)
                return;
        }

        Character[] characters = new Character[charactersToRemove.size()];
        for (int i = 0; i < characters.length; i++)
        {
            Character character = (Character) charactersToRemove.get(i).getUserObject();
            characters[i] = character;
        }

        plugin.getCreatorsPanel().deleteCharacters(characters);

        for (DefaultMutableTreeNode node : foldersToRemove)
            removeFolderNode(node);
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

            ArrayList<Character> panelsToAdd = new ArrayList<>();

            boolean folderSelected = false;
            String folderName = "";
            for (TreePath treePath : treePaths)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                if (node.getUserObject() instanceof Folder)
                {
                    if (folderName.isEmpty())
                        folderName = ((Folder) node.getUserObject()).getName();
                    folderSelected = true;
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
                folderName = ((Folder) folderNode.getUserObject()).getName();
                getObjectPanelChildren(folderNode, panelsToAdd);
            }

            toolBox.getManagerPanel().getObjectLabel().setText("Current Folder: " + folderName);
            resetObjectHolder(panelsToAdd);
        }
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
        ObjectPanel[] objectPanels = new ObjectPanel[0];
        JPanel objectHolder = toolBox.getManagerPanel().getObjectHolder();
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

