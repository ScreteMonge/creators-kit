package com.creatorskit;

import com.creatorskit.programming.MovementManager;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.MovementKeyFrame;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;

public class CreatorsOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final CreatorsConfig config;
    private final com.creatorskit.selection.SelectionManager selectionManager;
    private static final Color HOVERED_COLOUR = new Color(146, 206, 193, 255);
    private static final Color SELECTED_COLOUR = new Color(220, 253, 245);
    // Translucent pink fill for the Random Hazard Grid candidate-pool overlay.
    // Drawn only while the hazard-grid dialog is open. Stroke outline + fill
    // are stacked: the outline at higher alpha helps individual tiles stay
    // distinguishable when many overlap; the fill is what gives the area its
    // overall sense.
    private static final Color HAZARD_FILL_COLOUR = new Color(255, 64, 192, 60);
    private static final Color HAZARD_STROKE_COLOUR = new Color(255, 64, 192, 180);
    private static final Color GAME_OBJECT_COLOUR = new Color(255, 138, 18);
    private static final Color DYNAMIC_OBJECT_COLOUR = new Color(255, 190, 130);
    private static final Color GROUND_OBJECT_COLOUR = new Color(73, 255, 0);
    private static final Color WALL_OBJECT_COLOUR = new Color(255, 70, 70);
    private static final Color DECORATIVE_OBJECT_COLOUR = new Color(183, 126, 255);
    private static final Color PROJECTILE_COLOUR = new Color(255, 222, 2);
    private static final Color NPC_COLOUR = new Color(188, 198, 255);
    private static final Color PLAYER_COLOUR = new Color(221, 133, 255);
    private static final int MAX_DISTANCE = 2400;
    private final BasicStroke dashedLine = new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] { 5, 5, 5, 5 }, 0f);


    @Inject
    private CreatorsOverlay(Client client, CreatorsPlugin plugin, CreatorsConfig config, com.creatorskit.selection.SelectionManager selectionManager)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.selectionManager = selectionManager;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        WorldView topLevelWorldView = client.getTopLevelWorldView();
        renderOverlays(graphics, topLevelWorldView);

        IndexedObjectSet<? extends WorldView> worldViews = topLevelWorldView.worldViews();
        for (WorldView worldView : worldViews)
        {
            renderOverlays(graphics, worldView);
        }

        renderHazardGridPool(graphics, topLevelWorldView);

        return null;
    }

    /**
     * Pink tile-pool preview for the Random Hazard Grid dialog. Walks the
     * dialog state's resolved tile pool and fills each tile with a
     * translucent pink quad so the user can see the live candidate area as
     * they refine it with corners A/B + CTRL+I/E paint. No-op when the
     * dialog isn't open.
     *
     * <p>Tiles outside the loaded scene get skipped silently --
     * Perspective.getCanvasTilePoly returns null in that case, which is
     * the correct visual: the pool extends beyond what's currently
     * rendered but only the on-screen tiles need a quad.
     */
    private void renderHazardGridPool(Graphics2D graphics, WorldView worldView)
    {
        com.creatorskit.swing.CreatorsPanel.HazardGridState state = plugin.getCreatorsPanel().getHazardGridState();
        if (state == null) return;
        java.util.Set<WorldPoint> pool = state.resolvePool();
        if (pool.isEmpty()) return;
        int currentPlane = worldView.getPlane();
        for (WorldPoint wp : pool)
        {
            // Multi-plane pools are allowed (a CTRL+I-painted tile can be
            // on a different floor than the corners); only paint the ones
            // matching the current plane so the user doesn't see ghost
            // tiles when standing on a different floor.
            if (wp.getPlane() != currentPlane) continue;
            LocalPoint lp = LocalPoint.fromWorld(worldView, wp);
            if (lp == null) continue;
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly == null) continue;
            graphics.setColor(HAZARD_FILL_COLOUR);
            graphics.fillPolygon(poly);
            graphics.setColor(HAZARD_STROKE_COLOUR);
            graphics.drawPolygon(poly);
        }
    }

    public void renderOverlays(Graphics2D graphics, WorldView worldView)
    {
        boolean keyHeld = config.enableCtrlHotkeys() && client.isKeyPressed(KeyCode.KC_CONTROL);
        if (keyHeld)
        {
            renderSelectedRLObject(graphics, worldView);
        }

        if (!plugin.isOverlaysActive())
        {
            return;
        }

        if (config.myObjectOverlay())
        {
            renderRLObjects(graphics, keyHeld, worldView);
        }

        renderObjectsOverlay(graphics, worldView);

        if (config.pathOverlay())
        {
            if (MovementManager.useLocalLocations(worldView))
            {
                renderPOHProgramOverlay(graphics, worldView);
            }
            else
            {
                renderWorldProgramOverlay(graphics, worldView);
            }
        }

        if (config.npcOverlay())
        {
            renderNPCOverlay(graphics, worldView);
        }

        if (config.playerOverlay())
        {
            renderPlayerOverlay(graphics, worldView);
        }

        if (config.projectileOverlay())
        {
            renderProjectiles(graphics, worldView);
        }
    }

    public void renderWorldProgramOverlay(Graphics2D graphics, WorldView worldView)
    {
        graphics.setStroke(new BasicStroke(1));
        
        for (int e = 0; e < plugin.getCharacters().size(); e++)
        {
            Character character = plugin.getCharacters().get(e);
            if (!character.isInScene())
            {
                continue;
            }

            boolean poh = MovementManager.useLocalLocations(worldView);
            if ((!poh && character.isInPOH()) || (poh && !character.isInPOH()))
            {
                continue;
            }

            KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
            if (kf == null)
            {
                continue;
            }

            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            int plane = keyFrame.getPlane();
            if (worldView.getPlane() != plane)
            {
                continue;
            }

            int[][] path = keyFrame.getPath();

            boolean selectedCharacter = selectionManager.isSelected(character);
            Color color = character.getColor();
            if (selectedCharacter)
            {
                color = SELECTED_COLOUR;
            }

            if (path.length > 0)
            {
                WorldPoint startPoint = new WorldPoint(path[0][0], path[0][1], plane);
                if (worldView.isInstance())
                {
                    Collection<WorldPoint> wps = WorldPoint.toLocalInstance(worldView, startPoint);
                    if (!wps.isEmpty())
                    {
                        startPoint = wps.iterator().next();
                    }
                }

                LocalPoint localPoint = LocalPoint.fromWorld(worldView, startPoint);
                if (localPoint != null && localPoint.isInScene())
                {
                    Point p = Perspective.localToCanvas(client, localPoint, plane);
                    String abbreviation = getAbbreviation(character);
                    if (p != null)
                    {
                        OverlayUtil.renderTextLocation(graphics, p, abbreviation, color);
                    }
                }
            }
            
            for (int i = 0; i < path.length - 1; i++)
            {
                WorldPoint wpStart = new WorldPoint(path[i][0], path[i][1], plane);
                WorldPoint wpEnd = new WorldPoint(path[i + 1][0], path[i + 1][1], plane);

                if (worldView.isInstance())
                {
                    Collection<WorldPoint> wpsStart = WorldPoint.toLocalInstance(worldView, wpStart);
                    if (!wpsStart.isEmpty())
                    {
                        wpStart = wpsStart.iterator().next();
                    }

                    Collection<WorldPoint> wpsEnd = WorldPoint.toLocalInstance(worldView, wpEnd);
                    if (!wpsEnd.isEmpty())
                    {
                        wpEnd = wpsEnd.iterator().next();
                    }
                }

                LocalPoint lpStart = LocalPoint.fromWorld(worldView, wpStart.getX(), wpStart.getY());
                LocalPoint lpEnd = LocalPoint.fromWorld(worldView, wpEnd.getX(), wpEnd.getY());

                if (lpStart == null && lpEnd == null)
                {
                    continue;
                }

                if (lpStart != null && lpEnd == null)
                {
                    Point startPoint = Perspective.localToCanvas(client, lpStart, plane);
                    if (startPoint == null)
                    {
                        continue;
                    }

                    graphics.setColor(color);
                    graphics.drawRect(startPoint.getX() - 5, startPoint.getY() - 5, 10, 10);
                    continue;
                }

                if (lpStart == null && lpEnd != null)
                {
                    Point endPoint = Perspective.localToCanvas(client, lpEnd, plane);
                    if (endPoint == null)
                    {
                        continue;
                    }

                    graphics.setColor(color);
                    graphics.drawRect(endPoint.getX() - 5, endPoint.getY() - 5, 10, 10);
                    continue;
                }

                Point startPoint = Perspective.localToCanvas(client, lpStart, plane);
                if (startPoint == null)
                {
                    continue;
                }

                Point endPoint = Perspective.localToCanvas(client, lpEnd, plane);
                if (endPoint == null)
                {
                    continue;
                }

                graphics.setColor(color);
                graphics.setStroke(new BasicStroke(3));
                if (!selectedCharacter)
                {
                    graphics.setStroke(dashedLine);
                }

                graphics.drawLine(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            }
        }
    }

    public void renderPOHProgramOverlay(Graphics2D graphics, WorldView worldView)
    {
        graphics.setStroke(new BasicStroke(1));

        for (int e = 0; e < plugin.getCharacters().size(); e++)
        {
            Character character = plugin.getCharacters().get(e);
            if (!character.isInScene())
            {
                continue;
            }

            boolean poh = MovementManager.useLocalLocations(worldView);
            if ((!poh && character.isInPOH()) || (poh && !character.isInPOH()))
            {
                continue;
            }

            KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
            if (kf == null)
            {
                continue;
            }

            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            int plane = keyFrame.getPlane();
            if (worldView.getPlane() != plane)
            {
                continue;
            }

            boolean selectedCharacter = selectionManager.isSelected(character);
            Color color = character.getColor().brighter();
            if (selectedCharacter)
            {
                color = SELECTED_COLOUR;
            }

            int[][] path = keyFrame.getPath();

            if (path.length > 0)
            {
                LocalPoint localPoint = LocalPoint.fromScene(path[0][0], path[0][1], worldView);
                Point p = Perspective.localToCanvas(client, localPoint, plane);
                String abbreviation = getAbbreviation(character);
                if (p != null)
                {
                    OverlayUtil.renderTextLocation(graphics, p, abbreviation, color);
                }
            }

            for (int i = 0; i < path.length - 1; i++)
            {
                LocalPoint lpStart = LocalPoint.fromScene(path[i][0], path[i][1], worldView);
                LocalPoint lpEnd = LocalPoint.fromScene(path[i + 1][0], path[i + 1][1], worldView);

                if (!lpStart.isInScene() && !lpEnd.isInScene())
                {
                    continue;
                }

                if (lpStart.isInScene() && !lpEnd.isInScene())
                {
                    Point startPoint = Perspective.localToCanvas(client, lpStart, plane);
                    if (startPoint == null)
                    {
                        continue;
                    }

                    graphics.setColor(color);
                    graphics.drawRect(startPoint.getX() - 5, startPoint.getY() - 5, 10, 10);
                    continue;
                }

                if (!lpStart.isInScene() && lpEnd.isInScene())
                {
                    Point endPoint = Perspective.localToCanvas(client, lpEnd, plane);
                    if (endPoint == null)
                    {
                        continue;
                    }

                    graphics.setColor(color);
                    graphics.drawRect(endPoint.getX() - 5, endPoint.getY() - 5, 10, 10);
                    continue;
                }

                Point startPoint = Perspective.localToCanvas(client, lpStart, plane);
                if (startPoint == null)
                {
                    continue;
                }

                Point endPoint = Perspective.localToCanvas(client, lpEnd, plane);
                if (endPoint == null)
                {
                    continue;
                }

                graphics.setColor(color);
                graphics.setStroke(new BasicStroke(3));
                if (!selectedCharacter)
                {
                    graphics.setStroke(dashedLine);
                }

                graphics.drawLine(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            }
        }
    }

    private static String getAbbreviation(Character character)
    {
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

        return abbreviation;
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

        if (tiles == null)
        {
            return;
        }

        int z = worldView.getPlane();

        for (int x = 0; x < worldView.getSizeX(); x++)
        {
            for (int y = 0; y < worldView.getSizeY(); y++)
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

    public void renderSelectedRLObject(Graphics2D graphics, WorldView worldView)
    {
        boolean poh = MovementManager.useLocalLocations(worldView);
        Font originalFont = graphics.getFont();
        graphics.setFont(originalFont.deriveFont(Font.BOLD));
        try
        {
            for (Character character : selectionManager.getSelected())
            {
                if (character == null || !character.isInScene())
                {
                    continue;
                }

                if ((!poh && character.isInPOH()) || (poh && !character.isInPOH()))
                {
                    continue;
                }

                CKObject ckObject = character.getCkObject();
                if (ckObject == null || !ckObject.isActive())
                {
                    continue;
                }

                LocalPoint lp = ckObject.getLocation();
                if (lp == null || !lp.isInScene())
                {
                    continue;
                }

                Model model = ckObject.getModel();
                if (model == null)
                {
                    continue;
                }

                model.calculateBoundsCylinder();

                Point p = Perspective.getCanvasTextLocation(client, graphics, lp, character.getName(), model.getModelHeight());
                if (p != null)
                {
                    OverlayUtil.renderTextLocation(graphics, p, character.getName(), SELECTED_COLOUR);
                }
            }
        }
        finally
        {
            graphics.setFont(originalFont);
        }
    }

    public void renderRLObjects(Graphics2D graphics, boolean keyHeld, WorldView worldView)
    {
        for (int i = 0; i < plugin.getCharacters().size(); i++)
        {
            Character character = plugin.getCharacters().get(i);
            if (!character.isInScene())
            {
                continue;
            }

            CKObject ckObject = character.getCkObject();
            if (ckObject == null || !ckObject.isActive())
            {
                continue;
            }

            LocalPoint lp = ckObject.getLocation();
            if (lp == null || !lp.isInScene())
            {
                continue;
            }

            String name = character.getName();
            Model model = ckObject.getModel();
            if (model == null)
            {
                continue;
            }

            model.calculateBoundsCylinder();

            Point point = Perspective.getCanvasTextLocation(client, graphics, lp, name, model.getModelHeight());

            if (point == null)
            {
                continue;
            }

            if (selectionManager.isSelected(character))
            {
                if (keyHeld)
                {
                    continue;
                }

                Font prev = graphics.getFont();
                graphics.setFont(prev.deriveFont(Font.BOLD));
                try
                {
                    OverlayUtil.renderTextLocation(graphics, point, name, SELECTED_COLOUR);
                }
                finally
                {
                    graphics.setFont(prev);
                }
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

            Color colour = character.getColor();
            OverlayUtil.renderTextLocation(graphics, point, name, colour);
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

                    LocalPoint camera = client.getCameraFocusEntity().getCameraFocus();
                    LocalPoint freeCamera = new LocalPoint(client.getCameraX(), client.getCameraY(), worldView);
                    LocalPoint localPoint = gameObject.getLocalLocation();

                    if (!worldView.isTopLevel() || localPoint.distanceTo(camera) <= MAX_DISTANCE || localPoint.distanceTo(freeCamera) <= MAX_DISTANCE)
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
            int id = groundObject.getId();
            if (Arrays.stream(TOA_PUZZLE).anyMatch(e -> e == id))
            {
                return;
            }

            LocalPoint camera = client.getCameraFocusEntity().getCameraFocus();
            LocalPoint freeCamera = new LocalPoint(client.getCameraX(), client.getCameraY(), worldView);
            LocalPoint localPoint = groundObject.getLocalLocation();

            if (!worldView.isTopLevel() || localPoint.distanceTo(camera) <= MAX_DISTANCE || localPoint.distanceTo(freeCamera) <= MAX_DISTANCE)
            {
                OverlayUtil.renderTileOverlay(graphics, groundObject, "ID: " + groundObject.getId(), GROUND_OBJECT_COLOUR);
            }
        }
    }

    private final int[] TOA_PUZZLE = new int[]{
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON1,
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON2,
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON3,
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON4,
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON5,
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON6,
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON7,
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON8,
            ObjectID.TOA_SCABARAS_MEMORYGAME_BUTTON9,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE1,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE2,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE3,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE4,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE5,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE6,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE7,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE8,
            ObjectID.TOA_SCABARAS_MEMORYGAME_TILE9
    };

    public void renderWallObjects(Graphics2D graphics, Tile tile, WorldView worldView)
    {
        TileObject tileObject = tile.getWallObject();
        if (tileObject != null)
        {
            LocalPoint camera = client.getCameraFocusEntity().getCameraFocus();
            LocalPoint freeCamera = new LocalPoint(client.getCameraX(), client.getCameraY(), worldView);
            LocalPoint localPoint = tileObject.getLocalLocation();

            if (!worldView.isTopLevel() || localPoint.distanceTo(camera) <= MAX_DISTANCE || localPoint.distanceTo(freeCamera) <= MAX_DISTANCE)
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
            LocalPoint camera = client.getCameraFocusEntity().getCameraFocus();
            LocalPoint freeCamera = new LocalPoint(client.getCameraX(), client.getCameraY(), worldView);
            LocalPoint localPoint = tileObject.getLocalLocation();

            if (!worldView.isTopLevel() || localPoint.distanceTo(camera) <= MAX_DISTANCE || localPoint.distanceTo(freeCamera) <= MAX_DISTANCE)
            {
                OverlayUtil.renderTileOverlay(graphics, tileObject, "ID: " + tileObject.getId(), DECORATIVE_OBJECT_COLOUR);
            }
        }
    }

    private void renderProjectiles(Graphics2D graphics, WorldView worldView)
    {
        for (Projectile projectile : client.getProjectiles())
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
