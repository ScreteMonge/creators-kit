package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.DataFinder;
import com.creatorskit.programming.Programmer;
import com.creatorskit.swing.manager.ManagerPanel;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

@Getter
public class ToolBoxFrame extends JFrame
{
    private ClientThread clientThread;
    private final Client client;
    private final EventBus eventBus;
    private final CreatorsPlugin plugin;
    private final ConfigManager configManager;
    private final DataFinder dataFinder;
    private final ManagerPanel managerPanel;
    private final ModelOrganizer modelOrganizer;
    private final ModelAnvil modelAnvil;
    private final CacheSearcherTab cacheSearcher;
    private final ProgrammerPanel programPanel;
    private final TransmogPanel transmogPanel;
    private final TimeSheetPanel timeSheetPanel;
    private final Programmer programmer;
    private final BufferedImage ICON = ImageUtil.loadImageResource(getClass(), "/panelicon.png");

    @Inject
    public ToolBoxFrame(Client client, EventBus eventBus, ClientThread clientThread, CreatorsPlugin plugin, ConfigManager configManager, DataFinder dataFinder, ModelOrganizer modelOrganizer, ModelAnvil modelAnvil, TransmogPanel transmogPanel)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.configManager = configManager;
        this.dataFinder = dataFinder;
        this.modelOrganizer = modelOrganizer;
        this.modelAnvil = modelAnvil;
        this.transmogPanel = transmogPanel;

        Folder rootFolder = new Folder("Master Folder", FolderType.MASTER, null, null);
        DefaultMutableTreeNode managerRootNode = new DefaultMutableTreeNode(rootFolder);
        rootFolder.setLinkedManagerNode(managerRootNode);

        Folder sidePanelFolder = new Folder("Side Panel", FolderType.SIDE_PANEL, null, managerRootNode);
        Folder managerPanelFolder = new Folder("Manager", FolderType.MANAGER, null, managerRootNode);
        DefaultMutableTreeNode managerSideNode = new DefaultMutableTreeNode(sidePanelFolder);
        DefaultMutableTreeNode managerManagerNode = new DefaultMutableTreeNode(managerPanelFolder);
        sidePanelFolder.setLinkedManagerNode(managerSideNode);
        managerPanelFolder.setLinkedManagerNode(managerManagerNode);

        JPanel objectHolder = new JPanel();
        ManagerTree managerTree = new ManagerTree(this, plugin, objectHolder, managerRootNode, managerSideNode, managerManagerNode);

        JScrollBar scrollBar = new JScrollBar(Adjustable.HORIZONTAL);
        this.timeSheetPanel = new TimeSheetPanel(client, this, plugin, clientThread, dataFinder, managerTree, scrollBar);
        this.managerPanel = new ManagerPanel(client, plugin, objectHolder, managerTree);
        this.cacheSearcher = new CacheSearcherTab(plugin, clientThread, dataFinder);
        this.programPanel = new ProgrammerPanel(client, clientThread, plugin, managerTree);
        this.programmer = new Programmer(client, clientThread, plugin, timeSheetPanel, dataFinder);

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setTitle("Creator's Kit Toolbox");
        setIconImage(ICON);

        try
        {
            String string = configManager.getConfiguration("creatorssuite", "toolBoxSize");
            String[] dimensions = string.split(",");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);
            if (width < 150)
                width = 150;
            if (height < 150)
                height = 150;
            setPreferredSize(new Dimension(width, height));
        }
        catch (Exception e)
        {
            setPreferredSize(new Dimension(1500, 800));
        }

        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent componentEvent)
            {
                Dimension dimension = getSize();
                configManager.setConfiguration("creatorssuite", "toolBoxSize", (int) dimension.getWidth() + "," + (int) dimension.getHeight());
            }
        });

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FontManager.getRunescapeBoldFont());
        if (CreatorsPlugin.test2_0)
        {
            tabbedPane.addTab("Timesheet", timeSheetPanel);
            timeSheetPanel.getTreeScrollPane().setViewportView(managerTree);
        }
        else
        {
            managerPanel.getTreeScrollPane().setViewportView(managerTree);
        }
        tabbedPane.addTab("Manager", managerPanel);
        tabbedPane.addTab("Model Organizer", modelOrganizer);
        tabbedPane.addTab("Model Anvil", modelAnvil);
        tabbedPane.addTab("Cache Searcher", cacheSearcher);
        tabbedPane.addTab("Programmer", programPanel);
        tabbedPane.addTab("Transmogger", transmogPanel);
        tabbedPane.setToolTipTextAt(0, "Manage and organize all your Objects");
        tabbedPane.setToolTipTextAt(1, "Organize Custom Models you've loaded from the cache or Forged");
        tabbedPane.setToolTipTextAt(2, "Create Custom Models by modifying and merging different models together");
        tabbedPane.setToolTipTextAt(3, "Search the cache for NPCs, Items, and Objects for their models");
        tabbedPane.setToolTipTextAt(4, "Change your Object Programs' animations, speeds, and more");
        tabbedPane.setToolTipTextAt(5, "Set animations for Transmogging your player character");

        //Move the FolderTree between the Manager and Programmer tabs when the given tab is selected
        tabbedPane.addChangeListener(e -> {
            if (e.getSource() instanceof JTabbedPane)
            {
                JTabbedPane jTabbedPane = (JTabbedPane) e.getSource();
                if (jTabbedPane.getSelectedComponent() == managerPanel)
                {
                    managerPanel.getTreeScrollPane().setViewportView(managerTree);
                }

                if (jTabbedPane.getSelectedComponent() == programPanel)
                {
                    programPanel.getTreeScrollPane().setViewportView(managerTree);
                }

                if (jTabbedPane.getSelectedComponent() == timeSheetPanel)
                {
                    timeSheetPanel.getTreeScrollPane().setViewportView(managerTree);
                }
            }
            repaint();
            revalidate();
        });

        add(tabbedPane);
        pack();
        revalidate();
    }
}
