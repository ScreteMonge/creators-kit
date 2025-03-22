package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.TextKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class HealthOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final SpriteManager spriteManager;

    private final int MODEL_HEIGHT_BUFFER = 18;
    private final int Y_BUFFER = -1;
    private final int TEXT_BUFFER = -12;
    private final int X_BUFFER = -1;

    @Inject
    private HealthOverlay(Client client, CreatorsPlugin plugin, SpriteManager spriteManager)
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

        graphics.setFont(FontManager.getRunescapeSmallFont());

        ArrayList<com.creatorskit.Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            if (!character.isActive())
            {
                continue;
            }

            HealthKeyFrame healthKeyFrame = (HealthKeyFrame) character.getCurrentKeyFrame(KeyFrameType.HEALTH);
            if (healthKeyFrame == null)
            {
                continue;
            }

            if (!healthKeyFrame.isEnabled())
            {
                continue;
            }

            CKObject ckObject = character.getCkObject();
            LocalPoint lp = ckObject.getLocation();
            if (lp == null || !lp.isInScene())
            {
                continue;
            }

            BufferedImage redBar = spriteManager.getSprite(healthKeyFrame.getHealthbarSprite().getBackgroundSpriteID(), 0);
            if (redBar == null)
            {
                continue;
            }

            BufferedImage greenBar = spriteManager.getSprite(healthKeyFrame.getHealthbarSprite().getForegroundSpriteID(), 0);
            if (greenBar == null)
            {
                continue;
            }

            double ratio = (double) healthKeyFrame.getCurrentHealth() / (double) healthKeyFrame.getMaxHealth();
            int barWidth = (int) (ratio * redBar.getWidth());
            if (barWidth == 0 && ratio > 0)
            {
                barWidth = 1;
            }

            if (barWidth > greenBar.getWidth())
            {
                barWidth = greenBar.getWidth();
            }

            Model model = ckObject.getModel();
            model.calculateBoundsCylinder();
            int height = model.getModelHeight();

            int textBuffer = 0;
            TextKeyFrame textKeyFrame = (TextKeyFrame) character.getCurrentKeyFrame(KeyFrameType.TEXT);
            if (textKeyFrame != null)
            {
                int duration = textKeyFrame.getDuration();
                double startTick = textKeyFrame.getTick();
                double currentTick = plugin.getCurrentTick();
                if (currentTick <= duration + startTick)
                {
                    textBuffer = TEXT_BUFFER;
                }
            }

            Point base = Perspective.getCanvasImageLocation(client, lp, redBar, height + MODEL_HEIGHT_BUFFER);
            Point p = new Point(base.getX() + X_BUFFER, base.getY() + Y_BUFFER + textBuffer);

            OverlayUtil.renderImageLocation(graphics, p, redBar);

            if (barWidth > 0)
            {
                BufferedImage subImage = greenBar.getSubimage(0, 0, barWidth, redBar.getHeight());
                OverlayUtil.renderImageLocation(graphics, p, subImage);
            }

            BufferedImage spriteBase = spriteManager.getSprite(HitsplatSprite.BLOCK.getSpriteID(), 0);
            if (spriteBase == null)
            {
                continue;
            }

            Point hitsplatPoint = Perspective.getCanvasImageLocation(client, lp, spriteBase, height / 2);
            renderHitsplat(graphics, healthKeyFrame.getHitsplat1Sprite(), height, 0, 0, hitsplatPoint, healthKeyFrame.getHitsplat1(), lp);
            renderHitsplat(graphics, healthKeyFrame.getHitsplat2Sprite(), height, 0, -20, hitsplatPoint, healthKeyFrame.getHitsplat2(), lp);
            renderHitsplat(graphics, healthKeyFrame.getHitsplat3Sprite(), height, -15, -10, hitsplatPoint, healthKeyFrame.getHitsplat3(), lp);
            renderHitsplat(graphics, healthKeyFrame.getHitsplat4Sprite(), height, 15, -10, hitsplatPoint, healthKeyFrame.getHitsplat4(), lp);
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
