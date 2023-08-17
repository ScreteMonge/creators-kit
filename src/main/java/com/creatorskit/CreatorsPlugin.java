package com.creatorskit;

import com.creatorskit.models.*;
import com.creatorskit.programming.*;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.swing.ComplexPanel;
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
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;

@Slf4j
@PluginDescriptor(
		name = "Creator's Kit",
		description = "A suite of tools for creators",
		tags = {"tool", "creator", "content", "kit", "camera"}
)
public class CreatorsPlugin extends Plugin
{
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
	private MenuManager menuManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ModelGetter modelGetter;

	@Inject
	private PathFinder pathFinder;

	private CreatorsPanel creatorsPanel;
	private NavigationButton navigationButton;
	@Getter
	private boolean overlaysActive = false;
	@Getter
	private final ArrayList<Character> characters = new ArrayList<>();
	@Getter
	private final ArrayList<CustomModel> storedModels = new ArrayList<>();
	@Getter
	@Setter
	private Character selectedNPC;
	@Getter
	@Setter
	private Character hoveredNPC;
	@Getter
	private Tile[][][] allTiles;
	@Getter
	private byte[][][] allTileSettings;
	@Getter
	@Setter
	private ArrayList<Tile> adjacentTiles = new ArrayList<>();
	@Getter
	private final ArrayList<Tile> pathTiles = new ArrayList<>();
	@Getter
	private Tile selectedTile;
	private int savedRegion = -1;
	private AutoRotate autoRotateYaw = AutoRotate.OFF;
	private AutoRotate autoRotatePitch = AutoRotate.OFF;
	private final int BRIGHT_AMBIENT = 65;
	private final int BRIGHT_CONTRAST = 1400;
	private final int DARK_AMBIENT = 128;
	private final int DARK_CONTRAST = 4000;
	private boolean pauseMode = true;

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
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invoke(this::clearNPCs);
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

		if (client.getLocalPlayer() == null)
			return;

		WorldPoint wp = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation(), client.getPlane());
		int region = wp.getRegionID();
		if (savedRegion != region)
		{
			savedRegion = region;
			allTiles = client.getScene().getTiles();
			allTileSettings = client.getTileSettings();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
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

		for (Character character : characters)
		{
			Program program = character.getProgram();
			RuneLiteObject runeLiteObject = character.getRuneLiteObject();
			if (program == null)
				continue;

			if (!character.isProgramActive())
			{
				Animation animation = runeLiteObject.getAnimation();
				if (animation == null)
					continue;

				if (animation.getId() != program.getIdleAnim())
					runeLiteObject.setAnimation(client.loadAnimation(program.getIdleAnim()));

				continue;
			}


			double speed = 128 * (program.getSpeed() * 20 / 600);

			int currentStep = program.getCurrentStep();
			LocalPoint[] path = program.getPath();

			if (currentStep >= path.length)
			{
				if (runeLiteObject.getAnimation() == null)
					continue;

				if (runeLiteObject.getAnimation().getId() != program.getIdleAnim())
					runeLiteObject.setAnimation(client.loadAnimation(program.getIdleAnim()));

				continue;
			}

			Animation currentAnim = runeLiteObject.getAnimation();
			if (currentAnim != null && currentAnim.getId() != program.getWalkAnim())
			{
				int walkAnimId = program.getWalkAnim();
				if (walkAnimId == -1)
					walkAnimId = program.getIdleAnim();

				if (currentAnim.getId() != walkAnimId)
					runeLiteObject.setAnimation(client.loadAnimation(program.getWalkAnim()));
			}

			LocalPoint start = runeLiteObject.getLocation();
			LocalPoint destination = path[currentStep];

			int startX = start.getX();
			int startY = start.getY();
			int destX = destination.getX();
			int destY = destination.getY();

			int endX = startX;
			int endY = startY;

			double changeX = destX - startX;
			double changeY = destY - startY;
			double angle = Orientation.radiansToJAngle(Math.atan(changeY/changeX), changeX, changeY);

			if (currentStep == 0)
			{
				character.setTargetOrientation((int) angle);
			}

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
				program.setCurrentStep(nextStep);
				if (nextStep < path.length)
				{
					LocalPoint nextPath = path[nextStep];
					int nextX = nextPath.getSceneX();
					int nextY = nextPath.getSceneY();
					double nextChangeX = nextX - start.getSceneX();
					double nextChangeY = nextY - start.getSceneY();
					double nextAngle = Orientation.radiansToJAngle(Math.atan(nextChangeY/nextChangeX), nextChangeX, nextChangeY);
					character.setTargetOrientation((int) nextAngle);
				}
			}

			int orientation = runeLiteObject.getOrientation();
			int targetOrientation = character.getTargetOrientation();
			int turnSpeed = program.getTurnSpeed();
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
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getKey().equals("orbSpeed"))
		{
			client.setOculusOrbNormalSpeed(config.orbSpeed());
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		String target = event.getTarget();
		String option = event.getOption();

		Tile tile = client.getSelectedSceneTile();
		selectedTile = tile;

		NPC npc = event.getMenuEntry().getNpc();
		if (npc != null && option.equals("Examine"))
		{
			modelGetter.storeNPC(target, npc);
			modelGetter.sendToAnvilNPC(target, npc);
		}

        if (tile != null)
		{
			if (option.equals("Walk here"))
			{
				GameObject[] gameObjects = tile.getGameObjects();
				for (GameObject gameObject : gameObjects)
				{
					if (gameObject == null)
					{
						continue;
					}

					Renderable renderable = gameObject.getRenderable();
					if (renderable == null)
					{
						continue;
					}

					if (renderable instanceof Model)
					{
						Model model = (Model) renderable;
						modelGetter.addGameObjectGetter("<col=FFFF>GameObject", "GameObject", model);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>GameObject", "GameObject", gameObject.getId());
					}
				}

				GroundObject groundObject = tile.getGroundObject();
				if (groundObject != null)
				{
					Renderable renderable = groundObject.getRenderable();
					if (renderable instanceof Model)
					{
						Model model = (Model) groundObject.getRenderable();
						modelGetter.addGameObjectGetter("<col=FFFF>GroundObject", "GroundObject", model);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>GroundObject", "GroundObject", groundObject.getId());
					}
				}

				DecorativeObject decorativeObject = tile.getDecorativeObject();
				if (decorativeObject != null)
				{
					Renderable renderable = decorativeObject.getRenderable();
					if (renderable instanceof Model)
					{
						Model model = (Model) decorativeObject.getRenderable();
						modelGetter.addGameObjectGetter("<col=FFFF>DecorativeObject", "DecorativeObject", model);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>DecorativeObject", "DecorativeObject", decorativeObject.getId());
					}
				}

				WallObject wallObject = tile.getWallObject();
				if (wallObject != null)
				{
					Renderable renderable = wallObject.getRenderable1();
					if (renderable instanceof Model)
					{
						Model model = (Model) renderable;
						modelGetter.addGameObjectGetter("<col=FFFF>WallObject", "WallObject", model);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>WallObject", "WallObject", wallObject.getId());
					}
				}

				List<TileItem> tileItems = tile.getGroundItems();
				if (tileItems != null)
				{
					for (TileItem tileItem : tileItems)
					{
						Model model = tileItem.getModel();
						modelGetter.addGameObjectGetter("<col=FFFF>Item", "Item", model);
						modelGetter.addObjectGetterToAnvil("<col=FFFF>Item", "Item", tileItem.getId());
					}
				}
			}
		}

		Player player = event.getMenuEntry().getPlayer();
		if (player != null && option.equals("Trade with"))
		{
			modelGetter.addPlayerGetter(-1, target, "Store", player, false);
			modelGetter.addPlayerGetter(-2, target, "Anvil", player, true);
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && tile != null && option.equals("Walk here"))
		{
			if (tile.getLocalLocation().equals(localPlayer.getLocalLocation()))
			{
				modelGetter.addPlayerGetter(-1, localPlayer.getName(), "Store", localPlayer, false);
				modelGetter.addPlayerGetter(-2, localPlayer.getName(), "Anvil", localPlayer, true);
			}
		}
	}

	public void setLocation(Character character, boolean quickLocation)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		clientThread.invoke(() ->
		{
			LocalPoint localPoint;

			if (quickLocation)
			{
				Tile tile = client.getSelectedSceneTile();
				if (tile == null)
					return;

				localPoint = tile.getLocalLocation();
			}
			else
			{
				localPoint = client.getLocalPlayer().getLocalLocation();
			}

			if (localPoint == null)
				return;

			RuneLiteObject runeLiteObject = character.getRuneLiteObject();
			runeLiteObject.setActive(false);
			runeLiteObject.setLocation(localPoint, client.getPlane());
			runeLiteObject.setActive(true);
			character.getSpawnButton().setText("Despawn");
			character.setLocationSet(true);
			character.setSavedLocation(localPoint);
		});
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
			setLocation(character, false);
		}
	}

	public void spawnCharacter(Character character)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();

		clientThread.invoke(() -> {
			runeLiteObject.setActive(true);
		});
	}

	public void despawnCharacter(Character character)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() -> {
			runeLiteObject.setActive(false);
		});
	}

	public void setModel(Character character, boolean modelMode, int modelId)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() -> {
			if (modelMode)
			{
				CustomModel customModel = character.getStoredModel();
				Model model;
				if (customModel == null)
				{
					model = client.loadModel(29757);
				}
				else
				{
					model = customModel.getModel();
				}

				runeLiteObject.setModel(model);
				return;
			}

			Model model = client.loadModel(modelId);
			runeLiteObject.setModel(model);
		});
	}

	public void setAnimation(Character character, int animationId)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() ->
		{
			Animation animation = client.loadAnimation(animationId);
			runeLiteObject.setAnimation(animation);
		});
	}

	public void unsetAnimation(Character character)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() ->
		{
			Animation animation = client.loadAnimation(-1);
			runeLiteObject.setAnimation(animation);
		});
	}

	public void setRadius(Character character, int radius)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		clientThread.invoke(() -> {
			runeLiteObject.setRadius(radius);
		});
	}

	public void addOrientation(Character character, int addition)
	{
		RuneLiteObject runeLiteObject = character.getRuneLiteObject();
		int orientation = runeLiteObject.getOrientation();
		orientation += addition;
		if (orientation >= 2048)
		{
			orientation -= 2048;
		}

		if (orientation < 0)
		{
			orientation += 2048;
		}

		setOrientation(character, orientation);
		JPanel masterPanel = character.getPanel();
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
		clientThread.invoke(() -> {
			runeLiteObject.setOrientation(orientation);
		});
	}

	public Character buildCharacter(String name,
									int id,
									JPanel panel,
									JTextField nameTextField,
									JButton setLocationButton,
									JButton spawnButton,
									JButton animationButton,
									JButton modelButton,
									int modelId,
									JSpinner modelSpinner,
									JComboBox<CustomModel> modelComboBox,
									boolean customModelMode,
									int orientation,
									JSpinner orientationSpinner,
									int radius,
									JSpinner radiusSpinner,
									int animationId,
									JSpinner animationSpinner,
									Program program,
									JLabel programmerNameLabel,
									JSpinner programmerIdleSpinner)
	{
		RuneLiteObject runeLiteObject = client.createRuneLiteObject();
		runeLiteObject.setRadius(radius);
		runeLiteObject.setOrientation(orientation);

		Animation animation = client.loadAnimation(animationId);
		runeLiteObject.setAnimation(animation);
		runeLiteObject.setShouldLoop(true);
		runeLiteObject.setDrawFrontTilesFirst(true);
		CustomModel customModel = (CustomModel) modelComboBox.getSelectedItem();

		Character character = new Character(
				name,
				id,
				false,
				false,
				program,
				null,
				customModel,
				panel,
				customModelMode,
				modelComboBox,
				spawnButton,
				modelButton,
				modelSpinner,
				animationSpinner,
				programmerNameLabel,
				programmerIdleSpinner,
				runeLiteObject,
				0);

		characters.add(character);
		setModel(character, customModelMode, modelId);

		nameTextField.addActionListener(e ->
		{
			character.setName(nameTextField.getText());
			programmerNameLabel.setText(nameTextField.getText());
		});

		setLocationButton.addActionListener(e ->
				setLocation(character, false));

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
			if (modelComboBox.isVisible() && character == selectedNPC)
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

	public void clearNPCs()
	{
		for (Character character : characters)
		{
			RuneLiteObject runeLiteObject = character.getRuneLiteObject();
			runeLiteObject.setActive(false);
		}

		characters.clear();
	}

	public void sendChatMessage(String chatMessage)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final String message = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(chatMessage).build();
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(message).build());
	}

	public Model constructSimpleModel(int[] modelIds, short[] colourToFind, short[] colourToReplace, LightingStyle lightingStyle)
	{
		ModelData[] data = new ModelData[modelIds.length];

		for (int i = 0; i < modelIds.length; i++)
		{
			int id = modelIds[i];
			ModelData modelData = client.loadModelData(id);

			if (modelData != null && modelData.getFaceColors().length > 0)
			{
				modelData.cloneColors();
				for (int e = 0; e < colourToFind.length; e++)
				{
					modelData.recolor(colourToFind[e], colourToReplace[e]);
				}
			}

			data[i] = modelData;
		}

		switch (lightingStyle)
		{
			default:
			case DEFAULT:
				return client.mergeModels(data).light();
			case ACTOR:
				return client.mergeModels(data).light(BRIGHT_AMBIENT, BRIGHT_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
			case NONE:
				return client.mergeModels(data).light(DARK_AMBIENT, DARK_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
		}
	}

	public Model createComplexModel(DetailedModel[] detailedModels, boolean setPriority, LightingStyle lightingStyle)
	{
		ModelData[] models = new ModelData[detailedModels.length];

		for (int e = 0; e < detailedModels.length; e++)
		{
			DetailedModel detailedModel = detailedModels[e];
			ModelData modelData = client.loadModelData(detailedModel.getModelId());
			if (modelData == null)
			{
				return null;
			}

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

		Model model;
		switch (lightingStyle)
		{
			default:
			case DEFAULT:
				model =  client.mergeModels(models).light();
				break;
			case ACTOR:
				model = client.mergeModels(models).light(BRIGHT_AMBIENT, BRIGHT_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
				break;
			case NONE:
				model = client.mergeModels(models).light(DARK_AMBIENT, DARK_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
		}

		if (model == null)
		{
			return null;
		}

		if (setPriority)
		{
			byte[] renderPriorities = model.getFaceRenderPriorities();
			if (renderPriorities != null && renderPriorities.length > 0)
				Arrays.fill(renderPriorities, (byte) 0);
		}

		sendChatMessage("Model forged. Faces: " + model.getFaceCount() + ", Vertices: " + model.getVerticesCount());
		if (model.getFaceCount() >= 6200 && model.getVerticesCount() >= 3900)
			sendChatMessage("You've exceeded the max face count of 6200 or vertex count of 4000 in this model; any additional faces or vertices will not render");

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
					if (modelStats.getBodyPart() == BodyPart.NA) {
						creatorsPanel.getModelAnvil().createComplexPanel(
								"Item",
								modelStats.getModelId(),
								9,
								0, 0, 0,
								0, 0, 0,
								128, 128, 128,
								0,
								ModelFinder.shortArrayToString(modelStats.getRecolourTo()),
								ModelFinder.shortArrayToString(modelStats.getRecolourFrom()));
					}
					else
					{
						creatorsPanel.getModelAnvil().createComplexPanel(
								modelStats.getBodyPart().toString(),
								modelStats.getModelId(),
								9,
								0, 0, 0,
								0, 0, 0,
								128, 128, 128,
								0,
								KitRecolourer.getKitRecolourNew(modelStats.getBodyPart(), kitRecolours),
								KitRecolourer.getKitRecolourOld(modelStats.getBodyPart()));
					}
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
						ModelFinder.shortArrayToString(modelStats.getRecolourFrom()));
			}
		});
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
			return client.mergeModels(mds).light(65, 1400, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);

		return client.mergeModels(mds).light();
	}

	public void loadCustomModel(File file)
	{
		ArrayList<DetailedModel> list = new ArrayList<>();

		try
		{
			Scanner myReader = new Scanner(file);
			myReader.nextLine();
			String s = "";

			while ((s = myReader.nextLine()) != null) {
				//System.out.println("Line: " + s);
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
						//System.out.println("Data: " + data);
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

				DetailedModel detailedModel = new DetailedModel(name, modelId, group, xTile, yTile, zTile, xTranslate, yTranslate, zTranslate, xScale, yScale, zScale, rotate, newColours, oldColours);
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
							detailedModel.getRecolourOld());
				}
			});
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Could not find file.");
			e.printStackTrace();
		}
	}

	public void addCustomModel(CustomModel customModel, boolean setComboBox)
	{
		SwingUtilities.invokeLater(() ->
		{
			creatorsPanel.addModelOption(customModel, setComboBox);
		});
		storedModels.add(customModel);
	}

	public void removeCustomModel(CustomModel customModel)
	{
		SwingUtilities.invokeLater(() ->
		{
			creatorsPanel.removeModelOption(customModel);
		});
		storedModels.remove(customModel);
	}

	public void updateProgramPath(Program program)
	{
		LocalPoint[] steps = program.getSteps();
		LocalPoint[] path = new LocalPoint[0];
		Coordinate[] allCoordinates = new Coordinate[0];

		if (steps.length < 2)
		{
			program.setPath(path);
			program.setCoordinates(allCoordinates);
			return;
		}

		for (int i = 0; i < steps.length - 1; i++)
		{
			Coordinate[] coordinates = pathFinder.getPath(steps[i], steps[i + 1], program.getMovementType());
			if (coordinates == null)
			{
				sendChatMessage("A path could not be found.");
				program.setPath(new LocalPoint[0]);
				program.setCoordinates(new Coordinate[0]);
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
					path = ArrayUtils.add(path, LocalPoint.fromScene(x, y));
				}
			}
		}

		path = ArrayUtils.add(path, steps[steps.length - 1]);

		program.setPath(path);
		program.setCoordinates(allCoordinates);
	}

	public void updatePanelComboBoxes()
	{
		SwingUtilities.invokeLater(() ->
		{
			for (JComboBox<CustomModel> comboBox : creatorsPanel.getComboBoxes())
			{
				comboBox.updateUI();
			}
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
			if (overlaysActive)
			{
				overlaysActive = false;
				return;
			}

			overlaysActive = true;
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
			if (selectedNPC != null)
			{
				toggleSpawn(selectedNPC.getSpawnButton(), selectedNPC);
			}
		}
	};

	private final HotkeyListener quickLocationListener = new HotkeyListener(() -> config.quickLocationHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedNPC != null)
			{
				setLocation(selectedNPC, true);
			}
		}
	};

	private final HotkeyListener quickRotateCWListener = new HotkeyListener(() -> config.quickRotateCWHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedNPC != null)
			{
				addOrientation(selectedNPC, -512);
			}
		}
	};

	private final HotkeyListener quickRotateCCWListener = new HotkeyListener(() -> config.quickRotateCCWHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedNPC != null)
			{
				addOrientation(selectedNPC, 512);
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
			if (selectedNPC != null)
			{
				Tile tile = client.getSelectedSceneTile();
				if (tile == null)
					return;

				Program program = selectedNPC.getProgram();
				LocalPoint[] steps = program.getSteps();
				steps = ArrayUtils.add(steps, tile.getLocalLocation());
				program.setSteps(steps);
				updateProgramPath(program);

				if (steps.length == 1)
				{
					setLocation(selectedNPC, true);
				}
			}
		}
	};

	private final HotkeyListener removeProgramStepListener = new HotkeyListener(() -> config.removeProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedNPC != null)
			{
				Tile tile = client.getSelectedSceneTile();
				if (tile != null)
				{
					Program program = selectedNPC.getProgram();
					LocalPoint[] steps = program.getSteps();
					steps = ArrayUtils.removeElement(steps, tile.getLocalLocation());
					program.setSteps(steps);
					updateProgramPath(program);
				}
			}
		}
	};

	private final HotkeyListener clearProgramStepListener = new HotkeyListener(() -> config.clearProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedNPC != null)
			{
				Program program = selectedNPC.getProgram();
				program.setSteps(new LocalPoint[0]);
				updateProgramPath(program);
			}
		}
	};

	private final HotkeyListener playPauseListener = new HotkeyListener(() -> config.playPauseHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedNPC != null)
			{
				selectedNPC.setProgramActive(!selectedNPC.isProgramActive());
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
			{
				character.setProgramActive(!pauseMode);
			}
		}
	};

	private final HotkeyListener resetListener = new HotkeyListener(() -> config.resetHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedNPC != null)
			{
				Program program = selectedNPC.getProgram();
				program.setCurrentStep(0);

				if (program.getSteps().length == 0)
					return;

				LocalPoint lp = program.getSteps()[0];
				if (lp == null)
					return;

				selectedNPC.getRuneLiteObject().setLocation(lp, client.getPlane());
				selectedNPC.setProgramActive(false);
			}
		}
	};

	private final HotkeyListener resetAllListener = new HotkeyListener(() -> config.resetAllHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			for (Character character : characters)
			{
				Program program = character.getProgram();
				program.setCurrentStep(0);

				if (program.getSteps().length == 0)
				{
					continue;
				}

				LocalPoint lp = program.getSteps()[0];
				if (lp == null)
				{
					continue;
				}

				character.getRuneLiteObject().setLocation(lp, client.getPlane());
				character.setProgramActive(false);
			}
		}
	};

	@Provides
	CreatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CreatorsConfig.class);
	}
}
