package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.swing.timesheet.keyframe.CustomEasingCurve;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Modal Paint.NET-style curves editor for {@link CustomEasingCurve}. Opened
 * from the Camera kf's "Custom..." Easing option. Returns the curve to apply
 * to the kf (on Select), or {@code null} (on Discard).
 *
 * <p>Persistence: up to 6 preset slots live in the plugin config under
 * the {@link #CONFIG_KEY} key as a Gson-serialised JSON array; the dialog
 * reads them on open, writes back on Save. Slot index 0..5 in the array
 * corresponds to preset button 1..6 in the UI.
 *
 * <p>Spline interactions:
 * <ul>
 *   <li>Left-click on empty area: add a control point at that location.</li>
 *   <li>Left-click + drag a point: move it (x constrained to between
 *       neighbours; endpoints locked at (0,0) and (1,1)).</li>
 *   <li>Right-click on a point: remove it (endpoints excluded).</li>
 * </ul>
 *
 * <p>Buttons:
 * <ul>
 *   <li><b>Save</b>: writes the editor's current curve into the
 *       active preset slot. Active slot is whichever preset button the
 *       user last clicked; if none, Save is disabled.</li>
 *   <li><b>Select</b>: returns the editor's current curve (the dialog
 *       caller applies it to the kf).</li>
 *   <li><b>Discard changes</b>: returns null.</li>
 * </ul>
 */
public final class CurveEditorDialog
{
    private static final int CANVAS_W = 400;
    private static final int CANVAS_H = 400;
    private static final int POINT_RADIUS = 5;
    private static final int HIT_RADIUS = 10;
    private static final int CURVE_SAMPLES = 256;
    private static final int PRESET_COUNT = 6;

    private static final String CONFIG_GROUP = "creatorssuite";
    private static final String CONFIG_KEY = "customEasingPresets";

    private CurveEditorDialog() {}

    /**
     * Shows the modal editor. Blocks the caller until the dialog closes.
     * Returns the curve to apply (Select clicked) or {@code null} (Discard).
     */
    public static CustomEasingCurve show(Component parent, CustomEasingCurve initial,
                                          ConfigManager configManager, Gson gson)
    {
        return show(parent, initial, configManager, gson, -1);
    }

    /**
     * Variant that pre-selects {@code initialActiveSlot} (0..5) so Save
     * lands on the user's intended slot from the first click. Called by
     * the Easing dropdown when the user picks an empty "Preset N" and
     * the dialog opens to let them define it.
     */
    public static CustomEasingCurve show(Component parent, CustomEasingCurve initial,
                                          ConfigManager configManager, Gson gson,
                                          int initialActiveSlot)
    {
        // Load presets first -- the working-curve choice below depends on
        // whether the pre-selected slot has saved content.
        final CustomEasingCurve[] presets = loadPresets(configManager, gson);

        // Working-curve precedence:
        //   1. If a pre-selected slot has SAVED content, load that. This
        //      satisfies the "Custom... always loads at least one preset"
        //      contract: opening Custom on a kf using Preset N opens with
        //      Preset N visible (so editing starts from a known shape).
        //   2. Else if the caller passed a seed (e.g. the kf's existing
        //      non-preset curve), use that.
        //   3. Else identity (the fresh-start fallback).
        // The dialog mutates working[0]; the caller gets it (copied) on
        // Select, or nothing on Discard -- so case 1 doesn't risk losing
        // the kf's curve unless the user explicitly Selects.
        final CustomEasingCurve startCurve;
        if (initialActiveSlot >= 0 && initialActiveSlot < PRESET_COUNT && presets[initialActiveSlot] != null)
        {
            startCurve = presets[initialActiveSlot].copy();
        }
        else if (initial != null)
        {
            startCurve = initial.copy();
        }
        else
        {
            startCurve = new CustomEasingCurve();
        }
        final CustomEasingCurve[] working = { startCurve };
        final int[] activePreset = { initialActiveSlot >= 0 && initialActiveSlot < PRESET_COUNT ? initialActiveSlot : -1 };
        final boolean[] selected = { false };

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Edit custom easing curve", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(6, 6));
        dialog.setResizable(false);

        CurveCanvas canvas = new CurveCanvas(working);
        JPanel canvasWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        canvasWrap.setBorder(new EmptyBorder(8, 8, 0, 8));
        canvasWrap.add(canvas);
        dialog.add(canvasWrap, BorderLayout.NORTH);

        JLabel hint = new JLabel(
                "<html><div style='text-align:center'>"
                        + "Left-click empty area to add a control point. Drag points to move. "
                        + "Right-click a point to remove. Endpoints locked."
                        + "</div></html>", SwingConstants.CENTER);
        hint.setForeground(Color.LIGHT_GRAY);
        hint.setBorder(new EmptyBorder(4, 8, 4, 8));

        // Preset row.
        JPanel presetPanel = new JPanel(new GridLayout(1, PRESET_COUNT, 4, 0));
        presetPanel.setBorder(BorderFactory.createTitledBorder("Presets"));
        JToggleButton[] presetButtons = new JToggleButton[PRESET_COUNT];
        ButtonGroup presetGroup = new ButtonGroup();
        for (int i = 0; i < PRESET_COUNT; i++)
        {
            final int slot = i;
            JToggleButton b = new JToggleButton("Preset " + (i + 1));
            b.setFocusable(false);
            b.setToolTipText("<html>Click to load this preset. Click <b>Save</b> with it active to overwrite.</html>");
            b.addActionListener(e ->
            {
                activePreset[0] = slot;
                // Loading = pull the slot's curve into the editor. A null
                // slot (never saved) loads as a fresh identity so the user
                // can start designing without manually adding endpoints.
                CustomEasingCurve loaded = presets[slot] == null ? new CustomEasingCurve() : presets[slot].copy();
                working[0] = loaded;
                canvas.setCurve(working[0]);
                canvas.repaint();
            });
            presetButtons[i] = b;
            presetGroup.add(b);
            presetPanel.add(b);
        }

        // Action buttons.
        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("Overwrite the active preset with the current curve. Pick a preset slot above first.");
        saveButton.setEnabled(false);
        saveButton.addActionListener(e ->
        {
            int slot = activePreset[0];
            if (slot < 0) return;
            presets[slot] = working[0].copy();
            savePresets(configManager, gson, presets);
            // Visual ack: flash the button label briefly. No modal "saved"
            // popup -- the user is mid-design, popups would break flow.
            String prev = saveButton.getText();
            saveButton.setText("Saved");
            Timer t = new Timer(900, ev -> saveButton.setText(prev));
            t.setRepeats(false);
            t.start();
        });
        JButton selectButton = new JButton("Select");
        selectButton.setToolTipText("Apply the current curve to the keyframe and close.");
        selectButton.addActionListener(e ->
        {
            selected[0] = true;
            dialog.dispose();
        });
        JButton discardButton = new JButton("Discard changes");
        discardButton.setToolTipText("Close without applying. Preset saves you already made stay saved.");
        discardButton.addActionListener(e ->
        {
            selected[0] = false;
            dialog.dispose();
        });

        // Save enables when a preset is active.
        Runnable updateSaveEnabled = () -> saveButton.setEnabled(activePreset[0] >= 0);
        for (JToggleButton b : presetButtons)
        {
            b.addActionListener(e -> updateSaveEnabled.run());
        }
        // Reflect any caller-supplied initialActiveSlot so Save is usable
        // immediately and the matching preset button looks pressed.
        if (activePreset[0] >= 0)
        {
            presetButtons[activePreset[0]].setSelected(true);
            updateSaveEnabled.run();
        }

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        buttonRow.add(saveButton);
        buttonRow.add(selectButton);
        buttonRow.add(discardButton);

        JPanel south = new JPanel(new BorderLayout());
        south.add(hint, BorderLayout.NORTH);
        south.add(presetPanel, BorderLayout.CENTER);
        south.add(buttonRow, BorderLayout.SOUTH);
        dialog.add(south, BorderLayout.CENTER);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return selected[0] ? working[0].copy() : null;
    }

    // ===== Preset persistence ============================================

    /** Public so AttributePanel can match a kf's curve against the saved slots. */
    public static int presetCount() { return PRESET_COUNT; }

    public static CustomEasingCurve[] loadPresets(ConfigManager configManager, Gson gson)
    {
        CustomEasingCurve[] presets = new CustomEasingCurve[PRESET_COUNT];
        if (configManager == null) return presets;
        String raw = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
        if (raw == null || raw.isEmpty()) return presets;
        try
        {
            // JsonParser.parseString is Gson 2.8.6+. The plugin runtime ships
            // older Gson; use the instance form which has been around forever.
            JsonElement el = new JsonParser().parse(raw);
            if (!el.isJsonArray()) return presets;
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < Math.min(PRESET_COUNT, arr.size()); i++)
            {
                JsonElement slot = arr.get(i);
                if (slot == null || slot.isJsonNull()) continue;
                presets[i] = gson.fromJson(slot, CustomEasingCurve.class);
            }
        }
        catch (Exception ignored)
        {
            // Corrupt config (manual edit, version mismatch): treat as
            // empty rather than erroring out the dialog.
        }
        return presets;
    }

    private static void savePresets(ConfigManager configManager, Gson gson, CustomEasingCurve[] presets)
    {
        if (configManager == null) return;
        JsonArray arr = new JsonArray();
        for (int i = 0; i < PRESET_COUNT; i++)
        {
            arr.add(presets[i] == null ? null : gson.toJsonTree(presets[i]));
        }
        configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, arr.toString());
    }

    // ===== Canvas ========================================================

    /**
     * Curve-display panel. Draws axes / grid / diagonal reference / spline
     * fit / control points, and hosts the mouse listeners that mutate the
     * shared {@link CustomEasingCurve} reference.
     */
    private static final class CurveCanvas extends JPanel
    {
        private final CustomEasingCurve[] curveRef;
        private int dragIndex = -1;

        CurveCanvas(CustomEasingCurve[] curveRef)
        {
            this.curveRef = curveRef;
            setPreferredSize(new Dimension(CANVAS_W, CANVAS_H));
            setBackground(ColorScheme.DARK_GRAY_COLOR);
            setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    handlePress(e);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    dragIndex = -1;
                }
            });
            addMouseMotionListener(new MouseMotionAdapter()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    handleDrag(e);
                }
            });
        }

        void setCurve(CustomEasingCurve c)
        {
            curveRef[0] = c;
            dragIndex = -1;
        }

        // --- coordinate mapping ------------------------------------------
        // Curve space: x in [0,1], y in [0,1]. Pixel space: top-left origin.
        // Y is flipped: y=0 maps to bottom, y=1 maps to top, so the curve
        // reads as "progress vs time" with positive y going up like a graph.

        private double pxToCurveX(int px) { return (px - 4.0) / (getWidth() - 8.0); }
        private double pyToCurveY(int py) { return 1.0 - (py - 4.0) / (getHeight() - 8.0); }
        private int curveXToPx(double cx) { return (int) Math.round(4 + cx * (getWidth() - 8)); }
        private int curveYToPy(double cy) { return (int) Math.round(4 + (1.0 - cy) * (getHeight() - 8)); }

        private int hitTest(MouseEvent e)
        {
            CustomEasingCurve curve = curveRef[0];
            for (int i = 0; i < curve.size(); i++)
            {
                int px = curveXToPx(curve.xAt(i));
                int py = curveYToPy(curve.yAt(i));
                if (Math.hypot(e.getX() - px, e.getY() - py) <= HIT_RADIUS)
                {
                    return i;
                }
            }
            return -1;
        }

        private void handlePress(MouseEvent e)
        {
            CustomEasingCurve curve = curveRef[0];
            int hit = hitTest(e);
            if (SwingUtilities.isRightMouseButton(e))
            {
                // Right-click on a non-endpoint removes it. Endpoints
                // (index 0 and size-1) are locked at (0,0)/(1,1) so the
                // ease still starts and ends correctly.
                if (hit > 0 && hit < curve.size() - 1)
                {
                    removePoint(hit);
                    repaint();
                }
                return;
            }
            if (hit >= 0)
            {
                dragIndex = hit;
                return;
            }
            // Empty click: add a new point at the click location. Endpoints
            // are not movable, but you can still add points right next to
            // them; the spline interpolates fine with small spacings.
            double cx = clamp01(pxToCurveX(e.getX()));
            double cy = pyToCurveY(e.getY()); // y unclamped, allow overshoot for "bounce" feels
            if (cx <= 0.0 || cx >= 1.0) return; // can't add at the locked endpoints
            insertPoint(cx, cy);
            dragIndex = findIndexByX(cx);
            repaint();
        }

        private void handleDrag(MouseEvent e)
        {
            if (dragIndex < 0) return;
            CustomEasingCurve curve = curveRef[0];
            // Endpoints stay put.
            if (dragIndex == 0 || dragIndex == curve.size() - 1) return;

            double[] xs = new double[curve.size()];
            double[] ys = new double[curve.size()];
            for (int i = 0; i < curve.size(); i++) { xs[i] = curve.xAt(i); ys[i] = curve.yAt(i); }

            double newX = pxToCurveX(e.getX());
            double newY = pyToCurveY(e.getY());

            // Constrain x between immediate neighbours so the sequence stays
            // strictly ascending (the spline solver assumes that). A tiny
            // epsilon keeps the segments non-degenerate.
            double minX = xs[dragIndex - 1] + 1e-4;
            double maxX = xs[dragIndex + 1] - 1e-4;
            if (newX < minX) newX = minX;
            if (newX > maxX) newX = maxX;
            xs[dragIndex] = newX;
            ys[dragIndex] = newY;
            curve.setPoints(xs, ys);
            repaint();
        }

        private void insertPoint(double x, double y)
        {
            CustomEasingCurve curve = curveRef[0];
            int n = curve.size();
            // Locate the insert position so xs stays sorted.
            int pos = 0;
            while (pos < n && curve.xAt(pos) < x) pos++;
            double[] xs = new double[n + 1];
            double[] ys = new double[n + 1];
            for (int i = 0; i < pos; i++) { xs[i] = curve.xAt(i); ys[i] = curve.yAt(i); }
            xs[pos] = x;
            ys[pos] = y;
            for (int i = pos; i < n; i++) { xs[i + 1] = curve.xAt(i); ys[i + 1] = curve.yAt(i); }
            curve.setPoints(xs, ys);
        }

        private void removePoint(int idx)
        {
            CustomEasingCurve curve = curveRef[0];
            int n = curve.size();
            if (n <= 2) return; // never drop below the two endpoints
            double[] xs = new double[n - 1];
            double[] ys = new double[n - 1];
            int w = 0;
            for (int i = 0; i < n; i++)
            {
                if (i == idx) continue;
                xs[w] = curve.xAt(i);
                ys[w] = curve.yAt(i);
                w++;
            }
            curve.setPoints(xs, ys);
        }

        private int findIndexByX(double x)
        {
            CustomEasingCurve curve = curveRef[0];
            for (int i = 0; i < curve.size(); i++)
            {
                if (Math.abs(curve.xAt(i) - x) < 1e-9) return i;
            }
            return -1;
        }

        private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

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

                // Grid: 4x4 cells, dotted, mid-grey.
                g2.setColor(new Color(80, 80, 80));
                float[] dash = { 2f, 3f };
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, 0f));
                for (int i = 1; i < 4; i++)
                {
                    int gx = 4 + i * (w - 8) / 4;
                    int gy = 4 + i * (h - 8) / 4;
                    g2.drawLine(gx, 4, gx, h - 4);
                    g2.drawLine(4, gy, w - 4, gy);
                }

                // Diagonal identity reference (dashed).
                g2.setColor(new Color(110, 110, 110));
                g2.drawLine(4, h - 4, w - 4, 4);

                // Curve fit, sampled at CURVE_SAMPLES intervals.
                g2.setStroke(new BasicStroke(2.2f));
                g2.setColor(new Color(120, 140, 240));
                CustomEasingCurve curve = curveRef[0];
                int[] xpts = new int[CURVE_SAMPLES];
                int[] ypts = new int[CURVE_SAMPLES];
                for (int i = 0; i < CURVE_SAMPLES; i++)
                {
                    double tx = i / (double) (CURVE_SAMPLES - 1);
                    double ty = curve.evaluate(tx);
                    xpts[i] = curveXToPx(tx);
                    ypts[i] = curveYToPy(ty);
                }
                g2.drawPolyline(xpts, ypts, CURVE_SAMPLES);

                // Control points. Endpoints rendered a touch dimmer so the
                // user picks up on "those are locked" visually.
                for (int i = 0; i < curve.size(); i++)
                {
                    int px = curveXToPx(curve.xAt(i));
                    int py = curveYToPy(curve.yAt(i));
                    boolean endpoint = (i == 0 || i == curve.size() - 1);
                    g2.setColor(endpoint ? new Color(90, 110, 180) : new Color(160, 180, 255));
                    g2.fillOval(px - POINT_RADIUS, py - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
                    g2.setColor(Color.BLACK);
                    g2.drawOval(px - POINT_RADIUS, py - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
                }
            }
            finally
            {
                g2.dispose();
            }
        }
    }
}
