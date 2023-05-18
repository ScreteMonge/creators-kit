package com.creatorssuite.panels;

import com.creatorssuite.CreatorsPlugin;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ProgramPanel extends JFrame
{
    @Inject
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    JPanel graph = new JPanel();
    private final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private GridBagConstraints c = new GridBagConstraints();
    private int timeline = 50;
    private int rows = 5;

    @Inject
    public ProgramPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setTitle("RuneLite Object Programmer");
        setIconImage(icon);


        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        add(setupDopeSheet(), c);
        pack();
    }

    private JPanel setupDopeSheet()
    {
        JPanel dopeSheet = new JPanel();
        dopeSheet.setLayout(new GridBagLayout());
        c.fill = GridBagConstraints.BOTH;

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        JSlider arrowSlider = new JSlider(0, timeline, 0);
        arrowSlider.setPaintLabels(true);
        arrowSlider.setPreferredSize(new Dimension(800, 40));
        arrowSlider.setFont(FontManager.getRunescapeSmallFont());
        arrowSlider.setFocusable(false);
        arrowSlider.setSnapToTicks(true);
        arrowSlider.setMinorTickSpacing(1);
        arrowSlider.setPaintTicks(true);
        arrowSlider.setMajorTickSpacing(5);
        arrowSlider.setPaintTrack(true);
        arrowSlider.setUI(new JSliderUI(arrowSlider));
        dopeSheet.add(arrowSlider, c);


        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.ipadx = 5;
        JPanel column = new JPanel(new GridLayout(rows, 1));
        for (int i = 0; i < rows; i++)
        {
            JLabel label = new JLabel("Object_" + i);
            label.setFont(FontManager.getRunescapeSmallFont());
            column.add(label);
        }
        dopeSheet.add(column, c);



        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.ipadx = 0;
        graph.setLayout(new GridLayout(rows, timeline));
        graph.setBorder(new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        dopeSheet.add(graph, c);

        return dopeSheet;
    }
}
