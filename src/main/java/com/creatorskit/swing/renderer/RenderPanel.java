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
    public static final double Z_DEFAULT = 275;
    public static final int FOV_DEFAULT = 150;

    private double heading = 0;
    private double pitch = 0;

    private double x = 0;
    private double y = -90;
    private double z = 275;

    private final int ROLL = 0;
    private final int ZOOM_FACTOR = 25;

    private double mouseX = 0;
    private double mouseY = 0;

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
            z += ZOOM_FACTOR * e.getWheelRotation();
            repaint();
        });

        fovSlider.addChangeListener(e -> repaint());
    }

    public void updateModel(ModelData md)
    {
        modelExists = true;
        model = md;
        repaint();
        revalidate();
    }

    public void resetViewer()
    {
        modelExists = false;
        repaint();
        revalidate();
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

        for (int q = 0; q < zBuffer.length; q++)
        {
            zBuffer[q] = Double.NEGATIVE_INFINITY;
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
                    color
            ));
        }

        for (Triangle t : tris)
        {
            Vertex v1 = transform.transform(t.v1);
            Vertex v2 = transform.transform(t.v2);
            Vertex v3 = transform.transform(t.v3);

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
                            img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
                            zBuffer[zIndex] = depth;
                        }
                    }
                }
            }

        }

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

    Triangle (Vertex v1, Vertex v2, Vertex v3, Color color)
    {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
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
