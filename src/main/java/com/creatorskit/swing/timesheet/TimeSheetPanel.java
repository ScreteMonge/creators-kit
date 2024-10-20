package com.creatorskit.swing.timesheet;

import com.creatorskit.CreatorsPlugin;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class TimeSheetPanel extends JPanel
{
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final GridBagConstraints c = new GridBagConstraints();
    private final TimeSheet timeSheet;
    private final AttributeSheet attributeSheet;
    @Getter
    private final TimeTree timeTree;
    private final JScrollBar scrollBar;
    private final JPanel attributePanel = new JPanel();
    private final JPanel cardPanel = new JPanel();
    private final JPanel labelPanel = new JPanel();
    private final JPanel controlPanel = new JPanel();
    private final JLabel objectLabel = new JLabel("Pick an Object");
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");

    private final int ABSOLUTE_MAX_SEQUENCE_LENGTH = 100000;
    private final int ZOOM_MAX = 500;
    private final int ZOOM_MIN = 5;

    private final String MOVE_CARD = "Movement";
    private final String ANIM_CARD = "Animation";
    private final String ORI_CARD = "Orientation";
    private final String SPAWN_CARD = "Spawn";
    private final String MODEL_CARD = "Model";
    private final String TEXT_CARD = "Text";
    private final String OVER_CARD = "Overhead";
    private final String HITS_CARD = "Hitsplat";
    private final String HEALTH_CARD = "Healthbar";
    private final String LABEL_OFFSET = "  ";

    private double zoom = 50;
    private double hScroll = 0;
    private int vScroll = 0;
    private int maxHScroll = 200;

    private double currentTime = 0;

    boolean pauseScrollBarListener = false;

    @Inject
    public TimeSheetPanel(@Nullable Client client, CreatorsPlugin plugin, ClientThread clientThread, TimeSheet timeSheet, AttributeSheet attributeSheet, TimeTree timeTree, JScrollBar scrollBar)
    {
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.timeSheet = timeSheet;
        this.attributeSheet = attributeSheet;
        this.timeTree = timeTree;
        this.scrollBar = scrollBar;

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        setupTimeSheets();
        setupControlPanel();
        setupAttributePanel();
        setupAttributeSheet();
        setupScrollBar();
        setupManager();
    }

    private void setupAttributePanel()
    {
        attributePanel.setLayout(new GridBagLayout());
        attributePanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        objectLabel.setFont(FontManager.getDefaultBoldFont());
        objectLabel.setHorizontalAlignment(SwingConstants.LEFT);

        cardPanel.setLayout(new CardLayout());
        cardPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        attributePanel.add(objectLabel, c);

        c.weightx = 0;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        attributePanel.add(cardPanel, c);

        JPanel moveCard = new JPanel();
        JPanel animCard = new JPanel();
        JPanel oriCard = new JPanel();
        JPanel spawnCard = new JPanel();
        JPanel modelCard = new JPanel();
        JPanel textCard = new JPanel();
        JPanel overCard = new JPanel();
        JPanel hitsCard = new JPanel();
        JPanel healthCard = new JPanel();
        cardPanel.add(moveCard, MOVE_CARD);
        cardPanel.add(animCard, ANIM_CARD);
        cardPanel.add(oriCard, ORI_CARD);
        cardPanel.add(spawnCard, SPAWN_CARD);
        cardPanel.add(modelCard, MODEL_CARD);
        cardPanel.add(textCard, TEXT_CARD);
        cardPanel.add(overCard, OVER_CARD);
        cardPanel.add(hitsCard, HITS_CARD);
        cardPanel.add(healthCard, HEALTH_CARD);

        moveCard.add(new JLabel("Movement"));
    }

    private void setupManager()
    {
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 5;
        c.gridx = 0;
        c.gridy = 0;
        add(timeTree, c);

        c.gridwidth = 1;
        c.weightx = 5;
        c.weighty = 5;
        c.gridx = 2;
        c.gridy = 0;
        add(timeSheet, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 1;
        add(scrollBar, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 2;
        add(controlPanel, c);

        c.gridheight = 3;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 1;
        add(attributePanel, c);

        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 3;
        add(labelPanel, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 3;
        add(attributeSheet, c);
    }

    private void setupTimeSheets()
    {
        timeSheet.addMouseWheelListener(e ->
        {
            int amount = e.getWheelRotation();

            if (e.isAltDown())
            {
                onZoomEvent(amount, timeSheet);
                return;
            }

            onHorizontalScrollEvent(amount);
        });

        timeSheet.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                super.mouseClicked(e);
                double x = timeSheet.getMousePosition().getX();
                currentTime = round(x / timeSheet.getWidth() * zoom - hScroll);
                timeSheet.setCurrentTime(currentTime);
                attributeSheet.setCurrentTime(currentTime);
            }
        });

        attributeSheet.addMouseWheelListener(e ->
        {
            int amount = e.getWheelRotation();

            if (e.isAltDown())
            {
                onZoomEvent(amount, attributeSheet);
                return;
            }

            onHorizontalScrollEvent(amount);
        });

        attributeSheet.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                super.mouseClicked(e);
                double x = attributeSheet.getMousePosition().getX();
                currentTime = round(x / attributeSheet.getWidth() * zoom - hScroll);
                attributeSheet.setCurrentTime(currentTime);
                timeSheet.setCurrentTime(currentTime);
            }
        });
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

    private void setupControlPanel()
    {
        controlPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton backButton = new JButton(new ImageIcon(CLOSE));
        JButton playButton = new JButton(new ImageIcon(HELP));
        JButton forwardButton = new JButton(new ImageIcon(CLOSE));

        controlPanel.add(backButton);
        controlPanel.add(playButton);
        controlPanel.add(forwardButton);
    }

    private void setupAttributeSheet()
    {
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setBorder(new EmptyBorder(1, 0, 1, 0));
        labelPanel.setLayout(new GridLayout(0, 1, 0, 0));
        JLabel[] labels = new JLabel[10];
        for (int i = 0; i < 10; i++)
        {
            JLabel label = new JLabel();
            label.setFocusable(true);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setOpaque(true);
            label.setPreferredSize(new Dimension(100, 24));
            label.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            label.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    labels[attributeSheet.getSelectedIndex()].setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    label.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                    CardLayout cl = (CardLayout)(cardPanel.getLayout());
                    cl.show(cardPanel, label.getText().replaceAll(LABEL_OFFSET, ""));

                    super.mousePressed(e);
                    for (int f = 0; f < labels.length; f++)
                    {
                        if (labels[f] == label)
                        {
                            attributeSheet.setSelectedIndex(f);
                        }
                    }
                }
            });
            labels[i] = label;
            labelPanel.add(label);
        }

        labels[1].setText(MOVE_CARD + LABEL_OFFSET);
        labels[2].setText(ANIM_CARD + LABEL_OFFSET);
        labels[3].setText(ORI_CARD + LABEL_OFFSET);
        labels[4].setText(SPAWN_CARD + LABEL_OFFSET);
        labels[5].setText(MODEL_CARD + LABEL_OFFSET);
        labels[6].setText(TEXT_CARD + LABEL_OFFSET);
        labels[7].setText(OVER_CARD + LABEL_OFFSET);
        labels[8].setText(HITS_CARD + LABEL_OFFSET);
        labels[9].setText(HEALTH_CARD + LABEL_OFFSET);
    }

    public void onZoomEvent(int amount, TimeSheet source)
    {
        double x = source.getMousePosition(true).getX();
        double change = zoom;
        zoom += 5 * amount;
        if (zoom < ZOOM_MIN)
            zoom = ZOOM_MIN;

        if (zoom > ZOOM_MAX)
            zoom = ZOOM_MAX;


        change -= zoom;
        hScroll -= change * (x / source.getWidth());
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
        attributeSheet.setHScroll(hScroll);
        attributeSheet.setVScroll(vScroll);
        attributeSheet.setZoom(zoom);
    }

    private void updateScrollBar()
    {
        pauseScrollBarListener = true;
        scrollBar.setMaximum(maxHScroll);
        scrollBar.setBlockIncrement((int) (zoom / 5));
        scrollBar.setUnitIncrement((int) (zoom / 50));
        scrollBar.setVisibleAmount((int) (zoom));
        scrollBar.setValue((int) -hScroll);
        pauseScrollBarListener = false;
    }

    private double round (double value)
    {
        int scale = (int) Math.pow(10, 1);
        return (double) Math.round(value * scale) / scale;
    }
}
