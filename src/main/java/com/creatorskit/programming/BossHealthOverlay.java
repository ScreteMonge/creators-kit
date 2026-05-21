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
    /** Outer dark border that the engine paints around the boss HUD plate. */
    private static final Color PLATE_BORDER = new Color(0, 0, 0);
    /** Inner plate fill -- matches the HpbarHud widget's dark grey backing. */
    private static final Color PLATE_FILL = new Color(35, 35, 35);
    /** RuneLite's canonical "current HP" green from OpponentInfoOverlay. */
    private static final Color HP_GREEN = new Color(0, 146, 54);
    /** RuneLite's canonical "missing HP" red from OpponentInfoOverlay. */
    private static final Color HP_RED = new Color(102, 15, 16);
    /** OSRS HUD-style yellow used for boss name text on the engine's
     *  HpbarHud widget. Was white in the first cut but the reference
     *  screenshot (The Whisperer) clearly uses the engine yellow, so the
     *  Creator's Kit bar needed to match for cinematic captures to read as
     *  the real interface. */
    private static final Color NAME_YELLOW = new Color(255, 176, 0);
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

        // Plate: dark grey backing with a 1px black border, sized to hold the
        // boss-name strip + the HP bar. Matches the OSRS HpbarHud widget layout.
        int plateW = BAR_WIDTH + 6;
        int plateH = NAME_HEIGHT + BAR_HEIGHT + 6;
        graphics.setColor(PLATE_BORDER);
        graphics.fillRect(x - 3, y - 2, plateW, plateH);
        graphics.setColor(PLATE_FILL);
        graphics.fillRect(x - 2, y - 1, plateW - 2, plateH - 2);

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
        // fill (current HP). Drawing red first then green over it mirrors how
        // the engine layers the widget.
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

        // Damage indicator: bright pulse on the just-lost HP range that fades
        // over DAMAGE_FADE_TICKS. Drawn ON TOP of the red base so the user can
        // see the chunk that was lost distinctly from already-missing HP.
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
                    graphics.setColor(new Color(220, 80, 80));
                    graphics.fillRect(x + redStart, barY, redW, BAR_HEIGHT);
                    graphics.setComposite(oc);
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
