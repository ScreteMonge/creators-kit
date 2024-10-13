package com.creatorskit.swing.timesheet;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class KeyAction extends AbstractAction
{
    public KeyAction(String actionCommand)
    {
        putValue(ACTION_COMMAND_KEY, actionCommand);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        System.out.println(e.getActionCommand() + " pressed");
    }
}
