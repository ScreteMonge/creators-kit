package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CharacterSave;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.CustomModel;
import com.creatorskit.models.CustomModelComp;
import com.creatorskit.models.ModelFinder;
import com.creatorskit.models.ModelGetter;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class ModelOrganizer extends JFrame
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
    private final HashMap<CustomModel, JPanel> panelMap = new HashMap<>();
    JPanel modelPane = new JPanel();
    GridBagConstraints c = new GridBagConstraints();

    @Inject
    public ModelOrganizer(Client client, CreatorsPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());
        setTitle("Creator's Kit Model Organizer");
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

        modelPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        modelPane.setLayout(new GridLayout(0, 10, 8, 8));

        c.fill = GridBagConstraints.NONE;
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_END;
        JButton clearButton = new JButton("Clear Unused Models");
        clearButton.setFocusable(false);
        clearButton.setToolTipText("Clears all unused models from Custom Model dropdown menus");
        add(clearButton, c);
        clearButton.addActionListener(e ->
        {
            ArrayList<CustomModel> unusedModels = new ArrayList<>();

            for (int i = 0; i < plugin.getStoredModels().size(); i++)
            {
                CustomModel customModel = plugin.getStoredModels().get(i);

                boolean isBeingUsed = false;
                for (Character character : plugin.getCharacters())
                {
                    if (character.getStoredModel() == customModel)
                    {
                        isBeingUsed = true;
                        break;
                    }
                }

                if (!isBeingUsed)
                    unusedModels.add(customModel);
            }

            for (CustomModel customModel : unusedModels)
                plugin.removeCustomModel(customModel);
        });

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.anchor = GridBagConstraints.PAGE_START;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(modelPane);
        add(scrollPane, c);

        revalidate();
        pack();
    }

    public void createModelPanel(CustomModel model)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 2, 2));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        modelPane.add(panel);

        JTextField textField = new JTextField();
        textField.setText(model.getComp().getName());
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(textField);
        panelMap.put(model, panel);
        textField.addActionListener(e ->
        {
            model.getComp().setName(textField.getText());
            plugin.updatePanelComboBoxes();
        });

        JButton deleteButton = new JButton("Delete");
        deleteButton.setFocusable(false);
        deleteButton.setToolTipText("Remove this model from all Objects and dropdown menus");
        deleteButton.addActionListener(e ->
                plugin.removeCustomModel(model));
        panel.add(deleteButton);

        JButton anvilButton = new JButton("To Anvil");
        anvilButton.setFocusable(false);
        anvilButton.setToolTipText("Send this model to the Anvil");
        panel.add(anvilButton);
        anvilButton.addActionListener(e ->
                plugin.customModelToAnvil(model));
        revalidate();
        repaint();
    }

    public void removeModelPanel(CustomModel model)
    {
        JPanel panel = panelMap.get(model);
        modelPane.remove(panel);
        modelPane.updateUI();
        panelMap.remove(model);
        revalidate();
        repaint();
    }
}
