package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatVariant;
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
                HitsplatVariant variant = keyFrame.getVariant();
                sprite = getVariant(sprite, variant);

                int damage = keyFrame.getDamage();

                renderHitsplat(graphics, sprite, height, buffers[e][0], buffers[e][1], damage, lp);
            }
        }

        return null;
    }

    private void renderHitsplat(Graphics2D graphics, HitsplatSprite sprite, int height, int xBuffer, int yBuffer, int damage, LocalPoint lp)
    {
        if (sprite == HitsplatSprite.NONE)
        {
            return;
        }

        BufferedImage spriteImage = spriteManager.getSprite(sprite.getSpriteID(), 0);
        if (spriteImage == null)
        {
            return;
        }

        int width = spriteImage.getWidth();

        Point base = Perspective.getCanvasImageLocation(client, lp, spriteImage, height / 2);
        if (base == null)
        {
            return;
        }

        Point p = new Point(base.getX() + xBuffer, base.getY() + yBuffer + Y_BUFFER);
        OverlayUtil.renderImageLocation(graphics, p, spriteImage);

        if (damage == -1)
        {
            return;
        }

        String text = "" + damage;
        FontMetrics metrics = graphics.getFontMetrics();
        int textHeight = metrics.getHeight() / 2;

        Point textPoint = Perspective.getCanvasTextLocation(client, graphics, lp, text, height / 2);

        int textXBuffer = 0;
        if (width % 2 == 0)
        {
            textXBuffer = X_BUFFER;
        }

        Point textP = new Point(textPoint.getX() + xBuffer + textXBuffer, textPoint.getY() + textHeight + yBuffer + Y_BUFFER);
        OverlayUtil.renderTextLocation(graphics, textP, text, Color.WHITE);
    }

    private HitsplatSprite getVariant(HitsplatSprite sprite, HitsplatVariant variant)
    {
        if (variant == null || variant == HitsplatVariant.NORMAL)
        {
            return sprite;
        }

        switch (sprite)
        {
            case BLOCK:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.BLOCK;
                    case OTHER:
                        return HitsplatSprite.BLOCK_OTHER;
                }
            case DAMAGE:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.DAMAGE_MAX;
                    case OTHER:
                        return HitsplatSprite.DAMAGE_OTHER;
                }
            case POISON:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.POISON_MAX;
                    case OTHER:
                        return HitsplatSprite.POISON_OTHER;
                }
            case VENOM:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.VENOM;
                    case OTHER:
                        return HitsplatSprite.VENOM_OTHER;
                }
            case SHIELD:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.SHIELD_MAX;
                    case OTHER:
                        return HitsplatSprite.SHIELD_OTHER;
                }
            case FREEZE:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.FREEZE;
                    case OTHER:
                        return HitsplatSprite.FREEZE_OTHER;
                }
            case ARMOUR:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.ARMOUR_MAX;
                    case OTHER:
                        return HitsplatSprite.ARMOUR;
                }
            case POISE:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.POISE_MAX;
                    case OTHER:
                        return HitsplatSprite.POISE_OTHER;
                }
            case PRAYER_DRAIN:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.PRAYER_DRAIN_MAX;
                    case OTHER:
                        return HitsplatSprite.PRAYER_DRAIN_OTHER;
                }
            case CHARGE_UP:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.CHARGE_UP;
                    case OTHER:
                        return HitsplatSprite.CHARGE_UP_OTHER;
                }
            case CHARGE_DOWN:
                switch (variant)
                {
                    case MAX:
                        return HitsplatSprite.CHARGE_DOWN;
                    case OTHER:
                        return HitsplatSprite.CHARGE_DOWN_OTHER;
                }
        }

        return sprite;
    }
}
