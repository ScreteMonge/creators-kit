package com.creatorskit;

import com.creatorskit.programming.ProgramComp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
@Getter
@Setter
public class CharacterSave
{
    private String name;
    private boolean locationSet;
    private WorldPoint savedLocation;
    private int compId;
    private boolean customMode;
    private boolean minimized;
    private int modelId;
    private boolean active;
    private int radius;
    private int rotation;
    private int animationId;
    private ProgramComp programComp;
}
