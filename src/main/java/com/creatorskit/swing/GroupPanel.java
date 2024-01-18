package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class GroupPanel extends JPanel
{
    private CreatorsPlugin plugin;
    private Client client;
    private ClientThread clientThread;
    private final GridBagConstraints c = new GridBagConstraints();

    private final BufferedImage ICON = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private final BufferedImage TRANSLATE = ImageUtil.loadImageResource(getClass(), "/Translate.png");
    private final BufferedImage ROTATE = ImageUtil.loadImageResource(getClass(), "/Rotate.png");
    private final BufferedImage TRANSLATE_SUBTILE = ImageUtil.loadImageResource(getClass(), "/Translate subtile.png");
    private final BufferedImage SCALE = ImageUtil.loadImageResource(getClass(), "/Scale.png");
    private final Dimension SPINNER_DIMENSION = new Dimension(35, 25);
    private final JSpinner groupSpinner = new JSpinner();

    @Inject
    public GroupPanel(@Nullable Client client, CreatorsPlugin plugin, ClientThread clientThread)
    {
        this.plugin = plugin;
        this.clientThread = clientThread;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        JLabel title = new JLabel("Group Transformer");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        JLabel groupLabel = new JLabel("Group:");
        groupLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        add(groupLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(1, 1, 99, 1);
        groupSpinner.setModel(spinnerNumberModel);
        groupSpinner.setBackground(ModelAnvil.getBorderColour(6));
        groupSpinner.setToolTipText("Pick the Group number to Group Transform");
        add(groupSpinner, c);
        groupSpinner.addChangeListener(e ->
                groupSpinner.setBackground(ModelAnvil.getBorderColour((int) groupSpinner.getValue() * 6)));

        JPanel adjustPanel = createAdjustPanel();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        add(adjustPanel, c);

        JPanel setPanel = createSetPanel();
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        add(setPanel, c);

        revalidate();
        repaint();
    }

    public JPanel createAdjustPanel()
    {
        JPanel adjustPanel = new JPanel();
        adjustPanel.setLayout(new GridBagLayout());
        adjustPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        adjustPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JSpinner xTileSpinner = new JSpinner();
        JSpinner yTileSpinner = new JSpinner();
        JSpinner zTileSpinner = new JSpinner();
        JSpinner xSpinner = new JSpinner();
        JSpinner ySpinner = new JSpinner();
        JSpinner zSpinner = new JSpinner();
        JSpinner xScaleSpinner = new JSpinner();
        JSpinner yScaleSpinner = new JSpinner();
        JSpinner zScaleSpinner = new JSpinner();
        JComboBox<String> rotateBox = new JComboBox<>();
        JButton invertButton = new JButton("Invert");

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 4, 2, 4);
        c.weightx = 1;
        c.weighty = 0;

        c.gridwidth = 10;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        JLabel setLabel = new JLabel("Adjuster");
        setLabel.setToolTipText("Functions here adjust all values of the given Group BY the value entered");
        setLabel.setHorizontalAlignment(SwingConstants.CENTER);
        adjustPanel.add(setLabel, c);

        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel tileLabel = new JLabel(new ImageIcon(TRANSLATE));
        tileLabel.setToolTipText("Translate by full tile");
        tileLabel.setBackground(Color.BLACK);
        adjustPanel.add(tileLabel, c);

        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 2;
        JLabel translateLabel = new JLabel(new ImageIcon(TRANSLATE_SUBTILE));
        translateLabel.setToolTipText("Translate by sub-tile (1/128 of a tile)");
        translateLabel.setBackground(Color.BLACK);
        adjustPanel.add(translateLabel, c);

        c.gridx = 2;
        c.gridy = 5;
        c.gridwidth = 2;
        JLabel scaleLabel = new JLabel(new ImageIcon(SCALE));
        scaleLabel.setToolTipText("Scale. 128 is default scale");
        scaleLabel.setBackground(Color.BLACK);
        adjustPanel.add(scaleLabel, c);

        c.gridx = 2;
        c.gridy = 7;
        c.gridwidth = 2;
        JLabel rotateLabel = new JLabel(new ImageIcon(ROTATE));
        rotateLabel.setToolTipText("Rotate");
        rotateLabel.setBackground(Color.BLACK);
        adjustPanel.add(rotateLabel, c);

        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0;
        adjustPanel.add(createXYZPanel(), c);

        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 1;
        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new GridLayout(3, 0, 0, 1));
        adjustPanel.add(tilePanel, c);

        xTileSpinner.setToolTipText("E/W");
        xTileSpinner.setPreferredSize(SPINNER_DIMENSION);
        tilePanel.add(xTileSpinner);
        xTileSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(xTileSpinner, this, GroupOperation.X_TILE_SPINNER);
            }
        });

        yTileSpinner.setToolTipText("N/S");
        tilePanel.add(yTileSpinner);
        yTileSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(yTileSpinner, this, GroupOperation.Y_TILE_SPINNER);
            }
        });

        zTileSpinner.setToolTipText("U/D");
        tilePanel.add(zTileSpinner);
        zTileSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(zTileSpinner, this, GroupOperation.Z_TILE_SPINNER);
            }
        });

        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 0;
        adjustPanel.add(createXYZPanel(), c);

        c.gridx = 3;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 1;
        JPanel translatePanel = new JPanel();
        translatePanel.setLayout(new GridLayout(3, 0, 0, 1));
        adjustPanel.add(translatePanel, c);

        xSpinner.setToolTipText("E/W");
        xSpinner.setPreferredSize(SPINNER_DIMENSION);
        translatePanel.add(xSpinner);
        xSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(xSpinner, this, GroupOperation.X_SPINNER);
            }
        });

        ySpinner.setToolTipText("N/S");
        translatePanel.add(ySpinner);
        ySpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(ySpinner, this, GroupOperation.Y_SPINNER);
            }
        });

        zSpinner.setToolTipText("U/D");
        translatePanel.add(zSpinner);
        zSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(zSpinner, this, GroupOperation.Z_SPINNER);
            }
        });

        c.gridx = 2;
        c.gridy = 6;
        c.gridwidth = 1;
        c.weightx = 0;
        adjustPanel.add(createXYZPanel(), c);

        c.gridx = 3;
        c.gridy = 6;
        c.gridwidth = 2;
        c.weightx = 0;
        JPanel scalePanel = new JPanel();
        scalePanel.setLayout(new GridLayout(3, 0, 0, 1));
        adjustPanel.add(scalePanel, c);

        xScaleSpinner.setPreferredSize(SPINNER_DIMENSION);
        xScaleSpinner.setToolTipText("E/W");
        scalePanel.add(xScaleSpinner);
        xScaleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(xScaleSpinner, this, GroupOperation.X_SCALE_SPINNER);
            }
        });

        yScaleSpinner.setToolTipText("N/S");
        scalePanel.add(yScaleSpinner);
        yScaleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(yScaleSpinner, this, GroupOperation.Y_SCALE_SPINNER);
            }
        });

        zScaleSpinner.setToolTipText("U/D");
        scalePanel.add(zScaleSpinner);
        zScaleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                adjustValueSpinner(zScaleSpinner, this, GroupOperation.Z_SCALE_SPINNER);
            }
        });

        c.gridx = 2;
        c.gridy = 8;
        c.gridwidth = 2;
        adjustPanel.add(rotateBox, c);
        rotateBox.addItem("Rot");
        rotateBox.addItem("90");
        rotateBox.addItem("180");
        rotateBox.addItem("270");
        rotateBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                adjustValueComboBox(rotateBox, e, this);
            }
        });

        c.gridx = 2;
        c.gridy = 9;
        c.gridwidth = 2;
        adjustPanel.add(invertButton, c);
        invertButton.addActionListener(e ->
        {
            adjustValueInvert();
        });

        JFrame swapperFrame = new JFrame("Colour Swapper");
        swapperFrame.setVisible(false);
        swapperFrame.setEnabled(false);
        swapperFrame.setIconImage(ICON);
        swapperFrame.setLayout(new FlowLayout());

        JPanel gridMenu = new JPanel();
        gridMenu.setLayout(new GridLayout(0, 2, 2, 2));
        swapperFrame.add(gridMenu);

        adjustPanel.setEnabled(true);
        adjustPanel.setVisible(true);
        return adjustPanel;
    }

    private void adjustValueSpinner(JSpinner spinner, ChangeListener changeListener, GroupOperation operation)
    {
        int value = (int) spinner.getValue();

        for (ComplexPanel complexPanel : plugin.getComplexPanels())
        {
            if (complexPanel.getGroupSpinner().getValue() != groupSpinner.getValue())
                continue;

            JSpinner s;
            switch (operation)
            {
                case X_TILE_SPINNER:
                    s = complexPanel.getXTileSpinner();
                    s.setValue((int) s.getValue() + value);
                    break;
                case Y_TILE_SPINNER:
                    s = complexPanel.getYTileSpinner();
                    s.setValue((int) s.getValue() + value);
                    break;
                case Z_TILE_SPINNER:
                    s = complexPanel.getZTileSpinner();
                    s.setValue((int) s.getValue() + value);
                    break;
                case X_SPINNER:
                    s = complexPanel.getXSpinner();
                    s.setValue((int) s.getValue() + value);
                    break;
                case Y_SPINNER:
                    s = complexPanel.getYSpinner();
                    s.setValue((int) s.getValue() + value);
                    break;
                case Z_SPINNER:
                    s = complexPanel.getZSpinner();
                    s.setValue((int) s.getValue() + value);
                    break;
                case X_SCALE_SPINNER:
                    s = complexPanel.getXScaleSpinner();
                    s.setValue((int) s.getValue() + value);
                    break;
                case Y_SCALE_SPINNER:
                    s = complexPanel.getYScaleSpinner();
                    s.setValue((int) s.getValue() + value);
                    break;
                case Z_SCALE_SPINNER:
                    s = complexPanel.getZScaleSpinner();
                    s.setValue((int) s.getValue() + value);
            }
        }

        spinner.removeChangeListener(changeListener);
        spinner.setValue(0);
        spinner.addChangeListener(changeListener);
    }

    private void adjustValueComboBox(JComboBox<String> comboBox, ItemEvent e, ItemListener itemListener)
    {
        if (e.getItem() == "Rot")
            return;

        int change = Integer.parseInt((String) e.getItem());

        for (ComplexPanel complexPanel : plugin.getComplexPanels())
        {
            if (complexPanel.getGroupSpinner().getValue() == groupSpinner.getValue())
            {
                JCheckBox check90 = complexPanel.getCheck90();
                JCheckBox check180 = complexPanel.getCheck180();
                JCheckBox check270 = complexPanel.getCheck270();

                int currentRotation = 0;
                if (check90.isSelected())
                    currentRotation = 90;

                if (check180.isSelected())
                    currentRotation = 180;

                if (check270.isSelected())
                    currentRotation = 270;

                switch (currentRotation + change)
                {
                    case 90:
                    case 450:
                        complexPanel.getCheck90().setSelected(true);
                        complexPanel.getCheck180().setSelected(false);
                        complexPanel.getCheck270().setSelected(false);
                        break;
                    case 180:
                    case 540:
                        complexPanel.getCheck90().setSelected(false);
                        complexPanel.getCheck180().setSelected(true);
                        complexPanel.getCheck270().setSelected(false);
                        break;
                    case 270:
                        complexPanel.getCheck90().setSelected(false);
                        complexPanel.getCheck180().setSelected(false);
                        complexPanel.getCheck270().setSelected(true);
                        break;
                    case 360:
                        complexPanel.getCheck90().setSelected(false);
                        complexPanel.getCheck180().setSelected(false);
                        complexPanel.getCheck270().setSelected(false);
                }
            }
        }

        comboBox.removeItemListener(itemListener);
        comboBox.setSelectedItem("Rot");
        comboBox.addItemListener(itemListener);
    }

    private void adjustValueInvert()
    {
        for (ComplexPanel complexPanel : plugin.getComplexPanels())
        {
            if (complexPanel.getGroupSpinner().getValue() == groupSpinner.getValue())
            {
                JCheckBox invertFaces = complexPanel.getInvertFaces();
                invertFaces.setSelected(!invertFaces.isSelected());
            }
        }
    }

    public JPanel createSetPanel()
    {
        JPanel setPanel = new JPanel();
        setPanel.setLayout(new GridBagLayout());
        setPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        setPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JTextField colourNewField = new JTextField();
        JTextField colourOldField = new JTextField();
        JSpinner xTileSpinner = new JSpinner();
        JSpinner yTileSpinner = new JSpinner();
        JSpinner zTileSpinner = new JSpinner();
        JSpinner xSpinner = new JSpinner();
        JSpinner ySpinner = new JSpinner();
        JSpinner zSpinner = new JSpinner();
        JSpinner xScaleSpinner = new JSpinner();
        JSpinner yScaleSpinner = new JSpinner();
        JSpinner zScaleSpinner = new JSpinner();
        JComboBox<String> rotateBox = new JComboBox<>();
        JButton invertButton = new JButton("Inv On");

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 4, 2, 4);
        c.weightx = 1;
        c.weighty = 0;

        c.gridwidth = 10;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        JLabel setLabel = new JLabel("Setter");
        setLabel.setToolTipText("Functions here set all values of the given Group TO the value entered");
        setLabel.setHorizontalAlignment(SwingConstants.CENTER);
        setPanel.add(setLabel, c);

        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 2;
        JLabel tileLabel = new JLabel(new ImageIcon(TRANSLATE));
        tileLabel.setToolTipText("Translate by full tile");
        tileLabel.setBackground(Color.BLACK);
        setPanel.add(tileLabel, c);

        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 2;
        JLabel translateLabel = new JLabel(new ImageIcon(TRANSLATE_SUBTILE));
        translateLabel.setToolTipText("Translate by sub-tile (1/128 of a tile)");
        translateLabel.setBackground(Color.BLACK);
        setPanel.add(translateLabel, c);

        c.gridx = 2;
        c.gridy = 5;
        c.gridwidth = 2;
        JLabel scaleLabel = new JLabel(new ImageIcon(SCALE));
        scaleLabel.setToolTipText("Scale. 128 is default scale");
        scaleLabel.setBackground(Color.BLACK);
        setPanel.add(scaleLabel, c);

        c.gridx = 2;
        c.gridy = 7;
        c.gridwidth = 2;
        JLabel rotateLabel = new JLabel(new ImageIcon(ROTATE));
        rotateLabel.setToolTipText("Rotate");
        rotateLabel.setBackground(Color.BLACK);
        setPanel.add(rotateLabel, c);

        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0;
        setPanel.add(createXYZPanel(), c);

        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0;
        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new GridLayout(3, 0, 0, 1));
        setPanel.add(tilePanel, c);

        xTileSpinner.setToolTipText("E/W");
        xTileSpinner.setPreferredSize(SPINNER_DIMENSION);
        xTileSpinner.setUI(createEmptySpinner());
        tilePanel.add(xTileSpinner);
        xTileSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(xTileSpinner, this, GroupOperation.X_TILE_SPINNER);
            }
        });

        yTileSpinner.setToolTipText("N/S");
        yTileSpinner.setUI(createEmptySpinner());
        tilePanel.add(yTileSpinner);
        yTileSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(yTileSpinner, this, GroupOperation.Y_TILE_SPINNER);
            }
        });

        zTileSpinner.setToolTipText("U/D");
        zTileSpinner.setUI(createEmptySpinner());
        tilePanel.add(zTileSpinner);
        zTileSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(zTileSpinner, this, GroupOperation.Z_TILE_SPINNER);
            }
        });

        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 0;
        setPanel.add(createXYZPanel(), c);

        c.gridx = 3;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 1;
        JPanel translatePanel = new JPanel();
        translatePanel.setLayout(new GridLayout(3, 0, 0, 1));
        setPanel.add(translatePanel, c);

        xSpinner.setToolTipText("E/W");
        xSpinner.setPreferredSize(SPINNER_DIMENSION);
        xSpinner.setUI(createEmptySpinner());
        translatePanel.add(xSpinner);
        xSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(xSpinner, this, GroupOperation.X_SPINNER);
            }
        });

        ySpinner.setToolTipText("N/S");
        ySpinner.setUI(createEmptySpinner());
        translatePanel.add(ySpinner);
        ySpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(ySpinner, this, GroupOperation.Y_SPINNER);
            }
        });

        zSpinner.setToolTipText("U/D");
        zSpinner.setUI(createEmptySpinner());
        translatePanel.add(zSpinner);
        zSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(zSpinner, this, GroupOperation.Z_SPINNER);
            }
        });

        c.gridx = 2;
        c.gridy = 6;
        c.gridwidth = 1;
        c.weightx = 0;
        setPanel.add(createXYZPanel(), c);

        c.gridx = 3;
        c.gridy = 6;
        c.gridwidth = 1;
        c.weightx = 0;
        JPanel scalePanel = new JPanel();
        scalePanel.setLayout(new GridLayout(3, 0, 0, 1));
        setPanel.add(scalePanel, c);

        xScaleSpinner.setPreferredSize(SPINNER_DIMENSION);
        xScaleSpinner.setToolTipText("E/W");
        xScaleSpinner.setUI(createEmptySpinner());
        scalePanel.add(xScaleSpinner);
        xScaleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(xScaleSpinner, this, GroupOperation.X_SCALE_SPINNER);
            }
        });

        yScaleSpinner.setToolTipText("N/S");
        yScaleSpinner.setUI(createEmptySpinner());
        scalePanel.add(yScaleSpinner);
        yScaleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(yScaleSpinner, this, GroupOperation.Y_SCALE_SPINNER);
            }
        });

        zScaleSpinner.setToolTipText("U/D");
        zScaleSpinner.setUI(createEmptySpinner());
        scalePanel.add(zScaleSpinner);
        zScaleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setValueSpinner(zScaleSpinner, this, GroupOperation.Z_SCALE_SPINNER);
            }
        });

        c.gridx = 2;
        c.gridy = 8;
        c.gridwidth = 2;
        setPanel.add(rotateBox, c);
        rotateBox.addItem("Rot");
        rotateBox.addItem("0");
        rotateBox.addItem("90");
        rotateBox.addItem("180");
        rotateBox.addItem("270");
        rotateBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setValueComboBox(rotateBox, e, this);
            }
        });

        c.gridx = 2;
        c.gridy = 9;
        c.gridwidth = 2;
        setPanel.add(invertButton, c);
        invertButton.addActionListener(e ->
        {
            setValueInvert(invertButton);
        });

        HashMap<Short, Short> colourMap = new HashMap<>();

        colourNewField.setVisible(false);
        setPanel.add(colourNewField);

        colourOldField.setVisible(false);
        setPanel.add(colourOldField);

        JFrame swapperFrame = new JFrame("Colour Swapper");
        swapperFrame.setVisible(false);
        swapperFrame.setEnabled(false);
        swapperFrame.setIconImage(ICON);
        swapperFrame.setLayout(new FlowLayout());

        JPanel gridMenu = new JPanel();
        gridMenu.setLayout(new GridLayout(0, 2, 2, 2));
        swapperFrame.add(gridMenu);

        setPanel.setEnabled(true);
        setPanel.setVisible(true);
        return setPanel;
    }

    private void setValueSpinner(JSpinner spinner, ChangeListener changeListener, GroupOperation operation)
    {
        int value = (int) spinner.getValue();

        for (ComplexPanel complexPanel : plugin.getComplexPanels())
        {
            if (complexPanel.getGroupSpinner().getValue() != groupSpinner.getValue())
                continue;

            switch (operation)
            {
                case X_TILE_SPINNER:
                    complexPanel.getXTileSpinner().setValue(value);
                    break;
                case Y_TILE_SPINNER:
                    complexPanel.getYTileSpinner().setValue(value);
                    break;
                case Z_TILE_SPINNER:
                    complexPanel.getZTileSpinner().setValue(value);
                    break;
                case X_SPINNER:
                    complexPanel.getXSpinner().setValue(value);
                    break;
                case Y_SPINNER:
                    complexPanel.getYSpinner().setValue(value);
                    break;
                case Z_SPINNER:
                    complexPanel.getZSpinner().setValue(value);
                    break;
                case X_SCALE_SPINNER:
                    complexPanel.getXScaleSpinner().setValue(value);
                    break;
                case Y_SCALE_SPINNER:
                    complexPanel.getYScaleSpinner().setValue(value);
                    break;
                case Z_SCALE_SPINNER:
                    complexPanel.getZScaleSpinner().setValue(value);
            }
        }
    }

    private void setValueComboBox(JComboBox<String> comboBox, ItemEvent e, ItemListener itemListener)
    {
        if (e.getItem() == "Rot")
            return;

        for (ComplexPanel complexPanel : plugin.getComplexPanels())
        {
            if (complexPanel.getGroupSpinner().getValue() == groupSpinner.getValue())
            {
                switch (Integer.parseInt((String) e.getItem()))
                {
                    case 0:
                        complexPanel.getCheck90().setSelected(false);
                        complexPanel.getCheck180().setSelected(false);
                        complexPanel.getCheck270().setSelected(false);
                        break;
                    case 90:
                        complexPanel.getCheck90().setSelected(true);
                        complexPanel.getCheck180().setSelected(false);
                        complexPanel.getCheck270().setSelected(false);
                        break;
                    case 180:
                        complexPanel.getCheck90().setSelected(false);
                        complexPanel.getCheck180().setSelected(true);
                        complexPanel.getCheck270().setSelected(false);
                        break;
                    case 270:
                        complexPanel.getCheck90().setSelected(false);
                        complexPanel.getCheck180().setSelected(false);
                        complexPanel.getCheck270().setSelected(true);
                }
            }
        }

        comboBox.removeItemListener(itemListener);
        comboBox.setSelectedItem("Rot");
        comboBox.addItemListener(itemListener);
    }

    private void setValueInvert(JButton button)
    {
        boolean invert = button.getText().equals("Inv On");
        for (ComplexPanel complexPanel : plugin.getComplexPanels())
        {
            if (complexPanel.getGroupSpinner().getValue() == groupSpinner.getValue())
            {
                JCheckBox invertFaces = complexPanel.getInvertFaces();
                invertFaces.setSelected(invert);
            }
        }

        button.setText(invert ? "Inv Off" : "Inv On");
    }

    private JPanel createXYZPanel()
    {
        JPanel xyzPanel = new JPanel();
        xyzPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        xyzPanel.setLayout(new GridLayout(3, 0));

        JLabel xLabel = new JLabel("x:");
        xLabel.setToolTipText("East/West");
        xLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(xLabel);

        JLabel yLabel = new JLabel("y:");
        yLabel.setToolTipText("North/South");
        yLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(yLabel);

        JLabel zLabel = new JLabel("z:");
        zLabel.setToolTipText("Up/Down");
        zLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xyzPanel.add(zLabel);

        return xyzPanel;
    }

    private BasicSpinnerUI createEmptySpinner() {
        return new BasicSpinnerUI() {
            protected Component createNextButton() {
                return null;
            }

            protected Component createPreviousButton() {
                return null;
            }
        };
    }
}
