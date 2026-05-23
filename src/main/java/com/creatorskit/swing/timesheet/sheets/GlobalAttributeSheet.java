package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.saves.GlobalKeyFrames;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Dedicated timeline sheet for the three global keyframe types -- Camera,
 * Screen Fade, Screen Shake. Reads exclusively from the central
 * {@link GlobalKeyFrames} store rather than per-Character frames so the view
 * is fully independent of which Object (if any) is selected. The toggle
 * button in the labels column switches between this sheet and the local
 * {@link AttributeSheet} via a CardLayout in {@link TimeSheetPanel}.
 *
 * <p>Structure mirrors AttributeSheet (3 rows alphabetical: Camera, Screen
 * Fade, Screen Shake) but each draw/hit-test path iterates the central store
 * directly. No per-Character logic, no global/local mode toggle to juggle.
 */
@Getter
@Setter
public class GlobalAttributeSheet extends TimeSheet
{
    private ManagerTree tree;
    private CreatorsConfig config;
    private AttributePanel attributePanel;

    private final javax.swing.Timer pulseTimer;
    private static final int PULSE_PERIOD_MS = 1500;

    public GlobalAttributeSheet(ToolBoxFrame toolBox, CreatorsConfig config, ManagerTree tree, AttributePanel attributePanel)
    {
        super(toolBox, config, tree, attributePanel);
        this.config = config;
        this.tree = tree;
        this.attributePanel = attributePanel;

        setIndexBuffers(0);
        setSelectedIndex(1);
        this.rowHeightOffset = 1;
        this.rowHeight = 24;

        pulseTimer = new javax.swing.Timer(33, e -> {
            if (getTimeSheetPanel() != null
                    && getTimeSheetPanel().getSelectedKeyFrames() != null
                    && getTimeSheetPanel().getSelectedKeyFrames().length > 0)
            {
                repaint();
            }
        });
        pulseTimer.start();
    }

    /** Position of {@code type} in the global-only row stack, or -1 if local. */
    private int displayRowIndex(KeyFrameType type)
    {
        return KeyFrameType.getGlobalDisplayIndex(type);
    }

    private float pulseAlpha()
    {
        double t = (System.currentTimeMillis() % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        double s = 0.5 + 0.5 * Math.sin(2 * Math.PI * t);
        return (float) (0.55 + 0.45 * s);
    }

    private GlobalKeyFrames store()
    {
        TimeSheetPanel ts = getTimeSheetPanel();
        if (ts == null || ts.getPlugin() == null) return null;
        return ts.getPlugin().getGlobalKeyFrames();
    }

    @Override
    public void drawBackgroundText(Graphics g)
    {
        // Globals don't have a per-Object name to splash behind the rows.
    }

    @Override
    public void drawHighlight(Graphics g)
    {
        if (attributePanel == null) return;
        int dr = displayRowIndex(attributePanel.getSelectedKeyFramePage());
        if (dr < 0)
        {
            return; // active card isn't a global -- nothing to highlight here
        }
        int row = dr + 1; // +1 for the spacer header row above the labels
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, row * rowHeight + rowHeightOffset - getVScroll(), this.getWidth(), rowHeight);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        GlobalKeyFrames s = store();
        if (s == null) return;

        g.setColor(new Color(219, 137, 0));
        BufferedImage image = getKeyframeImage();
        int imageHeight = image.getHeight();
        int yImageOffset = (imageHeight - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        // Iterate the canonical global-type list so adding a new global type
        // (e.g. SOUND_x) automatically gets a row drawn here without a per-
        // type edit. Order matches GLOBAL_KEYFRAME_TYPES_ALPHABETICAL.
        for (KeyFrameType type : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
        {
            drawRow(g, s.getGlobalKeyFramesByType(type), type, image, imageHeight, yImageOffset, xImageOffset, zoomFactor);
        }
    }

    private void drawRow(Graphics g, KeyFrame[] keyFrames, KeyFrameType type, BufferedImage image, int imageHeight, int yImageOffset, int xImageOffset, double zoomFactor)
    {
        if (keyFrames == null || keyFrames.length == 0) return;
        int displayRow = displayRowIndex(type);
        if (displayRow < 0) return;

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
            int y = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;

            switch (type)
            {
                case SCREEN_FADE:
                    ScreenFadeKeyFrame sfkf = (ScreenFadeKeyFrame) keyFrame;
                    drawTail(g, e, keyFrames, sfkf.totalDurationTicks(), zoomFactor, sfkf.getTick(), x, y, imageHeight);
                    break;
                case SCREEN_SHAKE:
                    ScreenShakeKeyFrame sskf = (ScreenShakeKeyFrame) keyFrame;
                    drawTail(g, e, keyFrames, sskf.getDurationTicks(), zoomFactor, sskf.getTick(), x, y, imageHeight);
                    break;
                case CAMERA:
                    // Tail spans from this keyframe to the next; no duration
                    // field anymore, the segment length is implicit. The last
                    // keyframe gets no tail since it holds indefinitely.
                    if (e + 1 < keyFrames.length)
                    {
                        double span = keyFrames[e + 1].getTick() - keyFrame.getTick();
                        int pathLength = (int) (span * zoomFactor);
                        g.drawLine(x, y + imageHeight / 2, x + pathLength - 1, y + imageHeight / 2);
                    }
                    break;
                default: break;
            }

            if (endImage == getKeyframeSelected() && g instanceof Graphics2D)
            {
                Graphics2D g2 = (Graphics2D) g;
                Composite prev = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulseAlpha()));
                g2.drawImage(endImage, x - xImageOffset, y, null);
                g2.setComposite(prev);
            }
            else
            {
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
            if (difference < duration) duration = difference;
        }
        int pathLength = (int) (duration * zoomFactor);
        g.drawLine(x, y + imageHeight / 2, x + pathLength - 1, y + imageHeight / 2);
    }

    @Override
    public void drawPreviewKeyFrames(Graphics2D g)
    {
        if (!isKeyFrameClicked()) return;
        KeyFrame[] selectedKeyFrames = getSelectedKeyFrames();
        if (selectedKeyFrames.length == 0) return;

        TimelineUnits timelineUnits = config.timelineUnits();

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2F));

        double pointerX = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX();
        double mouseX = Math.max(0, Math.min(pointerX, getWidth()));
        double xCurrentTime = currentTimeToMouseX();

        double change;
        if (Math.abs(Math.abs(mouseX) - Math.abs(xCurrentTime)) > DRAG_STICK_RANGE)
        {
            change = round(timelineUnits, (mouseX - getMousePointOnPressed().getX()) * getZoom() / getWidth());
        }
        else
        {
            KeyFrame keyFrame = getClickedKeyFrames()[0];
            change = round(timelineUnits, getCurrentTime() - keyFrame.getTick());
        }

        int imageHeight = image.getHeight();
        for (KeyFrame keyFrame : selectedKeyFrames)
        {
            KeyFrameType type = keyFrame.getKeyFrameType();
            int displayRow = displayRowIndex(type);
            if (displayRow < 0) continue;

            int x = (int) ((keyFrame.getTick() + getHScroll() + change) * zoomFactor);
            int y = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;

            switch (type)
            {
                case SCREEN_FADE:
                    drawPreviewTail(g, x, y, imageHeight, ((ScreenFadeKeyFrame) keyFrame).totalDurationTicks(), zoomFactor);
                    break;
                case SCREEN_SHAKE:
                    drawPreviewTail(g, x, y, imageHeight, ((ScreenShakeKeyFrame) keyFrame).getDurationTicks(), zoomFactor);
                    break;
                case CAMERA:
                    // Duration field is gone for Camera kfs -- preview tail
                    // length isn't meaningful here either (the actual segment
                    // is to the next kf, but the dragged copy could land
                    // anywhere). Skip the tail entirely.
                    break;
                default: break;
            }

            g.drawImage(image, x - xImageOffset, y, null);
        }
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
        KeyFrame[] clickedKeyFrames = getClickedKeyFrames();
        if (clickedKeyFrames.length == 0) return;
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
        GlobalKeyFrames s = store();
        if (s == null) return null;
        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        for (KeyFrameType type : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
        {
            KeyFrame hit = hitRow(point, image, xImageOffset, yImageOffset, zoomFactor,
                    s.getGlobalKeyFramesByType(type), type);
            if (hit != null) return new KeyFrame[]{hit};
        }
        return null;
    }

    private KeyFrame hitRow(Point point, BufferedImage image, int xImageOffset, int yImageOffset, double zoomFactor, KeyFrame[] keyFrames, KeyFrameType type)
    {
        if (keyFrames == null || keyFrames.length == 0) return null;
        int displayRow = displayRowIndex(type);
        if (displayRow < 0) return null;
        int y1 = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;
        int y2 = y1 + image.getHeight();
        if (point.getY() < y1 || point.getY() > y2) return null;
        for (KeyFrame keyFrame : keyFrames)
        {
            int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
            int x2 = x1 + image.getWidth();
            if (point.getX() >= x1 && point.getX() <= x2) return keyFrame;
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
        KeyFrame[] hit = getKeyFrameClicked(point);
        KeyFrame foundKeyFrame = (hit != null && hit.length > 0) ? hit[0] : null;

        if (foundKeyFrame != null)
        {
            if (shiftKey)
            {
                KeyFrame[] selectedKeyFrames = getSelectedKeyFrames();
                boolean alreadyContains = false;
                for (KeyFrame kf : selectedKeyFrames)
                {
                    if (kf == foundKeyFrame) { alreadyContains = true; break; }
                }
                if (!alreadyContains)
                {
                    setSelectedKeyFrames(ArrayUtils.add(selectedKeyFrames, foundKeyFrame));
                }
            }
            else
            {
                setSelectedKeyFrames(new KeyFrame[]{foundKeyFrame});
            }
        }
        else if (!shiftKey)
        {
            setSelectedKeyFrames(new KeyFrame[0]);
        }
    }

    @Override
    public void checkRectangleForKeyFrames(Point point, boolean shiftKey)
    {
        if (!isAllowRectangleSelect()) return;
        GlobalKeyFrames s = store();
        if (s == null) return;

        Point absoluteMouse = MouseInfo.getPointerInfo().getLocation();
        Point rectangleSelectStart = getMousePointOnPressed();

        int x1 = (int) rectangleSelectStart.getX();
        int x2 = (int) (absoluteMouse.getX() - getLocationOnScreen().getX());
        int y1 = (int) rectangleSelectStart.getY();
        int y2 = (int) (absoluteMouse.getY() - getLocationOnScreen().getY());
        if (Math.abs(x1 - x2) < 10 && Math.abs(y1 - y2) < 10) return;

        int startX, endX, startY, endY;
        if (x1 < x2) { startX = x1; endX = x2; } else { startX = x2; endX = x1; }
        if (y1 < y2) { startY = y1; endY = y2; } else { startY = y2; endY = y1; }
        int buffer = 1;
        if (startX < buffer) startX = buffer;
        if (endX > getWidth() - 2) endX = getWidth() - 2;
        if (startY < buffer) startY = buffer;
        if (endY > getHeight() - 2) endY = getHeight() - 2;

        Rectangle2D rectangle = new Rectangle(startX, startY, endX - startX, endY - startY);

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame[] foundKeyFrames = shiftKey ? getSelectedKeyFrames() : new KeyFrame[0];
        for (KeyFrameType type : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
        {
            foundKeyFrames = rectRow(rectangle, image, xImageOffset, yImageOffset, zoomFactor,
                    s.getGlobalKeyFramesByType(type), type, foundKeyFrames);
        }
        setSelectedKeyFrames(foundKeyFrames);
    }

    private KeyFrame[] rectRow(Rectangle2D rectangle, BufferedImage image, int xImageOffset, int yImageOffset, double zoomFactor, KeyFrame[] keyFrames, KeyFrameType type, KeyFrame[] foundKeyFrames)
    {
        if (keyFrames == null || keyFrames.length == 0) return foundKeyFrames;
        int displayRow = displayRowIndex(type);
        if (displayRow < 0) return foundKeyFrames;
        int ky1 = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;
        for (KeyFrame keyFrame : keyFrames)
        {
            boolean alreadyContains = false;
            for (KeyFrame kf : foundKeyFrames)
            {
                if (keyFrame == kf) { alreadyContains = true; break; }
            }
            if (alreadyContains) continue;
            int kx1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
            Rectangle2D frameRect = new Rectangle(kx1, ky1, image.getWidth(), image.getHeight());
            if (rectangle.intersects(frameRect))
            {
                foundKeyFrames = ArrayUtils.add(foundKeyFrames, keyFrame);
            }
        }
        return foundKeyFrames;
    }

    /**
     * Mirror of {@link AttributeSheet#onMouseButton3Pressed} for the global
     * row stack. Same Premiere Pro-style context menu, but the gap search
     * runs against the central GlobalKeyFrames store instead of per-Character
     * frame matrices.
     */
    @Override
    public void onMouseButton3Pressed(Point p)
    {
        if (getKeyFrameClicked(p) != null) return;

        int displayRow = (int) Math.floor((p.getY() + getVScroll() - rowHeightOffset - rowHeight) / (double) rowHeight);
        if (displayRow < 0 || displayRow >= KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL.length) return;
        KeyFrameType type = KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL[displayRow];

        double clickTick = (double) p.getX() * getZoom() / getWidth() - getHScroll();
        double[] gap = findGapForRippleDelete(type, clickTick);
        showRippleDeleteContextPopup(p, gap[0], gap[1], type);
    }

    private double[] findGapForRippleDelete(KeyFrameType type, double clickTick)
    {
        GlobalKeyFrames s = store();
        KeyFrame[] arr = s == null ? new KeyFrame[0] : s.getGlobalKeyFramesByType(type);

        double prevTick = Double.NEGATIVE_INFINITY;
        double nextTick = Double.POSITIVE_INFINITY;
        for (KeyFrame kf : arr)
        {
            if (kf == null) continue;
            if (kf.getTick() <= clickTick && kf.getTick() > prevTick) prevTick = kf.getTick();
            if (kf.getTick() > clickTick && kf.getTick() < nextTick) nextTick = kf.getTick();
        }
        double from = prevTick == Double.NEGATIVE_INFINITY ? 0 : prevTick + 1;
        double to = nextTick == Double.POSITIVE_INFINITY ? clickTick : nextTick - 1;
        if (to < from) to = from;
        return new double[]{from, to};
    }

    private void showRippleDeleteContextPopup(Point p, double from, double to, KeyFrameType type)
    {
        TimeSheetPanel tsp = getTimeSheetPanel();
        if (tsp == null) return;

        JPopupMenu menu = new JPopupMenu();

        JMenuItem oneItem = new JMenuItem(
                String.format("Ripple delete gap on %s  [%.1f-%.1f]", type.getName(), from, to));
        oneItem.addActionListener(e -> tsp.executeRippleDeleteInstant(from, to, type));
        menu.add(oneItem);

        // "All properties" from the global sheet includes per-Character locals
        // too -- consistent with the Tools dialog meaning of "All".
        JMenuItem allItem = new JMenuItem(
                String.format("Ripple delete gap on all properties  [%.1f-%.1f]", from, to));
        allItem.addActionListener(e -> tsp.executeRippleDeleteInstant(from, to, null));
        menu.add(allItem);

        menu.addSeparator();

        JMenuItem dialogItem = new JMenuItem("Ripple Delete... (pick range / scope)");
        dialogItem.addActionListener(e -> tsp.showRippleDeleteDialog(from, to, type));
        menu.add(dialogItem);

        menu.show(this, (int) p.getX(), (int) p.getY());
    }
}
