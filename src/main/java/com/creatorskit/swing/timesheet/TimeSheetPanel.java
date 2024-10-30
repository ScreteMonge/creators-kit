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
import net.runelite.api.Client;
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

public class TimeSheetPanel extends JPanel
{
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final GridBagConstraints c = new GridBagConstraints();
    private final ToolBoxFrame toolBox;
    private final DataFinder dataFinder;
    @Getter
    private final SummarySheet summarySheet;
    private final AttributeSheet attributeSheet;
    @Getter
    private final TimeTree timeTree;
    private final JScrollBar scrollBar;
    private AttributePanel attributePanel;
    private final JPanel labelPanel = new JPanel();
    private final JPanel controlPanel = new JPanel();
    private final JSpinner timeSpinner = new JSpinner();
    private final BufferedImage CLOSE = ImageUtil.loadImageResource(getClass(), "/Close.png");
    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");

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

    @Getter
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

    private void setSelectedCharacter(Character character)
    {
        selectedCharacter = character;
        summarySheet.setSelectedCharacter(character);
        attributeSheet.setSelectedCharacter(character);
        attributePanel.setSelectedCharacter(character);
    }

    private void setupAttributePanel()
    {
        attributePanel = new AttributePanel(this, dataFinder);
    }

    /**
     * Adds the KeyFrame to every selected character in the TimeTree
     * @param keyFrame the keyframe to add or modify
     */
    public void addKeyFrame(KeyFrame keyFrame)
    {
        Character[] characters = timeTree.getSelectedCharacters();
        for (Character character : characters)
        {
            addKeyFrame(character, keyFrame);
        }
    }

    /**
     * Adds the keyframe to a specific character, or replaces a keyframe if the tick matches exactly
     * @param character the character to add the keyframe to
     * @param keyFrame the keyframe to add or modify for the character
     */
    public void addKeyFrame(Character character, KeyFrame keyFrame)
    {
        KeyFrameType type = keyFrame.getKeyFrameType();
        KeyFrame[] keyFrames = character.getKeyFrames(type);
        int[] framePosition = getFramePosition(keyFrames, keyFrame.getTick());
        if (framePosition[1] == 1)
        {
            keyFrames[framePosition[0]] = keyFrame;
        }
        else
        {
            keyFrames = ArrayUtils.insert(framePosition[0], keyFrames, keyFrame);
        }

        character.setKeyFrames(keyFrames, type);
    }

    /**
     * Gets the new position of the keyframe to add as an int[] of {index, boolean}
     * @param keyFrames the keyframe array to add to
     * @param newTick the tick of the new keyframe to be added
     * @return an int[] of {index, boolean}. The boolean determines whether the new keyframe will replace a previously existing keyframe of the exact same tick
     */
    private int[] getFramePosition(KeyFrame[] keyFrames, double newTick)
    {
        int frameIndex = 0;
        for (int i = 0; i < keyFrames.length; i++)
        {
            if (keyFrames[i].getTick() == newTick)
            {
                return new int[]{i, 1};
            }

            if (keyFrames[i].getTick() < newTick)
            {
                return new int[]{i, 0};
            }

            frameIndex++;
        }

        return new int[]{frameIndex, 0};
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
    }

    public void setPreviewTime(double tick)
    {
        attributeSheet.setPreviewTime(tick);
        summarySheet.setPreviewTime(tick);
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

        JButton backButton = new JButton(new ImageIcon(CLOSE));
        JButton playButton = new JButton(new ImageIcon(HELP));
        JButton forwardButton = new JButton(new ImageIcon(CLOSE));

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 0;
        JPanel controls = new JPanel();
        controls.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        controls.add(backButton);
        controls.add(playButton);
        controls.add(forwardButton);
        controlPanel.add(controls, c);
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
