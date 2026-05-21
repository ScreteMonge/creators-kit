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
import java.awt.Color;
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
 * <p>Styling matches the OSRS boss HP HUD widget (interface 303 /
 * {@code InterfaceID.HpbarHud}): boss name text on top, dark grey plate, and
 * a green/red HP bar below. Colours come from RuneLite's own OpponentInfoOverlay
 * so the keyframe-driven bar reads identically to engine-rendered ones.
 *
 * <p>Damage feedback is keyframe-driven and lookahead-style: if a future
 * BOSS_HEALTH keyframe on the same Character will lower HP, the chunk that's
 * about to disappear is overlaid as dark green and visually "deflates" toward
 * the future value as we approach that keyframe's tick. Pure linear interp
 * keeps scrubbing deterministic -- any visit to the same tick paints the
 * same pixels.
 */
public class BossHealthOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;

    /** Bar dimensions match the OSRS HpbarHud widget (~360px wide). */
    private static final int BAR_WIDTH = 360;
    private static final int BAR_HEIGHT = 18;
    private static final int NAME_HEIGHT = 16;
    /**
     * Vertical offset from the top of the canvas. Set high enough to clear
     * the area where XP orbs would normally sit in OSRS (top-right corner,
     * roughly y=0-70). Creator's Kit doesn't render XP orbs but the user
     * wants the bar positioned consistently with the normal OSRS layout --
     * top-centre, below the orbs band -- so cinematic captures look at home.
     */
    private static final int TOP_MARGIN = 80;
    /**
     * Padding around the bar contents inside the brown frame. The user asked
     * for "a little padding" to match the reference screenshot's chunky look;
     * the frame now sits 6px outside the name strip + HP bar on every side
     * (was 2-3px).
     */
    private static final int PLATE_PAD = 6;
    /** Brown frame around the HUD -- user-specified #3a352b. */
    private static final Color PLATE_BORDER = new Color(0x3a, 0x35, 0x2b);
    /** Inner plate fill behind the name strip. Slightly darker than the
     *  border so the boss name reads well on it. */
    private static final Color PLATE_FILL = new Color(0x1f, 0x1c, 0x16);
    /** Boss-HUD "current HP" green (user-specified #7abf43). */
    private static final Color HP_GREEN = new Color(0x7a, 0xbf, 0x43);
    /** Boss-HUD "missing HP" red (user-specified #a53714). */
    private static final Color HP_RED = new Color(0xa5, 0x37, 0x14);
    /** Boss name text colour (user-specified #b39557). Less saturated than
     *  the first cut's bright yellow -- matches the reference screenshot. */
    private static final Color NAME_YELLOW = new Color(0xb3, 0x95, 0x57);
    /**
     * Lookahead "this HP is about to be lost" overlay (user-specified
     * #3f8c21). Drawn between the bright green (current HP) and the red
     * (already-missing HP) when the NEXT BOSS_HEALTH keyframe on the same
     * Character will lower HP. The chunk's width shrinks linearly as we
     * approach the next keyframe so the bar visually "deflates" into the
     * future value -- this is the "decreasing health animation" the user
     * asked for.
     */
    private static final Color HP_DECAY_GREEN = new Color(0x3f, 0x8c, 0x21);

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

        // Plate: brown frame (#3a352b) outer + dark fill inside. PLATE_PAD
        // around the contents gives the "chunky" look the reference shows.
        // Frame is a single fillRect; we then paint the inside with PLATE_FILL
        // leaving a visible brown border on all four sides.
        final int frameThick = 2;
        int innerW = BAR_WIDTH;
        int innerH = NAME_HEIGHT + BAR_HEIGHT;
        int plateW = innerW + 2 * (frameThick + PLATE_PAD);
        int plateH = innerH + 2 * (frameThick + PLATE_PAD);
        int plateX = x - frameThick - PLATE_PAD;
        int plateY = y - frameThick - PLATE_PAD;
        graphics.setColor(PLATE_BORDER);
        graphics.fillRect(plateX, plateY, plateW, plateH);
        graphics.setColor(PLATE_FILL);
        graphics.fillRect(plateX + frameThick, plateY + frameThick,
                plateW - 2 * frameThick, plateH - 2 * frameThick);

        // Boss name -- pulls from the owning Character so the user can label
        // the bar by naming the Character that holds the keyframe.
        graphics.setFont(FontManager.getRunescapeBoldFont());
        FontMetrics nameFm = graphics.getFontMetrics();
        String bossName = bossChar.getName() == null ? "" : bossChar.getName();
        int nameX = x + (BAR_WIDTH - nameFm.stringWidth(bossName)) / 2;
        int nameY = y + nameFm.getAscent() - 2;
        graphics.setColor(Color.BLACK);
        graphics.drawString(bossName, nameX + 1, nameY + 1);
        graphics.setColor(NAME_YELLOW);
        graphics.drawString(bossName, nameX, nameY);

        // HP bar sits below the name strip. Red base (missing HP) -> green
        // fill (current HP) -> dark-green lookahead overlay if the next boss
        // keyframe will lower HP. Order matters: each layer covers the
        // previous within its slice.
        int barY = y + NAME_HEIGHT;
        graphics.setColor(HP_RED);
        graphics.fillRect(x, barY, BAR_WIDTH, BAR_HEIGHT);

        double ratio = (double) currentHp / (double) maxHp;
        int greenW = (int) Math.round(ratio * BAR_WIDTH);
        if (greenW > 0)
        {
            graphics.setColor(HP_GREEN);
            graphics.fillRect(x, barY, greenW, BAR_HEIGHT);
        }

        // Lookahead "decreasing health animation": if a NEXT BOSS_HEALTH
        // keyframe on this Character will lower HP, animate the bright-green
        // endpoint from currentHp toward that future HP over the time
        // between the two keyframes. The chunk that's currently being lost
        // (between the live endpoint and the current keyframe's static HP)
        // is overlaid in dark green so the user reads it as "this HP is
        // about to be gone" rather than as just-missing.
        //
        // At the next keyframe's tick, the dark-green chunk is at its full
        // width (delta) and then the next paint -- now reading the next
        // keyframe as current -- shows that range as red, so the visual
        // transition is dark-green -> red on the same pixels.
        HealthKeyFrame nextKf = findNextBossKeyFrame(bossChar, bossKf.getTick());
        if (nextKf != null && nextKf.getCurrentHealth() < currentHp)
        {
            int futureHp = Math.max(0, nextKf.getCurrentHealth());
            int delta = currentHp - futureHp;
            double durBetween = nextKf.getTick() - bossKf.getTick();
            if (durBetween > 0)
            {
                double progress = Math.max(0.0, Math.min(1.0,
                        (currentTick - bossKf.getTick()) / durBetween));
                double liveHp = currentHp - progress * delta;
                int liveGreenW = (int) Math.round(liveHp / maxHp * BAR_WIDTH);
                int decayW = Math.max(0, greenW - liveGreenW);
                if (decayW > 0)
                {
                    graphics.setColor(HP_DECAY_GREEN);
                    graphics.fillRect(x + liveGreenW, barY, decayW, BAR_HEIGHT);
                }
            }
        }

        // "current / max (pct%)" text centred inside the HP bar. Bold +
        // shadowed so it stays readable on either the green or red half
        // of the bar. The percentage matches the engine's HpbarHud widget
        // (one decimal place) so cinematic captures read as the real UI.
        graphics.setFont(FontManager.getRunescapeBoldFont());
        FontMetrics fm = graphics.getFontMetrics();
        double pct = ratio * 100.0;
        String text = String.format("%d / %d (%.1f%%)", currentHp, maxHp, pct);
        int textW = fm.stringWidth(text);
        int textX = x + (BAR_WIDTH - textW) / 2;
        int textY = barY + (BAR_HEIGHT + fm.getAscent()) / 2 - 2;
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, textX + 1, textY + 1);
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, textX, textY);

        return null;
    }

    /**
     * Walks {@code character}'s HEALTH keyframe array and returns the earliest
     * BOSS_HEALTH keyframe whose tick is strictly greater than {@code referenceTick}.
     * Used by the lookahead "decreasing health animation" to source the future
     * HP value the current bar is animating toward.
     */
    private HealthKeyFrame findNextBossKeyFrame(Character character, double referenceTick)
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
            if (hkf.getTick() <= referenceTick)
            {
                continue;
            }
            if (best == null || hkf.getTick() < best.getTick())
            {
                best = hkf;
            }
        }
        return best;
    }
}
