package com.creatorskit.swing.timesheet;

import com.creatorskit.Character;
import com.creatorskit.swing.Folder;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.image.BufferedImage;

@Getter
public class TimeTree extends JScrollPane
{
    private final TimeSheet timeSheet;
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final JPanel viewPort = new JPanel();
    private final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode sidePanelNode;
    private final DefaultMutableTreeNode managerNode;

    private final BufferedImage FOLDER_OPEN = ImageUtil.loadImageResource(getClass(), "/Folder_Open.png");
    private final BufferedImage FOLDER_CLOSED = ImageUtil.loadImageResource(getClass(), "/Folder_Closed.png");
    private final BufferedImage OBJECT = ImageUtil.loadImageResource(getClass(), "/Object.png");

    private final int BLOCK_HEIGHT = 24;

    @Inject
    public TimeTree(TimeSheet timeSheet, DefaultMutableTreeNode rootNode, DefaultMutableTreeNode sidePanelNode, DefaultMutableTreeNode managerNode)
    {
        this.timeSheet = timeSheet;
        this.rootNode = rootNode;
        this.sidePanelNode = sidePanelNode;
        this.managerNode = managerNode;

        viewPort.setLayout(new BorderLayout());
        viewPort.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        treeModel = new TimeTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setRowHeight(BLOCK_HEIGHT);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        tree.addTreeSelectionListener(e -> updateTreeSelectionIndex());
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event)
            {
                Object object = event.getPath().getLastPathComponent();
                if (object instanceof Character)
                {
                    Character character = (Character) object;
                    for (KeyFrame[] keyFrames : character.getFrames())
                    {
                        for (KeyFrame keyFrame : keyFrames)
                        {
                            timeSheet.addVisibleKeyFrame(keyFrame);
                        }
                    }
                }
                updateTreeSelectionIndex();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event)
            {
                Object object = event.getPath().getLastPathComponent();
                if (object instanceof Character)
                {
                    Character character = (Character) object;
                    for (KeyFrame[] keyFrames : character.getFrames())
                    {
                        for (KeyFrame keyFrame : keyFrames)
                        {
                            timeSheet.removeVisibleKeyFrame(keyFrame);
                        }
                    }
                }
                updateTreeSelectionIndex();
            }
        });

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(new ImageIcon(FOLDER_OPEN));
        renderer.setClosedIcon(new ImageIcon(FOLDER_CLOSED));
        renderer.setLeafIcon(new ImageIcon(OBJECT));
        tree.setCellRenderer(renderer);

        rootNode.add(sidePanelNode);
        rootNode.add(managerNode);

        viewPort.add(tree, BorderLayout.CENTER);
        setViewportView(viewPort);
        JScrollBar scrollBar = getVerticalScrollBar();
        scrollBar.addAdjustmentListener(e -> onPanelScrolled(scrollBar.getValue()));

    }

    public void moveNodes(DefaultMutableTreeNode[] managerNodes, DefaultMutableTreeNode newParent)
    {
        for (DefaultMutableTreeNode managerNode : managerNodes)
        {
            DefaultMutableTreeNode timeSheetParent = null;
            DefaultMutableTreeNode linkedTimeSheetNode = null;
            if (managerNode.getUserObject() instanceof Folder)
            {
                Folder folder = (Folder) managerNode.getUserObject();
                linkedTimeSheetNode = folder.getLinkedTimeSheetNode();
                timeSheetParent = folder.getParentTimeSheetNode();
            }

            if (managerNode.getUserObject() instanceof Character)
            {
                Character character = (Character) managerNode.getUserObject();
                linkedTimeSheetNode = character.getLinkedTimeSheetNode();
                timeSheetParent = character.getParentTimeSheetNode();
            }

            if (linkedTimeSheetNode == null || timeSheetParent == null)
            {
                continue;
            }

            treeModel.removeNodeFromParent(linkedTimeSheetNode);
            treeModel.insertNodeInto(linkedTimeSheetNode, timeSheetParent, timeSheetParent.getChildCount());
        }
    }

    public void addFolderNode(DefaultMutableTreeNode parent, Folder folder)
    {
        parent.add(new DefaultMutableTreeNode(folder));
        updateTreeSelectionIndex();
    }

    public void addCharacterNode(DefaultMutableTreeNode parent, Character character)
    {
        DefaultMutableTreeNode node = character.getLinkedTimeSheetNode();
        parent.add(node);
        updateTreeSelectionIndex();
    }

    public void addKeyFrame(Character character, KeyFrame keyFrame)
    {

    }

    private void onPanelScrolled(int scroll)
    {
        timeSheet.onVerticalScrollEvent(scroll);
    }

    private void updateTreeSelectionIndex()
    {
        int[] rows = tree.getSelectionRows();
        {
            if (rows == null || rows.length == 0)
            {
                return;
            }
            timeSheet.setSelectedIndex(rows[0]);
        }
    }
}
