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
 * <p>Damage animation: when the current keyframe has less HP than the
 * previous BOSS_HEALTH keyframe on the same Character, the bright-green
 * endpoint sweeps from the old HP down to the new HP over 1 game tick. A
 * dark-green tail trails the sweep, visually "draining" the lost HP. The
 * sweep is pure linear interp keyed on currentTick so scrubbing is
 * deterministic -- any visit to the same tick paints the same pixels.
 *
 * <p>HitsplatKeyFrame edits drive most damage events: they auto-create or
 * auto-update a matching Health keyframe on the same tick (handled in
 * the keyframe-add path), which is what trips this animation.
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
     * Vertical padding inside the brown frame (above the name, below the bar).
     * Reference shows ~2px of frame-only space; the bar fills the inner width
     * edge-to-edge horizontally. Set small so the bar sits chunky against the
     * frame instead of floating in a dark gap.
     */
    private static final int PLATE_PAD = 2;
    /** Brown frame around the HUD -- user-specified #3a352b. */
    private static final Color PLATE_BORDER = new Color(0x3a, 0x35, 0x2b);
    /** Inner plate fill behind the name strip. Slightly darker than the
     *  border so the boss name reads well on it. */
    private static final Color PLATE_FILL = new Color(0x1f, 0x1c, 0x16);
    /** Boss-HUD "current HP" green (user-specified #7abf43). */
    private static final Color HP_GREEN = new Color(0x7a, 0xbf, 0x43);
    /** Boss-HUD "missing HP" red (user-specified #a53714). */
    private static final Color HP_RED = new Color(0xa5, 0x37, 0x14);
    /** Boss name text colour (user-specified #b39557). */
    private static final Color NAME_YELLOW = new Color(0xb3, 0x95, 0x57);
    /**
     * Dark green for the "draining" tail that trails behind the bright green
     * during a damage animation (user-specified #3f8c21). Driven by the
     * PREVIOUS BOSS_HEALTH keyframe: at the moment of damage the bright
     * green is still at the old HP, then over {@link #DRAIN_DURATION_TICKS}
     * sweeps down to the new HP, leaving this dark green tail. After the
     * sweep the dark green is gone and the just-vacated area paints red.
     */
    private static final Color HP_DECAY_GREEN = new Color(0x3f, 0x8c, 0x21);
    /**
     * How long the bright-green sweep takes after a damage keyframe fires.
     * Spec from the user: "always lasts 1 tick".
     */
    private static final double DRAIN_DURATION_TICKS = 1.0;

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

        // Damage animation: when the PREVIOUS BOSS_HEALTH keyframe had more
        // HP, sweep the bright-green endpoint from the old HP down to the
        // new HP over DRAIN_DURATION_TICKS (1 tick). The chunk trailing
        // behind the sweep is dark green -- visually "draining" the lost
        // HP. After the sweep completes the chunk disappears, leaving the
        // static bright green at currentHp and red on the rest of the bar.
        //
        // Hitsplats auto-create/update the matching Health keyframe (handled
        // elsewhere), so most damage-animation-rendering trigger paths
        // originate from a HitsplatKeyFrame edit, not a manual Health KF.
        HealthKeyFrame previous = findPreviousBossKeyFrame(bossChar, bossKf.getTick());
        if (previous != null && previous.getCurrentHealth() > currentHp)
        {
            int oldHp = Math.min(previous.getCurrentHealth(), maxHp);
            int delta = oldHp - currentHp;
            double elapsed = currentTick - bossKf.getTick();
            if (elapsed >= 0 && elapsed < DRAIN_DURATION_TICKS)
            {
                double progress = elapsed / DRAIN_DURATION_TICKS; // 0 -> 1
                // Bright green endpoint sweeps from oldHp to newHp (=currentHp).
                double sweptHp = oldHp - progress * delta;
                int sweptW = (int) Math.round(sweptHp / maxHp * BAR_WIDTH);
                // The bright green base layer was drawn at currentHp width
                // (the post-damage value). Extend it back out to the swept
                // endpoint so the bar visually starts at oldHp and shrinks.
                if (sweptW > greenW)
                {
                    graphics.setColor(HP_GREEN);
                    graphics.fillRect(x + greenW, barY, sweptW - greenW, BAR_HEIGHT);
                }
                // Dark green tail from the swept endpoint to oldHp's
                // position -- the just-vacated portion that hasn't gone red
                // yet. Width grows over the tick from 0 to delta.
                int oldW = (int) Math.round((double) oldHp / maxHp * BAR_WIDTH);
                int decayW = Math.max(0, oldW - sweptW);
                if (decayW > 0)
                {
                    graphics.setColor(HP_DECAY_GREEN);
                    graphics.fillRect(x + sweptW, barY, decayW, BAR_HEIGHT);
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
     * Walks {@code character}'s HEALTH keyframe array and returns the latest
     * BOSS_HEALTH keyframe whose tick is strictly less than {@code referenceTick}.
     * Used by the damage animation to source the "old HP" the bright-green
     * endpoint sweeps down from.
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
