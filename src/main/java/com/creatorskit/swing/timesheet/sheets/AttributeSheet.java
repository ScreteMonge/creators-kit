package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static com.creatorskit.swing.timesheet.TimeSheetPanel.round;

@Getter
@Setter
public class AttributeSheet extends TimeSheet
{
    private ManagerTree tree;
    private AttributePanel attributePanel;

    public AttributeSheet(ToolBoxFrame toolBox, ManagerTree tree, AttributePanel attributePanel)
    {
        super(toolBox, tree, attributePanel);
        this.tree = tree;
        this.attributePanel = attributePanel;

        setIndexBuffers(0);
        setSelectedIndex(1);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        if (getSelectedCharacter() == null)
        {
            return;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame[][] frames = getSelectedCharacter().getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame[] keyFrames = frames[i];
            if (keyFrames == null)
            {
                continue;
            }

            for (int e = 0; e < keyFrames.length; e++)
            {
                KeyFrame frame = keyFrames[e];

                BufferedImage endImage = image;
                KeyFrame[] selectedKeyframes = getTimeSheetPanel().getSelectedKeyFrames();
                if (Arrays.stream(selectedKeyframes).anyMatch(s -> s == frame))
                {
                    endImage = getKeyframeSelected();
                }

                g.drawImage(
                        endImage,
                        (int) ((frame.getTick() + getHScroll()) * zoomFactor - xImageOffset),
                        ROW_HEIGHT_OFFSET + ROW_HEIGHT + ROW_HEIGHT * i - yImageOffset,
                        null);
            }
        }
    }

    @Override
    public void drawPreviewKeyFrames(Graphics2D g)
    {
        if (getSelectedCharacter() == null)
        {
            return;
        }

        if (!isKeyFrameClicked())
        {
            return;
        }

        KeyFrame[] selectedKeyFrames = getSelectedKeyFrames();
        if (selectedKeyFrames.length == 0)
        {
            return;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        BufferedImage bufferedImage = getKeyframeImage();
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2F));

        double x = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX();
        double mouseX = Math.max(0, Math.min(x, getWidth()));
        double change = round((mouseX - getMousePointOnPressed().getX()) * getZoom() / getWidth());


        for (int e = 0; e < selectedKeyFrames.length; e++)
        {
            KeyFrame frame = selectedKeyFrames[e];
            int i = KeyFrameType.getIndex(frame.getKeyFrameType());

            g.drawImage(
                    bufferedImage,
                    (int) ((frame.getTick() + getHScroll() + change) * zoomFactor - xImageOffset),
                    ROW_HEIGHT_OFFSET + ROW_HEIGHT + ROW_HEIGHT * i - yImageOffset,
                    null);
        }

        g.setComposite(composite);
    }

    @Override
    public KeyFrame getKeyFrameClicked(Point point)
    {
        if (getSelectedCharacter() == null)
        {
            return null;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame[][] frames = getSelectedCharacter().getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame[] keyFrames = frames[i];
            if (keyFrames == null)
            {
                continue;
            }

            for (int e = 0; e < keyFrames.length; e++)
            {
                KeyFrame keyFrame = keyFrames[e];
                int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
                int x2 = x1 + image.getWidth();
                int y1 = ROW_HEIGHT_OFFSET + ROW_HEIGHT + ROW_HEIGHT * i - yImageOffset;
                int y2 = y1 + image.getHeight();

                if (point.getX() >= x1 && point.getX() <= x2)
                {
                    if (point.getY() >= y1 && point.getY() <= y2)
                    {
                        return keyFrame;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void onKeyFrameClicked(Point point, boolean shiftKey)
    {
        if (getSelectedCharacter() == null)
        {
            return;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        boolean foundFrame = false;
        KeyFrame[][] frames = getSelectedCharacter().getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame[] keyFrames = frames[i];
            if (keyFrames == null)
            {
                continue;
            }

            for (int e = 0; e < keyFrames.length; e++)
            {
                KeyFrame keyFrame = keyFrames[e];
                int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
                int x2 = x1 + image.getWidth();
                int y1 = ROW_HEIGHT_OFFSET + ROW_HEIGHT + ROW_HEIGHT * i - yImageOffset;
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
                                setSelectedKeyFrames(ArrayUtils.add(getSelectedKeyFrames(), keyFrame));
                            }
                        }
                        else
                        {
                            setSelectedKeyFrames(new KeyFrame[]{keyFrame});
                        }

                        foundFrame = true;
                        break;
                    }
                }
            }

            if (foundFrame)
            {
                break;
            }
        }

        if (!foundFrame && !shiftKey)
        {
            setSelectedKeyFrames(new KeyFrame[0]);
        }
    }

    @Override
    public void checkRectangleForKeyFrames(Point point, boolean shiftKey)
    {
        if (getSelectedCharacter() == null)
        {
            return;
        }

        if (!isAllowRectangleSelect())
        {
            return;
        }

        Point absoluteMouse = MouseInfo.getPointerInfo().getLocation();
        Point rectangleSelectStart = getMousePointOnPressed();

        int x1 = (int) rectangleSelectStart.getX();
        int x2 = (int) (absoluteMouse.getX() - getLocationOnScreen().getX());
        int y1 = (int) rectangleSelectStart.getY();
        int y2 = (int) (absoluteMouse.getY() - getLocationOnScreen().getY());

        if (Math.abs(x1 - x2) < 10 && Math.abs(y1 - y2) < 10)
        {
            return;
        }

        int startX;
        int startY;
        int endX;
        int endY;

        if (x1 < x2)
        {
            startX = x1;
            endX = x2;
        }
        else
        {
            startX = x2;
            endX = x1;
        }

        if (y1 < y2)
        {
            startY = y1;
            endY = y2;
        }
        else
        {
            startY = y2;
            endY = y1;
        }

        int buffer = 1;

        if (startX < buffer)
        {
            startX = buffer;
        }

        if (endX > getWidth() - 2)
        {
            endX = getWidth() - 2;
        }

        if (startY < buffer)
        {
            startY = buffer;
        }

        if (endY > getHeight() - 2)
        {
            endY = getHeight() - 2;
        }

        Rectangle2D rectangle = new Rectangle(startX, startY, endX - startX, endY - startY);

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame[] foundKeyFrames = new KeyFrame[0];
        if (shiftKey)
        {
            foundKeyFrames = getSelectedKeyFrames();
        }

        KeyFrame[][] frames = getSelectedCharacter().getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame[] keyFrames = frames[i];
            if (keyFrames == null)
            {
                continue;
            }

            for (int e = 0; e < keyFrames.length; e++)
            {
                KeyFrame keyFrame = keyFrames[e];
                boolean alreadyContains = false;

                for (KeyFrame kf : foundKeyFrames)
                {
                    if (keyFrame == kf)
                    {
                        alreadyContains = true;
                        break;
                    }
                }

                if (alreadyContains)
                {
                    continue;
                }

                int kx1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
                int ky1 = ROW_HEIGHT_OFFSET + ROW_HEIGHT + ROW_HEIGHT * i - yImageOffset;

                Rectangle2D frameRect = new Rectangle(kx1, ky1, image.getWidth(), image.getHeight());

                if (rectangle.intersects(frameRect))
                {
                    foundKeyFrames = ArrayUtils.add(foundKeyFrames, keyFrame);
                }
            }
        }

        setSelectedKeyFrames(foundKeyFrames);
    }
}
