package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class HitsplatOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final SpriteManager spriteManager;

    private final int Y_BUFFER = -1;
    private final int X_BUFFER = -1;
    private final int[][] buffers = { {0, 0}, {0, -20}, {-15, -10}, {15, -10} };

    @Inject
    private HitsplatOverlay(Client client, CreatorsPlugin plugin, SpriteManager spriteManager)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.plugin = plugin;
        this.spriteManager = spriteManager;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        BufferedImage spriteBase = spriteManager.getSprite(HitsplatSprite.BLOCK.getSpriteID(), 0);
        if (spriteBase == null)
        {
            return null;
        }

        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            if (!character.isActive())
            {
                continue;
            }

            CKObject ckObject = character.getCkObject();
            if (!ckObject.isActive())
            {
                continue;
            }

            LocalPoint lp = ckObject.getLocation();
            if (lp == null || !lp.isInScene())
            {
                continue;
            }

            Model model = ckObject.getModel();
            if (model == null)
            {
                continue;
            }

            model.calculateBoundsCylinder();
            int height = model.getModelHeight();

            Point point = Perspective.getCanvasImageLocation(client, lp, spriteBase, height / 2);
            if (point == null)
            {
                continue;
            }

            for (int e = 0; e < KeyFrameType.HITSPLAT_TYPES.length; e++)
            {
                KeyFrameType hitsplat = KeyFrameType.HITSPLAT_TYPES[e];
                KeyFrame kf = character.getCurrentKeyFrame(hitsplat);
                if (kf == null)
                {
                    continue;
                }

                HitsplatKeyFrame keyFrame = (HitsplatKeyFrame) kf;
                double duration = keyFrame.getDuration();
                if (duration == -1)
                {
                    duration = HitsplatKeyFrame.DEFAULT_DURATION;
                }

                double startTick = keyFrame.getTick();
                double currentTick = plugin.getCurrentTick();
                if (currentTick > duration + startTick)
                {
                    continue;
                }

                HitsplatSprite sprite = keyFrame.getSprite();
                int damage = keyFrame.getDamage();

                renderHitsplat(graphics, sprite, height, buffers[e][0], buffers[e][1], point, damage, lp);
            }
        }

        return null;
    }

    private void renderHitsplat(Graphics2D graphics, HitsplatSprite sprite, int height, int xBuffer, int yBuffer, Point base, int damage, LocalPoint lp)
    {
        if (sprite == HitsplatSprite.NONE)
        {
            return;
        }

        BufferedImage spriteImage = spriteManager.getSprite(sprite.getSpriteID(), 0);
        Point p = new Point(base.getX() + xBuffer, base.getY() + yBuffer + Y_BUFFER);
        OverlayUtil.renderImageLocation(graphics, p, spriteImage);

        String text = "" + damage;
        FontMetrics metrics = graphics.getFontMetrics();
        int textHeight = metrics.getHeight() / 2;

        Point textPoint = Perspective.getCanvasTextLocation(client, graphics, lp, text, height / 2);
        Point textP = new Point(textPoint.getX() + xBuffer + X_BUFFER, textPoint.getY() + textHeight + yBuffer + Y_BUFFER);
        OverlayUtil.renderTextLocation(graphics, textP, text, Color.WHITE);
    }
}
