package com.creatorskit.swing.colours;

import com.creatorskit.swing.anvil.ComplexPanel;
import com.creatorskit.swing.anvil.ModelAnvil;
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
    private final ModelAnvil modelAnvil;

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

    /** Currently-highlighted Old colour, or HIGHLIGHT_NONE. Mirrored to RenderPanel.setHighlightedColour. */
    private static final int HIGHLIGHT_NONE = Integer.MIN_VALUE;
    private int highlightedColour = HIGHLIGHT_NONE;
    /** ColourPanel matching the currently-highlighted colour, for border restoration. */
    private ColourPanel highlightedPanel;
    private final javax.swing.border.Border HIGHLIGHTED_PANEL_BORDER = BorderFactory.createCompoundBorder(
            new LineBorder(net.runelite.client.ui.ColorScheme.BRAND_ORANGE, 2),
            new EmptyBorder(0, 2, 0, 2));
    private final javax.swing.border.Border UNHIGHLIGHTED_PANEL_BORDER = new EmptyBorder(2, 4, 2, 4);
    /** Held so we can scrollRectToVisible on the matching panel when a face is picked. */
    private JScrollPane colourScrollPane;

    @Inject
    public ColourSwapPanel(Client client, ClientThread clientThread, ArrayList<ComplexPanel> complexPanels, ModelAnvil modelAnvil)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.complexPanels = complexPanels;
        this.modelAnvil = modelAnvil;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setLayout(new GridBagLayout());

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

        // Preview row above the chooser: small colour swatch on the left, hex
        // text field on the right. Two-way bound to the JColorChooser's
        // selection model -- typing a hex code into the field updates the
        // chooser (and every connected swatch), and picking a colour via any
        // tab updates the field. Lives above the chooser instead of inside any
        // specific tab so it's accessible from Swatches / HSV / HSL / RGB / CMYK
        // alike, without depending on the internal layout of those panels.
        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 0;
        JPanel previewRow = new JPanel(new BorderLayout(4, 0));
        previewRow.setBorder(new EmptyBorder(2, 4, 2, 4));

        JPanel previewSwatch = new JPanel();
        previewSwatch.setBackground(colourChooser.getColor());
        previewSwatch.setPreferredSize(new Dimension(28, 28));
        previewSwatch.setBorder(new LineBorder(java.awt.Color.BLACK, 1));

        JTextField hexField = new JTextField(7);
        hexField.setToolTipText("<html>Hex colour code. Accepts #RRGGBB, RRGGBB, #RGB, or RGB.<br>Press Enter or click away to apply.</html>");
        hexField.setText(formatHex(colourChooser.getColor()));

        previewRow.add(previewSwatch, BorderLayout.WEST);
        previewRow.add(hexField, BorderLayout.CENTER);
        add(previewRow, c);

        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 1;
        // Use the FULL JColorChooser (default tabs: Swatches, HSV, HSL, RGB, CMYK)
        // rather than just the HSV panel the original implementation pinned to.
        // The Swatches tab (chooserPanels[0]) is what users naturally reach for
        // when picking from preset palettes; the HSV one was leftover from a
        // smaller layout. Hiding the chooser's built-in preview panel because
        // the previewRow above already shows the current picker colour AND has
        // the bidirectional hex field.
        colourChooser.setPreviewPanel(new JPanel());
        colourChooser.setBorder(new LineBorder(colourChooser.getColor(), 2));
        colourChooser.getSelectionModel().addChangeListener(e ->
        {
            java.awt.Color picked = colourChooser.getColor();
            colourChooser.setBorder(new LineBorder(picked, 2));
            previewSwatch.setBackground(picked);
            // Sync the hex field WITHOUT triggering its own listener -- if we
            // re-set the text from a programmatic chooser change, the text
            // didn't actually change as far as the user is concerned. The
            // actionListener fires on user-typed Enter, so setText alone is
            // safe (no actionPerformed dispatch). Skip if the field already
            // matches to avoid losing the user's cursor position when they're
            // mid-typing a different code.
            String formatted = formatHex(picked);
            if (!formatted.equalsIgnoreCase(hexField.getText().trim()))
            {
                hexField.setText(formatted);
            }
        });
        add(colourChooser, c);

        // Apply on Enter (action) AND on focus loss, so the user doesn't have
        // to remember to press Enter if they click away.
        Runnable applyHex = () ->
        {
            java.awt.Color parsed = parseHex(hexField.getText());
            if (parsed != null && !parsed.equals(colourChooser.getColor()))
            {
                colourChooser.setColor(parsed);
            }
            else
            {
                // Invalid or unchanged -- snap the field text back to the
                // canonical formatted form so the user sees what they have.
                hexField.setText(formatHex(colourChooser.getColor()));
            }
        };
        hexField.addActionListener(e -> applyHex.run());
        hexField.addFocusListener(new java.awt.event.FocusAdapter()
        {
            @Override
            public void focusLost(java.awt.event.FocusEvent e)
            {
                applyHex.run();
            }
        });

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
        // Held so selectColour can scroll the matching ColourPanel into view.
        this.colourScrollPane = scrollPane;
        add(scrollPane, c);

        // Hook the model-preview click: clicking a face calls highlightOldColour
        // with that face's Jagex colour, which scrolls / outlines the matching
        // ColourPanel and tints the model so all faces of that colour stand out.
        // Deferred behind invokeLater because RenderPanel is constructed before
        // ColourSwapPanel during ModelAnvil setup -- the field is non-null here
        // but the addMouseListener hook is wired right away regardless.
        if (modelAnvil.getRenderPanel() != null)
        {
            modelAnvil.getRenderPanel().setOnFaceClicked(this::highlightOldColour);
        }

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
        // Two-column layout halves the visual height of the list at the same
        // entry density. Compact swatches (28x28) easily fit two per row.
        colourHolder.setLayout(new GridLayout(0, 2, 4, 2));
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

            // Sort by HSL (hue first, then saturation, then luminance) so similar
            // colours land next to each other in the list. The original order was
            // raw cache-face order, which is essentially random visually.
            Short[] sorted = new Short[colourList.length];
            for (int i = 0; i < colourList.length; i++) sorted[i] = colourList[i];
            Arrays.sort(sorted, java.util.Comparator
                    .comparingInt((Short s) -> JagexColor.unpackHue(s))
                    .thenComparingInt(s -> JagexColor.unpackSaturation(s))
                    .thenComparingInt(s -> JagexColor.unpackLuminance(s)));
            for (int i = 0; i < sorted.length; i++) colourList[i] = sorted[i];

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
        clearHighlight();
    }

    /**
     * Square size for the compact old/new colour swatches. Small enough to keep
     * the list dense even when the model has 30+ unique colours; large enough to
     * be a comfortable click target. Black border is drawn 1px wide so the
     * swatches don't blur into each other or into a dark background.
     */
    private static final Dimension SWATCH_SIZE = new Dimension(28, 28);
    private static final java.awt.Color SWATCH_BORDER = java.awt.Color.BLACK;

    private ColourPanel createColourPanel(short oldColour, short newColour, boolean isColourSet)
    {
        // "Old" swatch -- click loads that colour into the picker. Used as the
        // visual anchor for finding which entry in the list corresponds to a
        // given face colour on the model.
        JButton oldSwatch = makeSwatch(colourFromShort(oldColour));
        oldSwatch.setToolTipText("Original colour. Click to load this colour into the picker.");

        // "New" swatch -- click applies the picker's current colour (or unsets
        // if already set). When unset, displays the old colour (since no swap
        // is active), but with a dashed border so the user can still tell.
        JButton newSwatch = makeSwatch(colourFromShort(isColourSet ? newColour : oldColour));
        newSwatch.setToolTipText("Replacement colour. Click to apply the picker colour, click again to clear.");
        applyNewSwatchBorder(newSwatch, isColourSet);

        // Hidden carry-along to preserve the existing setColour / unsetColour
        // contract -- those methods still call colourPanel.getNewColourButton()
        // and read its border to derive state. Keeping the field non-null lets
        // us reuse all the swap-management methods unchanged.
        JSpinner unusedSpinner = new JSpinner();

        ColourPanel colourPanel = new ColourPanel(isColourSet, oldColour, newColour, null, newSwatch, unusedSpinner);
        colourPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 6, 2));
        colourPanel.setBorder(UNHIGHLIGHTED_PANEL_BORDER);
        colourPanel.add(oldSwatch);
        // Tiny arrow label between the swatches makes the "old -> new" reading
        // obvious without taking up much room.
        JLabel arrow = new JLabel("→"); // →
        arrow.setForeground(java.awt.Color.LIGHT_GRAY);
        colourPanel.add(arrow);
        colourPanel.add(newSwatch);

        oldSwatch.addActionListener(e ->
        {
            colourChooser.setColor(colourFromShort(colourPanel.getOldColour()));
            // Also drive the highlight when the user clicks the Old swatch in
            // the list -- keeps the two entry points (model click + swatch
            // click) symmetric so either always lights up both surfaces.
            highlightOldColour(colourPanel.getOldColour());
        });
        newSwatch.addActionListener(e ->
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

    /**
     * Builds a flat coloured square button suitable for use as a swatch.
     * Borderless apart from a thin black line so the colour bleeds to the edge.
     */
    private JButton makeSwatch(java.awt.Color fill)
    {
        JButton b = new JButton();
        b.setPreferredSize(SWATCH_SIZE);
        b.setMinimumSize(SWATCH_SIZE);
        b.setMaximumSize(SWATCH_SIZE);
        b.setBackground(fill);
        b.setOpaque(true);
        b.setBorderPainted(true);
        b.setBorder(new LineBorder(SWATCH_BORDER, 1));
        b.setFocusable(false);
        b.setContentAreaFilled(true);
        return b;
    }

    /**
     * Switches the "new" swatch border between "swap active" (solid black) and
     * "swap inactive" (dashed light grey) so the user can tell from a glance
     * whether a row currently has a colour override applied, even though the
     * swatch fill is the old colour in both states.
     */
    private void applyNewSwatchBorder(JButton newSwatch, boolean isColourSet)
    {
        if (isColourSet)
        {
            newSwatch.setBorder(new LineBorder(SWATCH_BORDER, 1));
        }
        else
        {
            newSwatch.setBorder(BorderFactory.createDashedBorder(java.awt.Color.LIGHT_GRAY, 1f, 2f, 2f, false));
        }
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
        modelAnvil.updateRenderPanel();
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
            modelAnvil.updateRenderPanel();
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
        modelAnvil.updateRenderPanel();
    }

    private void setColour(ColourPanel colourPanel, Color rgbColour)
    {
        setColour(colourPanel, shortFromColour(rgbColour));
    }

    private void setColour(ColourPanel colourPanel, short colour)
    {
        JButton newSwatch = colourPanel.getNewColourButton();
        newSwatch.setBackground(colourFromShort(colour));
        applyNewSwatchBorder(newSwatch, true);
        colourPanel.setNewColour(colour);
        colourPanel.setColourSet(true);
    }

    private void unsetColour(ColourPanel colourPanel)
    {
        JButton newSwatch = colourPanel.getNewColourButton();
        // Reverts the swatch fill back to the old colour to mirror "no swap
        // applied"; the dashed border tells the user it's the inactive state.
        newSwatch.setBackground(colourFromShort(colourPanel.getOldColour()));
        applyNewSwatchBorder(newSwatch, false);
        colourPanel.setNewColour(colourPanel.getOldColour());
        colourPanel.setColourSet(false);
    }

    private void replaceColour(ColourPanel colourPanel, short newColourTo)
    {
        JButton newSwatch = colourPanel.getNewColourButton();
        newSwatch.setBackground(colourFromShort(newColourTo));
        applyNewSwatchBorder(newSwatch, true);
        colourPanel.setNewColour(newColourTo);
    }

    public void unsetAllColours()
    {
        for (ColourPanel colourPanel : colourPanels)
            unsetColour(colourPanel);

        removeAllColourSwaps();
        modelAnvil.updateRenderPanel();
    }

    private void addColourSwap(short colourFrom, short colourTo)
    {
        currentComplexPanel.setColoursFrom(ArrayUtils.add(currentComplexPanel.getColoursFrom(), colourFrom));
        currentComplexPanel.setColoursTo(ArrayUtils.add(currentComplexPanel.getColoursTo(), colourTo));
        modelAnvil.updateRenderPanel();
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
        modelAnvil.updateRenderPanel();
    }

    private void removeColourSwap(short colourFrom)
    {
        short[] coloursFrom = currentComplexPanel.getColoursFrom();
        short[] coloursTo = currentComplexPanel.getColoursTo();

        int index = ArrayUtils.indexOf(coloursFrom, colourFrom);
        currentComplexPanel.setColoursFrom(ArrayUtils.remove(coloursFrom, index));
        currentComplexPanel.setColoursTo(ArrayUtils.remove(coloursTo, index));
        modelAnvil.updateRenderPanel();
    }

    private void replaceColourSwap(short colourFrom, short newColourTo)
    {
        short[] coloursFrom = currentComplexPanel.getColoursFrom();
        short[] coloursTo = currentComplexPanel.getColoursTo();

        int index = ArrayUtils.indexOf(coloursFrom, colourFrom);
        coloursTo[index] = newColourTo;
        modelAnvil.updateRenderPanel();
    }

    public void removeAllColourSwaps()
    {
        currentComplexPanel.setColoursFrom(new short[0]);
        currentComplexPanel.setColoursTo(new short[0]);
        modelAnvil.updateRenderPanel();
    }

    /**
     * Formats a Color as a canonical "#RRGGBB" string. Always uppercase, always
     * 6 hex digits, leading '#'. Drops the alpha channel because the picker /
     * Jagex colour pipeline doesn't carry alpha.
     */
    private static String formatHex(java.awt.Color c)
    {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    /**
     * Parses a user-entered hex code into a Color. Accepts:
     *   - "#RRGGBB" or "RRGGBB" (6-digit form)
     *   - "#RGB" or "RGB" (3-digit shorthand, each digit doubled: F0A -> FF00AA)
     * Returns null for empty / malformed input rather than throwing -- caller
     * snaps the field back to the canonical formatted form on null.
     */
    private static java.awt.Color parseHex(String input)
    {
        if (input == null) return null;
        String s = input.trim();
        if (s.startsWith("#")) s = s.substring(1);
        try
        {
            if (s.length() == 6)
            {
                return new java.awt.Color(Integer.parseInt(s, 16));
            }
            if (s.length() == 3)
            {
                // #abc -> #aabbcc
                StringBuilder sb = new StringBuilder(6);
                for (int i = 0; i < 3; i++)
                {
                    char ch = s.charAt(i);
                    sb.append(ch).append(ch);
                }
                return new java.awt.Color(Integer.parseInt(sb.toString(), 16));
            }
            return null;
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
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
            modelAnvil.updateRenderPanel();
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
            modelAnvil.updateRenderPanel();
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

        modelAnvil.updateRenderPanel();
    }

    /**
     * Drives the "this colour is selected" surface in two places at once:
     * (1) tells the RenderPanel to tint every face of this colour so the user
     * can see exactly which parts of the model use it; (2) outlines the
     * matching ColourPanel row in BRAND_ORANGE and scrolls it into view so the
     * user can edit / unset the swap without hunting through the list. Clicking
     * the same Old colour twice clears the highlight (toggle off).
     */
    public void highlightOldColour(short oldColour)
    {
        // Toggle off if the user clicks the same swatch / face again.
        if (highlightedColour == (oldColour & 0xFFFF))
        {
            clearHighlight();
            return;
        }

        if (highlightedPanel != null)
        {
            highlightedPanel.setBorder(UNHIGHLIGHTED_PANEL_BORDER);
            highlightedPanel = null;
        }

        ColourPanel match = null;
        for (ColourPanel cp : colourPanels)
        {
            if (cp.getOldColour() == oldColour)
            {
                match = cp;
                break;
            }
        }

        if (match == null)
        {
            // Face the user clicked has a colour that isn't in our list. Can
            // happen if the model's face-colour set drifted from what we
            // populated. Tint the model anyway so the user gets visual feedback.
            highlightedColour = oldColour & 0xFFFF;
            if (modelAnvil.getRenderPanel() != null)
            {
                modelAnvil.getRenderPanel().setHighlightedColour(highlightedColour);
            }
            return;
        }

        highlightedColour = oldColour & 0xFFFF;
        highlightedPanel = match;
        match.setBorder(HIGHLIGHTED_PANEL_BORDER);

        if (modelAnvil.getRenderPanel() != null)
        {
            modelAnvil.getRenderPanel().setHighlightedColour(highlightedColour);
        }

        // Scroll the matching row into view inside the JScrollPane that wraps
        // the swap list. invokeLater because the panel may have just been
        // re-laid-out and the rectangle needs the layout manager to settle.
        if (colourScrollPane != null)
        {
            final ColourPanel target = match;
            SwingUtilities.invokeLater(() -> target.scrollRectToVisible(new Rectangle(0, 0, target.getWidth(), target.getHeight())));
        }
    }

    /**
     * Clears the current highlight surface in both places (model tint + row
     * outline). Called when the user clicks the same colour twice, when the
     * model is swapped, or when the swapper is unset.
     */
    public void clearHighlight()
    {
        highlightedColour = HIGHLIGHT_NONE;
        if (highlightedPanel != null)
        {
            highlightedPanel.setBorder(UNHIGHLIGHTED_PANEL_BORDER);
            highlightedPanel = null;
        }
        if (modelAnvil.getRenderPanel() != null)
        {
            modelAnvil.getRenderPanel().setHighlightedColour(com.creatorskit.swing.renderer.RenderPanel.HIGHLIGHT_NONE);
        }
    }

    public void clearColoursTextures(ComplexPanel complexPanel)
    {
        if (currentComplexPanel == complexPanel)
        {
            unsetAllColours();
            unsetAllTextures();
            modelAnvil.updateRenderPanel();
            return;
        }

        complexPanel.setColoursFrom(new short[0]);
        complexPanel.setColoursTo(new short[0]);
        complexPanel.setTexturesFrom(new short[0]);
        complexPanel.setTexturesTo(new short[0]);
        modelAnvil.updateRenderPanel();
    }
}
