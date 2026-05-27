package com.creatorskit.swing.tags;

import com.creatorskit.cache.metadata.Tag;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Persistent ("don't close on click") tag picker popup used by the
 * right-click Set Tag(s) and Remove Tag(s) actions on the cache
 * searcher.
 *
 * <p>UI shape:
 * <ul>
 *   <li>Header bar with title text ("Set Tags" / "Remove Tags") + a
 *       small {@code X} close button on the right.</li>
 *   <li>Vertical list of tag rows. Each row shows a coloured bullet
 *       in the tag's swatch, the tag name, and a check glyph that
 *       appears only when the tag is currently assigned (in Set mode)
 *       or every visible tag is assigned (in Remove mode).</li>
 *   <li>Clicking a tag row fires the toggle callback (caller decides
 *       what assignment to mutate) and re-renders the checkmark
 *       column without closing the popup.</li>
 * </ul>
 *
 * <p>Dismissal: explicit X click, Esc key, or any mouse press
 * outside the popup bounds (AWT-event tap watches every MOUSE_PRESSED
 * across the JVM and closes when the target isn't a descendant of
 * the popup's root).
 *
 * <p>Set vs Remove mode differs only in initial tag list: Set lists
 * every tag in the store; Remove lists only those currently assigned
 * to at least one of the targeted entries.
 */
public class TagPickerPopup extends JWindow
{
    /**
     * Per-row callback handle. Caller decides what the toggle means
     * (Set mode adds when isNowChecked is true, removes when false;
     * Remove mode interprets every click as a removal regardless of
     * checkbox state).
     */
    public interface OnToggle
    {
        void run(Tag tag, boolean isNowChecked);
    }

    private final JPanel listBody = new JPanel();
    private final Map<Tag, JLabel> checkLabels = new LinkedHashMap<>();
    private final BiConsumer<Tag, Boolean> toggleCallback;
    private final boolean removeMode;
    /** Live reference to the set the popup is checking against; refresh re-renders the check glyphs. */
    private Set<String> currentlyAssignedNames;
    private AWTEventListener clickAwayListener;

    public TagPickerPopup(Component owner,
                           String title,
                           boolean removeMode,
                           List<Tag> tags,
                           Set<String> currentlyAssignedNames,
                           BiConsumer<Tag, Boolean> toggleCallback)
    {
        super(SwingUtilities.getWindowAncestor(owner));
        this.removeMode = removeMode;
        this.currentlyAssignedNames = currentlyAssignedNames;
        this.toggleCallback = toggleCallback;
        setFocusableWindowState(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        root.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        root.add(buildHeader(title), BorderLayout.NORTH);
        root.add(buildList(tags), BorderLayout.CENTER);

        setContentPane(root);
        pack();

        installClickAwayListener();
        installEscListener();
    }

    private JPanel buildHeader(String title)
    {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(new EmptyBorder(4, 6, 4, 6));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(FontManager.getRunescapeBoldFont());
        header.add(titleLbl, BorderLayout.WEST);

        JLabel close = new JLabel("✕");  // X / multiplication sign
        close.setForeground(Color.LIGHT_GRAY);
        close.setBorder(new EmptyBorder(0, 8, 0, 0));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setToolTipText("Close");
        close.addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e) { dispose(); }
        });
        header.add(close, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildList(List<Tag> tags)
    {
        listBody.setLayout(new BoxLayout(listBody, BoxLayout.Y_AXIS));
        listBody.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        if (tags.isEmpty())
        {
            JLabel empty = new JLabel(removeMode ? "No tags assigned." : "No tags yet — create one in the Tag Manager.");
            empty.setForeground(Color.LIGHT_GRAY);
            empty.setBorder(new EmptyBorder(6, 8, 6, 8));
            listBody.add(empty);
        }
        else
        {
            for (Tag t : tags) listBody.add(buildRow(t));
        }

        JScrollPane scroll = new JScrollPane(listBody);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(220, Math.min(40 + tags.size() * 28, 320)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildRow(Tag tag)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(new EmptyBorder(4, 8, 4, 8));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel left = new JLabel(buildBulletLabel(tag));
        left.setForeground(Color.WHITE);
        row.add(left, BorderLayout.CENTER);

        JLabel check = new JLabel();
        check.setForeground(new Color(0x4ee54e));
        check.setText(isAssigned(tag) ? "✓" : "");
        check.setBorder(new EmptyBorder(0, 8, 0, 0));
        checkLabels.put(tag, check);
        row.add(check, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e) { onRowClicked(tag); }

            @Override public void mouseEntered(MouseEvent e)
            {
                row.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            }

            @Override public void mouseExited(MouseEvent e)
            {
                row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        });

        return row;
    }

    /** Builds the "&#9679; TagName" label with the bullet rendered in the tag's hex colour. */
    private static String buildBulletLabel(Tag tag)
    {
        return "<html><span style=\"color:" + tag.getColor().hex
                + "; font-size: 14px;\">&#9679;</span> " + tag.getName() + "</html>";
    }

    private boolean isAssigned(Tag tag)
    {
        return currentlyAssignedNames != null && currentlyAssignedNames.contains(tag.getName());
    }

    private void onRowClicked(Tag tag)
    {
        // In Set mode the click flips the assigned state. In Remove
        // mode every click is a removal; the row vanishes from the
        // popup after firing the callback so the user sees what they
        // already removed.
        boolean wasChecked = isAssigned(tag);
        boolean isNowChecked = removeMode ? false : !wasChecked;
        toggleCallback.accept(tag, isNowChecked);
        // Refresh visible checkmarks; the caller updated the store, so
        // the popup just re-reads its in-memory snapshot.
        if (removeMode)
        {
            // Hide the row -- can't toggle it back without re-opening.
            JLabel check = checkLabels.remove(tag);
            if (check != null)
            {
                Container parent = check.getParent();
                if (parent != null)
                {
                    parent.setVisible(false);
                    listBody.revalidate();
                    listBody.repaint();
                }
            }
            // Update our local set so subsequent isAssigned reads agree.
            if (currentlyAssignedNames != null) currentlyAssignedNames.remove(tag.getName());
        }
        else
        {
            // Update local set + checkmark glyph.
            if (currentlyAssignedNames != null)
            {
                if (isNowChecked) currentlyAssignedNames.add(tag.getName());
                else currentlyAssignedNames.remove(tag.getName());
            }
            JLabel check = checkLabels.get(tag);
            if (check != null) check.setText(isNowChecked ? "✓" : "");
        }
    }

    /**
     * Show the popup with its top-left at the given screen point.
     * The window is sized by {@link #pack()} in the ctor; this just
     * positions and reveals it.
     */
    public void showAt(Point screenLocation)
    {
        setLocation(screenLocation);
        setVisible(true);
    }

    /**
     * Watches every JVM-wide MOUSE_PRESSED. Closes the popup when the
     * press isn't a descendant of the popup's root pane. AWT's
     * Toolkit event listener is the canonical way to detect "click
     * outside this window" because focus-based detection fights
     * against the popup's per-row clicks.
     */
    private void installClickAwayListener()
    {
        clickAwayListener = event ->
        {
            if (!(event instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent) event;
            if (me.getID() != MouseEvent.MOUSE_PRESSED) return;
            // SwingUtilities.isDescendingFrom is null-safe for the
            // window owner -- a click in the parent frame counts as
            // outside the popup, which is what we want.
            if (me.getComponent() == null) return;
            if (!SwingUtilities.isDescendingFrom(me.getComponent(), this))
            {
                dispose();
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(clickAwayListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void installEscListener()
    {
        // KeyEvent registered on the JWindow's root pane via
        // InputMap/ActionMap so it survives focus moving into nested
        // components (the rows are JPanels and would otherwise steal
        // focus).
        JPanel root = (JPanel) getContentPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closePopup");
        root.getActionMap().put("closePopup", new AbstractAction()
        {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });
    }

    @Override
    public void dispose()
    {
        if (clickAwayListener != null)
        {
            Toolkit.getDefaultToolkit().removeAWTEventListener(clickAwayListener);
            clickAwayListener = null;
        }
        super.dispose();
    }
}
