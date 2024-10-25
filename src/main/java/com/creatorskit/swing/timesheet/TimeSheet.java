package com.creatorskit.swing.timesheet;

import com.creatorskit.Character;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

@Getter
@Setter
public class TimeSheet extends JPanel
{
    private ToolBoxFrame toolBox;

    private final BufferedImage KEYFRAME = ImageUtil.loadImageResource(getClass(), "/Keyframe.png");

    private double zoom = 50;
    private double hScroll = 0;
    private int vScroll = 0;
    private int selectedIndex = 0;

    private double currentTime = 0;
    private double previewTime = 0;
    private boolean timeIndicatorPressed = false;

    private final int ROW_HEIGHT = 24;
    private final int ROW_HEIGHT_OFFSET = 1;
    private final int TEXT_HEIGHT_OFFSET = 5;

    private final int SHOW_5_ZOOM = 200;
    private final int SHOW_1_ZOOM = 50;

    private boolean drawMainFrames = true;

    private KeyFrame[] visibleKeyFrames = new KeyFrame[0];
    private Character selectedCharacter;

    public TimeSheet(ToolBoxFrame toolBox)
    {
        this.toolBox = toolBox;

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

    public void addVisibleKeyFrame(KeyFrame keyFrame)
    {
        visibleKeyFrames = ArrayUtils.add(visibleKeyFrames, keyFrame);
    }

    public void removeVisibleKeyFrame(KeyFrame keyFrame)
    {
        visibleKeyFrames = ArrayUtils.removeElement(visibleKeyFrames, keyFrame);
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
        drawHighlight(g2);
        drawBackgroundLines(g2);
        drawTextHeader(g2);
        drawTimeIndicator(g2);
        drawPreviewTimeIndicator(g2);
        drawKeyFrames(g2);
        revalidate();
        repaint();
    }

    private void drawKeyFrames(Graphics g)
    {
        if (selectedCharacter == null)
        {
            return;
        }

        int yImageOffset = (KEYFRAME.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = KEYFRAME.getWidth() / 2;
        KeyFrame[][] frames = selectedCharacter.getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame[] keyFrames = frames[i];
            for (int e = 0; e < keyFrames.length; e++)
            {
                double zoomFactor = this.getWidth() / zoom;
                g.drawImage(
                        KEYFRAME,
                        (int) ((keyFrames[e].getTick() + hScroll) * zoomFactor - xImageOffset),
                        ROW_HEIGHT_OFFSET + ROW_HEIGHT + ROW_HEIGHT * i - yImageOffset,
                        null);
            }
        }
    }

    private void drawHighlight(Graphics g)
    {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, selectedIndex * ROW_HEIGHT + ROW_HEIGHT_OFFSET - vScroll, this.getWidth(), ROW_HEIGHT);
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
        int width = g.getFontMetrics().charsWidth(c, 0, c.length);
        int textBuffer = 16;

        g.setColor(new Color(74, 121, 192));
        g.drawLine((int) x, 0, (int) x, this.getHeight());

        g.fillRoundRect((int) (x - (width + textBuffer) / 2), 0, width + textBuffer, ROW_HEIGHT, 10, 10);

        g.setColor(Color.WHITE);
        g.drawChars(c, 0, c.length, (int) (x - width / 2), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);

    }

    private void drawPreviewTimeIndicator(Graphics g)
    {
        if (!timeIndicatorPressed)
        {
            return;
        }

        double x = (previewTime + hScroll) * this.getWidth() / zoom;
        char[] c = ("" + previewTime).toCharArray();
        int width = g.getFontMetrics().charsWidth(c, 0, c.length);
        int textBuffer = 16;

        g.setColor(new Color(49, 84, 128));
        g.drawLine((int) x, 0, (int) x, this.getHeight());

        g.fillRoundRect((int) (x - (width + textBuffer) / 2), 0, width + textBuffer, ROW_HEIGHT, 10, 10);

        g.setColor(Color.WHITE);
        g.drawChars(c, 0, c.length, (int) (x - width / 2), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);

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
                g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);
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
                g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);
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
            g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);
        }
    }

    private void setKeyBindings()
    {
        ActionMap actionMap = getActionMap();
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        String vkS = "VK_S";
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), vkS);
        actionMap.put(vkS, new KeyAction(vkS));
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

                if (getMousePosition().getY() < ROW_HEIGHT)
                {
                    timeIndicatorPressed = true;

                    TimeSheetPanel timeSheetPanel = toolBox.getTimeSheetPanel();
                    double previewTime = getTimeIndicatorPosition();
                    timeSheetPanel.setPreviewTime(previewTime);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                super.mouseClicked(e);
                requestFocusInWindow();

                if (e.getButton() != MouseEvent.BUTTON1)
                {
                    return;
                }

                if (!timeIndicatorPressed)
                {
                    return;
                }

                TimeSheetPanel timeSheetPanel = toolBox.getTimeSheetPanel();
                double time = getTimeIndicatorPosition();
                timeSheetPanel.setCurrentTime(time);
                timeIndicatorPressed = false;
            }
        });

        addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                super.mouseDragged(e);
                requestFocusInWindow();

                TimeSheetPanel timeSheetPanel = toolBox.getTimeSheetPanel();
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
                    toolBox.getTimeSheetPanel().onZoomEvent(amount, timeSheet);
                    return;
                }

                toolBox.getTimeSheetPanel().onHorizontalScrollEvent(amount);
            }
        });
    }

    private double getTimeIndicatorPosition()
    {
        double absoluteMouseX = MouseInfo.getPointerInfo().getLocation().getX();
        double x = absoluteMouseX - getLocationOnScreen().getX();

        double time = TimeSheetPanel.round(x / getWidth() * zoom - hScroll);

        if (time < -hScroll)
        {
            time = -hScroll;
        }

        double max = TimeSheetPanel.round(zoom - hScroll);
        if (time > max)
        {
            time = max;
        }

        return time;
    }
}
