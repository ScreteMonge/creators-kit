package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.timesheet.keyframe.HealthKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;

/**
 * Pinned boss-style HP bar drawn at the top centre of the canvas. Active when
 * any Character holds a {@link HealthKeyFrame} whose
 * {@link HealthbarSprite#BOSS_HEALTH} style is selected and whose tick window
 * contains the current tick. When several overlap (multiple "scene controller"
 * Characters), the most-recently-started one wins -- matches the
 * {@link ScreenFadeOverlay}/screen-shake pattern.
 *
 * <p>The damage indicator is keyframe-driven: when the active boss keyframe's
 * {@code currentHealth} is lower than the previous BOSS_HEALTH keyframe's on
 * the same Character, a red bar appears spanning the lost-HP range and fades
 * out over {@link #DAMAGE_FADE_TICKS} ticks from the active keyframe's start.
 * Tick-based fade (not wall-clock) keeps scrubbing deterministic: any visit
 * to the same tick paints the same alpha.
 */
public class BossHealthOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;

    private static final int BAR_WIDTH = 380;
    private static final int BAR_HEIGHT = 20;
    private static final int TOP_MARGIN = 12;
    /** Damage indicator fades to zero over this many game ticks (~600ms each). */
    private static final double DAMAGE_FADE_TICKS = 4.0;

    @Inject
    private BossHealthOverlay(Client client, CreatorsPlugin plugin)
    {
        // UNDER_WIDGETS so it draws above the 3D scene but below chat / minimap.
        // Same layer choice as ScreenFadeOverlay -- consistent "scene-level effect
        // but doesn't cover HUD" placement.
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
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

        double currentTick = plugin.getCurrentTick();

        Character bossChar = null;
        HealthKeyFrame bossKf = null;
        double bestStart = Double.NEGATIVE_INFINITY;

        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character c = characters.get(i);
            HealthKeyFrame kf = (HealthKeyFrame) c.getCurrentKeyFrame(KeyFrameType.HEALTH);
            if (kf == null || kf.getHealthbarSprite() != HealthbarSprite.BOSS_HEALTH)
            {
                continue;
            }
            double elapsed = currentTick - kf.getTick();
            if (elapsed < 0 || elapsed > kf.getDuration())
            {
                continue;
            }
            if (kf.getTick() > bestStart)
            {
                bestStart = kf.getTick();
                bossKf = kf;
                bossChar = c;
            }
        }

        if (bossKf == null || bossChar == null)
        {
            return null;
        }

        int currentHp = Math.max(0, bossKf.getCurrentHealth());
        int maxHp = Math.max(1, bossKf.getMaxHealth()); // Avoid div-by-zero in ratio.
        if (currentHp > maxHp)
        {
            currentHp = maxHp;
        }

        int canvasW = client.getCanvasWidth();
        int canvasH = client.getCanvasHeight();
        if (canvasW <= 0 || canvasH <= 0)
        {
            return null;
        }
        int x = (canvasW - BAR_WIDTH) / 2;
        int y = TOP_MARGIN;

        // Background plate with a 2px black border so the bar reads against
        // bright sky / overworld backgrounds.
        graphics.setColor(Color.BLACK);
        graphics.fillRect(x - 2, y - 2, BAR_WIDTH + 4, BAR_HEIGHT + 4);
        graphics.setColor(new Color(36, 36, 36));
        graphics.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);

        double ratio = (double) currentHp / (double) maxHp;
        int greenW = (int) Math.round(ratio * BAR_WIDTH);
        if (greenW > 0)
        {
            graphics.setColor(new Color(70, 190, 70));
            graphics.fillRect(x, y, greenW, BAR_HEIGHT);
        }

        // Damage indicator: find the previous BOSS_HEALTH keyframe on the same
        // Character and, if HP dropped going into the active keyframe, paint
        // a red bar from currentHP to previousHP, alpha-fading over the first
        // DAMAGE_FADE_TICKS ticks after the active keyframe started.
        HealthKeyFrame previous = findPreviousBossKeyFrame(bossChar, bossKf.getTick());
        if (previous != null && previous.getCurrentHealth() > currentHp)
        {
            int prevHp = Math.min(previous.getCurrentHealth(), maxHp);
            double sinceStart = currentTick - bossKf.getTick();
            double t = sinceStart / DAMAGE_FADE_TICKS;
            if (t >= 0 && t < 1.0)
            {
                float alpha = (float) Math.max(0.0, 1.0 - t);
                int redStart = greenW;
                int redEnd = (int) Math.round((double) prevHp / maxHp * BAR_WIDTH);
                int redW = Math.max(0, redEnd - redStart);
                if (redW > 0)
                {
                    Composite oc = graphics.getComposite();
                    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    graphics.setColor(new Color(200, 50, 50));
                    graphics.fillRect(x + redStart, y, redW, BAR_HEIGHT);
                    graphics.setComposite(oc);
                }
            }
        }

        // Centered "current / max" text. Uses the bold runescape font for
        // readability against the green fill.
        graphics.setFont(FontManager.getRunescapeBoldFont());
        FontMetrics fm = graphics.getFontMetrics();
        String text = currentHp + " / " + maxHp;
        int textW = fm.stringWidth(text);
        int textX = x + (BAR_WIDTH - textW) / 2;
        int textY = y + (BAR_HEIGHT + fm.getAscent()) / 2 - 2;
        // Shadow first, then white text -- legibility hack matching the engine's
        // own HP bar text style.
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, textX + 1, textY + 1);
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, textX, textY);

        return null;
    }

    /**
     * Walks {@code character}'s HEALTH keyframe array and returns the latest
     * BOSS_HEALTH keyframe whose tick is strictly less than {@code referenceTick}.
     * Used to source the "previous HP" for the damage indicator.
     */
    private HealthKeyFrame findPreviousBossKeyFrame(Character character, double referenceTick)
    {
        KeyFrame[] frames = character.getKeyFrames(KeyFrameType.HEALTH);
        if (frames == null)
        {
            return null;
        }
        HealthKeyFrame best = null;
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame kf = frames[i];
            if (!(kf instanceof HealthKeyFrame))
            {
                continue;
            }
            HealthKeyFrame hkf = (HealthKeyFrame) kf;
            if (hkf.getHealthbarSprite() != HealthbarSprite.BOSS_HEALTH)
            {
                continue;
            }
            if (hkf.getTick() >= referenceTick)
            {
                continue;
            }
            if (best == null || hkf.getTick() > best.getTick())
            {
                best = hkf;
            }
        }
        return best;
    }
}
