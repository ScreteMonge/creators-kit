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
    private JLabel[] labels = new JLabel[0]; // Local-view labels: labels[0] = spacer, labels[1..17] = type rows
    private JLabel[] globalLabels = new JLabel[0]; // Global-view labels: globalLabels[0] = spacer, globalLabels[1..3] = type rows

    private double zoom = 50;
    private double hScroll = 0;
    private double maxHScroll = 200;
    private double minHScroll = -10;

    private double currentTime = 0;
    private boolean pauseScrollBarListener = false;
    private Character selectedCharacter;
    private final com.creatorskit.selection.SelectionManager selectionManager;

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
                KeyFrame[] arr;
                if (type == KeyFrameType.CAMERA) arr = store.getCameraKeyFramesSafe();
                else if (type == KeyFrameType.SCREEN_FADE) arr = store.getScreenFadeKeyFramesSafe();
                else if (type == KeyFrameType.SCREEN_SHAKE) arr = store.getScreenShakeKeyFramesSafe();
                else arr = new KeyFrame[0];
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
    public TimeSheetPanel(@Nullable Client client, ToolBoxFrame toolBox, CreatorsPlugin plugin, CreatorsConfig config, ClientThread clientThread, DataFinder dataFinder, ManagerTree managerTree, MovementManager movementManager, com.creatorskit.selection.SelectionManager selectionManager)
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

        setLayout(new GridBagLayout());
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
        return type == KeyFrameType.CAMERA
                || type == KeyFrameType.SCREEN_FADE
                || type == KeyFrameType.SCREEN_SHAKE;
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
        if (globalType && chars.isEmpty())
        {
            for (double tick : ticks)
            {
                KeyFrame oldKf = findGlobalKeyFrameAt(type, tick);
                if (oldKf == null) continue;
                KeyFrame newKf = attributePanel.createKeyFrame(type, tick);
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

                KeyFrame newKf = attributePanel.createKeyFrame(type, tick);
                if (newKf == null)
                {
                    continue;
                }

                if (type == KeyFrameType.MOVEMENT)
                {
                    MovementKeyFrame oldKF = (MovementKeyFrame) oldKeyFrame;
                    MovementKeyFrame newKF = (MovementKeyFrame) newKf;
                    newKF.setPlane(oldKF.getPlane());
                    newKF.setPoh(oldKF.isPoh());
                    newKF.setPath(oldKF.getPath());
                    newKF.setCurrentStep(0);
                    newKF.setStepClientTick(0);
                }

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

    public void initializeOrientationKeyFrame(Character character, OrientationHotkeyMode hotkeyMode, LocalPoint localPoint, double tick, int start, int end, OrientationGoal og, int turnRate)
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

        // turnRate scales with step speed so a turn completes within roughly one tile of
        // movement (180 degrees per game tick at speed 1, faster at speed 2).
        int speedAwareTurnRate = (int) Math.round(OrientationKeyFrame.TURN_RATE * stepSpeed);

        MovementKeyFrame previous = findLastMovementKeyFrame(selectedCharacter);
        if (newKeyFrame || previous == null)
        {
            // First step: resolve a real source tile and pathfind from there to the
            // click. Source-resolution order:
            //   1. The Character's live CKObject location (valid once spawned in scene).
            //   2. The Character's saved instancedPoint (POH) or nonInstancedPoint
            //      (world). Used when the Character has been added but not yet drawn
            //      in the scene -- otherwise srcX/srcY would fall through to the
            //      clicked tile and the pathfinder would return an empty path,
            //      producing a 0-length keyframe at the clicked tile.
            //   3. Last resort: the clicked tile itself (path will be 1-tile = no
            //      movement; the chained-step branch on the next add-step will pick
            //      up from here).
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

            // Skip the no-movement degenerate case: if pathfinder returned no steps
            // (source == destination, or src resolution failed entirely), don't create
            // a 0-length keyframe at tick 0. The user wanted "skip the 0-length and
            // add the program step at tick 1 instead", which falls out naturally:
            // returning here means no keyframe is created, and the next add-step is
            // still treated as a "first step" (previous == null) so it lands on the
            // seeker's current tick with real movement.
            if (path.length <= 1)
            {
                return;
            }

            // Place the first keyframe at exactly the seeker's current tick. Tick 0
            // is valid (the saved-position fallback above ensures the path has real
            // movement even if the Character hasn't been drawn yet, so we never
            // produce a 0-length placeholder here).
            initializeMovementKeyFrame(selectedCharacter, currentTime, worldView.getPlane(), poh, path, false, stepSpeed, speedAwareTurnRate);

            // Auto-advance the seeker to the end of the keyframe we just placed so the
            // next add-step lands chained immediately after. Ceil the duration so the
            // seeker lands on an integer tick that matches AttributeSheet's
            // ceil()-rounded movement-bar width -- without this, fractional durations
            // (e.g. 3 tiles at speed 2 = 1.5 ticks) put the seeker between integer
            // ticks while the visual bar extends to the next integer, looking like
            // a 0.5-tick overlap for whatever step the user adds next.
            double tilesMoved = Math.max(0, path.length - 1);
            double newDuration = Math.ceil(tilesMoved / Math.max(0.0001, stepSpeed));
            setCurrentTime(currentTime + newDuration, false);
        }
        else
        {
            // Chained step: pathfind from the previous keyframe's end tile to the
            // clicked tile. The new MovementKeyFrame's path is the FULL pathfinder
            // output (multiple tiles for far-away clicks), so a 10-tile click takes
            // ~10 ticks at speed 1 instead of teleporting in one tick.
            int[] prevEnd = previous.getPath()[previous.getPath().length - 1];
            MovementKeyFrame pathfindSeed = new MovementKeyFrame(
                    0,
                    previous.getPlane(),
                    previous.isPoh(),
                    new int[][]{prevEnd},
                    0,
                    0,
                    false,
                    stepSpeed,
                    0);
            int[][] newPath = movementManager.addProgramStep(pathfindSeed, worldView, localPoint);

            // Schedule the new keyframe right after the previous one finishes.
            // Ceil the duration to an integer tick: AttributeSheet draws the
            // movement bar at ceil(steps/speed) wide when nothing follows, so
            // a fractional duration like 1.5 (3 tiles at speed 2) gets visually
            // rounded up to a 2-tick bar. Placing the next keyframe at the
            // exact 1.5 then puts it under that bar -- the user sees a 0.5-tick
            // overlap and has to drag the new step out by 1 tick. Snapping the
            // chained tick to the same ceil() the renderer uses keeps them in
            // sync at integer ticks.
            int prevTiles = previous.getPath().length;
            double prevDuration;
            if (prevTiles <= 1)
            {
                prevDuration = 1.0;
            }
            else
            {
                prevDuration = Math.ceil((prevTiles - 1) / Math.max(0.0001, previous.getSpeed()));
            }
            double newTick = previous.getTick() + prevDuration;

            initializeMovementKeyFrame(selectedCharacter, newTick, worldView.getPlane(), poh, newPath, false, stepSpeed, speedAwareTurnRate);

            // Auto-advance the seeker to the end of this just-placed keyframe.
            // Same ceil reason as above -- keeps the seeker on an integer tick
            // that lines up with the bar's drawn end.
            double tilesMoved = Math.max(0, newPath.length - 1);
            double newDuration = Math.ceil(tilesMoved / Math.max(0.0001, stepSpeed));
            setCurrentTime(newTick + newDuration, false);
        }

        programmer.register3DChanges(selectedCharacter);
    }

    /**
     * Returns the latest-in-time MovementKeyFrame for the Character, or null if there
     * are none. Used by onAddMovement to chain a new step after the existing path.
     */
    private MovementKeyFrame findLastMovementKeyFrame(Character character)
    {
        KeyFrame[] frames = character.getKeyFrames(KeyFrameType.MOVEMENT);
        if (frames == null || frames.length == 0)
        {
            return null;
        }
        return (MovementKeyFrame) frames[frames.length - 1];
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
                92);

        addKeyFrameAction(new KeyFrame[]{keyFrame});
    }

    /**
     * Creates a ProjectileKeyFrame using the selected projectile spotanim's id, anchored
     * at the current seeker tick on the selected Character. Other ProjectileKeyFrame
     * fields (target, height, slope, etc.) default to the constants on the class so the
     * user can edit them in the AttributePanel afterwards.
     */
    public void addProjectileKeyFrameFromCache(SpotanimData spotanimData)
    {
        if (selectedCharacter == null)
        {
            return;
        }

        ProjectileKeyFrame keyFrame = new ProjectileKeyFrame(
                plugin.getCurrentTick(),
                spotanimData.getId(),
                "",
                ProjectileKeyFrame.DEFAULT_START_HEIGHT,
                ProjectileKeyFrame.DEFAULT_END_HEIGHT,
                ProjectileKeyFrame.DEFAULT_SLOPE,
                ProjectileKeyFrame.DEFAULT_START_POS,
                ProjectileKeyFrame.DEFAULT_DURATION,
                ProjectileKeyFrame.DEFAULT_START_DELAY,
                ProjectileKeyFrame.DEFAULT_FACE_TRAJECTORY);

        addKeyFrameAction(new KeyFrame[]{keyFrame});
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
                    src.getHeight());
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

        onCurrentTimeChanged(tick);
    }

    public void onCurrentTimeChanged(double tick)
    {
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
        treeScrollPane.setPreferredSize(new Dimension(614, 0));

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
        attributePanel = new AttributePanel(client, clientThread, config, this, dataFinder, selectionManager);
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
        labels = buildLabelColumn(labelScrollPane,
                attributeSheet,
                KeyFrameType.LOCAL_KEYFRAME_TYPES_ALPHABETICAL,
                /*ownsToggleButton=*/ true);
        globalLabels = buildLabelColumn(globalLabelScrollPane,
                globalAttributeSheet,
                KeyFrameType.GLOBAL_KEYFRAME_TYPES_ALPHABETICAL,
                /*ownsToggleButton=*/ false);

        // Wire the two CardLayouts so the toggle button (built inside the
        // local column above) can swap both at once. Default to local view;
        // refreshGlobalRowsOnlyMode below picks the right view based on
        // selection state.
        labelCards.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelCards.add(labelScrollPane, VIEW_LOCAL);
        labelCards.add(globalLabelScrollPane, VIEW_GLOBAL);

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
        JPanel labelPanel = new JPanel();
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
        labelPanel.setFocusable(true);

        scrollPane.setViewportView(labelPanel);
        scrollPane.setBorder(new EmptyBorder(1, 0, 1, 0));
        scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.setPreferredSize(new Dimension(100, 150));
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
        bodySheet.setContentRowCount(types.length);

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

        JButton toggle = new JButton();
        toggle.setFocusable(false);
        toggle.setBorder(null);
        toggle.setMargin(new java.awt.Insets(0, 4, 0, 4));
        toggle.setPreferredSize(new Dimension(100, 24));
        toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.setBackground(ColorScheme.DARK_GRAY_COLOR);
        toggle.setForeground(Color.WHITE);
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

        JLabel[] result = new JLabel[types.length + 1];
        result[0] = new JLabel(); // dummy for the +1 index alignment

        for (int i = 0; i < types.length; i++)
        {
            JLabel label = new JLabel();
            label.setFocusable(true);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setOpaque(true);
            label.setPreferredSize(new Dimension(100, 24));
            label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            label.setBackground(i == 0 ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
            label.setText(types[i].getName() + LABEL_OFFSET);

            label.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    super.mousePressed(e);
                    String name = label.getText().replaceAll(LABEL_OFFSET, "");
                    if (e.isControlDown())
                    {
                        // CTRL+click on a property label: bulk-toggle every
                        // keyframe of that property across selected Characters
                        // (or globals from the central store). Mirrors the
                        // manager tree CTRL multi-select model -- a click on
                        // an already-fully-selected group deselects it; on a
                        // partly / not selected group adds the missing ones.
                        // Also switch the card so the user lands on the
                        // attribute panel for the property they just acted on.
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

            result[i + 1] = label;
            labelPanel.add(label);
        }
        return result;
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
            all = ArrayUtils.addAll(all, store.getCameraKeyFramesSafe());
            all = ArrayUtils.addAll(all, store.getScreenFadeKeyFramesSafe());
            all = ArrayUtils.addAll(all, store.getScreenShakeKeyFramesSafe());
        }

        setSelectedKeyFrames(all);
    }

    public void onDeleteKeyPressed()
    {
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

    private void setupManager()
    {
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.gridheight = 2;
        c.weightx = 0;
        c.weighty = 5;
        c.gridx = 0;
        c.gridy = 0;
        add(treeScrollPane, c);

        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 0;
        JLabel summaryLabel = new JLabel("Summary");
        summaryLabel.setFont(FontManager.getRunescapeBoldFont());
        summaryLabel.setBorder(new EmptyBorder(1, 2, 2, 2));
        add(summaryLabel, c);

        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 8;
        c.weighty = 5;
        c.gridx = 2;
        c.gridy = 1;
        add(summarySheet, c);

        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 2;
        add(scrollBar, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 3;
        add(controlPanel, c);

        c.gridheight = 3;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 2;
        add(attributePanel, c);

        // gridy=4 hosts the property labels (left) + timeline body (right).
        // weighty>0 here lets the user grow the window vertically and have
        // the timeline rows expand to show more properties before cropping.
        // Without this, the row was pinned at the labelScrollPane's prefSize
        // (150px), which only fit ~5 of 17 local rows.
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 4;
        add(labelCards, c);

        c.weightx = 8;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 4;
        add(sheetCards, c);
    }

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
        // Copies per Character is a min..max range -- each Character rolls
        // independently in that range. min == max behaves like a fixed count
        // (original single-spinner UX preserved when both set to the same
        // value). min == 0 lets a Character end up with its original block
        // simply deleted and zero copies added, useful for sparse rain where
        // not every tile should hit. Default 1..1 matches the previous default.
        JSpinner copiesMinSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1000, 1));
        JSpinner copiesMaxSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1000, 1));
        // Per-step copy count is an ALTERNATE mode (max > 0 enables it). When
        // active, the planner walks step ticks across [from, to] and for each
        // tick rolls K in [perStepMin, perStepMax] -- K distinct Characters
        // (without replacement, clamped to byOwner.size()) each get a copy of
        // their block anchored at that step tick. Density per step varies
        // independently from per-Character copy count. Use case: rain that
        // gets denser on certain beats. Both = 0 (default) preserves the
        // existing per-Character planning behaviour completely.
        JSpinner perStepMinSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        JSpinner perStepMaxSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("From tick:"));
        panel.add(fromSpinner);
        panel.add(new JLabel("To tick:"));
        panel.add(toSpinner);
        panel.add(new JLabel("Step size (min):"));
        panel.add(stepMinSpinner);
        panel.add(new JLabel("Step size (max):"));
        panel.add(stepMaxSpinner);
        panel.add(new JLabel("Copies per Character (min):"));
        panel.add(copiesMinSpinner);
        panel.add(new JLabel("Copies per Character (max):"));
        panel.add(copiesMaxSpinner);
        panel.add(new JLabel("Copies per step (min):"));
        panel.add(perStepMinSpinner);
        panel.add(new JLabel("Copies per step (max, 0 = off):"));
        panel.add(perStepMaxSpinner);

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

        // Branch to the per-step planner when the user enabled it (max > 0).
        // That mode is structurally different -- the step grid is global, K
        // copies fire per step tick drawn from random Characters (without
        // replacement, clamped to source count) -- so it lives in its own
        // method instead of trying to overload the per-Character feasibility
        // logic. Per-Character mode below is unchanged.
        if (perStepMax > 0)
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
     * Per-step mode of Scatter (active when "Copies per step (max)" > 0).
     *
     * <p>The step grid is GLOBAL here -- one step value rolled in [stepMin,
     * stepMax] drives every Character. At each step tick T in [from, to]:
     *   K = roll in [perStepMin, perStepMax], clamped to the count of
     *       Characters whose block still fits within fTo at this tick.
     *   K distinct Characters are picked (Fisher-Yates shuffle, first K) --
     *   each gets ONE copy of their block anchored at T.
     *
     * <p>"Density per beat" is the use case: K varies independently per step
     * so some beats fire more drops than others. Same-Character overlap
     * across step ticks IS allowed (the block at T=0 and at T=3 with
     * duration 5 will visually overlap in [3, 5]) -- per-step mode treats
     * that as intentional rain-density behaviour, not a planning failure.
     * Same-tick collisions for the same Character can't happen because the
     * within-step Character pick is without replacement.
     *
     * <p>Originals are removed once per Character and replaced with the
     * planned copies in a single undo group (via finalizeTickTransform).
     */
    private void scatterPerStep(java.util.Map<Character, java.util.List<KeyFrame>> byOwner,
                                double fFrom, double fTo,
                                int stepMin, int stepMax,
                                int perStepMin, int perStepMax)
    {
        // Block template: minTick (anchor for "delta from anchor" math) +
        // duration (max - min, used for feasibility filtering at each step).
        java.util.LinkedHashMap<Character, double[]> templates = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            double minTick = Double.POSITIVE_INFINITY;
            double maxTick = Double.NEGATIVE_INFINITY;
            for (KeyFrame kf : entry.getValue())
            {
                if (kf.getTick() < minTick) minTick = kf.getTick();
                if (kf.getTick() > maxTick) maxTick = kf.getTick();
            }
            templates.put(entry.getKey(), new double[]{minTick, maxTick - minTick});
        }

        final java.util.Random rng = new java.util.Random();
        // Roll once: global step shared by every step tick. Rolling per-step
        // would scramble the cadence so the user couldn't tell the density
        // variation apart from random timing.
        int globalStep = stepMin == stepMax
                ? stepMin
                : stepMin + rng.nextInt(stepMax - stepMin + 1);

        java.util.LinkedHashMap<Character, java.util.List<Double>> planned = new java.util.LinkedHashMap<>();
        for (Character owner : byOwner.keySet())
        {
            planned.put(owner, new ArrayList<>());
        }

        java.util.List<Character> ownerList = new ArrayList<>(byOwner.keySet());
        // Integer step counter (not double accumulation) so the final step
        // tick lands exactly at fFrom + N*step without floating-point drift.
        int totalSteps = (int) Math.floor((fTo - fFrom) / globalStep) + 1;
        for (int sIdx = 0; sIdx < totalSteps; sIdx++)
        {
            double T = fFrom + (double) sIdx * globalStep;

            // Only Characters whose block fits [T, T+dur] within [fFrom, fTo]
            // are eligible at this tick. A Character with a 5-tick block can't
            // be placed at T = fTo - 2 because it would spill past fTo.
            java.util.List<Character> feasible = new ArrayList<>();
            for (Character owner : ownerList)
            {
                double dur = templates.get(owner)[1];
                if (T + dur <= fTo + 1e-9)
                {
                    feasible.add(owner);
                }
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

        // Commit: remove originals, add planned copies. Mirrors the structure
        // of the per-Character commit phase so finalizeTickTransform sees a
        // single combined undo group.
        KeyFrameAction[] kfa = new KeyFrameAction[0];
        java.util.List<KeyFrame> newSelected = new ArrayList<>();
        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            java.util.List<KeyFrame> originalBlock = entry.getValue();
            double blockMin = templates.get(owner)[0];

            for (KeyFrame kf : originalBlock)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                owner.removeKeyFrame(kf);
            }

            for (double anchor : planned.get(owner))
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
            if (scope == null || scope == KeyFrameType.CAMERA)
            {
                kfa = rippleGlobalArray(store.getCameraKeyFramesSafe(), kfa, from, to, removed, store);
            }
            if (scope == null || scope == KeyFrameType.SCREEN_FADE)
            {
                kfa = rippleGlobalArray(store.getScreenFadeKeyFramesSafe(), kfa, from, to, removed, store);
            }
            if (scope == null || scope == KeyFrameType.SCREEN_SHAKE)
            {
                kfa = rippleGlobalArray(store.getScreenShakeKeyFramesSafe(), kfa, from, to, removed, store);
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
}
