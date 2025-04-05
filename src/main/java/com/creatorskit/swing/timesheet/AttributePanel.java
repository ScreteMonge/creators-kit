package com.creatorskit.swing.timesheet;

import com.creatorskit.Character;
import com.creatorskit.models.CustomModel;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.DataFinder.DataType;
import com.creatorskit.models.datatypes.NPCData;
import com.creatorskit.programming.Direction;
import com.creatorskit.swing.AutoCompletion;
import com.creatorskit.swing.timesheet.attributes.*;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.settings.*;
import lombok.Getter;
import lombok.Setter;
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

@Getter
@Setter
public class AttributePanel extends JPanel
{
    private TimeSheetPanel timeSheetPanel;
    private DataFinder dataFinder;

    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");
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

    private final AnimAttributes animAttributes = new AnimAttributes();
    private final OriAttributes oriAttributes = new OriAttributes();
    private final SpawnAttributes spawnAttributes = new SpawnAttributes();
    private final ModelAttributes modelAttributes = new ModelAttributes();
    private final TextAttributes textAttributes = new TextAttributes();
    private final OverheadAttributes overheadAttributes = new OverheadAttributes();
    private final HealthAttributes healthAttributes = new HealthAttributes();
    private final SpotAnimAttributes spotAnimAttributes = new SpotAnimAttributes();
    private final SpotAnimAttributes spotAnim2Attributes = new SpotAnimAttributes();

    @Inject
    public AttributePanel(TimeSheetPanel timeSheetPanel, DataFinder dataFinder)
    {
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
        keyFramed.setIcon(keyframeEmptyImage);
        keyFramed.setPreferredSize(new Dimension(18, 18));
        keyFramed.addActionListener(e -> timeSheetPanel.onKeyFrameIconPressedEvent());
        add(keyFramed, c);

        c.gridwidth = 3;
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
    public KeyFrame createKeyFrame()
    {
        switch (selectedKeyFramePage)
        {
            default:
            case MOVEMENT:
                break;
            case ANIMATION:
                return new AnimationKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        (int) animAttributes.getAction().getValue(),
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
                        timeSheetPanel.getCurrentTime(),
                        (int) oriAttributes.getManual().getValue(),
                        oriAttributes.getManualOverride().getSelectedItem() == OrientationToggle.MANUAL_ORIENTATION
                );
            case SPAWN:
                return new SpawnKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        spawnAttributes.getSpawn().getSelectedItem() == Toggle.ENABLE
                );
            case MODEL:
                return new ModelKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        modelAttributes.getModelOverride().getSelectedItem() == ModelToggle.CUSTOM_MODEL,
                        (int) modelAttributes.getModelId().getValue(),
                        (CustomModel) modelAttributes.getCustomModel().getSelectedItem()
                );
            case TEXT:
                return new TextKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        textAttributes.getEnableBox().getSelectedItem() == Toggle.ENABLE,
                        textAttributes.getText().getText()
                );
            case OVERHEAD:
                return new OverheadKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        (OverheadSprite) overheadAttributes.getSkullSprite().getSelectedItem(),
                        (OverheadSprite) overheadAttributes.getPrayerSprite().getSelectedItem()
                );
            case HEALTH:
                return new HealthKeyFrame(
                        timeSheetPanel.getCurrentTime(),
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
                        timeSheetPanel.getCurrentTime(),
                        KeyFrameType.SPOTANIM,
                        (int) spotAnimAttributes.getSpotAnimId().getValue(),
                        spotAnimAttributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        (int) spotAnimAttributes.getHeight().getValue()
                );
            case SPOTANIM2:
                return new SpotAnimKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        KeyFrameType.SPOTANIM2,
                        (int) spotAnim2Attributes.getSpotAnimId().getValue(),
                        spotAnim2Attributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        (int) spotAnim2Attributes.getHeight().getValue()
                );
        }

        return null;
    }

    private void setupMoveCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Movement");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(title, c);
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
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Manual Animation");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("<html>Enabling manual animation override lets you exactly control what animation is played at a given time" +
                "<br>Smart animation instead bases your animations off the Object's movement. For example:" +
                "<br>The Idle animation plays when your character is not moving, while the Run animation plays while the Object is moving >1 tile/tick</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel manualLabel = new JLabel("Manual: ");
        manualLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(manualLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner manual = animAttributes.getAction();
        manual.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        manual.setPreferredSize(spinnerSize);
        card.add(manual, c);

        c.gridwidth = 3;
        c.gridx = 2;
        c.gridy = 1;
        JComboBox<Toggle> manualComboBox = animAttributes.getLoop();
        manualComboBox.setFocusable(false);
        manualComboBox.addItem(Toggle.DISABLE);
        manualComboBox.addItem(Toggle.ENABLE);
        card.add(manualComboBox, c);

        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 2;
        JLabel smartTitle = new JLabel("Smart Animation");
        smartTitle.setFont(FontManager.getRunescapeBoldFont());
        card.add(smartTitle, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel idleLabel = new JLabel("Idle: ");
        idleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner idle = animAttributes.getIdle();
        idle.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idle.setPreferredSize(spinnerSize);
        card.add(idle, c);

        c.gridx = 2;
        c.gridy = 3;
        JLabel walk180Label = new JLabel("Walk 180: ");
        walk180Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walk180Label, c);

        c.gridx = 3;
        c.gridy = 3;
        JSpinner walk180 = animAttributes.getWalk180();
        walk180.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walk180.setPreferredSize(spinnerSize);
        card.add(walk180, c);

        c.gridx = 0;
        c.gridy = 4;
        JLabel walkLabel = new JLabel("Walk: ");
        walkLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner walk = animAttributes.getWalk();
        walk.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walk.setPreferredSize(spinnerSize);
        card.add(walk, c);

        c.gridx = 2;
        c.gridy = 4;
        JLabel walkRLabel = new JLabel("Walk Right: ");
        walkRLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkRLabel, c);

        c.gridx = 3;
        c.gridy = 4;
        JSpinner walkRight = animAttributes.getWalkRight();
        walkRight.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walkRight.setPreferredSize(spinnerSize);
        card.add(walkRight, c);

        c.gridx = 0;
        c.gridy = 5;
        JLabel runLabel = new JLabel("Run: ");
        runLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(runLabel, c);

        c.gridx = 1;
        c.gridy = 5;
        JSpinner run = animAttributes.getRun();
        run.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        run.setPreferredSize(spinnerSize);
        card.add(run, c);

        c.gridx = 2;
        c.gridy = 5;
        JLabel walkLLabel = new JLabel("Walk Left: ");
        walkLLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkLLabel, c);

        c.gridx = 3;
        c.gridy = 5;
        JSpinner walkLeft = animAttributes.getWalkLeft();
        walkLeft.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walkLeft.setPreferredSize(spinnerSize);
        card.add(walkLeft, c);

        c.gridx = 2;
        c.gridy = 6;
        JLabel idleRLabel = new JLabel("Idle Right: ");
        idleRLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleRLabel, c);

        c.gridx = 3;
        c.gridy = 6;
        JSpinner idleRight = animAttributes.getIdleRight();
        idleRight.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idleRight.setPreferredSize(spinnerSize);
        card.add(idleRight, c);

        c.gridx = 2;
        c.gridy = 7;
        JLabel idleLLabel = new JLabel("Idle Left: ");
        idleLLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleLLabel, c);

        c.gridx = 3;
        c.gridy = 7;
        JSpinner idleLeft = animAttributes.getIdleLeft();
        idleLeft.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idleLeft.setPreferredSize(spinnerSize);
        card.add(idleLeft, c);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 8;
        JLabel searcherLabel = new JLabel("NPC Animation Presets: ");
        searcherLabel.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(searcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 2;
        c.gridy = 8;
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
        card.add(searcher, c);

        c.gridwidth = 1;
        c.gridx = 5;
        c.gridy = 8;
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

        JLabel manualTitle = new JLabel("Manual Orientation");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("<html>Enabling manual orientation override lets you exactly control what orientation your Object will face" +
                "<br>Otherwise, the Object's orientation is instead based off of the direction of its movement</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel manualLabel = new JLabel("Manual: ");
        manualLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(manualLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner manual = oriAttributes.getManual();
        manual.setModel(new SpinnerNumberModel(0, 0, 2048, 1));
        manual.setPreferredSize(spinnerSize);
        card.add(manual, c);

        c.gridwidth = 2;
        c.gridx = 2;
        c.gridy = 1;
        JComboBox<OrientationToggle> manualCheckbox = oriAttributes.getManualOverride();
        manualCheckbox.setFocusable(false);
        manualCheckbox.addItem(OrientationToggle.SMART_ORIENTATION);
        manualCheckbox.addItem(OrientationToggle.MANUAL_ORIENTATION);
        card.add(manualCheckbox, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        JLabel presetLabel = new JLabel("Presets: ");
        presetLabel.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(presetLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<Direction> searcher = new JComboBox<>();
        AutoCompletion.enable(searcher);
        Direction[] directions = Direction.getAllDirections();
        for (Direction d : directions)
        {
            searcher.addItem(d);
        }
        searcher.setPreferredSize(new Dimension(100, 25));
        card.add(searcher, c);

        c.gridwidth = 1;
        c.gridx = 4;
        c.gridy = 2;
        JButton searchApply = new JButton("Apply");
        searchApply.addActionListener(e ->
        {
            Direction direction = (Direction) searcher.getSelectedItem();
            if (direction == null)
            {
                return;
            }

            manual.setValue(direction.getJUnit());
        });
        card.add(searchApply, c);


        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
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
        JComboBox<Toggle> toggleComboBox = textAttributes.getEnableBox();
        toggleComboBox.setFocusable(false);
        toggleComboBox.addItem(Toggle.DISABLE);
        toggleComboBox.addItem(Toggle.ENABLE);
        card.add(toggleComboBox, c);

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
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, cardName);
        cardLabel.setText(cardName);

        switch (cardName)
        {
            default:
            case MOVE_CARD:
                selectedKeyFramePage = KeyFrameType.MOVEMENT;
                break;
            case ANIM_CARD:
                selectedKeyFramePage = KeyFrameType.ANIMATION;
                break;
            case ORI_CARD:
                selectedKeyFramePage = KeyFrameType.ORIENTATION;
                break;
            case SPAWN_CARD:
                selectedKeyFramePage = KeyFrameType.SPAWN;
                break;
            case MODEL_CARD:
                selectedKeyFramePage = KeyFrameType.MODEL;
                break;
            case TEXT_CARD:
                selectedKeyFramePage = KeyFrameType.TEXT;
                break;
            case OVER_CARD:
                selectedKeyFramePage = KeyFrameType.OVERHEAD;
                break;
            case HEALTH_CARD:
                selectedKeyFramePage = KeyFrameType.HEALTH;
                break;
            case SPOTANIM_CARD:
                selectedKeyFramePage = KeyFrameType.SPOTANIM;
                break;
            case SPOTANIM2_CARD:
                selectedKeyFramePage = KeyFrameType.SPOTANIM2;
        }

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

