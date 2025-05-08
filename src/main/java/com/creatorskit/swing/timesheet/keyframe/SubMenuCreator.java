package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.swing.timesheet.TimeSheetPanel;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.client.util.ColorUtil;

import java.awt.*;

public class SubMenuCreator
{
    public static void createSubMenus(TimeSheetPanel timeSheetPanel, Menu menu)
    {
        double currentTime = timeSheetPanel.getCurrentTime();

        KeyFrameType[] types = new KeyFrameType[]
                {
                        KeyFrameType.MOVEMENT,
                        KeyFrameType.ANIMATION,
                        KeyFrameType.ORIENTATION,
                        KeyFrameType.SPAWN,
                        KeyFrameType.MODEL,
                        KeyFrameType.TEXT,
                        KeyFrameType.OVERHEAD,
                        KeyFrameType.HEALTH,
                        KeyFrameType.SPOTANIM,
                        KeyFrameType.SPOTANIM2
                };

        for (KeyFrameType type : types)
        {
            menu.createMenuEntry(0)
                    .setOption(ColorUtil.prependColorTag("Add", Color.ORANGE))
                    .setTarget(ColorUtil.colorTag(Color.WHITE) + type)
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> timeSheetPanel.onKeyFrameIconPressedEvent(currentTime, type));
        }
    }
}
