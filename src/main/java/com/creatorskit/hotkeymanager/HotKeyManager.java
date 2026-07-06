package com.creatorskit.hotkeymanager;

import com.creatorskit.*;
import com.creatorskit.Character;
import com.creatorskit.programming.MovementManager;
import com.creatorskit.programming.Programmer;
import com.creatorskit.programming.orientation.OrientationHotkeyMode;
import com.creatorskit.selection.SelectionCommand;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.selection.SelectionOrigin;
import com.creatorskit.swing.CreatorsPanel;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.MovementKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.OrientationKeyFrame;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.Keybind;
import net.runelite.client.util.HotkeyListener;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class HotKeyManager
{
    private Client client;
    private ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private CreatorsOverlay overlay;
    private CreatorsConfig config;
    private final SelectionManager selectionManager;
    private final MovementManager movementManager;

    @Setter
    private int oculusOrbSpeed = 36;
    private final int _45_DEGREES = 256;

    @Inject
    public HotKeyManager(Client client, ClientThread clientThread, CreatorsPlugin plugin, CreatorsOverlay overlay, CreatorsConfig config, SelectionManager selectionManager, MovementManager movementManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.overlay = overlay;
        this.config = config;
        this.selectionManager = selectionManager;
        this.movementManager = movementManager;

        oculusOrbSpeed = config.orbSpeed();
    }

    public final HotkeyListener oculusOrbListener = new HotkeyListener(() -> config.toggleOrbHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            if (client.getCameraMode() == 1)
            {
                client.setCameraMode(0);
                client.setFreeCameraSpeed(12);
                return;
            }

            client.setCameraMode(1);
            client.setFreeCameraSpeed(oculusOrbSpeed);
        }
    };

    public final HotkeyListener overlayKeyListener = new HotkeyListener(() -> config.toggleOverlaysHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            overlay.toggleOverlays();
        }
    };

    public final HotkeyListener orbPreset1Listener = new HotkeyListener(() -> config.orbSpeedHotkey1())
    {
        @Override
        public void hotkeyPressed()
        {
            client.setFreeCameraSpeed(config.speedHotkey1());
            plugin.sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey1());
        }
    };

    public final HotkeyListener orbPreset2Listener = new HotkeyListener(() -> config.orbSpeedHotkey2())
    {
        @Override
        public void hotkeyPressed()
        {
            client.setFreeCameraSpeed(config.speedHotkey2());
            plugin.sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey2());
        }
    };

    public final HotkeyListener orbPreset3Listener = new HotkeyListener(() -> config.orbSpeedHotkey3())
    {
        @Override
        public void hotkeyPressed()
        {
            client.setFreeCameraSpeed(config.speedHotkey3());
            plugin.sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey3());
        }
    };

    public final HotkeyListener quickSpawnListener = new HotkeyListener(() -> config.quickSpawnHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            for (Character c : selectionManager.getSelected())
            {
                c.toggleActive(clientThread);
            }
        }
    };

    public final HotkeyListener quickLocationListener = new HotkeyListener(() -> config.quickLocationHotkey())
    {
        @Override
        public void hotkeyPressed() { clientThread.invokeLater(() -> onSetLocation());}
    };

    public void onSetLocation()
    {
        Programmer programmer = plugin.getCreatorsPanel().getToolBox().getProgrammer();

        Character primary = selectionManager.getPrimary();
        if (primary == null)
        {
            return;
        }

        boolean inPOH = primary.isInPOH();
        int[] primaryCoordinates = new int[]{0, 0};
        if (inPOH)
        {
            LocalPoint lp = primary.getInstancedPoint();
            if (lp != null)
            {
                primaryCoordinates = new int[]{lp.getSceneX(), lp.getSceneY()};
            }
        }
        else
        {
            WorldPoint wp = primary.getNonInstancedPoint();
            if (wp != null)
            {
                primaryCoordinates = new int[]{wp.getX(), wp.getY()};
            }
        }

        for (Character c : selectionManager.getSelected())
        {
            if (c == primary)
            {
                continue;
            }

            if (c.isInPOH() != inPOH)
            {
                continue;
            }

            int[] diff = calculateLocationDifference(inPOH, c, primaryCoordinates);
            c.setLocation(client, clientThread, programmer, false, true, ActiveOption.ACTIVE, LocationOption.TO_HOVERED_TILE, diff);
        }

        primary.setLocation(client, clientThread, programmer, false, true, ActiveOption.ACTIVE, LocationOption.TO_HOVERED_TILE);
    }

    private int[] calculateLocationDifference(boolean inPOH, Character c, int[] primaryCoordinates)
    {
        int[] coords = new int[]{0, 0};

        if (inPOH)
        {
            LocalPoint lp = c.getInstancedPoint();
            if (lp != null)
            {
                coords = new int[]{lp.getSceneX(), lp.getSceneY()};
            }
        }
        else
        {
            WorldPoint wp = c.getNonInstancedPoint();
            if (wp != null)
            {
                coords = new int[]{wp.getX(), wp.getY()};
            }
        }

        return new int[]{coords[0] - primaryCoordinates[0], coords[1] - primaryCoordinates[1]};
    }

    public final HotkeyListener quickDuplicateListener = new HotkeyListener(() -> config.quickDuplicateHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            onDuplicate(true);
        }
    };

    public void onDuplicate(boolean setLocation)
    {
        Character primary = selectionManager.getPrimary();
        if (primary == null)
        {
            return;
        }

        CreatorsPanel creatorsPanel = plugin.getCreatorsPanel();

        boolean inPOH = primary.isInPOH();
        int[] primaryCoordinates = new int[]{0, 0};
        if (inPOH)
        {
            LocalPoint lp = primary.getInstancedPoint();
            if (lp != null)
            {
                primaryCoordinates = new int[]{lp.getSceneX(), lp.getSceneY()};
            }
        }
        else
        {
            WorldPoint wp = primary.getNonInstancedPoint();
            if (wp != null)
            {
                primaryCoordinates = new int[]{wp.getX(), wp.getY()};
            }
        }

        Set<Character> selected = selectionManager.getSelected();
        Set<Character> characters = new HashSet<>(selected);
        selectionManager.clear(SelectionOrigin.DIRECT);

        for (Character c : characters)
        {
            if (c == primary)
            {
                continue;
            }

            if (c.isInPOH() != inPOH)
            {
                continue;
            }

            int[] coords = new int[]{0, 0};

            if (inPOH)
            {
                LocalPoint lp = c.getInstancedPoint();
                if (lp != null)
                {
                    coords = new int[]{lp.getSceneX(), lp.getSceneY()};
                }


            }
            else
            {
                WorldPoint wp = c.getNonInstancedPoint();
                if (wp != null)
                {
                    coords = new int[]{wp.getX(), wp.getY()};
                }
            }

            int[] diff = new int[]{coords[0] - primaryCoordinates[0], coords[1] - primaryCoordinates[1]};

            creatorsPanel.onDuplicatePressed(c, setLocation, diff, SelectionCommand.ADD);
        }

        creatorsPanel.onDuplicatePressed(primary, setLocation, new int[]{0, 0}, SelectionCommand.ADD);
    }

    public final HotkeyListener quickRotateCWListener = new HotkeyListener(() -> config.quickRotateCWHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            for (Character c : selectionManager.getSelected())
            {
                c.addOrientation(-_45_DEGREES);
            }
        }
    };

    public final HotkeyListener quickRotateCCWListener = new HotkeyListener(() -> config.quickRotateCCWHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            for (Character c : selectionManager.getSelected())
            {
                c.addOrientation(_45_DEGREES);
            }
        }
    };

    public final HotkeyListener autoLeftListener = new HotkeyListener(() -> config.rotateLeftHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.setAutoRotateYaw((plugin.getAutoRotateYaw() == AutoRotate.OFF) ? AutoRotate.LEFT : AutoRotate.OFF);
        }
    };

    public final HotkeyListener autoRightListener = new HotkeyListener(() -> config.rotateRightHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.setAutoRotateYaw((plugin.getAutoRotateYaw() == AutoRotate.OFF) ? AutoRotate.RIGHT : AutoRotate.OFF);
        }
    };

    public final HotkeyListener autoUpListener = new HotkeyListener(() -> config.rotateUpHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.setAutoRotatePitch((plugin.getAutoRotatePitch() == AutoRotate.OFF) ? AutoRotate.UP : AutoRotate.OFF);
        }
    };

    public final HotkeyListener autoDownListener = new HotkeyListener(() -> config.rotateDownHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.setAutoRotatePitch((plugin.getAutoRotatePitch() == AutoRotate.OFF) ? AutoRotate.DOWN : AutoRotate.OFF);
        }
    };

    public final HotkeyListener addProgramStepListener = new HotkeyListener(() -> config.addProgramStepHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            clientThread.invokeLater(() -> addProgramStep());
        }
    };

    public void addProgramStep()
    {
        Character primary = selectionManager.getPrimary();
        if (primary == null)
        {
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        Tile tile = worldView.getSelectedSceneTile();
        if (tile == null)
        {
            return;
        }

        LocalPoint localPoint = tile.getLocalLocation();
        if (localPoint == null || !localPoint.isInScene())
        {
            return;
        }

        addMovementKeyFrames(primary, worldView, false, localPoint);
    }

    public void onAddMovementMenuOptionPressed()
    {
        Character primary = selectionManager.getPrimary();
        if (primary == null)
        {
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return;
        }

        CKObject ckObject = primary.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        LocalPoint localPoint = ckObject.getLocation();
        if (localPoint == null || !localPoint.isInScene())
        {
            return;
        }

        addMovementKeyFrames(primary, worldView, true, localPoint);
    }

    public void addMovementKeyFrames(Character primary, WorldView worldView, boolean createNew, LocalPoint localPoint)
    {
        boolean inPOH = primary.isInPOH();
        KeyFrame primaryKeyFrame = createMovementKeyFrame(primary, worldView, createNew, localPoint, new int[]{0, 0});
        Character[] characters = new Character[]{primary};
        KeyFrame[][] frames = new KeyFrame[][]{new KeyFrame[]{primaryKeyFrame}};

        int[] primaryCoordinates = new int[]{0, 0};
        if (inPOH)
        {
            LocalPoint lp = primary.getInstancedPoint();
            if (lp != null)
            {
                primaryCoordinates = new int[]{lp.getSceneX(), lp.getSceneY()};
            }
        }
        else
        {
            WorldPoint wp = primary.getNonInstancedPoint();
            if (wp != null)
            {
                primaryCoordinates = new int[]{wp.getX(), wp.getY()};
            }
        }

        for (Character c : selectionManager.getSelected())
        {
            if (c == primary)
            {
                continue;
            }

            int[] diff = calculateLocationDifference(inPOH, c, primaryCoordinates);
            KeyFrame keyFrame = createMovementKeyFrame(c, worldView, createNew, localPoint, diff);

            characters = ArrayUtils.add(characters, c);
            frames = ArrayUtils.add(frames, new KeyFrame[]{keyFrame});
        }

        TimeSheetPanel timeSheetPanel = plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel();
        timeSheetPanel.runKeyFrameAddActions(characters, frames);

        Programmer programmer = plugin.getCreatorsPanel().getToolBox().getProgrammer();
        for (Character c : characters)
        {
            c.setInScene(true);
            c.setActive(true, true, true, clientThread);
            programmer.register3DChanges(c);
        }
    }

    public KeyFrame createMovementKeyFrame(Character character, WorldView worldView, boolean createNew, LocalPoint localPoint, int[] diff)
    {
        boolean poh = MovementManager.useLocalLocations(worldView);
        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        KeyFrame newKeyFrame;
        LocalPoint lp = LocalPoint.fromScene(localPoint.getSceneX() + diff[0], localPoint.getSceneY() + diff[1], worldView);

        if (createNew || kf == null)
        {
            int x = lp.getSceneX();
            int y = lp.getSceneY();
            if (!poh)
            {
                WorldPoint wp = WorldPoint.fromLocalInstance(client, lp, worldView.getPlane());
                x = wp.getX();
                y = wp.getY();
            }

            int[][] path = new int[][]{new int[]{x, y}};
            newKeyFrame = new MovementKeyFrame(
                    plugin.getCurrentTick(),
                    worldView.getPlane(),
                    poh,
                    path,
                    0,
                    0,
                    false,
                    1,
                    OrientationKeyFrame.TURN_RATE);
        }
        else
        {
            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            int[][] path = movementManager.addProgramStep(keyFrame, worldView, lp);
            newKeyFrame = new MovementKeyFrame(
                    keyFrame.getTick(),
                    worldView.getPlane(),
                    poh,
                    path,
                    0,
                    0,
                    keyFrame.isLoop(),
                    keyFrame.getSpeed(),
                    keyFrame.getTurnRate());
        }

        return newKeyFrame;
    }

    public final HotkeyListener removeProgramStepListener = new HotkeyListener(() -> config.removeProgramStepHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            removeProgramSteps(false);
        }
    };

    public void removeProgramSteps(boolean clearAll)
    {
        Programmer programmer = plugin.getCreatorsPanel().getToolBox().getProgrammer();
        Character[] characters = new Character[0];
        KeyFrame[][] frames = new KeyFrame[0][0];

        for (Character c : selectionManager.getSelected())
        {
            KeyFrame kf = c.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
            if (kf == null)
            {
                continue;
            }

            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            int[][] path = keyFrame.getPath();

            if (path.length == 0)
            {
                continue;
            }

            if (clearAll)
            {
                path = new int[0][2];
            }
            else
            {
                path = ArrayUtils.remove(path, path.length - 1);
            }

            MovementKeyFrame newKeyFrame = new MovementKeyFrame(
                    keyFrame.getTick(),
                    keyFrame.getPlane(),
                    keyFrame.isPoh(),
                    path,
                    0,
                    0,
                    keyFrame.isLoop(),
                    keyFrame.getSpeed(),
                    keyFrame.getTurnRate());

            characters = ArrayUtils.add(characters, c);
            frames = ArrayUtils.add(frames, new KeyFrame[]{newKeyFrame});
        }

        TimeSheetPanel timeSheetPanel = plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel();
        timeSheetPanel.runKeyFrameAddActions(characters, frames);

        for (Character c : characters)
        {
            programmer.register3DChanges(c);
        }
    }

    public final HotkeyListener clearProgramStepListener = new HotkeyListener(() -> config.clearProgramStepHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            removeProgramSteps(true);
        }
    };

    public final HotkeyListener addOrientationStartListener = new HotkeyListener(() -> config.orientationStart()) {
        @Override
        public void hotkeyPressed()
        {
            clientThread.invokeLater(() -> plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().onOrientationKeyPressed(OrientationHotkeyMode.SET_START));
        }
    };

    public final HotkeyListener addOrientationGoalListener = new HotkeyListener(() -> config.orientationEnd()) {
        @Override
        public void hotkeyPressed()
        {
            clientThread.invokeLater(() -> plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().onOrientationKeyPressed(OrientationHotkeyMode.SET_GOAL));
        }
    };

    public final HotkeyListener playPauseListener = new HotkeyListener(() -> config.playPauseHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().getToolBox().getProgrammer().togglePlay();
        }
    };

    public final HotkeyListener resetTimelineListener = new HotkeyListener(() -> config.resetTimelineHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            ToolBoxFrame toolBox = plugin.getCreatorsPanel().getToolBox();
            toolBox.getTimeSheetPanel().setCurrentTime(0, false);
        }
    };

    public final HotkeyListener skipForwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK))
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().onAttributeSkipForward();
        }
    };

    public final HotkeyListener skipBackwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK))
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().onAttributeSkipPrevious();
        }
    };

    public final HotkeyListener skipSubForwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK))
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().inchTimeline(0.1);
        }
    };

    public final HotkeyListener skipSubBackwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK))
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().inchTimeline(-0.1);
        }
    };

    public final HotkeyListener saveListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK))
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().quickSaveToFile();
        }
    };

    public final HotkeyListener openListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK))
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().openLoadSetupDialog(true);
        }
    };

    public final HotkeyListener undoListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK))
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().undo();
        }
    };

    public final HotkeyListener redoListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK))
    {
        @Override
        public void hotkeyPressed()
        {
            plugin.getCreatorsPanel().getToolBox().getTimeSheetPanel().redo();
        }
    };
}
