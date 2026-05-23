package com.creatorskit.swing.timesheet.keyframe;

/**
 * Controls which way an {@link OrientationKeyFrame} rotates between its
 * (snapshotted) start angle and its end angle.
 *
 * <ul>
 *   <li>{@link #AUTO} - take the shortest path (legacy behaviour). Uses
 *   the sign of {@code Orientation.subtract(end, start)}.</li>
 *   <li>{@link #CLOCKWISE} - force a CW rotation (increasing jagex angle
 *   under the compass-image convention 0=S, 512=W, 1024=N, 1536=E;
 *   S→W→N→E is bottom→left→top→right, which is clockwise from above).
 *   If the short path is already CW this matches AUTO; otherwise it
 *   takes the long way around.</li>
 *   <li>{@link #COUNTER_CLOCKWISE} - force a CCW rotation (decreasing
 *   jagex angle). Same long-way fallback as CLOCKWISE when the short
 *   path is the other direction.</li>
 * </ul>
 */
public enum TurnDirection
{
    AUTO("Auto (shortest)"),
    CLOCKWISE("Clockwise"),
    COUNTER_CLOCKWISE("Counter-clockwise");

    private final String label;

    TurnDirection(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
