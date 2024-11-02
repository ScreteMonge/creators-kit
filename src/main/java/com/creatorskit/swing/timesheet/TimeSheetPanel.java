package com.creatorskit.swing.timesheet;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.DataFinder;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.sheets.AttributeSheet;
import com.creatorskit.swing.timesheet.sheets.SummarySheet;
import com.creatorskit.swing.timesheet.sheets.TimeSheet;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

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

@Getter
@Setter
public class TimeSheetPanel extends JPanel
{
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final GridBagConstraints c = new GridBagConstraints();
    private final ToolBoxFrame toolBox;
    private final DataFinder dataFinder;
    private final SummarySheet summarySheet;
    private final AttributeSheet attributeSheet;
    private final TimeTree timeTree;
    private final JScrollBar scrollBar;
    private AttributePanel attributePanel;
    private final JPanel labelPanel = new JPanel();
    private final JPanel controlPanel = new JPanel();
    private final JSpinner timeSpinner = new JSpinner();
    private final BufferedImage PLAY = ImageUtil.loadImageResource(getClass(), "/Play.png");
    private final BufferedImage STOP = ImageUtil.loadImageResource(getClass(), "/Stop.png");
    private final BufferedImage SKIP_LEFT = ImageUtil.loadImageResource(getClass(), "/Skip_Left.png");
    private final BufferedImage SKIP_RIGHT = ImageUtil.loadImageResource(getClass(), "/Skip_Right.png");

    private static final int ROW_HEIGHT = 24;
    private static final int INDEX_BUFFER = 20;
    private static final int ABSOLUTE_MAX_SEQUENCE_LENGTH = 100000;
    private static final int ZOOM_MAX = 500;
    private static final int ZOOM_MIN = 5;

    private final String MOVE_CARD = "Movement";
    private final String ANIM_CARD = "Animation";
    private final String ORI_CARD = "Orientation";
    private final String SPAWN_CARD = "Spawn";
    private final String MODEL_CARD = "Model";
    private final String TEXT_CARD = "Text";
    private final String OVER_CARD = "Overhead";
    private final String HITS_CARD = "Hitsplat";
    private final String HEALTH_CARD = "Healthbar";
    private final String LABEL_OFFSET = "  ";

    private double zoom = 50;
    private double hScroll = 0;
    private int vScroll = 0;
    private double maxHScroll = 200;

    private double currentTime = 0;
    private boolean pauseScrollBarListener = false;
    private Character selectedCharacter;

    private KeyFrame[] selectedKeyFrames;

    @Inject
    public TimeSheetPanel(@Nullable Client client, ToolBoxFrame toolBox, CreatorsPlugin plugin, ClientThread clientThread, DataFinder dataFinder, TimeTree timeTree, JScrollBar scrollBar)
    {
        this.toolBox = toolBox;
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.dataFinder = dataFinder;
        this.summarySheet = new SummarySheet(toolBox);
        this.attributeSheet = new AttributeSheet(toolBox);
        this.timeTree = timeTree;
        this.scrollBar = scrollBar;

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        setupControlPanel();
        setupAttributePanel();
        setupAttributeSheet();
        setupScrollBar();
        setupTimeTreeListener();
        setupManager();
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

        KeyFrameType type = attributePanel.getSelectedKeyFramePage();
        KeyFrame keyFrame = selectedCharacter.findNextKeyFrame(type, currentTime);
        if (keyFrame == null)
        {
            return;
        }

        setCurrentTime(keyFrame.getTick());
    }

    public void onAttributeSkipPrevious()
    {
        if (selectedCharacter == null)
        {
            return;
        }

        KeyFrameType type = attributePanel.getSelectedKeyFramePage();
        KeyFrame keyFrame = selectedCharacter.findPreviousKeyFrame(type, currentTime, false);
        if (keyFrame == null)
        {
            return;
        }

        setCurrentTime(keyFrame.getTick());
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

        if (hScroll > 10)
        {
            hScroll = 10;
        }

        double maxVisibleValue = round(zoom - hScroll);
        if (maxVisibleValue > maxHScroll)
        {
            maxHScroll = maxVisibleValue;
        }

        updateScrollBar();
        updateSheets();
    }

    public void onHorizontalScrollEvent(int amount)
    {
        hScroll = round(hScroll - amount * zoom / 50);
        if (hScroll < -ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            hScroll = -ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        if (hScroll > 10)
        {
            hScroll = 10;
        }

        double maxVisibleValue = round(zoom - hScroll);
        if (maxVisibleValue > maxHScroll)
        {
            maxHScroll = maxVisibleValue;
        }

        updateScrollBar();
        updateSheets();
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

            addKeyFrame(selectedCharacter, kf);
            return;
        }

       removeKeyFrame(selectedCharacter, keyFrame);
    }


    /**
     * Adds the keyframe to a specific character, or replaces a keyframe if the tick matches exactly
     * @param character the character to add the keyframe to
     * @param keyFrame the keyframe to add or modify for the character
     */
    public void addKeyFrame(Character character, KeyFrame keyFrame)
    {
        if (character == null)
        {
            return;
        }

        character.addKeyFrame(keyFrame);
        attributePanel.setKeyFramedIcon(true);
    }

    /**
     * Removes a keyframe from a specific character of the indicated type, at the indicated tick
     * @param character the character to remove the keyframe from
     * @param type the KeyFrameType
     * @param tick the tick to add the keyframe to
     */
    public void removeKeyFrame(Character character, KeyFrameType type, double tick)
    {
        if (character == null)
        {
            return;
        }

        character.removeKeyFrame(type, tick);
        attributePanel.setKeyFramedIcon(false);
    }

    /**
     * Removes a specific keyframe from the chosen character
     * @param character the character to remove the keyframe from
     * @param keyFrame the keyframe to remove
     */
    public void removeKeyFrame(Character character, KeyFrame keyFrame)
    {
        character.removeKeyFrame(keyFrame);
        attributePanel.setKeyFramedIcon(false);
    }

    private void setSelectedCharacter(Character character)
    {
        selectedCharacter = character;
        summarySheet.setSelectedCharacter(character);
        attributeSheet.setSelectedCharacter(character);
        attributePanel.setSelectedCharacter(character);
    }

    public void setCurrentTime(double tick)
    {
        if (tick < -10)
        {
            tick = -10;
        }

        if (tick > ABSOLUTE_MAX_SEQUENCE_LENGTH)
        {
            tick = ABSOLUTE_MAX_SEQUENCE_LENGTH;
        }

        currentTime = tick;
        attributeSheet.setCurrentTime(currentTime);
        summarySheet.setCurrentTime(currentTime);
        timeSpinner.setValue(tick);

        if (selectedCharacter == null)
        {
            return;
        }

        attributePanel.setKeyFramedIcon(selectedCharacter.findKeyFrame(attributePanel.getSelectedKeyFramePage(), currentTime) != null);
    }

    public void setPreviewTime(double tick)
    {
        attributeSheet.setPreviewTime(tick);
        summarySheet.setPreviewTime(tick);
    }

    private void setupControlPanel()
    {
        controlPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        controlPanel.setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        controlPanel.add(timeSpinner, c);

        timeSpinner.setBackground(ColorScheme.DARK_GRAY_COLOR);
        timeSpinner.setModel(new SpinnerNumberModel(0, -10, ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.1));
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) timeSpinner.getEditor();
        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(2);
        timeSpinner.setValue(0);
        timeSpinner.addChangeListener(e ->
        {
            double tick = TimeSheetPanel.round((double) timeSpinner.getValue());
            toolBox.getTimeSheetPanel().setCurrentTime(tick);
        });

        JButton playButton = new JButton(new ImageIcon(PLAY));

        JPanel backPanel = new JPanel();
        backPanel.setLayout(new BoxLayout(backPanel, BoxLayout.PAGE_AXIS));
        JButton backSummarySheet = new JButton(new ImageIcon(SKIP_LEFT));
        JButton backAttributeSheet = new JButton(new ImageIcon(SKIP_LEFT));
        backSummarySheet.addActionListener(e -> onSummarySkipPrevious());
        backAttributeSheet.addActionListener(e -> onAttributeSkipPrevious());
        backPanel.add(backSummarySheet);
        backPanel.add(backAttributeSheet);

        JPanel forwardPanel = new JPanel();
        forwardPanel.setLayout(new BoxLayout(forwardPanel, BoxLayout.PAGE_AXIS));
        JButton forwardSummarySheet = new JButton(new ImageIcon(SKIP_RIGHT));
        JButton forwardAttributeSheet = new JButton(new ImageIcon(SKIP_RIGHT));
        forwardSummarySheet.addActionListener(e -> onSummarySkipForward());
        forwardAttributeSheet.addActionListener(e -> onAttributeSkipForward());
        forwardPanel.add(forwardSummarySheet);
        forwardPanel.add(forwardAttributeSheet);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
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
        attributePanel = new AttributePanel(this, dataFinder);
    }

    private void setupAttributeSheet()
    {
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.setBorder(new EmptyBorder(1, 0, 1, 0));
        labelPanel.setLayout(new GridLayout(0, 1, 0, 0));
        JLabel[] labels = new JLabel[KeyFrameType.getTotalFrameTypes() + 1];
        for (int i = 0; i < KeyFrameType.getTotalFrameTypes() + 1; i++)
        {
            JLabel label = new JLabel();
            label.setFocusable(true);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setOpaque(true);
            label.setPreferredSize(new Dimension(100, 24));
            label.setBackground(ColorScheme.DARKER_GRAY_COLOR);

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
                        labels[attributeSheet.getSelectedIndex()].setBackground(ColorScheme.DARKER_GRAY_COLOR);
                        label.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);

                        attributePanel.switchCards(label.getText().replaceAll(LABEL_OFFSET, ""));

                        super.mousePressed(e);
                        for (int f = 0; f < labels.length; f++)
                        {
                            if (labels[f] == label)
                            {
                                attributeSheet.setSelectedIndex(f);
                            }
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
        labels[8].setText(HITS_CARD + LABEL_OFFSET);
        labels[9].setText(HEALTH_CARD + LABEL_OFFSET);
    }

    private void setupScrollBar()
    {
        scrollBar.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        scrollBar.setPreferredSize(new Dimension(0, 15));
        scrollBar.setMinimum(-10);
        scrollBar.setMaximum((int) maxHScroll);
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
            updateSheets();
        });
    }

    private void setupTimeTreeListener()
    {
        timeTree.getTree().addTreeSelectionListener(e ->
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
            }
        });
    }

    private void setupManager()
    {
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridheight = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        JLabel buffer = new JLabel("Folders");
        buffer.setFont(FontManager.getDefaultBoldFont());
        buffer.setHorizontalTextPosition(SwingConstants.LEFT);
        buffer.setPreferredSize(new Dimension(0, INDEX_BUFFER));
        add(buffer, c);

        c.gridwidth = 2;
        c.weightx = 0;
        c.weighty = 5;
        c.gridx = 0;
        c.gridy = 1;
        add(timeTree, c);

        c.gridwidth = 1;
        c.gridheight = 2;
        c.weightx = 8;
        c.weighty = 5;
        c.gridx = 2;
        c.gridy = 0;
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
        scrollBar.setMaximum((int) maxHScroll);
        scrollBar.setBlockIncrement((int) (zoom / 5));
        scrollBar.setUnitIncrement((int) (zoom / 50));
        scrollBar.setVisibleAmount((int) (zoom));
        scrollBar.setValue((int) -hScroll);
        pauseScrollBarListener = false;
    }

    public static double round (double value)
    {
        int scale = (int) Math.pow(10, 1);
        return (double) Math.round(value * scale) / scale;
    }
}
