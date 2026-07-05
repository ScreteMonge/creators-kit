package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterActionType;
import com.creatorskit.swing.timesheet.keyframe.keyframeselectionmanager.KeyFrameSelectionManager;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

@Getter
@Setter
public class TimeSheet extends JPanel
{
    private ToolBoxFrame toolBox;
    private CreatorsConfig config;
    private ManagerTree managerTree;
    private AttributePanel attributePanel;
    private KeyFrameSelectionManager kfsm;

    private final BufferedImage keyframeImage = ImageUtil.loadImageResource(getClass(), "/Keyframe.png");
    private final BufferedImage keyframeSelected = ImageUtil.loadImageResource(getClass(), "/Keyframe_Selected.png");
    private final BufferedImage keyframePrimary = ImageUtil.loadImageResource(getClass(), "/Keyframe_Primary.png");

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

    private final Color background1 = new Color(40, 40, 40);
    private final Color background2 = new Color(42, 42, 42);

    public final int SHOW_5_ZOOM = 200;
    public final int SHOW_1_ZOOM = 50;

    private boolean keyFrameClicked = false;
    private LinkedHashMap<Character, KeyFrame[]> clickedKeyFrames = new LinkedHashMap<>();

    public TimeSheet(ToolBoxFrame toolBox, CreatorsConfig config, ManagerTree managerTree, AttributePanel attributePanel, KeyFrameSelectionManager kfsm)
    {
        this.toolBox = toolBox;
        this.config = config;
        this.managerTree = managerTree;
        this.attributePanel = attributePanel;
        this.kfsm = kfsm;

        setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setFocusable(true);
        requestFocusInWindow();

        setMouseListeners(this);

        revalidate();
        repaint();
    }

    public void onVerticalScrollEvent(int scroll)
    {
        vScroll = scroll;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(1));
        drawBackground(g2);
        drawHighlight(g2);
        draw3DPreview(g2);
        drawBackgroundText(g2);
        drawBackgroundLines(g2);
        drawRectangleSelect(g2);
        drawKeyFrames(g2);
        drawPreviewKeyFrames(g2);
        drawTextHeader(g2);
        drawTimeIndicator(g2);
        drawPreviewTimeIndicator(g2);
        drawRowLabels(g2);
        repaint();
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

    public void draw3DPreview(Graphics g)
    {

    }

    public void drawBackgroundText(Graphics g)
    {

    }

    public void drawRowLabels(Graphics g)
    {

    }

    public void drawHighlight(Graphics g)
    {

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

    public void updateSelectedKeyFrameOnPressed(boolean shiftDown)
    {

    }

    private void onKeyFrameDragged(Point mousePosition, boolean shiftDown)
    {
        if (mousePosition.distance(mousePointOnPressed) < 1)
        {
            updateSelectedKeyFrameOnRelease(mousePosition, shiftDown);
            return;
        }

        TimeSheetPanel timeSheetPanel = getTimeSheetPanel();
        TimelineUnits timelineUnits = config.timelineUnits();

        final List<KeyFrameAction> kfa = new ArrayList<>();

        LinkedHashMap<Character, KeyFrame[]> selected = new LinkedHashMap<>(kfsm.getSelected());
        KeyFrame primary = kfsm.getPrimary();

        selected.forEach((Character character, KeyFrame[] keyFrames) ->
        {
            for (KeyFrame keyFrame : keyFrames)
            {
                timeSheetPanel.removeKeyFrame(character, keyFrame);
                kfa.add(new KeyFrameCharacterAction(keyFrame, character, KeyFrameCharacterActionType.REMOVE));
            }
        });

        double mouseX = Math.max(0, Math.min(mousePosition.getX(), getWidth()));
        double xCurrentTime = currentTimeToMouseX();

        final double[] change = new double[]{0};
        if (Math.abs(Math.abs(mouseX) - Math.abs(xCurrentTime)) > DRAG_STICK_RANGE)
        {
            change[0] = round(timelineUnits, (mouseX - getMousePointOnPressed().getX()) * getZoom() / getWidth());
        }
        else
        {
            LinkedHashMap<Character, KeyFrame[]> clickedFrames = getClickedKeyFrames();
            change[0] = 0;

            if (!clickedFrames.isEmpty())
            {
                Map.Entry<Character, KeyFrame[]> firstEntry = clickedFrames.entrySet().iterator().next();
                KeyFrame[] keyFrames = firstEntry.getValue();
                KeyFrame keyFrame = keyFrames[0];
                change[0] = round(timelineUnits, getCurrentTime() - keyFrame.getTick());
            }
        }

        LinkedHashMap<Character, KeyFrame[]> copies = new LinkedHashMap<>();
        KeyFrame[] primaryCopy = new KeyFrame[1];

        selected.forEach((Character character, KeyFrame[] keyFrames) ->
        {
            KeyFrame[] keyFrameCopies = new KeyFrame[keyFrames.length];
            for (int i = 0; i < keyFrames.length; i++)
            {
                KeyFrame keyFrame = keyFrames[i];
                KeyFrame copy = KeyFrame.createCopy(keyFrame, round(timelineUnits, keyFrame.getTick() + change[0]));
                keyFrameCopies[i] = copy;

                if (keyFrame == primary)
                {
                    primaryCopy[0] = copy;
                }

                KeyFrame keyFrameToReplace = timeSheetPanel.addKeyFrame(character, copy);
                if (keyFrameToReplace != null)
                {
                    kfa.add(new KeyFrameCharacterAction(keyFrameToReplace, character, KeyFrameCharacterActionType.REMOVE));
                }

                kfa.add(new KeyFrameCharacterAction(copy, character, KeyFrameCharacterActionType.ADD));
            }

            copies.put(character, keyFrameCopies);
        });

        kfsm.addAll(copies, primaryCopy[0]);
        timeSheetPanel.stackKeyFrameActions(kfa);
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
                keyFrameClicked = clickedKeyFrames != null && !clickedKeyFrames.isEmpty();
                if (keyFrameClicked)
                {
                    allowRectangleSelect = false;
                    updateSelectedKeyFrameOnPressed(e.isShiftDown());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                super.mouseClicked(e);
                requestFocusInWindow();

                Point mousePosition = e.getPoint();

                if (e.getButton() == MouseEvent.BUTTON3)
                {
                    onMouseButton3Pressed(mousePosition);
                    return;
                }

                if (e.getButton() != MouseEvent.BUTTON1)
                {
                    return;
                }

                if (timeIndicatorPressed)
                {
                    double time = getTimeIndicatorPosition();
                    setCurrentTime(time, false);
                    timeIndicatorPressed = false;
                    return;
                }

                boolean keyFrameWasClicked = keyFrameClicked;
                if (keyFrameClicked)
                {
                    onKeyFrameDragged(mousePosition, e.isShiftDown());
                    keyFrameClicked = false;
                    allowRectangleSelect = false;
                }

                boolean rectangleSelectionFound = false;
                if (allowRectangleSelect)
                {
                    rectangleSelectionFound = checkRectangleForKeyFrames(mousePosition, e.isShiftDown());
                    allowRectangleSelect = false;

                    if (!rectangleSelectionFound)
                    {
                        updateTableSelection(mousePosition);
                    }
                }

                if (!keyFrameWasClicked && !rectangleSelectionFound)
                {
                    kfsm.clear();
                    getTimeSheetPanel().onKeyFrameSelectionChanged();
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

                    getTimeSheetPanel().scrollAttributePanel(e.getWheelRotation());
                    return;
                }

                getTimeSheetPanel().onHorizontalScrollEvent(amount);
            }
        });
    }

    public void onMouseButton3Pressed(Point p) {};

    public void updateTableSelection(Point p) {};

    public LinkedHashMap<Character, KeyFrame[]> getKeyFrameClicked(Point point)
    {
        return null;
    }

    public void updateSelectedKeyFrameOnRelease(Point point, boolean shiftKey)
    {

    }

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

        return true;
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
