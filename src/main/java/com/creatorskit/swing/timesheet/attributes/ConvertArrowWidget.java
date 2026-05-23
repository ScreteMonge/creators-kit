package com.creatorskit.swing.timesheet.attributes;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Double-arrow conversion widget that sits between two fields. Clicking the
 * LEFT arrowhead converts right-to-left (e.g. Turn Rate -> Duration);
 * clicking the RIGHT arrowhead converts left-to-right (e.g. Duration -> Turn
 * Rate). Whichever half the cursor is over is highlighted in orange; the
 * other half stays white. Tooltip flips to describe the current half's
 * conversion direction as the cursor moves between them.
 *
 * <p>Direction routing is handled by the {@link Runnable} callbacks the
 * caller supplies (one for "convert left", one for "convert right"); the
 * widget owns only the visual + click-half detection.
 */
public class ConvertArrowWidget extends JComponent
{
    private static final Color BASE_COLOUR = Color.WHITE;
    private static final Color HOVER_COLOUR = new Color(255, 150, 30);  // orange

    private final Runnable onConvertLeft;
    private final Runnable onConvertRight;
    private final String leftTooltip;
    private final String rightTooltip;

    private int hoveredHalf = 0;  // -1 = left, 0 = none, 1 = right
    private boolean enabledByParent = true;

    /**
     * @param leftTooltip  shown when the cursor is over the left arrowhead
     * @param rightTooltip shown when the cursor is over the right arrowhead
     * @param onConvertLeft  fired by clicking the LEFT arrowhead
     * @param onConvertRight fired by clicking the RIGHT arrowhead
     */
    public ConvertArrowWidget(String leftTooltip, String rightTooltip,
                              Runnable onConvertLeft, Runnable onConvertRight)
    {
        this.leftTooltip = leftTooltip;
        this.rightTooltip = rightTooltip;
        this.onConvertLeft = onConvertLeft;
        this.onConvertRight = onConvertRight;

        Dimension d = new Dimension(34, 24);
        setPreferredSize(d);
        setMinimumSize(d);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                int newHalf = e.getX() < getWidth() / 2 ? -1 : 1;
                if (newHalf != hoveredHalf)
                {
                    hoveredHalf = newHalf;
                    setToolTipText(hoveredHalf == -1 ? leftTooltip : rightTooltip);
                    repaint();
                }
            }
        });
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseExited(MouseEvent e)
            {
                hoveredHalf = 0;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                if (!enabledByParent) return;
                if (e.getX() < getWidth() / 2)
                {
                    if (onConvertLeft != null) onConvertLeft.run();
                }
                else
                {
                    if (onConvertRight != null) onConvertRight.run();
                }
            }
        });
        setToolTipText(rightTooltip);  // default tooltip before any hover
    }

    public void setControlsEnabled(boolean enabled)
    {
        if (this.enabledByParent == enabled) return;
        this.enabledByParent = enabled;
        setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cy = h / 2;
            int margin = 4;

            float alpha = enabledByParent ? 1f : 0.4f;
            Color leftColour = applyAlpha(hoveredHalf == -1 ? HOVER_COLOUR : BASE_COLOUR, alpha);
            Color rightColour = applyAlpha(hoveredHalf == 1 ? HOVER_COLOUR : BASE_COLOUR, alpha);

            // Connecting shaft -- single horizontal stroke from left arrowhead tip
            // to right arrowhead tip. Half-coloured per hover state (left half uses
            // leftColour, right half uses rightColour) so the active half clearly
            // owns the line.
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(leftColour);
            g2.drawLine(margin, cy, w / 2, cy);
            g2.setColor(rightColour);
            g2.drawLine(w / 2, cy, w - margin, cy);

            // Left arrowhead (triangle).
            int headW = 6;
            int headH = 6;
            int[] leftX = new int[]{margin, margin + headW, margin + headW};
            int[] leftY = new int[]{cy, cy - headH, cy + headH};
            g2.setColor(leftColour);
            g2.fillPolygon(leftX, leftY, 3);

            // Right arrowhead (triangle).
            int[] rightX = new int[]{w - margin, w - margin - headW, w - margin - headW};
            int[] rightY = new int[]{cy, cy - headH, cy + headH};
            g2.setColor(rightColour);
            g2.fillPolygon(rightX, rightY, 3);
        }
        finally
        {
            g2.dispose();
        }
    }

    private static Color applyAlpha(Color base, float alpha)
    {
        int a = (int) (base.getAlpha() * alpha);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(255, a)));
    }
}
