package com.creatorskit.swing.renderer;

import com.creatorskit.swing.colours.ColourSwapPanel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.ModelData;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class RenderPanel extends JPanel
{
    @Getter
    @Setter
    private ModelData model;
    private boolean modelExists = false;
    private final JSlider fovSlider;

    public static final double HEADING_DEFAULT = 0;
    public static final double PITCH_DEFAULT = 0;
    public static final double X_DEFAULT = 0;
    public static final double Y_DEFAULT = -90;
    public static final double Z_DEFAULT = 350;
    public static final int FOV_DEFAULT = 150;

    public final int MIN_CAMERA_DISTANCE = 50;

    private double heading = HEADING_DEFAULT;
    private double pitch = PITCH_DEFAULT;

    private double x = X_DEFAULT;
    private double y = Y_DEFAULT;
    private double z = Z_DEFAULT;

    private final int ROLL = 0;
    private final int ZOOM_FACTOR = 35;

    private double mouseX = 0;
    private double mouseY = 0;

    /**
     * Sentinel for "no colour selected for highlight". JagexColor packed shorts
     * span the full short range, so any value outside that range works -- we use
     * int instead of short so the sentinel doesn't collide with a real colour.
     */
    public static final int HIGHLIGHT_NONE = Integer.MIN_VALUE;
    private int highlightedColour = HIGHLIGHT_NONE;
    /**
     * When non-NONE, highlight faces whose ORIGINAL (pre-recolor) colour
     * matches this value, rather than their current rendered colour. Enables
     * per-face origin highlighting so a user clicking the Old swatch of row
     * X->Y only lights up the faces that were ORIGINALLY X, not every face
     * that currently renders as Y (other rules might also target Y). Takes
     * precedence over highlightedColour when set.
     */
    private int highlightedOriginalColour = HIGHLIGHT_NONE;

    /**
     * Per-pixel face-index buffer written alongside the z-buffer during the
     * rasterise pass. When the user clicks the panel we look up the face index
     * at the click point in O(1) and report its colour via {@link #onFaceClicked}.
     * Allocated lazily inside paintComponent so it always matches the current
     * viewport size.
     */
    private int[] faceIdBuffer;
    private int faceIdBufferWidth;

    /**
     * Listener for click-to-pick-colour. Invoked with the Jagex colour short of
     * whichever face the user left-clicked. Drags don't fire this -- only true
     * clicks (press + release without movement) per Swing's MouseListener
     * contract, so existing camera-rotate-on-drag stays intact.
     */
    @Setter
    private java.util.function.Consumer<Short> onFaceClicked;

    /**
     * Highlight tint applied to faces whose colour matches {@link #highlightedColour}.
     * Picked to contrast with most model palettes -- orange is the plugin's
     * accent colour and rarely clashes with NPC / item models.
     */
    private static final Color HIGHLIGHT_TINT = new Color(255, 152, 31);

    public RenderPanel(JSlider fovSlider)
    {
        this.fovSlider = fovSlider;

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                Point p = e.getLocationOnScreen();
                mouseX = p.getX();
                mouseY = p.getY();
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                // mouseClicked only fires on press + release without movement,
                // so dragging to rotate the camera doesn't trigger picking.
                if (faceIdBuffer == null || model == null || onFaceClicked == null) return;
                int x = e.getX();
                int y = e.getY();
                if (x < 0 || y < 0 || x >= faceIdBufferWidth) return;
                int idx = y * faceIdBufferWidth + x;
                if (idx < 0 || idx >= faceIdBuffer.length) return;
                int faceIdx = faceIdBuffer[idx];
                if (faceIdx < 0) return;
                // Report the ORIGINAL (pre-recolor) colour of the clicked face
                // when we have the snapshot -- enables the swap-list highlight
                // to find the SPECIFIC row that caused this face's colour, even
                // if many Old rows swap to the same New colour. Falls back to
                // the current (post-swap) face colour when no snapshot exists,
                // matching the old single-colour behavior.
                short colourToReport;
                if (originalFaceColors != null && faceIdx < originalFaceColors.length)
                {
                    colourToReport = originalFaceColors[faceIdx];
                }
                else
                {
                    short[] colours = model.getFaceColors();
                    if (colours == null || faceIdx >= colours.length) return;
                    colourToReport = colours[faceIdx];
                }
                onFaceClicked.accept(colourToReport);
            }
        });

        addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                Point p = e.getLocationOnScreen();
                double dx = p.getX() - mouseX;
                double dy = p.getY() - mouseY;

                heading = heading + dx;
                pitch = pitch - dy;

                mouseX = (int) p.getX();
                mouseY = (int) p.getY();
                repaint();
            }
        });

        addMouseWheelListener(e ->
        {
            z = Math.max(z + ZOOM_FACTOR * e.getWheelRotation(), MIN_CAMERA_DISTANCE);
            repaint();
        });

        fovSlider.addChangeListener(e -> repaint());
    }

    /**
     * Pre-recolor snapshot of face colours, parallel to model.getFaceColors().
     * Used by mouseClicked to report the ORIGINAL colour of a clicked face
     * rather than its post-swap rendered colour -- lets the swap-list highlight
     * find the SPECIFIC Old colour row that the face originated from, even when
     * multiple Old colours all swap to the same New colour. Null when the
     * caller can't supply the snapshot (older code path / merge mismatch); the
     * click handler then falls back to current-colour reporting.
     */
    private short[] originalFaceColors;

    public void updateModel(ModelData md)
    {
        updateModel(md, null);
    }

    public void updateModel(ModelData md, short[] originalFaceColors)
    {
        modelExists = true;
        model = md;
        this.originalFaceColors = originalFaceColors;
        repaint();
        revalidate();
    }

    public void resetViewer()
    {
        modelExists = false;
        repaint();
        revalidate();
    }

    /**
     * Sets which Jagex colour is currently being "highlighted" -- the faces
     * whose CURRENT (post-swap) colour matches render with
     * {@link #HIGHLIGHT_TINT} instead of their normal shading.
     * Clears any active origin-colour highlight. Pass {@link #HIGHLIGHT_NONE}
     * to clear.
     */
    public void setHighlightedColour(int colour)
    {
        boolean changed = this.highlightedColour != colour || this.highlightedOriginalColour != HIGHLIGHT_NONE;
        this.highlightedColour = colour;
        this.highlightedOriginalColour = HIGHLIGHT_NONE;
        if (changed) repaint();
    }

    /**
     * Sets which ORIGINAL (pre-swap) Jagex colour to highlight. Faces whose
     * pre-recolor face colour matches render with {@link #HIGHLIGHT_TINT}.
     * Requires the per-face origin snapshot populated by
     * {@link #updateModel(ModelData, short[])} -- if no snapshot, falls back
     * to no highlight (caller should use setHighlightedColour instead).
     * Clears any active effective-colour highlight.
     */
    public void setHighlightedOriginalColour(int colour)
    {
        boolean changed = this.highlightedOriginalColour != colour || this.highlightedColour != HIGHLIGHT_NONE;
        this.highlightedOriginalColour = colour;
        this.highlightedColour = HIGHLIGHT_NONE;
        if (changed) repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (model == null || !modelExists)
        {
            g.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 16));
            g.setColor(ColorScheme.BRAND_ORANGE);
            FontMetrics fm = g.getFontMetrics();
            String s = "No Model";

            g.drawString(s, this.getWidth() / 2 - fm.stringWidth(s) / 2, this.getHeight() / 2 + fm.getHeight() / 2);
            return;
        }

        double convertedHeading = Math.toRadians(heading);
        Matrix4 headingTransform = new Matrix4(new double[] {
                Math.cos(convertedHeading), 0, -Math.sin(convertedHeading), 0,
                0, 1, 0, 0,
                Math.sin(convertedHeading), 0, Math.cos(convertedHeading), 0,
                0, 0, 0, 1
        });

        double convertedPitch = Math.toRadians(pitch);
        Matrix4 pitchTransform = new Matrix4(new double[] {
                1, 0, 0, 0,
                0, Math.cos(convertedPitch), Math.sin(convertedPitch), 0,
                0, -Math.sin(convertedPitch), Math.cos(convertedPitch), 0,
                0, 0, 0, 1
        });

        double roll = ROLL;
        Matrix4 rollTransform = new Matrix4(new double[] {
                Math.cos(roll), -Math.sin(roll), 0, 0,
                Math.sin(roll), Math.cos(roll), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        });

        Matrix4 panTransform = new Matrix4(new double[] {
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                -x, -y, -z, 1
        });

        double viewportWidth = getWidth();
        double viewportHeight = getHeight();
        double fovAngle = Math.toRadians(fovSlider.getValue());
        double fov = Math.tan(fovAngle / 2) * 170;

        Matrix4 transform =
                headingTransform
                        .multiply(pitchTransform)
                        .multiply(panTransform);

        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

        double[] zBuffer = new double[img.getWidth() * img.getHeight()];

        // Parallel face-index buffer: stores the index of whichever face is
        // visible at each pixel, mirroring the z-buffer updates. Click-picking
        // reads this in O(1). -1 = no face (background). Allocated fresh each
        // frame so resizing the panel doesn't leave stale dimensions.
        int[] localFaceIds = new int[img.getWidth() * img.getHeight()];

        for (int q = 0; q < zBuffer.length; q++)
        {
            zBuffer[q] = Double.NEGATIVE_INFINITY;
            localFaceIds[q] = -1;
        }

        short[] colours = model.getFaceColors();
        int[] f1 = model.getFaceIndices1();
        int[] f2 = model.getFaceIndices2();
        int[] f3 = model.getFaceIndices3();

        float[] mx = model.getVerticesX();
        float[] my = model.getVerticesY();
        float[] mz = model.getVerticesZ();

        ArrayList<Triangle> tris = new ArrayList<>();
        for (int i = 0; i < model.getFaceCount(); i++)
        {
            int vert1 = f1[i];
            int vert2 = f2[i];
            int vert3 = f3[i];

            double v1x = mx[vert1];
            double v1y = my[vert1];
            double v1z = -mz[vert1];

            double v2x = mx[vert2];
            double v2y = my[vert2];
            double v2z = -mz[vert2];

            double v3x = mx[vert3];
            double v3y = my[vert3];
            double v3z = -mz[vert3];

            Color color = ColourSwapPanel.colourFromShort(colours[i]);

            tris.add(new Triangle(
                    new Vertex(v1x, v1y, v1z, 1),
                    new Vertex(v2x, v2y, v2z, 1),
                    new Vertex(v3x, v3y, v3z, 1),
                    color,
                    i,
                    colours[i]
            ));
        }

        for (Triangle t : tris)
        {
            Vertex v1 = transform.transform(t.v1);
            Vertex v2 = transform.transform(t.v2);
            Vertex v3 = transform.transform(t.v3);

            double NEAR_CULL_DISTANCE = 50.0;
            if (v1.z > -NEAR_CULL_DISTANCE && v2.z > -NEAR_CULL_DISTANCE && v3.z > -NEAR_CULL_DISTANCE)
            {
                continue;
            }

            Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z, v2.w - v1.w);
            Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z, v3.w - v1.w);
            Vertex norm = new Vertex(
                    ab.y * ac.z - ab.z * ac.y,
                    ab.z * ac.x - ab.x * ac.z,
                    ab.x * ac.y - ab.y * ac.x,
                    1
            );
            double normalLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
            norm.x /= normalLength;
            norm.y /= normalLength;
            norm.z /= normalLength;

            double angleCos = Math.abs(norm.z);

            v1.x = v1.x / (-v1.z) * fov;
            v1.y = v1.y / (-v1.z) * fov;
            v2.x = v2.x / (-v2.z) * fov;
            v2.y = v2.y / (-v2.z) * fov;
            v3.x = v3.x / (-v3.z) * fov;
            v3.y = v3.y / (-v3.z) * fov;

            v1.x += viewportWidth / 2;
            v1.y += viewportHeight / 2;
            v2.x += viewportWidth / 2;
            v2.y += viewportHeight / 2;
            v3.x += viewportWidth / 2;
            v3.y += viewportHeight / 2;

            if ((v1.x < 0   && v2.x < 0   && v3.x < 0) ||
                    (v1.x >= viewportWidth  && v2.x >= viewportWidth  && v3.x >= viewportWidth) ||
                    (v1.y < 0   && v2.y < 0   && v3.y < 0) ||
                    (v1.y >= viewportHeight && v2.y >= viewportHeight))
            {
                continue;
            }

            int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
            int maxX = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
            int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
            int maxY = (int) Math.min(img.getHeight() - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

            double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

            for (int y = minY; y <= maxY; y++)
            {
                for (int x = minX; x <= maxX; x++)
                {
                    double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                    double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                    double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                    if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1)
                    {
                        double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                        int zIndex = y * img.getWidth() + x;
                        if (zBuffer[zIndex] < depth)
                        {
                            // Highlighted faces ignore angleCos shading and
                            // render in HIGHLIGHT_TINT. Two highlight modes:
                            // - effective: matches faces whose CURRENT colour
                            //   equals highlightedColour (legacy / fallback).
                            // - original: matches faces whose PRE-SWAP colour
                            //   equals highlightedOriginalColour (per-face
                            //   origin specificity -- only the faces actually
                            //   contributing to a specific swap rule light up,
                            //   not every face that visually looks the same).
                            // Comparisons use packed shorts to avoid HSL drift.
                            boolean hit;
                            if (highlightedOriginalColour != HIGHLIGHT_NONE
                                    && originalFaceColors != null
                                    && t.faceIndex < originalFaceColors.length)
                            {
                                hit = originalFaceColors[t.faceIndex] == (short) highlightedOriginalColour;
                            }
                            else
                            {
                                hit = highlightedColour != HIGHLIGHT_NONE
                                        && t.faceColour == (short) highlightedColour;
                            }
                            Color rasterColor = hit ? HIGHLIGHT_TINT : getShade(t.color, angleCos);
                            img.setRGB(x, y, rasterColor.getRGB());
                            zBuffer[zIndex] = depth;
                            localFaceIds[zIndex] = t.faceIndex;
                        }
                    }
                }
            }

        }

        // Publish the face-id buffer so mouseClicked can read it. Storing the
        // width separately because img.getWidth() depends on a still-valid
        // reference, while the buffer outlives the BufferedImage (it's only
        // freed when paintComponent re-allocates next frame).
        this.faceIdBuffer = localFaceIds;
        this.faceIdBufferWidth = img.getWidth();

        g2.drawImage(img, 0, 0, null);
    }

    public static Color getShade(Color color, double shade)
    {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLinear, 1.0/2.4);
        int green = (int) Math.pow(greenLinear, 1.0/2.4);
        int blue = (int) Math.pow(blueLinear, 1.0/2.4);

        return new Color(
                Math.min(255, Math.max(0, red)),
                Math.min(255, Math.max(0, green)),
                Math.min(255, Math.max(0, blue)));
    }

    public void resetCameraView()
    {
        heading = HEADING_DEFAULT;
        pitch = PITCH_DEFAULT;
        fovSlider.setValue(FOV_DEFAULT);
        x = X_DEFAULT;
        y = Y_DEFAULT;
        z = Z_DEFAULT;
        repaint();
    }
}

class Matrix4
{
    double[] values;
    Matrix4(double[] values)
    {
        this.values = values;
    }

    Matrix4 multiply(Matrix4 other)
    {
        double[] result = new double[16];
        for (int row = 0; row < 4; row++)
        {
            for (int col = 0; col < 4; col++)
            {
                for (int i = 0; i < 4; i++)
                {
                    result[row * 4 + col] += this.values[row * 4 + i] * other.values[i * 4 + col];
                }
            }
        }
        return new Matrix4(result);
    }

    Vertex transform(Vertex in)
    {
        return new Vertex(
                in.x * values[0] + in.y * values[4] + in.z * values[8] + in.w * values[12],
                in.x * values[1] + in.y * values[5] + in.z * values[9] + in.w * values[13],
                in.x * values[2] + in.y * values[6] + in.z * values[10] + in.w * values[14],
                in.x * values[3] + in.y * values[7] + in.z * values[11] + in.w * values[15]
        );
    }

    @Override public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                sb.append(values[row * 4 + col]);
                if (col != 3) {
                    sb.append(",");
                }
            }
            if (row != 3) {
                sb.append(";\n ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}

class Triangle
{
    Vertex v1;
    Vertex v2;
    Vertex v3;
    Color color;
    /** Original face index in the source ModelData -- written to the face-id buffer for click-picking. */
    int faceIndex;
    /** Original packed-Jagex colour of this face -- used to compare against the highlight target. */
    short faceColour;

    Triangle (Vertex v1, Vertex v2, Vertex v3, Color color, int faceIndex, short faceColour)
    {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
        this.faceIndex = faceIndex;
        this.faceColour = faceColour;
    }
}

class Vertex
{
    double x;
    double y;
    double z;
    double w;

    Vertex(double x, double y, double z, double w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
}
