package com.creatorskit.swing;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Lightweight collapsible group used by the cache searcher's left
 * column. Header is a clickable {@link JLabel} with a chevron prefix
 * ({@code ▾ Expanded} / {@code ▸ Collapsed}); body hides when
 * collapsed. Same chevron style as the timeline's group rows so the
 * UI vocabulary stays consistent.
 *
 * <p>Re-creating one of these in code is preferred over Swing's
 * heavyweight CardLayout / accordion idioms -- there's only one
 * collapse state to track per instance, no animation, and no need
 * to interact with the layout manager beyond {@code revalidate()}.
 */
public class CollapsiblePanel extends JPanel
{
    private final JLabel header;
    private final JPanel body;
    private final String title;
    private boolean collapsed;

    public CollapsiblePanel(String title, JComponent content, boolean startCollapsed)
    {
        this.title = title;
        this.collapsed = startCollapsed;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        header = new JLabel();
        header.setOpaque(true);
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(FontManager.getRunescapeBoldFont());
        header.setBorder(new EmptyBorder(4, 6, 4, 6));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                setCollapsed(!collapsed);
            }
        });

        body = new JPanel(new BorderLayout());
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        body.add(content, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        refreshHeader();
        body.setVisible(!collapsed);
    }

    public boolean isCollapsed() { return collapsed; }

    public void setCollapsed(boolean collapsed)
    {
        if (this.collapsed == collapsed) return;
        this.collapsed = collapsed;
        body.setVisible(!collapsed);
        refreshHeader();
        revalidate();
        repaint();
    }

    private void refreshHeader()
    {
        header.setText((collapsed ? "▸ " : "▾ ") + title);
    }
}
