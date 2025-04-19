package com.creatorskit.swing.timesheet;

import javax.swing.*;

public class InvisibleScrollBar extends JScrollBar
{
    @Override
    public boolean isVisible()
    {
        return true;
    }
}
