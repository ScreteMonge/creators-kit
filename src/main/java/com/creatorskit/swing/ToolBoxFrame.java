package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Getter
public class ToolBoxFrame extends JFrame
{
    private ClientThread clientThread;
    private final Client client;
    private final CreatorsPlugin plugin;
    private final ManagerPanel managerPanel;
    private final ModelOrganizer modelOrganizer;
    private final ModelAnvil modelAnvil;
    private final ProgramPanel programPanel;
    private final TransmogPanel transmogPanel;
    private final BufferedImage ICON = ImageUtil.loadImageResource(getClass(), "/panelicon.png");

    @Inject
    public ToolBoxFrame(Client client, ClientThread clientThread, CreatorsPlugin plugin, ManagerPanel managerPanel, ModelOrganizer modelOrganizer, ModelAnvil modelAnvil, ProgramPanel programPanel, TransmogPanel transmogPanel)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.managerPanel = managerPanel;
        this.modelOrganizer = modelOrganizer;
        this.modelAnvil = modelAnvil;
        this.programPanel = programPanel;
        this.transmogPanel = transmogPanel;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setTitle("Creator's Kit Toolbox");
        setIconImage(ICON);
        setPreferredSize(new Dimension(1200, 800));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FontManager.getRunescapeBoldFont());
        tabbedPane.addTab("Manager", managerPanel);
        tabbedPane.addTab("Model Organizer", modelOrganizer);
        tabbedPane.addTab("Model Anvil", modelAnvil);
        tabbedPane.addTab("Programmer", programPanel);
        tabbedPane.addTab("Transmogger", transmogPanel);
        tabbedPane.setToolTipTextAt(0, "Organize Custom Models you've loaded from the cache or Forged");
        tabbedPane.setToolTipTextAt(1, "Create Custom Models by modifying and merging different models together");
        tabbedPane.setToolTipTextAt(2, "Change your Object Programs' animations, speeds, and more");

        add(tabbedPane);
        pack();
        revalidate();
    }
}
