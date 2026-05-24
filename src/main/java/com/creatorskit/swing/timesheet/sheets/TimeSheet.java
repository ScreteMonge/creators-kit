package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterActionType;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

@Getter
@Setter
public class TimeSheet extends JPanel
{
    private ToolBoxFrame toolBox;
    private CreatorsConfig config;
    private ManagerTree managerTree;
    private AttributePanel attributePanel;

    private final BufferedImage keyframeImage = ImageUtil.loadImageResource(getClass(), "/Keyframe.png");
    private final BufferedImage keyframeSelected = ImageUtil.loadImageResource(getClass(), "/Keyframe_Selected.png");

    private double zoom = 50;
    private double hScroll = 0;
    private int vScroll = 0;
    private int selectedIndex = 0;

    private double currentTime = 0;
    private double previewTime = 0;
    private boolean timeIndicatorPressed = false;
    private boolean allowRectangleSelect = false;
    private Point mousePointOnPressed = new Point(0, 0);
    public final int DRAG_STICK_RANGE = 15;

    public int rowHeight = 28;
    public int rowHeightOffset = 0;
    public final int TEXT_HEIGHT_OFFSET = 5;
    private int indexBuffers = 1;

    /**
     * How many *real* property rows the timeline has (set by TimeSheetPanel
     * from the keyframe-type list it builds the labels from). drawBackground
     * iterates much further than this for the alternating row colors, but
     * the edge-fade overlay only fires when actual content is cropped --
     * i.e. when {@code (contentRowCount+1) * rowHeight > vScroll + viewport}
     * for the bottom edge, or {@code vScroll > 0} for the top.
     * +1 accounts for the row-0 header reserved for the time indicator chip.
     */
    private int contentRowCount = 0;

    private final Color background1 = new Color(40, 40, 40);
    private final Color background2 = new Color(42, 42, 42);

    public final int SHOW_5_ZOOM = 200;
    public final int SHOW_1_ZOOM = 50;

    private KeyFrame[] visibleKeyFrames = new KeyFrame[0];
    private Character selectedCharacter;
    private boolean keyFrameClicked = false;
    private KeyFrame[] clickedKeyFrames = new KeyFrame[0];

    /**
     * Characters whose keyframes should be rendered and interactive.
     * If a multi-selection is active, returns the full selected set. Otherwise the single
     * primary Character (or empty if none).
     */
    public java.util.List<Character> getVisibleCharacters()
    {
        com.creatorskit.selection.SelectionManager mgr = getTimeSheetPanel() == null
                ? null
                : getTimeSheetPanel().getSelectionManager();
        if (mgr != null && mgr.size() > 1)
        {
            return new java.util.ArrayList<>(mgr.getSelected());
        }
        Character primary = selectedCharacter;
        if (primary == null)
        {
            return java.util.Collections.emptyList();
        }
        return java.util.Collections.singletonList(primary);
    }

    /**
     * Finds the Character that owns the given keyframe by scanning each visible
     * Character's frame array. Returns null if not found.
     */
    public Character findKeyFrameOwner(KeyFrame keyFrame)
    {
        if (keyFrame == null)
        {
            return null;
        }
        for (Character c : getVisibleCharacters())
        {
            KeyFrame[][] frames = c.getFrames();
            if (frames == null)
            {
                continue;
            }
            for (KeyFrame[] row : frames)
            {
                if (row == null)
                {
                    continue;
                }
                for (KeyFrame kf : row)
                {
                    if (kf == keyFrame)
                    {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    public TimeSheet(ToolBoxFrame toolBox, CreatorsConfig config, ManagerTree managerTree, AttributePanel attributePanel)
    {
        this.toolBox = toolBox;
        this.config = config;
        this.managerTree = managerTree;
        this.attributePanel = attributePanel;

        setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setFocusable(true);
        requestFocusInWindow();

        setKeyBindings();
        setMouseListeners(this);

        revalidate();
        repaint();
    }

    public void onVerticalScrollEvent(int scroll)
    {
        vScroll = scroll;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(1));
        drawBackground(g2);
        drawBackgroundText(g2);
        drawHighlight(g2);
        drawBackgroundLines(g2);
        // Block rectangles sit BELOW keyframe icons so the icons remain
        // visible on top of the coloured block area. Premiere clip style.
        drawBlocks(g2);
        drawRectangleSelect(g2);
        drawKeyFrames(g2);
        drawPreviewKeyFrames(g2);
        // Fade is drawn AFTER keyframes (so they dim into the edge) but
        // BEFORE the text header / time indicator (so those stay crisp on
        // top of the fade). It's the visual cue that content is cropped --
        // pairs with the now-visible scrollbar in the labels column.
        drawCropFade(g2);
        drawTextHeader(g2);
        drawTimeIndicator(g2);
        drawPreviewTimeIndicator(g2);
        // A-B markers AFTER the time indicator so the marker chip overlays
        // when they coincide -- the X close box must be hit-testable, and
        // the markers are static config the user shouldn't lose visibility
        // of behind a moving playhead.
        drawABMarkers(g2);
        revalidate();
        repaint();
    }

    /**
     * Last-drawn screen rectangles for the A/B marker X close buttons --
     * updated by {@link #drawABMarkers} each paint and consulted by
     * {@link #mousePressed} so a click on the X removes the marker rather
     * than falling through to the time-indicator-press path. Nulled when
     * the corresponding marker isn't set, so a stale rectangle from a
     * previous paint can't take a click.
     */
    private Rectangle aMarkerXHit = null;
    private Rectangle bMarkerXHit = null;
    /**
     * Last-drawn full chip rectangles for the A/B markers. The X close box
     * sits inside this region, so the close check must happen first. Used by
     * mousePressed to start a drag and by mouseDragged to keep dragging until
     * release. Nulled when the corresponding marker isn't set so stale rects
     * can't trap a click.
     */
    private Rectangle aMarkerChipHit = null;
    private Rectangle bMarkerChipHit = null;
    /**
     * In-progress drag flags for the A/B markers. Set in mousePressed when
     * the user grabs the chip; cleared on mouseReleased. While set, mouse-
     * dragged events translate to setALoopTick / setBLoopTick at the
     * current pointer x, snapped to the 0.1-tick grid. Cleared by
     * clearTransientDragFlags so a stuck flag from a bad release can't
     * latch the next click into a phantom drag.
     */
    private boolean draggingAMarker = false;
    private boolean draggingBMarker = false;
    /**
     * Set in mousePressed when {@link #tryHandleBlockLeftClick} consumed
     * the press (the click landed inside a block rect). Read in
     * mouseReleased so the "empty-space click deselects everything"
     * fallback branch doesn't undo the block selection we just made.
     * Cleared by {@link #clearTransientDragFlags}.
     */
    private boolean blockLeftClicked = false;

    private void drawABMarkers(Graphics2D g)
    {
        TimeSheetPanel tsp = getTimeSheetPanel();
        aMarkerXHit = null;
        bMarkerXHit = null;
        aMarkerChipHit = null;
        bMarkerChipHit = null;
        if (tsp == null) return;
        Double a = tsp.getALoopTick();
        Double b = tsp.getBLoopTick();
        if (a != null) drawSingleMarker(g, a, "A", new Color(220, 60, 50), true);
        if (b != null) drawSingleMarker(g, b, "B", new Color(60, 110, 230), false);
    }

    private void drawSingleMarker(Graphics2D g, double tick, String label, Color color, boolean isA)
    {
        double x = (tick + hScroll) * getWidth() / zoom;

        // Translucent vertical line so background context still reads through.
        Color line = new Color(color.getRed(), color.getGreen(), color.getBlue(), 180);
        g.setColor(line);
        g.setStroke(new BasicStroke(2));
        g.drawLine((int) x, 0, (int) x, getHeight());
        g.setStroke(new BasicStroke(1));

        // Chip at top: letter on the left, X close on the right.
        int chipWidth = 30;
        int chipX = (int) (x - chipWidth / 2.0);
        int chipY = 0;
        g.setColor(color);
        g.fillRoundRect(chipX, chipY, chipWidth, rowHeight, 8, 8);

        g.setColor(Color.WHITE);
        Font old = g.getFont();
        g.setFont(new Font(old.getName(), Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int letterY = (rowHeight + fm.getAscent()) / 2 - 2;
        g.drawString(label, chipX + 5, letterY);
        g.setFont(old);

        // X close: an X drawn inside a small hit-rect at the right of the chip.
        int xBoxW = 12;
        int xBoxH = Math.min(rowHeight - 4, 16);
        int xBoxX = chipX + chipWidth - xBoxW - 1;
        int xBoxY = (rowHeight - xBoxH) / 2;
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(1.5f));
        int pad = 3;
        g.drawLine(xBoxX + pad, xBoxY + pad, xBoxX + xBoxW - pad, xBoxY + xBoxH - pad);
        g.drawLine(xBoxX + xBoxW - pad, xBoxY + pad, xBoxX + pad, xBoxY + xBoxH - pad);
        g.setStroke(new BasicStroke(1));

        Rectangle hit = new Rectangle(xBoxX, xBoxY, xBoxW, xBoxH);
        if (isA) aMarkerXHit = hit; else bMarkerXHit = hit;
        // Full-chip rect: drag handle for moving the marker. mousePressed
        // checks this AFTER the X close box (because the X sits inside the
        // chip), so an X click never accidentally starts a drag.
        Rectangle chipRect = new Rectangle(chipX, chipY, chipWidth, rowHeight);
        if (isA) aMarkerChipHit = chipRect; else bMarkerChipHit = chipRect;
    }

    /**
     * Returns true if {@code p} hit one of the marker X close boxes and the
     * appropriate marker was cleared. Called from {@link #setMouseListeners}
     * before the time-indicator-press path so a click on the X doesn't
     * scrub the playhead.
     */
    private boolean handleABMarkerClick(Point p)
    {
        TimeSheetPanel tsp = getTimeSheetPanel();
        if (tsp == null) return false;
        if (aMarkerXHit != null && aMarkerXHit.contains(p))
        {
            tsp.setALoopTick(null);
            return true;
        }
        if (bMarkerXHit != null && bMarkerXHit.contains(p))
        {
            tsp.setBLoopTick(null);
            return true;
        }
        return false;
    }

    /**
     * Vertical gradients at the top/bottom of the body that signal "there
     * are property rows cropped here." Only fires when there really is
     * something off-screen in that direction. fadeHeight matches the row
     * height so the gradient is exactly one row tall -- subtle enough to
     * not obscure interactions in the visible edge rows, obvious enough
     * the user notices.
     */
    private void drawCropFade(Graphics2D g)
    {
        if (contentRowCount <= 0)
        {
            return;
        }

        int h = getHeight();
        int v = getVScroll();
        int fadeHeight = Math.min(rowHeight, 18);
        // Don't overlap the header row (y=0..rowHeight) which carries the
        // time indicator chip / tick numbers; start the top fade below it.
        int topFadeY = rowHeight + rowHeightOffset;
        int contentBottom = (contentRowCount + 1) * rowHeight + rowHeightOffset;

        if (v > 0)
        {
            Paint old = g.getPaint();
            g.setPaint(new GradientPaint(
                    0, topFadeY, new Color(0, 0, 0, 170),
                    0, topFadeY + fadeHeight, new Color(0, 0, 0, 0)));
            g.fillRect(0, topFadeY, getWidth(), fadeHeight);
            g.setPaint(old);
        }

        if (v + h < contentBottom)
        {
            int bottomFadeStart = h - fadeHeight;
            Paint old = g.getPaint();
            g.setPaint(new GradientPaint(
                    0, bottomFadeStart, new Color(0, 0, 0, 0),
                    0, h, new Color(0, 0, 0, 170)));
            g.fillRect(0, bottomFadeStart, getWidth(), fadeHeight);
            g.setPaint(old);
        }
    }

    /**
     * Zeros every transient flag that tracks an in-progress mouse interaction.
     * Used as belt-and-suspenders cleanup at every mouseReleased return path
     * (and as a defensive sweep at end) so a previous bad release can't leave
     * the next press riding on stale state. Symptom of leaving these stuck:
     * rectangle-select box stays visible, or "clicking outside" continues
     * dragging keyframes because keyFrameClicked is still true.
     */
    private void clearTransientDragFlags()
    {
        keyFrameClicked = false;
        allowRectangleSelect = false;
        timeIndicatorPressed = false;
        draggingAMarker = false;
        draggingBMarker = false;
        blockLeftClicked = false;
    }

    private void drawRectangleSelect(Graphics2D g)
    {
        if (!allowRectangleSelect)
        {
            return;
        }

        Point absoluteMouse = MouseInfo.getPointerInfo().getLocation();

        int x1 = (int) mousePointOnPressed.getX();
        int x2 = (int) (absoluteMouse.getX() - getLocationOnScreen().getX());
        int y1 = (int) mousePointOnPressed.getY();
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

        g.setColor(new Color(93, 93, 93));
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2F));
        g.fillRect(startX, startY, endX - startX, endY - startY);

        g.setColor(new Color(255, 255, 255));
        g.drawRect(startX, startY, endX - startX, endY - startY);
        g.setComposite(composite);
    }

    private void drawBackground(Graphics g)
    {
        g.setColor(Color.DARK_GRAY);
        int iterations = (this.getHeight() + getVScroll() / rowHeight) + 1;
        boolean alternate = false;
        for (int i = 0; i < iterations; i++)
        {
            if (alternate)
            {
                g.setColor(background2);
            }
            else
            {
                g.setColor(background1);
            }

            g.fillRect(0, i * rowHeight + rowHeightOffset - getVScroll(), this.getWidth(), rowHeight);
            alternate = !alternate;
        }
    }

    public void drawBackgroundText(Graphics g)
    {

    }

    public void drawHighlight(Graphics g)
    {

    }

    /** Block rectangles. Default: no-op. AttributeSheet overrides to render
     *  the per-Character {@link com.creatorskit.swing.timesheet.keyframe.Block}
     *  list -- one opaque rect per block from min-tick to max-tick across
     *  every property row that's included in the block. */
    public void drawBlocks(Graphics g)
    {

    }

    /** Hit-test hook for clicks on a block rectangle. Default: no-op
     *  returning false. AttributeSheet overrides to: if the click landed
     *  inside a block rect (and not on a keyframe icon), select the block's
     *  member keyframes + register the block selection, then return true
     *  so the press doesn't fall through to marquee start. */
    public boolean tryHandleBlockLeftClick(Point p, boolean shiftDown)
    {
        return false;
    }

    /** Hit-test hook for right-clicks on a block rectangle. Default false.
     *  AttributeSheet overrides to show the block context menu (Dissolve /
     *  Delete / Rename / Recolour / Enter) when the click lands inside a
     *  block, intercepting before the empty-space Ripple Delete menu fires. */
    public boolean tryHandleBlockRightClick(Point p)
    {
        return false;
    }

    private void drawBackgroundLines(Graphics g)
    {
        double modeMultiplier = config.timelineUnits().getMultiplier();

        if (zoom <= SHOW_1_ZOOM)
        {
            g.setColor(ColorScheme.DARKER_GRAY_COLOR);

            double spacing = this.getWidth() / zoom / modeMultiplier;
            double startOffset = hScroll * modeMultiplier;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < zoom / modeMultiplier + firstIteration; i++)
            {
                g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());
            }
        }

        if (zoom <= SHOW_5_ZOOM)
        {
            g.setColor(ColorScheme.DARKER_GRAY_COLOR.darker());

            double iterations = zoom / 5 * modeMultiplier;
            double spacing = this.getWidth() / iterations;
            double startOffset = hScroll * modeMultiplier / 5;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < iterations + firstIteration; i++)
            {
                g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());
            }
        }

        g.setColor(ColorScheme.BORDER_COLOR.darker());

        double iterations = zoom / 5 * modeMultiplier;
        double spacing = this.getWidth() / iterations;
        double startOffset = hScroll * modeMultiplier / 5;
        int firstIteration = (int) Math.ceil(-1 * startOffset);
        boolean skip5Line = firstIteration % 2 != 0;

        for (int i = firstIteration; i < iterations + firstIteration; i++)
        {
            if (skip5Line)
            {
                skip5Line = false;
                continue;
            }

            g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());
            skip5Line = true;
        }
    }

    private void drawTimeIndicator(Graphics g)
    {
        TimelineUnits timelineUnits = config.timelineUnits();
        double modeMultiplier = timelineUnits.getMultiplier();

        double x = (currentTime + hScroll) * this.getWidth() / zoom;
        char[] c = ("" + round(timelineUnits, currentTime * modeMultiplier)).toCharArray();
        double width = g.getFontMetrics().charsWidth(c, 0, c.length);
        int textBuffer = 16;

        g.setColor(new Color(74, 121, 192));
        g.drawLine((int) x, 0, (int) x, this.getHeight());

        g.fillRoundRect((int) (x - (width + textBuffer) / 2), 0, (int) width + textBuffer, rowHeight, 10, 10);

        g.setColor(Color.WHITE);
        g.drawChars(c, 0, c.length, (int) (x - width / 2), rowHeight - TEXT_HEIGHT_OFFSET);

    }

    private void drawPreviewTimeIndicator(Graphics g)
    {
        if (!timeIndicatorPressed)
        {
            return;
        }

        TimelineUnits timelineUnits = config.timelineUnits();
        double modeMultiplier = timelineUnits.getMultiplier();

        double x = (previewTime + hScroll)  * this.getWidth() / zoom;
        char[] c = ("" + round(timelineUnits, previewTime * modeMultiplier)).toCharArray();
        double width = g.getFontMetrics().charsWidth(c, 0, c.length);
        int textBuffer = 16;

        g.setColor(new Color(49, 84, 128));
        g.drawLine((int) x, 0, (int) x, this.getHeight());

        g.fillRoundRect((int) (x - (width + textBuffer) / 2), 0, (int) width + textBuffer, rowHeight, 10, 10);

        g.setColor(Color.WHITE);
        g.drawChars(c, 0, c.length, (int) (x - width / 2), rowHeight - TEXT_HEIGHT_OFFSET);

    }

    private void drawTextHeader(Graphics g)
    {
        double modeMultiplier = config.timelineUnits().getMultiplier();

        g.setColor(Color.WHITE);

        if (zoom <= SHOW_1_ZOOM)
        {
            g.setColor(Color.WHITE.darker().darker());
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics fontMetrics = g.getFontMetrics();

            double spacing = this.getWidth() / zoom / modeMultiplier;
            double startOffset = hScroll * modeMultiplier;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < zoom / modeMultiplier + firstIteration; i++)
            {
                if (i % 5 == 0)
                {
                    continue;
                }

                char[] c = ("" + i).toCharArray();
                int width = fontMetrics.charsWidth(c, 0, c.length) / 2;
                g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), rowHeight - TEXT_HEIGHT_OFFSET);
            }
        }

        if (zoom <= SHOW_5_ZOOM)
        {
            g.setColor(Color.WHITE.darker());
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics fontMetrics = g.getFontMetrics();

            double iterations = zoom / 5 * modeMultiplier;
            double spacing = this.getWidth() / iterations;
            double startOffset = hScroll * modeMultiplier / 5;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < iterations + firstIteration; i++)
            {
                int draw = i * 5;
                if (draw % 10 == 0)
                {
                    continue;
                }

                char[] c = ("" + draw).toCharArray();
                int width = fontMetrics.charsWidth(c, 0, c.length) / 2;
                g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), rowHeight - TEXT_HEIGHT_OFFSET);
            }
        }

        g.setColor(Color.WHITE);
        g.setFont(FontManager.getRunescapeFont());
        FontMetrics fontMetrics = g.getFontMetrics();

        double iterations = zoom / 10 * modeMultiplier;
        double spacing = this.getWidth() / iterations;
        double startOffset = hScroll * modeMultiplier / 10;
        int firstIteration = (int) Math.ceil(-1 * startOffset);

        for (int i = firstIteration; i < iterations + firstIteration; i++)
        {
            char[] c = ("" + i * 10).toCharArray();
            int width = fontMetrics.charsWidth(c, 0, c.length) / 2;
            g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), rowHeight - TEXT_HEIGHT_OFFSET);
        }
    }

    public void drawKeyFrames(Graphics g)
    {

    }

    public void drawPreviewKeyFrames(Graphics2D g)
    {

    }

    private void setKeyBindings()
    {
        ActionMap actionMap = getActionMap();
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Plain LEFT / RIGHT scrub the timeline by +/- 1 tick. WHEN_IN_FOCUSED_WINDOW
        // means this fires whenever the toolbox window has focus and no inner
        // component consumed the key (text fields keep their caret-move
        // behaviour because component-level bindings take precedence). The
        // CTRL+arrow / CTRL+ALT+arrow cadence (jump-to-kf and 0.1-tick) lives
        // on the plugin-level HotkeyListener path so they fire from any window.
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "VK_LEFT");
        actionMap.put("VK_LEFT", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                getTimeSheetPanel().skipListener(-1.0);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "VK_RIGHT");
        actionMap.put("VK_RIGHT", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                getTimeSheetPanel().skipListener(1.0);
            }
        });
    }

    public void updateSelectedKeyFrameOnPressed(boolean shiftDown)
    {

    }

    private void setMouseListeners(TimeSheet timeSheet)
    {
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                requestFocusInWindow();

                if (e.getButton() != MouseEvent.BUTTON1)
                {
                    return;
                }

                Point mousePosition = getMousePosition();
                if (mousePosition == null)
                {
                    return;
                }

                // Reset any stale transient flags from a previous bad release.
                // Set fresh state below based on where this press lands.
                clearTransientDragFlags();

                // A/B marker X close-box click is the highest priority hit-
                // test: it overlaps the header row where the time indicator
                // press would otherwise fire, so check it first.
                if (handleABMarkerClick(mousePosition))
                {
                    return;
                }

                // A/B chip drag start. The chip rect contains the X close
                // box, so X must be tested above this point. Once a drag
                // is engaged, mouseDragged keeps updating the marker tick
                // and mouseReleased clears the flag.
                if (aMarkerChipHit != null && aMarkerChipHit.contains(mousePosition))
                {
                    draggingAMarker = true;
                    return;
                }
                if (bMarkerChipHit != null && bMarkerChipHit.contains(mousePosition))
                {
                    draggingBMarker = true;
                    return;
                }

                if (mousePosition.getY() < rowHeight)
                {
                    timeIndicatorPressed = true;
                    updatePreviewTime(getTimeIndicatorPosition());
                    return;
                }
                else
                {
                    allowRectangleSelect = true;
                    mousePointOnPressed = mousePosition;
                }

                clickedKeyFrames = getKeyFrameClicked(mousePosition);
                keyFrameClicked = clickedKeyFrames != null;
                // CTRL OR SHIFT = additive selection. The two modifiers
                // behave identically here -- user feedback was that CTRL
                // wasn't doing anything for kf multi-select on a single
                // Character; this aligns it with SHIFT.
                boolean additive = e.isShiftDown() || e.isControlDown();
                if (keyFrameClicked)
                {
                    allowRectangleSelect = false;
                    updateSelectedKeyFrameOnPressed(additive);
                }
                else if (tryHandleBlockLeftClick(mousePosition, additive))
                {
                    // Block click consumed the press -- don't start marquee
                    // dragging on top of a block selection, and signal the
                    // release handler so it doesn't fire its "empty-space
                    // click deselects everything" fallback.
                    allowRectangleSelect = false;
                    blockLeftClicked = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                super.mouseClicked(e);
                requestFocusInWindow();

                Point mousePosition = e.getPoint();

                // Belt + braces: every branch below either falls through to the
                // explicit clearTransientDragFlags() at the bottom of the body
                // OR early-returns after its own clearTransientDragFlags() call.
                // But if the selection-update downstream (setSelectedKeyFrames ->
                // switchCards -> resetAttributes) throws -- e.g. a card switch on
                // a new keyframe type with a missing case -- both cleanups get
                // skipped and the marquee box / drag flags stay stuck. Wrapping
                // the whole body in try/finally guarantees the flags reset even
                // when downstream Swing dispatch dies.
                try
                {

                if (e.getButton() == MouseEvent.BUTTON3)
                {
                    onMouseButton3Pressed(mousePosition);
                    // Right-click never starts a drag, but clear transient
                    // state anyway in case a stray left-press left flags on.
                    clearTransientDragFlags();
                    return;
                }

                if (e.getButton() != MouseEvent.BUTTON1)
                {
                    clearTransientDragFlags();
                    return;
                }

                // A/B marker drag end. mouseDragged has been writing the
                // tick continuously; release just clears the drag flag so
                // subsequent clicks aren't interpreted as drags.
                if (draggingAMarker || draggingBMarker)
                {
                    clearTransientDragFlags();
                    return;
                }

                TimeSheetPanel timeSheetPanel = getTimeSheetPanel();

                if (timeIndicatorPressed)
                {
                    double time = getTimeIndicatorPosition();
                    setCurrentTime(time, false);
                    // Belt-and-suspenders cleanup of every transient drag flag.
                    // Otherwise a timeIndicatorPressed release could leave a
                    // previous in-body press's keyFrameClicked / allowRectangleSelect
                    // stuck true and the next click would resume that interaction.
                    clearTransientDragFlags();
                    return;
                }

                if (e.getClickCount() == 2)
                {
                    onMouseButton1DoublePressed(mousePosition);
                }

                if (keyFrameClicked)
                {
                    // Anti-drag deadzone: small mouse drift between press and
                    // release counts as a click (select-only) instead of a
                    // drag. 1px was too tight -- the user could trigger an
                    // accidental drag by trembling on the press. 5px gives
                    // breathing room while still letting an intentional drag
                    // engage promptly.
                    if (mousePosition.distance(mousePointOnPressed) < 5)
                    {
                        updateSelectedKeyFrameOnRelease(mousePosition,
                                e.isShiftDown() || e.isControlDown());
                    }
                    else
                    {
                        TimelineUnits timelineUnits = config.timelineUnits();
                        double modeMultiplier = timelineUnits.getMultiplier();

                        KeyFrame[] keyFrames = getSelectedKeyFrames();
                        KeyFrameAction[] kfa = new KeyFrameAction[0];

                        Character[] owners = new Character[keyFrames.length];
                        for (int i = 0; i < keyFrames.length; i++)
                        {
                            KeyFrame keyFrame = keyFrames[i];
                            Character owner = findKeyFrameOwner(keyFrame);
                            if (owner == null)
                            {
                                owner = selectedCharacter;
                            }
                            owners[i] = owner;
                        }

                        double mouseX = Math.max(0, Math.min(mousePosition.getX(), getWidth()));
                        double xCurrentTime = currentTimeToMouseX();

                        double change;
                        if (Math.abs(Math.abs(mouseX) - Math.abs(xCurrentTime)) > DRAG_STICK_RANGE)
                        {
                            change = round(timelineUnits, (mouseX - getMousePointOnPressed().getX()) * getZoom() / getWidth());
                        }
                        else
                        {
                            KeyFrame[] clickedFrames = getClickedKeyFrames();
                            change = 0;

                            if (clickedFrames.length > 0)
                            {
                                KeyFrame keyFrame = clickedFrames[0];
                                change = round(timelineUnits, getCurrentTime() - keyFrame.getTick());
                            }
                        }

                        // Multi-select fan-out: if more than one Character is selected via
                        // the manager / folder, every Character also gets its KFs at the
                        // same (type, originalTick) shifted by the same delta. The marquee
                        // owner(s) plus every other selected Character go through the same
                        // remove + re-add loop so the batch becomes one undo step.
                        com.creatorskit.selection.SelectionManager mgr = getTimeSheetPanel().getSelectionManager();
                        java.util.List<Character> fanOut = (mgr != null && mgr.size() > 1)
                                ? new java.util.ArrayList<>(mgr.getSelected())
                                : java.util.Collections.emptyList();

                        KeyFrame[] copies = new KeyFrame[keyFrames.length];
                        java.util.Set<String> processedFanout = new java.util.HashSet<>();
                        for (int i = 0; i < keyFrames.length; i++)
                        {
                            KeyFrame keyFrame = keyFrames[i];
                            Character owner = owners[i];

                            // Primary marquee owner: remove + re-add at new tick. The new
                            // copy goes into the post-drag selection so the marquee follows
                            // the dragged KF(s).
                            double newTick = round(timelineUnits, keyFrame.getTick() + change);
                            timeSheetPanel.removeKeyFrame(owner, keyFrame);
                            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrame, owner, KeyFrameCharacterActionType.REMOVE));

                            KeyFrame copy = KeyFrame.createCopy(keyFrame, newTick);
                            copies[i] = copy;
                            KeyFrame keyFrameToReplace = timeSheetPanel.addKeyFrame(owner, copy);
                            if (keyFrameToReplace != null)
                            {
                                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, owner, KeyFrameCharacterActionType.REMOVE));
                            }
                            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(copy, owner, KeyFrameCharacterActionType.ADD));

                            // Fan-out: same (type, originalTick) moved on every other
                            // selected Character. Dedupe so multiple marqueed KFs at the
                            // same (type, originalTick) don't double-process the same
                            // Character. Owner skipped here (already handled above).
                            KeyFrameType type = keyFrame.getKeyFrameType();
                            double originalTick = keyFrame.getTick();
                            String dedupeKey = type.name() + "@" + originalTick;
                            if (!processedFanout.add(dedupeKey))
                            {
                                continue;
                            }
                            for (Character c : fanOut)
                            {
                                if (c == owner)
                                {
                                    continue;
                                }
                                KeyFrame found = c.findKeyFrame(type, originalTick);
                                if (found == null)
                                {
                                    continue;
                                }
                                timeSheetPanel.removeKeyFrame(c, found);
                                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(found, c, KeyFrameCharacterActionType.REMOVE));

                                KeyFrame fanCopy = KeyFrame.createCopy(found, newTick);
                                KeyFrame fanReplaced = timeSheetPanel.addKeyFrame(c, fanCopy);
                                if (fanReplaced != null)
                                {
                                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(fanReplaced, c, KeyFrameCharacterActionType.REMOVE));
                                }
                                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(fanCopy, c, KeyFrameCharacterActionType.ADD));
                            }
                        }

                        setSelectedKeyFrames(copies);
                        timeSheetPanel.addKeyFrameActions(kfa);
                    }

                    keyFrameClicked = false;
                    allowRectangleSelect = false;
                }
                else if (!blockLeftClicked)
                {
                    // Empty-space click on the body: deselect everything.
                    // Skipped when the press landed inside a block rect --
                    // tryHandleBlockLeftClick already set selectedKeyFrames
                    // to the block's members and we don't want to undo that.
                    setSelectedKeyFrames(new KeyFrame[0]);
                }

                if (allowRectangleSelect)
                {
                    checkRectangleForKeyFrames(mousePosition,
                            e.isShiftDown() || e.isControlDown());
                    allowRectangleSelect = false;
                }

                // Final defensive sweep: any leftover transient flags after
                // every branch above is consumed get explicitly zeroed so the
                // next press starts from a clean state. Was the root of the
                // "marquee box stays / clicking outside still drags" bug --
                // certain release paths (e.g. mouse released outside canvas)
                // skipped one or another cleanup site.
                clearTransientDragFlags();

                }
                finally
                {
                    // Final-final sweep: re-runs even if any branch above threw
                    // (e.g. a downstream Swing dispatch hit a missing case). The
                    // operation is idempotent so the redundant call on a clean
                    // exit is harmless.
                    clearTransientDragFlags();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                super.mouseDragged(e);
                requestFocusInWindow();

                // A/B marker drag: convert pointer x to a tick and push it
                // through the validating setter (which clamps B>=0 and A<=B).
                // Snap to 0.1 so the marker lands cleanly on the same grid the
                // playback uses. mouseDragged fires even when the cursor
                // leaves the panel, so the drag follows the pointer freely.
                if (draggingAMarker || draggingBMarker)
                {
                    double tick = (double) e.getX() * getZoom() / getWidth() - getHScroll();
                    double snapped = Math.round(tick * 10.0) / 10.0;
                    TimeSheetPanel tsp = getTimeSheetPanel();
                    if (tsp != null)
                    {
                        if (draggingAMarker) tsp.setALoopTick(snapped);
                        else tsp.setBLoopTick(snapped);
                    }
                    return;
                }

                updatePreviewTime(getTimeIndicatorPosition());
            }
        });

        addMouseWheelListener(new MouseAdapter()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                super.mouseWheelMoved(e);
                int amount = e.getWheelRotation();

                if (e.isAltDown())
                {
                    if (e.isControlDown() || e.isShiftDown())
                    {
                        return;
                    }

                    getTimeSheetPanel().onZoomEvent(amount, timeSheet);
                    return;
                }

                if (e.isControlDown())
                {
                    if (e.isAltDown() || e.isShiftDown())
                    {
                        return;
                    }

                    int currentRow = managerTree.getMinSelectionRow();
                    if (currentRow == -1)
                    {
                        managerTree.setSelectionRow(0);
                        return;
                    }

                    TreePath path = null;
                    int direction = e.getWheelRotation();
                    if (direction > 0)
                    {
                        while (path == null)
                        {
                            currentRow++;
                            if (currentRow >= managerTree.getRowCount())
                            {
                                currentRow = 0;
                            }

                            path = managerTree.getPathForRow(currentRow);
                        }
                    }

                    if (direction < 0)
                    {
                        while (path == null)
                        {
                            currentRow--;
                            if (currentRow < 0)
                            {
                                currentRow = managerTree.getRowCount() - 1;
                            }

                            path = managerTree.getPathForRow(currentRow);
                        }
                    }

                    if (path != null)
                    {
                        managerTree.setSelectionPath(path);
                    }
                    return;
                }

                if (e.isShiftDown())
                {
                    if (e.isControlDown() || e.isAltDown())
                    {
                        return;
                    }

                    // 2-at-a-time matches the labels-column handler. Without
                    // the * 2 the wheel only moved one row per notch even
                    // though every other property-scroll path already
                    // multiplied -- the user-perceived "linearly with much
                    // smaller scroll steps" bug.
                    getTimeSheetPanel().scrollAttributePanel(e.getWheelRotation() * 2);
                    return;
                }

                getTimeSheetPanel().onHorizontalScrollEvent(amount);
            }
        });
    }

    public void onMouseButton3Pressed(Point p) {};

    public void onMouseButton1DoublePressed(Point p) {};

    public KeyFrame[] getKeyFrameClicked(Point point)
    {
        return new KeyFrame[0];
    }

    public void updateSelectedKeyFrameOnRelease(Point point, boolean shiftKey)
    {

    }

    public void checkRectangleForKeyFrames(Point point, boolean shiftKey)
    {

    }

    private double getTimeIndicatorPosition()
    {
        double absoluteMouseX = MouseInfo.getPointerInfo().getLocation().getX();
        double x = absoluteMouseX - getLocationOnScreen().getX();

        TimelineUnits timelineUnits = config.timelineUnits();
        double modeMultiplier = timelineUnits.getMultiplier();

        double time = round(timelineUnits, (x / getWidth() * zoom - hScroll) * modeMultiplier);

        if (time < -hScroll * modeMultiplier)
        {
            time = -hScroll * modeMultiplier;
        }

        double max = round(timelineUnits, (zoom - hScroll) * modeMultiplier);
        if (time > max)
        {
            time = max;
        }

        return time;
    }

    public double currentTimeToMouseX()
    {
        return (currentTime + hScroll) * getWidth() / zoom;
    }

    public TimeSheetPanel getTimeSheetPanel()
    {
        return toolBox.getTimeSheetPanel();
    }


    public KeyFrame[] getSelectedKeyFrames()
    {
        return getTimeSheetPanel().getSelectedKeyFrames();
    }

    public void setSelectedKeyFrames(KeyFrame[] keyFrames)
    {
        // When every selected keyframe shares one type, switch to that type's card
        // BEFORE storing the selection — that way TimeSheetPanel.setSelectedKeyFrames'
        // refresh + resetAttributes use the new active type. When the selection spans
        // multiple types we leave the active card alone so refreshKeyFrameSelectionState
        // can show the mixed-types placeholder.
        if (keyFrames != null && keyFrames.length > 0)
        {
            KeyFrameType firstType = keyFrames[0].getKeyFrameType();
            boolean sameType = true;
            for (KeyFrame kf : keyFrames)
            {
                if (kf == null || kf.getKeyFrameType() != firstType)
                {
                    sameType = false;
                    break;
                }
            }
            if (sameType)
            {
                attributePanel.switchCards(firstType);
            }
        }
        getTimeSheetPanel().setSelectedKeyFrames(keyFrames);
    }

    private void updatePreviewTime(double time)
    {
        if (config.timelineUnits() == TimelineUnits.GAMETICKS)
        {
            getTimeSheetPanel().updatePreviewTime(time);
            return;
        }

        getTimeSheetPanel().updatePreviewTime(round_10(time / 0.6));
    }

    private void setCurrentTime(double time, boolean playing)
    {
        if (config.timelineUnits() == TimelineUnits.GAMETICKS)
        {
            getTimeSheetPanel().setCurrentTime(time, playing);
            return;
        }

        getTimeSheetPanel().setCurrentTime(round_10(time / 0.6), playing);
    }

    /**
     * Rounds the given value to the nearest 1/100th
     * @param value the value to round
     * @return the value, rounded to 1 decimal place
     */
    public static double round(TimelineUnits timelineUnits, double value)
    {
        if (timelineUnits == TimelineUnits.GAMETICKS)
        {
            return round_10(value);
        }

        return round_100(roundToGametick(value));
    }

    public static double roundToGametick(double value)
    {
        double gameTicks = round_10(value / 0.6);
        return gameTicks * 0.6;
    }

    /**
     * Rounds the given value to the nearest 1/100th
     * @param value the value to round
     * @return the value, rounded to 1 decimal place
     */
    public static double round_10(double value)
    {
        int scale = (int) Math.pow(10, 1);
        return (double) Math.round(value * scale) / scale;
    }

    /**
     * Rounds the given value to the nearest 1/100th
     * @param value the value to round
     * @return the value, rounded to 2 decimal places
     */
    public static double round_100(double value)
    {
        int scale = (int) Math.pow(100, 1);
        return (double) Math.round(value * scale) / scale;
    }
}
