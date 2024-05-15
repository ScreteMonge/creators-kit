package com.creatorskit.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class AnimSequence
{
    private AnimSequenceData mainHandData;
    private AnimSequenceData offHandData;
    private int mainHandItemId;
    private int offHandItemId;
}
