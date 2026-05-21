package com.creatorskit.programming;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.timesheet.keyframe.ScreenFadeKeyFrame;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Whisperer / Blackstone-Fragment style global fade -- fullscreen tint with a
 * soft circular cutout in the centre that lets the player (and anything in
 * the middle of the canvas) stay visible while everything around them goes
 * dark.
 *
 * <p>The fade is "global" -- any Character can own a {@link ScreenFadeKeyFrame}
 * but the overlay always renders to the full canvas regardless of which
 * Character holds the keyframe. This overlay scans every Character every
 * frame, picks the keyframe whose timing window contains the current tick,
 * and draws it. If multiple keyframes overlap (e.g. two scene-controller
 * characters both fade at the same time), the most-recently-started one
 * wins; this keeps the rendering deterministic without forcing the user to
 * dedup keyframes manually.
 */
public class ScreenFadeOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;

    @Inject
    private ScreenFadeOverlay(Client client, CreatorsPlugin plugin)
    {
        setPosition(OverlayPosition.DYNAMIC);
        // UNDER_WIDGETS renders AFTER the 3D scene AND after the game's own
        // overhead pass (HP bars, names) but BEFORE HUD widgets. ABOVE_SCENE
        // would draw under the overheads -- the player model would be covered
        // but their HP bar / name plate would still show through, ruining the
        // Whisperer/Blackstone "everything goes dark" effect. UNDER_WIDGETS
        // covers both while keeping chat/minimap/inventory visible.
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
        ScreenFadeKeyFrame active = findActiveFade(currentTick);
        if (active == null)
        {
            return null;
        }

        // Phase math: fadeIn → hold → fadeOut. All values in game ticks.
        double t = currentTick - active.getTick();
        if (t < 0 || t > active.totalDurationTicks())
        {
            return null;
        }

        int alpha = computeAlpha(active, t);
        if (alpha <= 0)
        {
            return null;
        }

        int canvasW = client.getCanvasWidth();
        int canvasH = client.getCanvasHeight();
        if (canvasW <= 0 || canvasH <= 0)
        {
            return null;
        }

        Color base = new Color(active.getRgb());
        int r = base.getRed();
        int g = base.getGreen();
        int b = base.getBlue();

        int ringRadius = active.getRingRadius();
        int ringFeather = Math.max(0, active.getRingFeather());

        if (ringRadius <= 0)
        {
            // No ring -- just a flat fullscreen tint.
            Color flat = new Color(r, g, b, alpha);
            graphics.setColor(flat);
            graphics.fillRect(0, 0, canvasW, canvasH);
            return null;
        }

        // Step 1: fill the canvas with the tint at peak alpha.
        Color filled = new Color(r, g, b, alpha);
        graphics.setColor(filled);
        graphics.fillRect(0, 0, canvasW, canvasH);

        // Step 2: punch a soft hole in the middle using DST_OUT. A radial
        // gradient with opaque centre and transparent ring boundary will
        // subtract alpha from the filled rect, leaving the ring clear and
        // the rest of the canvas tinted.
        Composite originalComposite = graphics.getComposite();
        Paint originalPaint = graphics.getPaint();
        graphics.setComposite(AlphaComposite.DstOut);

        float cx = canvasW * 0.5f;
        float cy = canvasH * 0.5f;
        float gradientRadius = ringRadius + Math.max(1, ringFeather);

        float innerStop = Math.min(0.999f, (float) ringRadius / gradientRadius);
        // Stops: opaque from centre to inner edge, then fade to transparent at the outer edge.
        float[] fractions = new float[]{0f, innerStop, 1f};
        Color[] colors = new Color[]{
                new Color(0, 0, 0, 255),
                new Color(0, 0, 0, 255),
                new Color(0, 0, 0, 0)
        };

        RadialGradientPaint hole = new RadialGradientPaint(
                new Point2D.Float(cx, cy),
                gradientRadius,
                fractions,
                colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
        graphics.setPaint(hole);
        graphics.fill(new Rectangle(0, 0, canvasW, canvasH));

        graphics.setComposite(originalComposite);
        graphics.setPaint(originalPaint);

        return null;
    }

    /**
     * Returns the most-recently-started fade keyframe that's currently within its
     * fade-in / hold / fade-out envelope. Returns null if no fade is active.
     * Phase 2: reads from the central GlobalKeyFrames store instead of walking
     * every Character -- globals no longer require a per-Character owner.
     */
    private ScreenFadeKeyFrame findActiveFade(double currentTick)
    {
        ScreenFadeKeyFrame best = null;
        double bestStart = Double.NEGATIVE_INFINITY;

        ScreenFadeKeyFrame[] all = plugin.getGlobalKeyFrames().getScreenFadeKeyFramesSafe();
        for (int i = 0; i < all.length; i++)
        {
            ScreenFadeKeyFrame kf = all[i];
            if (kf == null)
            {
                continue;
            }
            double elapsed = currentTick - kf.getTick();
            if (elapsed < 0 || elapsed > kf.totalDurationTicks())
            {
                continue;
            }
            if (kf.getTick() > bestStart)
            {
                bestStart = kf.getTick();
                best = kf;
            }
        }
        return best;
    }

    private int computeAlpha(ScreenFadeKeyFrame kf, double t)
    {
        double fadeIn = kf.getFadeInTicks();
        double hold = kf.getHoldTicks();
        double fadeOut = kf.getFadeOutTicks();
        int peak = Math.max(0, Math.min(255, kf.getPeakAlpha()));

        if (t < fadeIn)
        {
            if (fadeIn <= 0)
            {
                return peak;
            }
            return (int) Math.round(peak * (t / fadeIn));
        }
        if (t < fadeIn + hold)
        {
            return peak;
        }
        double outProgress = (t - fadeIn - hold) / Math.max(0.0001, fadeOut);
        return (int) Math.round(peak * Math.max(0.0, 1.0 - outProgress));
    }
}
