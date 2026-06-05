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
 * Per-Character (local) timeline sheet. Shows the
 * {@link KeyFrameType#LOCAL_KEYFRAME_TYPES_ALPHABETICAL} rows for the
 * currently-visible Characters. Globals (Camera / Screen Fade / Screen
 * Shake / Area Sound 1..4) live in {@link GlobalAttributeSheet} -- the
 * toggle button in the labels column switches between this sheet and
 * that one, so each view stays scoped to a single concern.
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

    /**
     * Alpha (0-255) for a label's translucent body fill -- the faint coloured
     * band that drops from the Labels row down through the keyframe rows so the
     * time slice reads as "belonging to" that label (Premiere clip style). The
     * label strip in the Labels row stays fully opaque as the header.
     */
    private static final int BLOCK_BODY_ALPHA = 70;

    public AttributeSheet(ToolBoxFrame toolBox, CreatorsConfig config, ManagerTree tree, AttributePanel attributePanel)
    {
        super(toolBox, config, tree, attributePanel);
        this.config = config;
        this.tree = tree;
        this.attributePanel = attributePanel;

        // 2 reserved top bands: time-header chip + the Labels row. Keyframe
        // rows start at band 2, so the highlight fallback offsets by 2.
        setIndexBuffers(2);
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
     * instead) OR its containing group is currently collapsed (e.g.
     * HITSPLAT_1..4 when "Hitsplats" is folded shut). Reads from the
     * TimeSheetPanel's row layout so the order respects the user's
     * latest collapse / expand toggle.
     */
    private int displayRowIndex(KeyFrameType type)
    {
        return getTimeSheetPanel().getLocalRowIndex(type);
    }

    @Override
    protected KeyFrameType typeAtRowIndex(int contentRow)
    {
        java.util.List<com.creatorskit.swing.timesheet.sheets.TimelineLocalRowLayout.Row> visible =
                getTimeSheetPanel().getLocalRowLayout().visibleRows();
        if (contentRow < 0 || contentRow >= visible.size()) return null;
        com.creatorskit.swing.timesheet.sheets.TimelineLocalRowLayout.Row r = visible.get(contentRow);
        // Parent rows have no type -- clicking their lane shouldn't
        // switch cards; the chevron toggle on the label handles them.
        return r.kind == com.creatorskit.swing.timesheet.sheets.TimelineLocalRowLayout.Row.Kind.LEAF ? r.type : null;
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
            row = dr + 2; // +2 reserved top bands (header chip + Labels row)
        }
        else
        {
            row = getSelectedIndex() + getIndexBuffers();
        }
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, row * rowHeight + rowHeightOffset - getVScroll(), this.getWidth(), rowHeight);
    }


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

        // Labels render as an opaque coloured strip in the dedicated Labels
        // row (the header -- carries the text), PLUS a faint translucent body
        // that drops through the keyframe rows below it, so the spanned time
        // slice reads as "belonging to" the label. That's the Premiere-clip
        // look the old Blocks had. Still non-interactive -- purely
        // organizational. Multiple labels may overlap; later ones draw on top
        // (their translucent bodies stack).
        int stripTop = labelStripTop();
        int stripH = rowHeight;
        // Body starts just under the Labels strip (the first keyframe row) and
        // runs to the bottom of the sheet; keyframe icons paint on top of it
        // since drawBlocks() runs before drawKeyFrames().
        int bodyTop = stripTop + stripH;
        int bodyH = Math.max(0, this.getHeight() - bodyTop);

        for (Character c : visible)
        {
            java.util.List<com.creatorskit.swing.timesheet.keyframe.Block> labels = c.getBlocks();
            if (labels == null || labels.isEmpty()) continue;
            for (com.creatorskit.swing.timesheet.keyframe.Block label : labels)
            {
                if (label == null) continue;

                double startTick = label.getStartTick();
                double endTick = label.getEndTick();
                int leftX = (int) ((startTick + getHScroll()) * zoomFactor) - xImageOffset;
                int rightX = (int) ((endTick + getHScroll()) * zoomFactor) + xImageOffset;
                int rectW = Math.max(1, rightX - leftX);

                Color baseColour = new Color(label.getColorRgb());

                // Translucent body under the keyframe rows.
                if (bodyH > 0)
                {
                    g2.setColor(new Color(baseColour.getRed(), baseColour.getGreen(),
                            baseColour.getBlue(), BLOCK_BODY_ALPHA));
                    g2.fillRect(leftX, bodyTop, rectW, bodyH);
                }

                // Opaque header strip in the dedicated Labels row.
                g2.setColor(baseColour);
                g2.fillRect(leftX, stripTop, rectW, stripH);
                g2.setColor(baseColour.darker());
                g2.drawRect(leftX, stripTop, rectW - 1, stripH - 1);

                String name = label.getName();
                if (name != null && !name.isEmpty())
                {
                    java.awt.Shape oldClip = g2.getClip();
                    g2.setClip(leftX, stripTop, rectW, stripH);
                    int textW = fm.stringWidth(name);
                    int textX = leftX + (rectW - textW) / 2;
                    int textY = stripTop + (stripH + fm.getAscent()) / 2 - 2;
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

    /** Y of the top of the single-row label strip (top-most row of the body). */
    private int labelStripTop()
    {
        return rowHeightOffset + rowHeight - getVScroll();
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
                int y = rowHeightOffset + rowHeight * 2 + rowHeight * displayRow - getVScroll() - yImageOffset;

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
                        drawOrientationTail(g, e, keyFrames, okf, zoomFactor, x, y, imageHeight);
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
                        drawTail(g, e, keyFrames, pkf.getDurationTicks(), zoomFactor, pkf.getTick(), x, y, imageHeight);
                        break;
                    case SHIELD:
                        ShieldKeyFrame shkf = (ShieldKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, shkf.getDuration(), zoomFactor, shkf.getTick(), x, y, imageHeight);
                        break;
                    case SPECIAL:
                        SpecialKeyFrame spkf = (SpecialKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, spkf.getDuration(), zoomFactor, spkf.getTick(), x, y, imageHeight);
                        break;
                    case COLOUR:
                        ColourKeyFrame clkf = (ColourKeyFrame) keyFrame;
                        drawTail(g, e, keyFrames, clkf.getFadeInTicks() + clkf.getHoldTicks() + clkf.getFadeOutTicks(), zoomFactor, clkf.getTick(), x, y, imageHeight);
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

    /** Phantom-completion colour: the turn needs MORE time than the authored
     *  duration gives it, so this green line shows where it would finish. */
    private static final Color PHANTOM_GREEN = new Color(60, 200, 90);
    /** Excess colour: the authored duration is LONGER than the turn needs, so
     *  this red line marks the leftover time where the turn is already done. */
    private static final Color EXCESS_RED = new Color(220, 70, 60);

    /**
     * Orientation tail with a duration-vs-turn-rate diagnostic. The turn needs
     * {@code |directionalDifference(start,end)| / (turnRate * gameTicksPerClientTick)}
     * timeline ticks to finish (mirrors Programmer.getOrientationStatic). We
     * draw the authored duration in the normal colour up to where the turn
     * completes, then:
     * <ul>
     *   <li>duration LONGER than needed -&gt; the excess is drawn {@link #EXCESS_RED};</li>
     *   <li>duration SHORTER than needed -&gt; a phantom {@link #PHANTOM_GREEN}
     *       line extends past the bar to where the turn would actually finish.</li>
     * </ul>
     * They line up exactly (all normal colour) when duration == needed. Letting
     * the turn finish is what keeps a FOLLOWING keyframe's start equal to the
     * character's real orientation (see Character.orientationOfOriAt) -- so this
     * tail is the authoring guide for avoiding the "starts from last-known
     * orientation" jump.
     */
    private void drawOrientationTail(Graphics g, int e, KeyFrame[] keyFrames,
            OrientationKeyFrame okf, double zoomFactor, int x, int y, int imageHeight)
    {
        int lineY = y + imageHeight / 2;
        Color normalColor = g.getColor();
        double duration = okf.getDuration();

        // Right edge of the SOLID (authored) bar is clamped to the next
        // keyframe so adjacent tails don't overlap -- same rule drawTail uses.
        // POSITIVE_INFINITY when there's no following keyframe.
        double tickLimit = (e + 1 < keyFrames.length)
                ? keyFrames[e + 1].getTick() - okf.getTick()
                : Double.POSITIVE_INFINITY;

        // Face Target (FOLLOW) keyframes track a MOVING target for their whole
        // duration -- there's no fixed end angle to complete a turn toward, so
        // the red/green diagnostic doesn't apply (and would mis-paint the
        // intended tracking time as red "excess"). Draw the plain tracking bar.
        String faceTargetName = okf.getTargetCharacterName();
        if (faceTargetName != null && !faceTargetName.trim().isEmpty())
        {
            drawTailRange(g, normalColor, lineY, x, 0, Math.min(duration, tickLimit), zoomFactor);
            return;
        }

        double turnRate = okf.getTurnRate();
        int difference = com.creatorskit.programming.orientation.Orientation
                .directionalDifference(okf.getStart(), okf.getEnd(), okf.getTurnDirection());
        double ratio = (double) net.runelite.api.Constants.GAME_TICK_LENGTH
                / net.runelite.api.Constants.CLIENT_TICK_LENGTH;

        // No turn to make, or a turn rate that can never resolve -> plain bar.
        if (difference == 0 || turnRate <= 0)
        {
            drawTailRange(g, normalColor, lineY, x, 0, Math.min(duration, tickLimit), zoomFactor);
            return;
        }

        double needed = Math.abs(difference) / (turnRate * ratio);

        // Normal portion: icon -> min(duration, needed), never past the next kf.
        drawTailRange(g, normalColor, lineY, x, 0,
                Math.min(Math.min(duration, needed), tickLimit), zoomFactor);

        if (duration > needed)
        {
            // Duration too long: red excess [needed, duration] (clamped to next kf).
            drawTailRange(g, EXCESS_RED, lineY, x, needed, Math.min(duration, tickLimit), zoomFactor);
        }
        else if (duration < needed)
        {
            // Duration too short: green phantom [duration, needed]. Allowed past
            // the next keyframe (capped at the component edge) so the overflow
            // -- the signal the turn can't finish in time -- stays visible.
            drawTailRange(g, PHANTOM_GREEN, lineY, x, duration, needed, zoomFactor);
        }

        g.setColor(normalColor);
    }

    /**
     * Draws a horizontal tail segment over the tick range [fromTicks, toTicks),
     * measured from the keyframe icon at pixel {@code x}. No-op for an empty
     * range; the right edge is capped at the component width so a long phantom
     * line never paints off-canvas.
     */
    private void drawTailRange(Graphics g, Color color, int lineY, int x,
            double fromTicks, double toTicks, double zoomFactor)
    {
        if (toTicks <= fromTicks) return;
        int x0 = x + (int) (fromTicks * zoomFactor);
        int x1 = x + (int) (toTicks * zoomFactor);
        int maxX = getWidth();
        if (x1 > maxX) x1 = maxX;
        if (x1 <= x0) return;
        g.setColor(color);
        g.drawLine(x0, lineY, x1 - 1, lineY);
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
            int y = rowHeightOffset + rowHeight * 2 + rowHeight * displayRow - getVScroll() - yImageOffset;

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
                case COLOUR:
                    ColourKeyFrame clkf = (ColourKeyFrame) keyFrame;
                    drawPreviewTail(g, x, y, imageHeight, clkf.getFadeInTicks() + clkf.getHoldTicks() + clkf.getFadeOutTicks(), zoomFactor);
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
                    int y1 = rowHeightOffset + rowHeight * 2 + rowHeight * displayRow - getVScroll() - yImageOffset;
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
                    int y1 = rowHeightOffset + rowHeight * 2 + rowHeight * displayRow - getVScroll() - yImageOffset;
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
                    int ky1 = rowHeightOffset + rowHeight * 2 + rowHeight * displayRow - getVScroll() - yImageOffset;

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
        // getKeyFrameClicked: y1 = rowHeightOffset + rowHeight*2 + rowHeight*dr - vScroll.
        // Data rows sit TWO bands below the top (header chip + Labels row).
        int displayRow = (int) Math.floor((p.getY() + getVScroll() - rowHeightOffset - rowHeight * 2) / (double) rowHeight);
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

        // Labels live in the single top row strip -- hit-test only that band.
        int stripTop = labelStripTop();
        int stripBottom = stripTop + rowHeight;
        if (p.getY() < stripTop || p.getY() > stripBottom) return null;

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
        // Labels are non-interactive: a left-click on one does nothing special
        // (no select-all). Return false so the click falls through to normal
        // keyframe handling.
        return false;
    }

    @Override
    public boolean tryHandleBlockRightClick(Point p)
    {
        TimeSheetPanel tsp = getTimeSheetPanel();
        if (tsp == null) return false;

        // Right-clicking an existing label -> Edit / Delete.
        BlockHit hit = findBlockAt(p);
        if (hit != null)
        {
            showLabelContextMenu(p, hit);
            return true;
        }

        // Right-clicking the empty Labels row (its own dedicated band now, so
        // no conflict with a keyframe row) -> create a new label for the
        // visible Character(s).
        int stripTop = labelStripTop();
        if (p.getY() >= stripTop && p.getY() <= stripTop + rowHeight)
        {
            javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
            javax.swing.JMenuItem create = new javax.swing.JMenuItem("New label...");
            create.addActionListener(e -> tsp.createLabelFromSelection());
            menu.add(create);
            menu.show(this, (int) p.getX(), (int) p.getY());
            return true;
        }
        return false;
    }

    private void showLabelContextMenu(Point p, BlockHit hit)
    {
        TimeSheetPanel tsp = getTimeSheetPanel();
        if (tsp == null) return;
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();

        javax.swing.JMenuItem edit = new javax.swing.JMenuItem("Edit label...");
        edit.addActionListener(e -> tsp.editLabel(hit.character, hit.block));
        menu.add(edit);

        javax.swing.JMenuItem delete = new javax.swing.JMenuItem("Delete label");
        delete.addActionListener(e -> tsp.deleteLabel(hit.character, hit.block));
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

        // Select-between-A-B: always present at the top so the user can
        // find it consistently. Greyed out when either marker is unset --
        // the tooltip explains why so they know to drop A + B first.
        JMenuItem selectAB;
        if (tsp.canSelectBetweenAB())
        {
            Double a = tsp.getALoopTick();
            Double b = tsp.getBLoopTick();
            selectAB = new JMenuItem(String.format("Select keyframes between A-B  [%.1f-%.1f]", a, b));
            selectAB.addActionListener(e -> tsp.selectAllKeyFramesBetweenAB());
        }
        else
        {
            selectAB = new JMenuItem("Select keyframes between A-B (drop A and B first)");
            selectAB.setEnabled(false);
        }
        menu.add(selectAB);

        // Selection-narrowing helper for the multi-Character + marquee case:
        // when the user has more Characters selected than they actually want
        // to operate on, this drops the non-keyframe-owners from the Character
        // selection in one click. Only shown when the context makes sense
        // (2+ Characters selected AND at least one keyframe in the marquee)
        // so the menu isn't cluttered for the single-Character path.
        if (tsp.canReduceSelectionToKeyFrameOwners())
        {
            JMenuItem reduce = new JMenuItem("Reduce selection to keyframe owners");
            reduce.addActionListener(e -> tsp.reduceSelectionToKeyFrameOwners());
            menu.add(reduce);
        }

        menu.addSeparator();

        // Labels: create a label for the selected Character(s). From / To
        // default to the current keyframe selection's tick extent (or 0/0 if
        // nothing is selected -- the user sets the range in the dialog).
        JMenuItem createLabel = new JMenuItem("New label...");
        createLabel.addActionListener(e -> tsp.createLabelFromSelection());
        menu.add(createLabel);
        menu.addSeparator();

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

        menu.addSeparator();

        // Ripple Insert: counterpart to Ripple Delete. Inserts empty ticks
        // at the playhead and pushes every later keyframe forward.
        JMenuItem rippleInsert = new JMenuItem("Ripple Insert... (at playhead)");
        rippleInsert.addActionListener(e -> tsp.showRippleInsertDialogAtPlaybar());
        menu.add(rippleInsert);

        menu.show(this, (int) p.getX(), (int) p.getY());
    }
}
