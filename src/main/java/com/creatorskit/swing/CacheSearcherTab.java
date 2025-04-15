package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.*;
import com.creatorskit.models.datatypes.*;
import com.creatorskit.swing.searchabletable.JFilterableTable;
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
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class CacheSearcherTab extends JPanel
{
    private final CreatorsPlugin plugin;
    private ClientThread clientThread;
    private final DataFinder dataFinder;

    private final GridBagConstraints c = new GridBagConstraints();
    private final String NPC = "NPC";
    private final String OBJECT = "OBJECT";
    private final String ITEM = "ITEM";
    private final String ANIM = "ANIM";
    private final String SPOTANIM = "SPOTANIM";

    private final JPanel npcPanel = new JPanel();
    private final JPanel objectPanel = new JPanel();
    private final JPanel itemPanel = new JPanel();
    private final JPanel animPanel = new JPanel();
    private final JPanel spotAnimPanel = new JPanel();

    private final JFilterableTable npcTable = new JFilterableTable("NPCs");
    private final JFilterableTable objectTable = new JFilterableTable("Objects");
    private final JFilterableTable itemTable = new JFilterableTable("Items");
    private final JFilterableTable animTable = new JFilterableTable("Animations");
    private final JFilterableTable spotAnimTable = new JFilterableTable("SpotAnims");

    private final JComboBox<CustomModelType> itemType = new JComboBox<>();

    private final JPanel display = new JPanel();

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
        setupAnimPanel();
        setupSpotAnimPanel();
        setupDisplay();
        setupLayout();
    }

    private void setupDisplay()
    {
        display.setLayout(new CardLayout());
        display.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        JPanel npcCard = new JPanel();
        setupNPCCard(npcCard);
        display.add(npcCard, NPC);

        JPanel objectCard = new JPanel();
        setupObjectCard(objectCard);
        display.add(objectCard, OBJECT);

        JPanel itemCard = new JPanel();
        setupItemCard(itemCard);
        display.add(itemCard, ITEM);

        JPanel spotAnimCard = new JPanel();
        setupSpotAnimCard(spotAnimCard);
        display.add(spotAnimCard, SPOTANIM);

        JPanel animCard = new JPanel();
        setupAnimCard(animCard);
        display.add(animCard, ANIM);
    }

    private void setupLayout()
    {
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 2;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel(""), c);

        c.gridwidth = 1;
        c.weightx = 2;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 1;
        add(npcPanel, c);

        c.gridx = 1;
        c.gridy = 2;
        add(objectPanel, c);

        c.gridx = 1;
        c.gridy = 3;
        add(itemPanel, c);

        c.gridx = 1;
        c.gridy = 4;
        add(spotAnimPanel, c);

        c.gridx = 1;
        c.gridy = 5;
        add(animPanel, c);

        c.gridx = 1;
        c.gridy = 6;
        add(new JLabel(""), c);

        c.gridheight = 6;
        c.weighty = 5;
        c.weightx = 2;
        c.gridx = 2;
        c.gridy = 1;
        add(display, c);

        c.weighty = 1;
        c.weightx = 2;
        c.gridx = 3;
        c.gridy = 7;
        add(new JLabel(""), c);

        repaint();
        revalidate();
    }

    private void setupNPCCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("NPCs Found:");
        title.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 32));
        title.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(title, c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane(npcTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        card.add(scrollPane, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 2;
        JLabel buffer = new JLabel(" ");
        card.add(buffer, c);

        c.gridx = 0;
        c.gridy = 3;
        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(0, 3, 2, 0));
        card.add(grid, c);

        JButton addObject = new JButton("Store & Add");
        addObject.setToolTipText("Stores the selected NPC as a Custom Model, then creates a new Object and attaches the model");
        grid.add(addObject);

        JButton addModel = new JButton("Store");
        addModel.setToolTipText("Stores the selected NPC as a new Custom Model");
        grid.add(addModel);

        JButton addAnvil = new JButton("Add to Anvil");
        addAnvil.setToolTipText("Sends the NPC's models to the Model Anvil");
        grid.add(addAnvil);

        addObject.addActionListener(e ->
        {
            Object o = npcTable.getSelectedObject();
            if (o instanceof NPCData)
            {
                NPCData data = (NPCData) o;
                addNPCObject(data);
            }
        });

        addModel.addActionListener(e ->
        {
            Object o = npcTable.getSelectedObject();
            if (o instanceof NPCData)
            {
                NPCData data = (NPCData) o;
                addCustomModel(CustomModelType.CACHE_NPC, data.getId());
            }
        });

        addAnvil.addActionListener(e ->
        {
            Object o = npcTable.getSelectedObject();
            if (o instanceof NPCData)
            {
                NPCData data = (NPCData) o;
                addToAnvil(CustomModelType.CACHE_NPC, data.getId());
            }
        });
    }

    private void setupObjectCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Objects Found:");
        title.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 32));
        title.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(title, c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane(objectTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        card.add(scrollPane, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel buffer = new JLabel(" ");
        card.add(buffer, c);

        c.gridx = 0;
        c.gridy = 3;
        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(0, 3, 2, 0));
        card.add(grid, c);

        JButton addObject = new JButton("Store & Add");
        addObject.setToolTipText("Stores the selected Object as a Custom Model, then creates a new Object and attaches the model");
        grid.add(addObject);

        JButton addModel = new JButton("Store");
        addModel.setToolTipText("Stores the selected Object as a new Custom Model");
        grid.add(addModel);

        JButton addAnvil = new JButton("Add to Anvil");
        addAnvil.setToolTipText("Sends the Object's models to the Model Anvil");
        grid.add(addAnvil);

        addObject.addActionListener(e ->
        {
            Object o = objectTable.getSelectedObject();
            if (o instanceof ObjectData)
            {
                ObjectData data = (ObjectData) o;
                addObjectObject(data);
            }
        });

        addModel.addActionListener(e ->
        {
            Object o = objectTable.getSelectedObject();
            if (o instanceof ObjectData)
            {
                ObjectData data = (ObjectData) o;
                addCustomModel(CustomModelType.CACHE_OBJECT, data.getId());
            }
        });

        addAnvil.addActionListener(e ->
        {
            Object o = objectTable.getSelectedObject();
            if (o instanceof ObjectData)
            {
                ObjectData data = (ObjectData) o;
                addToAnvil(CustomModelType.CACHE_OBJECT, data.getId());
            }
        });
    }

    private void setupItemCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Items Found:");
        title.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 32));
        title.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(title, c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane(itemTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        card.add(scrollPane, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel buffer = new JLabel(" ");
        card.add(buffer, c);

        c.gridx = 0;
        c.gridy = 3;
        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(0, 3, 2, 0));
        card.add(grid, c);

        JButton addObject = new JButton("Store & Add");
        addObject.setToolTipText("Stores the selected Item as a Custom Model, then creates a new Object and attaches the model");
        grid.add(addObject);

        JButton addModel = new JButton("Store");
        addModel.setToolTipText("Stores the selected Item as a new Custom Model");
        grid.add(addModel);

        JButton addAnvil = new JButton("Add to Anvil");
        addAnvil.setToolTipText("Sends the Item's models to the Model Anvil");
        grid.add(addAnvil);

        addObject.addActionListener(e ->
        {
            Object o = itemTable.getSelectedObject();
            if (o instanceof ItemData)
            {
                CustomModelType type = (CustomModelType) itemType.getSelectedItem();
                ItemData data = (ItemData) o;
                addItemObject(data, type);
            }
        });

        addModel.addActionListener(e ->
        {
            Object o = itemTable.getSelectedObject();
            if (o instanceof ItemData)
            {
                CustomModelType type = (CustomModelType) itemType.getSelectedItem();
                ItemData data = (ItemData) o;
                addCustomModel(type, data.getId());
            }
        });

        addAnvil.addActionListener(e ->
        {
            Object o = itemTable.getSelectedObject();
            if (o instanceof ItemData)
            {
                CustomModelType type = (CustomModelType) itemType.getSelectedItem();
                ItemData data = (ItemData) o;
                addToAnvil(type, data.getId());
            }
        });
    }

    private void setupSpotAnimCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("SpotAnims Found:");
        title.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 32));
        title.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(title, c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane(spotAnimTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        card.add(scrollPane, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel buffer = new JLabel("                                                    ");
        card.add(buffer, c);

        c.gridx = 0;
        c.gridy = 3;
        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(0, 3, 2, 0));
        card.add(grid, c);

        JButton addObject = new JButton("Store & Add");
        addObject.setToolTipText("Stores the selected SpotAnim as a Custom Model, then creates a new Object and attaches the model");
        grid.add(addObject);

        JButton addModel = new JButton("Store");
        addModel.setToolTipText("Stores the selected SpotAnim as a new Custom Model");
        grid.add(addModel);

        JButton addAnvil = new JButton("Add to Anvil");
        addAnvil.setToolTipText("Sends the SpotAnim's models to the Model Anvil");
        grid.add(addAnvil);

        addObject.addActionListener(e ->
        {
            Object o = spotAnimTable.getSelectedObject();
            if (o instanceof SpotanimData)
            {
                SpotanimData data = (SpotanimData) o;
                addSpotAnimObject(data);
            }
        });

        addModel.addActionListener(e ->
        {
            Object o = spotAnimTable.getSelectedObject();
            if (o instanceof SpotanimData)
            {
                SpotanimData data = (SpotanimData) o;
                addCustomModel(CustomModelType.CACHE_SPOTANIM, data.getId());
            }
        });

        addAnvil.addActionListener(e ->
        {
            Object o = spotAnimTable.getSelectedObject();
            if (o instanceof SpotanimData)
            {
                SpotanimData data = (SpotanimData) o;
                addToAnvil(CustomModelType.CACHE_SPOTANIM, data.getId());
            }
        });
    }

    private void setupAnimCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Animations Found:");
        title.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 32));
        title.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(title, c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane(animTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        card.add(scrollPane, c);

        animTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = animTable.getSelectedObject();
                    if (o instanceof AnimData)
                    {
                        AnimData data = (AnimData) o;
                        Character character = plugin.getSelectedCharacter();
                        if (character != null)
                        {
                            int animId = data.getId();
                            character.getAnimationSpinner().setValue(animId);
                        }
                    }
                }
            }
        });

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 2;
        JLabel buffer = new JLabel(" ");
        card.add(buffer, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel instructionLabel = new JLabel("Double click any Animation to set it to the currently selected Object");
        card.add(instructionLabel, c);
    }

    private void switchCards(String cardName)
    {
        CardLayout cl = (CardLayout) (display.getLayout());
        cl.show(display, cardName);
    }

    private void setupNPCPanel()
    {
        npcPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        npcPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        npcPanel.setLayout(new BorderLayout());
        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        npcPanel.add(holderPanel, BorderLayout.CENTER);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("NPC Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        JLabel nameLabel = new JLabel("NPC name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(nameLabel, c);

        c.gridwidth = 1;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 1;
        JTextField field = new JTextField("");
        field.setFont(FontManager.getRunescapeBoldFont());
        field.setForeground(ColorScheme.BRAND_ORANGE);
        holderPanel.add(field, c);

        field.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                switchCards(NPC);
            }

            @Override
            public void focusLost(FocusEvent e)
            {

            }
        });

        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                switchCards(NPC);
                String text = field.getText();
                npcTable.searchAndListEntries(text);
            }
        };
        field.addKeyListener(keyListener);

        if (dataFinder.isDataLoaded(DataFinder.DataType.NPC))
        {
            List<NPCData> dataList = dataFinder.getNpcData();
            List<Object> list = new ArrayList<>(dataList);
            npcTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.NPC, () ->
            {
                List<NPCData> dataList = dataFinder.getNpcData();
                List<Object> list = new ArrayList<>(dataList);
                npcTable.initialize(list);
            });
        }
    }

    private void setupObjectPanel()
    {
        objectPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        objectPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        objectPanel.setLayout(new BorderLayout());
        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        objectPanel.add(holderPanel, BorderLayout.CENTER);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Object Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        JLabel nameLabel = new JLabel("Object name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(nameLabel, c);

        c.gridwidth = 1;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 1;
        JTextField field = new JTextField("");
        field.setFont(FontManager.getRunescapeBoldFont());
        field.setForeground(ColorScheme.BRAND_ORANGE);
        holderPanel.add(field, c);

        field.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                switchCards(OBJECT);
            }

            @Override
            public void focusLost(FocusEvent e)
            {

            }
        });

        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                switchCards(OBJECT);
                String text = field.getText();
                objectTable.searchAndListEntries(text);
            }
        };
        field.addKeyListener(keyListener);

        if (dataFinder.isDataLoaded(DataFinder.DataType.OBJECT))
        {
            List<ObjectData> dataList = dataFinder.getObjectData();
            List<Object> list = new ArrayList<>(dataList);
            objectTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.OBJECT, () ->
            {
                List<ObjectData> dataList = dataFinder.getObjectData();
                List<Object> list = new ArrayList<>(dataList);
                objectTable.initialize(list);
            });
        }
    }

    private void setupItemPanel()
    {
        itemPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        itemPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        itemPanel.setLayout(new BorderLayout());
        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        itemPanel.add(holderPanel, BorderLayout.CENTER);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Item Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        JLabel modelTypeLabel = new JLabel("Model type: ");
        modelTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(modelTypeLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        itemType.setToolTipText("Set whether to search the cache for the Item's Ground model, Male worn model, or Female worn model");
        itemType.addItem(CustomModelType.CACHE_GROUND_ITEM);
        itemType.addItem(CustomModelType.CACHE_MAN_WEAR);
        itemType.addItem(CustomModelType.CACHE_WOMAN_WEAR);
        holderPanel.add(itemType, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 2;
        JLabel nameLabel = new JLabel("Item name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(nameLabel, c);

        c.gridwidth = 1;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 2;
        JTextField field = new JTextField("");
        field.setFont(FontManager.getRunescapeBoldFont());
        field.setForeground(ColorScheme.BRAND_ORANGE);
        holderPanel.add(field, c);

        field.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                switchCards(ITEM);
            }

            @Override
            public void focusLost(FocusEvent e)
            {

            }
        });

        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                switchCards(ITEM);
                String text = field.getText();
                itemTable.searchAndListEntries(text);
            }
        };
        field.addKeyListener(keyListener);

        if (dataFinder.isDataLoaded(DataFinder.DataType.ITEM))
        {
            List<ItemData> dataList = dataFinder.getItemData();
            List<Object> list = new ArrayList<>(dataList);
            itemTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.ITEM, () ->
            {
                List<ItemData> dataList = dataFinder.getItemData();
                List<Object> list = new ArrayList<>(dataList);
                itemTable.initialize(list);
            });
        }
    }

    private void setupSpotAnimPanel()
    {
        spotAnimPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        spotAnimPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        spotAnimPanel.setLayout(new BorderLayout());
        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        spotAnimPanel.add(holderPanel, BorderLayout.CENTER);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("SpotAnim Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        JLabel nameLabel = new JLabel("SpotAnim name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(nameLabel, c);

        c.gridwidth = 1;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 1;
        JTextField field = new JTextField("");
        field.setFont(FontManager.getRunescapeBoldFont());
        field.setForeground(ColorScheme.BRAND_ORANGE);
        holderPanel.add(field, c);

        field.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                switchCards(SPOTANIM);
            }

            @Override
            public void focusLost(FocusEvent e)
            {

            }
        });

        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                switchCards(SPOTANIM);
                String text = field.getText();
                spotAnimTable.searchAndListEntries(text);
            }
        };
        field.addKeyListener(keyListener);

        if (dataFinder.isDataLoaded(DataFinder.DataType.SPOTANIM))
        {
            List<SpotanimData> dataList = dataFinder.getSpotanimData();
            List<Object> list = new ArrayList<>(dataList);
            spotAnimTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.SPOTANIM, () ->
            {
                List<SpotanimData> dataList = dataFinder.getSpotanimData();
                List<Object> list = new ArrayList<>(dataList);
                spotAnimTable.initialize(list);
            });
        }
    }

    private void setupAnimPanel()
    {
        animPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        animPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        animPanel.setLayout(new BorderLayout());
        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        animPanel.add(holderPanel, BorderLayout.CENTER);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Animation Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        JLabel nameLabel = new JLabel("Animation name: ");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        holderPanel.add(nameLabel, c);

        c.gridwidth = 1;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 1;
        JTextField field = new JTextField("");
        field.setFont(FontManager.getRunescapeBoldFont());
        field.setForeground(ColorScheme.BRAND_ORANGE);
        holderPanel.add(field, c);

        field.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                switchCards(ANIM);
            }

            @Override
            public void focusLost(FocusEvent e)
            {

            }
        });

        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                switchCards(ANIM);
                String text = field.getText();
                animTable.searchAndListEntries(text);
            }
        };
        field.addKeyListener(keyListener);

        if (dataFinder.isDataLoaded(DataFinder.DataType.ANIM))
        {
            List<AnimData> dataList = dataFinder.getAnimData();
            List<Object> list = new ArrayList<>(dataList);
            animTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.ANIM, () ->
            {
                List<AnimData> dataList = dataFinder.getAnimData();
                List<Object> list = new ArrayList<>(dataList);
                animTable.initialize(list);
            });
        }
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
                    KeyFrameType.createDefaultSummary(),
                    creatorsPanel.createEmptyProgram(-1, -1),
                    false,
                    null,
                    null,
                    new int[0],
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
                    KeyFrameType.createDefaultSummary(),
                    creatorsPanel.createEmptyProgram(data.getAnimationId(), data.getAnimationId()),
                    false,
                    null,
                    null,
                    new int[0],
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
                    KeyFrameType.createDefaultSummary(),
                    creatorsPanel.createEmptyProgram(data.getStandingAnimation(), data.getWalkingAnimation()),
                    false,
                    null,
                    null,
                    new int[0],
                    -1,
                    false,
                    false);

            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));
        });
    }

    private void addSpotAnimObject(int id)
    {
        List<SpotanimData> data = dataFinder.getSpotanimData();
        for (SpotanimData n : data)
        {
            if (n.getId() == id)
            {
                addSpotAnimObject(n);
                return;
            }
        }

        plugin.sendChatMessage("Could not find the SpotAnim you were looking for in the cache.");
    }

    private void addSpotAnimObject(SpotanimData data)
    {
        ModelStats[] modelStats = dataFinder.findSpotAnim(data);
        if (modelStats == null || modelStats.length == 0)
        {
            plugin.sendChatMessage("Could not find the SpotAnim you were looking for in the cache.");
            return;
        }

        clientThread.invokeLater(() ->
        {
            LightingStyle ls = LightingStyle.SPOTANIM;
            CustomLighting lighting = new CustomLighting(ls.getAmbient() + data.getAmbient(), ls.getContrast() + data.getContrast(), ls.getX(), ls.getY(), ls.getZ());
            Model model = plugin.constructModelFromCache(modelStats, new int[0], false, LightingStyle.CUSTOM, lighting);
            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_SPOTANIM, data.getId(), modelStats, null, null, null, LightingStyle.CUSTOM, lighting, false, data.getName());
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
                    KeyFrameType.createDefaultSummary(),
                    creatorsPanel.createEmptyProgram(data.getAnimationId(), -1),
                    false,
                    null,
                    null,
                    new int[0],
                    -1,
                    false,
                    false);

            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));
        });
    }
}
