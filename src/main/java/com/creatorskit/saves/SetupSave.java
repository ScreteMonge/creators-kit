package com.creatorskit.saves;

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
    private CustomModelComp[] comps;
    private FolderNodeSave masterFolderNode;
    private CharacterSave[] saves;
    /**
     * Added Phase 2 (camera integration follow-up): central store for Camera /
     * Screen Fade / Screen Shake keyframes. Null in pre-Phase-2 saves; the
     * load path treats null as "no central store yet" and migrates each
     * CharacterSave's per-Character global fields into a new GlobalKeyFrames
     * instance. After migration these CharacterSave fields stay populated
     * (read-only on subsequent loads) so downgraded plugin versions can still
     * round-trip the file.
     */
    private GlobalKeyFrames globalKeyFrames;
    /**
     * Timeline A / B loop marker ticks (nullable). Timeline-global, not
     * per-Character, so they live here. Null in saves predating this field --
     * the load path leaves the markers unset in that case. Added at the end so
     * Lombok's @AllArgsConstructor appends them as the final positional args.
     */
    private Double aLoopTick;
    private Double bLoopTick;
}
