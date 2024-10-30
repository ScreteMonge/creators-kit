package com.creatorskit.swing.timesheet;

import com.creatorskit.Character;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.datatypes.NPCData;
import com.creatorskit.swing.AutoCompletion;
import com.creatorskit.swing.timesheet.attributes.AnimAttributes;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
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
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public class AttributePanel extends JPanel
{
    private TimeSheetPanel timeSheetPanel;
    private DataFinder dataFinder;

    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");
    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel cardPanel = new JPanel();
    private final JLabel objectLabel = new JLabel("Pick an Object");
    private final JLabel cardLabel = new JLabel("");

    private final String MOVE_CARD = "Movement";
    private final String ANIM_CARD = "Animation";
    private final String ORI_CARD = "Orientation";
    private final String SPAWN_CARD = "Spawn";
    private final String MODEL_CARD = "Model";
    private final String TEXT_CARD = "Text";
    private final String OVER_CARD = "Overhead";
    private final String HITS_CARD = "Hitsplat";
    private final String HEALTH_CARD = "Healthbar";

    private KeyFrameType hoveredKeyFrameType;

    private final AnimAttributes animAttributes = new AnimAttributes();

    @Inject
    public AttributePanel(TimeSheetPanel timeSheetPanel, DataFinder dataFinder)
    {
        this.timeSheetPanel = timeSheetPanel;
        this.dataFinder = dataFinder;

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setKeyBindings();

        objectLabel.setFont(FontManager.getDefaultBoldFont());
        objectLabel.setHorizontalAlignment(SwingConstants.LEFT);

        cardPanel.setLayout(new CardLayout());
        cardPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        add(objectLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        cardLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        cardLabel.setFont(FontManager.getRunescapeBoldFont());
        add(cardLabel, c);

        c.gridwidth = 2;
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
        JPanel hitsCard = new JPanel();
        JPanel healthCard = new JPanel();
        cardPanel.add(moveCard, MOVE_CARD);
        cardPanel.add(animCard, ANIM_CARD);
        cardPanel.add(oriCard, ORI_CARD);
        cardPanel.add(spawnCard, SPAWN_CARD);
        cardPanel.add(modelCard, MODEL_CARD);
        cardPanel.add(textCard, TEXT_CARD);
        cardPanel.add(overCard, OVER_CARD);
        cardPanel.add(hitsCard, HITS_CARD);
        cardPanel.add(healthCard, HEALTH_CARD);

        setupMoveCard(moveCard);
        setupAnimCard(animCard);

    }

    private void setupMoveCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());

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
        JSpinner manual = animAttributes.getOverride();
        manual.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        manual.setPreferredSize(spinnerSize);
        card.add(manual, c);

        c.gridwidth = 3;
        c.gridx = 2;
        c.gridy = 1;
        JCheckBox manualCheckbox = animAttributes.getCheckBox();
        manualCheckbox.setText("Enable manual override");
        manualCheckbox.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(manualCheckbox, c);

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

    public void switchCards(String cardName)
    {
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, cardName);
        cardLabel.setText(cardName);
    }

    public void setSelectedCharacter(Character character)
    {
        objectLabel.setText(character.getName());
    }

    private void addHoverListeners(Component component, KeyFrameType type)
    {
        component.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                super.mouseEntered(e);
                hoveredKeyFrameType = type;
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                super.mouseExited(e);
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
                switch (hoveredKeyFrameType)
                {
                    default:
                    case NULL:
                        return;
                    case MOVEMENT:
                        return;
                    case ANIMATION:
                        //timeSheetPanel.addKeyFrame(new AnimationKeyFrame(timeSheetPanel.getCurrentTime(), (int) animSpinner.getValue()));
                }
            }
        });
    }
}

