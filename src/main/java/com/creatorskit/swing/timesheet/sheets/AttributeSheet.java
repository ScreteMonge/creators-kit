package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
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
        this.rowHeightOffset = 1;
        this.rowHeight = 24;
    }

    @Override
    public void drawBackgroundText(Graphics g)
    {
        Character character = getSelectedCharacter();
        if (character == null)
        {
            return;
        }

        String name = character.getName();

        g.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 64));
        g.setColor(new Color(77, 77, 77, 50));
        FontMetrics fm = g.getFontMetrics();

        g.drawString(name, this.getWidth() / 2 - fm.stringWidth(name) / 2, this.getHeight() / 2 + fm.getHeight() / 2);
    }

    @Override
    public void drawHighlight(Graphics g)
    {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, (getSelectedIndex() + getIndexBuffers()) * rowHeight + rowHeightOffset - getVScroll(), this.getWidth(), rowHeight);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        if (getSelectedCharacter() == null)
        {
            return;
        }

        g.setColor(new Color(219, 137, 0));

        BufferedImage image = getKeyframeImage();
        int imageHeight = image.getHeight();
        int yImageOffset = (imageHeight - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame[][] frames = getSelectedCharacter().getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrameType type = KeyFrameType.getKeyFrameType(i);

            KeyFrame[] keyFrames = frames[i];
            if (keyFrames == null)
            {
                continue;
            }

            for (int e = 0; e < keyFrames.length; e++)
            {
                KeyFrame keyFrame = keyFrames[e];

                BufferedImage endImage = image;
                KeyFrame[] selectedKeyframes = getTimeSheetPanel().getSelectedKeyFrames();
                if (Arrays.stream(selectedKeyframes).anyMatch(s -> s == keyFrame))
                {
                    endImage = getKeyframeSelected();
                }

                int x = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor);
                int y = rowHeightOffset + rowHeight + rowHeight * i - getVScroll() - yImageOffset;

                switch (type)
                {
                    case MOVEMENT:
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
                            g.drawLine(x, y + image.getHeight() / 2, x + pathLength - 1, y + image.getHeight() / 2);
                        }
                        break;
                    case ORIENTATION:
                        OrientationKeyFrame okf = (OrientationKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, okf.getDuration(), zoomFactor, okf.getTick(), x, y, imageHeight);
                        break;
                    case HEALTH:
                        HealthKeyFrame hkf = (HealthKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, hkf.getDuration(), zoomFactor, hkf.getTick(), x, y, imageHeight);
                        break;
                    case HITSPLAT_1:
                    case HITSPLAT_2:
                    case HITSPLAT_3:
                    case HITSPLAT_4:
                        HitsplatKeyFrame hskf = (HitsplatKeyFrame) keyFrame;
                        double duration = hskf.getDuration();
                        if (duration == -1)
                        {
                            duration = HitsplatKeyFrame.DEFAULT_DURATION;
                        }

                        drawTail(g, e, keyFrames, duration, zoomFactor, hskf.getTick(), x, y, imageHeight);
                        break;
                    default:
                        break;
                }


                g.drawImage(endImage, x - xImageOffset, y, null);
            }
        }
    }

    private void drawTail(Graphics g, int e, KeyFrame[] keyFrames, double duration, double zoomFactor, double tick, int x, int y, int imageHeight)
    {
        if (e + 1 < keyFrames.length)
        {
            KeyFrame next = keyFrames[e + 1];
            double difference = next.getTick() - tick;
            if (difference < duration)
            {
                duration = difference;
            }
        }

        int pathLength = (int) (duration * zoomFactor);
        g.drawLine(x, y + imageHeight / 2, x + pathLength - 1, y + imageHeight / 2);
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
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        BufferedImage bufferedImage = getKeyframeImage();
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2F));

        double pointerX = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX();
        double mouseX = Math.max(0, Math.min(pointerX, getWidth()));

        double xCurrentTime = currentTimeToMouseX();

        double change;
        if (Math.abs(Math.abs(mouseX) - Math.abs(xCurrentTime)) > DRAG_STICK_RANGE)
        {
            change = round((mouseX - getMousePointOnPressed().getX()) * getZoom() / getWidth());
        }
        else
        {
            KeyFrame keyFrame = getClickedKeyFrames()[0];
            change = round(getCurrentTime() - keyFrame.getTick());
        }


        for (int e = 0; e < selectedKeyFrames.length; e++)
        {
            KeyFrame frame = selectedKeyFrames[e];
            int i = KeyFrameType.getIndex(frame.getKeyFrameType());
            KeyFrameType type = KeyFrameType.getKeyFrameType(i);

            int x = (int) ((frame.getTick() + getHScroll() + change) * zoomFactor);
            int y = rowHeightOffset + rowHeight + rowHeight * i - getVScroll() - yImageOffset;

            if (type == KeyFrameType.MOVEMENT)
            {
                MovementKeyFrame movementKeyFrame = (MovementKeyFrame) frame;
                int steps = (movementKeyFrame.getPath().length - 1);
                if (steps > 0)
                {
                    double ticks = Math.ceil(steps / movementKeyFrame.getSpeed());
                    int pathLength = (int) (ticks * zoomFactor);
                    g.drawLine(x, y + image.getHeight() / 2, x + pathLength - 1, y + image.getHeight() / 2);
                }
            }

            if (type == KeyFrameType.ORIENTATION)
            {
                OrientationKeyFrame orientationKeyFrame = (OrientationKeyFrame) frame;
                int pathLength = (int) (orientationKeyFrame.getDuration() * zoomFactor);
                g.drawLine(x, y + image.getHeight() / 2, x + pathLength - 1, y + image.getHeight() / 2);
            }

            g.drawImage(bufferedImage, x - xImageOffset, y, null);
        }

        g.setComposite(composite);
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
        if (getSelectedCharacter() == null)
        {
            return null;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
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
                int y1 = rowHeightOffset + rowHeight + rowHeight * i - getVScroll() - yImageOffset;
                int y2 = y1 + image.getHeight();

                if (point.getX() >= x1 && point.getX() <= x2)
                {
                    if (point.getY() >= y1 && point.getY() <= y2)
                    {
                        return new KeyFrame[]{keyFrame};
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void updateSelectedKeyFrameOnRelease(Point point, boolean shiftKey)
    {
        if (getSelectedCharacter() == null)
        {
            return;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
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
                int y1 = rowHeightOffset + rowHeight + rowHeight * i - getVScroll() - yImageOffset;
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
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
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
                int ky1 = rowHeightOffset + rowHeight + rowHeight * i - getVScroll() - yImageOffset;

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
