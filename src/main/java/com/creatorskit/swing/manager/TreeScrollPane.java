package com.creatorskit.swing.manager;

import com.creatorskit.swing.ParentPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TreeScrollPane extends JScrollPane
{
    private final ManagerTree tree;
    private final GridBagConstraints c = new GridBagConstraints();

    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage ADD = ImageUtil.loadImageResource(getClass(), "/Add.png");
    private final BufferedImage CLEAR = ImageUtil.loadImageResource(getClass(), "/Clear.png");

    public TreeScrollPane(ManagerTree tree)
    {
        this.tree = tree;

        JPanel folderHeader = setupFolderHeader();
        setColumnHeaderView(folderHeader);

        JScrollBar scrollBar = getVerticalScrollBar();
        scrollBar.addAdjustmentListener(e -> tree.onPanelScrolled(scrollBar.getValue()));
    }

    private JPanel setupFolderHeader()
    {
        Dimension buttonDimension = new Dimension(40, 18);

        JPanel folderHeader = new JPanel();
        folderHeader.setLayout(new GridBagLayout());
        folderHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        JLabel folderLabel = new JLabel("Folders");
        folderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        folderLabel.setVerticalAlignment(SwingConstants.CENTER);
        folderLabel.setFont(FontManager.getRunescapeBoldFont());
        folderHeader.add(folderLabel, c);

        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 0;
        JButton addFolderButton = new JButton(new ImageIcon(ADD));
        addFolderButton.setPreferredSize(buttonDimension);
        addFolderButton.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR));
        addFolderButton.setToolTipText("Add a new Folder to the currently selected Folder");
        addFolderButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        addFolderButton.addActionListener(e ->
        {
            TreePath path = tree.getSelectionPath();
            DefaultMutableTreeNode folderNode;
            if (path == null)
            {
                folderNode = tree.addFolderNode("New Folder", ParentPanel.MANAGER);
            }
            else
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                ParentPanel parentPanel = tree.treeContainsSidePanel(node) ? ParentPanel.SIDE_PANEL : ParentPanel.MANAGER;
                folderNode = tree.addFolderNode("New Folder", parentPanel);
            }

            TreePath treePath = new TreePath(folderNode.getPath());
            int row = tree.getRowForPath(new TreePath(folderNode.getPath()));
            tree.expandRow(row);
            tree.setSelectionPath(treePath);
        });
        folderHeader.add(addFolderButton, c);

        c.gridx = 2;
        c.gridy = 0;
        JButton removeFolderButton = new JButton(new ImageIcon(CLOSE));
        removeFolderButton.setPreferredSize(buttonDimension);
        removeFolderButton.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR));
        removeFolderButton.setToolTipText("Remove the currently selected Object or Folder and all children");
        removeFolderButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        removeFolderButton.addActionListener(e ->
        {
            TreePath[] treePaths = tree.getSelectionPaths();
            if (treePaths == null)
                return;

            tree.removeNodes(treePaths);
        });
        folderHeader.add(removeFolderButton, c);

        c.gridx = 3;
        c.gridy = 0;
        JButton clearFolderButton = new JButton(new ImageIcon(CLEAR));
        clearFolderButton.setPreferredSize(buttonDimension);
        clearFolderButton.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR));
        clearFolderButton.setToolTipText("Remove all Folders and all Objects in them");
        clearFolderButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        clearFolderButton.addActionListener(e ->
        {
            Thread thread = new Thread(tree::removeAllNodes);
            thread.start();
        });
        folderHeader.add(clearFolderButton, c);

        return folderHeader;
    }
}
