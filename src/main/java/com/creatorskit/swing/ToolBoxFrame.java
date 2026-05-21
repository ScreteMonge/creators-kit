package com.creatorskit.swing;

import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.ModelUtilities;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.programming.PathFinder;
import com.creatorskit.programming.Programmer;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.swing.anvil.ModelAnvil;
import com.creatorskit.swing.manager.Folder;
import com.creatorskit.swing.manager.FolderType;
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
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

@Getter
public class ToolBoxFrame extends JFrame
{
    private ClientThread clientThread;
    private final Client client;
    private final EventBus eventBus;
    private final CreatorsPlugin plugin;
    private final CreatorsConfig config;
    private final ConfigManager configManager;
    private final ModelUtilities modelUtilities;
    private final JMenuBar jMenuBar;
    private final DataFinder dataFinder;
    private final ManagerPanel managerPanel;
    private final ModelOrganizer modelOrganizer;
    private final ModelAnvil modelAnvil;
    private final CacheSearcherTab cacheSearcher;
    private final TransmogPanel transmogPanel;
    private final TimeSheetPanel timeSheetPanel;
    private final Programmer programmer;
    private final PathFinder pathFinder;
    private OkHttpClient httpClient;

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final BufferedImage ICON = ImageUtil.loadImageResource(getClass(), "/panelicon.png");

    @Inject
    public ToolBoxFrame(Client client, EventBus eventBus, ClientThread clientThread, CreatorsPlugin plugin, CreatorsConfig config, ConfigManager configManager, DataFinder dataFinder, ModelOrganizer modelOrganizer, ModelAnvil modelAnvil, TransmogPanel transmogPanel, PathFinder pathFinder, ModelUtilities modelUtilities, OkHttpClient httpClient, SelectionManager selectionManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.config = config;
        this.eventBus = eventBus;
        this.configManager = configManager;
        this.modelUtilities = modelUtilities;
        this.jMenuBar = new JMenuBar();
        this.dataFinder = dataFinder;
        this.modelOrganizer = modelOrganizer;
        this.modelAnvil = modelAnvil;
        this.transmogPanel = transmogPanel;
        this.pathFinder = pathFinder;
        this.httpClient = httpClient;

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
        ManagerTree managerTree = new ManagerTree(this, plugin, selectionManager, objectHolder, managerRootNode, managerSideNode, managerManagerNode);
        MovementManager movementManager = new MovementManager(client, config, pathFinder);

        setupMenuBar();
        this.timeSheetPanel = new TimeSheetPanel(client, this, plugin, config, clientThread, dataFinder, managerTree, movementManager, selectionManager);
        this.managerPanel = new ManagerPanel(client, plugin, objectHolder, managerTree);
        this.cacheSearcher = new CacheSearcherTab(plugin, clientThread, dataFinder, modelUtilities, httpClient);
        this.programmer = new Programmer(client, config, clientThread, plugin, timeSheetPanel, dataFinder, modelUtilities);

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

        tabbedPane.setFont(FontManager.getRunescapeBoldFont());

        tabbedPane.addTab("Timeline", timeSheetPanel);
        timeSheetPanel.getTreeScrollPane().setViewportView(managerTree);
        tabbedPane.addTab("Manager", managerPanel);
        tabbedPane.addTab("Model Organizer", modelOrganizer);
        tabbedPane.addTab("Model Anvil", modelAnvil);
        tabbedPane.addTab("Cache Searcher", cacheSearcher);
        tabbedPane.addTab("Transmogger", transmogPanel);
        tabbedPane.setToolTipTextAt(0, "Manage and organize all your Objects");
        tabbedPane.setToolTipTextAt(1, "Organize Custom Models you've loaded from the cache or Forged");
        tabbedPane.setToolTipTextAt(2, "Create Custom Models by modifying and merging different models together");
        tabbedPane.setToolTipTextAt(3, "Search the cache for NPCs, Items, and Objects for their models");
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
        revalidate();
        pack();
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

        JMenuItem save = new JMenuItem("Save Setup");
        save.addActionListener(e -> plugin.getCreatorsPanel().quickSaveToFile());
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        file.add(save);

        JMenuItem saveAs = new JMenuItem("Save Setup As...");
        saveAs.addActionListener(e -> plugin.getCreatorsPanel().openSaveDialog());
        file.add(saveAs);

        JMenuItem load = new JMenuItem("Load Setup");
        load.addActionListener(e -> plugin.getCreatorsPanel().openLoadSetupDialog());
        load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        file.add(load);

        // "Load last setup" -- one-click reload of whichever setup the user
        // most recently loaded via the file dialog. Label is regenerated on
        // every menu-open so it stays in sync with whatever the user just
        // loaded ("Load last setup... (foo.json)" or "Load last setup..."
        // disabled when no path has been remembered). Path is persisted in
        // the creatorssuite config group via CreatorsPanel.loadSetup so it
        // survives client restarts.
        JMenuItem loadLast = new JMenuItem("Load last setup...");
        loadLast.addActionListener(e ->
        {
            String path = configManager.getConfiguration("creatorssuite", "lastSetupPath");
            if (path == null || path.isEmpty())
            {
                plugin.sendChatMessage("No setup has been loaded yet this session.");
                return;
            }
            java.io.File f = new java.io.File(path);
            if (!f.exists())
            {
                plugin.sendChatMessage("Last setup file no longer exists: " + path);
                return;
            }
            plugin.getCreatorsPanel().loadSetup(f);
        });
        file.add(loadLast);

        file.addMenuListener(new javax.swing.event.MenuListener()
        {
            @Override
            public void menuSelected(javax.swing.event.MenuEvent e)
            {
                String path = configManager.getConfiguration("creatorssuite", "lastSetupPath");
                if (path == null || path.isEmpty())
                {
                    loadLast.setText("Load last setup...");
                    loadLast.setEnabled(false);
                    return;
                }
                java.io.File f = new java.io.File(path);
                loadLast.setText("Load last setup... (" + f.getName() + ")");
                loadLast.setEnabled(f.exists());
            }

            @Override public void menuDeselected(javax.swing.event.MenuEvent e) {}
            @Override public void menuCanceled(javax.swing.event.MenuEvent e) {}
        });

        JMenu timeSheet = new JMenu("Timeline");
        jMenuBar.add(timeSheet);

        JMenuItem togglePlay = new JMenuItem("Play/Pause");
        togglePlay.addActionListener(e -> programmer.togglePlay());
        togglePlay.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(togglePlay);

        JMenuItem reset = new JMenuItem("Reset Timeline");
        reset.addActionListener(e ->
        {
            programmer.pause();
            timeSheetPanel.setCurrentTime(0, false);
        });
        reset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(reset);

        JMenuItem selectAll = new JMenuItem("Select All");
        selectAll.addActionListener(e ->
        {
            if (tabbedPane.getSelectedComponent() == timeSheetPanel)
            {
                timeSheetPanel.onSelectAllPressed();
            }
        });
        selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(selectAll);

        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(e ->
        {
            if (tabbedPane.getSelectedComponent() == timeSheetPanel)
            {
                timeSheetPanel.onDeleteKeyPressed();
            }
        });
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        timeSheet.add(delete);

        JMenuItem copyKeyFrames = new JMenuItem("Copy");
        copyKeyFrames.addActionListener(e ->
        {
            if (tabbedPane.getSelectedComponent() == timeSheetPanel)
            {
                timeSheetPanel.copyKeyFrames();
            }
        });
        copyKeyFrames.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(copyKeyFrames);

        JMenuItem pasteKeyFrames = new JMenuItem("Paste");
        pasteKeyFrames.addActionListener(e ->
        {
            if (tabbedPane.getSelectedComponent() == timeSheetPanel)
            {
                timeSheetPanel.pasteKeyFrames();
            }
        });
        pasteKeyFrames.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(pasteKeyFrames);

        JMenuItem undo = new JMenuItem("Undo");
        undo.addActionListener(e ->
        {
            if (tabbedPane.getSelectedComponent() == timeSheetPanel)
            {
                timeSheetPanel.undo();
            }
        });
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(undo);

        JMenuItem redo = new JMenuItem("Redo");
        redo.addActionListener(e ->
        {
            if (tabbedPane.getSelectedComponent() == timeSheetPanel)
            {
                timeSheetPanel.redo();
            }
        });
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(redo);

        JMenuItem skipRight = new JMenuItem("Next KeyFrame");
        skipRight.addActionListener(e -> timeSheetPanel.onAttributeSkipForward());
        skipRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(skipRight);

        JMenuItem skipLeft = new JMenuItem("Last KeyFrame");
        skipLeft.addActionListener(e -> timeSheetPanel.onAttributeSkipPrevious());
        skipLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK));
        timeSheet.add(skipLeft);

        // Tools menu -- batch operations on the current multi-selection. Grouped by
        // submenu (Random for now) so this can grow without crowding the top bar.
        // Lambdas defer access to timeSheetPanel / managerPanel until click time
        // because setupMenuBar runs before those fields are initialised.
        JMenu tools = new JMenu("Tools");
        jMenuBar.add(tools);

        JMenu random = new JMenu("Random");
        tools.add(random);

        JMenuItem randomSelect = new JMenuItem("Random Select...");
        randomSelect.setToolTipText("<html>Replace selection with N randomly-picked direct children of<br>"
                + "the currently-selected folder. Click a folder in the Manager Tree first.</html>");
        randomSelect.addActionListener(e -> managerPanel.getManagerTree().selectRandomFromActiveFolder());
        random.add(randomSelect);

        JMenuItem jitter = new JMenuItem("Jitter keyframe ticks...");
        jitter.setToolTipText("<html>For every selected Character, shift each keyframe of the chosen<br>"
                + "type by a uniform random delta in [-max, +max]. Useful for de-syncing<br>"
                + "identical setups -- e.g. rain raindrops sharing the same spawn tick.</html>");
        jitter.addActionListener(e -> timeSheetPanel.showJitterDialog());
        random.add(jitter);

        JMenuItem scatter = new JMenuItem("Scatter keyframe ticks...");
        scatter.setToolTipText("<html>For every selected Character, SET each keyframe of the chosen<br>"
                + "type to a uniform random tick in [from, to]. Useful for distributing<br>"
                + "events across a time window without anchoring to existing ticks.</html>");
        scatter.addActionListener(e -> timeSheetPanel.showScatterDialog());
        random.add(scatter);

        // Layout submenu -- deterministic spatial arrangement of Characters
        // (sibling to Random's stochastic timing tools). First entry is Fill
        // Rectangle; future entries (Fill Line, Mirror, Array, Distribute) will
        // share this umbrella.
        JMenu layout = new JMenu("Layout");
        tools.add(layout);

        JMenuItem fillRect = new JMenuItem("Fill Rectangle...");
        fillRect.setToolTipText("<html>With exactly 2 Characters selected as opposite corners, fill the<br>"
                + "rectangle between them with duplicates of those Characters on every<br>"
                + "tile (alternating checkerboard by tile parity). Stride spinner lets<br>"
                + "you skip tiles for sparse fills. Duplicates land in a new folder so<br>"
                + "you can collapse / select / delete them as a group.</html>");
        fillRect.addActionListener(e -> plugin.getCreatorsPanel().showFillRectangleDialog());
        layout.add(fillRect);

        // Ripple Delete lives directly on Tools (not under Random / Layout) --
        // it's a destructive timeline editor, not a stochastic / spatial tool.
        // Same op is also exposed as a right-click context menu on empty
        // timeline space (Premiere Pro-style); this menu entry is the
        // discoverable / scope-picking entry point.
        JMenuItem rippleDelete = new JMenuItem("Ripple Delete keyframes...");
        rippleDelete.setToolTipText("<html>Remove every keyframe in [from, to] for the chosen scope, then<br>"
                + "shift everything after the deleted span back by (to - from + 1)<br>"
                + "so the gap collapses. Scope picks one property or all. Targets<br>"
                + "are the multi-selected Characters if any, else the primary.<br>"
                + "Globals (Camera / Fade / Shake) can be targeted by picking<br>"
                + "their name as the scope -- no Character needed.</html>");
        rippleDelete.addActionListener(e -> timeSheetPanel.showRippleDeleteDialog(0.0, 0.0, null));
        tools.add(rippleDelete);

        JMenu resources = new JMenu("Resources");
        jMenuBar.add(resources);

        JMenuItem wikiDB = new JMenuItem("Minimal OSRS Database");
        wikiDB.addActionListener(e -> openLink("https://chisel.weirdgloop.org/moid/index.html"));
        resources.add(wikiDB);

        JMenuItem runeMonk = new JMenuItem("RuneMonk");
        runeMonk.addActionListener(e -> openLink("https://runemonk.com/tools/entityviewer-beta/"));
        resources.add(runeMonk);


        JMenu help = new JMenu("Help");
        jMenuBar.add(help);

        JMenuItem youtube = new JMenuItem("Youtube Tutorial");
        youtube.addActionListener(e -> openLink("https://www.youtube.com/playlist?list=PL5-mTiHdZKNgcEbhEdadHzX-F4VNE0G9O"));
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


        JMenu donate = new JMenu("Donate");
        jMenuBar.add(donate);

        JMenuItem patreon = new JMenuItem("Patreon");
        patreon.addActionListener(e -> openLink("https://www.patreon.com/ScreteMonge"));
        donate.add(patreon);
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
