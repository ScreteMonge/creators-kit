package com.creatorskit;

import com.creatorskit.programming.Coordinate;
import com.creatorskit.programming.Program;
import com.creatorskit.programming.ProgramComp;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

public class CreatorsOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final CreatorsConfig config;
    private static final Color HOVERED_COLOUR = new Color(146, 206, 193, 255);
    private static final Color SELECTED_COLOUR = new Color(220, 253, 245);
    private static final Color MY_OBJECT_COLOUR = new Color(35, 208, 187);
    private static final Color GAME_OBJECT_COLOUR = new Color(255, 138, 18);
    private static final Color GROUND_OBJECT_COLOUR = new Color(73, 255, 0);
    private static final Color WALL_OBJECT_COLOUR = new Color(255, 70, 70);
    private static final Color DECORATIVE_OBJECT_COLOUR = new Color(183, 126, 255);
    private static final Color PROJECTILE_COLOUR = new Color(255, 222, 2);
    private static final Color GRAPHICS_OBJECT_COLOUR = new Color(117, 113, 252);
    private static final Color NPC_COLOUR = new Color(188, 198, 255);
    private static final Color PLAYER_COLOUR = new Color(221, 133, 255);
    private static final int MAX_DISTANCE = 2400;

    @Inject
    private CreatorsOverlay(Client client, CreatorsPlugin plugin, CreatorsConfig config)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !plugin.isOverlaysActive())
            return null;

        renderObjectsOverlay(graphics);

        if (config.pathOverlay())
        {
            if (client.isInInstancedRegion())
            {
                renderInstancedProgramOverlay(graphics);
            }
            else
            {
                renderNonInstancedProgramOverlay(graphics);
            }
        }

        if (config.npcOverlay())
            renderNPCOverlay(graphics);

        if (config.playerOverlay())
            renderPlayerOverlay(graphics);

        if (config.projectileOverlay())
            renderProjectiles(graphics);

        return null;
    }

    public void renderNonInstancedProgramOverlay(Graphics2D graphics)
    {
        for (int e = 0; e < plugin.getCharacters().size(); e++)
        {
            Character character = plugin.getCharacters().get(e);
            if (!plugin.isInScene(character))
                continue;

            Program program = character.getProgram();
            if (program == null)
                continue;

            ProgramComp comp = program.getComp();

            Coordinate[] coordinates = comp.getCoordinates();
            for (int i = 0; i < coordinates.length - 1; i++)
            {
                if (coordinates[i] == null)
                    continue;

                LocalPoint lpStart = LocalPoint.fromScene(coordinates[i].getColumn(), coordinates[i].getRow());
                LocalPoint lpEnd = LocalPoint.fromScene(coordinates[i + 1].getColumn(), coordinates[i + 1].getRow());
                Point startPoint = Perspective.localToCanvas(client, lpStart, client.getPlane());
                Point endPoint = Perspective.localToCanvas(client, lpEnd, client.getPlane());
                if (startPoint == null || endPoint == null)
                    continue;

                graphics.setColor(program.getColor());
                graphics.setStroke(new BasicStroke(1));
                graphics.drawLine(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            }

            WorldPoint[] points = comp.getStepsWP();
            String name = character.getName();
            String abbreviation = "";
            int abbreviationLength = 3;
            int nameLength = name.length();

            if (abbreviationLength > nameLength)
            {
                abbreviation = character.getName().substring(0, nameLength);
            }
            else
            {
                abbreviation = character.getName().substring(0, 3);
            }

            for (int i = 0; i < points.length; i++)
            {
                LocalPoint localPoint = LocalPoint.fromWorld(client, points[i]);
                if (localPoint == null)
                    continue;

                Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, abbreviation, 0);
                if (textPoint == null)
                    continue;

                OverlayUtil.renderTextLocation(graphics, textPoint, abbreviation, program.getColor());
            }
        }
    }

    public void renderInstancedProgramOverlay(Graphics2D graphics)
    {
        for (int e = 0; e < plugin.getCharacters().size(); e++)
        {
            Character character = plugin.getCharacters().get(e);
            if (!plugin.isInScene(character))
                continue;

            LocalPoint savedLocation = character.getInstancedPoint();
            if (savedLocation == null || !savedLocation.isInScene())
                continue;

            Program program = character.getProgram();
            if (program == null)
                continue;

            ProgramComp comp = program.getComp();

            Coordinate[] coordinates = comp.getCoordinates();
            for (int i = 0; i < coordinates.length - 1; i++)
            {
                if (coordinates[i] == null)
                    continue;

                LocalPoint lpStart = LocalPoint.fromScene(coordinates[i].getColumn(), coordinates[i].getRow());
                LocalPoint lpEnd = LocalPoint.fromScene(coordinates[i + 1].getColumn(), coordinates[i + 1].getRow());
                Point startPoint = Perspective.localToCanvas(client, lpStart, client.getPlane());
                Point endPoint = Perspective.localToCanvas(client, lpEnd, client.getPlane());
                if (startPoint == null || endPoint == null)
                    continue;

                graphics.setColor(program.getColor());
                graphics.setStroke(new BasicStroke(1));
                graphics.drawLine(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            }

            LocalPoint[] points = comp.getStepsLP();
            String name = character.getName();
            String abbreviation = "";
            int abbreviationLength = 3;
            int nameLength = name.length();

            if (abbreviationLength > nameLength)
            {
                abbreviation = character.getName().substring(0, nameLength - 1);
            }
            else
            {
                abbreviation = character.getName().substring(0, 3);
            }

            for (int i = 0; i < points.length; i++)
            {
                LocalPoint localPoint = points[i];
                if (localPoint == null)
                    continue;

                Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, abbreviation, 0);
                if (textPoint == null)
                    continue;

                OverlayUtil.renderTextLocation(graphics, textPoint, abbreviation, program.getColor());
            }
        }
    }

    public void renderPlayerOverlay(Graphics2D graphics)
    {
        for (Player player : client.getPlayers())
        {
            LocalPoint localPoint = player.getLocalLocation();
            if (localPoint == null)
                continue;

            StringBuilder spotAnims = new StringBuilder();
            for (ActorSpotAnim spotAnim : player.getSpotAnims())
            {
                spotAnims.append(", G: ");
                spotAnims.append(spotAnim.getId());
            }

            OverlayUtil.renderActorOverlay(graphics, player, "A: " + player.getAnimation() + ", P: " + player.getPoseAnimation() + spotAnims, PLAYER_COLOUR);
        }
    }

    public void renderNPCOverlay(Graphics2D graphics)
    {
        for (NPC npc : client.getNpcs())
        {
            LocalPoint localPoint = npc.getLocalLocation();
            if (localPoint == null)
                continue;

            StringBuilder spotAnims = new StringBuilder();
            for (ActorSpotAnim spotAnim : npc.getSpotAnims())
            {
                spotAnims.append(", G: ");
                spotAnims.append(spotAnim.getId());
            }

            OverlayUtil.renderActorOverlay(graphics, npc, "ID: " + npc.getId() + ", A: " + npc.getAnimation() + ", P: " + npc.getPoseAnimation() + spotAnims, NPC_COLOUR);
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
                                if (plugin.getSelectedCharacter() == character)
                                {
                                    OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), SELECTED_COLOUR);
                                    continue;
                                }

                                if (plugin.getHoveredCharacter() == character)
                                {
                                    OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), HOVERED_COLOUR);
                                    continue;
                                }

                                OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), MY_OBJECT_COLOUR);
                            }
                        }
                        continue;
                    }

                    LocalPoint camera = new LocalPoint(client.getCameraX(), client.getCameraY());
                    if (gameObject.getLocalLocation().distanceTo(camera) <= MAX_DISTANCE)
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

    private void renderProjectiles(Graphics2D graphics)
    {
        for (Projectile projectile : client.getProjectiles())
        {
            int projectileId = projectile.getId();
            String text = "ID: " + projectileId;
            int x = (int) projectile.getX();
            int y = (int) projectile.getY();
            LocalPoint projectilePoint = new LocalPoint(x, y);
            Point textLocation = Perspective.getCanvasTextLocation(client, graphics, projectilePoint, text, 0);
            if (textLocation != null)
            {
                OverlayUtil.renderTextLocation(graphics, textLocation, text, PROJECTILE_COLOUR);
            }
        }
    }
}
