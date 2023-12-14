package com.creatorskit;

import com.creatorskit.models.*;
import com.creatorskit.programming.*;
import com.creatorskit.saves.TransmogLoadOption;
import com.creatorskit.saves.TransmogSave;
import com.creatorskit.swing.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
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
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

@Slf4j
@Getter
@Setter
@PluginDescriptor(
		name = "Creator's Kit",
		description = "A suite of tools for creators",
		tags = {"tool", "creator", "content", "kit", "camera", "immersion"}
)
public class CreatorsPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

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
	private KeyManager keyManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ModelGetter modelGetter;

	@Inject
	private PathFinder pathFinder;

	@Inject
	private ModelFinder modelFinder;

	@Inject
	private Gson gson;
	
	private CreatorsPanel creatorsPanel;
	private NavigationButton navigationButton;
	private boolean overlaysActive = false;
	private final ArrayList<Character> characters = new ArrayList<>();
	private final ArrayList<CustomModel> storedModels = new ArrayList<>();
	private Animation[] loadedAnimations = new Animation[0];
	private Character selectedCharacter;
	private Character hoveredCharacter;
	private RuneLiteObject transmog;
	private CustomModel transmogModel;
	private int savedRegion = -1;
	private int savedPlane = -1;
	private AutoRotate autoRotateYaw = AutoRotate.OFF;
	private AutoRotate autoRotatePitch = AutoRotate.OFF;
	private final int BRIGHT_AMBIENT = 64;
	private final int BRIGHT_CONTRAST = 850;
	private final int DARK_AMBIENT = 128;
	private final int DARK_CONTRAST = 4000;
	private boolean pauseMode = true;
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

		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(overlay);
		keyManager.registerKeyListener(overlayKeyListener);
		keyManager.registerKeyListener(oculusOrbListener);
		keyManager.registerKeyListener(quickSpawnListener);
		keyManager.registerKeyListener(quickLocationListener);
		keyManager.registerKeyListener(quickRotateCWListener);
		keyManager.registerKeyListener(quickRotateCCWListener);
		keyManager.registerKeyListener(autoLeftListener);
		keyManager.registerKeyListener(autoRightListener);
		keyManager.registerKeyListener(autoUpListener);
		keyManager.registerKeyListener(autoDownListener);
		keyManager.registerKeyListener(addProgramStepListener);
		keyManager.registerKeyListener(removeProgramStepListener);
		keyManager.registerKeyListener(clearProgramStepListener);
		keyManager.registerKeyListener(resetListener);
		keyManager.registerKeyListener(playPauseListener);
		keyManager.registerKeyListener(playPauseAllListener);
		keyManager.registerKeyListener(resetAllListener);

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
				loadTransmog(TRANSMOG_DIR, TransmogLoadOption.BOTH);
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
		creatorsPanel.clearSidePanels(false);
		creatorsPanel.clearManagerPanels();
		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(overlayKeyListener);
		keyManager.unregisterKeyListener(oculusOrbListener);
		keyManager.unregisterKeyListener(quickSpawnListener);
		keyManager.unregisterKeyListener(quickLocationListener);
		keyManager.unregisterKeyListener(quickRotateCWListener);
		keyManager.unregisterKeyListener(quickRotateCCWListener);
		keyManager.unregisterKeyListener(autoLeftListener);
		keyManager.unregisterKeyListener(autoRightListener);
		keyManager.unregisterKeyListener(autoUpListener);
		keyManager.unregisterKeyListener(autoDownListener);
		keyManager.unregisterKeyListener(addProgramStepListener);
		keyManager.unregisterKeyListener(removeProgramStepListener);
		keyManager.unregisterKeyListener(clearProgramStepListener);
		keyManager.unregisterKeyListener(resetListener);
		keyManager.unregisterKeyListener(playPauseListener);
		keyManager.unregisterKeyListener(playPauseAllListener);
		keyManager.unregisterKeyListener(resetAllListener);
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

		int region = client.isInInstancedRegion() ? WorldPoint.fromLocalInstance(client, localPoint).getRegionID() : worldPoint.getRegionID();

		if (savedRegion != region)
			savedRegion = region;

		int plane = client.getPlane();
		if (savedPlane != plane)
		{
			savedPlane = plane;
			for (Character character : characters)
			{
				boolean active = character.isActive();
				setLocation(character, false, false, false, true);
				resetProgram(character, character.getProgram().getComp().isProgramActive());
				if (!active)
					despawnCharacter(character);
			}
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

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

		Player player = client.getLocalPlayer();

		for (Character character : characters)
		{
			Program program = character.getProgram();
			ProgramComp comp = program.getComp();
			RuneLiteObject runeLiteObject = character.getRuneLiteObject();
			boolean instance = client.isInInstancedRegion();

			if (!isInScene(character))
				continue;

			if (!comp.isProgramActive())
			{
				Animation animation = runeLiteObject.getAnimation();
				if (animation == null)
					continue;

				if (animation.getId() != comp.getIdleAnim())
					runeLiteObject.setAnimation(getAnimation(comp.getIdleAnim()));

				continue;
			}

			double speed = 128 * (comp.getSpeed() * 20 / 600);
			int currentStep = comp.getCurrentStep();

			int pathLength = instance ? comp.getPathLP().length : comp.getPathWP().length;
			if (currentStep >= pathLength)
			{
				if (comp.isLoop())
				{
					resetProgram(character, true);
					continue;
				}

				if (runeLiteObject.getAnimation() == null)
					continue;

				if (runeLiteObject.getAnimation().getId() != comp.getIdleAnim())
					runeLiteObject.setAnimation(getAnimation(comp.getIdleAnim()));

				continue;
			}

			Animation currentAnim = runeLiteObject.getAnimation();
			if (currentAnim != null && currentAnim.getId() != comp.getWalkAnim())
			{
				int walkAnimId = comp.getWalkAnim();
				if (walkAnimId == -1)
					walkAnimId = comp.getIdleAnim();

				if (currentAnim.getId() != walkAnimId)
					runeLiteObject.setAnimation(getAnimation(comp.getWalkAnim()));
			}

			LocalPoint start = runeLiteObject.getLocation();
			LocalPoint destination;

			if (instance)
			{
				destination = comp.getPathLP()[currentStep];
			}
			else
			{
				destination = LocalPoint.fromWorld(client, comp.getPathWP()[currentStep]);
			}

			if (destination == null)
				continue;

			int startX = start.getX();
			int startY = start.getY();
			int destX = destination.getX();
			int destY = destination.getY();

			int endX = startX;
			int endY = startY;

			double changeX = destX - startX;
			double changeY = destY - startY;
			double angle = Orientation.radiansToJAngle(Math.atan(changeY / changeX), changeX, changeY);

			character.setTargetOrientation((int) angle);

			if (destX != startX)
			{
				int change = ((int) speed * Orientation.orientationX(angle));
				endX = startX + change;
			}

			if (destY != startY)
			{
				int change = ((int) speed * Orientation.orientationY(angle));
				endY = startY + change;
			}

			if (endX == destX && endY == destY)
			{
				int nextStep = currentStep + 1;
				comp.setCurrentStep(nextStep);
				if (nextStep < pathLength)
				{
					LocalPoint nextPath;

					if (instance)
					{
						nextPath = comp.getPathLP()[currentStep];
					}
					else
					{
						nextPath = LocalPoint.fromWorld(client, comp.getPathWP()[currentStep]);
					}

					if (nextPath == null)
						continue;

					int nextX = nextPath.getSceneX();
					int nextY = nextPath.getSceneY();
					double nextChangeX = nextX - start.getSceneX();
					double nextChangeY = nextY - start.getSceneY();
					double nextAngle = Orientation.radiansToJAngle(Math.atan(nextChangeY / nextChangeX), nextChangeX, nextChangeY);
					character.setTargetOrientation((int) nextAngle);
				}
			}

			int orientation = runeLiteObject.getOrientation();
			int targetOrientation = character.getTargetOrientation();
			int turnSpeed = comp.getTurnSpeed();
			if (orientation != targetOrientation)
			{
				int newOrientation;
				int difference = Orientation.subtract(targetOrientation, orientation);

				if (difference > (turnSpeed * -1) && difference < turnSpeed)
				{
					newOrientation = targetOrientation;
				}
				else if (difference > 0)
				{
					newOrientation = Orientation.boundOrientation(orientation + turnSpeed);
				}
				else
				{
					newOrientation = Orientation.boundOrientation(orientation - turnSpeed);
				}

				runeLiteObject.setOrientation(newOrientation);
			}

			LocalPoint finalPoint = new LocalPoint(endX, endY);
			runeLiteObject.setLocation(finalPoint, client.getPlane());
		}

		TransmogPanel transmogPanel = creatorsPanel.getTransmogPanel();

		if (config.enableTransmog() && transmog != null)
		{
			if (player == null)
				return;

			LocalPoint localPoint = player.getLocalLocation();
			transmog.setLocation(localPoint, client.getPlane());
			transmog.setOrientation(player.getCurrentOrientation());
			if (!transmog.isActive())
				transmog.setActive(true);

			int playerAnimation = player.getAnimation();
			int playerPose = player.getPoseAnimation();
			Animation animation = transmog.getAnimation();

			int transmogAnimation = -1;
			if (animation != null)
				transmogAnimation = animation.getId();

			TransmogAnimationMode animationMode = transmogPanel.getTransmogAnimationMode();
			if (animationMode == TransmogAnimationMode.PLAYER)
			{
				if (playerAnimation == -1)
				{
					if (transmogAnimation != playerPose)
						transmog.setAnimation(getAnimation(playerPose));
				}
			}

			if (animationMode == TransmogAnimationMode.CUSTOM || animationMode == TransmogAnimationMode.MODIFIED)
			{
				if (playerAnimation == -1)
				{
					int pose = transmogPanel.getPoseAnimation();
					int walk = transmogPanel.getWalkAnimation();
					int run = transmogPanel.getRunAnimation();
					int backwards = transmogPanel.getBackwardsAnimation();
					int left = transmogPanel.getLeftAnimation();
					int right = transmogPanel.getRightAnimation();
					int rotate = transmogPanel.getRotateAnimation();

					if (animationMode == TransmogAnimationMode.MODIFIED)
					{
						if (pose == -1)
							pose = playerPose;
						if (walk == -1)
							walk = playerPose;
						if (run == -1)
							run = playerPose;
						if (backwards == -1)
							backwards = playerPose;
						if (left == -1)
							left = playerPose;
						if (right == -1)
							right = playerPose;
						if (rotate == -1)
							rotate = playerPose;
					}

					PoseAnimation poseAnimation = AnimationData.getPoseAnimation(playerPose);

					if (pose != -1 && poseAnimation == PoseAnimation.POSE)
					{
						if (transmogAnimation != pose)
							transmog.setAnimation(getAnimation(pose));
					}
					else if (walk != -1 && poseAnimation == PoseAnimation.WALK)
					{
						if (transmogAnimation != walk)
							transmog.setAnimation(getAnimation(walk));
					}
					else if (run != -1 && poseAnimation == PoseAnimation.RUN)
					{
						if (transmogAnimation != run)
							transmog.setAnimation(getAnimation(run));
					}
					else if (backwards != -1 && poseAnimation == PoseAnimation.BACKWARDS)
					{
						if (transmogAnimation != backwards)
							transmog.setAnimation(getAnimation(backwards));
					}
					else if (right != -1 && poseAnimation == PoseAnimation.SHUFFLE_RIGHT)
					{
						if (transmogAnimation != right)
							transmog.setAnimation(getAnimation(right));
					}
					else if (left != -1 && poseAnimation == PoseAnimation.SHUFFLE_LEFT)
					{
						if (transmogAnimation != left)
							transmog.setAnimation(getAnimation(left));
					}
					else if (rotate != -1 && poseAnimation == PoseAnimation.ROTATE)
					{
						if (transmogAnimation != rotate)
							transmog.setAnimation(getAnimation(rotate));
					}
					else if (animationMode == TransmogAnimationMode.MODIFIED)
					{
						transmog.setAnimation(getAnimation(playerPose));
					}
					else
					{
						if (transmogAnimation != walk)
							transmog.setAnimation(getAnimation(walk));
					}
				}
			}
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!config.enableTransmog() || transmog == null)
			return;

		TransmogPanel transmogPanel = creatorsPanel.getTransmogPanel();
		TransmogAnimationMode animationMode = transmogPanel.getTransmogAnimationMode();

		if (animationMode == TransmogAnimationMode.NONE)
			return;

		if (event.getActor() instanceof Player)
		{
			Player player = (Player) event.getActor();
			if (player != client.getLocalPlayer())
				return;

			int playerAnimation = player.getAnimation();
			Animation animation = transmog.getAnimation();
			int transmogAnimation = -1;
			if (animation != null)
				transmogAnimation = animation.getId();

			int action = transmogPanel.getActionAnimation();

			if (animationMode == TransmogAnimationMode.PLAYER && transmogAnimation != playerAnimation)
			{
				transmog.setAnimation(getAnimation(playerAnimation));
				return;
			}

			int[][] animationSwaps = transmogPanel.getAnimationSwaps();
			for (int[] swap : animationSwaps)
			{
				if (swap[0] == player.getAnimation() && transmogAnimation != swap[1])
				{
					transmog.setAnimation(getAnimation(swap[1]));
					return;
				}
			}

			if (animationMode == TransmogAnimationMode.MODIFIED && transmogAnimation != playerAnimation && action == -1)
				transmog.setAnimation(getAnimation(playerAnimation));

			if (animationMode == TransmogAnimationMode.MODIFIED && transmogAnimation != action && action != -1)
				transmog.setAnimation(getAnimation(action));

			if (animationMode == TransmogAnimationMode.CUSTOM && transmogAnimation != action)
				transmog.setAnimation(getAnimation(action));
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			boolean instance = client.isInInstancedRegion();

			for (Character character : characters)
			{
				boolean active = character.isActive();
				setLocation(character, false, false, false, true);
				resetProgram(character, character.getProgram().getComp().isProgramActive());
				if ((character.isInInstance() && instance && client.getPlane() == character.getInstancedPlane()) || (!character.isInInstance() && !instance))
					updateProgramPath(character.getProgram(), true, character.isInInstance());

				if (!active)
					despawnCharacter(character);
			}

			if (config.enableTransmog() && transmog != null)
			{
				transmog.setActive(false);
				transmog.setActive(true);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getKey().equals("orbSpeed"))
		{
			client.setOculusOrbNormalSpeed(config.orbSpeed());
		}

		if (event.getKey().equals("enableTransmog"))
		{
			if (transmog == null)
				return;

			clientThread.invokeLater(() ->
			{
				boolean enableTransmog = config.enableTransmog();
				transmog.setActive(enableTransmog);
				if (!enableTransmog)
				{
					transmog.setAnimation(getAnimation(-1));
				}
			});
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.rightClick())
			return;

		String target = event.getTarget();
		String option = event.getOption();

		Tile tile = client.getSelectedSceneTile();

		NPC npc = event.getMenuEntry().getNpc();
		if (npc != null && option.equals("Examine"))
		{
			modelGetter.storeNPC(-1, target, npc, "Store", false);
			modelGetter.sendToAnvilNPC(-2, target, npc);
			modelGetter.storeNPC(-3, target, npc, "Transmog", true);
		}

		if (tile != null)
		{
			if (option.equals("Walk here"))
			{
				GroundObject groundObject = tile.getGroundObject();
				if (groundObject != null)
				{
					Renderable renderable = groundObject.getRenderable();
					if (renderable instanceof Model)
					{
						Model model = (Model) groundObject.getRenderable();
						modelGetter.addGameObjectGetter(-1, "Store", "<col=FFFF>GroundObject", "GroundObject", model, groundObject.getId(), CustomModelType.CACHE_OBJECT, false);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>GroundObject", "GroundObject", groundObject.getId());
						modelGetter.addGameObjectGetter(-3, "Transmog", "<col=FFFF>GroundObject", "GroundObject", model, groundObject.getId(), CustomModelType.CACHE_OBJECT, true);
					}
				}

				DecorativeObject decorativeObject = tile.getDecorativeObject();
				if (decorativeObject != null)
				{
					Renderable renderable = decorativeObject.getRenderable();
					if (renderable instanceof Model)
					{
						Model model = (Model) decorativeObject.getRenderable();
						modelGetter.addGameObjectGetter(-1, "Store", "<col=FFFF>DecorativeObject", "DecorativeObject", model, decorativeObject.getId(), CustomModelType.CACHE_OBJECT, false);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>DecorativeObject", "DecorativeObject", decorativeObject.getId());
						modelGetter.addGameObjectGetter(-3, "Transmog", "<col=FFFF>DecorativeObject", "DecorativeObject", model, decorativeObject.getId(), CustomModelType.CACHE_OBJECT, true);
					}
				}

				WallObject wallObject = tile.getWallObject();
				if (wallObject != null)
				{
					Renderable renderable = wallObject.getRenderable1();
					if (renderable instanceof Model)
					{
						Model model = (Model) renderable;
						modelGetter.addGameObjectGetter(-1, "Store", "<col=FFFF>WallObject", "WallObject", model, wallObject.getId(), CustomModelType.CACHE_OBJECT, false);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>WallObject", "WallObject", wallObject.getId());
						modelGetter.addGameObjectGetter(-3, "Transmog", "<col=FFFF>WallObject", "WallObject", model, wallObject.getId(), CustomModelType.CACHE_OBJECT, true);
					}
				}

				List<TileItem> tileItems = tile.getGroundItems();
				if (tileItems != null)
				{
					for (TileItem tileItem : tileItems)
					{
						Model model = tileItem.getModel();
						modelGetter.addGameObjectGetter(-1, "Store", "<col=FFFF>Item", "Item", model, tileItem.getId(), CustomModelType.CACHE_GROUND_ITEM, false);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>Item", "Item", tileItem.getId());
						modelGetter.addGameObjectGetter(-3, "Transmog", "<col=FFFF>Item", "Item", model, tileItem.getId(), CustomModelType.CACHE_GROUND_ITEM, true);
					}
				}

				GameObject[] gameObjects = tile.getGameObjects();
				for (GameObject gameObject : gameObjects)
				{
					if (gameObject == null)
						continue;

					Renderable renderable = gameObject.getRenderable();
					if (renderable == null)
						continue;

					if (renderable instanceof Model)
					{
						Model model = (Model) renderable;
						modelGetter.addGameObjectGetter(-1, "Store", "<col=FFFF>GameObject", "GameObject", model, gameObject.getId(), CustomModelType.CACHE_OBJECT, false);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>GameObject", "GameObject", gameObject.getId());
						modelGetter.addGameObjectGetter(-3, "Transmog", "<col=FFFF>GameObject", "GameObject", model, gameObject.getId(), CustomModelType.CACHE_OBJECT, true);
					}
				}

				for (Character character : characters)
				{
					if (character.isActive() && character.getRuneLiteObject().getLocation().equals(tile.getLocalLocation()))
					{
						client.createMenuEntry(-1)
								.setOption("Select")
								.setTarget(ColorUtil.colorTag(Color.GREEN) + character.getName())
								.setType(MenuAction.RUNELITE)
								.onClick(e -> creatorsPanel.setSelectedCharacter(character, character.getObjectPanel()));
					}
				}
			}
		}

		Player player = event.getMenuEntry().getPlayer();
		if (player != null && option.equals("Trade with"))
		{
			modelGetter.addPlayerGetter(-1, target, "Store", player, false, false);
			modelGetter.addPlayerGetter(-2, target, "Anvil", player, true, false);
			modelGetter.addPlayerGetter(-3, target, "Transmog", player, false, true);
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && tile != null && option.equals("Walk here"))
		{
			if (tile.getLocalLocation().equals(localPlayer.getLocalLocation()))
			{
				modelGetter.addPlayerGetter(-1, localPlayer.getName(), "Store", localPlayer, false, false);
				modelGetter.addPlayerGetter(-2, localPlayer.getName(), "Anvil", localPlayer, true, false);
			}
		}
	}

	public void setLocation(Character character, boolean newLocation, boolean setToPlayer, boolean setToHoveredTile, boolean setToPathStart)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		boolean instance = client.isInInstancedRegion();

		if (!newLocation && !isInScene(character))
			return;

		if (instance)
		{
			setLocationInstance(character, setToPlayer, setToHoveredTile, setToPathStart);
			return;
		}

		setLocationNonInstance(character, setToPlayer, setToHoveredTile, setToPathStart);
	}

	public void setLocationNonInstance(Character character, boolean setToPlayer, boolean setToHoveredTile, boolean setToPathStart)
	{
		clientThread.invoke(() ->
		{
			LocalPoint localPoint;
			WorldPoint[] steps = character.getProgram().getComp().getStepsWP();

			if (setToPlayer)
			{
				localPoint = client.getLocalPlayer().getLocalLocation();
			}
			else if (setToHoveredTile)
			{
				Tile tile = client.getSelectedSceneTile();
				if (tile == null)
					return;

				localPoint = tile.getLocalLocation();
			}
			else if (setToPathStart && steps.length > 0)
			{
				localPoint = LocalPoint.fromWorld(client, steps[0]);
			}
			else
			{
				if (character.getNonInstancedPoint() == null)
				{
					localPoint = client.getLocalPlayer().getLocalLocation();
				}
				else
				{
					localPoint = LocalPoint.fromWorld(client, character.getNonInstancedPoint());
				}
			}

			if (localPoint == null)
				return;

			WorldPoint newLocation = WorldPoint.fromLocalInstance(client, localPoint);

			character.setLocationSet(true);

			if (newLocation != null)
			{
				pathFinder.transplantSteps(character, newLocation.getX(), newLocation.getY(), character.isInInstance(), false);
				character.setInInstance(false);
				character.setNonInstancedPoint(newLocation);
				updateProgramPath(character.getProgram(), false, false);
			}

			RuneLiteObject runeLiteObject = character.getRuneLiteObject();
			runeLiteObject.setActive(false);
			runeLiteObject.setLocation(localPoint, client.getPlane());
			runeLiteObject.setActive(true);
			runeLiteObject.setOrientation((int) character.getOrientationSpinner().getValue());
			character.setActive(true);
			character.getSpawnButton().setText("Despawn");
		});
	}

	public void setLocationInstance(Character character, boolean setToPlayer, boolean setToHoveredTile, boolean setToPathStart)
	{
		clientThread.invoke(() ->
		{
			LocalPoint localPoint;
			LocalPoint[] steps = character.getProgram().getComp().getStepsLP();

			if (setToPlayer)
			{
				localPoint = client.getLocalPlayer().getLocalLocation();
			}
			else if (setToHoveredTile)
			{
				Tile tile = client.getSelectedSceneTile();
				if (tile == null)
					return;

				localPoint = tile.getLocalLocation();
			}
			else if (setToPathStart && steps.length > 0)
			{
				localPoint = steps[0];
			}
			else
			{
				localPoint = character.getInstancedPoint();
			}

			if (localPoint == null)
				return;

			character.setInstancedPoint(localPoint);
			pathFinder.transplantSteps(character, localPoint.getSceneX(), localPoint.getSceneY(), character.isInInstance(), true);
			character.setLocationSet(true);
			character.setInInstance(true);
			character.setInstancedRegions(client.getMapRegions());
			character.setInstancedPlane(client.getPlane());
			updateProgramPath(character.getProgram(), false, true);
			RuneLiteObject runeLiteObject = character.getRuneLiteObject();
			runeLiteObject.setActive(false);
			runeLiteObject.setLocation(localPoint, client.getPlane());
			runeLiteObject.setActive(true);
			runeLiteObject.setOrientation((int) character.getOrientationSpinner().getValue());
			character.setActive(true);
			character.getSpawnButton().setText("Despawn");
		});
	}

	public boolean isInScene(Character character)
	{
		int[] mapRegions = client.getMapRegions();
		if (client.isInInstancedRegion() && character.isInInstance())
		{
			if (character.getInstancedPoint() == null)
				return false;

			if (character.getInstancedPlane() != client.getPlane())
				return false;

			//This function is finnicky with larger instances, in that only an exact region:region map will load
			//The alternative of finding any match will otherwise make spawns off if the regions don't match because the scenes won't exactly match
			Object[] mapRegionsObjects = {mapRegions};
			Object[] instancedRegionObjects = {character.getInstancedRegions()};
			return Arrays.deepEquals(mapRegionsObjects, instancedRegionObjects);
		}

		if (!client.isInInstancedRegion() && !character.isInInstance())
		{
			WorldPoint worldPoint = character.getNonInstancedPoint();
			if (worldPoint == null)
				return false;

			return worldPoint.isInScene(client);
		}

		return false;
	}

	public void toggleSpawn(JButton spawnButton, Character character)
	{
		if (character.getRuneLiteObject().isActive())
		{
			despawnCharacter(character);
			spawnButton.setText("Spawn");
			return;
		}

		spawnCharacter(character);
		spawnButton.setText("Despawn");

		if (!character.isLocationSet())
		{
			setLocation(character, true, true, false, false);
		}
	}

	public void spawnCharacter(Character character)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		character.setActive(true);
		clientThread.invoke(() -> runeLiteObject.setActive(true));
	}

	public void despawnCharacter(Character character)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		character.setActive(false);
		clientThread.invoke(() -> runeLiteObject.setActive(false));
	}

	public void setModel(Character character, boolean modelMode, int modelId)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() -> {
			if (modelMode)
			{
				CustomModel customModel = character.getStoredModel();
				Model model = customModel == null ? client.loadModel(29757) : customModel.getModel();
				runeLiteObject.setModel(model);
				return;
			}

			Model model = client.loadModel(modelId);
			runeLiteObject.setModel(model);
		});
	}

	public Animation getAnimation(int id)
	{
		for (Animation animation : loadedAnimations)
		{
			if (animation != null && animation.getId() == id)
				return animation;
		}

		Animation animation = client.loadAnimation(id);
		loadedAnimations = ArrayUtils.add(loadedAnimations, animation);
		return animation;
	}

	public void setAnimation(Character character, int animationId)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() ->
		{
			Animation animation = getAnimation(animationId);
			runeLiteObject.setAnimation(animation);
			loadedAnimations = ArrayUtils.removeAllOccurences(loadedAnimations, null);
		});
	}

	public void unsetAnimation(Character character)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() ->
		{
			Animation animation = getAnimation(-1);
			runeLiteObject.setAnimation(animation);
		});
	}

	public void setRadius(Character character, int radius)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() -> runeLiteObject.setRadius(radius));
	}

	public void addOrientation(Character character, int addition)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		int orientation = runeLiteObject.getOrientation();
		orientation += addition;
		if (orientation >= 2048)
			orientation -= 2048;

		if (orientation < 0)
			orientation += 2048;

		setOrientation(character, orientation);
		JPanel masterPanel = character.getObjectPanel();
		for (Component component : masterPanel.getComponents())
		{
			if (component instanceof JSpinner)
			{
				JSpinner spinner = (JSpinner) component;
				if (spinner.getName() != null && spinner.getName().equals("orientationSpinner"))
				{
					spinner.setValue(orientation);
					return;
				}
			}
		}
	}

	public void setOrientation(Character character, int orientation)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() -> runeLiteObject.setOrientation(orientation));
	}

	public Character buildCharacter(String name,
									ObjectPanel objectPanel,
									JTextField nameTextField,
									JButton setLocationButton,
									JButton spawnButton,
									JButton animationButton,
									JButton modelButton,
									int modelId,
									JSpinner modelSpinner,
									JComboBox<CustomModel> modelComboBox,
									boolean customModelMode,
									boolean minimized,
									int orientation,
									JSpinner orientationSpinner,
									int radius,
									JSpinner radiusSpinner,
									int animationId,
									JSpinner animationSpinner,
									Program program,
									JLabel programmerNameLabel,
									JSpinner programmerIdleSpinner,
									boolean active,
									WorldPoint savedWorldPoint,
									LocalPoint savedLocalPoint,
									int[] localPointRegion,
									int localPointPlane,
									boolean locatedInInstance)
	{
		RuneLiteObject runeLiteObject = client.createRuneLiteObject();
		runeLiteObject.setRadius(radius);
		runeLiteObject.setOrientation(orientation);

		CustomModel customModel = (CustomModel) modelComboBox.getSelectedItem();

		Character character = new Character(
				name,
				active,
				savedWorldPoint != null || savedLocalPoint != null,
				minimized,
				program,
				savedWorldPoint,
				savedLocalPoint,
				localPointRegion,
				localPointPlane,
				locatedInInstance,
				customModel,
				objectPanel,
				customModelMode,
				nameTextField,
				modelComboBox,
				spawnButton,
				modelButton,
				modelSpinner,
				animationSpinner,
				orientationSpinner,
				programmerNameLabel,
				programmerIdleSpinner,
				runeLiteObject,
				0);

		characters.add(character);

		runeLiteObject.setDrawFrontTilesFirst(true);
		runeLiteObject.setShouldLoop(true);
		setAnimation(character, animationId);
		setModel(character, customModelMode, modelId);

		setLocation(character, !character.isLocationSet(), false, false, false);
		runeLiteObject.setActive(active);
		character.setActive(active);

		nameTextField.addActionListener(e ->
		{
			character.setName(nameTextField.getText());
			programmerNameLabel.setText(nameTextField.getText());
		});

		setLocationButton.addActionListener(e ->
				setLocation(character, !character.isLocationSet(), false, false, false));

		spawnButton.addActionListener(e ->
				toggleSpawn(spawnButton, character));

		animationButton.addActionListener(e ->
		{
			Animation anim = runeLiteObject.getAnimation();

			if (anim == null)
			{
				animationButton.setText("Anim Off");
				programmerIdleSpinner.setValue((int) animationSpinner.getValue());
				setAnimation(character, (int) animationSpinner.getValue());
				return;
			}

			int animId = anim.getId();

			if (animId == -1)
			{
				animationButton.setText("Anim Off");
				programmerIdleSpinner.setValue((int) animationSpinner.getValue());
				setAnimation(character, (int) animationSpinner.getValue());
				return;
			}

			animationButton.setText("Anim On");
			unsetAnimation(character);
		});

		modelButton.addActionListener(e ->
		{
			if (character.isCustomMode())
			{
				character.setCustomMode(false);
				modelButton.setText("Custom");
				modelSpinner.setVisible(true);
				modelComboBox.setVisible(false);
				setModel(character, false, (int) modelSpinner.getValue());
			}
			else
			{
				character.setCustomMode(true);
				modelButton.setText("Id");
				modelSpinner.setVisible(false);
				modelComboBox.setVisible(true);
				setModel(character, true, -1);
			}
		});

		modelSpinner.addChangeListener(e ->
		{
			int modelNumber = (int) modelSpinner.getValue();
			setModel(character, false, modelNumber);
		});

		modelComboBox.addItemListener(e ->
		{
			CustomModel m = (CustomModel) modelComboBox.getSelectedItem();
			character.setStoredModel(m);
			if (modelComboBox.isVisible() && character == selectedCharacter)
				setModel(character, true, -1);
		});

		orientationSpinner.addChangeListener(e ->
		{
			int orient = (int) orientationSpinner.getValue();
			setOrientation(character, orient);
		});

		animationSpinner.addChangeListener(e ->
		{
			animationButton.setText("Anim Off");
			int animationNumber = (int) animationSpinner.getValue();
			setAnimation(character, animationNumber);
			programmerIdleSpinner.setValue(animationNumber);
		});

		radiusSpinner.addChangeListener(e ->
		{
			int rad = (int) radiusSpinner.getValue();
			setRadius(character, rad);
		});

		return character;
	}

	public void removeCharacters()
	{
		clientThread.invokeLater(() -> {
			for (Character character : characters)
			{
				RuneLiteObject runeLiteObject = character.getRuneLiteObject();
				runeLiteObject.setActive(false);
			}

			characters.clear();
		});
	}

	public void removeCharacters(Character[] charactersToRemove)
	{
		clientThread.invokeLater(() -> {
			for (Character character : charactersToRemove)
			{
				RuneLiteObject runeLiteObject = character.getRuneLiteObject();
				runeLiteObject.setActive(false);
				characters.remove(character);
			}
		});
	}

	public void removeCharacter(ObjectPanel objectPanel)
	{
		for (Character character : characters)
		{
			if (character.getObjectPanel() == objectPanel)
			{
				removeCharacter(character);
				return;
			}
		}
	}

	public void removeCharacter(Character character)
	{
		clientThread.invokeLater(() -> {
				RuneLiteObject runeLiteObject = character.getRuneLiteObject();
				runeLiteObject.setActive(false);
				characters.remove(character);
		});
	}

	public void sendChatMessage(String chatMessage)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		final String message = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(chatMessage).build();
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(message).build());
	}

	public Model createComplexModel(DetailedModel[] detailedModels, boolean setPriority, LightingStyle lightingStyle)
	{
		ModelData[] models = new ModelData[detailedModels.length];
		boolean[] facesToInvert = new boolean[0];

		for (int e = 0; e < detailedModels.length; e++)
		{
			DetailedModel detailedModel = detailedModels[e];
			ModelData modelData = client.loadModelData(detailedModel.getModelId());
			if (modelData == null)
				return null;

			modelData.cloneVertices().cloneColors();

			switch(detailedModel.getRotate())
			{
				case 0:
					break;
				case 1:
					modelData.rotateY270Ccw();
					break;
				case 2:
					modelData.rotateY180Ccw();
					break;
				case 3:
					modelData.rotateY90Ccw();
			}

			//swapping y and z, making y positive to align with traditional axes
			modelData.translate(detailedModel.getXTranslate() + detailedModel.getXTile() * 128, -1 * (detailedModel.getZTranslate() + detailedModel.getZTile() * 128), detailedModel.getYTranslate() + detailedModel.getYTile() * 128);
			modelData.scale(detailedModel.getXScale(), detailedModel.getZScale(), detailedModel.getYScale());

			boolean[] faceInvert = new boolean[modelData.getFaceCount()];
			Arrays.fill(faceInvert, detailedModel.isInvertFaces());
			facesToInvert = ArrayUtils.addAll(facesToInvert, faceInvert);

			String[] newColoursArray = detailedModel.getRecolourNew().split(",");
			short[] newColours = new short[newColoursArray.length];
			String[] oldColoursArray = detailedModel.getRecolourOld().split(",");
			short[] oldColours = new short[oldColoursArray.length];

			if (!detailedModel.getRecolourNew().isEmpty() && !detailedModel.getRecolourOld().isEmpty())
			{
				if (newColoursArray.length != oldColoursArray.length)
				{
					clientThread.invokeLater(() -> sendChatMessage("Please ensure that each model has the same number of New Colours as Old Colours"));
					return null;
				}

				try
				{
					for (int i = 0; i < oldColours.length; i++)
					{
						oldColours[i] = Short.parseShort(oldColoursArray[i]);
						newColours[i] = Short.parseShort(newColoursArray[i]);
					}
				}
				catch (Exception exception)
				{
					clientThread.invokeLater(() -> sendChatMessage("Please reformat your colour entry to CSV format (ex. 123,987,456"));
					return null;
				}
			}

			for (int i = 0; i < newColours.length; i++)
			{
				modelData.recolor(oldColours[i], newColours[i]);
			}

			models[e] = modelData;
		}

		ModelData modelData = client.mergeModels(models);

		int[] faces2 = modelData.getFaceIndices2();
		int[] faces3 = modelData.getFaceIndices3();
		int[] faces2Copy = Arrays.copyOf(faces2, faces2.length);
		for (int i = 0; i < modelData.getFaceCount(); i++)
		{
			if (facesToInvert[i])
			{
				faces2[i] = faces3[i];
				faces3[i] = faces2Copy[i];
			}
		}

		Model model;
		switch (lightingStyle)
		{
			default:
			case DEFAULT:
				model =  modelData.light();
				break;
			case ACTOR:
				model = modelData.light(BRIGHT_AMBIENT, BRIGHT_CONTRAST, -30, -50, -30);
				break;
			case NONE:
				model = modelData.light(DARK_AMBIENT, DARK_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
		}

		if (model == null)
			return null;

		if (setPriority)
		{
			byte[] renderPriorities = model.getFaceRenderPriorities();
			if (renderPriorities != null && renderPriorities.length > 0)
				Arrays.fill(renderPriorities, (byte) 0);
		}

		sendChatMessage("Model forged. Faces: " + model.getFaceCount() + ", Vertices: " + model.getVerticesCount());
		if (model.getFaceCount() >= 6200 && model.getVerticesCount() >= 3900)
			sendChatMessage("You've exceeded the max face count of 6200 or vertex count of 3900 in this model; any additional faces or vertices will not render");

		return model;
	}

	public void cacheToAnvil(ModelStats[] modelStatsArray, int[] kitRecolours, boolean player)
	{
		SwingUtilities.invokeLater(() ->
		{
			for (ModelStats modelStats : modelStatsArray)
			{
				if (player)
				{
					String name = "Item";
					if (modelStats.getBodyPart() != BodyPart.NA)
						name = modelStats.getBodyPart().toString();

					StringBuilder recolourNew = new StringBuilder();
					StringBuilder recolourOld = new StringBuilder();

					String itemRecolourNew = ModelFinder.shortArrayToString(modelStats.getRecolourTo());
					String itemRecolourOld = ModelFinder.shortArrayToString(modelStats.getRecolourFrom());
					String kitRecolourNew = KitRecolourer.getKitRecolourNew(modelStats.getBodyPart(), kitRecolours);
					String kitRecolourOld = KitRecolourer.getKitRecolourOld(modelStats.getBodyPart());

					recolourNew.append(itemRecolourNew);
					recolourOld.append(itemRecolourOld);

					if (!kitRecolourNew.equals(""))
					{
						if (!itemRecolourNew.equals(""))
							recolourNew.append(",");

						recolourNew.append(kitRecolourNew);
					}

					if (!kitRecolourOld.equals(""))
					{
						if (!itemRecolourOld.equals(""))
							recolourOld.append(",");

						recolourOld.append(kitRecolourOld);
					}

					creatorsPanel.getModelAnvil().createComplexPanel(
							name,
							modelStats.getModelId(),
							9,
							0, 0, 0,
							0, 0, 0,
							128, 128, 128,
							0,
							recolourNew.toString(),
							recolourOld.toString(),
							false);

					continue;
				}

				creatorsPanel.getModelAnvil().createComplexPanel(
						"Name",
						modelStats.getModelId(),
						8,
						0, 0, 0,
						0, 0, 0,
						128, 128, 128,
						0,
						ModelFinder.shortArrayToString(modelStats.getRecolourTo()),
						ModelFinder.shortArrayToString(modelStats.getRecolourFrom()),
						false);
			}
		});
	}

	public void cacheToAnvil(CustomModelType type, int id)
	{
		Thread thread = new Thread(() ->
		{
			ModelStats[] modelStats;
			String name;

			switch (type)
			{
				case CACHE_NPC:
					modelStats = modelFinder.findModelsForNPC(id);
					name = modelFinder.getLastFound();
					break;
				default:
				case CACHE_OBJECT:
					modelStats = modelFinder.findModelsForObject(id);
					name = modelFinder.getLastFound();
			}

			cacheToAnvil(modelStats, new int[0], false);
			sendChatMessage("Model sent to Anvil: " + name);
		});
		thread.start();
	}

	public void cacheToCustomModel(CustomModelType type, int id)
	{
		Thread thread = new Thread(() ->
		{
			ModelStats[] modelStats;
			String name;
			CustomModelComp comp;

			switch (type)
			{
				case CACHE_NPC:
					modelStats = modelFinder.findModelsForNPC(id);
					name = modelFinder.getLastFound();
					comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, id, modelStats, null, null, LightingStyle.ACTOR, false, name);
					break;
				default:
				case CACHE_OBJECT:
					modelStats = modelFinder.findModelsForObject(id);
					name = modelFinder.getLastFound();
					comp = new CustomModelComp(0, CustomModelType.CACHE_OBJECT, id, modelStats, null, null, LightingStyle.DEFAULT, false, name);
			}

			clientThread.invokeLater(() ->
			{
				Model model = constructModelFromCache(modelStats, new int[0], false, true);
				CustomModel customModel = new CustomModel(model, comp);
				addCustomModel(customModel, false);
				sendChatMessage("Model stored: " + name);
			});
		});
		thread.start();
	}

	public Model constructModelFromCache(ModelStats[] modelStatsArray, int[] kitRecolours, boolean player, boolean actorLighting)
	{
		ModelData[] mds = new ModelData[modelStatsArray.length];

		for (int i = 0; i < modelStatsArray.length; i++)
		{
			ModelStats modelStats = modelStatsArray[i];
			ModelData modelData = client.loadModelData(modelStats.getModelId());

			if (modelData == null)
				continue;

			modelData.cloneColors();
			for (short s = 0; s < modelStats.getRecolourFrom().length; s++)
				modelData.recolor(modelStats.getRecolourFrom()[s], modelStats.getRecolourTo()[s]);

			if (player)
				KitRecolourer.recolourKitModel(modelData, modelStats.getBodyPart(), kitRecolours);

			mds[i] = modelData;
		}

		if (actorLighting)
			return client.mergeModels(mds).light(BRIGHT_AMBIENT, BRIGHT_CONTRAST, -30, -50, -30);

		return client.mergeModels(mds).light();
	}

	public void customModelToAnvil(CustomModel customModel)
	{
		SwingUtilities.invokeLater(() ->
		{
			CustomModelComp comp = customModel.getComp();
			sendChatMessage("Model sent to Anvil: " + comp.getName());
			ModelAnvil modelAnvil = creatorsPanel.getModelAnvil();
			modelAnvil.getPriorityCheckBox().setSelected(comp.isPriority());
			modelAnvil.getLightingComboBox().setSelectedItem(comp.getLightingStyle());
			modelAnvil.getNameField().setText(comp.getName());

			if (comp.getModelStats() == null)
			{
				DetailedModel[] detailedModels = comp.getDetailedModels();
				for (DetailedModel detailedModel : detailedModels)
					modelAnvil.createComplexPanel(detailedModel);

				return;
			}

			switch(comp.getType())
			{
				case FORGED:
				case CACHE_NPC:
				case CACHE_OBJECT:
				case CACHE_GROUND_ITEM:
					cacheToAnvil(comp.getModelStats(), comp.getKitRecolours(), false);
					break;
				case CACHE_PLAYER:
					cacheToAnvil(comp.getModelStats(), comp.getKitRecolours(), true);
			}
		});
	}

	public void loadCustomModelToAnvil(File file, boolean priority, LightingStyle lightingStyle, String name)
	{
		try
		{
			Reader reader = Files.newBufferedReader(file.toPath());
			CustomModelComp comp = gson.fromJson(reader, CustomModelComp.class);
			ModelAnvil modelAnvil = creatorsPanel.getModelAnvil();
			SwingUtilities.invokeLater(() -> {
				for (DetailedModel detailedModel : comp.getDetailedModels())
				{
					modelAnvil.createComplexPanel(detailedModel);
				}
			});
			modelAnvil.getLightingComboBox().setSelectedItem(comp.getLightingStyle());
			modelAnvil.getPriorityCheckBox().setSelected(comp.isPriority());
			modelAnvil.getNameField().setText(comp.getName());
			reader.close();
			return;
		}
		catch (Exception e)
		{
			sendChatMessage("The file chosen is possibly in an older v1.2 file. Attempting conversion...");
		}

		try
		{
			Reader reader = Files.newBufferedReader(file.toPath());
			DetailedModel[] detailedModels = gson.fromJson(reader, DetailedModel[].class);
			SwingUtilities.invokeLater(() -> {
				for (DetailedModel detailedModel : detailedModels)
				{
					creatorsPanel.getModelAnvil().createComplexPanel(detailedModel);
				}
			});
			reader.close();

			CustomModelComp comp = new CustomModelComp(0, CustomModelType.FORGED, -1, null, null, detailedModels, lightingStyle, priority, name);
			try
			{
				file.delete();
				String fileName = file.getPath();
				File newFile = new File(fileName);
				FileWriter writer = new FileWriter(newFile, false);
				String string = gson.toJson(comp);
				writer.write(string);
				writer.close();
				sendChatMessage("The chosen v1.2 file has been successfully updated to a v1.3 file for future use.");
			}
			catch (IOException e)
			{
				e.printStackTrace();
				sendChatMessage("An error occurred while trying to convert this file to a .json file.");
			}

			return;
		}
		catch (Exception e)
		{
			sendChatMessage("The file chosen is possibly an older v1.0 file. Attempting conversion...");
		}

		convertTextToJson(file, priority, lightingStyle, name);
	}

	public void loadCustomModel(File file, boolean priority, LightingStyle lightingStyle, String name)
	{
		try
		{
			Reader reader = Files.newBufferedReader(file.toPath());
			CustomModelComp comp = gson.fromJson(reader, CustomModelComp.class);
			clientThread.invokeLater(() -> {
				Model model = createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle());
				CustomModel customModel = new CustomModel(model, comp);
				addCustomModel(customModel, false);
			});
			reader.close();
			return;
		}
		catch (Exception e)
		{
			sendChatMessage("The file chosen is possibly in an older v1.2 file. Attempting conversion...");
		}

		try
		{
			Reader reader = Files.newBufferedReader(file.toPath());
			DetailedModel[] detailedModels = gson.fromJson(reader, DetailedModel[].class);
			CustomModelComp comp = new CustomModelComp(0, CustomModelType.FORGED, -1, null, null, detailedModels, lightingStyle, priority, name);

			clientThread.invokeLater(() -> {
				Model model = createComplexModel(detailedModels, priority, lightingStyle);
				CustomModel customModel = new CustomModel(model, comp);
				addCustomModel(customModel, false);
			});
			reader.close();

			try
			{
				file.delete();
				String fileName = file.getPath();
				File newFile = new File(fileName);
				FileWriter writer = new FileWriter(newFile, false);
				String string = gson.toJson(comp);
				writer.write(string);
				writer.close();

				sendChatMessage("The chosen v1.2 file has been successfully updated to a v1.3 file for future use.");
			}
			catch (IOException e)
			{
				e.printStackTrace();
				sendChatMessage("An error occurred while trying to convert this v1.2 file to a v1.3 file.");
			}

			return;
		}
		catch (Exception e)
		{
			sendChatMessage("The file chosen is possibly an older v1.0 file. Attempting conversion...");
		}

		convertTextToJson(file, priority, lightingStyle, name);
	}

	private void convertTextToJson(File file, boolean priority, LightingStyle lightingStyle, String customModelName)
	{
		ArrayList<DetailedModel> list = new ArrayList<>();

		try
		{
			Scanner myReader = new Scanner(file);
			myReader.nextLine();
			String s = "";

			while ((s = myReader.nextLine()) != null) {
				String name = "";
				int modelId = 0;
				int group = 1;
				int xTile = 0;
				int yTile = 0;
				int zTile = 0;
				int xTranslate = 0;
				int yTranslate = 0;
				int zTranslate = 0;
				int xScale = 0;
				int yScale = 0;
				int zScale = 0;
				int rotate = 0;
				String newColours = "";
				String oldColours = "";
				boolean setBreak = false;

				String data = "";
				try
				{
					while (!(data = myReader.nextLine()).equals(""))
					{
						if (data.startsWith("name="))
						{
							String[] split = data.split("=");
							if (split.length > 1)
								name = split[1];
						}

						if (data.startsWith("modelid="))
							modelId = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("group="))
							group = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("xtile="))
							xTile = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("ytile="))
							yTile = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("ztile="))
							zTile = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("xt="))
							xTranslate = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("yt="))
							yTranslate = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("zt="))
							zTranslate = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("xs="))
							xScale = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("ys="))
							yScale = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("zs="))
							zScale = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("r="))
							rotate = Integer.parseInt(data.split("=")[1]);

						if (data.startsWith("n="))
							newColours = data.replaceAll("n=", "");

						if (data.startsWith("o="))
							oldColours = data.replaceAll("o=", "");
					}
				}
				catch (NoSuchElementException e)
				{
					setBreak = true;
				}

				DetailedModel detailedModel = new DetailedModel(name, modelId, group, xTile, yTile, zTile, xTranslate, yTranslate, zTranslate, xScale, yScale, zScale, rotate, newColours, oldColours, false);
				list.add(detailedModel);
				if (setBreak)
					break;
			}

			myReader.close();

			SwingUtilities.invokeLater(() ->
			{
				for (DetailedModel detailedModel : list)
				{
					creatorsPanel.getModelAnvil().createComplexPanel(
							detailedModel.getName(),
							detailedModel.getModelId(),
							detailedModel.getGroup(),
							detailedModel.getXTile(),
							detailedModel.getYTile(),
							detailedModel.getZTile(),
							detailedModel.getXTranslate(),
							detailedModel.getYTranslate(),
							detailedModel.getZTranslate(),
							detailedModel.getXScale(),
							detailedModel.getYScale(),
							detailedModel.getZScale(),
							detailedModel.getRotate(),
							detailedModel.getRecolourNew(),
							detailedModel.getRecolourOld(),
							detailedModel.isInvertFaces());
				}
			});

			try
			{
				String fileName = file.getPath();
				if (fileName.endsWith(".txt"))
				{
					fileName = fileName.substring(0, fileName.length() - 4);
				}

				File newFile = new File(fileName + ".json");
				FileWriter writer = new FileWriter(newFile, false);
				DetailedModel[] detailedModels = (DetailedModel[]) list.toArray();
				CustomModelComp comp = new CustomModelComp(0, CustomModelType.FORGED, -1, null, null, detailedModels, lightingStyle, priority, customModelName);
				String string = gson.toJson(comp);
				writer.write(string);
				writer.close();
				file.delete();
				sendChatMessage("The chosen v1.0 file has been successfully updated to v1.3 for future use.");
			}
			catch (IOException e)
			{
				e.printStackTrace();
				sendChatMessage("An error occurred while trying to convert this file from a v1.0 to a v1.3 file.");
			}
		}
		catch (FileNotFoundException e)
		{
			sendChatMessage("An error occurred while trying to convert this file from a v1.0 to a v1.3 file.");
			e.printStackTrace();
		}
	}

	public void loadTransmog(File file, TransmogLoadOption transmogLoadOption)
	{
		try
		{
			Reader reader = Files.newBufferedReader(file.toPath());
			TransmogSave transmogSave = gson.fromJson(reader, TransmogSave.class);
			CustomModelComp comp = transmogSave.getCustomModelComp();
			if (comp != null)
			{
				DetailedModel[] detailedModels = comp.getDetailedModels();
				if (detailedModels == null)
				{
					detailedModels = creatorsPanel.getModelOrganizer().modelToDetailedPanels(comp);
					comp.setDetailedModels(detailedModels);
				}
			}

			reader.close();

			boolean loadCustomModel = false;
			switch (transmogLoadOption)
			{
				case ANIMATIONS:
					creatorsPanel.getTransmogPanel().loadTransmog(transmogSave);
					break;
				case CUSTOM_MODEL:
					if (comp != null)
						loadCustomModel = true;
					break;
				case BOTH:
					creatorsPanel.getTransmogPanel().loadTransmog(transmogSave);
					if (comp != null)
						loadCustomModel = true;
			}

			if (loadCustomModel)
			{
				clientThread.invokeLater(() -> {
					Model model = createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle());
					CustomModel customModel = new CustomModel(model, comp);
					addCustomModel(customModel, false);
					creatorsPanel.getModelOrganizer().setTransmog(customModel);
				});
			}
		}
		catch (Exception e)
		{
			sendChatMessage("Failed to load the selected Transmog. Make sure you selected an appropriate transmog file.");
		}
	}

	public void addCustomModel(CustomModel customModel, boolean setComboBox)
	{
		SwingUtilities.invokeLater(() -> creatorsPanel.addModelOption(customModel, setComboBox));
		storedModels.add(customModel);
	}

	public void removeCustomModel(CustomModel customModel)
	{
		SwingUtilities.invokeLater(() -> creatorsPanel.removeModelOption(customModel));
		storedModels.remove(customModel);
	}

	public void updateProgramPath(Program program, boolean gameStateChanged, boolean instanced)
	{
		if (instanced)
		{
			updateInstancedProgramPath(program, gameStateChanged);
			return;
		}

		updateNonInstancedProgramPath(program, gameStateChanged);
	}

	public void updateInstancedProgramPath(Program program, boolean gameStateChanged)
	{
		ProgramComp comp = program.getComp();
		LocalPoint[] stepsLP = comp.getStepsLP();
		LocalPoint[] pathLP = new LocalPoint[0];
		Coordinate[] allCoordinates = new Coordinate[0];

		if (stepsLP.length < 2)
		{
			comp.setPathLP(pathLP);
			comp.setCoordinates(allCoordinates);
			return;
		}

		for (int i = 0; i < stepsLP.length - 1; i++)
		{
			Coordinate[] coordinates;
			coordinates = pathFinder.getPath(stepsLP[i], stepsLP[i + 1], comp.getMovementType());

			if (coordinates == null)
			{
				if (!gameStateChanged)
					sendChatMessage("A path could not be found.");

				comp.setPathLP(new LocalPoint[0]);
				comp.setCoordinates(new Coordinate[0]);
				return;
			}

			allCoordinates = ArrayUtils.addAll(allCoordinates, coordinates);

			Direction direction = Direction.UNSET;

			for (int c = 0; c < coordinates.length - 1; c++)
			{
				int x = coordinates[c].getColumn();
				int y = coordinates[c].getRow();
				int nextX = coordinates[c + 1].getColumn();
				int nextY = coordinates[c + 1].getRow();
				int changeX = nextX - x;
				int changeY = nextY - y;

				Direction newDirection = Direction.getDirection(changeX, changeY);
				if (direction == Direction.UNSET || direction != newDirection)
				{
					direction = newDirection;
					LocalPoint localPoint = LocalPoint.fromScene(x, y);
					pathLP = ArrayUtils.add(pathLP, localPoint);
				}
			}
		}

		pathLP = ArrayUtils.add(pathLP, stepsLP[stepsLP.length - 1]);
		comp.setPathLP(pathLP);
		comp.setCoordinates(allCoordinates);
	}

	public void updateNonInstancedProgramPath(Program program, boolean gameStateChanged)
	{
		ProgramComp comp = program.getComp();
		WorldPoint[] stepsWP = comp.getStepsWP();
		WorldPoint[] pathWP = new WorldPoint[0];
		Coordinate[] allCoordinates = new Coordinate[0];

		if (stepsWP.length < 2)
		{
			comp.setPathWP(pathWP);
			comp.setCoordinates(allCoordinates);
			return;
		}

		for (int i = 0; i < stepsWP.length - 1; i++)
		{
			Coordinate[] coordinates;
			coordinates = pathFinder.getPath(stepsWP[i], stepsWP[i + 1], comp.getMovementType());

			if (coordinates == null)
			{
				if (!gameStateChanged)
					sendChatMessage("A path could not be found.");

				comp.setPathWP(new WorldPoint[0]);
				comp.setCoordinates(new Coordinate[0]);
				return;
			}

			allCoordinates = ArrayUtils.addAll(allCoordinates, coordinates);

			Direction direction = Direction.UNSET;

			for (int c = 0; c < coordinates.length - 1; c++)
			{
				int x = coordinates[c].getColumn();
				int y = coordinates[c].getRow();
				int nextX = coordinates[c + 1].getColumn();
				int nextY = coordinates[c + 1].getRow();
				int changeX = nextX - x;
				int changeY = nextY - y;

				Direction newDirection = Direction.getDirection(changeX, changeY);
				if (direction == Direction.UNSET || direction != newDirection)
				{
					direction = newDirection;
					LocalPoint localPoint = LocalPoint.fromScene(x, y);
					pathWP = ArrayUtils.add(pathWP, WorldPoint.fromLocalInstance(client, localPoint));

				}
			}
		}

		pathWP = ArrayUtils.add(pathWP, stepsWP[stepsWP.length - 1]);
		comp.setPathWP(pathWP);
		comp.setCoordinates(allCoordinates);
	}

	public void resetProgram(Character character, boolean restart)
	{
		Program program = character.getProgram();
		ProgramComp comp = program.getComp();
		comp.setCurrentStep(0);

		if (!isInScene(character))
			return;

		boolean resetLocation = true;
		if (character.isInInstance())
		{
			if (comp.getStepsLP().length == 0)
				return;

			LocalPoint lp = comp.getStepsLP()[0];
			if (lp == null)
				return;

			if (!lp.isInScene())
				return;

			int arrayLength = comp.getStepsLP().length;
			if (restart && lp.distanceTo(comp.getStepsLP()[arrayLength - 1]) == 0)
				resetLocation = false;
		}

		if (!character.isInInstance())
		{
			if (comp.getStepsWP().length == 0)
				return;

			WorldPoint wp = comp.getStepsWP()[0];
			if (wp == null)
				return;

			int arrayLength = comp.getStepsWP().length;
			if (restart && wp.distanceTo(comp.getStepsWP()[arrayLength - 1]) == 0)
				resetLocation = false;
		}

		if (resetLocation)
			setLocation(character, false, false, false, true);

		comp.setProgramActive(restart);
	}

	public void updatePanelComboBoxes()
	{
		SwingUtilities.invokeLater(() ->
		{
			for (JComboBox<CustomModel> comboBox : creatorsPanel.getComboBoxes())
				comboBox.updateUI();
		});
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
		}
	};

	private final HotkeyListener oculusOrbListener = new HotkeyListener(() -> config.toggleOrbHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (client.getOculusOrbState() == 1)
			{
				client.setOculusOrbState(0);
				client.setOculusOrbNormalSpeed(12);
				return;
			}

			client.setOculusOrbState(1);
			client.setOculusOrbNormalSpeed(config.orbSpeed());
		}
	};

	private final HotkeyListener quickSpawnListener = new HotkeyListener(() -> config.quickSpawnHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				toggleSpawn(selectedCharacter.getSpawnButton(), selectedCharacter);
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
				setLocation(selectedCharacter, true, false, true, false);
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
			if (selectedCharacter != null)
			{
				Tile tile = client.getSelectedSceneTile();
				if (tile == null)
					return;

				Program program = selectedCharacter.getProgram();
				boolean isInScene = isInScene(selectedCharacter);

				if (isInScene && client.isInInstancedRegion())
				{
					LocalPoint[] steps = program.getComp().getStepsLP();

					if (steps.length == 0)
					{
						setLocation(selectedCharacter, true, false, true, false);
					}

					steps = ArrayUtils.add(steps, tile.getLocalLocation());
					program.getComp().setStepsLP(steps);
					updateProgramPath(program, false, selectedCharacter.isInInstance());
					return;
				}

				if (!isInScene && client.isInInstancedRegion())
				{
					program.getComp().setStepsLP(new LocalPoint[]{tile.getLocalLocation()});
					program.getComp().setStepsWP(new WorldPoint[0]);
					setLocation(selectedCharacter, true, false, true, false);
					updateProgramPath(program, false, selectedCharacter.isInInstance());
					return;
				}

				if (isInScene && !client.isInInstancedRegion())
				{
					WorldPoint[] steps = program.getComp().getStepsWP();

					if (steps.length == 0)
					{
						setLocation(selectedCharacter, true, false, true, false);
					}

					WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
					steps = ArrayUtils.add(steps, worldPoint);
					program.getComp().setStepsWP(steps);
					updateProgramPath(program, false, selectedCharacter.isInInstance());
					return;
				}

				if (!isInScene && !client.isInInstancedRegion())
				{
					WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
					program.getComp().setStepsWP(new WorldPoint[]{worldPoint});
					program.getComp().setStepsLP(new LocalPoint[0]);
					setLocation(selectedCharacter, true, false, true, false);
					updateProgramPath(program, false, selectedCharacter.isInInstance());
				}
			}
		}
	};

	private final HotkeyListener removeProgramStepListener = new HotkeyListener(() -> config.removeProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				if (!isInScene(selectedCharacter))
					return;

				Tile tile = client.getSelectedSceneTile();
				if (tile == null)
					return;

				Program program = selectedCharacter.getProgram();
				ProgramComp comp = program.getComp();

				if (client.isInInstancedRegion())
				{
					LocalPoint[] steps = comp.getStepsLP();
					steps = ArrayUtils.removeElement(steps, tile.getLocalLocation());
					comp.setStepsLP(steps);
				}
				else
				{
					WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
					WorldPoint[] steps = comp.getStepsWP();
					steps = ArrayUtils.removeElement(steps, worldPoint);
					comp.setStepsWP(steps);
				}

				comp.setCurrentStep(0);
				updateProgramPath(program, false, selectedCharacter.isInInstance());
				setLocation(selectedCharacter, false, false, false, true);
			}
		}
	};

	private final HotkeyListener clearProgramStepListener = new HotkeyListener(() -> config.clearProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				Program program = selectedCharacter.getProgram();
				program.getComp().setStepsWP(new WorldPoint[0]);
				program.getComp().setStepsLP(new LocalPoint[0]);
				program.getComp().setProgramActive(false);
				updateProgramPath(program, false, selectedCharacter.isInInstance());
				setLocation(selectedCharacter, false, false, false, false);
			}
		}
	};

	private final HotkeyListener playPauseListener = new HotkeyListener(() -> config.playPauseHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
			{
				ProgramComp comp = selectedCharacter.getProgram().getComp();
				comp.setProgramActive(!comp.isProgramActive());
			}
		}
	};

	private final HotkeyListener playPauseAllListener = new HotkeyListener(() -> config.playPauseAllHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			pauseMode = !pauseMode;

			for (Character character : characters)
				character.getProgram().getComp().setProgramActive(!pauseMode);
		}
	};

	private final HotkeyListener resetListener = new HotkeyListener(() -> config.resetHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedCharacter != null)
				resetProgram(selectedCharacter, false);
		}
	};

	private final HotkeyListener resetAllListener = new HotkeyListener(() -> config.resetAllHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			for (Character character : characters)
				resetProgram(character, false);
		}
	};

	@Provides
	CreatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CreatorsConfig.class);
	}
}
