package com.creatorskit.swing.timesheet.keyframe;

import com.creatorskit.swing.timesheet.keyframe.settings.HealthbarSprite;
import com.creatorskit.swing.timesheet.keyframe.settings.HitsplatSprite;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthKeyFrame extends KeyFrame
{
    /** Default stack position when no value is set in an old save. HP sits topmost by default. */
    public static final int DEFAULT_ORDER = 0;
    /** Sentinel for {@link #width}: 0 means "auto-scale from maxHealth". */
    public static final int AUTO_WIDTH = 0;

    private double duration;
    private HealthbarSprite healthbarSprite;
    private int maxHealth;
    private int currentHealth;
    /**
     * Added 2.3.x. Stack position relative to other bars (HP/Shield/Special). Lower
     * values draw higher on screen. Defaults to {@link #DEFAULT_ORDER} so pre-2.3
     * saves -- where Gson fills missing ints with 0 -- preserve HP-at-top behaviour.
     */
    private int order = DEFAULT_ORDER;
    /**
     * Added 2.3.x. Explicit pixel width override. {@link #AUTO_WIDTH} (0) -- the
     * Gson default for missing ints -- means "auto-scale from {@link #maxHealth}",
     * so pre-2.3 saves keep the auto-scaling behaviour they already had.
     */
    private int width = AUTO_WIDTH;
    /**
     * True when this keyframe was created by the hitsplat -> bar auto-sync
     * (see TimeSheetPanel.applyHitsplatSyncAt). The sync owns autoSynced
     * keyframes -- if all hitsplats at this tick are removed / moved away,
     * the cleanup pass removes any bar KFs left here that still have this
     * flag set. User-created keyframes default false (Gson fills missing
     * booleans with false) so they survive cleanup. Editing an autoSynced
     * keyframe via the card resets it to false -- promoting it to manual.
     */
    private boolean autoSynced = false;
    /**
     * INVERTED flag for the "Sync hitsplats" toggle. Stored as
     * disable-flag so the Gson default of {@code false} (for missing fields
     * on pre-existing saves) maps to the user-visible default of "Sync
     * hitsplats: ON". Read via {@link #isSyncHitsplats()} which returns
     * {@code !syncHitsplatsDisabled}.
     */
    private boolean syncHitsplatsDisabled = false;

    /** UI-facing convenience: "Sync hitsplats" checkbox state. True = on. */
    public boolean isSyncHitsplats()
    {
        return !syncHitsplatsDisabled;
    }

    /** UI-facing convenience: writes the checkbox state. True = on. */
    public void setSyncHitsplats(boolean syncHitsplats)
    {
        this.syncHitsplatsDisabled = !syncHitsplats;
    }

    public HealthKeyFrame(double tick, double duration,
                          HealthbarSprite healthbarSprite,
                          int maxHealth, int currentHealth)
    {
        this(tick, duration, healthbarSprite, maxHealth, currentHealth, DEFAULT_ORDER, AUTO_WIDTH);
    }

    public HealthKeyFrame(double tick, double duration,
                          HealthbarSprite healthbarSprite,
                          int maxHealth, int currentHealth,
                          int order)
    {
        this(tick, duration, healthbarSprite, maxHealth, currentHealth, order, AUTO_WIDTH);
    }

    public HealthKeyFrame(double tick, double duration,
                          HealthbarSprite healthbarSprite,
                          int maxHealth, int currentHealth,
                          int order, int width)
    {
        super(KeyFrameType.HEALTH, tick);
        this.duration = duration;
        this.healthbarSprite = healthbarSprite;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
        this.order = order;
        this.width = width;
    }
}
