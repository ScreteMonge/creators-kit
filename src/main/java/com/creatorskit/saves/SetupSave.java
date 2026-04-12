package com.creatorskit.saves;

import com.creatorskit.models.CKModel;
import com.creatorskit.models.CKModelComposition;
import com.creatorskit.models.CustomModelComp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SetupSave
{
    private String version;
    private CKModel[] ckModels;
    @Deprecated
    private CustomModelComp[] comps;
    private FolderNodeSave masterFolderNode;
    private CharacterSave[] saves;
}
