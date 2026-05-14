package com.creatorskit.programming;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.ShieldKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.SpecialKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.TextKeyFrame;
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

import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Renders Doom-of-Mokhaiotl-style Shield / Special bars stacked above the HP
 * bar (or in HP's slot if no HP keyframe is active). The HP bar itself is
 * still rendered by {@link HealthOverlay}; this overlay only owns the two
 * extra bars so the existing HP rendering stays untouched.
 *
 * <p>Drawn with Graphics2D rather than the sprite system because the user
 * picks the fill colour per-keyframe -- the sprite path would force one
 * colour per sprite.
 */
public class BarOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final SpriteManager spriteManager;

    /** Above-model offset before stacking starts. Matches HealthOverlay. */
    private static final int MODEL_HEIGHT_BUFFER = 18;
    private static final int TEXT_BUFFER = -12;

    /** Bar geometry. Tuned to match the standard HP-bar sprite (30x5). */
    private static final int BAR_WIDTH = 30;
    private static final int BAR_HEIGHT = 5;
    /** Vertical gap between stacked bars. */
    private static final int BAR_GAP = 1;
    private static final Color BAR_BACKGROUND = new Color(0, 0, 0, 153);  // 60% alpha black
    private static final Color BAR_BORDER = new Color(0, 0, 0, 220);

    @Inject
    private BarOverlay(Client client, CreatorsPlugin plugin, SpriteManager spriteManager)
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

        double currentTick = plugin.getCurrentTick();
        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            if (!character.isActive())
            {
                continue;
            }

            ShieldKeyFrame shieldKeyFrame = activeShield(character, currentTick);
            SpecialKeyFrame specialKeyFrame = activeSpecial(character, currentTick);
            if (shieldKeyFrame == null && specialKeyFrame == null)
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

            // Text shifts the entire stack up so the chat bubble doesn't overlap the bars.
            int textBuffer = textCollisionOffset(character, currentTick);

            // Use a dummy bar sprite to anchor the position with the same logic
            // HealthOverlay uses. The dummy is the size we want each bar to be.
            BufferedImage anchor = new BufferedImage(BAR_WIDTH, BAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Point base = Perspective.getCanvasImageLocation(client, lp, anchor, height + MODEL_HEIGHT_BUFFER);
            if (base == null)
            {
                continue;
            }

            // Determine stacking. HP (drawn elsewhere) is at the bottom slot.
            // If HP is active, our bars stack ABOVE it. Otherwise our bars
            // claim HP's slot so the user can show just a shield/special bar.
            boolean hpActive = activeHealth(character, currentTick) != null;
            int hpReservedRows = hpActive ? 1 : 0;

            int x = base.getX() - 1;
            int yHp = base.getY() - 1 + textBuffer;

            // Special is the top bar; Shield is between HP and Special.
            // Row 0 = HP slot, Row 1 = Shield, Row 2 = Special. When HP is
            // inactive we collapse so Shield takes row 0 and Special row 1.
            if (shieldKeyFrame != null)
            {
                int row = hpReservedRows;
                int y = yHp - row * (BAR_HEIGHT + BAR_GAP);
                drawBar(graphics, x, y, shieldKeyFrame.getRgb(),
                        shieldKeyFrame.getCurrentValue(),
                        shieldKeyFrame.getMaxValue());
            }

            if (specialKeyFrame != null)
            {
                // Special sits above shield (or above HP if no shield).
                int row = hpReservedRows + (shieldKeyFrame != null ? 1 : 0);
                int y = yHp - row * (BAR_HEIGHT + BAR_GAP);
                drawBar(graphics, x, y, specialKeyFrame.getRgb(),
                        specialKeyFrame.getCurrentValue(),
                        specialKeyFrame.getMaxValue());
            }
        }

        return null;
    }

    private void drawBar(Graphics2D g, int x, int y, int fillRgb, int current, int max)
    {
        // Translucent background so the bar reads as an overhead UI element.
        Composite originalComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);

        g.setColor(BAR_BACKGROUND);
        g.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);

        if (max > 0 && current > 0)
        {
            double ratio = Math.min(1.0, (double) current / (double) max);
            int fillWidth = (int) Math.round(ratio * BAR_WIDTH);
            if (fillWidth == 0 && ratio > 0)
            {
                fillWidth = 1;
            }
            g.setColor(new Color(fillRgb));
            g.fillRect(x, y, fillWidth, BAR_HEIGHT);
        }

        g.setColor(BAR_BORDER);
        g.drawRect(x, y, BAR_WIDTH - 1, BAR_HEIGHT - 1);
        g.setComposite(originalComposite);
    }

    /** HP / Shield / Special all share the same "active while within duration" rule. */
    private HealthKeyFrame activeHealth(Character character, double currentTick)
    {
        HealthKeyFrame kf = (HealthKeyFrame) character.getCurrentKeyFrame(KeyFrameType.HEALTH);
        if (kf == null)
        {
            return null;
        }
        if (currentTick > kf.getTick() + kf.getDuration())
        {
            return null;
        }
        return kf;
    }

    private ShieldKeyFrame activeShield(Character character, double currentTick)
    {
        ShieldKeyFrame kf = (ShieldKeyFrame) character.getCurrentKeyFrame(KeyFrameType.SHIELD);
        if (kf == null)
        {
            return null;
        }
        if (currentTick > kf.getTick() + kf.getDuration())
        {
            return null;
        }
        return kf;
    }

    private SpecialKeyFrame activeSpecial(Character character, double currentTick)
    {
        SpecialKeyFrame kf = (SpecialKeyFrame) character.getCurrentKeyFrame(KeyFrameType.SPECIAL);
        if (kf == null)
        {
            return null;
        }
        if (currentTick > kf.getTick() + kf.getDuration())
        {
            return null;
        }
        return kf;
    }

    /** Mirror of HealthOverlay's text-collision offset so all bars stay aligned. */
    private int textCollisionOffset(Character character, double currentTick)
    {
        TextKeyFrame textKeyFrame = (TextKeyFrame) character.getCurrentKeyFrame(KeyFrameType.TEXT);
        if (textKeyFrame == null)
        {
            return 0;
        }
        if (currentTick > textKeyFrame.getTick() + textKeyFrame.getDuration())
        {
            return 0;
        }
        return TEXT_BUFFER;
    }
}
