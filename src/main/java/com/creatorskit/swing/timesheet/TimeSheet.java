package com.creatorskit.swing.timesheet;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

@Getter
@Setter
public class TimeSheet extends JPanel
{
    private double zoom = 50;
    private double hScroll = 0;
    private int vScroll = 0;
    private int selectedIndex = 0;

    private final int ROW_HEIGHT = 24;
    private final int ROW_HEIGHT_OFFSET = 1;
    private final int TEXT_HEIGHT_OFFSET = 3;

    private final int SHOW_5_ZOOM = 300;
    private final int SHOW_1_ZOOM = 80;

    private KeyFrame[] visibleKeyFrames = new KeyFrame[0];

    public TimeSheet()
    {
        setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setFocusable(true);
        requestFocusInWindow();

        setKeyBindings();

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
        revalidate();
        repaint();
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
}
