package com.creatorskit.models.datatypes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeqData
{
    private final int id;
    private final int leftHandItem;
    private final int rightHandItem;
}
