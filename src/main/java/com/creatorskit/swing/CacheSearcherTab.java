package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.*;
import com.creatorskit.models.datatypes.ItemData;
import com.creatorskit.models.datatypes.NPCData;
import com.creatorskit.models.datatypes.ObjectData;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import net.runelite.api.Model;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.List;

public class CacheSearcherTab extends JPanel
{
    private final CreatorsPlugin plugin;
    private ClientThread clientThread;
    private final DataFinder dataFinder;

    private final GridBagConstraints c = new GridBagConstraints();
    private final String NAME = "NAME";
    private final String ID = "ID";
    private final Color ENTRY_COLOUR = new Color(35, 35, 40);

    private final JPanel npcPanel = new JPanel();
    private final JPanel objectPanel = new JPanel();
    private final JPanel itemPanel = new JPanel();

    private boolean npcsFound = false;
    private boolean objectsFound = false;
    private boolean itemsFound = false;

    @Inject
    public CacheSearcherTab(CreatorsPlugin plugin, ClientThread clientThread, DataFinder dataFinder)
    {
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.dataFinder = dataFinder;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setLayout(new GridBagLayout());

        setupNPCPanel();
        setupObjectPanel();
        setupItemPanel();
        setupLayout();
    }

    private void setupLayout()
    {
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Cache Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        add(title, c);

        c.gridx = 0;
        c.gridy = 1;
        add(npcPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        add(objectPanel, c);

        c.gridx = 0;
        c.gridy = 3;
        add(itemPanel, c);

        repaint();
        revalidate();
    }

    private void setupNPCPanel()
    {
        npcPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        npcPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        npcPanel.add(holderPanel);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("NPC Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel modeLabel = new JLabel("Search by: ");
        modeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(modeLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JComboBox<String> mode = new JComboBox<>();
        mode.setToolTipText("Set whether to search the cache via the NPC's name or NPC Id");
        mode.addItem("Name");
        mode.addItem("Id");
        holderPanel.add(mode, c);

        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 1;
        JLabel buffer = new JLabel("               ");
        holderPanel.add(buffer, c);

        c.gridx = 3;
        c.gridy = 1;
        JButton addObject = new JButton("Add Object w/Model");
        addObject.setToolTipText("Adds the selected NPC as a new Object and Custom Model, applying the Model to the new Object");
        holderPanel.add(addObject, c);

        c.gridx = 3;
        c.gridy = 2;
        JButton addModel = new JButton("Add Model");
        addModel.setToolTipText("Adds the selected NPC as a new Custom Model");
        holderPanel.add(addModel, c);

        c.gridx = 3;
        c.gridy = 3;
        JButton addAnvil = new JButton("Add to Anvil");
        addAnvil.setToolTipText("Sends the NPC's models to the Model Anvil");
        holderPanel.add(addAnvil, c);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 2;
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout());
        holderPanel.add(cardPanel, c);

        JPanel nameCard = new JPanel();
        JPanel idCard = new JPanel();
        cardPanel.add(nameCard, "NAME");
        cardPanel.add(idCard, "ID");

        JLabel nameLabel = new JLabel("NPC name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nameCard.add(nameLabel);

        JComboBox<NPCData> nameBox = new JComboBox<>();
        nameBox.setBackground(ENTRY_COLOUR);
        nameBox.setPreferredSize(new Dimension(270, 25));
        nameBox.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
                if (!npcsFound)
                {
                    List<NPCData> dataList = dataFinder.getNpcData();
                    npcsFound = true;
                    for (NPCData data : dataList)
                    {
                        nameBox.addItem(data);
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        AutoCompletion.enable(nameBox);
        nameCard.add(nameBox);

        JLabel idLabel = new JLabel("NPC Id: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        idCard.add(idLabel);

        JSpinner idSpinner = new JSpinner();
        idSpinner.setBackground(ENTRY_COLOUR);
        idSpinner.setPreferredSize(new Dimension(90, 25));
        idCard.add(idSpinner);

        mode.addItemListener(e ->
        {
            if (e.getItem() == null)
            {
                return;
            }

            String item = (String) e.getItem();
            CardLayout cl = (CardLayout)(cardPanel.getLayout());

            if (item.equalsIgnoreCase(NAME))
            {
                cl.show(cardPanel, NAME);
                return;
            }

            cl.show(cardPanel, ID);
        });

        addObject.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            if (modeString.equals("Name"))
            {
                NPCData npcData = (NPCData) nameBox.getSelectedItem();
                if (npcData == null)
                {
                    return;
                }

                addNPCObject((NPCData) nameBox.getSelectedItem());
            }
            else
            {
                addNPCObject((int) idSpinner.getValue());
            }
        });

        addModel.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            if (modeString.equals("Name"))
            {
                NPCData npcData = (NPCData) nameBox.getSelectedItem();
                if (npcData == null)
                {
                    return;
                }

                addCustomModel(CustomModelType.CACHE_NPC, npcData.getId());
            }
            else
            {
                addCustomModel(CustomModelType.CACHE_NPC, (int) idSpinner.getValue());
            }
        });

        addAnvil.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            if (modeString.equals("Name"))
            {
                NPCData npcData = (NPCData) nameBox.getSelectedItem();
                if (npcData == null)
                {
                    return;
                }

                addToAnvil(CustomModelType.CACHE_NPC, npcData.getId());
            }
            else
            {
                addToAnvil(CustomModelType.CACHE_NPC, (int) idSpinner.getValue());
            }

        });
    }

    private void setupObjectPanel()
    {
        objectPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        objectPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        objectPanel.add(holderPanel);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Object Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel modeLabel = new JLabel("Search by: ");
        modeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(modeLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JComboBox<String> mode = new JComboBox<>();
        mode.setToolTipText("Set whether to search the cache via the Object's name or Object Id");
        mode.addItem("Name");
        mode.addItem("Id");
        holderPanel.add(mode, c);

        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 1;
        JLabel buffer = new JLabel("               ");
        holderPanel.add(buffer, c);

        c.gridx = 3;
        c.gridy = 1;
        JButton addObject = new JButton("Add Object w/Model");
        addObject.setToolTipText("Adds the selected Object as a new Object and Custom Model, applying the Model to the new Object");
        holderPanel.add(addObject, c);

        c.gridx = 3;
        c.gridy = 2;
        JButton addModel = new JButton("Add Model");
        addModel.setToolTipText("Adds the selected Object as a new Custom Model");
        holderPanel.add(addModel, c);

        c.gridx = 3;
        c.gridy = 3;
        JButton addAnvil = new JButton("Add to Anvil");
        addAnvil.setToolTipText("Sends the Object's models to the Model Anvil");
        holderPanel.add(addAnvil, c);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 2;
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout());
        holderPanel.add(cardPanel, c);

        JPanel nameCard = new JPanel();
        JPanel idCard = new JPanel();
        cardPanel.add(nameCard, "NAME");
        cardPanel.add(idCard, "ID");

        JLabel nameLabel = new JLabel("Object name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nameCard.add(nameLabel);

        JComboBox<ObjectData> nameBox = new JComboBox<>();
        nameBox.setBackground(ENTRY_COLOUR);
        nameBox.setPreferredSize(new Dimension(270, 25));
        nameBox.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
                if (!objectsFound)
                {
                    List<ObjectData> dataList = dataFinder.getObjectData();
                    objectsFound = true;
                    for (ObjectData data : dataList)
                    {
                        String name = data.getName();
                        if (name.isEmpty()) {
                            continue;
                        }

                        if (name.equals("null")) {
                            continue;
                        }

                        nameBox.addItem(data);
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        AutoCompletion.enable(nameBox);
        nameCard.add(nameBox);

        JLabel idLabel = new JLabel("Object Id: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        idCard.add(idLabel);

        JSpinner idSpinner = new JSpinner();
        idSpinner.setBackground(ENTRY_COLOUR);
        idSpinner.setPreferredSize(new Dimension(90, 25));
        idCard.add(idSpinner);

        mode.addItemListener(e ->
        {
            if (e.getItem() == null)
            {
                return;
            }

            String item = (String) e.getItem();
            CardLayout cl = (CardLayout)(cardPanel.getLayout());

            if (item.equalsIgnoreCase(NAME))
            {
                cl.show(cardPanel, NAME);
                return;
            }

            cl.show(cardPanel, ID);
        });

        addObject.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            if (modeString.equals("Name"))
            {
                ObjectData objectData = (ObjectData) nameBox.getSelectedItem();
                if (objectData == null)
                {
                    return;
                }

                addObjectObject((ObjectData) nameBox.getSelectedItem());
            }
            else
            {
                addObjectObject((int) idSpinner.getValue());
            }
        });

        addModel.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            if (modeString.equals("Name"))
            {
                ObjectData objectData = (ObjectData) nameBox.getSelectedItem();
                if (objectData == null)
                {
                    return;
                }

                addCustomModel(CustomModelType.CACHE_OBJECT, objectData.getId());
            }
            else
            {
                addCustomModel(CustomModelType.CACHE_OBJECT, (int) idSpinner.getValue());
            }
        });

        addAnvil.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            if (modeString.equals("Name"))
            {
                ObjectData objectData = (ObjectData) nameBox.getSelectedItem();
                if (objectData == null)
                {
                    return;
                }

                addToAnvil(CustomModelType.CACHE_OBJECT, objectData.getId());
            }
            else
            {
                addToAnvil(CustomModelType.CACHE_OBJECT, (int) idSpinner.getValue());
            }

        });
    }

    private void setupItemPanel()
    {
        itemPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        itemPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        itemPanel.add(holderPanel);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Item Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel modeLabel = new JLabel("Search by: ");
        modeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(modeLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JComboBox<String> mode = new JComboBox<>();
        mode.setToolTipText("Set whether to search the cache via the Item's name or Item Id");
        mode.addItem("Name");
        mode.addItem("Id");
        holderPanel.add(mode, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel modelTypeLabel = new JLabel("Model type: ");
        modelTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(modelTypeLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JComboBox<CustomModelType> modelTypeBox = new JComboBox<>();
        modelTypeBox.setToolTipText("Set whether to search the cache for the Item's Ground model, Male worn model, or Female worn model");
        modelTypeBox.addItem(CustomModelType.CACHE_GROUND_ITEM);
        modelTypeBox.addItem(CustomModelType.CACHE_MAN_WEAR);
        modelTypeBox.addItem(CustomModelType.CACHE_WOMAN_WEAR);
        holderPanel.add(modelTypeBox, c);

        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 1;
        JLabel buffer = new JLabel("               ");
        holderPanel.add(buffer, c);

        c.gridx = 3;
        c.gridy = 1;
        JButton addObject = new JButton("Add Object w/Model");
        addObject.setToolTipText("Adds the selected Item as a new Object and Custom Model, applying the Model to the new Object");
        holderPanel.add(addObject, c);

        c.gridx = 3;
        c.gridy = 2;
        JButton addModel = new JButton("Add Model");
        addModel.setToolTipText("Adds the selected Item as a new Custom Model");
        holderPanel.add(addModel, c);

        c.gridx = 3;
        c.gridy = 3;
        JButton addAnvil = new JButton("Add to Anvil");
        addAnvil.setToolTipText("Sends the Item's models to the Model Anvil");
        holderPanel.add(addAnvil, c);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 3;
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout());
        holderPanel.add(cardPanel, c);

        JPanel nameCard = new JPanel();
        JPanel idCard = new JPanel();
        cardPanel.add(nameCard, "NAME");
        cardPanel.add(idCard, "ID");

        JLabel nameLabel = new JLabel("Item name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nameCard.add(nameLabel);

        JComboBox<ItemData> nameBox = new JComboBox<>();
        nameBox.setBackground(ENTRY_COLOUR);
        nameBox.setPreferredSize(new Dimension(270, 25));
        nameBox.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
                if (!itemsFound)
                {
                    List<ItemData> dataList = dataFinder.getItemData();
                    itemsFound = true;
                    for (ItemData data : dataList)
                    {
                        String name = data.getName();
                        if (name.isEmpty())
                        {
                            continue;
                        }

                        if (name.equals("null"))
                        {
                            continue;
                        }

                        nameBox.addItem(data);
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        AutoCompletion.enable(nameBox);
        nameCard.add(nameBox);

        JLabel idLabel = new JLabel("Item Id: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        idCard.add(idLabel);

        JSpinner idSpinner = new JSpinner();
        idSpinner.setBackground(ENTRY_COLOUR);
        idSpinner.setPreferredSize(new Dimension(90, 25));
        idCard.add(idSpinner);

        mode.addItemListener(e ->
        {
            if (e.getItem() == null)
            {
                return;
            }

            String item = (String) e.getItem();
            CardLayout cl = (CardLayout)(cardPanel.getLayout());

            if (item.equalsIgnoreCase(NAME))
            {
                cl.show(cardPanel, NAME);
                return;
            }

            cl.show(cardPanel, ID);
        });

        addObject.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            CustomModelType customModelType = (CustomModelType) modelTypeBox.getSelectedItem();

            if (modeString.equals("Name"))
            {
                ItemData itemData = (ItemData) nameBox.getSelectedItem();
                if (itemData == null)
                {
                    return;
                }

                addItemObject((ItemData) nameBox.getSelectedItem(), customModelType);
            }
            else
            {
                addItemObject((int) idSpinner.getValue(), customModelType);
            }
        });

        addModel.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            CustomModelType customModelType = (CustomModelType) modelTypeBox.getSelectedItem();

            if (modeString.equals("Name"))
            {
                ItemData itemData = (ItemData) nameBox.getSelectedItem();
                if (itemData == null)
                {
                    return;
                }

                addCustomModel(customModelType, itemData.getId());
            }
            else
            {
                addCustomModel(customModelType, (int) idSpinner.getValue());
            }
        });

        addAnvil.addActionListener(e ->
        {
            String modeString = (String) mode.getSelectedItem();
            if (modeString == null)
            {
                return;
            }

            CustomModelType customModelType = (CustomModelType) modelTypeBox.getSelectedItem();

            if (modeString.equals("Name"))
            {
                ItemData itemData = (ItemData) nameBox.getSelectedItem();
                if (itemData == null)
                {
                    return;
                }

                addToAnvil(customModelType, itemData.getId());
            }
            else
            {
                addToAnvil(customModelType, (int) idSpinner.getValue());
            }

        });
    }

    private void addToAnvil(CustomModelType type, int id)
    {
        plugin.cacheToAnvil(type, id);
    }

    private void addCustomModel(CustomModelType type, int id)
    {
        plugin.cacheToCustomModel(type, id, -1);
    }

    private void addItemObject(int id, CustomModelType customModelType)
    {
        List<ItemData> data = dataFinder.getItemData();
        for (ItemData n : data)
        {
            if (n.getId() == id)
            {
                addItemObject(n, customModelType);
                return;
            }
        }

        plugin.sendChatMessage("Could not find the Item you were looking for in the cache.");
    }

    private void addItemObject(ItemData data, CustomModelType customModelType)
    {
        ModelStats[] modelStats = dataFinder.findModelsForGroundItem(data.getId(), customModelType);
        if (modelStats == null || modelStats.length == 0)
        {
            plugin.sendChatMessage("Could not find the Item you were looking for in the cache.");
            return;
        }

        clientThread.invokeLater(() ->
        {
            LightingStyle ls;

            switch (customModelType)
            {
                default:
                case CACHE_GROUND_ITEM:
                    ls = LightingStyle.DEFAULT;
                    break;
                case CACHE_MAN_WEAR:
                case CACHE_WOMAN_WEAR:
                    ls = LightingStyle.ACTOR;
            }

            CustomLighting lighting = new CustomLighting(
                    ls.getAmbient(),
                    ls.getContrast(),
                    ls.getX(),
                    ls.getY(),
                    ls.getZ());

            Model model = plugin.constructModelFromCache(modelStats, new int[0], false, ls, lighting);

            CustomModelComp comp = new CustomModelComp(0, customModelType, data.getId(), modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, data.getName());
            CustomModel customModel = new CustomModel(model, comp);
            plugin.addCustomModel(customModel, false);

            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
            Character character = creatorsPanel.createCharacter(
                    ParentPanel.SIDE_PANEL,
                    data.getName(),
                    7699,
                    customModel,
                    true,
                    0,
                    -1,
                    -1,
                    60,
                    new KeyFrame[KeyFrameType.getTotalFrameTypes()][],
                    creatorsPanel.getRandomColor(),
                    false,
                    null,
                    null,
                    -1,
                    false,
                    false);

            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));
        });
    }

    private void addObjectObject(int id)
    {
        List<ObjectData> data = dataFinder.getObjectData();
        for (ObjectData n : data)
        {
            if (n.getId() == id)
            {
                addObjectObject(n);
                return;
            }
        }

        plugin.sendChatMessage("Could not find the Object you were looking for in the cache.");
    }

    private void addObjectObject(ObjectData data)
    {
        ModelStats[] modelStats = dataFinder.findModelsForObject(data.getId(), -1, LightingStyle.DEFAULT, true);
        if (modelStats == null || modelStats.length == 0)
        {
            plugin.sendChatMessage("Could not find the Object you were looking for in the cache.");
            return;
        }

        clientThread.invokeLater(() ->
        {
            CustomLighting lighting = new CustomLighting(64, 768, -50, -50, 10);
            Model model = plugin.constructModelFromCache(modelStats, new int[0], false, LightingStyle.DEFAULT, lighting);
            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_OBJECT, data.getId(), modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, data.getName());
            CustomModel customModel = new CustomModel(model, comp);
            plugin.addCustomModel(customModel, false);

            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
            Character character = creatorsPanel.createCharacter(
                    ParentPanel.SIDE_PANEL,
                    data.getName(),
                    7699,
                    customModel,
                    true,
                    0,
                    data.getAnimationId(),
                    -1,
                    60,
                    new KeyFrame[KeyFrameType.getTotalFrameTypes()][],
                    creatorsPanel.getRandomColor(),
                    false,
                    null,
                    null,
                    -1,
                    false,
                    false);

            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));
        });
    }

    private void addNPCObject(int id)
    {
        List<NPCData> data = dataFinder.getNpcData();
        for (NPCData n : data)
        {
            if (n.getId() == id)
            {
                addNPCObject(n);
                return;
            }
        }

        plugin.sendChatMessage("Could not find the NPC you were looking for in the cache.");
    }

    private void addNPCObject(NPCData data)
    {
        ModelStats[] modelStats = dataFinder.findModelsForNPC(data.getId());
        if (modelStats == null || modelStats.length == 0)
        {
            plugin.sendChatMessage("Could not find the NPC you were looking for in the cache.");
            return;
        }

        clientThread.invokeLater(() ->
        {
            CustomLighting lighting = new CustomLighting(64, 850, -30, -30, 50);
            Model model = plugin.constructModelFromCache(modelStats, new int[0], false, LightingStyle.ACTOR, lighting);
            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, data.getId(), modelStats, null, null, null, LightingStyle.ACTOR, lighting, false, data.getName());
            CustomModel customModel = new CustomModel(model, comp);
            plugin.addCustomModel(customModel, false);

            CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();
            Character character = creatorsPanel.createCharacter(
                    ParentPanel.SIDE_PANEL,
                    data.getName(),
                    7699,
                    customModel,
                    true,
                    0,
                    data.getStandingAnimation(),
                    -1,
                    data.getSize() * 60,
                    new KeyFrame[KeyFrameType.getTotalFrameTypes()][],
                    creatorsPanel.getRandomColor(),
                    false,
                    null,
                    null,
                    -1,
                    false,
                    false);

            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));
        });
    }
}
