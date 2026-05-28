package com.creatorskit.swing.timesheet;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.datatypes.PlayerAnimationType;
import com.creatorskit.models.datatypes.SpotanimData;
import com.creatorskit.models.datatypes.WeaponAnimData;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.programming.Programmer;
import com.creatorskit.programming.orientation.Orientation;
import com.creatorskit.programming.orientation.OrientationGoal;
import com.creatorskit.programming.orientation.OrientationHotkeyMode;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.Folder;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.manager.TreeScrollPane;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterActionType;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameActionType;
import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import com.creatorskit.swing.timesheet.sheets.AttributeSheet;
import com.creatorskit.swing.timesheet.sheets.SummarySheet;
import com.creatorskit.swing.timesheet.sheets.TimeSheet;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;

@Getter
@Setter
public class TimeSheetPanel extends JPanel
{
    private Client client;
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final CreatorsConfig config;

    private final GridBagConstraints c = new GridBagConstraints();
    private final ToolBoxFrame toolBox;
    private final DataFinder dataFinder;
    private SummarySheet summarySheet;
    private AttributeSheet attributeSheet;
    /** Dedicated sheet for Camera / Fade / Shake. Toggle button swaps it in/out of view. */
    private com.creatorskit.swing.timesheet.sheets.GlobalAttributeSheet globalAttributeSheet;
    private TreeScrollPane treeScrollPane;
    private final ManagerTree managerTree;
    private MovementManager movementManager;

    private final JComboBox<KeyFrameType> summaryComboBox = new JComboBox<>();
    private final JSpinner timeSpinner = new JSpinner();
    private boolean triggerTimeSpinnerChange = true;
    private JScrollBar scrollBar;
    private AttributePanel attributePanel;
    private final JScrollPane labelScrollPane = new JScrollPane();
    /** Scroll pane wrapping the global-view label column (3 type labels). */
    private final JScrollPane globalLabelScrollPane = new JScrollPane();
    /** CardLayout container at gridx 1 that swaps between the two label panels. */
    private final JPanel labelCards = new JPanel(new CardLayout());
    /** CardLayout container at gridx 2 that swaps between the two attribute sheets. */
    private final JPanel sheetCards = new JPanel(new CardLayout());
    private static final String VIEW_LOCAL = "local";
    private static final String VIEW_GLOBAL = "global";
    private final JPanel controlPanel = new JPanel();
    private final JButton playButton = new JButton();

    private final ImageIcon PLAY = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Play.png"));
    private final ImageIcon STOP = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Stop.png"));
    private final ImageIcon PAUSE = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Pause.png"));
    private final BufferedImage SKIP_LEFT = ImageUtil.loadImageResource(getClass(), "/Skip_Left.png");
    private final BufferedImage SKIP_RIGHT = ImageUtil.loadImageResource(getClass(), "/Skip_Right.png");

    public static final int ABSOLUTE_MAX_SEQUENCE_LENGTH = 100000;
    private static final int DEFAULT_MIN_H_SCROLL = -10;
    private static final int DEFAULT_MAX_H_SCROLL = 200;
    private static final int ZOOM_MAX = 500;
    private static final int ZOOM_MIN = 5;

    private final String LABEL_OFFSET = "  ";
    private JLabel[] labels = new JLabel[0]; // Local-view labels: labels[0] = spacer, labels[1..N] = visible rows in display order
    private JLabel[] globalLabels = new JLabel[0]; // Global-view labels: globalLabels[0] = spacer, globalLabels[1..3] = type rows

    /**
     * Layout for the local view's row order + collapse state. v1 hosts a
     * single group ("Hitsplats" -> HITSPLAT_1..4). Mutated by chevron
     * clicks in the label column; AttributeSheet.displayRowIndex and
     * AttributePanel's label-highlight lookup both read from here.
     */
    @Getter
    private final com.creatorskit.swing.timesheet.sheets.TimelineLocalRowLayout localRowLayout =
            new com.creatorskit.swing.timesheet.sheets.TimelineLocalRowLayout();
    /**
     * Flat row layout for the global view -- no groups yet, just the
     * hidden-types set the Filters dialog mutates.
     */
    @Getter
    private final com.creatorskit.swing.timesheet.sheets.TimelineGlobalRowLayout globalRowLayout =
            new com.creatorskit.swing.timesheet.sheets.TimelineGlobalRowLayout();
    private static final String CONFIG_KEY_COLLAPSED_GROUPS = "collapsedTimelineGroups";

    private double zoom = 50;
    private double hScroll = 0;
    private double maxHScroll = 200;
    private double minHScroll = -10;

    private double currentTime = 0;
    private boolean pauseScrollBarListener = false;
    private Character selectedCharacter;
    private final com.creatorskit.selection.SelectionManager selectionManager;
    private final com.creatorskit.cache.metadata.CacheMetadataStore cacheMetadataStore;

    private ArrayList<KeyFrameAction> keyFrameStack = new ArrayList<>();
    private KeyFrame[] selectedKeyFrames = new KeyFrame[0];
    private KeyFrame[] copiedKeyFrames = new KeyFrame[0];
    private KeyFrameAction[][] keyFrameActions = new KeyFrameAction[0][];

    /**
     * When true the attribute sheet hides every non-global row and the label
     * column collapses to just the 3 global rows (Camera / Fade / Shake).
     * Flipped automatically by {@link #setSelectedKeyFrames(KeyFrame[])} based
     * on whether any keyframes are selected -- the UX intent is "if you can't
     * mutate per-character data right now, don't bother rendering it."
     */
    // globalRowsOnlyMode / labelPanelRef were removed when the row-collapse
    // logic was replaced with two dedicated TimeSheet instances + a CardLayout.
    /**
     * User's manual choice of which view to render when at least one Object is
     * selected. {@code true} = pin to the 3-row global view; {@code false} =
     * show the full per-Character row list. Driven by the toggle button that
     * sits where the spacer header used to be in the labels column. Falls
     * back to "global" whenever the selection drops to zero Objects -- the
     * button itself disables in that case but the rendered mode follows.
     */
    private boolean userPrefersGlobalView = false;
    /** Toggle button shown above the local label column. */
    private JButton viewToggleButton;
    /** Mirror toggle button shown above the global label column (same handler). */
    private JButton globalViewToggleButton;

    private final int UNDO_LIMIT = 15;
    private int undoStack = 0;

    /**
     * Guard against re-entrant hitsplat -> bar sync inside
     * {@link #addKeyFrameActions(KeyFrameAction[])}. The sync mutates
     * Characters via addKeyFrame and folds new actions into the same undo
     * batch; without the guard those new actions would trigger another sync
     * pass and recurse forever.
     */
    private boolean inHitsplatSyncRecursionGuard = false;

    /**
     * A-B loop markers. When {@link #bLoopTick} is non-null, playback loops:
     * crossing B holds the timeline at B for one game tick (avoids the load
     * spike from immediately re-seeding everything) then jumps to A. If A is
     * null it defaults to tick 0 -- the user can set just B and get a "loop
     * from start" experience. B may never be < 0 (enforced in
     * {@link #setBLoopTick}). Both null = no loop, normal playback.
     *
     * <p>State lives here on the panel (not the Programmer) because the
     * timeline body renders the markers and the right-click + Tools menu
     * mutates them; the Programmer just reads bLoopTick / aLoopTick during
     * play.
     */
    // @Setter(NONE) here disables the class-level @Setter for these fields
    // so the validating wrappers below (setALoopTick / setBLoopTick / clearABLoop)
    // are the only mutation path. Anyone trying to bypass would still have to
    // write to the field directly.
    @Getter @lombok.Setter(lombok.AccessLevel.NONE) private Double aLoopTick = null;
    @Getter @lombok.Setter(lombok.AccessLevel.NONE) private Double bLoopTick = null;

    /**
     * Replaces the A marker tick. Pass {@code null} to remove the marker. A
     * has no >= 0 constraint (the spec only constrains B), but it must not
     * exceed B if B is currently set -- otherwise the loop window is
     * negative-width. We clamp A to B in that case rather than reject, since
     * "set A here" feels more responsive than a silent no-op.
     */
    public void setALoopTick(Double tick)
    {
        if (tick != null && bLoopTick != null && tick > bLoopTick)
        {
            tick = bLoopTick;
        }
        this.aLoopTick = tick;
        repaint();
    }

    /**
     * Replaces the B marker tick. Pass {@code null} to remove the marker.
     * B may never be < 0 (clamped to 0). If A is set, B must be >= A
     * (clamped to A) so the loop window stays non-negative.
     */
    public void setBLoopTick(Double tick)
    {
        if (tick != null && tick < 0)
        {
            tick = 0.0;
        }
        if (tick != null && aLoopTick != null && tick < aLoopTick)
        {
            tick = aLoopTick;
        }
        this.bLoopTick = tick;
        repaint();
    }

    /** Removes both markers; playback returns to non-looping. */
    public void clearABLoop()
    {
        this.aLoopTick = null;
        this.bLoopTick = null;
        repaint();
    }

    /** True when both A and B markers are set (so a between-A-B select can run). */
    public boolean canSelectBetweenAB()
    {
        return aLoopTick != null && bLoopTick != null;
    }

    /**
     * Selects every keyframe on every Character in the current selection
     * (or the primary Character if only one is selected) whose tick falls
     * inside the closed [A, B] interval. No-op when either marker is
     * unset. Used by the empty-space right-click menu in the timeline.
     *
     * <p>Honors {@link #resolveSelectionTargets()} so multi-Character
     * selection grabs from every selected Character, single selection
     * grabs from the primary only.
     *
     * <p>Globals (Camera / Screen Fade / Screen Shake) are intentionally
     * excluded -- those live in a central store and have their own
     * dedicated global view. This action is for per-Character work.
     */
    public void selectAllKeyFramesBetweenAB()
    {
        if (!canSelectBetweenAB()) return;
        double a = aLoopTick;
        double b = bLoopTick;
        java.util.Collection<Character> targets = resolveSelectionTargets();
        if (targets.isEmpty()) return;

        java.util.List<KeyFrame> collected = new java.util.ArrayList<>();
        for (Character c : targets)
        {
            KeyFrame[][] frames = c.getFrames();
            if (frames == null) continue;
            for (int typeIdx = 0; typeIdx < frames.length; typeIdx++)
            {
                KeyFrame[] row = frames[typeIdx];
                if (row == null) continue;
                KeyFrameType type = KeyFrameType.getKeyFrameType(typeIdx);
                // Globals live in the central store; skip them here so the
                // per-Character frames matrix doesn't accidentally
                // double-include them on saves that pre-date the Phase-2
                // global refactor (those still carry copies on Character).
                if (KeyFrameType.isGlobal(type)) continue;
                for (KeyFrame kf : row)
                {
                    if (kf == null) continue;
                    double t = kf.getTick();
                    if (t >= a && t <= b) collected.add(kf);
                }
            }
        }
        setSelectedKeyFrames(collected.toArray(new KeyFrame[0]));
    }

    /**
     * Bulk-toggle every keyframe of {@code type} into / out of the marquee
     * selection. Triggered by CTRL+click on a property label; mirrors the
     * manager-tree CTRL multi-select model (clicking an item that's already
     * selected deselects it; clicking an unselected item adds it).
     *
     * <p>Source set:
     *   <li>Local types -- every keyframe of {@code type} on every Character
     *       in {@link #resolveSelectionTargets()}. Honors multi-select.
     *   <li>Global types (Camera / Fade / Shake) -- every entry in the
     *       central {@link com.creatorskit.saves.GlobalKeyFrames} store for
     *       that type. No Character needed.
     *
     * <p>Toggle decision is "all-or-nothing": if every keyframe in the source
     * set is currently selected, the call removes them; otherwise the call
     * adds the missing ones. This matches a user mental model where the
     * property label is a single "group" being toggled, even though under
     * the hood it expands to many keyframes.
     */
    public void toggleSelectAllOfProperty(KeyFrameType type)
    {
        if (type == null) return;

        java.util.ArrayList<KeyFrame> matching = new java.util.ArrayList<>();
        if (isGlobalType(type))
        {
            com.creatorskit.saves.GlobalKeyFrames store = plugin.getGlobalKeyFrames();
            if (store != null)
            {
                KeyFrame[] arr = store.getGlobalKeyFramesByType(type);
                for (KeyFrame kf : arr) if (kf != null) matching.add(kf);
            }
        }
        else
        {
            java.util.Collection<Character> targets = resolveSelectionTargets();
            int idx = KeyFrameType.getIndex(type);
            for (Character c : targets)
            {
                KeyFrame[][] frames = c.getFrames();
                if (frames == null || idx >= frames.length) continue;
                KeyFrame[] row = frames[idx];
                if (row == null) continue;
                for (KeyFrame kf : row) if (kf != null) matching.add(kf);
            }
        }

        if (matching.isEmpty()) return;

        java.util.HashSet<KeyFrame> currentSet = new java.util.HashSet<>(java.util.Arrays.asList(selectedKeyFrames));
        boolean allAlreadyInSelection = true;
        for (KeyFrame kf : matching)
        {
            if (!currentSet.contains(kf)) { allAlreadyInSelection = false; break; }
        }

        if (allAlreadyInSelection)
        {
            java.util.HashSet<KeyFrame> toRemove = new java.util.HashSet<>(matching);
            java.util.ArrayList<KeyFrame> filtered = new java.util.ArrayList<>();
            for (KeyFrame kf : selectedKeyFrames)
            {
                if (!toRemove.contains(kf)) filtered.add(kf);
            }
            setSelectedKeyFrames(filtered.toArray(new KeyFrame[0]));
        }
        else
        {
            currentSet.addAll(matching);
            setSelectedKeyFrames(currentSet.toArray(new KeyFrame[0]));
        }
    }

    @Inject
    public TimeSheetPanel(@Nullable Client client, ToolBoxFrame toolBox, CreatorsPlugin plugin, CreatorsConfig config, ClientThread clientThread, DataFinder dataFinder, ManagerTree managerTree, MovementManager movementManager, com.creatorskit.selection.SelectionManager selectionManager, com.creatorskit.cache.metadata.CacheMetadataStore cacheMetadataStore)
    {
        this.client = client;
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.config = config;
        this.clientThread = clientThread;
        this.dataFinder = dataFinder;
        this.managerTree = managerTree;
        this.movementManager = movementManager;
        this.selectionManager = selectionManager;
        this.cacheMetadataStore = cacheMetadataStore;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        setupTreeScrollPane();
        setupControlPanel();
        setupAttributePanel();
        setupAttributeSheet();
        setupScrollBar();
        setupTimeTreeListener();
        setupManager();
        setKeyBindings();
        setMouseListeners();
    }

    public void onAttributeSkipForward()
    {
        // Fall back to the global stores when no Object is selected so the
        // skip-next button still finds Camera / Fade / Shake keyframes.
        KeyFrame keyFrame = selectedCharacter == null
                ? findNextAnyGlobalKeyFrame(currentTime)
                : selectedCharacter.findNextKeyFrame(currentTime);
        if (keyFrame == null)
        {
            return;
        }

        setCurrentTime(keyFrame.getTick(), false);
    }

    public void onAttributeSkipPrevious()
    {
        KeyFrame keyFrame = selectedCharacter == null
                ? findPreviousAnyGlobalKeyFrame(currentTime)
                : selectedCharacter.findPreviousKeyFrame(currentTime);
        if (keyFrame == null)
        {
            return;
        }

        setCurrentTime(keyFrame.getTick(), false);
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
        hScroll = round(hScroll - change * (x / source.getWidth()));
        if (hScroll < -ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            hScroll = -ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        if (hScroll > ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            hScroll = ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        boundSliderMinMax();
        updateScrollBar();
        updateSheets();
    }

    public void onHorizontalScrollEvent(double amount)
    {
        hScroll = round(hScroll - amount * zoom / 50);
        if (hScroll < -ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            hScroll = -ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        if (hScroll > ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            hScroll = ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        boundSliderMinMax();
        updateScrollBar();
        updateSheets();
    }

    private void boundSliderMinMax()
    {
        if (-hScroll < minHScroll)
        {
            minHScroll = -hScroll;
        }

        if (-hScroll > minHScroll)
        {
            KeyFrame firstKeyFrame = getFirstKeyFrame();
            if (firstKeyFrame == null)
            {
                minHScroll = -hScroll;
            }

            if (firstKeyFrame != null)
            {
                double firstTick = firstKeyFrame.getTick();
                minHScroll = Math.min(-hScroll, firstTick);
            }

            if (minHScroll > DEFAULT_MIN_H_SCROLL)
            {
                minHScroll = DEFAULT_MIN_H_SCROLL;
            }
        }

        double maxVisibleValue = round(zoom - hScroll);
        if (maxVisibleValue > maxHScroll)
        {
            maxHScroll = maxVisibleValue;
        }

        if (maxVisibleValue < maxHScroll)
        {
            KeyFrame lastKeyFrame = getLastKeyFrame();
            if (lastKeyFrame == null)
            {
                maxHScroll = maxVisibleValue;
            }

            if (lastKeyFrame != null)
            {
                double lastTick = lastKeyFrame.getTick();
                maxHScroll = Math.max(maxVisibleValue, lastTick);
            }

            if (maxHScroll < DEFAULT_MAX_H_SCROLL)
            {
                maxHScroll = DEFAULT_MAX_H_SCROLL;
            }
        }
    }

    private KeyFrame getLastKeyFrame()
    {
        ArrayList<Character> characters = plugin.getCharacters();
        if (characters.isEmpty())
        {
            return null;
        }

        KeyFrame lastKeyFrame = null;
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            KeyFrame comparison = character.findLastKeyFrame();
            if (lastKeyFrame == null)
            {
                if (comparison != null)
                {
                    lastKeyFrame = comparison;
                    continue;
                }
            }

            if (comparison == null)
            {
                continue;
            }

            if (comparison.getTick() > lastKeyFrame.getTick())
            {
                lastKeyFrame = comparison;
            }
        }

        return lastKeyFrame;
    }

    private KeyFrame getFirstKeyFrame()
    {
        ArrayList<Character> characters = plugin.getCharacters();
        if (characters.isEmpty())
        {
            return null;
        }

        KeyFrame firstKeyFrame = null;
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            KeyFrame comparison = character.findFirstKeyFrame();
            if (firstKeyFrame == null)
            {
                if (comparison != null)
                {
                    firstKeyFrame = comparison;
                    continue;
                }
            }

            if (comparison == null)
            {
                continue;
            }

            if (comparison.getTick() < firstKeyFrame.getTick())
            {
                firstKeyFrame = comparison;
            }
        }

        return firstKeyFrame;
    }

    public void onKeyFrameIconPressedEvent()
    {
        onKeyFrameIconPressedEvent(currentTime, attributePanel.getSelectedKeyFramePage());
    }

    /**
     * Add-only handler for the "+" button. Always adds (or replaces) a
     * keyframe at the current tick; never removes. Removal is on Delete only
     * so the user can't accidentally wipe a keyframe by double-clicking the
     * add button.
     */
    public void onAddKeyFrameButtonPressed()
    {
        onAddKeyFrameButtonPressed(currentTime, attributePanel.getSelectedKeyFramePage());
    }

    public void onAddKeyFrameButtonPressed(double currentTick, KeyFrameType type)
    {
        boolean globalType = isGlobalType(type);

        // No-Character + global branch: write directly to the central store.
        if (selectedCharacter == null)
        {
            if (!globalType) return;
            if (type == KeyFrameType.CAMERA)
            {
                attributePanel.captureLiveCameraIntoSpinners();
            }
            KeyFrame kf = attributePanel.createKeyFrame(type, currentTick);
            if (kf == null) return;
            KeyFrame replaced = plugin.getGlobalKeyFrames().add(kf);
            KeyFrameAction[] kfa = new KeyFrameAction[]{
                    new KeyFrameCharacterAction(kf, null, KeyFrameCharacterActionType.ADD)
            };
            if (replaced != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replaced, null, KeyFrameCharacterActionType.REMOVE));
            }
            addKeyFrameActions(kfa);
            setSelectedKeyFrames(new KeyFrame[]{kf});
            attributePanel.setKeyFramedIcon(true);
            return;
        }

        // With-Character branch: same path as the legacy add side of
        // onKeyFrameIconPressedEvent, minus the toggle-remove branch.
        if (type == KeyFrameType.CAMERA)
        {
            attributePanel.captureLiveCameraIntoSpinners();
        }
        KeyFrame kf = attributePanel.createKeyFrame(type, currentTick);
        if (kf == null) return;

        KeyFrame[] keyFrames = new KeyFrame[]{kf};
        if (type == KeyFrameType.SPAWN && currentTick > 0)
        {
            keyFrames = checkDespawnKeyFrameAt0(kf, keyFrames, currentTick);
        }
        addKeyFrameAction(keyFrames);
    }

    public void onKeyFrameIconPressedEvent(double currentTick, KeyFrameType type)
    {
        // No-Character branch for global types -- the user can author
        // Camera/Fade/Shake keyframes before any Object exists. Goes through
        // the GlobalKeyFrames central store directly and queues an undoable
        // action with a null Character ref (addKeyFrame/removeKeyFrame route
        // null-Character globals to the store).
        if (selectedCharacter == null)
        {
            if (!isGlobalType(type))
            {
                return;
            }
            KeyFrame existing = findGlobalKeyFrameAt(type, currentTick);
            if (existing == null)
            {
                if (type == KeyFrameType.CAMERA)
                {
                    attributePanel.captureLiveCameraIntoSpinners();
                }
                KeyFrame kf = attributePanel.createKeyFrame(type, currentTick);
                if (kf == null) return;
                KeyFrame replaced = plugin.getGlobalKeyFrames().add(kf);
                KeyFrameAction[] kfa = new KeyFrameAction[]{
                        new KeyFrameCharacterAction(kf, null, KeyFrameCharacterActionType.ADD)
                };
                if (replaced != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replaced, null, KeyFrameCharacterActionType.REMOVE));
                }
                addKeyFrameActions(kfa);
                // Auto-select the new keyframe so the card opens for editing
                // immediately -- otherwise the no-Character branch leaves the
                // panel in NO_SELECTION_CARD until the user clicks the icon.
                setSelectedKeyFrames(new KeyFrame[]{kf});
                attributePanel.setKeyFramedIcon(true);
                return;
            }
            plugin.getGlobalKeyFrames().remove(existing);
            addKeyFrameActions(new KeyFrameAction[]{
                    new KeyFrameCharacterAction(existing, null, KeyFrameCharacterActionType.REMOVE)
            });
            // Drop the deleted kf from the marquee so the card collapses back
            // to the placeholder instead of editing a now-orphan reference.
            setSelectedKeyFrames(new KeyFrame[0]);
            return;
        }

        KeyFrame keyFrame = selectedCharacter.findKeyFrame(type, currentTick);
        if (keyFrame == null)
        {
            // For a brand-new CAMERA keyframe, default the spinner values to
            // the live OSRS camera state before createKeyFrame snapshots them.
            // Without this the keyframe is built from the card defaults
            // (0/0/0 focal, 0 pitch/yaw) which would teleport the camera to
            // scene origin on playback -- almost never what the user wants
            // when they hit "+". The Update path is unaffected since it only
            // runs for existing keyframes.
            if (type == KeyFrameType.CAMERA)
            {
                attributePanel.captureLiveCameraIntoSpinners();
            }

            KeyFrame kf = attributePanel.createKeyFrame(type, currentTick);
            if (kf == null)
            {
                return;
            }

            KeyFrame[] keyFrames = new KeyFrame[]{kf};
            if (type == KeyFrameType.SPAWN && currentTick > 0)
            {
                keyFrames = checkDespawnKeyFrameAt0(kf, keyFrames, currentTick);
            }

            addKeyFrameAction(keyFrames);
            return;
        }

        // REMOVE branch: expand across multi-selection. The primary's keyframe was
        // already located above; iterate the rest of the selection and remove each
        // one's keyframe of the same type at the seeker tick. The primary's
        // removal happens through the same loop so the action history records
        // every removal as one batched undo step.
        //
        // Phase 2: globals live in a single central store, so the per-Character
        // findKeyFrame call returns the same instance for every target. Process
        // the primary only and skip the rest -- otherwise we'd file N redundant
        // remove actions for the same global keyframe.
        java.util.Collection<Character> targets = resolveSelectionTargets();
        boolean globalType = isGlobalType(type);
        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (Character c : targets)
        {
            KeyFrame kf = c.findKeyFrame(type, currentTick);
            if (kf == null)
            {
                continue;
            }
            removeKeyFrame(c, kf);
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, c, KeyFrameCharacterActionType.REMOVE));
            if (globalType)
            {
                // One central-store removal is enough -- every other selected
                // Character would see the same (already-removed) keyframe and
                // file a redundant action.
                break;
            }
        }
        addKeyFrameActions(kfa);
    }

    /**
     * Resolves the set of Characters that a keyframe-add / paste / remove action
     * should apply to. Returns every Character in {@link #selectionManager}'s
     * selection when more than one is selected (e.g. after clicking a folder),
     * else falls back to the single {@link #selectedCharacter} for the legacy
     * one-character-at-a-time path.
     */
    private java.util.Collection<Character> resolveSelectionTargets()
    {
        if (selectionManager != null && selectionManager.size() > 1)
        {
            return new java.util.ArrayList<>(selectionManager.getSelected());
        }
        if (selectedCharacter == null)
        {
            return java.util.Collections.emptyList();
        }
        return java.util.Collections.singletonList(selectedCharacter);
    }

    public void addKeyFrameAction(KeyFrame[] keyFrames)
    {
        java.util.Collection<Character> targets = resolveSelectionTargets();

        KeyFrameAction[] kfa = new KeyFrameAction[0];

        // No-Object branch: globals in the incoming batch still need a home.
        // Add them to the central store and record the action history so undo
        // works. Skip non-global types -- they need a Character owner.
        if (targets.isEmpty())
        {
            for (KeyFrame template : keyFrames)
            {
                if (template == null || !isGlobalType(template.getKeyFrameType())) continue;
                KeyFrame replaced = plugin.getGlobalKeyFrames().add(template);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(template, null, KeyFrameCharacterActionType.ADD));
                if (replaced != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replaced, null, KeyFrameCharacterActionType.REMOVE));
                }
            }
            if (kfa.length > 0)
            {
                addKeyFrameActions(kfa);
            }
            return;
        }

        for (Character c : targets)
        {
            // The primary keeps the original KeyFrame instance so callsites that
            // expect identity-preserving behaviour (e.g. paste's selectedKeyFrames
            // re-selection) still see "their" object. Secondary selections get a
            // clone -- two Characters can't share a single state-bearing keyframe
            // (MovementKeyFrame.currentStep, AnimationKeyFrame transient fields,
            // etc.) without their playback corrupting each other.
            boolean isPrimary = (c == selectedCharacter);
            for (KeyFrame template : keyFrames)
            {
                // Phase 2: global keyframes live in a single central store --
                // skip every non-primary Character so we don't end up writing
                // N copies of the same camera/fade/shake at the same tick.
                if (isGlobalType(template.getKeyFrameType()) && !isPrimary)
                {
                    continue;
                }
                KeyFrame perChar = isPrimary ? template : KeyFrame.createCopy(template, template.getTick());
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(perChar, c, KeyFrameCharacterActionType.ADD));

                KeyFrame keyFrameToReplace = addKeyFrame(c, perChar);
                if (keyFrameToReplace != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, c, KeyFrameCharacterActionType.REMOVE));
                }
            }
        }

        // Hitsplat-driven bar resync happens centrally in addKeyFrameActions
        // now, so every code path (this one, onUpdateButtonPressed, drag-
        // release, delete) gets the same treatment. No call needed here.
        addKeyFrameActions(kfa);
    }

    // (syncHealthFromHitsplats removed; replaced by the universal
    // maybeExpandWithHitsplatSync / applyHitsplatSyncAt hook in
    // addKeyFrameActions, which catches every commit path.)

    private static boolean isGlobalType(KeyFrameType type)
    {
        // Delegate to the canonical predicate on KeyFrameType so new global
        // types (e.g. SOUND_1..4) don't drift between the two.
        return KeyFrameType.isGlobal(type);
    }

    /**
     * Returns true when every Character in {@link #resolveSelectionTargets()}
     * already has a keyframe of {@code type} at exactly {@code tick}. For
     * global keyframe types (Camera / Fade / Shake) the check hits the
     * central store instead -- "present at this tick" is the same concept
     * either way. Used by AttributePanel to grey out the "+" add-keyframe
     * button when there's nothing to add.
     */
    public boolean allSelectedTargetsHaveKeyFrame(KeyFrameType type, double tick)
    {
        if (type == null) return false;
        if (isGlobalType(type))
        {
            return findGlobalKeyFrameAt(type, tick) != null;
        }
        java.util.Collection<Character> targets = resolveSelectionTargets();
        if (targets.isEmpty()) return false;
        for (Character c : targets)
        {
            if (c.findKeyFrame(type, tick) == null) return false;
        }
        return true;
    }

    /**
     * Looks up an existing global keyframe of {@code type} at exactly {@code tick}.
     * Used by the no-Character "+" path to decide between add and remove --
     * mirrors {@code Character.findKeyFrame(type, tick)} but reads from the
     * central store directly. Returns null if nothing sits on that tick.
     */
    /**
     * Closest global keyframe of {@code type} at or before {@code tick}, or
     * null if none. Symmetric to Character.findPreviousKeyFrame(type, tick, true)
     * but operates on the central store so the no-Character paths can fall
     * back to a sensible "previous" reference.
     */
    private KeyFrame findPreviousGlobalKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] arr;
        com.creatorskit.saves.GlobalKeyFrames store = plugin.getGlobalKeyFrames();
        switch (type)
        {
            case CAMERA:       arr = store.getCameraKeyFramesSafe(); break;
            case SCREEN_FADE:  arr = store.getScreenFadeKeyFramesSafe(); break;
            case SCREEN_SHAKE: arr = store.getScreenShakeKeyFramesSafe(); break;
            case SOUND_1:
            case SOUND_2:
            case SOUND_3:
            case SOUND_4:      arr = store.getSoundKeyFramesSafe(type); break;
            default:           return null;
        }
        KeyFrame best = null;
        for (KeyFrame kf : arr)
        {
            if (kf == null || kf.getTick() > tick) continue;
            if (best == null || kf.getTick() > best.getTick()) best = kf;
        }
        return best;
    }

    /**
     * Closest global keyframe of {@code type} strictly after {@code tick}, or
     * null if none.
     */
    private KeyFrame findNextGlobalKeyFrame(KeyFrameType type, double tick)
    {
        KeyFrame[] arr;
        com.creatorskit.saves.GlobalKeyFrames store = plugin.getGlobalKeyFrames();
        switch (type)
        {
            case CAMERA:       arr = store.getCameraKeyFramesSafe(); break;
            case SCREEN_FADE:  arr = store.getScreenFadeKeyFramesSafe(); break;
            case SCREEN_SHAKE: arr = store.getScreenShakeKeyFramesSafe(); break;
            case SOUND_1:
            case SOUND_2:
            case SOUND_3:
            case SOUND_4:      arr = store.getSoundKeyFramesSafe(type); break;
            default:           return null;
        }
        KeyFrame best = null;
        for (KeyFrame kf : arr)
        {
            if (kf == null || kf.getTick() <= tick) continue;
            if (best == null || kf.getTick() < best.getTick()) best = kf;
        }
        return best;
    }

    /** Scans all 3 global stores and returns the previous keyframe of ANY global type. */
    private KeyFrame findPreviousAnyGlobalKeyFrame(double tick)
    {
        KeyFrame best = null;
        for (KeyFrameType type : new KeyFrameType[]{
                KeyFrameType.CAMERA, KeyFrameType.SCREEN_FADE, KeyFrameType.SCREEN_SHAKE})
        {
            KeyFrame kf = findPreviousGlobalKeyFrame(type, tick);
            if (kf == null) continue;
            if (best == null || kf.getTick() > best.getTick()) best = kf;
        }
        return best;
    }

    /** Scans all 3 global stores and returns the next keyframe of ANY global type. */
    private KeyFrame findNextAnyGlobalKeyFrame(double tick)
    {
        KeyFrame best = null;
        for (KeyFrameType type : new KeyFrameType[]{
                KeyFrameType.CAMERA, KeyFrameType.SCREEN_FADE, KeyFrameType.SCREEN_SHAKE})
        {
            KeyFrame kf = findNextGlobalKeyFrame(type, tick);
            if (kf == null) continue;
            if (best == null || kf.getTick() < best.getTick()) best = kf;
        }
        return best;
    }

    private KeyFrame findGlobalKeyFrameAt(KeyFrameType type, double tick)
    {
        com.creatorskit.saves.GlobalKeyFrames store = plugin.getGlobalKeyFrames();
        KeyFrame[] arr;
        switch (type)
        {
            case CAMERA:       arr = store.getCameraKeyFramesSafe(); break;
            case SCREEN_FADE:  arr = store.getScreenFadeKeyFramesSafe(); break;
            case SCREEN_SHAKE: arr = store.getScreenShakeKeyFramesSafe(); break;
            case SOUND_1:
            case SOUND_2:
            case SOUND_3:
            case SOUND_4:      arr = store.getSoundKeyFramesSafe(type); break;
            default:           return null;
        }
        for (KeyFrame kf : arr)
        {
            if (kf != null && kf.getTick() == tick) return kf;
        }
        return null;
    }

    public void removeKeyFrameAction(KeyFrame keyFrame)
    {
        removeKeyFrame(selectedCharacter, keyFrame);
        KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(keyFrame, selectedCharacter, KeyFrameCharacterActionType.REMOVE)};
        addKeyFrameActions(kfa);
    }

    public void onUpdateButtonPressed()
    {
        KeyFrameType type = attributePanel.getSelectedKeyFramePage();
        boolean globalType = isGlobalType(type);

        // Step 1: figure out which ticks to apply the update at. Pulled from the
        // marquee (selectedKeyFrames) filtered by the panel's current type, then
        // deduped -- multiple marqueed KFs at the same tick should only trigger
        // one update per (type, tick) pair.
        java.util.LinkedHashSet<Double> ticks = new java.util.LinkedHashSet<>();
        for (KeyFrame kf : selectedKeyFrames)
        {
            if (kf == null || kf.getKeyFrameType() != type)
            {
                continue;
            }
            ticks.add(kf.getTick());
        }

        // Fallback: no marquee match -> previous KF of type at the seeker.
        // For globals we read from the central store so the update still
        // applies even when no Object is selected; for per-Character types
        // we still need a Character to source the previous keyframe from.
        if (ticks.isEmpty())
        {
            KeyFrame fallback = null;
            if (globalType)
            {
                fallback = findPreviousGlobalKeyFrame(type, currentTime);
            }
            else if (selectedCharacter != null)
            {
                fallback = selectedCharacter.findPreviousKeyFrame(type, currentTime, true);
            }
            if (fallback == null)
            {
                return;
            }
            ticks.add(fallback.getTick());
        }

        java.util.Collection<Character> chars = resolveSelectionTargets();

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        // Map old keyframe ref -> new replacement, so we can re-point selectedKeyFrames
        // at the end. Without this the AttributePanel's resetAttributes(...) keeps finding
        // the stale old reference via findSelectedKeyFrameOfCurrentType() and re-renders
        // the card with the pre-edit values — visible as the card "snapping back" right
        // after Update is clicked when the seeker isn't on the edited keyframe.
        java.util.IdentityHashMap<KeyFrame, KeyFrame> replacements = new java.util.IdentityHashMap<>();

        // Step 2: for each unique (type, tick), apply the card values to every
        // selected Character's matching KF. Each Character's MOVEMENT path /
        // plane / poh stays its own (per-character world geometry) so only
        // card-edited fields like speed / turn rate change.
        //
        // Phase 2: global types (Camera / Fade / Shake) live in the central
        // store -- a single update per tick, not per-character (otherwise we'd
        // write the same keyframe N times to the same store). When no Object
        // is selected, the per-Character loop wouldn't run at all and global
        // edits would silently no-op; handle that path explicitly below.
        // Per-property mutation: applyEditsTo returns a new kf that starts as
        // a copy of the original and overlays ONLY the panel fields the user
        // touched since the last selection load. Without this, every commit
        // rebuilt the kf from ALL panel values, so editing Radius on a
        // multi-select would clobber each kf's modelId / customModel /
        // useCustomModel with whatever values were displayed for the first
        // kf when the user clicked into the panel.
        if (globalType && chars.isEmpty())
        {
            for (double tick : ticks)
            {
                KeyFrame oldKf = findGlobalKeyFrameAt(type, tick);
                if (oldKf == null) continue;
                KeyFrame newKf = attributePanel.applyEditsTo(oldKf);
                if (newKf == null) continue;
                plugin.getGlobalKeyFrames().add(newKf); // overwrites at this tick
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(newKf, null, KeyFrameCharacterActionType.ADD));
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(oldKf, null, KeyFrameCharacterActionType.REMOVE));
                replacements.put(oldKf, newKf);
            }
            // Skip the per-Character loop -- everything for this update is already done.
            chars = java.util.Collections.emptyList();
        }
        for (double tick : ticks)
        {
            for (Character owner : chars)
            {
                KeyFrame oldKeyFrame = owner.findKeyFrame(type, tick);
                if (oldKeyFrame == null)
                {
                    continue;
                }

                KeyFrame newKf = attributePanel.applyEditsTo(oldKeyFrame);
                if (newKf == null)
                {
                    continue;
                }

                // MOVEMENT plane / poh / path / currentStep / stepClientTick
                // are not panel-editable; applyEditsTo already carries them
                // from the original. No special handling needed here anymore.

                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(newKf, owner, KeyFrameCharacterActionType.ADD));
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(oldKeyFrame, owner, KeyFrameCharacterActionType.REMOVE));
                addKeyFrame(owner, newKf);
                replacements.put(oldKeyFrame, newKf);

                if (globalType)
                {
                    // Already wrote through to the central store; one pass is enough.
                    break;
                }
            }
        }

        // Hitsplat-driven bar resync is now centralised in addKeyFrameActions
        // so it runs for every commit path (add / edit / drag / delete).
        // No call needed here.
        if (kfa.length > 0)
        {
            addKeyFrameActions(kfa);
        }

        // Rebind any stale selected-keyframe refs to their replacements and trigger one
        // final attribute refresh so the card reflects the just-committed values even
        // when the seeker bar is far from the edited keyframe.
        if (!replacements.isEmpty() && selectedKeyFrames.length > 0)
        {
            KeyFrame[] updated = selectedKeyFrames.clone();
            boolean changed = false;
            for (int i = 0; i < updated.length; i++)
            {
                KeyFrame replacement = replacements.get(updated[i]);
                if (replacement != null)
                {
                    updated[i] = replacement;
                    changed = true;
                }
            }
            if (changed)
            {
                setSelectedKeyFrames(updated);
            }
        }
    }

    /**
     * Locate the Character that owns the given keyframe by scanning the visible
     * Characters' frame arrays (selection-aware). Used by Update so multi-selected
     * keyframes spread across owners can all be edited at once.
     */
    private Character findKeyFrameOwner(KeyFrame keyFrame)
    {
        if (keyFrame == null)
        {
            return null;
        }
        java.util.List<Character> visible = (selectionManager != null && selectionManager.size() > 1)
                ? new ArrayList<>(selectionManager.getSelected())
                : (selectedCharacter != null ? java.util.Collections.singletonList(selectedCharacter) : java.util.Collections.emptyList());
        for (Character c : visible)
        {
            KeyFrame[][] frames = c.getFrames();
            if (frames == null)
            {
                continue;
            }
            for (KeyFrame[] row : frames)
            {
                if (row == null)
                {
                    continue;
                }
                for (KeyFrame kf : row)
                {
                    if (kf == keyFrame)
                    {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    public KeyFrame[] checkDespawnKeyFrameAt0(KeyFrame keyFrame, KeyFrame[] keyframes, double currentTick)
    {
        SpawnKeyFrame skf = (SpawnKeyFrame) keyFrame;

        KeyFrame previousKeyFrame = selectedCharacter.findPreviousKeyFrame(KeyFrameType.SPAWN, currentTick, false);
        if (previousKeyFrame == null)
        {
            SpawnKeyFrame spawn = new SpawnKeyFrame(0, !skf.isSpawnActive());
            keyframes = ArrayUtils.add(keyframes, spawn);
            return keyframes;
        }

        return keyframes;
    }

    public void onOrientationKeyPressed(OrientationHotkeyMode hotkeyMode)
    {
        if (selectedCharacter == null)
        {
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        Tile tile = worldView.getSelectedSceneTile();
        if (tile == null)
        {
            return;
        }

        LocalPoint localPoint = tile.getLocalLocation();
        if (localPoint == null || !localPoint.isInScene())
        {
            return;
        }

        Programmer programmer = toolBox.getProgrammer();

        CKObject ckObject = selectedCharacter.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        KeyFrame okf = selectedCharacter.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
        if (okf == null)
        {
            int orientation = ckObject.getOrientation();

            initializeOrientationKeyFrame(
                    selectedCharacter,
                    hotkeyMode,
                    localPoint,
                    currentTime,
                    orientation,
                    orientation,
                    OrientationGoal.POINT,
                    OrientationKeyFrame.TURN_RATE);
        }
        else
        {
            OrientationKeyFrame keyFrame = (OrientationKeyFrame) okf;
            initializeOrientationKeyFrame(
                    selectedCharacter,
                    hotkeyMode,
                    localPoint,
                    keyFrame.getTick(),
                    keyFrame.getStart(),
                    keyFrame.getEnd(),
                    keyFrame.getGoal(),
                    keyFrame.getTurnRate());
        }

        programmer.register3DChanges(selectedCharacter);
        selectedCharacter.setVisible(true, clientThread);
    }

    public void initializeOrientationKeyFrame(Character character, OrientationHotkeyMode hotkeyMode, LocalPoint localPoint, double tick, int start, int end, OrientationGoal og, double turnRate)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        LocalPoint lp = ckObject.getLocation();
        if (lp == null || !lp.isInScene())
        {
            return;
        }

        int startOrientation = start;
        int endOrientation = end;
        int angle = (int) Orientation.getAngleBetween(lp, localPoint);

        if (hotkeyMode == OrientationHotkeyMode.SET_START)
        {
            startOrientation = angle;
        }
        else
        {
            endOrientation = angle;
        }

        double turnDuration = AttributePanel.calculateOrientationDuration(startOrientation, endOrientation, turnRate);

        OrientationKeyFrame okf = new OrientationKeyFrame(
                tick,
                og,
                startOrientation,
                endOrientation,
                turnDuration,
                turnRate);

        addKeyFrameAction(new KeyFrame[]{okf});
    }

    public void onAddOrientationMenuOptionPressed()
    {
        if (selectedCharacter == null)
        {
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        CKObject ckObject = selectedCharacter.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        int orientation = ckObject.getOrientation();

        OrientationKeyFrame okf = new OrientationKeyFrame(
                currentTime,
                OrientationGoal.POINT,
                orientation,
                orientation,
                1,
                OrientationKeyFrame.TURN_RATE);

        addKeyFrameAction(new KeyFrame[]{okf});
    }

    public void onAddMovementKeyPressed()
    {
        if (selectedCharacter == null)
        {
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        Tile tile = worldView.getSelectedSceneTile();
        if (tile == null)
        {
            return;
        }

        LocalPoint localPoint = tile.getLocalLocation();
        if (localPoint == null || !localPoint.isInScene())
        {
            return;
        }

        onAddMovement(false, localPoint);
    }

    public void onAddMovementMenuOptionPressed()
    {
        if (selectedCharacter == null)
        {
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        CKObject ckObject = selectedCharacter.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        LocalPoint localPoint = ckObject.getLocation();
        if (localPoint == null || !localPoint.isInScene())
        {
            return;
        }

        onAddMovement(true, localPoint);
    }

    public void onAddMovement(boolean newKeyFrame, LocalPoint localPoint)
    {
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        // Refuse to overwrite if a Movement kf already sits at the
        // playhead. Per user spec: "warn the player to move the playhead
        // or clear the keyframe." We could just overwrite (initializeMovementKeyFrame
        // already calls addKeyFrame which returns the displaced kf), but
        // silently replacing the user's authored kf is exactly the kind
        // of data-loss surprise the warn-and-bail flow was designed to
        // prevent. Check is up here before we mutate anything (setInScene,
        // setActive) so a refused add is fully a no-op.
        KeyFrame existing = selectedCharacter == null
                ? null
                : selectedCharacter.findKeyFrame(KeyFrameType.MOVEMENT, currentTime);
        if (existing != null)
        {
            JOptionPane.showMessageDialog(this,
                    "A Movement keyframe already sits at the current playhead tick.\n"
                            + "Move the playhead or clear that keyframe before adding a step.",
                    "Movement keyframe occupied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        selectedCharacter.setInScene(true);
        selectedCharacter.setActive(true, true, true, clientThread);

        Programmer programmer = toolBox.getProgrammer();

        boolean poh = MovementManager.useLocalLocations(worldView);

        int x = localPoint.getSceneX();
        int y = localPoint.getSceneY();
        if (!poh)
        {
            WorldPoint wp = WorldPoint.fromLocalInstance(client, localPoint, worldView.getPlane());
            x = wp.getX();
            y = wp.getY();
        }

        double stepSpeed = plugin.getCurrentStepSpeed();

        // Default movement turn rate is the shared OrientationKeyFrame.TURN_RATE
        // (32 JUnits / client tick). Previously this was multiplied by stepSpeed
        // so a faster movement also turned faster -- removed because the user
        // wanted decoupled defaults: turn rate is its own knob, independent of
        // how many tiles per tick the path walks.
        int defaultMovementTurnRate = OrientationKeyFrame.TURN_RATE;

        // Both the menu-option path (newKeyFrame=true) and the hotkey path
        // (newKeyFrame=false) now place the new kf at the playhead. The
        // previous chained-mode logic placed the new kf at
        // previous.tick + prevDuration which only matched the playhead
        // by accident (the prior add auto-advanced the playhead to that
        // exact tick). A user who scrubbed between hotkey presses would
        // see the new step land at the old chain point instead of where
        // they were looking. Always honour the playhead -- if the user
        // wants to extend a sequence they can move the playhead to its
        // end first, and the source-tile resolution below picks up the
        // Character's position-at-playhead so the path connects cleanly.
        //
        // Source tile resolution order:
        //   1. The live CKObject location. After scrub this reflects the
        //      Character's position AT the playhead tick, so a new step
        //      starting at the playhead naturally chains from where the
        //      Character actually is. Covers both "first step" (initial
        //      spawn point) and "extend sequence" (end of prior path)
        //      transparently because the Programmer's scrub already
        //      handled the bookkeeping.
        //   2. The saved instancedPoint (POH) / nonInstancedPoint (world).
        //      Used when the Character has been added but not yet drawn
        //      in scene; otherwise srcX/srcY fall through to the clicked
        //      tile and the pathfinder returns an empty path, producing
        //      a 0-length keyframe at the clicked tile.
        //   3. The clicked tile itself. Last resort -- the addProgramStep
        //      pathfinder will return a 1-tile path and we'll bail on
        //      the length<=1 check below without placing anything.
        int srcX = x;
        int srcY = y;
        boolean resolvedSrc = false;
        CKObject ckObject = selectedCharacter.getCkObject();
        LocalPoint sourceLp = ckObject == null ? null : ckObject.getLocation();
        if (sourceLp != null && sourceLp.isInScene())
        {
            if (poh)
            {
                srcX = sourceLp.getSceneX();
                srcY = sourceLp.getSceneY();
            }
            else
            {
                WorldPoint sourceWp = WorldPoint.fromLocalInstance(client, sourceLp, worldView.getPlane());
                srcX = sourceWp.getX();
                srcY = sourceWp.getY();
            }
            resolvedSrc = true;
        }
        if (!resolvedSrc)
        {
            if (poh)
            {
                LocalPoint savedLp = selectedCharacter.getInstancedPoint();
                if (savedLp != null)
                {
                    srcX = savedLp.getSceneX();
                    srcY = savedLp.getSceneY();
                    resolvedSrc = true;
                }
            }
            else
            {
                WorldPoint savedWp = selectedCharacter.getNonInstancedPoint();
                if (savedWp != null)
                {
                    srcX = savedWp.getX();
                    srcY = savedWp.getY();
                    resolvedSrc = true;
                }
            }
        }

        MovementKeyFrame seed = new MovementKeyFrame(
                0,
                worldView.getPlane(),
                poh,
                new int[][]{new int[]{srcX, srcY}},
                0,
                0,
                false,
                stepSpeed,
                0);
        int[][] path = movementManager.addProgramStep(seed, worldView, localPoint);

        // Skip the no-movement degenerate case: pathfinder returned no
        // steps (source == destination, or src resolution failed and
        // we fell back to the clicked tile). Don't create a 0-length
        // keyframe; just no-op and let the user try a different tile.
        if (path.length <= 1)
        {
            return;
        }

        initializeMovementKeyFrame(selectedCharacter, currentTime, worldView.getPlane(), poh, path, false, stepSpeed, defaultMovementTurnRate);

        // Auto-advance the seeker to the end of the kf we just placed so
        // consecutive add-step presses chain naturally. Ceil the duration
        // so the seeker lands on an integer tick that matches
        // AttributeSheet's ceil()-rounded movement-bar width -- without
        // this, fractional durations (e.g. 3 tiles at speed 2 = 1.5
        // ticks) put the seeker between integer ticks while the visual
        // bar extends to the next integer, looking like a 0.5-tick
        // overlap for whatever step the user adds next.
        double tilesMoved = Math.max(0, path.length - 1);
        double newDuration = Math.ceil(tilesMoved / Math.max(0.0001, stepSpeed));
        setCurrentTime(currentTime + newDuration, false);

        programmer.register3DChanges(selectedCharacter);
    }

    public void initializeMovementKeyFrame(Character character, double tick, int plane, boolean poh, int[][] path, boolean loop, double speed, int turnRate)
    {
        KeyFrame kf = new MovementKeyFrame(
                tick,
                plane,
                poh,
                path,
                0,
                0,
                loop,
                speed,
                turnRate);

        KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(kf, character, KeyFrameCharacterActionType.ADD)};
        KeyFrame keyFrameToReplace = addKeyFrame(character, kf);

        if (keyFrameToReplace != null)
        {
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, character, KeyFrameCharacterActionType.REMOVE));
        }
        addKeyFrameActions(kfa);
    }

    /**
     * Builds a drain keyframe for the bar type targeted by a hitsplat sprite,
     * at the given tick, against the latest bar keyframe BEFORE that tick on
     * the given Character. Used by the hitsplat -> bar auto-sync path in
     * syncHealthFromHitsplats: the "Quick KeyFrame Hitsplat/Bar" button used
     * to invoke a manual version of this; now every hitsplat add/edit runs
     * through it automatically.
     *
     * <p>Sprite -> bar mapping:
     * <ul>
     *   <li>SHIELD / SHIELD_OTHER / SHIELD_MAX -> drains the Shield bar</li>
     *   <li>POISE  / POISE_OTHER  / POISE_MAX  -> drains the Special bar</li>
     *   <li>everything else -> drains the Health bar</li>
     * </ul>
     *
     * <p>Returns null if the target Character has no prior keyframe of the
     * mapped bar type -- per spec, hitsplats only sync onto bars the user
     * has already opted into.
     */
    private KeyFrame pickBarKeyFrameForHitsplat(Character character, com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite sprite, int damage, double tick)
    {
        switch (sprite)
        {
            case SHIELD:
            case SHIELD_OTHER:
            case SHIELD_MAX:
                return buildShieldDrainKeyFrame(character, damage, tick);
            case POISE:
            case POISE_OTHER:
            case POISE_MAX:
                return buildSpecialDrainKeyFrame(character, damage, tick);
            default:
                return buildHealthDrainKeyFrame(character, damage, tick);
        }
    }

    /**
     * Sources sprite / max / order / width from the latest Health keyframe
     * strictly before {@code tick}; returns a new Health keyframe at
     * {@code tick} with {@code currentHealth} reduced by {@code damage} and
     * a duration set to the REMAINING lifespan of the prior keyframe
     * ({@code prior.tick + prior.duration - tick}). When the hitsplat lands
     * inside the prior's window the new KF picks up exactly where the
     * original would have ended -- a 5-tick Health bar hit at +2 produces a
     * 3-tick follow-up. Falls back to the prior's full duration if the
     * remaining is non-positive (hitsplat past the prior's end), so the new
     * KF stays visible. Returns null if no prior Health keyframe exists
     * (sync gate).
     */
    private KeyFrame buildHealthDrainKeyFrame(Character character, int damage, double tick)
    {
        KeyFrame prev = character.findPreviousKeyFrame(KeyFrameType.HEALTH, tick, false);
        if (!(prev instanceof HealthKeyFrame)) return null;
        HealthKeyFrame healthKF = (HealthKeyFrame) prev;
        if (!healthKF.isSyncHitsplats()) return null; // user opted out on prior bar
        HealthKeyFrame drain = new HealthKeyFrame(
                tick,
                remainingDuration(healthKF.getTick(), healthKF.getDuration(), tick),
                healthKF.getHealthbarSprite(),
                healthKF.getMaxHealth(),
                Math.max(0, healthKF.getCurrentHealth() - damage),
                healthKF.getOrder(),
                healthKF.getWidth());
        // Inherit the toggle so the drain keyframe itself keeps the same
        // opt-in state -- subsequent hitsplats at later ticks chain off
        // this one and need to see the same toggle value.
        drain.setSyncHitsplats(healthKF.isSyncHitsplats());
        // Inherit fade-in/fade-out so the lifecycle bounds the BossHealthOverlay
        // computes from any keyframe in the chain agree on the same values --
        // a hitsplat-driven sync mid-fight mustn't reset / re-fire the fade.
        drain.setFadeInTicks(healthKF.getFadeInTicks());
        drain.setFadeOutTicks(healthKF.getFadeOutTicks());
        return drain;
    }

    private KeyFrame buildShieldDrainKeyFrame(Character character, int damage, double tick)
    {
        KeyFrame prev = character.findPreviousKeyFrame(KeyFrameType.SHIELD, tick, false);
        if (!(prev instanceof ShieldKeyFrame)) return null;
        ShieldKeyFrame shieldKF = (ShieldKeyFrame) prev;
        if (!shieldKF.isSyncHitsplats()) return null;
        ShieldKeyFrame drain = new ShieldKeyFrame(
                tick,
                remainingDuration(shieldKF.getTick(), shieldKF.getDuration(), tick),
                shieldKF.getRgb(),
                shieldKF.getMaxValue(),
                Math.max(0, shieldKF.getCurrentValue() - damage),
                shieldKF.getOrder(),
                shieldKF.getWidth());
        drain.setSyncHitsplats(shieldKF.isSyncHitsplats());
        return drain;
    }

    private KeyFrame buildSpecialDrainKeyFrame(Character character, int damage, double tick)
    {
        KeyFrame prev = character.findPreviousKeyFrame(KeyFrameType.SPECIAL, tick, false);
        if (!(prev instanceof SpecialKeyFrame)) return null;
        SpecialKeyFrame specialKF = (SpecialKeyFrame) prev;
        if (!specialKF.isSyncHitsplats()) return null;
        SpecialKeyFrame drain = new SpecialKeyFrame(
                tick,
                remainingDuration(specialKF.getTick(), specialKF.getDuration(), tick),
                specialKF.getRgb(),
                specialKF.getMaxValue(),
                Math.max(0, specialKF.getCurrentValue() - damage),
                specialKF.getOrder(),
                specialKF.getWidth());
        drain.setSyncHitsplats(specialKF.isSyncHitsplats());
        return drain;
    }

    /**
     * Returns the time remaining in the prior keyframe's lifespan at the
     * moment {@code newTick} fires: {@code priorTick + priorDuration - newTick}.
     * Clamped to a positive value -- if the hitsplat lands past the prior's
     * declared end we fall back to the prior's full duration so the new
     * keyframe stays visible instead of having zero / negative lifespan.
     */
    private static double remainingDuration(double priorTick, double priorDuration, double newTick)
    {
        double remaining = (priorTick + priorDuration) - newTick;
        return remaining > 0 ? remaining : priorDuration;
    }

    public void addAnimationKeyFrameFromCache(WeaponAnimData weaponAnim)
    {
        AnimationKeyFrame keyFrame = new AnimationKeyFrame(
                currentTime,
                false,
                -1,
                0,
                false,
                false,
                WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE),
                WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.WALK),
                WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.RUN),
                WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_180),
                WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_RIGHT),
                WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_LEFT),
                WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE_ROTATE_RIGHT),
                WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE_ROTATE_LEFT));

        addKeyFrameAction(new KeyFrame[]{keyFrame});
    }

    public void addSpotAnimKeyFrameFromCache(SpotanimData spotanimData)
    {
        KeyFrameType type = KeyFrameType.SPOTANIM;
        KeyFrame sp1 = selectedCharacter.findKeyFrame(KeyFrameType.SPOTANIM, currentTime);
        if (sp1 != null)
        {
            KeyFrame sp2 = selectedCharacter.findKeyFrame(KeyFrameType.SPOTANIM2, currentTime);
            if (sp2 == null)
            {
                type = KeyFrameType.SPOTANIM2;
            }
        }

        SpotAnimKeyFrame keyFrame = new SpotAnimKeyFrame(
                plugin.getCurrentTick(),
                type,
                spotanimData.getId(),
                false,
                92,
                60);

        addKeyFrameAction(new KeyFrame[]{keyFrame});
    }

    /**
     * Nudges the origin (Start X / Start Y / Start Height) of every
     * targetable projectile keyframe by ({@code dx}, {@code dy},
     * {@code dz}). Active only when the AttributePanel is currently
     * showing the Projectile card -- otherwise returns false so the
     * caller falls back to the regular per-Character nudge.
     *
     * <p>Targets resolve via the same rule as edit commits:
     * marquee-selected projectile kfs win; if none, the seeker
     * fallback picks the previous projectile kf at the playhead on the
     * primary Character. The destination tile is untouched -- nudging
     * only moves the spawn point so the user can align e.g. an arrow's
     * origin with a bow held in the firing model's hand.
     *
     * @return true when projectile mode was active (regardless of
     *   whether a kf was actually mutated), so the caller skips its
     *   non-projectile fallback.
     */
    public boolean tryNudgeProjectileOrigin(int dx, int dy, int dz)
    {
        if (attributePanel == null) return false;
        if (attributePanel.getSelectedKeyFramePage() != KeyFrameType.PROJECTILE) return false;

        java.util.List<com.creatorskit.swing.timesheet.keyframe.ProjectileKeyFrame> targets = new java.util.ArrayList<>();
        for (KeyFrame kf : selectedKeyFrames)
        {
            if (kf instanceof com.creatorskit.swing.timesheet.keyframe.ProjectileKeyFrame)
            {
                targets.add((com.creatorskit.swing.timesheet.keyframe.ProjectileKeyFrame) kf);
            }
        }
        // Seeker fallback: the kf the panel is currently showing on the
        // primary Character.
        if (targets.isEmpty() && selectedCharacter != null)
        {
            KeyFrame fb = selectedCharacter.findPreviousKeyFrame(KeyFrameType.PROJECTILE, currentTime, true);
            if (fb instanceof com.creatorskit.swing.timesheet.keyframe.ProjectileKeyFrame)
            {
                targets.add((com.creatorskit.swing.timesheet.keyframe.ProjectileKeyFrame) fb);
            }
        }

        if (targets.isEmpty())
        {
            // No kf to nudge but the user IS in projectile mode -- still
            // consume the hotkey so we don't accidentally nudge a
            // selected Character behind the user's back.
            return true;
        }

        // (dx, dy) arrive in WORLD frame (ALT+W = +Y north, ALT+D = +X
        // east, etc. -- see the hotkey listeners in CreatorsPlugin).
        // The kf stores its offset in the source character's LOCAL frame
        // so the spotanim pivots with the model on rotation, so we have
        // to project the world delta onto the local axes before adding.
        //
        // local_x = world . right(theta) = -cos(theta)*dx + sin(theta)*dy
        // local_y = world . forward(theta) = -sin(theta)*dx + -cos(theta)*dy
        //
        // where theta = orientation * 2pi/2048 (JAU 0 = south,
        // forward = (0,-1); JAU 1024 = north, forward = (0,1); etc.)
        // -- same convention Programmer.updateProjectiles uses for the
        // local->world rotation, just the inverse direction here.
        int ori = (selectedCharacter != null && selectedCharacter.getCkObject() != null)
                ? selectedCharacter.getCkObject().getOrientation()
                : 0;
        double angle = ori * (2.0 * Math.PI / 2048.0);
        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);
        int localDx = (int) Math.round(-cosA * dx + sinA * dy);
        int localDy = (int) Math.round(-sinA * dx - cosA * dy);

        for (com.creatorskit.swing.timesheet.keyframe.ProjectileKeyFrame p : targets)
        {
            p.setStartX(p.getStartX() + localDx);
            p.setStartY(p.getStartY() + localDy);
            p.setStartHeight(p.getStartHeight() + dz);
        }
        // Push the new values into the playback path + refresh the
        // panel so the spinners show the updated numbers.
        if (toolBox != null && toolBox.getProgrammer() != null)
        {
            toolBox.getProgrammer().updatePrograms(currentTime);
        }
        if (selectedCharacter != null)
        {
            attributePanel.resetAttributes(selectedCharacter, currentTime);
        }
        return true;
    }

    public void duplicateHitsplatKeyFrame(KeyFrameType previousType, KeyFrameType targetType)
    {
        // Multi-select aware: each Character independently picks its own latest
        // source-slot hitsplat (sprite / variant / damage can differ across
        // characters), and that gets cloned into the target slot. Characters with
        // no source-slot hitsplat are silently skipped.
        java.util.Collection<Character> targets = resolveSelectionTargets();
        if (targets.isEmpty())
        {
            return;
        }

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (Character c : targets)
        {
            KeyFrame kf = c.findPreviousKeyFrame(previousType, currentTime, true);
            if (kf == null)
            {
                continue;
            }
            HitsplatKeyFrame src = (HitsplatKeyFrame) kf;
            HitsplatKeyFrame hkf = new HitsplatKeyFrame(
                    src.getTick(),
                    targetType,
                    src.getDuration(),
                    src.getSprite(),
                    src.getVariant(),
                    src.getDamage());
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(hkf, c, KeyFrameCharacterActionType.ADD));
            KeyFrame replaced = addKeyFrame(c, hkf);
            if (replaced != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replaced, c, KeyFrameCharacterActionType.REMOVE));
            }
        }
        addKeyFrameActions(kfa);
    }

    public void duplicateSpotanimKeyFrame(KeyFrameType previousType, KeyFrameType targetType)
    {
        // Same shape as duplicateHitsplatKeyFrame -- per-Character source lookup
        // so each Character keeps its own spotanim id / loop / height in the clone.
        java.util.Collection<Character> targets = resolveSelectionTargets();
        if (targets.isEmpty())
        {
            return;
        }

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (Character c : targets)
        {
            KeyFrame kf = c.findPreviousKeyFrame(previousType, currentTime, true);
            if (kf == null)
            {
                continue;
            }
            SpotAnimKeyFrame src = (SpotAnimKeyFrame) kf;
            SpotAnimKeyFrame spkf = new SpotAnimKeyFrame(
                    src.getTick(),
                    targetType,
                    src.getSpotAnimId(),
                    src.isLoop(),
                    src.getHeight(),
                    src.getRadius(),
                    src.getAnimationSpeed());
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(spkf, c, KeyFrameCharacterActionType.ADD));
            KeyFrame replaced = addKeyFrame(c, spkf);
            if (replaced != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replaced, c, KeyFrameCharacterActionType.REMOVE));
            }
        }
        addKeyFrameActions(kfa);
    }

    /**
     * Counterpart to {@link #duplicateSpotanimKeyFrame} for the 4 global
     * Sound slots. Reads the most recent {@code previousType} (Sound 1..4)
     * keyframe at or before the playhead from the global store, clones it
     * into {@code targetType}, and writes it through the standard global add
     * path so undo/redo, timeline refresh, and overwrite-on-same-tick all
     * work uniformly.
     *
     * <p>No-op when the source slot has nothing at or before the playhead
     * (nothing to copy) or when the slots are the same (UI shouldn't allow
     * this since the current slot is rendered as a flat label, but guard
     * defensively in case the cards are ever reused with different wiring).
     */
    public void duplicateSoundKeyFrame(KeyFrameType previousType, KeyFrameType targetType)
    {
        if (previousType == targetType) return;
        KeyFrame kf = findPreviousGlobalKeyFrame(previousType, currentTime);
        if (!(kf instanceof com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame)) return;
        com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame src =
                (com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame) kf;
        // Reuse the source kf's tick so the copy lands NEXT to the original
        // on the timeline (same column, different row) -- matches user
        // expectation when looking at the Duplicate To buttons. If the user
        // wants it at the playhead instead they can drag it after.
        com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame copy =
                new com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame(
                        src.getTick(), targetType, src.getSoundId(), src.getVolume());
        addKeyFrameAction(new KeyFrame[]{copy});
    }

    /**
     * Adds the keyframe to a specific character, or replaces a keyframe if the tick matches exactly
     * @param character the character to add the keyframe to
     * @param keyFrame the keyframe to add or modify for the character
     * @return the keyframe that is being replaced; null if there is no keyframe being replaced
     */
    public KeyFrame addKeyFrame(Character character, KeyFrame keyFrame)
    {
        if (character == null)
        {
            // Phase 2 follow-up: globals live in the central store and don't
            // need a Character owner. Allow the no-Character branch to write
            // directly so the user can author Camera/Fade/Shake keyframes
            // before any Object exists in the scene.
            if (keyFrame != null && isGlobalType(keyFrame.getKeyFrameType()))
            {
                KeyFrame replaced = plugin.getGlobalKeyFrames().add(keyFrame);
                attributePanel.setKeyFramedIcon(true);
                // No selectedCharacter to drive resetAttributes, but a refresh
                // is still needed so the panel reflects the newly-added kf.
                attributePanel.refreshKeyFrameSelectionState();
                return replaced;
            }
            return null;
        }

        KeyFrame keyFrameToReplace = character.addKeyFrame(keyFrame, currentTime);
        attributePanel.setKeyFramedIcon(true);
        // Skip resetAttributes when an auto-update batch is in progress --
        // it reads selectedKeyFrames (still pointing at the OLD keyframe at
        // this moment in the batch) and snaps the spinners back to the old
        // values, causing a visible flicker. The outer setSelectedKeyFrames
        // at the bottom of onUpdateButtonPressed runs its own resetAttributes
        // with the replacement-mapped marquee, so this intermediate one is
        // redundant during auto-update.
        if (!attributePanel.isInAutoUpdateBatch())
        {
            attributePanel.resetAttributes(character, currentTime);
        }
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            toolBox.getProgrammer().updateProgram(character, currentTime);
        }

        return keyFrameToReplace;
    }

    /**
     * Removes a specific keyframe from the chosen character
     * @param character the character to remove the keyframe from
     * @param keyFrame the keyframe to remove
     */
    public void removeKeyFrame(Character character, KeyFrame keyFrame)
    {
        if (character == null)
        {
            // Symmetric to addKeyFrame -- globals can be undone (REMOVE-on-undo)
            // even when no Character owns the deletion.
            if (keyFrame != null && isGlobalType(keyFrame.getKeyFrameType()))
            {
                plugin.getGlobalKeyFrames().remove(keyFrame);
                attributePanel.refreshKeyFrameSelectionState();
            }
            return;
        }
        character.removeKeyFrame(keyFrame);
        attributePanel.resetAttributes(character, currentTime);
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            toolBox.getProgrammer().updateProgram(character, currentTime);
        }
    }

    /**
     * Removes the specified keyframes from the chosen character
     * @param character the character to remove the keyframes from
     * @param keyFrames the keyframes to remove
     */
    public void removeKeyFrame(Character character, KeyFrame[] keyFrames)
    {
        for (KeyFrame keyFrame : keyFrames)
        {
            character.removeKeyFrame(keyFrame);
        }

        attributePanel.resetAttributes(character, currentTime);
        toolBox.getProgrammer().updateProgram(character, currentTime);
    }

    public void addKeyFrameActions(KeyFrameAction[] actions)
    {
        // Universal hitsplat -> bar sync hook. Every keyframe-mutation code
        // path (add / edit / drag / delete) commits through this method, so
        // hooking here means hitsplat changes drive the matching bar drain
        // no matter how the user made the change. Recursion guard so the
        // sync's own actions don't trigger another sync pass.
        if (!inHitsplatSyncRecursionGuard)
        {
            actions = maybeExpandWithHitsplatSync(actions);
        }

        if (keyFrameActions.length == UNDO_LIMIT)
        {
            keyFrameActions = ArrayUtils.remove(keyFrameActions, 0);
        }

        if (keyFrameActions.length > 0)
        {
            while (keyFrameActions.length - 1 > undoStack)
            {
                keyFrameActions = ArrayUtils.remove(keyFrameActions, keyFrameActions.length - 1);
            }
        }

        keyFrameActions = ArrayUtils.add(keyFrameActions, actions);
        undoStack = keyFrameActions.length - 1;
    }

    /**
     * Scans the incoming action batch for hitsplat-related (ADD / REMOVE /
     * move) keyframe changes. For each unique (Character, tick) touched by a
     * hitsplat action, re-derives the matching bar drain from the current
     * state of the Character and folds the resulting actions into the same
     * batch so the whole change is one undo step.
     *
     * <p>"Current state" = post-mutation: the caller has already applied the
     * incoming actions to Characters before calling addKeyFrameActions, so
     * looking up hitsplats at (c, tick) now sees the user's just-finished
     * edit / move. Moves show up as two actions (REMOVE old tick + ADD new
     * tick); both ticks get resync'd, so the bar drain follows the hitsplat
     * to its new home.
     *
     * <p>Conservative deletion policy: when a (c, tick) pair has no
     * hitsplats left, we don't touch existing bar KFs at that tick -- the
     * user might want them to survive standalone. They can clean up
     * orphans manually.
     */
    private KeyFrameAction[] maybeExpandWithHitsplatSync(KeyFrameAction[] actions)
    {
        if (actions == null || actions.length == 0) return actions;

        // Dedupe (Character, tick) pairs across the incoming hitsplat actions.
        java.util.LinkedHashMap<String, Character> charByKey = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, Double> tickByKey = new java.util.LinkedHashMap<>();
        for (KeyFrameAction a : actions)
        {
            if (!(a instanceof KeyFrameCharacterAction)) continue;
            KeyFrameCharacterAction kfca = (KeyFrameCharacterAction) a;
            KeyFrame kf = kfca.getKeyFrame();
            if (kf == null) continue;
            if (!isHitsplatType(kf.getKeyFrameType())) continue;
            Character c = kfca.getCharacter();
            if (c == null) continue;
            String key = System.identityHashCode(c) + "@" + kf.getTick();
            charByKey.putIfAbsent(key, c);
            tickByKey.putIfAbsent(key, kf.getTick());
        }
        if (charByKey.isEmpty()) return actions;

        inHitsplatSyncRecursionGuard = true;
        try
        {
            KeyFrameAction[] syncActions = new KeyFrameAction[0];
            for (String key : charByKey.keySet())
            {
                syncActions = applyHitsplatSyncAt(syncActions, charByKey.get(key), tickByKey.get(key));
            }
            if (syncActions.length == 0) return actions;
            // Concat: original actions first, sync follow-ups after. Undo
            // replays the whole batch atomically so order within the batch
            // is purely cosmetic for the history view.
            KeyFrameAction[] merged = new KeyFrameAction[actions.length + syncActions.length];
            System.arraycopy(actions, 0, merged, 0, actions.length);
            System.arraycopy(syncActions, 0, merged, actions.length, syncActions.length);
            return merged;
        }
        finally
        {
            inHitsplatSyncRecursionGuard = false;
        }
    }

    /**
     * Resync hook fired by {@link #maybeExpandWithHitsplatSync} at one
     * (Character, tick) pair. Two branches:
     *
     * <p>1. Hitsplats present at (c, tick): for each sprite group, build a
     * bar drain (honouring the prior bar's "Sync hitsplats" toggle) and
     * replace the bar KF at this tick -- unless the existing bar KF there
     * was created by the user (autoSynced=false), in which case we leave
     * it alone (the user owns it).
     *
     * <p>2. No hitsplats at (c, tick) anymore (move-away / delete): walk
     * the three bar types and remove any keyframe at this tick whose
     * autoSynced flag is true. User-created keyframes (autoSynced=false)
     * survive cleanup.
     */
    private KeyFrameAction[] applyHitsplatSyncAt(KeyFrameAction[] kfa, Character c, double tick)
    {
        // Aggregate damage per sprite at this (Character, tick). Each unique
        // sprite -> its own bar drain via pickBarKeyFrameForHitsplat.
        java.util.LinkedHashMap<com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite, Integer> damageBySprite =
                new java.util.LinkedHashMap<>();
        for (KeyFrameType hsType : KeyFrameType.HITSPLAT_TYPES)
        {
            KeyFrame hsKf = c.findKeyFrame(hsType, tick);
            if (!(hsKf instanceof HitsplatKeyFrame)) continue;
            HitsplatKeyFrame hs = (HitsplatKeyFrame) hsKf;
            int dmg = Math.max(0, hs.getDamage());
            if (dmg <= 0) continue;
            damageBySprite.merge(hs.getSprite(), dmg, Integer::sum);
        }

        if (damageBySprite.isEmpty())
        {
            // Cleanup branch: tick has no hitsplats anymore. Remove
            // autoSynced bar KFs here so the bar reverts to its prior
            // (un-damaged) state. Manual bar KFs are left alone.
            for (KeyFrameType barType : new KeyFrameType[]{KeyFrameType.HEALTH, KeyFrameType.SHIELD, KeyFrameType.SPECIAL})
            {
                KeyFrame existing = c.findKeyFrame(barType, tick);
                if (existing == null) continue;
                if (!isAutoSynced(existing)) continue;
                c.removeKeyFrame(existing);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(existing, c, KeyFrameCharacterActionType.REMOVE));
            }
            return kfa;
        }

        for (java.util.Map.Entry<com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite, Integer> e : damageBySprite.entrySet())
        {
            // Determine the target bar type for this sprite so we can do the
            // manual-KF check BEFORE building the drain (cheaper, and lets
            // us preserve the user's manual edit even when a hitsplat sits
            // at the same tick).
            KeyFrameType targetBarType = barTypeForSprite(e.getKey());
            KeyFrame existing = c.findKeyFrame(targetBarType, tick);
            if (existing != null && !isAutoSynced(existing))
            {
                // User owns this bar KF (created or edited manually) -- leave
                // it alone. They explicitly want whatever value they set,
                // even with a hitsplat present at the same tick.
                continue;
            }

            KeyFrame drain = pickBarKeyFrameForHitsplat(c, e.getKey(), e.getValue(), tick);
            if (drain == null) continue; // no prior bar / toggle off

            markAutoSynced(drain);

            KeyFrame replaced = addKeyFrame(c, drain);
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(drain, c, KeyFrameCharacterActionType.ADD));
            if (replaced != null && replaced != drain)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replaced, c, KeyFrameCharacterActionType.REMOVE));
            }
        }
        return kfa;
    }

    /** True if {@code kf} carries an {@code autoSynced=true} flag. Used so
     *  the sync can leave user-created bar KFs alone while still owning the
     *  ones it created. */
    private static boolean isAutoSynced(KeyFrame kf)
    {
        if (kf instanceof HealthKeyFrame) return ((HealthKeyFrame) kf).isAutoSynced();
        if (kf instanceof ShieldKeyFrame) return ((ShieldKeyFrame) kf).isAutoSynced();
        if (kf instanceof SpecialKeyFrame) return ((SpecialKeyFrame) kf).isAutoSynced();
        return false;
    }

    /** Stamps {@code kf} with {@code autoSynced=true} so a later sync pass
     *  can recognise it as sync-owned. */
    private static void markAutoSynced(KeyFrame kf)
    {
        if (kf instanceof HealthKeyFrame) ((HealthKeyFrame) kf).setAutoSynced(true);
        else if (kf instanceof ShieldKeyFrame) ((ShieldKeyFrame) kf).setAutoSynced(true);
        else if (kf instanceof SpecialKeyFrame) ((SpecialKeyFrame) kf).setAutoSynced(true);
    }

    private static KeyFrameType barTypeForSprite(com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite sprite)
    {
        switch (sprite)
        {
            case SHIELD:
            case SHIELD_OTHER:
            case SHIELD_MAX:
                return KeyFrameType.SHIELD;
            case POISE:
            case POISE_OTHER:
            case POISE_MAX:
                return KeyFrameType.SPECIAL;
            default:
                return KeyFrameType.HEALTH;
        }
    }

    private static boolean isHitsplatType(KeyFrameType type)
    {
        return type == KeyFrameType.HITSPLAT_1
                || type == KeyFrameType.HITSPLAT_2
                || type == KeyFrameType.HITSPLAT_3
                || type == KeyFrameType.HITSPLAT_4;
    }

    public void removeKeyFrameActions(Character character)
    {
        for (int i = 0; i < keyFrameActions.length; i++)
        {
            KeyFrameAction[] actions = keyFrameActions[i];
            for (int e = 0; e < actions.length; e++)
            {
                KeyFrameAction kfa = actions[e];
                if (kfa.getActionType() == KeyFrameActionType.CHARACTER)
                {
                    KeyFrameCharacterAction kfca = (KeyFrameCharacterAction) kfa;
                    if (kfca.getCharacter() == character)
                    {
                        keyFrameActions = ArrayUtils.removeElement(keyFrameActions, actions);
                        break;
                    }
                }
            }
        }
        undoStack = keyFrameActions.length - 1;
    }

    public void undo()
    {
        if (undoStack == -1 || keyFrameActions.length == 0)
        {
            return;
        }

        selectedKeyFrames = new KeyFrame[0];
        KeyFrameAction[] lastActions = keyFrameActions[undoStack];
        for (int i = 0; i < lastActions.length; i++)
        {
            KeyFrameAction keyFrameAction = lastActions[i];

            if (keyFrameAction.getActionType() == KeyFrameActionType.CHARACTER)
            {
                KeyFrameCharacterAction kfca = (KeyFrameCharacterAction) keyFrameAction;

                KeyFrameCharacterActionType actionType = kfca.getCharacterActionType();
                if (actionType == KeyFrameCharacterActionType.ADD)
                {
                    removeKeyFrame(kfca.getCharacter(), keyFrameAction.getKeyFrame());
                }

                if (actionType == KeyFrameCharacterActionType.REMOVE)
                {
                    KeyFrame keyFrame = keyFrameAction.getKeyFrame();
                    addKeyFrame(kfca.getCharacter(), keyFrameAction.getKeyFrame());
                    selectedKeyFrames = ArrayUtils.add(selectedKeyFrames, keyFrame);
                }
            }
        }

        undoStack--;
    }

    public void redo()
    {
        if (undoStack + 1 >= keyFrameActions.length)
        {
            return;
        }

        undoStack++;

        selectedKeyFrames = new KeyFrame[0];
        KeyFrameAction[] lastUndoneActions = keyFrameActions[undoStack];
        for (KeyFrameAction keyFrameAction : lastUndoneActions)
        {
            if (keyFrameAction.getActionType() == KeyFrameActionType.CHARACTER)
            {
                KeyFrameCharacterAction keyFrameCharacterAction = (KeyFrameCharacterAction) keyFrameAction;
                KeyFrameCharacterActionType actionType = keyFrameCharacterAction.getCharacterActionType();
                if (actionType == KeyFrameCharacterActionType.ADD)
                {
                    KeyFrame keyFrame = keyFrameAction.getKeyFrame();
                    addKeyFrame(keyFrameCharacterAction.getCharacter(), keyFrame);
                    selectedKeyFrames = ArrayUtils.add(selectedKeyFrames, keyFrame);
                }

                if (actionType == KeyFrameCharacterActionType.REMOVE)
                {
                    removeKeyFrame(keyFrameCharacterAction.getCharacter(), keyFrameAction.getKeyFrame());
                }
            }
        }
    }

    public void setSelectedCharacter(Character character)
    {
        selectedCharacter = character;
        summarySheet.setSelectedCharacter(character);
        attributeSheet.setSelectedCharacter(character);
        if (globalAttributeSheet != null) globalAttributeSheet.setSelectedCharacter(character);
        attributePanel.setSelectedCharacter(character);
        attributePanel.resetAttributes(character, currentTime);
        // Selecting a Character should reveal its non-global rows even if the
        // user hasn't marqueed any keyframes yet -- otherwise the timeline
        // stays stuck in the 3-rows-only collapsed view from a prior empty
        // selection. Recompute the collapse rule with the new character state.
        refreshGlobalRowsOnlyMode();
    }

    /**
     * Explicit setter so that selection-state UI (AttributePanel) refreshes whenever
     * the keyframe selection changes. Replaces Lombok's auto-generated setter.
     * Also re-pulls the displayed attribute values so clicking a keyframe shows
     * THAT keyframe's data instead of the seeker-bar keyframe.
     */
    public void setSelectedKeyFrames(KeyFrame[] keyFrames)
    {
        this.selectedKeyFrames = keyFrames;
        // Any keyframe-selection change clears the block selection -- the
        // block-click path explicitly re-sets it after this call returns.
        // Marquee / keyframe-click / programmatic paths leave it cleared so
        // Delete-key behaviour reverts to "delete just keyframes" instead
        // of accidentally triggering the block-delete confirmation.
        clearBlockSelection();
        refreshGlobalRowsOnlyMode();
        if (attributePanel != null)
        {
            attributePanel.refreshKeyFrameSelectionState();
            // Always refresh attributes, even when no Character is selected --
            // global keyframes have a no-Character path in resetAttributesInner
            // that populates the card from the central store. Skipping the
            // call when selectedCharacter is null was leaving the card in the
            // state from switchCards' earlier (stale-marquee) reset, which
            // showed default / empty values for the just-marqueed keyframe.
            attributePanel.resetAttributes(selectedCharacter, currentTime);
        }
    }

    /**
     * Single source of truth for the active timeline view (local vs global).
     * Honors the explicit toggle button: if any Object is selected, the
     * user's last choice wins; otherwise the view is locked to global.
     * Pair-updates the toggle button labels + the CardLayouts so the UI
     * (labels column + sheet) match the chosen view.
     */
    private void refreshGlobalRowsOnlyMode()
    {
        boolean noObject = selectedCharacter == null
                && (selectionManager == null || selectionManager.size() == 0);
        boolean targetGlobal = noObject || userPrefersGlobalView;
        showView(targetGlobal ? VIEW_GLOBAL : VIEW_LOCAL);
        refreshViewToggleButtons(noObject, targetGlobal);
    }

    /** Click handler for the toggle buttons. No-op when no Object is selected. */
    private void onViewToggleClicked()
    {
        boolean noObject = selectedCharacter == null
                && (selectionManager == null || selectionManager.size() == 0);
        if (noObject) return;
        userPrefersGlobalView = !userPrefersGlobalView;
        refreshGlobalRowsOnlyMode();
    }

    /**
     * Swaps the active card in both label and sheet CardLayouts at once.
     * They must stay in lockstep -- showing the global sheet while the
     * local labels are visible would put the rows under the wrong names.
     */
    private void showView(String view)
    {
        ((CardLayout) labelCards.getLayout()).show(labelCards, view);
        ((CardLayout) sheetCards.getLayout()).show(sheetCards, view);
        labelCards.revalidate();
        labelCards.repaint();
        sheetCards.revalidate();
        sheetCards.repaint();
    }

    /** Updates both toggle buttons' text + enabled state to match the rendered view. */
    private void refreshViewToggleButtons(boolean noObject, boolean globalMode)
    {
        String text = globalMode ? "Manage local" : "Manage global";
        String tip = noObject
                ? "<html>Locked to global view -- select an Object to enable<br>per-Character rows.</html>"
                : (globalMode
                    ? "Switch to the per-Object property rows for the current selection."
                    : "Switch to the global property rows (Camera / Screen Fade / Screen Shake).");
        if (viewToggleButton != null)
        {
            viewToggleButton.setEnabled(!noObject);
            viewToggleButton.setText(text);
            viewToggleButton.setToolTipText(tip);
        }
        if (globalViewToggleButton != null)
        {
            globalViewToggleButton.setEnabled(!noObject);
            globalViewToggleButton.setText(text);
            globalViewToggleButton.setToolTipText(tip);
        }
    }

    public void setCurrentTime(double tick, boolean playing)
    {
        if (!playing)
        {
            toolBox.getProgrammer().pause();
        }

        if (tick < -ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            tick = -ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        if (tick > ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            tick = ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        currentTime = tick;
        attributeSheet.setCurrentTime(currentTime);
        summarySheet.setCurrentTime(currentTime);
        if (globalAttributeSheet != null) globalAttributeSheet.setCurrentTime(currentTime);

        triggerTimeSpinnerChange = false;
        timeSpinner.setValue(currentTime);
        triggerTimeSpinnerChange = true;

        Programmer programmer = toolBox.getProgrammer();

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            if (playing)
            {
                programmer.updateProgramsOnTick();
            }
            else
            {
                programmer.updatePrograms(tick);
            }
            // Push the camera keyframe in the same call as the time change.
            // Without this, applying the camera relies on onClientTick firing
            // AFTER the Programmer's time increment -- but plugin.onClientTick
            // registers first, so during play it reads the previous tick's
            // currentTime and the camera lags / stays static. Calling here
            // makes camera response symmetric with scrubbing.
            if (plugin != null)
            {
                plugin.applyCurrentCameraKeyframe();
            }
        }

        // Defensive cleanup of the preview-time indicator state. If a drag
        // press-and-release-outside-canvas left timeIndicatorPressed stuck
        // true on either sheet, the secondary "preview" tick label would
        // keep rendering at the stale press position even after the seeker
        // had moved elsewhere (visible as the duplicate "0.6" + "5.6" labels
        // in the bug report). Snapping previewTime onto the new currentTime
        // collapses the two indicators visually; clearing the pressed flag
        // makes sure subsequent renders skip the preview pass entirely.
        attributeSheet.setPreviewTime(tick);
        summarySheet.setPreviewTime(tick);
        attributeSheet.setTimeIndicatorPressed(false);
        summarySheet.setTimeIndicatorPressed(false);
        if (globalAttributeSheet != null)
        {
            globalAttributeSheet.setPreviewTime(tick);
            globalAttributeSheet.setTimeIndicatorPressed(false);
        }

        // Auto-scroll the horizontal timeline so the playbar stays visible.
        // Fires on playback, spinner edits, arrow-key skips, etc. -- any
        // time setCurrentTime is called -- so the user never loses sight of
        // the playhead during play and arrow-key seeking jumps the view
        // along with the seeker.
        autoScrollToKeepPlaybarVisible();

        onCurrentTimeChanged(tick);
    }

    /**
     * Scrolls the timeline horizontally if the playbar (currentTime) is
     * outside the visible window {@code [-hScroll, -hScroll + zoom]}.
     * Re-centres so the playbar appears at 10% from the left edge --
     * leaves most of the visible range showing what's coming, which is
     * the natural "follow the playhead during playback" behaviour. No-op
     * when the playbar is already on-screen, so manual scrubbing inside
     * the visible window doesn't trigger jumps.
     */
    private void autoScrollToKeepPlaybarVisible()
    {
        double visibleStart = -hScroll;
        double visibleEnd = visibleStart + zoom;
        if (currentTime >= visibleStart && currentTime <= visibleEnd)
        {
            return; // already in view, leave hScroll alone
        }
        // Anchor playbar at 10% from the left of the visible range: the
        // visibleStart sits 0.1*zoom before currentTime, so the user sees
        // what just passed plus most of the upcoming range.
        double newVisibleStart = currentTime - 0.1 * zoom;
        double newHScroll = -newVisibleStart;
        // Reuse the existing scrollbar plumbing so the scrollbar thumb
        // syncs and updateSheets fires.
        hScroll = round(newHScroll);
        if (hScroll < -ABSOLUTE_MAX_SEQUENCE_LENGTH) hScroll = -ABSOLUTE_MAX_SEQUENCE_LENGTH;
        if (hScroll > ABSOLUTE_MAX_SEQUENCE_LENGTH) hScroll = ABSOLUTE_MAX_SEQUENCE_LENGTH;
        boundSliderMinMax();
        updateScrollBar();
        updateSheets();
    }

    public void onCurrentTimeChanged(double tick)
    {
        // Skip the AttributePanel refresh while the Programmer is playing.
        // setCurrentTime fires every client tick during play, and each
        // refresh cascades into per-field spinner.setValue calls -- each
        // of those fires Swing ChangeListeners and a background-flash
        // repaint. Multiplied by the active card's field count and the
        // play tick rate, the EDT can spike enough to lock visible
        // playback in heavier scenes (especially with Orientation kfs,
        // which carry 6 fields each).
        //
        // The card is informational during play -- the user isn't
        // editing -- so a stale snapshot is fine. The next pause / scrub
        // / selection change goes through one of the other resetAttributes
        // call sites and refreshes the card to the live state.
        if (toolBox != null && toolBox.getProgrammer() != null && toolBox.getProgrammer().isPlaying())
        {
            return;
        }
        attributePanel.resetAttributes(selectedCharacter, tick);
    }

    public void setPlayButtonIcon(boolean playing)
    {
        playButton.setIcon(playing ? PAUSE : PLAY);
    }

    public void updatePreviewTime(double tick)
    {
        attributeSheet.setPreviewTime(tick);
        summarySheet.setPreviewTime(tick);
        if (globalAttributeSheet != null) globalAttributeSheet.setPreviewTime(tick);
    }

    private void setupTreeScrollPane()
    {
        treeScrollPane = new TreeScrollPane(managerTree);
        // Preferred width drives the initial position of the rootSplit's
        // vertical divider -- 360 keeps the tree narrow at startup so the
        // timeline / summary on the right get most of the canvas. User
        // can drag the divider to whatever they want from there.
        treeScrollPane.setPreferredSize(new Dimension(360, 0));

        MouseWheelListener[] mouseWheelListeners = treeScrollPane.getMouseWheelListeners();
        for (int i = 0; i < mouseWheelListeners.length; i++)
        {
            treeScrollPane.removeMouseWheelListener(mouseWheelListeners[i]);
        }

        treeScrollPane.addMouseWheelListener(new MouseAdapter()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                if (e.isControlDown())
                {
                    if (e.isAltDown() || e.isShiftDown())
                    {
                        return;
                    }

                    managerTree.scrollSelectedIndex(e.getWheelRotation());
                    return;
                }

                if (e.isShiftDown())
                {
                    if (e.isControlDown() || e.isAltDown())
                    {
                        return;
                    }

                    // Wheel jumps 2 properties per notch -- one-at-a-time felt
                    // slow given there are 17 local rows. Arrow keys keep the
                    // one-row step for precise navigation.
                    scrollAttributePanel(e.getWheelRotation() * 2);
                    return;
                }

                JScrollBar bar = treeScrollPane.getVerticalScrollBar();
                bar.setValue(bar.getValue() + e.getWheelRotation() * 15);
            }
        });
    }

    private void setupControlPanel()
    {
        controlPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        controlPanel.setLayout(new GridBagLayout());
        controlPanel.setFocusable(true);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        timeSpinner.setBackground(ColorScheme.DARK_GRAY_COLOR);
        timeSpinner.setModel(new SpinnerNumberModel(0, -ABSOLUTE_MAX_SEQUENCE_LENGTH, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.1));
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) timeSpinner.getEditor();
        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(2);
        timeSpinner.setValue(0);
        timeSpinner.addChangeListener(e ->
        {
            if (!triggerTimeSpinnerChange)
            {
                return;
            }

            double tick = round((double) timeSpinner.getValue());
            setCurrentTime(tick, false);
        });
        controlPanel.add(timeSpinner, c);

        playButton.setIcon(PLAY);
        playButton.setPreferredSize(new Dimension(35, 35));
        playButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        playButton.addActionListener(e -> toolBox.getProgrammer().togglePlay());

        JButton backAttributeSheet = new JButton(new ImageIcon(SKIP_LEFT));
        backAttributeSheet.setBackground(ColorScheme.DARK_GRAY_COLOR);
        backAttributeSheet.setPreferredSize(new Dimension(50, 35));
        backAttributeSheet.addActionListener(e -> onAttributeSkipPrevious());

        JButton forwardAttributeSheet = new JButton(new ImageIcon(SKIP_RIGHT));
        forwardAttributeSheet.setBackground(ColorScheme.DARK_GRAY_COLOR);
        forwardAttributeSheet.setPreferredSize(new Dimension(50, 35));
        forwardAttributeSheet.addActionListener(e -> onAttributeSkipForward());

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 0;
        JPanel controls = new JPanel();
        controls.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        controls.add(backAttributeSheet);
        controls.add(playButton);
        controls.add(forwardAttributeSheet);
        controlPanel.add(controls, c);
    }

    private void setupAttributePanel()
    {
        attributePanel = new AttributePanel(client, clientThread, config, this, dataFinder, selectionManager, cacheMetadataStore);
        summarySheet = new SummarySheet(toolBox, config, managerTree, attributePanel);
        attributeSheet = new AttributeSheet(toolBox, config, managerTree, attributePanel);
        globalAttributeSheet = new com.creatorskit.swing.timesheet.sheets.GlobalAttributeSheet(toolBox, config, managerTree, attributePanel);
    }

    private void setupAttributeSheet()
    {
        // Build TWO independent label columns -- one for the local view (17
        // per-Character types), one for the global view (Camera / Fade /
        // Shake). The viewToggleButton sits at the top of both columns so a
        // toggle is reachable from either view. Each column is wrapped in
        // its own scroll pane; the CardLayout in labelCards swaps which is
        // visible based on userPrefersGlobalView.
        // Wrap each scrollpane in a BorderLayout container so a Filters
        // button can sit pinned at the bottom of the column without
        // scrolling off when the row list overflows. The scrollpane
        // itself still owns the row labels via its viewport.
        JPanel localColumnContainer = new JPanel(new BorderLayout());
        localColumnContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JPanel globalColumnContainer = new JPanel(new BorderLayout());
        globalColumnContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        labels = buildLabelColumn(labelScrollPane,
                attributeSheet,
                KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL,
                /*ownsToggleButton=*/ true);
        globalLabels = buildLabelColumn(globalLabelScrollPane,
                globalAttributeSheet,
                KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL,
                /*ownsToggleButton=*/ false);

        localColumnContainer.add(labelScrollPane, BorderLayout.CENTER);
        localColumnContainer.add(makeFiltersButton(true), BorderLayout.SOUTH);

        globalColumnContainer.add(globalLabelScrollPane, BorderLayout.CENTER);
        globalColumnContainer.add(makeFiltersButton(false), BorderLayout.SOUTH);

        // Wire the two CardLayouts so the toggle button (built inside the
        // local column above) can swap both at once. Default to local view;
        // refreshGlobalRowsOnlyMode below picks the right view based on
        // selection state.
        labelCards.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelCards.add(localColumnContainer, VIEW_LOCAL);
        labelCards.add(globalColumnContainer, VIEW_GLOBAL);

        sheetCards.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        sheetCards.add(attributeSheet, VIEW_LOCAL);
        sheetCards.add(globalAttributeSheet, VIEW_GLOBAL);

        refreshGlobalRowsOnlyMode();
    }

    /**
     * Constructs one of the two label columns. Returns the populated label
     * array indexed labels[0] = spacer header, labels[1..N] = type rows in
     * the order given by {@code types}. The first column (local) also owns
     * the shared viewToggleButton -- replaces its spacer header so the
     * toggle is always at the top of that view. The global column gets a
     * second toggle button instance (separate JButton, same handler) so the
     * user can still toggle when only the global view is visible.
     */
    private JLabel[] buildLabelColumn(JScrollPane scrollPane, TimeSheet bodySheet, KeyFrameType[] types, boolean ownsToggleButton)
    {
        // Local view drives its row order through the TimelineLocalRowLayout
        // (currently inserts a "Hitsplats" collapsible parent over
        // HITSPLAT_1..4). Global keeps the flat types[] pass-through since
        // it has no grouping concept yet.
        boolean isLocalView = (types == KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL);

        JPanel labelPanel = new JPanel();
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
        labelPanel.setFocusable(true);

        scrollPane.setViewportView(labelPanel);
        scrollPane.setBorder(new EmptyBorder(1, 0, 1, 0));
        scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.setPreferredSize(new Dimension(LABEL_COL_WIDTH, 150));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // AS_NEEDED so the user actually sees the scrollbar when rows are
        // cropped. Used to be NEVER + InvisibleScrollBar -- the model still
        // drove vScroll but there was no visual cue, which is exactly the
        // "properties cropped silently" UX problem we're fixing now.
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        MouseWheelListener[] mouseWheelListeners = scrollPane.getMouseWheelListeners();
        for (int i = 0; i < mouseWheelListeners.length; i++)
        {
            scrollPane.removeMouseWheelListener(mouseWheelListeners[i]);
        }

        JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL);
        scrollBar.addAdjustmentListener(e -> bodySheet.onVerticalScrollEvent(e.getValue()));
        scrollPane.setVerticalScrollBar(scrollBar);
        // Body needs to know how many real rows live in the column so it can
        // draw the bottom edge-fade indicator at the right time (only when
        // there is actually content scrolled below the viewport).
        // Local view's row count depends on collapse state; global stays
        // flat.
        bodySheet.setContentRowCount(isLocalView ? localRowLayout.visibleRows().size() : types.length);

        scrollPane.addMouseWheelListener(new MouseAdapter()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                if (e.isControlDown() && !e.isAltDown() && !e.isShiftDown())
                {
                    managerTree.scrollSelectedIndex(e.getWheelRotation());
                    return;
                }
                if (e.isShiftDown() && !e.isControlDown() && !e.isAltDown())
                {
                    scrollAttributePanel(e.getWheelRotation() * 2);
                    return;
                }
                // Plain wheel: discrete 2-row jumps. The previous 15-px step
                // produced sub-row scrolling (rowHeight = 24) which is what
                // the user described as "scrolling linearly with much
                // smaller scroll steps." Using the body sheet's rowHeight
                // keeps the math row-aligned even if the row size changes.
                scrollBar.setValue(scrollBar.getValue()
                        + e.getWheelRotation() * bodySheet.getRowHeight() * 2);
            }
        });

        // Manage local/global toggle: needs to read as an obvious button,
        // not just another label row. BRAND_ORANGE background + a 1px
        // raised-look LineBorder in BRIGHT_GRAY_COLOR gives high contrast
        // against the dark labels beneath. Bumped to 28px so it's taller
        // than the property rows (24px) and visually separate.
        JButton toggle = new JButton();
        toggle.setFocusable(false);
        toggle.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
                new EmptyBorder(0, 6, 0, 6)));
        toggle.setMargin(new java.awt.Insets(0, 4, 0, 4));
        toggle.setPreferredSize(new Dimension(LABEL_COL_WIDTH, 28));
        toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.setBackground(ColorScheme.BRAND_ORANGE);
        toggle.setForeground(Color.WHITE);
        toggle.setOpaque(true);
        toggle.setFont(FontManager.getRunescapeBoldFont());
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> onViewToggleClicked());
        labelPanel.add(toggle);
        if (ownsToggleButton)
        {
            // Keep a reference on the panel-level field so the refresh code can
            // update its text. The other column's button mirrors it via the
            // shared handler -- we just refresh both buttons together.
            this.viewToggleButton = toggle;
        }
        else
        {
            this.globalViewToggleButton = toggle;
        }

        if (isLocalView)
        {
            // Build via the row layout (group parents + leaves).
            return rebuildLocalLabelRows(labelPanel, bodySheet);
        }
        // Global view: flat list, filtered by globalRowLayout.hidden.
        return rebuildGlobalLabelRows(labelPanel, bodySheet);
    }

    /**
     * Rebuilds the global label column based on the current
     * {@link #globalRowLayout} state (only filtering, no grouping). Same
     * shape as {@link #rebuildLocalLabelRows} so the Filters dialog can
     * call into either symmetrically.
     */
    private JLabel[] rebuildGlobalLabelRows(JPanel labelPanel, TimeSheet bodySheet)
    {
        while (labelPanel.getComponentCount() > 1)
        {
            labelPanel.remove(1);
        }
        java.util.List<com.creatorskit.swing.timesheet.sheets.TimelineGlobalRowLayout.Row> visible =
                globalRowLayout.visibleRows();
        JLabel[] result = new JLabel[visible.size() + 1];
        result[0] = new JLabel();

        for (int i = 0; i < visible.size(); i++)
        {
            com.creatorskit.swing.timesheet.sheets.TimelineGlobalRowLayout.Row row = visible.get(i);
            boolean isParent = row.kind == com.creatorskit.swing.timesheet.sheets.TimelineGlobalRowLayout.Row.Kind.GROUP_PARENT;
            String parentGroup = isParent ? row.groupName : null;
            // Capture group name once for the closures.
            final String groupNameFinal = parentGroup;
            JLabel label = makeRowLabel(
                    row.displayName(),
                    isParent,
                    parentGroup,
                    row.isGroupChild,
                    isParent ? () -> onToggleGlobalGroup(groupNameFinal) : null,
                    isParent ? () -> globalRowLayout.isCollapsed(groupNameFinal) : null);
            result[i + 1] = label;
            labelPanel.add(label);
        }
        bodySheet.setContentRowCount(visible.size());
        labelPanel.revalidate();
        labelPanel.repaint();
        bodySheet.repaint();
        return result;
    }

    /**
     * Toggle a global-view group's collapse state, rebuild the global
     * label column, repaint, and refresh the attribute-panel selection
     * highlight. Wired to each global group parent row's mouse listener.
     */
    private void onToggleGlobalGroup(String groupName)
    {
        if (groupName == null) return;
        globalRowLayout.toggleCollapsed(groupName);
        java.awt.Container labelPanel = (java.awt.Container) globalLabelScrollPane.getViewport().getView();
        this.globalLabels = rebuildGlobalLabelRows((JPanel) labelPanel, globalAttributeSheet);
        if (attributePanel != null) attributePanel.refreshKeyFrameSelectionState();
    }

    /**
     * Builds the "Filters..." button that pins to the bottom of a label
     * column. Click opens the two-column filters dialog scoped to the
     * column's view (local / global) and rewires the column rows on
     * change.
     */
    private JButton makeFiltersButton(boolean isLocalView)
    {
        JButton filters = new JButton("Filters...");
        filters.setFocusable(false);
        filters.setForeground(Color.WHITE);
        filters.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
        filters.setOpaque(true);
        filters.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
                new EmptyBorder(0, 6, 0, 6)));
        filters.setMargin(new java.awt.Insets(0, 4, 0, 4));
        filters.setPreferredSize(new Dimension(LABEL_COL_WIDTH, 28));
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        filters.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filters.addActionListener(e ->
        {
            if (isLocalView)
            {
                com.creatorskit.swing.timesheet.sheets.TimelineFiltersDialog.show(
                        this,
                        "Local property filters",
                        KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL,
                        localRowLayout.getHidden(),
                        h -> rebuildLocalAfterFilterChange());
            }
            else
            {
                com.creatorskit.swing.timesheet.sheets.TimelineFiltersDialog.show(
                        this,
                        "Global property filters",
                        KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL,
                        globalRowLayout.getHidden(),
                        h -> rebuildGlobalAfterFilterChange());
            }
        });
        return filters;
    }

    private void rebuildLocalAfterFilterChange()
    {
        JPanel labelPanel = (JPanel) labelScrollPane.getViewport().getView();
        this.labels = rebuildLocalLabelRows(labelPanel, attributeSheet);
        if (attributePanel != null) attributePanel.refreshKeyFrameSelectionState();
    }

    private void rebuildGlobalAfterFilterChange()
    {
        JPanel labelPanel = (JPanel) globalLabelScrollPane.getViewport().getView();
        this.globalLabels = rebuildGlobalLabelRows(labelPanel, globalAttributeSheet);
        if (attributePanel != null) attributePanel.refreshKeyFrameSelectionState();
    }

    /**
     * Populates the local-view label column based on the current
     * {@link #localRowLayout} state. Removes any previously-added label
     * rows from {@code labelPanel} (keeping the toggle button at index 0)
     * and rebuilds them. Returns the new label array.
     *
     * <p>Called from {@link #buildLabelColumn} at panel construction and
     * from {@link #onToggleLocalGroup} whenever the user clicks the
     * collapse chevron on a group parent.
     */
    private JLabel[] rebuildLocalLabelRows(JPanel labelPanel, TimeSheet bodySheet)
    {
        // Strip all label rows below the toggle button header
        // (labelPanel.getComponent(0) is the viewToggleButton).
        while (labelPanel.getComponentCount() > 1)
        {
            labelPanel.remove(1);
        }

        java.util.List<com.creatorskit.swing.timesheet.sheets.TimelineLocalRowLayout.Row> visible =
                localRowLayout.visibleRows();
        JLabel[] result = new JLabel[visible.size() + 1];
        result[0] = new JLabel(); // dummy for the +1 spacer-row index alignment

        for (int i = 0; i < visible.size(); i++)
        {
            com.creatorskit.swing.timesheet.sheets.TimelineLocalRowLayout.Row row = visible.get(i);
            boolean isParent = row.kind == com.creatorskit.swing.timesheet.sheets.TimelineLocalRowLayout.Row.Kind.GROUP_PARENT;
            String parentGroup = isParent ? row.groupName : null;
            final String groupNameFinal = parentGroup;
            JLabel label = makeRowLabel(
                    row.displayName(),
                    isParent,
                    parentGroup,
                    row.isGroupChild,
                    isParent ? () -> onToggleLocalGroup(groupNameFinal) : null,
                    isParent ? () -> localRowLayout.isCollapsed(groupNameFinal) : null);
            result[i + 1] = label;
            labelPanel.add(label);
        }

        bodySheet.setContentRowCount(visible.size());
        labelPanel.revalidate();
        labelPanel.repaint();
        bodySheet.repaint();
        return result;
    }

    /**
     * Constructs a single label row. Branches by row kind:
     * <ul>
     *   <li>{@code parentGroup != null}: collapsible group parent row.
     *       Renders a chevron prefix ({@code ▾} expanded / {@code ▸}
     *       collapsed) and fires {@code toggleAction} on click; the
     *       chevron direction is decided by {@code collapsedQuery}.</li>
     *   <li>{@code isChildOfGroup}: a leaf row that lives under a group.
     *       Renders with the parent / child padding shift so the
     *       hierarchy reads visually.</li>
     *   <li>Otherwise: a plain leaf row (existing CTRL+click +
     *       card-switch behaviour).</li>
     * </ul>
     *
     * <p>{@code toggleAction} and {@code collapsedQuery} are unused for
     * leaf rows and may be null. The local and global label-rebuild
     * paths pass their respective row-layout's toggle + isCollapsed
     * hooks here, so the same method serves both views.
     */
    private JLabel makeRowLabel(String displayName, boolean isParent, String parentGroup, boolean isChildOfGroup,
                                  Runnable toggleAction, java.util.function.BooleanSupplier collapsedQuery)
    {
        JLabel label = new JLabel();
        label.setFocusable(true);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setOpaque(true);
        label.setPreferredSize(new Dimension(LABEL_COL_WIDTH, 24));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        // 18px right padding (default) clears the AS_NEEDED vertical scrollbar
        // in the labels column. Group PARENT rows bump the right padding to
        // 30px so the parent text floats LEFT of the column edge -- child
        // rows stay at the default padding, so the children visually sit
        // RIGHTWARD of their parent. (The labels are right-aligned, so a
        // leading-space "indent" would be invisible -- the visual indent is
        // achieved by varying the right padding instead.)
        int rightPad = isParent ? 30 : 18;
        label.setBorder(new EmptyBorder(0, 4, 0, rightPad));

        if (isParent)
        {
            String chevron = collapsedQuery != null && collapsedQuery.getAsBoolean() ? "▸ " : "▾ ";
            label.setText(chevron + displayName + LABEL_OFFSET);
            label.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    super.mousePressed(e);
                    if (toggleAction != null) toggleAction.run();
                    label.requestFocusInWindow();
                }
            });
            return label;
        }

        // Leaf row. The indent for group children comes from the right
        // padding bump above, not from in-text spacing.
        label.setText(displayName + LABEL_OFFSET);
        label.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                String name = label.getText().replaceAll(LABEL_OFFSET, "").trim();
                if (e.isControlDown())
                {
                    // CTRL+click on a property label: bulk-toggle every
                    // keyframe of that property across selected Characters
                    // (or globals from the central store).
                    KeyFrameType type = null;
                    for (KeyFrameType t : KeyFrameType.values())
                    {
                        if (t.getName().equals(name)) { type = t; break; }
                    }
                    if (type != null)
                    {
                        toggleSelectAllOfProperty(type);
                        attributePanel.switchCards(name);
                    }
                    label.requestFocusInWindow();
                    return;
                }
                attributePanel.switchCards(name);
                label.requestFocusInWindow();
            }
        });
        label.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) scrollAttributePanel(1);
                if (e.getKeyCode() == KeyEvent.VK_UP) scrollAttributePanel(-1);
            }
        });
        return label;
    }

    /**
     * Toggle a local-view group's collapse state, rebuild the label
     * column, and repaint. Wired to the group parent row's mouse
     * listener.
     */
    private void onToggleLocalGroup(String groupName)
    {
        localRowLayout.toggleCollapsed(groupName);
        // Find the local label scroll pane via the labels array's first
        // component's parent. Simpler: walk attributeSheet's panel
        // ancestor chain. We stashed the JPanel inside labelScrollPane
        // when it was created; pull it back here.
        java.awt.Container labelPanel = (java.awt.Container) labelScrollPane.getViewport().getView();
        this.labels = rebuildLocalLabelRows((JPanel) labelPanel, attributeSheet);
        // The attribute panel's selected-row highlight relies on the
        // labels array; refresh it so the right label stays highlighted
        // after the rebuild.
        if (attributePanel != null) attributePanel.refreshKeyFrameSelectionState();
    }

    /**
     * Returns the visible-row index for {@code type} in the local view
     * (i.e. zero-based position in the label column ignoring the spacer
     * header). -1 if the type is globally-owned or its containing group
     * is collapsed -- callers use that as a "skip drawing" signal.
     */
    public int getLocalRowIndex(KeyFrameType type)
    {
        return localRowLayout.rowIndexOf(type);
    }

    private void setupScrollBar()
    {
        scrollBar = new JScrollBar(Adjustable.HORIZONTAL);
        scrollBar.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        scrollBar.setPreferredSize(new Dimension(0, 15));
        scrollBar.setMinimum(DEFAULT_MIN_H_SCROLL);
        scrollBar.setMaximum(DEFAULT_MAX_H_SCROLL);
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
            updateScrollBar();
            updateSheets();
        });
    }

    private void setupTimeTreeListener()
    {
        // Route selection changes through SelectionManager rather than the raw tree
        // event. TreeSelectionEvent.getPath() returns one path from a multi-path
        // change set without telling us whether it was added or removed -- so on
        // a single-character click that *replaces* a folder multi-select, getPath()
        // could be a path being deselected (the folder or one of its descendants),
        // leaving selectedCharacter pointing at the wrong Character and forcing
        // the user to click again to "fix" it. SelectionManager is the single
        // source of truth and is reliably updated from ManagerTree's own listener
        // via getSelectionPaths(). Subscribing here means selectedCharacter always
        // matches the primary, so the AttributePanel + timeline refresh in one
        // click and resolveSelectionTargets() never sees a stale multi-select.
        selectionManager.addListener(mgr -> setSelectedCharacter(mgr.getPrimary()));
    }

    public void copyKeyFrames()
    {
        copiedKeyFrames = selectedKeyFrames;
    }

    public void pasteKeyFrames()
    {
        java.util.Collection<Character> targets = resolveSelectionTargets();

        if (copiedKeyFrames == null || copiedKeyFrames.length == 0)
        {
            return;
        }

        // No-Object branch: paste any global keyframes from the clipboard
        // into the central store with their ticks re-anchored to the seeker
        // (same shift math as the per-Character path below). Non-global
        // entries in the clipboard are skipped because they need an owner.
        if (targets.isEmpty())
        {
            double firstTick = ABSOLUTE_MAX_SEQUENCE_LENGTH;
            for (KeyFrame keyFrame : copiedKeyFrames)
            {
                if (keyFrame == null || !isGlobalType(keyFrame.getKeyFrameType())) continue;
                if (keyFrame.getTick() < firstTick) firstTick = keyFrame.getTick();
            }
            KeyFrameAction[] kfa = new KeyFrameAction[0];
            KeyFrame[] reselect = new KeyFrame[0];
            for (KeyFrame keyFrame : copiedKeyFrames)
            {
                if (keyFrame == null || !isGlobalType(keyFrame.getKeyFrameType())) continue;
                double newTime = round(keyFrame.getTick() - firstTick + currentTime);
                KeyFrame copy = KeyFrame.createCopy(keyFrame, newTime);
                KeyFrame replaced = plugin.getGlobalKeyFrames().add(copy);
                reselect = ArrayUtils.add(reselect, copy);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(copy, null, KeyFrameCharacterActionType.ADD));
                if (replaced != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replaced, null, KeyFrameCharacterActionType.REMOVE));
                }
            }
            if (kfa.length > 0)
            {
                addKeyFrameActions(kfa);
                setSelectedKeyFrames(reselect);
            }
            return;
        }

        double firstTick = ABSOLUTE_MAX_SEQUENCE_LENGTH;
        for (KeyFrame keyFrame : copiedKeyFrames)
        {
            if (keyFrame.getTick() < firstTick)
            {
                firstTick = keyFrame.getTick();
            }
        }

        // selectedKeyFrames tracks the user's *active selection* in the timeline
        // (used by the marquee, Update button, etc.). Only the primary's new
        // pastes belong there -- secondary characters' copies still get added to
        // their owners' keyframe lists but aren't re-selected, otherwise the
        // selection would be a confusing mix across characters after a multi-paste.
        selectedKeyFrames = new KeyFrame[0];
        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (Character c : targets)
        {
            boolean isPrimary = (c == selectedCharacter);
            for (KeyFrame keyFrame : copiedKeyFrames)
            {
                double newTime = round(keyFrame.getTick() - firstTick + currentTime);
                KeyFrame copy = KeyFrame.createCopy(keyFrame, newTime);

                if (isPrimary)
                {
                    selectedKeyFrames = ArrayUtils.add(selectedKeyFrames, copy);
                }

                KeyFrame keyFrameToReplace = addKeyFrame(c, copy);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(copy, c, KeyFrameCharacterActionType.ADD));

                if (keyFrameToReplace != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, c, KeyFrameCharacterActionType.REMOVE));
                }
            }
        }

        addKeyFrameActions(kfa);
    }

    private void setKeyBindings()
    {
        ActionMap actionMap = getActionMap();
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0), "VK_I");
        actionMap.put("VK_I", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // No-Character branch: VK_I on a global page (Camera / Fade /
                // Shake) routes through addKeyFrameAction, which now handles
                // global types via the central store when targets are empty.
                if (selectedCharacter == null)
                {
                    KeyFrameType page = attributePanel.getSelectedKeyFramePage();
                    if (!isGlobalType(page)) return;
                    if (!attributeSheet.getBounds().contains(MouseInfo.getPointerInfo().getLocation())) return;
                    if (page == KeyFrameType.CAMERA)
                    {
                        attributePanel.captureLiveCameraIntoSpinners();
                    }
                    KeyFrame kf = attributePanel.createKeyFrame(currentTime);
                    if (kf == null) return;
                    addKeyFrameAction(new KeyFrame[]{kf});
                    setSelectedKeyFrames(new KeyFrame[]{kf});
                    return;
                }

                if (attributeSheet.getBounds().contains(MouseInfo.getPointerInfo().getLocation()))
                {
                    KeyFrame keyFrame = attributePanel.createKeyFrame(currentTime);
                    KeyFrame keyFrameToReplace = addKeyFrame(selectedCharacter, keyFrame);
                    KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(keyFrameToReplace, selectedCharacter, KeyFrameCharacterActionType.ADD)};

                    if (keyFrameToReplace != null)
                    {
                        kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
                    }
                    addKeyFrameActions(kfa);
                    return;
                }

                Component component = attributePanel.getHoveredComponent();
                if (component == null)
                {
                    return;
                }

                if (component instanceof JSpinner || component instanceof JTextField)
                {
                    if (component.isFocusOwner())
                    {
                        return;
                    }
                }

                KeyFrameType keyFrameType = attributePanel.getHoveredKeyFrameType();
                if (keyFrameType == null || keyFrameType == KeyFrameType.NULL)
                {
                    return;
                }

                KeyFrame keyFrame = attributePanel.createKeyFrame(currentTime);
                KeyFrame keyFrameToReplace = addKeyFrame(selectedCharacter, keyFrame);
                KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(keyFrame, selectedCharacter, KeyFrameCharacterActionType.ADD)};

                if (keyFrameToReplace != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
                }
                addKeyFrameActions(kfa);
            }
        });
    }

    public void onSelectAllPressed()
    {
        java.util.Collection<Character> targets = resolveSelectionTargets();

        KeyFrame[] all = new KeyFrame[0];
        for (Character c : targets)
        {
            all = ArrayUtils.addAll(all, c.getAllKeyFrames());
        }

        // Globals live in the central store and aren't reachable via
        // Character.getAllKeyFrames (their per-Character frames[] slots are
        // null post-Phase-2). Append them so CTRL+A picks them up too.
        com.creatorskit.saves.GlobalKeyFrames store = plugin.getGlobalKeyFrames();
        if (store != null)
        {
            for (KeyFrameType type : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
            {
                all = ArrayUtils.addAll(all, store.getGlobalKeyFramesByType(type));
            }
        }

        setSelectedKeyFrames(all);
    }

    public void onDeleteKeyPressed()
    {
        // Block selection takes precedence: if the user clicked a block
        // and then hit Delete, treat it as "delete the block + its
        // keyframes" with a confirmation (matches the right-click menu).
        if (selectedBlocks != null && !selectedBlocks.isEmpty())
        {
            for (int i = 0; i < selectedBlocks.size(); i++)
            {
                com.creatorskit.swing.timesheet.keyframe.Block b = selectedBlocks.get(i);
                Character owner = i < selectedBlockOwners.size() ? selectedBlockOwners.get(i) : null;
                if (owner != null && b != null)
                {
                    deleteBlockWithConfirm(owner, b);
                }
            }
            return;
        }

        if (selectedKeyFrames == null || selectedKeyFrames.length == 0)
        {
            return;
        }

        java.util.Collection<Character> targets = resolveSelectionTargets();
        // Don't early-return on empty targets -- the marquee may hold global
        // keyframes (Camera / Fade / Shake) which live in the central store
        // and have no Character owner. Process them below regardless.

        java.util.Set<String> processed = new java.util.HashSet<>();
        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (KeyFrame markedKf : selectedKeyFrames)
        {
            if (markedKf == null)
            {
                continue;
            }
            KeyFrameType type = markedKf.getKeyFrameType();
            double tick = markedKf.getTick();
            String dedupeKey = type.name() + "@" + tick;
            if (!processed.add(dedupeKey))
            {
                continue;
            }

            if (isGlobalType(type))
            {
                // Globals: one removal per (type, tick) regardless of how many
                // Objects are selected -- the central store holds a single
                // instance per tick. Skip the per-character fan-out below.
                KeyFrame storeKf = findGlobalKeyFrameAt(type, tick);
                if (storeKf != null)
                {
                    plugin.getGlobalKeyFrames().remove(storeKf);
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(storeKf, null, KeyFrameCharacterActionType.REMOVE));
                }
                continue;
            }

            // Per-Character types: fan out across every selected Object.
            for (Character c : targets)
            {
                KeyFrame kf = c.findKeyFrame(type, tick);
                if (kf == null)
                {
                    continue;
                }
                removeKeyFrame(c, kf);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, c, KeyFrameCharacterActionType.REMOVE));
            }
        }

        addKeyFrameActions(kfa);
        // Drop the freshly-deleted keyframes from the marquee so the card
        // collapses back to its placeholder / no-selection state instead of
        // editing dangling references.
        setSelectedKeyFrames(new KeyFrame[0]);
    }

    public void skipListener(double modifier)
    {
        setCurrentTime(round(currentTime + modifier), false);
    }

    private void setMouseListeners()
    {
        addMouseWheelListener(new MouseAdapter()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                super.mouseWheelMoved(e);
                if (e.isControlDown())
                {
                    if (e.isAltDown() || e.isShiftDown())
                    {
                        return;
                    }

                    managerTree.scrollSelectedIndex(e.getWheelRotation());
                    return;
                }

                if (e.isShiftDown())
                {
                    if (e.isControlDown() || e.isAltDown())
                    {
                        return;
                    }

                    scrollAttributePanel(e.getWheelRotation() * 2);
                }
            }
        });
    }

    public void scrollAttributePanel(int direction)
    {
        int index = KeyFrameType.getIndex(attributePanel.getSelectedKeyFramePage()) + direction;
        int totalFrameTypes = KeyFrameType.getTotalFrameTypes();
        if (index >= totalFrameTypes)
        {
            index = 0;
        }

        if (index == -1)
        {
            index = totalFrameTypes - 1;
        }

        attributePanel.switchCards(KeyFrameType.getKeyFrameType(index).toString());
    }

    /**
     * Lays out the four major sections of the timeline view as nested
     * JSplitPanes so the user can drag the dividers to resize each one
     * independently. Each section is wrapped in a JPanel with a visible
     * LineBorder (and the dividers themselves are painted in a contrast
     * colour) so the boundaries are obvious at a glance.
     *
     * <pre>
     * +----------------+----------------------------+
     * |                |                            |
     * |  Tree section  |  Summary section           |
     * |                |                            |
     * +- - - - - - - - +- - - - - - - - - - - - - -+
     * |                |                            |
     * |  Attribute     |  Timeline body section     |
     * |  section       |  (scrollbar + controls +   |
     * |                |   labels | sheet)          |
     * |                |                            |
     * +----------------+----------------------------+
     *  ^ leftSplit       ^ rightSplit
     *      (vertical)        (vertical)
     *  Both wrapped in a horizontal rootSplit.
     * </pre>
     *
     * GridBag is still used inside individual sections (controlPanel etc.);
     * only the OUTER layout switched to split panes.
     */
    private void setupManager()
    {
        // Wrap each major component in a bordered panel so the section
        // boundaries read as actual UI regions instead of just gaps.
        JPanel treeSection = wrapInSection(treeScrollPane);
        JPanel attrSection = wrapInSection(attributePanel);

        JPanel summarySection = new JPanel(new BorderLayout());
        summarySection.setBorder(SECTION_BORDER);
        summarySection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JLabel summaryLabel = new JLabel("Summary");
        summaryLabel.setFont(FontManager.getRunescapeBoldFont());
        summaryLabel.setBorder(new EmptyBorder(1, 4, 2, 2));
        summarySection.add(summaryLabel, BorderLayout.NORTH);
        summarySection.add(summarySheet, BorderLayout.CENTER);

        // Timeline section: horizontal scrollbar + transport controls
        // stack on top of the labels|sheet body. Keeping them inside the
        // same section means the bottom-right divider drag resizes the
        // whole timeline as a unit (scrollbar + controls + rows).
        JPanel timelineSection = new JPanel(new BorderLayout());
        timelineSection.setBorder(SECTION_BORDER);
        timelineSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel timelineHeader = new JPanel(new BorderLayout());
        timelineHeader.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        timelineHeader.add(scrollBar, BorderLayout.NORTH);
        timelineHeader.add(controlPanel, BorderLayout.CENTER);
        timelineSection.add(timelineHeader, BorderLayout.NORTH);

        JPanel timelineBody = new JPanel(new BorderLayout());
        timelineBody.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        timelineBody.add(labelCards, BorderLayout.WEST);
        timelineBody.add(sheetCards, BorderLayout.CENTER);
        timelineSection.add(timelineBody, BorderLayout.CENTER);

        // Section minimum sizes drive the JSplitPane's drag clamp. Children
        // like attributePanel naturally have a large preferred (and thus
        // minimum) width because of their internal layouts -- without
        // overriding here, the splits won't let the user shrink past those
        // values. Tiny minimums = full drag freedom; if the user drags too
        // narrow some content clips, which is their explicit choice.
        treeSection.setMinimumSize(SECTION_MIN);
        attrSection.setMinimumSize(SECTION_MIN);
        summarySection.setMinimumSize(SECTION_MIN);
        timelineSection.setMinimumSize(SECTION_MIN);

        // Three nested split panes. Default proportions from the user spec:
        //   rootSplit  (horizontal):  left 30% | right 70%
        //   leftSplit  (vertical):    tree 70% / attr 30%
        //   rightSplit (vertical):    summary 30% / timeline 70%
        // Persistence keys + defaults are wired in makeSplit -- divider
        // positions saved on drag and restored on next launch.
        JSplitPane leftSplit  = makeSplit(JSplitPane.VERTICAL_SPLIT,   treeSection,    attrSection,     0.7, 0.7, DIV_KEY_LEFT);
        JSplitPane rightSplit = makeSplit(JSplitPane.VERTICAL_SPLIT,   summarySection, timelineSection, 0.3, 0.3, DIV_KEY_RIGHT);
        JSplitPane rootSplit  = makeSplit(JSplitPane.HORIZONTAL_SPLIT, leftSplit,      rightSplit,      0.3, 0.3, DIV_KEY_ROOT);

        add(rootSplit, BorderLayout.CENTER);
    }

    /** Tiny absolute minimum so split-pane drag has full range. ~60 px
     *  on each axis is small enough that the user can crush a section
     *  to almost nothing; their problem if they can't read the inside. */
    private static final Dimension SECTION_MIN = new Dimension(60, 60);

    /** Config keys for divider-location persistence. Stored as fractions
     *  (0..1) so they scale when the toolbox window size changes. */
    private static final String DIV_KEY_ROOT  = "timelineDivider_root";
    private static final String DIV_KEY_LEFT  = "timelineDivider_left";
    private static final String DIV_KEY_RIGHT = "timelineDivider_right";
    private static final String DIV_CFG_GROUP = "creatorssuite";

    /** Wraps any component in a fixed-border section panel so the user
     *  can see where one section ends and another begins, even before
     *  they touch a divider. */
    private static JPanel wrapInSection(JComponent inner)
    {
        JPanel section = new JPanel(new BorderLayout());
        section.setBorder(SECTION_BORDER);
        section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        section.add(inner, BorderLayout.CENTER);
        return section;
    }

    /** Builds a JSplitPane styled to be obvious: a fat divider painted in
     *  the medium-grey accent with a "grip" of dots in the middle so the
     *  user reads it as a draggable boundary, not just a thin line.
     *
     *  Initial divider position = {@code persistKey}'s saved fraction
     *  (read via ConfigManager) or {@code defaultFraction} if no saved
     *  value exists. After the first paint, drag events save the new
     *  fraction back to ConfigManager so positions survive restarts. */
    private JSplitPane makeSplit(final int orientation, JComponent a, JComponent b,
                                 double resizeWeight, double defaultFraction, String persistKey)
    {
        JSplitPane split = new JSplitPane(orientation, a, b);
        split.setBorder(null);
        split.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        split.setDividerSize(SPLIT_DIVIDER_SIZE);
        split.setResizeWeight(resizeWeight);
        split.setContinuousLayout(true);
        installDividerPersistence(split, persistKey, defaultFraction);
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI()
        {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider()
            {
                javax.swing.plaf.basic.BasicSplitPaneDivider divider =
                        new javax.swing.plaf.basic.BasicSplitPaneDivider(this)
                {
                    @Override
                    public void paint(Graphics g)
                    {
                        // Solid contrast bar so the divider stays visible on
                        // the dark theme; default L&F painting blends in.
                        g.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        // 5-dot grip in the centre so the divider visually
                        // reads as a drag handle, not just a coloured stripe.
                        g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        if (orientation == JSplitPane.HORIZONTAL_SPLIT)
                        {
                            for (int dy = -8; dy <= 8; dy += 4)
                            {
                                g.fillRect(cx - 1, cy + dy - 1, 2, 2);
                            }
                        }
                        else
                        {
                            for (int dx = -8; dx <= 8; dx += 4)
                            {
                                g.fillRect(cx + dx - 1, cy - 1, 2, 2);
                            }
                        }
                    }
                };
                // Crosshair cursor when hovering the divider -- makes it
                // unambiguous that this is a drag target.
                divider.setCursor(Cursor.getPredefinedCursor(
                        orientation == JSplitPane.HORIZONTAL_SPLIT
                                ? Cursor.E_RESIZE_CURSOR
                                : Cursor.N_RESIZE_CURSOR));
                return divider;
            }
        });
        return split;
    }

    /** Thickness of every split divider. 10px is chunky enough to grab
     *  comfortably with the mouse and leaves room for the grip dots. */
    private static final int SPLIT_DIVIDER_SIZE = 10;

    /** Width of the property-labels column on the left of the timeline
     *  body. Was 100px; bumped so "Manage global" fits in the toggle
     *  without truncating to "Manage glo...". The 18px right padding on
     *  each label + the bold font on the toggle both eat into the usable
     *  text area. */
    private static final int LABEL_COL_WIDTH = 150;

    /**
     * Wires divider-position persistence onto a split pane:
     *   - On first show, restores the saved fraction (or {@code defaultFraction}
     *     if none) via {@link JSplitPane#setDividerLocation(double)}. Has to
     *     wait for the split pane to have a non-zero size before the call
     *     works -- HierarchyListener on SHOWING_CHANGED is the standard hook.
     *   - After the initial restore, every drag fires the listener which
     *     saves the new fraction to ConfigManager under {@code persistKey}.
     *     The listener is intentionally attached AFTER the first restore so
     *     the restore itself doesn't echo the default value back to disk.
     * Stored as a fraction (0..1) of the relevant axis so positions stay
     * proportional if the toolbox window is later resized to a different
     * dimension between sessions.
     */
    private void installDividerPersistence(JSplitPane split, String persistKey, double defaultFraction)
    {
        final double startFraction = loadDividerFraction(persistKey, defaultFraction);
        split.addHierarchyListener(new java.awt.event.HierarchyListener()
        {
            @Override
            public void hierarchyChanged(java.awt.event.HierarchyEvent e)
            {
                if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) == 0) return;
                if (!split.isShowing()) return;
                split.removeHierarchyListener(this);
                // Defer to invokeLater so the split pane's actual width/height
                // is set before setDividerLocation(double) computes pixels
                // from the fraction. Calling it from the listener directly
                // can race with the layout pass.
                javax.swing.SwingUtilities.invokeLater(() ->
                {
                    split.setDividerLocation(startFraction);
                    // Attach the save listener AFTER the restore so the
                    // restore-driven property change doesn't trigger a save
                    // of the default value. Subsequent drags persist normally.
                    split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt ->
                    {
                        if (plugin == null || plugin.getConfigManager() == null) return;
                        int loc = ((Number) evt.getNewValue()).intValue();
                        int size = split.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                                ? split.getWidth() : split.getHeight();
                        if (size <= 0) return;
                        double fraction = Math.max(0.0, Math.min(1.0, (double) loc / size));
                        plugin.getConfigManager().setConfiguration(
                                DIV_CFG_GROUP, persistKey, Double.toString(fraction));
                    });
                });
            }
        });
    }

    private double loadDividerFraction(String key, double defaultFraction)
    {
        if (plugin == null || plugin.getConfigManager() == null) return defaultFraction;
        String stored = plugin.getConfigManager().getConfiguration(DIV_CFG_GROUP, key);
        if (stored == null || stored.isEmpty()) return defaultFraction;
        try
        {
            double f = Double.parseDouble(stored);
            return (f > 0.0 && f < 1.0) ? f : defaultFraction;
        }
        catch (NumberFormatException ex)
        {
            return defaultFraction;
        }
    }
    /** 1px line in MEDIUM_GRAY -- contrasts against the DARKER_GRAY panel
     *  background so each section reads as a discrete region. */
    private static final javax.swing.border.Border SECTION_BORDER =
            new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1);

    private void updateSheets()
    {
        summarySheet.setHScroll(hScroll);
        summarySheet.setZoom(zoom);
        attributeSheet.setHScroll(hScroll);
        attributeSheet.setZoom(zoom);
        if (globalAttributeSheet != null)
        {
            globalAttributeSheet.setHScroll(hScroll);
            globalAttributeSheet.setZoom(zoom);
        }
    }

    private void updateScrollBar()
    {
        pauseScrollBarListener = true;
        scrollBar.setMinimum((int) minHScroll);
        scrollBar.setMaximum((int) maxHScroll);
        scrollBar.setBlockIncrement((int) (zoom / 5));
        scrollBar.setUnitIncrement((int) (zoom / 50));
        scrollBar.setVisibleAmount((int) (zoom));
        scrollBar.setValue((int) -hScroll);
        pauseScrollBarListener = false;
    }

    /**
     * Rounds the given value to the nearest 1/10th
     * @param value the value to round
     * @return the value, rounded to 1 decimal place
     */
    public static double round (double value)
    {
        int scale = (int) Math.pow(10, 1);
        return (double) Math.round(value * scale) / scale;
    }

    /**
     * Tools > Random > Jitter. Pops a dialog asking for a keyframe type, a max
     * ± delta, and a step size; then for every currently-selected Character
     * iterates every keyframe of that type and shifts its tick by a random
     * multiple of step in [-max, +max]. Step size matters because OSRS ticks
     * are naturally integer-valued -- a sub-step like 0.3 produces ticks that
     * don't line up with the game's clock and feel "off". Default step is 1.0;
     * drop to 0.5 / 0.25 for finer effects, or push higher (e.g. 5) for big
     * staggered groupings. Each shift is wrapped in REMOVE+ADD KeyFrameActions
     * so the whole batch is one undo step. Use case: emulating rain -- a folder
     * full of raindrop Characters all sharing the same spawn tick will fall as
     * a flat sheet; jittering their spawn ticks by ±5 in steps of 1 gives a
     * natural rainfall.
     */
    public void showJitterDialog()
    {
        if (selectedKeyFrames == null || selectedKeyFrames.length == 0)
        {
            JOptionPane.showMessageDialog(this, "No keyframes selected. Marquee or click keyframes first.", "Jitter", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.Map<Character, java.util.List<KeyFrame>> byOwner = groupSelectedKeyFramesByOwner();
        if (byOwner.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Couldn't resolve any owners for the selected keyframes.", "Jitter", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JSpinner deltaSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.5));
        JSpinner stepSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.1));

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Max delta (+/- ticks):"));
        panel.add(deltaSpinner);
        panel.add(new JLabel("Step size (ticks):"));
        panel.add(stepSpinner);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Jitter " + selectedKeyFrames.length + " keyframes across " + byOwner.size() + " Characters",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        final double maxDelta = ((Number) deltaSpinner.getValue()).doubleValue();
        final double step = ((Number) stepSpinner.getValue()).doubleValue();
        if (maxDelta <= 0 || step <= 0) return;

        // Pre-validate every Character. Allowed delta range per Character is
        // [-min(selectedTick), MAX - max(selectedTick)] intersected with
        // [-maxDelta, +maxDelta], discretised to step buckets. If any Character
        // has no valid bucket, refuse the entire op -- partial work would leave
        // mismatched state with no clean undo.
        java.util.Map<Character, int[]> bucketRanges = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            double minTick = Double.POSITIVE_INFINITY;
            double maxTick = Double.NEGATIVE_INFINITY;
            for (KeyFrame kf : entry.getValue())
            {
                if (kf.getTick() < minTick) minTick = kf.getTick();
                if (kf.getTick() > maxTick) maxTick = kf.getTick();
            }

            double minAllowed = Math.max(-maxDelta, -minTick);
            double maxAllowed = Math.min(maxDelta, ABSOLUTE_MAX_SEQUENCE_LENGTH - maxTick);
            int minBucket = (int) Math.ceil(minAllowed / step);
            int maxBucket = (int) Math.floor(maxAllowed / step);

            if (minBucket > maxBucket)
            {
                JOptionPane.showMessageDialog(this,
                        "Cannot jitter: Character '" + owner.getName() + "' has no valid step-aligned delta within [" + minAllowed + ", " + maxAllowed + "] for step " + step + ".",
                        "Jitter blocked", JOptionPane.WARNING_MESSAGE);
                return;
            }
            bucketRanges.put(owner, new int[]{minBucket, maxBucket});
        }

        // All Characters validated. Apply: one rng draw per Character,
        // broadcast the same delta to every selected keyframe of that Character.
        final java.util.Random rng = new java.util.Random();
        java.util.List<KeyFrame> newSelected = new ArrayList<>();
        KeyFrameAction[] kfa = new KeyFrameAction[0];

        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            int[] range = bucketRanges.get(owner);
            int n = rng.nextInt(range[1] - range[0] + 1) + range[0];
            double delta = n * step;

            // delta=0 means this Character's keyframes don't move at all. Keep
            // the originals selected so the marquee survives intact, no actions
            // needed.
            if (delta == 0.0)
            {
                newSelected.addAll(entry.getValue());
                continue;
            }

            for (KeyFrame kf : entry.getValue())
            {
                double newTick = round(kf.getTick() + delta);
                KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, owner, KeyFrameCharacterActionType.ADD));
                owner.removeKeyFrame(kf);
                owner.addKeyFrame(replacement, currentTime);
                newSelected.add(replacement);
            }
        }

        finalizeTickTransform(kfa, newSelected.toArray(new KeyFrame[0]));
    }

    /**
     * Tools > Random > Scatter. Same dialog shape as Jitter (now with step) but
     * SETs each matching keyframe's tick to a step-aligned random value in
     * [from, to] instead of nudging it by a delta. Anchoring picks to the step
     * grid keeps the tick values clean -- e.g. step=1 in [0, 20] gives integer
     * ticks 0..20 only, not 12.7. Use case: scattering an event (hitsplats,
     * crowd reactions) across a time window with predictable granularity.
     */
    public void showScatterDialog()
    {
        if (selectedKeyFrames == null || selectedKeyFrames.length == 0)
        {
            JOptionPane.showMessageDialog(this, "No keyframes selected. Marquee or click keyframes first.", "Scatter", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.Map<Character, java.util.List<KeyFrame>> byOwner = groupSelectedKeyFramesByOwner();
        if (byOwner.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Couldn't resolve any owners for the selected keyframes.", "Scatter", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JSpinner fromSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.5));
        JSpinner toSpinner = new JSpinner(new SpinnerNumberModel(20.0, 0.0, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.5));
        // Step size is also a min..max range now. Each Character independently
        // rolls a whole-tick step in [stepMin, stepMax] which then drives that
        // Character's anchor grid. Default 1..1 matches the previous fixed
        // step=1 UX; widen the range for varied per-Character cadence (e.g.
        // step 1..3 = some raindrops on a tighter rhythm than others).
        JSpinner stepMinSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        JSpinner stepMaxSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        // Copies per Character (per-Character mode only). min == max behaves
        // like a fixed count; min == 0 lets a Character end up with its
        // original block simply deleted and zero copies added.
        JSpinner copiesMinSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1000, 1));
        JSpinner copiesMaxSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1000, 1));
        // Per-step copy count (per-Step mode only). Per step tick the
        // planner rolls K in [perStepMin, perStepMax] and picks K distinct
        // Characters (without replacement, clamped to pool size + collision
        // filter) -- each gets a copy of the source block anchored at that
        // step tick.
        JSpinner perStepMinSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1000, 1));
        JSpinner perStepMaxSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1000, 1));

        // Explicit mode toggle replaces the old "perStepMax == 0 = off"
        // sentinel. Per-Character and Per-Step are conceptually different
        // planners (one rolls per-Character independently, the other
        // walks a global step grid), so the dialog has to make the choice
        // explicit -- the previous magic-zero pattern was both invisible
        // and let the user accidentally fall back to per-Character mode by
        // bumping perStepMax to 0 mid-edit.
        JRadioButton perCharRadio = new JRadioButton("Per Character", true);
        JRadioButton perStepRadio = new JRadioButton("Per Step", false);
        perCharRadio.setToolTipText("Each selected Character independently rolls a copy count and places its block at random anchors across the range.");
        perStepRadio.setToolTipText("Walks step ticks across the range; at each step picks K random Characters from the multi-selection -- each gets one copy of the source block anchored at that tick.");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(perCharRadio);
        modeGroup.add(perStepRadio);

        // Group-mode toggle: treats every Character under each DIRECTLY-
        // selected folder as one rigid block. Block duration = max - min
        // tick across the union of every member's selected keyframes;
        // every scatter copy applies one shared delta to all of them so
        // tightly-choreographed multi-Character effects (e.g. a damage
        // ring + projectile + fire sequence) stay in lockstep through
        // the randomisation.
        //
        // Auto-checked when the user has directly clicked a folder in the
        // tree (so the common "I selected the folder, now Scatter it"
        // path Just Works) and disabled otherwise -- we don't want a stale
        // Group toggle silently grouping siblings the user multi-selected
        // by Character. Tooltip explains the disabled state.
        Folder[] directFolders = managerTree != null ? managerTree.getDirectlySelectedFolders() : new Folder[0];
        boolean folderModeAvailable = directFolders.length > 0;
        JCheckBox groupModeCheck = new JCheckBox("Group mode (treat each selected folder as one rigid block)");
        groupModeCheck.setEnabled(folderModeAvailable);
        groupModeCheck.setSelected(folderModeAvailable);
        if (folderModeAvailable)
        {
            String list;
            if (directFolders.length == 1)
            {
                list = "\"" + directFolders[0].getName() + "\"";
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < directFolders.length; i++)
                {
                    if (i > 0) sb.append(", ");
                    sb.append("\"").append(directFolders[i].getName()).append("\"");
                }
                list = sb.toString();
            }
            groupModeCheck.setToolTipText("<html>Apply one shared shift to every Character in " + list + ".</html>");
        }
        else
        {
            groupModeCheck.setToolTipText("Click a folder in the manager tree first to enable group mode.");
        }

        // Greys out the spinner pair that doesn't apply to the current mode.
        // Visual cue + cannot-misclick safety. Step / range spinners are
        // used by both modes so they stay live.
        Runnable refreshEnabled = () ->
        {
            boolean group = groupModeCheck.isSelected();
            boolean perStep = perStepRadio.isSelected();
            // Group mode is incompatible with Per Step -- the per-step
            // planner picks K Characters per step, but in group mode every
            // member moves together so "picking K members" is meaningless.
            // Lock the Per-Step radio off while group mode is active.
            perCharRadio.setEnabled(!group);
            perStepRadio.setEnabled(!group);
            if (group && perStepRadio.isSelected())
            {
                perCharRadio.setSelected(true);
                perStep = false;
            }
            copiesMinSpinner.setEnabled(!perStep);
            copiesMaxSpinner.setEnabled(!perStep);
            perStepMinSpinner.setEnabled(!group && perStep);
            perStepMaxSpinner.setEnabled(!group && perStep);
        };
        perCharRadio.addActionListener(e -> refreshEnabled.run());
        perStepRadio.addActionListener(e -> refreshEnabled.run());
        groupModeCheck.addActionListener(e -> refreshEnabled.run());
        refreshEnabled.run();

        JPanel modePanel = new JPanel();
        modePanel.setLayout(new BoxLayout(modePanel, BoxLayout.Y_AXIS));
        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modeRow.add(new JLabel("Mode:"));
        modeRow.add(perCharRadio);
        modeRow.add(perStepRadio);
        modePanel.add(modeRow);
        JPanel groupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        groupRow.add(groupModeCheck);
        modePanel.add(groupRow);

        JPanel spinners = new JPanel(new GridLayout(0, 2, 6, 6));
        spinners.add(new JLabel("From tick:"));
        spinners.add(fromSpinner);
        spinners.add(new JLabel("To tick:"));
        spinners.add(toSpinner);
        spinners.add(new JLabel("Step size (min):"));
        spinners.add(stepMinSpinner);
        spinners.add(new JLabel("Step size (max):"));
        spinners.add(stepMaxSpinner);
        spinners.add(new JLabel("Copies per Character (min):"));
        spinners.add(copiesMinSpinner);
        spinners.add(new JLabel("Copies per Character (max):"));
        spinners.add(copiesMaxSpinner);
        spinners.add(new JLabel("Copies per step (min):"));
        spinners.add(perStepMinSpinner);
        spinners.add(new JLabel("Copies per step (max):"));
        spinners.add(perStepMaxSpinner);
        // Per-step mode draws from the multi-selected Characters in the
        // manager, NOT just the owners of the selected source kfs.
        JLabel perStepHint = new JLabel("<html><i>(per-step picks from multi-selected Characters in the manager;<br>"
                + "a Character is skipped at a step if its block would overlap one it already has)</i></html>");
        perStepHint.setFont(perStepHint.getFont().deriveFont(perStepHint.getFont().getSize2D() - 1f));
        spinners.add(new JLabel());
        spinners.add(perStepHint);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(modePanel, BorderLayout.NORTH);
        panel.add(spinners, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Scatter " + selectedKeyFrames.length + " keyframes across " + byOwner.size() + " Characters",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        double from = ((Number) fromSpinner.getValue()).doubleValue();
        double to = ((Number) toSpinner.getValue()).doubleValue();
        if (to < from) { double tmp = from; from = to; to = tmp; }
        int stepMinRaw = ((Number) stepMinSpinner.getValue()).intValue();
        int stepMaxRaw = ((Number) stepMaxSpinner.getValue()).intValue();
        int copiesMinRaw = ((Number) copiesMinSpinner.getValue()).intValue();
        int copiesMaxRaw = ((Number) copiesMaxSpinner.getValue()).intValue();
        int perStepMinRaw = ((Number) perStepMinSpinner.getValue()).intValue();
        int perStepMaxRaw = ((Number) perStepMaxSpinner.getValue()).intValue();
        if (stepMinRaw < 1 || stepMaxRaw < 1 || copiesMinRaw < 0 || copiesMaxRaw < 0
                || perStepMinRaw < 0 || perStepMaxRaw < 0) return;
        if (stepMinRaw > stepMaxRaw)
        {
            JOptionPane.showMessageDialog(this,
                    "Step size (min) must be less than or equal to Step size (max).",
                    "Scatter blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (copiesMinRaw > copiesMaxRaw)
        {
            JOptionPane.showMessageDialog(this,
                    "Copies (min) must be less than or equal to Copies (max).",
                    "Scatter blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (perStepMinRaw > perStepMaxRaw)
        {
            JOptionPane.showMessageDialog(this,
                    "Copies per step (min) must be less than or equal to Copies per step (max).",
                    "Scatter blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final int stepMin = stepMinRaw;
        final int stepMax = stepMaxRaw;
        final int copiesMin = copiesMinRaw;
        final int copiesMax = copiesMaxRaw;
        final int perStepMin = perStepMinRaw;
        final int perStepMax = perStepMaxRaw;
        final double fFrom = from;
        final double fTo = to;

        // Branch to the per-step planner when the user selected it. The three
        // modes are conceptually different planners (per-Character: each char
        // rolls independently; per-Step: walk a global step grid and pick K
        // chars per step; per-Group: treat the directly-selected folder as
        // one rigid block) so each lives in its own method instead of trying
        // to overload one feasibility loop.
        if (groupModeCheck.isSelected())
        {
            scatterPerGroup(byOwner, directFolders, fFrom, fTo, stepMin, stepMax, copiesMin, copiesMax);
            return;
        }
        if (perStepRadio.isSelected())
        {
            scatterPerStep(byOwner, fFrom, fTo, stepMin, stepMax, perStepMin, perStepMax);
            return;
        }

        // Pre-validate every Character against the WORST case of the random
        // roll: copiesMax copies at stepMax (largest step = fewest anchor
        // slots = hardest to fit non-overlapping copies). theoreticalMax
        // shrinks as step grows since the minimum anchor gap (duration + step)
        // gets larger. If copiesMax <= theoreticalMax_at_stepMax, then any
        // rolled (step, copies) pair where step <= stepMax and copies <=
        // copiesMax is guaranteed feasible -- no surprise mid-plan failures.
        // Refuses the whole op on first failure with an actionable message.
        // Slots are NOT stored in placements -- the planner re-derives them
        // per-Character from the rolled step, so a "worst case at stepMax"
        // computed here would be wrong for the lucky cases.
        java.util.Map<Character, double[]> placements = new java.util.LinkedHashMap<>(); // owner -> {blockMinTick, duration}
        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            double minTick = Double.POSITIVE_INFINITY;
            double maxTick = Double.NEGATIVE_INFINITY;
            for (KeyFrame kf : entry.getValue())
            {
                if (kf.getTick() < minTick) minTick = kf.getTick();
                if (kf.getTick() > maxTick) maxTick = kf.getTick();
            }
            double duration = maxTick - minTick;

            // copiesMax == 0 means every Character will just delete its
            // originals. Skip fit checks entirely.
            if (copiesMax > 0)
            {
                if (duration > (fTo - fFrom))
                {
                    JOptionPane.showMessageDialog(this,
                            "Cannot scatter: Character '" + owner.getName() + "' block spans " + duration + " ticks which is larger than the range " + (fTo - fFrom) + ".",
                            "Scatter blocked", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Theoretical max non-overlap copies at the WORST step: anchors
                // must differ by > duration, minimum gap on the grid is
                // (duration + stepMax). If copiesMax fits here, every rolled
                // step in [stepMin, stepMax] gives even more slots and fits.
                double anchorRange = fTo - fFrom - duration;
                int theoreticalMaxAtStepMax = (int) Math.floor(anchorRange / (duration + stepMax)) + 1;
                if (copiesMax > theoreticalMaxAtStepMax)
                {
                    JOptionPane.showMessageDialog(this,
                            "Cannot scatter: Character '" + owner.getName() + "' has room for at most " + theoreticalMaxAtStepMax + " non-overlapping copies at step " + stepMax + " (block duration " + duration + " in range " + (fTo - fFrom) + "). Reduce 'Copies (max)' to " + theoreticalMaxAtStepMax + " or below, reduce 'Step size (max)', widen the range, or shrink the selection block.",
                            "Scatter blocked", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            placements.put(owner, new double[]{minTick, duration});
        }

        // Phase 1: PLAN every Character's anchors using greedy collision-aware
        // selection. We treat each copy as an interval [anchor, anchor + D]
        // where D is the widest spread between the earliest and latest keyframe
        // of this Character's selection (regardless of keyframe type). Two
        // copies "collide" if their intervals overlap at all -- i.e. their
        // anchors are within D of each other. Tick-equality-only checking
        // (the previous attempt) misses this: blocks with sparse keyframes
        // like {tick 1, tick 6} (D=5) could be placed at anchors 0 and 3 with
        // no tick collision, yet the rain effects [0..5] and [3..8] visibly
        // overlap on the tile.
        //
        // No mutation in this phase -- if any Character runs out of valid
        // anchors mid-plan we refuse the whole op so the user gets a clean
        // error instead of partial scatter to undo manually. Cross-Character
        // collisions are still ignored (different objects, different timelines,
        // no visual conflict).
        final java.util.Random rng = new java.util.Random();
        java.util.Map<Character, double[]> plannedAnchors = new java.util.LinkedHashMap<>();

        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            double[] p = placements.get(owner);
            double blockDuration = p[1];

            // Roll this Character's copy count and step independently. min ==
            // max in either gives a fixed value (matches the pre-randomisation
            // UX). Step drives the anchor grid for this Character only --
            // different Characters can use different steps, which is exactly
            // the rain-rhythm variation we want.
            int rolledN = copiesMin == copiesMax
                    ? copiesMin
                    : copiesMin + rng.nextInt(copiesMax - copiesMin + 1);
            int rolledStep = stepMin == stepMax
                    ? stepMin
                    : stepMin + rng.nextInt(stepMax - stepMin + 1);
            final double charStep = rolledStep;

            // rolledN == 0 means delete this Character's selection without
            // adding anything. Plan an empty anchor array; commit phase will
            // still remove the originals.
            if (rolledN == 0)
            {
                plannedAnchors.put(owner, new double[0]);
                continue;
            }

            // Anchor slots for THIS Character's rolled step. Pre-validation
            // checked stepMax fits copiesMax, so any rolled step gives at
            // least as many slots and copiesMax always fits.
            int totalSlots = (int) Math.floor((fTo - fFrom - blockDuration) / charStep) + 1;

            // Greedy random anchor selection: each iteration enumerates every
            // step-aligned anchor whose interval [anchor, anchor + D] doesn't
            // overlap any previously chosen interval. Greedy with random pick
            // CAN paint itself into a corner (early choices fragment the
            // remaining space so a later iteration can't fit even though a
            // valid arrangement exists), so retry up to MAX_RETRIES with fresh
            // RNG sequences before refusing. copiesMax was pre-validated to be
            // <= theoretical max at stepMax, so a valid arrangement always
            // exists for any rolledN <= copiesMax with any step <= stepMax --
            // the retries are just to escape unlucky random fragmentation.
            final int MAX_RETRIES = 20;
            double[] anchors = null;
            for (int attempt = 0; attempt < MAX_RETRIES && anchors == null; attempt++)
            {
                double[] trial = new double[rolledN];
                boolean failed = false;
                for (int c = 0; c < rolledN; c++)
                {
                    java.util.List<Integer> validSlots = new ArrayList<>();
                    for (int s = 0; s < totalSlots; s++)
                    {
                        double candidate = fFrom + s * charStep;
                        boolean ok = true;
                        for (int prior = 0; prior < c; prior++)
                        {
                            if (Math.abs(candidate - trial[prior]) <= blockDuration)
                            {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) validSlots.add(s);
                    }
                    if (validSlots.isEmpty())
                    {
                        failed = true;
                        break;
                    }
                    int pickedSlot = validSlots.get(rng.nextInt(validSlots.size()));
                    trial[c] = fFrom + pickedSlot * charStep;
                }
                if (!failed) anchors = trial;
            }

            if (anchors == null)
            {
                JOptionPane.showMessageDialog(this,
                        "Cannot scatter " + rolledN + " non-overlapping copies for Character '" + owner.getName() + "' at step " + rolledStep + " after " + MAX_RETRIES + " attempts (block duration " + blockDuration + " in range " + (fTo - fFrom) + " ticks). The space is tight enough that random anchor choices keep fragmenting it -- widen the [from, to] range, lower 'Step size (max)', or lower 'Copies (max)'.",
                        "Scatter blocked", JOptionPane.WARNING_MESSAGE);
                return;
            }

            plannedAnchors.put(owner, anchors);
        }

        // Phase 2: COMMIT. Every Character planned successfully -- mutate now.
        // Remove originals once, then add the N planned copies per owner.
        KeyFrameAction[] kfa = new KeyFrameAction[0];
        java.util.List<KeyFrame> newSelected = new ArrayList<>();

        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            java.util.List<KeyFrame> originalBlock = entry.getValue();
            double blockMin = placements.get(owner)[0];
            double[] anchors = plannedAnchors.get(owner);

            for (KeyFrame kf : originalBlock)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                owner.removeKeyFrame(kf);
            }

            for (double anchor : anchors)
            {
                double delta = anchor - blockMin;
                for (KeyFrame kf : originalBlock)
                {
                    double newTick = round(kf.getTick() + delta);
                    KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, owner, KeyFrameCharacterActionType.ADD));
                    owner.addKeyFrame(replacement, currentTime);
                    newSelected.add(replacement);
                }
            }
        }

        finalizeTickTransform(kfa, newSelected.toArray(new KeyFrame[0]));
    }

    /**
     * Per-step mode of Scatter (active when the dialog's Mode radio is
     * "Per Step").
     *
     * <p>The step grid is GLOBAL -- one step value rolled in [stepMin,
     * stepMax] drives every Character. At each step tick T in [from, to]:
     *   K = roll in [perStepMin, perStepMax], clamped to the count of
     *       feasible Characters at T (block fits within [from, to] AND
     *       doesn't overlap a prior placement for the same Character).
     *   K distinct Characters are picked (Fisher-Yates shuffle, first K) --
     *   each gets ONE copy of their block anchored at T.
     *
     * <p>"Density per beat" is the use case: K varies independently per step
     * so some beats fire more drops than others. Same-Character collisions
     * across step ticks are PREVENTED -- a Character whose block already
     * occupies [T_prev, T_prev + dur] is excluded from feasibility at any
     * T where [T, T + dur] would overlap that span. Without this exclusion
     * the planner could silently overwrite earlier kfs via addKeyFrame's
     * same-tick-displaces behaviour, which manifested as "fewer kfs than
     * I asked for."
     *
     * <p>Originals are removed once per source Character and replaced with
     * the planned copies in a single undo group (via finalizeTickTransform).
     */
    private void scatterPerStep(java.util.Map<Character, java.util.List<KeyFrame>> byOwner,
                                double fFrom, double fTo,
                                int stepMin, int stepMax,
                                int perStepMin, int perStepMax)
    {
        // Per-step mode treats the selected keyframes as a SOURCE TEMPLATE
        // and the multi-selected Characters in the manager as the TARGET
        // POOL the picker draws from. Without this, a marquee on one
        // Character forces every step's K-pick to land on that same
        // Character, defeating the point of the per-step mode.
        //
        // Pool = byOwner.keySet() (sources, always eligible) ∪
        //        resolveSelectionTargets() (manager-selected, only eligible
        //        when distinct from sources). Non-source pool Characters
        //        clone the FIRST source's block template -- it's the
        //        unambiguous "this pattern" the user just demonstrated by
        //        selecting it.
        java.util.LinkedHashSet<Character> pool = new java.util.LinkedHashSet<>(byOwner.keySet());
        pool.addAll(resolveSelectionTargets());

        // First source's block template, used as the fallback pattern for
        // every non-source Character in the pool.
        java.util.List<KeyFrame> firstSourceBlock = byOwner.values().iterator().next();
        double firstSourceMin = Double.POSITIVE_INFINITY;
        double firstSourceMax = Double.NEGATIVE_INFINITY;
        for (KeyFrame kf : firstSourceBlock)
        {
            if (kf.getTick() < firstSourceMin) firstSourceMin = kf.getTick();
            if (kf.getTick() > firstSourceMax) firstSourceMax = kf.getTick();
        }
        final double firstSourceMinFinal = firstSourceMin;
        final double firstSourceDuration = firstSourceMax - firstSourceMin;

        // For each pool Character: which kfs to clone + [minTick, duration].
        // Source Characters use their own selected kfs; non-source Characters
        // use the first source's block.
        java.util.LinkedHashMap<Character, java.util.List<KeyFrame>> blocks = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<Character, double[]> templates = new java.util.LinkedHashMap<>();
        for (Character ch : pool)
        {
            if (byOwner.containsKey(ch))
            {
                java.util.List<KeyFrame> own = byOwner.get(ch);
                double minTick = Double.POSITIVE_INFINITY;
                double maxTick = Double.NEGATIVE_INFINITY;
                for (KeyFrame kf : own)
                {
                    if (kf.getTick() < minTick) minTick = kf.getTick();
                    if (kf.getTick() > maxTick) maxTick = kf.getTick();
                }
                blocks.put(ch, own);
                templates.put(ch, new double[]{minTick, maxTick - minTick});
            }
            else
            {
                blocks.put(ch, firstSourceBlock);
                templates.put(ch, new double[]{firstSourceMinFinal, firstSourceDuration});
            }
        }

        final java.util.Random rng = new java.util.Random();
        // Roll once: global step shared by every step tick. Rolling per-step
        // would scramble the cadence so the user couldn't tell the density
        // variation apart from random timing.
        int globalStep = stepMin == stepMax
                ? stepMin
                : stepMin + rng.nextInt(stepMax - stepMin + 1);

        // planned[ch] = list of anchor ticks where ch's block will be placed.
        // Used both for committing copies AND for per-Character collision
        // checks during the planning loop (a Character is excluded from
        // feasibility at T if its block would overlap any prior placement).
        java.util.LinkedHashMap<Character, java.util.List<Double>> planned = new java.util.LinkedHashMap<>();
        for (Character ch : pool)
        {
            planned.put(ch, new ArrayList<>());
        }

        java.util.List<Character> poolList = new ArrayList<>(pool);
        // Integer step counter (not double accumulation) so the final step
        // tick lands exactly at fFrom + N*step without floating-point drift.
        int totalSteps = (int) Math.floor((fTo - fFrom) / globalStep) + 1;
        for (int sIdx = 0; sIdx < totalSteps; sIdx++)
        {
            double T = fFrom + (double) sIdx * globalStep;

            // Feasibility: block fits within range, AND would not overlap a
            // prior placement for this same Character. Two intervals
            // [a, a+dur] and [b, b+dur] overlap iff |a - b| < dur, so for
            // each candidate Character we scan its planned anchors and reject
            // T when any prior anchor is within `dur` of T. Zero-duration
            // blocks (single kf) only collide on exact-tick equality, which
            // can't happen here since step ticks are distinct across sIdx.
            java.util.List<Character> feasible = new ArrayList<>();
            for (Character ch : poolList)
            {
                double dur = templates.get(ch)[1];
                if (T + dur > fTo + 1e-9) continue;  // spills past end
                boolean overlapsPrior = false;
                for (double prior : planned.get(ch))
                {
                    if (Math.abs(T - prior) < dur + 1e-9)
                    {
                        overlapsPrior = true;
                        break;
                    }
                }
                if (overlapsPrior) continue;
                feasible.add(ch);
            }
            if (feasible.isEmpty()) continue;

            int K = perStepMin == perStepMax
                    ? perStepMin
                    : perStepMin + rng.nextInt(perStepMax - perStepMin + 1);
            K = Math.min(K, feasible.size());
            if (K == 0) continue;

            java.util.Collections.shuffle(feasible, rng);
            for (int i = 0; i < K; i++)
            {
                planned.get(feasible.get(i)).add(T);
            }
        }

        // Commit: remove originals on SOURCE Characters only (non-source
        // pool Characters had nothing selected so there's nothing to remove),
        // then add planned copies for every pool Character that drew at
        // least one step. Single combined undo group via finalizeTickTransform.
        KeyFrameAction[] kfa = new KeyFrameAction[0];
        java.util.List<KeyFrame> newSelected = new ArrayList<>();

        // Phase 1: remove originals on source Characters.
        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            for (KeyFrame kf : entry.getValue())
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                owner.removeKeyFrame(kf);
            }
        }

        // Phase 2: add planned copies for every pool Character.
        for (Character ch : poolList)
        {
            java.util.List<KeyFrame> block = blocks.get(ch);
            double blockMin = templates.get(ch)[0];
            for (double anchor : planned.get(ch))
            {
                double delta = anchor - blockMin;
                for (KeyFrame kf : block)
                {
                    double newTick = round(kf.getTick() + delta);
                    KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, ch, KeyFrameCharacterActionType.ADD));
                    ch.addKeyFrame(replacement, currentTime);
                    newSelected.add(replacement);
                }
            }
        }

        if (kfa.length == 0)
        {
            JOptionPane.showMessageDialog(this,
                    "No copies were placed. Every step tick rolled K=0 or had no feasible Characters -- try widening the range or raising 'Copies per step (max)'.",
                    "Scatter", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        finalizeTickTransform(kfa, newSelected.toArray(new KeyFrame[0]));
    }

    /**
     * Group mode of Scatter (active when the dialog's "Group mode" checkbox
     * is on and the user has directly clicked one or more folders in the
     * manager tree).
     *
     * <p>Each directly-selected folder is treated as ONE rigid block. The
     * block's footprint is the union of every member Character's selected
     * keyframes -- {@code blockMin} = earliest selected tick across the
     * group, {@code blockMax} = latest, {@code blockDuration} = max-min.
     * Per-folder block: one shared (step, copies) roll, one anchor planner,
     * and at each anchor a single delta gets applied to every member's
     * keyframes, so a tightly-choreographed sequence (e.g. damage ring at
     * tick 0 + projectile at tick 2 + fire at tick 4) keeps its internal
     * timing intact through the randomisation.
     *
     * <p>Members with no selected keyframes contribute nothing to the
     * block and aren't touched -- if a folder has 5 Characters but only 3
     * have keyframes in the marquee, only those 3 get scattered. Folders
     * with zero selected-keyframe members are skipped silently (the
     * dialog already validated that {@code selectedKeyFrames} is non-empty
     * overall).
     *
     * <p>Pre-validation, greedy random anchor planning, and the
     * remove-once / add-N-copies commit shape mirror the per-Character
     * planner -- the only structural difference is that the unit of
     * planning is "folder" instead of "Character".
     */
    private void scatterPerGroup(java.util.Map<Character, java.util.List<KeyFrame>> byOwner,
                                 Folder[] directFolders,
                                 double fFrom, double fTo,
                                 int stepMin, int stepMax,
                                 int copiesMin, int copiesMax)
    {
        // Assemble the per-folder block contents. Fallback: if no folder
        // is directly selected (shouldn't normally reach here because the
        // dialog disables Group mode without one, but defensive in case
        // the user re-enabled it manually), treat the union of selection
        // owners as a single ad-hoc group so the action still does
        // something coherent.
        java.util.LinkedHashMap<Folder, java.util.List<Character>> groupMembers = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<Folder, java.util.List<KeyFrame>> groupBlock = new java.util.LinkedHashMap<>();
        if (directFolders != null && directFolders.length > 0)
        {
            for (Folder folder : directFolders)
            {
                if (folder == null || folder.getLinkedManagerNode() == null) continue;
                ArrayList<Character> members = new ArrayList<>();
                managerTree.getCharacterNodeChildren(folder.getLinkedManagerNode(), members);
                java.util.List<KeyFrame> block = new ArrayList<>();
                for (Character m : members)
                {
                    java.util.List<KeyFrame> mkfs = byOwner.get(m);
                    if (mkfs != null) block.addAll(mkfs);
                }
                if (block.isEmpty()) continue;
                groupMembers.put(folder, members);
                groupBlock.put(folder, block);
            }
        }
        else
        {
            // Synthetic single-group fallback. Use null folder key so the
            // commit-side error messages can detect "ad-hoc group" vs a
            // real folder by name.
            java.util.List<KeyFrame> block = new ArrayList<>();
            ArrayList<Character> members = new ArrayList<>();
            for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> e : byOwner.entrySet())
            {
                members.add(e.getKey());
                block.addAll(e.getValue());
            }
            groupMembers.put(null, members);
            groupBlock.put(null, block);
        }

        if (groupMembers.isEmpty())
        {
            JOptionPane.showMessageDialog(this,
                    "Group mode requires at least one selected keyframe inside a directly-selected folder. None of the chosen folders contain keyframes in the marquee.",
                    "Scatter blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Phase 1: PLAN per group. Same shape as per-Character pre-validation
        // and greedy-random anchor selection, but the "unit" is a whole folder
        // instead of one Character.
        final java.util.Random rng = new java.util.Random();
        java.util.LinkedHashMap<Folder, double[]> plannedAnchors = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<Folder, Double> plannedBlockMin = new java.util.LinkedHashMap<>();

        for (java.util.Map.Entry<Folder, java.util.List<KeyFrame>> entry : groupBlock.entrySet())
        {
            Folder folder = entry.getKey();
            java.util.List<KeyFrame> block = entry.getValue();
            String label = folder == null ? "(ad-hoc group)" : "\"" + folder.getName() + "\"";

            double blockMin = Double.POSITIVE_INFINITY;
            double blockMax = Double.NEGATIVE_INFINITY;
            for (KeyFrame kf : block)
            {
                if (kf.getTick() < blockMin) blockMin = kf.getTick();
                if (kf.getTick() > blockMax) blockMax = kf.getTick();
            }
            double blockDuration = blockMax - blockMin;
            plannedBlockMin.put(folder, blockMin);

            if (copiesMax > 0)
            {
                if (blockDuration > (fTo - fFrom))
                {
                    JOptionPane.showMessageDialog(this,
                            "Cannot scatter: group " + label + " spans " + blockDuration + " ticks across its selected keyframes, which is larger than the range " + (fTo - fFrom) + ".",
                            "Scatter blocked", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                double anchorRange = fTo - fFrom - blockDuration;
                int theoreticalMaxAtStepMax = (int) Math.floor(anchorRange / (blockDuration + stepMax)) + 1;
                if (copiesMax > theoreticalMaxAtStepMax)
                {
                    JOptionPane.showMessageDialog(this,
                            "Cannot scatter: group " + label + " has room for at most " + theoreticalMaxAtStepMax + " non-overlapping copies at step " + stepMax + " (block duration " + blockDuration + " in range " + (fTo - fFrom) + "). Reduce 'Copies (max)', reduce 'Step size (max)', widen the range, or shrink the marquee within the group.",
                            "Scatter blocked", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            int rolledN = copiesMin == copiesMax
                    ? copiesMin
                    : copiesMin + rng.nextInt(copiesMax - copiesMin + 1);
            int rolledStep = stepMin == stepMax
                    ? stepMin
                    : stepMin + rng.nextInt(stepMax - stepMin + 1);
            final double groupStep = rolledStep;

            if (rolledN == 0)
            {
                plannedAnchors.put(folder, new double[0]);
                continue;
            }

            int totalSlots = (int) Math.floor((fTo - fFrom - blockDuration) / groupStep) + 1;

            final int MAX_RETRIES = 20;
            double[] anchors = null;
            for (int attempt = 0; attempt < MAX_RETRIES && anchors == null; attempt++)
            {
                double[] trial = new double[rolledN];
                boolean failed = false;
                for (int c = 0; c < rolledN; c++)
                {
                    java.util.List<Integer> validSlots = new ArrayList<>();
                    for (int s = 0; s < totalSlots; s++)
                    {
                        double candidate = fFrom + s * groupStep;
                        boolean ok = true;
                        for (int prior = 0; prior < c; prior++)
                        {
                            if (Math.abs(candidate - trial[prior]) <= blockDuration)
                            {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) validSlots.add(s);
                    }
                    if (validSlots.isEmpty())
                    {
                        failed = true;
                        break;
                    }
                    int pickedSlot = validSlots.get(rng.nextInt(validSlots.size()));
                    trial[c] = fFrom + pickedSlot * groupStep;
                }
                if (!failed) anchors = trial;
            }

            if (anchors == null)
            {
                JOptionPane.showMessageDialog(this,
                        "Cannot scatter " + rolledN + " non-overlapping copies for group " + label + " at step " + rolledStep + " after " + MAX_RETRIES + " attempts (block duration " + blockDuration + " in range " + (fTo - fFrom) + " ticks). Widen the [from, to] range, lower 'Step size (max)', or lower 'Copies (max)'.",
                        "Scatter blocked", JOptionPane.WARNING_MESSAGE);
                return;
            }

            plannedAnchors.put(folder, anchors);
        }

        // Phase 2: COMMIT. For each group, remove every selected original
        // keyframe from its owner, then add N copies of the entire group
        // block at each planned anchor. Each member's keyframes shift by
        // (anchor - groupBlockMin) so internal offsets across members
        // are preserved exactly.
        //
        // We snapshot kf -> owner via the pre-existing byOwner map BEFORE
        // any removals so findKeyFrameOwner (which scans current Character
        // frames and would return null after removeKeyFrame) doesn't
        // matter here.
        java.util.IdentityHashMap<KeyFrame, Character> kfOwner = new java.util.IdentityHashMap<>();
        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> e : byOwner.entrySet())
        {
            for (KeyFrame kf : e.getValue()) kfOwner.put(kf, e.getKey());
        }

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        java.util.List<KeyFrame> newSelected = new ArrayList<>();

        for (java.util.Map.Entry<Folder, java.util.List<KeyFrame>> entry : groupBlock.entrySet())
        {
            Folder folder = entry.getKey();
            java.util.List<KeyFrame> block = entry.getValue();
            double blockMin = plannedBlockMin.get(folder);
            double[] anchors = plannedAnchors.get(folder);

            for (KeyFrame kf : block)
            {
                Character owner = kfOwner.get(kf);
                if (owner == null) continue;
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                owner.removeKeyFrame(kf);
            }

            // Plant copies. Each anchor is one "instance" of the whole
            // group block; iterate every member kf and re-anchor it on
            // its own owner. Same delta everywhere keeps multi-Character
            // synchronisation intact.
            for (double anchor : anchors)
            {
                double delta = anchor - blockMin;
                for (KeyFrame kf : block)
                {
                    Character owner = kfOwner.get(kf);
                    if (owner == null) continue;
                    double newTick = round(kf.getTick() + delta);
                    KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, owner, KeyFrameCharacterActionType.ADD));
                    owner.addKeyFrame(replacement, currentTime);
                    newSelected.add(replacement);
                }
            }
        }

        finalizeTickTransform(kfa, newSelected.toArray(new KeyFrame[0]));
    }

    /**
     * Right-click context guard for "Reduce selection to keyframe owners".
     * Returns true when both:
     *  - 2+ Characters are currently selected (otherwise reducing is a no-op
     *    or empty), AND
     *  - 1+ keyframes are currently marquee/click-selected (so we have an
     *    owner set to derive).
     */
    public boolean canReduceSelectionToKeyFrameOwners()
    {
        return selectedKeyFrames != null
                && selectedKeyFrames.length > 0
                && selectionManager.size() >= 2;
    }

    /**
     * Narrows the Character multi-selection down to just the owners of the
     * currently-selected keyframes. The keyframe selection itself is left
     * untouched -- only the SelectionManager state changes.
     *
     * <p>Use case: after a marquee that covers part of a folder, the user
     * wants to operate (move, recolour, etc.) on JUST the Characters that
     * have keyframes in the marquee window, not the whole folder they
     * originally had selected. Saves manually CTRL-clicking off siblings.
     *
     * <p>Tree highlight is refreshed via syncTreeFromSelection so the
     * manager panel reflects the new state immediately -- selectionManager
     * doesn't push to the tree on its own.
     */
    public void reduceSelectionToKeyFrameOwners()
    {
        java.util.Map<Character, java.util.List<KeyFrame>> byOwner = groupSelectedKeyFramesByOwner();
        if (byOwner.isEmpty()) return;
        selectionManager.selectAll(byOwner.keySet());
        if (managerTree != null) managerTree.syncTreeFromSelection();
    }

    /**
     * Groups the marquee selection by owning Character. Skips keyframes whose
     * owner can't be resolved (shouldn't normally happen since the marquee is
     * built from visible Characters' keyframes, but defensive against stale
     * refs from operations that already retargeted).
     */
    private java.util.Map<Character, java.util.List<KeyFrame>> groupSelectedKeyFramesByOwner()
    {
        java.util.Map<Character, java.util.List<KeyFrame>> grouped = new java.util.LinkedHashMap<>();
        for (KeyFrame kf : selectedKeyFrames)
        {
            if (kf == null) continue;
            Character owner = findKeyFrameOwner(kf);
            if (owner == null) continue;
            grouped.computeIfAbsent(owner, k -> new ArrayList<>()).add(kf);
        }
        return grouped;
    }

    /**
     * Common tail for Jitter / Scatter: registers the undo group, points
     * selectedKeyFrames at the post-op keyframes so the marquee still highlights
     * what the user was just operating on, and refreshes the renderer +
     * AttributePanel exactly once.
     *
     * <p>{@code newSelected} is the full post-op list of keyframes to highlight
     * -- Jitter passes 1-to-1 replacements (same size as the old selection),
     * Scatter with copies>1 passes N times as many (since each original block
     * becomes N independent copies). Without this re-pointing, the old refs are
     * gone from every Character's frame arrays after the remove/add round-trip
     * and a follow-up op would operate on dead refs.
     */
    private void finalizeTickTransform(KeyFrameAction[] kfa, KeyFrame[] newSelected)
    {
        if (kfa.length == 0) return;
        addKeyFrameActions(kfa);

        if (newSelected != null)
        {
            setSelectedKeyFrames(newSelected);
        }

        // Single refresh sweep at the end. updatePrograms walks every Character
        // and re-runs the renderer pipeline so the timeline + 3D state catch up
        // to the new keyframe ticks without N individual updateProgram calls.
        if (client != null && client.getGameState() == GameState.LOGGED_IN)
        {
            toolBox.getProgrammer().updatePrograms(currentTime);
        }
        attributePanel.setKeyFramedIcon(true);
        if (selectedCharacter != null)
        {
            attributePanel.resetAttributes(selectedCharacter, currentTime);
        }
    }

    // ----- Ripple Delete --------------------------------------------------

    /**
     * Tools > Ripple Delete keyframes...
     *
     * <p>Removes every keyframe whose tick is in [from, to] for the chosen
     * scope, then shifts every keyframe whose tick > to back by
     * {@code (to - from + 1)} ticks so the timeline collapses the deleted
     * span. Like Premiere Pro ripple-delete on a gap.
     *
     * <p>Scope:
     *   {@code null} = all properties (locals on every target Character +
     *     the three globals from the central store)
     *   a local {@code KeyFrameType} = that property only, on every target
     *     Character; globals untouched.
     *   a global {@code KeyFrameType} = that one global in the central
     *     store only; per-Character locals untouched.
     *
     * <p>Targets follow {@link #resolveSelectionTargets()} so multi-select
     * applies automatically. An empty target list is still valid for
     * globals-only scopes (Camera/Fade/Shake have no Character).
     *
     * <p>{@code prefillFrom/prefillTo/prefillScope} are passed in by the
     * right-click context menu so the dialog opens pre-filled with the
     * gap the user clicked into. The Tools menu passes 0/0/null.
     */
    public void showRippleDeleteDialog(double prefillFrom, double prefillTo, KeyFrameType prefillScope)
    {
        java.util.Collection<Character> targets = resolveSelectionTargets();

        JSpinner fromSpinner = new JSpinner(new SpinnerNumberModel(prefillFrom, 0.0, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.5));
        JSpinner toSpinner = new JSpinner(new SpinnerNumberModel(prefillTo, 0.0, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.5));

        final String ALL = "All properties";
        DefaultComboBoxModel<String> scopeModel = new DefaultComboBoxModel<>();
        scopeModel.addElement(ALL);
        for (KeyFrameType type : KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL)
        {
            scopeModel.addElement(type.getName());
        }
        for (KeyFrameType type : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
        {
            scopeModel.addElement(type.getName());
        }
        JComboBox<String> scopeCombo = new JComboBox<>(scopeModel);
        if (prefillScope != null)
        {
            scopeCombo.setSelectedItem(prefillScope.getName());
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("From tick (inclusive):"));
        panel.add(fromSpinner);
        panel.add(new JLabel("To tick (inclusive):"));
        panel.add(toSpinner);
        panel.add(new JLabel("Scope:"));
        panel.add(scopeCombo);

        String targetDesc = targets.isEmpty()
                ? "globals only (no Character selected)"
                : (targets.size() == 1 ? "1 Character" : targets.size() + " Characters");
        int result = JOptionPane.showConfirmDialog(this, panel,
                "Ripple Delete -- " + targetDesc,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        double from = ((Number) fromSpinner.getValue()).doubleValue();
        double to = ((Number) toSpinner.getValue()).doubleValue();
        if (to < from) { double tmp = from; from = to; to = tmp; }
        String scopeName = (String) scopeCombo.getSelectedItem();

        KeyFrameType scopeType = null;
        if (scopeName != null && !ALL.equals(scopeName))
        {
            for (KeyFrameType t : KeyFrameType.values())
            {
                if (t.getName().equals(scopeName))
                {
                    scopeType = t;
                    break;
                }
            }
        }

        executeRippleDelete(targets, from, to, scopeType);
    }

    /**
     * Direct, dialog-less ripple delete entry point used by the AttributeSheet
     * right-click context menu (Premiere Pro-style: right-click the gap, pick
     * a scope, done -- no dialog). Same logic as the dialog OK path.
     */
    public void executeRippleDeleteInstant(double from, double to, KeyFrameType scope)
    {
        if (to < from) { double tmp = from; from = to; to = tmp; }
        executeRippleDelete(resolveSelectionTargets(), from, to, scope);
    }

    private void executeRippleDelete(java.util.Collection<Character> targets, double from, double to, KeyFrameType scope)
    {
        // collapse span = inclusive width of the deleted region. Keyframes at
        // tick > to shift back by exactly this so a keyframe formerly at
        // (to + 1) lands at (from), making the deletion seamless.
        double removed = to - from + 1;
        if (removed <= 0)
        {
            return;
        }

        KeyFrameAction[] kfa = new KeyFrameAction[0];

        // --- LOCAL types: walk each Character's frame matrix.
        for (Character owner : targets)
        {
            KeyFrame[][] frames = owner.getFrames();
            if (frames == null) continue;

            for (int i = 0; i < frames.length; i++)
            {
                KeyFrame[] row = frames[i];
                if (row == null) continue;
                KeyFrameType type = KeyFrameType.getKeyFrameType(i);
                if (isGlobalType(type)) continue;            // globals handled below
                if (scope != null && type != scope) continue; // restricted scope

                // Snapshot before mutation -- owner.removeKeyFrame / addKeyFrame
                // both rewrite the underlying row, which would skip / re-visit
                // entries if we iterated the live array.
                KeyFrame[] snapshot = row.clone();
                for (KeyFrame kf : snapshot)
                {
                    if (kf == null) continue;
                    if (kf.getTick() >= from && kf.getTick() <= to)
                    {
                        kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                        owner.removeKeyFrame(kf);
                    }
                    else if (kf.getTick() > to)
                    {
                        double newTick = round(kf.getTick() - removed);
                        KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                        kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                        kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, owner, KeyFrameCharacterActionType.ADD));
                        owner.removeKeyFrame(kf);
                        owner.addKeyFrame(replacement, currentTime);
                    }
                }
            }
        }

        // --- GLOBAL types: walk the central store. scope=null hits all 3.
        com.creatorskit.saves.GlobalKeyFrames store = plugin.getGlobalKeyFrames();
        if (store != null)
        {
            // null scope means "all globals" -- iterate the canonical list so
            // new global types (SOUND_x) auto-participate in ripple delete.
            for (KeyFrameType globalType : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
            {
                if (scope == null || scope == globalType)
                {
                    kfa = rippleGlobalArray(store.getGlobalKeyFramesByType(globalType), kfa, from, to, removed, store);
                }
            }
        }

        if (kfa.length == 0)
        {
            JOptionPane.showMessageDialog(this,
                    "No keyframes in [" + from + ", " + to + "] for the chosen scope.",
                    "Ripple Delete", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        addKeyFrameActions(kfa);
        // The marquee may have held refs that got removed/replaced; clear it
        // so we don't dangle pointers into the card placeholder state.
        setSelectedKeyFrames(new KeyFrame[0]);
        if (client != null && client.getGameState() == GameState.LOGGED_IN)
        {
            toolBox.getProgrammer().updatePrograms(currentTime);
        }
        attributePanel.setKeyFramedIcon(true);
        if (selectedCharacter != null)
        {
            attributePanel.resetAttributes(selectedCharacter, currentTime);
        }
    }

    /**
     * Helper for the global-store side of ripple delete. Same delete-or-shift
     * decision as the local branch but routed through GlobalKeyFrames.add/remove.
     * Returns the updated action array so the caller can chain across multiple
     * global type arrays without losing the running list.
     */
    private KeyFrameAction[] rippleGlobalArray(KeyFrame[] arr, KeyFrameAction[] kfa, double from, double to, double removed, com.creatorskit.saves.GlobalKeyFrames store)
    {
        if (arr == null) return kfa;
        KeyFrame[] snapshot = arr.clone();
        for (KeyFrame kf : snapshot)
        {
            if (kf == null) continue;
            if (kf.getTick() >= from && kf.getTick() <= to)
            {
                store.remove(kf);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, null, KeyFrameCharacterActionType.REMOVE));
            }
            else if (kf.getTick() > to)
            {
                double newTick = round(kf.getTick() - removed);
                KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                store.remove(kf);
                KeyFrame displaced = store.add(replacement);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, null, KeyFrameCharacterActionType.REMOVE));
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, null, KeyFrameCharacterActionType.ADD));
                if (displaced != null && displaced != kf)
                {
                    // store.add returned an already-present keyframe that we
                    // overwrote (same tick). Record it as removed so undo
                    // restores it.
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(displaced, null, KeyFrameCharacterActionType.REMOVE));
                }
            }
        }
        return kfa;
    }

    /**
     * Adds a Sound keyframe to the first empty global Sound slot (1-4) at
     * the current playhead tick. Called from Cache Searcher > Sound Searcher's
     * Add KeyFrame button. If all four slots are occupied at this tick, the
     * Sound 1 slot is overwritten (same displace-on-same-tick rule as other
     * kfs). Returns the slot the kf landed in, or null if no global store.
     */
    public KeyFrameType addSoundKeyFrameFromCache(int soundId)
    {
        com.creatorskit.saves.GlobalKeyFrames store = plugin.getGlobalKeyFrames();
        if (store == null) return null;
        KeyFrameType chosen = null;
        for (KeyFrameType slot : KeyFrameType.SOUND_TYPES)
        {
            if (findGlobalKeyFrameAt(slot, currentTime) == null)
            {
                chosen = slot;
                break;
            }
        }
        if (chosen == null) chosen = KeyFrameType.SOUND_1;  // all slots full; overwrite slot 1
        com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame kf =
                new com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame(
                        currentTime, chosen, soundId,
                        com.creatorskit.swing.timesheet.keyframe.SoundKeyFrame.DEFAULT_VOLUME);
        addKeyFrameAction(new KeyFrame[]{kf});
        return chosen;
    }

    // ----- Ripple Insert --------------------------------------------------
    //
    // Counterpart to Ripple Delete. Inserts {@code amount} ticks of empty
    // space at the playhead by shifting every keyframe strictly after
    // {@code currentTime} forward by that much. Same scope as Ripple Delete
    // with scope=null: per-Character locals (across multi-Character
    // selection) plus globals from the central store.
    //
    // Keyframes AT the playhead tick stay put -- the convention "insert at
    // playhead" means the new gap opens AFTER any kf the user just landed
    // on, not THROUGH it.

    /**
     * Tools > Ripple Insert... entry point. Opens a small dialog that
     * picks the tick amount and then calls
     * {@link #executeRippleInsertAtPlaybar}.
     */
    public void showRippleInsertDialog()
    {
        JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 1000000.0, 1.0));
        amountSpinner.setPreferredSize(new Dimension(90, 24));

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        content.add(new JLabel("Insert ticks at playhead:"), gbc);
        gbc.gridx = 1;
        content.add(amountSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        content.add(new JLabel(String.format("(playhead is at tick %.1f -- keyframes after it shift right)", currentTime)), gbc);

        int choice = JOptionPane.showConfirmDialog(this, content, "Ripple Insert",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        double amount = ((Number) amountSpinner.getValue()).doubleValue();
        executeRippleInsertAtPlaybar(amount);
    }

    /**
     * Right-click empty-space entry point. Same as the Tools dialog but
     * runnable without dropping into another menu. Defers to the shared
     * dialog so the input UX stays identical.
     */
    public void showRippleInsertDialogAtPlaybar()
    {
        showRippleInsertDialog();
    }

    /**
     * Tools > Repeat selection... -- pastes N copies of the currently
     * selected keyframes contiguously after the existing block, each
     * separated by a configurable {@code gap}. Counterpart to Scatter
     * when the user wants a regular cadence instead of a random spread.
     *
     * <p>Block span = max(endTick) - min(tick) across the selection,
     * where {@code endTick} accounts for the per-keyframe duration
     * concept of each type (Hitsplat / Projectile / Shield / Special /
     * Colour all carry an explicit duration; everything else uses just
     * the tick). The i-th copy lands at {@code originalTick + i * (span + gap)}
     * so copy 1 starts exactly at the original block's duration line
     * with gap=0, or {@code gap} ticks after with gap>0.
     *
     * <p>Multi-Character aware: each kf's copy goes to its own owner.
     * Originals stay in place; only the new copies are added. Same-tick
     * displacement is allowed (matches addKeyFrame semantics) so e.g. a
     * single kf with no duration repeated at gap=0 would overwrite
     * itself, but that's a degenerate case the dialog guards against
     * up front (span + gap must be > 0).
     */
    public void showRepeatSelectionDialog()
    {
        if (selectedKeyFrames == null || selectedKeyFrames.length == 0)
        {
            JOptionPane.showMessageDialog(this,
                    "No keyframes selected. Marquee or click keyframes first.",
                    "Repeat selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JSpinner repeatSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        JSpinner gapSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.5));

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Repeat times (N):"));
        panel.add(repeatSpinner);
        panel.add(new JLabel("Gap between copies (M ticks):"));
        panel.add(gapSpinner);

        // Pre-compute the block span to show the user what their copies will
        // actually look like before they commit. Cheap walk; selection sizes
        // are bounded by what's drawn on screen.
        double previewStart = Double.POSITIVE_INFINITY;
        double previewEnd = Double.NEGATIVE_INFINITY;
        for (KeyFrame kf : selectedKeyFrames)
        {
            if (kf == null) continue;
            if (kf.getTick() < previewStart) previewStart = kf.getTick();
            double end = repeatEndTickOf(kf);
            if (end > previewEnd) previewEnd = end;
        }
        double previewSpan = previewEnd - previewStart;
        JLabel hint = new JLabel(String.format(
                "<html><i>Block: tick %.1f -> %.1f (span %.1f). Each copy = span + gap apart.</i></html>",
                previewStart, previewEnd, previewSpan));
        hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));
        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.add(panel, BorderLayout.CENTER);
        content.add(hint, BorderLayout.SOUTH);

        int choice = JOptionPane.showConfirmDialog(this, content,
                "Repeat " + selectedKeyFrames.length + " keyframes",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        int repeats = ((Number) repeatSpinner.getValue()).intValue();
        double gap = ((Number) gapSpinner.getValue()).doubleValue();
        if (repeats < 1) return;

        double blockSpan = previewSpan;
        if (blockSpan + gap <= 0)
        {
            JOptionPane.showMessageDialog(this,
                    "Block span is 0 (single tick selection) and gap is 0 -- every copy would land on the original. Set a gap > 0 to space them out.",
                    "Repeat selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Snapshot owners BEFORE any mutation. findKeyFrameOwner scans the
        // owner's frames, so it would return null after addKeyFrame's
        // same-tick-displace logic potentially removed a referenced kf
        // mid-batch. Identity-keyed map is enough since the original
        // selection holds object refs we control.
        java.util.IdentityHashMap<KeyFrame, Character> kfOwner = new java.util.IdentityHashMap<>();
        for (KeyFrame kf : selectedKeyFrames)
        {
            if (kf == null) continue;
            Character owner = findKeyFrameOwner(kf);
            if (owner != null) kfOwner.put(kf, owner);
        }

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        java.util.List<KeyFrame> newSelected = new ArrayList<>();
        // Keep the originals in the post-op selection so a follow-up
        // operation operates on "everything I just had + the new copies"
        // -- matches the "I see what I made" expectation rather than
        // dropping the selection.
        for (KeyFrame kf : selectedKeyFrames) if (kf != null) newSelected.add(kf);

        for (int i = 1; i <= repeats; i++)
        {
            double offset = i * (blockSpan + gap);
            for (KeyFrame kf : selectedKeyFrames)
            {
                if (kf == null) continue;
                Character owner = kfOwner.get(kf);
                if (owner == null) continue;
                double newTick = round(kf.getTick() + offset);
                KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                KeyFrame displaced = owner.addKeyFrame(replacement, currentTime);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, owner, KeyFrameCharacterActionType.ADD));
                if (displaced != null && displaced != replacement)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(displaced, owner, KeyFrameCharacterActionType.REMOVE));
                }
                newSelected.add(replacement);
            }
        }

        finalizeTickTransform(kfa, newSelected.toArray(new KeyFrame[0]));
    }

    /**
     * Returns the "duration line" tick of {@code kf} -- its tick plus
     * whatever per-type duration concept it carries. Mirrors the tail-
     * rendering logic in AttributeSheet / SummarySheet so what the user
     * sees on the timeline matches what Repeat treats as the block end.
     *
     * <p>Types without a duration (Animation, Movement, Spawn, etc.)
     * return just the tick. Movement's per-tile travel is a special case
     * we don't model here -- block-end based on the kf object alone is
     * the predictable contract; users can pad with the gap if their
     * Movement tail extends beyond.
     */
    private static double repeatEndTickOf(KeyFrame kf)
    {
        if (kf == null) return 0;
        double tick = kf.getTick();
        if (kf instanceof HitsplatKeyFrame)
        {
            double d = ((HitsplatKeyFrame) kf).getDuration();
            if (d == -1) d = HitsplatKeyFrame.DEFAULT_DURATION;
            return tick + d;
        }
        if (kf instanceof ProjectileKeyFrame)
        {
            return tick + ((ProjectileKeyFrame) kf).getDurationTicks();
        }
        if (kf instanceof ShieldKeyFrame)
        {
            return tick + ((ShieldKeyFrame) kf).getDuration();
        }
        if (kf instanceof SpecialKeyFrame)
        {
            return tick + ((SpecialKeyFrame) kf).getDuration();
        }
        if (kf instanceof ColourKeyFrame)
        {
            ColourKeyFrame ckf = (ColourKeyFrame) kf;
            return tick + ckf.getFadeInTicks() + ckf.getHoldTicks() + ckf.getFadeOutTicks();
        }
        return tick;
    }

    // ----- Iterate field values ------------------------------------------
    //
    // Sliding-window iterator over a sequence of values, applied to a field
    // on each of N selected keyframes in tick order. Generalises the
    // "kf at tick 4 wants Tile (1-5), kf at tick 8 wants Tile (6-10)..."
    // pattern across whatever field needs it.
    //
    // v2 adds:
    //   - Arithmetic sequence source (start / step / count) for the
    //     "92, 91, 90, ..." numeric decrement / increment use case.
    //   - Scalar numeric fields for the bar keyframes (Special / Shield /
    //     Health: currentValue, maxValue, duration, width, order). The
    //     slice's first element is parsed and written; window > 1 takes
    //     the leftmost value (useful when overlapping a string sequence,
    //     unusual for numeric).
    //
    // Adding a new field is: add to the combo's items in
    // {@link #populateIterateFieldCombo}, add an arm to
    // {@link #applySliceToKeyFrame}. The slicer + sequence-source +
    // preview + everything else is shared.

    /**
     * Tools &gt; Iterate field values... entry point. Refuses to open
     * without a homogeneous keyframe selection (every selected kf must
     * share a type so the field dropdown is well-defined). The dialog
     * is JOptionPane-modal because the user doesn't need to touch the
     * game while configuring -- the inputs are all text/spinners.
     */
    public void showIterateFieldDialog()
    {
        if (selectedKeyFrames == null || selectedKeyFrames.length == 0)
        {
            JOptionPane.showMessageDialog(this,
                    "No keyframes selected. Marquee the kfs you want to fill, then re-open this tool.",
                    "Iterate field values", JOptionPane.WARNING_MESSAGE);
            return;
        }
        KeyFrameType firstType = selectedKeyFrames[0].getKeyFrameType();
        for (KeyFrame kf : selectedKeyFrames)
        {
            if (kf.getKeyFrameType() != firstType)
            {
                JOptionPane.showMessageDialog(this,
                        "Selection mixes keyframe types (" + firstType.getName() + " + " + kf.getKeyFrameType().getName()
                                + "). This tool needs all kfs to share a type so the field dropdown is valid -- marquee a homogeneous slice.",
                        "Iterate field values", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Field picker. Populated per-type in populateIterateFieldCombo
        // (Projectile.Target from v1, plus numeric scalar fields on
        // Special / Shield / Health bar kfs from v2). Empty combo for an
        // unregistered type bails out below.
        javax.swing.JComboBox<String> fieldCombo = new javax.swing.JComboBox<>();
        populateIterateFieldCombo(fieldCombo, firstType);

        if (fieldCombo.getItemCount() == 0)
        {
            JOptionPane.showMessageDialog(this,
                    "No iterable fields registered for keyframe type \"" + firstType.getName() + "\" yet. "
                            + "Ping the developer with the field you want next.",
                    "Iterate field values", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        javax.swing.JRadioButton folderPatternRadio = new javax.swing.JRadioButton("Folder names matching pattern", true);
        javax.swing.JRadioButton literalListRadio = new javax.swing.JRadioButton("Literal comma list", false);
        javax.swing.JRadioButton arithmeticRadio = new javax.swing.JRadioButton("Arithmetic sequence (start, step, count)", false);
        ButtonGroup sourceGroup = new ButtonGroup();
        sourceGroup.add(folderPatternRadio);
        sourceGroup.add(literalListRadio);
        sourceGroup.add(arithmeticRadio);
        javax.swing.JTextField folderPatternField = new javax.swing.JTextField("Tile (*)", 20);
        folderPatternField.setToolTipText("Glob-style match where * is a wildcard. Folders sorted numerically when the wildcard captures digits, otherwise alphabetically.");
        javax.swing.JTextField literalListField = new javax.swing.JTextField("", 20);
        literalListField.setToolTipText("Comma-separated values, used verbatim as slice contents.");
        // Arithmetic source: start + i*step for i in [0, count). Default
        // count = selected-kf-count so the typical "one value per kf"
        // case Just Works without the user touching it.
        javax.swing.JSpinner arithmeticStart = new javax.swing.JSpinner(new SpinnerNumberModel(0, -1_000_000, 1_000_000, 1));
        javax.swing.JSpinner arithmeticStep  = new javax.swing.JSpinner(new SpinnerNumberModel(-1, -1_000_000, 1_000_000, 1));
        javax.swing.JSpinner arithmeticCount = new javax.swing.JSpinner(new SpinnerNumberModel(Math.max(1, selectedKeyFrames.length), 1, 1_000_000, 1));
        arithmeticStart.setToolTipText("First value in the sequence (written into the first selected kf).");
        arithmeticStep.setToolTipText("Per-step increment. Negative values decrement (e.g. step=-1 with start=92 gives 92, 91, 90, ...).");
        arithmeticCount.setToolTipText("How many values to generate. Set to the selected-kf count to fill every kf exactly once with window=stride=1.");

        // Bumping arithmetic radio also bumps window/stride to 1/1 -- the
        // sliding-window slicer still runs, but for the "one numeric value
        // per kf" use case window=1 is what you want. Users can override
        // afterwards if they want to overlap (unusual for scalars but
        // permitted).
        javax.swing.JSpinner windowSpinner = new javax.swing.JSpinner(new SpinnerNumberModel(5, 1, 1000, 1));
        javax.swing.JSpinner strideSpinner = new javax.swing.JSpinner(new SpinnerNumberModel(5, 1, 1000, 1));

        // Generate-missing toggle: when ON, ignores the rest of the
        // selection and treats the first selected kf as a template. Walks
        // i from 0 to N-1 (N = arithmetic count, or sequence/stride for
        // other sources), placing one kf per step at template.tick +
        // i*tickStep on the template's Character. Existing kfs at those
        // ticks are updated in place; missing ones are CREATED by copying
        // the template and overriding the iterated field. The "spawn a
        // full series from one seed kf" workflow -- e.g. one Special bar
        // kf at tick 10 with all the fields set, generate 92 more so the
        // bar drains 92 -> 0 across 93 ticks.
        javax.swing.JCheckBox generateMissingCheck = new javax.swing.JCheckBox("Generate missing kfs (single-template mode)");
        generateMissingCheck.setToolTipText("<html>Use the first selected kf as a template and generate one kf per sequence value, "
                + "spaced by Tick step. Existing kfs at those ticks are overwritten.</html>");
        javax.swing.JSpinner tickStepSpinner = new javax.swing.JSpinner(new SpinnerNumberModel(1.0, 0.01, 1_000_000.0, 0.1));
        tickStepSpinner.setToolTipText("Tick interval between generated kfs. 1.0 = one kf per game tick.");
        tickStepSpinner.setEnabled(false);
        generateMissingCheck.addActionListener(e -> tickStepSpinner.setEnabled(generateMissingCheck.isSelected()));
        // Stride defaults to window size (non-overlapping). Auto-link on
        // window edit so the common case "size 5, stride 5" Just Works
        // without the user touching stride. Manually editing stride later
        // breaks the link until the next window edit -- standard sticky
        // behaviour, matches how DAW grid editors handle it.
        final boolean[] strideLinked = { true };
        windowSpinner.addChangeListener(e ->
        {
            if (strideLinked[0])
            {
                strideSpinner.setValue(windowSpinner.getValue());
            }
        });
        strideSpinner.addChangeListener(e -> strideLinked[0] = strideSpinner.getValue().equals(windowSpinner.getValue()));

        javax.swing.JCheckBox wrapFolderCheck = new javax.swing.JCheckBox("Wrap each slice in f[...] (folder fan-out syntax)", true);
        wrapFolderCheck.setToolTipText("On: each slice is written as f[v1, v2, ...] so the Projectile target parser treats each value as a folder name. Off: slice is written as a plain comma list.");

        javax.swing.JTextArea previewArea = new javax.swing.JTextArea(10, 50);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        previewArea.setLineWrap(false);
        javax.swing.JScrollPane previewScroll = new javax.swing.JScrollPane(previewArea);
        previewScroll.setBorder(BorderFactory.createTitledBorder("Preview"));

        // The Characters that wrap each selected kf -- needed for the
        // tick-then-name sort. Snapshot once; the selection can't change
        // while the modal dialog is open.
        java.util.IdentityHashMap<KeyFrame, Character> kfOwner = new java.util.IdentityHashMap<>();
        for (KeyFrame kf : selectedKeyFrames)
        {
            Character owner = findKeyFrameOwner(kf);
            if (owner != null) kfOwner.put(kf, owner);
        }
        // Sort: tick ascending, then owner name alphabetically. KeyFrames
        // without a resolvable owner (shouldn't normally happen) sort to
        // the end so they don't break ordering of the real ones.
        java.util.List<KeyFrame> ordered = new ArrayList<>(java.util.Arrays.asList(selectedKeyFrames));
        ordered.sort((a, b) ->
        {
            int t = Double.compare(a.getTick(), b.getTick());
            if (t != 0) return t;
            Character oa = kfOwner.get(a);
            Character ob = kfOwner.get(b);
            String na = oa == null ? "~" : (oa.getName() == null ? "" : oa.getName());
            String nb = ob == null ? "~" : (ob.getName() == null ? "" : ob.getName());
            return na.compareTo(nb);
        });

        Runnable refreshPreview = () ->
        {
            java.util.List<String> sequence = resolveSequence(
                    folderPatternRadio.isSelected(),
                    arithmeticRadio.isSelected(),
                    folderPatternField.getText(),
                    literalListField.getText(),
                    ((Number) arithmeticStart.getValue()).intValue(),
                    ((Number) arithmeticStep.getValue()).intValue(),
                    ((Number) arithmeticCount.getValue()).intValue());
            int window = ((Number) windowSpinner.getValue()).intValue();
            int stride = ((Number) strideSpinner.getValue()).intValue();
            boolean wrap = folderPatternRadio.isSelected() && wrapFolderCheck.isSelected();
            boolean generate = generateMissingCheck.isSelected();
            double tickStep = ((Number) tickStepSpinner.getValue()).doubleValue();

            StringBuilder sb = new StringBuilder();
            sb.append("Sequence (").append(sequence.size()).append(" entries): ");
            if (sequence.isEmpty()) sb.append("<empty>");
            else
            {
                int show = Math.min(sequence.size(), 20);
                for (int i = 0; i < show; i++)
                {
                    if (i > 0) sb.append(", ");
                    sb.append(sequence.get(i));
                }
                if (sequence.size() > show) sb.append(", ...");
            }
            sb.append("\n\n");

            if (generate)
            {
                // Generate-mode preview: walk from template.tick by
                // tickStep increments. New kfs are flagged "[new]" so
                // the user can sanity check how many will be created.
                KeyFrame template = ordered.isEmpty() ? null : ordered.get(0);
                Character templateOwner = template == null ? null : kfOwner.get(template);
                if (template == null || templateOwner == null)
                {
                    sb.append("No template kf -- select one and re-open.\n");
                }
                else
                {
                    sb.append("Generate mode -- template @ tick ")
                            .append(String.format("%.1f", template.getTick()))
                            .append(" on ").append(templateOwner.getName())
                            .append(" (").append(template.getKeyFrameType().getName()).append(")\n");
                    int sliceCount = (sequence.size() - window) / stride + 1;
                    if (sliceCount < 0) sliceCount = 0;
                    int shown = 0;
                    int created = 0;
                    int updated = 0;
                    int total = 0;
                    for (int i = 0; i < sliceCount; i++)
                    {
                        int start = i * stride;
                        int end = start + window;
                        if (end > sequence.size()) break;
                        total++;
                        java.util.List<String> slice = sequence.subList(start, end);
                        String value = formatSlice(slice, wrap);
                        double tick = template.getTick() + i * tickStep;
                        KeyFrame existing = templateOwner.findKeyFrame(template.getKeyFrameType(), tick);
                        boolean isNew = (existing == null);
                        if (isNew) created++; else updated++;
                        if (shown < 12)
                        {
                            sb.append(String.format("  %s tick %.2f: %s%n", isNew ? "[new]" : "[upd]", tick, value));
                            shown++;
                        }
                    }
                    if (total > shown)
                    {
                        sb.append("  ... (").append(total - shown).append(" more)\n");
                    }
                    sb.append("\nWill write ").append(total).append(" kfs (")
                            .append(created).append(" new, ")
                            .append(updated).append(" updating existing).");
                }
            }
            else
            {
                int applied = 0;
                for (int i = 0; i < ordered.size(); i++)
                {
                    KeyFrame kf = ordered.get(i);
                    int start = i * stride;
                    int end = start + window;
                    if (end > sequence.size())
                    {
                        sb.append(String.format("  kf @ tick %.1f (%s): NOT APPLIED (sequence ran out at index %d, needed %d)\n",
                                kf.getTick(),
                                kfOwner.get(kf) == null ? "?" : kfOwner.get(kf).getName(),
                                sequence.size(), end));
                        continue;
                    }
                    java.util.List<String> slice = sequence.subList(start, end);
                    String value = formatSlice(slice, wrap);
                    sb.append(String.format("  kf @ tick %.1f (%s): %s\n",
                            kf.getTick(),
                            kfOwner.get(kf) == null ? "?" : kfOwner.get(kf).getName(),
                            value));
                    applied++;
                }
                sb.append("\nWill apply to ").append(applied).append(" of ").append(ordered.size()).append(" selected keyframes.");
            }
            previewArea.setText(sb.toString());
            previewArea.setCaretPosition(0);
        };

        // Pre-declare the "sync source to field type" runnable so any of
        // the change listeners below can call it. Toggles the source
        // radios + enables/disables the slicing controls so the dialog
        // visually narrows to the inputs that actually matter for the
        // picked field. Numeric fields auto-switch to arithmetic (the
        // typical "one value per kf" mode); the string field (Target)
        // sticks with folder pattern, the v1 default.
        Runnable syncSourceToField = () ->
        {
            String f = (String) fieldCombo.getSelectedItem();
            boolean stringField = isIterableStringField(f);
            // Wrap-in-f[...] is Projectile.Target specific; meaningless
            // for numeric fields and for arithmetic source.
            wrapFolderCheck.setEnabled(stringField && folderPatternRadio.isSelected());
            // Folder pattern produces strings ("Tile (3)"); they can't
            // parse as the numeric setters we wired in v2. Grey it out
            // so the user can see it's not the right tool, without
            // hiding the radio (preserves layout).
            folderPatternRadio.setEnabled(stringField);
            folderPatternField.setEnabled(stringField);
            // Literal-list is permitted on numeric fields -- user can
            // type "92, 91, 90" by hand if arithmetic doesn't fit.
            // Arithmetic source is permitted on string fields too
            // (would produce "0", "-1", "-2", ...). Both stay enabled
            // for either field type so we don't lock out edge cases.
        };
        // Wire every input to refresh the preview live.
        folderPatternRadio.addActionListener(e -> { syncSourceToField.run(); refreshPreview.run(); });
        literalListRadio.addActionListener(e -> { syncSourceToField.run(); refreshPreview.run(); });
        arithmeticRadio.addActionListener(e ->
        {
            // Auto-collapse window/stride to 1/1 when switching to the
            // arithmetic source -- the typical "one value per kf" case.
            // Users can override afterwards if they want overlap.
            if (arithmeticRadio.isSelected())
            {
                windowSpinner.setValue(1);
                strideSpinner.setValue(1);
            }
            syncSourceToField.run();
            refreshPreview.run();
        });
        // Field change auto-switches source: numeric fields jump to
        // arithmetic (the user's likely intent for the Special bar
        // "decrement Current Value by 1 each tick" use case); string
        // fields jump back to folder pattern (v1 default).
        fieldCombo.addActionListener(e ->
        {
            String f = (String) fieldCombo.getSelectedItem();
            if (f != null && !isIterableStringField(f) && !arithmeticRadio.isSelected())
            {
                arithmeticRadio.setSelected(true);
                windowSpinner.setValue(1);
                strideSpinner.setValue(1);
            }
            else if (f != null && isIterableStringField(f) && arithmeticRadio.isSelected())
            {
                folderPatternRadio.setSelected(true);
            }
            syncSourceToField.run();
            refreshPreview.run();
        });
        wrapFolderCheck.addActionListener(e -> refreshPreview.run());
        javax.swing.event.DocumentListener docListener = new javax.swing.event.DocumentListener()
        {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshPreview.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshPreview.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshPreview.run(); }
        };
        folderPatternField.getDocument().addDocumentListener(docListener);
        literalListField.getDocument().addDocumentListener(docListener);
        windowSpinner.addChangeListener(e -> refreshPreview.run());
        strideSpinner.addChangeListener(e -> refreshPreview.run());
        arithmeticStart.addChangeListener(e -> refreshPreview.run());
        arithmeticStep.addChangeListener(e -> refreshPreview.run());
        arithmeticCount.addChangeListener(e -> refreshPreview.run());
        generateMissingCheck.addActionListener(e -> refreshPreview.run());
        tickStepSpinner.addChangeListener(e -> refreshPreview.run());

        JPanel sourcePanel = new JPanel();
        sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.Y_AXIS));
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Value sequence"));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row1.add(folderPatternRadio);
        row1.add(folderPatternField);
        sourcePanel.add(row1);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row2.add(literalListRadio);
        row2.add(literalListField);
        sourcePanel.add(row2);
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row3.add(arithmeticRadio);
        row3.add(new JLabel("start:"));
        row3.add(arithmeticStart);
        row3.add(new JLabel("step:"));
        row3.add(arithmeticStep);
        row3.add(new JLabel("count:"));
        row3.add(arithmeticCount);
        sourcePanel.add(row3);

        JPanel slicePanel = new JPanel(new GridLayout(0, 2, 6, 6));
        slicePanel.setBorder(BorderFactory.createTitledBorder("Slicing"));
        slicePanel.add(new JLabel("Window size:"));
        slicePanel.add(windowSpinner);
        slicePanel.add(new JLabel("Stride:"));
        slicePanel.add(strideSpinner);

        JPanel generatePanel = new JPanel(new GridLayout(0, 2, 6, 6));
        generatePanel.setBorder(BorderFactory.createTitledBorder("Generation"));
        generatePanel.add(generateMissingCheck);
        generatePanel.add(new JLabel(""));
        generatePanel.add(new JLabel("Tick step:"));
        generatePanel.add(tickStepSpinner);

        JPanel headerPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        headerPanel.add(new JLabel("Selection:"));
        // Bare count was confusing -- the tool writes ONE slice per
        // selected kf, so a 1-kf selection means only one slice gets
        // written (= one value). Hint the multi-select expectation
        // inline so the user notices before clicking OK and seeing
        // "wrote 1 of 1" when they actually wanted 30+.
        String selSummary = selectedKeyFrames.length + " kfs of type " + firstType.getName();
        if (selectedKeyFrames.length == 1)
        {
            selSummary += "  (only one -- multi-select the kfs to iterate across)";
        }
        headerPanel.add(new JLabel(selSummary));
        headerPanel.add(new JLabel("Field:"));
        headerPanel.add(fieldCombo);
        headerPanel.add(new JLabel("Format:"));
        headerPanel.add(wrapFolderCheck);

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBorder(new EmptyBorder(6, 6, 6, 6));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(headerPanel);
        top.add(sourcePanel);
        top.add(slicePanel);
        top.add(generatePanel);
        content.add(top, BorderLayout.NORTH);
        content.add(previewScroll, BorderLayout.CENTER);

        // Initial sync: if the default field is numeric (e.g. Special's
        // first item "Current Value"), jump straight to arithmetic so
        // the dialog opens with the source the user is most likely to
        // want rather than the v1 folder-pattern default.
        String initialField = (String) fieldCombo.getSelectedItem();
        if (initialField != null && !isIterableStringField(initialField))
        {
            arithmeticRadio.setSelected(true);
            windowSpinner.setValue(1);
            strideSpinner.setValue(1);
        }
        syncSourceToField.run();
        refreshPreview.run();

        int result = JOptionPane.showConfirmDialog(this, content,
                "Iterate field values",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        java.util.List<String> sequence = resolveSequence(
                folderPatternRadio.isSelected(),
                arithmeticRadio.isSelected(),
                folderPatternField.getText(),
                literalListField.getText(),
                ((Number) arithmeticStart.getValue()).intValue(),
                ((Number) arithmeticStep.getValue()).intValue(),
                ((Number) arithmeticCount.getValue()).intValue());
        int window = ((Number) windowSpinner.getValue()).intValue();
        int stride = ((Number) strideSpinner.getValue()).intValue();
        boolean wrap = folderPatternRadio.isSelected() && wrapFolderCheck.isSelected();
        String field = (String) fieldCombo.getSelectedItem();

        boolean generate = generateMissingCheck.isSelected();
        double tickStep = ((Number) tickStepSpinner.getValue()).doubleValue();

        int applied = 0;
        int skipped = 0;
        int created = 0;

        if (generate)
        {
            // Generate path: template = first selected kf. Walks i from 0
            // to the sequence-derived slice count, placing one kf per
            // step on the template's Character. Existing kfs at the
            // computed tick are mutated in place; missing ticks get a
            // fresh KeyFrame.createCopy(template, tick) which is added
            // via Character.addKeyFrame. Every add / replace is recorded
            // as a KeyFrameAction for the undo stack.
            KeyFrame template = ordered.isEmpty() ? null : ordered.get(0);
            Character templateOwner = template == null ? null : kfOwner.get(template);
            if (template == null || templateOwner == null)
            {
                JOptionPane.showMessageDialog(this,
                        "Generate mode needs the first selected kf to have a resolvable owner -- got null.",
                        "Iterate field values", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int sliceCount = (sequence.size() - window) / stride + 1;
            if (sliceCount < 0) sliceCount = 0;
            KeyFrameAction[] kfa = new KeyFrameAction[0];
            for (int i = 0; i < sliceCount; i++)
            {
                int start = i * stride;
                int end = start + window;
                if (end > sequence.size()) break;
                java.util.List<String> slice = sequence.subList(start, end);
                String value = formatSlice(slice, wrap);
                double tick = template.getTick() + i * tickStep;
                KeyFrame existing = templateOwner.findKeyFrame(firstType, tick);
                if (existing != null)
                {
                    if (applySliceToKeyFrame(existing, firstType, field, value))
                    {
                        applied++;
                    }
                }
                else
                {
                    KeyFrame freshKf = KeyFrame.createCopy(template, tick);
                    if (!applySliceToKeyFrame(freshKf, firstType, field, value))
                    {
                        // Parse failure -- skip this kf, don't add a half-
                        // baked template copy with the wrong field value.
                        continue;
                    }
                    KeyFrame replaced = templateOwner.addKeyFrame(freshKf, currentTime);
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(freshKf, templateOwner, KeyFrameCharacterActionType.ADD));
                    if (replaced != null)
                    {
                        kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replaced, templateOwner, KeyFrameCharacterActionType.REMOVE));
                    }
                    applied++;
                    created++;
                }
            }
            if (kfa.length > 0)
            {
                addKeyFrameActions(kfa);
            }
        }
        else
        {
            for (int i = 0; i < ordered.size(); i++)
            {
                KeyFrame kf = ordered.get(i);
                int start = i * stride;
                int end = start + window;
                if (end > sequence.size())
                {
                    skipped = ordered.size() - i;
                    break;
                }
                java.util.List<String> slice = sequence.subList(start, end);
                String value = formatSlice(slice, wrap);
                if (applySliceToKeyFrame(kf, firstType, field, value))
                {
                    applied++;
                }
            }
        }

        // Re-run the play/scrub pipeline so the new field values land in
        // the renderer immediately. AttributePanel reset so the inspector
        // shows whatever was written if the user has a kf selected.
        if (client != null && client.getGameState() == net.runelite.api.GameState.LOGGED_IN)
        {
            toolBox.getProgrammer().updatePrograms(currentTime);
        }
        if (selectedCharacter != null)
        {
            attributePanel.resetAttributes(selectedCharacter, currentTime);
        }

        String msg;
        if (generate)
        {
            msg = "Iterate field values: wrote " + applied + " kfs (" + created + " new, " + (applied - created) + " updating existing)";
        }
        else
        {
            msg = "Iterate field values: wrote " + applied + " of " + ordered.size() + " selected keyframes";
            if (skipped > 0)
            {
                msg += " (" + skipped + " skipped -- sequence ran out)";
            }
        }
        plugin.sendChatMessage(msg + ".");
    }

    /**
     * Resolves the configured value sequence. Branches:
     * <ul>
     *   <li><b>folder pattern</b>: walks the manager tree, matches folder
     *       names against the glob, sorts numerically when the wildcard
     *       portion is parseable as an int.</li>
     *   <li><b>arithmetic</b>: generates [start + 0*step, start + 1*step,
     *       ..., start + (count-1)*step] as integer strings. Negative
     *       step lets the user decrement (92, 91, 90, ...).</li>
     *   <li><b>literal-list</b>: splits the text on commas and trims each.</li>
     * </ul>
     */
    private java.util.List<String> resolveSequence(boolean folderPattern, boolean arithmetic,
            String patternText, String literalText,
            int arithmeticStart, int arithmeticStep, int arithmeticCount)
    {
        if (arithmetic)
        {
            java.util.List<String> out = new ArrayList<>();
            int count = Math.max(0, arithmeticCount);
            for (int i = 0; i < count; i++)
            {
                out.add(Integer.toString(arithmeticStart + i * arithmeticStep));
            }
            return out;
        }
        if (folderPattern)
        {
            String pattern = patternText == null ? "" : patternText.trim();
            if (pattern.isEmpty()) return java.util.Collections.emptyList();
            int star = pattern.indexOf('*');
            if (star < 0)
            {
                // Exact match -- single-folder sequence.
                java.util.List<String> single = new ArrayList<>();
                if (folderExists(pattern)) single.add(pattern);
                return single;
            }
            String prefix = pattern.substring(0, star);
            String suffix = pattern.substring(star + 1);

            // Collect every folder name in the tree
            ManagerTree tree = managerTree;
            javax.swing.tree.DefaultMutableTreeNode root = tree.getRootNode();
            java.util.ArrayList<javax.swing.tree.DefaultMutableTreeNode> all = new java.util.ArrayList<>();
            tree.getAllNodes(root, all);
            java.util.List<String[]> matched = new ArrayList<>(); // [fullName, wildcardCapture]
            for (javax.swing.tree.DefaultMutableTreeNode node : all)
            {
                Object u = node.getUserObject();
                if (!(u instanceof Folder)) continue;
                String name = ((Folder) u).getName();
                if (name == null) continue;
                if (name.length() < prefix.length() + suffix.length()) continue;
                if (!name.startsWith(prefix)) continue;
                if (!name.endsWith(suffix)) continue;
                String capture = name.substring(prefix.length(), name.length() - suffix.length());
                matched.add(new String[]{name, capture});
            }

            // Try numeric sort on the capture; fall back to alpha.
            boolean allNumeric = !matched.isEmpty();
            for (String[] m : matched)
            {
                try { Integer.parseInt(m[1].trim()); }
                catch (NumberFormatException e) { allNumeric = false; break; }
            }
            if (allNumeric)
            {
                matched.sort((a, b) -> Integer.compare(Integer.parseInt(a[1].trim()), Integer.parseInt(b[1].trim())));
            }
            else
            {
                matched.sort((a, b) -> a[1].compareTo(b[1]));
            }

            java.util.List<String> out = new ArrayList<>();
            for (String[] m : matched) out.add(m[0]);
            return out;
        }
        else
        {
            String text = literalText == null ? "" : literalText.trim();
            if (text.isEmpty()) return java.util.Collections.emptyList();
            java.util.List<String> out = new ArrayList<>();
            for (String t : text.split(","))
            {
                String n = t.trim();
                if (!n.isEmpty()) out.add(n);
            }
            return out;
        }
    }

    private boolean folderExists(String name)
    {
        javax.swing.tree.DefaultMutableTreeNode root = managerTree.getRootNode();
        java.util.ArrayList<javax.swing.tree.DefaultMutableTreeNode> all = new java.util.ArrayList<>();
        managerTree.getAllNodes(root, all);
        for (javax.swing.tree.DefaultMutableTreeNode node : all)
        {
            Object u = node.getUserObject();
            if (u instanceof Folder && name.equalsIgnoreCase(((Folder) u).getName())) return true;
        }
        return false;
    }

    /** Joins {@code slice} as a comma-separated string, optionally wrapped in f[...]. */
    private static String formatSlice(java.util.List<String> slice, boolean wrapInFFolder)
    {
        String joined = String.join(", ", slice);
        return wrapInFFolder ? "f[" + joined + "]" : joined;
    }

    /**
     * Registers the iterable fields for {@code type} in {@code combo}.
     * Adding a new field means one entry here AND one arm in
     * {@link #applySliceToKeyFrame}. Order in this method drives the
     * dropdown order.
     */
    private void populateIterateFieldCombo(javax.swing.JComboBox<String> combo, KeyFrameType type)
    {
        if (type == KeyFrameType.PROJECTILE)
        {
            combo.addItem("Target");
            combo.addItem("Projectile ID");
            combo.addItem("Start Height");
            combo.addItem("End Height");
            combo.addItem("Slope");
            combo.addItem("Duration");
            combo.addItem("Radius");
        }
        else if (type == KeyFrameType.SPECIAL || type == KeyFrameType.SHIELD)
        {
            // Special and Shield share the same field schema (currentValue
            // / maxValue / duration / order / width). Same combo, same
            // apply path.
            combo.addItem("Current Value");
            combo.addItem("Max Value");
            combo.addItem("Duration");
            combo.addItem("Order");
            combo.addItem("Width");
        }
        else if (type == KeyFrameType.HEALTH)
        {
            combo.addItem("Current Health");
            combo.addItem("Max Health");
            combo.addItem("Duration");
            combo.addItem("Order");
            combo.addItem("Width");
        }
        else if (type == KeyFrameType.HITSPLAT_1 || type == KeyFrameType.HITSPLAT_2
                || type == KeyFrameType.HITSPLAT_3 || type == KeyFrameType.HITSPLAT_4)
        {
            combo.addItem("Damage");
            combo.addItem("Duration");
        }
        else if (type == KeyFrameType.MODEL)
        {
            combo.addItem("Model ID");
            combo.addItem("Radius");
        }
        else if (type == KeyFrameType.ANIMATION)
        {
            combo.addItem("Active");
            combo.addItem("Start Frame");
            combo.addItem("Last Frame");
            combo.addItem("Pause Ticks");
        }
        else if (type == KeyFrameType.ORIENTATION)
        {
            combo.addItem("End");
            combo.addItem("Duration");
            combo.addItem("Turn Rate");
        }
    }

    /**
     * Writes {@code value} to the named field of {@code kf}. For scalar
     * numeric fields the slice's first token is parsed; rejection on
     * parse failure is treated as "skip this kf" so a partly-numeric
     * sequence (one bad token) doesn't corrupt the rest.
     *
     * <p>Returns true on success. Add a new field: one branch here, one
     * entry in {@link #populateIterateFieldCombo}.
     */
    private boolean applySliceToKeyFrame(KeyFrame kf, KeyFrameType type, String field, String value)
    {
        String trimmed = value == null ? "" : value.trim();
        if (type == KeyFrameType.PROJECTILE && kf instanceof ProjectileKeyFrame)
        {
            ProjectileKeyFrame pkf = (ProjectileKeyFrame) kf;
            if ("Target".equals(field))       { pkf.setTarget(value); return true; }
            if ("Projectile ID".equals(field)) { Integer iv = tryParseInt(trimmed); if (iv == null) return false; pkf.setProjectileId(iv); return true; }
            if ("Start Height".equals(field)) { Integer iv = tryParseInt(trimmed); if (iv == null) return false; pkf.setStartHeight(iv); return true; }
            if ("End Height".equals(field))   { Integer iv = tryParseInt(trimmed); if (iv == null) return false; pkf.setEndHeight(iv); return true; }
            if ("Slope".equals(field))        { Integer iv = tryParseInt(trimmed); if (iv == null) return false; pkf.setSlope(iv); return true; }
            if ("Duration".equals(field))     { Double dv = tryParseDouble(trimmed); if (dv == null) return false; pkf.setDurationTicks(dv); return true; }
            if ("Radius".equals(field))       { Integer iv = tryParseInt(trimmed); if (iv == null) return false; pkf.setRadius(iv); return true; }
        }
        else if (type == KeyFrameType.SPECIAL && kf instanceof com.creatorskit.swing.timesheet.keyframe.SpecialKeyFrame)
        {
            com.creatorskit.swing.timesheet.keyframe.SpecialKeyFrame skf =
                    (com.creatorskit.swing.timesheet.keyframe.SpecialKeyFrame) kf;
            if ("Current Value".equals(field)) { Integer iv = tryParseInt(trimmed); if (iv == null) return false; skf.setCurrentValue(iv); return true; }
            if ("Max Value".equals(field))     { Integer iv = tryParseInt(trimmed); if (iv == null) return false; skf.setMaxValue(iv); return true; }
            if ("Duration".equals(field))      { Double dv = tryParseDouble(trimmed); if (dv == null) return false; skf.setDuration(dv); return true; }
            if ("Order".equals(field))         { Integer iv = tryParseInt(trimmed); if (iv == null) return false; skf.setOrder(iv); return true; }
            if ("Width".equals(field))         { Integer iv = tryParseInt(trimmed); if (iv == null) return false; skf.setWidth(iv); return true; }
        }
        else if (type == KeyFrameType.SHIELD && kf instanceof com.creatorskit.swing.timesheet.keyframe.ShieldKeyFrame)
        {
            com.creatorskit.swing.timesheet.keyframe.ShieldKeyFrame skf =
                    (com.creatorskit.swing.timesheet.keyframe.ShieldKeyFrame) kf;
            if ("Current Value".equals(field)) { Integer iv = tryParseInt(trimmed); if (iv == null) return false; skf.setCurrentValue(iv); return true; }
            if ("Max Value".equals(field))     { Integer iv = tryParseInt(trimmed); if (iv == null) return false; skf.setMaxValue(iv); return true; }
            if ("Duration".equals(field))      { Double dv = tryParseDouble(trimmed); if (dv == null) return false; skf.setDuration(dv); return true; }
            if ("Order".equals(field))         { Integer iv = tryParseInt(trimmed); if (iv == null) return false; skf.setOrder(iv); return true; }
            if ("Width".equals(field))         { Integer iv = tryParseInt(trimmed); if (iv == null) return false; skf.setWidth(iv); return true; }
        }
        else if (type == KeyFrameType.HEALTH && kf instanceof com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame)
        {
            com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame hkf =
                    (com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame) kf;
            if ("Current Health".equals(field)) { Integer iv = tryParseInt(trimmed); if (iv == null) return false; hkf.setCurrentHealth(iv); return true; }
            if ("Max Health".equals(field))     { Integer iv = tryParseInt(trimmed); if (iv == null) return false; hkf.setMaxHealth(iv); return true; }
            if ("Duration".equals(field))       { Double dv = tryParseDouble(trimmed); if (dv == null) return false; hkf.setDuration(dv); return true; }
            if ("Order".equals(field))          { Integer iv = tryParseInt(trimmed); if (iv == null) return false; hkf.setOrder(iv); return true; }
            if ("Width".equals(field))          { Integer iv = tryParseInt(trimmed); if (iv == null) return false; hkf.setWidth(iv); return true; }
        }
        else if ((type == KeyFrameType.HITSPLAT_1 || type == KeyFrameType.HITSPLAT_2
                || type == KeyFrameType.HITSPLAT_3 || type == KeyFrameType.HITSPLAT_4)
                && kf instanceof com.creatorskit.swing.timesheet.keyframe.HitsplatKeyFrame)
        {
            com.creatorskit.swing.timesheet.keyframe.HitsplatKeyFrame hkf =
                    (com.creatorskit.swing.timesheet.keyframe.HitsplatKeyFrame) kf;
            if ("Damage".equals(field))   { Integer iv = tryParseInt(trimmed); if (iv == null) return false; hkf.setDamage(iv); return true; }
            if ("Duration".equals(field)) { Double dv = tryParseDouble(trimmed); if (dv == null) return false; hkf.setDuration(dv); return true; }
        }
        else if (type == KeyFrameType.MODEL && kf instanceof ModelKeyFrame)
        {
            ModelKeyFrame mkf = (ModelKeyFrame) kf;
            if ("Model ID".equals(field)) { Integer iv = tryParseInt(trimmed); if (iv == null) return false; mkf.setModelId(iv); return true; }
            if ("Radius".equals(field))   { Integer iv = tryParseInt(trimmed); if (iv == null) return false; mkf.setRadius(iv); return true; }
        }
        else if (type == KeyFrameType.ANIMATION && kf instanceof AnimationKeyFrame)
        {
            AnimationKeyFrame akf = (AnimationKeyFrame) kf;
            if ("Active".equals(field))      { Integer iv = tryParseInt(trimmed); if (iv == null) return false; akf.setActive(iv); return true; }
            if ("Start Frame".equals(field)) { Integer iv = tryParseInt(trimmed); if (iv == null) return false; akf.setStartFrame(iv); return true; }
            if ("Last Frame".equals(field))  { Integer iv = tryParseInt(trimmed); if (iv == null) return false; akf.setLastFrame(iv); return true; }
            if ("Pause Ticks".equals(field)) { Integer iv = tryParseInt(trimmed); if (iv == null) return false; akf.setPauseTicks(iv); return true; }
        }
        else if (type == KeyFrameType.ORIENTATION && kf instanceof OrientationKeyFrame)
        {
            OrientationKeyFrame okf = (OrientationKeyFrame) kf;
            if ("End".equals(field))       { Integer iv = tryParseInt(trimmed); if (iv == null) return false; okf.setEnd(iv); return true; }
            if ("Duration".equals(field))  { Double dv = tryParseDouble(trimmed); if (dv == null) return false; okf.setDuration(dv); return true; }
            if ("Turn Rate".equals(field)) { Double dv = tryParseDouble(trimmed); if (dv == null) return false; okf.setTurnRate(dv); return true; }
        }
        return false;
    }

    private static Integer tryParseInt(String s)
    {
        if (s == null) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Double tryParseDouble(String s)
    {
        if (s == null) return null;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * Returns true if the named iterable field expects a string value
     * (currently just {@code Projectile.Target}). Used by the dialog to
     * choose a sensible default source on open / field-change: numeric
     * fields jump to the Arithmetic radio; string fields stick with
     * folder pattern (v1 default).
     */
    private static boolean isIterableStringField(String field)
    {
        return "Target".equals(field);
    }

    /**
     * Shifts every per-Character keyframe with {@code tick > currentTime}
     * forward by {@code amount} on every target from {@link #resolveSelectionTargets},
     * plus the same shift on global keyframes (Camera / Fade / Shake) in
     * the central store. Single undoable KeyFrameAction batch.
     *
     * <p>Iterates in descending tick order so the re-add of a shifted kf
     * never lands on a slot still held by a later kf (later kf has already
     * been moved out of the way). Strict inequality on the cutoff means
     * a keyframe sitting exactly on the playhead tick stays put -- the
     * inserted gap opens to its right.
     */
    public void executeRippleInsertAtPlaybar(double amount)
    {
        if (amount <= 0) return;
        double cutoff = currentTime;
        java.util.Collection<Character> targets = resolveSelectionTargets();

        KeyFrameAction[] kfa = new KeyFrameAction[0];

        // --- LOCAL types: walk each Character's frame matrix.
        for (Character owner : targets)
        {
            KeyFrame[][] frames = owner.getFrames();
            if (frames == null) continue;

            for (int i = 0; i < frames.length; i++)
            {
                KeyFrame[] row = frames[i];
                if (row == null) continue;
                KeyFrameType type = KeyFrameType.getKeyFrameType(i);
                if (isGlobalType(type)) continue;  // handled below

                kfa = shiftRowAfterCutoff(row, kfa, cutoff, amount, owner, null);
            }
        }

        // --- GLOBAL types: walk the central store.
        com.creatorskit.saves.GlobalKeyFrames store = plugin.getGlobalKeyFrames();
        if (store != null)
        {
            for (KeyFrameType globalType : KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL)
            {
                kfa = shiftRowAfterCutoff(store.getGlobalKeyFramesByType(globalType), kfa, cutoff, amount, null, store);
            }
        }

        if (kfa.length == 0)
        {
            JOptionPane.showMessageDialog(this,
                    String.format("No keyframes after tick %.1f to shift.", cutoff),
                    "Ripple Insert", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        addKeyFrameActions(kfa);
        // The marquee may have held refs that got replaced; clear it so we
        // don't dangle pointers into the card placeholder state.
        setSelectedKeyFrames(new KeyFrame[0]);
        if (client != null && client.getGameState() == GameState.LOGGED_IN)
        {
            toolBox.getProgrammer().updatePrograms(currentTime);
        }
        attributePanel.setKeyFramedIcon(true);
        if (selectedCharacter != null)
        {
            attributePanel.resetAttributes(selectedCharacter, currentTime);
        }
        repaint();
    }

    /**
     * Shared inner loop for Ripple Insert. Walks a keyframe array (Character
     * row OR global store array) in descending tick order and moves
     * everything strictly after {@code cutoff} forward by {@code amount}.
     * Exactly one of {@code owner} (local) or {@code store} (global) must be
     * non-null -- that controls which add/remove API the shift goes through.
     */
    private KeyFrameAction[] shiftRowAfterCutoff(KeyFrame[] arr, KeyFrameAction[] kfa, double cutoff, double amount,
                                                  Character owner, com.creatorskit.saves.GlobalKeyFrames store)
    {
        if (arr == null) return kfa;
        KeyFrame[] snapshot = arr.clone();
        // Descending sort so the highest-tick kf is processed first. Without
        // this, a kf at tick T moving to T+amount could land on an unmoved
        // kf at T+amount, and addKeyFrame's same-tick-displaces behaviour
        // would clobber the still-to-move target.
        java.util.Arrays.sort(snapshot, (a, b) ->
        {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return Double.compare(b.getTick(), a.getTick());
        });
        for (KeyFrame kf : snapshot)
        {
            if (kf == null) continue;
            if (kf.getTick() <= cutoff) continue;
            double newTick = round(kf.getTick() + amount);
            KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
            if (owner != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, owner, KeyFrameCharacterActionType.ADD));
                owner.removeKeyFrame(kf);
                owner.addKeyFrame(replacement, currentTime);
            }
            else if (store != null)
            {
                store.remove(kf);
                KeyFrame displaced = store.add(replacement);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, null, KeyFrameCharacterActionType.REMOVE));
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, null, KeyFrameCharacterActionType.ADD));
                if (displaced != null && displaced != kf)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(displaced, null, KeyFrameCharacterActionType.REMOVE));
                }
            }
        }
        return kfa;
    }

    // ===== Blocks (Phase 1) ============================================

    /**
     * Block(s) the user has selected via left-click on the block rect.
     * Parallel to {@link #selectedKeyFrames}: clicking a block sets BOTH
     * the keyframe selection (to the block's members) AND this list so the
     * Delete-key handler can offer the "delete block + its keyframes"
     * confirmation. Cleared the moment any other selection path fires
     * (marquee, keyframe click, Tools action).
     */
    @Getter
    private java.util.List<com.creatorskit.swing.timesheet.keyframe.Block> selectedBlocks = new java.util.ArrayList<>();
    /** Parallel list of the Character that owns each selected block. */
    private java.util.List<Character> selectedBlockOwners = new java.util.ArrayList<>();

    public void setSelectedBlock(com.creatorskit.swing.timesheet.keyframe.Block block, Character owner)
    {
        selectedBlocks = new java.util.ArrayList<>(java.util.Collections.singletonList(block));
        selectedBlockOwners = new java.util.ArrayList<>(java.util.Collections.singletonList(owner));
    }

    public void clearBlockSelection()
    {
        selectedBlocks = new java.util.ArrayList<>();
        selectedBlockOwners = new java.util.ArrayList<>();
    }

    /** Removes the block grouping from {@code character} but leaves all
     *  member keyframes in place. The user's "dissolve" action -- the
     *  keyframes go back to ungrouped life on the timeline. */
    public void dissolveBlock(Character character, com.creatorskit.swing.timesheet.keyframe.Block block)
    {
        if (character == null || block == null) return;
        character.getBlocks().remove(block);
        clearBlockSelection();
        attributeSheet.repaint();
        summarySheet.repaint();
    }

    /** Shows a confirmation dialog then deletes both the block AND every
     *  member keyframe. The "Delete block + keyframes..." action -- routes
     *  the kf removals through {@link #addKeyFrameActions} so undo can
     *  restore them in one step. Members are resolved live from the
     *  Character's frame matrix using the block's [startTick, endTick]
     *  range (range-based block model). */
    public void deleteBlockWithConfirm(Character character, com.creatorskit.swing.timesheet.keyframe.Block block)
    {
        if (character == null || block == null) return;
        java.util.List<com.creatorskit.swing.timesheet.keyframe.KeyFrame> members =
                block.resolveMembers(character);
        int n = members.size();
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete block \"" + (block.getName() == null ? "(unnamed)" : block.getName())
                        + "\" and its " + n + " keyframe" + (n == 1 ? "" : "s") + "?",
                "Delete block",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (com.creatorskit.swing.timesheet.keyframe.KeyFrame kf : members)
        {
            if (kf == null) continue;
            character.removeKeyFrame(kf);
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, character, KeyFrameCharacterActionType.REMOVE));
        }
        character.getBlocks().remove(block);
        if (kfa.length > 0) addKeyFrameActions(kfa);
        clearBlockSelection();
        setSelectedKeyFrames(new com.creatorskit.swing.timesheet.keyframe.KeyFrame[0]);
    }

    /** Pops the BlockEditDialog pre-filled with the block's current name
     *  and colour. On OK, writes back; on cancel, leaves the block as-is. */
    public void editBlockNameAndColour(Character character, com.creatorskit.swing.timesheet.keyframe.Block block)
    {
        if (block == null) return;
        com.creatorskit.swing.timesheet.blocks.BlockEditDialog.Result result =
                com.creatorskit.swing.timesheet.blocks.BlockEditDialog.show(this,
                        "Rename / Recolour block", block.getName(), block.getColorRgb());
        if (result == null) return;
        block.setName(result.name);
        block.setColorRgb(result.colorRgb);
        attributeSheet.repaint();
        summarySheet.repaint();
    }

    // ===== Blocks (Phase 1: create + render) ============================

    /**
     * Whether the current marquee selection forms at least one valid block
     * on at least one Character. Drives the enabled state of Tools > Create
     * Block and the right-click "Create Block" context menu item.
     */
    public boolean canCreateBlockFromSelection()
    {
        if (selectedKeyFrames == null || selectedKeyFrames.length < 2) return false;
        java.util.Map<Character, java.util.List<KeyFrame>> byOwner = groupSelectedKeyFramesByOwner();
        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> e : byOwner.entrySet())
        {
            if (com.creatorskit.swing.timesheet.keyframe.BlockValidator.isValidSelection(e.getKey(), e.getValue()))
            {
                double[] range = com.creatorskit.swing.timesheet.keyframe.BlockValidator.tickRange(e.getValue());
                if (!com.creatorskit.swing.timesheet.keyframe.BlockValidator.overlapsExistingBlock(e.getKey(), range[0], range[1]))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tools > Create Block / right-click "Create Block". Groups the marquee
     * by owner, runs the validator per Character, surfaces a count of
     * valid blocks vs total Characters with a selection, then prompts the
     * user once for a shared name + colour. Per the design: one prompt,
     * same name and colour for every Character that contributes a valid
     * block; Characters whose selection is invalid (gaps) or overlaps an
     * existing block are skipped with a warning.
     */
    public void showCreateBlockDialog()
    {
        if (selectedKeyFrames == null || selectedKeyFrames.length < 2)
        {
            JOptionPane.showMessageDialog(this,
                    "Select at least 2 keyframes (the easiest way is to marquee-drag a rectangle) before creating a block.",
                    "Create block", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        java.util.Map<Character, java.util.List<KeyFrame>> byOwner = groupSelectedKeyFramesByOwner();
        if (byOwner.isEmpty())
        {
            JOptionPane.showMessageDialog(this,
                    "Couldn't resolve any owners for the selected keyframes.",
                    "Create block", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Partition into "valid for block" and "invalid" with a reason
        // string per invalid Character so the warning surfaces specifics.
        java.util.LinkedHashMap<Character, java.util.List<KeyFrame>> validByOwner = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<Character, String> invalidReasons = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> e : byOwner.entrySet())
        {
            Character c = e.getKey();
            java.util.List<KeyFrame> sel = e.getValue();
            if (!com.creatorskit.swing.timesheet.keyframe.BlockValidator.isValidSelection(c, sel))
            {
                invalidReasons.put(c, "selection has gaps in an included property");
                continue;
            }
            double[] range = com.creatorskit.swing.timesheet.keyframe.BlockValidator.tickRange(sel);
            if (com.creatorskit.swing.timesheet.keyframe.BlockValidator.overlapsExistingBlock(c, range[0], range[1]))
            {
                invalidReasons.put(c, "range overlaps an existing block");
                continue;
            }
            validByOwner.put(c, sel);
        }

        if (validByOwner.isEmpty())
        {
            StringBuilder msg = new StringBuilder("No valid blocks can be created from this selection:\n");
            for (java.util.Map.Entry<Character, String> e : invalidReasons.entrySet())
            {
                msg.append("\n - ").append(e.getKey().getName()).append(": ").append(e.getValue());
            }
            JOptionPane.showMessageDialog(this, msg.toString(),
                    "Create block", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirmation with the "X blocks across Y Characters" headline the
        // spec asked for. Invalid Characters get a tail message so the user
        // knows they were skipped.
        StringBuilder warn = new StringBuilder();
        warn.append("Create ").append(validByOwner.size()).append(" block");
        if (validByOwner.size() != 1) warn.append("s");
        warn.append(" across ").append(validByOwner.size()).append(" Character");
        if (validByOwner.size() != 1) warn.append("s");
        warn.append("?");
        if (!invalidReasons.isEmpty())
        {
            warn.append("\n\nSkipping ").append(invalidReasons.size())
                    .append(" Character").append(invalidReasons.size() == 1 ? "" : "s")
                    .append(" with invalid selections:");
            for (java.util.Map.Entry<Character, String> e : invalidReasons.entrySet())
            {
                warn.append("\n  - ").append(e.getKey().getName()).append(": ").append(e.getValue());
            }
        }
        int confirm = JOptionPane.showConfirmDialog(this, warn.toString(),
                "Create block", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        // Name + colour prompt: shared across every Character per spec.
        com.creatorskit.swing.timesheet.blocks.BlockEditDialog.Result result =
                com.creatorskit.swing.timesheet.blocks.BlockEditDialog.show(this, "New block",
                        "Block " + (validByOwner.values().iterator().next().size()) + " keyframes",
                        com.creatorskit.swing.timesheet.keyframe.BlockPalette.DEFAULT_RGB);
        if (result == null) return; // user cancelled

        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> e : validByOwner.entrySet())
        {
            double[] range = com.creatorskit.swing.timesheet.keyframe.BlockValidator.tickRange(e.getValue());
            com.creatorskit.swing.timesheet.keyframe.Block block =
                    new com.creatorskit.swing.timesheet.keyframe.Block(
                            result.name, result.colorRgb, range[0], range[1]);
            e.getKey().getBlocks().add(block);
        }
        // Refresh the visible attribute sheets so the new block paints.
        attributeSheet.repaint();
        if (globalAttributeSheet != null) globalAttributeSheet.repaint();
        summarySheet.repaint();
    }
}
