package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.FontManager;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Per-Character (local) timeline sheet. Shows the 17 LOCAL_KEYFRAME_TYPES_ALPHABETICAL
 * rows for the currently-visible Characters. Globals (Camera / Screen Fade /
 * Screen Shake) live in {@link GlobalAttributeSheet} -- the toggle button in
 * the labels column switches between this sheet and that one, so each view
 * stays scoped to a single concern.
 */
@Getter
@Setter
public class AttributeSheet extends TimeSheet
{
    private ManagerTree tree;
    private CreatorsConfig config;
    private AttributePanel attributePanel;

    /**
     * 30 fps repaint timer that drives the breathing-pulse alpha on selected
     * keyframe icons. Cheaper than a global "always repaint" loop: only ticks
     * when at least one keyframe is selected; sits idle otherwise.
     */
    private final javax.swing.Timer pulseTimer;
    /** Period in ms for one full breathing cycle (dim -> bright -> dim). */
    private static final int PULSE_PERIOD_MS = 1500;

    public AttributeSheet(ToolBoxFrame toolBox, CreatorsConfig config, ManagerTree tree, AttributePanel attributePanel)
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

    /**
     * Returns the row index where {@code type} should be drawn, or -1 if
     * {@code type} is a global (which belongs in the GlobalAttributeSheet
     * instead). Rows are ordered by display name -- see
     * {@link KeyFrameType#LOCAL_KEYFRAME_TYPES_ALPHABETICAL}.
     */
    private int displayRowIndex(KeyFrameType type)
    {
        return KeyFrameType.getLocalDisplayIndex(type);
    }

    /**
     * Current breathing-pulse alpha in [0.55, 1.0] for the keyframe-selected
     * highlight. Sine wave over {@link #PULSE_PERIOD_MS}, floored at 0.55 so
     * icons stay readable at the dim end.
     */
    private float pulseAlpha()
    {
        double t = (System.currentTimeMillis() % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        double s = 0.5 + 0.5 * Math.sin(2 * Math.PI * t);
        return (float) (0.55 + 0.45 * s);
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
        // The highlight follows the active card. If the active card is a
        // global (Camera / Fade / Shake), skip -- that card belongs to the
        // GlobalAttributeSheet which paints its own highlight.
        int row;
        if (attributePanel != null)
        {
            KeyFrameType selected = attributePanel.getSelectedKeyFramePage();
            int dr = displayRowIndex(selected);
            if (dr < 0)
            {
                return;
            }
            row = dr + 1; // +1 for the spacer header row at labels[0]
        }
        else
        {
            row = getSelectedIndex() + getIndexBuffers();
        }
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, row * rowHeight + rowHeightOffset - getVScroll(), this.getWidth(), rowHeight);
    }

    /** Header strip height (above the rows) where the block name renders
     *  at full opacity. The rest of the block fill is translucent so the
     *  keyframe icons inside stay visible. */
    private static final int BLOCK_HEADER_HEIGHT = 18;
    /** Alpha for the body fill of a block rect. Low enough that the
     *  alternating row colours + keyframe icons inside still read; high
     *  enough that the block is unambiguous. */
    private static final int BLOCK_BODY_ALPHA = 70;

    @Override
    public void drawBlocks(Graphics g)
    {
        java.util.List<Character> visible = getVisibleCharacters();
        if (visible.isEmpty()) return;
        BufferedImage keyImg = getKeyframeImage();
        int xImageOffset = keyImg.getWidth() / 2;
        double zoomFactor = (double) this.getWidth() / getZoom();

        Graphics2D g2 = (Graphics2D) g;
        Font origFont = g2.getFont();
        g2.setFont(FontManager.getRunescapeBoldFont());
        FontMetrics fm = g2.getFontMetrics();

        // Full body span: from the top of row 0 (just under the time-header
        // chip) down to the bottom of the last keyframe-type row. Range
        // covers every row whether or not the block has a kf in it -- the
        // dumb range-based model says "everything in [startTick, endTick]
        // is a member", and the visual matches that.
        int bodyTop = rowHeightOffset + rowHeight - getVScroll();
        int bodyBottom = rowHeightOffset + rowHeight
                + rowHeight * KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL.length
                - getVScroll();
        if (bodyBottom > this.getHeight()) bodyBottom = this.getHeight();

        for (Character c : visible)
        {
            java.util.List<com.creatorskit.swing.timesheet.keyframe.Block> blocks = c.getBlocks();
            if (blocks == null || blocks.isEmpty()) continue;
            for (com.creatorskit.swing.timesheet.keyframe.Block block : blocks)
            {
                if (block == null) continue;

                double startTick = block.getStartTick();
                double endTick = block.getEndTick();
                int leftX = (int) ((startTick + getHScroll()) * zoomFactor) - xImageOffset;
                int rightX = (int) ((endTick + getHScroll()) * zoomFactor) + xImageOffset;
                int rectW = Math.max(1, rightX - leftX);
                int rectH = Math.max(1, bodyBottom - bodyTop);
                if (rectH <= 0) continue;

                Color baseColour = new Color(block.getColorRgb());

                // Body: translucent fill across the full timeline height so
                // keyframe icons inside the block stay visible (tinted).
                Color body = new Color(baseColour.getRed(), baseColour.getGreen(),
                        baseColour.getBlue(), BLOCK_BODY_ALPHA);
                g2.setColor(body);
                g2.fillRect(leftX, bodyTop, rectW, rectH);

                // Header strip: opaque colour with the block name, sits at
                // the top of the rows. Drawn AFTER the body so it covers
                // the body's alpha in the header region.
                int hdrTop = bodyTop;
                int hdrH = Math.min(BLOCK_HEADER_HEIGHT, rectH);
                g2.setColor(baseColour);
                g2.fillRect(leftX, hdrTop, rectW, hdrH);
                // 1px darker outline around the entire block rect so it
                // separates from neighbours and from the background.
                g2.setColor(baseColour.darker());
                g2.drawRect(leftX, bodyTop, rectW - 1, rectH - 1);

                // Name label centred in the header strip. White with a
                // 1px black shadow so it reads on every palette colour.
                // Clipped to the header rect to prevent overrun.
                String name = block.getName();
                if (name != null && !name.isEmpty())
                {
                    java.awt.Shape oldClip = g2.getClip();
                    g2.setClip(leftX, hdrTop, rectW, hdrH);
                    int textW = fm.stringWidth(name);
                    int textX = leftX + (rectW - textW) / 2;
                    int textY = hdrTop + (hdrH + fm.getAscent()) / 2 - 2;
                    g2.setColor(Color.BLACK);
                    g2.drawString(name, textX + 1, textY + 1);
                    g2.setColor(Color.WHITE);
                    g2.drawString(name, textX, textY);
                    g2.setClip(oldClip);
                }
            }
        }
        g2.setFont(origFont);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        java.util.List<Character> visible = getVisibleCharacters();
        if (visible.isEmpty())
        {
            return;
        }

        g.setColor(new Color(219, 137, 0));

        BufferedImage image = getKeyframeImage();
        int imageHeight = image.getHeight();
        int yImageOffset = (imageHeight - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();
        boolean multi = visible.size() > 1;

        for (Character character : visible)
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
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrameType type = KeyFrameType.getKeyFrameType(i);

            KeyFrame[] keyFrames = frames[i];
            if (keyFrames == null)
            {
                continue;
            }

            // Skip globals -- they're rendered exclusively by GlobalAttributeSheet.
            int displayRow = displayRowIndex(type);
            if (displayRow < 0)
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
                int y = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;

                Color tailColor = g.getColor();
                if (multi && character.getColor() != null)
                {
                    g.setColor(character.getColor());
                }
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
                    case PROJECTILE:
                        ProjectileKeyFrame pkf = (ProjectileKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, pkf.getStartDelayTicks() + pkf.getDurationTicks(), zoomFactor, pkf.getTick(), x, y, imageHeight);
                        break;
                    case SHIELD:
                        ShieldKeyFrame shkf = (ShieldKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, shkf.getDuration(), zoomFactor, shkf.getTick(), x, y, imageHeight);
                        break;
                    case SPECIAL:
                        SpecialKeyFrame spkf = (SpecialKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, spkf.getDuration(), zoomFactor, spkf.getTick(), x, y, imageHeight);
                        break;
                    case PULSE:
                        PulseKeyFrame plkf = (PulseKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, plkf.getFadeInTicks() + plkf.getHoldTicks() + plkf.getFadeOutTicks(), zoomFactor, plkf.getTick(), x, y, imageHeight);
                        break;
                    default:
                        break;
                }


                if (endImage == getKeyframeSelected() && g instanceof Graphics2D)
                {
                    Graphics2D g2 = (Graphics2D) g;
                    java.awt.Composite prevComposite = g2.getComposite();
                    g2.setComposite(java.awt.AlphaComposite.getInstance(
                            java.awt.AlphaComposite.SRC_OVER, pulseAlpha()));
                    g2.drawImage(endImage, x - xImageOffset, y, null);
                    g2.setComposite(prevComposite);
                }
                else
                {
                    g.drawImage(endImage, x - xImageOffset, y, null);
                }

                if (multi && character.getColor() != null)
                {
                    Color prev = g.getColor();
                    g.setColor(character.getColor());
                    int dotSize = 6;
                    g.fillOval(x - xImageOffset, y + endImage.getHeight() - dotSize, dotSize, dotSize);
                    g.setColor(prev);
                }
                else
                {
                    g.setColor(tailColor);
                }
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

        KeyFrame[] selectedKeyFrames = getSelectedKeyFrames();
        if (selectedKeyFrames.length == 0)
        {
            return;
        }

        TimelineUnits timelineUnits = config.timelineUnits();

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
            change = round(timelineUnits, (mouseX - getMousePointOnPressed().getX()) * getZoom() / getWidth());
        }
        else
        {
            KeyFrame keyFrame = getClickedKeyFrames()[0];
            change = round(timelineUnits, getCurrentTime() - keyFrame.getTick());
        }

        int imageHeight = image.getHeight();

        for (int e = 0; e < selectedKeyFrames.length; e++)
        {
            KeyFrame keyFrame = selectedKeyFrames[e];
            KeyFrameType type = keyFrame.getKeyFrameType();
            int displayRow = displayRowIndex(type);
            if (displayRow < 0)
            {
                continue; // Globals don't render here.
            }

            int x = (int) ((keyFrame.getTick() + getHScroll() + change) * zoomFactor);
            int y = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;

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
                case SHIELD:
                    ShieldKeyFrame shkf = (ShieldKeyFrame) keyFrame;
                    drawPreviewTail(g, x, y, imageHeight, shkf.getDuration(), zoomFactor);
                    break;
                case SPECIAL:
                    SpecialKeyFrame spkf = (SpecialKeyFrame) keyFrame;
                    drawPreviewTail(g, x, y, imageHeight, spkf.getDuration(), zoomFactor);
                    break;
                case PULSE:
                    PulseKeyFrame plkf = (PulseKeyFrame) keyFrame;
                    drawPreviewTail(g, x, y, imageHeight, plkf.getFadeInTicks() + plkf.getHoldTicks() + plkf.getFadeOutTicks(), zoomFactor);
                    break;
                default:
                    break;
            }

            g.drawImage(bufferedImage, x - xImageOffset, y, null);
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
        java.util.List<Character> visible = getVisibleCharacters();
        if (visible.isEmpty())
        {
            return null;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        for (Character c : visible)
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

                int displayRow = displayRowIndex(KeyFrameType.getKeyFrameType(i));
                if (displayRow < 0)
                {
                    continue;
                }

                for (int e = 0; e < keyFrames.length; e++)
                {
                    KeyFrame keyFrame = keyFrames[e];
                    int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
                    int x2 = x1 + image.getWidth();
                    int y1 = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;
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
        }

        return null;
    }

    @Override
    public void updateSelectedKeyFrameOnRelease(Point point, boolean shiftKey)
    {
        java.util.List<Character> visible = getVisibleCharacters();

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        KeyFrame foundKeyFrame = null;
        outer:
        for (Character c : visible)
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

                int displayRow = displayRowIndex(KeyFrameType.getKeyFrameType(i));
                if (displayRow < 0)
                {
                    continue;
                }

                for (int e = 0; e < keyFrames.length; e++)
                {
                    KeyFrame keyFrame = keyFrames[e];
                    int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
                    int x2 = x1 + image.getWidth();
                    int y1 = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;
                    int y2 = y1 + image.getHeight();

                    if (point.getX() >= x1 && point.getX() <= x2
                            && point.getY() >= y1 && point.getY() <= y2)
                    {
                        foundKeyFrame = keyFrame;
                        break outer;
                    }
                }
            }
        }

        if (foundKeyFrame != null)
        {
            if (shiftKey)
            {
                KeyFrame[] selectedKeyFrames = getSelectedKeyFrames();
                boolean alreadyContains = false;
                for (KeyFrame kf : selectedKeyFrames)
                {
                    if (kf == foundKeyFrame)
                    {
                        alreadyContains = true;
                        break;
                    }
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
        java.util.List<Character> visible = getVisibleCharacters();
        if (visible.isEmpty())
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

        for (Character c : visible)
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

                int displayRow = displayRowIndex(KeyFrameType.getKeyFrameType(i));
                if (displayRow < 0)
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
                    int ky1 = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;

                    Rectangle2D frameRect = new Rectangle(kx1, ky1, image.getWidth(), image.getHeight());

                    if (rectangle.intersects(frameRect))
                    {
                        foundKeyFrames = ArrayUtils.add(foundKeyFrames, keyFrame);
                    }
                }
            }
        }

        setSelectedKeyFrames(foundKeyFrames);
    }

    /**
     * Right-click on the body sheet shows a Premiere Pro-style ripple-delete
     * context menu. Skipped if the click landed on an existing keyframe --
     * that's an "edit this kf" context that belongs elsewhere. On empty
     * space, we identify the property row and tick the user clicked into,
     * walk the visible Characters' keyframes of that row to find the
     * surrounding gap [prevKf+1, nextKf-1], and offer:
     *   - instant ripple delete on JUST that property's gap,
     *   - instant ripple delete on ALL properties for the same range,
     *   - "Ripple Delete..." which opens the full dialog (pre-filled).
     */
    @Override
    public void onMouseButton3Pressed(Point p)
    {
        if (getKeyFrameClicked(p) != null) return;

        // Block right-click intercepts BEFORE the empty-space ripple-delete
        // menu so the user can manage the block directly via its rect.
        if (tryHandleBlockRightClick(p)) return;

        // Row math is the inverse of the y1 formula used by drawKeyFrames /
        // getKeyFrameClicked: y1 = rowHeightOffset + rowHeight + rowHeight*dr - vScroll.
        // The first data row sits one rowHeight below the header chip row.
        int displayRow = (int) Math.floor((p.getY() + getVScroll() - rowHeightOffset - rowHeight) / (double) rowHeight);
        if (displayRow < 0 || displayRow >= KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL.length) return;
        KeyFrameType type = KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL[displayRow];

        double clickTick = (double) p.getX() * getZoom() / getWidth() - getHScroll();
        double[] gap = findGapForRippleDelete(type, clickTick);
        showRippleDeleteContextPopup(p, gap[0], gap[1], type);
    }

    /**
     * Returns the (Character, Block) pair whose rect contains {@code p}, or
     * null if no block is hit. Iterates visible Characters and their blocks;
     * uses the same rect math as drawBlocks so hit-test agrees with what's
     * painted. First match wins -- blocks are disjoint in time on a single
     * Character per the spec, but with multi-Character selection two
     * different Characters could have overlapping block rects in screen
     * space; the iteration order matches visible Character order.
     */
    private BlockHit findBlockAt(Point p)
    {
        java.util.List<Character> visible = getVisibleCharacters();
        if (visible.isEmpty()) return null;
        BufferedImage keyImg = getKeyframeImage();
        int xImageOffset = keyImg.getWidth() / 2;
        double zoomFactor = (double) this.getWidth() / getZoom();

        // Same body span as drawBlocks -- full timeline height, since
        // range-based blocks cover all rows in their time range.
        int bodyTop = rowHeightOffset + rowHeight - getVScroll();
        int bodyBottom = rowHeightOffset + rowHeight
                + rowHeight * KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL.length
                - getVScroll();
        if (p.getY() < bodyTop || p.getY() > bodyBottom) return null;

        for (Character c : visible)
        {
            java.util.List<com.creatorskit.swing.timesheet.keyframe.Block> blocks = c.getBlocks();
            if (blocks == null) continue;
            for (com.creatorskit.swing.timesheet.keyframe.Block block : blocks)
            {
                if (block == null) continue;
                int leftX = (int) ((block.getStartTick() + getHScroll()) * zoomFactor) - xImageOffset;
                int rightX = (int) ((block.getEndTick() + getHScroll()) * zoomFactor) + xImageOffset;
                if (p.getX() >= leftX && p.getX() <= rightX)
                {
                    return new BlockHit(c, block);
                }
            }
        }
        return null;
    }

    private static final class BlockHit
    {
        final Character character;
        final com.creatorskit.swing.timesheet.keyframe.Block block;
        BlockHit(Character character, com.creatorskit.swing.timesheet.keyframe.Block block)
        {
            this.character = character;
            this.block = block;
        }
    }

    @Override
    public boolean tryHandleBlockLeftClick(Point p, boolean shiftDown)
    {
        BlockHit hit = findBlockAt(p);
        if (hit == null) return false;
        TimeSheetPanel tsp = getTimeSheetPanel();
        if (tsp == null) return false;
        // Resolve members live -- range-based block means "every kf on
        // this Character with tick in [startTick, endTick]". Selecting all
        // of them mirrors the previous block-membership behaviour without
        // a separately-tracked member list. setSelectedBlock fires after
        // setSelectedKeyFrames since the latter clears block selection.
        java.util.List<com.creatorskit.swing.timesheet.keyframe.KeyFrame> members =
                hit.block.resolveMembers(hit.character);
        tsp.setSelectedKeyFrames(members.toArray(new com.creatorskit.swing.timesheet.keyframe.KeyFrame[0]));
        tsp.setSelectedBlock(hit.block, hit.character);
        return true;
    }

    @Override
    public boolean tryHandleBlockRightClick(Point p)
    {
        BlockHit hit = findBlockAt(p);
        if (hit == null) return false;
        showBlockContextMenu(p, hit);
        return true;
    }

    private void showBlockContextMenu(Point p, BlockHit hit)
    {
        TimeSheetPanel tsp = getTimeSheetPanel();
        if (tsp == null) return;
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();

        javax.swing.JMenuItem enter = new javax.swing.JMenuItem("Enter block (nested view) -- coming soon");
        enter.setEnabled(false);
        menu.add(enter);

        menu.addSeparator();

        javax.swing.JMenuItem rename = new javax.swing.JMenuItem("Rename / Recolour...");
        rename.addActionListener(e -> tsp.editBlockNameAndColour(hit.character, hit.block));
        menu.add(rename);

        javax.swing.JMenuItem dissolve = new javax.swing.JMenuItem("Dissolve block (keep keyframes)");
        dissolve.addActionListener(e -> tsp.dissolveBlock(hit.character, hit.block));
        menu.add(dissolve);

        javax.swing.JMenuItem delete = new javax.swing.JMenuItem("Delete block + keyframes...");
        delete.addActionListener(e -> tsp.deleteBlockWithConfirm(hit.character, hit.block));
        menu.add(delete);

        menu.show(this, (int) p.getX(), (int) p.getY());
    }

    /**
     * Returns {@code [from, to]} = the empty span surrounding {@code clickTick}
     * on the given property row, derived from visible Characters' keyframes.
     * If no keyframe sits to the LEFT, from=0. If no keyframe to the RIGHT,
     * to=clickTick (delete a 0-width range, effectively nothing -- but the
     * dialog still opens so the user can adjust).
     */
    private double[] findGapForRippleDelete(KeyFrameType type, double clickTick)
    {
        double prevTick = Double.NEGATIVE_INFINITY;
        double nextTick = Double.POSITIVE_INFINITY;
        int idx = KeyFrameType.getIndex(type);
        for (Character c : getVisibleCharacters())
        {
            KeyFrame[][] frames = c.getFrames();
            if (frames == null || idx >= frames.length) continue;
            KeyFrame[] row = frames[idx];
            if (row == null) continue;
            for (KeyFrame kf : row)
            {
                if (kf == null) continue;
                if (kf.getTick() <= clickTick && kf.getTick() > prevTick) prevTick = kf.getTick();
                if (kf.getTick() > clickTick && kf.getTick() < nextTick) nextTick = kf.getTick();
            }
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

        // Blocks: only show when the marquee selection forms a valid block
        // on at least one Character. Cheap to test (canCreateBlockFromSelection
        // just iterates the selection once).
        if (tsp.canCreateBlockFromSelection())
        {
            JMenuItem createBlock = new JMenuItem("Create Block...");
            createBlock.addActionListener(e -> tsp.showCreateBlockDialog());
            menu.add(createBlock);
            menu.addSeparator();
        }

        JMenuItem oneItem = new JMenuItem(
                String.format("Ripple delete gap on %s  [%.1f-%.1f]", type.getName(), from, to));
        oneItem.addActionListener(e -> tsp.executeRippleDeleteInstant(from, to, type));
        menu.add(oneItem);

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
