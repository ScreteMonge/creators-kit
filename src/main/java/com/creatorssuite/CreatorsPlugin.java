package com.creatorssuite;

import com.creatorssuite.models.*;
import com.creatorssuite.panels.CreatorsPanel;
import com.creatorssuite.panels.ModelManager;
import com.creatorssuite.panels.ProgramPanel;
import com.creatorssuite.programming.Orientation;
import com.creatorssuite.programming.Program;
import com.creatorssuite.programming.ProgramManager;
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
		name = "0Creators' Suite",
		description = "A suite of tools for content creators",
		tags = {"tool", "creator", "content"}
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
	private ModelManager modelOrganizer;
	private ProgramPanel programPanel;
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

	@Override
	protected void startUp() throws Exception
	{
		creatorsPanel = injector.getInstance(CreatorsPanel.class);
		modelOrganizer = injector.getInstance(ModelManager.class);
		programPanel = injector.getInstance(ProgramPanel.class);
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
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invoke(this::clearNPCs);
		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(overlayKeyListener);
		keyManager.registerKeyListener(oculusOrbListener);
		keyManager.registerKeyListener(quickSpawnListener);
		keyManager.registerKeyListener(quickLocationListener);
		keyManager.registerKeyListener(quickRotateCWListener);
		keyManager.registerKeyListener(quickRotateCCWListener);
	}


	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = event.getMessage();
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
			return;
		}

		if (message.startsWith("Spawn"))
		{
			String[] split = message.split(",");
			String text = split[1];

			ArrayList<DetailedModel> list = new ArrayList<>();

			try {
				File myObj = new File(text + ".txt");
				Scanner myReader = new Scanner(myObj);
				myReader.nextLine();
				String s = "";

				while ((s = myReader.nextLine()) != null) {
					System.out.println("Line: " + s);
					int modelId = 0;
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
							if (data.startsWith("modelid="))
								modelId = Integer.parseInt(data.split("=")[1]);

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
								oldColours = data.split("=")[1];

							if (data.startsWith("o="))
								newColours = data.split("=")[1];
						}
					}
					catch (NoSuchElementException e)
					{
						setBreak = true;
					}


					String[] newSplit = newColours.split(",");
					short[] newShort = new short[newSplit.length];
					String[] oldSplit = oldColours.split(",");
					short[] oldShort = new short[newSplit.length];

					if (!newColours.isEmpty() && !oldColours.isEmpty() && newSplit.length == oldSplit.length)
					{
						for (int i = 0; i < newSplit.length; i++)
						{
							newShort[i] = Short.parseShort(newSplit[i]);
							oldShort[i] = Short.parseShort(oldSplit[i]);
						}
					}


					DetailedModel detailedModel = new DetailedModel(modelId, xTranslate, yTranslate, zTranslate, xScale, yScale, zScale, rotate, newShort, oldShort);
					System.out.println("ID: " + detailedModel.getModelId() + ", xT: " + detailedModel.getXTranslate());
					list.add(detailedModel);
					if (setBreak)
						break;
				}

				myReader.close();

				System.out.println("size: " + list.size());
				DetailedModel[] models = new DetailedModel[list.size()];
				for (int i = 0; i < list.size(); i++)
				{
					models[i] = list.get(i);
				}

				System.out.println("creating model");
				Model model = createComplexModel(models);
				CustomModel customModel = new CustomModel(model, text);
				addCustomModel(customModel, true);


			} catch (FileNotFoundException e)
			{
				System.out.println("An error occurred.");
				e.printStackTrace();
			}


		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
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
						NPCComposition npcComposition = npc.getComposition();
						int[] models = npcComposition.getModels();
						if (models == null)
						{
							sendChatMessage("Problem attempting to store this NPC :(");
							return;
						}
						CustomModel model = new CustomModel(constructSimpleModel(models, new short[0], new short[0]), npc.getName());
						storedModels.add(model);
						addCustomModel(model, false);
						sendChatMessage("Model stored: " + npc.getName());
					});
		}


        Tile tile = client.getSelectedSceneTile();
        if (tile != null)
		{
			if (option.equals("Examine"))
			{

			}

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
			addPlayerGetter(target, player);
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && tile != null && option.equals("Walk here"))
		{
			if (tile.getLocalLocation().equals(localPlayer.getLocalLocation()))
			{
				addPlayerGetter(localPlayer.getName(), localPlayer);
			}
		}
	}

	public void addPlayerGetter(String target, Player player)
	{
		client.createMenuEntry(-1)
				.setOption("Store")
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					PlayerComposition comp = player.getPlayerComposition();
					int[] items = comp.getEquipmentIds();
					int[] colours = comp.getColors();
					ColorTextureOverride[] textures = comp.getColorTextureOverrides();
					for (int colour : colours) {
						System.out.println("col: " + colour);
						/*
						0 = hair, jaw
						1 = torso, arms
						2 = pants
						3 = boots
						4 = skin
						 */
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

					Thread thread = new Thread(() ->
					{
						ModelStats[] modelStats = ModelFinder.findModelsForItems(false, comp.getGender() == 0, ids);
						clientThread.invokeLater(() ->
						{
							Model model = constructCacheModel(modelStats);
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

	public Model constructSimpleModel(int[] modelIds, short[] colourToFind, short[] colourToReplace)
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

		return client.mergeModels(data).light();
	}

	public Model createComplexModel(DetailedModel[] detailedModels)
	{
		ModelData[] models = new ModelData[detailedModels.length];

		for (int e = 0; e < detailedModels.length; e++)
		{
			DetailedModel detailedModel = detailedModels[e];
			ModelData modelData = client.loadModelData(detailedModel.getModelId());
			if (modelData == null)
			{
				System.out.println("modeldata null");
				return null;
			}

			modelData.cloneVertices().cloneColors();

			for (short s : modelData.getFaceColors())
			{
				System.out.println("Modelid: " + detailedModel.getModelId() + ", Colour: " + s);
			}


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
			modelData.translate(detailedModel.getXTranslate(), -1 * detailedModel.getZTranslate(), detailedModel.getYTranslate());
			modelData.scale(detailedModel.getXScale(), detailedModel.getZScale(), detailedModel.getYScale());


			for (int i = 0; i < detailedModel.getRecolourNew().length; i++)
			{
				modelData.recolor(detailedModel.getRecolourOld()[i], detailedModel.getRecolourNew()[i]);
			}

			models[e] = modelData;
		}

		return client.mergeModels(models).light();
	}

	public Model constructCacheModel(ModelStats[] modelStatsArray)
	{
		ModelData[] modelDatas = new ModelData[modelStatsArray.length];

		for (int i = 0; i < modelStatsArray.length; i++)
		{
			ModelStats modelStats = modelStatsArray[i];
			ModelData modelData = client.loadModelData(modelStats.getModelId());

			try
			{
				modelData.cloneColors();
				for (short s = 0; s < modelStats.getRecolourFrom().length; s++)
				{
					modelData.recolor(modelStats.getRecolourFrom()[s], modelStats.getRecolourTo()[s]);
				}
			}
			catch (Exception e)
			{

			}

			modelDatas[i] = modelData;
		}

		return client.mergeModels(modelDatas).light();
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

	private final HotkeyListener quickDuplicateListener = new HotkeyListener(() -> config.quickDuplicate())
	{
		@Override
		public void hotkeyPressed()
		{
			if (selectedNPC != null)
			{

			}
		}
	};

	@Provides
	CreatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CreatorsConfig.class);
	}
}
