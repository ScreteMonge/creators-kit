package com.creatorskit.swing.renderer;

import com.creatorskit.models.CustomLighting;
import com.creatorskit.models.LightingStyle;
import net.runelite.api.*;
import net.runelite.api.events.PostClientTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class RenderPanel extends JPanel
{
    private Client client;

    private Model model;
    private AnimationController ac;
    private boolean modelExists = false;
    private final JSlider fovSlider;
    private boolean enableAnimations = true;
    private BufferedImage img;

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

    public RenderPanel(Client client, ClientThread clientThread, JSlider fovSlider)
    {
        this.fovSlider = fovSlider;
        this.client = client;

        clientThread.invokeLater(() ->
        {
            this.ac = new AnimationController(client, -1);
            ac.setOnFinished(e ->
            {
                ac.reset();
            });
        });

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
            z = Math.max(z + ZOOM_FACTOR * e.getWheelRotation(), MIN_CAMERA_DISTANCE);
            repaint();
        });

        fovSlider.addChangeListener(e -> repaint());
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event)
    {
        if (!isShowing())
        {
            return;
        }

        if (!modelExists || ac.getAnimation() == null || !enableAnimations)
        {
            return;
        }

        int frame = ac.getFrame();
        ac.tick(1);
        if (frame == ac.getFrame())
        {
            return;
        }

        Model animated = ac.animate(model);
        updateModelParameters(animated);
        repaint();
    }

    public void updateAnimation(Animation animation)
    {
        ac.setAnimation(animation);
        repaint();
    }

    public void updateModel(ModelData md, LightingStyle ls)
    {
        updateModel(md, new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ()));
    }

    public void updateModel(ModelData md, CustomLighting ls)
    {
        model = md.light(ls.getAmbient(), ls.getContrast(), ls.getX(), -ls.getZ(), ls.getY());
        modelExists = true;
        updateModelParameters(model);
        repaint();
    }

    public void toggleAnimations(boolean enable)
    {
        enableAnimations = enable;
        ac.reset();
        updateModelParameters(model);
        repaint();
    }

    public void resetViewer()
    {
        modelExists = false;
        repaint();
        revalidate();
    }

    private ArrayList<Triangle> tris;

    private void updateModelParameters(Model model)
    {
        tris = RenderUtilities.buildTriangleList(model);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (!modelExists)
        {
            g.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 16));
            g.setColor(ColorScheme.BRAND_ORANGE);
            FontMetrics fm = g.getFontMetrics();
            String s = "No Model";

            g.drawString(s, this.getWidth() / 2 - fm.stringWidth(s) / 2, this.getHeight() / 2 + fm.getHeight() / 2);
            return;
        }

        img = RenderUtilities.render(img, tris, heading, pitch, x, y, z, getWidth(), getHeight(), RenderPosition.CENTER, fovSlider.getValue());
        g2.drawImage(img, 0, 0, null);
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