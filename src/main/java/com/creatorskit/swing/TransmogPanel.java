package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.saves.TransmogLoadOption;
import com.creatorskit.saves.TransmogSave;
import com.creatorskit.models.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
@Getter
public class TransmogPanel extends JPanel
{
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final File TRANSMOGS_DIR = new File(Paths.get(RuneLite.RUNELITE_DIR.getPath(), "creatorskit").toString(), "transmogs");
    private final GridBagConstraints c = new GridBagConstraints();
    private final JLabel transmogLabel = new JLabel("None");
    private final JSpinner radiusSpinner = new JSpinner(new SpinnerNumberModel(60, 0, 99999, 1));
    private final JComboBox<TransmogAnimationMode> animationComboBox = new JComboBox<>();
    private final JSpinner poseSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99999, 1));
    private final JSpinner walkSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99999, 1));
    private final JSpinner runSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99999, 1));
    private final JSpinner actionSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99999, 1));
    private final JSpinner backwardsSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99999, 1));
    private final JSpinner rightSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99999, 1));
    private final JSpinner leftSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99999, 1));
    private final JSpinner rotateSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99999, 1));
    private final JPanel animationSwapsPanel = new JPanel();
    private TransmogAnimationMode transmogAnimationMode = TransmogAnimationMode.PLAYER;
    private int[][] animationSwaps = new int[0][2];
    private int poseAnimation = -1;
    private int walkAnimation = -1;
    private int runAnimation = -1;
    private int actionAnimation = -1;
    private int backwardsAnimation = -1;
    private int rightAnimation = -1;
    private int leftAnimation = -1;
    private int rotateAnimation = -1;
    private int radius = 60;

    @Inject
    public TransmogPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane();
        add(scrollPane);

        JPanel rowHeaderPanel = new JPanel();
        rowHeaderPanel.setLayout(new GridBagLayout());
        rowHeaderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setRowHeaderView(rowHeaderPanel);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1;
        c.weighty = 0;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        JLabel currentLabel = new JLabel("Current Transmog");
        currentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        currentLabel.setFont(FontManager.getRunescapeBoldFont());
        rowHeaderPanel.add(currentLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        transmogLabel.setHorizontalAlignment(SwingConstants.CENTER);
        transmogLabel.setForeground(Color.YELLOW);
        rowHeaderPanel.add(transmogLabel, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        JLabel radiusLabel = new JLabel("Radius:");
        radiusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(radiusLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        radiusSpinner.setToolTipText("Set the radius of your transmog. 60 is best for an object the size of 1 tile");
        radiusSpinner.addChangeListener(e -> {
            radius = (int) radiusSpinner.getValue();
            CKObject ckObject = plugin.getTransmog();
            if (ckObject == null)
                return;

            clientThread.invokeLater(() -> ckObject.setRadius(radius));
        });
        rowHeaderPanel.add(radiusSpinner, c);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        JLabel swapTitle = new JLabel("Animation Swaps");
        swapTitle.setHorizontalAlignment(SwingConstants.CENTER);
        swapTitle.setFont(FontManager.getRunescapeBoldFont());
        rowHeaderPanel.add(swapTitle, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 6;
        JLabel systemLabel = new JLabel("System:");
        systemLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(systemLabel, c);

        c.gridx = 1;
        c.gridy = 6;
        animationComboBox.addItem(TransmogAnimationMode.PLAYER);
        animationComboBox.addItem(TransmogAnimationMode.MODIFIED);
        animationComboBox.addItem(TransmogAnimationMode.CUSTOM);
        animationComboBox.addItem(TransmogAnimationMode.NONE);
        animationComboBox.setFocusable(false);
        animationComboBox.setToolTipText("<html>" + "Set how to handle your Transmog's animations" + "<br>" +
                "1) Player: Copy your player character's animations," + "<br>" +
                "2) Modified: Use all swaps indicated here; otherwise, copy your player character's animations," + "<br>" +
                "3) Custom: Only use the animations indicated here," + "<br>" +
                "4) None: Don't animate at all" + "<html>");

        animationComboBox.addItemListener(e -> transmogAnimationMode = (TransmogAnimationMode) animationComboBox.getSelectedItem());
        rowHeaderPanel.add(animationComboBox, c);

        c.gridx = 0;
        c.gridy = 7;
        JLabel poseLabel = new JLabel("Pose:");
        poseLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(poseLabel, c);

        c.gridx = 1;
        c.gridy = 7;
        poseSpinner.setToolTipText("Custom only: Set your Transmog's idle/pose animation");
        poseSpinner.addChangeListener(e -> poseAnimation = (int) poseSpinner.getValue());
        rowHeaderPanel.add(poseSpinner, c);

        c.gridx = 0;
        c.gridy = 8;
        JLabel walkLabel = new JLabel("Walk:");
        walkLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(walkLabel, c);

        c.gridx = 1;
        c.gridy = 8;
        walkSpinner.setToolTipText("Custom only: Set your Transmog's walk animation");
        walkSpinner.addChangeListener(e -> walkAnimation = (int) walkSpinner.getValue());
        rowHeaderPanel.add(walkSpinner, c);

        c.gridx = 0;
        c.gridy = 9;
        JLabel runLabel = new JLabel("Run:");
        runLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(runLabel, c);

        c.gridx = 1;
        c.gridy = 9;
        runSpinner.setToolTipText("Custom only: Set your Transmog's run animation");
        runSpinner.addChangeListener(e -> runAnimation = (int) runSpinner.getValue());
        rowHeaderPanel.add(runSpinner, c);

        c.gridx = 0;
        c.gridy = 10;
        JLabel actionLabel = new JLabel("Action:");
        actionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(actionLabel, c);

        c.gridx = 1;
        c.gridy = 10;
        actionSpinner.setToolTipText("Custom only: Set your Transmog's default interact animation");
        actionSpinner.addChangeListener(e -> actionAnimation = (int) actionSpinner.getValue());
        rowHeaderPanel.add(actionSpinner, c);

        c.gridx = 0;
        c.gridy = 11;
        JLabel backwardsLabel = new JLabel("Backwards:");
        backwardsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(backwardsLabel, c);

        c.gridx = 1;
        c.gridy = 11;
        backwardsSpinner.setToolTipText("Custom only: Set your Transmog's backwards walk animation");
        backwardsSpinner.addChangeListener(e -> backwardsAnimation = (int) backwardsSpinner.getValue());
        rowHeaderPanel.add(backwardsSpinner, c);

        c.gridx = 0;
        c.gridy = 12;
        JLabel rightLabel = new JLabel("Shuffle Right:");
        rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(rightLabel, c);

        c.gridx = 1;
        c.gridy = 12;
        rightSpinner.setToolTipText("Custom only: Set your Transmog's shuffle right animation");
        rightSpinner.addChangeListener(e -> rightAnimation = (int) rightSpinner.getValue());
        rowHeaderPanel.add(rightSpinner, c);

        c.gridx = 0;
        c.gridy = 13;
        JLabel leftLabel = new JLabel("Shuffle Left:");
        leftLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(leftLabel, c);

        c.gridx = 1;
        c.gridy = 13;
        leftSpinner.setToolTipText("Custom only: Set your Transmog's shuffle left animation");
        leftSpinner.addChangeListener(e -> leftAnimation = (int) leftSpinner.getValue());
        rowHeaderPanel.add(leftSpinner, c);

        c.gridx = 0;
        c.gridy = 14;
        JLabel rotateLabel = new JLabel("Rotate:");
        rotateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowHeaderPanel.add(rotateLabel, c);

        c.gridx = 1;
        c.gridy = 14;
        rotateSpinner.setToolTipText("Custom only: Set your Transmog's rotate animation");
        rotateSpinner.addChangeListener(e -> rotateAnimation = (int) rotateSpinner.getValue());
        rowHeaderPanel.add(rotateSpinner, c);

        c.gridx = 0;
        c.gridy = 15;
        JButton defaultsButton = new JButton("Defaults");
        defaultsButton.setToolTipText("Sets all the Animation Swaps to player defaults");
        defaultsButton.addActionListener(e ->
        {
            poseSpinner.setValue(808);
            walkSpinner.setValue(819);
            runSpinner.setValue(824);
            actionSpinner.setValue(866);
            backwardsSpinner.setValue(820);
            rightSpinner.setValue(822);
            leftSpinner.setValue(821);
            rotateSpinner.setValue(823);
        });
        rowHeaderPanel.add(defaultsButton, c);

        c.gridx = 1;
        c.gridy = 15;
        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText("Resets all regular Animation Swaps to -1");
        resetButton.addActionListener(e ->
        {
            resetSidePanel();
        });
        rowHeaderPanel.add(resetButton, c);

        c.gridwidth = 2;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 16;
        JButton addButton = new JButton("Add additional swap");
        addButton.setToolTipText("Add another specific animation swap");
        addButton.addActionListener(e -> addSwapPanel());
        rowHeaderPanel.add(addButton, c);

        JPanel columnHeaderPanel = new JPanel();
        columnHeaderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setColumnHeaderView(columnHeaderPanel);

        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("Saves your Transmog and Animation Swaps for future use");
        columnHeaderPanel.add(saveButton);
        saveButton.addActionListener(e ->
        {
            openSaveDialog(transmogLabel.getText());
        });

        JButton loadButton = new JButton("Load");
        loadButton.setToolTipText("Load a previously saved Transmog and/or set of Animation Swaps");
        columnHeaderPanel.add(loadButton);
        loadButton.addActionListener(e ->
        {
            openLoadDialog();
        });

        JButton clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear all Additional Swaps");
        columnHeaderPanel.add(clearButton);
        clearButton.addActionListener(e ->
        {
            clearSwapPanels();
        });

        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel viewport = new JPanel();
        viewport.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        viewport.setBorder(new EmptyBorder(4, 6, 4, 6));
        viewport.add(animationSwapsPanel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        JLabel emptyLabel = new JLabel("");
        viewport.add(emptyLabel, c);

        scrollPane.setViewportView(viewport);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        animationSwapsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        animationSwapsPanel.setLayout(new GridLayout(0, 8, 8, 8));
        revalidate();
    }

    private void addSwapPanel()
    {
        addSwapPanel(-1, -1);
    }

    private void addSwapPanel(int swapOut, int swapIn)
    {
        JPanel swap = new JPanel();
        swap.setLayout(new GridBagLayout());
        swap.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1, true));

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;

        final int[] animationSwap = new int[]{swapOut, swapIn};
        if (swapOut != -1)
            addAnimationSwap(swapOut, swapIn);

        c.gridx = 0;
        c.gridy = 0;
        JLabel outLabel = new JLabel("Swap out:");
        outLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        swap.add(outLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        JSpinner outSpinner = new JSpinner(new SpinnerNumberModel(swapOut, -1, 99999, 1));
        outSpinner.setToolTipText("Pick which player animation to swap out");
        swap.add(outSpinner, c);

        c.gridx = 0;
        c.gridy = 1;
        JLabel inLabel = new JLabel("Swap in:");
        inLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        swap.add(inLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner inSpinner = new JSpinner(new SpinnerNumberModel(swapIn, -1, 99999, 1));
        inSpinner.setToolTipText("Pick which new animation to swap in");
        swap.add(inSpinner, c);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 2;
        JButton removeButton = new JButton("Remove");
        removeButton.setToolTipText("Removes this animation swap");
        removeButton.addActionListener(e ->
        {
            int out = (int) outSpinner.getValue();
            int in = (int) inSpinner.getValue();
            removeSwapPanel(swap, out, in);
        });
        swap.add(removeButton, c);

        inSpinner.addChangeListener(e ->
        {
            int previousIn = animationSwap[1];
            int newIn = (int) inSpinner.getValue();
            int newOut = (int) outSpinner.getValue();

            if (newOut != -1)
            {
                removeAnimationSwap(newOut, previousIn);
                addAnimationSwap(newOut, newIn);
            }

            animationSwap[0] = newOut;
            animationSwap[1] = newIn;
        });

        outSpinner.addChangeListener(e ->
        {
            int previousOut = animationSwap[0];
            int newIn = (int) inSpinner.getValue();
            int newOut = (int) outSpinner.getValue();

            if (newOut == -1)
                removeAnimationSwap(previousOut, newIn);

            if (newOut != -1 && previousOut != -1)
            {
                removeAnimationSwap(previousOut, newIn);
                addAnimationSwap(newOut, newIn);
            }

            if (newOut != -1 && previousOut == -1)
                addAnimationSwap(newOut, newIn);

            animationSwap[0] = newOut;
            animationSwap[1] = newIn;
        });

        animationSwapsPanel.add(swap);
        revalidate();
        repaint();
    }

    private void resetSidePanel()
    {
        poseSpinner.setValue(-1);
        walkSpinner.setValue(-1);
        runSpinner.setValue(-1);
        actionSpinner.setValue(-1);
        backwardsSpinner.setValue(-1);
        rightSpinner.setValue(-1);
        leftSpinner.setValue(-1);
        rotateSpinner.setValue(-1);
    }

    private void clearSwapPanels()
    {
        animationSwaps = new int[0][2];
        animationSwapsPanel.removeAll();
        revalidate();
        repaint();
    }

    private void removeSwapPanel(JPanel panel, int out, int in)
    {
        animationSwapsPanel.remove(panel);
        removeAnimationSwap(out, in);
        revalidate();
        repaint();
    }

    private void removeAnimationSwap(int out, int in)
    {
        int index = -1;
        for (int i = 0; i < animationSwaps.length; i++)
        {
            int[] array = animationSwaps[i];
            if (array[0] == out && array[1] == in)
            {
                index = i;
                break;
            }
        }

        if (index == -1)
            return;

        animationSwaps = ArrayUtils.remove(animationSwaps, index);
    }

    private void addAnimationSwap(int out, int in)
    {
        animationSwaps = ArrayUtils.add(animationSwaps, new int[]{out, in});
    }

    private void openSaveDialog(String name)
    {
        File outputDir = TRANSMOGS_DIR;
        outputDir.mkdirs();

        JFileChooser fileChooser = new JFileChooser(outputDir)
        {
            @Override
            public void approveSelection()
            {
                File f = getSelectedFile();
                if (!f.getName().endsWith(".json"))
                {
                    f = new File(f.getPath() + ".json");
                }
                if (f.exists() && getDialogType() == SAVE_DIALOG)
                {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            "File already exists, overwrite?",
                            "Warning",
                            JOptionPane.YES_NO_CANCEL_OPTION
                    );
                    switch (result)
                    {
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                        case JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }
        };
        fileChooser.setSelectedFile(new File(name));
        fileChooser.setDialogTitle("Save current model collection");

        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().endsWith(".json"))
            {
                selectedFile = new File(selectedFile.getPath() + ".json");
            }
            saveToFile(selectedFile);
        }
    }

    public void saveToFile(File file)
    {
        try {
            FileWriter writer = new FileWriter(file, false);

            CustomModelComp comp = null;
            CustomModel customModel = plugin.getTransmogModel();
            if (customModel != null && customModel.getComp() != null)
                comp = customModel.getComp();

            TransmogSave transmogSave = new TransmogSave(
                    comp,
                    transmogAnimationMode,
                    animationSwaps,
                    poseAnimation,
                    walkAnimation,
                    runAnimation,
                    actionAnimation,
                    backwardsAnimation,
                    rightAnimation,
                    leftAnimation,
                    rotateAnimation,
                    radius);
            String string = plugin.getGson().toJson(transmogSave);
            writer.write(string);
            writer.close();
        }
        catch (IOException e)
        {
            log.debug("Error when saving model to file via TransmogPanel");
        }
    }

    private void openLoadDialog()
    {
        TRANSMOGS_DIR.mkdirs();

        JFileChooser fileChooser = new JFileChooser(TRANSMOGS_DIR);
        fileChooser.setDialogTitle("Choose a transmog to load");

        JComboBox<TransmogLoadOption> comboBox = new JComboBox<>();
        comboBox.setToolTipText("Load the transmog model, animations, or both");
        comboBox.addItem(TransmogLoadOption.BOTH);
        comboBox.addItem(TransmogLoadOption.CUSTOM_MODEL);
        comboBox.addItem(TransmogLoadOption.ANIMATIONS);
        comboBox.setFocusable(false);

        fileChooser.setAccessory(comboBox);

        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileChooser.getSelectedFile();
            plugin.loadTransmog(selectedFile, (TransmogLoadOption) comboBox.getSelectedItem());
        }
    }

    public void loadTransmog(TransmogSave transmogSave)
    {
        transmogAnimationMode = transmogSave.getTransmogAnimationMode();
        poseAnimation = transmogSave.getPoseAnimation();
        walkAnimation = transmogSave.getWalkAnimation();
        runAnimation = transmogSave.getRunAnimation();
        actionAnimation = transmogSave.getActionAnimation();
        backwardsAnimation = transmogSave.getBackwardsAnimation();
        rightAnimation = transmogSave.getRightAnimation();
        leftAnimation = transmogSave.getLeftAnimation();
        rotateAnimation = transmogSave.getRotateAnimation();
        radius = transmogSave.getRadius();

        animationComboBox.setSelectedItem(transmogAnimationMode);
        poseSpinner.setValue(poseAnimation);
        walkSpinner.setValue(walkAnimation);
        runSpinner.setValue(runAnimation);
        actionSpinner.setValue(actionAnimation);
        backwardsSpinner.setValue(backwardsAnimation);
        rightSpinner.setValue(rightAnimation);
        leftSpinner.setValue(leftAnimation);
        rotateSpinner.setValue(rotateAnimation);
        radiusSpinner.setValue(radius);

        clearSwapPanels();
        animationSwaps = transmogSave.getAnimationSwaps();
        if (animationSwaps != null)
        {
            for (int[] swap : animationSwaps)
            {
                addSwapPanel(swap[0], swap[1]);
            }
        }
    }
}
