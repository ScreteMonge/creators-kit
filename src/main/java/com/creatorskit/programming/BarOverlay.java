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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the HP / Shield / Special overhead-bar stack. Consolidating all three
 * bars in a single overlay (as opposed to one overlay per bar) lets us:
 *
 * <ul>
 *   <li>Honour each bar's user-set {@code order} field to decide stacking
 *       independently per-keyframe.</li>
 *   <li>Match every bar's width to the same auto-scaled value derived from
 *       whichever bar has the largest {@code max}, so the stack stays
 *       rectangular even when one bar dwarfs the others.</li>
 *   <li>Apply text-bubble collision offset to the whole stack at once.</li>
 * </ul>
 *
 * <p>HP keeps its existing sprite (red/green from {@link
 * com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite}). Shield
 * and Special draw a custom-colour rectangle with a darker-shade depleted
 * portion -- mirroring the in-game Doom-of-Mokhaiotl / OSRS extra-bar style
 * (no black border, darker hue for the empty side).
 */
public class BarOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final SpriteManager spriteManager;

    /** Above-model offset before stacking starts. Matches the legacy HP placement. */
    private static final int MODEL_HEIGHT_BUFFER = 18;
    private static final int TEXT_BUFFER = -12;

    /** Minimum bar width. Each bar has its own min independent of the others. */
    private static final int MIN_BAR_WIDTH = 30;
    /** Hard cap so absurd max values don't fill the canvas. */
    private static final int MAX_BAR_WIDTH = 204;
    /**
     * Pixels per unit-of-max used to auto-scale bar width. Tuned so a 99-max
     * HP stays at the minimum width and a 1000-max boss bar lands around 170px.
     */
    private static final double PIXELS_PER_MAX = 0.17;

    private static final int BAR_HEIGHT = 5;
    private static final int BAR_GAP = 1;
    /** Brightness multiplier applied to the fill colour for the depleted portion. */
    private static final float DEPLETED_BRIGHTNESS = 0.30f;

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

            List<BarSpec> bars = collectActiveBars(character, currentTick);
            if (bars.isEmpty())
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

            int textOffset = textCollisionOffset(character, currentTick);

            // Sort by order ASC -- lowest order = topmost slot.
            bars.sort((a, b) -> Integer.compare(a.order, b.order));

            // Each bar centres horizontally on the model with its OWN width, so a
            // 99-HP bar can stay small even when a 5000-shield is alongside it.
            // Y-stacking is shared so bars still slot cleanly above each other.
            int stack = bars.size();
            for (int slot = 0; slot < stack; slot++)
            {
                BarSpec bar = bars.get(slot);
                int barWidth = resolveWidth(bar);
                BufferedImage anchor = new BufferedImage(barWidth, BAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Point base = Perspective.getCanvasImageLocation(client, lp, anchor, height + MODEL_HEIGHT_BUFFER);
                if (base == null)
                {
                    continue;
                }
                int slotFromBottom = stack - 1 - slot;
                int x = base.getX() - 1;
                int y = base.getY() - 1 + textOffset - slotFromBottom * (BAR_HEIGHT + BAR_GAP);
                renderBar(graphics, bar, x, y, barWidth);
            }
        }

        return null;
    }

    /**
     * Resolves a bar's pixel width: explicit override if set, else auto-scale
     * from the bar's own max. Per-bar (not per-Character) so HP/Shield/Special
     * are sized independently of each other.
     */
    private int resolveWidth(BarSpec bar)
    {
        if (bar.explicitWidth > 0)
        {
            return Math.max(MIN_BAR_WIDTH, Math.min(MAX_BAR_WIDTH, bar.explicitWidth));
        }
        int scaled = (int) Math.round(Math.max(0, bar.max) * PIXELS_PER_MAX) + MIN_BAR_WIDTH;
        return Math.max(MIN_BAR_WIDTH, Math.min(MAX_BAR_WIDTH, scaled));
    }

    /**
     * Picks up the active HP / Shield / Special keyframes for one Character and
     * returns them as a uniform list. "Active" = the seeker is inside the
     * keyframe's duration window. Returns an empty list if none are active so
     * the caller can short-circuit.
     */
    private List<BarSpec> collectActiveBars(Character character, double currentTick)
    {
        List<BarSpec> out = new ArrayList<>(3);

        HealthKeyFrame hp = (HealthKeyFrame) character.getCurrentKeyFrame(KeyFrameType.HEALTH);
        if (hp != null && currentTick >= hp.getTick() && currentTick <= hp.getTick() + hp.getDuration())
        {
            // BOSS_HEALTH is rendered by BossHealthOverlay as a pinned top-of-
            // screen bar -- skip the per-character overhead bar so we don't
            // double-render the same HP value in two places.
            if (hp.getHealthbarSprite() != com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite.BOSS_HEALTH)
            {
                out.add(BarSpec.health(hp));
            }
        }

        ShieldKeyFrame shield = (ShieldKeyFrame) character.getCurrentKeyFrame(KeyFrameType.SHIELD);
        if (shield != null && currentTick >= shield.getTick() && currentTick <= shield.getTick() + shield.getDuration())
        {
            out.add(BarSpec.shield(shield));
        }

        SpecialKeyFrame special = (SpecialKeyFrame) character.getCurrentKeyFrame(KeyFrameType.SPECIAL);
        if (special != null && currentTick >= special.getTick() && currentTick <= special.getTick() + special.getDuration())
        {
            out.add(BarSpec.special(special));
        }

        return out;
    }

    private void renderBar(Graphics2D g, BarSpec bar, int x, int y, int width)
    {
        if (bar.spriteHealth != null)
        {
            renderSpriteHpBar(g, bar.spriteHealth, x, y, width);
            return;
        }
        renderColouredBar(g, bar.rgb, bar.current, bar.max, x, y, width);
    }

    /**
     * HP keeps its sprite-based look so saves built before the Shield/Special
     * feature still render the same. Width is auto-scaled, so we slice the
     * sprite to fit instead of forcing the sprite's native width.
     */
    private void renderSpriteHpBar(Graphics2D g, HealthKeyFrame hp, int x, int y, int width)
    {
        BufferedImage red = spriteManager.getSprite(hp.getHealthbarSprite().getBackgroundSpriteID(), 0);
        BufferedImage green = spriteManager.getSprite(hp.getHealthbarSprite().getForegroundSpriteID(), 0);
        if (red == null || green == null)
        {
            // Sprite manager hasn't loaded yet -- fall back to the coloured-bar
            // path with OSRS red/green hues so the bar is still visible.
            renderColouredBar(g, 0x36FF36, hp.getCurrentHealth(), hp.getMaxHealth(), x, y, width);
            return;
        }

        // Draw the background (depleted/red) stretched to the auto-scaled width.
        g.drawImage(red, x, y, width, red.getHeight(), null);

        if (hp.getMaxHealth() <= 0 || hp.getCurrentHealth() <= 0)
        {
            return;
        }
        double ratio = Math.min(1.0, (double) hp.getCurrentHealth() / (double) hp.getMaxHealth());
        int fillWidth = (int) Math.round(ratio * width);
        if (fillWidth <= 0 && ratio > 0)
        {
            fillWidth = 1;
        }
        if (fillWidth > 0)
        {
            // Source-side slice keeps the texture grain consistent with the
            // background even after the auto-scaled stretch.
            int srcW = (int) Math.round(ratio * green.getWidth());
            if (srcW < 1) srcW = 1;
            BufferedImage sub = green.getSubimage(0, 0, srcW, green.getHeight());
            g.drawImage(sub, x, y, fillWidth, green.getHeight(), null);
        }
    }

    /** Shield/Special style: solid fill, darker-shade background, no border. */
    private void renderColouredBar(Graphics2D g, int rgb, int current, int max, int x, int y, int width)
    {
        Color fill = new Color(rgb);
        Color depleted = darken(fill, DEPLETED_BRIGHTNESS);

        g.setColor(depleted);
        g.fillRect(x, y, width, BAR_HEIGHT);

        if (max <= 0 || current <= 0)
        {
            return;
        }
        double ratio = Math.min(1.0, (double) current / (double) max);
        int fillWidth = (int) Math.round(ratio * width);
        if (fillWidth <= 0 && ratio > 0)
        {
            fillWidth = 1;
        }
        g.setColor(fill);
        g.fillRect(x, y, fillWidth, BAR_HEIGHT);
    }

    /** Returns a darker shade of the colour via HSB brightness scaling. */
    private static Color darken(Color c, float brightnessMultiplier)
    {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2] * brightnessMultiplier);
    }

    /** Mirror of the legacy HealthOverlay's text-collision offset. */
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

    /**
     * Uniform bar payload regardless of whether the source is HP (sprite render)
     * or Shield/Special (colour render). {@code order} drives stack position;
     * {@code max} feeds the per-bar auto-scaled width unless {@code explicitWidth}
     * is set, in which case the auto-scale is bypassed.
     */
    private static final class BarSpec
    {
        final int order;
        final int max;
        final int current;
        final int rgb;
        final int explicitWidth;
        /** Non-null only for HP, which uses the sprite renderer. */
        final HealthKeyFrame spriteHealth;

        private BarSpec(int order, int max, int current, int rgb, int explicitWidth, HealthKeyFrame spriteHealth)
        {
            this.order = order;
            this.max = max;
            this.current = current;
            this.rgb = rgb;
            this.explicitWidth = explicitWidth;
            this.spriteHealth = spriteHealth;
        }

        static BarSpec health(HealthKeyFrame kf)
        {
            return new BarSpec(kf.getOrder(), kf.getMaxHealth(), kf.getCurrentHealth(), 0, kf.getWidth(), kf);
        }

        static BarSpec shield(ShieldKeyFrame kf)
        {
            return new BarSpec(kf.getOrder(), kf.getMaxValue(), kf.getCurrentValue(), kf.getRgb(), kf.getWidth(), null);
        }

        static BarSpec special(SpecialKeyFrame kf)
        {
            return new BarSpec(kf.getOrder(), kf.getMaxValue(), kf.getCurrentValue(), kf.getRgb(), kf.getWidth(), null);
        }
    }
}
