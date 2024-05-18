package com.creatorskit;

import com.creatorskit.models.*;
import com.creatorskit.programming.*;
import com.creatorskit.saves.TransmogLoadOption;
import com.creatorskit.saves.TransmogSave;
import com.creatorskit.swing.*;
import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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
public class CreatorsPlugin extends Plugin implements MouseListener {
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
	private MouseManager mouseManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ModelGetter modelGetter;

	@Inject
	private PathFinder pathFinder;

	@Inject
	private ModelFinder modelFinder;

	@Inject
	private ModelExporter modelExporter;

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
	private RuneLiteObject previewObject;
	private Model previewArrow;
	private CustomModel transmogModel;
	private int savedRegion = -1;
	private int savedPlane = -1;
	private AutoRotate autoRotateYaw = AutoRotate.OFF;
	private AutoRotate autoRotatePitch = AutoRotate.OFF;
	private int oculusOrbSpeed = 36;
	private double clickX;
	private double clickY;
	private double mouseX;
	private double mouseY;
	private boolean mousePressed = false;
	private boolean pauseMode = true;
	private boolean autoSetupPathFound = true;
	private boolean autoTransmogFound = true;
	private boolean controlDown = false;

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
		keyManager.registerKeyListener(orbSpeedListener);
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
		keyManager.registerKeyListener(resetListener);
		keyManager.registerKeyListener(playPauseListener);
		keyManager.registerKeyListener(playPauseAllListener);
		keyManager.registerKeyListener(resetAllListener);
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
				loadTransmog(TRANSMOG_DIR, TransmogLoadOption.BOTH);
			}
			else
			{
				autoTransmogFound = false;
			}
		}

		oculusOrbSpeed = config.orbSpeed();
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
		keyManager.unregisterKeyListener(orbSpeedListener);
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
		keyManager.unregisterKeyListener(resetListener);
		keyManager.unregisterKeyListener(playPauseListener);
		keyManager.unregisterKeyListener(playPauseAllListener);
		keyManager.unregisterKeyListener(resetAllListener);
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

		int region = client.isInInstancedRegion() ? WorldPoint.fromLocalInstance(client, localPoint).getRegionID() : worldPoint.getRegionID();

		if (savedRegion != region)
			savedRegion = region;

		int plane = client.getPlane();
		if (savedPlane != plane)
		{
			savedPlane = plane;
			for (int i = 0; i < characters.size(); i++)
			{
				Character character = characters.get(i);
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

		updatePreviewObject(client.getSelectedSceneTile());

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

		for (int i = 0; i < characters.size(); i++)
		{
			Character character = characters.get(i);
			Program program = character.getProgram();
			ProgramComp comp = program.getComp();
			RuneLiteObject runeLiteObject = character.getRuneLiteObject();
			boolean instance = client.isInInstancedRegion();

			if (runeLiteObject == null)
				continue;

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
			oculusOrbSpeed = config.orbSpeed();
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
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!config.enableCtrlHotkeys())
		{
			return;
		}

		if (client.isKeyPressed(KeyCode.KC_CONTROL))
		{
			if (selectedCharacter != null)
			{
				client.createMenuEntry(-1)
						.setOption(ColorUtil.prependColorTag("Relocate", Color.ORANGE))
						.setTarget(ColorUtil.colorTag(Color.GREEN) + selectedCharacter.getName())
						.setType(MenuAction.RUNELITE)
						.onClick(e -> setLocation(selectedCharacter, true, false, true, false));
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.rightClick() && !config.transmogRightClick() && !config.rightSpotAnim() && !config.exportRightClick())
			return;

		String target = event.getTarget();
		String option = event.getOption();

		Tile tile = client.getSelectedSceneTile();

		NPC npc = event.getMenuEntry().getNpc();
		if (npc != null && option.equals("Examine"))
		{
			if (config.rightClick())
			{
				if (client.isKeyPressed(KeyCode.KC_CONTROL))
				{
					modelGetter.storeNPC(-1, target, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), npc, ModelMenuOption.STORE_AND_ADD);
				}
				else
				{
					modelGetter.storeNPC(-1, target, ColorUtil.prependColorTag("Store", Color.ORANGE), npc, ModelMenuOption.STORE);
				}
				modelGetter.sendToAnvilNPC(-2, target, npc);
			}

			if (config.transmogRightClick())
			{
				modelGetter.storeNPC(-3, target, ColorUtil.prependColorTag("Transmog", Color.ORANGE), npc, ModelMenuOption.TRANSMOG);
			}

			if (config.rightSpotAnim())
			{
				if (client.isKeyPressed(KeyCode.KC_CONTROL))
				{
					modelGetter.addSpotAnimGetter(-4, target, ColorUtil.prependColorTag("SpotAnim-Store-Add", Color.ORANGE), npc.getSpotAnims(), ModelMenuOption.STORE_AND_ADD);
				}
				else
				{
					modelGetter.addSpotAnimGetter(-4, target, ColorUtil.prependColorTag("SpotAnim-Store", Color.ORANGE), npc.getSpotAnims(), ModelMenuOption.STORE);
				}
				modelGetter.addSpotAnimGetter(-4, target, ColorUtil.prependColorTag("SpotAnim-Anvil", Color.ORANGE), npc.getSpotAnims(), ModelMenuOption.ANVIL);
			}

			if (config.exportRightClick())
			{
				modelGetter.addSpotAnimExporter(-5, target, npc.getSpotAnims());
				modelGetter.addNPCExporter(-5, target, npc);
			}
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
						int animationId = -1;
						if (renderable instanceof DynamicObject)
						{
							animationId = ((DynamicObject) renderable).getAnimation().getId();
						}

						int objectId = groundObject.getId();
						String name = client.getObjectDefinition(objectId).getName();
						if (name.equals("null"))
						{
							name = "GroundObj";
						}

						if (config.rightClick())
						{
							if (client.isKeyPressed(KeyCode.KC_CONTROL))
							{
								modelGetter.addGameObjectGetter(-1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE_AND_ADD);
							}
							else
							{
								modelGetter.addGameObjectGetter(-1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE);

							}
							modelGetter.addObjectGetterToAnvil(name, objectId);
						}

						if (config.transmogRightClick())
						{
							modelGetter.addGameObjectGetter(-3, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.TRANSMOG);
						}

						if (config.exportRightClick())
						{
							modelGetter.addObjectExporter(-4, name, objectId, model);
						}
					}
				}

				DecorativeObject decorativeObject = tile.getDecorativeObject();
				if (decorativeObject != null)
				{
					Renderable renderable = decorativeObject.getRenderable();
					if (renderable instanceof Model)
					{
						Model model = (Model) decorativeObject.getRenderable();
						int animationId = -1;
						if (renderable instanceof DynamicObject)
						{
							animationId = ((DynamicObject) renderable).getAnimation().getId();
						}

						int objectId = decorativeObject.getId();
						String name = client.getObjectDefinition(objectId).getName();
						if (name.equals("null"))
						{
							name = "DecorativeObj";
						}

						if (config.rightClick())
						{
							if (client.isKeyPressed(KeyCode.KC_CONTROL))
							{
								modelGetter.addGameObjectGetter(-1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE_AND_ADD);
							}
							else
							{
								modelGetter.addGameObjectGetter(-1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE);

							}
							modelGetter.addObjectGetterToAnvil(name, objectId);
						}

						if (config.transmogRightClick())
						{
							modelGetter.addGameObjectGetter(-3, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.TRANSMOG);
						}

						if (config.exportRightClick())
						{
							modelGetter.addObjectExporter(-4, name, objectId, model);
						}
					}
				}

				WallObject wallObject = tile.getWallObject();
				if (wallObject != null)
				{
					Renderable renderable = wallObject.getRenderable1();
					if (renderable instanceof Model)
					{
						Model model = (Model) renderable;
						int animationId = -1;
						if (renderable instanceof DynamicObject)
						{
							animationId = ((DynamicObject) renderable).getAnimation().getId();
						}

						int objectId = wallObject.getId();
						String name = client.getObjectDefinition(objectId).getName();
						if (name.equals("null"))
						{
							name = "WallObj";
						}

						if (config.rightClick())
						{
							if (client.isKeyPressed(KeyCode.KC_CONTROL))
							{
								modelGetter.addGameObjectGetter(-1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE_AND_ADD);
							}
							else
							{
								modelGetter.addGameObjectGetter(-1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.STORE);
							}
							modelGetter.addObjectGetterToAnvil(name, objectId);
						}

						if (config.transmogRightClick())
						{
							modelGetter.addGameObjectGetter(-3, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, 0, ModelMenuOption.TRANSMOG);
						}

						if (config.exportRightClick())
						{
							modelGetter.addObjectExporter(-4, name, objectId, model);
						}
					}
				}

				List<TileItem> tileItems = tile.getGroundItems();
				if (tileItems != null)
				{
					for (TileItem tileItem : tileItems)
					{
						int itemId = tileItem.getId();
						String name = client.getItemDefinition(itemId).getName();
						if (name.equals("null"))
						{
							name = "Item";
						}

						Model model = tileItem.getModel();
						if (config.rightClick())
						{
							if (client.isKeyPressed(KeyCode.KC_CONTROL))
							{
								modelGetter.addGroundItemGetter(-1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, itemId, 0, ModelMenuOption.STORE_AND_ADD);
							}
							else
							{
								modelGetter.addGroundItemGetter(-1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, itemId, 0, ModelMenuOption.STORE);
							}
							modelGetter.addGroundItemGetterToAnvil(name, itemId);
						}

						if (config.transmogRightClick())
						{
							modelGetter.addGroundItemGetter(-3, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, itemId, 0, ModelMenuOption.TRANSMOG);
						}

						if (config.exportRightClick())
						{
							modelGetter.addGroundItemExporter(-4, name, itemId, model);
						}
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
						int animationId = -1;
						if (renderable instanceof DynamicObject)
						{
							animationId = ((DynamicObject) renderable).getAnimation().getId();
						}

						int objectId = gameObject.getId();
						String name = client.getObjectDefinition(objectId).getName();
						if (name.equals("null"))
						{
							name = "GameObj";
						}

						Model model = (Model) renderable;
						if (config.rightClick())
						{
							if (client.isKeyPressed(KeyCode.KC_CONTROL))
							{
								modelGetter.addGameObjectGetter(-1, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.STORE_AND_ADD);
							}
							else
							{
								modelGetter.addGameObjectGetter(-1, ColorUtil.prependColorTag("Store", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.STORE);
							}
							modelGetter.addObjectGetterToAnvil(name, gameObject.getId());
						}

						if (config.transmogRightClick())
							modelGetter.addGameObjectGetter(-3, ColorUtil.prependColorTag("Transmog", Color.ORANGE), name, model, objectId, CustomModelType.CACHE_OBJECT, animationId, gameObject.getOrientation(), ModelMenuOption.TRANSMOG);

						if (config.exportRightClick())
						{
							modelGetter.addObjectExporter(-4, name, objectId, model);
						}
					}
				}

				if (config.rightSelect() || config.exportRightClick())
				{
					for (int i = 0; i < characters.size(); i++)
					{
						Character character = characters.get(i);
						RuneLiteObject runeLiteObject = character.getRuneLiteObject();
						if (character.isActive() && runeLiteObject != null)
						{
							LocalPoint localPoint = runeLiteObject.getLocation();
							if (localPoint != null && localPoint.equals(tile.getLocalLocation()))
							{
								if (config.exportRightClick())
								{
									modelGetter.addRLObjectExporter(-1, character);
								}

								if (config.rightSelect())
								{
									client.createMenuEntry(-1)
											.setOption(ColorUtil.prependColorTag("Select", Color.ORANGE))
											.setTarget(ColorUtil.colorTag(Color.GREEN) + character.getName())
											.setType(MenuAction.RUNELITE)
											.onClick(e -> creatorsPanel.setSelectedCharacter(character, character.getObjectPanel()));
								}
							}
						}
					}
				}
			}
		}

		Player player = event.getMenuEntry().getPlayer();
		if (player != null && option.equals("Trade with"))
		{
			if (config.rightClick())
			{
				if (client.isKeyPressed(KeyCode.KC_CONTROL))
				{
					modelGetter.addPlayerGetter(-1, target, ColorUtil.prependColorTag("Store-Add", Color.ORANGE), player, ModelMenuOption.STORE_AND_ADD);
				}
				else
				{
					modelGetter.addPlayerGetter(-1, target, ColorUtil.prependColorTag("Store", Color.ORANGE), player, ModelMenuOption.STORE);
				}

				modelGetter.addPlayerGetter(-2, target, ColorUtil.prependColorTag("Anvil", Color.ORANGE), player, ModelMenuOption.ANVIL);
			}

			if (config.transmogRightClick())
			{
				modelGetter.addPlayerGetter(-3, target, ColorUtil.prependColorTag("Transmog", Color.ORANGE), player, ModelMenuOption.TRANSMOG);
			}

			if (config.rightSpotAnim())
			{
				if (client.isKeyPressed(KeyCode.KC_CONTROL))
				{
					modelGetter.addSpotAnimGetter(-4, target, ColorUtil.prependColorTag("SpotAnim-Store-Add", Color.ORANGE), player.getSpotAnims(), ModelMenuOption.STORE_AND_ADD);
				}
				else
				{
					modelGetter.addSpotAnimGetter(-4, target, ColorUtil.prependColorTag("SpotAnim-Store", Color.ORANGE), player.getSpotAnims(), ModelMenuOption.STORE);
				}
				modelGetter.addSpotAnimGetter(-5, target, ColorUtil.prependColorTag("SpotAnim-Anvil", Color.ORANGE), player.getSpotAnims(), ModelMenuOption.ANVIL);
			}

			if (config.exportRightClick())
			{
				modelGetter.addPlayerExporter(-6, target, player);
			}
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && tile != null && option.equals("Walk here"))
		{
			if (tile.getLocalLocation().equals(localPlayer.getLocalLocation()))
			{
				if (config.rightClick())
				{
					if (client.isKeyPressed(KeyCode.KC_CONTROL))
					{
						modelGetter.addPlayerGetter(-1, "Local Player", ColorUtil.prependColorTag("Store-Add", Color.ORANGE), localPlayer, ModelMenuOption.STORE_AND_ADD);
					}
					else
					{
						modelGetter.addPlayerGetter(-1, "Local Player", ColorUtil.prependColorTag("Store", Color.ORANGE), localPlayer, ModelMenuOption.STORE);
					}
					modelGetter.addPlayerGetter(-2, "Local Player", ColorUtil.prependColorTag("Anvil", Color.ORANGE), localPlayer, ModelMenuOption.ANVIL);
				}

				if (config.rightSpotAnim())
				{
					if (client.isKeyPressed(KeyCode.KC_CONTROL))
					{
						modelGetter.addSpotAnimGetter(-3, "Local Player", ColorUtil.prependColorTag("SpotAnim-Store-Add", Color.ORANGE), localPlayer.getSpotAnims(), ModelMenuOption.STORE_AND_ADD);
					}
					else
					{
						modelGetter.addSpotAnimGetter(-3, "Local Player", ColorUtil.prependColorTag("SpotAnim-Store", Color.ORANGE), localPlayer.getSpotAnims(), ModelMenuOption.STORE);
					}
					modelGetter.addSpotAnimGetter(-4, "Local Player", ColorUtil.prependColorTag("SpotAnim-Anvil", Color.ORANGE), localPlayer.getSpotAnims(), ModelMenuOption.ANVIL);
				}

				if (config.exportRightClick())
				{
					modelGetter.addPlayerExporter(-5, "Local Player", localPlayer);
				}
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
			character.getSpawnButton().setText("Spawn");
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
			character.getSpawnButton().setText("Spawn");
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

			//This function is finicky with larger instances, in that only an exact region:region map will load
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
			spawnButton.setText("Depawn");
			return;
		}

		spawnCharacter(character);
		spawnButton.setText("Spawn");

		if (!character.isLocationSet())
		{
			setLocation(character, true, true, false, false);
		}
	}

	public void spawnCharacter(Character character)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		character.setActive(true);
		clientThread.invokeLater(() -> runeLiteObject.setActive(true));
	}

	public void despawnCharacter(Character character)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		character.setActive(false);
		clientThread.invokeLater(() -> runeLiteObject.setActive(false));
	}

	public void setModel(Character character, boolean modelMode, int modelId)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invokeLater(() -> {
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
	}

	public void setOrientation(Character character, int orientation)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		character.getOrientationSpinner().setValue(orientation);
		clientThread.invokeLater(() -> runeLiteObject.setOrientation(orientation));
	}

	public void setupRLObject(Character character, boolean setHoveredTile)
	{
		clientThread.invoke(() ->
		{
			RuneLiteObject runeLiteObject = client.createRuneLiteObject();
			character.setRuneLiteObject(runeLiteObject);

			runeLiteObject.setRadius((int) character.getRadiusSpinner().getValue());
			runeLiteObject.setOrientation((int) character.getOrientationSpinner().getValue());
			runeLiteObject.setDrawFrontTilesFirst(true);
			runeLiteObject.setShouldLoop(true);

			boolean active = character.isActive();

			setModel(character, character.isCustomMode(), (int) character.getModelSpinner().getValue());
			setAnimation(character, (int) character.getAnimationSpinner().getValue());

			if (setHoveredTile)
			{
				setLocation(character, !character.isLocationSet(), false, true, false);
			}
			else
			{
				setLocation(character, !character.isLocationSet(), false, false, false);
			}

			runeLiteObject.setActive(active);
			character.setActive(active);
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

	public Model createComplexModel(DetailedModel[] detailedModels, boolean setPriority, LightingStyle lightingStyle, CustomLighting cl)
	{
		ModelData modelData = createComplexModelData(detailedModels);

		if (cl == null)
		{
			cl = new CustomLighting(lightingStyle.getAmbient(), lightingStyle.getContrast(), lightingStyle.getX(), lightingStyle.getY(), lightingStyle.getZ());
		}

		CustomLighting finalLighting;
		if (lightingStyle == LightingStyle.CUSTOM)
		{
			finalLighting = cl;
		}
		else
		{
			finalLighting = new CustomLighting(lightingStyle.getAmbient(), lightingStyle.getContrast(), lightingStyle.getX(), lightingStyle.getY(), lightingStyle.getZ());
		}

		Model model;
		try
		{
			model = modelData.light(
					finalLighting.getAmbient(),
					finalLighting.getContrast(),
					finalLighting.getX(),
					finalLighting.getZ() * -1,
					finalLighting.getY());
		}
		catch (Exception e)
		{
			sendChatMessage("Could not Forge this model with the chosen Lighting Settings. Please adjust them and try again.");
			return null;
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

	public ModelData createComplexModelData(DetailedModel[] detailedModels)
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

			short[] coloursFrom = detailedModel.getColoursFrom();
			short[] coloursTo = detailedModel.getColoursTo();

			if (coloursFrom == null || coloursTo == null)
			{
				if (!detailedModel.getRecolourNew().isEmpty() && !detailedModel.getRecolourOld().isEmpty())
				{
					String[] newColoursArray = detailedModel.getRecolourNew().split(",");
					coloursTo = new short[newColoursArray.length];
					String[] oldColoursArray = detailedModel.getRecolourOld().split(",");
					coloursFrom = new short[oldColoursArray.length];

					for (int i = 0; i < coloursFrom.length; i++)
					{
						coloursFrom[i] = Short.parseShort(oldColoursArray[i]);
						coloursTo[i] = Short.parseShort(newColoursArray[i]);
					}
				}
				else
				{
					coloursFrom = new short[0];
					coloursTo = new short[0];
				}

				detailedModel.setColoursFrom(coloursFrom);
				detailedModel.setColoursTo(coloursTo);
			}

			for (int i = 0; i < coloursTo.length; i++)
			{
				modelData.recolor(coloursFrom[i], coloursTo[i]);
			}

			short[] texturesFrom = detailedModel.getTexturesFrom();
			short[] texturesTo = detailedModel.getTexturesTo();
			if (texturesFrom != null && texturesTo != null)
			{
				try
				{
					modelData.cloneTextures();
					for (int i = 0; i < texturesTo.length; i++)
					{
						modelData.retexture(texturesFrom[i], texturesTo[i]);
					}
				}
				catch (Exception f)
				{
				}
			}
			else
			{
				detailedModel.setTexturesFrom(new short[0]);
				detailedModel.setTexturesFrom(new short[0]);
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

		return modelData;
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

					short[] itemRecolourTo = modelStats.getRecolourTo();
					short[] itemRecolourFrom = modelStats.getRecolourFrom();
					short[] kitRecolourTo = KitRecolourer.getKitRecolourTo(modelStats.getBodyPart(), kitRecolours);
					short[] kitRecolourFrom = KitRecolourer.getKitRecolourFrom(modelStats.getBodyPart());

					itemRecolourTo = ArrayUtils.addAll(itemRecolourTo, kitRecolourTo);
					itemRecolourFrom = ArrayUtils.addAll(itemRecolourFrom, kitRecolourFrom);

					creatorsPanel.getModelAnvil().createComplexPanel(
							name,
							modelStats.getModelId(),
							9,
							0, 0, 0,
							0, 0, 0,
							modelStats.getResizeX(), modelStats.getResizeY(), modelStats.getResizeZ(),
							0,
							"", "",
							itemRecolourFrom, itemRecolourTo,
							modelStats.getTextureFrom(), modelStats.getTextureTo(),
							false);

					continue;
				}

				creatorsPanel.getModelAnvil().createComplexPanel(
						"Name",
						modelStats.getModelId(),
						8,
						0, 0, 0,
						0, 0, 0,
						modelStats.getResizeX(), modelStats.getResizeY(), modelStats.getResizeZ(),
						0,
						"", "",
						modelStats.getRecolourFrom(), modelStats.getRecolourTo(),
						modelStats.getTextureFrom(), modelStats.getTextureTo(),
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
					break;
				case CACHE_GROUND_ITEM:
					modelStats = modelFinder.findModelsForGroundItem(id, CustomModelType.CACHE_GROUND_ITEM);
					name = modelFinder.getLastFound();
					break;
				case CACHE_MAN_WEAR:
					modelStats = modelFinder.findModelsForGroundItem(id, CustomModelType.CACHE_MAN_WEAR);
					name = modelFinder.getLastFound();
					break;
				case CACHE_WOMAN_WEAR:
					modelStats = modelFinder.findModelsForGroundItem(id, CustomModelType.CACHE_WOMAN_WEAR);
					name = modelFinder.getLastFound();
			}

			if (modelStats == null)
			{
				String idType = type.toString();
				if (type == CustomModelType.CACHE_MAN_WEAR || type == CustomModelType.CACHE_WOMAN_WEAR || type == CustomModelType.CACHE_GROUND_ITEM)
					idType = "Item";

				sendChatMessage("Could not find any associated Models for the given " + idType + " Id.");
				return;
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
			CustomLighting lighting;

			switch (type)
			{
				case CACHE_NPC:
					modelStats = modelFinder.findModelsForNPC(id);
					name = modelFinder.getLastFound();
					lighting = new CustomLighting(64, 850, -30, -30, 50);
					comp = new CustomModelComp(0, CustomModelType.CACHE_NPC, id, modelStats, null, null, null, LightingStyle.ACTOR, lighting, false, name);
					break;
				default:
				case CACHE_OBJECT:
					modelStats = modelFinder.findModelsForObject(id);
					name = modelFinder.getLastFound();
					lighting = modelStats[0].getLighting();
					comp = new CustomModelComp(0, CustomModelType.CACHE_OBJECT, id, modelStats, null, null, null, LightingStyle.CUSTOM, lighting, false, name);
					break;
				case CACHE_GROUND_ITEM:
					modelStats = modelFinder.findModelsForGroundItem(id, CustomModelType.CACHE_GROUND_ITEM);
					name = modelFinder.getLastFound();
					lighting = new CustomLighting(64, 768, -50, -50, 10);
					comp = new CustomModelComp(0, CustomModelType.CACHE_GROUND_ITEM, id, modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, name);
					break;
				case CACHE_MAN_WEAR:
					modelStats = modelFinder.findModelsForGroundItem(id, CustomModelType.CACHE_MAN_WEAR);
					name = modelFinder.getLastFound();
					lighting = new CustomLighting(64, 768, -50, -50, 10);
					comp = new CustomModelComp(0, CustomModelType.CACHE_MAN_WEAR, id, modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, name);
					break;
				case CACHE_WOMAN_WEAR:
					modelStats = modelFinder.findModelsForGroundItem(id, CustomModelType.CACHE_WOMAN_WEAR);
					name = modelFinder.getLastFound();
					lighting = new CustomLighting(64, 768, -50, -50, 10);
					comp = new CustomModelComp(0, CustomModelType.CACHE_WOMAN_WEAR, id, modelStats, null, null, null, LightingStyle.DEFAULT, lighting, false, name);
			}

			if (modelStats == null)
			{
				String idType = type.toString();
				if (type == CustomModelType.CACHE_MAN_WEAR || type == CustomModelType.CACHE_WOMAN_WEAR || type == CustomModelType.CACHE_GROUND_ITEM)
					idType = "Item";

				sendChatMessage("Could not find any associated Models for the given " + idType + " Id.");
				return;
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

			modelData.cloneColors().cloneVertices();

			for (short s = 0; s < modelStats.getRecolourFrom().length; s++)
				modelData.recolor(modelStats.getRecolourFrom()[s], modelStats.getRecolourTo()[s]);

			if (player)
				KitRecolourer.recolourKitModel(modelData, modelStats.getBodyPart(), kitRecolours);

			short[] textureFrom = modelStats.getTextureFrom();
			short[] textureTo = modelStats.getTextureTo();

			if (textureFrom == null || textureTo == null)
			{
				modelStats.setTextureFrom(new short[0]);
				modelStats.setTextureTo(new short[0]);
			}

			textureFrom = modelStats.getTextureFrom();
			textureTo = modelStats.getTextureTo();

			if (textureFrom.length > 0 && textureTo.length > 0)
			{
				for (int e = 0; e < textureFrom.length; e++)
				{
					modelData.retexture(textureFrom[e], textureTo[e]);
				}
			}

			if (modelStats.getResizeX() == 0 && modelStats.getResizeY() == 0 && modelStats.getResizeZ() == 0)
			{
				modelStats.setResizeX(128);
				modelStats.setResizeY(128);
				modelStats.setResizeZ(128);
			}

			modelData.scale(modelStats.getResizeX(), modelStats.getResizeZ(), modelStats.getResizeY());

			mds[i] = modelData;
		}

		if (actorLighting)
			return client.mergeModels(mds).light(64, 850, -30, -50, -30);

		return client.mergeModels(mds).light();
	}

	public void customModelToAnvil(CustomModel customModel)
	{
		if (customModel.getComp().getType() == CustomModelType.BLENDER)
		{
			sendChatMessage("Blender models cannot currently be used in the Anvil.");
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			CustomModelComp comp = customModel.getComp();
			sendChatMessage("Model sent to Anvil: " + comp.getName());
			ModelAnvil modelAnvil = creatorsPanel.getModelAnvil();

			CustomLighting cl;
			LightingStyle lightingStyle = comp.getLightingStyle();
			if (lightingStyle == LightingStyle.CUSTOM)
			{
				cl = comp.getCustomLighting();
			}
			else
			{
				cl = new CustomLighting(
						lightingStyle.getAmbient(),
						lightingStyle.getContrast(),
						lightingStyle.getX(),
						lightingStyle.getY(),
						lightingStyle.getZ());
			}

			modelAnvil.setLightingSettings(
					comp.getLightingStyle(),
					cl.getAmbient(),
					cl.getContrast(),
					cl.getX(),
					cl.getY(),
					cl.getZ());

			modelAnvil.getPriorityCheckBox().setSelected(comp.isPriority());
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
				case CACHE_MAN_WEAR:
				case CACHE_WOMAN_WEAR:
					cacheToAnvil(comp.getModelStats(), comp.getKitRecolours(), false);
					break;
				case CACHE_PLAYER:
					cacheToAnvil(comp.getModelStats(), comp.getKitRecolours(), true);
			}
		});
	}

	public void loadCustomModelToAnvil(File file, boolean priority, String name)
	{
		ModelAnvil modelAnvil = creatorsPanel.getModelAnvil();
		try
		{
			Reader reader = Files.newBufferedReader(file.toPath());
			CustomModelComp comp = gson.fromJson(reader, CustomModelComp.class);

			SwingUtilities.invokeLater(() ->
			{
				for (DetailedModel detailedModel : comp.getDetailedModels())
				{
					modelAnvil.createComplexPanel(detailedModel);
				}
			});

			LightingStyle ls = comp.getLightingStyle();
			CustomLighting cl = comp.getCustomLighting();
			if (cl == null)
				cl = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());

			modelAnvil.setLightingSettings(
					comp.getLightingStyle(),
					cl.getAmbient(),
					cl.getContrast(),
					cl.getX(),
					cl.getY(),
					cl.getZ());

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
			SwingUtilities.invokeLater(() ->
			{
				for (DetailedModel detailedModel : detailedModels)
				{
					creatorsPanel.getModelAnvil().createComplexPanel(detailedModel);
				}
			});
			reader.close();

			CustomLighting lighting = new CustomLighting(64, 768, -50, -50, 10);
			CustomModelComp comp = new CustomModelComp(0, CustomModelType.FORGED, -1, null, null, detailedModels, null, LightingStyle.DEFAULT, lighting, priority, name);
			modelAnvil.getPriorityCheckBox().setSelected(comp.isPriority());
			modelAnvil.getNameField().setText(comp.getName());

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
		}
		catch (Exception e)
		{
			sendChatMessage("The file chosen is possibly an older v1.0 file. Please see ScreteMonge for help adapting this.");
		}
	}

	public void loadCustomModel(File file, boolean priority, String name)
	{
		try
		{
			Reader reader = Files.newBufferedReader(file.toPath());
			CustomModelComp comp = gson.fromJson(reader, CustomModelComp.class);
			clientThread.invokeLater(() ->
			{
				LightingStyle ls = comp.getLightingStyle();
				CustomLighting cl = comp.getCustomLighting();
				if (cl == null)
					cl = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());

				Model model = createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle(), cl);
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
			CustomLighting lighting = new CustomLighting(64, 768, -50, -50, 10);
			CustomModelComp comp = new CustomModelComp(0, CustomModelType.FORGED, -1, null, null, detailedModels, null, LightingStyle.DEFAULT, lighting, priority, name);

			clientThread.invokeLater(() ->
			{
				Model model = createComplexModel(detailedModels, priority, LightingStyle.DEFAULT, lighting);
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
		}
		catch (Exception e)
		{
			sendChatMessage("The file chosen is possibly an older v1.0 file. Please see ScreteMonge for help adapting this.");
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
				clientThread.invokeLater(() ->
				{
					LightingStyle ls = comp.getLightingStyle();
					CustomLighting cl = comp.getCustomLighting();
					if (cl == null)
						cl = new CustomLighting(ls.getAmbient(), ls.getContrast(), ls.getX(), ls.getY(), ls.getZ());
					Model model = createComplexModel(comp.getDetailedModels(), comp.isPriority(), comp.getLightingStyle(), cl);
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
		creatorsPanel.removeModelOption(customModel);
		storedModels.remove(customModel);
	}

	public void updateProgramPath(Program program, boolean gameStateChanged, boolean instanced)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

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

				comp.setPathFound(false);
				return;
			}

			comp.setPathFound(true);
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

			if (coordinates == null) {
				if (!gameStateChanged)
					sendChatMessage("A path could not be found.");

				comp.setPathFound(false);
				return;
			}

			comp.setPathFound(true);
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

	private void updatePreviewObject(Tile tile)
	{
		if (previewObject == null)
		{
			clientThread.invokeLater(() ->
			{
				previewObject = client.createRuneLiteObject();
				previewObject.setActive(false);

				ModelData arrow = client.loadModelData(4852);
				ModelData transparent = client.loadModelData(9925);
				if (arrow == null || transparent == null)
				{
					return;
				}

				transparent.cloneVertices();
				int[] tx = transparent.getVerticesX();
				int[] ty = transparent.getVerticesY();
				int[] tz = transparent.getVerticesZ();
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

		RuneLiteObject rlObject = selectedCharacter.getRuneLiteObject();
		if (rlObject == null)
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
				orientation = Rotation.roundRotation(rlObject.getOrientation());
			}
			else
			{
				allowArrow = true;
				orientation = Rotation.getJagexDegrees(p.getX() - clickX, (p.getY() - clickY) * -1, yaw, pitch);
			}
		}
		else
		{
			orientation = rlObject.getOrientation();
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
			lp = rlObject.getLocation();
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
				lp = rlObject.getLocation();
			}
		}

		if (lp == null)
		{
			return;
		}

		Animation animation;
		if (allowArrow)
		{
			animation = client.loadAnimation(-1);
		}
		else
		{
			animation = rlObject.getAnimation();
		}

		previewObject.setModel(model);
		previewObject.setOrientation(orientation);
		previewObject.setAnimation(animation);
		previewObject.setDrawFrontTilesFirst(true);
		previewObject.setLocation(lp, client.getPlane());
		previewObject.setRadius(rlObject.getRadius());
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
			client.setOculusOrbNormalSpeed(oculusOrbSpeed);
		}
	};

	private final HotkeyListener orbSpeedListener = new HotkeyListener(() -> config.setOrbSpeedHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			SwingUtilities.invokeLater(() ->
			{
				String result = JOptionPane.showInputDialog(creatorsPanel, "Set Orb Speed (5 = Walk, 9 = Run)", 36);
				try
				{
					int value = Integer.parseInt(result);
					client.setOculusOrbNormalSpeed(value);
					oculusOrbSpeed = value;
				}
				catch (Exception f)
				{
					sendChatMessage("Invalid input; enter a number to set the Oculus Orb speed.");
				}
			});
		}
	};

	private final HotkeyListener orbPreset1Listener = new HotkeyListener(() -> config.orbSpeedHotkey1())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setOculusOrbNormalSpeed(config.speedHotkey1());
			sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey1());
		}
	};

	private final HotkeyListener orbPreset2Listener = new HotkeyListener(() -> config.orbSpeedHotkey2())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setOculusOrbNormalSpeed(config.speedHotkey2());
			sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey2());
		}
	};

	private final HotkeyListener orbPreset3Listener = new HotkeyListener(() -> config.orbSpeedHotkey3())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setOculusOrbNormalSpeed(config.speedHotkey3());
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
			addProgramStep();
		}
	};

	private void addProgramStep()
	{
		if (selectedCharacter != null)
		{
			Tile tile = client.getSelectedSceneTile();
			if (tile == null)
				return;

			LocalPoint localPoint = tile.getLocalLocation();
			if (localPoint == null)
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

				if (steps.length > 0)
				{
					Coordinate[] coordinates = pathFinder.getPath(steps[steps.length - 1], localPoint, program.getComp().getMovementType());
					if (coordinates == null)
					{
						sendChatMessage("A path could not be found to this tile");
						return;
					}
				}

				steps = ArrayUtils.add(steps, localPoint);
				program.getComp().setStepsLP(steps);
				updateProgramPath(program, false, selectedCharacter.isInInstance());
				return;
			}

			if (!isInScene && client.isInInstancedRegion())
			{
				program.getComp().setStepsLP(new LocalPoint[]{localPoint});
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

				WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

				if (steps.length > 0)
				{
					Coordinate[] coordinates = pathFinder.getPath(steps[steps.length - 1], worldPoint, program.getComp().getMovementType());
					if (coordinates == null)
					{
						sendChatMessage("A path could not be found to this tile");
						return;
					}
				}

				steps = ArrayUtils.add(steps, worldPoint);
				program.getComp().setStepsWP(steps);
				updateProgramPath(program, false, selectedCharacter.isInInstance());
				return;
			}

			if (!isInScene && !client.isInInstancedRegion())
			{
				WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
				program.getComp().setStepsWP(new WorldPoint[]{worldPoint});
				program.getComp().setStepsLP(new LocalPoint[0]);
				setLocation(selectedCharacter, true, false, true, false);
				updateProgramPath(program, false, selectedCharacter.isInInstance());
			}
		}
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

	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		if (config.enableCtrlHotkeys() && event.isControlDown())
		{
			creatorsPanel.scrollSelectedCharacter(selectedCharacter, event.getWheelRotation());
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
