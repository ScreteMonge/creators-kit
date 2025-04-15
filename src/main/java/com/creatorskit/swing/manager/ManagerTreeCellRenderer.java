package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.swing.Folder;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class ManagerTreeCellRenderer implements TreeCellRenderer
{
    DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

    private final ImageIcon FOLDER_OPEN = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Folder_Open.png"));
    private final ImageIcon FOLDER_CLOSED = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Folder_Closed.png"));
    private final ImageIcon OBJECT = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Object.png"));

    private final JPanel renderer = new JPanel();
    private final JLabel icon = new JLabel();
    private final JLabel name = new JLabel();

    public ManagerTreeCellRenderer()
    {
        renderer.add(icon);
        renderer.add(name);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
        if (!(value instanceof DefaultMutableTreeNode))
        {
            return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        renderer.setOpaque(false);

        Object o = node.getUserObject();
        if (o instanceof Folder)
        {
            Folder folder = (Folder) o;

            if (expanded)
            {
                icon.setIcon(FOLDER_OPEN);
            }
            else
            {
                icon.setIcon(FOLDER_CLOSED);
            }

            name.setText(folder.getName());

            renderer.setEnabled(tree.isEnabled());
            return renderer;
        }

        if (o instanceof Character)
        {
            Character character = (Character) o;

            icon.setIcon(OBJECT);
            name.setText(character.getName());

            renderer.setEnabled(tree.isEnabled());
            return renderer;
        }

        return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }
}
