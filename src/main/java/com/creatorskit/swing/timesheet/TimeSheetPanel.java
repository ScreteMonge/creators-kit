package com.creatorskit.swing.timesheet;

import com.creatorskit.CreatorsPlugin;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

public class TimeSheetPanel extends JPanel
{
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final GridBagConstraints c = new GridBagConstraints();
    @Getter
    private final TimeSheet timeSheet;
    @Getter
    private final TimeTree timeTree;
    private final JScrollBar scrollBar;
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage SWITCH = ImageUtil.loadImageResource(getClass(), "/Switch.png");

    private int ABSOLUTE_MAX_SEQUENCE_LENGTH = 100000;
    private final int ZOOM_MAX = 500;
    private final int ZOOM_MIN = 5;

    private double zoom = 50;
    private double hScroll = 0;
    private int vScroll = 0;
    private int maxHScroll = 200;

    boolean pauseScrollBarListener = false;

    @Inject
    public TimeSheetPanel(@Nullable Client client, CreatorsPlugin plugin, ClientThread clientThread, TimeSheet timeSheet, TimeTree timeTree, JScrollBar scrollBar)
    {
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.timeSheet = timeSheet;
        this.timeTree = timeTree;
        this.scrollBar = scrollBar;

        timeSheet.addMouseWheelListener(e ->
        {
            int amount = e.getWheelRotation();

            if (e.isAltDown())
            {
                onZoomEvent(amount);
                return;
            }

            onHorizontalScrollEvent(amount);
        });

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setupScrollBar();

        setupManager();
    }

    private void setupManager()
    {
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.weightx = 0;
        c.weighty = 5;
        c.gridx = 0;
        c.gridy = 1;
        add(timeTree, c);

        c.weightx = 10;
        c.weighty = 5;
        c.gridx = 1;
        c.gridy = 1;
        add(timeSheet, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 2;
        add(scrollBar, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 3;
        add(buildControlPanel(), c);
    }

    private void setupScrollBar()
    {
        scrollBar.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        scrollBar.setPreferredSize(new Dimension(0, 15));
        scrollBar.setMinimum(-10);
        scrollBar.setMaximum(maxHScroll);
        scrollBar.setBlockIncrement((int) (zoom / 5));
        scrollBar.setUnitIncrement((int) (zoom / 50));
        scrollBar.setVisibleAmount((int) (zoom));
        scrollBar.setValue(0);

        scrollBar.addAdjustmentListener(e ->
        {
            if (pauseScrollBarListener)
            {
                return;
            }

            hScroll = -e.getValue();
            updateSheets();
        });


    }

    private JPanel buildControlPanel()
    {
        JPanel backPanel = new JPanel();
        backPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton backButton = new JButton(new ImageIcon(CLOSE));
        JButton playButton = new JButton(new ImageIcon(SWITCH));
        JButton forwardButton = new JButton(new ImageIcon(CLOSE));

        backPanel.add(backButton);
        backPanel.add(playButton);
        backPanel.add(forwardButton);

        return backPanel;
    }

    public void onZoomEvent(int amount)
    {
        double x = timeSheet.getMousePosition(true).getX();
        double change = zoom;
        zoom += 5 * amount;
        if (zoom < ZOOM_MIN)
            zoom = ZOOM_MIN;

        if (zoom > ZOOM_MAX)
            zoom = ZOOM_MAX;


        change -= zoom;
        hScroll -= change * (x / timeSheet.getWidth());
        if (hScroll < -ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            hScroll = -ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        if (hScroll > 10)
        {
            hScroll = 10;
        }

        int maxVisibleValue = (int) (zoom - hScroll);
        if (maxVisibleValue > maxHScroll)
        {
            maxHScroll = maxVisibleValue;
        }

        pauseScrollBarListener = true;
        updateScrollBar();
        pauseScrollBarListener = false;
        updateSheets();
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

        int maxVisibleValue = (int) (zoom - hScroll);
        if (maxVisibleValue > maxHScroll)
        {
            maxHScroll = maxVisibleValue;
        }

        updateScrollBar();
        updateSheets();
    }

    private void updateSheets()
    {
        timeSheet.setHScroll(hScroll);
        timeSheet.setVScroll(vScroll);
        timeSheet.setZoom(zoom);
    }

    private void updateScrollBar()
    {
        scrollBar.setMaximum(maxHScroll);
        scrollBar.setBlockIncrement((int) (zoom / 5));
        scrollBar.setUnitIncrement((int) (zoom / 50));
        scrollBar.setVisibleAmount((int) (zoom));
        scrollBar.setValue((int) -hScroll);
    }
}
