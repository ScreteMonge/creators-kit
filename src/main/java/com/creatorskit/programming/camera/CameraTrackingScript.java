package com.creatorskit.programming.camera;

import com.creatorskit.Character;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CameraTrackingScript extends CameraScript
{
    private Character character;

    public CameraTrackingScript(CameraMotionType type, EaseType ease, double pitch, double yaw, int scale, Character character)
    {
        super(type, ease, pitch, yaw, scale);
        this.character = character;
    }
}
