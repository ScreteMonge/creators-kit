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
    public ToolBoxFrame(Client client, EventBus eventBus, ClientThread clientThread, CreatorsPlugin plugin, CreatorsConfig config, ConfigManager configManager, DataFinder dataFinder, ModelOrganizer modelOrganizer, ModelAnvil modelAnvil, TransmogPanel transmogPanel, PathFinder pathFinder, ModelUtilities modelUtilities, OkHttpClient httpClient, SelectionManager selectionManager, com.creatorskit.programming.ColourController colourController, com.creatorskit.programming.SoundController soundController, com.creatorskit.cache.metadata.CacheMetadataStore cacheMetadataStore)
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
        this.cacheSearcher = new CacheSearcherTab(plugin, clientThread, dataFinder, modelUtilities, httpClient, cacheMetadataStore);
        this.programmer = new Programmer(client, config, clientThread, plugin, timeSheetPanel, dataFinder, modelUtilities, colourController, soundController);

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
            // With an A-B loop set, "reset" means rewind to A (the loop's
            // start), not to tick 0. Mirrors the resetTimelineListener
            // hotkey path in CreatorsPlugin -- both routes share the same
            // semantics so the menu and the hotkey can't drift apart.
            double rewindTo = timeSheetPanel.getALoopTick() != null
                    ? timeSheetPanel.getALoopTick() : 0.0;
            timeSheetPanel.setCurrentTime(rewindTo, false);
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

        // Tools menu -- batch operations on the current multi-selection.
        // v2 layout (alphabetised, max depth 3, root counts as depth 1):
        //   A-B Loop ▶
        //     Clear A-B Loop
        //     Set A marker at current tick
        //     Set B marker at current tick
        //   Hide GameObjects...
        //   Keyframes ▶
        //     Create Block...
        //     Iterate field values...
        //     Repeat selection...
        //     Ripple Delete keyframes...
        //     Ripple Insert...
        //   Layout ▶
        //     Fill Rectangle...
        //     Random Hazard Grid...
        //   Random ▶
        //     Jitter keyframe ticks...
        //     Random Select...
        //     Scatter keyframe ticks...
        //
        // Items are kept in alphabetical order in the source too so the
        // code layout mirrors what the user sees. Lambdas defer access to
        // timeSheetPanel / managerPanel until click time because
        // setupMenuBar runs before those fields are initialised.
        JMenu tools = new JMenu("Tools");
        jMenuBar.add(tools);

        // --- A-B Loop --------------------------------------------------
        // Vertical loop markers on the timeline. Markers also removable via
        // X buttons drawn on the chips themselves (Premiere-style); this
        // submenu is the discoverable / hotkey-friendly entry point.
        // Submenu has no tooltip on purpose -- tooltips only belong on leaf
        // nodes; the relevant per-action context is spread across the three
        // leaf items below.
        JMenu abLoop = new JMenu("A-B Loop");
        tools.add(abLoop);

        // Per STRINGS_STYLE.md: menu items carry NO tooltips. The label is
        // the doc; deeper explanation lives in the dialog the item opens.
        JMenuItem clearAB = new JMenuItem("Clear A-B Loop");
        clearAB.addActionListener(e -> timeSheetPanel.clearABLoop());
        abLoop.add(clearAB);

        JMenuItem setA = new JMenuItem("Set A marker at current tick");
        setA.addActionListener(e -> timeSheetPanel.setALoopTick(timeSheetPanel.getCurrentTime()));
        abLoop.add(setA);

        JMenuItem setB = new JMenuItem("Set B marker at current tick");
        setB.addActionListener(e -> timeSheetPanel.setBLoopTick(timeSheetPanel.getCurrentTime()));
        abLoop.add(setB);

        // --- Hide GameObjects... (single-item, stays at root) ----------
        JMenuItem hideObjects = new JMenuItem("Hide GameObjects...");
        hideObjects.addActionListener(e -> plugin.showHideGameObjectsDialog());
        tools.add(hideObjects);

        // --- Keyframes -------------------------------------------------
        // Timeline-editing ops that mutate keyframes. Pulled off root so
        // Tools doesn't carry ~10 loose items.
        JMenu keyframesMenu = new JMenu("Keyframes");
        tools.add(keyframesMenu);

        // Blocks: Premiere-style nested-clip grouping of keyframes. Greyed
        // out when the marquee selection isn't a valid block on at least
        // one Character (the per-Character no-gaps + no-overlap rules
        // live in BlockValidator). Enable state is recomputed every time
        // the Keyframes submenu is opened so the gate reflects the
        // current marquee without listening for every selection event.
        JMenuItem createBlock = new JMenuItem("Create Block...");
        createBlock.addActionListener(e -> timeSheetPanel.showCreateBlockDialog());
        keyframesMenu.addMenuListener(new javax.swing.event.MenuListener()
        {
            @Override public void menuSelected(javax.swing.event.MenuEvent e)
            {
                createBlock.setEnabled(timeSheetPanel.canCreateBlockFromSelection());
            }
            @Override public void menuDeselected(javax.swing.event.MenuEvent e) {}
            @Override public void menuCanceled(javax.swing.event.MenuEvent e) {}
        });
        keyframesMenu.add(createBlock);

        JMenuItem iterateFields = new JMenuItem("Iterate field values...");
        iterateFields.addActionListener(e -> timeSheetPanel.showIterateFieldDialog());
        keyframesMenu.add(iterateFields);

        JMenuItem repeatSelection = new JMenuItem("Repeat selection...");
        repeatSelection.addActionListener(e -> timeSheetPanel.showRepeatSelectionDialog());
        keyframesMenu.add(repeatSelection);

        JMenuItem rippleDelete = new JMenuItem("Ripple Delete...");
        rippleDelete.addActionListener(e -> timeSheetPanel.showRippleDeleteDialog(0.0, 0.0, null));
        keyframesMenu.add(rippleDelete);

        JMenuItem rippleInsert = new JMenuItem("Ripple Insert...");
        rippleInsert.addActionListener(e -> timeSheetPanel.showRippleInsertDialog());
        keyframesMenu.add(rippleInsert);

        // --- Layout ----------------------------------------------------
        // Spatial arrangement of Characters / stamps. Random Hazard Grid
        // is a layout tool that uses randomness; lives here next to Fill
        // Rectangle rather than under Random (which is for ticks /
        // selection, not space).
        JMenu layout = new JMenu("Layout");
        tools.add(layout);

        JMenuItem fillRect = new JMenuItem("Fill Rectangle...");
        fillRect.addActionListener(e -> plugin.getCreatorsPanel().showFillRectangleDialog());
        layout.add(fillRect);

        JMenuItem hazardGrid = new JMenuItem("Random Hazard Grid...");
        hazardGrid.addActionListener(e -> plugin.getCreatorsPanel().showRandomHazardGridDialog());
        layout.add(hazardGrid);

        // --- Random ----------------------------------------------------
        // Stochastic ops on selection / ticks. Spatial randomness lives
        // under Layout (Random Hazard Grid) so this group stays focused
        // on non-spatial randomness.
        JMenu random = new JMenu("Random");
        tools.add(random);

        JMenuItem jitter = new JMenuItem("Jitter keyframe ticks...");
        jitter.addActionListener(e -> timeSheetPanel.showJitterDialog());
        random.add(jitter);

        JMenuItem randomSelect = new JMenuItem("Random Select...");
        randomSelect.addActionListener(e -> managerPanel.getManagerTree().selectRandomFromActiveFolder());
        random.add(randomSelect);

        JMenuItem scatter = new JMenuItem("Scatter keyframe ticks...");
        scatter.addActionListener(e -> timeSheetPanel.showScatterDialog());
        random.add(scatter);

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

        help.addSeparator();

        JMenuItem debugLogging = new JMenuItem("Debug logging...");
        debugLogging.addActionListener(e -> showDebugLoggingDialog());
        help.add(debugLogging);


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

    /**
     * Help &gt; Debug logging dialog. Mirrors the Debugging section in
     * the RuneLite config sidebar but lives in the toolbox menu so it's
     * discoverable without hunting the sidebar. Reads and writes the
     * same config keys (creatorssuite.debugCharacterName,
     * creatorssuite.debugLogToChat) so both surfaces stay in sync.
     */
    private void showDebugLoggingDialog()
    {
        ConfigManager cm = plugin.getConfigManager();
        String currentName = cm.getConfiguration("creatorssuite", "debugCharacterName");
        if (currentName == null) currentName = "";
        String currentChatStr = cm.getConfiguration("creatorssuite", "debugLogToChat");
        boolean currentChat = "true".equalsIgnoreCase(currentChatStr);

        JTextField nameField = new JTextField(currentName, 20);
        nameField.setToolTipText("Exact name of the Character to log for. Empty = disabled.");
        JCheckBox chatToggle = new JCheckBox("Also log debug lines to in-game chat", currentChat);
        chatToggle.setToolTipText("Default off (logs only go to the RuneLite log file). On = also visible in chat live.");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Character name to log for:"));
        panel.add(nameField);
        panel.add(javax.swing.Box.createVerticalStrut(8));
        panel.add(chatToggle);
        panel.add(javax.swing.Box.createVerticalStrut(8));
        JLabel hint = new JLabel("<html><i>Logs Movement / Orientation kf activations, play-loop arbitration,<br>"
                + "Face Target snaps, and Start-angle snapshots. Output goes to<br>"
                + "the RuneLite log file (and the IntelliJ console when running<br>"
                + "from source).</i></html>");
        hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));
        panel.add(hint);

        int res = JOptionPane.showConfirmDialog(this, panel, "Debug logging",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String newName = nameField.getText() == null ? "" : nameField.getText().trim();
        cm.setConfiguration("creatorssuite", "debugCharacterName", newName);
        cm.setConfiguration("creatorssuite", "debugLogToChat", String.valueOf(chatToggle.isSelected()));
    }
}
