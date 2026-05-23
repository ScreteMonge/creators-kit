package com.creatorskit.swing.timesheet.attributes;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Clickable compass for the Orientation card. Owns:
 * <ul>
 *   <li>a white circular border that visually contains the compass image;</li>
 *   <li>two coloured indicator lines (Start = green, End = red) drawn from
 *       centre outward at the Start / End spinner angles, repainted whenever
 *       either spinner changes;</li>
 *   <li>click-to-snap selection of one of the 8 cardinal / intercardinal
 *       directions, alternating each click between Start and End so the user
 *       can pick both with no UI toggle;</li>
 *   <li>a {@link #setControlsEnabled} override so the parent card can grey
 *       out the whole widget when Face Target overrides explicit orientation.</li>
 * </ul>
 *
 * <p>Angle convention follows {@link com.creatorskit.programming.orientation.Orientation}:
 * 0 = N, 512 = W, 1024 = S, 1536 = E (counter-clockwise from N in jagex units,
 * 2048 = full turn). The 8 click targets are spaced every 256 units.
 */
public class CompassPanel extends JComponent
{
    /** Full jagex turn = 2048 units, 8 directions = 256 each. */
    private static final int JAGEX_FULL_TURN = 2048;
    private static final int SNAP_STEP = JAGEX_FULL_TURN / 8;  // 256

    /** Indicator line colours -- chosen to read clearly against the dark compass face. */
    private static final Color START_COLOUR = new Color(80, 200, 95);   // green
    private static final Color END_COLOUR = new Color(220, 80, 80);     // red

    /** Where the next compass click writes its picked angle. */
    enum NextTarget { START, END }

    private final BufferedImage compassImage;
    private final JSpinner startSpinner;
    private final JSpinner endSpinner;
    private NextTarget nextTarget = NextTarget.START;
    private boolean enabledByParent = true;

    /**
     * @param compassImage the compass face PNG (drawn inside the circle)
     * @param startSpinner Start Orientation spinner -- click writes here on START turns,
     *                     line drawn green at this angle
     * @param endSpinner   End Orientation spinner -- click writes here on END turns,
     *                     line drawn red at this angle
     */
    public CompassPanel(BufferedImage compassImage, JSpinner startSpinner, JSpinner endSpinner)
    {
        this.compassImage = compassImage;
        this.startSpinner = startSpinner;
        this.endSpinner = endSpinner;

        // ~3x the old static label size so the 8 click targets are easy to hit
        // without precision aim. 120px diameter leaves room for the white border
        // around a 110px compass image.
        Dimension d = new Dimension(120, 120);
        setPreferredSize(d);
        setMinimumSize(d);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ChangeListener repaintOnChange = e -> repaint();
        startSpinner.addChangeListener(repaintOnChange);
        endSpinner.addChangeListener(repaintOnChange);

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (!enabledByParent) return;
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int dx = e.getX() - cx;
                int dy = e.getY() - cy;
                if (dx * dx + dy * dy < 25) return;  // dead-zone in centre to avoid noise

                int snapped = clickToSnappedJagex(dx, dy);
                if (nextTarget == NextTarget.START)
                {
                    startSpinner.setValue(snapped);
                    nextTarget = NextTarget.END;
                }
                else
                {
                    endSpinner.setValue(snapped);
                    nextTarget = NextTarget.START;
                }
                repaint();
            }
        });
    }

    /**
     * Greys-out the whole widget (visual + click no-op) when Face Target
     * is populated. Not the same as {@link #setEnabled} because that path
     * also affects child component look in some LaFs; this lets us keep the
     * compass image visible (just at reduced alpha) so the user can still
     * read the angles.
     */
    public void setControlsEnabled(boolean enabled)
    {
        if (this.enabledByParent == enabled) return;
        this.enabledByParent = enabled;
        setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    /** Maps a click offset (dx, dy from centre) to a snapped jagex angle (0..1792 in 256 steps). */
    private static int clickToSnappedJagex(int dx, int dy)
    {
        // atan2(-dx, -dy) inverts both axes so the result is jagex_rad, where
        // direction (sin, cos) = (-sin(rad), -cos(rad)) per the codebase's
        // orientation convention (N=0=-Y, W=512=-X, S=1024=+Y, E=1536=+X).
        double rad = Math.atan2(-dx, -dy);
        if (rad < 0) rad += 2 * Math.PI;
        int jagex = (int) Math.round(rad * JAGEX_FULL_TURN / (2 * Math.PI));
        // Snap to nearest 8-direction (multiple of 256).
        int snapped = ((int) Math.round(jagex / (double) SNAP_STEP)) * SNAP_STEP;
        snapped %= JAGEX_FULL_TURN;
        if (snapped < 0) snapped += JAGEX_FULL_TURN;
        return snapped;
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
            int size = Math.min(w, h);
            int cx = w / 2;
            int cy = h / 2;
            int circleR = size / 2 - 2;  // leave 2px gutter so the stroke isn't clipped
            int imageR = circleR - 4;    // inset the compass image slightly inside the circle

            float alpha = enabledByParent ? 1f : 0.4f;

            // White circular border that contains the compass.
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 255, 255, (int) (alpha * 220)));
            g2.drawOval(cx - circleR, cy - circleR, circleR * 2, circleR * 2);

            // Compass face.
            if (compassImage != null)
            {
                int imgSide = imageR * 2;
                java.awt.Composite prev = g2.getComposite();
                if (!enabledByParent)
                {
                    g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
                }
                g2.drawImage(compassImage, cx - imageR, cy - imageR, imgSide, imgSide, null);
                g2.setComposite(prev);
            }

            // Indicator lines: drawn last so they sit on top of the compass face.
            int startJ = readSpinnerInt(startSpinner, 0);
            int endJ = readSpinnerInt(endSpinner, 0);
            int lineR = imageR - 6;

            drawIndicator(g2, cx, cy, lineR, startJ, applyAlpha(START_COLOUR, alpha));
            drawIndicator(g2, cx, cy, lineR, endJ, applyAlpha(END_COLOUR, alpha));
        }
        finally
        {
            g2.dispose();
        }
    }

    private static int readSpinnerInt(JSpinner sp, int fallback)
    {
        Object v = sp.getValue();
        if (v instanceof Number) return ((Number) v).intValue();
        return fallback;
    }

    private static Color applyAlpha(Color base, float alpha)
    {
        int a = (int) (base.getAlpha() * alpha);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /**
     * Draws an indicator line from centre to the rim at the given jagex angle.
     * Convention: jagex 0 points up (N), 512 left (W), 1024 down (S), 1536 right (E)
     * -- counter-clockwise. The unit-direction formula
     * {@code (sin*-1, cos*-1)} matches {@link com.creatorskit.programming.orientation.Orientation}.
     */
    private static void drawIndicator(Graphics2D g2, int cx, int cy, int r, int jagex, Color color)
    {
        double rad = jagex * 2 * Math.PI / JAGEX_FULL_TURN;
        double ux = -Math.sin(rad);
        double uy = -Math.cos(rad);
        int endX = cx + (int) (r * ux);
        int endY = cy + (int) (r * uy);

        g2.setColor(color);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx, cy, endX, endY);

        // Small filled dot at the rim so the angle is readable even on dark backgrounds.
        int dot = 5;
        g2.fillOval(endX - dot / 2, endY - dot / 2, dot, dot);
    }
}
