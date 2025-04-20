package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
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

import static com.creatorskit.swing.timesheet.TimeSheetPanel.round;

@Getter
@Setter
public class TimeSheet extends JPanel
{
    private ToolBoxFrame toolBox;
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

    private final Color background1 = new Color(40, 40, 40);
    private final Color background2 = new Color(42, 42, 42);

    public final int SHOW_5_ZOOM = 200;
    public final int SHOW_1_ZOOM = 50;

    private KeyFrame[] visibleKeyFrames = new KeyFrame[0];
    private Character selectedCharacter;
    private boolean keyFrameClicked = false;
    private KeyFrame[] clickedKeyFrames = new KeyFrame[0];

    public TimeSheet(ToolBoxFrame toolBox, ManagerTree managerTree, AttributePanel attributePanel)
    {
        this.toolBox = toolBox;
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
        drawRectangleSelect(g2);
        drawKeyFrames(g2);
        drawPreviewKeyFrames(g2);
        drawTextHeader(g2);
        drawTimeIndicator(g2);
        drawPreviewTimeIndicator(g2);
        revalidate();
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

    public void drawBackgroundText(Graphics g)
    {

    }

    public void drawHighlight(Graphics g)
    {

    }

    private void drawBackgroundLines(Graphics g)
    {
        if (zoom <= SHOW_1_ZOOM)
        {
            g.setColor(ColorScheme.DARKER_GRAY_COLOR);

            double spacing = this.getWidth() / zoom;
            double startOffset = hScroll;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < zoom + firstIteration; i++)
            {
                g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());
            }
        }

        if (zoom <= SHOW_5_ZOOM)
        {
            g.setColor(ColorScheme.DARKER_GRAY_COLOR.darker());

            double iterations = zoom / 5;
            double spacing = this.getWidth() / iterations;
            double startOffset = hScroll / 5;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < iterations + firstIteration; i++)
            {
                g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());

            }
        }

        g.setColor(ColorScheme.BORDER_COLOR.darker());

        double iterations = zoom / 5;
        double spacing = this.getWidth() / iterations;
        double startOffset = hScroll / 5;
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
        double x = (currentTime + hScroll) * this.getWidth() / zoom;
        char[] c = ("" + currentTime).toCharArray();
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

        double x = (previewTime + hScroll) * this.getWidth() / zoom;
        char[] c = ("" + previewTime).toCharArray();
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
        g.setColor(Color.WHITE);

        if (zoom <= SHOW_1_ZOOM)
        {
            g.setColor(Color.WHITE.darker().darker());
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics fontMetrics = g.getFontMetrics();

            double spacing = this.getWidth() / zoom;
            double startOffset = hScroll;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < zoom + firstIteration; i++)
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

            double iterations = zoom / 5;
            double spacing = this.getWidth() / iterations;
            double startOffset = hScroll / 5;
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

        double iterations = zoom / 10;
        double spacing = this.getWidth() / iterations;
        double startOffset = hScroll / 10;
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

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "VK_LEFT");
        actionMap.put("VK_LEFT", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                getTimeSheetPanel().setCurrentTime(TimeSheetPanel.round(currentTime - 0.1), false);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "VK_RIGHT");
        actionMap.put("VK_RIGHT", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                getTimeSheetPanel().setCurrentTime(TimeSheetPanel.round(currentTime + 0.1), false);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "VK_DELETE");
        actionMap.put("VK_DELETE", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (selectedCharacter == null)
                {
                    return;
                }

                getTimeSheetPanel().removeKeyFrame(selectedCharacter, getTimeSheetPanel().getSelectedKeyFrames());
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

                if (mousePosition.getY() < rowHeight)
                {
                    timeIndicatorPressed = true;

                    TimeSheetPanel timeSheetPanel = getTimeSheetPanel();
                    double previewTime = getTimeIndicatorPosition();
                    timeSheetPanel.setPreviewTime(previewTime);
                }
                else
                {
                    allowRectangleSelect = true;
                    mousePointOnPressed = mousePosition;
                }

                clickedKeyFrames = getKeyFrameClicked(mousePosition);
                keyFrameClicked = clickedKeyFrames != null;
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

                if (e.getClickCount() == 2)
                {
                    onMouseButton1DoublePressed(mousePosition);
                }

                TimeSheetPanel timeSheetPanel = getTimeSheetPanel();

                if (keyFrameClicked)
                {
                    if (mousePosition.distance(mousePointOnPressed) < 1)
                    {
                        updateSelectedKeyFrameOnRelease(mousePosition, e.isShiftDown());
                    }
                    else
                    {
                        KeyFrame[] keyFrames = getSelectedKeyFrames();
                        KeyFrameAction[] kfa = new KeyFrameAction[0];

                        for (KeyFrame keyFrame : keyFrames)
                        {
                            timeSheetPanel.removeKeyFrame(selectedCharacter, keyFrame);
                            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrame, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
                        }

                        double mouseX = Math.max(0, Math.min(mousePosition.getX(), getWidth()));
                        double xCurrentTime = currentTimeToMouseX();

                        double change;
                        if (Math.abs(Math.abs(mouseX) - Math.abs(xCurrentTime)) > DRAG_STICK_RANGE)
                        {
                            change = round((mouseX - getMousePointOnPressed().getX()) * getZoom() / getWidth());
                        }
                        else
                        {
                            KeyFrame[] clickedFrames = getClickedKeyFrames();
                            change = 0;

                            if (clickedFrames.length > 0)
                            {
                                KeyFrame keyFrame = clickedFrames[0];
                                change = round(getCurrentTime() - keyFrame.getTick());
                            }
                        }

                        KeyFrame[] copies = new KeyFrame[keyFrames.length];
                        for (int i = 0; i < keyFrames.length; i++)
                        {
                            KeyFrame keyFrame = keyFrames[i];
                            KeyFrame copy = KeyFrame.createCopy(keyFrame, round(keyFrame.getTick() + change));
                            copies[i] = copy;
                            KeyFrame keyFrameToReplace = timeSheetPanel.addKeyFrame(selectedCharacter, copy);
                            if (keyFrameToReplace != null)
                            {
                                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
                            }

                            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(copy, selectedCharacter, KeyFrameCharacterActionType.ADD));
                        }

                        setSelectedKeyFrames(copies);
                        timeSheetPanel.addKeyFrameActions(kfa);
                    }

                    keyFrameClicked = false;
                    allowRectangleSelect = false;
                }
                else
                {
                    setSelectedKeyFrames(new KeyFrame[0]);
                }

                if (allowRectangleSelect)
                {
                    checkRectangleForKeyFrames(mousePosition, e.isShiftDown());
                    allowRectangleSelect = false;
                }

                if (timeIndicatorPressed)
                {
                    double time = getTimeIndicatorPosition();
                    timeSheetPanel.setCurrentTime(time, false);
                    timeIndicatorPressed = false;
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

                TimeSheetPanel timeSheetPanel = getTimeSheetPanel();
                double previewTime = getTimeIndicatorPosition();
                timeSheetPanel.setPreviewTime(previewTime);


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

        double time = round(x / getWidth() * zoom - hScroll);

        if (time < -hScroll)
        {
            time = -hScroll;
        }

        double max = round(zoom - hScroll);
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
        getTimeSheetPanel().setSelectedKeyFrames(keyFrames);
        if (keyFrames != null && keyFrames.length > 0)
        {
            KeyFrameType type = keyFrames[keyFrames.length - 1].getKeyFrameType();
            attributePanel.switchCards(type);
        }
    }
}
