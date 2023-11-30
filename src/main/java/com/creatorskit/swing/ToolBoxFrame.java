package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.jtree.FolderTree;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Getter
public class ToolBoxFrame extends JFrame
{
    private ClientThread clientThread;
    private final Client client;
    private final CreatorsPlugin plugin;
    private final ManagerPanel managerPanel;
    private final ModelOrganizer modelOrganizer;
    private final ModelAnvil modelAnvil;
    private final ProgrammerPanel programPanel;
    private final TransmogPanel transmogPanel;
    private final BufferedImage ICON = ImageUtil.loadImageResource(getClass(), "/panelicon.png");

    @Inject
    public ToolBoxFrame(Client client, ClientThread clientThread, CreatorsPlugin plugin, ManagerPanel managerPanel, ModelOrganizer modelOrganizer, ModelAnvil modelAnvil, ProgrammerPanel programPanel, TransmogPanel transmogPanel)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.managerPanel = managerPanel;
        this.modelOrganizer = modelOrganizer;
        this.modelAnvil = modelAnvil;
        this.programPanel = programPanel;
        this.transmogPanel = transmogPanel;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setTitle("Creator's Kit Toolbox");
        setIconImage(ICON);
        setPreferredSize(new Dimension(1500, 800));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FontManager.getRunescapeBoldFont());
        tabbedPane.addTab("Manager", managerPanel);
        tabbedPane.addTab("Model Organizer", modelOrganizer);
        tabbedPane.addTab("Model Anvil", modelAnvil);
        tabbedPane.addTab("Programmer", programPanel);
        tabbedPane.addTab("Transmogger", transmogPanel);
        tabbedPane.setToolTipTextAt(0, "Manage and organize all your Objects");
        tabbedPane.setToolTipTextAt(1, "Organize Custom Models you've loaded from the cache or Forged");
        tabbedPane.setToolTipTextAt(2, "Create Custom Models by modifying and merging different models together");
        tabbedPane.setToolTipTextAt(3, "Change your Object Programs' animations, speeds, and more");
        tabbedPane.setToolTipTextAt(4, "Set animations for Transmogging your player character");

        //Move the FolderTree between the Manager and Programmer tabs when the given tab is selected
        tabbedPane.addChangeListener(e -> {
            if (e.getSource() instanceof JTabbedPane)
            {
                JTabbedPane jTabbedPane = (JTabbedPane) e.getSource();
                if (jTabbedPane.getSelectedComponent() == managerPanel)
                {
                    FolderTree folderTree = managerPanel.getFolderTree();
                    GridBagConstraints c = managerPanel.getC();
                    c.fill = GridBagConstraints.BOTH;
                    c.gridx = 0;
                    c.gridy = 2;
                    c.weightx = 0;
                    c.weighty = 1;
                    c.gridwidth = 1;
                    c.gridheight = 2;
                    managerPanel.add(folderTree, c);
                    setHeaderButtonsVisible(true);
                }

                if (jTabbedPane.getSelectedComponent() == programPanel)
                {
                    FolderTree folderTree = managerPanel.getFolderTree();
                    GridBagConstraints c = programPanel.getC();
                    c.fill = GridBagConstraints.BOTH;
                    c.gridx = 0;
                    c.gridy = 1;
                    c.weightx = 0;
                    c.weighty = 1;
                    c.gridwidth = 1;
                    c.gridheight = 1;
                    programPanel.add(folderTree, c);
                    setHeaderButtonsVisible(false);
                }
            }
            repaint();
            revalidate();
        });

        add(tabbedPane);
        pack();
        revalidate();
    }

    private void setHeaderButtonsVisible(boolean setVisible)
    {
        JButton[] headerButtons = managerPanel.getFolderTree().getHeaderButtons();
        for (JButton button : headerButtons)
            button.setVisible(setVisible);
    }
}
