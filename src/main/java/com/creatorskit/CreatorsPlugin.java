package com.creatorskit;

import com.creatorskit.hotkeymanager.HotKeyManager;
import com.creatorskit.hotkeymanager.LocationOption;
import com.creatorskit.models.*;
import com.creatorskit.programming.*;
import com.creatorskit.programming.orientation.Orientation;
import com.creatorskit.saves.TransmogLoadOption;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.swing.*;
import com.creatorskit.swing.anvil.ComplexPanel;
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
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.externalplugins.PluginHubManifest;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
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

	@Inject
	private SelectionManager selectionManager;

	@Inject
	private HotKeyManager hotKeyManager;

	private CreatorsPanel creatorsPanel;
	private NavigationButton navigationButton;
	private final ArrayList<Character> characters = new ArrayList<>();
	private final ArrayList<CustomModel> storedModels = new ArrayList<>();
	private Character hoveredCharacter;
	private CKObject transmog;
	private CKObject previewObject;
	private Random random = new Random();
	private Model previewArrow;
	private CustomModel transmogModel;
	private int savedRegion = -1;
	private int savedPlane = -1;
	private AutoRotate autoRotateYaw = AutoRotate.OFF;
	private AutoRotate autoRotatePitch = AutoRotate.OFF;
	private LocalPoint draggedPoint;
	private double clickX;
	private double clickY;
	private boolean mousePressed = false;
	private boolean autoSetupPathFound = true;
	private boolean autoTransmogFound = true;

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

		ToolBoxFrame toolBox = creatorsPanel.getToolBox();

		eventBus.register(toolBox.getProgrammer());
		eventBus.register(toolBox.getTimeSheetPanel().getSummarySheet());
		eventBus.register(toolBox.getTransmogPanel());
		eventBus.register(toolBox.getCacheSearcher().getRenderPanel());

		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(overlay);
		overlayManager.add(overheadOverlay);
		overlayManager.add(healthOverlay);
		overlayManager.add(hitsplatOverlay);
		overlayManager.add(textOverlay);

		keyManager.registerKeyListener(hotKeyManager.overlayKeyListener);
		keyManager.registerKeyListener(hotKeyManager.oculusOrbListener);
		keyManager.registerKeyListener(hotKeyManager.orbPreset1Listener);
		keyManager.registerKeyListener(hotKeyManager.orbPreset2Listener);
		keyManager.registerKeyListener(hotKeyManager.orbPreset3Listener);
		keyManager.registerKeyListener(hotKeyManager.quickSpawnListener);
		keyManager.registerKeyListener(hotKeyManager.quickLocationListener);
		keyManager.registerKeyListener(hotKeyManager.quickDuplicateListener);
		keyManager.registerKeyListener(hotKeyManager.quickRotateCWListener);
		keyManager.registerKeyListener(hotKeyManager.quickRotateCCWListener);
		keyManager.registerKeyListener(hotKeyManager.autoLeftListener);
		keyManager.registerKeyListener(hotKeyManager.autoRightListener);
		keyManager.registerKeyListener(hotKeyManager.autoUpListener);
		keyManager.registerKeyListener(hotKeyManager.autoDownListener);
		keyManager.registerKeyListener(hotKeyManager.addProgramStepListener);
		keyManager.registerKeyListener(hotKeyManager.removeProgramStepListener);
		keyManager.registerKeyListener(hotKeyManager.clearProgramStepListener);
		keyManager.registerKeyListener(hotKeyManager.addOrientationStartListener);
		keyManager.registerKeyListener(hotKeyManager.addOrientationGoalListener);
		keyManager.registerKeyListener(hotKeyManager.playPauseListener);
		keyManager.registerKeyListener(hotKeyManager.resetTimelineListener);
		keyManager.registerKeyListener(hotKeyManager.skipForwardListener);
		keyManager.registerKeyListener(hotKeyManager.skipSubForwardListener);
		keyManager.registerKeyListener(hotKeyManager.skipBackwardListener);
		keyManager.registerKeyListener(hotKeyManager.skipSubBackwardListener);
		keyManager.registerKeyListener(hotKeyManager.saveListener);
		keyManager.registerKeyListener(hotKeyManager.openListener);
		keyManager.registerKeyListener(hotKeyManager.undoListener);
		keyManager.registerKeyListener(hotKeyManager.redoListener);
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
				creatorsPanel.loadSetup(SETUP_DIR, true);
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
	}

	@Override
	protected void shutDown() throws Exception
	{
		creatorsPanel.deleteCharacters(characters.toArray(new Character[0]));

		ToolBoxFrame toolBox = creatorsPanel.getToolBox();
		eventBus.unregister(toolBox.getProgrammer());
		eventBus.unregister(toolBox.getTimeSheetPanel().getSummarySheet());
		eventBus.unregister(toolBox.getTransmogPanel());
		eventBus.unregister(toolBox.getCacheSearcher().getRenderPanel());
		toolBox.dispose();

		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(overlay);
		overlayManager.remove(textOverlay);
		overlayManager.remove(overheadOverlay);
		overlayManager.remove(healthOverlay);
		overlayManager.remove(hitsplatOverlay);

		keyManager.unregisterKeyListener(hotKeyManager.overlayKeyListener);
		keyManager.unregisterKeyListener(hotKeyManager.oculusOrbListener);
		keyManager.unregisterKeyListener(hotKeyManager.orbPreset1Listener);
		keyManager.unregisterKeyListener(hotKeyManager.orbPreset2Listener);
		keyManager.unregisterKeyListener(hotKeyManager.orbPreset3Listener);
		keyManager.unregisterKeyListener(hotKeyManager.quickSpawnListener);
		keyManager.unregisterKeyListener(hotKeyManager.quickLocationListener);
		keyManager.unregisterKeyListener(hotKeyManager.quickDuplicateListener);
		keyManager.unregisterKeyListener(hotKeyManager.quickRotateCWListener);
		keyManager.unregisterKeyListener(hotKeyManager.quickRotateCCWListener);
		keyManager.unregisterKeyListener(hotKeyManager.autoLeftListener);
		keyManager.unregisterKeyListener(hotKeyManager.autoRightListener);
		keyManager.unregisterKeyListener(hotKeyManager.autoUpListener);
		keyManager.unregisterKeyListener(hotKeyManager.autoDownListener);
		keyManager.unregisterKeyListener(hotKeyManager.addProgramStepListener);
		keyManager.unregisterKeyListener(hotKeyManager.removeProgramStepListener);
		keyManager.unregisterKeyListener(hotKeyManager.clearProgramStepListener);
		keyManager.unregisterKeyListener(hotKeyManager.addOrientationStartListener);
		keyManager.unregisterKeyListener(hotKeyManager.addOrientationGoalListener);
		keyManager.unregisterKeyListener(hotKeyManager.playPauseListener);
		keyManager.unregisterKeyListener(hotKeyManager.resetTimelineListener);
		keyManager.unregisterKeyListener(hotKeyManager.skipForwardListener);
		keyManager.unregisterKeyListener(hotKeyManager.skipSubForwardListener);
		keyManager.unregisterKeyListener(hotKeyManager.skipBackwardListener);
		keyManager.unregisterKeyListener(hotKeyManager.skipSubBackwardListener);
		keyManager.unregisterKeyListener(hotKeyManager.saveListener);
		keyManager.unregisterKeyListener(hotKeyManager.openListener);
		keyManager.unregisterKeyListener(hotKeyManager.undoListener);
		keyManager.unregisterKeyListener(hotKeyManager.redoListener);
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
			Programmer programmer = creatorsPanel.getToolBox().getProgrammer();
			clientThread.invokeLater(() ->
			{
				for (int i = 0; i < characters.size(); i++)
				{
					Character character = characters.get(i);
					character.setLocation(client, clientThread, programmer, false, false, character.isActive() ? ActiveOption.ACTIVE : ActiveOption.INACTIVE, LocationOption.TO_CURRENT_TICK);
				}
			});
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		updatePreviewObject(client.getTopLevelWorldView().getSelectedSceneTile());

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
			hotKeyManager.setOculusOrbSpeed(config.orbSpeed());
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
			Character selectedCharacter = selectionManager.getPrimary();
			if (selectedCharacter != null)
			{
				client.getMenu().createMenuEntry(-1)
						.setOption(ColorUtil.prependColorTag("Relocate", Color.ORANGE))
						.setTarget(ColorUtil.colorTag(Color.GREEN) + selectedCharacter.getName())
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							hotKeyManager.onSetLocation();
						});

				MenuEntry me = client.getMenu().createMenuEntry(-2)
						.setOption(ColorUtil.prependColorTag("Keyframe", Color.ORANGE))
						.setTarget(ColorUtil.colorTag(Color.GREEN) + selectedCharacter.getName())
						.setType(MenuAction.RUNELITE);

				Menu menu = me.createSubMenu();
				menu.createMenuEntry(0)
						.setOption(ColorUtil.prependColorTag("Add", Color.ORANGE))
						.setTarget(ColorUtil.colorTag(Color.WHITE) + KeyFrameType.MOVEMENT)
						.setType(MenuAction.RUNELITE)
						.onClick(e -> hotKeyManager.onAddMovementMenuOptionPressed());
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

	public double getCurrentTick()
	{
		return creatorsPanel.getToolBox().getTimeSheetPanel().getCurrentTime();
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
                Arrays.fill(arrow.getVerticesY(), -5);

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

		Character primary = selectionManager.getPrimary();
		if (!client.isKeyPressed(KeyCode.KC_CONTROL)
			|| client.isMenuOpen()
			|| tile == null
			|| primary == null)
		{
			previewObject.setActive(false);
			return;
		}

		CKObject ckObject = primary.getCkObject();
		if (ckObject == null)
		{
			return;
		}

		boolean allowArrow = false;
		double orientation = ckObject.getOrientation();
		if (mousePressed)
		{
			LocalPoint hovered = tile.getLocalLocation();

			Point p = client.getMouseCanvasPosition();
			double x = p.getX() - clickX;
			double y = -1 * (p.getY() - clickY);

			if ((Math.sqrt(x * x + y * y) > 40)
				&& hovered != null
				&& draggedPoint != null)
			{
				allowArrow = true;
				orientation = Orientation.getAngleBetween(draggedPoint, hovered);
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
			if (primary.isCustomMode())
			{
				if (primary.getStoredModel() == null)
				{
					model = client.loadModel(29757);
				}
				else
				{
					model = primary.getStoredModel().getModel();
				}
			}
			else
			{
				model = client.loadModel((int) primary.getModelSpinner().getValue());
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
		previewObject.setOrientation((int) orientation);
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

	public static String getPluginVersion()
	{
		PluginHubManifest.DisplayData displayData = ExternalPluginManager.getDisplayData(CreatorsPlugin.class);

		if (displayData != null && displayData.getVersion() != null)
		{
			return displayData.getVersion();
		}

		return "1000.0.0";
	}

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
			clientThread.invokeLater(() ->
			{
				Tile tile = client.getTopLevelWorldView().getSelectedSceneTile();
				if (tile == null)
				{
					return;
				}

				LocalPoint lp = tile.getLocalLocation();
				if (lp == null)
				{
					return;
				}

				mousePressed = true;
				clickX = e.getPoint().getX();
				clickY = e.getPoint().getY();
				draggedPoint = lp;
			});
		}

		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		mousePressed = false;

		if (!config.enableCtrlHotkeys() || e.getButton() != MouseEvent.BUTTON1 || !client.isKeyPressed(KeyCode.KC_CONTROL))
		{
			return e;
		}

		if (draggedPoint == null)
		{
			return e;
		}

		double x = e.getX() - clickX;
		double y = -1 * (e.getY() - clickY);
		if (Math.sqrt(x * x + y * y) < 40)
		{
			return e;
		}

		clientThread.invokeLater(() ->
		{
			WorldView worldView = client.getTopLevelWorldView();
			Tile tile = worldView.getSelectedSceneTile();
			if (tile == null)
			{
				return;
			}

			LocalPoint lp = tile.getLocalLocation();
			if (lp == null)
			{
				return;
			}

			int orientation = (int) Orientation.getAngleBetween(draggedPoint, lp);

			Set<Character> selected = new HashSet<>(selectionManager.getSelected());
			for (Character character : selected)
			{
				character.setOrientation(orientation);
			}
		});

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
