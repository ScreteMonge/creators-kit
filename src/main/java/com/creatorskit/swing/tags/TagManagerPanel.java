package com.creatorskit.swing.tags;

import com.creatorskit.cache.metadata.CacheMetadataStore;
import com.creatorskit.cache.metadata.Tag;
import com.creatorskit.cache.metadata.TagColor;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Compact Tag Manager UI. Lives in a collapsible panel under the
 * Cache Searcher's Model Id Breakdown column.
 *
 * <ul>
 *   <li>Six clickable colour swatches; clicking one selects the
 *       colour for the next Create Tag.</li>
 *   <li>20-char-max text field for the tag name (truncated harder by
 *       the store; the UI also reject typed overflow).</li>
 *   <li><b>Create Tag</b> button -- adds a new tag with the current
 *       name + colour, then resets the name field. Disabled when the
 *       name is empty or duplicates an existing tag.</li>
 *   <li><b>Delete Tag</b> button -- removes the tag currently
 *       selected in the dropdown (including stripping it from every
 *       entry's assignment set).</li>
 *   <li>Dropdown listing all tags as {@code "● TagName"} where the
 *       bullet is rendered in the tag's colour. Sorted alphabetically
 *       when expanded; the collapsed value field shows the most
 *       recently created tag.</li>
 * </ul>
 *
 * <p>The panel subscribes to {@link CacheMetadataStore} change events
 * so tag creation / deletion from anywhere else re-renders the
 * dropdown immediately.
 */
public class TagManagerPanel extends JPanel
{
    private final CacheMetadataStore store;
    private final JTextField nameField = new JTextField();
    private final JComboBox<Tag> dropdown = new JComboBox<>();
    private final JButton createButton = new JButton("Create Tag");
    private final JButton deleteButton = new JButton("Delete Tag");
    private final JPanel swatchRow = new JPanel(new GridLayout(1, 6, 4, 0));
    private final JLabel[] swatches = new JLabel[TagColor.values().length];
    private TagColor selectedColor = TagColor.RED;

    /** Set true while we rebuild the dropdown so item-listener events from setModel don't fire user handlers. */
    private boolean rebuilding = false;

    public TagManagerPanel(CacheMetadataStore store)
    {
        this.store = store;
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(6, 8, 6, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        buildSwatchRow();
        buildNameField();
        buildButtons();
        buildDropdown();

        layoutComponents();

        rebuildDropdown();
        refreshButtonStates();

        // Re-render on any store mutation (tag added/deleted elsewhere).
        store.addListener(() -> SwingUtilities.invokeLater(this::rebuildDropdown));
    }

    // ----- construction helpers -----

    private void buildSwatchRow()
    {
        swatchRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        TagColor[] colors = TagColor.values();
        for (int i = 0; i < colors.length; i++)
        {
            final TagColor c = colors[i];
            JLabel sw = new JLabel();
            sw.setOpaque(true);
            sw.setBackground(c.awt);
            sw.setPreferredSize(new Dimension(20, 20));
            sw.setBorder(new LineBorder(Color.BLACK, 1));
            sw.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            sw.setToolTipText(c.name().toLowerCase());
            sw.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    selectedColor = c;
                    refreshSwatchSelection();
                }
            });
            swatches[i] = sw;
            swatchRow.add(sw);
        }
        refreshSwatchSelection();
    }

    private void refreshSwatchSelection()
    {
        TagColor[] colors = TagColor.values();
        for (int i = 0; i < colors.length; i++)
        {
            // Selected swatch gets a white border to stand out.
            swatches[i].setBorder(new LineBorder(
                    colors[i] == selectedColor ? Color.WHITE : Color.BLACK, 2));
        }
    }

    private void buildNameField()
    {
        nameField.setFont(FontManager.getRunescapeFont());
        // Layout-only sizing hint (~chars wide). The hard cap on actual
        // accepted input comes from CacheMetadataStore.MAX_TAG_NAME_LENGTH
        // via the DocumentFilter below.
        nameField.setColumns(12);
        // Hard cap: every keystroke past 8 chars is dropped. The store
        // ALSO trims defensively to the same length.
        ((javax.swing.text.AbstractDocument) nameField.getDocument()).setDocumentFilter(
                new javax.swing.text.DocumentFilter()
                {
                    @Override
                    public void insertString(FilterBypass fb, int offset, String string, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException
                    {
                        if (string == null) return;
                        int allowed = CacheMetadataStore.MAX_TAG_NAME_LENGTH - fb.getDocument().getLength();
                        if (allowed <= 0) return;
                        super.insertString(fb, offset, string.substring(0, Math.min(string.length(), allowed)), attr);
                    }

                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String string, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException
                    {
                        if (string == null) string = "";
                        int allowed = CacheMetadataStore.MAX_TAG_NAME_LENGTH - (fb.getDocument().getLength() - length);
                        if (allowed <= 0) { super.replace(fb, offset, length, "", attr); return; }
                        super.replace(fb, offset, length, string.substring(0, Math.min(string.length(), allowed)), attr);
                    }
                });
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
        {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshButtonStates(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshButtonStates(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshButtonStates(); }
        });
    }

    private void buildButtons()
    {
        createButton.setFont(FontManager.getRunescapeFont());
        createButton.addActionListener(e -> {
            String name = nameField.getText();
            Tag created = store.addTag(name, selectedColor);
            if (created != null)
            {
                nameField.setText("");
                // rebuildDropdown will fire via the store listener; in
                // case anyone is suppressing listeners we also bump
                // selection to the new tag explicitly.
                SwingUtilities.invokeLater(() ->
                {
                    dropdown.setSelectedItem(created);
                    refreshButtonStates();
                });
            }
        });

        deleteButton.setFont(FontManager.getRunescapeFont());
        deleteButton.addActionListener(e -> {
            Object sel = dropdown.getSelectedItem();
            if (sel instanceof Tag) store.deleteTag(((Tag) sel).getName());
        });
    }

    private void buildDropdown()
    {
        dropdown.setRenderer(new TagListCellRenderer());
        dropdown.setMaximumRowCount(12);
        dropdown.addActionListener(e -> { if (!rebuilding) refreshButtonStates(); });
    }

    private void layoutComponents()
    {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        add(swatchRow, c);

        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        add(new JLabel("Name:"), c);
        c.gridx = 1;
        c.weightx = 1;
        add(nameField, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 1;
        add(createButton, c);
        c.gridx = 1;
        add(deleteButton, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1;
        add(dropdown, c);
    }

    // ----- state refresh -----

    /**
     * Rebuild the dropdown to reflect the store's current tags.
     * Latest-created tag is pre-selected so the collapsed combo box
     * reads as "the tag I just made"; expanding sorts alphabetically.
     */
    private void rebuildDropdown()
    {
        rebuilding = true;
        try
        {
            Tag latest = store.getLatestTag();
            List<Tag> alpha = store.getTagsAlphabetical();
            dropdown.removeAllItems();
            for (Tag t : alpha) dropdown.addItem(t);
            if (latest != null) dropdown.setSelectedItem(latest);
        }
        finally
        {
            rebuilding = false;
        }
        refreshButtonStates();
    }

    private void refreshButtonStates()
    {
        String name = nameField.getText().trim();
        boolean canCreate = !name.isEmpty() && store.findTag(name) == null;
        createButton.setEnabled(canCreate);
        deleteButton.setEnabled(dropdown.getSelectedItem() instanceof Tag);
    }

    // ----- cell renderer for tag dropdown -----

    /**
     * Renders each tag as a coloured bullet glyph followed by the
     * name. The bullet is drawn directly via a custom renderer
     * (rather than relying on HTML colour codes) so the size + spacing
     * are tighter than HTML's default rendering.
     */
    private static final class TagListCellRenderer extends JLabel implements ListCellRenderer<Tag>
    {
        TagListCellRenderer()
        {
            setOpaque(true);
            setBorder(new EmptyBorder(2, 4, 2, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Tag> list, Tag tag, int index, boolean isSelected, boolean cellHasFocus)
        {
            if (tag == null)
            {
                setText(" ");
                setBackground(isSelected ? Color.DARK_GRAY : ColorScheme.DARKER_GRAY_COLOR);
                return this;
            }
            // The bullet glyph (●) is rendered in the tag's hex via
            // inline HTML; rest of the label uses the default fg.
            setText("<html><span style=\"color:" + tag.getColor().hex + "; font-size: 14px;\">&#9679;</span> "
                    + tag.getName() + "</html>");
            setForeground(Color.WHITE);
            setBackground(isSelected ? Color.DARK_GRAY : ColorScheme.DARKER_GRAY_COLOR);
            return this;
        }
    }
}
