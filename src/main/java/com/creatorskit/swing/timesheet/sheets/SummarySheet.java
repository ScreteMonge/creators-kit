package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.swing.Folder;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.MovementKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.OrientationKeyFrame;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;

@Getter
@Setter
public class SummarySheet extends TimeSheet
{
    private ManagerTree tree;
    private AttributePanel attributePanel;
    private JPopupMenu popupMenu;
    private Character rightClickedCharacter;
    private JLabel popupTitle;
    private JMenu[] menuItems;
    private final int FONT_SPACER = 9;

    public SummarySheet(ToolBoxFrame toolBox, ManagerTree tree, AttributePanel attributePanel)
    {
        super(toolBox, tree, attributePanel);
        this.tree = tree;
        this.attributePanel = attributePanel;

        setupPopupMenu();
    }

    private void setupPopupMenu()
    {
        popupMenu = new JPopupMenu();
        popupTitle = new JLabel("");
        popupTitle.setFont(FontManager.getRunescapeBoldFont());
        popupMenu.add(popupTitle);

        menuItems = new JMenu[]
                {
                        new JMenu(KeyFrameType.MOVEMENT.getName()),
                        new JMenu(KeyFrameType.ANIMATION.getName()),
                        new JMenu(KeyFrameType.ORIENTATION.getName())
                };

        for (int i = 0; i < menuItems.length; i++)
        {
            int finalI = i;
            JMenu menuItem = menuItems[i];
            popupMenu.add(menuItem);

            JMenuItem movement = new JMenuItem(KeyFrameType.MOVEMENT.getName());
            movement.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.MOVEMENT));
            menuItem.add(movement);

            JMenuItem animation = new JMenuItem(KeyFrameType.ANIMATION.getName());
            animation.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.ANIMATION));
            menuItem.add(animation);

            JMenuItem spawn = new JMenuItem(KeyFrameType.SPAWN.getName());
            spawn.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.SPAWN));
            menuItem.add(spawn);

            JMenuItem model = new JMenuItem(KeyFrameType.MODEL.getName());
            model.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.MODEL));
            menuItem.add(model);

            JMenuItem orientation = new JMenuItem(KeyFrameType.ORIENTATION.getName());
            orientation.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.ORIENTATION));
            menuItem.add(orientation);

            JMenuItem text = new JMenuItem(KeyFrameType.TEXT.getName());
            text.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.TEXT));
            menuItem.add(text);

            JMenuItem overhead = new JMenuItem(KeyFrameType.OVERHEAD.getName());
            overhead.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.OVERHEAD));
            menuItem.add(overhead);

            JMenuItem health = new JMenuItem(KeyFrameType.HEALTH.getName());
            health.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.HEALTH));
            menuItem.add(health);

            JMenuItem spotanim1 = new JMenuItem(KeyFrameType.SPOTANIM.getName());
            spotanim1.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.SPOTANIM));
            menuItem.add(spotanim1);

            JMenuItem spotanim2 = new JMenuItem(KeyFrameType.SPOTANIM2.getName());
            spotanim2.addActionListener(e -> onKeyFrameTypePressed(finalI, KeyFrameType.SPOTANIM2));
            menuItem.add(spotanim2);
        }
    }

    @Override
    public void drawHighlight(Graphics g)
    {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, getSelectedIndex() * rowHeight - rowHeightOffset - getVScroll(), this.getWidth(), rowHeight);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<>();
        nodes.add(tree.getRootNode());
        tree.getAllNodes(tree.getRootNode(), nodes);
        int index = -2;

        g.setFont(FontManager.getRunescapeSmallFont());
        g.setColor(ColorScheme.BRAND_ORANGE);

        for (DefaultMutableTreeNode node : nodes)
        {
            index++;

            TreePath path = tree.getPathForRow(index);
            if (path == null)
            {
                continue;
            }

            if (node.getUserObject() instanceof Folder)
            {
                continue;
            }

            Character character = (Character) node.getUserObject();
            drawFrameIcons(g, character, index);
        }
    }

    private void drawFrameIcons(Graphics g, Character character, int index)
    {
        KeyFrameType[] types = character.getSummary();
        FontMetrics fontMetrics = g.getFontMetrics();
        int stringHeight = fontMetrics.getHeight();

        for (int i = 0; i < types.length; i++)
        {
            KeyFrameType type = types[i];
            KeyFrame[] keyFrames = character.getKeyFrames(type);
            if (keyFrames == null || keyFrames.length == 0)
            {
                continue;
            }

            String name = type.getShortHand();

            int xStringOffset = fontMetrics.stringWidth(name) / 2;
            int yStringOffset = fontMetrics.getHeight() - 1 + i * FONT_SPACER;

            drawFrameIcons(
                    g,
                    keyFrames,
                    type,
                    name,
                    index,
                    stringHeight,
                    xStringOffset,
                    yStringOffset
            );
        }
    }

    private void drawFrameIcons(Graphics g, KeyFrame[] keyFrames, KeyFrameType type, String name, int index, int stringHeight, int xStringOffset, int yStringOffset)
    {
        for (int e = 0; e < keyFrames.length; e++)
        {
            KeyFrame keyFrame = keyFrames[e];

            double zoomFactor = this.getWidth() / getZoom();
            int x = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor);
            int y = (index * rowHeight) - rowHeightOffset - getVScroll() + yStringOffset;

            if (type == KeyFrameType.MOVEMENT)
            {
                MovementKeyFrame movementKeyFrame = (MovementKeyFrame) keyFrame;
                int steps = (movementKeyFrame.getPath().length - 1);
                if (steps > 0)
                {
                    double ticks = steps / movementKeyFrame.getSpeed();
                    boolean round = true;
                    if (e + 1 < keyFrames.length)
                    {
                        KeyFrame next = keyFrames[e + 1];
                        double difference = next.getTick() - keyFrame.getTick();
                        if (difference < ticks)
                        {
                            ticks = difference;
                            round = false;
                        }
                    }

                    if (round)
                    {
                        ticks = Math.ceil(ticks);
                    }

                    int pathLength = (int) (ticks * zoomFactor);
                    g.drawLine(x + xStringOffset, y - stringHeight / 2, x + pathLength - 1, y - stringHeight / 2);
                }
            }

            if (type == KeyFrameType.ORIENTATION)
            {
                OrientationKeyFrame orientationKeyFrame = (OrientationKeyFrame) keyFrame;
                double ticks = orientationKeyFrame.getDuration();
                if (e + 1 < keyFrames.length)
                {
                    KeyFrame next = keyFrames[e + 1];
                    double difference = next.getTick() - keyFrame.getTick();
                    if (difference < ticks)
                    {
                        ticks = difference;
                    }
                }


                int pathLength = (int) (ticks * zoomFactor);
                g.drawLine(x  + xStringOffset, y - stringHeight / 2, x + pathLength - 1, y - stringHeight / 2);
            }


            g.drawString(
                    name,
                    x - xStringOffset,
                    y);
        }
    }

    @Override
    public void onMouseButton1DoublePressed(Point p)
    {
        getTree().setRowSelection(p);
    }

    @Override
    public void onMouseButton3Pressed(Point p)
    {
        int x = (int) p.getX();
        int y = (int) p.getY();
        int row = tree.getClosestRowForLocation(x, y);

        TreePath path = tree.getPathForRow(row);
        if (path == null)
        {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(node.getUserObject() instanceof Character))
        {
            return;
        }

        Character character = (Character) node.getUserObject();
        showSummaryPopup(this, character, x, y);
    }

    private void onKeyFrameTypePressed(int index, KeyFrameType type)
    {
        if (rightClickedCharacter == null)
        {
            return;
        }

        KeyFrameType[] summary = rightClickedCharacter.getSummary();
        summary[index] = type;
    }

    public void showSummaryPopup(JComponent component, Character character, int x, int y)
    {
        if (character == null)
        {
            return;
        }

        rightClickedCharacter = character;
        KeyFrameType[] summary = rightClickedCharacter.getSummary();
        for (int i = 0; i < summary.length; i++)
        {
            menuItems[i].setText(summary[i].getName());
        }

        popupTitle.setText(character.getName() + " showing:");
        popupMenu.show(component, x, y);
    }
}
