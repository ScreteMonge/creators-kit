package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.jtree.FolderTree;
import com.creatorskit.swing.timesheet.TimeTree;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

@Slf4j
@Getter
public class ManagerPanel extends JPanel
{
    private final CreatorsPlugin plugin;
    private final Client client;
    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel objectHolder = new JPanel();
    private final ArrayList<Character> managerCharacters = new ArrayList<>();
    private final FolderTree folderTree;
    private final BufferedImage SWITCH_ALL = ImageUtil.loadImageResource(getClass(), "/Switch All.png");
    private final JLabel objectLabel = new JLabel("Current Folder: Master Folder");

    @Inject
    public ManagerPanel(@Nullable Client client, CreatorsPlugin plugin)
    {
        this.plugin = plugin;
        this.client = client;
        this.folderTree = new FolderTree(this, plugin);

        setBackground(ColorScheme.DARK_GRAY_COLOR);
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
        saveButton.addActionListener(e -> plugin.getCreatorsPanel().openSaveDialog());

        JButton loadButton = new JButton("Load Setup");
        loadButton.setToolTipText("Load a previously saved setup");
        headerPanel.add(loadButton);
        loadButton.addActionListener(e -> plugin.getCreatorsPanel().openLoadSetupDialog());

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.weighty = 1;
        c.gridwidth = 1;
        c.gridheight = 2;
        add(folderTree, c);

        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 0.8;
        c.weighty = 1;
        c.gridwidth = 1;
        c.gridheight = 2;
        JScrollPane objectScrollPane = new JScrollPane();
        objectScrollPane.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        add(objectScrollPane, c);

        JPanel objectHeader = new JPanel();
        objectHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);
        objectHeader.setLayout(new GridBagLayout());
        objectScrollPane.setColumnHeaderView(objectHeader);

        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        objectLabel.setFont(FontManager.getRunescapeBoldFont());
        objectHeader.add(objectLabel, c);

        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        JButton addObjectButton = new JButton("Add Object");
        addObjectButton.setToolTipText("Add an new Object to the palette");
        addObjectButton.setFocusable(false);
        addObjectButton.setPreferredSize(new Dimension(150, 30));
        addObjectButton.addActionListener(e ->
        {
            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
            Character character = creatorsPanel.createCharacter(objectHolder);
            creatorsPanel.addPanel(managerCharacters, objectHolder, character);
        });
        objectHeader.add(addObjectButton, c);

        c.gridx = 2;
        c.gridy = 0;
        JButton switchPanelsButton = new JButton(new ImageIcon(SWITCH_ALL));
        switchPanelsButton.setToolTipText("Send all shown Objects to the Side Panel");
        switchPanelsButton.setFocusable(false);
        switchPanelsButton.addActionListener(e ->
        {
            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
            creatorsPanel.switchPanels(objectHolder, getShownCharacters());
        });
        objectHeader.add(switchPanelsButton, c);

        objectHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        objectHolder.setLayout(new GridBagLayout());
        objectScrollPane.setViewportView(objectHolder);

        repaint();
        revalidate();
    }

    public Character[] getShownCharacters()
    {
        return folderTree.getShownCharacters();
    }
}
