package com.creatorskit;

import net.runelite.client.config.*;

import java.awt.event.KeyEvent;

@ConfigGroup("creatorssuite")
public interface CreatorsConfig extends Config
{
	@ConfigSection(
			name = "Oculus Orb",
			description = "Settings for enabling and modifying Oculus Orb mode",
			position = 0
	)
	String cameraSettings = "cameraSettings";

	@ConfigItem(
			keyName = "orbToggle",
			name = "Toggle Oculus Orb Mode",
			description = "Hotkey to toggle Oculus Orb mode",
			section = cameraSettings,
			position = 1
	)
	default Keybind toggleOrbHotkey()
	{
		return new Keybind(KeyEvent.VK_PAGE_UP, 0);
	}

	@ConfigItem(
			keyName = "setOrbSpeed",
			name = "Set Orb Speed",
			description = "Hotkey to quickly set Oculus Orb speed",
			section = cameraSettings,
			position = 2
	)
	default Keybind setOrbSpeedHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "orbSpeed",
			name = "Orb Speed",
			description = "Set the normal speed of the Oculus Orb. Unset to disable",
			section = cameraSettings,
			position = 3
	)
	default int orbSpeed()
	{
		return 36;
	}

	@ConfigItem(
			keyName = "rotateLeft",
			name = "AutoRotate Left",
			description = "Hotkey to toggle automatic camera rotation to the left",
			section = cameraSettings,
			position = 4
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
			position = 5
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
			position = 6
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
			position = 7
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
			position = 8
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
			position = 9
	)
	default int rotateVerticalSpeed()
	{
		return 3;
	}

	@ConfigSection(
			name = "Scene",
			description = "Settings for setting up your scene",
			position = 10
	)
	String sceneSettings = "sceneSettings";

	@ConfigItem(
			keyName = "enableRightClick",
			name = "Enable Right-Click",
			description = "Enables Right-Click menu options to Store & Anvil objects/NPCs, and selecting spawned Objects",
			section = sceneSettings,
			position = 11
	)
	default boolean rightClick()
	{
		return true;
	}

	@ConfigItem(
			keyName = "toggleAutoSetup",
			name = "Enable Auto-Setup",
			description = "Automatically loads the saved Setup from the file path below" +
				"<br>Please note that enabling this feature will slow down client start-up",
			section = sceneSettings,
			position = 12
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
			position = 13
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
			position = 14
	)
	default Keybind quickSpawnHotkey()
	{
		return new Keybind(KeyEvent.VK_INSERT, 0);
	}

	@ConfigItem(
			keyName = "quickLocation",
			name = "Set Location Hotkey",
			description = "Hotkey to set the selected object to the mouse location",
			section = sceneSettings,
			position = 15
	)
	default Keybind quickLocationHotkey()
	{
		return new Keybind(KeyEvent.VK_HOME, 0);
	}

	@ConfigItem(
			keyName = "quickRotateCW",
			name = "Rotate CW Hotkey",
			description = "Hotkey to rotate the selected object clockwise",
			section = sceneSettings,
			position = 16
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
			position = 17
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
			position = 18
	)
	default Rotation rotateDegrees()
	{
		return Rotation._90_DEGREES;
	}

	@ConfigSection(
			name = "Overlays",
			description = "Settings for enabling/disabling overlays",
			position = 19
	)
	String overlaySettings = "overlaySettings";

	@ConfigItem(
			keyName = "toggleOverlays",
			name = "Toggle Overlays",
			description = "Hotkey to toggle all overlays. Unset to disable",
			section = overlaySettings,
			position = 20
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
			position = 21
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
			position = 22
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
			position = 23
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
			position = 24
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
			position = 25
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
			position = 26
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
			position = 27
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
			position = 28
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
			position = 29
	)
	default boolean projectileOverlay()
	{
		return false;
	}

	@ConfigSection(
			name = "Programmer",
			description = "Settings for quickly programming the selected object",
			position = 30
	)
	String programmer = "programmer";

	@ConfigItem(
			keyName = "addStep",
			name = "Add Program Step",
			description = "Hotkey to add the hovered location to the selected object's program",
			section = programmer,
			position = 31
	)
	default Keybind addProgramStepHotkey()
	{
		return new Keybind(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "removeStep",
			name = "Remove Program Step",
			description = "Hotkey to remove the hovered location from the selected object's program",
			section = programmer,
			position = 32
	)
	default Keybind removeProgramStepHotkey()
	{
		return new Keybind(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "clearSteps",
			name = "Clear Program Steps",
			description = "Hotkey to clear all steps from the selected object's program",
			section = programmer,
			position = 33
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
			position = 34
	)
	default Keybind playPauseHotkey()
	{
		return new Keybind(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "playPauseAllToggle",
			name = "Play/Pause All",
			description = "Hotkey to play/pause all programs",
			section = programmer,
			position = 35
	)
	default Keybind playPauseAllHotkey()
	{
		return new Keybind(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "resetLocations",
			name = "Reset Locations",
			description = "Hotkey to set selected object to its start location",
			section = programmer,
			position = 36
	)
	default Keybind resetHotkey()
	{
		return new Keybind(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "resetAllLocations",
			name = "Reset All Locations",
			description = "Hotkey to set all objects to their start location",
			section = programmer,
			position = 37
	)
	default Keybind resetAllHotkey()
	{
		return new Keybind(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigSection(
			name = "transmogrification",
			description = "Settings for replacing your player character with a saved Custom Model",
			position = 38
	)
	String transmogrification = "Transmogrification";

	@ConfigItem(
			keyName = "enableTransmog",
			name = "Enable Transmogrification",
			description = "Allow your character to be transmogrified into a chosen Custom Model",
			section = transmogrification,
			position = 39
	)
	default boolean enableTransmog()
	{
		return true;
	}

	@ConfigItem(
			keyName = "transmogRightClick",
			name = "Enable Transmog Right-Click",
			description = "Provides a right-click option to Transmog on any object or NPC",
			section = transmogrification,
			position = 40
	)
	default boolean transmogRightClick()
	{
		return false;
	}

	@ConfigItem(
			keyName = "enableAutoTransmog",
			name = "Enable Auto-Transmog",
			description = "Automatically loads the Transmog from the file path below" +
					"<br>Please note that enabling this feature will slow down client start-up",
			section = transmogrification,
			position = 41
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
			position = 42
	)
	default String transmogPath()
	{
		return "";
	}
}
