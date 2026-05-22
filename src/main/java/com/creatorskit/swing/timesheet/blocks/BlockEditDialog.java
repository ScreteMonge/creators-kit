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
 * Modal dialog for block create / rename / recolour. Shows a name field at
 * the top and a 4x3 grid of {@link BlockPalette} swatches below; user picks
 * one swatch (highlighted with a border) and presses OK.
 *
 * <p>No RGB / HSV chooser per the spec -- the palette is the whole point of
 * the constraint, so blocks stay visually consistent across saves and the
 * user doesn't have to fiddle with sliders for every block.
 */
public class BlockEditDialog
{
    /** Result returned to the caller. {@code null} when the user cancelled. */
    public static class Result
    {
        public final String name;
        public final int colorRgb;
        public Result(String name, int colorRgb)
        {
            this.name = name;
            this.colorRgb = colorRgb;
        }
    }

    /**
     * Opens the dialog modally on top of {@code parent}. {@code initialName}
     * pre-fills the name field; {@code initialColorRgb} pre-selects the
     * matching swatch (falls back to {@link BlockPalette#DEFAULT_RGB} if no
     * palette entry matches). Returns null when the user cancels.
     */
    public static Result show(JComponent parent, String title, String initialName, int initialColorRgb)
    {
        final int[] picked = new int[]{initialColorRgb};
        final JTextField nameField = new JTextField(initialName == null ? "" : initialName, 18);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        content.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel namePanel = new JPanel(new BorderLayout(6, 0));
        namePanel.setOpaque(false);
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(FontManager.getRunescapeBoldFont());
        namePanel.add(nameLabel, BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);
        content.add(namePanel, BorderLayout.NORTH);

        // 4x3 swatch grid. Each swatch is a JPanel painted with the palette
        // colour; clicking selects it. Selected swatch gets a white border.
        JPanel swatches = new JPanel(new GridLayout(3, 4, 4, 4));
        swatches.setOpaque(false);
        final JPanel[] swatchPanels = new JPanel[BlockPalette.COLOURS.length];
        for (int i = 0; i < BlockPalette.COLOURS.length; i++)
        {
            final int rgb = BlockPalette.COLOURS[i];
            JPanel sw = new JPanel();
            sw.setPreferredSize(new Dimension(48, 36));
            sw.setBackground(new Color(rgb));
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
        content.add(swatchWrap, BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(parent, content, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return null;

        String name = nameField.getText();
        if (name == null) name = "";
        name = name.trim();
        if (name.isEmpty()) name = "Block";

        return new Result(name, picked[0]);
    }
}
