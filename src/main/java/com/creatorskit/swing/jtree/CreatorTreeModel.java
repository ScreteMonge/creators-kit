package com.creatorskit.swing.jtree;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.ObjectPanel;
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
        if (node.getUserObject() instanceof String)
        {
            String folderName = StringHandler.cleanString((String) newValue);
            node.setUserObject(folderName);
            plugin.getCreatorsPanel().getToolBox().getManagerPanel().getObjectLabel().setText("Current Folder: " + folderName);
        }

        //Check if node being modified is an ObjectPanel
        if (node.getUserObject() instanceof ObjectPanel)
        {
            ObjectPanel objectPanel = (ObjectPanel) node.getUserObject();
            String name = StringHandler.cleanString((String) newValue);
            objectPanel.setName(name);
            node.setUserObject(objectPanel);
            for (Character character : plugin.getCharacters())
            {
                if (character.getObjectPanel() == objectPanel)
                {
                    character.setName(name);
                    character.getNameField().setText(name);
                    character.getProgram().getNameLabel().setText(name);
                    return;
                }
            }
        }
    }
}
