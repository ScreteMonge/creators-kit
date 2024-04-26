package com.creatorskit.swing.jtree;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.Folder;
import com.creatorskit.swing.StringHandler;

import javax.inject.Inject;
import javax.swing.tree.*;

public class CreatorTreeModel extends DefaultTreeModel
{
    private final CreatorsPlugin plugin;

    @Inject
    public CreatorTreeModel(TreeNode root, CreatorsPlugin plugin)
    {
        super(root);
        this.plugin = plugin;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node == root)
            return;

        //Check if node being modified is a Folder
        if (node.getUserObject() instanceof Folder)
        {
            Folder folder = (Folder) node.getUserObject();
            String name = StringHandler.cleanString((String) newValue);
            folder.setName(name);
            node.setUserObject(folder);
            plugin.getCreatorsPanel().getToolBox().getManagerPanel().getObjectLabel().setText("Current Folder: " + name);
        }

        //Check if node being modified is an ObjectPanel
        if (node.getUserObject() instanceof Character)
        {
            Character character = (Character) node.getUserObject();
            String name = StringHandler.cleanString((String) newValue);
            character.setName(name);
            character.getNameField().setText(name);
            character.getProgram().getNameLabel().setText(name);
            node.setUserObject(character);
        }
    }
}
