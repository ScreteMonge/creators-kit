package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.Folder;
import com.creatorskit.swing.StringHandler;
import com.creatorskit.swing.ToolBoxFrame;

import javax.inject.Inject;
import javax.swing.tree.*;

public class ManagerTreeModel extends DefaultTreeModel
{
    private final CreatorsPlugin plugin;
    private final DefaultMutableTreeNode sidePanelNode;
    private final DefaultMutableTreeNode managerNode;

    @Inject
    public ManagerTreeModel(TreeNode root, DefaultMutableTreeNode sidePanelNode, DefaultMutableTreeNode managerNode, CreatorsPlugin plugin)
    {
        super(root);
        this.plugin = plugin;
        this.sidePanelNode = sidePanelNode;
        this.managerNode = managerNode;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node == root
        || node == sidePanelNode
        || node == managerNode)
        {
            return;
        }

        ToolBoxFrame toolBox = plugin.getCreatorsPanel().getToolBox();

        //Check if node being modified is a Folder
        if (node.getUserObject() instanceof Folder)
        {
            Folder folder = (Folder) node.getUserObject();
            String name = StringHandler.cleanString((String) newValue);
            folder.setName(name);
            node.setUserObject(folder);
            nodeChanged(node);
            toolBox.getManagerPanel().getObjectLabel().setText("Current Folder: " + name);
        }

        //Check if node being modified is an ObjectPanel
        if (node.getUserObject() instanceof Character)
        {
            Character character = (Character) node.getUserObject();
            String name = StringHandler.cleanString((String) newValue);
            character.setName(name);
            character.getNameField().setText(name);
            node.setUserObject(character);
            nodeChanged(node);
            toolBox.getTimeSheetPanel().getAttributePanel().updateObjectLabel(character);
        }
    }
}
