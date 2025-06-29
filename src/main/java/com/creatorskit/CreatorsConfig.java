package com.creatorskit;

import com.creatorskit.models.exporters.ExportFileFormat;
import com.creatorskit.programming.MovementType;
import net.runelite.client.config.*;

import java.awt.event.KeyEvent;

@ConfigGroup("creatorssuite")
public interface CreatorsConfig extends Config
{
	@ConfigSection(
			name = "Scene",
			description = "Settings for setting up your scene",
			position = 0
	)
	String sceneSettings = "sceneSettings";

	@ConfigItem(
			keyName = "enableRightClick",
			name = "Enable Right-Click Options",
			description = "Enables Right-Click menu options for on Objects, NPCs, and Players",
			section = sceneSettings,
			position = 0
	)
	default boolean rightClick()
	{
		return true;
	}

	@ConfigItem(
			keyName = "enableCtrlHotkeys",
			name = "Enable Ctrl Hotkeys",
			description = "Enables various additional menu options when holding Ctrl",
			section = sceneSettings,
			position = 1
	)
	default boolean enableCtrlHotkeys()
	{
		return false;
	}

	@ConfigItem(
			keyName = "toggleAutoSetup",
			name = "Enable Auto-Setup",
			description = "Automatically loads the saved Setup from the file path below" +
				"<br>Please note that enabling this feature will slow down client start-up",
			section = sceneSettings,
			position = 6
	)
	default boolean autoSetup()
	{
		return false;
	}

	@ConfigItem(
			keyName = "setupPath",
			name = "Auto-Setup Path",
			description = "Enter the file path of a previously saved setup to automatically load on client start-up",
			section = sceneSettings,
			position = 7
	)
	default String setupPath()
	{
		return "";
	}

	@ConfigItem(
			keyName = "quickSpawn",
			name = "Spawn Hotkey",
			description = "Hotkey to toggle the spawn or despawn state of the selected object",
			section = sceneSettings,
			position = 8
	)
	default Keybind quickSpawnHotkey()
	{
		return new Keybind(KeyEvent.VK_INSERT, 0);
	}

	@ConfigItem(
			keyName = "quickLocation",
			name = "Set Relocate Hotkey",
			description = "Hotkey to set the selected object to the mouse location",
			section = sceneSettings,
			position = 9
	)
	default Keybind quickLocationHotkey()
	{
		return new Keybind(KeyEvent.VK_HOME, 0);
	}

	@ConfigItem(
			keyName = "quickDuplicate",
			name = "Duplicate Hotkey",
			description = "Hotkey to duplicate the selected object",
			section = sceneSettings,
			position = 10
	)
	default Keybind quickDuplicateHotkey()
	{
		return new Keybind(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "quickRotateCW",
			name = "Rotate CW Hotkey",
			description = "Hotkey to rotate the selected object clockwise",
			section = sceneSettings,
			position = 11
	)
	default Keybind quickRotateCWHotkey()
	{
		return new Keybind(KeyEvent.VK_DELETE, 0);
	}

	@ConfigItem(
			keyName = "quickRotateCCW",
			name = "Rotate CCW Hotkey",
			description = "Hotkey to rotate the selected object counter-clockwise",
			section = sceneSettings,
			position = 12
	)
	default Keybind quickRotateCCWHotkey()
	{
		return new Keybind(KeyEvent.VK_END, 0);
	}

	@ConfigItem(
			keyName = "rotateDegrees",
			name = "Rotate Degrees",
			description = "Determines how much the Rotate Hotkeys rotate the Object by",
			section = sceneSettings,
			position = 13
	)
	default Rotation rotateDegrees()
	{
		return Rotation._90_DEGREES;
	}

	@ConfigSection(
			name = "Overlays",
			description = "Settings for enabling/disabling overlays",
			position = 1
	)
	String overlaySettings = "overlaySettings";

	@ConfigItem(
			keyName = "toggleOverlays",
			name = "Toggle Overlays",
			description = "Hotkey to toggle all overlays. Unset to disable",
			section = overlaySettings,
			position = 0
	)
	default Keybind toggleOverlaysHotkey()
	{
		return new Keybind(KeyEvent.VK_PAGE_DOWN, 0);
	}

	@ConfigItem(
			keyName = "myObjectOverlay",
			name = "My Object Overlay",
			description = "Enables an overlay for objects introduced via this plugin",
			section = overlaySettings,
			position = 1
	)
	default boolean myObjectOverlay()
	{
		return true;
	}

	@ConfigItem(
			keyName = "pathOverlay",
			name = "Object Path Overlay",
			description = "Enables an overlay for the pathing of programmed objects introduced via this plugin",
			section = overlaySettings,
			position = 2
	)
	default boolean pathOverlay()
	{
		return true;
	}

	@ConfigItem(
			keyName = "gameObjectOverlay",
			name = "Game Object Overlay",
			description = "Enables an overlay for GameObjects",
			section = overlaySettings,
			position = 3
	)
	default boolean gameObjectOverlay()
	{
		return false;
	}

	@ConfigItem(
			keyName = "playerOverlay",
			name = "Player Overlay",
			description = "Enables an overlay for Players",
			section = overlaySettings,
			position = 4
	)
	default boolean playerOverlay()
	{
		return false;
	}

	@ConfigItem(
			keyName = "npcOverlay",
			name = "NPC Overlay",
			description = "Enables an overlay for NPCs",
			section = overlaySettings,
			position = 5
	)
	default boolean npcOverlay()
	{
		return false;
	}

	@ConfigItem(
			keyName = "groundObjectOverlay",
			name = "Ground Object Overlay",
			description = "Enables an overlay for GroundObjects",
			section = overlaySettings,
			position = 6
	)
	default boolean groundObjectOverlay()
	{
		return false;
	}

	@ConfigItem(
			keyName = "wallObjectOverlay",
			name = "Wall Object Overlay",
			description = "Enables an overlay for TileObjects",
			section = overlaySettings,
			position = 7
	)
	default boolean wallObjectOverlay()
	{
		return false;
	}

	@ConfigItem(
			keyName = "decorativeObjectOverlay",
			name = "Decorative Object Overlay",
			description = "Enables an overlay for DecorativeObjects",
			section = overlaySettings,
			position = 8
	)
	default boolean decorativeObjectOverlay()
	{
		return false;
	}

	@ConfigItem(
			keyName = "projectileOverlay",
			name = "Projectile Overlay",
			description = "Enables an overlay for Projectiles",
			section = overlaySettings,
			position = 9
	)
	default boolean projectileOverlay()
	{
		return false;
	}

	@ConfigSection(
			name = "Programmer",
			description = "Settings for quickly programming the selected object",
			position = 2
	)
	String programmer = "programmer";

	@ConfigItem(
			keyName = "pathAlgorithm",
			name = "Path Algorithm",
			description = "The Object pathing algorithm by which to modify Movement Keyframes",
			section = programmer,
			position = 0
	)
	default MovementType movementAlgorithm()
	{
		return MovementType.NORMAL;
	}

	@ConfigItem(
			keyName = "addStep",
			name = "Add Program Step",
			description = "Hotkey to add the hovered location to the selected object's program",
			section = programmer,
			position = 1
	)
	default Keybind addProgramStepHotkey()
	{
		return new Keybind(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "deleteStep",
			name = "Remove Program Step",
			description = "Hotkey to remove the last step from the selected object's program",
			section = programmer,
			position = 2
	)
	default Keybind removeProgramStepHotkey()
	{
		return new Keybind(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "clearSteps",
			name = "Clear Program Steps",
			description = "Hotkey to clear all steps from the selected object's program",
			section = programmer,
			position = 3
	)
	default Keybind clearProgramStepHotkey()
	{
		return new Keybind(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "playPauseToggle",
			name = "Play/Pause Toggle",
			description = "Hotkey to play/pause selected object",
			section = programmer,
			position = 4
	)
	default Keybind playPauseHotkey()
	{
		return new Keybind(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "resetTimeline",
			name = "Reset Timeline",
			description = "Hotkey to set reset the Timeline to 0",
			section = programmer,
			position = 5
	)
	default Keybind resetTimelineHotkey()
	{
		return new Keybind(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigSection(
			name = "Transmogrification",
			description = "Settings for replacing your player character with a saved Custom Model",
			position = 3
	)
	String transmogrification = "Transmogrification";

	@ConfigItem(
			keyName = "enableTransmog",
			name = "Enable Transmogrification",
			description = "Allow your character to be transmogrified into a chosen Custom Model",
			section = transmogrification,
			position = 0
	)
	default boolean enableTransmog()
	{
		return true;
	}

	@ConfigItem(
			keyName = "enableAutoTransmog",
			name = "Enable Auto-Transmog",
			description = "Automatically loads the Transmog from the file path below" +
					"<br>Please note that enabling this feature will slow down client start-up",
			section = transmogrification,
			position = 1
	)
	default boolean autoTransmog()
	{
		return false;
	}

	@ConfigItem(
			keyName = "transmogPath",
			name = "Auto-Transmog Path",
			description = "Enter the file path of a previously saved Transmog to automatically load on client start-up",
			section = transmogrification,
			position = 2
	)
	default String transmogPath()
	{
		return "";
	}

	@ConfigSection(
			name = "Oculus Orb",
			description = "Settings for enabling and modifying Oculus Orb mode",
			position = 4
	)
	String cameraSettings = "cameraSettings";

	@ConfigItem(
			keyName = "orbToggle",
			name = "Toggle Oculus Orb Mode",
			description = "Hotkey to toggle Oculus Orb mode",
			section = cameraSettings,
			position = 0
	)
	default Keybind toggleOrbHotkey()
	{
		return new Keybind(KeyEvent.VK_PAGE_UP, 0);
	}

	@ConfigItem(
			keyName = "orbSpeed",
			name = "Default Orb Speed",
			description = "Set the default normal speed of the Oculus Orb when the client starts",
			section = cameraSettings,
			position = 1
	)
	default int orbSpeed()
	{
		return 36;
	}

	@ConfigItem(
			keyName = "orbPreset1",
			name = "Orb Speed Hotkey 1",
			description = "Hotkey to quickly set Oculus Orb speed to Preset 1",
			section = cameraSettings,
			position = 3
	)
	default Keybind orbSpeedHotkey1()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "orbPreset2",
			name = "Orb Speed Hotkey 2",
			description = "Hotkey to quickly set Oculus Orb speed to Preset 2",
			section = cameraSettings,
			position = 4
	)
	default Keybind orbSpeedHotkey2()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "orbPreset3",
			name = "Orb Speed Hotkey 3",
			description = "Hotkey to quickly set Oculus Orb speed to Preset 3",
			section = cameraSettings,
			position = 5
	)
	default Keybind orbSpeedHotkey3()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "orbSpeed1",
			name = "Orb Speed Preset 1",
			description = "Set the Orb Speed for Hotkey 1",
			section = cameraSettings,
			position = 6
	)
	default int speedHotkey1()
	{
		return 5;
	}

	@ConfigItem(
			keyName = "orbSpeed2",
			name = "Orb Speed Preset 2",
			description = "Set the Orb Speed for Hotkey 2",
			section = cameraSettings,
			position = 7
	)
	default int speedHotkey2()
	{
		return 9;
	}

	@ConfigItem(
			keyName = "orbSpeed3",
			name = "Orb Speed Preset 3",
			description = "Set the Orb Speed for Hotkey 3",
			section = cameraSettings,
			position = 8
	)
	default int speedHotkey3()
	{
		return 36;
	}

	@ConfigItem(
			keyName = "rotateLeft",
			name = "AutoRotate Left",
			description = "Hotkey to toggle automatic camera rotation to the left",
			section = cameraSettings,
			position = 9
	)
	default Keybind rotateLeftHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "rotateRight",
			name = "AutoRotate Right",
			description = "Hotkey to toggle automatic camera rotation to the right",
			section = cameraSettings,
			position = 10
	)
	default Keybind rotateRightHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "rotateUp",
			name = "AutoRotate Up",
			description = "Hotkey to toggle automatic camera rotation up",
			section = cameraSettings,
			position = 11
	)
	default Keybind rotateUpHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "rotateDown",
			name = "AutoRotate Down",
			description = "Hotkey to toggle automatic camera rotation down",
			section = cameraSettings,
			position = 12
	)
	default Keybind rotateDownHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "rotateHorizontalSpeed",
			name = "Rotate Horizontal Speed",
			description = "Set the horizontal automatic camera rotation speed",
			section = cameraSettings,
			position = 13
	)
	default int rotateHorizontalSpeed()
	{
		return 3;
	}

	@ConfigItem(
			keyName = "rotateVerticalSpeed",
			name = "Rotate Vertical Speed",
			description = "Set the vertical automatic camera rotation speed",
			section = cameraSettings,
			position = 14
	)
	default int rotateVerticalSpeed()
	{
		return 3;
	}

	@ConfigSection(
			name = "Model Exporter",
			description = "Settings for changing how Models are Exported to 3D model formats",
			position = 5
	)
	String modelExporter = "modelExporter";

	@ConfigItem(
			keyName = "exportFileFormat",
			name = "Export File Format",
			description = "Pick the file format for exported Models" +
					"<br>Blender json files are intended to be used with the associated Runescape Import addon" +
					"<br>See ScreteMonge for more details",
			section = modelExporter,
			position = 0
	)
	default ExportFileFormat exportFileFormat()
	{
		return ExportFileFormat.BLENDER;
	}

	@ConfigItem(
			keyName = "vertexColours",
			name = "Vertex Colours",
			description = "Makes Blender Model .json files export with Vertex Colours instead of Face Colours",
			section = modelExporter,
			position = 1
	)
	default boolean vertexColours()
	{
		return false;
	}

	@ConfigItem(
			keyName = "exportTPose",
			name = "Export Neutral Pose",
			description = "Puts the Model into its neutral pose (disables animations) for exporting",
			section = modelExporter,
			position = 2
	)
	default boolean exportTPose()
	{
		return false;
	}
}
