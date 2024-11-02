package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;

@Getter
@Setter
public class AttributeSheet extends TimeSheet
{
    private ToolBoxFrame toolBox;

    public AttributeSheet(ToolBoxFrame toolBox)
    {
        super(toolBox);
        setIndexBuffers(0);
        setSelectedIndex(1);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        if (getSelectedCharacter() == null)
        {
            return;
        }

        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;
        KeyFrame[][] frames = getSelectedCharacter().getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame[] keyFrames = frames[i];
            for (int e = 0; e < keyFrames.length; e++)
            {
                double zoomFactor = this.getWidth() / getZoom();
                g.drawImage(
                        image,
                        (int) ((keyFrames[e].getTick() + getHScroll()) * zoomFactor - xImageOffset),
                        ROW_HEIGHT_OFFSET + ROW_HEIGHT + ROW_HEIGHT * i - yImageOffset,
                        null);
            }
        }
    }
}
