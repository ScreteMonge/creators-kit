package com.creatorskit.swing.timesheet.keyframe;

import lombok.Getter;

/**
 * Per-keyframe custom easing curve, stored on a {@link CameraKeyFrame} when
 * {@link CameraKeyFrame#getEase()} is {@link CameraEaseType#CUSTOM}.
 *
 * <p>Modelled as N control points (xs, ys) in [0, 1] space and evaluated via
 * natural cubic spline interpolation -- same family of curves Paint.NET's
 * "Curves" dialog produces, which the user pointed to as the reference UX.
 *
 * <p>Endpoints are conventionally pinned to (0, 0) and (1, 1) so a CUSTOM
 * ease still starts at progress 0 and ends at progress 1 like the built-in
 * eases. The editor enforces this; the math doesn't strictly require it.
 *
 * <p>The serialised form is just the two arrays -- Gson handles them as
 * plain double[]. The transient second-derivative cache is rebuilt lazily
 * on the first {@link #evaluate(double)} after a load, so saved curves
 * survive a roundtrip without any custom deserialiser.
 */
@Getter
public class CustomEasingCurve
{
    /** Control-point x coordinates, sorted ascending. Range [0, 1]. */
    private double[] xs;
    /** Control-point y coordinates. Typically [0, 1] but overshoot allowed. */
    private double[] ys;

    /**
     * Cached second derivatives for the natural cubic spline, rebuilt
     * lazily on the first evaluate() call after a load / point edit.
     * Transient so the JSON form stays compact and forward-compatible.
     */
    private transient double[] zCache;

    /** Default ctor (Gson + the editor's "start a fresh curve" path): identity. */
    public CustomEasingCurve()
    {
        this.xs = new double[]{0.0, 1.0};
        this.ys = new double[]{0.0, 1.0};
    }

    public CustomEasingCurve(double[] xs, double[] ys)
    {
        if (xs == null || ys == null || xs.length != ys.length || xs.length < 2)
        {
            throw new IllegalArgumentException("xs and ys must be non-null, same length, length >= 2");
        }
        this.xs = xs.clone();
        this.ys = ys.clone();
    }

    /** Deep copy -- safe to use as a kf's curve without aliasing the source. */
    public CustomEasingCurve copy()
    {
        return new CustomEasingCurve(xs, ys);
    }

    public int size()
    {
        return xs == null ? 0 : xs.length;
    }

    public double xAt(int i) { return xs[i]; }
    public double yAt(int i) { return ys[i]; }

    /**
     * Replace the control point set in one shot. {@code xs} must be sorted
     * ascending and the same length as {@code ys}; the editor enforces the
     * ordering invariant during drag.
     */
    public void setPoints(double[] xs, double[] ys)
    {
        if (xs == null || ys == null || xs.length != ys.length || xs.length < 2)
        {
            throw new IllegalArgumentException("xs and ys must be non-null, same length, length >= 2");
        }
        this.xs = xs.clone();
        this.ys = ys.clone();
        this.zCache = null;
    }

    /**
     * Evaluates the spline at {@code x} (clamped to the control-point
     * range). The cache is built on demand so a fresh-from-Gson curve
     * doesn't need a "post-load" hook to be evaluable.
     */
    public double evaluate(double x)
    {
        if (xs == null || xs.length < 2) return x;
        int n = xs.length;
        if (x <= xs[0]) return ys[0];
        if (x >= xs[n - 1]) return ys[n - 1];

        // Locate the segment [xs[i], xs[i+1]] that contains x.
        int i = 0;
        while (i < n - 1 && xs[i + 1] < x) i++;

        double h = xs[i + 1] - xs[i];
        if (h <= 0) return ys[i]; // degenerate (shouldn't happen with sorted distinct xs)

        if (zCache == null) zCache = solveSecondDerivatives();

        // Standard natural-cubic-spline segment formula:
        //   S(x) = a*y_i + b*y_{i+1} + ((a^3-a)*z_i + (b^3-b)*z_{i+1}) * h^2 / 6
        // where a = (xs[i+1]-x)/h, b = (x-xs[i])/h.
        double b = (x - xs[i]) / h;
        double a = 1.0 - b;
        double yLin = a * ys[i] + b * ys[i + 1];
        double corr = ((a * a * a - a) * zCache[i] + (b * b * b - b) * zCache[i + 1]) * (h * h) / 6.0;
        return yLin + corr;
    }

    /**
     * Solves the tridiagonal system for the natural cubic spline's second
     * derivatives at each knot. z[0] = z[n-1] = 0 (the "natural" boundary
     * condition). Uses the Thomas algorithm on an n-by-n system reduced
     * to an (n-2)-by-(n-2) interior block.
     */
    private double[] solveSecondDerivatives()
    {
        int n = xs.length;
        double[] z = new double[n];
        if (n <= 2) return z; // straight line; second derivative is 0 everywhere

        double[] h = new double[n - 1];
        for (int i = 0; i < n - 1; i++) h[i] = xs[i + 1] - xs[i];

        double[] u = new double[n];
        double[] v = new double[n];
        u[1] = 2 * (h[0] + h[1]);
        v[1] = 6 * ((ys[2] - ys[1]) / h[1] - (ys[1] - ys[0]) / h[0]);

        for (int i = 2; i < n - 1; i++)
        {
            u[i] = 2 * (h[i - 1] + h[i]) - h[i - 1] * h[i - 1] / u[i - 1];
            v[i] = 6 * ((ys[i + 1] - ys[i]) / h[i] - (ys[i] - ys[i - 1]) / h[i - 1])
                    - h[i - 1] * v[i - 1] / u[i - 1];
        }

        z[0] = 0;
        z[n - 1] = 0;
        for (int i = n - 2; i >= 1; i--)
        {
            z[i] = (v[i] - h[i] * z[i + 1]) / u[i];
        }
        return z;
    }
}
