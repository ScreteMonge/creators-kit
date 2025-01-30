package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.swing.Folder;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

@Getter
@Setter
public class SummarySheet extends TimeSheet
{
    private ManagerTree tree;
    private AttributePanel attributePanel;

    public SummarySheet(ToolBoxFrame toolBox, ManagerTree tree, AttributePanel attributePanel)
    {
        super(toolBox, tree, attributePanel);
        this.tree = tree;
        this.attributePanel = attributePanel;
    }

    @Override
    public void drawHighlight(Graphics g)
    {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, getSelectedIndex() * ROW_HEIGHT - ROW_HEIGHT_OFFSET - getVScroll(), this.getWidth(), ROW_HEIGHT);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;

        ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<>();
        nodes.add(tree.getRootNode());
        tree.getAllNodes(tree.getRootNode(), nodes);
        int index = -2;

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
            KeyFrameType keyFrameType = getToolBox().getTimeSheetPanel().getSummaryKeyFrameType();
            switch (keyFrameType)
            {
                default:
                case NULL:
                case SUMMARY:
                    double[] ticks = getSummaryKeyFrames(character);
                    drawFrameIcons(g, ticks, image, index, xImageOffset, yImageOffset);
                    break;
                case MOVEMENT:
                case ANIMATION:
                case SPAWN:
                case MODEL:
                case ORIENTATION:
                case TEXT:
                case OVERHEAD:
                case HEALTH:
                case SPOTANIM:
                case SPOTANIM2:
                    KeyFrame[] keyFrames = character.getKeyFrames(keyFrameType);
                    drawFrameIcons(g, keyFrames, image, index, xImageOffset, yImageOffset);
            }
        }
    }

    private double[] getSummaryKeyFrames(Character character)
    {
        double[] ticks = new double[0];
        KeyFrame[][] frames = character.getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame[] keyFrames = frames[i];
            if (keyFrames == null)
            {
                continue;
            }

            for (int e = 0; e < keyFrames.length; e++)
            {
                KeyFrame k = keyFrames[e];
                if (ticks.length == 0)
                {
                    ticks = ArrayUtils.add(ticks, k.getTick());
                }

                double tick = k.getTick();
                boolean contains = false;
                for (double d : ticks)
                {
                    if (tick == d)
                    {
                        contains = true;
                        break;
                    }
                }

                if (contains)
                {
                    continue;
                }

                ticks = ArrayUtils.add(ticks, tick);
            }
        }

        return ticks;
    }

    private void drawFrameIcons(Graphics g, double[] ticks, BufferedImage image, int index, int xImageOffset, int yImageOffset)
    {
        for (int e = 0; e < ticks.length; e++)
        {
            double d = ticks[e];

            double zoomFactor = this.getWidth() / getZoom();
            g.drawImage(
                    image,
                    (int) ((d + getHScroll()) * zoomFactor - xImageOffset),
                    (index * ROW_HEIGHT) - ROW_HEIGHT_OFFSET - yImageOffset - getVScroll(),
                    null);
        }
    }

    private void drawFrameIcons(Graphics g, KeyFrame[] keyFrames, BufferedImage image, int index, int xImageOffset, int yImageOffset)
    {
        for (int e = 0; e < keyFrames.length; e++)
        {
            double d = keyFrames[e].getTick();

            double zoomFactor = this.getWidth() / getZoom();
            g.drawImage(
                    image,
                    (int) ((d + getHScroll()) * zoomFactor - xImageOffset),
                    (index * ROW_HEIGHT) - ROW_HEIGHT_OFFSET - yImageOffset - getVScroll(),
                    null);
        }
    }

    @Override
    public void updateSelectedKeyFrameOnPressed(boolean shiftDown)
    {
        KeyFrame[] clickedKeyFrames = getClickedKeyFrames();
        if (clickedKeyFrames.length == 0)
        {
            return;
        }

        KeyFrame[] selectedKeyFrames = getSelectedKeyFrames();
        KeyFrame clickedKeyFrame = clickedKeyFrames[0];
        if (Arrays.stream(selectedKeyFrames).noneMatch(n -> n == clickedKeyFrame))
        {
            if (shiftDown)
            {
                setSelectedKeyFrames(ArrayUtils.add(selectedKeyFrames, clickedKeyFrame));
            }
            else
            {
                setSelectedKeyFrames(new KeyFrame[]{clickedKeyFrame});
            }
        }
    }

    @Override
    public KeyFrame[] getKeyFrameClicked(Point point)
    {
        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrameType keyFrameType = getToolBox().getTimeSheetPanel().getSummaryKeyFrameType();

        ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<>();
        nodes.add(tree.getRootNode());
        tree.getAllNodes(tree.getRootNode(), nodes);

        KeyFrame[] keyFramesClicked = new KeyFrame[0];

        int index = -2;

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
            KeyFrame[] keyFrames = character.getKeyFrames(keyFrameType);
            if (keyFrames == null)
            {
                continue;
            }

            for (KeyFrame keyFrame : keyFrames)
            {
                int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
                int x2 = x1 + image.getWidth();
                int y1 = (index * ROW_HEIGHT) - ROW_HEIGHT_OFFSET - yImageOffset - getVScroll();
                int y2 = y1 + image.getHeight();

                if (point.getX() >= x1 && point.getX() <= x2)
                {
                    if (point.getY() >= y1 && point.getY() <= y2)
                    {
                        keyFramesClicked = ArrayUtils.add(keyFramesClicked, keyFrame);
                    }
                }
            }
        }

        return keyFramesClicked;
    }

    @Override
    public void updateSelectedKeyFrameOnRelease(Point point, boolean shiftKey)
    {
        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrameType keyFrameType = getToolBox().getTimeSheetPanel().getSummaryKeyFrameType();

        ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<>();
        nodes.add(tree.getRootNode());
        tree.getAllNodes(tree.getRootNode(), nodes);

        KeyFrame[] keyFramesClicked = new KeyFrame[0];

        int index = -2;

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
            KeyFrame[] keyFrames = character.getKeyFrames(keyFrameType);
            if (keyFrames == null)
            {
                return;
            }

            for (KeyFrame keyFrame : keyFrames)
            {
                int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
                int x2 = x1 + image.getWidth();
                int y1 = (index * ROW_HEIGHT) - ROW_HEIGHT_OFFSET - yImageOffset - getVScroll();
                int y2 = y1 + image.getHeight();

                if (point.getX() >= x1 && point.getX() <= x2)
                {
                    if (point.getY() >= y1 && point.getY() <= y2)
                    {
                        if (shiftKey)
                        {
                            KeyFrame[] selectedKeyFrames = getSelectedKeyFrames();
                            boolean alreadyContains = false;

                            for (KeyFrame kf : selectedKeyFrames)
                            {
                                if (kf == keyFrame)
                                {
                                    alreadyContains = true;
                                    break;
                                }
                            }

                            if (!alreadyContains)
                            {
                                keyFramesClicked = ArrayUtils.add(keyFramesClicked, keyFrame);
                            }
                        }
                        else
                        {
                            keyFramesClicked = ArrayUtils.add(keyFramesClicked, keyFrame);
                        }
                    }
                }
            }
        }

        setSelectedKeyFrames(keyFramesClicked);
    }
}
