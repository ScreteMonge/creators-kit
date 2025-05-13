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
    private static final Color DYNAMIC_OBJECT_COLOUR = new Color(255, 190, 130);
    private static final Color GROUND_OBJECT_COLOUR = new Color(73, 255, 0);
    private static final Color WALL_OBJECT_COLOUR = new Color(255, 70, 70);
    private static final Color DECORATIVE_OBJECT_COLOUR = new Color(183, 126, 255);
    private static final Color PROJECTILE_COLOUR = new Color(255, 222, 2);
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
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        WorldView worldView = client.getTopLevelWorldView();
        Scene scene = worldView.getScene();

        boolean keyHeld = config.enableCtrlHotkeys() && client.isKeyPressed(KeyCode.KC_CONTROL);
        if (keyHeld)
        {
            renderSelectedRLObject(graphics);
        }

        if (!plugin.isOverlaysActive())
        {
            return null;
        }

        renderRLObjects(graphics, keyHeld);

        renderObjectsOverlay(graphics, worldView);

        if (config.pathOverlay())
        {
            if (scene.isInstance())
            {
                renderInstancedProgramOverlay(graphics, worldView);
            }
            else
            {
                renderNonInstancedProgramOverlay(graphics, worldView);
            }
        }

        if (config.npcOverlay())
            renderNPCOverlay(graphics, worldView);

        if (config.playerOverlay())
            renderPlayerOverlay(graphics, worldView);

        if (config.projectileOverlay())
            renderProjectiles(graphics, worldView);

        return null;
    }

    public void renderNonInstancedProgramOverlay(Graphics2D graphics, WorldView worldView)
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

                if (!comp.isPathFound())
                    continue;

                LocalPoint lpStart = LocalPoint.fromScene(coordinates[i].getColumn(), coordinates[i].getRow(), worldView.getScene());
                LocalPoint lpEnd = LocalPoint.fromScene(coordinates[i + 1].getColumn(), coordinates[i + 1].getRow(), worldView.getScene());
                Point startPoint = Perspective.localToCanvas(client, lpStart, worldView.getPlane());
                Point endPoint = Perspective.localToCanvas(client, lpEnd, worldView.getPlane());
                if (startPoint == null || endPoint == null)
                    continue;

                graphics.setColor(program.getColor());
                graphics.setStroke(new BasicStroke(1));
                graphics.drawLine(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            }

            WorldPoint[] points = comp.getStepsWP();
            String name = character.getName();
            String abbreviation;
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
                LocalPoint localPoint = LocalPoint.fromWorld(worldView, points[i]);
                if (localPoint == null)
                    continue;

                Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, abbreviation, 0);
                if (textPoint == null)
                    continue;

                OverlayUtil.renderTextLocation(graphics, textPoint, abbreviation, program.getColor());
            }
        }
    }

    public void renderInstancedProgramOverlay(Graphics2D graphics, WorldView worldView)
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

                if (!comp.isPathFound())
                    continue;

                LocalPoint lpStart = LocalPoint.fromScene(coordinates[i].getColumn(), coordinates[i].getRow(), worldView.getScene());
                LocalPoint lpEnd = LocalPoint.fromScene(coordinates[i + 1].getColumn(), coordinates[i + 1].getRow(), worldView.getScene());
                Point startPoint = Perspective.localToCanvas(client, lpStart, worldView.getPlane());
                Point endPoint = Perspective.localToCanvas(client, lpEnd, worldView.getPlane());
                if (startPoint == null || endPoint == null)
                    continue;

                graphics.setColor(program.getColor());
                graphics.setStroke(new BasicStroke(1));
                graphics.drawLine(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            }

            LocalPoint[] points = comp.getStepsLP();
            String name = character.getName();
            String abbreviation;
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

    public void renderPlayerOverlay(Graphics2D graphics, WorldView worldView)
    {
        for (Player player : worldView.players())
        {
            LocalPoint localPoint = player.getLocalLocation();
            if (localPoint == null)
                continue;

            StringBuilder spotAnims = new StringBuilder();
            for (ActorSpotAnim spotAnim : player.getSpotAnims())
            {
                spotAnims.append(", G: ");
                spotAnims.append(spotAnim.getId());
                spotAnims.append(", H: ");
                spotAnims.append(spotAnim.getHeight());
            }

            OverlayUtil.renderActorOverlay(graphics, player, "A: " + player.getAnimation() + ", P: " + player.getPoseAnimation() + spotAnims, PLAYER_COLOUR);
        }
    }

    public void renderNPCOverlay(Graphics2D graphics, WorldView worldView)
    {
        for (NPC npc : worldView.npcs())
        {
            LocalPoint localPoint = npc.getLocalLocation();
            if (localPoint == null)
                continue;

            StringBuilder spotAnims = new StringBuilder();
            for (ActorSpotAnim spotAnim : npc.getSpotAnims())
            {
                spotAnims.append(", G: ");
                spotAnims.append(spotAnim.getId());
                spotAnims.append(", H: ");
                spotAnims.append(spotAnim.getHeight());
            }

            OverlayUtil.renderActorOverlay(graphics, npc, "ID: " + npc.getId() + ", A: " + npc.getAnimation() + ", P: " + npc.getPoseAnimation() + spotAnims, NPC_COLOUR);
        }
    }

    public void renderObjectsOverlay(Graphics2D graphics, WorldView worldView)
    {
        Scene scene = worldView.getScene();
        Tile[][][] tiles = scene.getTiles();
        int z = worldView.getPlane();

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
                    renderGameObjects(graphics, tile, worldView);
                }

                if (config.groundObjectOverlay())
                {
                    renderGroundObjects(graphics, tile, worldView);
                }

                if (config.wallObjectOverlay())
                {
                    renderWallObjects(graphics, tile, worldView);
                }

                if (config.decorativeObjectOverlay())
                {
                    renderDecorativeObjects(graphics, tile, worldView);
                }
            }
        }
    }

    public void renderSelectedRLObject(Graphics2D graphics)
    {
        Character character = plugin.getSelectedCharacter();
        if (character == null)
        {
            return;
        }

        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        LocalPoint lp = ckObject.getLocation();
        if (lp == null || !plugin.isInScene(character))
        {
            return;
        }

        Point p = Perspective.getCanvasTextLocation(client, graphics, lp, character.getName(), 0);
        if (p != null)
        {
            OverlayUtil.renderTextLocation(graphics, p, character.getName(), SELECTED_COLOUR);
        }
    }

    public void renderRLObjects(Graphics2D graphics, boolean keyHeld)
    {
        for (int i = 0; i < plugin.getCharacters().size(); i++)
        {
            Character character = plugin.getCharacters().get(i);
            if (!plugin.isInScene(character) || !character.isActive())
            {
                continue;
            }

            CKObject ckObject = character.getCkObject();
            if (ckObject == null)
            {
                continue;
            }

            LocalPoint lp = ckObject.getLocation();
            if (lp == null)
            {
                continue;
            }

            String name = character.getName();
            Point point = Perspective.getCanvasTextLocation(client, graphics, lp, name, 0);

            if (point == null)
            {
                continue;
            }

            if (plugin.getSelectedCharacter() == character)
            {
                if (keyHeld)
                {
                    continue;
                }

                OverlayUtil.renderTextLocation(graphics, point, name, SELECTED_COLOUR);
                continue;
            }

            if (!config.myObjectOverlay())
            {
                continue;
            }

            if (plugin.getHoveredCharacter() == character)
            {
                OverlayUtil.renderTextLocation(graphics, point, name, HOVERED_COLOUR);
                continue;
            }

            OverlayUtil.renderTextLocation(graphics, point, name, MY_OBJECT_COLOUR);
        }
    }

    public void renderGameObjects(Graphics2D graphics, Tile tile, WorldView worldView)
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

                    if (gameObject.getRenderable() instanceof RuneLiteObject)
                    {
                        continue;
                    }

                    LocalPoint camera = new LocalPoint(client.getCameraX(), client.getCameraY(), worldView);
                    if (gameObject.getLocalLocation().distanceTo(camera) <= MAX_DISTANCE)
                    {
                        if (!config.gameObjectOverlay())
                        {
                            continue;
                        }

                        stringBuilder.append("ID: ").append(gameObject.getId());
                        Color color = GAME_OBJECT_COLOUR;
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

                            color = DYNAMIC_OBJECT_COLOUR;
                        }
                        else
                        {
                            stringBuilder.append(" A: -1");
                        }

                        OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), color);
                    }
                }
            }
        }
    }

    public void renderGroundObjects(Graphics2D graphics, Tile tile, WorldView worldView)
    {
        GroundObject groundObject = tile.getGroundObject();
        if (groundObject != null)
        {
            LocalPoint camera = new LocalPoint(client.getCameraX(), client.getCameraY(), worldView);
            if (groundObject.getLocalLocation().distanceTo(camera) <= MAX_DISTANCE)
            {
                OverlayUtil.renderTileOverlay(graphics, groundObject, "ID: " + groundObject.getId(), GROUND_OBJECT_COLOUR);
            }
        }
    }

    public void renderWallObjects(Graphics2D graphics, Tile tile, WorldView worldView)
    {
        TileObject tileObject = tile.getWallObject();
        if (tileObject != null)
        {
            LocalPoint camera = new LocalPoint(client.getCameraX(), client.getCameraY(), worldView);
            if (tileObject.getLocalLocation().distanceTo(camera) <= MAX_DISTANCE)
            {
                OverlayUtil.renderTileOverlay(graphics, tileObject, "ID: " + tileObject.getId(), WALL_OBJECT_COLOUR);
            }
        }
    }

    public void renderDecorativeObjects(Graphics2D graphics, Tile tile, WorldView worldView)
    {
        TileObject tileObject = tile.getDecorativeObject();
        if (tileObject != null)
        {
            LocalPoint camera = new LocalPoint(client.getCameraX(), client.getCameraY(), worldView);
            if (tileObject.getLocalLocation().distanceTo(camera) <= MAX_DISTANCE)
            {
                OverlayUtil.renderTileOverlay(graphics, tileObject, "ID: " + tileObject.getId(), DECORATIVE_OBJECT_COLOUR);
            }
        }
    }

    private void renderProjectiles(Graphics2D graphics, WorldView worldView)
    {
        for (Projectile projectile : worldView.getProjectiles())
        {
            int projectileId = projectile.getId();
            String text = "ID: " + projectileId;
            int x = (int) projectile.getX();
            int y = (int) projectile.getY();
            LocalPoint projectilePoint = new LocalPoint(x, y, worldView);
            Point textLocation = Perspective.getCanvasTextLocation(client, graphics, projectilePoint, text, 0);
            if (textLocation != null)
            {
                OverlayUtil.renderTextLocation(graphics, textLocation, text, PROJECTILE_COLOUR);
            }
        }
    }
}
