package com.creatorskit.swing.timesheet.attributes;

import com.creatorskit.models.CustomModel;
import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameState;
import com.creatorskit.swing.timesheet.keyframe.ModelKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.settings.ModelToggle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class ModelAttributes extends Attributes
{
    private final JSpinner modelId = new JSpinner();
    private final JComboBox<ModelToggle> modelOverride = new JComboBox<>();
    private final JComboBox<CustomModel> customModel = new JComboBox<>();

    public ModelAttributes()
    {
        addChangeListeners();
        modelOverride.setOpaque(true);
    }

    @Override
    public void setAttributes(KeyFrame keyFrame)
    {
        ModelKeyFrame kf = (ModelKeyFrame) keyFrame;
        modelId.setValue(kf.getModelId());
        modelOverride.setSelectedItem(kf.isUseCustomModel() ? ModelToggle.CUSTOM_MODEL : ModelToggle.MODEL_ID);
        customModel.setSelectedItem(kf.getCustomModel());
    }

    @Override
    public void setBackgroundColours(Color color)
    {
        modelId.setBackground(color);
        modelOverride.setBackground(color);
        customModel.setBackground(color);
    }

    @Override
    public JComponent[] getAllComponents()
    {
        return new JComponent[]
                {
                        modelId,
                        modelOverride,
                        customModel
                };
    }

    @Override
    public void addChangeListeners()
    {
        modelId.addChangeListener(e ->
        {
            modelId.setBackground(getRed());
        });

        modelOverride.addItemListener(e ->
        {
            modelOverride.setBackground(getRed());
        });

        customModel.addItemListener(e ->
        {
            customModel.setBackground(getRed());
        });
    }

    @Override
    public void resetAttributes(boolean resetBackground)
    {
        modelId.setValue(-1);
        modelOverride.setSelectedItem(ModelToggle.CUSTOM_MODEL);
        customModel.setSelectedItem(null);
        super.resetAttributes(resetBackground);
    }
}