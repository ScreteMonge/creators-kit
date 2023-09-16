package com.creatorskit;

import com.creatorskit.models.CustomModelComp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SaveFile
{
    private CustomModelComp[] comps;
    private CharacterSave[] saves;
}
