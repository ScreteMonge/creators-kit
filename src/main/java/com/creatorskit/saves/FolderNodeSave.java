package com.creatorskit.saves;

import com.creatorskit.swing.manager.FolderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class FolderNodeSave
{
    private FolderType folderType;
    private String name;
    private CharacterSave[] characterSaves;
    private FolderNodeSave[] folderSaves;
}
