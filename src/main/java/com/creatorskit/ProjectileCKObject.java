package com.creatorskit;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;

/**
 * CKObject variant for face-trajectory projectiles. The base CKObject's getModel()
 * returns the result of {@code applyTransformations(baseModel, animation, ...)} -- a
 * fresh animated copy each frame. Animation transforms vertices based on
 * bone-vertex associations relative to their ORIGINAL positions in the base mesh, so
 * pre-rotating baseModel's vertices (as the previous approach did) breaks the
 * bone-skin correspondence and tears the rigging at non-zero pitch.
 *
 * <p>By overriding getModel() to apply the pitch rotation AFTER the parent class'
 * animation pipeline runs, we leave baseModel untouched. The animated vertices are
 * rotated each frame around the live mesh's geometric center, so animation and
 * pitch compose cleanly.
 *
 * <p>The rotation is applied around the X axis of the animated mesh's centroid;
 * since OSRS yaw (orientation) handles horizontal facing, the combined transform
 * aligns the projectile's nose with the velocity vector when the caller sets the
 * pitch from the trajectory.
 */
@Getter
@Setter
public class ProjectileCKObject extends CKObject
{
    /** Pitch angle in radians. Positive = nose up. */
    private double pitchRadians = 0.0;

    /**
     * When false, the override is a no-op and behaves exactly like CKObject -- useful
     * for one-off allocation patterns where the projectile sometimes doesn't pitch.
     */
    private boolean pitchEnabled = false;

    public ProjectileCKObject(Client client)
    {
        super(client);
    }

    @Override
    public Model getModel()
    {
        Model model = super.getModel();
        if (!pitchEnabled || pitchRadians == 0.0 || model == null)
        {
            return model;
        }

        float[] verticesY = model.getVerticesY();
        float[] verticesZ = model.getVerticesZ();
        if (verticesY == null || verticesZ == null || verticesY.length != verticesZ.length || verticesY.length == 0)
        {
            return model;
        }

        // Centroid is computed from the LIVE animated mesh each frame, so the pitch
        // pivot tracks any animation-induced shifts in the model's geometric center.
        double sumY = 0;
        double sumZ = 0;
        int n = verticesY.length;
        for (int i = 0; i < n; i++)
        {
            sumY += verticesY[i];
            sumZ += verticesZ[i];
        }
        float cy = (float) (sumY / n);
        float cz = (float) (sumZ / n);

        double sinP = Math.sin(pitchRadians);
        double cosP = Math.cos(pitchRadians);
        for (int i = 0; i < n; i++)
        {
            double dy = verticesY[i] - cy;
            double dz = verticesZ[i] - cz;
            verticesY[i] = (float) (dy * cosP - dz * sinP + cy);
            verticesZ[i] = (float) (dy * sinP + dz * cosP + cz);
        }
        model.calculateBoundsCylinder();
        return model;
    }
}
