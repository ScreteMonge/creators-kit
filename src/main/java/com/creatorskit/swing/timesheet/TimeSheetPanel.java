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
    private TreeScrollPane treeScrollPane;
    private final ManagerTree managerTree;
    private MovementManager movementManager;

    private final JComboBox<KeyFrameType> summaryComboBox = new JComboBox<>();
    private final JSpinner timeSpinner = new JSpinner();
    private boolean triggerTimeSpinnerChange = true;
    private JScrollBar scrollBar;
    private AttributePanel attributePanel;
    private final JScrollPane labelScrollPane = new JScrollPane();
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
    private JLabel[] labels = new JLabel[0];

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

    private final int UNDO_LIMIT = 15;
    private int undoStack = 0;

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
        if (selectedCharacter == null)
        {
            return;
        }

        KeyFrame keyFrame = selectedCharacter.findNextKeyFrame(currentTime);
        if (keyFrame == null)
        {
            return;
        }

        setCurrentTime(keyFrame.getTick(), false);
    }

    public void onAttributeSkipPrevious()
    {
        if (selectedCharacter == null)
        {
            return;
        }

        KeyFrame keyFrame = selectedCharacter.findPreviousKeyFrame(currentTime);
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

    public void onKeyFrameIconPressedEvent(double currentTick, KeyFrameType type)
    {
        if (selectedCharacter == null)
        {
            return;
        }

        KeyFrame keyFrame = selectedCharacter.findKeyFrame(type, currentTick);
        if (keyFrame == null)
        {
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
        java.util.Collection<Character> targets = resolveSelectionTargets();
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
        if (targets.isEmpty())
        {
            return;
        }

        KeyFrameAction[] kfa = new KeyFrameAction[0];
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
                KeyFrame perChar = isPrimary ? template : KeyFrame.createCopy(template, template.getTick());
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(perChar, c, KeyFrameCharacterActionType.ADD));

                KeyFrame keyFrameToReplace = addKeyFrame(c, perChar);
                if (keyFrameToReplace != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, c, KeyFrameCharacterActionType.REMOVE));
                }
            }
        }

        addKeyFrameActions(kfa);
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

        // Fallback: no marquee match -> previous KF of type on primary at seeker.
        if (ticks.isEmpty())
        {
            if (selectedCharacter == null)
            {
                return;
            }
            KeyFrame keyFrame = selectedCharacter.findPreviousKeyFrame(type, currentTime, true);
            if (keyFrame == null)
            {
                return;
            }
            ticks.add(keyFrame.getTick());
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
            }
        }

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
            // next add-step lands chained immediately after.
            double tilesMoved = Math.max(0, path.length - 1);
            double newDuration = tilesMoved / Math.max(0.0001, stepSpeed);
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
            // Walking (speed 1) puts the next keyframe 1 tick per tile after this one;
            // running (speed 2) puts it 0.5 ticks per tile after.
            int prevTiles = previous.getPath().length;
            double prevDuration;
            if (prevTiles <= 1)
            {
                prevDuration = 1.0;
            }
            else
            {
                prevDuration = (prevTiles - 1) / Math.max(0.0001, previous.getSpeed());
            }
            double newTick = previous.getTick() + prevDuration;

            initializeMovementKeyFrame(selectedCharacter, newTick, worldView.getPlane(), poh, newPath, false, stepSpeed, speedAwareTurnRate);

            // Auto-advance the seeker to the end of this just-placed keyframe.
            double tilesMoved = Math.max(0, newPath.length - 1);
            double newDuration = tilesMoved / Math.max(0.0001, stepSpeed);
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
     * Adds the requested HitsplatKeyFrame and one bar keyframe whose value drops by
     * the hitsplat's damage. The bar is picked from the hitsplat's SPRITE:
     *
     * <ul>
     *   <li>SHIELD / SHIELD_OTHER / SHIELD_MAX -> drains the Shield bar</li>
     *   <li>POISE  / POISE_OTHER  / POISE_MAX  -> drains the Special bar</li>
     *   <li>everything else -> drains the Health bar (legacy behaviour)</li>
     * </ul>
     *
     * <p>For each bar type the previous keyframe of that bar is read as the source
     * of truth for duration / colour-or-sprite / max / current. If no previous
     * keyframe exists, falls back to the bar's natural defaults so the user gets
     * a sensible starting state.
     */
    public void initializeHealthKeyFrame(KeyFrameType type)
    {
        java.util.Collection<Character> targets = resolveSelectionTargets();
        if (targets.isEmpty())
        {
            return;
        }

        // Build the hitsplat template once from the card's current values. Per-
        // Character we clone the template so each owner has its own KeyFrame
        // instance (state-bearing keyframes can't safely be shared). The bar
        // drain is computed per-Character against THAT character's previous bar
        // keyframe so each one's "remaining" value reflects their own state.
        KeyFrame hitsplatTemplate = attributePanel.createKeyFrame(type, currentTime);
        if (hitsplatTemplate == null)
        {
            return;
        }
        HitsplatKeyFrame template = (HitsplatKeyFrame) hitsplatTemplate;
        com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite sprite = template.getSprite();
        int damage = template.getDamage();

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        boolean primaryProcessed = false;
        for (Character c : targets)
        {
            // Primary keeps the template instance so any caller holding the
            // returned reference still sees its object; secondary characters
            // get clones.
            boolean isPrimary = !primaryProcessed && c == selectedCharacter;
            KeyFrame hits = isPrimary
                    ? hitsplatTemplate
                    : KeyFrame.createCopy(hitsplatTemplate, hitsplatTemplate.getTick());
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(hits, c, KeyFrameCharacterActionType.ADD));
            KeyFrame replacedHits = addKeyFrame(c, hits);
            if (replacedHits != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacedHits, c, KeyFrameCharacterActionType.REMOVE));
            }

            KeyFrame bar = pickBarKeyFrameForHitsplat(c, sprite, damage);
            if (bar != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(bar, c, KeyFrameCharacterActionType.ADD));
                KeyFrame replacedBar = addKeyFrame(c, bar);
                if (replacedBar != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacedBar, c, KeyFrameCharacterActionType.REMOVE));
                }
            }
            if (isPrimary)
            {
                primaryProcessed = true;
            }
        }
        addKeyFrameActions(kfa);
    }

    /**
     * Reads the previous keyframe of the bar type targeted by the hitsplat sprite
     * on the given Character, subtracts the damage from its current value
     * (clamped to 0), and returns a new keyframe at the seeker tick. Each Character
     * is looked up independently so multi-select bar drains reflect each owner's
     * own state.
     */
    private KeyFrame pickBarKeyFrameForHitsplat(Character character, com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite sprite, int damage)
    {
        switch (sprite)
        {
            case SHIELD:
            case SHIELD_OTHER:
            case SHIELD_MAX:
                return buildShieldDrainKeyFrame(character, damage);
            case POISE:
            case POISE_OTHER:
            case POISE_MAX:
                return buildSpecialDrainKeyFrame(character, damage);
            default:
                return buildHealthDrainKeyFrame(character, damage);
        }
    }

    private KeyFrame buildHealthDrainKeyFrame(Character character, int damage)
    {
        double duration;
        HealthbarSprite sprite;
        int maxHealth;
        int currentHealth;
        int order;
        int width;

        KeyFrame healthKeyFrame = character.findPreviousKeyFrame(KeyFrameType.HEALTH, currentTime, true);
        if (healthKeyFrame == null)
        {
            duration = 3.0;
            sprite = HealthbarSprite.DEFAULT;
            maxHealth = 99;
            currentHealth = 99;
            order = HealthKeyFrame.DEFAULT_ORDER;
            width = HealthKeyFrame.AUTO_WIDTH;
        }
        else
        {
            HealthKeyFrame healthKF = (HealthKeyFrame) healthKeyFrame;
            duration = healthKF.getDuration();
            sprite = healthKF.getHealthbarSprite();
            maxHealth = healthKF.getMaxHealth();
            currentHealth = healthKF.getCurrentHealth();
            order = healthKF.getOrder();
            width = healthKF.getWidth();
        }

        return new HealthKeyFrame(
                currentTime,
                duration,
                sprite,
                maxHealth,
                Math.max(0, currentHealth - damage),
                order,
                width);
    }

    private KeyFrame buildShieldDrainKeyFrame(Character character, int damage)
    {
        double duration;
        int rgb;
        int max;
        int current;
        int order;
        int width;

        KeyFrame prev = character.findPreviousKeyFrame(KeyFrameType.SHIELD, currentTime, true);
        if (prev == null)
        {
            duration = com.creatorskit.swing.timesheet.attributes.ShieldAttributes.DEFAULT_DURATION;
            rgb = com.creatorskit.swing.timesheet.attributes.ShieldAttributes.DEFAULT_RGB;
            max = com.creatorskit.swing.timesheet.attributes.ShieldAttributes.DEFAULT_MAX;
            current = max;
            order = ShieldKeyFrame.DEFAULT_ORDER;
            width = ShieldKeyFrame.AUTO_WIDTH;
        }
        else
        {
            ShieldKeyFrame shieldKF = (ShieldKeyFrame) prev;
            duration = shieldKF.getDuration();
            rgb = shieldKF.getRgb();
            max = shieldKF.getMaxValue();
            current = shieldKF.getCurrentValue();
            order = shieldKF.getOrder();
            width = shieldKF.getWidth();
        }

        return new ShieldKeyFrame(
                currentTime,
                duration,
                rgb,
                max,
                Math.max(0, current - damage),
                order,
                width);
    }

    private KeyFrame buildSpecialDrainKeyFrame(Character character, int damage)
    {
        double duration;
        int rgb;
        int max;
        int current;
        int order;
        int width;

        KeyFrame prev = character.findPreviousKeyFrame(KeyFrameType.SPECIAL, currentTime, true);
        if (prev == null)
        {
            duration = com.creatorskit.swing.timesheet.attributes.SpecialAttributes.DEFAULT_DURATION;
            rgb = com.creatorskit.swing.timesheet.attributes.SpecialAttributes.DEFAULT_RGB;
            max = com.creatorskit.swing.timesheet.attributes.SpecialAttributes.DEFAULT_MAX;
            current = max;
            order = SpecialKeyFrame.DEFAULT_ORDER;
            width = SpecialKeyFrame.AUTO_WIDTH;
        }
        else
        {
            SpecialKeyFrame specialKF = (SpecialKeyFrame) prev;
            duration = specialKF.getDuration();
            rgb = specialKF.getRgb();
            max = specialKF.getMaxValue();
            current = specialKF.getCurrentValue();
            order = specialKF.getOrder();
            width = specialKF.getWidth();
        }

        return new SpecialKeyFrame(
                currentTime,
                duration,
                rgb,
                max,
                Math.max(0, current - damage),
                order,
                width);
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
            return null;
        }

        KeyFrame keyFrameToReplace = character.addKeyFrame(keyFrame, currentTime);
        attributePanel.setKeyFramedIcon(true);
        attributePanel.resetAttributes(character, currentTime);
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
        attributePanel.setSelectedCharacter(character);
        attributePanel.resetAttributes(character, currentTime);
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
        if (attributePanel != null)
        {
            attributePanel.refreshKeyFrameSelectionState();
            if (selectedCharacter != null)
            {
                attributePanel.resetAttributes(selectedCharacter, currentTime);
            }
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

                    scrollAttributePanel(e.getWheelRotation());
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
    }

    private void setupAttributeSheet()
    {
        JPanel labelPanel = new JPanel();
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setLayout(new GridLayout(0, 1, 0, 0));
        labelPanel.setFocusable(true);

        labelScrollPane.setViewportView(labelPanel);
        labelScrollPane.setBorder(new EmptyBorder(1, 0, 1, 0));
        labelScrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelScrollPane.setPreferredSize(new Dimension(100, 150));
        labelScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        labelScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        MouseWheelListener[] mouseWheelListeners = labelScrollPane.getMouseWheelListeners();
        for (int i = 0; i < mouseWheelListeners.length; i++)
        {
            labelScrollPane.removeMouseWheelListener(mouseWheelListeners[i]);
        }

        InvisibleScrollBar labelScrollBar = new InvisibleScrollBar();
        labelScrollBar.addAdjustmentListener(e -> attributeSheet.onVerticalScrollEvent(e.getValue()));
        labelScrollPane.setVerticalScrollBar(labelScrollBar);

        labelScrollPane.addMouseWheelListener(new MouseAdapter()
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

                    scrollAttributePanel(e.getWheelRotation());
                    return;
                }

                labelScrollBar.setValue(labelScrollBar.getValue() + e.getWheelRotation() * 15);
            }
        });

        labels = new JLabel[KeyFrameType.getTotalFrameTypes() + 1];
        for (int i = 0; i < KeyFrameType.getTotalFrameTypes() + 1; i++)
        {
            JLabel label = new JLabel();
            label.setFocusable(true);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setOpaque(true);
            label.setPreferredSize(new Dimension(100, 24));
            label.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            // Skip the empty label
            if (i == 1)
            {
                label.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            }

            if (i != 0)
            {
                label.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        super.mousePressed(e);
                        attributePanel.switchCards(label.getText().replaceAll(LABEL_OFFSET, ""));
                        label.requestFocusInWindow();
                    }
                });

                label.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        if (e.getKeyCode() == KeyEvent.VK_DOWN)
                        {
                            scrollAttributePanel(1);
                        }

                        if (e.getKeyCode() == KeyEvent.VK_UP)
                        {
                            scrollAttributePanel(-1);
                        }
                    }
                });
            }

            labels[i] = label;
            labelPanel.add(label);
        }

        labels[1].setText(AttributePanel.MOVE_CARD + LABEL_OFFSET);
        labels[2].setText(AttributePanel.ANIM_CARD + LABEL_OFFSET);
        labels[3].setText(AttributePanel.ORI_CARD + LABEL_OFFSET);
        labels[4].setText(AttributePanel.SPAWN_CARD + LABEL_OFFSET);
        labels[5].setText(AttributePanel.MODEL_CARD + LABEL_OFFSET);
        labels[6].setText(AttributePanel.SPOTANIM_CARD + LABEL_OFFSET);
        labels[7].setText(AttributePanel.SPOTANIM2_CARD + LABEL_OFFSET);
        labels[8].setText(AttributePanel.TEXT_CARD + LABEL_OFFSET);
        labels[9].setText(AttributePanel.OVER_CARD + LABEL_OFFSET);
        labels[10].setText(AttributePanel.HEALTH_CARD + LABEL_OFFSET);
        labels[11].setText(AttributePanel.HITSPLAT_1_CARD + LABEL_OFFSET);
        labels[12].setText(AttributePanel.HITSPLAT_2_CARD + LABEL_OFFSET);
        labels[13].setText(AttributePanel.HITSPLAT_3_CARD + LABEL_OFFSET);
        labels[14].setText(AttributePanel.HITSPLAT_4_CARD + LABEL_OFFSET);
        labels[15].setText(AttributePanel.PROJECTILE_CARD + LABEL_OFFSET);
        labels[16].setText(AttributePanel.SHIELD_CARD + LABEL_OFFSET);
        labels[17].setText(AttributePanel.SPECIAL_CARD + LABEL_OFFSET);
        labels[18].setText(AttributePanel.SCREEN_FADE_CARD + LABEL_OFFSET);
        labels[19].setText(AttributePanel.SCREEN_SHAKE_CARD + LABEL_OFFSET);
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
        if (targets.isEmpty())
        {
            return;
        }

        if (copiedKeyFrames == null || copiedKeyFrames.length == 0)
        {
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
                if (selectedCharacter == null)
                {
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
        if (selectedCharacter == null)
        {
            return;
        }

        setSelectedKeyFrames(selectedCharacter.getAllKeyFrames());
    }

    public void onDeleteKeyPressed()
    {
        if (selectedKeyFrames == null || selectedKeyFrames.length == 0)
        {
            return;
        }

        java.util.Collection<Character> targets = resolveSelectionTargets();
        if (targets.isEmpty())
        {
            return;
        }

        // Marquee can contain multiple KFs at the same (type, tick) across owners --
        // dedupe so we only iterate each unique (type, tick) once. For each unique
        // pair, walk every selected Character and remove their KF at that position.
        // Mirrors paste's fan-out shape: best-effort apply per Character, silently
        // skip Characters that don't have a matching KF.
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

                    scrollAttributePanel(e.getWheelRotation());
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

        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 4;
        add(labelScrollPane, c);

        c.weightx = 8;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 4;
        add(attributeSheet, c);
    }

    private void updateSheets()
    {
        summarySheet.setHScroll(hScroll);
        summarySheet.setZoom(zoom);
        attributeSheet.setHScroll(hScroll);
        attributeSheet.setZoom(zoom);
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
        java.util.IdentityHashMap<KeyFrame, KeyFrame> replacements = new java.util.IdentityHashMap<>();
        KeyFrameAction[] kfa = new KeyFrameAction[0];

        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            int[] range = bucketRanges.get(owner);
            int n = rng.nextInt(range[1] - range[0] + 1) + range[0];
            double delta = n * step;
            if (delta == 0.0) continue;

            for (KeyFrame kf : entry.getValue())
            {
                double newTick = round(kf.getTick() + delta);
                KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, owner, KeyFrameCharacterActionType.ADD));
                owner.removeKeyFrame(kf);
                owner.addKeyFrame(replacement, currentTime);
                replacements.put(kf, replacement);
            }
        }

        finalizeTickTransform(kfa, replacements);
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
        JSpinner stepSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.1));

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("From tick:"));
        panel.add(fromSpinner);
        panel.add(new JLabel("To tick:"));
        panel.add(toSpinner);
        panel.add(new JLabel("Step size (ticks):"));
        panel.add(stepSpinner);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Scatter " + selectedKeyFrames.length + " keyframes across " + byOwner.size() + " Characters",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        double from = ((Number) fromSpinner.getValue()).doubleValue();
        double to = ((Number) toSpinner.getValue()).doubleValue();
        if (to < from) { double tmp = from; from = to; to = tmp; }
        final double step = ((Number) stepSpinner.getValue()).doubleValue();
        if (step <= 0) return;
        final double fFrom = from;
        final double fTo = to;

        // Pre-validate every Character. Each block must fit in [from, to] AND
        // have at least one step-aligned anchor. Refuse the whole op on the
        // first failure so the user can widen the range or shrink selection.
        java.util.Map<Character, double[]> placements = new java.util.LinkedHashMap<>(); // owner -> {blockMinTick, slotCount}
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

            if (duration > (fTo - fFrom))
            {
                JOptionPane.showMessageDialog(this,
                        "Cannot scatter: Character '" + owner.getName() + "' block spans " + duration + " ticks which is larger than the range " + (fTo - fFrom) + ".",
                        "Scatter blocked", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double anchorRange = fTo - fFrom - duration;
            int slots = (int) Math.floor(anchorRange / step) + 1;
            if (slots <= 0)
            {
                JOptionPane.showMessageDialog(this,
                        "Cannot scatter: step size " + step + " too large for Character '" + owner.getName() + "' in range [" + fFrom + ", " + fTo + "].",
                        "Scatter blocked", JOptionPane.WARNING_MESSAGE);
                return;
            }
            placements.put(owner, new double[]{minTick, slots});
        }

        // All validated. Per Character pick a random slot, compute the delta
        // that translates the block to that anchor, broadcast to every selected
        // keyframe of the owner.
        final java.util.Random rng = new java.util.Random();
        java.util.IdentityHashMap<KeyFrame, KeyFrame> replacements = new java.util.IdentityHashMap<>();
        KeyFrameAction[] kfa = new KeyFrameAction[0];

        for (java.util.Map.Entry<Character, java.util.List<KeyFrame>> entry : byOwner.entrySet())
        {
            Character owner = entry.getKey();
            double[] p = placements.get(owner);
            double blockMin = p[0];
            int slots = (int) p[1];
            double newAnchor = fFrom + rng.nextInt(slots) * step;
            double delta = newAnchor - blockMin;
            if (delta == 0.0) continue;

            for (KeyFrame kf : entry.getValue())
            {
                double newTick = round(kf.getTick() + delta);
                KeyFrame replacement = KeyFrame.createCopy(kf, newTick);
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(kf, owner, KeyFrameCharacterActionType.REMOVE));
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(replacement, owner, KeyFrameCharacterActionType.ADD));
                owner.removeKeyFrame(kf);
                owner.addKeyFrame(replacement, currentTime);
                replacements.put(kf, replacement);
            }
        }

        finalizeTickTransform(kfa, replacements);
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
     * Common tail for Jitter / Scatter: registers the undo group, refreshes the
     * renderer + AttributePanel exactly once, and re-points selectedKeyFrames at
     * the replacement instances so the marquee still highlights what the user
     * was just operating on (otherwise the old refs are gone from every
     * Character's frame arrays after the remove/add round-trip, and a follow-up
     * jitter / Update would operate on dead refs).
     */
    private void finalizeTickTransform(KeyFrameAction[] kfa, java.util.IdentityHashMap<KeyFrame, KeyFrame> replacements)
    {
        if (kfa.length == 0) return;
        addKeyFrameActions(kfa);

        if (!replacements.isEmpty() && selectedKeyFrames.length > 0)
        {
            KeyFrame[] updated = selectedKeyFrames.clone();
            boolean changed = false;
            for (int i = 0; i < updated.length; i++)
            {
                KeyFrame r = replacements.get(updated[i]);
                if (r != null) { updated[i] = r; changed = true; }
            }
            if (changed) setSelectedKeyFrames(updated);
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
}
