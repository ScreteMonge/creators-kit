package com.creatorskit.swing.renderer;

import com.creatorskit.swing.colours.ColourSwapPanel;
import net.runelite.api.Model;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class RenderUtilities
{
    private static final int ROLL = 0;

    public static BufferedImage render(BufferedImage img, ArrayList<Triangle> tris, double heading, double pitch, double x, double y, double z, double viewportWidth, double viewportHeight, int fov)
    {
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

        double fovAngle = Math.toRadians(fov);
        double fovAdjusted = Math.tan(fovAngle / 2) * 170;

        Matrix4 rotation = headingTransform.multiply(pitchTransform);
        Matrix4 transform = rotation.multiply(panTransform);

        img = new BufferedImage((int) viewportWidth, (int) viewportHeight, BufferedImage.TYPE_INT_ARGB);

        double[] zBuffer = new double[img.getWidth() * img.getHeight()];

        for (int q = 0; q < zBuffer.length; q++)
        {
            zBuffer[q] = Double.NEGATIVE_INFINITY;
        }

        if (tris == null)
        {
            return img;
        }

        ArrayList<TriangleRender> opaque = new ArrayList<>();
        ArrayList<TriangleRender> transparent = new ArrayList<>();

        for (int i = 0; i < tris.size(); i++)
        {
            Triangle t = tris.get(i);
            Vertex v1 = transform.transform(t.v1);
            Vertex v2 = transform.transform(t.v2);
            Vertex v3 = transform.transform(t.v3);

            double NEAR_CULL_DISTANCE = 50.0;
            if (v1.z > -NEAR_CULL_DISTANCE && v2.z > -NEAR_CULL_DISTANCE && v3.z > -NEAR_CULL_DISTANCE)
            {
                continue;
            }

            v1.x = v1.x / (-v1.z) * fovAdjusted;
            v1.y = v1.y / (-v1.z) * fovAdjusted;
            v2.x = v2.x / (-v2.z) * fovAdjusted;
            v2.y = v2.y / (-v2.z) * fovAdjusted;
            v3.x = v3.x / (-v3.z) * fovAdjusted;
            v3.y = v3.y / (-v3.z) * fovAdjusted;

            v1.x += viewportWidth / 2;
            v1.y += viewportHeight / 2;
            v2.x += viewportWidth / 2;
            v2.y += viewportHeight / 2;
            v3.x += viewportWidth / 2;
            v3.y += viewportHeight / 2;

            double ax = v2.x - v1.x;
            double ay = v2.y - v1.y;
            double bx = v3.x - v1.x;
            double by = v3.y - v1.y;

            double cross = ax * by - ay * bx;

            if (cross >= 0)
            {
                continue;
            }

            if ((v1.x < 0   && v2.x < 0   && v3.x < 0) ||
                    (v1.x >= viewportWidth  && v2.x >= viewportWidth  && v3.x >= viewportWidth) ||
                    (v1.y < 0   && v2.y < 0   && v3.y < 0) ||
                    (v1.y >= viewportHeight && v2.y >= viewportHeight))
            {
                continue;
            }

            double avgDepth = (v1.z + v2.z + v3.z) / 3.0;

            Vector3 n1t = transformNormal(rotation, t.n1);
            Vector3 n2t = transformNormal(rotation, t.n2);
            Vector3 n3t = transformNormal(rotation, t.n3);

            TriangleRender rt = new TriangleRender(
                    v1, v2, v3,
                    t.c1, t.c2, t.c3,
                    n1t, n2t, n3t,
                    t.alpha,
                    avgDepth
            );

            if (t.alpha == 255)
            {
                opaque.add(rt);
            }
            else
            {
                transparent.add(rt);
            }
        }

        renderTriangles(opaque, img, zBuffer, rotation, false);
        transparent.sort((a, b) -> Double.compare(a.avgDepth, b.avgDepth));
        renderTriangles(transparent, img, zBuffer, rotation, true);
        return img;
    }

    private static void renderTriangles(ArrayList<TriangleRender> triangleRenders, BufferedImage img, double[] zBuffer, Matrix4 rotation, boolean transparent)
    {
        for (TriangleRender t : triangleRenders)
        {
            Vertex v1 = t.v1;
            Vertex v2 = t.v2;
            Vertex v3 = t.v3;

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
                            Vector3 normal = interpolateNormal(t.n1, t.n2, t.n3, b1, b2, b3);
                            normal.normalize();

                            Color c = interpolateColor(t.c1, t.c2, t.c3, b1, b2, b3, t.alpha);

                            if (transparent)
                            {
                                int dstARGB = img.getRGB(x, y);
                                int blended = blend(c, dstARGB);
                                img.setRGB(x, y, blended);
                            }
                            else
                            {
                                img.setRGB(x, y, c.getRGB());
                                zBuffer[zIndex] = depth;
                            }
                        }
                    }
                }
            }
        }
    }

    public static ArrayList<Triangle> buildTriangleList(Model model)
    {
        int fc = model.getFaceCount();
        int vc = model.getVerticesCount();

        int[] f1 = Arrays.copyOf(model.getFaceIndices1(), fc);
        int[] f2 = Arrays.copyOf(model.getFaceIndices2(), fc);
        int[] f3 = Arrays.copyOf(model.getFaceIndices3(), fc);
        float[] mx = Arrays.copyOf(model.getVerticesX(), vc);
        float[] my = Arrays.copyOf(model.getVerticesY(), vc);
        float[] mz = Arrays.copyOf(model.getVerticesZ(), vc);
        double[] nx = new double[vc];
        double[] ny = new double[vc];
        double[] nz = new double[vc];

        int[] col1 = Arrays.copyOf(model.getFaceColors1(), fc);
        int[] col2 = Arrays.copyOf(model.getFaceColors2(), fc);
        int[] col3 = Arrays.copyOf(model.getFaceColors3(), fc);

        short[] c1 = new short[fc];
        short[] c2 = new short[fc];
        short[] c3 = new short[fc];

        boolean transparencyExists = false;
        byte[] faceAlpha = new byte[fc];
        Arrays.fill(faceAlpha, (byte) 255);

        if (model.getFaceTransparencies() != null)
        {
            transparencyExists = true;
            faceAlpha = Arrays.copyOf(model.getFaceTransparencies(), fc);
        }

        for (int i = 0; i < fc; i++)
        {
            c1[i] = (short) col1[i];
            c2[i] = (short) col2[i];
            c3[i] = (short) col3[i];
        }

        for (int i = 0; i < f1.length; i++)
        {
            int v1 = f1[i];
            int v2 = f2[i];
            int v3 = f3[i];

            double x1 = mx[v1], y1 = my[v1], z1 = mz[v1];
            double x2 = mx[v2], y2 = my[v2], z2 = mz[v2];
            double x3 = mx[v3], y3 = my[v3], z3 = mz[v3];

            double ax = x2 - x1;
            double ay = y2 - y1;
            double az = z2 - z1;

            double bx = x3 - x1;
            double by = y3 - y1;
            double bz = z3 - z1;

            double nxFace = ay * bz - az * by;
            double nyFace = az * bx - ax * bz;
            double nzFace = ax * by - ay * bx;

            nx[v1] += nxFace; ny[v1] += nyFace; nz[v1] += nzFace;
            nx[v2] += nxFace; ny[v2] += nyFace; nz[v2] += nzFace;
            nx[v3] += nxFace; ny[v3] += nyFace; nz[v3] += nzFace;
        }

        for (int i = 0; i < nx.length; i++)
        {
            double len = Math.sqrt(nx[i]*nx[i] + ny[i]*ny[i] + nz[i]*nz[i]);
            if (len != 0) {
                nx[i] /= len;
                ny[i] /= len;
                nz[i] /= len;
            }
        }

        ArrayList<Triangle> tris = new ArrayList<>();
        for (int i = 0; i < fc; i++)
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

            Vector3 n1 = new Vector3(nx[vert1], ny[vert1], nz[vert1]);
            Vector3 n2 = new Vector3(nx[vert2], ny[vert2], nz[vert2]);
            Vector3 n3 = new Vector3(nx[vert3], ny[vert3], nz[vert3]);

            short fc1 = c1[i];
            short fc2 = c2[i];
            short fc3 = c3[i];

            if (fc3 == -1)
            {
                fc2 = fc1;
                fc3 = fc1;
            }

            Color color1 = ColourSwapPanel.colourFromShort(fc1);
            Color color2 = ColourSwapPanel.colourFromShort(fc2);
            Color color3 = ColourSwapPanel.colourFromShort(fc3);

            int alpha = 255;

            if (transparencyExists)
            {
                alpha = 255 - (faceAlpha[i] & 0xFF);
            }

            tris.add(new Triangle(
                    new Vertex(v1x, v1y, v1z, 1),
                    new Vertex(v2x, v2y, v2z, 1),
                    new Vertex(v3x, v3y, v3z, 1),
                    color1,
                    color2,
                    color3,
                    n1, n2, n3,
                    alpha
            ));
        }

        return tris;
    }

    private static Vector3 transformNormal(Matrix4 rotation, Vector3 n)
    {
        Vertex v = rotation.transform(new Vertex(n.x, n.y, n.z, 0));
        Vector3 out = new Vector3(v.x, v.y, v.z);
        out.normalize();
        return out;
    }

    private static Color interpolateColor(Color c1, Color c2, Color c3, double b1, double b2, double b3, int alpha)
    {
        int r = (int)(c1.getRed()   * b1 + c2.getRed()   * b2 + c3.getRed()   * b3);
        int g = (int)(c1.getGreen() * b1 + c2.getGreen() * b2 + c3.getGreen() * b3);
        int b = (int)(c1.getBlue()  * b1 + c2.getBlue()  * b2 + c3.getBlue()  * b3);

        return new Color(r, g, b, alpha);
    }

    private static Vector3 interpolateNormal(Vector3 n1, Vector3 n2, Vector3 n3,
                                      double b1, double b2, double b3)
    {
        double x = n1.x * b1 + n2.x * b2 + n3.x * b3;
        double y = n1.y * b1 + n2.y * b2 + n3.y * b3;
        double z = n1.z * b1 + n2.z * b2 + n3.z * b3;
        return new Vector3(x, y, z);
    }

    private static int blend(Color src, int dstARGB)
    {
        int sa = src.getAlpha();
        if (sa == 0)
        {
            return dstARGB;
        }

        double a = sa / 255.0;

        int sr = src.getRed();
        int sg = src.getGreen();
        int sb = src.getBlue();

        int da = (dstARGB >> 24) & 0xFF;
        int dr = (dstARGB >> 16) & 0xFF;
        int dg = (dstARGB >> 8) & 0xFF;
        int db = dstARGB & 0xFF;

        int outA = (int)(sa + da * (1 - a));
        int outR = (int)(sr * a + dr * (1 - a));
        int outG = (int)(sg * a + dg * (1 - a));
        int outB = (int)(sb * a + db * (1 - a));

        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    public static void overrideAlpha(BufferedImage img, byte alpha)
    {
        for (int cx = 0; cx < img.getWidth(); cx++)
        {
            for (int cy = 0; cy < img.getHeight(); cy++)
            {
                int color = img.getRGB(cx, cy);

                int mc = (alpha << 24) | 0x00ffffff;
                int newcomer = color & mc;
                img.setRGB(cx, cy, newcomer);
            }
        }
    }
}
