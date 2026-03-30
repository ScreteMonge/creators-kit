package com.creatorskit.swing;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.models.*;
import com.creatorskit.models.datatypes.*;
import com.creatorskit.swing.renderer.RenderPanel;
import com.creatorskit.swing.searchabletable.JFilterableTable;
import com.creatorskit.swing.timesheet.keyframe.AnimationKeyFrame;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ModelData;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import okhttp3.*;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CacheSearcherTab extends JPanel
{
    private Client client;
    private final CreatorsPlugin plugin;
    private ClientThread clientThread;
    private final DataFinder dataFinder;
    private final ModelUtilities modelUtilities;
    private OkHttpClient httpClient;

    private final GridBagConstraints c = new GridBagConstraints();
    private final String NPC = "NPC";
    private final String OBJECT = "OBJECT";
    private final String ITEM = "ITEM";
    private final String ANIM = "ANIM";
    private final String SPOTANIM = "SPOTANIM";
    private final String SOUND = "SOUND";

    private final JPanel npcPanel = new JPanel();
    private final JPanel objectPanel = new JPanel();
    private final JPanel itemPanel = new JPanel();
    private final JPanel animPanel = new JPanel();
    private final JPanel spotAnimPanel = new JPanel();
    private final JPanel soundPanel = new JPanel();

    private final JPanel previewPanel = new JPanel();
    private final JPanel breakdownPanel = new JPanel();

    @Getter
    private RenderPanel renderPanel;

    private final JFilterableTable npcTable = new JFilterableTable("NPCs");
    private final JFilterableTable objectTable = new JFilterableTable("Objects");
    private final JFilterableTable itemTable = new JFilterableTable("Items");
    private final JFilterableTable animTable = new JFilterableTable("Animations");
    private final JFilterableTable spotAnimTable = new JFilterableTable("SpotAnims");
    private final JFilterableTable soundTable = new JFilterableTable("Sounds");
    private final JFilterableTable modelTable = new JFilterableTable("Model Id Breakdown");

    private final JComboBox<CustomModelType> itemType = new JComboBox<>();
    private final JPanel display = new JPanel();
    public static final File SOUNDS_DIR = new File(RuneLite.RUNELITE_DIR, "creatorskit/sound-exports");

    private CustomModelType selectedType;

    @Inject
    public CacheSearcherTab(Client client, CreatorsPlugin plugin, ClientThread clientThread, DataFinder dataFinder, ModelUtilities modelUtilities, OkHttpClient httpClient)
    {
        this.client = client;
        this.plugin = plugin;
        this.clientThread = clientThread;
        this.dataFinder = dataFinder;
        this.modelUtilities = modelUtilities;
        this.httpClient = httpClient;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setLayout(new GridBagLayout());

        setupNPCPanel();
        setupObjectPanel();
        setupItemPanel();
        setupAnimPanel();
        setupSpotAnimPanel();
        setupSoundPanel();
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

        JPanel soundCard = new JPanel();
        setupSoundCard(soundCard);
        display.add(soundCard, SOUND);
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

            if (selectedType == null)
            {
                renderPanel.resetViewer();
                return;
            }

            switch (selectedType)
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
                        updateRenderPanel(selectedType, data.getId(), renderAll, modelId);
                    }
                    break;
                case CACHE_SPOTANIM:
                    Object spotAnim = spotAnimTable.getSelectedObject();
                    if (spotAnim instanceof SpotanimData)
                    {
                        SpotanimData data = (SpotanimData) spotAnim;
                        updateRenderPanel(selectedType, data.getId(), renderAll, modelId);
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
        LightingStyle ls = LightingStyle.DEFAULT;

        switch (type)
        {
            case CACHE_NPC:
                modelStats = dataFinder.findModelsForNPC(id);
                ls = LightingStyle.ACTOR;
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
                ls = LightingStyle.SPOTANIM;
        }

        if (modelStats == null || modelStats.length == 0)
        {
            renderPanel.resetViewer();
            return;
        }

        LightingStyle finalLs = ls;
        if (renderAll)
        {
            ModelStats[] allModelStats = modelStats;
            clientThread.invokeLater(() ->
            {
                ModelData md = modelUtilities.constructModelDataFromCache(allModelStats, new int[0], false);
                renderPanel.updateModel(md, finalLs);
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
                    renderPanel.updateModel(md, finalLs);
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

        if (modelIds != null)
        {
            for (int i : modelIds)
            {
                Object e = i;
                dataList.add(e);
            }
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

        renderPanel = new RenderPanel(client, clientThread, fovSlider);
        previewPanel.add(renderPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BorderLayout());
        previewPanel.add(footer, BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel(new FlowLayout());
        footer.add(controlPanel, BorderLayout.NORTH);

        JButton resetButton = new JButton("Reset Camera View");
        resetButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        resetButton.addActionListener(e -> renderPanel.resetCameraView());
        controlPanel.add(resetButton);

        JCheckBox animate = new JCheckBox("Enable Animations");
        animate.setSelected(true);
        animate.addActionListener(e -> clientThread.invokeLater(() -> renderPanel.toggleAnimations(animate.isSelected())));
        controlPanel.add(animate);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 3, 3, 3));
        footer.add(buttonPanel, BorderLayout.SOUTH);
        setupRenderPanelButtons(buttonPanel);
    }

    private void setupRenderPanelButtons(JPanel buttonPanel)
    {
        JButton addObject = new JButton("Store & Add");
        addObject.setToolTipText("Stores the displayed model as a Custom Model, then creates a new Object and attaches the model");
        buttonPanel.add(addObject);

        JButton addObjectAnim = new JButton("Store/Add/Animate");
        addObjectAnim.setToolTipText("<html>Stores the displayed model as a Custom Model, then creates a new Object and attaches the model,<br>and creates an Animation KeyFrame with the appropriate NPC animations</html>");
        buttonPanel.add(addObjectAnim);

        JButton addModel = new JButton("Store Only");
        addModel.setToolTipText("Stores the displayed model as a new Custom Model");
        buttonPanel.add(addModel);

        JButton addAnvil = new JButton("Add to Anvil");
        addAnvil.setToolTipText("Sends the displayed model's component models to the Model Anvil");
        buttonPanel.add(addAnvil);

        JButton export = new JButton("Export 3D");
        export.setToolTipText("Exports the displayed 3D model");
        buttonPanel.add(export);

        addObject.addActionListener(e -> addModel(true, false));
        addObjectAnim.addActionListener(e -> addModel(true, true));
        addModel.addActionListener(e -> addModel(false, false));
        addAnvil.addActionListener(e -> addToAnvil());
        export.addActionListener(e -> export3DModel());
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
        add(soundPanel, c);

        c.gridx = 1;
        c.gridy = 6;
        add(breakdownPanel, c);

        c.gridx = 1;
        c.gridy = 7;
        c.weighty = 1;
        add(new JLabel(""), c);

        c.gridheight = 8;
        c.weighty = 5;
        c.weightx = 2;
        c.gridx = 2;
        c.gridy = 0;
        add(display, c);

        c.gridheight = 8;
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

                selectedType = CustomModelType.CACHE_NPC;
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

        objectTable.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = objectTable.getSelectedObject();
            if (o instanceof ObjectData)
            {
                ObjectData data = (ObjectData) o;

                selectedType = CustomModelType.CACHE_OBJECT;
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
        JButton addKeyFrame = new JButton("KeyFrame Animation");
        addKeyFrame.setToolTipText("Finds the animations for the given item (if a weapon) and applies it to the currently selected Object as an Animation KeyFrame");
        card.add(addKeyFrame, c);

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

                selectedType = (CustomModelType) itemType.getSelectedItem();
                updateRenderPanel(selectedType, itemId, true, -1);

                int[] modelIds;
                switch (selectedType)
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
        JButton addKeyFrame = new JButton("KeyFrame SpotAnim");
        addKeyFrame.setToolTipText("Adds the currently selected SpotAnim as a KeyFrame to the currently selected Object");
        card.add(addKeyFrame, c);

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

        spotAnimTable.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = spotAnimTable.getSelectedObject();
            if (o instanceof SpotanimData)
            {
                SpotanimData data = (SpotanimData) o;

                selectedType = CustomModelType.CACHE_SPOTANIM;
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

        animTable.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = animTable.getSelectedObject();
            if (o instanceof AnimData)
            {
                AnimData data = (AnimData) o;
                int animId = data.getId();
                clientThread.invokeLater(() -> renderPanel.updateAnimation(client.loadAnimation(animId)));
            }
        });

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
        JLabel instructionLabel = new JLabel("<html>Select an Animation to play it in the renderer to the right (if a Model is being displayed)<br>Double click an Animation to set it to the currently selected Object</html>");
        card.add(instructionLabel, c);
    }

    private void setupSoundCard(JPanel card)
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
        JLabel title = new JLabel("Sounds Found:");
        title.setFont(new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 32));
        title.setHorizontalAlignment(SwingConstants.LEFT);
        card.add(title, c);

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane(soundTable);
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
        JLabel instructionLabel = new JLabel("Select any sound to play. Ensure you're logged in, and that Sound Effects volume is up/not muted.");
        card.add(instructionLabel, c);

        c.gridx = 0;
        c.gridy = 4;
        JButton export = new JButton("Export Sound File");
        card.add(export, c);

        export.addActionListener((e ->
        {
            Object o = soundTable.getSelectedObject();
            if (o == null)
            {
                return;
            }

            if (o instanceof SoundData)
            {
                SoundData sd = (SoundData) o;
                String url = "https://github.com/ScreteMonge/Sound-Effect-Cache/raw/refs/heads/main/.idea/SoundEffects/" + sd.createLookupName() + ".wav";

                Request request = new Request.Builder().url(url).build();
                Call call = httpClient.newCall(request);
                call.enqueue(new Callback()
                {
                    @Override
                    public void onFailure(Call call, IOException e)
                    {
                        log.debug("Failed to access URL: " + url);
                        plugin.sendChatMessage("This sound file could not be found.");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException
                    {
                        if (response.isSuccessful() && response.body() != null)
                        {
                            InputStream inputStream = response.body().byteStream();

                            if (!SOUNDS_DIR.exists())
                            {
                                SOUNDS_DIR.mkdirs();
                            }

                            FileOutputStream outputStream = new FileOutputStream(new File(SOUNDS_DIR, sd + ".wav"));
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1)
                            {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            outputStream.close();
                            inputStream.close();

                            plugin.sendChatMessage("Exported " + sd + ".wav to your /.runelite/creatorskit/sound-exports directory.");
                            response.body().close();
                        }
                    }
                });
            }
        }));
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

    private void setupSoundPanel()
    {
        soundPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        soundPanel.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        soundPanel.setLayout(new BorderLayout());
        JPanel holderPanel = new JPanel();
        holderPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        holderPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holderPanel.setLayout(new GridBagLayout());
        soundPanel.add(holderPanel, BorderLayout.CENTER);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JLabel title = new JLabel("Sound Searcher");
        title.setFont(FontManager.getRunescapeBoldFont());
        holderPanel.add(title, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        JLabel nameLabel = new JLabel("Sound name: ");
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
                switchCards(SOUND);
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
                switchCards(SOUND);
                String text = field.getText();
                soundTable.searchAndListEntries(text);
            }
        };
        field.addKeyListener(keyListener);

        if (dataFinder.isDataLoaded(DataFinder.DataType.SOUND))
        {
            List<SoundData> dataList = dataFinder.getSoundData();
            List<Object> list = new ArrayList<>(dataList);
            soundTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.SOUND, () ->
            {
                List<SoundData> dataList = dataFinder.getSoundData();
                List<Object> list = new ArrayList<>(dataList);
                soundTable.initialize(list);
            });
        }

        soundTable.getSelectionModel().addListSelectionListener(e ->
        {
            Object o = soundTable.getSelectedObject();
            if (o instanceof SoundData)
            {
                SoundData data = (SoundData) o;
                clientThread.invokeLater(() -> plugin.getClient().playSoundEffect(data.getId()));
            }
        });
    }

    private void addModel(boolean addObject, boolean addAnimationKeyframe)
    {
        if (selectedType == null)
        {
            return;
        }

        String name = "Unnamed";
        int id = 0;
        int size = 1;
        int animId = -1;
        AnimationKeyFrame akf = null;

        switch (selectedType)
        {
            case CACHE_NPC:
                Object npc = npcTable.getSelectedObject();
                if (npc instanceof NPCData)
                {
                    NPCData data = (NPCData) npc;
                    name = data.getName();
                    id = data.getId();
                    size = data.getSize();
                    animId = data.getStandingAnimation();

                    if (addAnimationKeyframe)
                    {
                        akf = new AnimationKeyFrame(
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
                    }
                }
                break;
            case CACHE_OBJECT:
                Object obj = objectTable.getSelectedObject();
                if (obj instanceof ObjectData)
                {
                    ObjectData data = (ObjectData) obj;
                    name = data.getName();
                    id = data.getId();
                    animId = data.getAnimationId();

                    if (addAnimationKeyframe)
                    {
                        akf = new AnimationKeyFrame(
                                plugin.getCurrentTick(),
                                false,
                                -1,
                                0,
                                false,
                                false,
                                data.getAnimationId(),
                                -1,
                                -1,
                                -1,
                                -1,
                                -1,
                                -1,
                                -1);
                    }
                }
                break;
            case CACHE_GROUND_ITEM:
            case CACHE_MAN_WEAR:
            case CACHE_WOMAN_WEAR:
                Object item = itemTable.getSelectedObject();
                if (item instanceof ItemData)
                {
                    ItemData data = (ItemData) item;
                    name = data.getName();
                    id = data.getId();
                }
                break;
            case CACHE_SPOTANIM:
                Object sa = spotAnimTable.getSelectedObject();
                if (sa instanceof SpotanimData)
                {
                    SpotanimData data = (SpotanimData) sa;
                    name = data.getName();
                    id = data.getId();
                    animId = data.getAnimationId();

                    if (addAnimationKeyframe)
                    {
                        akf = new AnimationKeyFrame(
                                plugin.getCurrentTick(),
                                false,
                                -1,
                                0,
                                false,
                                false,
                                data.getAnimationId(),
                                -1,
                                -1,
                                -1,
                                -1,
                                -1,
                                -1,
                                -1);
                    }
                }
        }

        Object anim = animTable.getSelectedObject();
        if (anim instanceof AnimData)
        {
            AnimData data = (AnimData) anim;
            animId = data.getId();
        }

        modelUtilities.cacheToCustomModel(selectedType, id, -1, size, name, animId, addObject, akf);
    }

    private void addToAnvil()
    {
        if (selectedType == null)
        {
            return;
        }

        int id = 0;

        switch (selectedType)
        {
            case CACHE_NPC:
                Object npc = npcTable.getSelectedObject();
                if (npc instanceof NPCData)
                {
                    NPCData data = (NPCData) npc;
                    id = data.getId();
                }
                break;
            case CACHE_OBJECT:
                Object obj = objectTable.getSelectedObject();
                if (obj instanceof ObjectData)
                {
                    ObjectData data = (ObjectData) obj;
                    id = data.getId();
                }
                break;
            case CACHE_GROUND_ITEM:
            case CACHE_MAN_WEAR:
            case CACHE_WOMAN_WEAR:
                Object item = itemTable.getSelectedObject();
                if (item instanceof ItemData)
                {
                    ItemData data = (ItemData) item;
                    id = data.getId();
                }
                break;
            case CACHE_SPOTANIM:
                Object sa = spotAnimTable.getSelectedObject();
                if (sa instanceof SpotanimData)
                {
                    SpotanimData data = (SpotanimData) sa;
                    id = data.getId();
                }
        }

        boolean renderAll = false;
        int modelId = 0;

        Object o = modelTable.getSelectedObject();
        if (o == null)
        {
            modelUtilities.cacheToAnvil(selectedType, id, true, -1);
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

        modelUtilities.cacheToAnvil(selectedType, id, renderAll, modelId);
    }

    private void export3DModel()
    {
        if (selectedType == null)
        {
            return;
        }

        int id = 0;
        String name = "Unnamed";

        switch (selectedType)
        {
            case CACHE_NPC:
                Object npc = npcTable.getSelectedObject();
                if (npc instanceof NPCData)
                {
                    NPCData data = (NPCData) npc;
                    name = data.getName();
                    id = data.getId();
                }
                break;
            case CACHE_OBJECT:
                Object obj = objectTable.getSelectedObject();
                if (obj instanceof ObjectData)
                {
                    ObjectData data = (ObjectData) obj;
                    name = data.getName();
                    id = data.getId();
                }
                break;
            case CACHE_GROUND_ITEM:
            case CACHE_MAN_WEAR:
            case CACHE_WOMAN_WEAR:
                Object item = itemTable.getSelectedObject();
                if (item instanceof ItemData)
                {
                    ItemData data = (ItemData) item;
                    name = data.getName();
                    id = data.getId();
                }
                break;
            case CACHE_SPOTANIM:
                Object sa = spotAnimTable.getSelectedObject();
                if (sa instanceof SpotanimData)
                {
                    SpotanimData data = (SpotanimData) sa;
                    name = data.getName();
                    id = data.getId();
                }
        }

        ModelGetter modelGetter = plugin.getModelGetter();
        boolean renderAll = false;
        int modelId = 0;

        Object o = modelTable.getSelectedObject();
        if (o == null)
        {
            modelGetter.exportModelFromCache(selectedType, id, name, true, -1);;
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

        modelGetter.exportModelFromCache(selectedType, id, name, renderAll, modelId);
    }
}