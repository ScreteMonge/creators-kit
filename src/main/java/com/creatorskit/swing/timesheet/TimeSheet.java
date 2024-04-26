package com.creatorskit.swing.timesheet;

import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

@Getter
@Setter
public class TimeSheet extends JPanel implements MouseWheelListener
{
    private double zoom = 250;
    private double hScroll = 0;
    private int vScroll = 0;
    private int selectedIndex = 0;
    private double maxScrollLength = 500;

    private final int ROW_HEIGHT = 24;
    private final int ROW_HEIGHT_OFFSET = 1;
    private final int TEXT_HEIGHT_OFFSET = 3;
    private int ABSOLUTE_MAX_SEQUENCE_LENGTH = 100000;

    private KeyFrame[] visibleKeyFrames = new KeyFrame[0];

    private final JScrollBar scrollBar = new JScrollBar();

    public TimeSheet()
    {
        setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setFocusable(true);
        requestFocusInWindow();

        setKeyBindings();
        addMouseWheelListener(this);

        revalidate();
        repaint();

        //as zoom decreases (hones in), thumb should shrink
        /*
        System.out.println("zoom: " + zoom);
        System.out.println("maxscrollength: " + maxScrollLength);
        System.out.println("hScroll: " + hScroll);

        BoundedRangeModel boundedRangeModel = scrollBar.getModel();
        boundedRangeModel.setMaximum((int) maxScrollLength);
        boundedRangeModel.setValue((int) -hScroll);
        boundedRangeModel.setExtent((int) (maxScrollLength + hScroll));
        scrollBar.setOrientation(JScrollBar.HORIZONTAL);
        add(scrollBar, BorderLayout.PAGE_END);

         */

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



    public void onZoomEvent(int amount)
    {
        double x = getMousePosition().getX();
        double change = zoom;
        zoom += 5 * amount;
        if (zoom < 50)
            zoom = 50;

        if (zoom > 500)
            zoom = 500;


        change -= zoom;
        hScroll -= change * (x / getWidth());
        if (hScroll < -ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            hScroll = -ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        if (hScroll > 10)
        {
            hScroll = 10;
        }

        //updateScrollBar();
    }

    public void onHorizontalScrollEvent(int amount)
    {
        hScroll -= amount * zoom / 50;
        if (hScroll < -ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            hScroll = -ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        if (hScroll > 10)
        {
            hScroll = 10;
        }

        //updateScrollBar();
    }

    private void updateScrollBar()
    {
        if (hScroll <= -1000)
        {
            maxScrollLength = -hScroll;
        }

        System.out.println("zoom: " + zoom);
        System.out.println("maxscrollength: " + maxScrollLength);
        System.out.println("hScroll: " + hScroll);

        BoundedRangeModel boundedRangeModel = scrollBar.getModel();
        boundedRangeModel.setMaximum((int) maxScrollLength);
        boundedRangeModel.setValue((int) -hScroll);
        boundedRangeModel.setExtent((int) (maxScrollLength + hScroll));
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
        Color lightLine = ColorScheme.DARKER_GRAY_COLOR;
        Color darkLine = ColorScheme.BORDER_COLOR.darker();

        double iterations = zoom / 5;
        double spacing = this.getWidth() / iterations;
        double startOffset = hScroll / 5;
        int firstIteration = (int) Math.ceil(-1 * startOffset);
        boolean useDarkLine = firstIteration % 2 == 0;

        for (int i = firstIteration; i < iterations + firstIteration; i++)
        {
            g.setColor(useDarkLine ? darkLine : lightLine);
            g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());
            useDarkLine = !useDarkLine;
        }
    }

    private void drawTextHeader(Graphics g)
    {
        g.setColor(Color.WHITE);

        double iterations = zoom / 10;
        double spacing = this.getWidth() / iterations;
        double startOffset = hScroll / 10;
        int firstIteration = (int) Math.ceil(-1 * startOffset);
        FontMetrics fontMetrics = g.getFontMetrics();

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

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        int amount = e.getWheelRotation();

        if (e.isAltDown())
        {
            onZoomEvent(amount);
            return;
        }

        onHorizontalScrollEvent(amount);
    }
}
