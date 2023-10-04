package com.creatorskit;

import net.runelite.client.config.*;

import java.awt.event.KeyEvent;

@ConfigGroup("creatorssuite")
public interface CreatorsConfig extends Config
{
	enum TransmogAnimation
	{
		PLAYER,
		CONFIG,
		NONE
	}

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
			keyName = "orbSpeed",
			name = "Orb Speed",
			description = "Set the normal speed of the Oculus Orb. Unset to disable",
			section = cameraSettings,
			position = 2
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
			position = 3
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
			position = 4
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
			position = 5
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
			position = 6
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
			position = 7
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
			position = 8
	)
	default int rotateVerticalSpeed()
	{
		return 3;
	}

	@ConfigSection(
			name = "Scene",
			description = "Settings for setting up your scene",
			position = 9
	)
	String sceneSettings = "sceneSettings";

	@ConfigItem(
			keyName = "enableRightClick",
			name = "Enable Right-Click",
			description = "Enables Right-Click menu options to Store & Anvil objects/NPCs, and selecting spawned Objects",
			section = sceneSettings,
			position = 10
	)
	default boolean rightClick()
	{
		return true;
	}

	@ConfigItem(
			keyName = "toggleAutoSetup",
			name = "Enable Auto-Setup",
			description = "Automatically loads the saved setup from the file path below" +
				"<br>Please note that enabling this feature will slow down client start-up",
			section = sceneSettings,
			position = 11
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
			position = 12
	)
	default String setupPath()
	{
		return "";
	}

	@ConfigItem(
			keyName = "quickSpawn",
			name = "Quick Spawn",
			description = "Hotkey to toggle the spawn or despawn state of the selected object",
			section = sceneSettings,
			position = 13
	)
	default Keybind quickSpawnHotkey()
	{
		return new Keybind(KeyEvent.VK_INSERT, 0);
	}

	@ConfigItem(
			keyName = "quickLocation",
			name = "Quick Location",
			description = "Hotkey to set the selected object to the mouse location",
			section = sceneSettings,
			position = 14
	)
	default Keybind quickLocationHotkey()
	{
		return new Keybind(KeyEvent.VK_HOME, 0);
	}

	@ConfigItem(
			keyName = "quickRotateCW",
			name = "Quick Rotate CW",
			description = "Hotkey to rotate the selected object by 90 degrees clockwise",
			section = sceneSettings,
			position = 15
	)
	default Keybind quickRotateCWHotkey()
	{
		return new Keybind(KeyEvent.VK_DELETE, 0);
	}

	@ConfigItem(
			keyName = "quickRotateCCW",
			name = "Quick Rotate CCW",
			description = "Hotkey to rotate the selected object by 90 degrees counter-clockwise",
			section = sceneSettings,
			position = 16
	)
	default Keybind quickRotateCCWHotkey()
	{
		return new Keybind(KeyEvent.VK_END, 0);
	}

	@ConfigSection(
			name = "Overlays",
			description = "Settings for enabling/disabling overlays",
			position = 17
	)
	String overlaySettings = "overlaySettings";

	@ConfigItem(
			keyName = "toggleOverlays",
			name = "Toggle Overlays",
			description = "Hotkey to toggle all overlays. Unset to disable",
			section = overlaySettings,
			position = 18
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
			position = 19
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
			position = 20
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
			position = 21
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
			position = 22
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
			position = 23
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
			position = 24
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
			position = 25
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
			position = 26
	)
	default boolean decorativeObjectOverlay()
	{
		return false;
	}

	@ConfigSection(
			name = "Programmer",
			description = "Settings for quickly programming the selected object",
			position = 27
	)
	String programmer = "programmer";

	@ConfigItem(
			keyName = "addStep",
			name = "Add Program Step",
			description = "Hotkey to add the hovered location to the selected object's program",
			section = programmer,
			position = 28
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
			position = 29
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
			position = 30
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
			position = 31
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
			position = 32
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
			position = 33
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
			position = 34
	)
	default Keybind resetAllHotkey()
	{
		return new Keybind(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK);
	}

	@ConfigSection(
			name = "Transmogification",
			description = "Settings for replacing your player character with a saved Custom Model",
			position = 35
	)
	String transmogrification = "transmogrification";

	@ConfigItem(
			keyName = "enableTransmog",
			name = "Enable Transmogrification",
			description = "Allow your character to be transmogrified into a chosen Custom Model",
			section = transmogrification,
			position = 36
	)
	default boolean enableTransmog()
	{
		return true;
	}

	@ConfigItem(
			keyName = "transmogAnimation",
			name = "Animation Type",
			description = "Gives your transmog animations based on 1) your player animations, 2) the animations below, or 3) no animation",
			section = transmogrification,
			position = 37
	)
	default TransmogAnimation transmogAnimations()
	{
		return TransmogAnimation.PLAYER;
	}

	@ConfigItem(
			keyName = "transmogPose",
			name = "Pose Animation",
			description = "Sets the pose animation of your transmog if the Config animation type is chosen",
			section = transmogrification,
			position = 38
	)
	default int transmogPose()
	{
		return 808;
	}

	@ConfigItem(
			keyName = "transmogWalk",
			name = "Walk Animation",
			description = "Sets the walk animation of your transmog if the Config animation type is chosen",
			section = transmogrification,
			position = 39
	)
	default int transmogWalk()
	{
		return 819;
	}

	@ConfigItem(
			keyName = "transmogAction",
			name = "Action Animation",
			description = "Sets the action animation of your transmog if the Config animation type is chosen",
			section = transmogrification,
			position = 40
	)
	default int transmogAction()
	{
		return 866;
	}

	@ConfigItem(
			keyName = "transmogRadius",
			name = "Radius",
			description = "Sets the radius of your transmog. 60 is best for an object the size of 1 tile",
			section = transmogrification,
			position = 41
	)
	default int transmogRadius()
	{
		return 60;
	}

	@ConfigItem(
			keyName = "transmogRightClick",
			name = "Enable Transmog Right-Click",
			description = "Provides a right-click option to Transmog on any object or NPC",
			section = transmogrification,
			position = 42
	)
	default boolean transmogRightClick()
	{
		return false;
	}
}
