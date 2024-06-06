package com.creatorskit.swing.colours;

import com.creatorskit.swing.ComplexPanel;
import com.creatorskit.swing.CreatorsPanel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.ModelData;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ColourSwapPanel extends JPanel
{
    private final Client client;
    private final ClientThread clientThread;
    private final ArrayList<ComplexPanel> complexPanels;
    private final GridBagConstraints c = new GridBagConstraints();
    private final Dimension LABEL_DIMENSION = new Dimension(250, 30);
    private final Dimension TOP_BAR_DIMENSION = new Dimension(100, 25);
    private final BufferedImage COPY_COLOURS = ImageUtil.loadImageResource(getClass(), "/Copy_Colours.png");
    private final BufferedImage PASTE_COLOURS = ImageUtil.loadImageResource(getClass(), "/Paste_Colours.png");
    private final JColorChooser colourChooser = new JColorChooser();
    @Getter
    private final JComboBox<ComplexPanel> comboBox = new JComboBox<>();
    @Getter
    private ColourPanel[] colourPanels = new ColourPanel[0];
    @Getter
    private TexturePanel[] texturePanels = new TexturePanel[0];
    @Getter
    private final JPanel colourHolder = new JPanel();
    @Getter
    private final JPanel textureHolder = new JPanel();
    @Getter
    @Setter
    private ComplexPanel currentComplexPanel;
    @Getter
    @Setter
    private short[][] copiedColoursTextures = new short[4][0];

    @Inject
    public ColourSwapPanel(Client client, ClientThread clientThread, ArrayList<ComplexPanel> complexPanels)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.complexPanels = complexPanels;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(2, 2, 2, 2));

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);

        c.gridwidth = 1;
        c.weightx = 6;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel comboPane = new JPanel();
        comboPane.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        add(comboPane, c);

        JLabel swapLabel = new JLabel("Model:  ", SwingConstants.RIGHT);
        swapLabel.setFont(FontManager.getRunescapeBoldFont());
        swapLabel.setPreferredSize(TOP_BAR_DIMENSION);
        comboPane.add(swapLabel);

        comboBox.addActionListener(e -> onSwapperPressed((ComplexPanel) comboBox.getSelectedItem()));
        comboBox.setPreferredSize(new Dimension(250, 25));
        comboPane.add(comboBox);

        JButton copyButton = new JButton(new ImageIcon(COPY_COLOURS));
        copyButton.setToolTipText("Copy all swapped colours & textures");
        copyButton.setPreferredSize(TOP_BAR_DIMENSION);
        copyButton.addActionListener(e -> copyColoursTextures(currentComplexPanel));
        comboPane.add(copyButton);

        JButton pasteButton = new JButton(new ImageIcon(PASTE_COLOURS));
        pasteButton.setToolTipText("Paste all copied colours & textures");
        pasteButton.setPreferredSize(TOP_BAR_DIMENSION);
        pasteButton.addActionListener(e -> pasteColoursTextures(currentComplexPanel));
        comboPane.add(pasteButton);

        JButton clearEverythingButton = new JButton("Clear Everything");
        clearEverythingButton.setToolTipText("Clears every swapped colour & texture for every model in the Anvil");
        clearEverythingButton.setPreferredSize(new Dimension(130, 25));
        clearEverythingButton.addActionListener(e -> clearColoursEverything());
        comboPane.add(clearEverythingButton);

        JButton setAllModelColours = new JButton("Set Everything");
        setAllModelColours.setToolTipText("Sets colours for every model in the Anvil to the colour currently selected in the Colour Picker");
        setAllModelColours.setPreferredSize(new Dimension(120, 25));
        setAllModelColours.addActionListener(e -> setColoursEverything());
        comboPane.add(setAllModelColours);

        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 0;
        JPanel previewPanel = new JPanel();
        previewPanel.setBackground(colourChooser.getColor());
        add(previewPanel, c);

        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 1;
        AbstractColorChooserPanel[] chooserPanels = {colourChooser.getChooserPanels()[1]};
        colourChooser.setChooserPanels(chooserPanels);
        colourChooser.setPreviewPanel(new JPanel());
        colourChooser.setBorder(new LineBorder(colourChooser.getColor(), 2));
        colourChooser.getSelectionModel().addChangeListener(e ->
        {
            colourChooser.setBorder(new LineBorder(colourChooser.getColor(), 2));
            previewPanel.setBackground(colourChooser.getColor());
        });
        add(colourChooser, c);

        c.weighty = 1;
        c.gridx = 1;
        c.gridy = 2;
        JLabel emptyLabel = new JLabel("");
        add(emptyLabel, c);

        c.gridheight = 2;
        c.weightx = 4;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane();
        add(scrollPane, c);

        JPanel swapPanel = new JPanel();
        swapPanel.setLayout(new GridBagLayout());
        swapPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        scrollPane.setViewportView(swapPanel);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel textureLabel = new JLabel("Textures", SwingConstants.CENTER);
        textureLabel.setPreferredSize(LABEL_DIMENSION);
        textureLabel.setFont(FontManager.getRunescapeBoldFont());
        swapPanel.add(textureLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        JLabel colourLabel = new JLabel("Colours", SwingConstants.CENTER);
        colourLabel.setPreferredSize(LABEL_DIMENSION);
        colourLabel.setFont(FontManager.getRunescapeBoldFont());
        swapPanel.add(colourLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        JPanel textureHeader = new JPanel();
        textureHeader.setLayout(new GridLayout(1, 0, 1, 1));
        swapPanel.add(textureHeader, c);

        JLabel oldTextureLabel = new JLabel("Old", SwingConstants.CENTER);
        oldTextureLabel.setFont(FontManager.getRunescapeBoldFont());
        textureHeader.add(oldTextureLabel);

        JLabel newTextureLabel = new JLabel("New", SwingConstants.CENTER);
        newTextureLabel.setFont(FontManager.getRunescapeBoldFont());
        textureHeader.add(newTextureLabel);

        JButton clearTextures = new JButton("Clear");
        clearTextures.setToolTipText("Clear all swapped textures back to default");
        clearTextures.addActionListener(e -> {
            unsetAllTextures();
            removeAllTextureSwaps();
            repaint();
            revalidate();
        });
        textureHeader.add(clearTextures);

        c.gridx = 1;
        c.gridy = 1;
        JPanel colourHeader = new JPanel();
        colourHeader.setLayout(new GridLayout(2, 0, 1, 1));
        swapPanel.add(colourHeader, c);

        JLabel oldColourLabel = new JLabel("Old", SwingConstants.CENTER);
        oldColourLabel.setFont(FontManager.getRunescapeBoldFont());
        colourHeader.add(oldColourLabel);

        JLabel newColourLabel = new JLabel("New", SwingConstants.CENTER);
        newColourLabel.setFont(FontManager.getRunescapeBoldFont());
        colourHeader.add(newColourLabel);

        JButton clearColours = new JButton("Clear");
        clearColours.setToolTipText("Clear all swapped colours back to default");
        clearColours.addActionListener(e -> {
            unsetAllColours();
            removeAllColourSwaps();
            repaint();
            revalidate();
        });
        colourHeader.add(clearColours);

        colourHeader.add(new JLabel(""));
        colourHeader.add(new JLabel(""));

        JButton setAllButton = new JButton("Set All");
        setAllButton.setToolTipText("Sets all colours for this model to the colour currently selected in the Colour Picker");
        setAllButton.addActionListener(e -> {
            setAllColoursHere();
            repaint();
            revalidate();
        });
        colourHeader.add(setAllButton);

        c.gridx = 0;
        c.gridy = 2;
        textureHolder.setLayout(new GridLayout(0, 1, 1, 2));
        textureHolder.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        swapPanel.add(textureHolder, c);

        c.gridx = 1;
        c.gridy = 2;
        colourHolder.setLayout(new GridLayout(0, 1, 1, 2));
        colourHolder.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        swapPanel.add(colourHolder, c);

        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel emptyLabel3 = new JLabel("");
        swapPanel.add(emptyLabel3, c);

        repaint();
        revalidate();
    }

    public void addComplexPanelOption(ComplexPanel complexPanel)
    {
        comboBox.addItem(complexPanel);
    }

    public void removeAllComplexPanelOptions()
    {
        comboBox.removeAllItems();
        unsetSwapper();
        repaint();
        revalidate();
    }

    public void removeComplexPanelOption(ComplexPanel complexPanel)
    {
        if (comboBox.getSelectedItem() == complexPanel)
        {
            unsetSwapper();
            repaint();
            revalidate();
        }

        comboBox.removeItem(complexPanel);
    }

    public void setComboBox(ComplexPanel complexPanel)
    {
        comboBox.setSelectedItem(complexPanel);
    }

    public void onSwapperPressed(ComplexPanel complexPanel)
    {
        unsetSwapper();
        if (complexPanel == null)
        {
            repaint();
            revalidate();
            return;
        }

        int modelId = (int) complexPanel.getModelIdSpinner().getValue();
        if (modelId == -1)
        {
            repaint();
            revalidate();
            return;
        }

        setupSwapper(complexPanel);
    }

    public void setupSwapper(ComplexPanel complexPanel)
    {
        int modelId = (int) complexPanel.getModelIdSpinner().getValue();

        currentComplexPanel = complexPanel;

        HashMap<Short, Short> colourMap = new HashMap<>();
        for (int i = 0; i < complexPanel.getColoursFrom().length; i++)
        {
            colourMap.put(complexPanel.getColoursFrom()[i], complexPanel.getColoursTo()[i]);
        }

        HashMap<Short, Short> textureMap = new HashMap<>();
        for (int i = 0; i < complexPanel.getTexturesFrom().length; i++)
        {
            textureMap.put(complexPanel.getTexturesFrom()[i], complexPanel.getTexturesTo()[i]);
        }

        clientThread.invokeLater(() ->
        {
            ModelData modelData = client.loadModelData(modelId);
            if (modelData == null)
                return;

            short[] colourList = new short[0];
            for (short s : modelData.getFaceColors())
            {
                if (!ArrayUtils.contains(colourList, s))
                    colourList = ArrayUtils.add(colourList, s);
            }

            for (short colour : colourList)
            {
                if (colourMap.containsKey(colour))
                {
                    createColourPanel(colour, colourMap.get(colour), true);
                    continue;
                }

                createColourPanel(colour, colour, false);
            }

            try
            {
                modelData.cloneTextures();
            }
            catch (Exception e)
            {
                repaint();
                revalidate();
                return;
            }

            short[] textureList = new short[0];
            for (short s : modelData.getFaceTextures())
            {
                if (!ArrayUtils.contains(textureList, s))
                    textureList = ArrayUtils.add(textureList, s);
            }

            for (short texture : textureList)
            {
                if (textureMap.containsKey(texture))
                {
                    createTexturePanel(texture, textureMap.get(texture), true);
                    continue;
                }

                createTexturePanel(texture, texture, false);
            }

            repaint();
            revalidate();
        });
    }

    public void unsetSwapper()
    {
        colourHolder.removeAll();
        colourPanels = new ColourPanel[0];

        textureHolder.removeAll();
        texturePanels = new TexturePanel[0];

        currentComplexPanel = null;
    }

    private ColourPanel createColourPanel(short oldColour, short newColour, boolean isColourSet)
    {
        JLabel oldColourLabel = new JLabel("" + oldColour, SwingConstants.CENTER);
        oldColourLabel.setFont(FontManager.getRunescapeSmallFont());
        oldColourLabel.setBorder(new LineBorder(colourFromShort(oldColour), 10));
        oldColourLabel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton newColourButton = new JButton("" + (isColourSet ? newColour : "Set"));
        newColourButton.setToolTipText("Swap this Old colour to the colour currently selected in the Colour Picker");
        newColourButton.setFont(FontManager.getRunescapeSmallFont());
        newColourButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        if (isColourSet)
            newColourButton.setBorder(new LineBorder(colourFromShort(newColour), 10));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1, 0, 1));

        JButton findButton = new JButton("Find");
        findButton.setToolTipText("Find the current New colour swap in the colour picker");
        buttonPanel.add(findButton);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -32768, 32767, 1));
        spinner.setToolTipText("Apply an exact Jagex colour as it is stored in the cache");
        buttonPanel.add(spinner);

        ColourPanel colourPanel = new ColourPanel(isColourSet, oldColour, newColour, oldColourLabel, newColourButton, spinner);
        colourPanel.setLayout(new GridLayout(1, 0, 1, 1));
        colourPanel.add(oldColourLabel);
        colourPanel.add(newColourButton);
        colourPanel.add(buttonPanel);

        spinner.addChangeListener(e ->
        {
            short colourToAdd = (short) ((int) spinner.getValue());
            if (colourPanel.isColourSet())
            {
                replaceColour(colourPanel, colourToAdd);
                replaceColourSwap(colourPanel.getOldColour(), colourToAdd);
                return;
            }

            setColour(colourPanel, colourToAdd);
            addColourSwap(colourPanel.getOldColour(), colourToAdd);
        });
        findButton.addActionListener(e -> colourChooser.setColor(colourFromShort(colourPanel.getNewColour())));
        newColourButton.addActionListener(e ->
        {
            if (colourPanel.isColourSet())
            {
                unsetColour(colourPanel);
                removeColourSwap(colourPanel.getOldColour());
                return;
            }

            setColour(colourPanel, colourChooser.getColor());
            addColourSwap(colourPanel.getOldColour(), colourPanel.getNewColour());
        });

        colourHolder.add(colourPanel);
        colourPanels = ArrayUtils.add(colourPanels, colourPanel);
        return colourPanel;
    }

    private void setAllColoursHere()
    {
        short colour = shortFromColour(colourChooser.getColor());

        for (ColourPanel colourPanel : colourPanels)
        {
            if (colourPanel.isColourSet())
            {
                replaceColour(colourPanel, colour);
                replaceColourSwap(colourPanel.getOldColour(), colour);
                continue;
            }

            setColour(colourPanel, colour);
            addColourSwap(colourPanel.getOldColour(), colourPanel.getNewColour());
        }
    }

    public void setColoursEverything()
    {
        short colour = shortFromColour(colourChooser.getColor());
        clientThread.invokeLater(() ->
        {
            for (ComplexPanel complexPanel : complexPanels)
            {
                int modelId = (int) complexPanel.getModelIdSpinner().getValue();
                if (modelId == -1)
                {
                    continue;
                }

                ModelData modelData = client.loadModelData(modelId);
                if (modelData == null)
                    continue;

                short[] coloursFrom = new short[0];
                for (short s : modelData.getFaceColors())
                {
                    if (!ArrayUtils.contains(coloursFrom, s))
                    {
                        coloursFrom = ArrayUtils.add(coloursFrom, s);
                    }
                }

                short[] coloursTo = new short[coloursFrom.length];
                Arrays.fill(coloursTo, colour);
                complexPanel.setColoursFrom(coloursFrom);
                complexPanel.setColoursTo(coloursTo);
            }

            onSwapperPressed((ComplexPanel) comboBox.getSelectedItem());
        });
    }

    private void clearColoursEverything()
    {
        for (ComplexPanel complexPanel : complexPanels)
        {
            if ((int) complexPanel.getModelIdSpinner().getValue() == -1)
            {
                continue;
            }

            complexPanel.setColoursFrom(new short[0]);
            complexPanel.setColoursTo(new short[0]);
        }

        onSwapperPressed((ComplexPanel) comboBox.getSelectedItem());
    }

    private void setColour(ColourPanel colourPanel, Color rgbColour)
    {
        setColour(colourPanel, shortFromColour(rgbColour));
    }

    private void setColour(ColourPanel colourPanel, short colour)
    {
        JButton newColourButton = colourPanel.getNewColourButton();
        newColourButton.setText("" + colour);
        newColourButton.setBorder(new LineBorder(colourFromShort(colour), 10));
        colourPanel.setNewColour(colour);
        colourPanel.setColourSet(true);
    }

    private void unsetColour(ColourPanel colourPanel)
    {
        JButton newColourButton = colourPanel.getNewColourButton();
        newColourButton.setText("Set");
        newColourButton.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 2));
        colourPanel.setNewColour(colourPanel.getOldColour());
        colourPanel.setColourSet(false);
    }

    private void replaceColour(ColourPanel colourPanel, short newColourTo)
    {
        JButton newColourButton = colourPanel.getNewColourButton();
        newColourButton.setText("" + newColourTo);
        newColourButton.setBorder(new LineBorder(colourFromShort(newColourTo), 10));
        colourPanel.setNewColour(newColourTo);
    }

    public void unsetAllColours()
    {
        for (ColourPanel colourPanel : colourPanels)
            unsetColour(colourPanel);

        removeAllColourSwaps();
    }

    private void addColourSwap(short colourFrom, short colourTo)
    {
        currentComplexPanel.setColoursFrom(ArrayUtils.add(currentComplexPanel.getColoursFrom(), colourFrom));
        currentComplexPanel.setColoursTo(ArrayUtils.add(currentComplexPanel.getColoursTo(), colourTo));
    }

    public void pasteColourSwaps(ComplexPanel complexPanel, short[] newColoursFrom, short[] newColoursTo)
    {
        short[] coloursFrom = complexPanel.getColoursFrom();
        short[] coloursTo = complexPanel.getColoursTo();

        for (int i = 0; i < newColoursFrom.length; i++)
        {
            boolean contains = false;
            for (int e = 0; e < coloursFrom.length; e++)
            {
                if (coloursFrom[e] == newColoursFrom[i])
                {
                    coloursTo[e] = newColoursTo[i];
                    contains = true;
                    break;
                }
            }

            if (contains)
                continue;

            coloursFrom = ArrayUtils.add(coloursFrom, newColoursFrom[i]);
            coloursTo = ArrayUtils.add(coloursTo, newColoursTo[i]);
        }

        complexPanel.setColoursFrom(coloursFrom);
        complexPanel.setColoursTo(coloursTo);
    }

    private void removeColourSwap(short colourFrom)
    {
        short[] coloursFrom = currentComplexPanel.getColoursFrom();
        short[] coloursTo = currentComplexPanel.getColoursTo();

        int index = ArrayUtils.indexOf(coloursFrom, colourFrom);
        currentComplexPanel.setColoursFrom(ArrayUtils.remove(coloursFrom, index));
        currentComplexPanel.setColoursTo(ArrayUtils.remove(coloursTo, index));
    }

    private void replaceColourSwap(short colourFrom, short newColourTo)
    {
        short[] coloursFrom = currentComplexPanel.getColoursFrom();
        short[] coloursTo = currentComplexPanel.getColoursTo();

        int index = ArrayUtils.indexOf(coloursFrom, colourFrom);
        coloursTo[index] = newColourTo;
    }

    public void removeAllColourSwaps()
    {
        currentComplexPanel.setColoursFrom(new short[0]);
        currentComplexPanel.setColoursTo(new short[0]);
    }

    public static Color colourFromShort(short s)
    {
        float hue = ((float) JagexColor.unpackHue(s) / JagexColor.HUE_MAX) * 360;
        float sat = ((float) JagexColor.unpackSaturation(s) / JagexColor.SATURATION_MAX) * 100;
        float lum = ((float) JagexColor.unpackLuminance(s) / JagexColor.LUMINANCE_MAX) * 100;
        return HSLColor.toRGB(hue, sat, lum);
    }

    public static short shortFromColour(Color color)
    {
        float[] col = HSLColor.fromRGB(color);
        int hue = (int) (col[0] / 360 * JagexColor.HUE_MAX);
        int sat = (int) (col[1] / 100 * JagexColor.SATURATION_MAX);
        int lum = (int) (col[2] / 100 * JagexColor.LUMINANCE_MAX);
        return JagexColor.packHSL(hue, sat, lum);
    }

    private TexturePanel createTexturePanel(short oldTexture, short newTexture, boolean isTextureSet)
    {
        JLabel oldTextureLabel = new JLabel("" + oldTexture, SwingConstants.CENTER);
        oldTextureLabel.setFont(FontManager.getRunescapeSmallFont());
        oldTextureLabel.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        oldTextureLabel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton newTextureButton = new JButton("" + (isTextureSet ? newTexture : "Set"));
        newTextureButton.setFont(FontManager.getRunescapeSmallFont());
        newTextureButton.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1, 0, 1));

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -1, 150, 1));
        spinner.setToolTipText("Apply a Texture Id as it is stored in the cache");
        buttonPanel.add(spinner);

        TexturePanel texturePanel = new TexturePanel(isTextureSet, oldTexture, newTexture, oldTextureLabel, newTextureButton, spinner);
        texturePanel.setLayout(new GridLayout(1, 0, 1, 1));
        texturePanel.add(oldTextureLabel);
        texturePanel.add(newTextureButton);
        texturePanel.add(buttonPanel);

        spinner.addChangeListener(e ->
        {
            short textureToAdd = (short) ((int) spinner.getValue());
            if (texturePanel.isTextureSet())
            {
                replaceTexture(texturePanel, textureToAdd);
                replaceTextureSwap(texturePanel.getOldTexture(), textureToAdd);
                return;
            }

            setTexture(texturePanel, textureToAdd);
            addTextureSwap(texturePanel.getOldTexture(), textureToAdd);
        });
        newTextureButton.addActionListener(e ->
        {
            if (texturePanel.isTextureSet())
            {
                unsetTexture(texturePanel);
                removeTextureSwap(texturePanel.getOldTexture());
                return;
            }

            short textureToAdd = (short) ((int) spinner.getValue());
            setTexture(texturePanel, textureToAdd);
            addTextureSwap(texturePanel.getOldTexture(), texturePanel.getNewTexture());
        });

        textureHolder.add(texturePanel);
        textureHolder.revalidate();
        textureHolder.repaint();
        texturePanels = ArrayUtils.add(texturePanels, texturePanel);
        return texturePanel;
    }

    private void setTexture(TexturePanel texturePanel, short texture)
    {
        JButton newColourButton = texturePanel.getNewTextureButton();
        newColourButton.setText("" + texture);
        texturePanel.setNewTexture(texture);
        texturePanel.setTextureSet(true);
    }

    private void unsetTexture(TexturePanel texturePanel)
    {
        JButton newTextureButton = texturePanel.getNewTextureButton();
        newTextureButton.setText("Set");
        texturePanel.setNewTexture(texturePanel.getOldTexture());
        texturePanel.setTextureSet(false);
    }

    private void replaceTexture(TexturePanel texturePanel, short newTextureTo)
    {
        JButton newTextureButton = texturePanel.getNewTextureButton();
        newTextureButton.setText("" + newTextureTo);
        texturePanel.setNewTexture(newTextureTo);
    }

    public void unsetAllTextures()
    {
        for (TexturePanel texturePanel : texturePanels)
            unsetTexture(texturePanel);

        removeAllTextureSwaps();
    }

    private void addTextureSwap(short textureFrom, short textureTo)
    {
        currentComplexPanel.setTexturesFrom(ArrayUtils.add(currentComplexPanel.getTexturesFrom(), textureFrom));
        currentComplexPanel.setTexturesTo(ArrayUtils.add(currentComplexPanel.getTexturesTo(), textureTo));
    }

    public void pasteTextureSwaps(ComplexPanel complexPanel, short[] newTexturesFrom, short[] newTexturesTo)
    {
        short[] texturesFrom = complexPanel.getTexturesFrom();
        short[] texturesTo = complexPanel.getTexturesTo();

        for (int i = 0; i < newTexturesFrom.length; i++)
        {
            boolean contains = false;
            for (int e = 0; e < texturesFrom.length; e++)
            {
                if (texturesFrom[e] == newTexturesFrom[i])
                {
                    texturesTo[e] = newTexturesTo[i];
                    contains = true;
                    break;
                }
            }

            if (contains)
                continue;

            texturesFrom = ArrayUtils.add(texturesFrom, newTexturesFrom[i]);
            texturesTo = ArrayUtils.add(texturesTo, newTexturesTo[i]);
        }

        complexPanel.setTexturesFrom(texturesFrom);
        complexPanel.setTexturesTo(texturesTo);
    }

    private void removeTextureSwap(short textureFrom)
    {
        short[] texturesFrom = currentComplexPanel.getTexturesFrom();
        short[] texturesTo = currentComplexPanel.getTexturesTo();

        int index = ArrayUtils.indexOf(texturesFrom, textureFrom);
        currentComplexPanel.setTexturesFrom(ArrayUtils.remove(texturesFrom, index));
        currentComplexPanel.setTexturesTo(ArrayUtils.remove(texturesTo, index));
    }

    private void replaceTextureSwap(short textureFrom, short newTextureTo)
    {
        short[] texturesFrom = currentComplexPanel.getTexturesFrom();
        short[] texturesTo = currentComplexPanel.getTexturesTo();

        int index = ArrayUtils.indexOf(texturesFrom, textureFrom);
        texturesTo[index] = newTextureTo;
    }

    public void removeAllTextureSwaps()
    {
        currentComplexPanel.setTexturesFrom(new short[0]);
        currentComplexPanel.setTexturesTo(new short[0]);
    }

    public void copyColoursTextures(ComplexPanel complexPanel)
    {
       copiedColoursTextures = new short[][]{complexPanel.getColoursFrom(), complexPanel.getColoursTo(), complexPanel.getTexturesFrom(), complexPanel.getTexturesTo()};
    }

    public void pasteColoursTextures(ComplexPanel complexPanel)
    {
        pasteColourSwaps(
                complexPanel,
                copiedColoursTextures[0],
                copiedColoursTextures[1]);

        pasteTextureSwaps(
                complexPanel,
                copiedColoursTextures[2],
                copiedColoursTextures[3]);

        if (currentComplexPanel == complexPanel)
            onSwapperPressed(complexPanel);
    }

    public void clearColoursTextures(ComplexPanel complexPanel)
    {
        if (currentComplexPanel == complexPanel)
        {
            unsetAllColours();
            unsetAllTextures();
            return;
        }

        complexPanel.setColoursFrom(new short[0]);
        complexPanel.setColoursTo(new short[0]);
        complexPanel.setTexturesFrom(new short[0]);
        complexPanel.setTexturesTo(new short[0]);
    }
}
