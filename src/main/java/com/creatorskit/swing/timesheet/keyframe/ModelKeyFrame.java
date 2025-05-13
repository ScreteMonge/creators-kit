package com.creatorskit.swing.timesheet.keyframe;


import com.creatorskit.models.CustomModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelKeyFrame extends KeyFrame
{
    private boolean useCustomModel;
    private int modelId;
    private CustomModel customModel;
    private int radius;

    public ModelKeyFrame(double tick, boolean useCustomModel, int modelId, CustomModel customModel, int radius)
    {
        super(KeyFrameType.MODEL, tick);
        this.useCustomModel = useCustomModel;
        this.modelId = modelId;
        this.customModel = customModel;
        this.radius = radius;
    }
}
