package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

@Slf4j
@Getter
public class ManagerPanel extends JPanel
{
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final Client client;
    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel objectHolder = new JPanel();
    private final ArrayList<JPanel> managerPanels = new ArrayList<>();
    private final FolderTree folderTree = new FolderTree();
    private JPanel[] folders = new JPanel[0];
    private final JLabel objectLabel = new JLabel("Current Folder:");

    @Inject
    public ManagerPanel(@Nullable Client client, ClientThread clientThread, CreatorsPlugin plugin)
    {
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.client = client;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 2;
        JLabel titleLabel = new JLabel("Manager");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        JPanel headerPanel = new JPanel();
        add(headerPanel, c);

        JButton saveButton = new JButton("Save Setup");
        saveButton.setToolTipText("Save this setup for future use");
        headerPanel.add(saveButton);

        JButton loadButton = new JButton("Load Setup");
        loadButton.setToolTipText("Load a previously saved setup");
        headerPanel.add(loadButton);

        JButton clearButton = new JButton("Clear Setup");
        clearButton.setToolTipText("Clear all Folders and Objects from the Manager");
        headerPanel.add(clearButton);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.15;
        c.weighty = 1;
        c.gridwidth = 1;
        add(folderTree, c);

        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 0.85;
        c.weighty = 1;
        c.gridwidth = 1;
        JScrollPane objectScrollPane = new JScrollPane();
        add(objectScrollPane, c);

        JPanel objectHeader = new JPanel();
        objectHeader.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        objectScrollPane.setColumnHeaderView(objectHeader);

        objectLabel.setFont(FontManager.getRunescapeBoldFont());
        objectHeader.add(objectLabel);

        JButton addObjectButton = new JButton("Add Object");
        addObjectButton.setText("Add Object");
        addObjectButton.setToolTipText("Add an new Object to the palette");
        addObjectButton.setFocusable(false);
        addObjectButton.addActionListener(e ->
        {
            //JPanel panel = plugin.getCreatorsPanel().createPanel(managerPanels, objectHolder);
            //addPanel(panel);
        });
        objectHeader.add(addObjectButton);

        objectHolder.setBackground(Color.BLACK);
        objectHolder.setLayout(new GridLayout(0, 3, 5, 5));
        objectScrollPane.setViewportView(objectHolder);

        repaint();
        revalidate();
    }

    public void addPanel(JPanel panel)
    {
        objectHolder.add(panel);
        repaint();
        revalidate();
    }
}
