package com.creatorskit;

import com.creatorskit.programming.Coordinate;
import com.creatorskit.programming.PathFinder;
import com.creatorskit.programming.Program;
import com.creatorskit.swing.CreatorsPanel;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

public class CreatorsOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final CreatorsPanel panel;
    private final CreatorsConfig config;
    private final PathFinder pathFinder;
    private static final Color PATH_COLOUR = new Color(238, 255, 0);
    private static final Color COORDINATE_COLOUR = new Color(255, 255, 255);
    private static final Color HOVERED_COLOUR = new Color(146, 206, 193, 255);
    private static final Color SELECTED_COLOUR = new Color(220, 253, 245);
    private static final Color MY_OBJECT_COLOUR = new Color(35, 208, 187);
    private static final Color GAME_OBJECT_COLOUR = new Color(255, 138, 18);
    private static final Color GROUND_OBJECT_COLOUR = new Color(73, 255, 0);
    private static final Color WALL_OBJECT_COLOUR = new Color(255, 70, 70);
    private static final Color DECORATIVE_OBJECT_COLOUR = new Color(183, 126, 255);
    private static final Color NPC_COLOUR = new Color(188, 198, 255);
    private static final Color PLAYER_COLOUR = new Color(221, 133, 255);
    private static final int MAX_DISTANCE = 2400;
    BufferedImage panelIcon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");

    @Inject
    private CreatorsOverlay(Client client, CreatorsPlugin plugin, CreatorsPanel panel, CreatorsConfig config, PathFinder pathFinder)
    {
        this.pathFinder = pathFinder;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.panel = panel;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !plugin.isOverlaysActive())
        {
            return null;
        }

        //renderWalkableOverlay(graphics);
        renderCoordinates(graphics);
        renderObjectsOverlay(graphics);
        renderProgramOverlay(graphics);

        if (config.npcOverlay())
        {
            renderNPCOverlay(graphics);
        }

        if (config.playerOverlay())
        {
            renderPlayerOverlay(graphics);
        }

        return null;
    }

    public void renderProgramOverlay(Graphics2D graphics)
    {
        for (Character character : plugin.getCharacters())
        {
            Program program = character.getProgram();
            if (program == null)
                continue;

            Coordinate[] coordinates = character.getProgram().getCoordinates();
            for (int i = 0; i < coordinates.length; i++)
            {
                if (coordinates[i] == null)
                    continue;

                LocalPoint localPoint = LocalPoint.fromScene(coordinates[i].getColumn(), coordinates[i].getRow());
                Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, "" + i, 0);
                OverlayUtil.renderTextLocation(graphics, textPoint, "" + i, COORDINATE_COLOUR);
            }

            LocalPoint[] points = character.getProgram().getSteps();
            for (int i = 0; i < points.length; i++)
            {
                Point textPoint = Perspective.getCanvasTextLocation(client, graphics, points[i], character.getName(), 0);
                OverlayUtil.renderTextLocation(graphics, textPoint, character.getName(), PATH_COLOUR);
            }
        }
    }

    public void renderCoordinates(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
            return;

        Tile[][] tiles = client.getScene().getTiles()[client.getPlane()];

        if (plugin.getCoords() == null)
        {
            return;
        }

        for (Coordinate coordinate : plugin.getCoords())
        {
            Tile tile = tiles[coordinate.getColumn()][coordinate.getRow()];
            if (tile == null)
                continue;

            OverlayUtil.renderTileOverlay(client, graphics, tile.getLocalLocation(), panelIcon, Color.WHITE);
        }
    }

    public void renderWalkableOverlay(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
            return;

        plugin.getAdjacentTiles().clear();

        LocalPoint lp = client.getLocalPlayer().getLocalLocation();
        int curX = lp.getSceneX();
        int curY = lp.getSceneY();
        int z = client.getPlane();

        for (int x = -1; x <= 1; x++)
        {
            for (int y = -1; y <= 1; y++)
            {
                Tile tile = plugin.getAllTiles()[z][curX + x][curY + y];
                if (client.getCollisionMaps() == null)
                {
                    return;
                }

                CollisionData data = client.getCollisionMaps()[client.getPlane()];
                int setting = data.getFlags()[curX + x][curY + y];

                if ((setting & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0)
                {
                    plugin.getAdjacentTiles().add(tile);
                }
            }
        }

        for (Tile tile : plugin.getAdjacentTiles())
        {
            OverlayUtil.renderTileOverlay(client, graphics, tile.getLocalLocation(), panelIcon, Color.WHITE);
        }
    }

    public void renderPlayerOverlay(Graphics2D graphics)
    {
        for (Player player : client.getPlayers())
        {
            LocalPoint localPoint = player.getLocalLocation();
            if (localPoint == null)
            {
                continue;
            }

            OverlayUtil.renderActorOverlay(graphics, player, "A: " + player.getAnimation() + ", P: " + player.getPoseAnimation(), PLAYER_COLOUR);
        }
    }

    public void renderNPCOverlay(Graphics2D graphics)
    {
        for (NPC npc : client.getNpcs())
        {
            LocalPoint localPoint = npc.getLocalLocation();
            if (localPoint == null)
            {
                continue;
            }

            OverlayUtil.renderActorOverlay(graphics, npc, "ID: " + npc.getId() + ", A: " + npc.getAnimation() + ", P: " + npc.getPoseAnimation(), NPC_COLOUR);
        }
    }

    public void renderObjectsOverlay(Graphics2D graphics)
    {
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        int z = client.getPlane();

        for (int x = 0; x < Constants.SCENE_SIZE; x++)
        {
            for (int y = 0; y < Constants.SCENE_SIZE; y++)
            {
                Tile tile = tiles[z][x][y];

                if (tile == null)
                {
                    continue;
                }

                Player player = client.getLocalPlayer();
                if (player == null)
                {
                    continue;
                }

                if (config.gameObjectOverlay() || config.myObjectOverlay())
                {
                    renderGameObjects(graphics, tile, player);
                }

                if (config.groundObjectOverlay())
                {
                    renderGroundObjects(graphics, tile, player);
                }

                if (config.wallObjectOverlay())
                {
                    renderWallObjects(graphics, tile, player);
                }

                if (config.decorativeObjectOverlay())
                {
                    renderDecorativeObjects(graphics, tile, player);
                }
            }
        }
    }

    public void renderGameObjects(Graphics2D graphics, Tile tile, Player player)
    {
        GameObject[] gameObjects = tile.getGameObjects();
        if (gameObjects != null)
        {
            for (GameObject gameObject : gameObjects)
            {
                if (gameObject != null && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()))
                {
                    if (gameObject.getRenderable() instanceof Actor)
                    {
                        continue;
                    }

                    StringBuilder stringBuilder = new StringBuilder();

                    if (gameObject.getRenderable() instanceof RuneLiteObject && config.myObjectOverlay())
                    {
                        RuneLiteObject runeLiteObject = (RuneLiteObject) gameObject.getRenderable();
                        for (int i = 0; i < plugin.getCharacters().size(); i++)
                        {
                            Character character = plugin.getCharacters().get(i);
                            if (character.getRuneLiteObject() == runeLiteObject)
                            {
                                stringBuilder.append(character.getName());
                                if (plugin.getSelectedNPC() == character)
                                {
                                    OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), SELECTED_COLOUR);
                                    continue;
                                }

                                if (plugin.getHoveredNPC() == character)
                                {
                                    OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), HOVERED_COLOUR);
                                    continue;
                                }

                                OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), MY_OBJECT_COLOUR);
                            }
                        }
                        continue;
                    }

                    if (player.getLocalLocation().distanceTo(gameObject.getLocalLocation()) <= MAX_DISTANCE)
                    {
                        if (!config.gameObjectOverlay())
                        {
                            continue;
                        }

                        stringBuilder.append("ID: ").append(gameObject.getId());
                        if (gameObject.getRenderable() instanceof DynamicObject)
                        {
                            Animation animation = ((DynamicObject) gameObject.getRenderable()).getAnimation();
                            if (animation != null)
                            {
                                stringBuilder.append(" A: ").append(animation.getId());
                            }
                            else
                            {
                                stringBuilder.append(" A: -1");
                            }
                        }
                        else
                        {
                            stringBuilder.append(" A: -1");
                        }

                        OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), GAME_OBJECT_COLOUR);
                    }
                }
            }
        }
    }

    public void renderGroundObjects(Graphics2D graphics, Tile tile, Player player)
    {
        GroundObject groundObject = tile.getGroundObject();
        if (groundObject != null)
        {
            if (player.getLocalLocation().distanceTo(groundObject.getLocalLocation()) <= MAX_DISTANCE)
            {
                OverlayUtil.renderTileOverlay(graphics, groundObject, "ID: " + groundObject.getId(), GROUND_OBJECT_COLOUR);
            }
        }
    }

    public void renderWallObjects(Graphics2D graphics, Tile tile, Player player)
    {
        TileObject tileObject = tile.getWallObject();
        if (tileObject != null)
        {
            if (player.getLocalLocation().distanceTo(tileObject.getLocalLocation()) <= MAX_DISTANCE)
            {
                OverlayUtil.renderTileOverlay(graphics, tileObject, "ID: " + tileObject.getId(), WALL_OBJECT_COLOUR);
            }
        }
    }

    public void renderDecorativeObjects(Graphics2D graphics, Tile tile, Player player)
    {
        TileObject tileObject = tile.getDecorativeObject();
        if (tileObject != null)
        {
            if (player.getLocalLocation().distanceTo(tileObject.getLocalLocation()) <= MAX_DISTANCE)
            {
                OverlayUtil.renderTileOverlay(graphics, tileObject, "ID: " + tileObject.getId(), DECORATIVE_OBJECT_COLOUR);
            }
        }
    }
}
