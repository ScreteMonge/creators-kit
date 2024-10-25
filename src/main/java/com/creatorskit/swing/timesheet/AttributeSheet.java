package com.creatorskit.swing.timesheet;

import com.creatorskit.swing.ToolBoxFrame;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttributeSheet extends TimeSheet
{
    private ToolBoxFrame toolBox;

    public AttributeSheet(ToolBoxFrame toolBox)
    {
        super(toolBox);
        setDrawMainFrames(false);
    }
}
