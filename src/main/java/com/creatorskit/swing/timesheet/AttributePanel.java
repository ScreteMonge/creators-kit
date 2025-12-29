package com.creatorskit.swing.timesheet;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.models.CustomModel;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.datatypes.*;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.programming.orientation.Orientation;
import com.creatorskit.programming.orientation.OrientationGoal;
import com.creatorskit.swing.searchabletable.JFilterableTable;
import com.creatorskit.swing.timesheet.attributes.*;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.settings.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
@Setter
public class AttributePanel extends JPanel
{
    private Client client;
    private ClientThread clientThread;
    private CreatorsConfig config;
    private TimeSheetPanel timeSheetPanel;
    private DataFinder dataFinder;

    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");
    private final BufferedImage COMPASS = ImageUtil.loadImageResource(getClass(), "/Orientation_compass.png");
    private final BufferedImage RESET = ImageUtil.loadImageResource(getClass(), "/Reset.png");
    private final Icon keyframeImage = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Keyframe.png"));
    private final Icon keyframeEmptyImage = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Keyframe_Empty.png"));

    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel cardPanel = new JPanel();
    private final JLabel objectLabel = new JLabel("[No Object Selected]");
    private final JLabel cardLabel = new JLabel("");
    private final JButton keyFramed = new JButton();

    private final JFilterableTable npcTable = new JFilterableTable("NPCs");
    private final JFilterableTable itemTable = new JFilterableTable("Items");
    private final JFilterableTable animTable = new JFilterableTable("Animations");
    private final JFilterableTable spotanimTable = new JFilterableTable("SpotAnims");

    private final JPopupMenu spotanimPopup = new JPopupMenu("SpotAnims");

    public static final String MOVE_CARD = "Movement";
    public static final String ANIM_CARD = "Animation";
    public static final String ORI_CARD = "Orientation";
    public static final String SPAWN_CARD = "Spawn";
    public static final String MODEL_CARD = "Model";
    public static final String TEXT_CARD = "Text";
    public static final String OVER_CARD = "Overhead";
    public static final String HEALTH_CARD = "Health";
    public static final String SPOTANIM_CARD = "SpotAnim 1";
    public static final String SPOTANIM2_CARD = "SpotAnim 2";
    public static final String HITSPLAT_1_CARD = "Hitsplat 1";
    public static final String HITSPLAT_2_CARD = "Hitsplat 2";
    public static final String HITSPLAT_3_CARD = "Hitsplat 3";
    public static final String HITSPLAT_4_CARD = "Hitsplat 4";

    private final String NO_OBJECT_SELECTED = "[No Object Selected]";
    private Font attributeFont = new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 32);

    private KeyFrameType hoveredKeyFrameType;
    private Component hoveredComponent;
    private KeyFrameType selectedKeyFramePage = KeyFrameType.MOVEMENT;

    private final MovementAttributes movementAttributes = new MovementAttributes();
    private final AnimAttributes animAttributes = new AnimAttributes();
    private final OriAttributes oriAttributes = new OriAttributes();
    private final SpawnAttributes spawnAttributes = new SpawnAttributes();
    private final ModelAttributes modelAttributes = new ModelAttributes();
    private final TextAttributes textAttributes = new TextAttributes();
    private final OverheadAttributes overheadAttributes = new OverheadAttributes();
    private final HealthAttributes healthAttributes = new HealthAttributes();
    private final SpotAnimAttributes spotAnimAttributes = new SpotAnimAttributes();
    private final SpotAnimAttributes spotAnim2Attributes = new SpotAnimAttributes();
    private final HitsplatAttributes hitsplat1Attributes = new HitsplatAttributes();
    private final HitsplatAttributes hitsplat2Attributes = new HitsplatAttributes();
    private final HitsplatAttributes hitsplat3Attributes = new HitsplatAttributes();
    private final HitsplatAttributes hitsplat4Attributes = new HitsplatAttributes();

    private final Random random = new Random();

    @Inject
    public AttributePanel(Client client, ClientThread clientThread, CreatorsConfig config, TimeSheetPanel timeSheetPanel, DataFinder dataFinder)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.timeSheetPanel = timeSheetPanel;
        this.dataFinder = dataFinder;

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        objectLabel.setFont(attributeFont);
        objectLabel.setForeground(Color.WHITE);
        objectLabel.setHorizontalAlignment(SwingConstants.LEFT);

        cardPanel.setLayout(new CardLayout());
        cardPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cardPanel.setFocusable(true);
        addMouseFocusListener(cardPanel);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        add(objectLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        cardLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        cardLabel.setFont(FontManager.getRunescapeBoldFont());
        cardLabel.setText(MOVE_CARD);
        add(cardLabel, c);

        c.gridx = 2;
        c.gridy = 0;
        JButton update = new JButton("Update");
        update.setBackground(ColorScheme.DARK_GRAY_COLOR);
        update.addActionListener(e -> timeSheetPanel.onUpdateButtonPressed());
        add(update, c);

        c.gridx = 3;
        c.gridy = 0;
        JButton reset = new JButton(new ImageIcon(RESET));
        reset.setBackground(ColorScheme.DARK_GRAY_COLOR);
        reset.setToolTipText("Reset all the parameters of the currently visible KeyFrame");
        reset.addActionListener(e -> setAttributesEmpty(false));
        add(reset, c);

        c.gridx = 4;
        c.gridy = 0;
        keyFramed.setIcon(keyframeEmptyImage);
        keyFramed.setPreferredSize(new Dimension(32, 32));
        keyFramed.setBackground(ColorScheme.DARK_GRAY_COLOR);
        keyFramed.addActionListener(e -> timeSheetPanel.onKeyFrameIconPressedEvent());
        add(keyFramed, c);

        c.gridwidth = 5;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        add(cardPanel, c);

        JPanel moveCard = new JPanel();
        JPanel animCard = new JPanel();
        JPanel oriCard = new JPanel();
        JPanel spawnCard = new JPanel();
        JPanel modelCard = new JPanel();
        JPanel textCard = new JPanel();
        JPanel overCard = new JPanel();
        JPanel healthCard = new JPanel();
        JPanel spotanimCard = new JPanel();
        JPanel spotanim2Card = new JPanel();
        JPanel hitsplat1Card = new JPanel();
        JPanel hitsplat2Card = new JPanel();
        JPanel hitsplat3Card = new JPanel();
        JPanel hitsplat4Card = new JPanel();
        cardPanel.add(moveCard, MOVE_CARD);
        cardPanel.add(animCard, ANIM_CARD);
        cardPanel.add(oriCard, ORI_CARD);
        cardPanel.add(spawnCard, SPAWN_CARD);
        cardPanel.add(modelCard, MODEL_CARD);
        cardPanel.add(textCard, TEXT_CARD);
        cardPanel.add(overCard, OVER_CARD);
        cardPanel.add(healthCard, HEALTH_CARD);
        cardPanel.add(spotanimCard, SPOTANIM_CARD);
        cardPanel.add(spotanim2Card, SPOTANIM2_CARD);
        cardPanel.add(hitsplat1Card, HITSPLAT_1_CARD);
        cardPanel.add(hitsplat2Card, HITSPLAT_2_CARD);
        cardPanel.add(hitsplat3Card, HITSPLAT_3_CARD);
        cardPanel.add(hitsplat4Card, HITSPLAT_4_CARD);

        setupMoveCard(moveCard);
        setupAnimCard(animCard);
        setupOriCard(oriCard);
        setupSpawnCard(spawnCard);
        setupModelCard(modelCard);
        setupTextCard(textCard);
        setupOverheadCard(overCard);
        setupHealthCard(healthCard);
        setupSpotAnimFinder();
        setupSpotAnimCard(spotanimCard, KeyFrameType.SPOTANIM);
        setupSpotAnimCard(spotanim2Card, KeyFrameType.SPOTANIM2);
        setupHitsplatCard(hitsplat1Card, KeyFrameType.HITSPLAT_1);
        setupHitsplatCard(hitsplat2Card, KeyFrameType.HITSPLAT_2);
        setupHitsplatCard(hitsplat3Card, KeyFrameType.HITSPLAT_3);
        setupHitsplatCard(hitsplat4Card, KeyFrameType.HITSPLAT_4);

        setupKeyListeners();
    }

    /**
     * Create a keyframe out of the current AttributePanel settings, based on which card is currently being shown
     * @return a keyframe of type depending on which card is currently showing, with settings based on what is displayed on that card
     */
    public KeyFrame createKeyFrame(double tick)
    {
        return createKeyFrame(selectedKeyFramePage, tick);
    }

    /**
     * Create a keyframe of a specified type out of the current AttributePanel settings for the given card of the specified KeyFrameType
     * @param keyFrameType the type of keyframe to add
     * @return a keyframe of indicated type, with settings based on what is displayed on that card
     */
    public KeyFrame createKeyFrame(KeyFrameType keyFrameType, double tick)
    {
        switch (keyFrameType)
        {
            default:
            case MOVEMENT:
                WorldView worldView = client.getTopLevelWorldView();
                if (worldView == null || worldView.getMapRegions() == null)
                {
                    return null;
                }

                return new MovementKeyFrame(
                        tick,
                        worldView.getPlane(),
                        MovementManager.useLocalLocations(worldView),
                        new int[0][],
                        0,
                        0,
                        movementAttributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        (double) movementAttributes.getSpeed().getValue(),
                        (int) movementAttributes.getTurnRate().getValue()
                );
            case ANIMATION:
                return new AnimationKeyFrame(
                        tick,
                        animAttributes.getStall().getSelectedItem() == Toggle.ENABLE,
                        (int) animAttributes.getActive().getValue(),
                        (int) animAttributes.getStartFrame().getValue(),
                        animAttributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        animAttributes.getFreeze().getSelectedItem() == Toggle.ENABLE,
                        (int) animAttributes.getIdle().getValue(),
                        (int) animAttributes.getWalk().getValue(),
                        (int) animAttributes.getRun().getValue(),
                        (int) animAttributes.getWalk180().getValue(),
                        (int) animAttributes.getWalkRight().getValue(),
                        (int) animAttributes.getWalkLeft().getValue(),
                        (int) animAttributes.getIdleRight().getValue(),
                        (int) animAttributes.getIdleLeft().getValue()
                );
            case ORIENTATION:
                return new OrientationKeyFrame(
                        tick,
                        OrientationGoal.POINT,
                        (int) oriAttributes.getStart().getValue(),
                        (int) oriAttributes.getEnd().getValue(),
                        (double) oriAttributes.getDuration().getValue(),
                        (int) oriAttributes.getTurnRate().getValue()
                );
            case SPAWN:
                return new SpawnKeyFrame(
                        tick,
                        spawnAttributes.getSpawn().getSelectedItem() == Toggle.ENABLE
                );
            case MODEL:
                return new ModelKeyFrame(
                        tick,
                        modelAttributes.getModelOverride().getSelectedItem() == ModelToggle.CUSTOM_MODEL,
                        (int) modelAttributes.getModelId().getValue(),
                        (CustomModel) modelAttributes.getCustomModel().getSelectedItem(),
                        (int) modelAttributes.getRadius().getValue()
                );
            case TEXT:
                return new TextKeyFrame(
                        tick,
                        (double) textAttributes.getDuration().getValue(),
                        textAttributes.getText().getText()
                );
            case OVERHEAD:
                return new OverheadKeyFrame(
                        tick,
                        (OverheadSprite) overheadAttributes.getSkullSprite().getSelectedItem(),
                        (OverheadSprite) overheadAttributes.getPrayerSprite().getSelectedItem()
                );
            case HEALTH:
                return new HealthKeyFrame(
                        tick,
                        (double) healthAttributes.getDuration().getValue(),
                        (HealthbarSprite) healthAttributes.getHealthbarSprite().getSelectedItem(),
                        (int) healthAttributes.getMaxHealth().getValue(),
                        (int) healthAttributes.getCurrentHealth().getValue()
                );
            case SPOTANIM:
            case SPOTANIM2:
                SpotAnimAttributes spAttributes;
                switch (keyFrameType)
                {
                    default:
                    case SPOTANIM:
                        spAttributes = spotAnimAttributes;
                        break;
                    case SPOTANIM2:
                        spAttributes = spotAnim2Attributes;
                }
                return new SpotAnimKeyFrame(
                        tick,
                        keyFrameType,
                        (int) spAttributes.getSpotAnimId().getValue(),
                        spAttributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        (int) spAttributes.getHeight().getValue()
                );
            case HITSPLAT_1:
            case HITSPLAT_2:
            case HITSPLAT_3:
            case HITSPLAT_4:
                HitsplatAttributes attributes;
                switch (keyFrameType)
                {
                    default:
                    case HITSPLAT_1:
                        attributes = hitsplat1Attributes;
                        break;
                    case HITSPLAT_2:
                        attributes = hitsplat2Attributes;
                        break;
                    case HITSPLAT_3:
                        attributes = hitsplat3Attributes;
                        break;
                    case HITSPLAT_4:
                        attributes = hitsplat4Attributes;
                }

                return new HitsplatKeyFrame(
                        tick,
                        keyFrameType,
                        (int) attributes.getDuration().getValue(),
                        (HitsplatSprite) attributes.getSprite().getSelectedItem(),
                        (HitsplatVariant) attributes.getVariant().getSelectedItem(),
                        (int) attributes.getDamage().getValue()
                );
        }
    }

    private void setupMoveCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Movement");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("<html>Set how the Object moves. Hotkeys for adding and removing steps in the scene are as follows: " +
                "<br>" + config.addProgramStepHotkey().toString() + ": adds program steps to the hovered tile" +
                "<br>" + config.removeProgramStepHotkey().toString() + ": removes the last program step" +
                "<br>" + config.clearProgramStepHotkey().toString() + ": clears all steps for the current Movement KeyFrame</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel loopLabel = new JLabel("Loop: ");
        loopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        //card.add(loopLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JComboBox<Toggle> loop = movementAttributes.getLoop();
        loop.setToolTipText("Choose whether the program should loop its designated path");
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        loop.setBackground(ColorScheme.DARK_GRAY_COLOR);
        //card.add(loop, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel speedLabel = new JLabel("Speed: ");
        speedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(speedLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JSpinner speed = movementAttributes.getSpeed();
        speed.setToolTipText("Set the speed at which the Object moves, in tiles/tick");
        speed.setModel(new SpinnerNumberModel(1.0, 0.5, 10, 0.5));
        card.add(speed, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel turnRateLabel = new JLabel("Turn Rate: ");
        turnRateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(turnRateLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner turnRate = movementAttributes.getTurnRate();
        turnRate.setToolTipText("Determines the rate at which the Object rotates during movement in JUnits/clientTick");
        turnRate.setModel(new SpinnerNumberModel(OrientationKeyFrame.TURN_RATE, 0, 2048, 1));
        card.add(turnRate, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupAnimCard(JPanel card)
    {
        Dimension spinnerSize = new Dimension(90, 25);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel generalTitlePanel = new JPanel();
        generalTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(generalTitlePanel, c);

        JLabel generalTitle = new JLabel("General");
        generalTitle.setHorizontalAlignment(SwingConstants.LEFT);
        generalTitle.setFont(FontManager.getRunescapeBoldFont());
        generalTitlePanel.add(generalTitle);

        JLabel help = new JLabel(new ImageIcon(HELP));
        help.setHorizontalAlignment(SwingConstants.LEFT);
        help.setBorder(new EmptyBorder(0, 4, 0, 4));
        help.setToolTipText("<html>Pose Animations dynamically update your Object based on its current movement trajectory" +
                "<br>For example: an Object that isn't moving will use the given Idle animation; an Object taking a 90 degree right turn will use Walk Right animation." +
                "<br>Active Animations will instead override the current Pose Animation, playing regardless of the Object's movement trajectory</html>");
        generalTitlePanel.add(help);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel startFrameLabel = new JLabel("1st Frame: ");
        startFrameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(startFrameLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner startFrame = animAttributes.getStartFrame();
        startFrame.setToolTipText("Set the frame at which the animation starts at");
        startFrame.setModel(new SpinnerNumberModel(0, 0, 99999, 1));
        startFrame.setPreferredSize(spinnerSize);
        card.add(startFrame, c);

        c.gridx = 2;
        c.gridy = 1;
        JButton randomize = new JButton("Random");
        randomize.setToolTipText("Sets a random starting frame between 0 to the maximum number of frames for the animation that is currently playing");
        card.add(randomize, c);

        /*
        c.gridx = 0;
        c.gridy = 2;
        JLabel stallLabel = new JLabel("Stall: ");
        stallLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(stallLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JComboBox<Toggle> stall = animAttributes.getStall();
        stall.setFocusable(false);
        stall.addItem(Toggle.DISABLE);
        stall.addItem(Toggle.ENABLE);
        card.add(stall, c);

         */

        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 3;
        JLabel manualTitle = new JLabel("Active Animation");
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        card.add(manualTitle, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 4;
        JLabel manualLabel = new JLabel("Active: ");
        manualLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(manualLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner manual = animAttributes.getActive();
        manual.setToolTipText("Set the Active animation. This animation overrides the Pose animation, and should be used when performing an action like an attack or emote");
        manual.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        manual.setPreferredSize(spinnerSize);
        card.add(manual, c);

        c.gridx = 2;
        c.gridy = 4;
        JLabel loopLabel = new JLabel("Loop: ");
        loopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(loopLabel, c);

        c.gridx = 3;
        c.gridy = 4;
        JComboBox<Toggle> loop = animAttributes.getLoop();
        loop.setToolTipText("Sets whether the Active animation should loop until the next Animation KeyFrame");
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        card.add(loop, c);

        c.gridx = 4;
        c.gridy = 4;
        JLabel freezeLabel = new JLabel("Freeze: ");
        freezeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(freezeLabel, c);

        c.gridx = 5;
        c.gridy = 4;
        JComboBox<Toggle> freeze = animAttributes.getFreeze();
        freeze.setToolTipText("Set whether the animation should freeze on the frame indicated by 1st Frame");
        freeze.setFocusable(false);
        freeze.addItem(Toggle.DISABLE);
        freeze.addItem(Toggle.ENABLE);
        card.add(freeze, c);

        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 5;
        JLabel smartTitle = new JLabel("Pose Animations");
        smartTitle.setFont(FontManager.getRunescapeBoldFont());
        card.add(smartTitle, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 6;
        JLabel idleLabel = new JLabel("Idle: ");
        idleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleLabel, c);

        c.gridx = 1;
        c.gridy = 6;
        JSpinner idle = animAttributes.getIdle();
        idle.setToolTipText("The animation to play while standing idly without moving");
        idle.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idle.setPreferredSize(spinnerSize);
        card.add(idle, c);

        c.gridx = 2;
        c.gridy = 6;
        JLabel walk180Label = new JLabel("Walk 180: ");
        walk180Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walk180Label, c);

        c.gridx = 3;
        c.gridy = 6;
        JSpinner walk180 = animAttributes.getWalk180();
        walk180.setToolTipText("The animation to play while moving and turning in a 180");
        walk180.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walk180.setPreferredSize(spinnerSize);
        card.add(walk180, c);

        c.gridx = 0;
        c.gridy = 7;
        JLabel walkLabel = new JLabel("Walk: ");
        walkLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkLabel, c);

        c.gridx = 1;
        c.gridy = 7;
        JSpinner walk = animAttributes.getWalk();
        walk.setToolTipText("The animation to play while walking");
        walk.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walk.setPreferredSize(spinnerSize);
        card.add(walk, c);

        c.gridx = 2;
        c.gridy = 7;
        JLabel walkRLabel = new JLabel("Walk Right: ");
        walkRLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkRLabel, c);

        c.gridx = 3;
        c.gridy = 7;
        JSpinner walkRight = animAttributes.getWalkRight();
        walkRight.setToolTipText("The animation to play while walking and rotating to the right");
        walkRight.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walkRight.setPreferredSize(spinnerSize);
        card.add(walkRight, c);

        c.gridx = 4;
        c.gridy = 7;
        JLabel idleRLabel = new JLabel("Idle Right: ");
        idleRLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleRLabel, c);

        c.gridx = 5;
        c.gridy = 7;
        JSpinner idleRight = animAttributes.getIdleRight();
        idleRight.setToolTipText("The animation to play while standing and rotating to the right");
        idleRight.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idleRight.setPreferredSize(spinnerSize);
        card.add(idleRight, c);

        c.gridx = 0;
        c.gridy = 8;
        JLabel runLabel = new JLabel("Run: ");
        runLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(runLabel, c);

        c.gridx = 1;
        c.gridy = 8;
        JSpinner run = animAttributes.getRun();
        run.setToolTipText("The animation to play while running");
        run.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        run.setPreferredSize(spinnerSize);
        card.add(run, c);

        c.gridx = 2;
        c.gridy = 8;
        JLabel walkLLabel = new JLabel("Walk Left: ");
        walkLLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkLLabel, c);

        c.gridx = 3;
        c.gridy = 8;
        JSpinner walkLeft = animAttributes.getWalkLeft();
        walkLeft.setToolTipText("The animation to play while walking and rotating to the left");
        walkLeft.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walkLeft.setPreferredSize(spinnerSize);
        card.add(walkLeft, c);

        c.gridx = 4;
        c.gridy = 8;
        JLabel idleLLabel = new JLabel("Idle Left: ");
        idleLLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleLLabel, c);

        c.gridx = 5;
        c.gridy = 8;
        JSpinner idleLeft = animAttributes.getIdleLeft();
        idleLeft.setToolTipText("The animation to play while standing and rotating to the left");
        idleLeft.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idleLeft.setPreferredSize(spinnerSize);
        card.add(idleLeft, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 11;
        JLabel npcSearcherLabel = new JLabel("NPC Presets: ");
        npcSearcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(npcSearcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 11;
        JTextField npcField = new JTextField("");
        npcField.setToolTipText("Search up different NPCs, and double click the name to apply all of its Pose animations");
        npcField.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(npcField, c);

        JPopupMenu npcPopup = new JPopupMenu("NPCs");
        JScrollPane npcScrollPane = new JScrollPane(npcTable);
        npcPopup.add(npcScrollPane);

        KeyListener npcListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = npcField.getText();
                npcTable.searchAndListEntries(text);
                npcPopup.setVisible(true);
                Point p = npcField.getLocationOnScreen();
                npcPopup.setLocation(new Point((int) p.getX() + npcField.getWidth(), (int) p.getY()));
            }
        };
        npcField.addKeyListener(npcListener);

        npcField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                npcPopup.setVisible(false);
            }
        });

        npcTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = npcTable.getSelectedObject();
                    if (o instanceof NPCData)
                    {
                        NPCData data = (NPCData) o;
                        idle.setValue(data.getStandingAnimation());
                        walk.setValue(data.getWalkingAnimation());
                        run.setValue(data.getRunAnimation());
                        walk180.setValue(data.getRotate180Animation());
                        walkRight.setValue(data.getRotateRightAnimation());
                        walkLeft.setValue(data.getRotateLeftAnimation());
                        idleRight.setValue(data.getIdleRotateRightAnimation());
                        idleLeft.setValue(data.getIdleRotateLeftAnimation());
                    }

                    npcPopup.setVisible(false);
                }
            }
        });

        if (dataFinder.isDataLoaded(DataFinder.DataType.NPC))
        {
            List<NPCData> dataList = dataFinder.getNpcData();
            List<Object> list = new ArrayList<>(dataList);
            npcTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.NPC, () ->
            {
                List<NPCData> dataList = dataFinder.getNpcData();
                List<Object> list = new ArrayList<>(dataList);
                npcTable.initialize(list);
            });
        }

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 12;
        JLabel itemSearcherLabel = new JLabel("Weapon Presets: ");
        itemSearcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(itemSearcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 12;
        JTextField itemField = new JTextField("");
        itemField.setToolTipText("Search up all items, and double click the name to apply all of its Pose animations");
        itemField.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(itemField, c);

        JPopupMenu itemPopup = new JPopupMenu("Items");
        JScrollPane itemScrollPane = new JScrollPane(itemTable);
        itemPopup.add(itemScrollPane);

        KeyListener itemListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = itemField.getText();
                itemTable.searchAndListEntries(text);
                itemPopup.setVisible(true);
                Point p = itemField.getLocationOnScreen();
                itemPopup.setLocation(new Point((int) p.getX() + itemField.getWidth(), (int) p.getY()));
            }
        };
        itemField.addKeyListener(itemListener);

        itemField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                itemPopup.setVisible(false);
            }
        });

        itemTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = itemTable.getSelectedObject();
                    if (o instanceof ItemData)
                    {
                        ItemData data = (ItemData) o;
                        int itemId = data.getId();

                        boolean foundMatch = false;

                        List<WeaponAnimData> weaponAnimSets = dataFinder.getWeaponAnimData();
                        for (WeaponAnimData weaponAnim : weaponAnimSets)
                        {
                            int[] ids = weaponAnim.getId();
                            if (ids == null || ids.length == 0)
                            {
                                continue;
                            }

                            for (int i : ids)
                            {
                                if (i == itemId)
                                {
                                    idle.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE));
                                    walk.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.WALK));
                                    run.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.RUN));
                                    walk180.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_180));
                                    walkRight.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_RIGHT));
                                    walkLeft.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_LEFT));
                                    idleRight.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE_ROTATE_RIGHT));
                                    idleLeft.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE_ROTATE_LEFT));
                                    foundMatch = true;
                                    break;
                                }
                            }

                            if (foundMatch)
                            {
                                break;
                            }
                        }

                        if (!foundMatch)
                        {
                            idle.setValue(-1);
                            walk.setValue(-1);
                            run.setValue(-1);
                            walk180.setValue(-1);
                            walkRight.setValue(-1);
                            walkLeft.setValue(-1);
                            idleRight.setValue(-1);
                            idleLeft.setValue(-1);
                        }
                    }

                    itemPopup.setVisible(false);
                }
            }
        });

        if (dataFinder.isDataLoaded(DataFinder.DataType.ITEM))
        {
            List<ItemData> dataList = dataFinder.getItemData();
            List<Object> list = new ArrayList<>(dataList);
            itemTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.ITEM, () ->
            {
                List<ItemData> dataList = dataFinder.getItemData();
                List<Object> list = new ArrayList<>(dataList);
                itemTable.initialize(list);
            });
        }

        NPCData player = new NPCData(
                -1,
                "Player",
                new int[0],
                1,
                808,
                819,
                824,
                823,
                823,
                820,
                821,
                822,
                1,
                1,
                new int[0],
                new int[0]);

        c.gridwidth = 2;
        c.gridx = 4;
        c.gridy = 12;
        JButton addPlayer = new JButton("Unarmed");
        addPlayer.setToolTipText("Apply all the default Pose animations for an unarmed player");
        addPlayer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        addPlayer.addActionListener(e ->
        {
            idle.setValue(player.getStandingAnimation());
            walk.setValue(player.getWalkingAnimation());
            run.setValue(player.getRunAnimation());
            walk180.setValue(player.getRotate180Animation());
            walkRight.setValue(player.getRotateRightAnimation());
            walkLeft.setValue(player.getRotateLeftAnimation());
            idleRight.setValue(player.getIdleRotateRightAnimation());
            idleLeft.setValue(player.getIdleRotateLeftAnimation());
        });
        card.add(addPlayer, c);

        randomize.addActionListener(e ->
        {
            Character character = timeSheetPanel.getSelectedCharacter();
            if (character == null)
            {
                return;
            }

            CKObject ckObject = character.getCkObject();

            clientThread.invokeLater(() ->
            {
                Animation[] animations = ckObject.getAnimations();
                int animId;
                Animation activeAnim = animations[0];
                Animation poseAnim = animations[1];

                if (activeAnim == null || activeAnim.getId() == -1)
                {
                    if (poseAnim == null || poseAnim.getId() == -1)
                    {
                        return;
                    }

                    animId = poseAnim.getId();
                }
                else
                {
                    animId = activeAnim.getId();
                }

                Animation animation = client.loadAnimation(animId);
                if (animation == null)
                {
                    return;
                }

                int frames = animation.getNumFrames();
                int randomFrame = random.nextInt(frames);
                startFrame.setValue(randomFrame);
            });
        });

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 13;
        JLabel animSearcherLabel = new JLabel("Animations: ");
        animSearcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(animSearcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 13;
        JTextField animField = new JTextField("");
        animField.setToolTipText("Search up all animations, and double click the name to apply it as the Active animation");
        animField.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(animField, c);

        JPopupMenu animPopup = new JPopupMenu("Animations");
        JScrollPane animScrollPane = new JScrollPane(animTable);
        animPopup.add(animScrollPane);

        KeyListener animListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = animField.getText();
                animTable.searchAndListEntries(text);
                animPopup.setVisible(true);
                Point p = animField.getLocationOnScreen();
                animPopup.setLocation(new Point((int) p.getX() + animField.getWidth(), (int) p.getY()));
            }
        };
        animField.addKeyListener(animListener);

        animField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                animPopup.setVisible(false);
            }
        });

        animTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = animTable.getSelectedObject();
                    if (o instanceof AnimData)
                    {
                        AnimData data = (AnimData) o;
                        manual.setValue(data.getId());

                    }

                    animPopup.setVisible(false);
                }
            }
        });

        if (dataFinder.isDataLoaded(DataFinder.DataType.ANIM))
        {
            List<AnimData> dataList = dataFinder.getAnimData();
            List<Object> list = new ArrayList<>(dataList);
            animTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.ANIM, () ->
            {
                List<AnimData> dataList = dataFinder.getAnimData();
                List<Object> list = new ArrayList<>(dataList);
                animTable.initialize(list);
            });
        }

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupOriCard(JPanel card)
    {
        Dimension spinnerSize = new Dimension(90, 25);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Orientation");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("<html>Setting an Orientation keyframe allows you to take direct control of an Object's orientation" +
                "<br>Otherwise, the Object's orientation is instead based off of the direction of its movement" +
                "<br>Start is the orientation to set at the start of the keyframe, while End determines where the Object will eventually point" +
                "<br>Use Ctrl+[ on a tile to set that orientation, relative to the Object's current tile, as the Start" +
                "<br>Use Ctrl+] on a tile to set that orientation, relative to the Object's current tile, as the End</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel startLabel = new JLabel("Start Orientation: ");
        startLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(startLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner start = oriAttributes.getStart();
        start.setToolTipText("Set the starting orientation that will apply at the beginning of the KeyFrame");
        start.setModel(new SpinnerNumberModel(0, 0, 2048, 1));
        start.setPreferredSize(spinnerSize);
        card.add(start, c);

        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 1;
        JLabel endLabel = new JLabel("End Orientation: ");
        endLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(endLabel, c);

        c.gridx = 3;
        c.gridy = 1;
        JSpinner end = oriAttributes.getEnd();
        end.setToolTipText("Set the ending orientation that the KeyFrame will try to reach");
        end.setModel(new SpinnerNumberModel(0, 0, 2048, 1));
        end.setPreferredSize(spinnerSize);
        card.add(end, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JButton getStart = new JButton("Grab");
        getStart.setToolTipText("Grab the current orientation of the Object, and apply it as the Start");
        getStart.addActionListener(e ->
        {
            Character selectedCharacter = timeSheetPanel.getSelectedCharacter();
            if (selectedCharacter == null)
            {
                return;
            }

            CKObject ckObject = selectedCharacter.getCkObject();
            if (ckObject == null)
            {
                return;
            }

            start.setValue(ckObject.getOrientation());
        });
        card.add(getStart, c);

        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 2;
        JButton getEnd = new JButton("Grab");
        getEnd.setToolTipText("Grab the current Orientation of the Object, and apply it as the End");
        getEnd.addActionListener(e ->
        {
            Character selectedCharacter = timeSheetPanel.getSelectedCharacter();
            if (selectedCharacter == null)
            {
                return;
            }

            CKObject ckObject = selectedCharacter.getCkObject();
            if (ckObject == null)
            {
                return;
            }

            end.setValue(ckObject.getOrientation());
        });
        card.add(getEnd, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel durationLabel = new JLabel("Duration: ");
        durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(durationLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner duration = oriAttributes.getDuration();
        duration.setToolTipText("<html>Set the duration for how long the Object will attempt to point towards its End orientation" +
                "<br>If the Object reaches the End orientation, it will remain in that state until the Duration is over, regardless of its movement trajectory</html>");
        duration.setModel(new SpinnerNumberModel(1.0, 0, TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.1));
        duration.setPreferredSize(spinnerSize);
        card.add(duration, c);

        c.gridwidth = 2;
        c.gridx = 2;
        c.gridy = 3;
        JButton calculate = new JButton("Calculate");
        calculate.setToolTipText("Calculates the exact duration based on the start and end orientation and the current turn rate");
        calculate.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(calculate, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 4;
        JLabel turnRateLabel = new JLabel("Turn Rate: ");
        turnRateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(turnRateLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner turnRate = oriAttributes.getTurnRate();
        turnRate.setToolTipText("Determines the rate at which the Object rotates in JUnits/clientTick");
        turnRate.setModel(new SpinnerNumberModel(OrientationKeyFrame.TURN_RATE, 0, 2048, 1));
        card.add(turnRate, c);

        calculate.addActionListener(e ->
        {
            double turnDuration = calculateOrientationDuration((int) start.getValue(), (int) end.getValue(), (int) turnRate.getValue());
            duration.setValue(turnDuration);

        });

        c.gridx = 5;
        c.gridy = 5;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        JLabel compass = new JLabel(new ImageIcon(COMPASS));
        compass.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(compass, c);
    }

    public static double calculateOrientationDuration(int start, int end, double turnRate)
    {
        int difference = Orientation.subtract(end, start);
        double ticks = (double) difference / turnRate * Constants.CLIENT_TICK_LENGTH / Constants.GAME_TICK_LENGTH;
        int scale = (int) Math.pow(10, 1);
        return Math.abs(Math.ceil(ticks * scale) / scale);
    }

    private void setupSpawnCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Spawn");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set whether the object appears or not");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel spawnLabel = new JLabel("Spawn status: ");
        spawnLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(spawnLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JComboBox<Toggle> manualCheckbox = spawnAttributes.getSpawn();
        manualCheckbox.setToolTipText("Sets whether the Object is spawned or not");
        manualCheckbox.setFocusable(false);
        manualCheckbox.addItem(Toggle.ENABLE);
        manualCheckbox.addItem(Toggle.DISABLE);
        card.add(manualCheckbox, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    public void setupModelCard(JPanel card)
    {
        Dimension spinnerSize = new Dimension(90, 25);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Model");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("<html>Switch between using a 3D model based on the Model Id from the cache," +
                "<br>or a Custom Model that you've grabbed from the environment, found via the Cache Searcher, or created in the Model Anvil</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel modelLabel = new JLabel("Model Type");
        modelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(modelLabel, c);

        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 1;
        JComboBox<ModelToggle> modelOverride = modelAttributes.getModelOverride();
        modelOverride.setToolTipText("Set whether to use a 3D model based on Model Id, or a Custom Model found via this plugin");
        modelOverride.setFocusable(false);
        modelOverride.addItem(ModelToggle.CUSTOM_MODEL);
        modelOverride.addItem(ModelToggle.MODEL_ID);
        card.add(modelOverride, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel customLabel = new JLabel("Custom Model: ");
        customLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(customLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<CustomModel> customComboBox = modelAttributes.getCustomModel();
        customComboBox.setToolTipText("The Custom Model to apply, if Model Type is set to Custom");
        customComboBox.setFocusable(false);
        card.add(customComboBox, c);

        c.gridx = 2;
        c.gridy = 2;
        JButton grab = new JButton("Grab");
        grab.setToolTipText("Grabs the original Custom Model and Radius set to the Object");
        card.add(grab, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel idLabel = new JLabel("Model Id: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner id = modelAttributes.getModelId();
        id.setToolTipText("The Model Id from the cache to apply, if Model Type is set to Model Id");
        id.setValue(-1);
        id.setPreferredSize(spinnerSize);
        card.add(id, c);

        c.gridx = 0;
        c.gridy = 4;
        JLabel radiusLabel = new JLabel("Radius: ");
        radiusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(radiusLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner radius = modelAttributes.getRadius();
        radius.setToolTipText("How far the Model should render vs clip with other tiles around it, measured in 1/128th tiles");
        radius.setValue(60);
        radius.setPreferredSize(spinnerSize);
        card.add(radius, c);

        grab.addActionListener(e ->
        {
            Character selectedCharacter = timeSheetPanel.getSelectedCharacter();
            if (selectedCharacter == null)
            {
                return;
            }

            customComboBox.setSelectedItem(selectedCharacter.getStoredModel());
            radius.setValue((int) selectedCharacter.getRadiusSpinner().getValue());
        });

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupTextCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Text");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the text to display over this Object's head");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel durationLabel = new JLabel("Duration: ");
        durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(durationLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JSpinner duration = textAttributes.getDuration();
        duration.setToolTipText("How long the text should render for");
        duration.setModel(new SpinnerNumberModel(5.0, 0, 1000000, 0.1));
        card.add(duration, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel textLabel = new JLabel("Overhead Text: ");
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(textLabel, c);

        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 2;
        JTextArea text = textAttributes.getText();
        text.setToolTipText("The text to show overhead");
        text.setText("");
        text.setLineWrap(true);
        card.add(text, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupOverheadCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Overhead");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the prayer and/or skull icon to display over this Object's head");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel skullLabel = new JLabel("Enable Skull: ");
        skullLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(skullLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JComboBox<OverheadSprite> toggleSkull = overheadAttributes.getSkullSprite();
        toggleSkull.setToolTipText("Set the skull icon to display overhead");
        toggleSkull.setFocusable(false);
        toggleSkull.addItem(OverheadSprite.NONE);
        toggleSkull.addItem(OverheadSprite.SKULL);
        toggleSkull.addItem(OverheadSprite.SKULL_HIGH_RISK);
        toggleSkull.addItem(OverheadSprite.SKULL_FIGHT_PITS);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_1);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_2);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_3);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_4);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_5);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_1);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_2);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_3);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_4);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_5);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_1);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_2);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_3);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_4);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_5);
        card.add(toggleSkull, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel textLabel = new JLabel("Overhead Icon: ");
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(textLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<OverheadSprite> spriteBox = overheadAttributes.getPrayerSprite();
        spriteBox.setToolTipText("Set the prayer icon to display overhead");
        spriteBox.setFocusable(false);
        spriteBox.addItem(OverheadSprite.NONE);
        spriteBox.addItem(OverheadSprite.PROTECT_MAGIC);
        spriteBox.addItem(OverheadSprite.PROTECT_RANGED);
        spriteBox.addItem(OverheadSprite.PROTECT_MELEE);
        spriteBox.addItem(OverheadSprite.REDEMPTION);
        spriteBox.addItem(OverheadSprite.RETRIBUTION);
        spriteBox.addItem(OverheadSprite.SMITE);
        spriteBox.addItem(OverheadSprite.PROTECT_RANGE_MAGE);
        spriteBox.addItem(OverheadSprite.PROTECT_RANGE_MELEE);
        spriteBox.addItem(OverheadSprite.PROTECT_MAGE_MELEE);
        spriteBox.addItem(OverheadSprite.PROTECT_RANGE_MAGE_MELEE);
        spriteBox.addItem(OverheadSprite.DEFLECT_MAGE);
        spriteBox.addItem(OverheadSprite.DEFLECT_RANGE);
        spriteBox.addItem(OverheadSprite.DEFLECT_MELEE);
        spriteBox.addItem(OverheadSprite.SOUL_SPLIT);
        spriteBox.addItem(OverheadSprite.WRATH);
        card.add(spriteBox, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupHealthCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Healthbar");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the healthbar state for this Object. The amount of damage that will be shown is the Current health relative to the Maximum health");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel durationLabel = new JLabel("Duration: ");
        durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(durationLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JSpinner duration = healthAttributes.getDuration();
        duration.setToolTipText("Set how long the healthbar should appear for");
        duration.setModel(new SpinnerNumberModel(5.0, 0, 1000000, 1));
        card.add(duration, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel healthbarLabel = new JLabel("Healthbar Sprite: ");
        healthbarLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(healthbarLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<HealthbarSprite> healthbarSprite = healthAttributes.getHealthbarSprite();
        healthbarSprite.setToolTipText("Set the sprite for the healthbar to show");
        healthbarSprite.setFocusable(false);
        healthbarSprite.addItem(HealthbarSprite.DEFAULT);
        card.add(healthbarSprite, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel maxHealthLabel = new JLabel("Max Health: ");
        maxHealthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(maxHealthLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 3;
        JSpinner maxHealth = healthAttributes.getMaxHealth();
        maxHealth.setToolTipText("Set the Object's maximum health");
        maxHealth.setModel(new SpinnerNumberModel(99, 0, 99999, 1));
        maxHealth.setValue(99);
        card.add(maxHealth, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 4;
        JLabel currentHealthLabel = new JLabel("Current Health: ");
        currentHealthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(currentHealthLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 4;
        JSpinner currentHealth = healthAttributes.getCurrentHealth();
        currentHealth.setToolTipText("Set the Object's current health remaining");
        currentHealth.setModel(new SpinnerNumberModel(99, 0, 99999, 1));
        currentHealth.setValue(99);
        card.add(currentHealth, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupSpotAnimCard(JPanel card, KeyFrameType spotAnimType)
    {
        SpotAnimAttributes spAttributes;
        if (spotAnimType == KeyFrameType.SPOTANIM)
        {
            spAttributes = spotAnimAttributes;
        }
        else
        {
            spAttributes = spotAnim2Attributes;
        }

        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("SpotAnim");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the SpotAnim (like spell effects) to play on the Object");
        manualTitlePanel.add(manualTitleHelp);

        c.weightx = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel idLabel = new JLabel("SpotAnim: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JSpinner id = spAttributes.getSpotAnimId();
        id.setValue(-1);
        card.add(id, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel loop1Label = new JLabel("Loop: ");
        loop1Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(loop1Label, c);

        c.gridx = 1;
        c.gridy = 2;
        JComboBox<Toggle> loop = spAttributes.getLoop();
        loop.setToolTipText("Set whether the SpotAnim animation should loop");
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        card.add(loop, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel heightLabel = new JLabel("Height");
        heightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(heightLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner height = spAttributes.getHeight();
        height.setToolTipText("Sets the height at which the SpotAnim spawns");
        height.setModel(new SpinnerNumberModel(92, 0, 9999, 1));
        card.add(height, c);

        c.gridx = 0;
        c.gridy = 4;
        JLabel searcherLabel = new JLabel("SpotAnims: ");
        searcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(searcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 4;
        JTextField spotanimField = new JTextField("");
        spotanimField.setToolTipText("Find all SpotAnims from the cache, and double click the name to apply its Id");
        spotanimField.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(spotanimField, c);

        KeyListener listener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = spotanimField.getText();
                spotanimTable.searchAndListEntries(text);
                spotanimPopup.setVisible(true);
                Point p = spotanimField.getLocationOnScreen();
                spotanimPopup.setLocation(new Point((int) p.getX() + spotanimField.getWidth(), (int) p.getY()));
            }
        };
        spotanimField.addKeyListener(listener);

        spotanimField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                spotanimPopup.setVisible(false);
            }
        });

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 5;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 3;
        c.gridy = 6;
        JPanel duplicatePanel = new JPanel();
        duplicatePanel.setLayout(new GridLayout(0, 1, 2, 2));
        card.add(duplicatePanel, c);
        duplicatePanel.add(new JLabel("Duplicate To:"));

        if (spotAnimType == KeyFrameType.SPOTANIM)
        {
            JLabel emptySpotAnim = new JLabel("Spotanim 1");
            emptySpotAnim.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptySpotAnim);
        }
        else
        {
            JButton type1 = new JButton("SpotAnim 1");
            type1.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type1);
            type1.addActionListener(e -> timeSheetPanel.duplicateSpotanimKeyFrame(spotAnimType, KeyFrameType.SPOTANIM));
        }

        if (spotAnimType == KeyFrameType.SPOTANIM2)
        {
            JLabel emptySpotAnim = new JLabel("Spotanim 2");
            emptySpotAnim.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptySpotAnim);
        }
        else
        {
            JButton type2 = new JButton("SpotAnim 2");
            type2.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type2);
            type2.addActionListener(e -> timeSheetPanel.duplicateSpotanimKeyFrame(spotAnimType, KeyFrameType.SPOTANIM2));
        }
    }

    private void setupSpotAnimFinder()
    {
        spotanimTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);

                KeyFrameType spotAnimType = selectedKeyFramePage;
                if (selectedKeyFramePage != KeyFrameType.SPOTANIM && selectedKeyFramePage != KeyFrameType.SPOTANIM2)
                {
                    return;
                }

                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = spotanimTable.getSelectedObject();
                    if (o instanceof SpotanimData)
                    {
                        SpotanimData data = (SpotanimData) o;
                        JSpinner id;
                        if (spotAnimType == KeyFrameType.SPOTANIM)
                        {
                            id = spotAnimAttributes.getSpotAnimId();
                        }
                        else
                        {
                            id = spotAnim2Attributes.getSpotAnimId();
                        }

                        id.setValue(data.getId());
                    }

                    spotanimPopup.setVisible(false);
                }
            }
        });

        if (dataFinder.isDataLoaded(DataFinder.DataType.SPOTANIM))
        {
            List<SpotanimData> dataList = dataFinder.getSpotanimData();
            List<Object> list = new ArrayList<>(dataList);
            spotanimTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.SPOTANIM, () ->
            {
                List<SpotanimData> dataList = dataFinder.getSpotanimData();
                List<Object> list = new ArrayList<>(dataList);
                spotanimTable.initialize(list);
            });
        }

        JScrollPane scrollPane = new JScrollPane(spotanimTable);
        spotanimPopup.add(scrollPane);
    }

    private void setupHitsplatCard(JPanel card, KeyFrameType hitsplatType)
    {
        HitsplatAttributes attributes;
        String name;

        switch (hitsplatType)
        {
            default:
            case HITSPLAT_1:
                attributes = hitsplat1Attributes;
                name = HITSPLAT_1_CARD;
                break;
            case HITSPLAT_2:
                attributes = hitsplat2Attributes;
                name = HITSPLAT_2_CARD;
                break;
            case HITSPLAT_3:
                attributes = hitsplat3Attributes;
                name = HITSPLAT_3_CARD;
                break;
            case HITSPLAT_4:
                attributes = hitsplat4Attributes;
                name = HITSPLAT_4_CARD;
        }

        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel(name);
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the hitsplat sprite and damage to overlay on the Object");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel durationLabel = new JLabel("Duration: ");
        durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(durationLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner duration = attributes.getDuration();
        duration.setToolTipText("Set the duration, in game ticks, for how long the Hitsplat lasts. -1 sets it to default value, which is 5/3 ticks");
        duration.setModel(new SpinnerNumberModel(-1, -1, 1000000, 1));
        card.add(duration, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel spriteLabel = new JLabel("Sprite: ");
        spriteLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(spriteLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JComboBox<HitsplatSprite> sprite = attributes.getSprite();
        sprite.setToolTipText("Set the Hitsplat sprite to display");
        sprite.setFocusable(false);
        sprite.addItem(HitsplatSprite.BLOCK);
        sprite.addItem(HitsplatSprite.DAMAGE);
        sprite.addItem(HitsplatSprite.POISON);
        sprite.addItem(HitsplatSprite.VENOM);
        sprite.addItem(HitsplatSprite.HEAL);
        sprite.addItem(HitsplatSprite.SHIELD);
        sprite.addItem(HitsplatSprite.DISEASE);
        sprite.addItem(HitsplatSprite.FREEZE);
        sprite.addItem(HitsplatSprite.NO_KILL_CREDIT);
        sprite.addItem(HitsplatSprite.ARMOUR);
        sprite.addItem(HitsplatSprite.BURN);
        sprite.addItem(HitsplatSprite.BLEED);
        sprite.addItem(HitsplatSprite.CORRUPTION);
        sprite.addItem(HitsplatSprite.DOOM);
        sprite.addItem(HitsplatSprite.POISE);
        sprite.addItem(HitsplatSprite.PRAYER_DRAIN);
        sprite.addItem(HitsplatSprite.SANITY_DRAIN);
        sprite.addItem(HitsplatSprite.SANITY_RESTORE);
        sprite.addItem(HitsplatSprite.CHARGE_UP);
        sprite.addItem(HitsplatSprite.CHARGE_DOWN);
        card.add(sprite, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel variantLabel = new JLabel("Variant: ");
        variantLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(variantLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JComboBox<HitsplatVariant> variant = attributes.getVariant();
        variant.setToolTipText("Set the Hitsplat variant to display");
        variant.setFocusable(false);
        variant.addItem(HitsplatVariant.NORMAL);
        variant.addItem(HitsplatVariant.MAX);
        variant.addItem(HitsplatVariant.OTHER);
        card.add(variant, c);

        c.gridx = 0;
        c.gridy = 4;
        JLabel damageLabel = new JLabel("Damage: ");
        damageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(damageLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner damage = attributes.getDamage();
        damage.setToolTipText("Set the damage value to show on the hisplat. -1 will not render any damage number");
        damage.setModel(new SpinnerNumberModel(0, -1, 999, 1));
        card.add(damage, c);

        c.gridx = 2;
        c.gridy = 4;
        JButton keyFrameHealth = new JButton("Quick KeyFrame Hitsplat/Health");
        keyFrameHealth.setToolTipText("Creates the Hitsplat KeyFrame and an appropriate Health KeyFrame as if this damage were applied");
        keyFrameHealth.setBackground(ColorScheme.DARK_GRAY_COLOR);
        keyFrameHealth.addActionListener(e -> timeSheetPanel.initializeHealthKeyFrame(hitsplatType));
        card.add(keyFrameHealth, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 3;
        c.gridy = 5;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 5;
        c.gridy = 7;
        JPanel duplicatePanel = new JPanel();
        duplicatePanel.setLayout(new GridLayout(0, 1, 2, 2));
        card.add(duplicatePanel, c);
        duplicatePanel.add(new JLabel("Duplicate To:"));

        if (hitsplatType == KeyFrameType.HITSPLAT_1)
        {
            JLabel emptyHitsplat = new JLabel("Hitsplat 1");
            emptyHitsplat.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptyHitsplat);
        }
        else
        {
            JButton type1 = new JButton("Hitsplat 1");
            type1.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type1);
            type1.addActionListener(e -> timeSheetPanel.duplicateHitsplatKeyFrame(hitsplatType, KeyFrameType.HITSPLAT_1));
        }

        if (hitsplatType == KeyFrameType.HITSPLAT_2)
        {
            JLabel emptyHitsplat = new JLabel("Hitsplat 2");
            emptyHitsplat.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptyHitsplat);
        }
        else
        {
            JButton type2 = new JButton("Hitsplat 2");
            type2.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type2);
            type2.addActionListener(e -> timeSheetPanel.duplicateHitsplatKeyFrame(hitsplatType, KeyFrameType.HITSPLAT_2));
        }

        if (hitsplatType == KeyFrameType.HITSPLAT_3)
        {
            JLabel emptyHitsplat = new JLabel("Hitsplat 3");
            emptyHitsplat.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptyHitsplat);
        }
        else
        {
            JButton type3 = new JButton("Hitsplat 3");
            type3.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type3);
            type3.addActionListener(e -> timeSheetPanel.duplicateHitsplatKeyFrame(hitsplatType, KeyFrameType.HITSPLAT_3));
        }

        if (hitsplatType == KeyFrameType.HITSPLAT_4)
        {
            JLabel emptyHitsplat = new JLabel("Hitsplat 4");
            emptyHitsplat.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptyHitsplat);
        }
        else
        {
            JButton type4 = new JButton("Hitsplat 4");
            type4.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type4);
            type4.addActionListener(e -> timeSheetPanel.duplicateHitsplatKeyFrame(hitsplatType, KeyFrameType.HITSPLAT_4));
        }
    }

    public void switchCards(String cardName)
    {
        KeyFrameType type;
        switch (cardName)
        {
            default:
            case MOVE_CARD:
                type = KeyFrameType.MOVEMENT;
                break;
            case ANIM_CARD:
                type = KeyFrameType.ANIMATION;
                break;
            case ORI_CARD:
                type = KeyFrameType.ORIENTATION;
                break;
            case SPAWN_CARD:
                type = KeyFrameType.SPAWN;
                break;
            case MODEL_CARD:
                type = KeyFrameType.MODEL;
                break;
            case TEXT_CARD:
                type = KeyFrameType.TEXT;
                break;
            case OVER_CARD:
                type = KeyFrameType.OVERHEAD;
                break;
            case HEALTH_CARD:
                type = KeyFrameType.HEALTH;
                break;
            case SPOTANIM_CARD:
                type = KeyFrameType.SPOTANIM;
                break;
            case SPOTANIM2_CARD:
                type = KeyFrameType.SPOTANIM2;
                break;
            case HITSPLAT_1_CARD:
                type = KeyFrameType.HITSPLAT_1;
                break;
            case HITSPLAT_2_CARD:
                type = KeyFrameType.HITSPLAT_2;
                break;
            case HITSPLAT_3_CARD:
                type = KeyFrameType.HITSPLAT_3;
                break;
            case HITSPLAT_4_CARD:
                type = KeyFrameType.HITSPLAT_4;
        }

        switchCards(type);
    }

    public void switchCards(KeyFrameType type)
    {
        selectedKeyFramePage = type;
        String cardName = selectedKeyFramePage.getName();
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, cardName);
        cardLabel.setText(cardName);

        JLabel[] labels = timeSheetPanel.getLabels();
        JLabel selectedLabel;

        selectedLabel = labels[KeyFrameType.getIndex(selectedKeyFramePage) + 1];
        for (int f = 0; f < labels.length; f++)
        {
            JLabel label = labels[f];
            if (label == selectedLabel)
            {
                timeSheetPanel.getAttributeSheet().setSelectedIndex(f);
                label.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            }
            else
            {
                label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        }

        Character character = timeSheetPanel.getSelectedCharacter();
        double currentTick = timeSheetPanel.getCurrentTime();
        if (character == null)
        {
            setKeyFramedIcon(false);
            resetAttributes(null, currentTick);
            return;
        }

        KeyFrame keyFrame = character.findKeyFrame(selectedKeyFramePage, currentTick);
        setKeyFramedIcon(keyFrame != null);
        resetAttributes(character, currentTick);
    }

    public void setSelectedCharacter(Character character)
    {
        double tick = timeSheetPanel.getCurrentTime();
        updateObjectLabel(character);

        if (character == null)
        {
            setKeyFramedIcon(false);
            resetAttributes(null, tick);
            return;
        }

        KeyFrame keyFrame = character.findKeyFrame(selectedKeyFramePage, tick);
        setKeyFramedIcon(keyFrame != null);
        resetAttributes(character, tick);
    }

    public void updateObjectLabel(Character character)
    {
        if (character == null)
        {
            objectLabel.setForeground(Color.WHITE);
            objectLabel.setText(NO_OBJECT_SELECTED);
            return;
        }

        objectLabel.setForeground(ColorScheme.BRAND_ORANGE);
        StringBuilder name = new StringBuilder(character.getName());

        FontMetrics metrics = objectLabel.getFontMetrics(attributeFont);
        int maxWidth = 275;
        while (metrics.stringWidth(name.toString()) > maxWidth)
        {
            name = name.deleteCharAt(name.length() - 1);
        }

        objectLabel.setText(name.toString());
    }

    private void setupKeyListeners()
    {
        for (JComponent c : movementAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.MOVEMENT);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.MOVEMENT);
        }

        for (JComponent c : animAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.ANIMATION);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.ANIMATION);
        }

        for (JComponent c : oriAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.ORIENTATION);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.ORIENTATION);
        }

        for (JComponent c : spawnAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.SPAWN);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.SPAWN);
        }

        for (JComponent c : modelAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.MODEL);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.MODEL);
        }

        for (JComponent c : textAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.TEXT);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.TEXT);
        }

        for (JComponent c : overheadAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.OVERHEAD);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.OVERHEAD);
        }

        for (JComponent c : healthAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HEALTH);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HEALTH);
        }

        for (JComponent c : spotAnimAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.SPOTANIM);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.SPOTANIM);
        }

        for (JComponent c : spotAnim2Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.SPOTANIM2);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.SPOTANIM2);
        }

        for (JComponent c : hitsplat1Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HITSPLAT_1);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HITSPLAT_1);
        }


        for (JComponent c : hitsplat2Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HITSPLAT_2);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HITSPLAT_2);
        }


        for (JComponent c : hitsplat3Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HITSPLAT_3);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HITSPLAT_3);
        }


        for (JComponent c : hitsplat4Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HITSPLAT_4);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HITSPLAT_4);
        }
    }

    private void addMouseFocusListener(JComponent component)
    {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                component.requestFocusInWindow();
            }
        });
    }

    private void addHoverListeners(Component component, KeyFrameType type)
    {
        component.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                super.mouseEntered(e);
                hoveredComponent = component;
                hoveredKeyFrameType = type;
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                super.mouseExited(e);
                hoveredComponent = null;
                hoveredKeyFrameType = KeyFrameType.NULL;
            }
        });
    }

    private void addHoverListenersWithChildren(JComponent component, KeyFrameType type)
    {
        ArrayList<Component> components = new ArrayList<>();
        getAllComponentChildren(components, component);
        for (Component c : components)
        {
            addHoverListeners(c, type);
        }
    }

    private void getAllComponentChildren(ArrayList<Component> components, JComponent component)
    {
        for (Component c : component.getComponents())
        {
            components.add(c);
            getAllComponentChildren(components, (JComponent) c);
        }
    }

    public void resetAttributes(Character character, double tick)
    {
        if (character == null)
        {
            setAttributesEmpty(true);
            return;
        }

        setKeyFramedIcon(character.findKeyFrame(selectedKeyFramePage, tick) != null);
        KeyFrame keyFrame = character.findPreviousKeyFrame(selectedKeyFramePage, tick, true);

        if (keyFrame == null)
        {
            keyFrame = character.findNextKeyFrame(selectedKeyFramePage, tick);

            if (keyFrame == null)
            {
                setAttributesEmpty(true);
                return;
            }
        }

        KeyFrameState keyFrameState = tick == keyFrame.getTick() ? KeyFrameState.ON_KEYFRAME : KeyFrameState.OFF_KEYFRAME;

        switch (selectedKeyFramePage)
        {
            default:
            case MOVEMENT:
                movementAttributes.setAttributes(keyFrame);
                movementAttributes.setBackgroundColours(keyFrameState);
                break;
            case ANIMATION:
                animAttributes.setAttributes(keyFrame);
                animAttributes.setBackgroundColours(keyFrameState);
                break;
            case ORIENTATION:
                oriAttributes.setAttributes(keyFrame);
                oriAttributes.setBackgroundColours(keyFrameState);
                break;
            case SPAWN:
                spawnAttributes.setAttributes(keyFrame);
                spawnAttributes.setBackgroundColours(keyFrameState);
                break;
            case MODEL:
                modelAttributes.setAttributes(keyFrame);
                modelAttributes.setBackgroundColours(keyFrameState);
                break;
            case TEXT:
                textAttributes.setAttributes(keyFrame);
                textAttributes.setBackgroundColours(keyFrameState);
                break;
            case OVERHEAD:
                overheadAttributes.setAttributes(keyFrame);
                overheadAttributes.setBackgroundColours(keyFrameState);
                break;
            case HEALTH:
                healthAttributes.setAttributes(keyFrame);
                healthAttributes.setBackgroundColours(keyFrameState);
                break;
            case SPOTANIM:
                spotAnimAttributes.setAttributes(keyFrame);
                spotAnimAttributes.setBackgroundColours(keyFrameState);
                break;
            case SPOTANIM2:
                spotAnim2Attributes.setAttributes(keyFrame);
                spotAnim2Attributes.setBackgroundColours(keyFrameState);
                break;
            case HITSPLAT_1:
                hitsplat1Attributes.setAttributes(keyFrame);
                hitsplat1Attributes.setBackgroundColours(keyFrameState);
                break;
            case HITSPLAT_2:
                hitsplat2Attributes.setAttributes(keyFrame);
                hitsplat2Attributes.setBackgroundColours(keyFrameState);
                break;
            case HITSPLAT_3:
                hitsplat3Attributes.setAttributes(keyFrame);
                hitsplat3Attributes.setBackgroundColours(keyFrameState);
                break;
            case HITSPLAT_4:
                hitsplat4Attributes.setAttributes(keyFrame);
                hitsplat4Attributes.setBackgroundColours(keyFrameState);
        }
    }

    public void setAttributesEmpty(boolean resetBackground)
    {
        switch (selectedKeyFramePage)
        {
            default:
            case MOVEMENT:
                movementAttributes.resetAttributes(resetBackground);
                break;
            case ANIMATION:
                animAttributes.resetAttributes(resetBackground);
                break;
            case ORIENTATION:
                oriAttributes.resetAttributes(resetBackground);
                break;
            case SPAWN:
                spawnAttributes.resetAttributes(resetBackground);
                break;
            case MODEL:
                modelAttributes.resetAttributes(resetBackground);
                break;
            case TEXT:
                textAttributes.resetAttributes(resetBackground);
                break;
            case OVERHEAD:
                overheadAttributes.resetAttributes(resetBackground);
                break;
            case HEALTH:
                healthAttributes.resetAttributes(resetBackground);
                break;
            case SPOTANIM:
                spotAnimAttributes.resetAttributes(resetBackground);
                break;
            case SPOTANIM2:
                spotAnim2Attributes.resetAttributes(resetBackground);
                break;
            case HITSPLAT_1:
                hitsplat1Attributes.resetAttributes(resetBackground);
                break;
            case HITSPLAT_2:
                hitsplat2Attributes.resetAttributes(resetBackground);
                break;
            case HITSPLAT_3:
                hitsplat3Attributes.resetAttributes(resetBackground);
                break;
            case HITSPLAT_4:
                hitsplat4Attributes.resetAttributes(resetBackground);
        }
    }

    public void setKeyFramedIcon(boolean isKeyFramed)
    {
        if (isKeyFramed)
        {
            keyFramed.setIcon(keyframeImage);
            return;
        }

        keyFramed.setIcon(keyframeEmptyImage);
    }
}