package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.programming.orientation.OrientationHotkeyMode;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.client.util.ColorUtil;

import java.awt.*;

public class SubMenuCreator
{
    private static final KeyFrameType[] types = new KeyFrameType[]
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

    public static void createSubMenus(TimeSheetPanel timeSheetPanel, Menu menu)
    {
        double currentTime = timeSheetPanel.getCurrentTime();

        for (KeyFrameType type : types)
        {
            if (type == KeyFrameType.MOVEMENT)
            {
                menu.createMenuEntry(0)
                        .setOption(ColorUtil.prependColorTag("Add", Color.ORANGE))
                        .setTarget(ColorUtil.colorTag(Color.WHITE) + KeyFrameType.MOVEMENT)
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> timeSheetPanel.onAddMovementMenuOptionPressed());
                continue;
            }

            if (type == KeyFrameType.ORIENTATION)
            {
                menu.createMenuEntry(0)
                        .setOption(ColorUtil.prependColorTag("Add", Color.ORANGE))
                        .setTarget(ColorUtil.colorTag(Color.WHITE) + KeyFrameType.ORIENTATION)
                        .setType(MenuAction.RUNELITE)
                        .onClick(e -> timeSheetPanel.onAddOrientationMenuOptionPressed());
                continue;
            }

            menu.createMenuEntry(0)
                    .setOption(ColorUtil.prependColorTag("Add", Color.ORANGE))
                    .setTarget(ColorUtil.colorTag(Color.WHITE) + type)
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> timeSheetPanel.onKeyFrameIconPressedEvent(currentTime, type));
        }
    }
}
