package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.CustomModel;
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
import java.util.HashMap;

public class ModelOrganizer extends JFrame
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private final HashMap<CustomModel, JPanel> panelMap = new HashMap<>();
    JPanel modelFrame = new JPanel();
    GridBagConstraints c = new GridBagConstraints();

    @Inject
    public ModelOrganizer(Client client, CreatorsPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setTitle("RuneLite Model Organizer");
        setIconImage(icon);
        setPreferredSize(new Dimension(1100, 200));

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;

        c.gridx = 0;
        c.gridy = 0;
        JLabel organizerLabel = new JLabel("Model Organizer");
        organizerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        organizerLabel.setFont(FontManager.getRunescapeBoldFont());
        add(organizerLabel, c);

        modelFrame.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        modelFrame.setLayout(new GridLayout(0, 10, 8, 8));

        c.weighty = 1;
        c.anchor = GridBagConstraints.PAGE_START;
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(modelFrame);
        add(scrollPane, c);

        System.out.println("MO created");
        revalidate();
        pack();
    }

    public void createModelPanel(CustomModel model)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 0, 2, 2));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        modelFrame.add(panel);

        JTextField textField = new JTextField();
        textField.setText(model.toString());
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(textField);
        panelMap.put(model, panel);
        textField.addActionListener(e ->
        {
            model.setString(textField.getText());
            plugin.updatePanelComboBoxes();
        });

        JButton saveButton = new JButton("Save");
        saveButton.setFocusable(false);
        panel.add(saveButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.setFocusable(false);
        deleteButton.addActionListener(e ->
        {
            plugin.removeCustomModel(model);
        });
        panel.add(deleteButton);

        revalidate();
        repaint();
    }

    public void removeModelPanel(CustomModel model)
    {
        JPanel panel = panelMap.get(model);
        modelFrame.remove(panel);
        modelFrame.updateUI();
        panelMap.remove(model);
        revalidate();
        repaint();
    }
}
