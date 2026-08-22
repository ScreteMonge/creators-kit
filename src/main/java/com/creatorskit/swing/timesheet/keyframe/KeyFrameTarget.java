package com.creatorskit.swing.timesheet.keyframe;

import lombok.Data;

import javax.annotation.Nullable;
import java.util.Objects;

@Data
public class KeyFrameTarget
{
    private final KeyFrameCategory type;
    private final Object value;

    public KeyFrameTarget(KeyFrameCategory type, @Nullable Object value)
    {
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof KeyFrameTarget))
            return false;

        KeyFrameTarget other = (KeyFrameTarget) o;

        return type == other.type
                && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, value);
    }
}
