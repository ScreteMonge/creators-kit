package com.creatorskit;

import com.creatorskit.swing.CreatorsPanel;
import net.runelite.api.*;
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
    private static final Color SELECTED_TILE_COLOUR = new Color(238, 255, 0);
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
    private CreatorsOverlay(Client client, CreatorsPlugin plugin, CreatorsPanel panel, CreatorsConfig config)
    {
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

        renderObjectsOverlay(graphics);

        if (config.npcOverlay())
        {
            renderNPCOverlay(graphics);
        }

        if (config.playerOverlay())
        {
            renderPlayerOverlay(graphics);
        }

        if (config.selectOverlay())
        {
            renderSelectedTile(graphics);
        }

        return null;
    }

    public  void renderPlayerOverlay(Graphics2D graphics)
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

    public void renderSelectedTile(Graphics2D graphics)
    {
        Tile tile = plugin.getSelectedTile();
        if (tile == null)
        {
            return;
        }

        LocalPoint localPoint = tile.getLocalLocation();
        Polygon poly = Perspective.getCanvasTilePoly(client, localPoint, 0);
        OverlayUtil.renderPolygon(graphics, poly, SELECTED_TILE_COLOUR);
        OverlayUtil.renderImageLocation(client, graphics, localPoint, panelIcon, 10);
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
                    if (player.getLocalLocation().distanceTo(gameObject.getLocalLocation()) <= MAX_DISTANCE)
                    {
                        if (gameObject.getRenderable() instanceof Actor)
                        {
                            continue;
                        }

                        StringBuilder stringBuilder = new StringBuilder();

                        if (gameObject.getRenderable() instanceof RuneLiteObject && config.myObjectOverlay())
                        {
                            RuneLiteObject runeLiteObject = (RuneLiteObject) gameObject.getRenderable();
                            for (int i = 0; i < plugin.getNpcCharacters().size(); i++)
                            {
                                NPCCharacter npcCharacter = plugin.getNpcCharacters().get(i);
                                if (npcCharacter.getRuneLiteObject() == runeLiteObject)
                                {
                                    stringBuilder.append(npcCharacter.getName());
                                    if (plugin.getSelectedNPC() == npcCharacter)
                                    {
                                        OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), SELECTED_COLOUR);
                                        continue;
                                    }

                                    if (plugin.getHoveredNPC() == npcCharacter)
                                    {
                                        OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), HOVERED_COLOUR);
                                        continue;
                                    }

                                    OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), MY_OBJECT_COLOUR);
                                }
                            }

                            continue;
                        }

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
