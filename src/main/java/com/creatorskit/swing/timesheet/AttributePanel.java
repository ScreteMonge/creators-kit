package com.creatorskit.swing.timesheet;

import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

@Getter
@Setter
public class AttributePanel extends JPanel
{
    private TimeSheetPanel timeSheetPanel;
    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel cardPanel = new JPanel();

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

    private final JSpinner animSpinner = new JSpinner();

    @Inject
    public AttributePanel(TimeSheetPanel timeSheetPanel, JLabel objectLabel)
    {
        this.timeSheetPanel = timeSheetPanel;

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
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        add(objectLabel, c);

        c.weightx = 0;
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
        card.setLayout(new GridBagLayout());
        card.setFocusable(true);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Animation");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(title, c);

        c.gridx = 0;
        c.gridy++;
        card.add(animSpinner, c);
        addHoverListenersWithChildren(animSpinner, KeyFrameType.ANIMATION);

        c.weightx = 0;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy++;
        JLabel empty = new JLabel("");
        card.add(empty, c);

        c.weightx = 1;
        c.weighty = 0;
        c.gridx++;
        c.gridy = 0;
        JLabel empty2 = new JLabel("");
        card.add(empty2, c);
    }

    public void switchCards(String cardName)
    {
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, cardName);
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
                        timeSheetPanel.addKeyFrame(new AnimationKeyFrame(timeSheetPanel.getCurrentTime(), (int) animSpinner.getValue()));
                }
            }
        });
    }
}

