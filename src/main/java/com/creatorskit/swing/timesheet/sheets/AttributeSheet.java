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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

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

    /**
     * When true the sheet only draws the 3 global rows (Camera / Fade / Shake)
     * and collapses them to the top of the sheet. Mirrors the label-collapse
     * in {@link TimeSheetPanel}. Flipped by {@link TimeSheetPanel#setGlobalRowsOnlyMode(boolean)}.
     */
    @Setter
    private boolean globalRowsOnlyMode = false;

    /**
     * Returns the row index where {@code type} should be drawn given the
     * current mode, or -1 if the type is hidden. In expanded mode this is the
     * type's alphabetical position (rows are ordered by getName()); in
     * collapsed mode only the three globals are placed at rows 0-2.
     */
    private int displayRowIndex(KeyFrameType type)
    {
        if (!globalRowsOnlyMode)
        {
            return KeyFrameType.getDisplayIndex(type);
        }
        switch (type)
        {
            case SCREEN_FADE:  return 0;
            case SCREEN_SHAKE: return 1;
            case CAMERA:       return 2;
            default:           return -1;
        }
    }

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
            // Only repaint when there's something to animate; otherwise a 30Hz
            // repaint loop would chew CPU for no visual change. Timer stays
            // running but the early-out keeps the per-tick cost down.
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
     * Returns the current breathing-pulse alpha in [0.55, 1.0] for the keyframe-
     * selected highlight. A full {@link #PULSE_PERIOD_MS} cycle goes dim -> bright
     * -> dim via a sine wave. Floor of 0.55 keeps the icon clearly readable
     * even at the dim end.
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
        // In globals-only mode the static selectedIndex (a label-array index)
        // doesn't map to a row position anymore -- a SCREEN_FADE selection
        // would be drawn at row 17 (its KeyFrameType index) which is below the
        // collapsed sheet. Re-derive from the active card's type so the
        // highlight always lands under its visible row.
        int row;
        if (globalRowsOnlyMode && attributePanel != null)
        {
            int dr = displayRowIndex(attributePanel.getSelectedKeyFramePage());
            if (dr < 0)
            {
                return; // Selected card isn't a global -- no row to highlight in this mode.
            }
            row = dr + 1; // +1 for labels[0] empty header equivalence.
        }
        else
        {
            row = getSelectedIndex() + getIndexBuffers();
        }
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, row * rowHeight + rowHeightOffset - getVScroll(), this.getWidth(), rowHeight);
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

        // Phase 2: globals (Camera / Fade / Shake) live in the central
        // GlobalKeyFrames store rather than per-Character frames[][], so the
        // per-character loop above will skip them. Draw them once total from
        // the central store at the global row positions (computed via
        // displayRowIndex so collapse mode places them at the top).
        drawGlobalKeyFrames(g, image, imageHeight, yImageOffset, xImageOffset, zoomFactor);
    }

    private void drawGlobalKeyFrames(Graphics g, BufferedImage image, int imageHeight, int yImageOffset, int xImageOffset, double zoomFactor)
    {
        TimeSheetPanel ts = getTimeSheetPanel();
        if (ts == null || ts.getPlugin() == null)
        {
            return;
        }
        com.creatorskit.saves.GlobalKeyFrames store = ts.getPlugin().getGlobalKeyFrames();
        if (store == null)
        {
            return;
        }
        drawGlobalRow(g, store.getCameraKeyFramesSafe(),
                KeyFrameType.CAMERA, image, imageHeight, yImageOffset, xImageOffset, zoomFactor);
        drawGlobalRow(g, store.getScreenFadeKeyFramesSafe(),
                KeyFrameType.SCREEN_FADE, image, imageHeight, yImageOffset, xImageOffset, zoomFactor);
        drawGlobalRow(g, store.getScreenShakeKeyFramesSafe(),
                KeyFrameType.SCREEN_SHAKE, image, imageHeight, yImageOffset, xImageOffset, zoomFactor);
    }

    private void drawGlobalRow(Graphics g, KeyFrame[] keyFrames, KeyFrameType type, BufferedImage image, int imageHeight, int yImageOffset, int xImageOffset, double zoomFactor)
    {
        if (keyFrames == null || keyFrames.length == 0)
        {
            return;
        }
        int displayRow = displayRowIndex(type);
        if (displayRow < 0)
        {
            return;
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

            // Duration tails for the global types so the bar shows how long the
            // effect lasts (matches drawCharacterKeyFrames' behaviour for the
            // same types pre-Phase-2).
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
                    CameraKeyFrame ckf = (CameraKeyFrame) keyFrame;
                    drawTail(g, e, keyFrames, ckf.getDurationTicks(), zoomFactor, ckf.getTick(), x, y, imageHeight);
                    break;
                default: break;
            }

            // Pulsing-alpha for selected globals matches drawCharacterKeyFrames
            // for per-character keyframes -- selection cue is the same in both.
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

            // Skip rows that the current mode hides (i.e. non-globals when
            // globalRowsOnlyMode is on). Drawing them would put icons under
            // a hidden label which is just visual noise.
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
                        // Total visible duration of the projectile = optional start delay +
                        // flight duration. Render the tail length to match.
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
                    case SCREEN_FADE:
                        ScreenFadeKeyFrame sfkf = (ScreenFadeKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, sfkf.totalDurationTicks(), zoomFactor, sfkf.getTick(), x, y, imageHeight);
                        break;
                    case SCREEN_SHAKE:
                        ScreenShakeKeyFrame sskf = (ScreenShakeKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, sskf.getDurationTicks(), zoomFactor, sskf.getTick(), x, y, imageHeight);
                        break;
                    case CAMERA:
                        CameraKeyFrame ckf = (CameraKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, ckf.getDurationTicks(), zoomFactor, ckf.getTick(), x, y, imageHeight);
                        break;
                    default:
                        break;
                }


                // Selected keyframes breathe between dim and bright as a
                // higher-contrast cue than the static brighter icon alone.
                // Animation runs at 30 fps via pulseTimer (only when something
                // is selected). Non-selected icons render at full alpha as usual.
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
                // Type is hidden in the current collapse mode -- nothing to drag onto.
                continue;
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
                case SCREEN_FADE:
                    ScreenFadeKeyFrame sfkf = (ScreenFadeKeyFrame) keyFrame;
                    drawPreviewTail(g, x, y, imageHeight, sfkf.totalDurationTicks(), zoomFactor);
                    break;
                case SCREEN_SHAKE:
                    ScreenShakeKeyFrame sskf = (ScreenShakeKeyFrame) keyFrame;
                    drawPreviewTail(g, x, y, imageHeight, sskf.getDurationTicks(), zoomFactor);
                    break;
                case CAMERA:
                    CameraKeyFrame ckf = (CameraKeyFrame) keyFrame;
                    drawPreviewTail(g, x, y, imageHeight, ckf.getDurationTicks(), zoomFactor);
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
        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - rowHeight) / 2;
        int xImageOffset = image.getWidth() / 2;
        double zoomFactor = this.getWidth() / getZoom();

        // Phase 2: hit-test the global keyframes from the central store first
        // so they're clickable even when no Character is visible. Globals don't
        // live in per-Character frames anymore.
        KeyFrame globalHit = hitTestGlobalRows(point, image, xImageOffset, yImageOffset, zoomFactor);
        if (globalHit != null)
        {
            return new KeyFrame[]{globalHit};
        }

        java.util.List<Character> visible = getVisibleCharacters();
        if (visible.isEmpty())
        {
            return null;
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

    /**
     * Hit-tests {@code point} against the three global-row arrays in the
     * central GlobalKeyFrames store. Used by every click/drag handler in this
     * sheet so global keyframes are selectable and draggable even when no
     * Character is selected. Returns the first keyframe whose icon rect
     * contains {@code point}, or null if none.
     */
    private KeyFrame hitTestGlobalRows(Point point, BufferedImage image, int xImageOffset, int yImageOffset, double zoomFactor)
    {
        TimeSheetPanel ts = getTimeSheetPanel();
        if (ts == null || ts.getPlugin() == null)
        {
            return null;
        }
        com.creatorskit.saves.GlobalKeyFrames store = ts.getPlugin().getGlobalKeyFrames();
        if (store == null)
        {
            return null;
        }
        KeyFrame hit = hitTestGlobalRow(point, image, xImageOffset, yImageOffset, zoomFactor,
                store.getCameraKeyFramesSafe(), KeyFrameType.CAMERA);
        if (hit != null) return hit;
        hit = hitTestGlobalRow(point, image, xImageOffset, yImageOffset, zoomFactor,
                store.getScreenFadeKeyFramesSafe(), KeyFrameType.SCREEN_FADE);
        if (hit != null) return hit;
        return hitTestGlobalRow(point, image, xImageOffset, yImageOffset, zoomFactor,
                store.getScreenShakeKeyFramesSafe(), KeyFrameType.SCREEN_SHAKE);
    }

    private KeyFrame hitTestGlobalRow(Point point, BufferedImage image, int xImageOffset, int yImageOffset, double zoomFactor, KeyFrame[] keyFrames, KeyFrameType type)
    {
        if (keyFrames == null || keyFrames.length == 0)
        {
            return null;
        }
        int displayRow = displayRowIndex(type);
        if (displayRow < 0)
        {
            return null;
        }
        int y1 = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;
        int y2 = y1 + image.getHeight();
        if (point.getY() < y1 || point.getY() > y2)
        {
            return null;
        }
        for (KeyFrame keyFrame : keyFrames)
        {
            int x1 = (int) ((keyFrame.getTick() + getHScroll()) * zoomFactor - xImageOffset);
            int x2 = x1 + image.getWidth();
            if (point.getX() >= x1 && point.getX() <= x2)
            {
                return keyFrame;
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

        // Phase 2: globals first -- they're outside the per-Character frames.
        KeyFrame foundKeyFrame = hitTestGlobalRows(point, image, xImageOffset, yImageOffset, zoomFactor);

        java.util.List<Character> visible = getVisibleCharacters();
        if (foundKeyFrame == null && visible.isEmpty())
        {
            // Nothing global hit and no character to scan -- still want to
            // clear the marquee on a non-shift miss, so fall through to the
            // post-loop selection update rather than early-returning.
        }

        outer:
        for (Character c : visible)
        {
            if (foundKeyFrame != null) break;
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
        // Allow rect-select even with no visible characters -- globals are
        // selectable on their own (the central store doesn't need a Character).

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

        // Phase 2: also rect-select globals from the central store. Same
        // dedupe + rect-intersect logic as the per-character loop above.
        foundKeyFrames = addRectangleHitGlobals(rectangle, image, xImageOffset, yImageOffset, zoomFactor, foundKeyFrames);

        setSelectedKeyFrames(foundKeyFrames);
    }

    private KeyFrame[] addRectangleHitGlobals(Rectangle2D rectangle, BufferedImage image, int xImageOffset, int yImageOffset, double zoomFactor, KeyFrame[] foundKeyFrames)
    {
        TimeSheetPanel ts = getTimeSheetPanel();
        if (ts == null || ts.getPlugin() == null)
        {
            return foundKeyFrames;
        }
        com.creatorskit.saves.GlobalKeyFrames store = ts.getPlugin().getGlobalKeyFrames();
        if (store == null)
        {
            return foundKeyFrames;
        }
        foundKeyFrames = addRectangleHitGlobalRow(rectangle, image, xImageOffset, yImageOffset, zoomFactor,
                store.getCameraKeyFramesSafe(), KeyFrameType.CAMERA, foundKeyFrames);
        foundKeyFrames = addRectangleHitGlobalRow(rectangle, image, xImageOffset, yImageOffset, zoomFactor,
                store.getScreenFadeKeyFramesSafe(), KeyFrameType.SCREEN_FADE, foundKeyFrames);
        foundKeyFrames = addRectangleHitGlobalRow(rectangle, image, xImageOffset, yImageOffset, zoomFactor,
                store.getScreenShakeKeyFramesSafe(), KeyFrameType.SCREEN_SHAKE, foundKeyFrames);
        return foundKeyFrames;
    }

    private KeyFrame[] addRectangleHitGlobalRow(Rectangle2D rectangle, BufferedImage image, int xImageOffset, int yImageOffset, double zoomFactor, KeyFrame[] keyFrames, KeyFrameType type, KeyFrame[] foundKeyFrames)
    {
        if (keyFrames == null || keyFrames.length == 0)
        {
            return foundKeyFrames;
        }
        int displayRow = displayRowIndex(type);
        if (displayRow < 0)
        {
            return foundKeyFrames;
        }
        int ky1 = rowHeightOffset + rowHeight + rowHeight * displayRow - getVScroll() - yImageOffset;
        for (KeyFrame keyFrame : keyFrames)
        {
            boolean alreadyContains = false;
            for (KeyFrame kf : foundKeyFrames)
            {
                if (keyFrame == kf)
                {
                    alreadyContains = true;
                    break;
                }
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
}
