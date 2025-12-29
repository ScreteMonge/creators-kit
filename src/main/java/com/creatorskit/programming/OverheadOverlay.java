package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.OverheadKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.TextKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.OverheadSprite;
import net.runelite.api.*;
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

public class OverheadOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final SpriteManager spriteManager;

    private final int HEIGHT_BUFFER = 18;
    private final int OVERHEAD_Y_BUFFER = -18;
    private final int SKULL_Y_BUFFER = -25;
    private final int TEXT_BUFFER = -12;
    private final int X_BUFFER = -1;

    @Inject
    private OverheadOverlay(Client client, CreatorsPlugin plugin, SpriteManager spriteManager)
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

            OverheadKeyFrame overheadKeyFrame = (OverheadKeyFrame) character.getCurrentKeyFrame(KeyFrameType.OVERHEAD);
            if (overheadKeyFrame == null)
            {
                continue;
            }

            OverheadSprite prayerSprite = overheadKeyFrame.getPrayerSprite();
            OverheadSprite skullSprite = overheadKeyFrame.getSkullSprite();
            if (prayerSprite == OverheadSprite.NONE && skullSprite == OverheadSprite.NONE)
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

            BufferedImage icon = spriteManager.getSprite(prayerSprite.getSpriteID(), prayerSprite.getFile());
            if (icon == null)
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

            int textBuffer = 0;
            TextKeyFrame textKeyFrame = (TextKeyFrame) character.getCurrentKeyFrame(KeyFrameType.TEXT);
            if (textKeyFrame != null)
            {
                double duration = textKeyFrame.getDuration();
                double startTick = textKeyFrame.getTick();
                double currentTick = plugin.getCurrentTick();
                if (currentTick <= duration + startTick)
                {
                    textBuffer = TEXT_BUFFER;
                }
            }

            Point base = Perspective.getCanvasImageLocation(client, lp, icon, height + HEIGHT_BUFFER);
            if (base == null)
            {
                continue;
            }

            int skullBuffer = 0;
            if (skullSprite != OverheadSprite.NONE)
            {
                Point p = new Point(base.getX() + X_BUFFER, base.getY() + OVERHEAD_Y_BUFFER + textBuffer);
                skullBuffer = SKULL_Y_BUFFER;
                BufferedImage skull = spriteManager.getSprite(skullSprite.getSpriteID(), skullSprite.getFile());
                OverlayUtil.renderImageLocation(graphics, p, skull);
            }

            if (prayerSprite != OverheadSprite.NONE)
            {
                Point p = new Point(base.getX() + X_BUFFER, base.getY() + OVERHEAD_Y_BUFFER + skullBuffer + textBuffer);
                OverlayUtil.renderImageLocation(graphics, p, icon);
            }
        }

        return null;
    }
}
