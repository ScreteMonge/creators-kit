package com.creatorskit.swing.timesheet;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.models.CustomModel;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.DataFinder.DataType;
import com.creatorskit.models.datatypes.NPCData;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.programming.orientation.OrientationGoal;
import com.creatorskit.swing.AutoCompletion;
import com.creatorskit.swing.timesheet.attributes.*;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.settings.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Animation;
import net.runelite.api.Client;
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
import java.util.Random;

@Getter
@Setter
public class AttributePanel extends JPanel
{
    private Client client;
    private ClientThread clientThread;
    private TimeSheetPanel timeSheetPanel;
    private DataFinder dataFinder;

    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");
    private final BufferedImage COMPASS = ImageUtil.loadImageResource(getClass(), "/Orientation_compass.png");
    private final Icon keyframeImage = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Keyframe.png"));
    private final Icon keyframeEmptyImage = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Keyframe_Empty.png"));

    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel cardPanel = new JPanel();
    private final JLabel objectLabel = new JLabel("Pick an Object");
    private final JLabel cardLabel = new JLabel("");
    private final JButton keyFramed = new JButton();

    private JComboBox<NPCData> searcher = new JComboBox<>();

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

    private final Random random = new Random();

    @Inject
    public AttributePanel(Client client, ClientThread clientThread, TimeSheetPanel timeSheetPanel, DataFinder dataFinder)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.timeSheetPanel = timeSheetPanel;
        this.dataFinder = dataFinder;

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        objectLabel.setFont(FontManager.getRunescapeBoldFont());
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
        keyFramed.setIcon(keyframeEmptyImage);
        keyFramed.setPreferredSize(new Dimension(18, 18));
        keyFramed.setBackground(ColorScheme.DARK_GRAY_COLOR);
        keyFramed.addActionListener(e -> timeSheetPanel.onKeyFrameIconPressedEvent());
        add(keyFramed, c);

        c.gridwidth = 4;
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

        setupMoveCard(moveCard);
        setupAnimCard(animCard);
        setupOriCard(oriCard);
        setupSpawnCard(spawnCard);
        setupModelCard(modelCard);
        setupTextCard(textCard);
        setupOverheadCard(overCard);
        setupHealthCard(healthCard);
        setupSpotAnimCard(spotanimCard, KeyFrameType.SPOTANIM);
        setupSpotAnimCard(spotanim2Card, KeyFrameType.SPOTANIM2);

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
                        (CustomModel) modelAttributes.getCustomModel().getSelectedItem()
                );
            case TEXT:
                return new TextKeyFrame(
                        tick,
                        (int) textAttributes.getDuration().getValue(),
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
                        healthAttributes.getEnableBox().getSelectedItem() == Toggle.ENABLE,
                        (HealthbarSprite) healthAttributes.getHealthbarSprite().getSelectedItem(),
                        (int) healthAttributes.getMaxHealth().getValue(),
                        (int) healthAttributes.getCurrentHealth().getValue(),
                        (HitsplatSprite) healthAttributes.getHitsplat1Sprite().getSelectedItem(),
                        (HitsplatSprite) healthAttributes.getHitsplat2Sprite().getSelectedItem(),
                        (HitsplatSprite) healthAttributes.getHitsplat3Sprite().getSelectedItem(),
                        (HitsplatSprite) healthAttributes.getHitsplat4Sprite().getSelectedItem(),
                        (int) healthAttributes.getHitsplat1().getValue(),
                        (int) healthAttributes.getHitsplat2().getValue(),
                        (int) healthAttributes.getHitsplat3().getValue(),
                        (int) healthAttributes.getHitsplat4().getValue()
                );
            case SPOTANIM:
                return new SpotAnimKeyFrame(
                        tick,
                        KeyFrameType.SPOTANIM,
                        (int) spotAnimAttributes.getSpotAnimId().getValue(),
                        spotAnimAttributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        (int) spotAnimAttributes.getHeight().getValue()
                );
            case SPOTANIM2:
                return new SpotAnimKeyFrame(
                        tick,
                        KeyFrameType.SPOTANIM2,
                        (int) spotAnim2Attributes.getSpotAnimId().getValue(),
                        spotAnim2Attributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        (int) spotAnim2Attributes.getHeight().getValue()
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
        manualTitleHelp.setToolTipText("Set how the Object moves");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel loopLabel = new JLabel("Loop: ");
        loopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(loopLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JComboBox<Toggle> loop = movementAttributes.getLoop();
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        loop.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(loop, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel speedLabel = new JLabel("Speed: ");
        speedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(speedLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JSpinner speed = movementAttributes.getSpeed();
        speed.setModel(new SpinnerNumberModel(1, 0.5, 10, 0.5));
        card.add(speed, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel turnRateLabel = new JLabel("Turn Rate: ");
        turnRateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(turnRateLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner turnRate = movementAttributes.getTurnRate();
        turnRate.setToolTipText("Determines the rate at which the Object rotates during movement. -1 sets it to default value of 256 / 7.5");
        turnRate.setModel(new SpinnerNumberModel(-1, -1, 2048, 1));
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
        help.setToolTipText("<html>Movement Animations dynamically update your Object based on its current movement trajectory" +
                "<br>For example: an Object that isn't moving will use the given Idle animation; an Object taking a 90 degree right turn will use Walk Right animation." +
                "<br>Active Animations will instead override the current Movement Animation, playing regardless of the Object's movement trajectory</html>");
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
        startFrame.setModel(new SpinnerNumberModel(0, 0, 99999, 1));
        startFrame.setPreferredSize(spinnerSize);
        card.add(startFrame, c);

        c.gridx = 2;
        c.gridy = 1;
        JButton randomize = new JButton("Random");
        card.add(randomize, c);

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
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        card.add(loop, c);

        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 5;
        JLabel smartTitle = new JLabel("Movement Animations");
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
        idleLeft.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idleLeft.setPreferredSize(spinnerSize);
        card.add(idleLeft, c);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 11;
        JLabel searcherLabel = new JLabel("NPC Animation Presets: ");
        searcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(searcherLabel, c);

        c.gridwidth = 4;
        c.gridx = 2;
        c.gridy = 11;
        AutoCompletion.enable(searcher);
        searcher.setPreferredSize(new Dimension(270, 25));

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
        searcher.addItem(player);
        if (dataFinder.isDataLoaded(DataType.NPC))
        {
            dataFinder.getNpcData().forEach(searcher::addItem);
        }
        else
        {
            dataFinder.addLoadCallback(DataType.NPC, () -> {
                SwingUtilities.invokeLater(() -> dataFinder.getNpcData().forEach(searcher::addItem));
            });
        }
        searcher.setPreferredSize(new Dimension(270, 25));
        card.add(searcher, c);

        c.gridwidth = 1;
        c.gridx = 6;
        c.gridy = 11;
        JButton searchApply = new JButton("Apply");
        searchApply.addActionListener(e ->
        {
            NPCData data = (NPCData) searcher.getSelectedItem();
            if (data == null)
            {
                return;
            }

            idle.setValue(data.getStandingAnimation());
            walk.setValue(data.getWalkingAnimation());
            run.setValue(data.getRunAnimation());
            walk180.setValue(data.getRotate180Animation());
            walkRight.setValue(data.getRotateRightAnimation());
            walkLeft.setValue(data.getRotateLeftAnimation());
            idleRight.setValue(data.getIdleRotateRightAnimation());
            idleLeft.setValue(data.getIdleRotateLeftAnimation());
        });
        card.add(searchApply, c);

        randomize.addActionListener(e ->
        {
            int animId = (int) manual.getValue();
            if (animId == -1)
            {
                return;
            }

            clientThread.invokeLater(() ->
            {
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
                "<br>Otherwise, the Object's orientation is instead based off of the direction of its movement." +
                "<br>Start is the orientaiton to set at the start of the keyframe, while End determines where the Object will eventually point</html>");
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
        end.setModel(new SpinnerNumberModel(0, 0, 2048, 1));
        end.setPreferredSize(spinnerSize);
        card.add(end, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JButton getStart = new JButton("Grab");
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
        duration.setModel(new SpinnerNumberModel(2.0, 0, TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.1));
        duration.setPreferredSize(spinnerSize);
        card.add(duration, c);

        c.gridx = 0;
        c.gridy = 4;
        JLabel turnRateLabel = new JLabel("Turn Rate: ");
        turnRateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(turnRateLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner turnRate = oriAttributes.getTurnRate();
        turnRate.setToolTipText("Determines the rate at which the Object rotates. -1 sets it to default value of 256 / 7.5");
        turnRate.setModel(new SpinnerNumberModel(-1, -1, 2048, 1));
        card.add(turnRate, c);

        c.gridx = 5;
        c.gridy = 5;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        JLabel compass = new JLabel(new ImageIcon(COMPASS));
        compass.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(compass, c);
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

        JLabel manualTitle = new JLabel("Set Spawn");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set whether the object appears or not");
        manualTitlePanel.add(manualTitleHelp);

        c.gridx = 0;
        c.gridy = 1;
        JComboBox<Toggle> manualCheckbox = spawnAttributes.getSpawn();
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
                "<br>or a Custom Model that you've grabbed from the environment or created in the Model Anvil</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 1;
        JComboBox<ModelToggle> modelOverride = modelAttributes.getModelOverride();
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
        customComboBox.setFocusable(false);
        card.add(customComboBox, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel idLabel = new JLabel("Model Id: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner id = modelAttributes.getModelId();
        id.setValue(-1);
        id.setPreferredSize(spinnerSize);
        card.add(id, c);

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

        JLabel manualTitle = new JLabel("Overhead Text");
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
        duration.setModel(new SpinnerNumberModel(5, 0, 1000000, 1));
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

        JLabel manualTitle = new JLabel("Overhead Prayer");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the prayer icon to display over this Object's head");
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
        toggleSkull.setFocusable(false);
        toggleSkull.addItem(OverheadSprite.NONE);
        toggleSkull.addItem(OverheadSprite.SKULL);
        toggleSkull.addItem(OverheadSprite.SKULL_HIGH_RISK);
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
        manualTitleHelp.setToolTipText("Set the healthbar and hitsplats for this Object");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JComboBox<Toggle> toggleComboBox = healthAttributes.getEnableBox();
        toggleComboBox.setFocusable(false);
        toggleComboBox.addItem(Toggle.ENABLE);
        toggleComboBox.addItem(Toggle.DISABLE);
        card.add(toggleComboBox, c);

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
        currentHealth.setModel(new SpinnerNumberModel(99, 0, 99999, 1));
        currentHealth.setValue(99);
        card.add(currentHealth, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 5;
        JLabel hitsplatTitle = new JLabel("Hitsplats");
        hitsplatTitle.setHorizontalAlignment(SwingConstants.LEFT);
        hitsplatTitle.setFont(FontManager.getRunescapeBoldFont());
        card.add(hitsplatTitle, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 6;
        JLabel hitsplat1TypeLabel = new JLabel("Hitsplat 1 Icon: ");
        hitsplat1TypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplat1TypeLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 6;
        JComboBox<HitsplatSprite> hitsplat1Type = healthAttributes.getHitsplat1Sprite();
        hitsplat1Type.setFocusable(false);
        hitsplat1Type.addItem(HitsplatSprite.NONE);
        hitsplat1Type.addItem(HitsplatSprite.BLOCK);
        hitsplat1Type.addItem(HitsplatSprite.DAMAGE);
        hitsplat1Type.addItem(HitsplatSprite.POISON);
        hitsplat1Type.addItem(HitsplatSprite.VENOM);
        hitsplat1Type.addItem(HitsplatSprite.HEAL);
        hitsplat1Type.addItem(HitsplatSprite.DISEASE);
        card.add(hitsplat1Type, c);

        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 6;
        JLabel hitsplat1Label = new JLabel(" Damage: ");
        hitsplat1Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplat1Label, c);

        c.gridwidth = 1;
        c.gridx = 4;
        c.gridy = 6;
        JSpinner hitsplat1 = healthAttributes.getHitsplat1();
        hitsplat1.setModel(new SpinnerNumberModel(1, 0, 999, 1));
        card.add(hitsplat1, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 7;
        JLabel hitsplat2TypeLabel = new JLabel("Hitsplat 2 Icon: ");
        hitsplat2TypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplat2TypeLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 7;
        JComboBox<HitsplatSprite> hitsplat2Type = healthAttributes.getHitsplat2Sprite();
        hitsplat2Type.setFocusable(false);
        hitsplat2Type.addItem(HitsplatSprite.NONE);
        hitsplat2Type.addItem(HitsplatSprite.BLOCK);
        hitsplat2Type.addItem(HitsplatSprite.DAMAGE);
        hitsplat2Type.addItem(HitsplatSprite.POISON);
        hitsplat2Type.addItem(HitsplatSprite.VENOM);
        hitsplat2Type.addItem(HitsplatSprite.HEAL);
        hitsplat2Type.addItem(HitsplatSprite.DISEASE);
        card.add(hitsplat2Type, c);

        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 7;
        JLabel hitsplat2Label = new JLabel(" Damage: ");
        hitsplat2Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplat2Label, c);

        c.gridwidth = 1;
        c.gridx = 4;
        c.gridy = 7;
        JSpinner hitsplat2 = healthAttributes.getHitsplat2();
        hitsplat2.setModel(new SpinnerNumberModel(1, 0, 999, 1));
        card.add(hitsplat2, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 8;
        JLabel hitsplat3TypeLabel = new JLabel("Hitsplat 3 Icon: ");
        hitsplat3TypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplat3TypeLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 8;
        JComboBox<HitsplatSprite> hitsplat3Type = healthAttributes.getHitsplat3Sprite();
        hitsplat3Type.setFocusable(false);
        hitsplat3Type.addItem(HitsplatSprite.NONE);
        hitsplat3Type.addItem(HitsplatSprite.BLOCK);
        hitsplat3Type.addItem(HitsplatSprite.DAMAGE);
        hitsplat3Type.addItem(HitsplatSprite.POISON);
        hitsplat3Type.addItem(HitsplatSprite.VENOM);
        hitsplat3Type.addItem(HitsplatSprite.HEAL);
        hitsplat3Type.addItem(HitsplatSprite.DISEASE);
        card.add(hitsplat3Type, c);

        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 8;
        JLabel hitsplat3Label = new JLabel(" Damage: ");
        hitsplat3Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplat3Label, c);

        c.gridwidth = 1;
        c.gridx = 4;
        c.gridy = 8;
        JSpinner hitsplat3 = healthAttributes.getHitsplat3();
        hitsplat3.setModel(new SpinnerNumberModel(1, 0, 999, 1));
        card.add(hitsplat3, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 9;
        JLabel hitsplat4TypeLabel = new JLabel("Hitsplat 4 Icon: ");
        hitsplat4TypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplat4TypeLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 9;
        JComboBox<HitsplatSprite> hitsplat4Type = healthAttributes.getHitsplat4Sprite();
        hitsplat4Type.setFocusable(false);
        hitsplat4Type.addItem(HitsplatSprite.NONE);
        hitsplat4Type.addItem(HitsplatSprite.BLOCK);
        hitsplat4Type.addItem(HitsplatSprite.DAMAGE);
        hitsplat4Type.addItem(HitsplatSprite.POISON);
        hitsplat4Type.addItem(HitsplatSprite.VENOM);
        hitsplat4Type.addItem(HitsplatSprite.HEAL);
        hitsplat4Type.addItem(HitsplatSprite.DISEASE);
        card.add(hitsplat4Type, c);

        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 9;
        JLabel hitsplat4Label = new JLabel(" Damage: ");
        hitsplat4Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplat4Label, c);

        c.gridwidth = 1;
        c.gridx = 4;
        c.gridy = 9;
        JSpinner hitsplat4 = healthAttributes.getHitsplat4();
        hitsplat4.setModel(new SpinnerNumberModel(1, 0, 999, 1));
        card.add(hitsplat4, c);

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
        JLabel spotAnimLabel = new JLabel("SpotAnim: ");
        spotAnimLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(spotAnimLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JSpinner spotAnim1 = spAttributes.getSpotAnimId();
        spotAnim1.setValue(-1);
        card.add(spotAnim1, c);

        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 1;
        JLabel loop1Label = new JLabel("Loop: ");
        loop1Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(loop1Label, c);

        c.gridx = 3;
        c.gridy = 1;
        JComboBox<Toggle> loop = spAttributes.getLoop();
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        card.add(loop, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
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

        if (character == null)
        {
            objectLabel.setText("");
            setKeyFramedIcon(false);
            setAttributesEmpty();
            return;
        }

        objectLabel.setText(character.getName());
        KeyFrame keyFrame = character.findKeyFrame(selectedKeyFramePage, tick);
        setKeyFramedIcon(keyFrame != null);
        resetAttributes(character, tick);
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
            setAttributesEmpty();
            return;
        }

        setKeyFramedIcon(character.findKeyFrame(selectedKeyFramePage, tick) != null);
        KeyFrame keyFrame = character.findPreviousKeyFrame(selectedKeyFramePage, tick, true);

        if (keyFrame == null)
        {
            keyFrame = character.findNextKeyFrame(selectedKeyFramePage, tick);

            if (keyFrame == null)
            {
                switch (selectedKeyFramePage)
                {
                    default:
                    case MOVEMENT:
                        movementAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case ANIMATION:
                        animAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case ORIENTATION:
                        oriAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case SPAWN:
                        spawnAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case MODEL:
                        modelAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case TEXT:
                        textAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case OVERHEAD:
                        overheadAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case HEALTH:
                        healthAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case SPOTANIM:
                        spotAnimAttributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                    case SPOTANIM2:
                        spotAnim2Attributes.setBackgroundColours(KeyFrameState.EMPTY);
                        break;
                }

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
        }
    }

    public void setAttributesEmpty()
    {
        switch (selectedKeyFramePage)
        {
            default:
            case MOVEMENT:
                movementAttributes.resetAttributes();
                break;
            case ANIMATION:
                animAttributes.resetAttributes();
                break;
            case ORIENTATION:
                oriAttributes.resetAttributes();
                break;
            case SPAWN:
                spawnAttributes.resetAttributes();
                break;
            case MODEL:
                modelAttributes.resetAttributes();
                break;
            case TEXT:
                textAttributes.resetAttributes();
                break;
            case OVERHEAD:
                overheadAttributes.resetAttributes();
                break;
            case HEALTH:
                healthAttributes.resetAttributes();
                break;
            case SPOTANIM:
                spotAnimAttributes.resetAttributes();
                break;
            case SPOTANIM2:
                spotAnim2Attributes.resetAttributes();
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

