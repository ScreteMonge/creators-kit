package com.creatorskit;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;

/**
 * CKObject variant for face-trajectory projectiles.
 *
 * <p>The render path runs the standard CKObject animation pipeline first,
 * then DEEP-COPIES the resulting Model into an owned instance and applies
 * pitch to the copy's vertex arrays. The deep copy step is mandatory
 * because {@link Client#applyTransformations} returns the engine's shared
 * single-instance buffer: its vertex arrays get overwritten by ANY later
 * applyTransformations call (other entities in the scene, engine-internal
 * calls between our return and the engine's actual draw), so mutating the
 * shared buffer in place -- whether to apply pitch or to pre-rotate before
 * animation -- loses the mutation before render. The shared-buffer warning
 * is explicit in the API:
 * <blockquote>"The returned model is shared and shouldn't be used after
 * any other call to applyTransformations, including calls made by the
 * client internally."</blockquote>
 *
 * <p>Earlier attempts to fix face-trajectory (pre-rotating baseModel, or
 * skipping the rest-pose restore when animated == base) couldn't survive
 * the shared-buffer overwrite because the pitched data lived in the shared
 * buffer either way. Owning the model via {@link Client#mergeModels(Model...)}
 * with a single input gives us a fresh Model whose vertex arrays the engine
 * doesn't touch -- pitch survives until the next frame's getModel runs.
 *
 * <p>{@link #setModel} no longer needs special snapshot bookkeeping; the
 * deep copy is recreated every frame from the animation pipeline output.
 */
@Getter
@Setter
public class ProjectileCKObject extends CKObject
{
    /** Pitch angle in radians. Positive = nose up. */
    private double pitchRadians = 0.0;

    /**
     * When false, {@link #getModel} skips the deep-copy + pitch path and
     * returns the standard CKObject animation result directly.
     */
    private boolean pitchEnabled = false;

    private final Client client;

    public ProjectileCKObject(Client client)
    {
        super(client);
        this.client = client;
    }

    @Override
    public Model getModel()
    {
        Model sharedAnimated = super.getModel();
        if (!pitchEnabled || pitchRadians == 0.0 || sharedAnimated == null)
        {
            return sharedAnimated;
        }

        // Deep-copy the shared animation buffer into an owned Model. Single-
        // input mergeModels allocates a fresh Model with its own vertex /
        // face arrays so we can mutate them without racing the engine's
        // internal applyTransformations calls. If mergeModels returns null
        // (loading / failure), fall back to the shared buffer -- the pitch
        // is lost but at least the projectile renders with its animation.
        Model owned = client.mergeModels(new Model[]{sharedAnimated}, 1);
        if (owned == null)
        {
            return sharedAnimated;
        }

        float[] vy = owned.getVerticesY();
        float[] vz = owned.getVerticesZ();
        if (vy == null || vz == null || vy.length != vz.length || vy.length == 0)
        {
            return owned;
        }

        applyPitchToVerts(vy, vz, pitchRadians);
        owned.calculateBoundsCylinder();
        return owned;
    }

    private static void applyPitchToVerts(float[] vy, float[] vz, double pitchRadians)
    {
        // Rotate around the Y/Z centroid of the animated mesh so the pitch
        // pivots through the model's geometric center rather than the OSRS
        // origin (which would translate the whole projectile during pitch).
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
