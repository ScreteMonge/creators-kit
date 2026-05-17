package com.creatorskit;

import com.creatorskit.models.*;
import com.creatorskit.programming.*;
import com.creatorskit.programming.orientation.OrientationHotkeyMode;
import com.creatorskit.saves.TransmogLoadOption;
import com.creatorskit.selection.SelectionManager;
import com.creatorskit.swing.*;
import com.creatorskit.swing.anvil.ComplexPanel;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Menu;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Slf4j
@Getter
@Setter
@PluginDescriptor(
		name = "Creator's Kit (DEV)",
		description = "A suite of tools for creators",
		tags = {"tool", "creator", "content", "kit", "camera", "immersion", "export"}
)
public class CreatorsPlugin extends Plugin implements MouseListener {
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private CreatorsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CreatorsOverlay overlay;

	@Inject
	private TextOverlay textOverlay;

	@Inject
	private OverheadOverlay overheadOverlay;

	@Inject
	private HealthOverlay healthOverlay;

	@Inject
	private HitsplatOverlay hitsplatOverlay;

	@Inject
	private BarOverlay barOverlay;

	@Inject
	private ScreenFadeOverlay screenFadeOverlay;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ModelGetter modelGetter;

	@Inject
	private PathFinder pathFinder;

	@Inject
	private DataFinder dataFinder;

	@Inject
	private Gson gson;

	@Inject
	private SelectionManager selectionManager;

	private CreatorsPanel creatorsPanel;
	private NavigationButton navigationButton;
	private boolean overlaysActive = false;
	private final ArrayList<Character> characters = new ArrayList<>();
	private final ArrayList<CustomModel> storedModels = new ArrayList<>();
	private Character hoveredCharacter;

	/**
	 * Character the camera is currently locked onto, or null if no lock is engaged.
	 * Only one Character can be locked at a time -- selecting a different one via
	 * the manager-tree menu or a sidebar checkbox switches the lock to that one.
	 * Cleared automatically if the locked Character's CKObject becomes stale.
	 */
	private Character cameraLockedCharacter;


	/**
	 * Captured camera mode at lock engage-time so the camera restores to whatever it
	 * was in (0 = normal, 1 = free) when the lock disengages. Without this the user
	 * would be stuck in free-camera mode after unlocking.
	 */
	private int cameraLockPreviousMode = -1;

	/**
	 * Captured Oculus Orb state at lock engage-time. Detached Camera plugin sets the
	 * orb on; CK's lock needs free-camera mode and the two states conflict at the
	 * engine level (the orb hijacks focal point updates), so the lock disables the orb
	 * while engaged. Restored on release so re-enabling the lock followed by
	 * disabling it leaves Detached Camera in the state it was in.
	 */
	private int cameraLockPreviousOculusOrbState = -1;

	public Character getSelectedCharacter()
	{
		return selectionManager.getPrimary();
	}

	public void setSelectedCharacter(Character selected)
	{
		if (selected == null)
		{
			selectionManager.clear();
		}
		else
		{
			selectionManager.select(selected);
		}
	}

	/**
	 * Returns the Character the camera is currently locked onto, or null if no lock
	 * is engaged. UI components (manager-tree menu, sidebar checkbox) read this to
	 * compute their checked state.
	 */
	public Character getCameraLockedCharacter()
	{
		return cameraLockedCharacter;
	}

	/**
	 * Engages, switches, or releases the camera lock. Passing the currently-locked
	 * Character releases it; passing a different Character switches the lock; passing
	 * null also releases it.
	 *
	 * <p>Engaging captures the (camera focal point - Character position) offset and
	 * switches the camera into free-camera mode (the mode that exposes
	 * setCameraFocalPointX/Y/Z). The per-tick focal-point update in
	 * {@link #updateCameraLock()} (called from the main onClientTick) keeps the camera
	 * glued to the Character through movement keyframes and sub-tile offsets. Disengaging restores
	 * whatever camera mode was active before the lock so the user isn't stranded in
	 * free-camera mode.
	 *
	 * <p>Also syncs the UI: the per-Character renderFix-style sidebar checkbox and
	 * the manager-tree checkbox-menu-item for the newly-locked Character flip on,
	 * and the previously-locked Character's UI flips off.
	 */
	public void setCameraLockedCharacter(Character target)
	{
		Character previous = cameraLockedCharacter;
		if (previous == target)
		{
			target = null; // re-press releases
		}

		if (previous != null && previous != target && previous.getCameraLockCheckBox() != null
				&& previous.getCameraLockCheckBox().isSelected())
		{
			previous.getCameraLockCheckBox().setSelected(false);
		}

		if (target == null)
		{
			cameraLockedCharacter = null;
			final int restoreMode = cameraLockPreviousMode;
			final int restoreOrb = cameraLockPreviousOculusOrbState;
			cameraLockPreviousMode = -1;
			cameraLockPreviousOculusOrbState = -1;
			if (restoreMode != -1 || restoreOrb != -1)
			{
				clientThread.invokeLater(() ->
				{
					if (restoreMode != -1)
					{
						client.setCameraMode(restoreMode);
					}
					if (restoreOrb != -1)
					{
						client.setOculusOrbState(restoreOrb);
					}
				});
			}
			return;
		}

		CKObject ck = target.getCkObject();
		if (ck == null)
		{
			return; // Character has no CKObject yet (e.g. before scene load) -- bail silently.
		}

		final Character finalTarget = target;
		clientThread.invokeLater(() ->
		{
			// Snapshot the pre-lock camera state on the client thread (the only place
			// it's safe to query). Detached Camera plugin sets the orb on; our free-
			// camera mode and the orb conflict at the engine level (the orb hijacks
			// focal point updates), so we disable the orb while locked.
			if (cameraLockPreviousMode == -1)
			{
				cameraLockPreviousMode = client.getCameraMode();
			}
			if (cameraLockPreviousOculusOrbState == -1)
			{
				cameraLockPreviousOculusOrbState = client.getOculusOrbState();
			}

			// Switch modes and publish the lock on the SAME client-thread tick so we
			// never publish a "locked" state in which the renderer would observe
			// (cameraLockedCharacter != null) AND (cameraMode != 1) -- that
			// combination caused setCameraFocalPointZ to throw IllegalArgumentException
			// on every ClientTick (50/s) until the JVM heap filled with stack traces.
			client.setOculusOrbState(0);
			client.setCameraMode(1);


			cameraLockedCharacter = finalTarget;
			if (finalTarget.getCameraLockCheckBox() != null
					&& !finalTarget.getCameraLockCheckBox().isSelected())
			{
				finalTarget.getCameraLockCheckBox().setSelected(true);
			}
		});
	}

	/**
	 * Per-tick camera-lock pump. Called from the main {@link #onClientTick(ClientTick)}
	 * handler -- can't be its own {@code @Subscribe} method because RuneLite's EventBus
	 * requires the method name to match the event type, and that name is already taken.
	 */
	private void updateCameraLock()
	{
		if (cameraLockedCharacter == null)
		{
			return;
		}
		// Defensive: setCameraFocalPointX/Y/Z requires free-camera mode (1) and throws
		// IllegalArgumentException otherwise. If the mode has been flipped from under
		// us (another plugin, a cutscene, or the user toggling Detached Camera back to
		// Oculus Orb), drop the lock cleanly rather than spam exceptions every tick.
		if (client.getCameraMode() != 1)
		{
			setCameraLockedCharacter(null);
			return;
		}
		CKObject ck = cameraLockedCharacter.getCkObject();
		if (ck == null)
		{
			// CKObject was rebuilt (render-fix toggle, model swap, etc.) -- drop the
			// lock rather than chase a stale ref. UI checkboxes follow via the setter.
			setCameraLockedCharacter(null);
			return;
		}
		try
		{
			// Axis mapping (confirmed by the diagnostic): camera focal-point coords are
			// (eastWest, height, northSouth) while CKObject coords are (eastWest,
			// northSouth, height). We swap Y/Z when feeding ck into the setters.
			//
			// Constant-fraction lag lerp -- emulates the way the OSRS player camera
			// trails the player. Each client tick the focal point closes a fixed
			// percentage of the remaining distance to the Character. During steady
			// motion the camera maintains a constant trail distance (because the
			// fraction closed each tick exactly balances the new distance the
			// Character has opened); the trail visually reads as "easing" because
			// the closer the camera gets to a stationary target the smaller each
			// step becomes (natural ease-out).
			//
			// Config "Camera lock responsiveness" maps directly to the lerp factor.
			// Higher percent = larger fraction closed per tick = tighter follow with
			// less trail. Lower percent = more trail.
			double lerpFactor = Math.max(1, Math.min(100, config.cameraLockEasing())) / 100.0;

			double targetX = ck.getX();
			double targetY = ck.getZ(); // height
			double targetZ = ck.getY(); // north-south
			double cx = client.getCameraFocalPointX();
			double cy = client.getCameraFocalPointY();
			double cz = client.getCameraFocalPointZ();
			client.setCameraFocalPointX(cx + (targetX - cx) * lerpFactor);
			client.setCameraFocalPointY(cy + (targetY - cy) * lerpFactor);
			client.setCameraFocalPointZ(cz + (targetZ - cz) * lerpFactor);
		}
		catch (IllegalArgumentException ex)
		{
			// Camera mode flipped during this tick -- back out and don't spam.
			setCameraLockedCharacter(null);
		}
	}

	/**
	 * Previous tick's screen-shake offset, stored so {@link #undoPreviousScreenShake}
	 * can subtract it from the focal point before the camera lock runs. Three
	 * doubles instead of a small array to avoid the per-tick allocation churn.
	 */
	private double prevShakeOffsetX = 0;
	private double prevShakeOffsetY = 0;
	private double prevShakeOffsetZ = 0;

	/**
	 * Subtracts last tick's screen-shake offset from the focal point. Restores the
	 * "true" focal so the camera-lock lerp doesn't chase the shaken position --
	 * otherwise the lock would smooth out the jitter into a slow drift toward
	 * the average shake position.
	 */
	private void undoPreviousScreenShake()
	{
		if (prevShakeOffsetX == 0 && prevShakeOffsetY == 0 && prevShakeOffsetZ == 0)
		{
			return;
		}
		if (client.getCameraMode() != 1)
		{
			prevShakeOffsetX = prevShakeOffsetY = prevShakeOffsetZ = 0;
			return;
		}
		try
		{
			client.setCameraFocalPointX(client.getCameraFocalPointX() - prevShakeOffsetX);
			client.setCameraFocalPointY(client.getCameraFocalPointY() - prevShakeOffsetY);
			client.setCameraFocalPointZ(client.getCameraFocalPointZ() - prevShakeOffsetZ);
		}
		catch (IllegalArgumentException ignored)
		{
			// Mode flipped between checks; nothing to undo.
		}
		finally
		{
			prevShakeOffsetX = prevShakeOffsetY = prevShakeOffsetZ = 0;
		}
	}

	/**
	 * Computes the current frame's shake offset (multi-octave noise, camera-relative)
	 * and adds it to the focal point. Caches the offset so the next tick's
	 * {@link #undoPreviousScreenShake} can wind it back before the camera-lock
	 * lerp runs (otherwise the lerp would smooth the jitter into a slow drift).
	 *
	 * <p>Silent no-op when the camera isn't in free-camera mode (setCameraFocalPoint
	 * throws otherwise) or no shake keyframe is active. The shake is camera-
	 * relative: "horizontal" amplitude always reads as screen left/right regardless
	 * of the camera's yaw, "vertical" as screen up/down. Achieved by rotating the
	 * world-space horizontal offset by the camera yaw so it lands on the camera's
	 * right-vector.
	 *
	 * <p>The noise is a 3-octave sum (base + 2.7x + 4.1x with phase offsets) -- a
	 * cheap deterministic approximation of Perlin noise that scrubbing and pausing
	 * sample correctly (no per-tick randomness).
	 */
	private void applyCurrentScreenShake()
	{
		if (client.getCameraMode() != 1)
		{
			return;
		}
		com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame active = findActiveScreenShake();
		if (active == null)
		{
			return;
		}
		double t = getCurrentTick() - active.getTick();
		if (t < 0 || t > active.getDurationTicks())
		{
			return;
		}

		// Multi-octave noise (cheap fake-Perlin): base sine + two harmonics at
		// non-integer multiples with offset phases. The result is chaotic enough
		// to read as vibration rather than a clean oscillation, but stays
		// deterministic so pause / scrub / rewind sample the same value at the
		// same tick.
		double phaseH = 2 * Math.PI * active.getFrequency() * t;
		double phaseV = phaseH + 1.7; // arbitrary phase offset so V doesn't track H

		double noiseH = 0.55 * Math.sin(phaseH)
				+ 0.30 * Math.sin(phaseH * 2.7 + 1.3)
				+ 0.15 * Math.sin(phaseH * 4.1 + 2.9);
		double noiseV = 0.55 * Math.sin(phaseV)
				+ 0.30 * Math.sin(phaseV * 2.3 + 0.5)
				+ 0.15 * Math.sin(phaseV * 3.7 + 2.1);

		// Camera-relative horizontal: rotate by yaw so horizontalAmp always reads
		// as screen left/right. OSRS yaw is in JAU (2048 units = full circle).
		// cos/sin of yaw give the camera's right-vector projected onto the horizontal
		// (X/Z focal-point) plane.
		double yawRadians = client.getCameraYaw() * (2 * Math.PI / 2048.0);
		double rightCos = Math.cos(yawRadians);
		double rightSin = Math.sin(yawRadians);

		double horizontalMagnitude = active.getAmplitudeHorizontal() * noiseH;
		double ox = horizontalMagnitude * rightCos;
		double oz = horizontalMagnitude * rightSin;
		// Vertical maps directly to focal-point Y (height) -- screen-vertical
		// matches world-vertical closely enough at typical OSRS camera pitches.
		double oy = active.getAmplitudeVertical() * noiseV;

		try
		{
			client.setCameraFocalPointX(client.getCameraFocalPointX() + ox);
			client.setCameraFocalPointY(client.getCameraFocalPointY() + oy);
			client.setCameraFocalPointZ(client.getCameraFocalPointZ() + oz);
			prevShakeOffsetX = ox;
			prevShakeOffsetY = oy;
			prevShakeOffsetZ = oz;
		}
		catch (IllegalArgumentException ignored)
		{
			prevShakeOffsetX = prevShakeOffsetY = prevShakeOffsetZ = 0;
		}
	}

	/**
	 * Iterates every Character's current SCREEN_SHAKE keyframe and returns the
	 * most-recently-started one that's still inside its duration window. Mirrors
	 * {@code ScreenFadeOverlay.findActiveFade}: lets multiple "scene controller"
	 * Characters define shakes, latest-started wins deterministically.
	 */
	private com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame findActiveScreenShake()
	{
		double currentTick = getCurrentTick();
		com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame best = null;
		double bestStart = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < characters.size(); i++)
		{
			Character c = characters.get(i);
			com.creatorskit.swing.timesheet.keyframe.KeyFrame kf =
					c.getCurrentKeyFrame(com.creatorskit.swing.timesheet.keyframe.KeyFrameType.SCREEN_SHAKE);
			if (!(kf instanceof com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame))
			{
				continue;
			}
			com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame sk =
					(com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame) kf;
			double elapsed = currentTick - sk.getTick();
			if (elapsed < 0 || elapsed > sk.getDurationTicks())
			{
				continue;
			}
			if (sk.getTick() > bestStart)
			{
				bestStart = sk.getTick();
				best = sk;
			}
		}
		return best;
	}

	private CKObject transmog;
	private CKObject previewObject;
	private Random random = new Random();
	private Model previewArrow;
	private CustomModel transmogModel;
	private final int GOLDEN_CHIN = 29757;
	private int savedRegion = -1;
	private int savedPlane = -1;
	private AutoRotate autoRotateYaw = AutoRotate.OFF;
	private AutoRotate autoRotatePitch = AutoRotate.OFF;
	private int oculusOrbSpeed = 36;
	private double clickX;
	private double clickY;
	private boolean mousePressed = false;
	private boolean autoSetupPathFound = true;
	private boolean autoTransmogFound = true;
	private boolean addProgramStep = false;
	private boolean addStepKeyHeld = false;
	private boolean scrolledDuringStepHold = false;
	private double currentStepSpeed = 1.0;
	private java.util.concurrent.ScheduledExecutorService setupVersionExecutor;
	private java.util.concurrent.ScheduledFuture<?> setupVersionTask;

	@Override
	protected void startUp() throws Exception
	{
		creatorsPanel = injector.getInstance(CreatorsPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/panelicon.png");
		navigationButton = NavigationButton.builder()
				.tooltip("Creator's Kit")
				.icon(icon)
				.priority(10)
				.panel(creatorsPanel)
				.build();

		eventBus.register(creatorsPanel.getToolBox().getProgrammer());
		eventBus.register(creatorsPanel.getToolBox().getTransmogPanel());

		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(overlay);
		overlayManager.add(overheadOverlay);
		// HealthOverlay deliberately not registered -- BarOverlay now renders the HP
		// bar alongside Shield / Special so all three respect the per-keyframe
		// order field and share the same auto-scaled width.
		overlayManager.add(hitsplatOverlay);
		overlayManager.add(textOverlay);
		overlayManager.add(barOverlay);
		overlayManager.add(screenFadeOverlay);

		keyManager.registerKeyListener(overlayKeyListener);
		keyManager.registerKeyListener(oculusOrbListener);
		keyManager.registerKeyListener(orbPreset1Listener);
		keyManager.registerKeyListener(orbPreset2Listener);
		keyManager.registerKeyListener(orbPreset3Listener);
		keyManager.registerKeyListener(quickSpawnListener);
		keyManager.registerKeyListener(quickLocationListener);
		keyManager.registerKeyListener(quickDuplicateListener);
		keyManager.registerKeyListener(quickRotateCWListener);
		keyManager.registerKeyListener(quickRotateCCWListener);
		keyManager.registerKeyListener(autoLeftListener);
		keyManager.registerKeyListener(autoRightListener);
		keyManager.registerKeyListener(autoUpListener);
		keyManager.registerKeyListener(autoDownListener);
		keyManager.registerKeyListener(addProgramStepListener);
		keyManager.registerKeyListener(removeProgramStepListener);
		keyManager.registerKeyListener(clearProgramStepListener);
		keyManager.registerKeyListener(addOrientationStartListener);
		keyManager.registerKeyListener(addOrientationGoalListener);
		keyManager.registerKeyListener(playPauseListener);
		keyManager.registerKeyListener(resetTimelineListener);
		keyManager.registerKeyListener(skipForwardListener);
		keyManager.registerKeyListener(skipSubForwardListener);
		keyManager.registerKeyListener(skipBackwardListener);
		keyManager.registerKeyListener(skipSubBackwardListener);
		keyManager.registerKeyListener(saveListener);
		keyManager.registerKeyListener(openListener);
		keyManager.registerKeyListener(undoListener);
		keyManager.registerKeyListener(redoListener);
		keyManager.registerKeyListener(nudgeNorthListener);
		keyManager.registerKeyListener(nudgeSouthListener);
		keyManager.registerKeyListener(nudgeEastListener);
		keyManager.registerKeyListener(nudgeWestListener);
		keyManager.registerKeyListener(nudgeUpListener);
		keyManager.registerKeyListener(nudgeDownListener);
		keyManager.registerKeyListener(scaleUpListener);
		keyManager.registerKeyListener(scaleDownListener);
		mouseManager.registerMouseWheelListener(this::mouseWheelMoved);
		mouseManager.registerMouseListener(this);

		if (config.autoSetup())
		{
			File SETUP_DIR = new File(config.setupPath());
			if (!SETUP_DIR.exists())
			{
				SETUP_DIR = new File(config.setupPath() + ".json");
				if (!SETUP_DIR.exists())
				{
					SETUP_DIR = new File(config.setupPath().replaceAll("/", "\\\\"));
					if (!SETUP_DIR.exists())
					{
						SETUP_DIR = new File(config.setupPath().replaceAll("/", "\\\\") + ".json");
					}
				}
			}

			if (SETUP_DIR.exists())
			{
				creatorsPanel.loadSetup(SETUP_DIR);
			}
			else
			{
				autoSetupPathFound = false;
			}
		}

		if (config.autoTransmog())
		{
			File TRANSMOG_DIR = new File(config.transmogPath());
			if (!TRANSMOG_DIR.exists())
			{
				TRANSMOG_DIR = new File(config.transmogPath() + ".json");
				if (!TRANSMOG_DIR.exists())
				{
					TRANSMOG_DIR = new File(config.transmogPath().replaceAll("/", "\\\\"));
					if (!TRANSMOG_DIR.exists())
					{
						TRANSMOG_DIR = new File(config.transmogPath().replaceAll("/", "\\\\") + ".json");
					}
				}
			}

			if (TRANSMOG_DIR.exists())
			{
				creatorsPanel.getToolBox().getModelUtilities().loadTransmog(TRANSMOG_DIR, TransmogLoadOption.BOTH);
			}
			else
			{
				autoTransmogFound = false;
			}
		}

		oculusOrbSpeed = config.orbSpeed();

		String string = configManager.getConfiguration("creatorssuite", "overlaysActive");
		try
		{
            overlaysActive = Boolean.parseBoolean(string);
		}
		catch (Exception e)
		{
			overlaysActive = false;
		}

		scheduleSetupVersioning();
	}

	/**
	 * (Re)schedules the periodic setup snapshot task based on the current config
	 * value. Cancels any existing task first. 0 interval disables periodic snapshots
	 * (manual saves still version via CreatorsPanel.saveToFile).
	 */
	private void scheduleSetupVersioning()
	{
		if (setupVersionTask != null)
		{
			setupVersionTask.cancel(false);
			setupVersionTask = null;
		}
		int minutes = config.setupVersionIntervalMinutes();
		if (minutes <= 0)
		{
			return;
		}
		if (setupVersionExecutor == null)
		{
			setupVersionExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r ->
			{
				Thread t = new Thread(r, "ck-setup-versioner");
				t.setDaemon(true);
				return t;
			});
		}
		long periodMs = minutes * 60_000L;
		setupVersionTask = setupVersionExecutor.scheduleAtFixedRate(
				() ->
				{
					try
					{
						if (creatorsPanel != null)
						{
							creatorsPanel.periodicSetupSnapshot();
						}
					}
					catch (Exception ex)
					{
						log.warn("Periodic setup snapshot failed: {}", ex.toString());
					}
				},
				periodMs,
				periodMs,
				java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	@Override
	protected void shutDown() throws Exception
	{
		if (setupVersionTask != null)
		{
			setupVersionTask.cancel(false);
			setupVersionTask = null;
		}
		if (setupVersionExecutor != null)
		{
			setupVersionExecutor.shutdownNow();
			setupVersionExecutor = null;
		}

		creatorsPanel.clearSidePanels(false);
		creatorsPanel.clearManagerPanels();
		creatorsPanel.getToolBox().dispose();

		eventBus.unregister(creatorsPanel.getToolBox().getProgrammer());
		eventBus.unregister(creatorsPanel.getToolBox().getTransmogPanel());

		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(overlay);
		overlayManager.remove(textOverlay);
		overlayManager.remove(overheadOverlay);
		// HealthOverlay is no longer registered -- BarOverlay handles HP. Keeping
		// the no-op remove call would warn on shutdown.
		overlayManager.remove(hitsplatOverlay);
		overlayManager.remove(barOverlay);
		overlayManager.remove(screenFadeOverlay);

		keyManager.unregisterKeyListener(overlayKeyListener);
		keyManager.unregisterKeyListener(oculusOrbListener);
		keyManager.unregisterKeyListener(orbPreset1Listener);
		keyManager.unregisterKeyListener(orbPreset2Listener);
		keyManager.unregisterKeyListener(orbPreset3Listener);
		keyManager.unregisterKeyListener(quickSpawnListener);
		keyManager.unregisterKeyListener(quickLocationListener);
		keyManager.unregisterKeyListener(quickDuplicateListener);
		keyManager.unregisterKeyListener(quickRotateCWListener);
		keyManager.unregisterKeyListener(quickRotateCCWListener);
		keyManager.unregisterKeyListener(autoLeftListener);
		keyManager.unregisterKeyListener(autoRightListener);
		keyManager.unregisterKeyListener(autoUpListener);
		keyManager.unregisterKeyListener(autoDownListener);
		keyManager.unregisterKeyListener(addProgramStepListener);
		keyManager.unregisterKeyListener(removeProgramStepListener);
		keyManager.unregisterKeyListener(clearProgramStepListener);
		keyManager.unregisterKeyListener(addOrientationStartListener);
		keyManager.unregisterKeyListener(addOrientationGoalListener);
		keyManager.unregisterKeyListener(playPauseListener);
		keyManager.unregisterKeyListener(resetTimelineListener);
		keyManager.unregisterKeyListener(skipForwardListener);
		keyManager.unregisterKeyListener(skipSubForwardListener);
		keyManager.unregisterKeyListener(skipBackwardListener);
		keyManager.unregisterKeyListener(skipSubBackwardListener);
		keyManager.unregisterKeyListener(saveListener);
		keyManager.unregisterKeyListener(openListener);
		keyManager.unregisterKeyListener(undoListener);
		keyManager.unregisterKeyListener(redoListener);
		keyManager.unregisterKeyListener(nudgeNorthListener);
		keyManager.unregisterKeyListener(nudgeSouthListener);
		keyManager.unregisterKeyListener(nudgeEastListener);
		keyManager.unregisterKeyListener(nudgeWestListener);
		keyManager.unregisterKeyListener(nudgeUpListener);
		keyManager.unregisterKeyListener(nudgeDownListener);
		keyManager.unregisterKeyListener(scaleUpListener);
		keyManager.unregisterKeyListener(scaleDownListener);
		mouseManager.unregisterMouseWheelListener(this::mouseWheelMoved);
		mouseManager.unregisterMouseListener(this);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		if (!autoSetupPathFound)
		{
			autoSetupPathFound = true;
			sendChatMessage("Creator's Kit auto-Setup has failed to find the file at the path: " + config.setupPath());
			sendChatMessage("Please ensure the config menu has the appropriate file path.");
		}

		if (!autoTransmogFound)
		{
			autoTransmogFound = true;
			sendChatMessage("Creator's Kit auto-Transmog has failed to find the file at the path: " + config.transmogPath());
			sendChatMessage("Please ensure the config menu has the appropriate file path.");
		}

		if (client.getLocalPlayer() == null)
			return;

		WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();
		LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
		WorldView worldView = client.getTopLevelWorldView();

		int region = worldView.getScene().isInstance() ? WorldPoint.fromLocalInstance(client, localPoint).getRegionID() : worldPoint.getRegionID();

		if (savedRegion != region)
			savedRegion = region;

		int plane = worldView.getPlane();
		if (savedPlane != plane)
		{
			savedPlane = plane;
			for (int i = 0; i < characters.size(); i++)
			{
				Character character = characters.get(i);
				setLocation(character, false, false, character.isActive() ? ActiveOption.ACTIVE : ActiveOption.INACTIVE, LocationOption.TO_CURRENT_TICK);
			}
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		updatePreviewObject(client.getTopLevelWorldView().getSelectedSceneTile());

		// Order: undo previous tick's shake -> camera lock (operates on un-shaken
		// focal so its lerp target is stable) -> apply new shake on top. Keeps
		// the two effects orthogonal: lock follows the character smoothly,
		// shake adds jitter without polluting the lock's lerp.
		undoPreviousScreenShake();
		updateCameraLock();
		applyCurrentScreenShake();

		if (addProgramStep)
		{
			addProgramStep = false;
			addProgramStep();
		}

		switch (autoRotateYaw)
		{
			case LEFT:
				client.setCameraYawTarget(client.getCameraYaw() - config.rotateHorizontalSpeed());
				break;
			case RIGHT:
				client.setCameraYawTarget(client.getCameraYaw() + config.rotateHorizontalSpeed());
		}

		switch (autoRotatePitch)
		{
			case UP:
				client.setCameraPitchTarget(client.getCameraPitch() + config.rotateVerticalSpeed());
				break;
			case DOWN:
				client.setCameraPitchTarget(client.getCameraPitch() - config.rotateVerticalSpeed());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			creatorsPanel.getToolBox().getProgrammer().updatePrograms(getCurrentTick());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getKey().equals("orbSpeed"))
		{
			client.setFreeCameraSpeed(config.orbSpeed());
			oculusOrbSpeed = config.orbSpeed();
		}

		if (event.getKey().equals("enableTransmog"))
		{
			if (transmog == null)
				return;

			clientThread.invokeLater(() ->
			{
				transmog.setActive(config.enableTransmog());
			});
		}

		if (event.getKey().equals("setupVersionInterval"))
		{
			scheduleSetupVersioning();
		}
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (config.enableCtrlHotkeys() && client.isKeyPressed(KeyCode.KC_CONTROL))
		{
			Character selectedCharacter = getSelectedCharacter();
			if (selectedCharacter != null)
			{
				client.getMenu().createMenuEntry(-1)
						.setOption(ColorUtil.prependColorTag("Relocate", Color.ORANGE))
						.setTarget(ColorUtil.colorTag(Color.GREEN) + selectedCharacter.getName())
						.setType(MenuAction.RUNELITE)
						.onClick(e -> setLocation(selectedCharacter, false, true, ActiveOption.ACTIVE, LocationOption.TO_HOVERED_TILE));

				MenuEntry me = client.getMenu().createMenuEntry(-2)
						.setOption(ColorUtil.prependColorTag("Keyframe", Color.ORANGE))
						.setTarget(ColorUtil.colorTag(Color.GREEN) + selectedCharacter.getName())
						.setType(MenuAction.RUNELITE);

				Menu menu = me.createSubMenu();
				SubMenuCreator.createSubMenus(creatorsPanel.getToolBox().getTimeSheetPanel(), menu);
			}
		}

		if (!config.rightClick())
		{
			return;
		}

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		addMenuEntries(topLevelWorldView);

		IndexedObjectSet<? extends WorldView> worldViews = topLevelWorldView.worldViews();
		for (WorldView worldView : worldViews)
		{
			addMenuEntries(worldView);
		}
	}

	public void addMenuEntries(WorldView worldView)
	{
		Tile tile = worldView.getSelectedSceneTile();
		if (tile == null)
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();
		boolean hoveringTile = false;
		for (MenuEntry menuEntry : menuEntries)
		{
			String option = menuEntry.getOption();
			if (option.equals("Walk here") || option.equals("Set heading"))
			{
				hoveringTile = true;
				break;
			}
		}

		if (!hoveringTile)
		{
			return;
		}

		modelGetter.addCharacterMenuEntries(tile);
		modelGetter.addLocalPlayerMenuEntries(tile);
		modelGetter.addTileItemMenuEntries(tile);
		modelGetter.addTileObjectMenuEntries(tile);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.rightClick())
		{
			return;
		}

		String target = event.getTarget();
		String option = event.getOption();

		NPC npc = event.getMenuEntry().getNpc();
		if (npc != null && option.equals("Examine"))
		{
			modelGetter.addNPCMenuEntries(target, npc);
		}

		Player player = event.getMenuEntry().getPlayer();
		if (player != null && option.equals("Trade with"))
		{
			modelGetter.addPlayerMenuEntries(target, player);
		}
	}

	public void setLocation(Character character, boolean initialize, boolean transplant, ActiveOption activeOption, LocationOption locationOption)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		boolean poh = MovementManager.useLocalLocations(client.getTopLevelWorldView());

		if (poh)
		{
			setLocationPOH(character, initialize, transplant, activeOption, locationOption);
			return;
		}

		setLocationWorld(character, initialize, transplant, activeOption, locationOption);
	}

	public void setLocationWorld(Character character, boolean initialize, boolean transplant, ActiveOption activeOption, LocationOption locationOption)
	{
		WorldView worldView = client.getTopLevelWorldView();
		LocalPoint localPoint = null;
		WorldPoint wp = character.getNonInstancedPoint();
		if (wp != null)
		{
			Collection<WorldPoint> wps = WorldPoint.toLocalInstance(worldView, wp);
			if (!wps.isEmpty())
			{
				wp = wps.iterator().next();
				localPoint = LocalPoint.fromWorld(worldView, wp);
			}
		}

		switch (locationOption)
		{
			case TO_PLAYER:
				localPoint = client.getLocalPlayer().getLocalLocation();
				break;
			case TO_HOVERED_TILE:
				Tile tile = worldView.getSelectedSceneTile();
				if (tile == null)
				{
					return;
				}

				localPoint = tile.getLocalLocation();
				break;
			case TO_SAVED_LOCATION:
			case TO_CURRENT_TICK:
			default:
				break;
		}

		if (localPoint == null || !localPoint.isInScene())
		{
			character.setInScene(false);
			return;
		}

		character.setInScene(true);

		if (initialize)
		{
			WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
			character.setNonInstancedPoint(worldPoint);
			character.setInPOH(false);
		}

		if (transplant)
		{
			WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
			pathFinder.transplantSteps(character, worldView, worldPoint.getX(), worldPoint.getY());

			KeyFrame kf = character.findNextKeyFrame(KeyFrameType.MOVEMENT, -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH);
			if (kf != null)
			{
				MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
				int[][] step = keyFrame.getPath();
				if (step.length != 0)
				{
					int[] first = step[0];
					worldPoint = new WorldPoint(first[0], first[1], worldView.getPlane());
				}
			}

			character.setNonInstancedPoint(worldPoint);
			character.setInPOH(false);
		}

		LocalPoint finalLocalPoint = localPoint;
		clientThread.invokeLater(() -> character.setLocation(finalLocalPoint, worldView.getPlane()));

		if (locationOption == LocationOption.TO_HOVERED_TILE)
		{
			creatorsPanel.getToolBox().getProgrammer().register3DChanges(character);
		}

		switch (activeOption)
		{
			case ACTIVE:
				character.setActive(true, true, true, clientThread);
				break;
			case INACTIVE:
				character.setActive(false, false, false, clientThread);
				break;
			case UNCHANGED:
				character.resetActive(clientThread);
		}
	}

	public void setLocationPOH(Character character, boolean initialize, boolean transplant, ActiveOption activeOption, LocationOption locationOption)
	{
		WorldView worldView = client.getTopLevelWorldView();
		LocalPoint localPoint = null;
		if (character.getInstancedPoint() != null)
		{
			localPoint = character.getInstancedPoint();
		}

		switch (locationOption)
		{
			case TO_PLAYER:
				localPoint = client.getLocalPlayer().getLocalLocation();
				break;
			case TO_HOVERED_TILE:
				Tile tile = worldView.getSelectedSceneTile();
				if (tile == null)
				{
					return;
				}

				localPoint = tile.getLocalLocation();
				break;
			case TO_CURRENT_TICK:
			case TO_SAVED_LOCATION:
			default:
				break;
		}

		if (localPoint == null || !localPoint.isInScene())
		{
			character.setInScene(false);
			return;
		}

		character.setInScene(true);

		if (initialize)
		{
			character.setInstancedPoint(localPoint);
			character.setInstancedPlane(worldView.getPlane());
			character.setInPOH(true);
		}

		if (transplant)
		{
			pathFinder.transplantSteps(character, worldView, localPoint.getSceneX(), localPoint.getSceneY());
			LocalPoint savedPoint = localPoint;

			KeyFrame kf = character.findNextKeyFrame(KeyFrameType.MOVEMENT, -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH);
			if (kf != null)
			{
				MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
				int[][] step = keyFrame.getPath();
				if (step.length != 0)
				{
					int[] first = step[0];
					savedPoint = new LocalPoint(first[0], first[1], worldView);
				}
			}

			character.setInstancedPoint(savedPoint);
			character.setInstancedPlane(worldView.getPlane());
			character.setInPOH(true);
		}

		LocalPoint finalLocalPoint = localPoint;
		clientThread.invokeLater(() -> character.setLocation(finalLocalPoint, worldView.getPlane()));

		if (locationOption == LocationOption.TO_HOVERED_TILE)
		{
			creatorsPanel.getToolBox().getProgrammer().register3DChanges(character);
		}

		switch (activeOption)
		{
			case ACTIVE:
				character.setActive(true, true, true, clientThread);
				break;
			case INACTIVE:
				character.setActive(true, false, false, clientThread);
				break;
			case UNCHANGED:
				character.resetActive(clientThread);
		}
	}

	public double getCurrentTick()
	{
		return creatorsPanel.getToolBox().getTimeSheetPanel().getCurrentTime();
	}

	public void setModel(Character character, boolean modelMode, int modelId)
	{
		CKObject ckObject = character.getCkObject();
		if (ckObject == null)
		{
			return;
		}

		clientThread.invokeLater(() -> {
			if (modelMode)
			{
				CustomModel customModel = character.getStoredModel();
				Model model = customModel == null ? client.loadModel(GOLDEN_CHIN) : customModel.getModel();
				ckObject.setModel(model);
				return;
			}

			Model model = client.loadModel(modelId);
			ckObject.setModel(model);
		});
	}

	public void setRadius(Character character, int radius)
	{
		CKObject ckObject = character.getCkObject();
		clientThread.invoke(() -> ckObject.setRadius(radius));
	}

	public void addOrientation(Character character, int addition)
	{
		CKObject ckObject = character.getCkObject();
		int orientation = ckObject.getOrientation();
		orientation += addition;
		if (orientation >= 2048)
			orientation -= 2048;

		if (orientation < 0)
			orientation += 2048;

		setOrientation(character, orientation);
	}

	public void setOrientation(Character character, int orientation)
	{
		CKObject ckObject = character.getCkObject();
		character.getOrientationSpinner().setValue(orientation);
		clientThread.invokeLater(() -> ckObject.setOrientation(orientation));
	}

	public void setupRLObject(Character character, boolean setHoveredTile, boolean transplant)
	{
		clientThread.invoke(() ->
		{
			CKObject ckObject = character.getCkObject();
			client.registerRuneLiteObject(ckObject);

			ckObject.setRadius((int) character.getRadiusSpinner().getValue());
			ckObject.setOrientation((int) character.getOrientationSpinner().getValue());

			boolean active = character.isActive();

			setModel(character, character.isCustomMode(), (int) character.getModelSpinner().getValue());
			character.setAnimation(client, random, AnimationType.ACTIVE, (int) character.getAnimationSpinner().getValue(), (int) character.getAnimationFrameSpinner().getValue(), config.randomizeStartFrame(), true);

			LocationOption locationOption = setHoveredTile ? LocationOption.TO_HOVERED_TILE : LocationOption.TO_SAVED_LOCATION;
			setLocation(character, true, transplant, active ? ActiveOption.ACTIVE : ActiveOption.INACTIVE, locationOption);

			if (client.getGameState() == GameState.LOGGED_IN)
			{
				creatorsPanel.getToolBox().getProgrammer().updateProgram(character);
			}
		});
	}

	public void sendChatMessage(String chatMessage)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		final String message = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(chatMessage).build();
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(message).build());
	}

	private void updatePreviewObject(Tile tile)
	{
		if (previewObject == null)
		{
			clientThread.invokeLater(() ->
			{
				previewObject = new CKObject(client);
				client.registerRuneLiteObject(previewObject);
				previewObject.setActive(false);

				ModelData arrow = client.loadModelData(4852);
				ModelData transparent = client.loadModelData(9925);
				if (arrow == null || transparent == null)
				{
					return;
				}

				transparent.cloneVertices();
				float[] tx = transparent.getVerticesX();
				float[] ty = transparent.getVerticesY();
				float[] tz = transparent.getVerticesZ();
				for (int i = 0; i < tx.length; i++)
				{
					tx[i] = 0;
					ty[i] = 0;
					tz[i] = 0;
				}

				arrow.cloneVertices().rotateY180Ccw().scale(256, 256, 256);
				ModelData merge = client.mergeModels(arrow, transparent);
				merge.cloneTransparencies();
				byte[] transparencies = merge.getFaceTransparencies();
				for (byte b = 0; b < merge.getFaceCount(); b++)
				{
					transparencies[b] = 115;
				}

				previewArrow = merge.light();
			});
			return;
		}

		if (!config.enableCtrlHotkeys())
		{
			previewObject.setActive(false);
			return;
		}

		Character selectedCharacter = getSelectedCharacter();
		if (!client.isKeyPressed(KeyCode.KC_CONTROL)
			|| client.isMenuOpen()
			|| tile == null
			|| selectedCharacter == null)
		{
			previewObject.setActive(false);
			return;
		}

		CKObject ckObject = selectedCharacter.getCkObject();
		if (ckObject == null)
		{
			return;
		}

		boolean allowArrow = false;
		int orientation;
		if (mousePressed)
		{
			final int yaw = client.getCameraYaw();
			final int pitch = client.getCameraPitch();
			Point p = client.getMouseCanvasPosition();
			double x = p.getX() - clickX;
			double y = -1 * (p.getY() - clickY);
			if (Math.sqrt(x * x + y * y) < 40)
			{
				orientation = Rotation.roundRotation(ckObject.getOrientation());
			}
			else
			{
				allowArrow = true;
				orientation = Rotation.getJagexDegrees(p.getX() - clickX, (p.getY() - clickY) * -1, yaw, pitch);
			}
		}
		else
		{
			orientation = ckObject.getOrientation();
		}

		Model model;
		if (mousePressed && previewArrow != null && allowArrow)
		{
			model = previewArrow;
		}
		else
		{
			if (selectedCharacter.isCustomMode())
			{
				if (selectedCharacter.getStoredModel() == null)
				{
					model = client.loadModel(29757);
				}
				else
				{
					model = selectedCharacter.getStoredModel().getModel();
				}
			}
			else
			{
				model = client.loadModel((int) selectedCharacter.getModelSpinner().getValue());
			}
		}

		LocalPoint lp;
		if (mousePressed)
		{
			lp = ckObject.getLocation();
			if (lp == null)
			{
				lp = tile.getLocalLocation();
			}
		}
		else
		{
			lp = tile.getLocalLocation();
			if (lp == null)
			{
				lp = ckObject.getLocation();
			}
		}

		if (lp == null || !lp.isInScene())
		{
			return;
		}

		int animId;
		if (allowArrow)
		{
			animId = -1;
		}
		else
		{
			animId = ckObject.getAnimationId();
		}

		previewObject.setModel(model);
		previewObject.setOrientation(orientation);
		previewObject.setAnimation(AnimationType.ACTIVE, animId);
		previewObject.setAnimationFrame(AnimationType.ACTIVE, ckObject.getAnimationFrame(AnimationType.ACTIVE), random, false,true);
		// Mirror sub-tile offsets onto the preview BEFORE setLocation so the previewObject's
		// own setLocation override picks them up and shifts the ghost off the tile center the
		// same way the placed Character will be. Without this the preview sits at the raw tile
		// position while the real Character is offset by whatever ALT+WASD/R/F set.
		previewObject.setOffsetX(ckObject.getOffsetX());
		previewObject.setOffsetY(ckObject.getOffsetY());
		previewObject.setOffsetZ(ckObject.getOffsetZ());
		previewObject.setLocation(lp, client.getTopLevelWorldView().getPlane());
		previewObject.setRadius(ckObject.getRadius());
		// Mirror render-fix state onto the preview so the ghost runs through the same
		// bracket-scaled animation pipeline as the placed Character. Now safe to do
		// (was previously reverted) because baseModel is restored to its original
		// vertices after each getModel() call -- the two objects can share the same
		// Model reference without leaking shrunken vertices between them.
		previewObject.setRenderFix(ckObject.isRenderFix());
		previewObject.setWidthScale(ckObject.getWidthScale());
		previewObject.setHeightScale(ckObject.getHeightScale());
		previewObject.setActive(true);
	}

	public ArrayList<ComplexPanel> getComplexPanels()
	{
		return creatorsPanel.getModelAnvil().getComplexPanels();
	}

	private final HotkeyListener overlayKeyListener = new HotkeyListener(() -> config.toggleOverlaysHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			overlaysActive = !overlaysActive;
			configManager.setConfiguration("creatorssuite", "overlaysActive", String.valueOf(overlaysActive));
		}
	};

	private final HotkeyListener oculusOrbListener = new HotkeyListener(() -> config.toggleOrbHotkey())
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

	private final HotkeyListener orbPreset1Listener = new HotkeyListener(() -> config.orbSpeedHotkey1())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setFreeCameraSpeed(config.speedHotkey1());
			sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey1());
		}
	};

	private final HotkeyListener orbPreset2Listener = new HotkeyListener(() -> config.orbSpeedHotkey2())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setFreeCameraSpeed(config.speedHotkey2());
			sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey2());
		}
	};

	private final HotkeyListener orbPreset3Listener = new HotkeyListener(() -> config.orbSpeedHotkey3())
	{
		@Override
		public void hotkeyPressed()
		{
			client.setFreeCameraSpeed(config.speedHotkey3());
			sendChatMessage("Oculus Orb set to speed: " + config.speedHotkey3());
		}
	};
	private final HotkeyListener quickSpawnListener = new HotkeyListener(() -> config.quickSpawnHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			Character selectedCharacter = getSelectedCharacter();
			if (selectedCharacter != null)
			{
				selectedCharacter.toggleActive(clientThread);
			}
		}
	};

	private final HotkeyListener quickLocationListener = new HotkeyListener(() -> config.quickLocationHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			Character selectedCharacter = getSelectedCharacter();
			if (selectedCharacter != null)
			{
				setLocation(selectedCharacter, false, true, ActiveOption.ACTIVE, LocationOption.TO_HOVERED_TILE);
			}
		}
	};

	private final HotkeyListener quickDuplicateListener = new HotkeyListener(() -> config.quickDuplicateHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			Character selectedCharacter = getSelectedCharacter();
			if (selectedCharacter != null)
			{
				creatorsPanel.onDuplicatePressed(selectedCharacter, true);
			}
		}
	};

	private final HotkeyListener quickRotateCWListener = new HotkeyListener(() -> config.quickRotateCWHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			Character selectedCharacter = getSelectedCharacter();
			if (selectedCharacter != null)
			{
				addOrientation(selectedCharacter, config.rotateDegrees().degrees * -1);
			}
		}
	};

	private final HotkeyListener quickRotateCCWListener = new HotkeyListener(() -> config.quickRotateCCWHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			Character selectedCharacter = getSelectedCharacter();
			if (selectedCharacter != null)
			{
				addOrientation(selectedCharacter, config.rotateDegrees().degrees);
			}
		}
	};

	private final HotkeyListener autoLeftListener = new HotkeyListener(() -> config.rotateLeftHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotateYaw = (autoRotateYaw == AutoRotate.OFF) ? AutoRotate.LEFT : AutoRotate.OFF;
		}
	};

	private final HotkeyListener autoRightListener = new HotkeyListener(() -> config.rotateRightHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotateYaw = (autoRotateYaw == AutoRotate.OFF) ? AutoRotate.RIGHT : AutoRotate.OFF;
		}
	};

	private final HotkeyListener autoUpListener = new HotkeyListener(() -> config.rotateUpHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotatePitch = (autoRotatePitch == AutoRotate.OFF) ? AutoRotate.UP : AutoRotate.OFF;
		}
	};

	private final HotkeyListener autoDownListener = new HotkeyListener(() -> config.rotateDownHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			autoRotatePitch = (autoRotatePitch == AutoRotate.OFF) ? AutoRotate.DOWN : AutoRotate.OFF;
		}
	};

	private final HotkeyListener addProgramStepListener = new HotkeyListener(() -> config.addProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			addStepKeyHeld = true;
			scrolledDuringStepHold = false;
		}

		@Override
		public void hotkeyReleased()
		{
			// Press without scrolling = add a step. Press + scroll = speed adjust only.
			if (!scrolledDuringStepHold)
			{
				addProgramStep = true;
			}
			addStepKeyHeld = false;
		}
	};

	private void addProgramStep()
	{
		creatorsPanel.getToolBox().getTimeSheetPanel().onAddMovementKeyPressed();
	}

	private final HotkeyListener removeProgramStepListener = new HotkeyListener(() -> config.removeProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			removeProgramStep();
		}
	};

	private void removeProgramStep()
	{
		Character selectedCharacter = getSelectedCharacter();
		if (selectedCharacter == null)
		{
			return;
		}

		KeyFrame kf = selectedCharacter.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
		if (kf == null)
		{
			return;
		}

		MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
		int[][] path = keyFrame.getPath();

		if (path.length == 0)
		{
			return;
		}

		int newLength = path.length - 1;
		keyFrame.setPath(ArrayUtils.remove(path, newLength));
		creatorsPanel.getToolBox().getProgrammer().register3DChanges(selectedCharacter);
	}

	private final HotkeyListener clearProgramStepListener = new HotkeyListener(() -> config.clearProgramStepHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			clearProgramSteps();
		}
	};

	private void clearProgramSteps()
	{
		Character selectedCharacter = getSelectedCharacter();
		if (selectedCharacter == null)
		{
			return;
		}

		KeyFrame kf = selectedCharacter.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
		if (kf == null)
		{
			return;
		}

		MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
		keyFrame.setPath(new int[0][2]);
		keyFrame.setCurrentStep(0);
	}

	private final HotkeyListener addOrientationStartListener = new HotkeyListener(() -> config.orientationStart()) {
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().onOrientationKeyPressed(OrientationHotkeyMode.SET_START);
		}
	};

	private final HotkeyListener addOrientationGoalListener = new HotkeyListener(() -> config.orientationEnd()) {
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().onOrientationKeyPressed(OrientationHotkeyMode.SET_GOAL);
		}
	};

	private final HotkeyListener playPauseListener = new HotkeyListener(() -> config.playPauseHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getProgrammer().togglePlay();
		}
	};

	private final HotkeyListener resetTimelineListener = new HotkeyListener(() -> config.resetTimelineHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			ToolBoxFrame toolBox = creatorsPanel.getToolBox();
			toolBox.getTimeSheetPanel().setCurrentTime(0, false);
		}
	};

	private final HotkeyListener skipForwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().onAttributeSkipForward();
		}
	};

	private final HotkeyListener skipBackwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().onAttributeSkipPrevious();
		}
	};

	private final HotkeyListener skipSubForwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().skipListener(0.1);
		}
	};

	private final HotkeyListener skipSubBackwardListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().skipListener(-0.1);
		}
	};

	private final HotkeyListener saveListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.quickSaveToFile();
		}
	};

	private final HotkeyListener openListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.openLoadSetupDialog();
		}
	};

	private final HotkeyListener undoListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().undo();
		}
	};

	private final HotkeyListener redoListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			creatorsPanel.getToolBox().getTimeSheetPanel().redo();
		}
	};

	/** Default per-press nudge distance in scene units (1 tile = 128 units, so 5 is sub-tile). */
	private static final int NUDGE_STEP = 5;

	/**
	 * ALT + WASD/R/F nudge the currently-selected Character(s) by {@link #NUDGE_STEP}
	 * scene units in the chosen direction. WASD are cardinal in scene space (W = north,
	 * S = south, D = east, A = west); R/F are vertical (R = up, F = down). Operates on
	 * every Character in the SelectionManager so a multi-selection nudges together.
	 *
	 * <p>ALT was picked over SHIFT to avoid a clash with the Detached Camera plugin,
	 * which uses SHIFT + WASDRF for slow camera movement.
	 *
	 * <p>Hotkeys are hardcoded rather than config-driven because they're a fixed gesture
	 * tied to the WASD convention; promoting them to user-configurable keybinds later
	 * is straightforward if needed.
	 */
	private void nudgeSelectedCharacters(int dx, int dy, int dz)
	{
		java.util.Collection<Character> targets = selectionManager.getSelected();
		if (targets.isEmpty())
		{
			Character primary = getSelectedCharacter();
			if (primary == null)
			{
				return;
			}
			primary.nudgeOffset(dx, dy, dz);
			return;
		}
		for (Character c : targets)
		{
			c.nudgeOffset(dx, dy, dz);
		}
	}

	/**
	 * ALT+Scroll companion to {@link #nudgeSelectedCharacters}: multiplies every
	 * selected Character's extraScale by {@code factor}. Compounding multiplication
	 * (not addition) so successive ups + downs return to the original size --
	 * additive scale steps drift because (1+x)*(1-x) != 1.
	 */
	private void scaleSelectedCharacters(double factor)
	{
		java.util.Collection<Character> targets = selectionManager.getSelected();
		if (targets.isEmpty())
		{
			Character primary = getSelectedCharacter();
			if (primary == null)
			{
				return;
			}
			primary.scaleBy(factor);
			return;
		}
		for (Character c : targets)
		{
			c.scaleBy(factor);
		}
	}

	private final HotkeyListener nudgeNorthListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_W, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			nudgeSelectedCharacters(0, NUDGE_STEP, 0);
		}
	};

	private final HotkeyListener nudgeSouthListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			nudgeSelectedCharacters(0, -NUDGE_STEP, 0);
		}
	};

	private final HotkeyListener nudgeEastListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			nudgeSelectedCharacters(NUDGE_STEP, 0, 0);
		}
	};

	private final HotkeyListener nudgeWestListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			nudgeSelectedCharacters(-NUDGE_STEP, 0, 0);
		}
	};

	// R/F for the vertical axis -- matches the WASDRF cluster most 3D nudge tools
	// use. Nudge gestures are ALT + WASDRF (the SHIFT + WASDRF cluster collides
	// with Detached Camera's slow-movement modifier).
	private final HotkeyListener nudgeUpListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			nudgeSelectedCharacters(0, 0, NUDGE_STEP);
		}
	};

	private final HotkeyListener nudgeDownListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			nudgeSelectedCharacters(0, 0, -NUDGE_STEP);
		}
	};

	/**
	 * ALT + = and ALT + - keyboard equivalents of ALT + Scroll for scaling.
	 * Same compounding math as the wheel handler -- one press = one notch.
	 * Useful when the wheel modifier is being eaten by a different plugin /
	 * mouse driver or the user just prefers the keyboard. VK_EQUALS is the
	 * "+" key without requiring SHIFT, and VK_MINUS is the dedicated "-".
	 */
	private final HotkeyListener scaleUpListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_EQUALS, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			double stepPct = Math.max(1, Math.min(50, config.scaleStepPercent())) / 100.0;
			scaleSelectedCharacters(1.0 + stepPct);
		}
	};

	private final HotkeyListener scaleDownListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_MINUS, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			double stepPct = Math.max(1, Math.min(50, config.scaleStepPercent())) / 100.0;
			scaleSelectedCharacters(1.0 / (1.0 + stepPct));
		}
	};

	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		// While the Add Program Step hotkey is held, scrolling adjusts the speed used
		// for new movement keyframes by 0.5 increments (up = faster, down = slower).
		if (addStepKeyHeld)
		{
			double next = currentStepSpeed - event.getWheelRotation() * 0.5;
			next = Math.max(0.5, Math.min(10.0, next));
			if (next != currentStepSpeed)
			{
				currentStepSpeed = next;
				scrolledDuringStepHold = true;
				if (creatorsPanel != null)
				{
					creatorsPanel.updateStepSpeedLabel(currentStepSpeed);
				}
			}
			event.consume();
			return event;
		}

		// ALT + Scroll resizes the currently-selected Character(s). Up = bigger,
		// down = smaller. Compounding by (1 + step) up vs / (1 + step) down so a
		// matched up/down pair cancels exactly -- additive deltas drift over time.
		//
		// Detect ALT via client.isKeyPressed(KC_ALT) instead of event.isAltDown().
		// MouseWheelEvent's modifiersEx had two quirks that made isAltDown()
		// unreliable here: (a) on some setups middle-click + scroll set the ALT
		// bit even with ALT not held, which used to silently fire the scale path;
		// (b) the workaround for (a) -- excluding BUTTON2_DOWN_MASK -- also
		// blocked real ALT + scroll on setups where the mouse driver always
		// reported the middle-button bit. The keyboard-state probe avoids both.
		if (client.isKeyPressed(KeyCode.KC_ALT))
		{
			int rotation = event.getWheelRotation();
			if (rotation == 0)
			{
				return event;
			}
			double stepPct = Math.max(1, Math.min(50, config.scaleStepPercent())) / 100.0;
			// Each notch of rotation applies one multiply. Negative rotation = scroll up = grow.
			double factor = rotation < 0
					? Math.pow(1.0 + stepPct, -rotation)
					: Math.pow(1.0 / (1.0 + stepPct), rotation);
			scaleSelectedCharacters(factor);
			event.consume();
			return event;
		}

		return event;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		if (config.enableCtrlHotkeys()
				&& e.getButton() == MouseEvent.BUTTON1
				&& client.isKeyPressed(KeyCode.KC_CONTROL))
		{
			mousePressed = true;
			clickX = e.getPoint().getX();
			clickY = e.getPoint().getY();
		}

		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		mousePressed = false;

		Character selectedCharacter = getSelectedCharacter();
		if (config.enableCtrlHotkeys() &&
				e.getButton() == MouseEvent.BUTTON1 &&
				client.isKeyPressed(KeyCode.KC_CONTROL) &&
				selectedCharacter != null)
		{
			double x = e.getX() - clickX;
			double y = -1 * (e.getY() - clickY);
			if (Math.sqrt(x * x + y * y) < 40)
			{
				return e;
			}

			final int yaw = client.getCameraYaw();
			final int pitch = client.getCameraPitch();
			int jUnit = Rotation.getJagexDegrees(x, y, yaw, pitch);
			setOrientation(selectedCharacter, jUnit);
		}

		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		return e;
	}

	@Provides
	CreatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CreatorsConfig.class);
	}
}
