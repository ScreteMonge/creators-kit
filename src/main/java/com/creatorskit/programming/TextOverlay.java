package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.TextKeyFrame;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;

public class TextOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final int TEXT_BUFFER = 4;

    @Inject
    private TextOverlay(Client client, CreatorsPlugin plugin)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        graphics.setFont(FontManager.getRunescapeBoldFont());

        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            if (!character.isActive())
            {
                continue;
            }

            TextKeyFrame textKeyFrame = (TextKeyFrame) character.getCurrentKeyFrame(KeyFrameType.TEXT);
            if (textKeyFrame == null)
            {
                continue;
            }

            int duration = textKeyFrame.getDuration();
            double startTick = textKeyFrame.getTick();
            double currentTick = plugin.getCurrentTick();
            if (currentTick > duration + startTick)
            {
                continue;
            }

            CKObject ckObject = character.getCkObject();
            LocalPoint lp = ckObject.getLocation();
            if (lp == null || !lp.isInScene())
            {
                continue;
            }

            String text = textKeyFrame.getText();
            if (text.isEmpty())
            {
                continue;
            }

            Model model = ckObject.getModel();
            model.calculateBoundsCylinder();
            int height = model.getModelHeight();

            Point point = Perspective.getCanvasTextLocation(client, graphics, lp, text, height);
            if (point == null)
            {
                continue;
            }

            Point p = new Point(point.getX(), point.getY() + TEXT_BUFFER);
            OverlayUtil.renderTextLocation(graphics, p, text, Color.YELLOW);
        }

        return null;
    }
}