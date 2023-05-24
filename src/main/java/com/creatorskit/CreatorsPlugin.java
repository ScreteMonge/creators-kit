package com.creatorskit;

import com.creatorskit.models.*;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.programming.Orientation;
import com.creatorskit.programming.Program;
import com.creatorskit.programming.ProgramManager;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;

@Slf4j
@PluginDescriptor(
		name = "0Creators' Kit",
		description = "A suite of tools for content creators",
		tags = {"tool", "creator", "content", "kit"}
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

	private ProgramManager programManager = new ProgramManager();
	private CreatorsPanel creatorsPanel;
	private NavigationButton navigationButton;
	@Getter
	private boolean overlaysActive = true;
	@Getter
	private Tile selectedTile;
	@Getter
	private final ArrayList<NPCCharacter> npcCharacters = new ArrayList<>();
	@Getter
	private final ArrayList<CustomModel> storedModels = new ArrayList<>();
	@Getter
	@Setter
	private NPCCharacter selectedNPC;
	@Getter
	@Setter
	private NPCCharacter hoveredNPC;
	@Getter
	private double speed = 1;
	private AutoRotate autoRotateYaw = AutoRotate.OFF;
	private AutoRotate autoRotatePitch = AutoRotate.OFF;
	private final int actorAmbient = 65;
	private final int actorContrast = 1400;

	@Override
	protected void startUp() throws Exception
	{
		creatorsPanel = injector.getInstance(CreatorsPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
		navigationButton = NavigationButton.builder()
				.tooltip("Creators' Suite")
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
		keyManager.registerKeyListener(autoStopListener);
		keyManager.registerKeyListener(autoLeftListener);
		keyManager.registerKeyListener(autoRightListener);
		keyManager.registerKeyListener(autoUpListener);
		keyManager.registerKeyListener(autoDownListener);
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
		keyManager.unregisterKeyListener(autoStopListener);
		keyManager.unregisterKeyListener(autoLeftListener);
		keyManager.unregisterKeyListener(autoRightListener);
		keyManager.unregisterKeyListener(autoUpListener);
		keyManager.unregisterKeyListener(autoDownListener);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = event.getMessage();

		if (message.startsWith("Spawn"))
		{
			ModelData md = client.loadModelData(Integer.parseInt(message.split(",")[1]));
			for (short s : md.getFaceColors())
				System.out.println("Colour: " + s);
		}

		if (message.startsWith("Speed"))
		{
			String[] split = message.split(",");
			speed = Double.parseDouble(split[1]);
			return;
		}

		if (message.startsWith("Set"))
		{
			if (selectedNPC == null && selectedTile == null)
			{
				return;
			}

			String[] split = message.split(",");
			double s = Double.parseDouble(split[1]);

			selectedNPC.setProgram(new Program(s, selectedTile.getLocalLocation()));
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

		for (NPCCharacter npcCharacter : npcCharacters)
		{
			Program program = npcCharacter.getProgram();
			if (program == null || !npcCharacter.isMoving())
			{
				continue;
			}

			LocalPoint endTile = program.getEndLocation();
			double speed = 128 * ((double) program.getSpeed() * 20 / 600);
			RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
			LocalPoint lp = runeLiteObject.getLocation();

			int startX = lp.getX();
			int startY = lp.getY();
			int flagX = endTile.getX();
			int flagY = endTile.getY();

			int endX = startX;
			int endY = startY;

			double changeX = flagX - startX;
			double changeY = flagY - startY;
			double angle = Orientation.radiansToJAngle(Math.atan(changeY/changeX), changeX, changeY);

			if (flagX - startX != 0)
			{
				endX = startX + ((int) speed * Orientation.orientationX(angle));
			}

			if (flagY - startY != 0)
			{
				endY = startY + ((int) speed * Orientation.orientationY(angle));
			}

			LocalPoint finalPoint = new LocalPoint(endX, endY);
			runeLiteObject.setLocation(finalPoint, client.getPlane());
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!overlaysActive)
		{
			return;
		}

		setRightClick();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		String target = event.getTarget();
		String option = event.getOption();

		NPC npc = event.getMenuEntry().getNpc();
		if (npc != null && option.equals("Examine"))
		{
			client.createMenuEntry(-1)
					.setOption("Store")
					.setTarget(target)
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						Thread thread = new Thread(() ->
						{
							ModelStats[] modelStats = ModelFinder.findModelsForNPC(npc.getId());
							clientThread.invokeLater(() ->
							{
								Model model = constructModelFromCache(modelStats, new int[0], false, true);
								CustomModel customModel = new CustomModel(model, npc.getName());
								storedModels.add(customModel);
								addCustomModel(customModel, false);
								sendChatMessage("Model stored: " + npc.getName());
							});
						});
						thread.start();
					});

			client.createMenuEntry(-2)
					.setOption("Anvil")
					.setTarget(target)
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						Thread thread = new Thread(() ->
						{
							ModelStats[] modelStats = ModelFinder.findModelsForNPC(npc.getId());
							clientThread.invokeLater(() ->
							{
								cacheToAnvil(modelStats, new int[0], false);
								sendChatMessage("Model sent to Anvil Complex Mode: " + npc.getName());
							});
						});
						thread.start();

					});
		}


        Tile tile = client.getSelectedSceneTile();
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
						addGameObjectGetter("<col=FFFF>GameObject", "GameObject", model);
					}
				}

				GroundObject groundObject = tile.getGroundObject();
				if (groundObject != null)
				{
					Renderable renderable = groundObject.getRenderable();
					if (renderable instanceof Model)
					{
						Model model = (Model) groundObject.getRenderable();
						addGameObjectGetter("<col=FFFF>GroundObject", "GroundObject", model);
					}
				}

				DecorativeObject decorativeObject = tile.getDecorativeObject();
				if (decorativeObject != null)
				{
					Renderable renderable = decorativeObject.getRenderable();
					if (renderable instanceof Model)
					{
						Model model = (Model) decorativeObject.getRenderable();
						addGameObjectGetter("<col=FFFF>DecorativeObject", "DecorativeObject", model);
					}
				}

				WallObject wallObject = tile.getWallObject();
				if (wallObject != null)
				{
					Renderable renderable = wallObject.getRenderable1();
					if (renderable instanceof Model)
					{
						Model model = (Model) renderable;
						addGameObjectGetter("<col=FFFF>WallObject", "WallObject", model);
					}
				}

				List<TileItem> tileItems = tile.getGroundItems();
				if (tileItems != null)
				{
					for (TileItem tileItem : tileItems)
					{
						Model model = tileItem.getModel();
						addGameObjectGetter("<col=FFFF>Item", "Item", model);
					}
				}
			}
		}

		Player player = event.getMenuEntry().getPlayer();
		if (player != null && option.equals("Trade with"))
		{
			addPlayerGetter(-1, target, "Store", player, false);
			addPlayerGetter(-2, target, "Anvil", player, true);
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && tile != null && option.equals("Walk here"))
		{
			if (tile.getLocalLocation().equals(localPlayer.getLocalLocation()))
			{
				addPlayerGetter(-1, localPlayer.getName(), "Store", localPlayer, false);
				addPlayerGetter(-2, localPlayer.getName(), "Anvil", localPlayer, true);
			}
		}
	}

	public void addPlayerGetter(int index, String target, String option, Player player, boolean sendToAnvil)
	{
		client.createMenuEntry(index)
				.setOption(option)
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					PlayerComposition comp = player.getPlayerComposition();
					int[] items = comp.getEquipmentIds();
					int[] colours = comp.getColors();
					ColorTextureOverride[] textures = comp.getColorTextureOverrides();
					int entry = 0;
					for (int colour : colours)
					{
						System.out.println(entry + ", col: " + colour);
						entry++;
					}

					if (textures != null)
					{
						for (ColorTextureOverride colorTextureOverride : textures)
						{
							short[] replaceWith = colorTextureOverride.getColorToReplaceWith();
							for (short s : replaceWith)
							{
								System.out.println("ColOverride: " + s);
							}

							short[] textureWith = colorTextureOverride.getTextureToReplaceWith();
							for (short s : textureWith)
							{
								System.out.println("TextOverride: " + s);
							}
						}
					}

					//Convert equipmentId to itemId or kitId as appropriate
					int[] ids = new int[items.length];
					for (int i = 0; i < ids.length; i++)
					{
						int item = items[i];

						if (item >= 256 && item <= 512)
						{
							ids[i] = item - 256;
							continue;
						}

						if (item > 512)
						{
							ids[i] = item - 512;
						}
					}

					if (sendToAnvil)
					{
						Thread thread = new Thread(() ->
						{
							ModelStats[] modelStats = ModelFinder.findModelsForPlayer(false, comp.getGender() == 0, ids);
							clientThread.invokeLater(() ->
							{
								cacheToAnvil(modelStats, comp.getColors(), true);
								sendChatMessage("Model sent to Anvil Complex Mode: " + player.getName());
							});
						});
						thread.start();
						return;
					}

					Thread thread = new Thread(() ->
					{
						ModelStats[] modelStats = ModelFinder.findModelsForPlayer(false, comp.getGender() == 0, ids);
						clientThread.invokeLater(() ->
						{
							Model model = constructModelFromCache(modelStats, comp.getColors(), false, true);
							CustomModel customModel = new CustomModel(model, player.getName());
							addCustomModel(customModel, false);
						});
					});
					thread.start();
				});
	}

	public void addGameObjectGetter(String target, String name, Model model)
	{
		client.createMenuEntry(-1)
				.setOption("Store")
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					CustomModel customModel = new CustomModel(model, name);
					addCustomModel(customModel, false);
					sendChatMessage("Model stored: " + name);
				});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getKey().equals("orbSpeed"))
		{
			client.setOculusOrbNormalSpeed(config.orbSpeed());
		}
	}

	public void setRightClick()
	{
		Tile tile = client.getSelectedSceneTile();
		if (tile == null)
		{
			return;
		}

		client.createMenuEntry(-1)
				.setOption("Select tile")
				.onClick(c ->
				{
					if (selectedTile == tile)
					{
						selectedTile = null;
						return;
					}

					selectedTile = tile;
				});

	}

	public void setLocation(NPCCharacter npcCharacter, boolean quickLocation)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		clientThread.invoke(() ->
		{
			RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();

			if (quickLocation)
			{
				Tile tile = client.getSelectedSceneTile();
				if (tile == null)
				{
					return;
				}

				LocalPoint localPoint = tile.getLocalLocation();
				runeLiteObject.setLocation(localPoint, client.getPlane());
				runeLiteObject.setActive(true);
				npcCharacter.getSpawnButton().setText("Despawn");
				npcCharacter.setLocationSet(true);
				npcCharacter.setSavedLocation(localPoint);
				return;
			}

			if (selectedTile == null)
			{
				Player player = client.getLocalPlayer();
				if (player == null)
				{
					return;
				}
				LocalPoint localPoint = player.getLocalLocation();
				runeLiteObject.setLocation(localPoint, client.getPlane());
				runeLiteObject.setActive(true);
				npcCharacter.getSpawnButton().setText("Despawn");
				npcCharacter.setLocationSet(true);
				npcCharacter.setSavedLocation(localPoint);
				return;
			}

			LocalPoint localPoint = selectedTile.getLocalLocation();
			if (localPoint == null)
			{
				return;
			}

			runeLiteObject.setActive(true);
			npcCharacter.getSpawnButton().setText("Despawn");
			npcCharacter.setLocationSet(true);
			npcCharacter.setSavedLocation(localPoint);
			runeLiteObject.setLocation(localPoint, client.getPlane());
		});
	}

	public void toggleSpawn(JButton spawnButton, NPCCharacter npcCharacter)
	{
		if (npcCharacter.getRuneLiteObject().isActive())
		{
			despawnNPC(npcCharacter);
			spawnButton.setText("Spawn");
			return;
		}

		spawnNPC(npcCharacter);
		spawnButton.setText("Despawn");

		if (!npcCharacter.isLocationSet())
		{
			setLocation(npcCharacter, false);
		}
	}

	public void spawnNPC(NPCCharacter npcCharacter)
	{
		RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();

		clientThread.invoke(() -> {
			runeLiteObject.setActive(true);
		});
	}

	public void despawnNPC(NPCCharacter npcCharacter)
	{
		RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
		clientThread.invoke(() -> {
			runeLiteObject.setActive(false);
		});
	}

	public void setModel(NPCCharacter npcCharacter, boolean modelMode, int modelId)
	{
		RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
		clientThread.invoke(() -> {
			if (modelMode)
			{
				CustomModel customModel = npcCharacter.getStoredModel();
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

	public void setAnimation(NPCCharacter npcCharacter, int animationId)
	{
		RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
		clientThread.invoke(() -> {
			Animation animation = client.loadAnimation(animationId);
			runeLiteObject.setAnimation(animation);
		});
	}

	public void unsetAnimation(NPCCharacter npcCharacter)
	{
		RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
		clientThread.invoke(() -> {
			Animation animation = client.loadAnimation(-1);
			runeLiteObject.setAnimation(animation);
		});
	}

	public void setRadius(NPCCharacter npcCharacter, int radius)
	{
		RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
		clientThread.invoke(() -> {
			runeLiteObject.setRadius(radius);
		});
	}

	public void addOrientation(NPCCharacter npcCharacter, int addition)
	{
		RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
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

		setOrientation(npcCharacter, orientation);
		JPanel masterPanel = npcCharacter.getPanel();
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

	public void setOrientation(NPCCharacter npcCharacter, int orientation)
	{
		RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
		clientThread.invoke(() -> {
			runeLiteObject.setOrientation(orientation);
		});
	}

	public NPCCharacter buildNPC(String name,
						 int id,
						 JPanel panel,
						 JTextField nameTextField,
						 JComboBox<CustomModel> comboBox,
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
						 JSpinner animationSpinner)
	{
		RuneLiteObject runeLiteObject = client.createRuneLiteObject();
		runeLiteObject.setRadius(radius);
		runeLiteObject.setOrientation(orientation);

		Animation animation = client.loadAnimation(animationId);
		runeLiteObject.setAnimation(animation);
		runeLiteObject.setShouldLoop(true);
		runeLiteObject.setDrawFrontTilesFirst(true);
		CustomModel customModel = (CustomModel) modelComboBox.getSelectedItem();

		NPCCharacter npcCharacter = new NPCCharacter(
				name,
				id,
				false,
				false,
				null,
				null,
				customModel,
				panel,
				comboBox,
				spawnButton,
				modelButton,
				runeLiteObject);

		npcCharacters.add(npcCharacter);
		setModel(npcCharacter, customModelMode, modelId);

		nameTextField.addActionListener(e ->
		{
			npcCharacter.setName(nameTextField.getText());
		});

		setLocationButton.addActionListener(e ->
		{
			setLocation(npcCharacter, false);
		});

		spawnButton.addActionListener(e ->
		{
			toggleSpawn(spawnButton, npcCharacter);
		});

		animationButton.addActionListener(e ->
		{
			Animation anim = runeLiteObject.getAnimation();

			if (anim == null)
			{
				animationButton.setText("Anim Off");
				setAnimation(npcCharacter, (int) animationSpinner.getValue());
				return;
			}

			int animId = anim.getId();

			if (animId == -1)
			{
				animationButton.setText("Anim Off");
				setAnimation(npcCharacter, (int) animationSpinner.getValue());
				return;
			}

			animationButton.setText("Anim On");
			unsetAnimation(npcCharacter);
		});

		modelButton.addActionListener(e ->
		{
			if (modelComboBox.isVisible())
			{
				modelButton.setText("Custom");
				modelSpinner.setVisible(true);
				modelComboBox.setVisible(false);
				setModel(npcCharacter, false, (int) modelSpinner.getValue());
			}
			else
			{
				modelButton.setText("Model ID");
				modelSpinner.setVisible(false);
				modelComboBox.setVisible(true);
				setModel(npcCharacter, true, -1);
			}
		});

		modelSpinner.addChangeListener(e ->
		{
			int modelNumber = (int) modelSpinner.getValue();
			setModel(npcCharacter, false, modelNumber);
		});

		modelComboBox.addActionListener(e ->
		{
			CustomModel m = (CustomModel) modelComboBox.getSelectedItem();
			npcCharacter.setStoredModel(m);
			setModel(npcCharacter, true, -1);
		});

		orientationSpinner.addChangeListener(e ->
		{
			int orient = (int) orientationSpinner.getValue();
			setOrientation(npcCharacter, orient);
		});

		animationSpinner.addChangeListener(e ->
		{
			animationButton.setText("Anim Off");
			int animationNumber = (int) animationSpinner.getValue();
			setAnimation(npcCharacter, animationNumber);
		});

		radiusSpinner.addChangeListener(e ->
		{
			int rad = (int) radiusSpinner.getValue();
			setRadius(npcCharacter, rad);
		});

		return npcCharacter;
	}

	public void clearNPCs()
	{
		for (NPCCharacter npcCharacter : npcCharacters)
		{
			RuneLiteObject runeLiteObject = npcCharacter.getRuneLiteObject();
			runeLiteObject.setActive(false);
		}

		npcCharacters.clear();
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

	public Model constructSimpleModel(int[] modelIds, short[] colourToFind, short[] colourToReplace, boolean actorLighting)
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

		if (actorLighting)
			return client.mergeModels(data).light(actorAmbient, actorContrast, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);

		return client.mergeModels(data).light();
	}

	public Model createComplexModel(DetailedModel[] detailedModels, boolean setPriority, boolean actorLighting)
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

		Model model	= (actorLighting) ? client.mergeModels(models).light(actorAmbient, actorContrast, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z) : client.mergeModels(models).light();

		if (model == null)
		{
			return null;
		}

		if (setPriority)
		{
			byte[] renderPriorities = model.getFaceRenderPriorities();
			Arrays.fill(renderPriorities, (byte) 0);
		}

		//6200 faces, 4000 vertices is about max
		if (model.getFaceCount() >= 6200 && model.getVerticesCount() >= 4000)
			sendChatMessage("You've exceeded the max face count of 6200 or vertex count of 4000 in this model; any additional faces will not render");

		return model;
	}

	private void cacheToAnvil(ModelStats[] modelStatsArray, int[] kitRecolours, boolean player)
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
		ModelData[] modelDatas = new ModelData[modelStatsArray.length];

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

			modelDatas[i] = modelData;
		}

		if (actorLighting)
			return client.mergeModels(modelDatas).light(65, 1400, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);

		return client.mergeModels(modelDatas).light();
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
				System.out.println("Line: " + s);
				String name = "";
				int modelId = 0;
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
						System.out.println("Data: " + data);
						if (data.startsWith("name="))
							name = data.split(",")[1];

						if (data.startsWith("modelid="))
							modelId = Integer.parseInt(data.split("=")[1]);

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

				DetailedModel detailedModel = new DetailedModel(name, modelId, xTile, yTile, zTile, xTranslate, yTranslate, zTranslate, xScale, yScale, zScale, rotate, newColours, oldColours);
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
			System.out.println("An error occurred.");
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

	private final HotkeyListener autoStopListener = new HotkeyListener(() -> config.stopRotationHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotateYaw = AutoRotate.OFF;
			autoRotatePitch = AutoRotate.OFF;
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

	@Provides
	CreatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CreatorsConfig.class);
	}
}
