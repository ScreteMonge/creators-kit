package com.creatorskit.saves;

import com.creatorskit.models.CustomModelComp;
import com.creatorskit.saves.CharacterSave;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SetupSave
{
    private CustomModelComp[] comps;
    private CharacterSave[] saves;
}
