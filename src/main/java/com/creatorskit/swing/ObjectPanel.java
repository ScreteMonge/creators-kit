package com.creatorskit.swing;

import com.creatorskit.swing.renderer.RenderPosition;
import com.creatorskit.swing.renderer.RenderUtilities;
import com.creatorskit.swing.renderer.Triangle;
import net.runelite.api.Model;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class ObjectPanel extends JPanel
{
    private static final int HEADING = -35;
    private static final int PITCH = -15;
    private static final int FOV_DEFAULT = 150;
    private final int WIDTH_DEFAULT = 227;
    private final int HEIGHT_DEFAULT = 157;

    private BufferedImage image;

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (image != null)
        {
            g.drawImage(image, 0, 0, this);
        }
    }

    public void updateImage(Model model)
    {
        int width = getWidth();
        int height = getHeight();
        if (getWidth() <= 0 || getHeight() <= 0)
        {
            width = WIDTH_DEFAULT;
            height = HEIGHT_DEFAULT;
        }

        if (model == null)
        {
            return;
        }

        int verts = model.getVerticesCount();
        if (verts == 0)
        {
            return;
        }

        float[] xVerts = Arrays.copyOf(model.getVerticesX(), verts);
        float[] yVerts = Arrays.copyOf(model.getVerticesY(), verts);

        Arrays.sort(xVerts);
        Arrays.sort(yVerts);

        float minX = xVerts[0];
        float minY = yVerts[0];
        float maxX = xVerts[xVerts.length - 1];
        float maxY = yVerts[yVerts.length - 1];

        double xAvg = (maxX - minX) / 2;
        double yAvg = (maxY - minY) / 2;
        final int Z_FACTOR = 20;
        double z = Math.max(xAvg, yAvg) * Z_FACTOR;

        double xTranslate = xAvg + minX;
        double yTranslate = yAvg + minY;

        ArrayList<Triangle> tris = RenderUtilities.buildTriangleList(model);
        image = RenderUtilities.render(image, tris, HEADING, PITCH, xTranslate, yTranslate, z, width, height, RenderPosition.CENTER, FOV_DEFAULT);
        RenderUtilities.overrideAlpha(image, (byte) 175);
        repaint();
    }
}
