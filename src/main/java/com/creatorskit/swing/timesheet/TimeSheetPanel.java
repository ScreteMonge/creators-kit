package com.creatorskit.swing.timesheet;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.datatypes.PlayerAnimationType;
import com.creatorskit.models.datatypes.SpotAnimDefinition;
import com.creatorskit.models.datatypes.WeaponAnimData;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.programming.Programmer;
import com.creatorskit.programming.camera.*;
import com.creatorskit.programming.orientation.Orientation;
import com.creatorskit.programming.orientation.OrientationGoal;
import com.creatorskit.programming.orientation.OrientationHotkeyMode;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.manager.TreeScrollPane;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCameraAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameActionType;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameCategory;
import com.creatorskit.swing.timesheet.keyframe.keyframeselectionmanager.KeyFrameSelectionManager;
import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import com.creatorskit.swing.timesheet.keyframe.subtypes.*;
import com.creatorskit.swing.timesheet.sheets.AttributeSheet;
import com.creatorskit.swing.timesheet.sheets.CameraSheet;
import com.creatorskit.swing.timesheet.sheets.SummarySheet;
import com.creatorskit.swing.timesheet.sheets.TimeSheet;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.Point;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

@Getter
@Setter
public class TimeSheetPanel extends JSplitPane
{
    private Client client;
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final CreatorsConfig config;
    private final ConfigManager configManager;

    private final ToolBoxFrame toolBox;
    private final DataFinder dataFinder;
    private SummarySheet summarySheet;
    private AttributeSheet attributeSheet;
    private CameraSheet cameraSheet;
    private CameraManager cameraManager;
    private TreeScrollPane treeScrollPane;
    private final ManagerTree managerTree;
    private MovementManager movementManager;
    private final SelectionManager selectionManager;
    private final KeyFrameSelectionManager kfsm;

    private JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
    private JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
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

    private ArrayList<KeyFrameAction> keyFrameStack = new ArrayList<>();
    private LinkedHashMap<KeyFrameTarget, KeyFrame[]> copiedKeyFrames = new LinkedHashMap<>();
    private KeyFrameAction[][] keyFrameActions = new KeyFrameAction[0][];

    private final int UNDO_LIMIT = 15;
    private int undoStack = 0;

    @Inject
    public TimeSheetPanel(@Nullable Client client, ToolBoxFrame toolBox, CreatorsPlugin plugin, CreatorsConfig config, ConfigManager configManager, ClientThread clientThread, DataFinder dataFinder, ManagerTree managerTree, MovementManager movementManager, SelectionManager selectionManager, KeyFrameSelectionManager kfsm, CameraManager cameraManager)
    {
        this.client = client;
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.config = config;
        this.configManager = configManager;
        this.clientThread = clientThread;
        this.dataFinder = dataFinder;
        this.managerTree = managerTree;
        this.movementManager = movementManager;
        this.selectionManager = selectionManager;
        this.kfsm = kfsm;
        this.cameraManager = cameraManager;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        setupTreeScrollPane();
        setupControlPanel();
        setupAttributePanel();
        setupAttributeSheet();
        setupScrollBar();
        setupLayout();
        setMouseListeners();
        selectionManager.addListener((manager, origin) -> attributePanel.updateObjectLabel(manager.getPrimary()));
    }

    public void onAttributeSkipForward()
    {
        KeyFrame next = null;
        for (Character c : selectionManager.getSelected())
        {
            KeyFrame keyFrame = c.findNextKeyFrame(currentTime);
            if (next == null && keyFrame != null)
            {
                next = keyFrame;
                continue;
            }

            if (keyFrame == null)
            {
                continue;
            }

            if (keyFrame.getTick() < next.getTick())
            {
                next = keyFrame;
            }
        }

        if (next == null)
        {
            return;
        }

        setCurrentTime(next.getTick(), false);
    }

    public void onAttributeSkipPrevious()
    {
        KeyFrame previous = null;
        for (Character c : selectionManager.getSelected())
        {
            KeyFrame keyFrame = c.findPreviousKeyFrame(currentTime);
            if (previous == null && keyFrame != null)
            {
                previous = keyFrame;
                continue;
            }

            if (keyFrame == null)
            {
                continue;
            }

            if (keyFrame.getTick() > previous.getTick())
            {
                previous = keyFrame;
            }
        }

        if (previous == null)
        {
            return;
        }

        setCurrentTime(previous.getTick(), false);
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
        KeyFrameType type = attributePanel.getSelectedKeyFramePage();
        List<KeyFrameAction> kfa = new ArrayList<>();

        if (type == KeyFrameType.CAMERA)
        {
            KeyFrame kf = attributePanel.createKeyFrame(KeyFrameType.CAMERA, currentTime);
            if (kf == null)
            {
                return;
            }

            kfa.add(new KeyFrameCameraAction(kf, KeyFrameActionType.ADD));

            KeyFrame keyFrameToReplace = addKeyFrame(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), kf);
            if (keyFrameToReplace != null)
            {
                kfa.add(new KeyFrameCameraAction(keyFrameToReplace, KeyFrameActionType.REMOVE));
            }

            stackKeyFrameActions(kfa);
            attributePanel.updateAttributes();
            return;
        }

        for (Character c : selectionManager.getSelected())
        {
            KeyFrame keyFrame = c.findKeyFrame(type, currentTime);
            if (keyFrame == null)
            {
                KeyFrame kf = attributePanel.createKeyFrame(type, currentTime);
                if (kf == null)
                {
                    continue;
                }

                kfa.add(new KeyFrameCharacterAction(kf, c, KeyFrameActionType.ADD));

                if (type == KeyFrameType.SPAWN && currentTime > 0)
                {
                    KeyFrame spawn0 = checkDespawnKeyFrameAt0(c, kf, currentTime);
                    if (spawn0 != null)
                    {
                        kfa.add(new KeyFrameCharacterAction(spawn0, c, KeyFrameActionType.ADD));
                    }
                }

                KeyFrame keyFrameToReplace = addKeyFrame(new KeyFrameTarget(KeyFrameCategory.CHARACTER, c), kf);
                if (keyFrameToReplace != null)
                {
                    kfa.add(new KeyFrameCharacterAction(keyFrameToReplace, c, KeyFrameActionType.REMOVE));
                }
                continue;
            }

            removeKeyFrame(new KeyFrameTarget(KeyFrameCategory.CHARACTER, c), keyFrame);
            kfa.add(new KeyFrameCharacterAction(keyFrame, c, KeyFrameActionType.REMOVE));
        }

        stackKeyFrameActions(kfa);
        attributePanel.updateAttributes();
    }

    public void runKeyFrameAddActions(KeyFrame[] cameraKeyFrames)
    {
        runKeyFrameAddActions(cameraKeyFrames, new Character[0], new KeyFrame[0][0]);
    }

    public void runKeyFrameAddActions(Character[] characters, KeyFrame[][] keyFrameSets)
    {
        runKeyFrameAddActions(new KeyFrame[0], characters, keyFrameSets);
    }

    public void runKeyFrameAddActions(KeyFrame[] cameraKeyFrames, Character[] characters, KeyFrame[][] keyFrameSets)
    {
        KeyFrameAction[] kfa = new KeyFrameAction[0];

        for (int i = 0; i < cameraKeyFrames.length; i++)
        {
            for (KeyFrame keyFrame : cameraKeyFrames)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCameraAction(keyFrame, KeyFrameActionType.ADD));

                KeyFrame keyFrameToReplace = addKeyFrame(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), keyFrame);
                if (keyFrameToReplace != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCameraAction(keyFrameToReplace, KeyFrameActionType.REMOVE));
                }
            }
        }

        for (int i = 0; i < characters.length; i++)
        {
            Character c = characters[i];
            KeyFrame[] keyFrames = keyFrameSets[i];

            for (KeyFrame keyFrame : keyFrames)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrame, c, KeyFrameActionType.ADD));

                KeyFrame keyFrameToReplace = addKeyFrame(new KeyFrameTarget(KeyFrameCategory.CHARACTER, c), keyFrame);
                if (keyFrameToReplace != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, c, KeyFrameActionType.REMOVE));
                }
            }
        }

        stackKeyFrameActions(kfa);
    }

    public void onUpdateButtonPressed()
    {
        List<KeyFrameAction> kfa = new ArrayList<>();
        KeyFrameType type = attributePanel.getSelectedKeyFramePage();
        LinkedHashMap<KeyFrameTarget, KeyFrame[]> selected = new LinkedHashMap<>(kfsm.getSelected());

        selected.forEach((target, keyframes) ->
        {
            for (KeyFrame keyFrame : keyframes)
            {
                if (keyFrame.getKeyFrameType() != type)
                {
                    continue;
                }

                KeyFrame newKf = attributePanel.createKeyFrame(type, keyFrame.getTick());
                if (newKf == null)
                {
                    continue;
                }

                if (type == KeyFrameType.CAMERA)
                {
                    CameraKeyFrame oldKF = (CameraKeyFrame) keyFrame;
                    CameraKeyFrame newKF = (CameraKeyFrame) newKf;
                    CameraScript oldScript = oldKF.getScript();
                    CameraScript newScript = newKF.getScript();

                    if (oldScript.getType() == CameraMotionType.DIRECTIONAL && newScript.getType() == CameraMotionType.DIRECTIONAL)
                    {
                        CameraDirectionalScript oldDS = (CameraDirectionalScript) oldScript;
                        CameraDirectionalScript newDS = (CameraDirectionalScript) newScript;
                        newDS.setInPOH(oldDS.isInPOH());
                        newDS.setFocalX(oldDS.getFocalX());
                        newDS.setFocalY(oldDS.getFocalY());
                        newDS.setFocalZ(oldDS.getFocalZ());
                        newDS.setOffsetX(oldDS.getOffsetX());
                        newDS.setOffsetZ(oldDS.getOffsetZ());
                    }
                    else if (oldScript.getType() == CameraMotionType.TRACKING && newScript.getType() == CameraMotionType.TRACKING)
                    {
                        CameraTrackingScript oldTS = (CameraTrackingScript) oldScript;
                        CameraTrackingScript newTS = (CameraTrackingScript) newScript;
                        newTS.setCharacter(oldTS.getCharacter());
                    }
                }

                if (type == KeyFrameType.MOVEMENT)
                {
                    MovementKeyFrame oldKF = (MovementKeyFrame) keyFrame;
                    MovementKeyFrame newKF = (MovementKeyFrame) newKf;
                    newKF.setPlane(oldKF.getPlane());
                    newKF.setPoh(oldKF.isPoh());
                    newKF.setPath(oldKF.getPath());
                    newKF.setCurrentStep(0);
                    newKF.setStepClientTick(0);
                }

                switch (target.getType())
                {
                    case CHARACTER:
                        Character c = (Character) target.getValue();
                        kfa.add(new KeyFrameCharacterAction(newKf, c, KeyFrameActionType.ADD));
                        kfa.add(new KeyFrameCharacterAction(keyFrame, c, KeyFrameActionType.REMOVE));
                        break;
                    case CAMERA:
                        kfa.add(new KeyFrameCameraAction(newKf, KeyFrameActionType.ADD));
                        kfa.add(new KeyFrameCameraAction(keyFrame, KeyFrameActionType.REMOVE));
                        break;
                }

                addKeyFrame(target, newKf);
            }
        });

        if (!kfa.isEmpty())
        {
            stackKeyFrameActions(kfa);
        }

        attributePanel.updateAttributes();
    }

    public KeyFrame checkDespawnKeyFrameAt0(Character c, KeyFrame keyFrame, double currentTick)
    {
        SpawnKeyFrame skf = (SpawnKeyFrame) keyFrame;

        KeyFrame previousKeyFrame = c.findPreviousKeyFrame(KeyFrameType.SPAWN, currentTick, false);
        if (previousKeyFrame == null)
        {
            return new SpawnKeyFrame(0, !skf.isSpawnActive());
        }

        return null;
    }

    public void onOrientationKeyPressed(OrientationHotkeyMode hotkeyMode)
    {
        if (selectionManager.getSelected().isEmpty())
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

        Character[] characters = new Character[0];
        KeyFrame[][] keyFrameSets = new KeyFrame[0][0];

        Character primary = selectionManager.getPrimary();
        CKObject ckObject = primary.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        int startOrientation = ckObject.getOrientation();
        int endOrientation;

        KeyFrame okf = primary.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
        KeyFrame keyFrame;

        if (okf == null)
        {
            endOrientation = ckObject.getOrientation();

            keyFrame = initializeOrientationKeyFrame(
                    primary,
                    hotkeyMode,
                    localPoint,
                    currentTime,
                    startOrientation,
                    endOrientation,
                    OrientationGoal.POINT,
                    OrientationKeyFrame.TURN_RATE);
        }
        else
        {
            OrientationKeyFrame kf = (OrientationKeyFrame) okf;
            startOrientation = kf.getStart();
            endOrientation = kf.getEnd();
            keyFrame = initializeOrientationKeyFrame(
                    primary,
                    hotkeyMode,
                    localPoint,
                    kf.getTick(),
                    startOrientation,
                    endOrientation,
                    kf.getGoal(),
                    kf.getTurnRate());
        }

        if (keyFrame == null)
        {
            return;
        }

        characters = ArrayUtils.add(characters, primary);
        keyFrameSets = ArrayUtils.add(keyFrameSets, new KeyFrame[]{keyFrame});

        for (Character c : selectionManager.getSelected())
        {
            if (c == primary)
            {
                continue;
            }

            KeyFrame keyFrameCopy = KeyFrame.createCopy(keyFrame, keyFrame.getTick());
            characters = ArrayUtils.add(characters, c);
            keyFrameSets = ArrayUtils.add(keyFrameSets, new KeyFrame[]{keyFrameCopy});
        }

        runKeyFrameAddActions(characters, keyFrameSets);

        for (Character c : selectionManager.getSelected())
        {
            programmer.register3DChanges(c);
            c.setVisible(true, clientThread);
        }
    }

    public OrientationKeyFrame initializeOrientationKeyFrame(Character character, OrientationHotkeyMode hotkeyMode, LocalPoint localPoint, double tick, int start, int end, OrientationGoal og, int turnRate)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return null;
        }

        LocalPoint lp = ckObject.getLocation();
        if (lp == null || !lp.isInScene())
        {
            return null;
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

        return new OrientationKeyFrame(
                tick,
                og,
                startOrientation,
                endOrientation,
                turnDuration,
                turnRate);
    }

    /**
     * Adds a new HealthKeyFrame based on the last HealthKeyFrame as if the HitsplatKeyFrame argument were applied
     */
    public void initializeHealthKeyFrame(KeyFrameType type)
    {
        Character[] characters = new Character[0];
        KeyFrame[][] keyFrameSets = new KeyFrame[0][0];

        for (Character c : selectionManager.getSelected())
        {
            KeyFrame hitsplatKeyFrame = attributePanel.createKeyFrame(type, currentTime);
            KeyFrame[] keyFrames = new KeyFrame[]{hitsplatKeyFrame};

            HitsplatKeyFrame hitsKF = (HitsplatKeyFrame) hitsplatKeyFrame;

            double duration;
            HealthbarSprite sprite;
            int maxHealth;
            int currentHealth;

            KeyFrame healthKeyFrame = c.findPreviousKeyFrame(KeyFrameType.HEALTH, currentTime, true);
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

            keyFrames = ArrayUtils.add(keyFrames, nextKF);

            characters = ArrayUtils.add(characters, c);
            keyFrameSets = ArrayUtils.add(keyFrameSets, keyFrames);
        }

        runKeyFrameAddActions(characters, keyFrameSets);
    }

    public void addAnimationKeyFrameFromCache(WeaponAnimData weaponAnim)
    {
        Character[] characters = new Character[0];
        KeyFrame[][] keyFrameSets = new KeyFrame[0][0];

        for (Character c : selectionManager.getSelected())
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

            characters = ArrayUtils.add(characters, c);
            keyFrameSets = ArrayUtils.add(keyFrameSets, new KeyFrame[]{keyFrame});
        }

        runKeyFrameAddActions(characters, keyFrameSets);
    }

    public void addSpotAnimKeyFrameFromCache(SpotAnimDefinition spotanimData)
    {
        Character[] characters = new Character[0];
        KeyFrame[][] keyFrameSets = new KeyFrame[0][0];

        for (Character c : selectionManager.getSelected())
        {
            KeyFrameType type = KeyFrameType.SPOTANIM;
            KeyFrame sp1 = c.findKeyFrame(KeyFrameType.SPOTANIM, currentTime);
            if (sp1 != null)
            {
                KeyFrame sp2 = c.findKeyFrame(KeyFrameType.SPOTANIM2, currentTime);
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

            characters = ArrayUtils.add(characters, c);
            keyFrameSets = ArrayUtils.add(keyFrameSets, new KeyFrame[]{keyFrame});
        }

        runKeyFrameAddActions(characters, keyFrameSets);
    }

    public void duplicateHitsplatKeyFrame(KeyFrameType previousType, KeyFrameType targetType)
    {
        Character[] characters = new Character[0];
        KeyFrame[][] keyFrameSets = new KeyFrame[0][0];

        for (Character c : selectionManager.getSelected())
        {
            KeyFrame kf = c.findPreviousKeyFrame(previousType, currentTime, true);
            if (kf == null)
            {
                continue;
            }

            HitsplatKeyFrame keyFrame = (HitsplatKeyFrame) kf;

            HitsplatKeyFrame hkf = new HitsplatKeyFrame(
                    keyFrame.getTick(),
                    targetType,
                    keyFrame.getDuration(),
                    keyFrame.getSprite(),
                    keyFrame.getVariant(),
                    keyFrame.getDamage());

            characters = ArrayUtils.add(characters, c);
            keyFrameSets = ArrayUtils.add(keyFrameSets, new KeyFrame[]{hkf});
        }

        runKeyFrameAddActions(characters, keyFrameSets);
    }

    public void duplicateSpotanimKeyFrame(KeyFrameType previousType, KeyFrameType targetType)
    {
        Character[] characters = new Character[0];
        KeyFrame[][] keyFrameSets = new KeyFrame[0][0];

        for (Character c : selectionManager.getSelected())
        {
            KeyFrame kf = c.findPreviousKeyFrame(previousType, currentTime, true);
            if (kf == null)
            {
                continue;
            }

            SpotAnimKeyFrame keyFrame = (SpotAnimKeyFrame) kf;

            SpotAnimKeyFrame spkf = new SpotAnimKeyFrame(
                    keyFrame.getTick(),
                    targetType,
                    keyFrame.getSpotAnimId(),
                    keyFrame.isLoop(),
                    keyFrame.getHeight());

            characters = ArrayUtils.add(characters, c);
            keyFrameSets = ArrayUtils.add(keyFrameSets, new KeyFrame[]{spkf});
        }

        runKeyFrameAddActions(characters, keyFrameSets);
    }

    /**
     * Adds the keyframe to a specific character, or replaces a keyframe if the tick matches exactly
     * @param target the Object (camera, character) to add the keyframe to
     * @param keyFrame the keyframe to add or modify for the character
     * @return the keyframe that is being replaced; null if there is no keyframe being replaced
     */
    public KeyFrame addKeyFrame(KeyFrameTarget target, KeyFrame keyFrame)
    {
        KeyFrame keyFrameToReplace = null;
        switch (target.getType())
        {
            case CHARACTER:
                Character c = (Character) target.getValue();
                keyFrameToReplace = c.addKeyFrame(keyFrame, currentTime);
                kfsm.select(new KeyFrameTarget(KeyFrameCategory.CHARACTER, c), keyFrame);

                if (client.getGameState() == GameState.LOGGED_IN)
                {
                    toolBox.getProgrammer().updateProgram(c, currentTime);
                }
                break;
            case CAMERA:
                keyFrameToReplace = cameraManager.addKeyFrame(keyFrame);
                kfsm.select(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), keyFrame);
                break;
        }

        return keyFrameToReplace;
    }

    /**
     * Removes a specific keyframe from the chosen character
     * @param target the Object (character, camera) to remove the keyframe from
     * @param keyFrame the keyframe to remove
     */
    public void removeKeyFrame(KeyFrameTarget target, KeyFrame keyFrame)
    {
        switch (target.getType())
        {
            case CHARACTER:
                Character c = (Character) target.getValue();
                c.removeKeyFrame(keyFrame);

                if (client.getGameState() == GameState.LOGGED_IN)
                {
                    toolBox.getProgrammer().updateProgram(c, currentTime);
                }
                break;
            case CAMERA:
                cameraManager.removeKeyFrame(keyFrame);
                break;
        }

        kfsm.remove(target, keyFrame);


    }

    public void stackKeyFrameActions(List<KeyFrameAction> actions)
    {
        stackKeyFrameActions(actions.toArray(new KeyFrameAction[0]));
    }

    public void stackKeyFrameActions(KeyFrameAction[] actions)
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

    public void unstackKeyFrameActions(Character character)
    {
        for (int i = 0; i < keyFrameActions.length; i++)
        {
            KeyFrameAction[] actions = keyFrameActions[i];
            keyFrameActions[i] = Arrays.stream(actions)
                    .filter(kfa ->
                    {
                        if (kfa.getCategory() != KeyFrameCategory.CHARACTER)
                        {
                            return true;
                        }

                        KeyFrameCharacterAction kfca = (KeyFrameCharacterAction) kfa;
                        return kfca.getCharacter() != character;
                    })
                    .toArray(KeyFrameAction[]::new);
        }

        List<KeyFrameAction[]> actionSetsToRemove = new ArrayList<>();
        for (KeyFrameAction[] actions : keyFrameActions)
        {
            if (actions.length == 0)
            {
                actionSetsToRemove.add(actions);
            }
        }

        for (KeyFrameAction[] actions : actionSetsToRemove)
        {
            keyFrameActions = ArrayUtils.removeElement(keyFrameActions, actions);
        }

        undoStack = keyFrameActions.length - 1;
    }

    public void undo()
    {
        if (undoStack == -1 || keyFrameActions.length == 0)
        {
            return;
        }

        kfsm.clear();

        KeyFrameAction[] lastActions = keyFrameActions[undoStack];
        for (int i = 0; i < lastActions.length; i++)
        {
            KeyFrameAction keyFrameAction = lastActions[i];
            KeyFrameActionType actionType = keyFrameAction.getActionType();

            switch (keyFrameAction.getCategory())
            {
                case CHARACTER:
                    KeyFrameCharacterAction kfca = (KeyFrameCharacterAction) keyFrameAction;

                    if (actionType == KeyFrameActionType.ADD)
                    {
                        removeKeyFrame(new KeyFrameTarget(KeyFrameCategory.CHARACTER, kfca.getCharacter()), keyFrameAction.getKeyFrame());
                    }

                    if (actionType == KeyFrameActionType.REMOVE)
                    {
                        KeyFrame keyFrame = keyFrameAction.getKeyFrame();
                        Character character = kfca.getCharacter();
                        KeyFrameTarget target = new KeyFrameTarget(KeyFrameCategory.CHARACTER, character);
                        addKeyFrame(target, keyFrame);
                        kfsm.add(target, keyFrame);
                    }
                    break;
                case CAMERA:
                    if (actionType == KeyFrameActionType.ADD)
                    {
                        removeKeyFrame(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), keyFrameAction.getKeyFrame());
                    }

                    if (actionType == KeyFrameActionType.REMOVE)
                    {
                        KeyFrame keyFrame = keyFrameAction.getKeyFrame();
                        KeyFrameTarget target = new KeyFrameTarget(KeyFrameCategory.CAMERA, null);
                        addKeyFrame(target, keyFrame);
                        kfsm.add(target, keyFrame);
                    }
                    break;
            }
        }

        onKeyFrameSelectionChanged();
        undoStack--;
    }

    public void redo()
    {
        if (undoStack + 1 >= keyFrameActions.length)
        {
            return;
        }

        undoStack++;
        kfsm.clear();

        KeyFrameAction[] lastUndoneActions = keyFrameActions[undoStack];
        for (KeyFrameAction keyFrameAction : lastUndoneActions)
        {
            KeyFrameActionType actionType = keyFrameAction.getActionType();

            switch (keyFrameAction.getCategory())
            {
                case CHARACTER:
                    KeyFrameCharacterAction kfca = (KeyFrameCharacterAction) keyFrameAction;
                    if (actionType == KeyFrameActionType.ADD)
                    {
                        KeyFrame keyFrame = keyFrameAction.getKeyFrame();
                        Character character = kfca.getCharacter();
                        KeyFrameTarget target = new KeyFrameTarget(KeyFrameCategory.CHARACTER, character);
                        addKeyFrame(target, keyFrame);
                        kfsm.add(target, keyFrame);
                    }

                    if (actionType == KeyFrameActionType.REMOVE)
                    {
                        removeKeyFrame(new KeyFrameTarget(KeyFrameCategory.CHARACTER, kfca.getCharacter()), keyFrameAction.getKeyFrame());
                    }

                    break;
                case CAMERA:
                    if (actionType == KeyFrameActionType.ADD)
                    {
                        KeyFrame keyFrame = keyFrameAction.getKeyFrame();
                        KeyFrameTarget target = new KeyFrameTarget(KeyFrameCategory.CAMERA, null);
                        addKeyFrame(target, keyFrame);
                        kfsm.add(target, keyFrame);
                    }

                    if (actionType == KeyFrameActionType.REMOVE)
                    {
                        removeKeyFrame(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), keyFrameAction.getKeyFrame());
                    }

                    break;
            }
        }

        onKeyFrameSelectionChanged();
    }

    public void onKeyFrameSelectionChanged()
    {
        attributePanel.updateAttributes();
        KeyFrame primary = kfsm.getPrimary();
        if (primary != null)
        {
            attributePanel.switchCards(primary.getKeyFrameType());
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
        cameraSheet.setCurrentTime(currentTime);

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

        attributePanel.updateAttributes();
    }

    public void setPlayButtonIcon(boolean playing)
    {
        playButton.setIcon(playing ? PAUSE : PLAY);
    }

    public void updatePreviewTime(double tick)
    {
        attributeSheet.setPreviewTime(tick);
        summarySheet.setPreviewTime(tick);
        cameraSheet.setPreviewTime(tick);
    }

    private void setupTreeScrollPane()
    {
        treeScrollPane = new TreeScrollPane(managerTree);

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

        GridBagConstraints c = new GridBagConstraints();
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
        attributePanel = new AttributePanel(client, clientThread, plugin, config, this, dataFinder, selectionManager, kfsm);
        summarySheet = new SummarySheet(toolBox, config, managerTree, attributePanel, kfsm);
        attributeSheet = new AttributeSheet(toolBox, config, managerTree, attributePanel, selectionManager, kfsm);
        cameraSheet = new CameraSheet(toolBox, config, managerTree, attributePanel, kfsm, cameraManager);
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

        labels = new JLabel[KeyFrameType.getTotalCharacterKeyFrameTypes() + 1];
        for (int i = 0; i < KeyFrameType.getTotalCharacterKeyFrameTypes() + 1; i++)
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

    public void copyKeyFrames()
    {
        copiedKeyFrames = new LinkedHashMap<>(kfsm.getSelected());
    }

    public void pasteKeyFrames()
    {
        if (copiedKeyFrames.isEmpty())
        {
            return;
        }

        Point p = getMousePosition(true);
        if (p == null)
        {
            p = getLocationOnScreen();
            if (p == null)
            {
                return;
            }
        }

        Set<KeyFrame> cameraKeyFrames = new HashSet<>();
        copiedKeyFrames.forEach((target, keyFrames) ->
        {
            if (target.getType() == KeyFrameCategory.CAMERA)
            {
                cameraKeyFrames.addAll(List.of(keyFrames));
            }
        });

        KeyFrame[] cameraCopies = createKeyFrameCopies(KeyFrameCategory.CAMERA, cameraKeyFrames.toArray(new KeyFrame[0]));

        if (selectionManager.getSelectionSize() == 1)
        {
            Character[] characters = new Character[0];
            KeyFrame[][] keyFrameSet = new KeyFrame[0][0];

            for (Character character : selectionManager.getSelected())
            {
                KeyFrame[] keyFrames = clearOverridingCopies(character, copiedKeyFrames);
                KeyFrame[] copies = createKeyFrameCopies(KeyFrameCategory.CHARACTER, keyFrames);

                characters = ArrayUtils.add(characters, character);
                keyFrameSet = ArrayUtils.add(keyFrameSet, copies);
            }

            runKeyFrameAddActions(cameraCopies, characters, keyFrameSet);
            return;
        }

        if (selectionManager.getSelectionSize() == 0 && cameraCopies.length > 0)
        {
            runKeyFrameAddActions(cameraCopies);
            return;
        }

        JPopupMenu popup = new JPopupMenu();

        JLabel title = new JLabel("Paste to Whom?");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setBorder(new EmptyBorder(2, 6, 2, 6));
        popup.add(title);
        popup.addSeparator();

        JMenuItem pasteToEach = new JMenuItem("Paste To Each");
        pasteToEach.setToolTipText("Will paste every copied keyframes to every selected Object");
        pasteToEach.addActionListener(e ->
        {
            Character[] characters = new Character[0];
            KeyFrame[][] keyFrameSet = new KeyFrame[0][0];

            for (Character character : selectionManager.getSelected())
            {
                KeyFrame[] keyFrames = clearOverridingCopies(character, copiedKeyFrames);
                KeyFrame[] copies = createKeyFrameCopies(KeyFrameCategory.CHARACTER, keyFrames);

                characters = ArrayUtils.add(characters, character);
                keyFrameSet = ArrayUtils.add(keyFrameSet, copies);
            }

            runKeyFrameAddActions(cameraCopies, characters, keyFrameSet);
        });
        popup.add(pasteToEach);

        JMenuItem pasteIteratively = new JMenuItem("Paste Iteratively");
        pasteIteratively.setToolTipText("Will paste copied keyframes only to the Object they originate from");
        pasteIteratively.addActionListener(e ->
        {
            List<KeyFrame[]> keyFrameSet = new ArrayList<>();
            List<Character> characters = new ArrayList<>();

            LinkedHashMap<Character, KeyFrame[]> map = new LinkedHashMap<>();
            copiedKeyFrames.forEach((target, keyframes) ->
            {
                if (target.getType() != KeyFrameCategory.CHARACTER)
                {
                    return;
                }

                KeyFrame[] copies = createKeyFrameCopies(KeyFrameCategory.CHARACTER, keyframes);
                keyFrameSet.add(copies);
                characters.add((Character) target.getValue());
            });

            runKeyFrameAddActions(cameraCopies, characters.toArray(new Character[0]), keyFrameSet.toArray(new KeyFrame[0][]));
        });
        popup.add(pasteIteratively);

        JMenu pasteSingle = new JMenu("Paste to Only:");
        pasteSingle.setToolTipText("Will paste copied keyframes only to the following Object");
        popup.add(pasteSingle);

        for (Character character : selectionManager.getSelected())
        {
            JMenuItem pasteTo = new JMenuItem(character.getName());
            pasteTo.addActionListener(e ->
                    {
                        KeyFrame[] keyFrames = clearOverridingCopies(character, copiedKeyFrames);
                        KeyFrame[] copies = createKeyFrameCopies(KeyFrameCategory.CHARACTER, keyFrames);
                        runKeyFrameAddActions(cameraCopies, new Character[]{character}, new KeyFrame[][]{copies});
                    });
            pasteSingle.add(pasteTo);
        }

        int x = (int) p.getX();
        int y = (int) p.getY();

        popup.show(this, x, y);
    }

    private KeyFrame[] clearOverridingCopies(Character masterCharacter, LinkedHashMap<KeyFrameTarget, KeyFrame[]> keyFrameGroups)
    {
        KeyFrame[] masterKeyFrames = keyFrameGroups.get(new KeyFrameTarget(KeyFrameCategory.CHARACTER, masterCharacter));
        List<KeyFrame> keyFramesToAdd = new ArrayList<>();

        if (masterKeyFrames != null)
        {
            keyFramesToAdd.addAll(Arrays.asList(masterKeyFrames));
        }

        keyFrameGroups.forEach((target, keyFrames) ->
        {
            for (KeyFrame keyFrame : keyFrames)
            {
                boolean overrides = false;
                if (masterKeyFrames != null)
                {
                    for (KeyFrame master : masterKeyFrames)
                    {
                        if (keyFrame.getTick() == master.getTick() && keyFrame.getKeyFrameType() == master.getKeyFrameType())
                        {
                            overrides = true;
                            break;
                        }
                    }
                }

                for (KeyFrame alreadyAdded : keyFramesToAdd)
                {
                    if (keyFrame.getTick() == alreadyAdded.getTick() && keyFrame.getKeyFrameType() == alreadyAdded.getKeyFrameType())
                    {
                        overrides = true;
                        break;
                    }
                }

                if (!overrides)
                {
                    keyFramesToAdd.add(keyFrame);
                }
            }
        });

        return keyFramesToAdd.toArray(new KeyFrame[0]);
    }

    private KeyFrame[] createKeyFrameCopies(KeyFrameCategory category, KeyFrame[] keyFrames)
    {
        double firstTick = ABSOLUTE_MAX_SEQUENCE_LENGTH;
        for (KeyFrame keyFrame : keyFrames)
        {
            if (keyFrame.getTick() < firstTick)
            {
                firstTick = keyFrame.getTick();
            }
        }

        KeyFrame[] selected = new KeyFrame[0];
        for (KeyFrame keyFrame : keyFrames)
        {
            if (!KeyFrameCategory.contains(category, keyFrame.getKeyFrameType()))
            {
                continue;
            }

            double newTime = round(keyFrame.getTick() - firstTick + currentTime);
            KeyFrame copy = KeyFrame.createCopy(keyFrame, newTime);
            selected = ArrayUtils.add(selected, copy);
        }

        return selected;
    }

    public void onSelectAllPressed()
    {
        kfsm.clear();

        KeyFrame[] cameraKeyFrames = cameraManager.getKeyFrames();
        if (cameraKeyFrames.length > 0)
        {
            KeyFrame primary = cameraKeyFrames[0];
            kfsm.add(new KeyFrameTarget(KeyFrameCategory.CAMERA, null), cameraKeyFrames, primary);
        }

        for (Character character : selectionManager.getSelected())
        {
            KeyFrame[][] frames = character.getFrames();
            List<KeyFrame> kfs = new ArrayList<>();

            KeyFrame primary = null;
            for (KeyFrame[] keyFrames : frames)
            {
                if (keyFrames == null || keyFrames.length == 0)
                {
                    continue;
                }

                primary = keyFrames[0];
                kfs.addAll(Arrays.asList(keyFrames));
            }

            kfsm.add(new KeyFrameTarget(KeyFrameCategory.CHARACTER, character), kfs.toArray(new KeyFrame[0]), primary);
        }

        onKeyFrameSelectionChanged();
    }

    public void onDeleteKeyPressed()
    {
        ArrayList<KeyFrameAction> kfa = new ArrayList<>();

        LinkedHashMap<KeyFrameTarget, KeyFrame[]> selected = new LinkedHashMap<>(kfsm.getSelected());

        selected.forEach((KeyFrameTarget target, KeyFrame[] keyFrames) ->
        {
            switch (target.getType())
            {
                case CHARACTER:
                    for (KeyFrame keyFrame : keyFrames)
                    {
                        kfa.add(new KeyFrameCharacterAction(keyFrame, (Character) target.getValue(), KeyFrameActionType.REMOVE));
                        removeKeyFrame(target, keyFrame);
                    }
                    break;
                case CAMERA:
                    for (KeyFrame keyFrame : keyFrames)
                    {
                        kfa.add(new KeyFrameCameraAction(keyFrame, KeyFrameActionType.REMOVE));
                        removeKeyFrame(target, keyFrame);
                    }
                    break;
            }
        });

        stackKeyFrameActions(kfa);
    }

    public void inchTimeline(double modifier)
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
        int index = KeyFrameType.getCharacterKeyFrameIndex(attributePanel.getSelectedKeyFramePage()) + direction;
        int totalFrameTypes = KeyFrameType.getTotalCharacterKeyFrameTypes();
        if (index >= totalFrameTypes)
        {
            index = 0;
        }

        if (index == -1)
        {
            index = totalFrameTypes - 1;
        }

        attributePanel.switchCards(KeyFrameType.getCharacterKeyFrameType(index).toString());
        attributePanel.updateAttributes();
    }

    private void setupLayout()
    {
        setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        setContinuousLayout(true);

        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        leftSplitPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rightSplitPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(leftSplitPane, JSplitPane.LEFT);
        add(rightSplitPane, JSplitPane.RIGHT);

        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JLabel summaryLabel = new JLabel("Summary");
        summaryLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        summaryLabel.setFont(FontManager.getRunescapeBoldFont());
        summaryLabel.setBorder(new EmptyBorder(4, 2, 3, 2));
        summaryPanel.add(summaryLabel, BorderLayout.NORTH);
        summaryPanel.add(summarySheet, BorderLayout.CENTER);

        JPanel cameraPanel = new JPanel(new BorderLayout());
        cameraPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel cameraLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cameraLabelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel cameraLabel = new JLabel("Camera");
        cameraLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cameraLabel.setFont(FontManager.getRunescapeBoldFont());
        cameraLabel.setBorder(new EmptyBorder(4, 2, 3, 2));
        cameraLabelPanel.add(cameraLabel);

        JCheckBox cameraToggle = new JCheckBox("Enable Camera", true);
        cameraToggle.addActionListener(e ->
        {
            cameraManager.setEnabled(cameraToggle.isSelected());
        });
        cameraLabelPanel.add(cameraToggle);

        cameraPanel.add(cameraLabelPanel, BorderLayout.NORTH);
        cameraPanel.add(cameraSheet, BorderLayout.CENTER);
        summaryPanel.add(cameraPanel, BorderLayout.SOUTH);

        JPanel attributeControls = new JPanel(new BorderLayout());
        attributeControls.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JPanel scrollControls = new JPanel(new BorderLayout());
        scrollControls.add(scrollBar, BorderLayout.NORTH);
        scrollControls.add(controlPanel, BorderLayout.CENTER);
        attributeControls.add(scrollControls, BorderLayout.NORTH);
        attributeControls.add(attributeSheet, BorderLayout.CENTER);

        leftSplitPane.add(treeScrollPane);
        leftSplitPane.add(attributePanel);
        rightSplitPane.add(summaryPanel);
        rightSplitPane.add(attributeControls);
        initializeDimensions();
    }

    private void initializeDimensions()
    {
        int horizontalSplit = 700;
        int verticalLeftSplit = 600;
        int verticalRightSplit = 600;
        String summaryConfig = configManager.getConfiguration("creatorssuite", "timesheetsplit");
        if (summaryConfig != null)
        {
            String[] dimensions = summaryConfig.split(",");
            horizontalSplit = Integer.parseInt(dimensions[0]);
            verticalLeftSplit = Integer.parseInt(dimensions[1]);
            verticalRightSplit = Integer.parseInt(dimensions[2]);
        }

        setDividerLocation(horizontalSplit + getInsets().left);
        leftSplitPane.setDividerLocation(verticalLeftSplit + getInsets().bottom);
        rightSplitPane.setDividerLocation(verticalRightSplit + getInsets().bottom);

        addPropertyChangeListener("dividerLocation", e -> updateDividerDimensions());
        leftSplitPane.addPropertyChangeListener("dividerLocation", e -> updateDividerDimensions());
        rightSplitPane.addPropertyChangeListener("dividerLocation", e -> updateDividerDimensions());
    }

    public void updateDividerDimensions()
    {
        int horizontalSplit = getLastDividerLocation();
        int verticalLeftSplit = leftSplitPane.getLastDividerLocation();
        int verticalRightSplit = rightSplitPane.getLastDividerLocation();

        if (horizontalSplit == -1)
        {
            horizontalSplit = getDividerLocation();
        }

        if (verticalLeftSplit == -1)
        {
            verticalLeftSplit = leftSplitPane.getDividerLocation();
        }

        if (verticalRightSplit == -1)
        {
            verticalRightSplit = rightSplitPane.getDividerLocation();
        }

        String dividerDimensions = horizontalSplit + "," + verticalLeftSplit + "," + verticalRightSplit;
        configManager.setConfiguration("creatorssuite", "timesheetsplit", dividerDimensions);
    }

    private void updateSheets()
    {
        summarySheet.setHScroll(hScroll);
        summarySheet.setZoom(zoom);
        attributeSheet.setHScroll(hScroll);
        attributeSheet.setZoom(zoom);
        cameraSheet.setHScroll(hScroll);
        cameraSheet.setZoom(zoom);
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
