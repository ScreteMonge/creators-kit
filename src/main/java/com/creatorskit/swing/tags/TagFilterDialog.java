package com.creatorskit.swing.tags;

import com.creatorskit.cache.metadata.Tag;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Two-column tag filter dialog. Left column lists active filter tags;
 * right column lists available tags. Clicking a row moves it to the
 * other side and fires the change callback so the caller can re-run
 * the table filter pass.
 *
 * <p>Includes an AND / OR toggle at the top: AND means an entry must
 * carry every active filter tag to remain visible; OR means an entry
 * is visible if it carries any of them. AND is the default since it
 * narrows results more aggressively (common case for "find me the
 * thing tagged 'Boss' AND 'ToA'").
 */
public final class TagFilterDialog
{
    public enum Mode { AND, OR }

    private TagFilterDialog() {}

    public static void show(Component parent,
                             List<Tag> allTags,
                             Set<String> activeFilterNames,
                             Mode initialMode,
                             Consumer<FilterState> onChanged)
    {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Filter by tag(s)", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));

        // AND / OR toggle. Stacked vertically because the verbose
        // labels ("every filter must match" / "any filter matches")
        // don't fit on a single FlowLayout row at the dialog's 420px
        // width -- the OR button used to clip behind the columns
        // panel below. BoxLayout.Y_AXIS keeps the helper text visible.
        JPanel modeRow = new JPanel();
        modeRow.setLayout(new BoxLayout(modeRow, BoxLayout.Y_AXIS));
        modeRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        JLabel modeLabel = new JLabel("Mode:");
        modeLabel.setForeground(Color.LIGHT_GRAY);
        modeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeRow.add(modeLabel);
        JRadioButton andBtn = new JRadioButton("AND  (every filter must match)");
        JRadioButton orBtn = new JRadioButton("OR  (any filter matches)");
        andBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        orBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup grp = new ButtonGroup();
        grp.add(andBtn);
        grp.add(orBtn);
        if (initialMode == Mode.AND) andBtn.setSelected(true);
        else orBtn.setSelected(true);
        modeRow.add(andBtn);
        modeRow.add(orBtn);
        content.add(modeRow, BorderLayout.NORTH);

        // Holders that mutate via per-row click handlers + rebuild.
        final Set<String> active = new LinkedHashSet<>(activeFilterNames);
        final Mode[] modeBox = new Mode[]{initialMode};

        JPanel columns = new JPanel(new GridLayout(1, 2, 12, 0));

        JPanel leftCol = makeColumn("Active filters");
        JPanel rightCol = makeColumn("Available");

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

        final Runnable[] rebuildHolder = new Runnable[1];
        Runnable fireChanged = () -> onChanged.accept(new FilterState(new LinkedHashSet<>(active), modeBox[0]));
        rebuildHolder[0] = () ->
        {
            leftBody.removeAll();
            rightBody.removeAll();
            for (Tag t : allTags)
            {
                final Tag tag = t;
                if (active.contains(tag.getName()))
                {
                    leftBody.add(makeTagRow(tag, () ->
                    {
                        active.remove(tag.getName());
                        fireChanged.run();
                        rebuildHolder[0].run();
                    }));
                }
                else
                {
                    rightBody.add(makeTagRow(tag, () ->
                    {
                        active.add(tag.getName());
                        fireChanged.run();
                        rebuildHolder[0].run();
                    }));
                }
            }
            leftBody.revalidate();
            leftBody.repaint();
            rightBody.revalidate();
            rightBody.repaint();
        };

        andBtn.addActionListener(e -> { modeBox[0] = Mode.AND; fireChanged.run(); });
        orBtn.addActionListener(e -> { modeBox[0] = Mode.OR; fireChanged.run(); });

        rebuildHolder[0].run();
        content.add(columns, BorderLayout.CENTER);

        JButton clear = new JButton("Clear filters");
        clear.addActionListener(e -> { active.clear(); fireChanged.run(); rebuildHolder[0].run(); });
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomRow.add(clear);
        bottomRow.add(close);
        content.add(bottomRow, BorderLayout.SOUTH);

        dialog.add(content, BorderLayout.CENTER);
        // Height bumped from 380 -> 420 to absorb the extra row the
        // vertically-stacked AND/OR radios eat at the top.
        dialog.setPreferredSize(new Dimension(420, 420));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static JPanel makeColumn(String headerText)
    {
        JPanel col = new JPanel(new BorderLayout(0, 4));
        JLabel header = new JLabel(headerText, SwingConstants.CENTER);
        header.setForeground(ColorScheme.BRAND_ORANGE);
        header.setFont(FontManager.getRunescapeBoldFont());
        col.add(header, BorderLayout.NORTH);
        return col;
    }

    private static JPanel makeTagRow(Tag tag, Runnable onClick)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(new EmptyBorder(4, 8, 4, 8));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel lbl = new JLabel("<html><span style=\"color:" + tag.getColor().hex
                + "; font-size: 14px;\">&#9679;</span> " + tag.getName() + "</html>");
        lbl.setForeground(Color.WHITE);
        row.add(lbl, BorderLayout.CENTER);
        row.addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e) { onClick.run(); }
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(ColorScheme.MEDIUM_GRAY_COLOR); }
            @Override public void mouseExited(MouseEvent e) { row.setBackground(ColorScheme.DARKER_GRAY_COLOR); }
        });
        return row;
    }

    /** Result struct passed back to the caller on every state change. */
    public static final class FilterState
    {
        public final Set<String> activeFilterNames;
        public final Mode mode;

        public FilterState(Set<String> activeFilterNames, Mode mode)
        {
            this.activeFilterNames = activeFilterNames;
            this.mode = mode;
        }
    }
}
