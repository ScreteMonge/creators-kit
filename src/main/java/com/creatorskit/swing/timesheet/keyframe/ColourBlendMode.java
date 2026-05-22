package com.creatorskit.swing.timesheet.keyframe;

/**
 * How a {@link ColourKeyFrame}'s authored colour combines with the model's
 * original face colours during the fade-in/hold/fade-out envelope.
 *
 * <ul>
 *   <li>{@link #REPLACE} - face colour is fully lerped from original toward
 *   the target colour. At peak the model is the target colour (shading
 *   information is preserved through the underlying luminance ramp -- the
 *   renderer still shades, the hue/saturation just change).</li>
 *   <li>{@link #MULTIPLY} - luminance is scaled down toward the target
 *   colour's luminance and the hue/sat shifts toward the target. Reads
 *   as "darken / tint" -- damage-flash style.</li>
 *   <li>{@link #ADD} - default. Brightens the face by lerping its luminance
 *   toward {@code LUMINANCE_MAX} weighted by the target colour's luminance,
 *   while hue/sat shift toward the target. Reads as "hit-flash / glow."</li>
 * </ul>
 */
public enum ColourBlendMode
{
    REPLACE("Replace"),
    MULTIPLY("Multiply"),
    ADD("Add");

    private final String label;

    ColourBlendMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
