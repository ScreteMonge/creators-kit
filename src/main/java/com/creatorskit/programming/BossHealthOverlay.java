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
 * <p>Styling matches the OSRS boss HP HUD widget (interface 303 /
 * {@code InterfaceID.HpbarHud}): boss name text on top, dark grey plate, and
 * a green/red HP bar below. Colours come from RuneLite's own OpponentInfoOverlay
 * so the keyframe-driven bar reads identically to engine-rendered ones.
 *
 * <p>Damage animation: when the current keyframe has less HP than the
 * previous BOSS_HEALTH keyframe on the same Character, the bright green
 * snaps immediately to the new (lower) endpoint and a dark-green chunk
 * paints at FULL WIDTH over the just-lost range. Over 1 game tick the
 * chunk's right edge retreats toward its left edge (the bright green
 * endpoint), shrinking the chunk right-to-left until it's gone --
 * unrolled pixels reveal the red base. Pure linear width interp keyed
 * on currentTick keeps scrubbing deterministic.
 *
 * <p>HitsplatKeyFrame edits drive most damage events: TimeSheetPanel auto-
 * creates / auto-updates the matching bar keyframe (Health / Shield /
 * Special depending on the hitsplat sprite) on the same tick, which is
 * what trips this animation.
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
     * Total time the dark-green damage chunk is on screen after a damage
     * keyframe fires. Made up of a {@link #DRAIN_HOLD_TICKS} hold phase at
     * full width followed by a drain phase that retracts the right edge to
     * zero -- so total = hold + drain.
     */
    private static final double DRAIN_DURATION_TICKS = 1.0;
    /**
     * Hold phase: how long the chunk sits at full width before the right
     * edge starts retreating. User asked for "~0.5 ticks" of hold so the
     * eye registers the chunk before it animates away.
     */
    private static final double DRAIN_HOLD_TICKS = 0.5;

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

        // Lifecycle fade alpha. Compute lifecycle bounds across every
        // boss-health keyframe on bossChar (manual + synced), then check
        // currentTick against the fade-in window at the start and the
        // fade-out window at the end. Synced keyframes inherit fade values
        // from the manual one, so reading from the first / last keyframe
        // by tick gives the user's authored values. Alpha == 1.0 in the
        // middle of the lifecycle so hitsplats don't trigger any fade.
        float lifecycleAlpha = computeLifecycleAlpha(bossChar, currentTick);
        if (lifecycleAlpha <= 0f) return null;
        Composite originalComposite = null;
        if (lifecycleAlpha < 1f)
        {
            originalComposite = graphics.getComposite();
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, lifecycleAlpha));
        }

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

        // Damage animation: when the previous BOSS_HEALTH keyframe had more
        // HP, the bright green is already at its new (lower) endpoint above.
        // We paint the just-lost chunk as dark green at FULL WIDTH for
        // DRAIN_HOLD_TICKS, then retract its right edge to zero over the
        // remaining (DRAIN_DURATION - DRAIN_HOLD) ticks. Left edge stays
        // anchored at the bright green endpoint; right edge slides left.
        HealthKeyFrame previous = findPreviousBossKeyFrame(bossChar, bossKf.getTick());
        if (previous != null && previous.getCurrentHealth() > currentHp)
        {
            int oldHp = Math.min(previous.getCurrentHealth(), maxHp);
            double elapsed = currentTick - bossKf.getTick();
            if (elapsed >= 0 && elapsed < DRAIN_DURATION_TICKS)
            {
                int decayStart = greenW; // anchored left edge (bright green endpoint)
                int decayEnd = (int) Math.round((double) oldHp / maxHp * BAR_WIDTH);
                int fullW = Math.max(0, decayEnd - decayStart);
                // Hold phase keeps width = fullW; drain phase linearly retracts
                // to 0 over the remaining window. drainSpan must be > 0 (else
                // fall back to "always full while in window" -- the constants
                // both being equal would be a misconfig but shouldn't crash).
                double drainSpan = DRAIN_DURATION_TICKS - DRAIN_HOLD_TICKS;
                double drainProgress = drainSpan > 0
                        ? Math.max(0.0, (elapsed - DRAIN_HOLD_TICKS) / drainSpan)
                        : 0.0;
                int decayW = (int) Math.round(fullW * (1.0 - drainProgress));
                if (decayW > 0)
                {
                    graphics.setColor(HP_DECAY_GREEN);
                    graphics.fillRect(x + decayStart, barY, decayW, BAR_HEIGHT);
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

        if (originalComposite != null)
        {
            graphics.setComposite(originalComposite);
        }

        return null;
    }

    /**
     * Lifecycle alpha for the boss healthbar's fade-in / fade-out animation.
     *
     * <p>The "lifecycle" spans from the earliest boss-health keyframe's tick
     * to the latest {@code tick + duration} across every boss-health
     * keyframe on the Character. Within {@code fadeInTicks} of the start
     * the alpha ramps 0 -> 1; within {@code fadeOutTicks} of the end it
     * ramps 1 -> 0. Outside both windows alpha is 1.0, so hitsplats /
     * mid-fight events never re-trigger a fade.
     *
     * <p>fadeIn is sourced from the FIRST keyframe (lowest tick) and
     * fadeOut from the LAST one (highest tick+duration). Since
     * sync-created keyframes inherit fade values from the prior manual
     * keyframe, both ends agree on the user-authored values.
     *
     * <p>Returns 0 when the lifecycle is degenerate or currentTick is
     * outside the lifecycle entirely -- the render caller treats <= 0 as
     * "skip the bar this frame".
     */
    private float computeLifecycleAlpha(Character character, double currentTick)
    {
        KeyFrame[] frames = character.getKeyFrames(KeyFrameType.HEALTH);
        if (frames == null) return 1f;

        double lifecycleStart = Double.POSITIVE_INFINITY;
        double lifecycleEnd = Double.NEGATIVE_INFINITY;
        HealthKeyFrame first = null;
        HealthKeyFrame last = null;
        for (KeyFrame kf : frames)
        {
            if (!(kf instanceof HealthKeyFrame)) continue;
            HealthKeyFrame h = (HealthKeyFrame) kf;
            if (h.getHealthbarSprite() != HealthbarSprite.BOSS_HEALTH) continue;
            double start = h.getTick();
            double end = h.getTick() + h.getDuration();
            if (start < lifecycleStart) { lifecycleStart = start; first = h; }
            if (end > lifecycleEnd) { lifecycleEnd = end; last = h; }
        }
        if (first == null || last == null) return 1f;
        if (currentTick < lifecycleStart || currentTick > lifecycleEnd) return 0f;

        double fadeIn = Math.max(0.0, first.getFadeInTicks());
        double fadeOut = Math.max(0.0, last.getFadeOutTicks());

        // Fade-in window: [lifecycleStart, lifecycleStart + fadeIn]
        if (fadeIn > 0 && currentTick < lifecycleStart + fadeIn)
        {
            double t = (currentTick - lifecycleStart) / fadeIn;
            return (float) Math.max(0.0, Math.min(1.0, t));
        }
        // Fade-out window: [lifecycleEnd - fadeOut, lifecycleEnd]
        if (fadeOut > 0 && currentTick > lifecycleEnd - fadeOut)
        {
            double t = (lifecycleEnd - currentTick) / fadeOut;
            return (float) Math.max(0.0, Math.min(1.0, t));
        }
        return 1f;
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
