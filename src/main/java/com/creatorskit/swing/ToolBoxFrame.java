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
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.net.URI;

@Getter
public class ToolBoxFrame extends JFrame
{
    private ClientThread clientThread;
    private final Client client;
    private final EventBus eventBus;
    private final CreatorsPlugin plugin;
    private final ConfigManager configManager;
    private final JMenuBar jMenuBar;
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
        this.jMenuBar = new JMenuBar();
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

        setupMenuBar();
        this.timeSheetPanel = new TimeSheetPanel(client, this, plugin, clientThread, dataFinder, managerTree);
        this.managerPanel = new ManagerPanel(client, plugin, objectHolder, managerTree);
        this.cacheSearcher = new CacheSearcherTab(plugin, clientThread, dataFinder);
        this.programPanel = new ProgrammerPanel(client, clientThread, plugin, managerTree);
        this.programmer = new Programmer(client, clientThread, plugin, timeSheetPanel, dataFinder);

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setTitle("Creator's Kit Toolbox");
        setIconImage(ICON);
        setLayout(new BorderLayout());
        setupWindow();

        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent componentEvent)
            {
                Dimension dimension = getSize();
                configManager.setConfiguration("creatorssuite", "toolBoxSize", (int) dimension.getWidth() + "," + (int) dimension.getHeight());

                if (getExtendedState() == Frame.NORMAL)
                {
                    configManager.setConfiguration("creatorssuite", "toolBoxMaximized", "false");
                }

                if (getExtendedState() == Frame.MAXIMIZED_BOTH)
                {
                    configManager.setConfiguration("creatorssuite", "toolBoxMaximized", "true");
                }
            }

            public void componentMoved(ComponentEvent componentEvent)
            {
                try
                {
                    Point p = getLocationOnScreen();
                    configManager.setConfiguration("creatorssuite", "toolBoxPoint", (int) p.getX() + "," + (int) p.getY());
                }
                catch (Exception e) {}
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

        add(jMenuBar, BorderLayout.PAGE_START);
        add(tabbedPane, BorderLayout.CENTER);
        pack();
        revalidate();
    }

    private void setupWindow()
    {
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

        try
        {
            String string = configManager.getConfiguration("creatorssuite", "toolBoxPoint");
            String[] point = string.split(",");
            int x = Integer.parseInt(point[0]);
            int y = Integer.parseInt(point[1]);
            setLocation(x, y);
        }
        catch (Exception e)
        {
            setLocation(0, 0);
        }

        try
        {
            String string = configManager.getConfiguration("creatorssuite", "toolBoxMaximized");
            boolean maximize = Boolean.parseBoolean(string);
            if (maximize)
            {
                setExtendedState(Frame.MAXIMIZED_BOTH);
            }
            else
            {
                setExtendedState(Frame.NORMAL);
            }
        }
        catch (Exception e)
        {
            setExtendedState(Frame.NORMAL);
        }
    }

    private void setupMenuBar()
    {
        jMenuBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
        jMenuBar.setFont(FontManager.getRunescapeBoldFont());
        jMenuBar.setBorder(new MatteBorder(0, 0, 1, 0, ColorScheme.DARKER_GRAY_COLOR));

        JMenu file = new JMenu("File");
        jMenuBar.add(file);

        JMenuItem saveAs = new JMenuItem("Save Setup");
        saveAs.addActionListener(e -> plugin.getCreatorsPanel().openSaveDialog());
        file.add(saveAs);

        JMenuItem load = new JMenuItem("Load Setup");
        load.addActionListener(e -> plugin.getCreatorsPanel().openLoadSetupDialog());
        file.add(load);

        if (CreatorsPlugin.test2_0)
        {
            JMenu timeSheet = new JMenu("Timesheet");
            jMenuBar.add(timeSheet);

            JMenuItem copyKeyFrames = new JMenuItem("Copy KeyFrames");
            copyKeyFrames.addActionListener(e -> timeSheetPanel.copyKeyFrames());
            timeSheet.add(copyKeyFrames);

            JMenuItem pasteKeyFrames = new JMenuItem("Paste KeyFrames");
            pasteKeyFrames.addActionListener(e -> timeSheetPanel.pasteKeyFrames());
            timeSheet.add(pasteKeyFrames);

            JMenuItem undo = new JMenuItem("Undo KeyFrames");
            undo.addActionListener(e -> timeSheetPanel.undo());
            timeSheet.add(undo);

            JMenuItem redo = new JMenuItem("Redo KeyFrames");
            redo.addActionListener(e -> timeSheetPanel.redo());
            timeSheet.add(redo);
        }

        JMenu resources = new JMenu("Resources");
        jMenuBar.add(resources);

        JMenuItem wikiDB = new JMenuItem("Minimal OSRS Database");
        wikiDB.addActionListener(e -> openLink("https://chisel.weirdgloop.org/moid/index.html"));
        resources.add(wikiDB);

        JMenuItem runeMonk = new JMenuItem("RuneMonk");
        runeMonk.addActionListener(e -> openLink("https://runemonk.com/tools/entityviewer-beta/"));
        resources.add(runeMonk);

        JMenuItem animations = new JMenuItem("Community Animation List");
        animations.addActionListener(e -> openLink("https://docs.google.com/spreadsheets/d/16umDNbfiBCF-fI5DNe0dGQo2D5a2l2RgJuNVkQgTm_k/edit?gid=0#gid=0"));
        resources.add(animations);


        JMenu help = new JMenu("Help");
        jMenuBar.add(help);

        JMenuItem youtube = new JMenuItem("Youtube Tutorial");
        youtube.addActionListener(e -> openLink("https://www.youtube.com/watch?v=E_9c-LwDRRY"));
        help.add(youtube);

        JMenuItem discord = new JMenuItem("Discord");
        discord.addActionListener(e -> openLink("https://discord.gg/DSpPfC2Ebh"));
        help.add(discord);

        JMenuItem twitter = new JMenuItem("X/Twitter");
        twitter.addActionListener(e -> openLink("https://x.com/ScreteMonge"));
        help.add(twitter);

        JMenuItem github = new JMenuItem("Github");
        github.addActionListener(e -> openLink("https://github.com/ScreteMonge/creators-kit"));
        help.add(github);
    }

    public void openLink(String url)
    {
        try
        {
            LinkBrowser.browse(url);
        }
        catch (Exception exception)
        {
            plugin.sendChatMessage("Failed to open link.");
        }
    }
}
