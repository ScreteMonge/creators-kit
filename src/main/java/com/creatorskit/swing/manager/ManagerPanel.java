package com.creatorskit.swing.manager;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.swing.ParentPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;

@Slf4j
@Getter
public class ManagerPanel extends JPanel
{
    private final CreatorsPlugin plugin;
    private final Client client;
    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel objectHolder;
    private final JPanel scrollPanel = new JPanel();
    private final ArrayList<Character> managerCharacters = new ArrayList<>();
    private final ManagerTree managerTree;
    private final JLabel objectLabel = new JLabel("Current Folder: Master Folder");

    @Inject
    public ManagerPanel(@Nullable Client client, CreatorsPlugin plugin, JPanel objectHolder, ManagerTree managerTree)
    {
        this.plugin = plugin;
        this.client = client;
        this.objectHolder = objectHolder;
        this.managerTree = managerTree;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
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
        c.weightx = 1;
        c.weighty = 1;
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setHgap(4);
        scrollPanel.setLayout(borderLayout);
        add(scrollPanel, c);

        scrollPanel.add(managerTree, BorderLayout.LINE_START);

        JScrollPane objectScrollPane = new JScrollPane();
        objectScrollPane.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR, 1));
        scrollPanel.add(objectScrollPane, BorderLayout.CENTER);

        JPanel objectHeader = new JPanel();
        objectHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);
        objectHeader.setLayout(new BorderLayout());
        objectScrollPane.setColumnHeaderView(objectHeader);

        objectLabel.setFont(FontManager.getRunescapeBoldFont());
        objectHeader.add(objectLabel, BorderLayout.LINE_START);

        JPanel rightButtons = new JPanel();
        objectHeader.add(rightButtons, BorderLayout.LINE_END);

        JButton addObjectButton = new JButton("Add Object");
        addObjectButton.setToolTipText("Add an new Object to the palette");
        addObjectButton.setFocusable(false);
        addObjectButton.setPreferredSize(new Dimension(150, 30));
        addObjectButton.addActionListener(e ->
        {
            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
            TreePath path = managerTree.getTree().getSelectionPath();

            if (path == null)
            {
                Character character = creatorsPanel.createCharacter(ParentPanel.MANAGER);
                creatorsPanel.addPanel(ParentPanel.MANAGER, character);
                return;
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            ParentPanel parentPanel = managerTree.treeContainsSidePanel(node) ? ParentPanel.SIDE_PANEL : ParentPanel.MANAGER;
            Character character = creatorsPanel.createCharacter(parentPanel);
            creatorsPanel.addPanel(parentPanel, character);
        });
        rightButtons.add(addObjectButton);

        JPanel viewport = new JPanel();
        viewport.setLayout(new GridBagLayout());
        viewport.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        this.objectHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        viewport.add(this.objectHolder, c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        viewport.add(new JLabel(""), c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 1;
        viewport.add(new JLabel(""), c);

        this.objectHolder.setLayout(new GridLayout(0, 5, 4, 4));
        objectScrollPane.setViewportView(viewport);

        repaint();
        revalidate();
    }

    public Character[] getShownCharacters()
    {
        return managerTree.getShownCharacters();
    }
}
