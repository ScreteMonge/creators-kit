package com.creatorskit.swing.timesheet;

import com.creatorskit.Character;
import com.creatorskit.models.CustomModel;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.datatypes.NPCData;
import com.creatorskit.programming.Direction;
import com.creatorskit.swing.AutoCompletion;
import com.creatorskit.swing.timesheet.attributes.*;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.settings.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.HeadIcon;
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
    private final JLabel keyFramed = new JLabel();

    private final String MOVE_CARD = "Movement";
    private final String ANIM_CARD = "Animation";
    private final String ORI_CARD = "Orientation";
    private final String SPAWN_CARD = "Spawn";
    private final String MODEL_CARD = "Model";
    private final String TEXT_CARD = "Text";
    private final String OVER_CARD = "Overhead";
    private final String HEALTH_CARD = "Health";
    private final String SPOTANIM_CARD = "SpotAnim";

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
        keyFramed.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                super.mouseReleased(e);
                timeSheetPanel.onKeyFrameIconPressedEvent();
            }
        });
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
        cardPanel.add(moveCard, MOVE_CARD);
        cardPanel.add(animCard, ANIM_CARD);
        cardPanel.add(oriCard, ORI_CARD);
        cardPanel.add(spawnCard, SPAWN_CARD);
        cardPanel.add(modelCard, MODEL_CARD);
        cardPanel.add(textCard, TEXT_CARD);
        cardPanel.add(overCard, OVER_CARD);
        cardPanel.add(healthCard, HEALTH_CARD);
        cardPanel.add(spotanimCard, SPOTANIM_CARD);

        setupMoveCard(moveCard);
        setupAnimCard(animCard);
        setupOriCard(oriCard);
        setupSpawnCard(spawnCard);
        setupModelCard(modelCard);
        setupTextCard(textCard);
        setupOverheadCard(overCard);
        setupHealthCard(healthCard);
        setupSpotAnimCard(spotanimCard);

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
                        (int) animAttributes.getManual().getValue(),
                        animAttributes.getManualOverride().getSelectedItem() == AnimationToggle.MANUAL_ANIMATION,
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
                        textAttributes.getText().getText(),
                        (int) textAttributes.getHeight().getValue()
                );
            case OVERHEAD:
                return new OverheadKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        overheadAttributes.getEnableBox().getSelectedItem() == Toggle.ENABLE,
                        (HeadIcon) overheadAttributes.getHeadIcon().getSelectedItem(),
                        (int) overheadAttributes.getHeight().getValue()
                );
            case HEALTH:
                return new HealthKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        healthAttributes.getEnableBox().getSelectedItem() == Toggle.ENABLE,
                        (HitsplatType) healthAttributes.getHitsplatType().getSelectedItem(),
                        (int) healthAttributes.getHitsplatHeight().getValue(),
                        (int) healthAttributes.getMaxHealth().getValue(),
                        (int) healthAttributes.getCurrentHealth().getValue(),
                        (int) healthAttributes.getHealthbarHeight().getValue()
                );
            case SPOTANIM:
                return new SpotAnimKeyFrame(
                        timeSheetPanel.getCurrentTime(),
                        (int) spotAnimAttributes.getSpotAnimId1().getValue(),
                        (int) spotAnimAttributes.getSpotAnimId2().getValue()
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
        JSpinner manual = animAttributes.getManual();
        manual.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        manual.setPreferredSize(spinnerSize);
        card.add(manual, c);

        c.gridwidth = 3;
        c.gridx = 2;
        c.gridy = 1;
        JComboBox<AnimationToggle> manualComboBox = animAttributes.getManualOverride();
        manualComboBox.setFocusable(false);
        manualComboBox.addItem(AnimationToggle.SMART_ANIMATION);
        manualComboBox.addItem(AnimationToggle.MANUAL_ANIMATION);
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
        JComboBox<NPCData> searcher = new JComboBox<>();
        AutoCompletion.enable(searcher);
        List<NPCData> npcData = dataFinder.getNpcData();
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
        for (NPCData n : npcData)
        {
            searcher.addItem(n);
        }
        searcher.setPreferredSize(new Dimension(270, 25));
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

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel idLabel = new JLabel("Model Id: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner id = modelAttributes.getModelId();
        id.setValue(-1);
        id.setPreferredSize(spinnerSize);
        card.add(id, c);

        c.gridwidth = 2;
        c.gridx = 2;
        c.gridy = 1;
        JComboBox<ModelToggle> toggleComboBox = modelAttributes.getModelOverride();
        toggleComboBox.setFocusable(false);
        toggleComboBox.addItem(ModelToggle.MODEL_ID);
        toggleComboBox.addItem(ModelToggle.CUSTOM_MODEL);
        card.add(toggleComboBox, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel customLabel = new JLabel("Custom Model: ");
        customLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(customLabel, c);

        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<CustomModel> customComboBox = modelAttributes.getCustomModel();
        customComboBox.setFocusable(false);
        //customComboBox.addItem(ModelToggle.MODEL_ID);
        card.add(customComboBox, c);

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

        c.weightx = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel heightLabel = new JLabel("Height: ");
        heightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(heightLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 3;
        JSpinner height = textAttributes.getHeight();
        height.setValue(60);
        card.add(height, c);

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
        JComboBox<Toggle> toggleComboBox = overheadAttributes.getEnableBox();
        toggleComboBox.setFocusable(false);
        toggleComboBox.addItem(Toggle.DISABLE);
        toggleComboBox.addItem(Toggle.ENABLE);
        card.add(toggleComboBox, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel textLabel = new JLabel("Overhead Icon: ");
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(textLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<HeadIcon> headIconBox = overheadAttributes.getHeadIcon();
        headIconBox.setFocusable(false);
        headIconBox.addItem(HeadIcon.MAGIC);
        headIconBox.addItem(HeadIcon.RANGED);
        headIconBox.addItem(HeadIcon.MELEE);
        headIconBox.addItem(HeadIcon.REDEMPTION);
        headIconBox.addItem(HeadIcon.RETRIBUTION);
        headIconBox.addItem(HeadIcon.SMITE);
        headIconBox.addItem(HeadIcon.RANGE_MAGE);
        headIconBox.addItem(HeadIcon.RANGE_MELEE);
        headIconBox.addItem(HeadIcon.MAGE_MELEE);
        headIconBox.addItem(HeadIcon.RANGE_MAGE_MELEE);
        headIconBox.addItem(HeadIcon.DEFLECT_MAGE);
        headIconBox.addItem(HeadIcon.DEFLECT_RANGE);
        headIconBox.addItem(HeadIcon.DEFLECT_MELEE);
        headIconBox.addItem(HeadIcon.SOUL_SPLIT);
        headIconBox.addItem(HeadIcon.WRATH);
        card.add(headIconBox, c);

        c.weightx = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel heightLabel = new JLabel("Height: ");
        heightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(heightLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 3;
        JSpinner height = overheadAttributes.getHeight();
        height.setValue(60);
        card.add(height, c);

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

        JLabel manualTitle = new JLabel("Healthbar & Hitsplats");
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
        toggleComboBox.addItem(Toggle.DISABLE);
        toggleComboBox.addItem(Toggle.ENABLE);
        card.add(toggleComboBox, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel textLabel = new JLabel("Hitsplat Icon: ");
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(textLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<HitsplatType> hitsplatComboBox = healthAttributes.getHitsplatType();
        hitsplatComboBox.setFocusable(false);
        hitsplatComboBox.addItem(HitsplatType.DAMAGE);
        hitsplatComboBox.addItem(HitsplatType.BLOCK);
        card.add(hitsplatComboBox, c);

        c.weightx = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel hitsplatHeightLabel = new JLabel("Hitsplat Height: ");
        hitsplatHeightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(hitsplatHeightLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 3;
        JSpinner hitsplatHeight = healthAttributes.getHitsplatHeight();
        hitsplatHeight.setValue(30);
        card.add(hitsplatHeight, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 4;
        JLabel maxHealthLabel = new JLabel("Max Health: ");
        maxHealthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(maxHealthLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 4;
        JSpinner maxHealth = healthAttributes.getMaxHealth();
        maxHealth.setValue(99);
        card.add(maxHealth, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 5;
        JLabel currentHealthLabel = new JLabel("Current Health: ");
        currentHealthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(currentHealthLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 5;
        JSpinner currentHealth = healthAttributes.getCurrentHealth();
        currentHealth.setValue(99);
        card.add(currentHealth, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 6;
        JLabel healthbarHeightLabel = new JLabel("Healthbar Height: ");
        healthbarHeightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(healthbarHeightLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 6;
        JSpinner healthbarHeight = healthAttributes.getHealthbarHeight();
        healthbarHeight.setValue(60);
        card.add(healthbarHeight, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupSpotAnimCard(JPanel card)
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
        JLabel spotAnim1Label = new JLabel("SpotAnim 1: ");
        spotAnim1Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(spotAnim1Label, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JSpinner spotAnim1 = spotAnimAttributes.getSpotAnimId1();
        spotAnim1.setValue(-1);
        card.add(spotAnim1, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel spotAnim2Label = new JLabel("SpotAnim 2: ");
        spotAnim2Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(spotAnim2Label, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JSpinner spotAnim2 = spotAnimAttributes.getSpotAnimId2();
        spotAnim2.setValue(-1);
        card.add(spotAnim2, c);

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
        }
    }

    private void setAttributesEmpty()
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

