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
    private TimeSheetPanel timeSheetPanel;
    private TimeSheet timeSheet;
    private final DefaultMutableTreeNode rootNode;
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final JPanel viewPort = new JPanel();
    private final DefaultMutableTreeNode sidePanelNode = new DefaultMutableTreeNode();
    private final DefaultMutableTreeNode managerNode = new DefaultMutableTreeNode();

    private final BufferedImage FOLDER_OPEN = ImageUtil.loadImageResource(getClass(), "/Folder_Open.png");
    private final BufferedImage FOLDER_CLOSED = ImageUtil.loadImageResource(getClass(), "/Folder_Closed.png");
    private final BufferedImage OBJECT = ImageUtil.loadImageResource(getClass(), "/Object.png");

    private final int BLOCK_HEIGHT = 24;

    @Inject
    public TimeTree(TimeSheetPanel timeSheetPanel, TimeSheet timeSheet)
    {
        this.timeSheetPanel = timeSheetPanel;
        this.timeSheet = timeSheet;

        setPreferredSize(new Dimension(170, 0));
        viewPort.setLayout(new BorderLayout());
        viewPort.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        rootNode = new DefaultMutableTreeNode("Folders");



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

        treeModel.insertNodeInto(this.sidePanelNode, rootNode, rootNode.getChildCount());
        treeModel.insertNodeInto(managerNode, rootNode, rootNode.getChildCount());

        viewPort.add(tree, BorderLayout.CENTER);
        setViewportView(viewPort);
        JScrollBar scrollBar = getVerticalScrollBar();
        scrollBar.addAdjustmentListener(e -> onPanelScrolled(scrollBar.getValue()));

    }

    public void addFolderNode(DefaultMutableTreeNode parent, Folder folder)
    {
        parent.add(new DefaultMutableTreeNode(folder));
        updateTreeSelectionIndex();
    }

    public void addCharacterNode(DefaultMutableTreeNode parent, Character character)
    {
        DefaultMutableTreeNode node = character.getTimeTreeNode();
        parent.add(node);
        addKeyFrameNodes(node, character);
        updateTreeSelectionIndex();
    }

    private void addKeyFrameNodes(DefaultMutableTreeNode newNode, Character character)
    {
        DefaultMutableTreeNode[] nodes = character.getTimeTreeNodes();
        newNode.add(nodes[0]);
        newNode.add(nodes[1]);
        newNode.add(nodes[2]);
        newNode.add(nodes[3]);
        newNode.add(nodes[4]);
        newNode.add(nodes[5]);
        newNode.add(nodes[6]);
        newNode.add(nodes[7]);
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

    public static DefaultMutableTreeNode[] createEmptyNodeTree()
    {
        return new DefaultMutableTreeNode[]
                {
                        new DefaultMutableTreeNode("Location"),
                        new DefaultMutableTreeNode("Animation"),
                        new DefaultMutableTreeNode("Spawn"),
                        new DefaultMutableTreeNode("Orientation"),
                        new DefaultMutableTreeNode("Text"),
                        new DefaultMutableTreeNode("Overhead"),
                        new DefaultMutableTreeNode("Hitsplat"),
                        new DefaultMutableTreeNode("Healthbar")
                };
    }
}