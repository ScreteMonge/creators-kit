package com.creatorskit.programming;

import com.creatorskit.*;
import com.creatorskit.Character;
import com.creatorskit.models.*;
import com.creatorskit.models.datatypes.SpotanimData;
import com.creatorskit.programming.orientation.Orientation;
import com.creatorskit.programming.orientation.OrientationAction;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.programming.orientation.OrientationInstruction;
import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

public class Programmer
{
    private final Client client;
    private final CreatorsConfig config;
    private final ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final TimeSheetPanel timeSheetPanel;
    private final DataFinder dataFinder;
    private final ModelUtilities modelUtilities;

    private int clientTickAtLastProgramTick = 0;
    private final int GOLDEN_CHIN = 29757;
    private final int TILE_LENGTH = 128;
    private final int TILE_DIAGONAL = 181; //Math.sqrt(Math.pow(128, 2) + Math.pow(128, 2))

    /**
     * Tracks which projectile spotanim id each slot's CKObject was built from, so
     * ensureProjectileSlot can detect when the keyframe's projectileId has changed
     * and rebuild the model instead of stalely reusing the previous one.
     */
    private final java.util.IdentityHashMap<CKObject, Integer> projectileLoadedIds = new java.util.IdentityHashMap<>();

    @Getter
    @Setter
    private boolean playing = false;
    private boolean triggerPause = false;

    @Inject
    public Programmer(Client client, CreatorsConfig config, ClientThread clientThread, CreatorsPlugin plugin, TimeSheetPanel timeSheetPanel, DataFinder dataFinder, ModelUtilities modelUtilities)
    {
        this.client = client;
        this.config = config;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.timeSheetPanel = timeSheetPanel;
        this.dataFinder = dataFinder;
        this.modelUtilities = modelUtilities;
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (playing)
        {
            clientTickAtLastProgramTick++;
            if (clientTickAtLastProgramTick >= 3)
            {
                clientTickAtLastProgramTick = 0;
                incrementSubTime();
            }
            // Stamp the wall-clock for sub-ClientTick interpolation in
            // getSmoothedCurrentTime. Without this stamp, frames between
            // ClientTicks all get the same smoothed value (~16 of them at
            // 60fps) and the camera apply stutters.
            lastClientTickRealtimeMs = System.currentTimeMillis();

            updateCharacter3D();
            if (clientTickAtLastProgramTick == 0 && triggerPause)
            {
                pause();
            }
        }
    }

    private void updateCharacter3D()
    {
        WorldView worldView = client.getTopLevelWorldView();
        double currentClientTick = (timeSheetPanel.getCurrentTime() * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH) + clientTickAtLastProgramTick;

        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            CKObject ckObject = character.getCkObject();
            if (ckObject == null)
            {
                character.setInScene(false);
                continue;
            }

            // Smoothly render the active ProjectileKeyFrame every client tick, blending
            // through sub-tick time so the arc stays in sync with the timeline at any
            // playback speed and remains pause-correct when scrubbing stops here.
            double projectileTime = timeSheetPanel.getCurrentTime()
                    + clientTickAtLastProgramTick * Constants.CLIENT_TICK_LENGTH / (double) Constants.GAME_TICK_LENGTH;
            updateProjectiles(character, projectileTime);

            KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
            if (kf == null)
            {
                transform3D(worldView, character, currentClientTick);
                continue;
            }

            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            if (keyFrame.getPlane() != worldView.getPlane())
            {
                character.setInScene(false);
                continue;
            }

            int[][] path = keyFrame.getPath();
            int currentStep = keyFrame.getCurrentStep();
            int pathLength = path.length;
            if (currentStep >= pathLength || currentStep == -1)
            {
                transform3D(worldView, character, currentClientTick);
                continue;
            }

            double tileSpeed = keyFrame.getSpeed();
            double speed = tileSpeed * Constants.CLIENT_TICK_LENGTH / Constants.GAME_TICK_LENGTH;
            int clientTicksPassed = client.getGameCycle() - keyFrame.getStepClientTick();
            double stepsComplete = clientTicksPassed * speed;
            currentStep = (int) Math.floor(stepsComplete);

            double endSpeed = (pathLength - 1) - (Math.floor(((pathLength - 1) / tileSpeed)) * tileSpeed);
            double finalSpeed = tileSpeed;

            if (stepsComplete + endSpeed > pathLength - 1)
            {
                double jumps = (pathLength - 1) % tileSpeed;
                if (jumps != 0)
                {
                    double ticksPreSlowdown = (pathLength - 1 - endSpeed) / tileSpeed;
                    double stepsPreSlowdown = ticksPreSlowdown * tileSpeed;

                    stepsComplete = ((clientTicksPassed) - (ticksPreSlowdown * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH)) * endSpeed * Constants.CLIENT_TICK_LENGTH / Constants.GAME_TICK_LENGTH + stepsPreSlowdown;
                    currentStep = (int) (stepsComplete);
                    finalSpeed = endSpeed;
                }
            }

            if (currentStep > pathLength)
            {
                currentStep = pathLength;
            }

            keyFrame.setCurrentStep(currentStep);

            transform3D(worldView, character, keyFrame, OrientationAction.ADJUST, currentStep, stepsComplete, clientTicksPassed, currentClientTick, finalSpeed);
        }
    }

    /**
     * Transforms the Character by Animation, Orientation, and based on its next MovementKeyFrame. For intended use when no current MovementKeyFrame exists
     * @param character the Character to animate
     */
    public void transform3D(WorldView worldView, Character character, double currentClientTick)
    {
        if (!playing)
        {
            KeyFrame mkf = character.findNextKeyFrame(KeyFrameType.MOVEMENT, timeSheetPanel.getCurrentTime());
            KeyFrame okf = character.findPreviousKeyFrame(KeyFrameType.ORIENTATION, timeSheetPanel.getCurrentTime(), true);

            if (okf == null)
            {
                okf = character.findNextKeyFrame(KeyFrameType.ORIENTATION, timeSheetPanel.getCurrentTime());
            }

            if (mkf == null && okf == null)
            {
                setAnimation(character, false, 0, 0);
                setOrientation(character, currentClientTick);
                plugin.setLocation(character, false, false, ActiveOption.UNCHANGED, LocationOption.TO_SAVED_LOCATION);
                return;
            }

            boolean useMovement = false;

            if (mkf == null)
            {
                useMovement = false;
            }

            if (okf == null)
            {
                useMovement = true;
            }

            if (mkf != null && okf != null)
            {
                useMovement = !(okf.getTick() <= mkf.getTick());
            }

            if (useMovement)
            {
                MovementKeyFrame keyFrame = (MovementKeyFrame) mkf;
                transform3D(worldView, character, keyFrame, OrientationAction.SET, 0, 0, 0, 0, 0);
                return;
            }

            setAnimation(character, false, 0, 0);
            setOrientationStatic(character);
            if (okf instanceof OrientationKeyFrame)
            {
                applyFaceTarget(character, (OrientationKeyFrame) okf);
            }
            plugin.setLocation(character, false, false, ActiveOption.UNCHANGED, LocationOption.TO_SAVED_LOCATION);
            return;
        }

        setAnimation(character, false, 0, 0);
        setOrientation(character, currentClientTick);
        KeyFrame activeOri = character.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
        if (activeOri instanceof OrientationKeyFrame)
        {
            applyFaceTarget(character, (OrientationKeyFrame) activeOri);
        }
    }

    /**
     * Transforms the Character's 3D model, by location with the intent to move it to the next subtile based on its current location and the predefined path,
     * by orientation to align with the current trajectory, and by animation according to what idle pose should be playing based on the direction of motion and orientation.
     * For intended use when playing the Programmer, not when setting the time manually
     * @param worldView the current worldview
     * @param character the Character to transform
     * @param mkf the MovementKeyFrame from which the location is being drawn
     * @param orientationAction indicate whether to set, adjust, or freeze orientation
     * @param currentStep the number of whole steps that have already been performed
     * @param stepsComplete the number of whole + sub steps that have already been performed
     * @param clientTicksPassed the number of client ticks that have passed since the start of the keyframe
     * @param finalSpeed the speed of the keyframe
     */
    public void transform3D(WorldView worldView, Character character, MovementKeyFrame mkf, OrientationAction orientationAction, int currentStep, double stepsComplete, int clientTicksPassed, double currentClientTick, double finalSpeed)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            character.setInScene(false);
            return;
        }

        if (mkf.getPlane() != worldView.getPlane())
        {
            character.setInScene(false);
            return;
        }

        double turnRate = mkf.getTurnRate();

        MovementComposition mc = getMovementComposition(worldView, character, mkf, currentStep, stepsComplete, orientationAction, clientTicksPassed, turnRate);
        if (mc == null)
        {
            character.setInScene(false);
            setAnimation(character, false, 0, 0);
            setOrientation(character, currentClientTick);
            return;
        }

        character.setInScene(true);
        setLocation(character, mkf, mc);

        int orientation = ckObject.getOrientation();
        int orientationGoal = mc.getOrientationGoal();
        int difference = Orientation.subtract(orientationGoal, orientation);

        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
        if (kf == null)
        {
            setOrientation(character, mc, orientationGoal, difference, stepsComplete, turnRate);
            setAnimation(character, mc.isMoving(), difference, finalSpeed);
            return;
        }

        OrientationKeyFrame okf = (OrientationKeyFrame) kf;
        OrientationInstruction instruction = findLastOrientation(mkf, okf, currentClientTick);
        if (instruction.getType() == KeyFrameType.ORIENTATION)
        {
            setOrientation(character, currentClientTick);
            applyFaceTarget(character, okf);
            setAnimation(character, mc.isMoving(), difference, finalSpeed);
            return;
        }

        if (instruction.isSetOrientation())
        {
            mc.setOrientationAction(OrientationAction.SET);
        }

        setOrientation(character, mc, orientationGoal, difference, stepsComplete, turnRate);
        setAnimation(character, mc.isMoving(), difference, finalSpeed);
    }

    /**
     * Feature B helper: if the OrientationKeyFrame names a target Character that
     * exists in the scene, snap the source's orientation to face it. Combat-style
     * "turn before attacking" — applied per tick so the source tracks a moving target.
     */
    private void applyFaceTarget(Character source, OrientationKeyFrame oriKeyFrame)
    {
        if (oriKeyFrame == null)
        {
            return;
        }
        String targetName = oriKeyFrame.getTargetCharacterName();
        if (targetName == null || targetName.isEmpty())
        {
            return;
        }
        Character target = findCharacterByName(targetName);
        if (target == null || target == source)
        {
            return;
        }
        CKObject sourceObj = source.getCkObject();
        CKObject targetObj = target.getCkObject();
        if (sourceObj == null || targetObj == null)
        {
            return;
        }
        LocalPoint sourceLp = sourceObj.getLocation();
        LocalPoint targetLp = targetObj.getLocation();
        if (sourceLp == null || targetLp == null
                || (sourceLp.getX() == targetLp.getX() && sourceLp.getY() == targetLp.getY()))
        {
            return;
        }
        int angle = (int) Orientation.getAngleBetween(sourceLp, targetLp);
        source.setOrientation(angle);
    }

    private Character findCharacterByName(String name)
    {
        if (name == null)
        {
            return null;
        }
        for (Character c : plugin.getCharacters())
        {
            if (name.equals(c.getName()))
            {
                return c;
            }
        }
        return null;
    }

    private OrientationInstruction findLastOrientation(MovementKeyFrame mkf, OrientationKeyFrame okf, double clientTicksForCurrentTime)
    {
        double oriEndClientTick = (okf.getTick() + okf.getDuration()) * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH;

        if (clientTicksForCurrentTime <= oriEndClientTick)
        {
            return new OrientationInstruction(KeyFrameType.ORIENTATION, false);
        }

        double pathDuration = Math.ceil((mkf.getPath().length - 1) / mkf.getSpeed());
        double movementEndTick = (mkf.getTick() + pathDuration) * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH;

        if (oriEndClientTick >= movementEndTick)
        {
            return new OrientationInstruction(KeyFrameType.ORIENTATION, false);
        }

        boolean setOrientation = Math.round(clientTicksForCurrentTime - oriEndClientTick) == 1;
        return new OrientationInstruction(KeyFrameType.MOVEMENT, setOrientation);
    }


    /**
     * Transforms the Character's 3D model, by location according to the last MovementKeyFrame, by orientation based on the last OrientationKeyFrame (or MovementKeyFrame, if later),
     * and by animation according to what idle pose should be playing based on the direction of motion and orientation.
     * For intended use when setting the keyframe manually
     * @param worldView the current worldview
     * @param character the Character to transform
     * @param mkf the MovementKeyFrame from which the location is being drawn
     * @param currentStep the number of whole steps that have already been performed
     * @param stepsComplete the number of whole + sub steps that have already been performed
     * @param clientTicksPassed the number of client ticks that have passed since the start of the keyframe
     * @param finalSpeed the speed of the keyframe
     */
    public void transform3DStatic(WorldView worldView, Character character, MovementKeyFrame mkf, int currentStep, double stepsComplete, int clientTicksPassed, double finalSpeed)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            character.setInScene(false);
            return;
        }

        if (mkf.getPlane() != worldView.getPlane())
        {
            character.setInScene(false);
            return;
        }

        double turnRate = mkf.getTurnRate();

        MovementComposition mc = getMovementComposition(worldView, character, mkf, currentStep, stepsComplete, OrientationAction.SET, clientTicksPassed, turnRate);

        int orientation = ckObject.getOrientation();
        int orientationGoal = 0;
        int difference = 0;
        int differenceToGoal = 0;
        boolean isMoving = false;

        if (mc == null)
        {
            character.setInScene(false);
            setAnimation(character, false, 0, 0);
        }
        else
        {
            character.setInScene(true);
            setLocation(character, mkf, mc);
            orientationGoal = mc.getOrientationGoal();
            difference = Orientation.subtract(orientationGoal, orientation);
            differenceToGoal = Orientation.subtract(mc.getOrientationToSet(), orientation);
            isMoving = mc.isMoving();
        }

        KeyFrameType orientationDeterminant = findLastOrientation(character);

        if (orientationDeterminant == KeyFrameType.ORIENTATION)
        {
            setOrientationStatic(character);
            KeyFrame activeOri = character.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
            if (activeOri instanceof OrientationKeyFrame)
            {
                applyFaceTarget(character, (OrientationKeyFrame) activeOri);
            }
            setAnimation(character, isMoving, differenceToGoal, finalSpeed);
            return;
        }

        if (orientationDeterminant == KeyFrameType.MOVEMENT)
        {
            if (mc == null)
            {
                setAnimation(character, false, 0, 0);
                return;
            }

            setOrientation(character, mc, orientationGoal, difference, stepsComplete, turnRate);
            orientation = ckObject.getOrientation();
            difference = Orientation.subtract(orientationGoal, orientation);
            setAnimation(character, mc.isMoving(), difference, finalSpeed);
            return;
        }

        setAnimation(character, false, 0, 0);
    }

    /**
     * Sets the location if a Movement keyframe is present
     * @param character the character to set the location for
     * @param keyFrame the Movement keyframe of interest
     * @param mc the MovementComposition regarding the current character's intended location
     */
    private void setLocation(Character character, MovementKeyFrame keyFrame, MovementComposition mc)
    {
        LocalPoint lp = mc.getLocalPoint();
        if (lp == null)
        {
            return;
        }

        character.setLocation(lp, keyFrame.getPlane());
    }

    /**
     * Sets the orientation of the Character based on its Orientation KeyFrame
     * Intended for use when playing the programmer
     * @param character the character to modify
     * @param currentClientTick the current time, in client ticks
     */
    private void setOrientation(Character character, double currentClientTick)
    {
        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
        if (kf == null)
        {
            return;
        }

        OrientationKeyFrame keyFrame = (OrientationKeyFrame) kf;
        double ticksPassed = currentClientTick - (keyFrame.getTick() * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH);
        double duration = keyFrame.getDuration() * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH;
        if (ticksPassed > duration)
        {
            return;
        }

        int orientation = getOrientation(keyFrame, ticksPassed, duration);
        character.setOrientation(orientation);
    }

    /**
     * Gets the current intended orientation of the Character, assuming that no movement keyframe exists, or the last movement keyframe has ended.
     * Intended for use when playing the programmer
     * @param keyFrame the Orientation keyframe of interest
     * @param ticksPassed the current time, in client ticks
     * @param duration the keyframe duration, in client ticks
     * @return the orientation the Character should be set to
     */
    private int getOrientation(OrientationKeyFrame keyFrame, double ticksPassed, double duration)
    {
        if (ticksPassed > duration)
        {
            ticksPassed = duration;
        }

        int start = keyFrame.getStart();
        int end = keyFrame.getEnd();

        int difference = Orientation.subtract(end, start);
        double turnRate = keyFrame.getTurnRate();

        double rotation = turnRate * ticksPassed;

        int newOrientation;
        if (difference > (rotation * -1) && difference < rotation)
        {
            newOrientation = end;
        }
        else if (difference > 0)
        {
            newOrientation = Orientation.boundOrientation((int) (start + rotation));
        }
        else
        {
            newOrientation = Orientation.boundOrientation((int) (start - rotation));
        }

        return newOrientation;
    }

    /**
     * Sets the orientation of the Character based on its Orientation KeyFrame
     * Intended for use when manually setting the program time
     * @param character the character to modify
     */
    private void setOrientationStatic(Character character)
    {
        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
        if (kf == null)
        {
            int orientation = (int) character.getOrientationSpinner().getValue();
            character.setOrientation(orientation);
            return;
        }

        OrientationKeyFrame keyFrame = (OrientationKeyFrame) kf;
        int orientation = getOrientationStatic(keyFrame);
        character.setOrientation(orientation);
    }

    /**
     * Gets the orientation dictated by the current OrientationKeyFrame for the current tick. Intended for use when setting the time, not running the program
     * @param keyFrame the keyframe to examine
     * @return the appropriate orientation for the given keyframe
     */
    public int getOrientationStatic(OrientationKeyFrame keyFrame)
    {
        double ticksPassed = timeSheetPanel.getCurrentTime() - keyFrame.getTick();
        double duration = keyFrame.getDuration();
        if (ticksPassed > duration)
        {
            ticksPassed = duration;
        }

        int start = keyFrame.getStart();
        int end = keyFrame.getEnd();

        int difference = Orientation.subtract(end, start);
        double turnRate = keyFrame.getTurnRate();

        double rotation = turnRate * ticksPassed * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH;

        int newOrientation;
        if (difference > (rotation * -1) && difference < rotation)
        {
            newOrientation = end;
        }
        else if (difference > 0)
        {
            newOrientation = Orientation.boundOrientation((int) (start + rotation));
        }
        else
        {
            newOrientation = Orientation.boundOrientation((int) (start - rotation));
        }

        return newOrientation;
    }

    /**
     * Sets the orientation of the character based on its movement keyframe
     * Intended for use while playing the programmer
     * @param character the character to modify
     * @param mc the MovementComposition, based on its movement keyframe
     * @param orientationGoal the orientation end-goal, determined by the trajectory of movement
     * @param difference the difference between the current orientation and end goal orientation
     * @param stepsComplete the number of steps complete
     * @param turnRate the rate at which the Character should turn
     */
    private void setOrientation(Character character, MovementComposition mc, int orientationGoal, int difference, double stepsComplete, double turnRate)
    {
        OrientationAction orientationAction = mc.getOrientationAction();
        CKObject ckObject = character.getCkObject();
        if (orientationAction == OrientationAction.SET)
        {
            if (ckObject.getOrientation() != orientationGoal)
            {
                character.setOrientation(orientationGoal);
            }

            return;
        }

        if (orientationAction == OrientationAction.FREEZE)
        {
            return;
        }

        if (difference != 0)
        {
            if (Math.ceil(stepsComplete) == Math.floor(stepsComplete))
            {
                return;
            }

            int orientation = ckObject.getOrientation();
            int turnSpeed = (int) (turnRate);

            int newOrientation;
            if (difference > (turnSpeed * -1) && difference < turnSpeed)
            {
                newOrientation = orientationGoal;
            }
            else if (difference > 0)
            {
                newOrientation = Orientation.boundOrientation(orientation + turnSpeed);
            }
            else
            {
                newOrientation = Orientation.boundOrientation(orientation - turnSpeed);
            }

            character.setOrientation(newOrientation);
        }
    }

    private void setAnimation(Character character, boolean isMoving, int orientationDifference, double speed)
    {
        CKObject ckObject = character.getCkObject();

        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ANIMATION);
        if (kf == null)
        {
            int animId = (int) character.getAnimationSpinner().getValue();
            int animFrame = (int) character.getAnimationFrameSpinner().getValue();
            Animation animation = ckObject.getAnimations()[0];
            if (animation == null || animation.getId() != animId)
            {
                character.setAnimation(clientThread, client, plugin.getRandom(), AnimationType.ACTIVE, animId, animFrame, config.randomizeStartFrame(), true);
            }
            return;
        }

        AnimationKeyFrame keyFrame = (AnimationKeyFrame) kf;
        boolean randomizeStartFrame = false;
        int pose = getPoseAnimation(keyFrame, isMoving, orientationDifference, speed);
        int poseStartFrame = 0;
        if (pose == keyFrame.getIdle() || pose == keyFrame.getWalk() || pose == keyFrame.getRun())
        {
            poseStartFrame = keyFrame.getStartFrame();
            if (config.randomizeStartFrame())
            {
                randomizeStartFrame = true;
            }
        }

        int finalPoseStartFrame = poseStartFrame;
        boolean finalRandomizeStartFrame = randomizeStartFrame;
        clientThread.invoke(() ->
        {
            Animation currentPose = ckObject.getAnimations()[1];

            if (pose == -1)
            {
                if (currentPose != null && currentPose.getId() != -1)
                {
                    ckObject.unsetAnimation(AnimationType.POSE);
                }
            }

            if (pose != -1)
            {
                if (currentPose == null || currentPose.getId() != pose)
                {
                    ckObject.setAnimation(AnimationType.POSE, pose);
                    ckObject.setAnimationFrame(AnimationType.POSE, finalPoseStartFrame, plugin.getRandom(), finalRandomizeStartFrame, false);
                }

                if (!playing)
                {
                    double poseAnimSpeed = keyFrame.getSpeed();
                    if (poseAnimSpeed <= 0) poseAnimSpeed = 1.0;
                    setPoseAnimationFrame(ckObject, timeSheetPanel.getCurrentTime(), keyFrame.getTick(), finalRandomizeStartFrame, finalPoseStartFrame, poseAnimSpeed);
                }
            }
        });
    }

    private int getPoseAnimation(AnimationKeyFrame keyFrame, boolean isMoving, int orientationDifference, double speed)
    {
        if (speed == 0 || !isMoving)
        {
            return keyFrame.getIdle();
        }

        int idle = keyFrame.getIdle();
        int move = keyFrame.getWalk();
        int run = keyFrame.getRun();
        if (speed > 1 && run != -1)
        {
            move = keyFrame.getRun();
        }

        if (orientationDifference <= 256 && orientationDifference >= -256)
        {
            if (move == -1)
            {
                return idle;
            }

            return move;
        }

        if (orientationDifference > 256 && orientationDifference < 736)
        {
            int animId = keyFrame.getWalkRight();
            if (animId == -1)
            {
                if (move == -1)
                {
                    return idle;
                }

                return move;
            }

            return animId;
        }

        if (orientationDifference < -256 && orientationDifference > -736)
        {
            int animId = keyFrame.getWalkLeft();
            if (animId == -1)
            {
                if (move == -1)
                {
                    return idle;
                }

                return move;
            }

            return animId;
        }

        int animId = keyFrame.getWalk180();
        if (animId == -1)
        {
            if (move == -1)
            {
                return idle;
            }

            return move;
        }

        return animId;
    }

    /**
     * Gets the LocalPoint corresponding to the exact tick for the current time, including both full tile and sub tile coordinates.
     * @param worldView the current WorldView
     * @param character the character to find the location for
     * @param keyFrame the keyframe from the character to get the path from
     * @param currentStep the current whole number step
     * @param stepsComplete the current non-whole number step
     * @return the LocalPoint corresponding to the current tick along the character's currently defined path
     */
    public MovementComposition getMovementComposition(WorldView worldView, Character character, MovementKeyFrame keyFrame, int currentStep, double stepsComplete, OrientationAction orientationAction, int clientTicksPassed, double turnRate)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return null;
        }

        int pathLength = keyFrame.getPath().length;
        if (currentStep >= pathLength)
        {
            LocalPoint secondLast = getLocation(worldView, keyFrame, pathLength - 2);
            LocalPoint last = getLocation(worldView, keyFrame, pathLength - 1);
            if (last == null)
            {
                return null;
            }

            if (secondLast == null)
            {
                return new MovementComposition(false, last, OrientationAction.FREEZE, 0, 0);
            }

            double directionX = last.getSceneX() - secondLast.getSceneX();
            double directionY = last.getSceneY() - secondLast.getSceneY();
            double angle = Orientation.radiansToJAngle(Math.atan(directionY / directionX), directionX, directionY);
            return new MovementComposition(false, last, OrientationAction.SET, (int) angle, 0);
        }

        LocalPoint previous = getLocation(worldView, keyFrame, currentStep - 1);
        LocalPoint start = getLocation(worldView, keyFrame, currentStep);
        LocalPoint destination = getLocation(worldView, keyFrame, currentStep + 1);

        if (start == null && destination == null)
        {
            return null;
        }

        if (start != null && destination != null)
        {
            double percentComplete = stepsComplete - currentStep;
            double subSteps = percentComplete * TILE_LENGTH;

            double angle = Orientation.getAngleBetween(start, destination);
            int startX = start.getX();
            int startY = start.getY();

            int changeX = (int) (subSteps * Orientation.orientationX(angle));
            int currentX = startX + changeX;

            int changeY = (int) (subSteps * Orientation.orientationY(angle));
            int currentY = startY + changeY;

            LocalPoint lp = new LocalPoint(currentX, currentY, worldView);
            if (orientationAction == OrientationAction.ADJUST)
            {
                return new MovementComposition(true, lp, OrientationAction.ADJUST, (int) angle, 0);
            }

            //Only relevant when OrientationAction.SET, in other words, when setting the time manually and not playing
            int orientationFromTick = getOrientationFromTick(previous, start, angle, clientTicksPassed, currentStep, keyFrame.getSpeed(), turnRate);
            return new MovementComposition(true, lp, OrientationAction.SET, orientationFromTick, (int) angle);
        }

        if (start == null)
        {
            if (previous == null)
            {
                return new MovementComposition(false, destination, OrientationAction.FREEZE, 0, 0);
            }

            double directionX = destination.getSceneX() - previous.getSceneX();
            double directionY = destination.getSceneY() - previous.getSceneY();
            double angle = Orientation.radiansToJAngle(Math.atan(directionY / directionX), directionX, directionY);

            return new MovementComposition(false, destination, orientationAction, (int) angle, 0);
        }

        if (previous == null)
        {
            return new MovementComposition(false, start, OrientationAction.FREEZE, 0, 0);
        }

        double angle = Orientation.getAngleBetween(previous, start);
        return new MovementComposition(false, start, OrientationAction.SET, (int) angle, 0);
    }

    private int getOrientationFromTick(LocalPoint previous, LocalPoint start, double angle, int clientTicksPassed, int currentStep, double speed, double turnRate)
    {
        if (previous == null)
        {
            return (int) angle;
        }

        double originalAngle = Orientation.getAngleBetween(previous, start);


        int angleDifference = Orientation.subtract((int) angle, (int) originalAngle);
        if (angleDifference == 0)
        {
            return (int) angle;
        }

        double ticksSinceLastStep = (clientTicksPassed * speed) - (((double) currentStep) * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH);
        double change = turnRate * ticksSinceLastStep;

        if (angleDifference > (change * -1) && angleDifference < change)
        {
            return (int) angle;
        }

        if (angleDifference > 0)
        {
            return Orientation.boundOrientation((int) (originalAngle + change));
        }

        return Orientation.boundOrientation((int) (originalAngle - change));
    }



    /**
     * Gets the LocalPoint of a specific integer step. Used primarily to assess the start and end tiles of a path in order to calculate the movement between
     * @param keyFrame the keyframe for which to grab the path
     * @param step the number of whole number steps complete
     * @return the LocalPoint pertaining to the current step for the keyframe
     */
    private LocalPoint getLocation(WorldView worldView, MovementKeyFrame keyFrame, int step)
    {
        boolean isInPOH = MovementManager.useLocalLocations(worldView);
        if (keyFrame.isPoh() != isInPOH)
        {
            return null;
        }

        int[][] path = keyFrame.getPath();
        if (step >= path.length || step < 0)
        {
            return null;
        }

        int[] currentStep = path[step];

        if (isInPOH)
        {
            LocalPoint lp = LocalPoint.fromScene(currentStep[0], currentStep[1], worldView);
            if (!lp.isInScene())
            {
                return null;
            }

            return lp;
        }

        WorldPoint wp = new WorldPoint(currentStep[0], currentStep[1], keyFrame.getPlane());
        if (worldView.isInstance())
        {
            Collection<WorldPoint> wps = WorldPoint.toLocalInstance(worldView, wp);
            if (!wps.isEmpty())
            {
                wp = wps.iterator().next();
            }
        }

        LocalPoint lp = LocalPoint.fromWorld(worldView, wp);
        if (lp == null || !lp.isInScene())
        {
            return null;
        }

        return lp;
    }

    private void incrementSubTime()
    {
        double time = TimeSheetPanel.round(timeSheetPanel.getCurrentTime() + 0.1);
        timeSheetPanel.setCurrentTime(time, true);
    }

    /**
     * Wall-clock timestamp of the most recent {@link #onClientTick} during play.
     * Used by {@link #getSmoothedCurrentTime} to interpolate between
     * ClientTicks at millisecond precision -- without this the smoothed time
     * has only 50Hz granularity, so the camera-keyframe apply (fired per
     * BeforeRender, 60Hz+) sees the same value for ~16 frames in a row and
     * stutters visibly. Tracking wall-clock ms gets us continuous motion.
     */
    private long lastClientTickRealtimeMs = 0L;

    /**
     * Sub-tick-precision timeline time at millisecond resolution. Adds the
     * elapsed wall-clock fraction of the current ClientTick window on top of
     * the regular per-ClientTick offset, so per-frame consumers (camera
     * keyframe apply at BeforeRender) get a unique value per frame instead
     * of one shared value across all the frames that fall inside the same
     * 20ms ClientTick gap. Pause / scrub return the raw timeline tick.
     */
    public double getSmoothedCurrentTime()
    {
        double base = timeSheetPanel.getCurrentTime();
        if (!playing)
        {
            return base;
        }
        // Per-ClientTick component: 0, 1/30, 2/30 within the current 60ms /
        // 0.1-game-tick window.
        double clientTickComponent = clientTickAtLastProgramTick
                * Constants.CLIENT_TICK_LENGTH / (double) Constants.GAME_TICK_LENGTH;
        // Sub-ClientTick component: wall-clock ms since the last ClientTick,
        // expressed in timeline-tick units. 1 ms = 1 / GAME_TICK_LENGTH game
        // ticks (because one game-tick window of timeline time spans 600 ms
        // of real time at 1x playback).
        long now = System.currentTimeMillis();
        long elapsed = lastClientTickRealtimeMs == 0L ? 0L : Math.max(0L, now - lastClientTickRealtimeMs);
        // Cap at CLIENT_TICK_LENGTH so a long pause / slow tick doesn't let
        // the smoothed time overshoot the next ClientTick's actual advance.
        elapsed = Math.min(elapsed, Constants.CLIENT_TICK_LENGTH);
        double subClientTickComponent = elapsed / (double) Constants.GAME_TICK_LENGTH;
        return base + clientTickComponent + subClientTickComponent;
    }

    public void togglePlay()
    {
        togglePlay(!playing);
    }

    public void togglePlay(boolean play)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (play)
        {
            triggerPause = false;
            playing = true;
            timeSheetPanel.setPlayButtonIcon(true);
            double currentTime = timeSheetPanel.getCurrentTime();

            ArrayList<Character> characters = plugin.getCharacters();
            for (int i = 0; i < characters.size(); i++)
            {
                Character character = characters.get(i);

                character.play();
                character.resetMovementKeyFrame(client.getGameCycle(), currentTime);
            }
            return;
        }

        triggerPause = true;
    }

    public void pause()
    {
        triggerPause = false;
        playing = false;
        // Reset the wall-clock stamp so a subsequent play doesn't add stale
        // pause-duration into the smoothed-time interpolation.
        lastClientTickRealtimeMs = 0L;
        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            character.pause();
        }

        timeSheetPanel.setPlayButtonIcon(false);
    }

    /**
     * Updates the program with their current KeyFrame at the current time in the TimeSheetPanel
     * Intended for use adding or removing a KeyFrame from a specific Character
     */
    public void updateProgram(Character character)
    {
        updateProgram(character, timeSheetPanel.getCurrentTime());
    }

    /**
     * Updates the program with their current KeyFrame for the given time
     * Intended for use adding or removing a KeyFrame from a specific Character
     * @param character the character to update
     * @param tick the tick at which to look for KeyFrames
     */
    public void updateProgram(Character character, double tick)
    {
        character.updateProgram(tick);
        character.setPlaying(playing);
        registerSpotAnimChanges(character, KeyFrameType.SPOTANIM, tick);
        registerSpotAnimChanges(character, KeyFrameType.SPOTANIM2, tick);
        register3DChanges(character);
        registerActiveAnimationChanges(character);
        registerModelChanges(character);
        registerSpawnChanges(character);
        updateProjectiles(character, tick);
    }

    /**
     * Loops through all Characters and updates their current KeyFrame for the given time
     * Intended for use when manually setting the current tick in the program
     * @param tick the tick at which to look for KeyFrames
     */
    public void updatePrograms(double tick)
    {
        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            updateProgram(characters.get(i), tick);
        }
    }

    /**
     * Loops through all Characters and updates their current KeyFrame for the current time in the TimeSheetPanel
     * Intended for use when Playing the programmer, not when manually setting the time
     */
    public void updateProgramsOnTick()
    {
        double currentTime = timeSheetPanel.getCurrentTime();
        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            KeyFrame[] currentFrames = character.getCurrentFrames();

            KeyFrame currentMovement = currentFrames[KeyFrameType.getIndex(KeyFrameType.MOVEMENT)];
            double lastMovementTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentMovement != null)
            {
                lastMovementTick = currentMovement.getTick();
            }

            KeyFrame nextMovement = character.findNextKeyFrame(KeyFrameType.MOVEMENT, lastMovementTick);
            if (nextMovement != null)
            {
                if (nextMovement.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextMovement, KeyFrameType.MOVEMENT);
                    // Reset currentStep before resetMovementKeyFrame so updateCharacter3D's
                    // early-out (currentStep >= pathLength -> skip movement) doesn't read
                    // a stale value left over from a previous playthrough. Without this
                    // reset, rewinding past a keyframe then playing would leave the
                    // keyframe's currentStep at pathLength; when the seeker re-crosses
                    // the keyframe tick the early-out skipped the re-computation, so
                    // movement only started when the user scrubbed EXACTLY onto the
                    // keyframe (where register3DChanges resets currentStep to 0 itself).
                    ((MovementKeyFrame) nextMovement).setCurrentStep(0);
                    character.resetMovementKeyFrame(client.getGameCycle(), currentTime);
                    // Intentionally skip register3DChanges here during playback. That
                    // helper calls transform3DStatic which uses OrientationAction.SET
                    // and snaps the orientation to the new MKF's first segment angle —
                    // visible as the snap at every keyframe boundary. Letting the next
                    // updateCharacter3D tick render with ADJUST instead means the in-
                    // progress turn from the previous keyframe carries through and
                    // smoothly interpolates toward this keyframe's direction.
                }
            }


            KeyFrame currentAnimation = currentFrames[KeyFrameType.getIndex(KeyFrameType.ANIMATION)];
            double lastAnimationTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentAnimation != null)
            {
                lastAnimationTick = currentAnimation.getTick();
            }

            KeyFrame nextAnimation = character.findNextKeyFrame(KeyFrameType.ANIMATION, lastAnimationTick);
            if (nextAnimation != null)
            {
                if (nextAnimation.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextAnimation, KeyFrameType.ANIMATION);
                    registerActiveAnimationChanges(character);
                }
            }


            KeyFrame currentSpawn = currentFrames[KeyFrameType.getIndex(KeyFrameType.SPAWN)];
            double lastSpawnTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentSpawn != null)
            {
                lastSpawnTick = currentSpawn.getTick();
            }

            KeyFrame nextSpawn = character.findNextKeyFrame(KeyFrameType.SPAWN, lastSpawnTick);
            if (nextSpawn != null)
            {
                if (nextSpawn.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextSpawn, KeyFrameType.SPAWN);
                    registerSpawnChanges(character);
                }
            }


            KeyFrame currentModel = currentFrames[KeyFrameType.getIndex(KeyFrameType.MODEL)];
            double lastModelTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentModel != null)
            {
                lastModelTick = currentModel.getTick();
            }

            KeyFrame nextModel = character.findNextKeyFrame(KeyFrameType.MODEL, lastModelTick);
            if (nextModel != null)
            {
                if (nextModel.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextModel, KeyFrameType.MODEL);
                    registerModelChanges(character);
                }
            }


            KeyFrame currentOrientation = currentFrames[KeyFrameType.getIndex(KeyFrameType.ORIENTATION)];
            double lastOrientationTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentOrientation != null)
            {
                lastOrientationTick = currentOrientation.getTick();
            }

            KeyFrame nextOrientation = character.findNextKeyFrame(KeyFrameType.ORIENTATION, lastOrientationTick);
            if (nextOrientation != null)
            {
                if (nextOrientation.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextOrientation, KeyFrameType.ORIENTATION);
                    registerOrientationChanges(character);
                }
            }


            KeyFrame currentText = currentFrames[KeyFrameType.getIndex(KeyFrameType.TEXT)];
            double lastTextTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentText != null)
            {
                lastTextTick = currentText.getTick();
            }

            KeyFrame nextText = character.findNextKeyFrame(KeyFrameType.TEXT, lastTextTick);
            if (nextText != null)
            {
                if (nextText.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextText, KeyFrameType.TEXT);
                }
            }


            KeyFrame currentOverhead = currentFrames[KeyFrameType.getIndex(KeyFrameType.OVERHEAD)];
            double lastOverheadTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentOverhead != null)
            {
                lastOverheadTick = currentOverhead.getTick();
            }

            KeyFrame nextOverhead = character.findNextKeyFrame(KeyFrameType.OVERHEAD, lastOverheadTick);
            if (nextOverhead != null)
            {
                if (nextOverhead.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextOverhead, KeyFrameType.OVERHEAD);
                }
            }


            KeyFrame currentHealth = currentFrames[KeyFrameType.getIndex(KeyFrameType.HEALTH)];
            double lastHealthTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentHealth != null)
            {
                lastHealthTick = currentHealth.getTick();
            }

            KeyFrame nextHealth = character.findNextKeyFrame(KeyFrameType.HEALTH, lastHealthTick);
            if (nextHealth != null)
            {
                if (nextHealth.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextHealth, KeyFrameType.HEALTH);
                }
            }


            for (KeyFrameType spotanimType : KeyFrameType.SPOTANIM_TYPES)
            {
                KeyFrame currentSpotAnim = currentFrames[KeyFrameType.getIndex(spotanimType)];
                double lastSpotAnimTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
                if (currentSpotAnim != null)
                {
                    lastSpotAnimTick = currentSpotAnim.getTick();
                }

                KeyFrame nextSpotAnim = character.findNextKeyFrame(spotanimType, lastSpotAnimTick);
                if (nextSpotAnim != null)
                {
                    if (nextSpotAnim.getTick() <= currentTime)
                    {
                        character.setCurrentKeyFrame(nextSpotAnim, spotanimType);
                        registerSpotAnimChanges(character, spotanimType, currentTime);
                    }
                }
            }


            for (KeyFrameType hitsplatType : KeyFrameType.HITSPLAT_TYPES)
            {
                KeyFrame currentHitsplat = currentFrames[KeyFrameType.getIndex(hitsplatType)];
                double lastHitsplatTick = Integer.MIN_VALUE;
                if (currentHitsplat != null)
                {
                    lastHitsplatTick = currentHitsplat.getTick();
                }

                KeyFrame nextHitsplat = character.findNextKeyFrame(hitsplatType, lastHitsplatTick);
                if (nextHitsplat != null)
                {
                    if (nextHitsplat.getTick() <= currentTime)
                    {
                        character.setCurrentKeyFrame(nextHitsplat, hitsplatType);
                    }
                }
            }

            KeyFrame currentProjectile = currentFrames[KeyFrameType.getIndex(KeyFrameType.PROJECTILE)];
            double lastProjectileTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentProjectile != null)
            {
                lastProjectileTick = currentProjectile.getTick();
            }

            KeyFrame nextProjectile = character.findNextKeyFrame(KeyFrameType.PROJECTILE, lastProjectileTick);
            if (nextProjectile != null && nextProjectile.getTick() <= currentTime)
            {
                // Just advance the current PROJECTILE pointer so currentFrames stays in sync.
                // Rendering is handled per-frame by updateProjectiles(...) from updateCharacter3D,
                // so timeline scrubbing and pausing reflect the projectile's true position.
                character.setCurrentKeyFrame(nextProjectile, KeyFrameType.PROJECTILE);
            }

            // Shield / Special / ScreenFade — purely display-state keyframes (no scene
            // mutation), so the only job here is to advance currentFrames so the
            // overlays can see them. The original loop above missed these and so the
            // overlays only lit up while scrubbing (which goes through the
            // updateProgram path that iterates ALL_KEYFRAME_TYPES).
            for (KeyFrameType barType : new KeyFrameType[]{KeyFrameType.SHIELD, KeyFrameType.SPECIAL, KeyFrameType.SCREEN_FADE, KeyFrameType.SCREEN_SHAKE})
            {
                KeyFrame currentBar = currentFrames[KeyFrameType.getIndex(barType)];
                double lastBarTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
                if (currentBar != null)
                {
                    lastBarTick = currentBar.getTick();
                }
                KeyFrame nextBar = character.findNextKeyFrame(barType, lastBarTick);
                if (nextBar != null && nextBar.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextBar, barType);
                }
            }
        }
    }

    /**
     * Renders the active ProjectileKeyFrame for a Character based on the current timeline
     * position. Unlike {@code client.createProjectile} (which is driven by real-world game
     * cycles and so doesn't pause when the timeline pauses), this manually places one
     * CKObject per resolved target, computing position and parabolic Z arc from
     * {@code currentTime} so scrubbing, pausing, and stepping all behave correctly.
     *
     * <p>Active window is {@code [tick + startDelay, tick + startDelay + duration]}.
     * Outside that window all of the Character's projectile CKObjects are deactivated.
     */
    private void updateProjectiles(Character character, double currentTime)
    {
        java.util.List<CKObject> projObjs = character.getProjectileObjects();

        ProjectileKeyFrame kf = (ProjectileKeyFrame) character.findPreviousKeyFrame(KeyFrameType.PROJECTILE, currentTime, true);
        if (kf == null)
        {
            deactivateProjectileObjects(projObjs, 0);
            return;
        }

        double startTime = kf.getTick() + kf.getStartDelayTicks();
        double duration = Math.max(0.0001, kf.getDurationTicks());
        double endTime = startTime + duration;

        if (currentTime < startTime || currentTime > endTime)
        {
            deactivateProjectileObjects(projObjs, 0);
            return;
        }

        if (kf.getTarget() == null || kf.getTarget().trim().isEmpty())
        {
            deactivateProjectileObjects(projObjs, 0);
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        LocalPoint sourceLp = resolveCharacterLocalPoint(character, worldView);
        if (sourceLp == null)
        {
            deactivateProjectileObjects(projObjs, 0);
            return;
        }

        java.util.List<Character> targets = resolveProjectileTargets(kf.getTarget());
        if (targets.isEmpty())
        {
            deactivateProjectileObjects(projObjs, 0);
            return;
        }

        double t = (currentTime - startTime) / duration;
        if (t < 0) t = 0;
        if (t > 1) t = 1;

        int level = character.getCkObject() != null ? character.getCkObject().getLevel() : worldView.getPlane();
        int sourceTileZ = Perspective.getTileHeight(client, sourceLp, level);

        int slot = 0;
        for (Character target : targets)
        {
            if (target == character)
            {
                continue;
            }
            LocalPoint targetLp = resolveCharacterLocalPoint(target, worldView);
            if (targetLp == null)
            {
                continue;
            }

            int sx = sourceLp.getX();
            int sy = sourceLp.getY();
            int tx = targetLp.getX();
            int ty = targetLp.getY();

            int x = (int) Math.round(sx + (tx - sx) * t);
            int y = (int) Math.round(sy + (ty - sy) * t);

            LocalPoint here = new LocalPoint(x, y, sourceLp.getWorldView());
            int tileZ;
            if (here.isInScene())
            {
                int targetTileZ = Perspective.getTileHeight(client, targetLp, level);
                tileZ = (int) Math.round(sourceTileZ + (targetTileZ - sourceTileZ) * t);
            }
            else
            {
                tileZ = sourceTileZ;
            }

            double heightOffset = kf.getStartHeight() * (1 - t)
                    + kf.getEndHeight() * t
                    + kf.getSlope() * t * (1 - t);
            int zFinal = (int) Math.round(tileZ - heightOffset);

            // Angle from source -> target in JAU (0 = south, 512 = west, 1024 = north,
            // 1536 = east). atan2(dx,dy) measures angle from north going east; +1024
            // rotates from "facing the source" to "facing the target".
            int dx = tx - sx;
            int dy = ty - sy;
            int orientation = 0;
            if (dx != 0 || dy != 0)
            {
                orientation = ((int) Math.round(Math.atan2(dx, dy) * 2048.0 / (2 * Math.PI)) + 1024) & 0x7FF;
            }

            // Pitch angle for the face-trajectory feature. Velocity is the analytic
            // derivative of the arc position: horizontal is constant (target - source),
            // vertical is the derivative of (tileZ_lerp - heightOffset). Sign is chosen
            // to match the visual convention "nose follows velocity": on ascent the
            // projectile's head tilts up, on descent it tilts down. Determined
            // empirically -- adjust the dzDt sign if the model points the wrong way.
            double pitchRadians = 0.0;
            if (kf.isFaceTrajectory())
            {
                double dHeightOffsetDt = (kf.getEndHeight() - kf.getStartHeight())
                        + kf.getSlope() * (1 - 2 * t);
                int targetTileZForPitch = here.isInScene()
                        ? Perspective.getTileHeight(client, targetLp, level)
                        : sourceTileZ;
                double dTileZDt = targetTileZForPitch - sourceTileZ;
                double dzDt = dTileZDt - dHeightOffsetDt;
                double horizDist = Math.hypot(dx, dy);
                if (horizDist > 0.0001 || Math.abs(dzDt) > 0.0001)
                {
                    pitchRadians = Math.atan2(dzDt, horizDist);
                }
            }

            ensureProjectileSlot(character, projObjs, slot, kf.getProjectileId());

            CKObject obj = projObjs.get(slot);
            final int finalZ = zFinal;
            final int finalOrientation = orientation;
            final LocalPoint finalHere = here;
            final int finalLevel = level;
            final double finalPitch = pitchRadians;
            final boolean applyPitch = kf.isFaceTrajectory();
            clientThread.invokeLater(() ->
            {
                if (!finalHere.isInScene())
                {
                    obj.setActive(false);
                    return;
                }
                // setLocation copies x/y from the LocalPoint (which is already sub-tile
                // accurate) and sets z to the tile baseline; we then override z with our
                // computed arc height so the projectile travels along the parabola.
                obj.setLocation(finalHere, finalLevel);
                obj.setZ(finalZ);
                obj.setOrientation(finalOrientation);
                applyProjectilePitch(obj, finalPitch, applyPitch);
                if (!obj.isActive())
                {
                    obj.setActive(true);
                }
            });

            slot++;
        }

        deactivateProjectileObjects(projObjs, slot);
    }

    /**
     * Lazily allocates a CKObject for the given projectile slot, loads its model from the
     * spotanim cache, and registers it with the scene. Rebuilds the slot's CKObject when
     * the projectile id has changed since it was last loaded — without this, editing the
     * keyframe's Projectile ID (or deleting and re-adding the keyframe) would keep
     * rendering the original model because the slot was non-null and the previous
     * implementation early-returned on null check alone.
     */
    private void ensureProjectileSlot(Character character, java.util.List<CKObject> projObjs, int slot, int projectileId)
    {
        while (projObjs.size() <= slot)
        {
            projObjs.add(null);
        }

        CKObject existing = projObjs.get(slot);
        if (existing != null)
        {
            Integer loadedId = projectileLoadedIds.get(existing);
            if (loadedId != null && loadedId == projectileId)
            {
                return; // Slot is up to date.
            }
            // Projectile id has changed — tear down the old CKObject so a fresh one
            // gets built with the new spotanim's model. Done on the client thread to
            // avoid racing with the renderer.
            final CKObject oldObj = existing;
            projectileLoadedIds.remove(oldObj);
            clientThread.invokeLater(() ->
            {
                if (oldObj.isActive())
                {
                    oldObj.setActive(false);
                }
            });
            projObjs.set(slot, null);
        }

        SpotanimData data = dataFinder.getSpotAnimData(projectileId);
        if (data == null)
        {
            return;
        }
        ModelStats[] stats = dataFinder.findSpotAnim(data);

        // Use ProjectileCKObject so face-trajectory pitch can be applied AFTER animation
        // in getModel(), rather than by pre-rotating baseModel vertices (which breaks
        // bone-skin correspondence and tears the rigging at non-zero pitch).
        ProjectileCKObject obj = new ProjectileCKObject(client);
        obj.setDrawFrontTilesFirst(true);
        obj.setHasAnimKeyFrame(true);
        projObjs.set(slot, obj);
        projectileLoadedIds.put(obj, projectileId);

        clientThread.invokeLater(() ->
        {
            LightingStyle ls = LightingStyle.SPOTANIM;
            CustomLighting cl = new CustomLighting(ls.getAmbient() + data.getAmbient(), ls.getContrast() + data.getContrast(), ls.getX(), ls.getY(), ls.getZ());
            Model model = modelUtilities.constructModelFromCache(stats, new int[0], false, LightingStyle.CUSTOM, cl);
            obj.setModel(model);
            if (data.getAnimationId() != -1)
            {
                obj.setAnimation(AnimationType.ACTIVE, data.getAnimationId());
                obj.setLoop(true);
                obj.setPlaying(true);
            }
        });
    }

    /**
     * Updates the pitch the {@link ProjectileCKObject} applies after animation in its
     * getModel() override. baseModel is never mutated -- the rotation is layered on top
     * of the animation pipeline output each frame, so animation skinning stays correct
     * even at extreme pitches.
     */
    private void applyProjectilePitch(CKObject obj, double pitchRadians, boolean enabled)
    {
        if (!(obj instanceof ProjectileCKObject))
        {
            return;
        }
        ProjectileCKObject pco = (ProjectileCKObject) obj;
        pco.setPitchEnabled(enabled);
        pco.setPitchRadians(enabled ? pitchRadians : 0.0);
    }

    /**
     * Deactivates and removes from the scene every CKObject in {@code projObjs} at index
     * {@code fromIndex} and above. Used both when the projectile is inactive (deactivate
     * all) and when the active target count shrinks between frames (deactivate extras).
     */
    private void deactivateProjectileObjects(java.util.List<CKObject> projObjs, int fromIndex)
    {
        for (int i = fromIndex; i < projObjs.size(); i++)
        {
            CKObject obj = projObjs.get(i);
            if (obj == null)
            {
                continue;
            }
            clientThread.invokeLater(() ->
            {
                if (obj.isActive())
                {
                    obj.setActive(false);
                }
            });
        }
    }

    /**
     * Returns the Character's best-known LocalPoint, preferring the live CKObject location
     * (set during playback / scrubbing) and falling back to the saved nonInstancedPoint /
     * instancedPoint so projectile endpoints work even before any movement keyframe.
     */
    private LocalPoint resolveCharacterLocalPoint(Character character, WorldView worldView)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject != null)
        {
            LocalPoint lp = ckObject.getLocation();
            if (lp != null && lp.isInScene())
            {
                return lp;
            }
        }
        if (character.getInstancedPoint() != null)
        {
            LocalPoint lp = character.getInstancedPoint();
            if (lp.isInScene())
            {
                return lp;
            }
        }
        if (character.getNonInstancedPoint() != null)
        {
            WorldPoint wp = character.getNonInstancedPoint();
            if (worldView.isInstance())
            {
                Collection<WorldPoint> wps = WorldPoint.toLocalInstance(worldView, wp);
                if (!wps.isEmpty())
                {
                    wp = wps.iterator().next();
                }
            }
            LocalPoint lp = LocalPoint.fromWorld(worldView, wp);
            if (lp != null && lp.isInScene())
            {
                return lp;
            }
        }
        return null;
    }

    /**
     * Parses a ProjectileKeyFrame target string. Supports:
     *   "Player"                    -> single Character lookup
     *   "Player, NPC1, NPC2"        -> comma-separated list
     *   "folder:Foldername"         -> every Character recursively under that folder
     */
    private java.util.List<Character> resolveProjectileTargets(String spec)
    {
        java.util.ArrayList<Character> out = new java.util.ArrayList<>();
        if (spec == null)
        {
            return out;
        }
        String trimmed = spec.trim();
        if (trimmed.isEmpty())
        {
            return out;
        }

        if (trimmed.toLowerCase().startsWith("folder:"))
        {
            String folderName = trimmed.substring(7).trim();
            if (folderName.isEmpty())
            {
                return out;
            }
            com.creatorskit.swing.manager.ManagerTree tree = plugin.getCreatorsPanel().getToolBox().getManagerPanel().getManagerTree();
            javax.swing.tree.DefaultMutableTreeNode root = tree.getRootNode();
            java.util.ArrayList<javax.swing.tree.DefaultMutableTreeNode> all = new java.util.ArrayList<>();
            tree.getAllNodes(root, all);
            for (javax.swing.tree.DefaultMutableTreeNode node : all)
            {
                Object user = node.getUserObject();
                if (user instanceof com.creatorskit.swing.manager.Folder
                        && folderName.equalsIgnoreCase(((com.creatorskit.swing.manager.Folder) user).getName()))
                {
                    tree.getObjectPanelChildren(node, out);
                    break;
                }
            }
            return out;
        }

        for (String name : trimmed.split(","))
        {
            String n = name.trim();
            if (n.isEmpty())
            {
                continue;
            }
            for (Character c : plugin.getCharacters())
            {
                if (n.equals(c.getName()))
                {
                    if (!out.contains(c))
                    {
                        out.add(c);
                    }
                    break;
                }
            }
        }
        return out;
    }

    private void registerSpawnChanges(Character character)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        if (!character.isInScene())
        {
            character.setActive(false, character.isActive(), false, clientThread);
            return;
        }

        WorldView worldView = client.getTopLevelWorldView();
        boolean poh = MovementManager.useLocalLocations(worldView);

        SpawnKeyFrame spawnKeyFrame = (SpawnKeyFrame) character.getCurrentKeyFrame(KeyFrameType.SPAWN);
        if (spawnKeyFrame == null)
        {
            boolean active = character.isActive();
            character.setActive(active, active, active, clientThread);
            return;
        }

        boolean active = spawnKeyFrame.isSpawnActive();
        if (active)
        {
            if ((poh && character.isInPOH()) || (!poh && !character.isInPOH()))
            {
                character.setActive(true, true, true, clientThread);
                return;
            }

            character.setActive(false, true, false, clientThread);
            return;
        }

        character.setActive(false, false, false, clientThread);
    }

    public void register3DChanges(Character character)
    {
        WorldView worldView = client.getTopLevelWorldView();
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        double currentTime = timeSheetPanel.getCurrentTime();

        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        if (kf == null)
        {
            transform3D(worldView, character, currentTime);
            return;
        }

        MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
        if (keyFrame.getPlane() != worldView.getPlane())
        {
            character.setInScene(false);
            return;
        }

        int[][] path = keyFrame.getPath();
        int pathLength = path.length;

        double tileSpeed = keyFrame.getSpeed();
        double timePassed = timeSheetPanel.getCurrentTime() - keyFrame.getTick();
        int clientTicksPassed = (int) (timePassed * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH);
        double stepsComplete = timePassed * tileSpeed;
        int currentStep = (int) Math.floor(stepsComplete);
        double endSpeed = (pathLength - 1) - (Math.floor(((pathLength - 1) / tileSpeed)) * tileSpeed);
        double finalSpeed = tileSpeed;

        if (stepsComplete + endSpeed > pathLength - 1)
        {
            double jumps = (pathLength - 1) % tileSpeed;
            if (jumps != 0)
            {
                double ticksPreSlowdown = (pathLength - 1 - endSpeed) / tileSpeed;
                double stepsPreSlowdown = ticksPreSlowdown * tileSpeed;

                stepsComplete = (timePassed - ticksPreSlowdown) * endSpeed + stepsPreSlowdown;
                currentStep = (int) (stepsComplete);
                finalSpeed = endSpeed;
            }
        }

        if (currentStep > pathLength)
        {
            currentStep = pathLength;
        }

        keyFrame.setCurrentStep(currentStep);

        transform3DStatic(worldView,
                character,
                keyFrame,
                currentStep,
                stepsComplete,
                clientTicksPassed,
                finalSpeed);
    }

    private void registerOrientationChanges(Character character)
    {
        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
        if (kf == null)
        {
            return;
        }

        OrientationKeyFrame keyFrame = (OrientationKeyFrame) kf;
        double ticksPassed = timeSheetPanel.getCurrentTime() - keyFrame.getTick();
        double duration = keyFrame.getDuration();
        if (ticksPassed > duration)
        {
            return;
        }

        int orientation = getOrientationStatic(keyFrame);
        character.setOrientation(orientation);
    }

    public KeyFrameType findLastOrientation(Character character)
    {
        KeyFrame movement = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        KeyFrame orientation = character.getCurrentKeyFrame(KeyFrameType.ORIENTATION);

        if (movement == null && orientation == null)
        {
            return null;
        }

        if (movement == null)
        {
            return KeyFrameType.ORIENTATION;
        }

        if (orientation == null)
        {
            return KeyFrameType.MOVEMENT;
        }

        MovementKeyFrame mkf = (MovementKeyFrame) movement;
        OrientationKeyFrame okf = (OrientationKeyFrame) orientation;

        double oriEndTick = okf.getTick() + okf.getDuration();
        if (timeSheetPanel.getCurrentTime() <= oriEndTick)
        {
            return KeyFrameType.ORIENTATION;
        }

        double pathDuration = Math.ceil((mkf.getPath().length - 1) / mkf.getSpeed());
        double movementEndTick = mkf.getTick() + pathDuration;

        if (oriEndTick >= movementEndTick)
        {
            return KeyFrameType.ORIENTATION;
        }

        return KeyFrameType.MOVEMENT;
    }

    private void registerActiveAnimationChanges(Character character)
    {
        CKObject ckObject = character.getCkObject();

        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ANIMATION);
        if (kf == null)
        {
            // No keyframe active -- reset the multiplier and range so a previously-
            // active keyframe's settings don't leak into the spinner-driven defaults.
            ckObject.setAnimationSpeed(1.0);
            ckObject.setAnimationRange(0, 0, 0);
            ckObject.setHasAnimKeyFrame(false);
            int animId = (int) character.getAnimationSpinner().getValue();
            int animFrame = (int) character.getAnimationFrameSpinner().getValue();
            Animation current = ckObject.getAnimations()[0];
            if (current == null)
            {
                character.setAnimation(clientThread, client, plugin.getRandom(), AnimationType.ACTIVE, animId, animFrame, config.randomizeStartFrame(), true);
                return;
            }

            if (current.getId() != animId)
            {
                character.setAnimation(clientThread, client, plugin.getRandom(), AnimationType.ACTIVE, animId, animFrame, config.randomizeStartFrame(), true);
            }

            return;
        }

        AnimationKeyFrame keyFrame = (AnimationKeyFrame) kf;
        // Speed drives both the scrub-time frame computation (via setActiveAnimationFrame
        // -> getAnimFrame) AND the per-tick playback advance (via ckObject.tick reading
        // animationSpeed). Pre-2.3 saves serialize speed as 0; treat <= 0 as "use 1.0".
        double animSpeed = keyFrame.getSpeed();
        if (animSpeed <= 0) animSpeed = 1.0;
        ckObject.setAnimationSpeed(animSpeed);
        // lastFrame + pauseTicks are honoured during both scrub (via getAnimFrame's
        // range-mode overload) and playback (via CKAnimationController's range fields).
        // 0/0 keeps legacy semantics (full animation, no pause).
        int lastFrame = keyFrame.getLastFrame();
        int pauseTicks = keyFrame.getPauseTicks();
        ckObject.setAnimationRange(keyFrame.getStartFrame(), lastFrame, pauseTicks);
        setActiveAnimationFrame(ckObject, keyFrame.getActive(), timeSheetPanel.getCurrentTime(), keyFrame.getTick(), keyFrame.getStartFrame(), keyFrame.isLoop(), keyFrame.isFreeze(), false, animSpeed, lastFrame, pauseTicks);
    }

    private void registerModelChanges(Character character)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        ModelKeyFrame modelKeyFrame = (ModelKeyFrame) character.getCurrentKeyFrame(KeyFrameType.MODEL);

        if (modelKeyFrame == null)
        {
            plugin.setModel(character, character.isCustomMode(), (int) character.getModelSpinner().getValue());
            ckObject.setRadius((int) character.getRadiusSpinner().getValue());
            return;
        }

        ckObject.setRadius(modelKeyFrame.getRadius());

        if (modelKeyFrame.isUseCustomModel())
        {
            CustomModel customModel = modelKeyFrame.getCustomModel();
            if (customModel == null)
            {
                clientThread.invokeLater(() -> ckObject.setModel(client.loadModel(GOLDEN_CHIN)));
                return;
            }

            Model model = customModel.getModel();
            if (model == null)
            {
                return;
            }

            ckObject.setModel(model);
        }
        else
        {
            int modelId = modelKeyFrame.getModelId();
            if (modelId == -1)
            {
                modelId = 7699;
            }

            final int id = modelId;

            clientThread.invokeLater(() -> ckObject.setModel(client.loadModel(id)));
        }
    }

    private void registerSpotAnimChanges(Character character, KeyFrameType keyFrameType, double currentTime)
    {
        CKObject ckObject = character.getCkObject();
        CKObject spotAnim;
        if (keyFrameType == KeyFrameType.SPOTANIM)
        {
            spotAnim = character.getSpotAnim1();
        }
        else
        {
            spotAnim = character.getSpotAnim2();
        }

        SpotAnimKeyFrame spotAnimKeyFrame = (SpotAnimKeyFrame) character.getCurrentKeyFrame(keyFrameType);

        if (spotAnimKeyFrame == null)
        {
            clientThread.invokeLater(() ->
            {
                if (spotAnim == null)
                {
                    return;
                }

                spotAnim.setActive(false);
            });
            return;
        }

        LocalPoint lp = ckObject.getLocation();
        int plane = ckObject.getLevel();

        updateSpotAnim(keyFrameType, spotAnimKeyFrame.getSpotAnimId(), spotAnimKeyFrame.getHeight(), character, currentTime, spotAnimKeyFrame.getTick(), spotAnimKeyFrame.isLoop(), lp, plane, ckObject.getOrientation());
    }

    private void updateSpotAnim(KeyFrameType keyFrameType, int spotAnimId, int height, Character character, double currentTime, double startTick, boolean loop, LocalPoint lp, int plane, int orientation)
    {
        CKObject spotAnim;
        if (keyFrameType == KeyFrameType.SPOTANIM)
        {
            spotAnim = character.getSpotAnim1();
        }
        else
        {
            spotAnim = character.getSpotAnim2();
        }

        if (spotAnim == null)
        {
            if (spotAnimId == -1)
            {
                return;
            }
        }

        SpotanimData data = dataFinder.getSpotAnimData(spotAnimId);

        if (data != null)
        {
            ModelStats[] stats = dataFinder.findSpotAnim(data);
            clientThread.invokeLater(() ->
            {
                CKObject ckObject;
                if (spotAnim == null)
                {
                    ckObject = new CKObject(client);
                    client.registerRuneLiteObject(ckObject);
                    ckObject.setDrawFrontTilesFirst(true);
                    ckObject.setDespawnOnFinish(true);
                    ckObject.setHasAnimKeyFrame(true);
                    // Inherit the parent Character's current sub-tile offset
                    // and extraScale so a fresh spotanim drops in at the
                    // already-nudged-and-scaled position rather than the base
                    // tile centre. setOffsetX/Y/Z on the Character keeps live
                    // spotanims in sync; this branch covers the first frame.
                    ckObject.setOffsetX(character.getOffsetX());
                    ckObject.setOffsetY(character.getOffsetY());
                    ckObject.setOffsetZ(character.getOffsetZ());
                    ckObject.setExtraScale(character.getExtraScale());
                    character.setSpotAnim(ckObject, keyFrameType);
                }
                else
                {
                    ckObject = spotAnim;
                }

                ckObject.setOrientation(orientation);
                ckObject.setPlaying(playing);
                ckObject.setActive(false);
                ckObject.setActive(true);

                LightingStyle ls = LightingStyle.SPOTANIM;
                CustomLighting cl = new CustomLighting(ls.getAmbient() + data.getAmbient(), ls.getContrast() + data.getContrast(), ls.getX(), ls.getY(), ls.getZ());
                for (ModelStats ms : stats)
                {
                    ms.setTranslateZ(height);
                }

                Model model = modelUtilities.constructModelFromCache(stats, new int[0], false, LightingStyle.CUSTOM, cl);

                ckObject.setModel(model);
                setActiveAnimationFrame(ckObject, data.getAnimationId(), currentTime, startTick, 0, loop, false, true);
            });
        }
    }

    /** Back-compat shim -- spotanim / projectile path that doesn't carry a user-set speed. */
    public void setActiveAnimationFrame(CKObject ckObject, int animId, double currentTime, double startTime, int startFrame, boolean loop, boolean freeze, boolean despawnOnFinished)
    {
        setActiveAnimationFrame(ckObject, animId, currentTime, startTime, startFrame, loop, freeze, despawnOnFinished, 1.0);
    }

    public void setActiveAnimationFrame(CKObject ckObject, int animId, double currentTime, double startTime, int startFrame, boolean loop, boolean freeze, boolean despawnOnFinished, double animSpeed)
    {
        setActiveAnimationFrame(ckObject, animId, currentTime, startTime, startFrame, loop, freeze, despawnOnFinished, animSpeed, 0, 0);
    }

    public void setActiveAnimationFrame(CKObject ckObject, int animId, double currentTime, double startTime, int startFrame, boolean loop, boolean freeze, boolean despawnOnFinished, double animSpeed, int lastFrame, int pauseTicks)
    {
        if (animId == -1)
        {
            clientThread.invoke(() ->
            {
                ckObject.unsetAnimation(AnimationType.ACTIVE);
                ckObject.setLoop(false);
                ckObject.setFreeze(false);
                ckObject.setHasAnimKeyFrame(true);
                ckObject.setFinished(true);
            });
            return;
        }

        Animation[] animations = ckObject.getAnimations();
        Animation active = animations[0];
        if (active == null || active.getId() != animId)
        {
            clientThread.invoke(() ->
            {
                Animation animation = client.loadAnimation(animId);
                setActiveAnimationFrame(ckObject, animation, currentTime, startTime, startFrame, loop, freeze, despawnOnFinished, animSpeed, lastFrame, pauseTicks);
            });
            return;
        }

        clientThread.invoke(() ->
        {
            setActiveAnimationFrame(ckObject, active, currentTime, startTime, startFrame, loop, freeze, despawnOnFinished, animSpeed, lastFrame, pauseTicks);
        });
    }

    /** Back-compat shim. */
    public void setActiveAnimationFrame(CKObject ckObject, Animation animation, double currentTime, double startTime, int startFrame, boolean loop, boolean freeze, boolean despawnOnFinished)
    {
        setActiveAnimationFrame(ckObject, animation, currentTime, startTime, startFrame, loop, freeze, despawnOnFinished, 1.0, 0, 0);
    }

    public void setActiveAnimationFrame(CKObject ckObject, Animation animation, double currentTime, double startTime, int startFrame, boolean loop, boolean freeze, boolean despawnOnFinished, double animSpeed)
    {
        setActiveAnimationFrame(ckObject, animation, currentTime, startTime, startFrame, loop, freeze, despawnOnFinished, animSpeed, 0, 0);
    }

    public void setActiveAnimationFrame(CKObject ckObject, Animation animation, double currentTime, double startTime, int startFrame, boolean loop, boolean freeze, boolean despawnOnFinished, double animSpeed, int lastFrame, int pauseTicks)
    {
        int[] animFrame = getAnimFrame(animation, currentTime, startTime, startFrame, loop, animSpeed, lastFrame, pauseTicks);
        int frame = animFrame[0];
        int tick = animFrame[1];

        if (freeze)
        {
            frame = startFrame;
            tick = 0;
        }

        if (!freeze && animFrame[0] == -1)
        {
            ckObject.setFinished(true);
            ckObject.unsetAnimation(AnimationType.ACTIVE);
            ckObject.setLoop(false);
            ckObject.setFreeze(false);
            if (despawnOnFinished)
            {
                ckObject.setActive(false);
            }
        }
        else
        {
            ckObject.setAnimation(AnimationType.ACTIVE, animation);
            ckObject.setAnimationFrame(AnimationType.ACTIVE, frame, plugin.getRandom(), false, freeze);
            ckObject.tick(tick);
            ckObject.setLoop(loop);
            ckObject.setFinished(false);
            ckObject.setHasAnimKeyFrame(true);
        }
    }

    /** Back-compat shim. */
    public void setPoseAnimationFrame(CKObject ckObject, double currentTime, double startTime, boolean randomizeStartFrame, int startFrame)
    {
        setPoseAnimationFrame(ckObject, currentTime, startTime, randomizeStartFrame, startFrame, 1.0);
    }

    public void setPoseAnimationFrame(CKObject ckObject, double currentTime, double startTime, boolean randomizeStartFrame, int startFrame, double animSpeed)
    {
        Animation[] animations = ckObject.getAnimations();

        Animation pose = animations[1];
        if (pose != null && pose.getId() != -1)
        {
            int[] animFrame = getAnimFrame(pose, currentTime, startTime, startFrame, true, animSpeed);
            if (animFrame[0] != -1)
            {
                clientThread.invoke(() ->
                {
                    ckObject.setAnimationFrame(AnimationType.POSE, animFrame[0], plugin.getRandom(), randomizeStartFrame, false);
                    ckObject.tick(animFrame[1]);
                });
            }
        }
    }

    /**
     * Gets the current Animation Frame and Client Ticks that have passed that should be playing from the given time
     * @param animation The animation playing
     * @param currentTime The current time from which to calculate
     * @param startTime The start of the KeyFrame
     * @param startFrame The frame the animation started on
     * @param loop Whether the animation is looping
     * @return an int[] consisting of {frame from which to play, number of Client Ticks that have passed this frame}
     */
    private int[] getAnimFrame(Animation animation, double currentTime, double startTime, int startFrame, boolean loop)
    {
        return getAnimFrame(animation, currentTime, startTime, startFrame, loop, 1.0);
    }

    /**
     * Routing shim. Falls through to the legacy 6-arg implementation when the user
     * hasn't set lastFrame or pauseTicks (preserves the original loop-to-frame-0
     * semantics for old saves and for non-keyframe call sites like spotanims).
     * When either is set, switches to the range-based implementation in
     * {@link #getAnimFrameWithRange} where loop wraps back to {@code startFrame}
     * (not 0) and the last frame can dwell for {@code pauseTicks} client ticks
     * before each loop.
     */
    private int[] getAnimFrame(Animation animation, double currentTime, double startTime, int startFrame, boolean loop, double animSpeed, int lastFrame, int pauseTicks)
    {
        if (lastFrame <= 0 && pauseTicks <= 0)
        {
            return getAnimFrame(animation, currentTime, startTime, startFrame, loop, animSpeed);
        }
        return getAnimFrameWithRange(animation, currentTime, startTime, startFrame, loop, animSpeed, lastFrame, pauseTicks);
    }

    /**
     * Same as the 5-arg overload but with a multiplier on the elapsed-tick count so
     * animations can run faster or slower than native. {@code animSpeed} of 1.0
     * gives identical output to the legacy overload; 2.0 advances frames twice as
     * fast, 0.5 half as fast. Driven by AnimationKeyFrame.speed.
     */
    private int[] getAnimFrame(Animation animation, double currentTime, double startTime, int startFrame, boolean loop, double animSpeed)
    {
        double gameTicksPassed = currentTime - startTime;
        int clientTicksPassed = (int) (gameTicksPassed * 30 * animSpeed);

        if (animation.isMayaAnim())
        {
            int duration = animation.getDuration();
            int totalTicksPassed = clientTicksPassed + startFrame;
            if (loop)
            {
                int loops = (totalTicksPassed) / duration;
                totalTicksPassed = totalTicksPassed - loops * duration;
                if (totalTicksPassed < 0)
                {
                    totalTicksPassed = 0;
                }

                return new int[]{totalTicksPassed, 0};
            }

            if (totalTicksPassed >= duration)
            {
                return new int[]{-1, 0};
            }

            if (totalTicksPassed < 0)
            {
                totalTicksPassed = 0;
            }

            return new int[]{totalTicksPassed, 0};
        }

        int[] frameLengths = animation.getFrameLengths();

        int duration = 0;
        int startTick = 0;
        for (int i = 0; i < frameLengths.length; i++)
        {
            int frameLength = frameLengths[i];
            duration += frameLength;

            if (i < startFrame)
            {
                startTick += frameLength;
            }
        }

        int totalTicksPassed = clientTicksPassed + startTick;
        if (loop)
        {
            int loops = totalTicksPassed / duration;
            totalTicksPassed = totalTicksPassed - loops * duration;

            int framesPassed = 0;
            for (int i = 0; i < frameLengths.length; i++)
            {
                int frameLength = frameLengths[i];
                if (framesPassed + frameLength > totalTicksPassed)
                {
                    int ticksPassed = totalTicksPassed - framesPassed;
                    if (ticksPassed < 0)
                    {
                        ticksPassed = 0;
                    }

                    return new int[]{i, ticksPassed};
                }

                framesPassed += frameLength;
            }

            return new int[]{-1, 0};
        }

        if (totalTicksPassed >= duration)
        {
            return new int[]{-1, 0};
        }

        int framesPassed = 0;
        for (int i = 0; i < frameLengths.length; i++)
        {
            int frameLength = frameLengths[i];
            if (framesPassed + frameLength > totalTicksPassed)
            {
                int ticksPassed = totalTicksPassed - framesPassed;
                if (ticksPassed < 0)
                {
                    ticksPassed = 0;
                }

                return new int[]{i, ticksPassed};
            }

            framesPassed += frameLength;
        }

        return new int[]{-1, 0};
    }

    /**
     * Range-based animation frame computation. Plays only frames
     * {@code [startFrame, lastFrame]} of the animation; when looping, wraps back
     * to {@code startFrame} (not 0) and optionally dwells on the last frame for
     * {@code pauseTicks} client ticks before each loop.
     *
     * <p>Maya animations don't have per-frame lengths (frame index == elapsed
     * tick) so we fall back to the legacy maya path; lastFrame / pauseTicks are
     * silently ignored there.
     */
    private int[] getAnimFrameWithRange(Animation animation, double currentTime, double startTime, int startFrame, boolean loop, double animSpeed, int lastFrame, int pauseTicks)
    {
        if (animation.isMayaAnim())
        {
            // Maya path: range / pause not supported (no frame-length array).
            return getAnimFrame(animation, currentTime, startTime, startFrame, loop, animSpeed);
        }

        double gameTicksPassed = currentTime - startTime;
        int clientTicksPassed = (int) (gameTicksPassed * 30 * animSpeed);

        int[] frameLengths = animation.getFrameLengths();
        int totalFrames = frameLengths.length;
        if (totalFrames == 0)
        {
            return new int[]{-1, 0};
        }

        // lastFrame == 0 (or out of range) means "use the natural last frame".
        int effectiveLast = (lastFrame > 0 && lastFrame < totalFrames) ? lastFrame : totalFrames - 1;
        int effectiveFirst = Math.max(0, Math.min(startFrame, effectiveLast));

        int rangeDuration = 0;
        for (int i = effectiveFirst; i <= effectiveLast; i++)
        {
            rangeDuration += frameLengths[i];
        }
        int safePause = Math.max(0, pauseTicks);

        if (loop)
        {
            int loopLength = rangeDuration + safePause;
            if (loopLength <= 0)
            {
                loopLength = 1;
            }
            int timeIntoLoop = clientTicksPassed % loopLength;
            if (timeIntoLoop < 0)
            {
                timeIntoLoop += loopLength;
            }
            if (timeIntoLoop >= rangeDuration)
            {
                // Pause phase -- dwell on the last frame for safePause ticks.
                return new int[]{effectiveLast, timeIntoLoop - rangeDuration};
            }
            int accumulated = 0;
            for (int i = effectiveFirst; i <= effectiveLast; i++)
            {
                int frameLength = frameLengths[i];
                if (accumulated + frameLength > timeIntoLoop)
                {
                    return new int[]{i, timeIntoLoop - accumulated};
                }
                accumulated += frameLength;
            }
            return new int[]{effectiveLast, 0};
        }

        // Non-loop: play the range once, then signal end.
        if (clientTicksPassed >= rangeDuration)
        {
            return new int[]{-1, 0};
        }
        if (clientTicksPassed < 0)
        {
            clientTicksPassed = 0;
        }
        int accumulated = 0;
        for (int i = effectiveFirst; i <= effectiveLast; i++)
        {
            int frameLength = frameLengths[i];
            if (accumulated + frameLength > clientTicksPassed)
            {
                return new int[]{i, clientTicksPassed - accumulated};
            }
            accumulated += frameLength;
        }
        return new int[]{-1, 0};
    }
}
