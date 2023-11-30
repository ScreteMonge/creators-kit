package com.creatorskit.saves;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class FolderNodeSave
{
    boolean masterFolder;
    String name;
    CharacterSave[] characterSaves;
    FolderNodeSave[] folderSaves;
}
