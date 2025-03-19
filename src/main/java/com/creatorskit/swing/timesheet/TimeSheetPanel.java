package com.creatorskit.swing.timesheet;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.DataFinder;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.programming.MovementType;
import com.creatorskit.programming.Programmer;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.manager.TreeScrollPane;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameAction;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameCharacterActionType;
import com.creatorskit.swing.timesheet.keyframe.keyframeactions.KeyFrameActionType;
import com.creatorskit.swing.timesheet.sheets.AttributeSheet;
import com.creatorskit.swing.timesheet.sheets.SummarySheet;
import com.creatorskit.swing.timesheet.sheets.TimeSheet;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
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
    private final GridBagConstraints c = new GridBagConstraints();
    private final ToolBoxFrame toolBox;
    private final DataFinder dataFinder;
    private SummarySheet summarySheet;
    private AttributeSheet attributeSheet;
    private TreeScrollPane treeScrollPane;
    private final ManagerTree managerTree;
    private final JComboBox<KeyFrameType> summaryComboBox = new JComboBox<>();
    private JScrollBar scrollBar;
    private AttributePanel attributePanel;
    private final JPanel labelPanel = new JPanel();
    private final JPanel controlPanel = new JPanel();
    private final JButton playButton = new JButton();
    private final ImageIcon PLAY = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Play.png"));
    private final ImageIcon STOP = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Stop.png"));
    private final ImageIcon PAUSE = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Pause.png"));
    private final BufferedImage SKIP_LEFT = ImageUtil.loadImageResource(getClass(), "/Skip_Left.png");
    private final BufferedImage SKIP_RIGHT = ImageUtil.loadImageResource(getClass(), "/Skip_Right.png");

    private static final int ABSOLUTE_MAX_SEQUENCE_LENGTH = 100000;
    private static final int DEFAULT_MIN_H_SCROLL = -10;
    private static final int DEFAULT_MAX_H_SCROLL = 200;
    private static final int ZOOM_MAX = 500;
    private static final int ZOOM_MIN = 5;

    private final String MOVE_CARD = "Movement";
    private final String ANIM_CARD = "Animation";
    private final String ORI_CARD = "Orientation";
    private final String SPAWN_CARD = "Spawn";
    private final String MODEL_CARD = "Model";
    private final String TEXT_CARD = "Text";
    private final String OVER_CARD = "Overhead";
    private final String HEALTH_CARD = "Health";
    private final String SPOTANIM_CARD = "SpotAnim 1";
    private final String SPOTANIM2_CARD = "SpotAnim 2";
    private final String LABEL_OFFSET = "  ";
    private JLabel[] labels = new JLabel[0];

    private double zoom = 50;
    private double hScroll = 0;
    private int vScroll = 0;
    private double maxHScroll = 200;
    private double minHScroll = -10;

    private double currentTime = 0;
    private boolean pauseScrollBarListener = false;
    private Character selectedCharacter;
    private KeyFrameType summaryKeyFrameType = KeyFrameType.SUMMARY;

    private ArrayList<KeyFrameAction> keyFrameStack = new ArrayList<>();
    private KeyFrame[] selectedKeyFrames = new KeyFrame[0];
    private KeyFrame[] copiedKeyFrames = new KeyFrame[0];
    private KeyFrameAction[][] keyFrameActions = new KeyFrameAction[0][];

    private final int UNDO_LIMIT = 15;
    private int undoStack = 0;

    @Inject
    public TimeSheetPanel(@Nullable Client client, ToolBoxFrame toolBox, CreatorsPlugin plugin, ClientThread clientThread, DataFinder dataFinder, ManagerTree managerTree)
    {
        this.client = client;
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.dataFinder = dataFinder;
        this.managerTree = managerTree;

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        setupSummaryComboBox();
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

    public void onSummarySkipForward()
    {
        
    }

    public void onSummarySkipPrevious()
    {

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
        if (selectedCharacter == null)
        {
            return;
        }

        KeyFrame keyFrame = selectedCharacter.findKeyFrame(attributePanel.getSelectedKeyFramePage(), currentTime);
        if (keyFrame == null)
        {
            KeyFrame kf = attributePanel.createKeyFrame();
            if (kf == null)
            {
                return;
            }

            KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(kf, selectedCharacter, KeyFrameCharacterActionType.ADD)};
            KeyFrame keyFrameToReplace = addKeyFrame(selectedCharacter, kf);

            if (keyFrameToReplace != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
            }
            addKeyFrameActions(kfa);
            return;
        }

       removeKeyFrame(selectedCharacter, keyFrame);
       KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(keyFrame, selectedCharacter, KeyFrameCharacterActionType.REMOVE)};
       addKeyFrameActions(kfa);
    }

    public void initializeMovementKeyFrame(Character character, WorldView worldView, LocalPoint localPoint)
    {
        boolean poh = MovementManager.isInPOH(worldView);

        KeyFrame keyFrame = character.findKeyFrame(KeyFrameType.MOVEMENT, currentTime);
        if (keyFrame == null)
        {
            int x = localPoint.getSceneX();
            int y = localPoint.getSceneY();
            if (!poh)
            {
                WorldPoint wp = WorldPoint.fromLocalInstance(client, localPoint, worldView.getPlane());
                x = wp.getX();
                y = wp.getY();
            }

            KeyFrame kf = new MovementKeyFrame(
                    currentTime,
                    worldView.getPlane(),
                    poh,
                    new int[][]{new int[]{x, y}},
                    0,
                    0,
                    false,
                    1,
                    MovementType.NORMAL);

            KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(kf, character, KeyFrameCharacterActionType.ADD)};
            KeyFrame keyFrameToReplace = addKeyFrame(character, kf);

            if (keyFrameToReplace != null)
            {
                kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, character, KeyFrameCharacterActionType.REMOVE));
            }
            addKeyFrameActions(kfa);
            return;
        }

        removeKeyFrame(character, keyFrame);
        KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(keyFrame, character, KeyFrameCharacterActionType.REMOVE)};
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
        toolBox.getProgrammer().updateProgram(character, currentTime);

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
        toolBox.getProgrammer().updateProgram(character, currentTime);
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
        int length = keyFrameActions.length;
        if (length == UNDO_LIMIT)
        {
            keyFrameActions = ArrayUtils.remove(keyFrameActions, length - 1);
        }

        if (undoStack != length - 1)
        {
            for (int i = undoStack + 1; i == length - 1; i++)
            {
                keyFrameActions = ArrayUtils.remove(keyFrameActions, undoStack + 1);
            }
        }

        keyFrameActions = ArrayUtils.add(keyFrameActions, actions);
        undoStack = keyFrameActions.length - 1;
    }

    public void undo()
    {
        if (undoStack == -1)
        {
            return;
        }

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
                    addKeyFrame(kfca.getCharacter(), keyFrameAction.getKeyFrame());
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

        KeyFrameAction[] lastUndoneActions = keyFrameActions[undoStack];
        for (KeyFrameAction keyFrameAction : lastUndoneActions)
        {
            if (keyFrameAction.getActionType() == KeyFrameActionType.CHARACTER)
            {
                KeyFrameCharacterAction keyFrameCharacterAction = (KeyFrameCharacterAction) keyFrameAction;
                KeyFrameCharacterActionType actionType = keyFrameCharacterAction.getCharacterActionType();
                if (actionType == KeyFrameCharacterActionType.ADD)
                {
                    addKeyFrame(keyFrameCharacterAction.getCharacter(), keyFrameAction.getKeyFrame());
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
    }

    public void setCurrentTime(double tick, boolean playing)
    {
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

        Programmer programmer = toolBox.getProgrammer();

        if (playing)
        {
            programmer.updateProgramsOnTick();
        }
        else
        {
            programmer.updatePrograms(tick);
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

    public void setPreviewTime(double tick)
    {
        attributeSheet.setPreviewTime(tick);
        summarySheet.setPreviewTime(tick);
    }

    private void setupSummaryComboBox()
    {
        summaryComboBox.setBackground(ColorScheme.DARK_GRAY_COLOR);

        summaryComboBox.addItem(KeyFrameType.SUMMARY);
        summaryComboBox.addItem(KeyFrameType.MOVEMENT);
        summaryComboBox.addItem(KeyFrameType.ANIMATION);
        summaryComboBox.addItem(KeyFrameType.SPAWN);
        summaryComboBox.addItem(KeyFrameType.MODEL);
        summaryComboBox.addItem(KeyFrameType.ORIENTATION);
        summaryComboBox.addItem(KeyFrameType.TEXT);
        summaryComboBox.addItem(KeyFrameType.OVERHEAD);
        summaryComboBox.addItem(KeyFrameType.HEALTH);
        summaryComboBox.addItem(KeyFrameType.SPOTANIM);
        summaryComboBox.addItem(KeyFrameType.SPOTANIM2);

        summaryComboBox.addItemListener(e -> summaryKeyFrameType = (KeyFrameType) summaryComboBox.getSelectedItem());
    }

    private void setupTreeScrollPane()
    {
        treeScrollPane = new TreeScrollPane(managerTree);
        treeScrollPane.setPreferredSize(new Dimension(614, 0));
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
        JSpinner timeSpinner = new JSpinner();
        timeSpinner.setBackground(ColorScheme.DARK_GRAY_COLOR);
        timeSpinner.setModel(new SpinnerNumberModel(0, -ABSOLUTE_MAX_SEQUENCE_LENGTH, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.1));
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) timeSpinner.getEditor();
        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(2);
        timeSpinner.setValue(0);
        timeSpinner.addChangeListener(e ->
        {
            double tick = TimeSheetPanel.round((double) timeSpinner.getValue());
            toolBox.getTimeSheetPanel().setCurrentTime(tick, false);
        });
        controlPanel.add(timeSpinner, c);

        playButton.setIcon(PLAY);
        playButton.setPreferredSize(new Dimension(35, 35));
        playButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        playButton.addActionListener(e -> toolBox.getProgrammer().togglePlay());

        JPanel backPanel = new JPanel();
        backPanel.setLayout(new BoxLayout(backPanel, BoxLayout.PAGE_AXIS));
        JButton backSummarySheet = new JButton(new ImageIcon(SKIP_LEFT));
        JButton backAttributeSheet = new JButton(new ImageIcon(SKIP_LEFT));
        backSummarySheet.setBackground(ColorScheme.DARK_GRAY_COLOR);
        backAttributeSheet.setBackground(ColorScheme.DARK_GRAY_COLOR);
        backSummarySheet.addActionListener(e -> onSummarySkipPrevious());
        backAttributeSheet.addActionListener(e -> onAttributeSkipPrevious());
        backPanel.add(backSummarySheet);
        backPanel.add(backAttributeSheet);

        JPanel forwardPanel = new JPanel();
        forwardPanel.setLayout(new BoxLayout(forwardPanel, BoxLayout.PAGE_AXIS));
        JButton forwardSummarySheet = new JButton(new ImageIcon(SKIP_RIGHT));
        JButton forwardAttributeSheet = new JButton(new ImageIcon(SKIP_RIGHT));
        forwardSummarySheet.setBackground(ColorScheme.DARK_GRAY_COLOR);
        forwardAttributeSheet.setBackground(ColorScheme.DARK_GRAY_COLOR);
        forwardSummarySheet.addActionListener(e -> onSummarySkipForward());
        forwardAttributeSheet.addActionListener(e -> onAttributeSkipForward());
        forwardPanel.add(forwardSummarySheet);
        forwardPanel.add(forwardAttributeSheet);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 0;
        JPanel controls = new JPanel();
        controls.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        controls.add(backPanel);
        controls.add(playButton);
        controls.add(forwardPanel);
        controlPanel.add(controls, c);
    }

    private void setupAttributePanel()
    {
        attributePanel = new AttributePanel(client, this, dataFinder);
        summarySheet = new SummarySheet(toolBox, managerTree, attributePanel);
        attributeSheet = new AttributeSheet(toolBox, managerTree, attributePanel);
    }

    private void setupAttributeSheet()
    {
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setBorder(new EmptyBorder(1, 0, 1, 0));
        labelPanel.setLayout(new GridLayout(0, 1, 0, 0));
        labelPanel.setFocusable(true);
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

        labels[1].setText(MOVE_CARD + LABEL_OFFSET);
        labels[2].setText(ANIM_CARD + LABEL_OFFSET);
        labels[3].setText(ORI_CARD + LABEL_OFFSET);
        labels[4].setText(SPAWN_CARD + LABEL_OFFSET);
        labels[5].setText(MODEL_CARD + LABEL_OFFSET);
        labels[6].setText(TEXT_CARD + LABEL_OFFSET);
        labels[7].setText(OVER_CARD + LABEL_OFFSET);
        labels[8].setText(HEALTH_CARD + LABEL_OFFSET);
        labels[9].setText(SPOTANIM_CARD + LABEL_OFFSET);
        labels[10].setText(SPOTANIM2_CARD + LABEL_OFFSET);
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

        KeyFrameAction[] kfa = new KeyFrameAction[0];
        for (KeyFrame keyFrame : copiedKeyFrames)
        {
            double newTime = round(keyFrame.getTick() - firstTick + currentTime);
            KeyFrame copy = KeyFrame.createCopy(keyFrame, newTime);
            if (copy == null)
            {
                continue;
            }

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
                    KeyFrame keyFrame = attributePanel.createKeyFrame();
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

                KeyFrame keyFrame = attributePanel.createKeyFrame();
                KeyFrame keyFrameToReplace = addKeyFrame(selectedCharacter, keyFrame);
                KeyFrameAction[] kfa = new KeyFrameAction[]{new KeyFrameCharacterAction(keyFrame, selectedCharacter, KeyFrameCharacterActionType.ADD)};

                if (keyFrameToReplace != null)
                {
                    kfa = ArrayUtils.add(kfa, new KeyFrameCharacterAction(keyFrameToReplace, selectedCharacter, KeyFrameCharacterActionType.REMOVE));
                }
                addKeyFrameActions(kfa);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "VK_C");
        actionMap.put("VK_C", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                copyKeyFrames();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "VK_V");
        actionMap.put("VK_V", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                pasteKeyFrames();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "VK_Z");
        actionMap.put("VK_Z", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                undo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "VK_Y");
        actionMap.put("VK_Y", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                redo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), "VK_SPACE");
        actionMap.put("VK_SPACE", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                toolBox.getProgrammer().togglePlay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "VK_R");
        actionMap.put("VK_R", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setCurrentTime(0, false);
                toolBox.getProgrammer().togglePlay(false);
            }
        });
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

                    int currentRow = managerTree.getMinSelectionRow();
                    if (currentRow == -1)
                    {
                        managerTree.setSelectionRow(0);
                        return;
                    }

                    TreePath path = null;
                    int direction = e.getWheelRotation();
                    if (direction > 0)
                    {
                        while (path == null)
                        {
                            currentRow++;
                            if (currentRow >= managerTree.getRowCount())
                            {
                                currentRow = 0;
                            }

                            path = managerTree.getPathForRow(currentRow);
                        }
                    }

                    if (direction < 0)
                    {
                        while (path == null)
                        {
                            currentRow--;
                            if (currentRow < 0)
                            {
                                currentRow = managerTree.getRowCount() - 1;
                            }

                            path = managerTree.getPathForRow(currentRow);
                        }
                    }

                    if (path != null)
                    {
                        managerTree.setSelectionPath(path);
                    }
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
        add(summaryComboBox, c);

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
        add(labelPanel, c);

        c.weightx = 8;
        c.weighty = 0;
        c.gridx = 2;
        c.gridy = 4;
        add(attributeSheet, c);
    }

    private void updateSheets()
    {
        summarySheet.setHScroll(hScroll);
        summarySheet.setVScroll(vScroll);
        summarySheet.setZoom(zoom);
        attributeSheet.setHScroll(hScroll);
        attributeSheet.setVScroll(vScroll);
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
