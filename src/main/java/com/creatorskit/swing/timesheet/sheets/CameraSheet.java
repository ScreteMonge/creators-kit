package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.programming.camera.CameraManager;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.keyframeselectionmanager.KeyFrameSelectionManager;
import com.creatorskit.swing.timesheet.keyframe.subtypes.*;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

public class CameraSheet extends TimeSheet
{
    private ManagerTree tree;
    private CreatorsConfig config;
    private AttributePanel attributePanel;
    private KeyFrameSelectionManager kfsm;
    private CameraManager cameraManager;
    private final int FONT_SPACER = 9;

    private final BufferedImage cameraImage = ImageUtil.loadImageResource(getClass(), "/Camera.png");

    public CameraSheet(ToolBoxFrame toolBox, CreatorsConfig config, ManagerTree tree, AttributePanel attributePanel, KeyFrameSelectionManager kfsm, CameraManager cameraManager)
    {
        super(toolBox, config, tree, attributePanel, kfsm);
        this.config = config;
        this.tree = tree;
        this.attributePanel = attributePanel;
        this.kfsm = kfsm;
        this.cameraManager = cameraManager;

        setPreferredSize(new Dimension(0, 60));
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

        int y = 2 * rowHeight - textHeight / 2 + HEIGHT_BUFFER;
        g.drawString(KeyFrameType.CAMERA.toString(), X, y);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        BufferedImage image = getKeyframeImage();
        int imageHeight = image.getHeight();
        int yImageOffset = (imageHeight - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        Collection<KeyFrame> selectedFrames = kfsm.getSelected().values().stream()
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());

        KeyFrame[] keyFrames = cameraManager.getKeyFrames();
        for (int e = 0; e < keyFrames.length; e++)
        {
            KeyFrame keyFrame = keyFrames[e];

            BufferedImage endImage = image;

            if (selectedFrames.contains(keyFrame))
            {
                endImage = getKeyframeSelected();
            }

            if (kfsm.getPrimary() == keyFrame)
            {
                endImage = getKeyframePrimary();
            }

            int x = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor);
            int y = rowHeightOffset + rowHeight - yImageOffset;

            g.drawImage(endImage, x - xImageOffset, y, null);
            g.drawImage(cameraImage, x - xImageOffset, y + endImage.getHeight() - cameraImage.getHeight(), null);
        }
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

        BufferedImage selectedImage = getKeyframeSelected();
        BufferedImage primaryImage = getKeyframePrimary();
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
            LinkedHashMap<KeyFrameTarget, KeyFrame[]> clickedKeyFrames = getClickedKeyFrames();
            if (!clickedKeyFrames.isEmpty())
            {
                Map.Entry<KeyFrameTarget, KeyFrame[]> firstEntry = clickedKeyFrames.entrySet().iterator().next();
                KeyFrame keyFrame = firstEntry.getValue()[0];
                change[0] = round(timelineUnits, getCurrentTime() - keyFrame.getTick());
            }
        }

        kfsm.getSelected().forEach((KeyFrameTarget target, KeyFrame[] keyFrames) ->
        {
            if (target.getType() != KeyFrameCategory.CAMERA)
            {
                return;
            }

            for (KeyFrame keyFrame : keyFrames)
            {
                BufferedImage endImage = selectedImage;
                if (kfsm.getPrimary() == keyFrame)
                {
                    endImage = primaryImage;
                }

                int x = (int) ((keyFrame.getTick() + getHScroll() + change[0]) * zoomFactor);
                int y = rowHeightOffset + rowHeight - yImageOffset;
                g.drawImage(endImage, x - xImageOffset, y, null);
            }
        });

        g.setComposite(composite);
    }

    @Override
    public void updateSelectedKeyFrameOnPressed(boolean shiftDown)
    {
        LinkedHashMap<KeyFrameTarget, KeyFrame[]> clickedKeyFrames = getClickedKeyFrames();
        if (clickedKeyFrames.isEmpty())
        {
            return;
        }

        Map.Entry<KeyFrameTarget, KeyFrame[]> firstEntry = clickedKeyFrames.entrySet().iterator().next();
        KeyFrameTarget target = firstEntry.getKey();
        if (target.getType() != KeyFrameCategory.CAMERA)
        {
            return;
        }

        KeyFrame[] keyFrames = firstEntry.getValue(); //only registers for the first clicked keyframe

        if (!shiftDown)
        {
            if (!kfsm.containsKeyFrame(keyFrames))
            {
                kfsm.clear();
            }
        }

        KeyFrame primary = keyFrames[0];
        kfsm.add(target, keyFrames, primary);
        getTimeSheetPanel().onKeyFrameSelectionChanged();
    }

    @Override
    public LinkedHashMap<KeyFrameTarget, KeyFrame[]> getKeyFrameClicked(Point point)
    {
        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame[] keyFrames = cameraManager.getKeyFrames();
        for (int e = 0; e < keyFrames.length; e++)
        {
            KeyFrame keyFrame = keyFrames[e];
            int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
            int x2 = x1 + image.getWidth();
            int y1 = rowHeightOffset + rowHeight - yImageOffset;
            int y2 = y1 + image.getHeight();

            if (point.getX() >= x1 && point.getX() <= x2)
            {
                if (point.getY() >= y1 && point.getY() <= y2)
                {
                    LinkedHashMap<KeyFrameTarget, KeyFrame[]> selectedKeyFrames = new LinkedHashMap<>();
                    selectedKeyFrames.put(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), new KeyFrame[]{keyFrame});
                    return selectedKeyFrames;
                }
            }
        }

        return null;
    }

    @Override
    public void updateSelectedKeyFrameOnRelease(Point point, boolean shiftKey)
    {
        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame foundKeyFrame = null;
        KeyFrame[] keyFrames = cameraManager.getKeyFrames();
        for (int e = 0; e < keyFrames.length; e++)
        {
            KeyFrame keyFrame = keyFrames[e];
            int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
            int x2 = x1 + image.getWidth();
            int y1 = rowHeightOffset + rowHeight - yImageOffset;
            int y2 = y1 + image.getHeight();

            if (point.getX() >= x1 && point.getX() <= x2
                    && point.getY() >= y1 && point.getY() <= y2)
            {
                foundKeyFrame = keyFrame;
                break;
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

            KeyFrame finalFoundKeyFrame = foundKeyFrame;
            if (Arrays.stream(keyFrames).noneMatch(e -> e == finalFoundKeyFrame))
            {
                return;
            }

            kfsm.add(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), foundKeyFrame);
        }
    }

    @Override
    public boolean checkRectangleForKeyFrames(Point point, boolean shiftKey)
    {
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

        LinkedHashMap<KeyFrameTarget, KeyFrame[]> selectedKeyFrames = kfsm.getSelected();
        if (!shiftKey)
        {
            kfsm.clear();
        }

        KeyFrame primary = null;
        KeyFrame[] keyFrames = cameraManager.getKeyFrames();
        KeyFrame[] foundKeyFrames = selectedKeyFrames.get(new KeyFrameTarget(KeyFrameCategory.CAMERA, null));
        if (foundKeyFrames == null)
        {
            foundKeyFrames = new KeyFrame[0];
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
            int ky1 = rowHeightOffset + rowHeight - yImageOffset;

            Rectangle2D frameRect = new Rectangle(kx1, ky1, image.getWidth(), image.getHeight());

            if (rectangle.intersects(frameRect))
            {
                foundKeyFrames = ArrayUtils.add(foundKeyFrames, keyFrame);
                primary = keyFrame;
            }
        }

        kfsm.addCameraGroups(foundKeyFrames, primary);
        attributePanel.updateAttributes();
        if (primary != null)
        {
            attributePanel.switchCards(primary.getKeyFrameType());
        }
        return true;
    }

    @Override
    public void updateTableSelection(Point p)
    {
        attributePanel.switchCards(KeyFrameType.CAMERA);
        attributePanel.updateAttributes();
    }
}
