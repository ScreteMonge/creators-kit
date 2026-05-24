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
	@Getter
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
	private BossHealthOverlay bossHealthOverlay;

	/**
	 * Phase 2 central store for Camera / Screen Fade / Screen Shake keyframes.
	 * Replaces per-Character storage for these three "global" types -- the
	 * runtime overlays read from this; UI editing writes back here. Old saves
	 * still have the per-Character fields populated and the load path migrates
	 * them into this store on import. Lombok generates the getter+setter via
	 * the class-level {@code @Getter}/{@code @Setter}. Non-final so loadSetup
	 * can swap the whole instance during file load.
	 */
	private com.creatorskit.saves.GlobalKeyFrames globalKeyFrames =
			new com.creatorskit.saves.GlobalKeyFrames();

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
		// Use the same wall-clock-smoothed time as the camera apply so shake
		// jitter updates per render frame instead of stepping every ClientTick.
		double currentTick = creatorsPanel != null
				? creatorsPanel.getToolBox().getProgrammer().getSmoothedCurrentTime()
				: getCurrentTick();
		double t = currentTick - active.getTick();
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
	 * Walks the central GlobalKeyFrames store for the most-recently-started
	 * SCREEN_SHAKE keyframe inside its duration window. Phase 2 refactor: was
	 * a per-Character scan; now reads from {@link #globalKeyFrames} so globals
	 * no longer need a Character owner.
	 */
	private com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame findActiveScreenShake()
	{
		double currentTick = getCurrentTick();
		com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame best = null;
		double bestStart = Double.NEGATIVE_INFINITY;
		com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame[] all =
				globalKeyFrames.getScreenShakeKeyFramesSafe();
		for (int i = 0; i < all.length; i++)
		{
			com.creatorskit.swing.timesheet.keyframe.ScreenShakeKeyFrame sk = all[i];
			if (sk == null)
			{
				continue;
			}
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

	/**
	 * Applies the currently-active CameraKeyFrame (if any) to the engine camera.
	 * Mirrors mlgudi/keyframe-camera's Playback.tick: find the segment whose
	 * window contains the current tick, interpolate from {@code from} to
	 * {@code to} via the keyframe's easing curve, and push focal point + pitch
	 * + yaw + zoom to the client.
	 *
	 * <p>Phase 2: reads from {@link #globalKeyFrames} instead of per-Character
	 * storage. "Next" is just the keyframe with the smallest tick strictly
	 * greater than the active one -- no Character-owner constraint anymore.
	 *
	 * <p>Forces camera mode to 1 (free-camera) before touching the focal-point
	 * setters -- those throw IllegalArgumentException otherwise. Won't fight
	 * the camera-lock lerp because that bails when no cameraLockedCharacter
	 * is set; users picking camera keyframes should not also hold a lock.
	 *
	 * <p>Public so {@link com.creatorskit.swing.timesheet.TimeSheetPanel#setCurrentTime}
	 * can call it the moment the timeline cursor moves -- otherwise during
	 * playback we'd wait for the next {@code onClientTick} subscriber order
	 * to fire, which the Programmer's time increment may have already
	 * displaced (was the "scrub works, play doesn't" bug).
	 */
	public void applyCurrentCameraKeyframe()
	{
		// Sub-tick precision during play so the camera interpolates at 50 Hz
		// instead of stepping every 60 ms (the 0.1-per-3-client-ticks playback
		// cadence). Falls back to the raw timeline tick when paused / scrubbing.
		double currentTick = creatorsPanel != null
				? creatorsPanel.getToolBox().getProgrammer().getSmoothedCurrentTime()
				: getCurrentTick();
		com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame[] all =
				globalKeyFrames.getCameraKeyFramesSafe();
		if (all.length == 0)
		{
			return;
		}

		// Release the camera when we're neither playing back nor scrubbing
		// directly onto an existing camera keyframe. Without this, dragging
		// the seeker across the timeline holds the camera locked through
		// every interpolated value, robbing the user of manual control. The
		// rule: camera is driven when playing (so the sequence plays out),
		// or when the seeker is exactly on a camera keyframe's tick (so the
		// user can preview that frame's shot). Anywhere else = user controls.
		boolean playing = creatorsPanel != null
				&& creatorsPanel.getToolBox().getProgrammer().isPlaying();
		if (!playing && !isSeekerOnCameraKeyFrame(all, currentTick))
		{
			return;
		}

		com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame best = null;
		double bestStart = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < all.length; i++)
		{
			com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame ck = all[i];
			if (ck == null)
			{
				continue;
			}
			// Window starts at this keyframe's tick. If there's a next keyframe
			// we interpolate up to its tick; if there's no next we HOLD at this
			// keyframe's values indefinitely (the user releases via the orb
			// hotkey or by adding another camera keyframe). durationTicks is
			// kept on the keyframe for save-format compat but no longer drives
			// the active window.
			if (currentTick < ck.getTick())
			{
				continue;
			}
			com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame nextCk = findNextCameraKf(all, ck.getTick());
			if (nextCk != null && currentTick > nextCk.getTick())
			{
				continue;
			}
			if (ck.getTick() > bestStart)
			{
				bestStart = ck.getTick();
				best = ck;
			}
		}
		if (best == null)
		{
			return;
		}

		com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame next = findNextCameraKf(all, best.getTick());
		com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame interp;
		if (next != null)
		{
			double span = next.getTick() - best.getTick();
			double t = span <= 0
					? 1.0
					: Math.max(0.0, Math.min(1.0, (currentTick - best.getTick()) / span));
			interp = com.creatorskit.swing.timesheet.keyframe.CameraEase.interpolate(best, next, t);
		}
		else
		{
			interp = best; // No next -- hold current values until duration runs out.
		}

		if (client.getCameraMode() != 1)
		{
			client.setCameraMode(1);
		}
		try
		{
			client.setCameraFocalPointX(interp.getFocalX());
			client.setCameraFocalPointY(interp.getFocalY());
			client.setCameraFocalPointZ(interp.getFocalZ());
			client.setCameraPitchTarget(
					com.creatorskit.swing.timesheet.keyframe.CameraEase.radiansToJau(interp.getPitch()));
			client.setCameraYawTarget(
					com.creatorskit.swing.timesheet.keyframe.CameraEase.radiansToJau(interp.getYaw()) % 2047);
			client.runScript(ScriptID.CAMERA_DO_ZOOM, interp.getScale(), interp.getScale());
		}
		catch (IllegalArgumentException ignored)
		{
			// Camera mode flipped under us between the check and the setters; bail.
		}
	}

	/**
	 * Whether {@code currentTick} sits exactly on a camera keyframe's tick.
	 * Used by the camera apply to decide whether to drive the camera while
	 * paused/scrubbing -- only on-frame previews count; anywhere else the
	 * camera is released so the user can pan manually. Epsilon comparison
	 * because ticks are doubles; CK rounds to 0.1 increments but precision
	 * drift through arithmetic still warrants a tolerance.
	 */
	private boolean isSeekerOnCameraKeyFrame(
			com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame[] all, double currentTick)
	{
		for (com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame ck : all)
		{
			if (ck != null && Math.abs(ck.getTick() - currentTick) < 0.001)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the camera keyframe with the smallest tick strictly greater than
	 * {@code referenceTick}, or null if none. O(n) scan -- the keyframe count
	 * in the central store is small enough that sorting wouldn't pay off, and
	 * the array isn't guaranteed sorted (UI inserts append, not insert-sort).
	 */
	private com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame findNextCameraKf(
			com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame[] all, double referenceTick)
	{
		com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame best = null;
		for (int i = 0; i < all.length; i++)
		{
			com.creatorskit.swing.timesheet.keyframe.CameraKeyFrame ck = all[i];
			if (ck == null || ck.getTick() <= referenceTick)
			{
				continue;
			}
			if (best == null || ck.getTick() < best.getTick())
			{
				best = ck;
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

	/**
	 * Paint-mode state for the "paste at cursor" hotkey. While {@code paintHeld}
	 * is true the plugin's mouseMoved hook drops a duplicate of the
	 * currently-selected Character onto each NEW tile the cursor crosses --
	 * {@code paintedTiles} dedupes so a single tile only gets one copy per
	 * gesture even if the user wiggles the cursor over it multiple times.
	 * Both are reset on hotkeyReleased.
	 */
	private boolean paintHeld = false;
	private final java.util.Set<net.runelite.api.coords.WorldPoint> paintedTiles = new java.util.HashSet<>();
	private java.util.concurrent.ScheduledExecutorService setupVersionExecutor;
	private java.util.concurrent.ScheduledFuture<?> setupVersionTask;

	@Override
	protected void startUp() throws Exception
	{
		// Wire the Character class's static reference to the central GlobalKeyFrames
		// store so per-Character getKeyFrames/setKeyFrames delegate the 3 global
		// types (Camera / Fade / Shake) to the shared store. Must happen before
		// any Character is constructed via the load path.
		Character.setGlobalKeyFramesStore(globalKeyFrames);

		// Restore the persisted hide-list before any GameObjectSpawned events
		// fire (login spawns the initial scene, which we want filtered).
		loadHiddenGameObjectIds();

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
		overlayManager.add(bossHealthOverlay);

		keyManager.registerKeyListener(pasteAtCursorListener);
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
		overlayManager.remove(bossHealthOverlay);

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
		keyManager.unregisterKeyListener(pasteAtCursorListener);
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

	/**
	 * Re-applies the camera keyframe just before every rendered frame. Fires
	 * at the renderer's frequency (60Hz+ at 60fps, higher with higher fps)
	 * which is faster than the 50Hz ClientTick the Programmer drives time
	 * advance with. The smoothed time only steps once per ClientTick so the
	 * apply itself is idempotent between client ticks -- but pushing the
	 * pitch/yaw targets every render frame lets the engine's own ease re-
	 * compute toward them every frame, which smooths the visible motion.
	 *
	 * <p>Inspired by mlgudi/keyframe-camera, which uses BeforeRender for the
	 * same reason -- ClientTick alone produces visible stutter on fast pans.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		// Full camera-frame cycle every rendered frame so screen shake stays
		// in sync with the camera keyframe. Order matches onClientTick:
		// undoPrev recovers the base focal point, applyCamera overwrites it
		// (when active), applyShake re-adds this frame's shake offset. When
		// camera is released, undoPrev gives us back the user's focal and
		// shake layers on top of that.
		undoPreviousScreenShake();
		applyCurrentCameraKeyframe();
		applyCurrentScreenShake();
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		updatePreviewObject(client.getTopLevelWorldView().getSelectedSceneTile());

		// Order: undo previous tick's shake -> camera lock (operates on un-shaken
		// focal so its lerp target is stable) -> camera keyframe (overrides lock
		// when active; the keyframe directly drives the focal point) -> apply
		// new shake on top. Keeps the effects orthogonal: lock/keyframe set the
		// base camera, shake adds jitter without polluting either.
		undoPreviousScreenShake();
		updateCameraLock();
		applyCurrentCameraKeyframe();
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

	// ----- Hide GameObjects ----------------------------------------------
	//
	// Persistent per-user list of GameObject ids to suppress from rendering.
	// Stored as a comma-separated string under config key {@link #HIDDEN_GAMEOBJECT_IDS_KEY}
	// (configManager rather than @ConfigItem because the canonical UI is a
	// dedicated Tools dialog, not the RuneLite sidebar). Hit by:
	//   * onGameObjectSpawned -- drops matching objects from the Scene as
	//     they appear (handles region/tile reload, login, instance enter/exit).
	//   * hideGameObjectId() -- when the user picks "Hide" from a right-click,
	//     also walks the live Scene so the object disappears immediately
	//     without waiting for natural respawn.
	//
	// Caveats:
	//   * Scene.removeGameObject() is GameObject-only. WallObject /
	//     DecorativeObject sibling categories can't be removed via the
	//     public API today, so they're out of scope.
	//   * Unhiding doesn't restore a live object -- the scene has no
	//     re-spawn event. User has to walk away / re-load the area for
	//     the natural spawn to recreate the object now that it's not
	//     filtered. The manage dialog flags this explicitly.

	private static final String HIDDEN_GAMEOBJECT_IDS_KEY = "hiddenGameObjectIds";

	private final java.util.Set<Integer> hiddenGameObjectIds = new java.util.LinkedHashSet<>();

	private void loadHiddenGameObjectIds()
	{
		hiddenGameObjectIds.clear();
		String raw = configManager.getConfiguration("creatorssuite", HIDDEN_GAMEOBJECT_IDS_KEY);
		if (raw == null || raw.isEmpty()) return;
		for (String token : raw.split(","))
		{
			String trimmed = token.trim();
			if (trimmed.isEmpty()) continue;
			try { hiddenGameObjectIds.add(Integer.parseInt(trimmed)); }
			catch (NumberFormatException ignored) {}
		}
	}

	private void saveHiddenGameObjectIds()
	{
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Integer id : hiddenGameObjectIds)
		{
			if (!first) sb.append(",");
			sb.append(id);
			first = false;
		}
		configManager.setConfiguration("creatorssuite", HIDDEN_GAMEOBJECT_IDS_KEY, sb.toString());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();
		if (obj != null && hiddenGameObjectIds.contains(obj.getId()))
		{
			client.getScene().removeGameObject(obj);
		}
	}

	/**
	 * Live-scene snapshot of the user's hidden ids. Read-only -- mutate via
	 * {@link #hideGameObjectId} / {@link #unhideGameObjectId} so the persisted
	 * config and the active scene stay in sync.
	 */
	public java.util.Set<Integer> getHiddenGameObjectIds()
	{
		return java.util.Collections.unmodifiableSet(hiddenGameObjectIds);
	}

	/**
	 * Adds {@code id} to the hidden set, persists, and walks the current
	 * scene removing every live GameObject matching that id. Called from
	 * the right-click "Hide" menu so the object disappears the moment the
	 * user picks the option, not on the next tile reload.
	 *
	 * <p>Returns true when the id was newly added (so callers can short-
	 * circuit chat-message / refresh logic on a no-op).
	 */
	public boolean hideGameObjectId(int id)
	{
		if (!hiddenGameObjectIds.add(id)) return false;
		saveHiddenGameObjectIds();
		// Walk the current scene to catch already-spawned instances. The
		// spawn event won't re-fire for them; only future spawns hit
		// onGameObjectSpawned, so without this pass the user would have
		// to walk away and back to see the hide take effect.
		clientThread.invoke(() ->
		{
			Scene scene = client.getScene();
			if (scene == null) return;
			Tile[][][] tiles = scene.getTiles();
			for (Tile[][] plane : tiles)
			{
				if (plane == null) continue;
				for (Tile[] row : plane)
				{
					if (row == null) continue;
					for (Tile t : row)
					{
						if (t == null) continue;
						GameObject[] gos = t.getGameObjects();
						if (gos == null) continue;
						for (GameObject go : gos)
						{
							if (go != null && go.getId() == id)
							{
								scene.removeGameObject(go);
							}
						}
					}
				}
			}
		});
		return true;
	}

	/**
	 * Removes {@code id} from the hidden set and persists. Does NOT re-spawn
	 * the live object (no public scene API to do so) -- the user has to
	 * walk away / re-load the area to see it again. Returns true when the
	 * id was actually in the set.
	 */
	public boolean unhideGameObjectId(int id)
	{
		if (!hiddenGameObjectIds.remove(id)) return false;
		saveHiddenGameObjectIds();
		return true;
	}

	/**
	 * Best-effort cache name for a GameObject id. Returns just the id
	 * formatted as a fallback when the ObjectComposition lookup is null or
	 * the cache name is the sentinel "null".
	 */
	public String getGameObjectName(int id)
	{
		try
		{
			ObjectComposition comp = client.getObjectDefinition(id);
			if (comp != null)
			{
				String n = comp.getName();
				if (n != null && !n.equals("null") && !n.isEmpty()) return n;
			}
		}
		catch (Exception ignored) {}
		return "Object " + id;
	}

	/**
	 * Opens the management dialog under Tools &gt; Hide GameObjects.
	 * Lists currently-hidden ids with their cache names, lets the user
	 * remove individual entries, clear the whole list, or manually add an
	 * id by number. The walk-away caveat for unhide is shown inline.
	 *
	 * <p>Built on Swing because the rest of the toolbox is Swing; the
	 * dialog is non-modal so the user can keep playing the game with it
	 * open and right-click to add more hides without dismissing it.
	 */
	public void showHideGameObjectsDialog()
	{
		javax.swing.SwingUtilities.invokeLater(() ->
		{
			javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(6, 6));
			panel.setBorder(new javax.swing.border.EmptyBorder(8, 8, 8, 8));

			javax.swing.DefaultListModel<String> model = new javax.swing.DefaultListModel<>();
			java.util.LinkedHashMap<String, Integer> rowToId = new java.util.LinkedHashMap<>();
			Runnable refresh = () ->
			{
				model.clear();
				rowToId.clear();
				for (Integer id : hiddenGameObjectIds)
				{
					String row = id + "  -  " + getGameObjectName(id);
					model.addElement(row);
					rowToId.put(row, id);
				}
			};
			refresh.run();

			javax.swing.JList<String> list = new javax.swing.JList<>(model);
			list.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			list.setVisibleRowCount(10);
			javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(list);
			scroll.setPreferredSize(new java.awt.Dimension(360, 200));
			panel.add(scroll, java.awt.BorderLayout.CENTER);

			javax.swing.JPanel south = new javax.swing.JPanel();
			south.setLayout(new javax.swing.BoxLayout(south, javax.swing.BoxLayout.Y_AXIS));

			javax.swing.JPanel buttons = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
			javax.swing.JButton remove = new javax.swing.JButton("Remove selected");
			remove.setToolTipText("Drop the highlighted ids from the hide list. Live instances stay invisible until the area reloads -- walk away and back to see them again.");
			remove.addActionListener(ev ->
			{
				for (String row : list.getSelectedValuesList())
				{
					Integer id = rowToId.get(row);
					if (id != null) unhideGameObjectId(id);
				}
				refresh.run();
			});
			buttons.add(remove);

			javax.swing.JButton clear = new javax.swing.JButton("Clear all");
			clear.setToolTipText("Drop every id from the hide list at once.");
			clear.addActionListener(ev ->
			{
				java.util.List<Integer> snap = new java.util.ArrayList<>(hiddenGameObjectIds);
				for (Integer id : snap) unhideGameObjectId(id);
				refresh.run();
			});
			buttons.add(clear);
			south.add(buttons);

			javax.swing.JPanel addRow = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
			addRow.add(new javax.swing.JLabel("Add id manually:"));
			javax.swing.JTextField idField = new javax.swing.JTextField(8);
			addRow.add(idField);
			javax.swing.JButton addBtn = new javax.swing.JButton("Add");
			Runnable doAdd = () ->
			{
				String txt = idField.getText() == null ? "" : idField.getText().trim();
				if (txt.isEmpty()) return;
				try
				{
					int id = Integer.parseInt(txt);
					if (hideGameObjectId(id)) refresh.run();
					idField.setText("");
				}
				catch (NumberFormatException nfe)
				{
					idField.setBackground(java.awt.Color.PINK);
				}
			};
			addBtn.addActionListener(ev -> doAdd.run());
			idField.addActionListener(ev -> doAdd.run());
			addRow.add(addBtn);
			south.add(addRow);

			javax.swing.JLabel note = new javax.swing.JLabel("<html><i>Note: removing an id from this list doesn't<br>"
					+ "re-spawn already-hidden objects. Walk away or<br>"
					+ "re-load the area to see them again.</i></html>");
			note.setBorder(new javax.swing.border.EmptyBorder(6, 0, 0, 0));
			south.add(note);

			panel.add(south, java.awt.BorderLayout.SOUTH);

			javax.swing.JFrame frame = new javax.swing.JFrame("Hidden GameObjects");
			frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
			frame.getContentPane().add(panel);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	/**
	 * Right-click menu hook: adds a red "Hide GameObject" entry for every
	 * non-already-hidden GameObject on the hovered tile. Gated by
	 * {@link CreatorsConfig#rightClick()} alongside the other Creator's Kit
	 * menu options. Called from {@link #addMenuEntries(WorldView)}.
	 *
	 * <p>Identical objects across multiple tiles (multi-tile scenery) only
	 * appear once per tile so the menu doesn't get spammed -- the de-dup
	 * is by object identity on this tile, since the same id can show up
	 * multiple times legitimately (e.g. paired flower beds).
	 */
	public void addHideGameObjectMenuEntries(Tile tile)
	{
		if (tile == null) return;
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects == null) return;
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (GameObject go : gameObjects)
		{
			if (go == null) continue;
			int id = go.getId();
			if (!seen.add(id)) continue;
			if (hiddenGameObjectIds.contains(id)) continue;
			String name = getGameObjectName(id);
			client.getMenu().createMenuEntry(-1)
					.setOption(ColorUtil.prependColorTag("Hide", Color.RED))
					.setTarget(ColorUtil.prependColorTag(name + " (" + id + ")", Color.CYAN))
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						if (hideGameObjectId(id))
						{
							sendChatMessage("Hiding GameObject " + name + " (" + id + "). Manage via Tools > Hide GameObjects.");
						}
					});
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
		// Hide-this-object entries on every GameObject on the tile. Gated
		// by the same rightClick() master toggle as everything else in
		// this branch.
		addHideGameObjectMenuEntries(tile);
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
		// ALT + Scroll / ALT + = / ALT + - drive extraScale on the live Character.
		// Without mirroring it onto the preview, the ghost stays at the unscaled
		// size and you can't see what the next paste will actually look like.
		previewObject.setExtraScale(ckObject.getExtraScale());
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

	/**
	 * Paste-at-cursor + paint-drag hotkey. Single press = paste a duplicate of
	 * the selected Character at the cursor's hovered tile. Hold + drag = paint
	 * a duplicate at every NEW tile the cursor crosses (mouseMoved drives the
	 * paint loop; paintedTiles dedupes so single tiles only get one copy per
	 * gesture). Both behaviours share the same path -- press always paints the
	 * initial tile, drag just extends it.
	 */
	private final HotkeyListener pasteAtCursorListener = new HotkeyListener(() -> config.pasteAtCursorHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			paintHeld = true;
			paintedTiles.clear();
			paintAtCurrentHoveredTile();
		}

		@Override
		public void hotkeyReleased()
		{
			paintHeld = false;
			paintedTiles.clear();
		}
	};

	/**
	 * Spawns a duplicate of {@link #getSelectedCharacter()} at the currently
	 * hovered scene tile if one exists. Called from the paste hotkey's initial
	 * press and from mouseMoved during paint-drag. Uses the explicit tile
	 * WorldPoint captured at call time rather than letting the placement path
	 * re-read getSelectedSceneTile inside a deferred clientThread invoke --
	 * that would let cursor movement between the duplicate call and the
	 * setLocation call cause copies to land at stale tiles, which during a
	 * fast drag would visibly lag the cursor.
	 */
	private void paintAtCurrentHoveredTile()
	{
		Character source = getSelectedCharacter();
		if (source == null) return;
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null) return;
		net.runelite.api.Tile tile = worldView.getSelectedSceneTile();
		if (tile == null) return;
		LocalPoint lp = tile.getLocalLocation();
		if (lp == null || !lp.isInScene()) return;
		WorldPoint wp = WorldPoint.fromLocalInstance(client, lp);
		if (wp == null) return;
		if (!paintedTiles.add(wp))
		{
			// Already painted this tile during the current gesture -- skip so
			// a wiggle inside a single tile doesn't pile up copies.
			return;
		}
		creatorsPanel.pasteCharacterAtTile(source, wp.getX(), wp.getY(), wp.getPlane(), false, worldView);
	}

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
			com.creatorskit.swing.timesheet.TimeSheetPanel ts = toolBox.getTimeSheetPanel();
			// If the user has set an A-B loop, "reset" means rewind to the
			// loop's start, not to tick 0. A alone => rewind to A; B alone
			// (A null) => rewind to 0 (the implicit A per the AB-loop spec);
			// neither set => rewind to 0 (original behaviour).
			double rewindTo = ts.getALoopTick() != null ? ts.getALoopTick() : 0.0;
			ts.setCurrentTime(rewindTo, false);
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

	/**
	 * ALT + WASD/R/F nudge the currently-selected Character(s) by
	 * {@code config.nudgeStep()} scene units in the chosen direction. WASD are
	 * cardinal in scene space (W = north, S = south, D = east, A = west); R/F
	 * are vertical (R = up, F = down). Operates on every Character in the
	 * SelectionManager so a multi-selection nudges together.
	 *
	 * <p>ALT was picked over SHIFT to avoid a clash with the Detached Camera plugin,
	 * which uses SHIFT + WASDRF for slow camera movement.
	 *
	 * <p>The step size is user-configurable (see {@code CreatorsConfig.nudgeStep})
	 * but defaults to 4 -- a divisor of 128 so 32 presses cover exactly one tile.
	 * Hotkeys themselves are still hardcoded; promoting them to user-configurable
	 * keybinds later is straightforward if needed.
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
			int step = config.nudgeStep();
			nudgeSelectedCharacters(0, step, 0);
		}
	};

	private final HotkeyListener nudgeSouthListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			int step = config.nudgeStep();
			nudgeSelectedCharacters(0, -step, 0);
		}
	};

	private final HotkeyListener nudgeEastListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			int step = config.nudgeStep();
			nudgeSelectedCharacters(step, 0, 0);
		}
	};

	private final HotkeyListener nudgeWestListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			int step = config.nudgeStep();
			nudgeSelectedCharacters(-step, 0, 0);
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
			int step = config.nudgeStep();
			nudgeSelectedCharacters(0, 0, step);
		}
	};

	private final HotkeyListener nudgeDownListener = new HotkeyListener(() -> new Keybind(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK))
	{
		@Override
		public void hotkeyPressed()
		{
			int step = config.nudgeStep();
			nudgeSelectedCharacters(0, 0, -step);
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
		// Paint-drag dispatch: while the paste hotkey is held, every cursor
		// move calls into paintAtCurrentHoveredTile which spawns a copy at the
		// hovered tile (deduped per gesture). Skipped when paintHeld is false
		// so non-paint mouse motion has zero overhead.
		if (paintHeld)
		{
			paintAtCurrentHoveredTile();
		}
		return e;
	}

	@Provides
	CreatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CreatorsConfig.class);
	}
}
