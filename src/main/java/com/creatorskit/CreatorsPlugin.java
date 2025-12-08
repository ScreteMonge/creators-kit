package com.creatorskit;

import com.creatorskit.models.*;
import com.creatorskit.programming.*;
import com.creatorskit.programming.orientation.OrientationHotkeyMode;
import com.creatorskit.saves.TransmogLoadOption;
import com.creatorskit.swing.*;
import com.creatorskit.swing.anvil.ComplexPanel;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Menu;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Slf4j
@Getter
@Setter
@PluginDescriptor(
		name = "Creator's Kit",
		description = "A suite of tools for creators",
		tags = {"tool", "creator", "content", "kit", "camera", "immersion", "export"}
)
public class CreatorsPlugin extends Plugin implements MouseListener {
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private CreatorsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CreatorsOverlay overlay;

	@Inject
	private TextOverlay textOverlay;

	@Inject
	private OverheadOverlay overheadOverlay;

	@Inject
	private HealthOverlay healthOverlay;

	@Inject
	private HitsplatOverlay hitsplatOverlay;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ModelGetter modelGetter;

	@Inject
	private PathFinder pathFinder;

	@Inject
	private DataFinder dataFinder;

	@Inject
	private Gson gson;

	private CreatorsPanel creatorsPanel;
	private NavigationButton navigationButton;
	private boolean overlaysActive = false;
	private final ArrayList<Character> characters = new ArrayList<>();
	private final ArrayList<CustomModel> storedModels = new ArrayList<>();
	private Character selectedCharacter;
	private Character hoveredCharacter;
	private CKObject transmog;
	private CKObject previewObject;
	private Random random = new Random();
	private Model previewArrow;
	private CustomModel transmogModel;
	private final int GOLDEN_CHIN = 29757;
	private int savedRegion = -1;
	private int savedPlane = -1;
	private AutoRotate autoRotateYaw = AutoRotate.OFF;
	private AutoRotate autoRotatePitch = AutoRotate.OFF;
	private int oculusOrbSpeed = 36;
	private double clickX;
	private double clickY;
	private boolean mousePressed = false;
	private boolean autoSetupPathFound = true;
	private boolean autoTransmogFound = true;
	private boolean addProgramStep = false;

	@Override
	protected void startUp() throws Exception
	{
		creatorsPanel = injector.getInstance(CreatorsPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
		navigationButton = NavigationButton.builder()
				.tooltip("Creator's Kit")
				.icon(icon)
				.priority(10)
				.panel(creatorsPanel)
				.build();

		eventBus.register(creatorsPanel.getToolBox().getProgrammer());
		eventBus.register(creatorsPanel.getToolBox().getTransmogPanel());

		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(overlay);
		overlayManager.add(overheadOverlay);
		overlayManager.add(healthOverlay);
		overlayManager.add(hitsplatOverlay);
		overlayManager.add(textOverlay);

		keyManager.registerKeyListener(overlayKeyListener);
		keyManager.registerKeyListener(oculusOrbListener);
		keyManager.registerKeyListener(orbPreset1Listener);
		keyManager.registerKeyListener(orbPreset2Listener);
		keyManager.registerKeyListener(orbPreset3Listener);
		keyManager.registerKeyListener(quickSpawnListener);
		keyManager.registerKeyListener(quickLocationListener);
		keyManager.registerKeyListener(quickDuplicateListener);
		keyManager.registerKeyListener(quickRotateCWListener);
		keyManager.registerKeyListener(quickRotateCCWListener);
		keyManager.registerKeyListener(autoLeftListener);
		keyManager.registerKeyListener(autoRightListener);
		keyManager.registerKeyListener(autoUpListener);
		keyManager.registerKeyListener(autoDownListener);
		keyManager.registerKeyListener(addProgramStepListener);
		keyManager.registerKeyListener(removeProgramStepListener);
		keyManager.registerKeyListener(clearProgramStepListener);
		keyManager.registerKeyListener(addOrientationStartListener);
		keyManager.registerKeyListener(addOrientationGoalListener);
		keyManager.registerKeyListener(playPauseListener);
		keyManager.registerKeyListener(resetTimelineListener);
		keyManager.registerKeyListener(skipForwardListener);
		keyManager.registerKeyListener(skipSubForwardListener);
		keyManager.registerKeyListener(skipBackwardListener);
		keyManager.registerKeyListener(skipSubBackwardListener);
		keyManager.registerKeyListener(saveListener);
		keyManager.registerKeyListener(openListener);
		keyManager.registerKeyListener(undoListener);
		keyManager.registerKeyListener(redoListener);
		mouseManager.registerMouseWheelListener(this::mouseWheelMoved);
		mouseManager.registerMouseListener(this);

		if (config.autoSetup())
		{
			File SETUP_DIR = new File(config.setupPath());
			if (!SETUP_DIR.exists())
			{
				SETUP_DIR = new File(config.setupPath() + ".json");
				if (!SETUP_DIR.exists())
				{
					SETUP_DIR = new File(config.setupPath().replaceAll("/", "\\\\"));
					if (!SETUP_DIR.exists())
					{
						SETUP_DIR = new File(config.setupPath().replaceAll("/", "\\\\") + ".json");
					}
				}
			}

			if (SETUP_DIR.exists())
			{
				creatorsPanel.loadSetup(SETUP_DIR);
			}
			else
			{
				autoSetupPathFound = false;
			}
		}

		if (config.autoTransmog())
		{
			File TRANSMOG_DIR = new File(config.transmogPath());
			if (!TRANSMOG_DIR.exists())
			{
				TRANSMOG_DIR = new File(config.transmogPath() + ".json");
				if (!TRANSMOG_DIR.exists())
				{
					TRANSMOG_DIR = new File(config.transmogPath().replaceAll("/", "\\\\"));
					if (!TRANSMOG_DIR.exists())
					{
						TRANSMOG_DIR = new File(config.transmogPath().replaceAll("/", "\\\\") + ".json");
					}
				}
			}

			if (TRANSMOG_DIR.exists())
			{
				creatorsPanel.getToolBox().getModelUtilities().loadTransmog(TRANSMOG_DIR, TransmogLoadOption.BOTH);
			}
			else
			{
				autoTransmogFound = false;
			}
		}

		oculusOrbSpeed = config.orbSpeed();

		String string = configManager.getConfiguration("creatorssuite", "overlaysActive");
		try
		{
            overlaysActive = Boolean.parseBoolean(string);
		}
		catch (Exception e)
		{
			overlaysActive = false;
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		creatorsPanel.clearSidePanels(false);
		creatorsPanel.clearManagerPanels();
		creatorsPanel.getToolBox().dispose();

		eventBus.unregister(creatorsPanel.getToolBox().getProgrammer());
		eventBus.unregister(creatorsPanel.getToolBox().getTransmogPanel());

		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(overlay);
		overlayManager.remove(textOverlay);
		overlayManager.remove(overheadOverlay);
		overlayManager.remove(healthOverlay);
		overlayManager.remove(hitsplatOverlay);

		keyManager.unregisterKeyListener(overlayKeyListener);
		keyManager.unregisterKeyListener(oculusOrbListener);
		keyManager.unregisterKeyListener(orbPreset1Listener);
		keyManager.unregisterKeyListener(orbPreset2Listener);
		keyManager.unregisterKeyListener(orbPreset3Listener);
		keyManager.unregisterKeyListener(quickSpawnListener);
		keyManager.unregisterKeyListener(quickLocationListener);
		keyManager.unregisterKeyListener(quickDuplicateListener);
		keyManager.unregisterKeyListener(quickRotateCWListener);
		keyManager.unregisterKeyListener(quickRotateCCWListener);
		keyManager.unregisterKeyListener(autoLeftListener);
		keyManager.unregisterKeyListener(autoRightListener);
		keyManager.unregisterKeyListener(autoUpListener);
		keyManager.unregisterKeyListener(autoDownListener);
		keyManager.unregisterKeyListener(addProgramStepListener);
		keyManager.unregisterKeyListener(removeProgramStepListener);
		keyManager.unregisterKeyListener(clearProgramStepListener);
		keyManager.unregisterKeyListener(addOrientationStartListener);
		keyManager.unregisterKeyListener(addOrientationGoalListener);
		keyManager.unregisterKeyListener(playPauseListener);
		keyManager.unregisterKeyListener(resetTimelineListener);
		keyManager.unregisterKeyListener(skipForwardListener);
		keyManager.unregisterKeyListener(skipSubForwardListener);
		keyManager.unregisterKeyListener(skipBackwardListener);
		keyManager.unregisterKeyListener(skipSubBackwardListener);
		keyManager.unregisterKeyListener(saveListener);
		keyManager.unregisterKeyListener(openListener);
		keyManager.unregisterKeyListener(undoListener);
		keyManager.unregisterKeyListener(redoListener);
		mouseManager.unregisterMouseWheelListener(this::mouseWheelMoved);
		mouseManager.unregisterMouseListener(this);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		if (!autoSetupPathFound)
		{
			autoSetupPathFound = true;
			sendChatMessage("Creator's Kit auto-Setup has failed to find the file at the path: " + config.setupPath());
			sendChatMessage("Please ensure the config menu has the appropriate file path.");
		}

		if (!autoTransmogFound)
		{
			autoTransmogFound = true;
			sendChatMessage("Creator's Kit auto-Transmog has failed to find the file at the path: " + config.transmogPath());
			sendChatMessage("Please ensure the config menu has the appropriate file path.");
		}

		if (client.getLocalPlayer() == null)
			return;

		WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();
		LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
		WorldView worldView = client.getTopLevelWorldView();

		int region = worldView.getScene().isInstance() ? WorldPoint.fromLocalInstance(client, localPoint).getRegionID() : worldPoint.getRegionID();

		if (savedRegion != region)
			savedRegion = region;

		int plane = worldView.getPlane();
		if (savedPlane != plane)
		{
			savedPlane = plane;
			for (int i = 0; i < characters.size(); i++)
			{
				Character character = characters.get(i);
				setLocation(character, false, false, character.isActive() ? ActiveOption.ACTIVE : ActiveOption.INACTIVE, LocationOption.TO_CURRENT_TICK);
			}
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		updatePreviewObject(client.getTopLevelWorldView().getSelectedSceneTile());

		if (addProgramStep)
		{
			addProgramStep = false;
			addProgramStep();
		}

		switch (autoRotateYaw)
		{
			case LEFT:
				client.setCameraYawTarget(client.getCameraYaw() - config.rotateHorizontalSpeed());
				break;
			case RIGHT:
				client.setCameraYawTarget(client.getCameraYaw() + config.rotateHorizontalSpeed());
		}

		switch (autoRotatePitch)
		{
			case UP:
				client.setCameraPitchTarget(client.getCameraPitch() + config.rotateVerticalSpeed());
				break;
			case DOWN:
				client.setCameraPitchTarget(client.getCameraPitch() - config.rotateVerticalSpeed());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			creatorsPanel.getToolBox().getProgrammer().updatePrograms(getCurrentTick());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getKey().equals("orbSpeed"))
		{
			client.setFreeCameraSpeed(config.orbSpeed());
			oculusOrbSpeed = config.orbSpeed();
		}

		if (event.getKey().equals("enableTransmog"))
		{
			if (transmog == null)
				return;

			clientThread.invokeLater(() ->
			{
				transmog.setActive(config.enableTransmog());
			});
		}
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (config.enableCtrlHotkeys() && client.isKeyPressed(KeyCode.KC_CONTROL))
		{
			if (selectedCharacter != null)
			{
				client.getMenu().createMenuEntry(-1)
						.setOption(ColorUtil.prependColorTag("Relocate", Color.ORANGE))
						.setTarget(ColorUtil.colorTag(Color.GREEN) + selectedCharacter.getName())
						.setType(MenuAction.RUNELITE)
						.onClick(e -> setLocation(selectedCharacter, false, true, ActiveOption.ACTIVE, LocationOption.TO_HOVERED_TILE));

				MenuEntry me = client.getMenu().createMenuEntry(-2)
						.setOption(ColorUtil.prependColorTag("Keyframe", Color.ORANGE))
						.setTarget(ColorUtil.colorTag(Color.GREEN) + selectedCharacter.getName())
						.setType(MenuAction.RUNELITE);

				Menu menu = me.createSubMenu();
				SubMenuCreator.createSubMenus(creatorsPanel.getToolBox().getTimeSheetPanel(), menu);
			}
		}

		if (!config.rightClick())
		{
			return;
		}

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		addMenuEntries(topLevelWorldView);

		IndexedObjectSet<? extends WorldView> worldViews = topLevelWorldView.worldViews();
		for (WorldView worldView : worldViews)
		{
			addMenuEntries(worldView);
		}
	}

	public void addMenuEntries(WorldView worldView)
	{
		Tile tile = worldView.getSelectedSceneTile();
		if (tile == null)
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();
		boolean hoveringTile = false;
		for (MenuEntry menuEntry : menuEntries)
		{
			String option = menuEntry.getOption();
			if (option.equals("Walk here") || option.equals("Set heading"))
			{
				hoveringTile = true;
				break;
			}
		}

		if (!hoveringTile)
		{
			return;
		}

		modelGetter.addCharacterMenuEntries(tile);
		modelGetter.addLocalPlayerMenuEntries(tile);
		modelGetter.addTileItemMenuEntries(tile);
		modelGetter.addTileObjectMenuEntries(tile);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.rightClick())
		{
			return;
		}

		String target = event.getTarget();
		String option = event.getOption();

		NPC npc = event.getMenuEntry().getNpc();
		if (npc != null && option.equals("Examine"))
		{
			modelGetter.addNPCMenuEntries(target, npc);
		}

		Player player = event.getMenuEntry().getPlayer();
		if (player != null && option.equals("Trade with"))
		{
			modelGetter.addPlayerMenuEntries(target, player);
		}
	}

	public void setLocation(Character character, boolean initialize, boolean transplant, ActiveOption activeOption, LocationOption locationOption)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		boolean poh = MovementManager.useLocalLocations(client.getTopLevelWorldView());

		if (poh)
		{
			setLocationPOH(character, initialize, transplant, activeOption, locationOption);
			return;
		}

		setLocationWorld(character, initialize, transplant, activeOption, locationOption);
	}

	public void setLocationWorld(Character character, boolean initialize, boolean transplant, ActiveOption activeOption, LocationOption locationOption)
	{
		WorldView worldView = client.getTopLevelWorldView();
		LocalPoint localPoint = null;
		WorldPoint wp = character.getNonInstancedPoint();
		if (wp != null)
		{
			Collection<WorldPoint> wps = WorldPoint.toLocalInstance(worldView, wp);
			if (!wps.isEmpty())
			{
				wp = wps.iterator().next();
				localPoint = LocalPoint.fromWorld(worldView, wp);
			}
		}

		switch (locationOption)
		{
			case TO_PLAYER:
				localPoint = client.getLocalPlayer().getLocalLocation();
				break;
			case TO_HOVERED_TILE:
				Tile tile = worldView.getSelectedSceneTile();
				if (tile == null)
				{
					return;
				}

				localPoint = tile.getLocalLocation();
				break;
			case TO_SAVED_LOCATION:
			case TO_CURRENT_TICK:
			default:
				break;
		}

		if (localPoint == null || !localPoint.isInScene())
		{
			character.setInScene(false);
			return;
		}

		character.setInScene(true);

		if (initialize)
		{
			WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
			character.setNonInstancedPoint(worldPoint);
			character.setInPOH(false);
		}

		if (transplant)
		{
			WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
			pathFinder.transplantSteps(character, worldView, worldPoint.getX(), worldPoint.getY());

			KeyFrame kf = character.findNextKeyFrame(KeyFrameType.MOVEMENT, -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH);
			if (kf != null)
			{
				MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
				int[][] step = keyFrame.getPath();
				if (step.length != 0)
				{
					int[] first = step[0];
					worldPoint = new WorldPoint(first[0], first[1], worldView.getPlane());
				}
			}

			character.setNonInstancedPoint(worldPoint);
			character.setInPOH(false);
		}

		LocalPoint finalLocalPoint = localPoint;
		clientThread.invokeLater(() -> character.setLocation(finalLocalPoint, worldView.getPlane()));

		if (locationOption == LocationOption.TO_HOVERED_TILE)
		{
			creatorsPanel.getToolBox().getProgrammer().register3DChanges(character);
		}

		switch (activeOption)
		{
			case ACTIVE:
				character.setActive(true, true, true, clientThread);
				break;
			case INACTIVE:
				character.setActive(false, false, false, clientThread);
				break;
			case UNCHANGED:
				character.resetActive(clientThread);
		}
	}

	public void setLocationPOH(Character character, boolean initialize, boolean transplant, ActiveOption activeOption, LocationOption locationOption)
	{
		WorldView worldView = client.getTopLevelWorldView();
		LocalPoint localPoint = null;
		if (character.getInstancedPoint() != null)
		{
			localPoint = character.getInstancedPoint();
		}

		switch (locationOption)
		{
			case TO_PLAYER:
				localPoint = client.getLocalPlayer().getLocalLocation();
				break;
			case TO_HOVERED_TILE:
				Tile tile = worldView.getSelectedSceneTile();
				if (tile == null)
				{
					return;
				}

				localPoint = tile.getLocalLocation();
				break;
			case TO_CURRENT_TICK:
			case TO_SAVED_LOCATION:
			default:
				break;
		}

		if (localPoint == null || !localPoint.isInScene())
		{
			character.setInScene(false);
			return;
		}

		character.setInScene(true);

		if (initialize)
		{
			character.setInstancedPoint(localPoint);
			character.setInstancedPlane(worldView.getPlane());
			character.setInPOH(true);
		}

		if (transplant)
		{
			pathFinder.transplantSteps(character, worldView, localPoint.getSceneX(), localPoint.getSceneY());
			LocalPoint savedPoint = localPoint;

			KeyFrame kf = character.findNextKeyFrame(KeyFrameType.MOVEMENT, -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH);
			if (kf != null)
			{
				MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
				int[][] step = keyFrame.getPath();
				if (step.length != 0)
				{
					int[] first = step[0];
					savedPoint = new LocalPoint(first[0], first[1], worldView);
				}
			}

			character.setInstancedPoint(savedPoint);
			character.setInstancedPlane(worldView.getPlane());
			character.setInPOH(true);
		}

		LocalPoint finalLocalPoint = localPoint;
		clientThread.invokeLater(() -> character.setLocation(finalLocalPoint, worldView.getPlane()));

		if (locationOption == LocationOption.TO_HOVERED_TILE)
		{
			creatorsPanel.getToolBox().getProgrammer().register3DChanges(character);
		}

		switch (activeOption)
		{
			case ACTIVE:
				character.setActive(true, true, true, clientThread);
				break;
			case INACTIVE:
				character.setActive(true, false, false, clientThread);
				break;
			case UNCHANGED:
				character.resetActive(clientThread);
		}
	}

	public double getCurrentTick()
	{
		return creatorsPanel.getToolBox().getTimeSheetPanel().getCurrentTime();
	}

	public void setModel(Character character, boolean modelMode, int modelId)
	{
		CKObject ckObject = character.getCkObject();
		if (ckObject == null)
		{
			return;
		}

		clientThread.invokeLater(() -> {
			if (modelMode)
			{
				CustomModel customModel = character.getStoredModel();
				Model model = customModel == null ? client.loadModel(GOLDEN_CHIN) : customModel.getModel();
				ckObject.setModel(model);
				return;
			}

			Model model = client.loadModel(modelId);
			ckObject.setModel(model);
		});
	}

	public void setRadius(Character character, int radius)
	{
		CKObject ckObject = character.getCkObject();
		clientThread.invoke(() -> ckObject.setRadius(radius));
	}

	public void addOrientation(Character character, int addition)
	{
		CKObject ckObject = character.getCkObject();
		int orientation = ckObject.getOrientation();
		orientation += addition;
		if (orientation >= 2048)
			orientation -= 2048;

		if (orientation < 0)
			orientation += 2048;

		setOrientation(character, orientation);
	}

	public void setOrientation(Character character, int orientation)
	{
		CKObject ckObject = character.getCkObject();
		character.getOrientationSpinner().setValue(orientation);
		clientThread.invokeLater(() -> ckObject.setOrientation(orientation));
	}

	public void setupRLObject(Character character, boolean setHoveredTile, boolean transplant)
	{
		clientThread.invoke(() ->
		{
			CKObject ckObject = character.getCkObject();
			client.registerRuneLiteObject(ckObject);

			ckObject.setRadius((int) character.getRadiusSpinner().getValue());
			ckObject.setOrientation((int) character.getOrientationSpinner().getValue());

			boolean active = character.isActive();

			setModel(character, character.isCustomMode(), (int) character.getModelSpinner().getValue());
			character.setAnimation(client, random, AnimationType.ACTIVE, (int) character.getAnimationSpinner().getValue(), (int) character.getAnimationFrameSpinner().getValue(), config.randomizeStartFrame(), true);

			LocationOption locationOption = setHoveredTile ? LocationOption.TO_HOVERED_TILE : LocationOption.TO_SAVED_LOCATION;
			setLocation(character, true, transplant, active ? ActiveOption.ACTIVE : ActiveOption.INACTIVE, locationOption);

			if (client.getGameState() == GameState.LOGGED_IN)
			{
				creatorsPanel.getToolBox().getProgrammer().updateProgram(character);
			}
		});
	}

	public void sendChatMessage(String chatMessage)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		final String message = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(chatMessage).build();
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(message).build());
	}

	private void updatePreviewObject(Tile tile)
	{
		if (previewObject == null)
		{
			clientThread.invokeLater(() ->
			{
				previewObject = new CKObject(client);
				client.registerRuneLiteObject(previewObject);
				previewObject.setActive(false);

				ModelData arrow = client.loadModelData(4852);
				ModelData transparent = client.loadModelData(9925);
				if (arrow == null || transparent == null)
				{
					return;
				}

				transparent.cloneVertices();
				float[] tx = transparent.getVerticesX();
				float[] ty = transparent.getVerticesY();
				float[] tz = transparent.getVerticesZ();
				for (int i = 0; i < tx.length; i++)
				{
					tx[i] = 0;
					ty[i] = 0;
					tz[i] = 0;
				}

				arrow.cloneVertices().rotateY180Ccw().scale(256, 256, 256);
				ModelData merge = client.mergeModels(arrow, transparent);
				merge.cloneTransparencies();
				byte[] transparencies = merge.getFaceTransparencies();
				for (byte b = 0; b < merge.getFaceCount(); b++)
				{
					transparencies[b] = 115;
				}

				previewArrow = merge.light();
			});
			return;
		}

		if (!config.enableCtrlHotkeys())
		{
			previewObject.setActive(false);
			return;
		}

		if (!client.isKeyPressed(KeyCode.KC_CONTROL)
			|| client.isMenuOpen()
			|| tile == null
			|| selectedCharacter == null)
		{
			previewObject.setActive(false);
			return;
		}

		CKObject ckObject = selectedCharacter.getCkObject();
		if (ckObject == null)
		{
			return;
		}

		boolean allowArrow = false;
		int orientation;
		if (mousePressed)
		{
			final int yaw = client.getCameraYaw();
			final int pitch = client.getCameraPitch();
			Point p = client.getMouseCanvasPosition();
			double x = p.getX() - clickX;
			double y = -1 * (p.getY() - clickY);
			if (Math.sqrt(x * x + y * y) < 40)
			{
				orientation = Rotation.roundRotation(ckObject.getOrientation());
			}
			else
			{
				allowArrow = true;
				orientation = Rotation.getJagexDegrees(p.getX() - clickX, (p.getY() - clickY) * -1, yaw, pitch);
			}
		}
		else
		{
			orientation = ckObject.getOrientation();
		}

		Model model;
		if (mousePressed && previewArrow != null && allowArrow)
		{
			model = previewArrow;
		}
		else
		{
			if (selectedCharacter.isCustomMode())
			{
				if (selectedCharacter.getStoredModel() == null)
				{
					model = client.loadModel(29757);
				}
				else
				{
					model = selectedCharacter.getStoredModel().getModel();
				}
			}
			else
			{
				model = client.loadModel((int) selectedCharacter.getModelSpinner().getValue());
			}
		}

		LocalPoint lp;
		if (mousePressed)
		{
			lp = ckObject.getLocation();
			if (lp == null)
			{
				lp = tile.getLocalLocation();
			}
		}
		else
		{
			lp = tile.getLocalLocation();
			if (lp == null)
			{
				lp = ckObject.getLocation();
			}
		}

		if (lp == null || !lp.isInScene())
		{
			return;
		}

		int animId;
		if (allowArrow)
		{
			animId = -1;
		}
		else
		{
			animId = ckObject.getAnimationId();
		}

		previewObject.setModel(model);
		previewObject.setOrientation(orientation);
		previewObject.setAnimation(AnimationType.ACTIVE, animId);
		previewObject.setAnimationFrame(AnimationType.ACTIVE, ckObject.getAnimationFrame(AnimationType.ACTIVE), random, false,true);
		previewObject.setLocation(lp, client.getTopLevelWorldView().getPlane());
		previewObject.setRadius(ckObject.getRadius());
		previewObject.setActive(true);
	}

	public ArrayList<ComplexPanel> getComplexPanels()
	{
		return creatorsPanel.getModelAnvil().getComplexPanels();
	}

	private final HotkeyListener overlayKeyListener = new HotkeyListener(() -> config.toggleOverlaysHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			overlaysActive = !overlaysActive;
			configManager.setConfiguration("creatorssuite", "overlaysActive", String.valueOf(overlaysActive));
		}
	};

	private final HotkeyListener oculusOrbListener = new HotkeyListener(() -> config.toggleOrbHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (client.getCameraMode() == 1)
			{
				client.setCameraMode(0);
				client.setFreeCameraSpeed(12);
				return;
			}

			client.setCameraMode(1);
			client.setFreeCameraSpeed(oculusOrbSpeed);
		}
	};

	private final HotkeyListener orbPreset1Listener = new HotkeyListener(() -> config.orbSpeedHotkey1())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setFreeCameraSpeed(config.speedHotkey1());
			sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey1());
		}
	};

	private final HotkeyListener orbPreset2Listener = new HotkeyListener(() -> config.orbSpeedHotkey2())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setFreeCameraSpeed(config.speedHotkey2());
			sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey2());
		}
	};

	private final HotkeyListener orbPreset3Listener = new HotkeyListener(() -> config.orbSpeedHotkey3())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setFreeCameraSpeed(config.speedHotkey3());
			sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey3());
		}
	};
	private final HotkeyListener quickSpawnListener = new HotkeyListener(() -> config.quickSpawnHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				selectedCharacter.toggleActive(clientThread);
			}
		}
	};

	private final HotkeyListener quickLocationListener = new HotkeyListener(() -> config.quickLocationHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				setLocation(selectedCharacter, false, true, ActiveOption.ACTIVE, LocationOption.TO_HOVERED_TILE);
			}
		}
	};

	private final HotkeyListener quickDuplicateListener = new HotkeyListener(() -> config.quickDuplicateHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				creatorsPanel.onDuplicatePressed(selectedCharacter, true);
			}
		}
	};

	private final HotkeyListener quickRotateCWListener = new HotkeyListener(() -> config.quickRotateCWHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				addOrientation(selectedCharacter, config.rotateDegrees().degrees * -1);
			}
		}
	};

	private final HotkeyListener quickRotateCCWListener = new HotkeyListener(() -> config.quickRotateCCWHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				addOrientation(selectedCharacter, config.rotateDegrees().degrees);
			}
		}
	};

	private final HotkeyListener autoLeftListener = new HotkeyListener(() -> config.rotateLeftHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotateYaw = (autoRotateYaw == AutoRotate.OFF) ? AutoRotate.LEFT : AutoRotate.OFF;
		}
	};

	private final HotkeyListener autoRightListener = new HotkeyListener(() -> config.rotateRightHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotateYaw = (autoRotateYaw == AutoRotate.OFF) ? AutoRotate.RIGHT : AutoRotate.OFF;
		}
	};

	private final HotkeyListener autoUpListener = new HotkeyListener(() -> config.rotateUpHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotatePitch = (autoRotatePitch == AutoRotate.OFF) ? AutoRotate.UP : AutoRotate.OFF;
		}
	};

	private final HotkeyListener autoDownListener = new HotkeyListener(() -> config.rotateDownHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotatePitch = (autoRotatePitch == AutoRotate.OFF) ? AutoRotate.DOWN : AutoRotate.OFF;
		}
	};

	private final HotkeyListener addProgramStepListener = new HotkeyListener(() -> config.addProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			addProgramStep = true;
		}
	};

	private void addProgramStep()
	{
		creatorsPanel.getToolBox().getTimeSheetPanel().onAddMovementKeyPressed();
	}

	private final HotkeyListener removeProgramStepListener = new HotkeyListener(() -> config.removeProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			removeProgramStep();
		}
	};

	private void removeProgramStep()
	{
		if (selectedCharacter == null)
		{
			return;
		}

		KeyFrame kf = selectedCharacter.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
		if (kf == null)
		{
			return;
		}

		MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
		int[][] path = keyFrame.getPath();

		if (path.length == 0)
		{
			return;
		}

		int newLength = path.length - 1;
		keyFrame.setPath(ArrayUtils.remove(path, newLength));
		creatorsPanel.getToolBox().getProgrammer().register3DChanges(selectedCharacter);
	}

	private final HotkeyListener clearProgramStepListener = new HotkeyListener(() -> config.clearProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			clearProgramSteps();
		}
	};

	private void clearProgramSteps()
	{
		if (selectedCharacter == null)
		{
			return;
		}

		KeyFrame kf = selectedCharacter.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
		if (kf == null)
		{
			return;
		}

		MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
		keyFrame.setPath(new int[0][2]);
		keyFrame.setCurrentStep(0);
	}

	private final HotkeyListener addOrientationStartListener = new HotkeyListener(() -> config.orientationStart()) {
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().onOrientationKeyPressed(OrientationHotkeyMode.SET_START);
		}
	};

	private final HotkeyListener addOrientationGoalListener = new HotkeyListener(() -> config.orientationEnd()) {
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().onOrientationKeyPressed(OrientationHotkeyMode.SET_GOAL);
		}
	};

	private final HotkeyListener playPauseListener = new HotkeyListener(() -> config.playPauseHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getProgrammer().togglePlay();
		}
	};

	private final HotkeyListener resetTimelineListener = new HotkeyListener(() -> config.resetTimelineHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			ToolBoxFrame toolBox = creatorsPanel.getToolBox();
			toolBox.getTimeSheetPanel().setCurrentTime(0, false);
		}
	};

	private final HotkeyListener skipForwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().onAttributeSkipForward();
		}
	};

	private final HotkeyListener skipBackwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().onAttributeSkipPrevious();
		}
	};

	private final HotkeyListener skipSubForwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().skipListener(0.1);
		}
	};

	private final HotkeyListener skipSubBackwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().skipListener(-0.1);
		}
	};

	private final HotkeyListener saveListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.quickSaveToFile();
		}
	};

	private final HotkeyListener openListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.openLoadSetupDialog();
		}
	};

	private final HotkeyListener undoListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().undo();
		}
	};

	private final HotkeyListener redoListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().redo();
		}
	};

	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		if (config.enableCtrlHotkeys() && event.isControlDown())
		{
			creatorsPanel.scrollSelectedCharacter(event.getWheelRotation());
			event.consume();
		}

		return event;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		if (config.enableCtrlHotkeys()
				&& e.getButton() == MouseEvent.BUTTON1
				&& client.isKeyPressed(KeyCode.KC_CONTROL))
		{
			mousePressed = true;
			clickX = e.getPoint().getX();
			clickY = e.getPoint().getY();
		}

		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		mousePressed = false;

		if (config.enableCtrlHotkeys() &&
				e.getButton() == MouseEvent.BUTTON1 &&
				client.isKeyPressed(KeyCode.KC_CONTROL) &&
				selectedCharacter != null)
		{
			double x = e.getX() - clickX;
			double y = -1 * (e.getY() - clickY);
			if (Math.sqrt(x * x + y * y) < 40)
			{
				return e;
			}

			final int yaw = client.getCameraYaw();
			final int pitch = client.getCameraPitch();
			int jUnit = Rotation.getJagexDegrees(x, y, yaw, pitch);
			setOrientation(selectedCharacter, jUnit);
		}

		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		return e;
	}

	@Provides
	CreatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CreatorsConfig.class);
	}
}
