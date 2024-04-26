package com.creatorskit.swing.timesheet;

import com.creatorskit.CreatorsPlugin;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class TimeSheetPanel extends JPanel
{
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final GridBagConstraints c = new GridBagConstraints();
    @Getter
    private TimeSheet timeSheet;
    @Getter
    private TimeTree timeTree;

    @Inject
    public TimeSheetPanel(@Nullable Client client, CreatorsPlugin plugin, ClientThread clientThread)
    {
        this.plugin = plugin;
        this.clientThread = clientThread;

        setLayout(new GridBagLayout());
        setupManager();
    }

    private void setupManager()
    {
        timeSheet = new TimeSheet();
        timeTree = new TimeTree(this, timeSheet);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel label = new JLabel("Timesheet");
        add(label, c);

        c.weightx = 0;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        add(timeTree, c);

        c.weightx = 10;
        c.weighty = 10;
        c.gridx = 1;
        c.gridy = 1;
        add(timeSheet, c);
    }

    public void addFolder(DefaultMutableTreeNode node)
    {
    }

    public void removeFolder(DefaultMutableTreeNode node)
    {
    }

    public void addCharacter(TimeSheetComp timeSheetComp)
    {
        add(timeSheetComp.getPanel());
    }

    public void removeCharacter(TimeSheetComp timeSheetComp)
    {
        remove(timeSheetComp.getPanel());
    }

}
