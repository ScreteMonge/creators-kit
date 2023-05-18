package com.creatorssuite.panels;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

public class JSliderUI extends BasicSliderUI {
    public JSliderUI (JSlider slider)
    {
        super(slider);
    }

    @Override
    public void paintTrack(Graphics graphics)
    {
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(Color.CYAN);
        graphics2D.fillRoundRect(2, slider.getHeight() / 2 - 2, slider.getWidth() - 5, 4, 1, 1);
    }

    @Override
    public void scrollDueToClickInTrack(int dir)
    {
        scrollByUnit(dir);
    }
}
