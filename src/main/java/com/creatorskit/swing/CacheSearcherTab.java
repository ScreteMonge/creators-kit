package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.*;
import com.creatorskit.models.datatypes.*;
import com.creatorskit.swing.renderer.RenderPanel;
import com.creatorskit.swing.searchabletable.JFilterableTable;
import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
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
    private final ModelUtilities modelUtilities;

    private final GridBagConstraints c = new GridBagConstraints();
    private final String NPC = "NPC";
    private final String OBJECT = "OBJECT";
    private final String ITEM = "ITEM";
    private final String ANIM = "ANIM";
    private final String SPOTANIM = "SPOTANIM";
    private String currentCard = NPC;

    private final JPanel npcPanel = new JPanel();
    private final JPanel objectPanel = new JPanel();
    private final JPanel itemPanel = new JPanel();
    private final JPanel animPanel = new JPanel();
    private final JPanel spotAnimPanel = new JPanel();

    private final JPanel previewPanel = new JPanel();
    private final JPanel breakdownPanel = new JPanel();
    private RenderPanel renderPanel;

    private final JFilterableTable npcTable = new JFilterableTable("NPCs");
    private final JFilterableTable objectTable = new JFilterableTable("Objects");
    private final JFilterableTable itemTable = new JFilterableTable("Items");
    private final JFilterableTable animTable = new JFilterableTable("Animations");
    private final JFilterableTable spotAnimTable = new JFilterableTable("SpotAnims");
    private final JFilterableTable modelTable = new JFilterableTable("Model Id Breakdown");

    private final JComboBox<CustomModelType> itemType = new JComboBox<>();

    private final JPanel display = new JPanel();

    @Inject
    public CacheSearcherTab(CreatorsPlugin plugin, ClientThread clientThread, DataFinder dataFinder, ModelUtilities modelUtilities)
    {
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.dataFinder = dataFinder;
        this.modelUtilities = modelUtilities;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setLayout(new GridBagLayout());

        setupNPCPanel();
        setupObjectPanel();
        setupItemPanel();
        setupAnimPanel();
        setupSpotAnimPanel();
        setupDisplay();
        setupBreakdownTable();
        setupRenderPanel();
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

    private void setupBreakdownTable()
    {
        breakdownPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        breakdownPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        breakdownPanel.setLayout(new BorderLayout());
        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        breakdownPanel.add(holderPanel, BorderLayout.CENTER);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Model Id Breakdown");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1;
        modelTable.getSelectionModel().addListSelectionListener(e ->
        {
            boolean renderAll = false;
            int modelId = 0;

            Object o = modelTable.getSelectedObject();
            if (o instanceof String)
            {
                String s = (String) o;
                if (s.equals("All"))
                {
                    renderAll = true;
                }
            }

            if (o instanceof Integer)
            {
                modelId = (Integer) o;
            }

            CustomModelType type = getCurrentTypeSelected();
            if (type == null)
            {
                renderPanel.resetViewer();
                return;
            }

            switch (type)
            {
                case CACHE_NPC:
                    Object npc = npcTable.getSelectedObject();
                    if (npc instanceof NPCData)
                    {
                        NPCData data = (NPCData) npc;
                        updateRenderPanel(CustomModelType.CACHE_NPC, data.getId(), renderAll, modelId);
                    }
                    break;
                case CACHE_OBJECT:
                    Object object = objectTable.getSelectedObject();
                    if (object instanceof ObjectData)
                    {
                        ObjectData data = (ObjectData) object;
                        updateRenderPanel(CustomModelType.CACHE_OBJECT, data.getId(), renderAll, modelId);
                    }
                    break;
                case CACHE_GROUND_ITEM:
                case CACHE_MAN_WEAR:
                case CACHE_WOMAN_WEAR:
                    Object item = itemTable.getSelectedObject();
                    if (item instanceof ItemData)
                    {
                        ItemData data = (ItemData) item;
                        updateRenderPanel(type, data.getId(), renderAll, modelId);
                    }
                    break;
                case CACHE_SPOTANIM:
                    Object spotAnim = spotAnimTable.getSelectedObject();
                    if (spotAnim instanceof SpotanimData)
                    {
                        SpotanimData data = (SpotanimData) spotAnim;
                        updateRenderPanel(type, data.getId(), renderAll, modelId);
                    }
                    break;
                default:
                    renderPanel.resetViewer();
            }
        });

        holderPanel.add(modelTable, c);
    }

    private void updateRenderPanel(CustomModelType type, int id, boolean renderAll, int modelId)
    {
        ModelStats[] modelStats = new ModelStats[0];
        switch (type)
        {
            case CACHE_NPC:
                modelStats = dataFinder.findModelsForNPC(id);
                break;
            case CACHE_OBJECT:
                modelStats = dataFinder.findModelsForObject(id, 0, LightingStyle.DEFAULT, true);
                break;
            case CACHE_GROUND_ITEM:
            case CACHE_MAN_WEAR:
            case CACHE_WOMAN_WEAR:
                modelStats = dataFinder.findModelsForGroundItem(id, type);
                break;
            case CACHE_SPOTANIM:
                modelStats = dataFinder.findSpotAnim(id);
        }

        if (modelStats == null || modelStats.length == 0)
        {
            renderPanel.resetViewer();
            return;
        }

        if (renderAll)
        {
            ModelStats[] allModelStats = modelStats;
            clientThread.invokeLater(() ->
            {
                ModelData md = modelUtilities.constructModelDataFromCache(allModelStats, new int[0], false);
                renderPanel.updateModel(md);
            });
            return;
        }

        for (ModelStats modelStat : modelStats)
        {
            if (modelStat.getModelId() == modelId)
            {
                clientThread.invokeLater(() ->
                {
                    ModelData md = modelUtilities.constructModelDataFromCache(new ModelStats[]{modelStat}, new int[0], false);
                    renderPanel.updateModel(md);
                });
                return;
            }
        }

        renderPanel.resetViewer();
    }

    private void updateModelBreakdownTable(int[] modelIds)
    {
        List<Object> dataList = new ArrayList<>();
        Object all = "All";
        dataList.add(all);

        for (int i : modelIds)
        {
            Object e = i;
            dataList.add(e);
        }

        List<Object> list = new ArrayList<>(dataList);
        modelTable.initialize(list);
        modelTable.searchAndListEntries("");
        revalidate();
    }

    private void setupRenderPanel()
    {
        previewPanel.setLayout(new BorderLayout());
        previewPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        previewPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        JSlider fovSlider = new JSlider(1, 179, RenderPanel.FOV_DEFAULT);
        previewPanel.add(fovSlider, BorderLayout.NORTH);

        renderPanel = new RenderPanel(fovSlider);
        previewPanel.add(renderPanel, BorderLayout.CENTER);

        JButton resetButton = new JButton("Reset Camera View");
        resetButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        resetButton.addActionListener(e -> renderPanel.resetCameraView());
        previewPanel.add(resetButton, BorderLayout.SOUTH);
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
        JLabel empty = new JLabel("");
        empty.setFocusable(true);
        add(empty, c);

        c.gridwidth = 1;
        c.weightx = 2;
        c.weighty = 0;
        c.gridx = 1;
        c.gridy = 0;
        add(npcPanel, c);

        c.gridx = 1;
        c.gridy = 1;
        add(objectPanel, c);

        c.gridx = 1;
        c.gridy = 2;
        add(itemPanel, c);

        c.gridx = 1;
        c.gridy = 3;
        add(spotAnimPanel, c);

        c.gridx = 1;
        c.gridy = 4;
        add(animPanel, c);

        c.gridx = 1;
        c.gridy = 5;
        add(breakdownPanel, c);

        c.gridx = 1;
        c.gridy = 6;
        c.weighty = 1;
        add(new JLabel(""), c);

        c.gridheight = 7;
        c.weighty = 5;
        c.weightx = 2;
        c.gridx = 2;
        c.gridy = 0;
        add(display, c);

        c.gridheight = 7;
        c.weighty = 5;
        c.weightx = 8;
        c.gridx = 3;
        c.gridy = 0;
        add(previewPanel, c);

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
        grid.setLayout(new GridLayout(0, 4, 2, 0));
        card.add(grid, c);

        JButton addObject = new JButton("Store & Add");
        addObject.setToolTipText("Stores the selected NPC as a Custom Model, then creates a new Object and attaches the model");
        grid.add(addObject);

        JButton addObjectAnim = new JButton("Store/Add/Animate");
        addObjectAnim.setToolTipText("Stores the selected NPC as a Custom Model, then creates a new Object and attaches the model," +
                "<br>and creates an Animation KeyFrame with the appropriate NPC animations");
        grid.add(addObjectAnim);

        JButton addModel = new JButton("Store Only");
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
                addNPCObject(data, false);
            }
        });

        addObjectAnim.addActionListener(e ->
        {
            Object o = npcTable.getSelectedObject();
            if (o instanceof NPCData)
            {
                NPCData data = (NPCData) o;
                addNPCObject(data, true);
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

        c.gridx = 0;
        c.gridy = 4;
        JPanel anims = new JPanel();
        anims.setLayout(new GridLayout(0, 3, 2, 2));
        anims.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        anims.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.add(anims, c);

        JLabel idle = new JLabel("Idle: -1");
        JLabel walk180 = new JLabel("Walk 180: -1");
        JLabel walk = new JLabel("Walk: -1");
        JLabel walkRight = new JLabel("Walk Right: -1");
        JLabel walkLeft = new JLabel("Walk Left: -1");
        JLabel idleRight = new JLabel("Idle Right: -1");
        JLabel idleLeft = new JLabel("Idle Left: -1");
        JLabel run = new JLabel("Run: -1");
        anims.add(idle);
        anims.add(walk180);
        anims.add(new JLabel(""));
        anims.add(walk);
        anims.add(walkRight);
        anims.add(idleRight);
        anims.add(run);
        anims.add(walkLeft);
        anims.add(idleLeft);

        npcTable.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = npcTable.getSelectedObject();
            if (o instanceof NPCData)
            {
                NPCData data = (NPCData) o;

                idle.setText("Idle: " + data.getStandingAnimation());
                walk.setText("Walk: " + data.getWalkingAnimation());
                run.setText("Run: " + data.getRunAnimation());
                walk180.setText("Walk 180: " + data.getRotate180Animation());
                walkRight.setText("Walk Right: " + data.getRotateRightAnimation());
                walkLeft.setText("Walk Left: " + data.getRotateLeftAnimation());
                idleRight.setText("Idle Right: " + data.getIdleRotateRightAnimation());
                idleLeft.setText("Idle Left: " + data.getIdleRotateLeftAnimation());

                updateRenderPanel(CustomModelType.CACHE_NPC, data.getId(), true, -1);
                updateModelBreakdownTable(data.getModels());
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

        JButton addModel = new JButton("Store Only");
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

        objectTable.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = objectTable.getSelectedObject();
            if (o instanceof ObjectData)
            {
                ObjectData data = (ObjectData) o;

                updateRenderPanel(CustomModelType.CACHE_OBJECT, data.getId(), true, -1);
                updateModelBreakdownTable(data.getObjectModels());
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
        grid.setLayout(new GridLayout(0, 4, 2, 0));
        card.add(grid, c);

        JButton addObject = new JButton("Store & Add");
        addObject.setToolTipText("Stores the selected Item as a Custom Model, then creates a new Object and attaches the model");
        grid.add(addObject);

        JButton addKeyFrame = new JButton("KeyFrame Animation");
        addKeyFrame.setToolTipText("Finds the animations for the given item (if a weapon) and applies it to the currently selected Object as an Animation KeyFrame");
        grid.add(addKeyFrame);

        JButton addModel = new JButton("Store Only");
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

        addKeyFrame.addActionListener(e ->
        {
            if (plugin.getSelectedCharacter() == null)
            {
                return;
            }

            Object o = itemTable.getSelectedObject();
            if (o instanceof ItemData)
            {
                ItemData data = (ItemData) o;
                int itemId = data.getId();
                WeaponAnimData weaponAnimData = dataFinder.findWeaponAnimData(itemId);
                if (weaponAnimData == null)
                {
                    return;
                }

                plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().addAnimationKeyFrameFromCache(weaponAnimData);
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


        c.gridx = 0;
        c.gridy = 4;
        JPanel anims = new JPanel();
        anims.setLayout(new GridLayout(0, 3, 2, 2));
        anims.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        anims.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.add(anims, c);

        JLabel idle = new JLabel("Idle: -1");
        JLabel walk180 = new JLabel("Walk 180: -1");
        JLabel walk = new JLabel("Walk: -1");
        JLabel walkRight = new JLabel("Walk Right: -1");
        JLabel walkLeft = new JLabel("Walk Left: -1");
        JLabel idleRight = new JLabel("Idle Right: -1");
        JLabel idleLeft = new JLabel("Idle Left: -1");
        JLabel run = new JLabel("Run: -1");
        JLabel special = new JLabel("Special: -1");
        JLabel stab = new JLabel("Stab: -1");
        JLabel slash = new JLabel("Slash: -1");
        JLabel crush = new JLabel("Crush: -1");
        JLabel slash2 = new JLabel("Slash2: -1");
        JLabel crush2 = new JLabel("Crush2: -1");
        JLabel defend = new JLabel("Defend: -1");
        anims.add(idle);
        anims.add(walk180);
        anims.add(new JLabel(""));
        anims.add(walk);
        anims.add(walkRight);
        anims.add(idleRight);
        anims.add(run);
        anims.add(walkLeft);
        anims.add(idleLeft);
        anims.add(special);
        anims.add(defend);
        anims.add(new JLabel(""));
        anims.add(stab);
        anims.add(slash);
        anims.add(crush);
        anims.add(new JLabel(""));
        anims.add(slash2);
        anims.add(crush2);

        itemTable.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = itemTable.getSelectedObject();
            if (o instanceof ItemData)
            {
                ItemData data = (ItemData) o;
                int itemId = data.getId();

                CustomModelType type = (CustomModelType) itemType.getSelectedItem();
                updateRenderPanel((CustomModelType) itemType.getSelectedItem(), itemId, true, -1);

                int[] modelIds;
                switch (type)
                {
                    default:
                    case CACHE_GROUND_ITEM:
                        modelIds = new int[]{data.getInventoryModel()};
                        break;
                    case CACHE_MAN_WEAR:
                        modelIds = new int[]{data.getMaleModel0(), data.getMaleModel1(), data.getMaleModel2()};
                        break;
                    case CACHE_WOMAN_WEAR:
                        modelIds = new int[]{data.getFemaleModel0(), data.getFemaleModel1(), data.getFemaleModel2()};
                }
                updateModelBreakdownTable(modelIds);

                boolean foundMatch = false;

                List<WeaponAnimData> weaponAnimSets = dataFinder.getWeaponAnimData();
                for (WeaponAnimData weaponAnim : weaponAnimSets)
                {
                    int[] ids = weaponAnim.getId();
                    if (ids == null || ids.length == 0)
                    {
                        continue;
                    }

                    for (int i : ids)
                    {
                        if (i == itemId)
                        {
                            idle.setText("Idle: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE));
                            walk.setText("Walk: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.WALK));
                            run.setText("Run: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.RUN));
                            walk180.setText("Walk 180: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_180));
                            walkRight.setText("Walk Right: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_RIGHT));
                            walkLeft.setText("Walk Left: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_LEFT));
                            idleRight.setText("Idle Right: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE_ROTATE_RIGHT));
                            idleLeft.setText("Idle Left: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE_ROTATE_LEFT));
                            special.setText("Special: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.SPECIAL));
                            stab.setText("Stab: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.STAB));
                            slash.setText("Slash: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.SLASH));
                            crush.setText("Crush: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.CRUSH));
                            slash2.setText("Slash2: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.SLASH_2));
                            crush2.setText("Crush2: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.CRUSH_2));
                            defend.setText("Defend: " + WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.DEFEND));
                            foundMatch = true;
                            break;
                        }
                    }

                    if (foundMatch)
                    {
                        break;
                    }
                }

                if (!foundMatch)
                {
                    idle.setText("Idle: " + -1);
                    walk.setText("Walk: " + -1);
                    run.setText("Run: " + -1);
                    walk180.setText("Walk 180: " + -1);
                    walkRight.setText("Walk Right: " + -1);
                    walkLeft.setText("Walk Left: " + -1);
                    idleRight.setText("Idle Right: " + -1);
                    idleLeft.setText("Idle Left: " + -1);
                    special.setText("Special: " + -1);
                    stab.setText("Stab: " + -1);
                    slash.setText("Slash: " + -1);
                    crush.setText("Crush: " + -1);
                    slash2.setText("Slash2: " + -1);
                    crush2.setText("Crush2: " + -1);
                    defend.setText("Defend: " + -1);
                }
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
        grid.setLayout(new GridLayout(0, 4, 2, 0));
        card.add(grid, c);

        JButton addObject = new JButton("Store & Add");
        addObject.setToolTipText("Stores the selected SpotAnim as a Custom Model, then creates a new Object and attaches the model");
        grid.add(addObject);

        JButton addKeyFrame = new JButton("KeyFrame SpotAnim");
        addKeyFrame.setToolTipText("Adds the currently selected SpotAnim as a KeyFrame to the currently selected Object");
        grid.add(addKeyFrame);

        JButton addModel = new JButton("Store Only");
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

        addKeyFrame.addActionListener(e ->
        {
            if (plugin.getSelectedCharacter() == null)
            {
                return;
            }

            Object o = spotAnimTable.getSelectedObject();
            if (o instanceof SpotanimData)
            {
                SpotanimData data = (SpotanimData) o;
                plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().addSpotAnimKeyFrameFromCache(data);
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

        spotAnimTable.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = spotAnimTable.getSelectedObject();
            if (o instanceof SpotanimData)
            {
                SpotanimData data = (SpotanimData) o;

                updateRenderPanel(CustomModelType.CACHE_SPOTANIM, data.getId(), true, -1);
                updateModelBreakdownTable(new int[]{data.getModelId()});
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
        if (!cardName.equals(ANIM))
        {
            currentCard = cardName;
        }
    }

    private CustomModelType getCurrentTypeSelected()
    {
        //Ignores Anim
        switch(currentCard)
        {
            default:
                return null;
            case NPC:
                return CustomModelType.CACHE_NPC;
            case OBJECT:
                return CustomModelType.CACHE_OBJECT;
            case ITEM:
                return (CustomModelType) itemType.getSelectedItem();
            case SPOTANIM:
                return CustomModelType.CACHE_SPOTANIM;
        }
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
        itemType.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e) {
                switchCards(ITEM);
            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });

        itemType.addItemListener(e ->
        {
            Object o = itemTable.getSelectedObject();
            if (o instanceof ItemData)
            {
                ItemData data = (ItemData) o;

                CustomModelType type = (CustomModelType) itemType.getSelectedItem();
                updateRenderPanel(type, data.getId(), true, -1);

                int[] modelIds;
                switch (type)
                {
                    default:
                    case CACHE_GROUND_ITEM:
                        modelIds = new int[]{data.getInventoryModel()};
                        break;
                    case CACHE_MAN_WEAR:
                        modelIds = new int[]{data.getMaleModel0(), data.getMaleModel1(), data.getMaleModel2()};
                        break;
                    case CACHE_WOMAN_WEAR:
                        modelIds = new int[]{data.getFemaleModel0(), data.getFemaleModel1(), data.getFemaleModel2()};
                }
                updateModelBreakdownTable(modelIds);
            }
        });
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
        boolean renderAll = false;
        int modelId = 0;

        Object o = modelTable.getSelectedObject();
        if (o == null)
        {
            modelUtilities.cacheToAnvil(type, id, true, -1);
            return;
        }

        if (o instanceof String)
        {
            String s = (String) o;
            if (s.equals("All"))
            {
                renderAll = true;
            }
        }

        if (o instanceof Integer)
        {
            modelId = (Integer) o;
        }

        modelUtilities.cacheToAnvil(type, id, renderAll, modelId);
    }

    private void addCustomModel(CustomModelType type, int id)
    {
        modelUtilities.cacheToCustomModel(type, id, -1);
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

            Model model = modelUtilities.constructModelFromCache(modelStats, new int[0], false, ls, lighting);

            CustomModelComp comp = new CustomModelComp(0, customModelType, data.getId(), modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, data.getName());
            CustomModel customModel = new CustomModel(model, comp);
            modelUtilities.addCustomModel(customModel, false);

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
                    creatorsPanel.getRandomColor(),
                    false,
                    null,
                    null,
                    -1,
                    false,
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
            Model model = modelUtilities.constructModelFromCache(modelStats, new int[0], false, LightingStyle.DEFAULT, lighting);
            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_OBJECT, data.getId(), modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, data.getName());
            CustomModel customModel = new CustomModel(model, comp);
            modelUtilities.addCustomModel(customModel, false);

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
                    creatorsPanel.getRandomColor(),
                    false,
                    null,
                    null,
                    -1,
                    false,
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
                addNPCObject(n, false);
                return;
            }
        }

        plugin.sendChatMessage("Could not find the NPC you were looking for in the cache.");
    }

    private void addNPCObject(NPCData data, boolean addAnimKeyFrame)
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
            Model model = modelUtilities.constructModelFromCache(modelStats, new int[0], false, LightingStyle.ACTOR, lighting);
            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, data.getId(), modelStats, null, null, null, LightingStyle.ACTOR, lighting, false, data.getName());
            CustomModel customModel = new CustomModel(model, comp);
            modelUtilities.addCustomModel(customModel, false);

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
                    creatorsPanel.getRandomColor(),
                    false,
                    null,
                    null,
                    -1,
                    false,
                    false,
                    false);

            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));

            if (addAnimKeyFrame)
            {
                AnimationKeyFrame keyFrame = new AnimationKeyFrame(
                        plugin.getCurrentTick(),
                        false,
                        -1,
                        0,
                        false,
                        false,
                        data.getStandingAnimation(),
                        data.getWalkingAnimation(),
                        data.getRunAnimation(),
                        data.getRotate180Animation(),
                        data.getRotateRightAnimation(),
                        data.getRotateLeftAnimation(),
                        data.getIdleRotateRightAnimation(),
                        data.getIdleRotateRightAnimation());

                character.setKeyFrames(new KeyFrame[]{keyFrame}, KeyFrameType.ANIMATION);
                creatorsPanel.getToolBox().getProgrammer().updateProgram(character);
            }
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
            Model model = modelUtilities.constructModelFromCache(modelStats, new int[0], false, LightingStyle.CUSTOM, lighting);
            CustomModelComp comp = new CustomModelComp(0, CustomModelType.CACHE_SPOTANIM, data.getId(), modelStats, null, null, null, LightingStyle.CUSTOM, lighting, false, data.getName());
            CustomModel customModel = new CustomModel(model, comp);
            modelUtilities.addCustomModel(customModel, false);

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
                    creatorsPanel.getRandomColor(),
                    false,
                    null,
                    null,
                    -1,
                    false,
                    false,
                    false);

            SwingUtilities.invokeLater(() -> creatorsPanel.addPanel(ParentPanel.SIDE_PANEL, character, true, false));
        });
    }
}
