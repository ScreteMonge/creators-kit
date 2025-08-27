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

    private ArrayList<KeyFrameAction> keyFrameStack = new ArrayList<>();
    private KeyFrame[] selectedKeyFrames = new KeyFrame[0];
    private KeyFrame[] copiedKeyFrames = new KeyFrame[0];
    private KeyFrameAction[][] keyFrameActions = new KeyFrameAction[0][];

    private final int UNDO_LIMIT = 15;
    private int undoStack = 0;

    @Inject
    public TimeSheetPanel(@Nullable Client client, ToolBoxFrame toolBox, CreatorsPlugin plugin, CreatorsConfig config, ClientThread clientThread, DataFinder dataFinder, ManagerTree managerTree, MovementManager movementManager)
    {
        this.client = client;
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.config = config;
        this.clientThread = clientThread;
        this.dataFinder = dataFinder;
        this.managerTree = managerTree;
        this.movementManager = movementManager;

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

       removeKeyFrameAction(keyFrame);
    }

    public void addKeyFrameAction(KeyFrame[] keyFrames)
    {
        KeyFrameAction[] kfa = new KeyFrameAction[0];

        for (KeyFrame keyFrame : keyFrames)
        {
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrame, selectedCharacter, KeyFrameCharacterActionType.ADD));

            KeyFrame keyFrameToReplace = addKeyFrame(selectedCharacter, keyFrame);
            if (keyFrameToReplace != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
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
        if (selectedCharacter == null)
        {
            return;
        }

        KeyFrameType type = attributePanel.getSelectedKeyFramePage();
        KeyFrame keyFrame = selectedCharacter.findPreviousKeyFrame(type, currentTime, true);
        if (keyFrame == null)
        {
            return;
        }

        KeyFrame kf = attributePanel.createKeyFrame(type, keyFrame.getTick());
        if (kf == null)
        {
            return;
        }

        if (type == KeyFrameType.MOVEMENT)
        {
            MovementKeyFrame oldKF = (MovementKeyFrame) keyFrame;

            MovementKeyFrame newKF = (MovementKeyFrame) kf;
            newKF.setPlane(oldKF.getPlane());
            newKF.setPoh(oldKF.isPoh());
            newKF.setPath(oldKF.getPath());
            newKF.setCurrentStep(0);
            newKF.setStepClientTick(0);
        }

        KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(kf, selectedCharacter, KeyFrameCharacterActionType.ADD), new KeyFrameCharacterAction(keyFrame, selectedCharacter, KeyFrameCharacterActionType.REMOVE)};
        addKeyFrame(selectedCharacter, kf);
        addKeyFrameActions(kfa);
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

        KeyFrame kf = selectedCharacter.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        if (newKeyFrame || kf == null)
        {
            int x = localPoint.getSceneX();
            int y = localPoint.getSceneY();
            if (!poh)
            {
                WorldPoint wp = WorldPoint.fromLocalInstance(client, localPoint, worldView.getPlane());
                x = wp.getX();
                y = wp.getY();
            }

            int[][] path = new int[][]{new int[]{x, y}};
            initializeMovementKeyFrame(selectedCharacter, currentTime, worldView.getPlane(), poh, path, false, 1, OrientationKeyFrame.TURN_RATE);
        }
        else
        {
            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            int[][] path = movementManager.addProgramStep(keyFrame, worldView, localPoint);
            initializeMovementKeyFrame(selectedCharacter, keyFrame.getTick(), worldView.getPlane(), poh, path, keyFrame.isLoop(), keyFrame.getSpeed(), keyFrame.getTurnRate());
        }

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
     * Adds a new HealthKeyFrame based on the last HealthKeyFrame as if the HitsplatKeyFrame argument were applied
     */
    public void initializeHealthKeyFrame(KeyFrameType type)
    {
        if (selectedCharacter == null)
        {
            return;
        }

        KeyFrame hitsplatKeyFrame = attributePanel.createKeyFrame(type, currentTime);
        addKeyFrameAction(new KeyFrame[]{hitsplatKeyFrame});

        HitsplatKeyFrame hitsKF = (HitsplatKeyFrame) hitsplatKeyFrame;

        double duration;
        HealthbarSprite sprite;
        int maxHealth;
        int currentHealth;

        KeyFrame healthKeyFrame = selectedCharacter.findPreviousKeyFrame(KeyFrameType.HEALTH, currentTime, true);
        if (healthKeyFrame == null)
        {
            duration = 3.0;
            sprite = HealthbarSprite.DEFAULT;
            maxHealth = 99;
            currentHealth = 99;
        }
        else
        {
            HealthKeyFrame healthKF = (HealthKeyFrame) healthKeyFrame;
            duration = healthKF.getDuration();
            sprite = healthKF.getHealthbarSprite();
            maxHealth = healthKF.getMaxHealth();
            currentHealth = healthKF.getCurrentHealth();
        }

        int damage = hitsKF.getDamage();

        int remaining = currentHealth - damage;
        if (remaining < 0)
        {
            remaining = 0;
        }

        HealthKeyFrame nextKF = new HealthKeyFrame(
                currentTime,
                duration,
                sprite,
                maxHealth,
                remaining);

        addKeyFrameAction(new KeyFrame[]{nextKF});
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

    public void duplicateHitsplatKeyFrame(KeyFrameType previousType, KeyFrameType targetType)
    {
        KeyFrame kf = selectedCharacter.findPreviousKeyFrame(previousType, currentTime, true);
        if (kf == null)
        {
            return;
        }

        HitsplatKeyFrame keyFrame = (HitsplatKeyFrame) kf;

        HitsplatKeyFrame hkf = new HitsplatKeyFrame(
                keyFrame.getTick(),
                targetType,
                keyFrame.getDuration(),
                keyFrame.getSprite(),
                keyFrame.getVariant(),
                keyFrame.getDamage());

        addKeyFrameAction(new KeyFrame[]{hkf});
    }

    public void duplicateSpotanimKeyFrame(KeyFrameType previousType, KeyFrameType targetType)
    {
        KeyFrame kf = selectedCharacter.findPreviousKeyFrame(previousType, currentTime, true);
        if (kf == null)
        {
            return;
        }

        SpotAnimKeyFrame keyFrame = (SpotAnimKeyFrame) kf;

        SpotAnimKeyFrame spkf = new SpotAnimKeyFrame(
                keyFrame.getTick(),
                targetType,
                keyFrame.getSpotAnimId(),
                keyFrame.isLoop(),
                keyFrame.getHeight());

        addKeyFrameAction(new KeyFrame[]{spkf});
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
        attributePanel = new AttributePanel(client, clientThread, config, this, dataFinder);
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
        managerTree.addTreeSelectionListener(e ->
        {
            TreePath treePath = e.getPath();
            if (treePath == null)
            {
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            Object o = node.getUserObject();
            if (o == null)
            {
                return;
            }

            if (o instanceof Character)
            {
                setSelectedCharacter((Character) o);
                return;
            }

            setSelectedCharacter(null);
        });
    }

    public void copyKeyFrames()
    {
        copiedKeyFrames = selectedKeyFrames;
    }

    public void pasteKeyFrames()
    {
        if (selectedCharacter == null)
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

        selectedKeyFrames = new KeyFrame[0];
        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (KeyFrame keyFrame : copiedKeyFrames)
        {
            double newTime = round(keyFrame.getTick() - firstTick + currentTime);
            KeyFrame copy = KeyFrame.createCopy(keyFrame, newTime);
            selectedKeyFrames = ArrayUtils.add(selectedKeyFrames, copy);

            KeyFrame keyFrameToReplace = addKeyFrame(selectedCharacter, copy);
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(copy, selectedCharacter, KeyFrameCharacterActionType.ADD));

            if (keyFrameToReplace != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
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
        if (selectedCharacter == null)
        {
            return;
        }

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (KeyFrame keyFrame : selectedKeyFrames)
        {
            kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrame, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
            removeKeyFrame(selectedCharacter, keyFrame);
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
}
