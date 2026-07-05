package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.keyframeselectionmanager.KeyFrameSelectionManager;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class AttributeSheet extends TimeSheet
{
    private ManagerTree tree;
    private CreatorsConfig config;
    private AttributePanel attributePanel;
    private SelectionManager selectionManager;
    private KeyFrameSelectionManager kfsm;

    public AttributeSheet(ToolBoxFrame toolBox, CreatorsConfig config, ManagerTree tree, AttributePanel attributePanel, SelectionManager selectionManager, KeyFrameSelectionManager kfsm)
    {
        super(toolBox, config, tree, attributePanel, kfsm);
        this.config = config;
        this.tree = tree;
        this.attributePanel = attributePanel;
        this.selectionManager = selectionManager;
        this.kfsm = kfsm;

        setIndexBuffers(0);
        setSelectedIndex(1);
        this.rowHeightOffset = 1;
    }

    @Override
    public void drawBackgroundText(Graphics g)
    {
        Set<Character> selected = selectionManager.getSelected();
        String name = "[No Object Selected]";
        if (!selected.isEmpty())
        {
            Character primary = selectionManager.getPrimary();
            if (selectionManager.getPrimary() != null)
            {
                name = primary.getName();
            }

            if (selected.size() > 1)
            {
                name = "[" + selected.size() + " Objects Selected]";
            }
        }

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
    public void drawRowLabels(Graphics g)
    {
        g.setFont(FontManager.getRunescapeFont());
        g.setColor(ColorScheme.LIGHT_GRAY_COLOR);

        FontMetrics fontMetrics = g.getFontMetrics();
        int textHeight = fontMetrics.getHeight();
        final int X = 5;
        final int HEIGHT_BUFFER = 1;

        KeyFrameType[] keyFrameTypes = KeyFrameType.ALL_KEYFRAME_TYPES;
        for (int i = 0; i < keyFrameTypes.length; i++)
        {
            KeyFrameType type = keyFrameTypes[i];
            int y = (i + 2) * rowHeight - textHeight / 2 + HEIGHT_BUFFER;
            g.drawString(type.getName(), X, y);
        }
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        Set<Character> selected = selectionManager.getSelected();
        if (selected.isEmpty())
        {
            return;
        }

        BufferedImage image = getKeyframeImage();
        int imageHeight = image.getHeight();
        int yImageOffset = (imageHeight - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();
        boolean multi = selected.size() > 1;

        for (Character character : selected)
        {
            drawCharacterKeyFrames(g, character, image, imageHeight, yImageOffset, xImageOffset, zoomFactor, multi);
        }
    }

    private void drawCharacterKeyFrames(Graphics g, Character character, BufferedImage image, int imageHeight, int yImageOffset, int xImageOffset, double zoomFactor, boolean multi)
    {
        KeyFrame[][] frames = character.getFrames();
        if (frames == null)
        {
            return;
        }

        g.setColor(character.getColor());

        Collection<KeyFrame> selectedFrames = kfsm.getSelected().values().stream()
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());

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

                if (selectedFrames.contains(keyFrame))
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
                    case TEXT:
                        TextKeyFrame tkf = (TextKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, tkf.getDuration(), zoomFactor, tkf.getTick(), x, y, imageHeight);
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

                int dotSize = 7;
                g.fillOval(x - xImageOffset, y + endImage.getHeight() - dotSize, dotSize, dotSize);
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
        if (!isKeyFrameClicked())
        {
            return;
        }

        if (kfsm.isEmpty())
        {
            return;
        }

        TimelineUnits timelineUnits = config.timelineUnits();
        double modeMultiplier = timelineUnits.getMultiplier();

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

        final double[] change = new double[]{0};
        if (Math.abs(Math.abs(mouseX) - Math.abs(xCurrentTime)) > DRAG_STICK_RANGE)
        {
            change[0] = round(timelineUnits, (mouseX - getMousePointOnPressed().getX()) * getZoom() / getWidth());
        }
        else
        {
            LinkedHashMap<Character, KeyFrame[]> clickedKeyFrames = getClickedKeyFrames();
            if (!clickedKeyFrames.isEmpty())
            {
                Map.Entry<Character, KeyFrame[]> firstEntry = clickedKeyFrames.entrySet().iterator().next();
                KeyFrame keyFrame = firstEntry.getValue()[0];
                change[0] = round(timelineUnits, getCurrentTime() - keyFrame.getTick());
            }
        }

        int imageHeight = image.getHeight();

        kfsm.getSelected().forEach((Character c, KeyFrame[] keyFrames) ->
        {
            for (KeyFrame keyFrame : keyFrames)
            {
                int i = KeyFrameType.getIndex(keyFrame.getKeyFrameType());
                KeyFrameType type = KeyFrameType.getKeyFrameType(i);

                int x = (int) ((keyFrame.getTick() + getHScroll() + change[0]) * zoomFactor);
                int y = rowHeightOffset + rowHeight + rowHeight * i - getVScroll() - yImageOffset;

                switch (type)
                {
                    case MOVEMENT:
                        MovementKeyFrame mkf = (MovementKeyFrame) keyFrame;
                        int steps = (mkf.getPath().length - 1);
                        if (steps > 0)
                        {
                            double ticks = Math.ceil(steps / mkf.getSpeed());
                            int pathLength = (int) (ticks * zoomFactor);
                            g.drawLine(x, y + imageHeight / 2, x + pathLength - 1, y + imageHeight / 2);
                        }
                        break;
                    case ORIENTATION:
                        OrientationKeyFrame okf = (OrientationKeyFrame) keyFrame;
                        drawPreviewTail(g, x, y, imageHeight, okf.getDuration(), zoomFactor);
                        break;
                    case TEXT:
                        TextKeyFrame tkf = (TextKeyFrame) keyFrame;
                        drawPreviewTail(g, x, y, imageHeight, tkf.getDuration(), zoomFactor);
                        break;
                    case HEALTH:
                        HealthKeyFrame hkf = (HealthKeyFrame) keyFrame;
                        drawPreviewTail(g, x, y, imageHeight, hkf.getDuration(), zoomFactor);
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

                        drawPreviewTail(g, x, y, imageHeight, duration, zoomFactor);
                        break;
                    default:
                        break;
                }

                g.drawImage(bufferedImage, x - xImageOffset, y, null);
            }
        });

        g.setComposite(composite);
    }

    private void drawPreviewTail(Graphics g, int x, int y, int imageHeight, double duration, double zoomFactor)
    {
        int pathLength = (int) (duration * zoomFactor);
        g.drawLine(x, y + imageHeight / 2, x + pathLength - 1, y + imageHeight / 2);
    }

    @Override
    public void updateSelectedKeyFrameOnPressed(boolean shiftDown)
    {
        LinkedHashMap<Character, KeyFrame[]> clickedKeyFrames = getClickedKeyFrames();
        if (clickedKeyFrames.isEmpty())
        {
            return;
        }

        Map.Entry<Character, KeyFrame[]> firstEntry = clickedKeyFrames.entrySet().iterator().next();
        Character character = firstEntry.getKey();
        KeyFrame[] keyFrames = firstEntry.getValue(); //only registers for the first clicked keyframe

        if (!shiftDown)
        {
            if (!kfsm.containsKeyFrame(keyFrames))
            {
                kfsm.clear();
            }
        }

        KeyFrame primary = keyFrames[0];
        kfsm.addAll(character, keyFrames, primary);
        getTimeSheetPanel().onKeyFrameSelectionChanged();
    }

    @Override
    public LinkedHashMap<Character, KeyFrame[]> getKeyFrameClicked(Point point)
    {
        Set<Character> selected = selectionManager.getSelected();
        if (selected.isEmpty())
        {
            return null;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        for (Character c : selected)
        {
            KeyFrame[][] frames = c.getFrames();
            if (frames == null)
            {
                continue;
            }

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
                            LinkedHashMap<Character, KeyFrame[]> selectedKeyFrames = new LinkedHashMap<>();
                            selectedKeyFrames.put(c, new KeyFrame[]{keyFrame});
                            return selectedKeyFrames;
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void updateSelectedKeyFrameOnRelease(Point point, boolean shiftKey)
    {
        Set<Character> selected = selectionManager.getSelected();
        if (selected.isEmpty())
        {
            return;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame foundKeyFrame = null;

        for (Character c : selected)
        {
            KeyFrame[][] frames = c.getFrames();
            if (frames == null)
            {
                continue;
            }

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

                    if (point.getX() >= x1 && point.getX() <= x2
                            && point.getY() >= y1 && point.getY() <= y2)
                    {
                        foundKeyFrame = keyFrame;
                        break;
                    }
                }
            }
        }

        if (!shiftKey)
        {
            kfsm.clear();
        }

        if (foundKeyFrame != null)
        {
            if (!shiftKey)
            {
                kfsm.clear();
            }

            for (Character c : selected)
            {
                if (!c.containsKeyFrame(foundKeyFrame))
                {
                    continue;
                }

                kfsm.add(c, foundKeyFrame);
                break;
            }
        }
    }

    @Override
    public boolean checkRectangleForKeyFrames(Point point, boolean shiftKey)
    {
        Set<Character> selected = selectionManager.getSelected();
        if (selected.isEmpty())
        {
            return false;
        }

        Point absoluteMouse = MouseInfo.getPointerInfo().getLocation();
        Point rectangleSelectStart = getMousePointOnPressed();

        int x1 = (int) rectangleSelectStart.getX();
        int x2 = (int) (absoluteMouse.getX() - getLocationOnScreen().getX());
        int y1 = (int) rectangleSelectStart.getY();
        int y2 = (int) (absoluteMouse.getY() - getLocationOnScreen().getY());

        if (Math.abs(x1 - x2) < 10 && Math.abs(y1 - y2) < 10)
        {
            return false;
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

        LinkedHashMap<Character, KeyFrame[]> selectedKeyFrames = kfsm.getSelected();
        if (!shiftKey)
        {
            kfsm.clear();
        }

        for (Character c : selected)
        {
            KeyFrame[][] frames = c.getFrames();
            KeyFrame[] foundKeyFrames = selectedKeyFrames.get(c);
            if (foundKeyFrames == null)
            {
                foundKeyFrames = new KeyFrame[0];
            }

            KeyFrame primary = null;
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
                        primary = keyFrame;
                    }
                }
            }

            kfsm.addAll(c, foundKeyFrames, primary);
        }

        return true;
    }

    @Override
    public void updateTableSelection(Point p)
    {
        KeyFrameType[] types = KeyFrameType.ALL_KEYFRAME_TYPES;
        int y = (int) p.getY();
        final int ROW_BUFFER = 1;

        int row = y / rowHeight - ROW_BUFFER;
        if (row > types.length)
        {
            return;
        }

        attributePanel.switchCards(types[row]);
        attributePanel.updateAttributes();
    }
}
