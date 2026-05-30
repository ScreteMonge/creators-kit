package com.creatorskit.swing.timesheet.blocks;

import com.creatorskit.swing.timesheet.keyframe.BlockPalette;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modal dialog for creating / editing a timeline <b>Label</b> (the colored,
 * non-interactive organizational marker that replaced the old interactive
 * Blocks). Fields: a read-only context header ("Label for &lt;name&gt;" /
 * "&lt;X Characters&gt;"), an editable label text field, a 4x3 grid of
 * {@link BlockPalette} swatches, and From / To timestamp spinners.
 *
 * <p>No RGB / HSV chooser per the spec -- the palette keeps labels visually
 * consistent. Range validation (To &gt;= From, and the To==From no-op) is the
 * caller's job; this dialog just collects the values.
 */
public class BlockEditDialog
{
    /** Result returned to the caller. {@code null} when the user cancelled. */
    public static class Result
    {
        public final String name;
        public final int colorRgb;
        public final double fromTick;
        public final double toTick;
        public Result(String name, int colorRgb, double fromTick, double toTick)
        {
            this.name = name;
            this.colorRgb = colorRgb;
            this.fromTick = fromTick;
            this.toTick = toTick;
        }
    }

    /**
     * Opens the dialog modally on top of {@code parent}. {@code header} is the
     * read-only context line; {@code initialName} pre-fills the text field;
     * {@code initialColorRgb} pre-selects the matching swatch; {@code initialFrom}
     * / {@code initialTo} seed the From / To spinners. Returns null on cancel.
     */
    public static Result show(JComponent parent, String title, String header,
                              String initialName, int initialColorRgb,
                              double initialFrom, double initialTo)
    {
        final int[] picked = new int[]{initialColorRgb};
        final JTextField nameField = new JTextField(initialName == null ? "" : initialName, 18);
        final JSpinner fromSpinner = new JSpinner(new SpinnerNumberModel(initialFrom, 0.0, 1_000_000.0, 1.0));
        final JSpinner toSpinner = new JSpinner(new SpinnerNumberModel(initialTo, 0.0, 1_000_000.0, 1.0));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        content.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Read-only context header at the very top.
        JLabel headerLabel = new JLabel(header == null ? "Label" : header);
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
        content.add(headerLabel, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 2, 2, 2);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        JLabel nameLabel = new JLabel("Label:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(FontManager.getRunescapeBoldFont());
        fields.add(nameLabel, gc);
        gc.gridx = 1; gc.weightx = 1;
        fields.add(nameField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        JLabel fromLabel = new JLabel("From:");
        fromLabel.setForeground(Color.WHITE);
        fields.add(fromLabel, gc);
        gc.gridx = 1; gc.weightx = 1;
        fields.add(fromSpinner, gc);

        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0;
        JLabel toLabel = new JLabel("To:");
        toLabel.setForeground(Color.WHITE);
        fields.add(toLabel, gc);
        gc.gridx = 1; gc.weightx = 1;
        fields.add(toSpinner, gc);

        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setOpaque(false);
        namePanel.add(fields, BorderLayout.NORTH);

        // 4x3 swatch grid. Each swatch is a JPanel painted with the palette
        // colour; clicking selects it. Selected swatch gets a white border.
        // The paintComponent override is load-bearing -- RuneLite ships a
        // Synth-based LaF whose JPanel UI ignores setBackground for plain
        // (no-child) panels and leaves them with the default gray. Doing the
        // fill ourselves bypasses the LaF entirely so each swatch actually
        // shows its palette colour instead of all twelve looking identical.
        JPanel swatches = new JPanel(new GridLayout(3, 4, 4, 4));
        swatches.setOpaque(false);
        final JPanel[] swatchPanels = new JPanel[BlockPalette.COLOURS.length];
        for (int i = 0; i < BlockPalette.COLOURS.length; i++)
        {
            final int rgb = BlockPalette.COLOURS[i];
            final Color fill = new Color(rgb);
            JPanel sw = new JPanel()
            {
                @Override
                protected void paintComponent(Graphics g)
                {
                    g.setColor(fill);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            sw.setOpaque(true);
            sw.setPreferredSize(new Dimension(48, 36));
            sw.setBackground(fill);
            sw.setBorder(new LineBorder(rgb == initialColorRgb ? Color.WHITE : ColorScheme.MEDIUM_GRAY_COLOR,
                    rgb == initialColorRgb ? 3 : 1));
            sw.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            sw.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    picked[0] = rgb;
                    // Re-paint borders so only the picked one shows the
                    // highlight ring.
                    for (int j = 0; j < swatchPanels.length; j++)
                    {
                        int otherRgb = BlockPalette.COLOURS[j];
                        swatchPanels[j].setBorder(new LineBorder(
                                otherRgb == picked[0] ? Color.WHITE : ColorScheme.MEDIUM_GRAY_COLOR,
                                otherRgb == picked[0] ? 3 : 1));
                    }
                }
            });
            swatchPanels[i] = sw;
            swatches.add(sw);
        }
        JPanel swatchWrap = new JPanel(new BorderLayout());
        swatchWrap.setOpaque(false);
        swatchWrap.setBorder(new EmptyBorder(8, 0, 0, 0));
        JLabel swatchLabel = new JLabel("Colour:");
        swatchLabel.setForeground(Color.WHITE);
        swatchLabel.setFont(FontManager.getRunescapeBoldFont());
        swatchLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
        swatchWrap.add(swatchLabel, BorderLayout.NORTH);
        swatchWrap.add(swatches, BorderLayout.CENTER);

        // Both the fields panel and the swatch grid want the CENTER region, but
        // BorderLayout only lays out ONE component per region -- adding both to
        // `content` directly silently dropped the fields panel (so the label
        // text box never appeared). Stack them in a shared parent instead:
        // fields on top, swatches filling the rest.
        JPanel centerStack = new JPanel(new BorderLayout(0, 8));
        centerStack.setOpaque(false);
        centerStack.add(namePanel, BorderLayout.NORTH);
        centerStack.add(swatchWrap, BorderLayout.CENTER);
        content.add(centerStack, BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(parent, content, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return null;

        String name = nameField.getText();
        if (name == null) name = "";
        name = name.trim();
        if (name.isEmpty()) name = "Label";

        double from = ((Number) fromSpinner.getValue()).doubleValue();
        double to = ((Number) toSpinner.getValue()).doubleValue();
        return new Result(name, picked[0], from, to);
    }
}
