package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Two-column property filter for a timeline view. Left column lists the
 * properties currently visible in the timeline; right column lists the
 * ones currently filtered out. Clicking a row in either column moves it
 * to the other side. State is applied incrementally -- closing the
 * dialog is a no-op confirm (changes already landed via the click
 * callbacks).
 *
 * <p>Used by both the local label column ("Filters..." button under the
 * local rows) and the global one. The caller supplies the canonical
 * ordering, the current hidden set, and a writer callback that mutates
 * the hidden set + asks the timeline to repaint.
 */
public final class TimelineFiltersDialog
{
    private TimelineFiltersDialog() {}

    /**
     * Opens the modal filters dialog.
     *
     * @param parent           anchor component for dialog positioning
     * @param title            window title (e.g. "Local property filters")
     * @param allTypes         the canonical type list for this view (already alphabetised)
     * @param hidden           the current hidden set; mutated as the user clicks
     * @param onChanged        called after each click so the caller can rebuild + repaint
     */
    public static void show(Component parent, String title,
                             KeyFrameType[] allTypes,
                             Set<KeyFrameType> hidden,
                             Consumer<Set<KeyFrameType>> onChanged)
    {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel columns = new JPanel(new GridLayout(1, 2, 12, 0));

        JPanel leftCol = makeColumn("Visible", true);
        JPanel rightCol = makeColumn("Hidden", false);

        JPanel leftBody = new JPanel();
        leftBody.setLayout(new BoxLayout(leftBody, BoxLayout.Y_AXIS));
        leftBody.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JScrollPane leftScroll = new JScrollPane(leftBody);
        leftScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        leftScroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        leftCol.add(leftScroll, BorderLayout.CENTER);

        JPanel rightBody = new JPanel();
        rightBody.setLayout(new BoxLayout(rightBody, BoxLayout.Y_AXIS));
        rightBody.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JScrollPane rightScroll = new JScrollPane(rightBody);
        rightScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        rightScroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rightCol.add(rightScroll, BorderLayout.CENTER);

        columns.add(leftCol);
        columns.add(rightCol);

        // Rebuild both columns from the current hidden set. Clicks fire
        // `onChanged` (caller mutates state + refreshes the timeline)
        // and then re-run rebuild so the moved row jumps sides without
        // closing the dialog.
        final Runnable[] rebuildHolder = new Runnable[1];
        rebuildHolder[0] = () ->
        {
            leftBody.removeAll();
            rightBody.removeAll();
            for (KeyFrameType t : allTypes)
            {
                final KeyFrameType type = t;
                if (hidden.contains(type))
                {
                    rightBody.add(makeTypeRow(type, () ->
                    {
                        hidden.remove(type);
                        onChanged.accept(hidden);
                        rebuildHolder[0].run();
                    }));
                }
                else
                {
                    leftBody.add(makeTypeRow(type, () ->
                    {
                        hidden.add(type);
                        onChanged.accept(hidden);
                        rebuildHolder[0].run();
                    }));
                }
            }
            leftBody.revalidate();
            leftBody.repaint();
            rightBody.revalidate();
            rightBody.repaint();
        };
        rebuildHolder[0].run();

        content.add(columns, BorderLayout.CENTER);

        JLabel hint = new JLabel("Click a property to move it between Visible and Hidden.");
        hint.setForeground(Color.LIGHT_GRAY);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        hint.setBorder(new EmptyBorder(0, 0, 4, 0));
        content.add(hint, BorderLayout.NORTH);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomRow.add(close);
        content.add(bottomRow, BorderLayout.SOUTH);

        dialog.add(content, BorderLayout.CENTER);
        dialog.setPreferredSize(new Dimension(360, 420));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static JPanel makeColumn(String headerText, boolean isLeft)
    {
        JPanel col = new JPanel(new BorderLayout(0, 4));
        JLabel header = new JLabel(headerText, SwingConstants.CENTER);
        header.setForeground(ColorScheme.BRAND_ORANGE);
        header.setBorder(new EmptyBorder(0, 0, 4, 0));
        col.add(header, BorderLayout.NORTH);
        return col;
    }

    private static JLabel makeTypeRow(KeyFrameType type, Runnable onClick)
    {
        JLabel row = new JLabel(" " + type.getName());
        row.setOpaque(true);
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setForeground(Color.LIGHT_GRAY);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setPreferredSize(new Dimension(140, 22));
        row.setBorder(new EmptyBorder(2, 4, 2, 4));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e)
            {
                row.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e)
            {
                row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                onClick.run();
            }
        });
        return row;
    }
}
