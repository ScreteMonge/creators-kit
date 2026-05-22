package com.creatorskit.swing.timesheet;

import com.creatorskit.CKObject;
import com.creatorskit.Character;
import com.creatorskit.CreatorsConfig;
import com.creatorskit.models.CustomModel;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.datatypes.*;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.programming.orientation.Orientation;
import com.creatorskit.programming.orientation.OrientationGoal;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.swing.searchabletable.JFilterableTable;
import com.creatorskit.swing.timesheet.attributes.*;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.creatorskit.swing.timesheet.sheets.TimeSheet;
import com.creatorskit.swing.timesheet.keyframe.settings.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
@Setter
public class AttributePanel extends JPanel
{
    private Client client;
    private ClientThread clientThread;
    private CreatorsConfig config;
    private TimeSheetPanel timeSheetPanel;
    private DataFinder dataFinder;
    private SelectionManager selectionManager;

    private final BufferedImage HELP = ImageUtil.loadImageResource(getClass(), "/Help.png");
    private final BufferedImage COMPASS = ImageUtil.loadImageResource(getClass(), "/Orientation_compass.png");
    private final BufferedImage RESET = ImageUtil.loadImageResource(getClass(), "/Reset.png");
    private final Icon keyframeImage = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Keyframe.png"));
    private final Icon keyframeEmptyImage = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/Keyframe_Empty.png"));

    private final GridBagConstraints c = new GridBagConstraints();
    private final JPanel cardPanel = new JPanel();
    private final JLabel objectLabel = new JLabel("[No Object Selected]");
    private final JLabel cardLabel = new JLabel("");
    private final JButton keyFramed = new JButton();
    private final JButton updateButton = new JButton("Update");
    private final JButton resetButton = new JButton();

    private final JFilterableTable npcTable = new JFilterableTable("NPCs");
    private final JFilterableTable itemTable = new JFilterableTable("Items");
    private final JFilterableTable animTable = new JFilterableTable("Animations");
    private final JFilterableTable spotanimTable = new JFilterableTable("SpotAnims");

    private final JPopupMenu spotanimPopup = new JPopupMenu("SpotAnims");

    public static final String MOVE_CARD = "Movement";
    public static final String ANIM_CARD = "Animation";
    public static final String ORI_CARD = "Orientation";
    public static final String SPAWN_CARD = "Spawn";
    public static final String MODEL_CARD = "Model";
    public static final String TEXT_CARD = "Text";
    public static final String OVER_CARD = "Overhead";
    public static final String HEALTH_CARD = "Health";
    public static final String SPOTANIM_CARD = "SpotAnim 1";
    public static final String SPOTANIM2_CARD = "SpotAnim 2";
    public static final String HITSPLAT_1_CARD = "Hitsplat 1";
    public static final String HITSPLAT_2_CARD = "Hitsplat 2";
    public static final String HITSPLAT_3_CARD = "Hitsplat 3";
    public static final String HITSPLAT_4_CARD = "Hitsplat 4";
    public static final String PROJECTILE_CARD = "Projectile";
    public static final String SHIELD_CARD = "Shield";
    public static final String SPECIAL_CARD = "Special";
    public static final String SCREEN_FADE_CARD = "Screen Fade";
    public static final String SCREEN_SHAKE_CARD = "Screen Shake";
    public static final String CAMERA_CARD = "Camera";
    public static final String PULSE_CARD = "Pulse";
    public static final String MIXED_TYPES_CARD = "MixedTypes";
    public static final String NO_SELECTION_CARD = "NoSelection";

    private final String NO_OBJECT_SELECTED = "[No Object Selected]";
    private String activeCard = MOVE_CARD;
    private Font attributeFont = new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 32);
    /**
     * Smaller font for the multi-select label so the "N Keyframes across M Objects"
     * string fits two lines without overflowing the panel width.
     */
    private Font multiSelectFont = new Font(FontManager.getRunescapeBoldFont().getName(), Font.PLAIN, 18);

    private KeyFrameType hoveredKeyFrameType;
    private Component hoveredComponent;
    private KeyFrameType selectedKeyFramePage = KeyFrameType.MOVEMENT;

    /**
     * Reentrant guard for auto-update. When > 0 the field-change listeners are
     * suppressed because the panel itself just pushed values into the spinners
     * via setAttributes (e.g. after a selection change). Without this guard
     * every programmatic setValue would echo back as "user edit" and either
     * loop or rewrite the freshly-loaded keyframe with its own values.
     *
     * <p>Int counter rather than boolean so nested guard pushes (e.g. a
     * setAttributes call inside another setAttributes path) don't clear the
     * suppression on the inner finally before the outer one is done.
     */
    private int suppressAutoUpdateDepth = 0;

    private final MovementAttributes movementAttributes = new MovementAttributes();
    private final AnimAttributes animAttributes = new AnimAttributes();
    private final OriAttributes oriAttributes = new OriAttributes();
    private final SpawnAttributes spawnAttributes = new SpawnAttributes();
    private final ModelAttributes modelAttributes = new ModelAttributes();
    private final TextAttributes textAttributes = new TextAttributes();
    private final OverheadAttributes overheadAttributes = new OverheadAttributes();
    private final HealthAttributes healthAttributes = new HealthAttributes();
    private final SpotAnimAttributes spotAnimAttributes = new SpotAnimAttributes();
    private final SpotAnimAttributes spotAnim2Attributes = new SpotAnimAttributes();
    private final HitsplatAttributes hitsplat1Attributes = new HitsplatAttributes();
    private final HitsplatAttributes hitsplat2Attributes = new HitsplatAttributes();
    private final HitsplatAttributes hitsplat3Attributes = new HitsplatAttributes();
    private final HitsplatAttributes hitsplat4Attributes = new HitsplatAttributes();
    private final ProjectileAttributes projectileAttributes = new ProjectileAttributes();
    private final ShieldAttributes shieldAttributes = new ShieldAttributes();
    private final SpecialAttributes specialAttributes = new SpecialAttributes();
    private final ScreenFadeAttributes screenFadeAttributes = new ScreenFadeAttributes();
    private final ScreenShakeAttributes screenShakeAttributes = new ScreenShakeAttributes();
    private final com.creatorskit.swing.timesheet.attributes.CameraAttributes cameraAttributes = new com.creatorskit.swing.timesheet.attributes.CameraAttributes();
    private final com.creatorskit.swing.timesheet.attributes.PulseAttributes pulseAttributes = new com.creatorskit.swing.timesheet.attributes.PulseAttributes();

    private final Random random = new Random();

    @Inject
    public AttributePanel(Client client, ClientThread clientThread, CreatorsConfig config, TimeSheetPanel timeSheetPanel, DataFinder dataFinder, SelectionManager selectionManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.timeSheetPanel = timeSheetPanel;
        this.dataFinder = dataFinder;
        this.selectionManager = selectionManager;
        selectionManager.addListener(mgr -> updateObjectLabel(mgr.getPrimary()));

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        objectLabel.setFont(attributeFont);
        objectLabel.setForeground(Color.WHITE);
        objectLabel.setHorizontalAlignment(SwingConstants.LEFT);

        cardPanel.setLayout(new CardLayout());
        cardPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cardPanel.setFocusable(true);
        addMouseFocusListener(cardPanel);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        add(objectLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        cardLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        cardLabel.setFont(FontManager.getRunescapeBoldFont());
        cardLabel.setText(MOVE_CARD);
        add(cardLabel, c);

        // Update button removed -- auto-update on field change is the default
        // now, so explicit Update is redundant. updateButton field stays for
        // any callsite that still references it but it's never added to the
        // layout. Reset is kept for the "wipe spinner values to defaults"
        // workflow which auto-update doesn't replace.
        c.gridx = 2;
        c.gridy = 0;
        resetButton.setIcon(new ImageIcon(RESET));
        resetButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        resetButton.setToolTipText("Reset all the parameters of the currently visible KeyFrame");
        resetButton.addActionListener(e -> setAttributesEmpty(false));
        add(resetButton, c);

        c.gridx = 3;
        c.gridy = 0;
        keyFramed.setIcon(keyframeEmptyImage);
        keyFramed.setPreferredSize(new Dimension(32, 32));
        keyFramed.setBackground(ColorScheme.DARK_GRAY_COLOR);
        keyFramed.setToolTipText("Add keyframe at the current tick. Use Delete to remove.");
        keyFramed.addActionListener(e -> timeSheetPanel.onAddKeyFrameButtonPressed());
        add(keyFramed, c);

        c.gridwidth = 5;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 1;
        add(cardPanel, c);

        JPanel moveCard = new JPanel();
        JPanel animCard = new JPanel();
        JPanel oriCard = new JPanel();
        JPanel spawnCard = new JPanel();
        JPanel modelCard = new JPanel();
        JPanel textCard = new JPanel();
        JPanel overCard = new JPanel();
        JPanel healthCard = new JPanel();
        JPanel spotanimCard = new JPanel();
        JPanel spotanim2Card = new JPanel();
        JPanel hitsplat1Card = new JPanel();
        JPanel hitsplat2Card = new JPanel();
        JPanel hitsplat3Card = new JPanel();
        JPanel hitsplat4Card = new JPanel();
        JPanel projectileCard = new JPanel();
        JPanel shieldCard = new JPanel();
        JPanel specialCard = new JPanel();
        JPanel screenFadeCard = new JPanel();
        cardPanel.add(moveCard, MOVE_CARD);
        cardPanel.add(animCard, ANIM_CARD);
        cardPanel.add(oriCard, ORI_CARD);
        cardPanel.add(spawnCard, SPAWN_CARD);
        cardPanel.add(modelCard, MODEL_CARD);
        cardPanel.add(textCard, TEXT_CARD);
        cardPanel.add(overCard, OVER_CARD);
        cardPanel.add(healthCard, HEALTH_CARD);
        cardPanel.add(spotanimCard, SPOTANIM_CARD);
        cardPanel.add(spotanim2Card, SPOTANIM2_CARD);
        cardPanel.add(hitsplat1Card, HITSPLAT_1_CARD);
        cardPanel.add(hitsplat2Card, HITSPLAT_2_CARD);
        cardPanel.add(hitsplat3Card, HITSPLAT_3_CARD);
        cardPanel.add(hitsplat4Card, HITSPLAT_4_CARD);
        cardPanel.add(projectileCard, PROJECTILE_CARD);
        cardPanel.add(shieldCard, SHIELD_CARD);
        cardPanel.add(specialCard, SPECIAL_CARD);
        cardPanel.add(screenFadeCard, SCREEN_FADE_CARD);
        JPanel screenShakeCard = new JPanel();
        cardPanel.add(screenShakeCard, SCREEN_SHAKE_CARD);
        JPanel cameraCard = new JPanel();
        cardPanel.add(cameraCard, CAMERA_CARD);
        JPanel pulseCard = new JPanel();
        cardPanel.add(pulseCard, PULSE_CARD);

        // Empty placeholder shown when the keyframe selection spans multiple types.
        // Using a CardLayout entry keeps the cardPanel at its normal height instead
        // of letting the layout collapse when we hide editing controls.
        JPanel mixedTypesCard = new JPanel();
        mixedTypesCard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cardPanel.add(mixedTypesCard, MIXED_TYPES_CARD);

        // Placeholder shown when no keyframe is selected. The card editor is hidden
        // because there's nothing to mutate -- auto-update fires per spinner change,
        // and the previous behaviour (silently rewriting the seeker-adjacent keyframe
        // when the user typed values without a selection) was the main source of
        // the "I lost all my edits" UX complaints.
        JPanel noSelectionCard = new JPanel();
        noSelectionCard.setLayout(new GridBagLayout());
        noSelectionCard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JLabel noSelectionLabel = new JLabel("Select a keyframe to edit");
        noSelectionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        noSelectionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        noSelectionCard.add(noSelectionLabel);
        cardPanel.add(noSelectionCard, NO_SELECTION_CARD);

        setupMoveCard(moveCard);
        setupAnimCard(animCard);
        setupOriCard(oriCard);
        setupSpawnCard(spawnCard);
        setupModelCard(modelCard);
        setupTextCard(textCard);
        setupOverheadCard(overCard);
        setupHealthCard(healthCard);
        setupSpotAnimFinder();
        setupSpotAnimCard(spotanimCard, KeyFrameType.SPOTANIM);
        setupSpotAnimCard(spotanim2Card, KeyFrameType.SPOTANIM2);
        setupHitsplatCard(hitsplat1Card, KeyFrameType.HITSPLAT_1);
        setupHitsplatCard(hitsplat2Card, KeyFrameType.HITSPLAT_2);
        setupHitsplatCard(hitsplat3Card, KeyFrameType.HITSPLAT_3);
        setupHitsplatCard(hitsplat4Card, KeyFrameType.HITSPLAT_4);
        setupProjectileCard(projectileCard);
        setupBarCard(shieldCard, KeyFrameType.SHIELD);
        setupBarCard(specialCard, KeyFrameType.SPECIAL);
        setupScreenFadeCard(screenFadeCard);
        setupScreenShakeCard(screenShakeCard);
        setupCameraCard(cameraCard);
        setupPulseCard(pulseCard);

        // Wire up auto-update on every Attributes instance. Each card's setupXxxCard
        // already attached the "set red on change" listeners for the dirty-state
        // visual; this adds a parallel listener that commits the change to the
        // selected keyframe immediately, replacing the old click-Update-or-lose-it
        // flow. Done AFTER setupXxxCard so the initial SpinnerNumberModel installs
        // don't fire as "user edits".
        com.creatorskit.swing.timesheet.attributes.Attributes[] allAttributes = {
                movementAttributes, animAttributes, oriAttributes, spawnAttributes,
                modelAttributes, textAttributes, overheadAttributes, healthAttributes,
                spotAnimAttributes, spotAnim2Attributes,
                hitsplat1Attributes, hitsplat2Attributes, hitsplat3Attributes, hitsplat4Attributes,
                projectileAttributes, shieldAttributes, specialAttributes,
                screenFadeAttributes, screenShakeAttributes, cameraAttributes,
                pulseAttributes
        };
        for (com.creatorskit.swing.timesheet.attributes.Attributes attrs : allAttributes)
        {
            wireAutoUpdate(attrs);
        }

        setupKeyListeners();
    }

    /**
     * Loads the live OSRS free-cam state (focal X/Y/Z, pitch, yaw, zoom) into
     * the Camera card's spinners. Used both by the Capture button and by the
     * "+" keyframe icon when adding a NEW camera keyframe -- the user almost
     * always wants the new keyframe to start at the current view, not the
     * 0/0/0 defaults that would teleport the camera to scene origin. The
     * suppress-auto-update guard prevents the spinner setValue cascade from
     * firing fireAutoUpdate while we're populating the fields.
     */
    public void captureLiveCameraIntoSpinners()
    {
        if (client == null) return;
        suppressAutoUpdateDepth++;
        try
        {
            cameraAttributes.getFocalX().setValue(client.getCameraFocalPointX());
            cameraAttributes.getFocalY().setValue(client.getCameraFocalPointY());
            cameraAttributes.getFocalZ().setValue(client.getCameraFocalPointZ());
            cameraAttributes.getPitchDeg().setValue(Math.toDegrees(client.getCameraFpPitch()));
            cameraAttributes.getYawDeg().setValue(Math.toDegrees(client.getCameraFpYaw()));
            cameraAttributes.getScale().setValue(client.getVarcIntValue(net.runelite.api.VarClientInt.CAMERA_ZOOM_FIXED_VIEWPORT));
        }
        finally
        {
            suppressAutoUpdateDepth--;
        }
    }

    /**
     * Walks every component in {@code attrs}' card and attaches a "user edited"
     * listener that calls {@link #fireAutoUpdate()}. Skips {@link JButton}s
     * because their own action handlers do the right thing already (e.g. the
     * Camera card's Capture button reads the live camera, not the spinner state).
     * Text fields are skipped too because their per-keystroke fire rate would
     * spam the undo stack -- text edits still need the manual Update button.
     */
    private void wireAutoUpdate(com.creatorskit.swing.timesheet.attributes.Attributes attrs)
    {
        for (JComponent c : attrs.getAllComponents())
        {
            if (c instanceof JSpinner)
            {
                ((JSpinner) c).addChangeListener(e -> {
                    flagFieldEdited(c);
                    fireAutoUpdate();
                });
            }
            else if (c instanceof JComboBox)
            {
                ((JComboBox<?>) c).addActionListener(e -> {
                    flagFieldEdited(c);
                    fireAutoUpdate();
                });
            }
            else if (c instanceof JCheckBox)
            {
                ((JCheckBox) c).addActionListener(e -> {
                    flagFieldEdited(c);
                    fireAutoUpdate();
                });
            }
            // JButton / JTextField / JTextArea intentionally not wired -- see method javadoc.
        }
    }

    // Per-field edit-feedback ---------------------------------------------
    // When a user-driven edit fires (suppressAutoUpdateDepth == 0), the
    // touched field's background flashes green and decays back to gray over
    // ~700ms via the shared fieldFadeTimer. Replaces the old yellow/green
    // ON_KEYFRAME / OFF_KEYFRAME background semantic with a single
    // "did my edit land?" cue.
    private static final Color FIELD_BASE = net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR;
    private static final Color FIELD_FLASH = new Color(60, 180, 70);
    private static final int FIELD_FADE_MS = 700;
    private final java.util.Map<JComponent, Long> recentlyEditedFields = new java.util.WeakHashMap<>();
    private javax.swing.Timer fieldFadeTimer;

    private void flagFieldEdited(JComponent c)
    {
        // Suppress while the panel itself is pushing values via setAttributes
        // -- otherwise loading a keyframe would flash every field, which is
        // visually noisy and meaningless.
        if (suppressAutoUpdateDepth > 0) return;
        recentlyEditedFields.put(c, System.currentTimeMillis());
        c.setBackground(FIELD_FLASH);
        ensureFieldFadeTimer();
    }

    private void ensureFieldFadeTimer()
    {
        if (fieldFadeTimer != null) return;
        fieldFadeTimer = new javax.swing.Timer(33, e -> {
            long now = System.currentTimeMillis();
            java.util.Iterator<java.util.Map.Entry<JComponent, Long>> it = recentlyEditedFields.entrySet().iterator();
            while (it.hasNext())
            {
                java.util.Map.Entry<JComponent, Long> entry = it.next();
                JComponent c = entry.getKey();
                long elapsed = now - entry.getValue();
                if (elapsed >= FIELD_FADE_MS)
                {
                    c.setBackground(FIELD_BASE);
                    it.remove();
                }
                else
                {
                    float t = elapsed / (float) FIELD_FADE_MS;
                    c.setBackground(lerpColor(FIELD_FLASH, FIELD_BASE, t));
                }
            }
            if (recentlyEditedFields.isEmpty())
            {
                // Idle: stop the timer so we're not repainting every 33ms for nothing.
                fieldFadeTimer.stop();
                fieldFadeTimer = null;
            }
        });
        fieldFadeTimer.start();
    }

    private static Color lerpColor(Color a, Color b, float t)
    {
        int r = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, bl)));
    }

    /**
     * Commits the current card values onto the selected keyframe(s). No-op when
     * the panel itself just pushed values in (the guard counter is non-zero) or
     * when there's no keyframe of the current type selected -- the seeker-fallback
     * in {@link TimeSheetPanel#onUpdateButtonPressed()} would silently rewrite
     * the keyframe under the playhead, which is exactly the surprise the new
     * UX is trying to remove.
     */
    private void fireAutoUpdate()
    {
        if (suppressAutoUpdateDepth > 0)
        {
            return;
        }
        if (timeSheetPanel == null)
        {
            return;
        }
        if (findSelectedKeyFrameOfCurrentType() == null)
        {
            return;
        }
        // Mark that we're mid-auto-update so the inner addKeyFrame call's
        // own resetAttributes doesn't fire and snap the spinners back to the
        // pre-replacement marquee state for a frame. The outer
        // setSelectedKeyFrames at the end of onUpdateButtonPressed runs its
        // own resetAttributes with the freshly-replaced marquee, which is
        // the one that should win.
        inAutoUpdateBatch = true;
        try
        {
            timeSheetPanel.onUpdateButtonPressed();
        }
        finally
        {
            inAutoUpdateBatch = false;
        }
        // Per-field flash (flagFieldEdited above) replaces the card-label
        // flash -- the field-level cue is more specific and the header
        // flash on top would be visual noise.
    }

    /** Flag inspected by TimeSheetPanel.addKeyFrame to skip its inner resetAttributes when auto-update owns the batch. */
    @Getter
    private boolean inAutoUpdateBatch = false;

    // flashCardLabelSaved was removed once flagFieldEdited started flashing
    // each touched field individually -- the header-level cue was redundant.

    /**
     * Create a keyframe out of the current AttributePanel settings, based on which card is currently being shown
     * @return a keyframe of type depending on which card is currently showing, with settings based on what is displayed on that card
     */
    public KeyFrame createKeyFrame(double tick)
    {
        return createKeyFrame(selectedKeyFramePage, tick);
    }

    /**
     * Create a keyframe of a specified type out of the current AttributePanel settings for the given card of the specified KeyFrameType
     * @param keyFrameType the type of keyframe to add
     * @return a keyframe of indicated type, with settings based on what is displayed on that card
     */
    public KeyFrame createKeyFrame(KeyFrameType keyFrameType, double tick)
    {
        switch (keyFrameType)
        {
            default:
            case MOVEMENT:
                WorldView worldView = client.getTopLevelWorldView();
                if (worldView == null || worldView.getMapRegions() == null)
                {
                    return null;
                }

                return new MovementKeyFrame(
                        tick,
                        worldView.getPlane(),
                        MovementManager.useLocalLocations(worldView),
                        new int[0][],
                        0,
                        0,
                        movementAttributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        (double) movementAttributes.getSpeed().getValue(),
                        (int) movementAttributes.getTurnRate().getValue()
                );
            case ANIMATION:
                return new AnimationKeyFrame(
                        tick,
                        animAttributes.getStall().getSelectedItem() == Toggle.ENABLE,
                        (int) animAttributes.getActive().getValue(),
                        (int) animAttributes.getStartFrame().getValue(),
                        animAttributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        animAttributes.getFreeze().getSelectedItem() == Toggle.ENABLE,
                        (int) animAttributes.getIdle().getValue(),
                        (int) animAttributes.getWalk().getValue(),
                        (int) animAttributes.getRun().getValue(),
                        (int) animAttributes.getWalk180().getValue(),
                        (int) animAttributes.getWalkRight().getValue(),
                        (int) animAttributes.getWalkLeft().getValue(),
                        (int) animAttributes.getIdleRight().getValue(),
                        (int) animAttributes.getIdleLeft().getValue(),
                        ((Number) animAttributes.getSpeed().getValue()).doubleValue(),
                        (int) animAttributes.getLastFrame().getValue(),
                        (int) animAttributes.getPauseTicks().getValue()
                );
            case ORIENTATION:
                return new OrientationKeyFrame(
                        tick,
                        OrientationGoal.POINT,
                        (int) oriAttributes.getStart().getValue(),
                        (int) oriAttributes.getEnd().getValue(),
                        (double) oriAttributes.getDuration().getValue(),
                        (int) oriAttributes.getTurnRate().getValue(),
                        oriAttributes.getTargetCharacterNameValue()
                );
            case SPAWN:
                return new SpawnKeyFrame(
                        tick,
                        spawnAttributes.getSpawn().getSelectedItem() == Toggle.ENABLE
                );
            case MODEL:
                return new ModelKeyFrame(
                        tick,
                        modelAttributes.getModelOverride().getSelectedItem() == ModelToggle.CUSTOM_MODEL,
                        (int) modelAttributes.getModelId().getValue(),
                        (CustomModel) modelAttributes.getCustomModel().getSelectedItem(),
                        (int) modelAttributes.getRadius().getValue()
                );
            case TEXT:
                return new TextKeyFrame(
                        tick,
                        (double) textAttributes.getDuration().getValue(),
                        textAttributes.getText().getText()
                );
            case OVERHEAD:
                return new OverheadKeyFrame(
                        tick,
                        (OverheadSprite) overheadAttributes.getSkullSprite().getSelectedItem(),
                        (OverheadSprite) overheadAttributes.getPrayerSprite().getSelectedItem()
                );
            case HEALTH:
            {
                HealthKeyFrame healthKf = new HealthKeyFrame(
                        tick,
                        (double) healthAttributes.getDuration().getValue(),
                        (HealthbarSprite) healthAttributes.getHealthbarSprite().getSelectedItem(),
                        (int) healthAttributes.getMaxHealth().getValue(),
                        (int) healthAttributes.getCurrentHealth().getValue(),
                        (int) healthAttributes.getOrder().getValue(),
                        (int) healthAttributes.getWidth().getValue()
                );
                // "Sync hitsplats" toggle from the card. Defaults true on the
                // checkbox so add-new keeps the prior behaviour; user can
                // uncheck per Health KF to opt out of the hitsplat -> Health
                // auto-sync. Card-driven create defaults autoSynced=false
                // (the new keyframe is user-authored, not sync-owned).
                healthKf.setSyncHitsplats(healthAttributes.getSyncHitsplats().isSelected());
                healthKf.setFadeInTicks(((Number) healthAttributes.getFadeInTicks().getValue()).doubleValue());
                healthKf.setFadeOutTicks(((Number) healthAttributes.getFadeOutTicks().getValue()).doubleValue());
                return healthKf;
            }
            case SPOTANIM:
            case SPOTANIM2:
                SpotAnimAttributes spAttributes;
                switch (keyFrameType)
                {
                    default:
                    case SPOTANIM:
                        spAttributes = spotAnimAttributes;
                        break;
                    case SPOTANIM2:
                        spAttributes = spotAnim2Attributes;
                }
                return new SpotAnimKeyFrame(
                        tick,
                        keyFrameType,
                        (int) spAttributes.getSpotAnimId().getValue(),
                        spAttributes.getLoop().getSelectedItem() == Toggle.ENABLE,
                        (int) spAttributes.getHeight().getValue(),
                        (int) spAttributes.getRadius().getValue()
                );
            case HITSPLAT_1:
            case HITSPLAT_2:
            case HITSPLAT_3:
            case HITSPLAT_4:
                HitsplatAttributes attributes;
                switch (keyFrameType)
                {
                    default:
                    case HITSPLAT_1:
                        attributes = hitsplat1Attributes;
                        break;
                    case HITSPLAT_2:
                        attributes = hitsplat2Attributes;
                        break;
                    case HITSPLAT_3:
                        attributes = hitsplat3Attributes;
                        break;
                    case HITSPLAT_4:
                        attributes = hitsplat4Attributes;
                }

                return new HitsplatKeyFrame(
                        tick,
                        keyFrameType,
                        ((Number) attributes.getDuration().getValue()).doubleValue(),
                        (HitsplatSprite) attributes.getSprite().getSelectedItem(),
                        (HitsplatVariant) attributes.getVariant().getSelectedItem(),
                        (int) attributes.getDamage().getValue()
                );
            case PROJECTILE:
                return new ProjectileKeyFrame(
                        tick,
                        (int) projectileAttributes.getProjectileId().getValue(),
                        projectileAttributes.getTargetValue(),
                        (int) projectileAttributes.getStartHeight().getValue(),
                        (int) projectileAttributes.getEndHeight().getValue(),
                        (int) projectileAttributes.getSlope().getValue(),
                        (int) projectileAttributes.getStartPos().getValue(),
                        ((Number) projectileAttributes.getDurationTicks().getValue()).doubleValue(),
                        ((Number) projectileAttributes.getStartDelayTicks().getValue()).doubleValue(),
                        projectileAttributes.getFaceTrajectory().isSelected()
                );
            case SHIELD:
                return new ShieldKeyFrame(
                        tick,
                        ((Number) shieldAttributes.getDuration().getValue()).doubleValue(),
                        shieldAttributes.getRgb(),
                        (int) shieldAttributes.getMaxValue().getValue(),
                        (int) shieldAttributes.getCurrentValue().getValue(),
                        (int) shieldAttributes.getOrder().getValue(),
                        (int) shieldAttributes.getWidth().getValue()
                );
            case SPECIAL:
                return new SpecialKeyFrame(
                        tick,
                        ((Number) specialAttributes.getDuration().getValue()).doubleValue(),
                        specialAttributes.getRgb(),
                        (int) specialAttributes.getMaxValue().getValue(),
                        (int) specialAttributes.getCurrentValue().getValue(),
                        (int) specialAttributes.getOrder().getValue(),
                        (int) specialAttributes.getWidth().getValue()
                );
            case SCREEN_FADE:
                return new ScreenFadeKeyFrame(
                        tick,
                        screenFadeAttributes.getRgb(),
                        (int) screenFadeAttributes.getPeakAlpha().getValue(),
                        (int) screenFadeAttributes.getRingRadius().getValue(),
                        (int) screenFadeAttributes.getRingFeather().getValue(),
                        ((Number) screenFadeAttributes.getFadeInTicks().getValue()).doubleValue(),
                        ((Number) screenFadeAttributes.getHoldTicks().getValue()).doubleValue(),
                        ((Number) screenFadeAttributes.getFadeOutTicks().getValue()).doubleValue()
                );
            case SCREEN_SHAKE:
                return new ScreenShakeKeyFrame(
                        tick,
                        (int) screenShakeAttributes.getAmplitudeHorizontal().getValue(),
                        (int) screenShakeAttributes.getAmplitudeVertical().getValue(),
                        ((Number) screenShakeAttributes.getFrequency().getValue()).doubleValue(),
                        ((Number) screenShakeAttributes.getDurationTicks().getValue()).doubleValue()
                );
            case CAMERA:
                return new com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame(
                        tick,
                        ((Number) cameraAttributes.getFocalX().getValue()).doubleValue(),
                        ((Number) cameraAttributes.getFocalY().getValue()).doubleValue(),
                        ((Number) cameraAttributes.getFocalZ().getValue()).doubleValue(),
                        Math.toRadians(((Number) cameraAttributes.getPitchDeg().getValue()).doubleValue()),
                        Math.toRadians(((Number) cameraAttributes.getYawDeg().getValue()).doubleValue()),
                        ((Number) cameraAttributes.getScale().getValue()).intValue(),
                        (com.creatorskit.swing.timesheet.keyframe.CameraEaseType) cameraAttributes.getEase().getSelectedItem(),
                        ((Number) cameraAttributes.getDurationTicks().getValue()).doubleValue()
                );
            case PULSE:
                return new com.creatorskit.swing.timesheet.keyframe.PulseKeyFrame(
                        tick,
                        pulseAttributes.getRgb(),
                        ((Number) pulseAttributes.getFadeInTicks().getValue()).doubleValue(),
                        ((Number) pulseAttributes.getHoldTicks().getValue()).doubleValue(),
                        ((Number) pulseAttributes.getFadeOutTicks().getValue()).doubleValue(),
                        (com.creatorskit.swing.timesheet.keyframe.PulseBlendMode) pulseAttributes.getBlendMode().getSelectedItem(),
                        pulseAttributes.getEaseInOut().getSelectedItem() == com.creatorskit.swing.timesheet.keyframe.settings.Toggle.ENABLE,
                        pulseAttributes.getAffectSpotAnims().getSelectedItem() == com.creatorskit.swing.timesheet.keyframe.settings.Toggle.ENABLE
                );
        }
    }

    private void setupMoveCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Movement");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("<html>Set how the Object moves. Hotkeys for adding and removing steps in the scene are as follows: " +
                "<br>" + config.addProgramStepHotkey().toString() + ": adds program steps to the hovered tile" +
                "<br>" + config.removeProgramStepHotkey().toString() + ": removes the last program step" +
                "<br>" + config.clearProgramStepHotkey().toString() + ": clears all steps for the current Movement KeyFrame</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel loopLabel = new JLabel("Loop: ");
        loopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        //card.add(loopLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JComboBox<Toggle> loop = movementAttributes.getLoop();
        loop.setToolTipText("Choose whether the program should loop its designated path");
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        loop.setBackground(ColorScheme.DARK_GRAY_COLOR);
        //card.add(loop, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel speedLabel = new JLabel("Speed: ");
        speedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(speedLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JSpinner speed = movementAttributes.getSpeed();
        speed.setToolTipText("Set the speed at which the Object moves, in tiles/tick");
        speed.setModel(new SpinnerNumberModel(1.0, 0.5, 10, 0.5));
        card.add(speed, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel turnRateLabel = new JLabel("Turn Rate: ");
        turnRateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(turnRateLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner turnRate = movementAttributes.getTurnRate();
        turnRate.setToolTipText("Determines the rate at which the Object rotates during movement in JUnits/clientTick");
        turnRate.setModel(new SpinnerNumberModel(OrientationKeyFrame.TURN_RATE, 0, 2048, 1));
        card.add(turnRate, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupAnimCard(JPanel card)
    {
        Dimension spinnerSize = new Dimension(90, 25);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel generalTitlePanel = new JPanel();
        generalTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(generalTitlePanel, c);

        JLabel generalTitle = new JLabel("General");
        generalTitle.setHorizontalAlignment(SwingConstants.LEFT);
        generalTitle.setFont(FontManager.getRunescapeBoldFont());
        generalTitlePanel.add(generalTitle);

        JLabel help = new JLabel(new ImageIcon(HELP));
        help.setHorizontalAlignment(SwingConstants.LEFT);
        help.setBorder(new EmptyBorder(0, 4, 0, 4));
        help.setToolTipText("<html>Pose Animations dynamically update your Object based on its current movement trajectory" +
                "<br>For example: an Object that isn't moving will use the given Idle animation; an Object taking a 90 degree right turn will use Walk Right animation." +
                "<br>Active Animations will instead override the current Pose Animation, playing regardless of the Object's movement trajectory</html>");
        generalTitlePanel.add(help);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel startFrameLabel = new JLabel("1st Frame: ");
        startFrameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(startFrameLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner startFrame = animAttributes.getStartFrame();
        startFrame.setToolTipText("Set the frame at which the animation starts at");
        startFrame.setModel(new SpinnerNumberModel(0, 0, 99999, 1));
        startFrame.setPreferredSize(spinnerSize);
        card.add(startFrame, c);

        c.gridx = 2;
        c.gridy = 1;
        JButton randomize = new JButton("Random");
        randomize.setToolTipText("Sets a random starting frame between 0 to the maximum number of frames for the animation that is currently playing");
        card.add(randomize, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel lastFrameLabel = new JLabel("Last Frame: ");
        lastFrameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(lastFrameLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JSpinner lastFrame = animAttributes.getLastFrame();
        lastFrame.setToolTipText("Upper bound of the play range. 0 (default) = use the animation's natural last frame. "
                + "When set, the animation plays frames [1st Frame, Last Frame] and -- if Loop is on -- wraps back to 1st Frame.");
        lastFrame.setModel(new SpinnerNumberModel(AnimationKeyFrame.LAST_FRAME_DISABLED, 0, 99999, 1));
        lastFrame.setPreferredSize(spinnerSize);
        card.add(lastFrame, c);

        c.gridx = 4;
        c.gridy = 2;
        JLabel pauseTicksLabel = new JLabel("Pause Ticks: ");
        pauseTicksLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(pauseTicksLabel, c);

        c.gridx = 5;
        c.gridy = 2;
        JSpinner pauseTicks = animAttributes.getPauseTicks();
        pauseTicks.setToolTipText("When Loop is on, hold the last frame this many client ticks before looping back to 1st Frame. 0 = no pause.");
        pauseTicks.setModel(new SpinnerNumberModel(0, 0, 99999, 1));
        card.add(pauseTicks, c);

        /*
        c.gridx = 0;
        c.gridy = 2;
        JLabel stallLabel = new JLabel("Stall: ");
        stallLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(stallLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JComboBox<Toggle> stall = animAttributes.getStall();
        stall.setFocusable(false);
        stall.addItem(Toggle.DISABLE);
        stall.addItem(Toggle.ENABLE);
        card.add(stall, c);

         */

        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 3;
        JLabel manualTitle = new JLabel("Active Animation");
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        card.add(manualTitle, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 4;
        JLabel manualLabel = new JLabel("Active: ");
        manualLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(manualLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner manual = animAttributes.getActive();
        manual.setToolTipText("Set the Active animation. This animation overrides the Pose animation, and should be used when performing an action like an attack or emote");
        manual.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        manual.setPreferredSize(spinnerSize);
        card.add(manual, c);

        c.gridx = 2;
        c.gridy = 4;
        JLabel loopLabel = new JLabel("Loop: ");
        loopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(loopLabel, c);

        c.gridx = 3;
        c.gridy = 4;
        JComboBox<Toggle> loop = animAttributes.getLoop();
        loop.setToolTipText("Sets whether the Active animation should loop until the next Animation KeyFrame");
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        card.add(loop, c);

        c.gridx = 4;
        c.gridy = 4;
        JLabel freezeLabel = new JLabel("Freeze: ");
        freezeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(freezeLabel, c);

        c.gridx = 5;
        c.gridy = 4;
        JComboBox<Toggle> freeze = animAttributes.getFreeze();
        freeze.setToolTipText("Set whether the animation should freeze on the frame indicated by 1st Frame");
        freeze.setFocusable(false);
        freeze.addItem(Toggle.DISABLE);
        freeze.addItem(Toggle.ENABLE);
        card.add(freeze, c);

        c.gridwidth = 1;
        c.gridx = 4;
        c.gridy = 1;
        JLabel speedLabel = new JLabel("Speed: ");
        speedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(speedLabel, c);

        c.gridx = 5;
        c.gridy = 1;
        JSpinner animSpeed = animAttributes.getSpeed();
        animSpeed.setToolTipText("Animation playback-rate multiplier. 1.0 = native speed, 2.0 = double speed, 0.5 = half speed. Range 0.25..4.0, 0.05 increments. Applies to both the Active and Pose animations on this keyframe.");
        animSpeed.setModel(new SpinnerNumberModel(AnimationKeyFrame.DEFAULT_SPEED, 0.25, 4.0, 0.05));
        card.add(animSpeed, c);

        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 5;
        JLabel smartTitle = new JLabel("Pose Animations");
        smartTitle.setFont(FontManager.getRunescapeBoldFont());
        card.add(smartTitle, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 6;
        JLabel idleLabel = new JLabel("Idle: ");
        idleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleLabel, c);

        c.gridx = 1;
        c.gridy = 6;
        JSpinner idle = animAttributes.getIdle();
        idle.setToolTipText("The animation to play while standing idly without moving");
        idle.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idle.setPreferredSize(spinnerSize);
        card.add(idle, c);

        c.gridx = 2;
        c.gridy = 6;
        JLabel walk180Label = new JLabel("Walk 180: ");
        walk180Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walk180Label, c);

        c.gridx = 3;
        c.gridy = 6;
        JSpinner walk180 = animAttributes.getWalk180();
        walk180.setToolTipText("The animation to play while moving and turning in a 180");
        walk180.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walk180.setPreferredSize(spinnerSize);
        card.add(walk180, c);

        c.gridx = 0;
        c.gridy = 7;
        JLabel walkLabel = new JLabel("Walk: ");
        walkLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkLabel, c);

        c.gridx = 1;
        c.gridy = 7;
        JSpinner walk = animAttributes.getWalk();
        walk.setToolTipText("The animation to play while walking");
        walk.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walk.setPreferredSize(spinnerSize);
        card.add(walk, c);

        c.gridx = 2;
        c.gridy = 7;
        JLabel walkRLabel = new JLabel("Walk Right: ");
        walkRLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkRLabel, c);

        c.gridx = 3;
        c.gridy = 7;
        JSpinner walkRight = animAttributes.getWalkRight();
        walkRight.setToolTipText("The animation to play while walking and rotating to the right");
        walkRight.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walkRight.setPreferredSize(spinnerSize);
        card.add(walkRight, c);

        c.gridx = 4;
        c.gridy = 7;
        JLabel idleRLabel = new JLabel("Idle Right: ");
        idleRLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleRLabel, c);

        c.gridx = 5;
        c.gridy = 7;
        JSpinner idleRight = animAttributes.getIdleRight();
        idleRight.setToolTipText("The animation to play while standing and rotating to the right");
        idleRight.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idleRight.setPreferredSize(spinnerSize);
        card.add(idleRight, c);

        c.gridx = 0;
        c.gridy = 8;
        JLabel runLabel = new JLabel("Run: ");
        runLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(runLabel, c);

        c.gridx = 1;
        c.gridy = 8;
        JSpinner run = animAttributes.getRun();
        run.setToolTipText("The animation to play while running");
        run.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        run.setPreferredSize(spinnerSize);
        card.add(run, c);

        c.gridx = 2;
        c.gridy = 8;
        JLabel walkLLabel = new JLabel("Walk Left: ");
        walkLLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(walkLLabel, c);

        c.gridx = 3;
        c.gridy = 8;
        JSpinner walkLeft = animAttributes.getWalkLeft();
        walkLeft.setToolTipText("The animation to play while walking and rotating to the left");
        walkLeft.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        walkLeft.setPreferredSize(spinnerSize);
        card.add(walkLeft, c);

        c.gridx = 4;
        c.gridy = 8;
        JLabel idleLLabel = new JLabel("Idle Left: ");
        idleLLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idleLLabel, c);

        c.gridx = 5;
        c.gridy = 8;
        JSpinner idleLeft = animAttributes.getIdleLeft();
        idleLeft.setToolTipText("The animation to play while standing and rotating to the left");
        idleLeft.setModel(new SpinnerNumberModel(-1, -1, 99999, 1));
        idleLeft.setPreferredSize(spinnerSize);
        card.add(idleLeft, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 11;
        JLabel npcSearcherLabel = new JLabel("NPC Presets: ");
        npcSearcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(npcSearcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 11;
        JTextField npcField = new JTextField("");
        npcField.setToolTipText("Search up different NPCs, and double click the name to apply all of its Pose animations");
        npcField.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(npcField, c);

        JPopupMenu npcPopup = new JPopupMenu("NPCs");
        JScrollPane npcScrollPane = new JScrollPane(npcTable);
        npcPopup.add(npcScrollPane);

        KeyListener npcListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = npcField.getText();
                npcTable.searchAndListEntries(text);
                npcPopup.setVisible(true);
                Point p = npcField.getLocationOnScreen();
                npcPopup.setLocation(new Point((int) p.getX() + npcField.getWidth(), (int) p.getY()));
            }
        };
        npcField.addKeyListener(npcListener);

        npcField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                npcPopup.setVisible(false);
            }
        });

        npcTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = npcTable.getSelectedObject();
                    if (o instanceof NPCData)
                    {
                        NPCData data = (NPCData) o;
                        idle.setValue(data.getStandingAnimation());
                        walk.setValue(data.getWalkingAnimation());
                        run.setValue(data.getRunAnimation());
                        walk180.setValue(data.getRotate180Animation());
                        walkRight.setValue(data.getRotateRightAnimation());
                        walkLeft.setValue(data.getRotateLeftAnimation());
                        idleRight.setValue(data.getIdleRotateRightAnimation());
                        idleLeft.setValue(data.getIdleRotateLeftAnimation());
                    }

                    npcPopup.setVisible(false);
                }
            }
        });

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

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 12;
        JLabel itemSearcherLabel = new JLabel("Weapon Presets: ");
        itemSearcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(itemSearcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 12;
        JTextField itemField = new JTextField("");
        itemField.setToolTipText("Search up all items, and double click the name to apply all of its Pose animations");
        itemField.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(itemField, c);

        JPopupMenu itemPopup = new JPopupMenu("Items");
        JScrollPane itemScrollPane = new JScrollPane(itemTable);
        itemPopup.add(itemScrollPane);

        KeyListener itemListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = itemField.getText();
                itemTable.searchAndListEntries(text);
                itemPopup.setVisible(true);
                Point p = itemField.getLocationOnScreen();
                itemPopup.setLocation(new Point((int) p.getX() + itemField.getWidth(), (int) p.getY()));
            }
        };
        itemField.addKeyListener(itemListener);

        itemField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                itemPopup.setVisible(false);
            }
        });

        itemTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = itemTable.getSelectedObject();
                    if (o instanceof ItemData)
                    {
                        ItemData data = (ItemData) o;
                        int itemId = data.getId();

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
                                    idle.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE));
                                    walk.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.WALK));
                                    run.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.RUN));
                                    walk180.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_180));
                                    walkRight.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_RIGHT));
                                    walkLeft.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.ROTATE_LEFT));
                                    idleRight.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE_ROTATE_RIGHT));
                                    idleLeft.setValue(WeaponAnimData.getAnimation(weaponAnim, PlayerAnimationType.IDLE_ROTATE_LEFT));
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
                            idle.setValue(-1);
                            walk.setValue(-1);
                            run.setValue(-1);
                            walk180.setValue(-1);
                            walkRight.setValue(-1);
                            walkLeft.setValue(-1);
                            idleRight.setValue(-1);
                            idleLeft.setValue(-1);
                        }
                    }

                    itemPopup.setVisible(false);
                }
            }
        });

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

        NPCData player = new NPCData(
                -1,
                "Player",
                new int[0],
                1,
                WeaponAnimData.IDLE_UNARMED,
                WeaponAnimData.WALK_UNARMED,
                WeaponAnimData.RUN_UNARMED,
                WeaponAnimData.IDLE_ROTATE_LEFT_UNARMED,
                WeaponAnimData.IDLE_ROTATE_RIGHT_UNARMED,
                WeaponAnimData.ROTATE_180,
                WeaponAnimData.ROTATE_LEFT,
                WeaponAnimData.ROTATE_RIGHT,
                1,
                1,
                new int[0],
                new int[0]);

        c.gridwidth = 2;
        c.gridx = 4;
        c.gridy = 12;
        JButton addPlayer = new JButton("Unarmed");
        addPlayer.setToolTipText("Apply all the default Pose animations for an unarmed player");
        addPlayer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        addPlayer.addActionListener(e ->
        {
            idle.setValue(player.getStandingAnimation());
            walk.setValue(player.getWalkingAnimation());
            run.setValue(player.getRunAnimation());
            walk180.setValue(player.getRotate180Animation());
            walkRight.setValue(player.getRotateRightAnimation());
            walkLeft.setValue(player.getRotateLeftAnimation());
            idleRight.setValue(player.getIdleRotateRightAnimation());
            idleLeft.setValue(player.getIdleRotateLeftAnimation());
        });
        card.add(addPlayer, c);

        randomize.addActionListener(e ->
        {
            Character character = timeSheetPanel.getSelectedCharacter();
            if (character == null)
            {
                return;
            }

            CKObject ckObject = character.getCkObject();

            clientThread.invokeLater(() ->
            {
                Animation[] animations = ckObject.getAnimations();
                int animId;
                Animation activeAnim = animations[0];
                Animation poseAnim = animations[1];

                if (activeAnim == null || activeAnim.getId() == -1)
                {
                    if (poseAnim == null || poseAnim.getId() == -1)
                    {
                        return;
                    }

                    animId = poseAnim.getId();
                }
                else
                {
                    animId = activeAnim.getId();
                }

                Animation animation = client.loadAnimation(animId);
                if (animation == null)
                {
                    return;
                }

                int frames = animation.getNumFrames();
                int randomFrame = random.nextInt(frames);
                startFrame.setValue(randomFrame);
            });
        });

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 13;
        JLabel animSearcherLabel = new JLabel("Animations: ");
        animSearcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(animSearcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 13;
        JTextField animField = new JTextField("");
        animField.setToolTipText("Search up all animations, and double click the name to apply it as the Active animation");
        animField.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(animField, c);

        JPopupMenu animPopup = new JPopupMenu("Animations");
        JScrollPane animScrollPane = new JScrollPane(animTable);
        animPopup.add(animScrollPane);

        KeyListener animListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = animField.getText();
                animTable.searchAndListEntries(text);
                animPopup.setVisible(true);
                Point p = animField.getLocationOnScreen();
                animPopup.setLocation(new Point((int) p.getX() + animField.getWidth(), (int) p.getY()));
            }
        };
        animField.addKeyListener(animListener);

        animField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                animPopup.setVisible(false);
            }
        });

        animTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = animTable.getSelectedObject();
                    if (o instanceof AnimData)
                    {
                        AnimData data = (AnimData) o;
                        manual.setValue(data.getId());

                    }

                    animPopup.setVisible(false);
                }
            }
        });

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

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupOriCard(JPanel card)
    {
        Dimension spinnerSize = new Dimension(90, 25);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Orientation");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("<html>Setting an Orientation keyframe allows you to take direct control of an Object's orientation" +
                "<br>Otherwise, the Object's orientation is instead based off of the direction of its movement" +
                "<br>Start is the orientation to set at the start of the keyframe, while End determines where the Object will eventually point" +
                "<br>Use Ctrl+[ on a tile to set that orientation, relative to the Object's current tile, as the Start" +
                "<br>Use Ctrl+] on a tile to set that orientation, relative to the Object's current tile, as the End</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel startLabel = new JLabel("Start Orientation: ");
        startLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(startLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner start = oriAttributes.getStart();
        start.setToolTipText("Set the starting orientation that will apply at the beginning of the KeyFrame");
        start.setModel(new SpinnerNumberModel(0, 0, 2048, 1));
        start.setPreferredSize(spinnerSize);
        card.add(start, c);

        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 1;
        JLabel endLabel = new JLabel("End Orientation: ");
        endLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(endLabel, c);

        c.gridx = 3;
        c.gridy = 1;
        JSpinner end = oriAttributes.getEnd();
        end.setToolTipText("Set the ending orientation that the KeyFrame will try to reach");
        end.setModel(new SpinnerNumberModel(0, 0, 2048, 1));
        end.setPreferredSize(spinnerSize);
        card.add(end, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JButton getStart = new JButton("Grab");
        getStart.setToolTipText("Grab the current orientation of the Object, and apply it as the Start");
        getStart.addActionListener(e ->
        {
            Character selectedCharacter = timeSheetPanel.getSelectedCharacter();
            if (selectedCharacter == null)
            {
                return;
            }

            CKObject ckObject = selectedCharacter.getCkObject();
            if (ckObject == null)
            {
                return;
            }

            start.setValue(ckObject.getOrientation());
        });
        card.add(getStart, c);

        c.gridwidth = 1;
        c.gridx = 3;
        c.gridy = 2;
        JButton getEnd = new JButton("Grab");
        getEnd.setToolTipText("Grab the current Orientation of the Object, and apply it as the End");
        getEnd.addActionListener(e ->
        {
            Character selectedCharacter = timeSheetPanel.getSelectedCharacter();
            if (selectedCharacter == null)
            {
                return;
            }

            CKObject ckObject = selectedCharacter.getCkObject();
            if (ckObject == null)
            {
                return;
            }

            end.setValue(ckObject.getOrientation());
        });
        card.add(getEnd, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel durationLabel = new JLabel("Duration: ");
        durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(durationLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner duration = oriAttributes.getDuration();
        duration.setToolTipText("<html>Set the duration for how long the Object will attempt to point towards its End orientation" +
                "<br>If the Object reaches the End orientation, it will remain in that state until the Duration is over, regardless of its movement trajectory</html>");
        duration.setModel(new SpinnerNumberModel(1.0, 0, TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH, 0.1));
        duration.setPreferredSize(spinnerSize);
        card.add(duration, c);

        c.gridwidth = 2;
        c.gridx = 2;
        c.gridy = 3;
        JButton calculate = new JButton("Calculate");
        calculate.setToolTipText("Calculates the exact duration based on the start and end orientation and the current turn rate");
        calculate.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(calculate, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 4;
        JLabel turnRateLabel = new JLabel("Turn Rate: ");
        turnRateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(turnRateLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner turnRate = oriAttributes.getTurnRate();
        turnRate.setToolTipText("Determines the rate at which the Object rotates in JUnits/clientTick");
        turnRate.setModel(new SpinnerNumberModel(OrientationKeyFrame.TURN_RATE, 0, 2048, 1));
        card.add(turnRate, c);

        // Face target row: while this orientation keyframe is active, the Object snaps
        // every tick to face the named Character (combat-style turn-to-target).
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 5;
        JLabel faceTargetLabel = new JLabel("Face target: ");
        faceTargetLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(faceTargetLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 5;
        JTextField faceTarget = oriAttributes.getTargetCharacterName();
        faceTarget.setPreferredSize(spinnerSize);
        card.add(faceTarget, c);
        c.gridwidth = 1;

        calculate.addActionListener(e ->
        {
            double turnDuration = calculateOrientationDuration((int) start.getValue(), (int) end.getValue(), (int) turnRate.getValue());
            duration.setValue(turnDuration);

        });

        c.gridx = 5;
        c.gridy = 5;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        JLabel compass = new JLabel(new ImageIcon(COMPASS));
        compass.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(compass, c);
    }

    public static double calculateOrientationDuration(int start, int end, double turnRate)
    {
        int difference = Orientation.subtract(end, start);
        double ticks = (double) difference / turnRate * Constants.CLIENT_TICK_LENGTH / Constants.GAME_TICK_LENGTH;
        int scale = (int) Math.pow(10, 1);
        return Math.abs(Math.ceil(ticks * scale) / scale);
    }

    private void setupSpawnCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Spawn");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set whether the object appears or not");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel spawnLabel = new JLabel("Spawn status: ");
        spawnLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(spawnLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JComboBox<Toggle> manualCheckbox = spawnAttributes.getSpawn();
        manualCheckbox.setToolTipText("Sets whether the Object is spawned or not");
        manualCheckbox.setFocusable(false);
        manualCheckbox.addItem(Toggle.ENABLE);
        manualCheckbox.addItem(Toggle.DISABLE);
        card.add(manualCheckbox, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    public void setupModelCard(JPanel card)
    {
        Dimension spinnerSize = new Dimension(90, 25);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Model");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("<html>Switch between using a 3D model based on the Model Id from the cache," +
                "<br>or a Custom Model that you've grabbed from the environment, found via the Cache Searcher, or created in the Model Anvil</html>");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel modelLabel = new JLabel("Model Type");
        modelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(modelLabel, c);

        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 1;
        JComboBox<ModelToggle> modelOverride = modelAttributes.getModelOverride();
        modelOverride.setToolTipText("Set whether to use a 3D model based on Model Id, or a Custom Model found via this plugin");
        modelOverride.setFocusable(false);
        modelOverride.addItem(ModelToggle.CUSTOM_MODEL);
        modelOverride.addItem(ModelToggle.MODEL_ID);
        card.add(modelOverride, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel customLabel = new JLabel("Custom Model: ");
        customLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(customLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<CustomModel> customComboBox = modelAttributes.getCustomModel();
        customComboBox.setToolTipText("The Custom Model to apply, if Model Type is set to Custom");
        customComboBox.setFocusable(false);
        card.add(customComboBox, c);

        c.gridx = 2;
        c.gridy = 2;
        JButton grab = new JButton("Grab");
        grab.setToolTipText("Grabs the original Custom Model and Radius set to the Object");
        card.add(grab, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel idLabel = new JLabel("Model Id: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner id = modelAttributes.getModelId();
        id.setToolTipText("The Model Id from the cache to apply, if Model Type is set to Model Id");
        id.setValue(-1);
        id.setPreferredSize(spinnerSize);
        card.add(id, c);

        c.gridx = 0;
        c.gridy = 4;
        JLabel radiusLabel = new JLabel("Radius: ");
        radiusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(radiusLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner radius = modelAttributes.getRadius();
        radius.setToolTipText("How far the Model should render vs clip with other tiles around it, measured in 1/128th tiles");
        radius.setValue(60);
        radius.setPreferredSize(spinnerSize);
        card.add(radius, c);

        grab.addActionListener(e ->
        {
            Character selectedCharacter = timeSheetPanel.getSelectedCharacter();
            if (selectedCharacter == null)
            {
                return;
            }

            customComboBox.setSelectedItem(selectedCharacter.getStoredModel());
            radius.setValue((int) selectedCharacter.getRadiusSpinner().getValue());
        });

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupTextCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Text");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the text to display over this Object's head");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel durationLabel = new JLabel("Duration: ");
        durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(durationLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JSpinner duration = textAttributes.getDuration();
        duration.setToolTipText("How long the text should render for");
        duration.setModel(new SpinnerNumberModel(5.0, 0, 1000000, 0.1));
        card.add(duration, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel textLabel = new JLabel("Overhead Text: ");
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(textLabel, c);

        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 2;
        JTextArea text = textAttributes.getText();
        text.setToolTipText("The text to show overhead");
        text.setText("");
        text.setLineWrap(true);
        card.add(text, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupOverheadCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Overhead");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the prayer and/or skull icon to display over this Object's head");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel skullLabel = new JLabel("Enable Skull: ");
        skullLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(skullLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JComboBox<OverheadSprite> toggleSkull = overheadAttributes.getSkullSprite();
        toggleSkull.setToolTipText("Set the skull icon to display overhead");
        toggleSkull.setFocusable(false);
        toggleSkull.addItem(OverheadSprite.NONE);
        toggleSkull.addItem(OverheadSprite.SKULL);
        toggleSkull.addItem(OverheadSprite.SKULL_HIGH_RISK);
        toggleSkull.addItem(OverheadSprite.SKULL_FIGHT_PITS);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_1);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_2);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_3);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_4);
        toggleSkull.addItem(OverheadSprite.SKULL_BH_5);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_1);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_2);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_3);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_4);
        toggleSkull.addItem(OverheadSprite.SKULL_FORINTHRY_5);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_1);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_2);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_3);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_4);
        toggleSkull.addItem(OverheadSprite.SKULL_DEADMAN_5);
        card.add(toggleSkull, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel textLabel = new JLabel("Overhead Icon: ");
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(textLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<OverheadSprite> spriteBox = overheadAttributes.getPrayerSprite();
        spriteBox.setToolTipText("Set the prayer icon to display overhead");
        spriteBox.setFocusable(false);
        spriteBox.addItem(OverheadSprite.NONE);
        spriteBox.addItem(OverheadSprite.PROTECT_MAGIC);
        spriteBox.addItem(OverheadSprite.PROTECT_RANGED);
        spriteBox.addItem(OverheadSprite.PROTECT_MELEE);
        spriteBox.addItem(OverheadSprite.REDEMPTION);
        spriteBox.addItem(OverheadSprite.RETRIBUTION);
        spriteBox.addItem(OverheadSprite.SMITE);
        spriteBox.addItem(OverheadSprite.PROTECT_RANGE_MAGE);
        spriteBox.addItem(OverheadSprite.PROTECT_RANGE_MELEE);
        spriteBox.addItem(OverheadSprite.PROTECT_MAGE_MELEE);
        spriteBox.addItem(OverheadSprite.PROTECT_RANGE_MAGE_MELEE);
        spriteBox.addItem(OverheadSprite.DEFLECT_MAGE);
        spriteBox.addItem(OverheadSprite.DEFLECT_RANGE);
        spriteBox.addItem(OverheadSprite.DEFLECT_MELEE);
        spriteBox.addItem(OverheadSprite.SOUL_SPLIT);
        spriteBox.addItem(OverheadSprite.WRATH);
        card.add(spriteBox, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupHealthCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("Healthbar");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the healthbar state for this Object. The amount of damage that will be shown is the Current health relative to the Maximum health");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel durationLabel = new JLabel("Duration: ");
        durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(durationLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JSpinner duration = healthAttributes.getDuration();
        duration.setToolTipText("Set how long the healthbar should appear for");
        duration.setModel(new SpinnerNumberModel(5.0, 0, 1000000, 1));
        card.add(duration, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel healthbarLabel = new JLabel("Healthbar Sprite: ");
        healthbarLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(healthbarLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        JComboBox<HealthbarSprite> healthbarSprite = healthAttributes.getHealthbarSprite();
        healthbarSprite.setToolTipText("Set the sprite for the healthbar to show");
        healthbarSprite.setFocusable(false);
        healthbarSprite.addItem(HealthbarSprite.DEFAULT);
        healthbarSprite.addItem(HealthbarSprite.BOSS_HEALTH);
        card.add(healthbarSprite, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 3;
        JLabel maxHealthLabel = new JLabel("Max Health: ");
        maxHealthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(maxHealthLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 3;
        JSpinner maxHealth = healthAttributes.getMaxHealth();
        maxHealth.setToolTipText("Set the Object's maximum health");
        maxHealth.setModel(new SpinnerNumberModel(99, 0, 99999, 1));
        maxHealth.setValue(99);
        card.add(maxHealth, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 4;
        JLabel currentHealthLabel = new JLabel("Current Health: ");
        currentHealthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(currentHealthLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 4;
        JSpinner currentHealth = healthAttributes.getCurrentHealth();
        currentHealth.setToolTipText("Set the Object's current health remaining");
        currentHealth.setModel(new SpinnerNumberModel(99, 0, 99999, 1));
        currentHealth.setValue(99);
        card.add(currentHealth, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 5;
        JLabel orderLabel = new JLabel("Stack Order: ");
        orderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(orderLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 5;
        JSpinner orderSpinner = healthAttributes.getOrder();
        orderSpinner.setToolTipText("Position in the HP/Shield/Special stack (0 = topmost, higher = lower). Default 0 keeps HP at the top.");
        orderSpinner.setModel(new SpinnerNumberModel(HealthKeyFrame.DEFAULT_ORDER, 0, 9, 1));
        card.add(orderSpinner, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 6;
        JLabel widthLabel = new JLabel("Width (0=auto): ");
        widthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(widthLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 6;
        JSpinner widthSpinner = healthAttributes.getWidth();
        widthSpinner.setToolTipText("Override the bar's pixel width. 0 auto-scales from Max Health. "
                + "Each bar has its own width -- HP / Shield / Special are sized independently.");
        widthSpinner.setModel(new SpinnerNumberModel(HealthKeyFrame.AUTO_WIDTH, 0, 500, 1));
        card.add(widthSpinner, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 7;
        JLabel syncHitsplatsLabel = new JLabel("Sync hitsplats: ");
        syncHitsplatsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(syncHitsplatsLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 7;
        JCheckBox syncHitsplatsCheck = healthAttributes.getSyncHitsplats();
        syncHitsplatsCheck.setToolTipText("<html>When ON (default), hitsplats whose sprite routes to the Health bar<br>"
                + "automatically create a follow-up Health keyframe at the same tick<br>"
                + "with their damage subtracted. Turn OFF to lock this Health bar<br>"
                + "against incoming hitsplats so the bar stays at its declared value.</html>");
        card.add(syncHitsplatsCheck, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 8;
        JLabel fadeInLabel = new JLabel("Fade in (ticks): ");
        fadeInLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(fadeInLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 8;
        JSpinner fadeInSpinner = healthAttributes.getFadeInTicks();
        fadeInSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1));
        fadeInSpinner.setToolTipText("<html><b>Boss healthbar only.</b><br>"
                + "Fade the bar in over this many ticks at the very start of the bar's lifecycle<br>"
                + "(= the earliest boss-health keyframe's tick). 0 = no fade, bar snaps in.<br>"
                + "Sync-created follow-up keyframes inherit this value, so hitsplats don't<br>"
                + "re-trigger the fade mid-fight.</html>");
        card.add(fadeInSpinner, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 9;
        JLabel fadeOutLabel = new JLabel("Fade out (ticks): ");
        fadeOutLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(fadeOutLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 9;
        JSpinner fadeOutSpinner = healthAttributes.getFadeOutTicks();
        fadeOutSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1));
        fadeOutSpinner.setToolTipText("<html><b>Boss healthbar only.</b><br>"
                + "Fade the bar out over this many ticks at the very end of the bar's lifecycle<br>"
                + "(= max tick+duration across every boss-health keyframe). 0 = no fade,<br>"
                + "bar snaps out.</html>");
        card.add(fadeOutSpinner, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);
    }

    private void setupSpotAnimCard(JPanel card, KeyFrameType spotAnimType)
    {
        SpotAnimAttributes spAttributes;
        if (spotAnimType == KeyFrameType.SPOTANIM)
        {
            spAttributes = spotAnimAttributes;
        }
        else
        {
            spAttributes = spotAnim2Attributes;
        }

        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel("SpotAnim");
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the SpotAnim (like spell effects) to play on the Object");
        manualTitlePanel.add(manualTitleHelp);

        c.weightx = 0;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel idLabel = new JLabel("SpotAnim: ");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(idLabel, c);

        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 1;
        JSpinner id = spAttributes.getSpotAnimId();
        id.setValue(-1);
        card.add(id, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        JLabel loop1Label = new JLabel("Loop: ");
        loop1Label.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(loop1Label, c);

        c.gridx = 1;
        c.gridy = 2;
        JComboBox<Toggle> loop = spAttributes.getLoop();
        loop.setToolTipText("Set whether the SpotAnim animation should loop");
        loop.setFocusable(false);
        loop.addItem(Toggle.DISABLE);
        loop.addItem(Toggle.ENABLE);
        card.add(loop, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel heightLabel = new JLabel("Height");
        heightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(heightLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner height = spAttributes.getHeight();
        height.setToolTipText("Sets the height at which the SpotAnim spawns");
        height.setModel(new SpinnerNumberModel(92, 0, 9999, 1));
        card.add(height, c);

        // Radius spinner -- mirrors Character.radiusSpinner semantics so users
        // can match the spotanim's clip radius to wide / tall models the same
        // way they would on a standalone Character placement.
        c.gridx = 0;
        c.gridy = 4;
        JLabel radiusLabel = new JLabel("Radius: ");
        radiusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(radiusLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner radius = spAttributes.getRadius();
        radius.setToolTipText("How far the SpotAnim should render vs clip with other tiles around it, measured in 1/128th tiles");
        radius.setModel(new SpinnerNumberModel(60, 0, 9999, 1));
        card.add(radius, c);

        c.gridx = 0;
        c.gridy = 5;
        JLabel searcherLabel = new JLabel("SpotAnims: ");
        searcherLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(searcherLabel, c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 5;
        JTextField spotanimField = new JTextField("");
        spotanimField.setToolTipText("Find all SpotAnims from the cache, and double click the name to apply its Id");
        spotanimField.setBackground(ColorScheme.DARK_GRAY_COLOR);
        card.add(spotanimField, c);

        KeyListener listener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                String text = spotanimField.getText();
                spotanimTable.searchAndListEntries(text);
                spotanimPopup.setVisible(true);
                Point p = spotanimField.getLocationOnScreen();
                spotanimPopup.setLocation(new Point((int) p.getX() + spotanimField.getWidth(), (int) p.getY()));
            }
        };
        spotanimField.addKeyListener(listener);

        spotanimField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent e)
            {

            }

            @Override
            public void focusLost(FocusEvent e)
            {
                spotanimPopup.setVisible(false);
            }
        });

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 2;
        c.gridy = 6;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 3;
        c.gridy = 7;
        JPanel duplicatePanel = new JPanel();
        duplicatePanel.setLayout(new GridLayout(0, 1, 2, 2));
        card.add(duplicatePanel, c);
        duplicatePanel.add(new JLabel("Duplicate To:"));

        if (spotAnimType == KeyFrameType.SPOTANIM)
        {
            JLabel emptySpotAnim = new JLabel("Spotanim 1");
            emptySpotAnim.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptySpotAnim);
        }
        else
        {
            JButton type1 = new JButton("SpotAnim 1");
            type1.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type1);
            type1.addActionListener(e -> timeSheetPanel.duplicateSpotanimKeyFrame(spotAnimType, KeyFrameType.SPOTANIM));
        }

        if (spotAnimType == KeyFrameType.SPOTANIM2)
        {
            JLabel emptySpotAnim = new JLabel("Spotanim 2");
            emptySpotAnim.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptySpotAnim);
        }
        else
        {
            JButton type2 = new JButton("SpotAnim 2");
            type2.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type2);
            type2.addActionListener(e -> timeSheetPanel.duplicateSpotanimKeyFrame(spotAnimType, KeyFrameType.SPOTANIM2));
        }
    }

    private void setupSpotAnimFinder()
    {
        spotanimTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);

                KeyFrameType spotAnimType = selectedKeyFramePage;
                if (selectedKeyFramePage != KeyFrameType.SPOTANIM && selectedKeyFramePage != KeyFrameType.SPOTANIM2)
                {
                    return;
                }

                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                {
                    Object o = spotanimTable.getSelectedObject();
                    if (o instanceof SpotanimData)
                    {
                        SpotanimData data = (SpotanimData) o;
                        JSpinner id;
                        if (spotAnimType == KeyFrameType.SPOTANIM)
                        {
                            id = spotAnimAttributes.getSpotAnimId();
                        }
                        else
                        {
                            id = spotAnim2Attributes.getSpotAnimId();
                        }

                        id.setValue(data.getId());
                    }

                    spotanimPopup.setVisible(false);
                }
            }
        });

        if (dataFinder.isDataLoaded(DataFinder.DataType.SPOTANIM))
        {
            List<SpotanimData> dataList = dataFinder.getSpotanimData();
            List<Object> list = new ArrayList<>(dataList);
            spotanimTable.initialize(list);
        }
        else
        {
            dataFinder.addLoadCallback(DataFinder.DataType.SPOTANIM, () ->
            {
                List<SpotanimData> dataList = dataFinder.getSpotanimData();
                List<Object> list = new ArrayList<>(dataList);
                spotanimTable.initialize(list);
            });
        }

        JScrollPane scrollPane = new JScrollPane(spotanimTable);
        spotanimPopup.add(scrollPane);
    }

    private void setupHitsplatCard(JPanel card, KeyFrameType hitsplatType)
    {
        HitsplatAttributes attributes;
        String name;

        switch (hitsplatType)
        {
            default:
            case HITSPLAT_1:
                attributes = hitsplat1Attributes;
                name = HITSPLAT_1_CARD;
                break;
            case HITSPLAT_2:
                attributes = hitsplat2Attributes;
                name = HITSPLAT_2_CARD;
                break;
            case HITSPLAT_3:
                attributes = hitsplat3Attributes;
                name = HITSPLAT_3_CARD;
                break;
            case HITSPLAT_4:
                attributes = hitsplat4Attributes;
                name = HITSPLAT_4_CARD;
        }

        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel manualTitlePanel = new JPanel();
        manualTitlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(manualTitlePanel, c);

        JLabel manualTitle = new JLabel(name);
        manualTitle.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitle.setFont(FontManager.getRunescapeBoldFont());
        manualTitlePanel.add(manualTitle);

        JLabel manualTitleHelp = new JLabel(new ImageIcon(HELP));
        manualTitleHelp.setHorizontalAlignment(SwingConstants.LEFT);
        manualTitleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        manualTitleHelp.setToolTipText("Set the hitsplat sprite and damage to overlay on the Object");
        manualTitlePanel.add(manualTitleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel durationLabel = new JLabel("Duration: ");
        durationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(durationLabel, c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner duration = attributes.getDuration();
        duration.setToolTipText("Set the duration, in game ticks, for how long the Hitsplat lasts. -1 sets it to default value, which is 5/3 ticks. Fractional values allowed (0.1 increments).");
        duration.setModel(new SpinnerNumberModel(-1.0, -1.0, 1000000.0, 0.1));
        card.add(duration, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel spriteLabel = new JLabel("Sprite: ");
        spriteLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(spriteLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        JComboBox<HitsplatSprite> sprite = attributes.getSprite();
        sprite.setToolTipText("Set the Hitsplat sprite to display");
        sprite.setFocusable(false);
        sprite.addItem(HitsplatSprite.BLOCK);
        sprite.addItem(HitsplatSprite.DAMAGE);
        sprite.addItem(HitsplatSprite.POISON);
        sprite.addItem(HitsplatSprite.VENOM);
        sprite.addItem(HitsplatSprite.HEAL);
        sprite.addItem(HitsplatSprite.SHIELD);
        sprite.addItem(HitsplatSprite.DISEASE);
        sprite.addItem(HitsplatSprite.FREEZE);
        sprite.addItem(HitsplatSprite.NO_KILL_CREDIT);
        sprite.addItem(HitsplatSprite.ARMOUR);
        sprite.addItem(HitsplatSprite.BURN);
        sprite.addItem(HitsplatSprite.BLEED);
        sprite.addItem(HitsplatSprite.CORRUPTION);
        sprite.addItem(HitsplatSprite.DOOM);
        sprite.addItem(HitsplatSprite.POISE);
        sprite.addItem(HitsplatSprite.PRAYER_DRAIN);
        sprite.addItem(HitsplatSprite.SANITY_DRAIN);
        sprite.addItem(HitsplatSprite.SANITY_RESTORE);
        sprite.addItem(HitsplatSprite.CHARGE_UP);
        sprite.addItem(HitsplatSprite.CHARGE_DOWN);
        card.add(sprite, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel variantLabel = new JLabel("Variant: ");
        variantLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(variantLabel, c);

        c.gridx = 1;
        c.gridy = 3;
        JComboBox<HitsplatVariant> variant = attributes.getVariant();
        variant.setToolTipText("Set the Hitsplat variant to display");
        variant.setFocusable(false);
        variant.addItem(HitsplatVariant.NORMAL);
        variant.addItem(HitsplatVariant.MAX);
        variant.addItem(HitsplatVariant.OTHER);
        card.add(variant, c);

        c.gridx = 0;
        c.gridy = 4;
        JLabel damageLabel = new JLabel("Damage: ");
        damageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(damageLabel, c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner damage = attributes.getDamage();
        damage.setToolTipText("Set the damage value to show on the hisplat. -1 will not render any damage number");
        damage.setModel(new SpinnerNumberModel(0, -1, 999, 1));
        card.add(damage, c);

        // The "Quick KeyFrame Hitsplat/Bar" button used to live at gridx=2 of
        // gridy=4. Removed because adding / editing a hitsplat now auto-syncs
        // the matching bar keyframe at the same tick (see TimeSheetPanel's
        // syncHealthFromHitsplats path). The user explicitly asked for that
        // button to go once the auto-sync covered its functionality.

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 3;
        c.gridy = 5;
        JLabel empty1 = new JLabel("");
        card.add(empty1, c);

        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 5;
        c.gridy = 7;
        JPanel duplicatePanel = new JPanel();
        duplicatePanel.setLayout(new GridLayout(0, 1, 2, 2));
        card.add(duplicatePanel, c);
        duplicatePanel.add(new JLabel("Duplicate To:"));

        if (hitsplatType == KeyFrameType.HITSPLAT_1)
        {
            JLabel emptyHitsplat = new JLabel("Hitsplat 1");
            emptyHitsplat.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptyHitsplat);
        }
        else
        {
            JButton type1 = new JButton("Hitsplat 1");
            type1.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type1);
            type1.addActionListener(e -> timeSheetPanel.duplicateHitsplatKeyFrame(hitsplatType, KeyFrameType.HITSPLAT_1));
        }

        if (hitsplatType == KeyFrameType.HITSPLAT_2)
        {
            JLabel emptyHitsplat = new JLabel("Hitsplat 2");
            emptyHitsplat.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptyHitsplat);
        }
        else
        {
            JButton type2 = new JButton("Hitsplat 2");
            type2.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type2);
            type2.addActionListener(e -> timeSheetPanel.duplicateHitsplatKeyFrame(hitsplatType, KeyFrameType.HITSPLAT_2));
        }

        if (hitsplatType == KeyFrameType.HITSPLAT_3)
        {
            JLabel emptyHitsplat = new JLabel("Hitsplat 3");
            emptyHitsplat.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptyHitsplat);
        }
        else
        {
            JButton type3 = new JButton("Hitsplat 3");
            type3.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type3);
            type3.addActionListener(e -> timeSheetPanel.duplicateHitsplatKeyFrame(hitsplatType, KeyFrameType.HITSPLAT_3));
        }

        if (hitsplatType == KeyFrameType.HITSPLAT_4)
        {
            JLabel emptyHitsplat = new JLabel("Hitsplat 4");
            emptyHitsplat.setHorizontalAlignment(SwingConstants.CENTER);
            duplicatePanel.add(emptyHitsplat);
        }
        else
        {
            JButton type4 = new JButton("Hitsplat 4");
            type4.setBackground(ColorScheme.DARK_GRAY_COLOR);
            duplicatePanel.add(type4);
            type4.addActionListener(e -> timeSheetPanel.duplicateHitsplatKeyFrame(hitsplatType, KeyFrameType.HITSPLAT_4));
        }
    }

    private void setupProjectileCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(titlePanel, c);

        JLabel title = new JLabel("Projectile");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title);

        JLabel titleHelp = new JLabel(new ImageIcon(HELP));
        titleHelp.setBorder(new EmptyBorder(0, 4, 0, 4));
        titleHelp.setToolTipText("<html>Fires an OSRS projectile from this Character's position toward the named target(s)."
                + "<br>Uses the game's native createProjectile API — arc / height / slope are interpolated"
                + "<br>by the engine just like real spells and arrows.<br>"
                + "<br>Target accepts: single name, comma-separated list, or \"folder:Foldername\".</html>");
        titlePanel.add(titleHelp);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        card.add(rightLabel("Projectile ID:"), c);

        c.gridx = 1;
        c.gridy = 1;
        JSpinner projectileId = projectileAttributes.getProjectileId();
        projectileId.setToolTipText("SpotanimID of the projectile graphic (e.g. 119 = fireball travel, 9 = iron arrow travel)");
        projectileId.setModel(new SpinnerNumberModel(ProjectileKeyFrame.DEFAULT_PROJECTILE_ID, -1, 99999, 1));
        card.add(projectileId, c);

        c.gridx = 0;
        c.gridy = 2;
        card.add(rightLabel("Target:"), c);

        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 2;
        JTextField target = projectileAttributes.getTarget();
        target.setToolTipText(titleHelp.getToolTipText());
        card.add(target, c);
        c.gridwidth = 1;

        c.gridx = 0;
        c.gridy = 3;
        card.add(rightLabel("Start Height:"), c);

        c.gridx = 1;
        c.gridy = 3;
        JSpinner startHeight = projectileAttributes.getStartHeight();
        startHeight.setToolTipText("Z height at the source tile when the projectile spawns. Range supports extreme values for bosses like Yama that shoot straight up.");
        startHeight.setModel(new SpinnerNumberModel(ProjectileKeyFrame.DEFAULT_START_HEIGHT, -100000, 100000, 1));
        card.add(startHeight, c);

        c.gridx = 2;
        c.gridy = 3;
        card.add(rightLabel("End Height:"), c);

        c.gridx = 3;
        c.gridy = 3;
        JSpinner endHeight = projectileAttributes.getEndHeight();
        endHeight.setToolTipText("Z height at the target tile when the projectile impacts");
        endHeight.setModel(new SpinnerNumberModel(ProjectileKeyFrame.DEFAULT_END_HEIGHT, -100000, 100000, 1));
        card.add(endHeight, c);

        c.gridx = 0;
        c.gridy = 4;
        card.add(rightLabel("Slope:"), c);

        c.gridx = 1;
        c.gridy = 4;
        JSpinner slope = projectileAttributes.getSlope();
        slope.setToolTipText("Arc magnitude — higher = taller arc at the midpoint. Default 15. Large values (10000+) produce the extreme up-and-back-down arcs used by bosses like Yama.");
        slope.setModel(new SpinnerNumberModel(ProjectileKeyFrame.DEFAULT_SLOPE, -100000, 100000, 1));
        card.add(slope, c);

        c.gridx = 2;
        c.gridy = 4;
        card.add(rightLabel("Start Pos:"), c);

        c.gridx = 3;
        c.gridy = 4;
        JSpinner startPos = projectileAttributes.getStartPos();
        startPos.setToolTipText("Offset from the source tile (game's internal start offset). Default 64.");
        startPos.setModel(new SpinnerNumberModel(ProjectileKeyFrame.DEFAULT_START_POS, -100000, 100000, 1));
        card.add(startPos, c);

        c.gridx = 0;
        c.gridy = 5;
        card.add(rightLabel("Duration:"), c);

        c.gridx = 1;
        c.gridy = 5;
        JSpinner duration = projectileAttributes.getDurationTicks();
        duration.setToolTipText("Game ticks the projectile takes to fly from source to target");
        duration.setModel(new SpinnerNumberModel(ProjectileKeyFrame.DEFAULT_DURATION, 0.1, 100, 0.1));
        card.add(duration, c);

        c.gridx = 2;
        c.gridy = 5;
        card.add(rightLabel("Start Delay:"), c);

        c.gridx = 3;
        c.gridy = 5;
        JSpinner startDelay = projectileAttributes.getStartDelayTicks();
        startDelay.setToolTipText("Game ticks of delay between this keyframe firing and the projectile actually spawning. Useful for syncing with cast animations.");
        startDelay.setModel(new SpinnerNumberModel(ProjectileKeyFrame.DEFAULT_START_DELAY, 0, 100, 0.1));
        card.add(startDelay, c);

        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 6;
        JCheckBox faceTrajectory = projectileAttributes.getFaceTrajectory();
        faceTrajectory.setToolTipText("<html>When enabled, the projectile model pitches to follow the trajectory:"
                + "<br>nose-up while ascending, nose-down while crashing back down."
                + "<br>Recommended for high-slope arcs (e.g. Yama's overhead barrage) where"
                + "<br>a fixed-pitch model looks unnatural at the top of the arc.</html>");
        card.add(faceTrajectory, c);
        c.gridwidth = 1;

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 4;
        c.gridy = 7;
        card.add(new JLabel(""), c);
    }

    /**
     * Shared layout for the Shield and Special bar cards. They have identical
     * fields (colour + duration + max + current) so a single method handles
     * both -- the only difference is which attributes object backs the
     * components.
     */
    private void setupBarCard(JPanel card, KeyFrameType type)
    {
        final boolean isShield = type == KeyFrameType.SHIELD;
        final String typeName = isShield ? "Shield" : "Special";
        final int defaultRgb = isShield ? ShieldAttributes.DEFAULT_RGB : SpecialAttributes.DEFAULT_RGB;

        final JSpinner duration;
        final JButton colour;
        final JSpinner maxValue;
        final JSpinner currentValue;
        final JSpinner orderSpinner;
        final JSpinner widthSpinner;
        if (isShield)
        {
            duration = shieldAttributes.getDuration();
            colour = shieldAttributes.getColour();
            maxValue = shieldAttributes.getMaxValue();
            currentValue = shieldAttributes.getCurrentValue();
            orderSpinner = shieldAttributes.getOrder();
            widthSpinner = shieldAttributes.getWidth();
        }
        else
        {
            duration = specialAttributes.getDuration();
            colour = specialAttributes.getColour();
            maxValue = specialAttributes.getMaxValue();
            currentValue = specialAttributes.getCurrentValue();
            orderSpinner = specialAttributes.getOrder();
            widthSpinner = specialAttributes.getWidth();
        }

        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(titlePanel, c);

        JLabel title = new JLabel(typeName + " Bar");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title);

        JLabel help = new JLabel(new ImageIcon(HELP));
        help.setBorder(new EmptyBorder(0, 4, 0, 4));
        help.setToolTipText("<html>Doom-of-Mokhaiotl-style overhead " + typeName.toLowerCase() + " bar."
                + "<br>Stacks above the HP bar (or replaces it when no HP bar is active)."
                + "<br>The fill colour is independent of HP -- pick whatever you like.</html>");
        titlePanel.add(help);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        card.add(rightLabel("Duration: "), c);

        c.gridx = 1;
        c.gridy = 1;
        duration.setToolTipText("How long the bar stays on screen (game ticks)");
        duration.setModel(new SpinnerNumberModel(ShieldAttributes.DEFAULT_DURATION, 0, 1000000, 1));
        card.add(duration, c);

        c.gridx = 0;
        c.gridy = 2;
        card.add(rightLabel("Colour: "), c);

        c.gridx = 1;
        c.gridy = 2;
        colour.setToolTipText("Click to pick the fill colour for this " + typeName.toLowerCase() + " bar");
        colour.setFocusable(false);
        colour.setPreferredSize(new Dimension(120, 26));
        colour.setOpaque(true);
        colour.setBackground(new Color(defaultRgb));
        if (isShield)
        {
            shieldAttributes.setRgb(defaultRgb);
        }
        else
        {
            specialAttributes.setRgb(defaultRgb);
        }
        colour.addActionListener(e ->
        {
            int currentRgb = isShield ? shieldAttributes.getRgb() : specialAttributes.getRgb();
            Color picked = JColorChooser.showDialog(this, typeName + " Bar Colour", new Color(currentRgb));
            if (picked != null)
            {
                int newRgb = picked.getRGB() & 0xFFFFFF;
                if (isShield)
                {
                    shieldAttributes.setRgb(newRgb);
                }
                else
                {
                    specialAttributes.setRgb(newRgb);
                }
                colour.setBackground(picked);
                colour.setBorder(BorderFactory.createLineBorder(
                        isShield ? shieldAttributes.getRed() : specialAttributes.getRed(), 2));
                // wireAutoUpdate skips JButton on purpose -- the colour
                // chooser action listener has to fire the auto-update itself
                // so the selected keyframe actually gets rewritten with the
                // new RGB. Without this the picked colour shows on the swatch
                // but the keyframe still carries the old value, and a save /
                // reload silently reverts to the old colour.
                fireAutoUpdate();
            }
        });
        card.add(colour, c);

        c.gridx = 0;
        c.gridy = 3;
        card.add(rightLabel("Max " + typeName + ": "), c);

        c.gridx = 1;
        c.gridy = 3;
        maxValue.setToolTipText("Maximum value the " + typeName.toLowerCase() + " bar can hold");
        maxValue.setModel(new SpinnerNumberModel(ShieldAttributes.DEFAULT_MAX, 0, 99999, 1));
        card.add(maxValue, c);

        c.gridx = 0;
        c.gridy = 4;
        card.add(rightLabel("Current " + typeName + ": "), c);

        c.gridx = 1;
        c.gridy = 4;
        currentValue.setToolTipText("Current value remaining; fill width = current / max");
        currentValue.setModel(new SpinnerNumberModel(ShieldAttributes.DEFAULT_MAX, 0, 99999, 1));
        card.add(currentValue, c);

        c.gridx = 0;
        c.gridy = 5;
        card.add(rightLabel("Stack Order: "), c);

        c.gridx = 1;
        c.gridy = 5;
        orderSpinner.setToolTipText("Position in the HP/Shield/Special stack (0 = topmost, higher = lower). "
                + "Defaults: HP=0, Shield=1, Special=2.");
        int defaultOrder = isShield ? ShieldKeyFrame.DEFAULT_ORDER : SpecialKeyFrame.DEFAULT_ORDER;
        orderSpinner.setModel(new SpinnerNumberModel(defaultOrder, 0, 9, 1));
        card.add(orderSpinner, c);

        c.gridx = 0;
        c.gridy = 6;
        card.add(rightLabel("Width (0=auto): "), c);

        c.gridx = 1;
        c.gridy = 6;
        widthSpinner.setToolTipText("Override the bar's pixel width. 0 auto-scales from Max " + typeName
                + ". Each bar (HP / Shield / Special) is sized independently.");
        int defaultWidth = isShield ? ShieldKeyFrame.AUTO_WIDTH : SpecialKeyFrame.AUTO_WIDTH;
        widthSpinner.setModel(new SpinnerNumberModel(defaultWidth, 0, 500, 1));
        card.add(widthSpinner, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        card.add(new JLabel(""), c);
    }

    private void setupScreenFadeCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(titlePanel, c);

        JLabel title = new JLabel("Screen Fade");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title);

        JLabel help = new JLabel(new ImageIcon(HELP));
        help.setBorder(new EmptyBorder(0, 4, 0, 4));
        help.setToolTipText("<html>Whisperer / Blackstone-Fragment style fullscreen tint with a circular cutout in the centre."
                + "<br>The effect is GLOBAL -- it doesn't matter which Character owns this keyframe; the fade"
                + "<br>covers the whole canvas. Park it on a 'scene controller' Character if you want it"
                + "<br>somewhere easy to find.</html>");
        titlePanel.add(help);

        JButton colour = screenFadeAttributes.getColour();
        JSpinner peakAlpha = screenFadeAttributes.getPeakAlpha();
        JSpinner ringRadius = screenFadeAttributes.getRingRadius();
        JSpinner ringFeather = screenFadeAttributes.getRingFeather();
        JSpinner fadeInTicks = screenFadeAttributes.getFadeInTicks();
        JSpinner holdTicks = screenFadeAttributes.getHoldTicks();
        JSpinner fadeOutTicks = screenFadeAttributes.getFadeOutTicks();

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        card.add(rightLabel("Colour: "), c);

        c.gridx = 1;
        c.gridy = 1;
        colour.setToolTipText("Click to pick the tint colour. Default is Whisperer purple.");
        colour.setFocusable(false);
        colour.setPreferredSize(new Dimension(120, 26));
        colour.setOpaque(true);
        colour.setBackground(new Color(ScreenFadeKeyFrame.DEFAULT_RGB));
        screenFadeAttributes.setRgb(ScreenFadeKeyFrame.DEFAULT_RGB);
        colour.addActionListener(e ->
        {
            Color picked = JColorChooser.showDialog(this, "Screen Fade Colour", new Color(screenFadeAttributes.getRgb()));
            if (picked != null)
            {
                int newRgb = picked.getRGB() & 0xFFFFFF;
                screenFadeAttributes.setRgb(newRgb);
                colour.setBackground(picked);
                colour.setBorder(BorderFactory.createLineBorder(screenFadeAttributes.getRed(), 2));
                // wireAutoUpdate skips JButton -- fire from here so the keyframe gets rewritten.
                fireAutoUpdate();
            }
        });
        card.add(colour, c);

        c.gridx = 2;
        c.gridy = 1;
        card.add(rightLabel("Peak Alpha: "), c);

        c.gridx = 3;
        c.gridy = 1;
        peakAlpha.setToolTipText("Peak opacity (0-255). Default 220.");
        peakAlpha.setModel(new SpinnerNumberModel(ScreenFadeKeyFrame.DEFAULT_PEAK_ALPHA, 0, 255, 1));
        card.add(peakAlpha, c);

        c.gridx = 0;
        c.gridy = 2;
        card.add(rightLabel("Ring Radius: "), c);

        c.gridx = 1;
        c.gridy = 2;
        ringRadius.setToolTipText("Centre cutout radius in screen pixels. Set to 0 for a uniform fade with no ring.");
        ringRadius.setModel(new SpinnerNumberModel(ScreenFadeKeyFrame.DEFAULT_RING_RADIUS, 0, 4096, 5));
        card.add(ringRadius, c);

        c.gridx = 2;
        c.gridy = 2;
        card.add(rightLabel("Ring Feather: "), c);

        c.gridx = 3;
        c.gridy = 2;
        ringFeather.setToolTipText("Soft-edge width of the ring boundary in screen pixels. 0 = hard edge.");
        ringFeather.setModel(new SpinnerNumberModel(ScreenFadeKeyFrame.DEFAULT_RING_FEATHER, 0, 1024, 5));
        card.add(ringFeather, c);

        c.gridx = 0;
        c.gridy = 3;
        card.add(rightLabel("Fade In Ticks: "), c);

        c.gridx = 1;
        c.gridy = 3;
        fadeInTicks.setToolTipText("Game ticks the fade takes to ramp from 0 to peak alpha");
        fadeInTicks.setModel(new SpinnerNumberModel(ScreenFadeKeyFrame.DEFAULT_FADE_IN, 0, 1000, 0.1));
        card.add(fadeInTicks, c);

        c.gridx = 2;
        c.gridy = 3;
        card.add(rightLabel("Hold Ticks: "), c);

        c.gridx = 3;
        c.gridy = 3;
        holdTicks.setToolTipText("Game ticks the fade holds at peak alpha before ramping down");
        holdTicks.setModel(new SpinnerNumberModel(ScreenFadeKeyFrame.DEFAULT_HOLD, 0, 1000, 0.1));
        card.add(holdTicks, c);

        c.gridx = 0;
        c.gridy = 4;
        card.add(rightLabel("Fade Out Ticks: "), c);

        c.gridx = 1;
        c.gridy = 4;
        fadeOutTicks.setToolTipText("Game ticks the fade takes to ramp from peak alpha back to 0");
        fadeOutTicks.setModel(new SpinnerNumberModel(ScreenFadeKeyFrame.DEFAULT_FADE_OUT, 0, 1000, 0.1));
        card.add(fadeOutTicks, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        card.add(new JLabel(""), c);
    }

    private void setupScreenShakeCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(titlePanel, c);

        JLabel title = new JLabel("Screen Shake");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title);

        JLabel help = new JLabel(new ImageIcon(HELP));
        help.setBorder(new EmptyBorder(0, 4, 0, 4));
        help.setToolTipText("<html>Global camera shake -- mirrors the in-game vibration during boss attacks"
                + "<br>like ToA Wardens. Camera-relative: horizontal amplitude is always screen"
                + "<br>left/right regardless of yaw, vertical is always screen up/down."
                + "<br>"
                + "<br>The shake is driven by multi-octave noise (sum of 3 sin waves at different"
                + "<br>frequencies and phases) for a chaotic feel, not a uniform sine. Snaps in at"
                + "<br>the keyframe tick and snaps out when the duration elapses -- no fade."
                + "<br>"
                + "<br>Requires free-camera mode (mode 1). Silently no-ops outside mode 1. If you're"
                + "<br>using the camera-lock feature, lock keeps the focal point on the character"
                + "<br>and shake adds the jitter on top -- both work together.</html>");
        titlePanel.add(help);

        JSpinner amplitudeHorizontal = screenShakeAttributes.getAmplitudeHorizontal();
        JSpinner amplitudeVertical = screenShakeAttributes.getAmplitudeVertical();
        JSpinner frequency = screenShakeAttributes.getFrequency();
        JSpinner durationTicks = screenShakeAttributes.getDurationTicks();

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        card.add(rightLabel("Horizontal Amp: "), c);

        c.gridx = 1;
        c.gridy = 1;
        amplitudeHorizontal.setToolTipText("Peak screen-horizontal jitter in scene units (1 tile = 128). "
                + "Small (5-15) reads as a vibration; large (50+) as a slam.");
        amplitudeHorizontal.setModel(new SpinnerNumberModel(ScreenShakeKeyFrame.DEFAULT_AMPLITUDE_HORIZONTAL, 0, 1024, 1));
        card.add(amplitudeHorizontal, c);

        c.gridx = 2;
        c.gridy = 1;
        card.add(rightLabel("Vertical Amp: "), c);

        c.gridx = 3;
        c.gridy = 1;
        amplitudeVertical.setToolTipText("Peak screen-vertical jitter in scene units. Set to 0 for purely horizontal shake.");
        amplitudeVertical.setModel(new SpinnerNumberModel(ScreenShakeKeyFrame.DEFAULT_AMPLITUDE_VERTICAL, 0, 1024, 1));
        card.add(amplitudeVertical, c);

        c.gridx = 0;
        c.gridy = 2;
        card.add(rightLabel("Frequency: "), c);

        c.gridx = 1;
        c.gridy = 2;
        frequency.setToolTipText("Base oscillation rate in cycles per game tick. Higher = tighter vibration; "
                + "lower = looser sway. Two harmonics ride on top so the result isn't a clean sine.");
        frequency.setModel(new SpinnerNumberModel(ScreenShakeKeyFrame.DEFAULT_FREQUENCY, 0.5, 50, 0.5));
        card.add(frequency, c);

        c.gridx = 2;
        c.gridy = 2;
        card.add(rightLabel("Duration: "), c);

        c.gridx = 3;
        c.gridy = 2;
        durationTicks.setToolTipText("How long the shake runs in game ticks. Starts and stops instantly -- no fade.");
        durationTicks.setModel(new SpinnerNumberModel(ScreenShakeKeyFrame.DEFAULT_DURATION, 0.1, 1000, 0.1));
        card.add(durationTicks, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        card.add(new JLabel(""), c);
    }

    /**
     * Camera keyframe card. Ports the field set from mlgudi/keyframe-camera --
     * focal point (X/Y/Z in 1/128 scene units), pitch + yaw (DEGREES in the UI,
     * radians internally), engine zoom scale, easing curve, and duration to
     * the next keyframe. "Capture from camera" reads the live free-cam state
     * and writes it into the spinners so the user can fly to a shot and snap
     * the keyframe into place rather than typing numbers.
     */
    private void setupCameraCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(titlePanel, c);

        JLabel title = new JLabel("Camera");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title);

        JLabel help = new JLabel(new ImageIcon(HELP));
        help.setBorder(new EmptyBorder(0, 4, 0, 4));
        help.setToolTipText("<html>Global free-cam keyframe (ported from mlgudi/keyframe-camera)."
                + "<br>Focal point + pitch/yaw + zoom captured at this tick; the renderer"
                + "<br>interpolates between consecutive camera keyframes using the chosen ease."
                + "<br>"
                + "<br>Requires free-camera mode -- the renderer flips into mode 1 automatically"
                + "<br>when a camera keyframe is active and back when there are none."
                + "<br>"
                + "<br>Capture from camera reads the current OSRS camera state and writes it"
                + "<br>into the fields. Pan around with the in-game free-cam controls until the"
                + "<br>shot looks right, then capture.</html>");
        titlePanel.add(help);

        JSpinner focalX = cameraAttributes.getFocalX();
        JSpinner focalY = cameraAttributes.getFocalY();
        JSpinner focalZ = cameraAttributes.getFocalZ();
        JSpinner pitchDeg = cameraAttributes.getPitchDeg();
        JSpinner yawDeg = cameraAttributes.getYawDeg();
        JSpinner scale = cameraAttributes.getScale();
        JSpinner durationTicks = cameraAttributes.getDurationTicks();
        JComboBox<com.creatorskit.swing.timesheet.keyframe.CameraEaseType> ease = cameraAttributes.getEase();
        JButton capture = cameraAttributes.getCapture();

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        card.add(rightLabel("Focal X: "), c);

        c.gridx = 1;
        c.gridy = 1;
        focalX.setToolTipText("Camera focal point X in 1/128 scene units. Capture from camera to fill.");
        focalX.setModel(new SpinnerNumberModel(com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame.DEFAULT_FOCAL_X, -1000000d, 1000000d, 1d));
        card.add(focalX, c);

        c.gridx = 2;
        c.gridy = 1;
        card.add(rightLabel("Focal Y: "), c);

        c.gridx = 3;
        c.gridy = 1;
        focalY.setToolTipText("Camera focal point Y (vertical). Capture from camera to fill.");
        focalY.setModel(new SpinnerNumberModel(com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame.DEFAULT_FOCAL_Y, -1000000d, 1000000d, 1d));
        card.add(focalY, c);

        c.gridx = 0;
        c.gridy = 2;
        card.add(rightLabel("Focal Z: "), c);

        c.gridx = 1;
        c.gridy = 2;
        focalZ.setToolTipText("Camera focal point Z in 1/128 scene units. Capture from camera to fill.");
        focalZ.setModel(new SpinnerNumberModel(com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame.DEFAULT_FOCAL_Z, -1000000d, 1000000d, 1d));
        card.add(focalZ, c);

        c.gridx = 2;
        c.gridy = 2;
        card.add(rightLabel("Zoom: "), c);

        c.gridx = 3;
        c.gridy = 2;
        scale.setToolTipText("Engine zoom scale (CAMERA_ZOOM_FIXED_VIEWPORT). Higher = closer.");
        scale.setModel(new SpinnerNumberModel(com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame.DEFAULT_SCALE, 100, 1024, 16));
        card.add(scale, c);

        c.gridx = 0;
        c.gridy = 3;
        card.add(rightLabel("Pitch (deg): "), c);

        c.gridx = 1;
        c.gridy = 3;
        pitchDeg.setToolTipText("Camera pitch in degrees. 0 = looking horizontally; positive = looking down.");
        pitchDeg.setModel(new SpinnerNumberModel(0d, -180d, 180d, 1d));
        card.add(pitchDeg, c);

        c.gridx = 2;
        c.gridy = 3;
        card.add(rightLabel("Yaw (deg): "), c);

        c.gridx = 3;
        c.gridy = 3;
        yawDeg.setToolTipText("Camera yaw in degrees. 0 = facing north.");
        yawDeg.setModel(new SpinnerNumberModel(0d, -360d, 720d, 1d));
        card.add(yawDeg, c);

        // Duration intentionally omitted from the card. Segment length is now
        // implicit from (next.tick - this.tick) and the last keyframe holds its
        // values indefinitely. The spinner instance still exists on
        // CameraAttributes for the auto-update wiring + save format, but the
        // field is not displayed -- still backed by DEFAULT_DURATION so old
        // saves round-trip cleanly.
        durationTicks.setModel(new SpinnerNumberModel(com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame.DEFAULT_DURATION, 0.1, 1000d, 0.1));

        c.gridx = 0;
        c.gridy = 4;
        card.add(rightLabel("Ease: "), c);

        c.gridx = 1;
        c.gridy = 4;
        ease.setToolTipText("Easing curve from this keyframe to the next.");
        card.add(ease, c);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 4;
        capture.setToolTipText("Snap the current OSRS free-cam state into the spinners above.");
        capture.addActionListener(e -> captureLiveCameraIntoSpinners());
        card.add(capture, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        card.add(new JLabel(""), c);
    }

    private void setupPulseCard(JPanel card)
    {
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(4, 4, 4, 4));
        card.setFocusable(true);
        addMouseFocusListener(card);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);

        c.gridwidth = 4;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        card.add(titlePanel, c);

        JLabel title = new JLabel("Pulse");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title);

        JLabel help = new JLabel(new ImageIcon(HELP));
        help.setBorder(new EmptyBorder(0, 4, 0, 4));
        help.setToolTipText("<html>Temporarily recolours the Character's current model."
                + "<br>The underlying model is whatever the Character is rendering -- typically"
                + "<br>the nearest preceding Model keyframe, or the base model if none."
                + "<br>"
                + "<br>Envelope = fadeIn + hold + fadeOut (all in ticks). Inside the envelope,"
                + "<br>the chosen colour is blended into each face's original HSL by blend mode:"
                + "<br>&nbsp;&nbsp;Add - hit-flash / glow (brightens toward the colour)"
                + "<br>&nbsp;&nbsp;Multiply - damage tint (darkens toward the colour)"
                + "<br>&nbsp;&nbsp;Replace - wash (face becomes the colour at peak)"
                + "<br>"
                + "<br>Outside the envelope, the model is left alone. Pulses snapshot the"
                + "<br>original face colours on activation and restore them on the way out.</html>");
        titlePanel.add(help);

        JButton colour = pulseAttributes.getColour();
        JSpinner fadeIn = pulseAttributes.getFadeInTicks();
        JSpinner hold = pulseAttributes.getHoldTicks();
        JSpinner fadeOut = pulseAttributes.getFadeOutTicks();
        JComboBox<com.creatorskit.swing.timesheet.keyframe.PulseBlendMode> blendMode = pulseAttributes.getBlendMode();
        JComboBox<com.creatorskit.swing.timesheet.keyframe.settings.Toggle> easeInOut = pulseAttributes.getEaseInOut();
        JComboBox<com.creatorskit.swing.timesheet.keyframe.settings.Toggle> affectSpotAnims = pulseAttributes.getAffectSpotAnims();

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        card.add(rightLabel("Colour: "), c);

        c.gridx = 1;
        c.gridy = 1;
        colour.setToolTipText("Click to pick the pulse target colour");
        colour.setFocusable(false);
        colour.setPreferredSize(new Dimension(120, 26));
        colour.setOpaque(true);
        colour.setBackground(new Color(com.creatorskit.swing.timesheet.attributes.PulseAttributes.DEFAULT_RGB));
        pulseAttributes.setRgb(com.creatorskit.swing.timesheet.attributes.PulseAttributes.DEFAULT_RGB);
        colour.addActionListener(e ->
        {
            Color picked = JColorChooser.showDialog(this, "Pulse Colour", new Color(pulseAttributes.getRgb()));
            if (picked != null)
            {
                int newRgb = picked.getRGB() & 0xFFFFFF;
                pulseAttributes.setRgb(newRgb);
                colour.setBackground(picked);
                colour.setBorder(BorderFactory.createLineBorder(pulseAttributes.getRed(), 2));
                // wireAutoUpdate skips JButton on purpose -- the colour
                // chooser action listener has to fire the auto-update itself
                // so the selected Pulse keyframe actually gets rewritten with
                // the new RGB. Without this the picked colour shows on the
                // swatch but the keyframe still carries the old value, and
                // playback keeps using the previous tint.
                fireAutoUpdate();
            }
        });
        card.add(colour, c);

        c.gridx = 0;
        c.gridy = 2;
        card.add(rightLabel("Fade In: "), c);

        c.gridx = 1;
        c.gridy = 2;
        fadeIn.setToolTipText("Ticks to ramp the blend factor from 0 to 1 (0 = instant on)");
        fadeIn.setModel(new SpinnerNumberModel(com.creatorskit.swing.timesheet.attributes.PulseAttributes.DEFAULT_FADE_IN, 0d, 1000000d, 0.5));
        card.add(fadeIn, c);

        c.gridx = 0;
        c.gridy = 3;
        card.add(rightLabel("Hold: "), c);

        c.gridx = 1;
        c.gridy = 3;
        hold.setToolTipText("Ticks the blend factor stays at peak");
        hold.setModel(new SpinnerNumberModel(com.creatorskit.swing.timesheet.attributes.PulseAttributes.DEFAULT_HOLD, 0d, 1000000d, 0.5));
        card.add(hold, c);

        c.gridx = 0;
        c.gridy = 4;
        card.add(rightLabel("Fade Out: "), c);

        c.gridx = 1;
        c.gridy = 4;
        fadeOut.setToolTipText("Ticks to ramp the blend factor from 1 back to 0 (0 = instant off)");
        fadeOut.setModel(new SpinnerNumberModel(com.creatorskit.swing.timesheet.attributes.PulseAttributes.DEFAULT_FADE_OUT, 0d, 1000000d, 0.5));
        card.add(fadeOut, c);

        c.gridx = 0;
        c.gridy = 5;
        card.add(rightLabel("Blend: "), c);

        c.gridx = 1;
        c.gridy = 5;
        blendMode.setToolTipText("How the pulse colour combines with the model's original face colours");
        blendMode.setFocusable(false);
        blendMode.removeAllItems();
        for (com.creatorskit.swing.timesheet.keyframe.PulseBlendMode m : com.creatorskit.swing.timesheet.keyframe.PulseBlendMode.values())
        {
            blendMode.addItem(m);
        }
        blendMode.setSelectedItem(com.creatorskit.swing.timesheet.attributes.PulseAttributes.DEFAULT_BLEND_MODE);
        card.add(blendMode, c);

        c.gridx = 0;
        c.gridy = 6;
        card.add(rightLabel("Ease in/out: "), c);

        c.gridx = 1;
        c.gridy = 6;
        easeInOut.setToolTipText("Smoothstep the fade ramps (organic) vs linear ramps (mechanical)");
        easeInOut.setFocusable(false);
        easeInOut.removeAllItems();
        easeInOut.addItem(com.creatorskit.swing.timesheet.keyframe.settings.Toggle.DISABLE);
        easeInOut.addItem(com.creatorskit.swing.timesheet.keyframe.settings.Toggle.ENABLE);
        card.add(easeInOut, c);

        c.gridx = 0;
        c.gridy = 7;
        card.add(rightLabel("Affect SpotAnims: "), c);

        c.gridx = 1;
        c.gridy = 7;
        affectSpotAnims.setToolTipText("Also tint the Character's SpotAnim 1 / 2 CKObjects when the pulse is active");
        affectSpotAnims.setFocusable(false);
        affectSpotAnims.removeAllItems();
        affectSpotAnims.addItem(com.creatorskit.swing.timesheet.keyframe.settings.Toggle.DISABLE);
        affectSpotAnims.addItem(com.creatorskit.swing.timesheet.keyframe.settings.Toggle.ENABLE);
        card.add(affectSpotAnims, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 8;
        c.gridy = 15;
        card.add(new JLabel(""), c);
    }

    private JLabel rightLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    public void switchCards(String cardName)
    {
        KeyFrameType type;
        switch (cardName)
        {
            default:
            case MOVE_CARD:
                type = KeyFrameType.MOVEMENT;
                break;
            case ANIM_CARD:
                type = KeyFrameType.ANIMATION;
                break;
            case ORI_CARD:
                type = KeyFrameType.ORIENTATION;
                break;
            case SPAWN_CARD:
                type = KeyFrameType.SPAWN;
                break;
            case MODEL_CARD:
                type = KeyFrameType.MODEL;
                break;
            case TEXT_CARD:
                type = KeyFrameType.TEXT;
                break;
            case OVER_CARD:
                type = KeyFrameType.OVERHEAD;
                break;
            case HEALTH_CARD:
                type = KeyFrameType.HEALTH;
                break;
            case SPOTANIM_CARD:
                type = KeyFrameType.SPOTANIM;
                break;
            case SPOTANIM2_CARD:
                type = KeyFrameType.SPOTANIM2;
                break;
            case HITSPLAT_1_CARD:
                type = KeyFrameType.HITSPLAT_1;
                break;
            case HITSPLAT_2_CARD:
                type = KeyFrameType.HITSPLAT_2;
                break;
            case HITSPLAT_3_CARD:
                type = KeyFrameType.HITSPLAT_3;
                break;
            case HITSPLAT_4_CARD:
                type = KeyFrameType.HITSPLAT_4;
                break;
            case PROJECTILE_CARD:
                type = KeyFrameType.PROJECTILE;
                break;
            case SHIELD_CARD:
                type = KeyFrameType.SHIELD;
                break;
            case SPECIAL_CARD:
                type = KeyFrameType.SPECIAL;
                break;
            case SCREEN_FADE_CARD:
                type = KeyFrameType.SCREEN_FADE;
                break;
            case SCREEN_SHAKE_CARD:
                type = KeyFrameType.SCREEN_SHAKE;
                break;
            case CAMERA_CARD:
                type = KeyFrameType.CAMERA;
                break;
            case PULSE_CARD:
                type = KeyFrameType.PULSE;
        }

        switchCards(type);
    }

    public void switchCards(KeyFrameType type)
    {
        selectedKeyFramePage = type;
        String cardName = selectedKeyFramePage.getName();
        activeCard = cardName;
        cardLabel.setText(cardName);

        // Pick the right labels column based on whether the requested type is
        // local or global. Each column maintains its own alphabetical order
        // so the row index must be looked up against the corresponding array.
        boolean isGlobal = KeyFrameType.isGlobal(selectedKeyFramePage);
        JLabel[] labels = isGlobal ? timeSheetPanel.getGlobalLabels() : timeSheetPanel.getLabels();
        int displayIdx = isGlobal
                ? KeyFrameType.getGlobalDisplayIndex(selectedKeyFramePage)
                : KeyFrameType.getLocalDisplayIndex(selectedKeyFramePage);
        JLabel selectedLabel = (displayIdx >= 0 && displayIdx + 1 < labels.length)
                ? labels[displayIdx + 1]
                : labels[1];
        TimeSheet sheet = isGlobal
                ? timeSheetPanel.getGlobalAttributeSheet()
                : timeSheetPanel.getAttributeSheet();
        for (int f = 0; f < labels.length; f++)
        {
            JLabel label = labels[f];
            if (label == selectedLabel)
            {
                if (sheet != null) sheet.setSelectedIndex(f);
                label.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            }
            else
            {
                label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        }

        Character character = timeSheetPanel.getSelectedCharacter();
        double currentTick = timeSheetPanel.getCurrentTime();
        if (character == null)
        {
            setKeyFramedIcon(false);
            resetAttributes(null, currentTick);
            // Re-evaluate placeholder vs card visibility after the type change.
            // resetAttributes does the spinner-load but only refreshKeyFrameSelectionState
            // routes the CardLayout to the right card (active vs NO_SELECTION_CARD).
            refreshKeyFrameSelectionState();
            return;
        }

        KeyFrame keyFrame = character.findKeyFrame(selectedKeyFramePage, currentTick);
        setKeyFramedIcon(keyFrame != null);
        resetAttributes(character, currentTick);
        refreshKeyFrameSelectionState();
    }

    public void setSelectedCharacter(Character character)
    {
        double tick = timeSheetPanel.getCurrentTime();
        updateObjectLabel(character);

        if (character == null)
        {
            setKeyFramedIcon(false);
            resetAttributes(null, tick);
            refreshKeyFrameSelectionState();
            return;
        }

        KeyFrame keyFrame = character.findKeyFrame(selectedKeyFramePage, tick);
        setKeyFramedIcon(keyFrame != null);
        resetAttributes(character, tick);
        refreshKeyFrameSelectionState();
    }

    /**
     * Returns the first user-selected keyframe whose type matches the panel's
     * current page (selectedKeyFramePage). Used so the panel always reflects the
     * keyframe the user clicked, not the one under the seeker bar.
     */
    private KeyFrame findSelectedKeyFrameOfCurrentType()
    {
        if (timeSheetPanel == null)
        {
            return null;
        }
        KeyFrame[] selected = timeSheetPanel.getSelectedKeyFrames();
        for (KeyFrame kf : selected)
        {
            if (kf != null && kf.getKeyFrameType() == selectedKeyFramePage)
            {
                return kf;
            }
        }
        return null;
    }

    /**
     * Reflects the current keyframe selection in the panel: when multiple keyframe
     * TYPES are selected (e.g. Movement + Animation across the marquee), swap the
     * card editor for an empty placeholder and disable Update/Reset/keyframe-icon.
     * The placeholder card preserves the cardPanel's height so the panel doesn't
     * collapse vertically.
     */
    public void refreshKeyFrameSelectionState()
    {
        if (timeSheetPanel == null)
        {
            return;
        }
        KeyFrame[] selected = timeSheetPanel.getSelectedKeyFrames();
        java.util.EnumSet<KeyFrameType> types = java.util.EnumSet.noneOf(KeyFrameType.class);
        for (KeyFrame kf : selected)
        {
            if (kf != null)
            {
                types.add(kf.getKeyFrameType());
            }
        }
        boolean mixedTypes = types.size() > 1;
        // No keyframe of the current card's type is selected -- show the placeholder
        // so the user can't enter values that would silently rewrite the previous-at-
        // seeker keyframe via the auto-update path. The "+" keyframe icon button
        // stays enabled so adding a new keyframe at the seeker still works.
        boolean noSelectionForCurrentType = !mixedTypes
                && findSelectedKeyFrameOfCurrentType() == null;

        CardLayout cl = (CardLayout) cardPanel.getLayout();
        if (mixedTypes)
        {
            cl.show(cardPanel, MIXED_TYPES_CARD);
            cardLabel.setText("Mixed types");
        }
        else if (noSelectionForCurrentType)
        {
            cl.show(cardPanel, NO_SELECTION_CARD);
            cardLabel.setText(activeCard);
        }
        else
        {
            cl.show(cardPanel, activeCard);
            cardLabel.setText(activeCard);
        }
        boolean editable = !mixedTypes && !noSelectionForCurrentType;
        updateButton.setEnabled(editable);
        resetButton.setEnabled(editable);
        // The "+" / keyframe icon is a CREATE button -- disabled when the
        // marquee mixes types (ambiguous which to add) OR when every
        // selected target already has a keyframe at the current tick for
        // the current page (nothing to add). The all-have check covers
        // both per-Character types (every selected Character has one) and
        // global types (the central store has one at this tick).
        refreshAddKeyFrameEnabled(mixedTypes);

        // Keep the object label in sync with the marquee count -- updateObjectLabel
        // reads timeSheetPanel.getSelectedKeyFrames() to compose the
        // "X Keyframes across N Objects" string, so it needs to re-run whenever
        // selectedKeyFrames changes (which is exactly when this method is called
        // by TimeSheetPanel.setSelectedKeyFrames).
        updateObjectLabel(timeSheetPanel.getSelectedCharacter());

        revalidate();
        repaint();
    }

    public void updateObjectLabel(Character character)
    {
        int selectionSize = selectionManager == null ? 0 : selectionManager.size();
        // Surface the marquee count too when multi-select is active. The Tools
        // > Random ops (Jitter / Scatter) and the Update button all operate on
        // the marquee, so the user wants to know "how many keyframes is this
        // operation going to touch" at a glance, in addition to the Character
        // count. Falls back to the plain "N Objects Selected" when there's no
        // marquee (e.g. the user just folder-clicked to multi-select but
        // hasn't marqueed any keyframes yet).
        int keyFrameCount = 0;
        if (timeSheetPanel != null)
        {
            KeyFrame[] selectedKfs = timeSheetPanel.getSelectedKeyFrames();
            if (selectedKfs != null) keyFrameCount = selectedKfs.length;
        }

        if (selectionSize > 1)
        {
            objectLabel.setForeground(ColorScheme.BRAND_ORANGE);
            // Use the smaller multi-select font + a <br> after "across" so the
            // string doesn't blow past the panel width. Same JLabel; HTML mode
            // gets engaged via the <html> wrapper, plain text takes the
            // single-character path below.
            objectLabel.setFont(multiSelectFont);
            if (keyFrameCount > 0)
            {
                objectLabel.setText("<html>[" + keyFrameCount + " Keyframes across<br>"
                        + selectionSize + " Objects Selected]</html>");
            }
            else
            {
                objectLabel.setText("[" + selectionSize + " Objects Selected]");
            }
            return;
        }

        if (character == null)
        {
            objectLabel.setForeground(Color.WHITE);
            objectLabel.setFont(attributeFont);
            objectLabel.setText(NO_OBJECT_SELECTED);
            return;
        }

        objectLabel.setForeground(ColorScheme.BRAND_ORANGE);
        objectLabel.setFont(attributeFont);
        StringBuilder name = new StringBuilder(character.getName());

        FontMetrics metrics = objectLabel.getFontMetrics(attributeFont);
        int maxWidth = 275;
        while (metrics.stringWidth(name.toString()) > maxWidth)
        {
            name = name.deleteCharAt(name.length() - 1);
        }

        objectLabel.setText(name.toString());
    }

    private void setupKeyListeners()
    {
        for (JComponent c : movementAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.MOVEMENT);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.MOVEMENT);
        }

        for (JComponent c : animAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.ANIMATION);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.ANIMATION);
        }

        for (JComponent c : oriAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.ORIENTATION);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.ORIENTATION);
        }

        for (JComponent c : spawnAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.SPAWN);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.SPAWN);
        }

        for (JComponent c : modelAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.MODEL);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.MODEL);
        }

        for (JComponent c : textAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.TEXT);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.TEXT);
        }

        for (JComponent c : overheadAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.OVERHEAD);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.OVERHEAD);
        }

        for (JComponent c : healthAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HEALTH);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HEALTH);
        }

        for (JComponent c : spotAnimAttributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.SPOTANIM);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.SPOTANIM);
        }

        for (JComponent c : spotAnim2Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.SPOTANIM2);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.SPOTANIM2);
        }

        for (JComponent c : hitsplat1Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HITSPLAT_1);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HITSPLAT_1);
        }


        for (JComponent c : hitsplat2Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HITSPLAT_2);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HITSPLAT_2);
        }


        for (JComponent c : hitsplat3Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HITSPLAT_3);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HITSPLAT_3);
        }


        for (JComponent c : hitsplat4Attributes.getAllComponents())
        {
            if (c instanceof JComboBox)
            {
                addHoverListeners(c, KeyFrameType.HITSPLAT_4);
                continue;
            }

            addHoverListenersWithChildren(c, KeyFrameType.HITSPLAT_4);
        }
    }

    private void addMouseFocusListener(JComponent component)
    {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                component.requestFocusInWindow();
            }
        });
    }

    private void addHoverListeners(Component component, KeyFrameType type)
    {
        component.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                super.mouseEntered(e);
                hoveredComponent = component;
                hoveredKeyFrameType = type;
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                super.mouseExited(e);
                hoveredComponent = null;
                hoveredKeyFrameType = KeyFrameType.NULL;
            }
        });
    }

    private void addHoverListenersWithChildren(JComponent component, KeyFrameType type)
    {
        ArrayList<Component> components = new ArrayList<>();
        getAllComponentChildren(components, component);
        for (Component c : components)
        {
            addHoverListeners(c, type);
        }
    }

    private void getAllComponentChildren(ArrayList<Component> components, JComponent component)
    {
        for (Component c : component.getComponents())
        {
            components.add(c);
            getAllComponentChildren(components, (JComponent) c);
        }
    }

    public void resetAttributes(Character character, double tick)
    {
        // Suppress auto-update for the whole load path -- every spinner.setValue
        // below would otherwise echo back through fireAutoUpdate and either loop
        // or rewrite the freshly-loaded keyframe with its own (just-loaded) values.
        suppressAutoUpdateDepth++;
        try
        {
            resetAttributesInner(character, tick);
        }
        finally
        {
            suppressAutoUpdateDepth--;
        }
    }

    private void resetAttributesInner(Character character, double tick)
    {
        // No-Character path: only continue if a global keyframe is currently
        // selected. Globals live in the central store and don't need a
        // Character to look up; everything else falls back to the empty card.
        if (character == null)
        {
            KeyFrame selectedGlobal = findSelectedKeyFrameOfCurrentType();
            boolean isGlobal = selectedKeyFramePage == KeyFrameType.CAMERA
                    || selectedKeyFramePage == KeyFrameType.SCREEN_FADE
                    || selectedKeyFramePage == KeyFrameType.SCREEN_SHAKE;
            if (selectedGlobal != null && isGlobal)
            {
                setKeyFramedIcon(true);
                KeyFrameState s = tick == selectedGlobal.getTick()
                        ? KeyFrameState.ON_KEYFRAME
                        : KeyFrameState.OFF_KEYFRAME;
                switch (selectedKeyFramePage)
                {
                    case CAMERA:
                        cameraAttributes.setAttributes(selectedGlobal);
                        cameraAttributes.setBackgroundColours(s);
                        break;
                    case SCREEN_FADE:
                        screenFadeAttributes.setAttributes(selectedGlobal);
                        screenFadeAttributes.setBackgroundColours(s);
                        break;
                    case SCREEN_SHAKE:
                        screenShakeAttributes.setAttributes(selectedGlobal);
                        screenShakeAttributes.setBackgroundColours(s);
                        break;
                    default: break;
                }
                return;
            }
            setAttributesEmpty(true);
            return;
        }

        // Prefer a user-selected keyframe of the current type over the time-based
        // lookup so clicking a keyframe in the timeline immediately edits THAT
        // keyframe rather than whichever one happens to live under the seeker bar.
        KeyFrame keyFrame = findSelectedKeyFrameOfCurrentType();
        if (keyFrame != null)
        {
            setKeyFramedIcon(true);
        }
        else
        {
            setKeyFramedIcon(character.findKeyFrame(selectedKeyFramePage, tick) != null);
            keyFrame = character.findPreviousKeyFrame(selectedKeyFramePage, tick, true);

            if (keyFrame == null)
            {
                keyFrame = character.findNextKeyFrame(selectedKeyFramePage, tick);

                if (keyFrame == null)
                {
                    setAttributesEmpty(true);
                    return;
                }
            }
        }

        KeyFrameState keyFrameState = tick == keyFrame.getTick() ? KeyFrameState.ON_KEYFRAME : KeyFrameState.OFF_KEYFRAME;

        switch (selectedKeyFramePage)
        {
            default:
            case MOVEMENT:
                movementAttributes.setAttributes(keyFrame);
                movementAttributes.setBackgroundColours(keyFrameState);
                break;
            case ANIMATION:
                animAttributes.setAttributes(keyFrame);
                animAttributes.setBackgroundColours(keyFrameState);
                break;
            case ORIENTATION:
                oriAttributes.setAttributes(keyFrame);
                oriAttributes.setBackgroundColours(keyFrameState);
                break;
            case SPAWN:
                spawnAttributes.setAttributes(keyFrame);
                spawnAttributes.setBackgroundColours(keyFrameState);
                break;
            case MODEL:
                modelAttributes.setAttributes(keyFrame);
                modelAttributes.setBackgroundColours(keyFrameState);
                break;
            case TEXT:
                textAttributes.setAttributes(keyFrame);
                textAttributes.setBackgroundColours(keyFrameState);
                break;
            case OVERHEAD:
                overheadAttributes.setAttributes(keyFrame);
                overheadAttributes.setBackgroundColours(keyFrameState);
                break;
            case HEALTH:
                healthAttributes.setAttributes(keyFrame);
                healthAttributes.setBackgroundColours(keyFrameState);
                break;
            case SPOTANIM:
                spotAnimAttributes.setAttributes(keyFrame);
                spotAnimAttributes.setBackgroundColours(keyFrameState);
                break;
            case SPOTANIM2:
                spotAnim2Attributes.setAttributes(keyFrame);
                spotAnim2Attributes.setBackgroundColours(keyFrameState);
                break;
            case HITSPLAT_1:
                hitsplat1Attributes.setAttributes(keyFrame);
                hitsplat1Attributes.setBackgroundColours(keyFrameState);
                break;
            case HITSPLAT_2:
                hitsplat2Attributes.setAttributes(keyFrame);
                hitsplat2Attributes.setBackgroundColours(keyFrameState);
                break;
            case HITSPLAT_3:
                hitsplat3Attributes.setAttributes(keyFrame);
                hitsplat3Attributes.setBackgroundColours(keyFrameState);
                break;
            case HITSPLAT_4:
                hitsplat4Attributes.setAttributes(keyFrame);
                hitsplat4Attributes.setBackgroundColours(keyFrameState);
                break;
            case PROJECTILE:
                projectileAttributes.setAttributes(keyFrame);
                projectileAttributes.setBackgroundColours(keyFrameState);
                break;
            case SHIELD:
                shieldAttributes.setAttributes(keyFrame);
                shieldAttributes.setBackgroundColours(keyFrameState);
                break;
            case SPECIAL:
                specialAttributes.setAttributes(keyFrame);
                specialAttributes.setBackgroundColours(keyFrameState);
                break;
            case SCREEN_FADE:
                screenFadeAttributes.setAttributes(keyFrame);
                screenFadeAttributes.setBackgroundColours(keyFrameState);
                break;
            case SCREEN_SHAKE:
                screenShakeAttributes.setAttributes(keyFrame);
                screenShakeAttributes.setBackgroundColours(keyFrameState);
                break;
            case CAMERA:
                cameraAttributes.setAttributes(keyFrame);
                cameraAttributes.setBackgroundColours(keyFrameState);
                break;
            case PULSE:
                pulseAttributes.setAttributes(keyFrame);
                pulseAttributes.setBackgroundColours(keyFrameState);
        }
    }

    public void setAttributesEmpty(boolean resetBackground)
    {
        suppressAutoUpdateDepth++;
        try
        {
            setAttributesEmptyInner(resetBackground);
        }
        finally
        {
            suppressAutoUpdateDepth--;
        }
    }

    private void setAttributesEmptyInner(boolean resetBackground)
    {
        switch (selectedKeyFramePage)
        {
            default:
            case MOVEMENT:
                movementAttributes.resetAttributes(resetBackground);
                break;
            case ANIMATION:
                animAttributes.resetAttributes(resetBackground);
                break;
            case ORIENTATION:
                oriAttributes.resetAttributes(resetBackground);
                break;
            case SPAWN:
                spawnAttributes.resetAttributes(resetBackground);
                break;
            case MODEL:
                modelAttributes.resetAttributes(resetBackground);
                break;
            case TEXT:
                textAttributes.resetAttributes(resetBackground);
                break;
            case OVERHEAD:
                overheadAttributes.resetAttributes(resetBackground);
                break;
            case HEALTH:
                healthAttributes.resetAttributes(resetBackground);
                break;
            case SPOTANIM:
                spotAnimAttributes.resetAttributes(resetBackground);
                break;
            case SPOTANIM2:
                spotAnim2Attributes.resetAttributes(resetBackground);
                break;
            case HITSPLAT_1:
                hitsplat1Attributes.resetAttributes(resetBackground);
                break;
            case HITSPLAT_2:
                hitsplat2Attributes.resetAttributes(resetBackground);
                break;
            case HITSPLAT_3:
                hitsplat3Attributes.resetAttributes(resetBackground);
                break;
            case HITSPLAT_4:
                hitsplat4Attributes.resetAttributes(resetBackground);
                break;
            case PROJECTILE:
                projectileAttributes.resetAttributes(resetBackground);
                break;
            case SHIELD:
                shieldAttributes.resetAttributes(resetBackground);
                break;
            case SPECIAL:
                specialAttributes.resetAttributes(resetBackground);
                break;
            case SCREEN_FADE:
                screenFadeAttributes.resetAttributes(resetBackground);
                break;
            case SCREEN_SHAKE:
                screenShakeAttributes.resetAttributes(resetBackground);
                break;
            case CAMERA:
                cameraAttributes.resetAttributes(resetBackground);
                break;
            case PULSE:
                pulseAttributes.resetAttributes(resetBackground);
        }
    }

    public void setKeyFramedIcon(boolean isKeyFramed)
    {
        if (isKeyFramed)
        {
            keyFramed.setIcon(keyframeImage);
        }
        else
        {
            keyFramed.setIcon(keyframeEmptyImage);
        }
        // Icon changes happen when the seeker moves, the Character changes,
        // or a keyframe is added/removed -- exactly the events that flip
        // the "+" enabled state. Re-derive both at the same time so the
        // grey-out stays in sync with the icon.
        refreshAddKeyFrameEnabled(computeMixedTypes());
    }

    /**
     * Sets {@code keyFramed.setEnabled} per the spec: disabled when the
     * marquee mixes types (ambiguous which to create) OR when every
     * selected target already has a keyframe at the current tick for
     * the current page. {@code mixedTypes} is passed in by the caller
     * because both callsites already compute it.
     */
    private void refreshAddKeyFrameEnabled(boolean mixedTypes)
    {
        if (timeSheetPanel == null)
        {
            keyFramed.setEnabled(!mixedTypes);
            return;
        }
        boolean allHave = timeSheetPanel.allSelectedTargetsHaveKeyFrame(
                selectedKeyFramePage, timeSheetPanel.getCurrentTime());
        keyFramed.setEnabled(!mixedTypes && !allHave);
    }

    /** Recomputes mixedTypes from the current marquee. Mirrors the inline
     *  scan in refreshKeyFrameSelectionState; pulled out so setKeyFramedIcon
     *  can re-derive it without re-running the full selection-state pass. */
    private boolean computeMixedTypes()
    {
        if (timeSheetPanel == null) return false;
        KeyFrame[] selected = timeSheetPanel.getSelectedKeyFrames();
        if (selected == null) return false;
        java.util.EnumSet<KeyFrameType> types = java.util.EnumSet.noneOf(KeyFrameType.class);
        for (KeyFrame kf : selected)
        {
            if (kf != null) types.add(kf.getKeyFrameType());
        }
        return types.size() > 1;
    }
}