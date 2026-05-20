package com.creatorskit;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;

/**
 * CKObject variant for face-trajectory projectiles.
 *
 * <p>Pitch is applied by pre-rotating the projectile's own {@code baseModel}
 * before the animation pipeline runs, NOT by mutating the post-animation
 * model. The post-animation model is the shared buffer returned by
 * {@code Client.applyTransformations} -- mutating it in place was visibly
 * coupling the projectile's render to the source character's animation,
 * because any subsequent {@code applyTransformations} call from the source's
 * own {@code getModel()} (or any other entity in the scene) would overwrite
 * the buffer between our return and the engine's draw, leaving the projectile
 * rendering with whoever's vertices were written last.
 *
 * <p>{@code baseModel} is unique to this projectile (built fresh in
 * {@code Programmer.ensureProjectileSlot}), so mutating it is safe -- nothing
 * else reads from the same memory. We snapshot the rest-pose vertices once
 * after {@link #setModel} and restore from the snapshot at the start of every
 * {@link #getModel} so per-frame pitch values don't accumulate.
 *
 * <p>Trade-off: applying pitch via base-mesh rotation means the bone-vertex
 * associations the animation pipeline uses are based on the rotated
 * positions. For morph-based spotanims (most projectiles -- fireballs,
 * arrows, etc.) this is invisible. For bone-rigged spotanims at extreme
 * pitches the rigging may show small artifacts; the alternative
 * (post-animation vertex mutation) caused the much worse source-coupling
 * bug, so we accept the trade-off here.
 */
@Getter
@Setter
public class ProjectileCKObject extends CKObject
{
    /** Pitch angle in radians. Positive = nose up. */
    private double pitchRadians = 0.0;

    /**
     * When false, {@link #getModel} skips the entire pitch path and behaves
     * exactly like {@link CKObject} -- baseModel stays at rest pose.
     */
    private boolean pitchEnabled = false;

    /**
     * Snapshot of {@code baseModel}'s rest-pose Y/Z vertex arrays. Captured on
     * the first {@link #getModel} call after a {@link #setModel} (deferred so
     * we don't snapshot a model that hasn't been built yet) and reused every
     * subsequent frame to undo the previous frame's pitch before applying the
     * current one. {@link #setModel} clears this so the next snapshot matches
     * the new model's vertex count.
     */
    private float[][] basePitchSnapshot;

    public ProjectileCKObject(Client client)
    {
        super(client);
    }

    @Override
    public void setModel(Model baseModel)
    {
        super.setModel(baseModel);
        // Defer snapshot until the first getModel -- baseModel here may not have
        // its vertex arrays populated yet (the projectile constructor pipeline
        // assigns these on the client thread).
        this.basePitchSnapshot = null;
    }

    @Override
    public Model getModel()
    {
        Model base = getBaseModel();
        if (!pitchEnabled || pitchRadians == 0.0 || base == null)
        {
            return super.getModel();
        }

        float[] vy = base.getVerticesY();
        float[] vz = base.getVerticesZ();
        if (vy == null || vz == null || vy.length != vz.length || vy.length == 0)
        {
            return super.getModel();
        }

        // Snapshot once per baseModel so subsequent frames can wind back the
        // previous pitch before applying this frame's. Lazy-init handles the
        // common case where setModel runs before vertex arrays are populated.
        if (basePitchSnapshot == null || basePitchSnapshot[0].length != vy.length)
        {
            basePitchSnapshot = new float[][]{vy.clone(), vz.clone()};
        }
        else
        {
            System.arraycopy(basePitchSnapshot[0], 0, vy, 0, vy.length);
            System.arraycopy(basePitchSnapshot[1], 0, vz, 0, vz.length);
        }

        applyPitchToBase(vy, vz, pitchRadians);
        base.calculateBoundsCylinder();

        // Now run the animation pipeline (CKObject.getModel) against the rotated
        // baseModel. The animated result is the shared applyTransformations
        // buffer with vertex data already pitched, so we don't have to touch
        // it after the fact -- the source-coupling bug came from doing so.
        Model animated = super.getModel();

        // Defensively restore baseModel to its rest pose now, before returning,
        // so anything reading baseModel between this getModel and the next
        // (e.g. a Ctrl-hover preview or an inspect) sees the unrotated mesh.
        //
        // BUT skip the restore when the animation pipeline returned baseModel
        // unchanged (animated == base). That happens whenever no skeletal
        // transformation applies -- RuneLite's AnimationController.animate
        // short-circuits to `return model` when both this and the pose
        // controller have a null animation, which is the actual state for a
        // freshly-loaded spotanim before its first tick OR for any animation
        // frame whose rigging is identity. Restoring vy/vz in that case would
        // wipe the pitch we just baked into baseModel BEFORE the engine reads
        // the returned model -- and since `animated` is literally `base`, the
        // user sees the projectile rendered at rest pose despite face-trajectory
        // being on. Skipping the end-of-frame restore in this branch keeps the
        // pitched vertices visible; the next frame's start-of-frame
        // snapshot-restore-then-apply still resets cleanly, so no drift.
        if (animated != base)
        {
            System.arraycopy(basePitchSnapshot[0], 0, vy, 0, vy.length);
            System.arraycopy(basePitchSnapshot[1], 0, vz, 0, vz.length);
            base.calculateBoundsCylinder();
        }

        return animated;
    }

    private static void applyPitchToBase(float[] vy, float[] vz, double pitchRadians)
    {
        // Rotate around the Y/Z centroid of the rest mesh so the pitch pivots
        // through the model's geometric center rather than the OSRS origin.
        double sumY = 0;
        double sumZ = 0;
        int n = vy.length;
        for (int i = 0; i < n; i++)
        {
            sumY += vy[i];
            sumZ += vz[i];
        }
        float cy = (float) (sumY / n);
        float cz = (float) (sumZ / n);

        double sinP = Math.sin(pitchRadians);
        double cosP = Math.cos(pitchRadians);
        for (int i = 0; i < n; i++)
        {
            double dy = vy[i] - cy;
            double dz = vz[i] - cz;
            vy[i] = (float) (dy * cosP - dz * sinP + cy);
            vz[i] = (float) (dy * sinP + dz * cosP + cz);
        }
    }
}
