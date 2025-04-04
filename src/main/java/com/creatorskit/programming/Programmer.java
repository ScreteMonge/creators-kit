package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.LocationOption;
import com.creatorskit.models.*;
import com.creatorskit.models.datatypes.SpotanimData;
import com.creatorskit.programming.orientation.Orientation;
import com.creatorskit.programming.orientation.OrientationAction;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.attributes.OrientationInstruction;
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
    private final ClientThread clientThread;
    private final CreatorsPlugin plugin;
    private final TimeSheetPanel timeSheetPanel;
    private final DataFinder dataFinder;
    private int clientTickAtLastProgramTick = 0;
    private final int GOLDEN_CHIN = 29757;
    private final int TILE_LENGTH = 128;
    private final int TILE_DIAGONAL = 181; //Math.sqrt(Math.pow(128, 2) + Math.pow(128, 2))
    private final double TURN_RATE = 256 / 7.5; //In JUnits/clientTick; Derived by examining turn rates in game

    @Getter
    @Setter
    private boolean playing = false;
    private boolean triggerPause = false;

    @Inject
    public Programmer(Client client, ClientThread clientThread, CreatorsPlugin plugin, TimeSheetPanel timeSheetPanel, DataFinder dataFinder)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.timeSheetPanel = timeSheetPanel;
        this.dataFinder = dataFinder;
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
                continue;
            }

            KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
            if (kf == null)
            {
                transform3D(character, currentClientTick);
                continue;
            }

            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            if (keyFrame.getPlane() != worldView.getPlane())
            {
                continue;
            }

            int[][] path = keyFrame.getPath();
            int currentStep = keyFrame.getCurrentStep();
            int pathLength = path.length;
            if (currentStep >= pathLength || currentStep == -1)
            {
                transform3D(character, currentClientTick);
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
     * Animates the Character. For intended use when no MovementKeyFrame exists
     * @param character the Character to animate
     */
    public void transform3D(Character character, double currentClientTick)
    {
        setAnimation(character, false, 0, 0);
        setOrientation(character, currentClientTick);
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
            return;
        }

        if (mkf.getPlane() != worldView.getPlane())
        {
            return;
        }

        double turnRate = mkf.getTurnRate();
        if (turnRate == -1)
        {
            turnRate = TURN_RATE;
        }

        MovementComposition mc = getMovementComposition(worldView, character, mkf, currentStep, stepsComplete, orientationAction, clientTicksPassed, turnRate);
        if (mc == null)
        {
            setAnimation(character, false, 0, 0);
            setOrientation(character, currentClientTick);
            return;
        }

        setLocation(character, mkf, mc);

        int orientation = ckObject.getOrientation();
        int orientationGoal = mc.getOrientationGoal();
        int difference = Orientation.subtract(orientationGoal, orientation);

        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ORIENTATION);
        if (kf == null)
        {
            setOrientation(character, mc, orientationGoal, difference, stepsComplete, mkf.getSpeed(), turnRate);
            setAnimation(character, mc.isMoving(), difference, finalSpeed);
            return;
        }

        OrientationKeyFrame okf = (OrientationKeyFrame) kf;
        OrientationInstruction instruction = findLastOrientation(mkf, okf, currentClientTick);
        if (instruction.getType() == KeyFrameType.ORIENTATION)
        {
            setOrientation(character, okf, mkf.getSpeed());
            setAnimation(character, mc.isMoving(), difference, finalSpeed);
            return;
        }

        if (instruction.isSetOrientation())
        {
            mc.setOrientationAction(OrientationAction.SET);
        }

        setOrientation(character, mc, orientationGoal, difference, stepsComplete, mkf.getSpeed(), turnRate);
        setAnimation(character, mc.isMoving(), difference, finalSpeed);
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
            return;
        }

        if (mkf.getPlane() != worldView.getPlane())
        {
            return;
        }

        double turnRate = mkf.getTurnRate();
        if (turnRate == -1)
        {
            turnRate = TURN_RATE;
        }

        MovementComposition mc = getMovementComposition(worldView, character, mkf, currentStep, stepsComplete, OrientationAction.SET, clientTicksPassed, turnRate);

        int orientation = ckObject.getOrientation();
        int orientationGoal;
        int difference = 0;
        boolean isMoving = false;

        if (mc == null)
        {
            setAnimation(character, false, 0, 0);
        }
        else
        {
            setLocation(character, mkf, mc);
            orientationGoal = mc.getOrientationGoal();
            difference = Orientation.subtract(orientationGoal, orientation);
            isMoving = mc.isMoving();
        }

        KeyFrameType orientationDeterminant = findLastOrientation(character);

        if (orientationDeterminant == KeyFrameType.ORIENTATION)
        {
            setOrientationStatic(character);
            setAnimation(character, isMoving, difference, finalSpeed);
            return;
        }

        if (orientationDeterminant == KeyFrameType.MOVEMENT)
        {
            if (mc == null)
            {
                setAnimation(character, false, 0, 0);
                return;
            }

            orientationGoal = mc.getOrientationGoal();
            setOrientation(character, mc, orientationGoal, difference, stepsComplete, mkf.getSpeed(), turnRate);
            setAnimation(character, mc.isMoving(), difference, finalSpeed);
            return;
        }

        setAnimation(character, false, 0, 0);
    }

    private void setLocation(Character character, MovementKeyFrame keyFrame, MovementComposition mc)
    {
        LocalPoint lp = mc.getLocalPoint();
        if (lp == null)
        {
            character.setVisible(false, clientThread);
            return;
        }

        character.setVisible(true, clientThread);
        character.getCkObject().setLocation(lp, keyFrame.getPlane());
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
        character.getCkObject().setOrientation(orientation);
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
        if (turnRate == -1)
        {
            turnRate = TURN_RATE;
        }

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
            return;
        }

        OrientationKeyFrame keyFrame = (OrientationKeyFrame) kf;
        int orientation = getOrientationStatic(keyFrame);
        character.getCkObject().setOrientation(orientation);
    }

    /**
     * Gets the orientation dictated by the current OrientationKeyFrame for the current tick. Intended for use when setting the time, not running the program
     * @param keyFrame the keyframe to examine
     * @return the appropriate orientation for the given keyframe
     */
    private int getOrientationStatic(OrientationKeyFrame keyFrame)
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
        if (turnRate == -1)
        {
            turnRate = TURN_RATE;
        }

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
     * Sets the orientation of the character based on its orientation keyframe
     * Intended for use while playing the programmer
     * @param character the character to set orientation
     * @param speed the movement speed (in gameTicks) determined by the movement keyframe
     */
    private void setOrientation(Character character, OrientationKeyFrame keyFrame, double speed)
    {
        CKObject ckObject = character.getCkObject();
        int orientationGoal = keyFrame.getEnd();
        int current = ckObject.getOrientation();
        int difference = Orientation.subtract(orientationGoal, current);
        double turnRate = keyFrame.getTurnRate();

        if (speed == 0)
        {
            speed = 1;
        }

        if (turnRate == -1)
        {
            turnRate = TURN_RATE;
        }

        if (difference != 0)
        {
            int orientation = ckObject.getOrientation();
            int turnSpeed = (int) (speed * turnRate);

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

            ckObject.setOrientation(newOrientation);
        }
    }

    /**
     * Sets the orientation of the character based on its movement keyframe
     * Intended for use while playing the programmer
     * @param character the character to modify
     * @param mc the MovementComposition, based on its movement keyframe
     * @param orientationGoal the orientation end-goal, determined by the trajectory of movement
     * @param difference the difference between the current orientation and end goal orientation
     * @param stepsComplete the number of steps complete
     * @param speed the speed at which the Character moves
     * @param turnRate the rate at which the Character should turn
     */
    private void setOrientation(Character character, MovementComposition mc, int orientationGoal, int difference, double stepsComplete, double speed, double turnRate)
    {
        OrientationAction orientationAction = mc.getOrientationAction();
        CKObject ckObject = character.getCkObject();
        if (orientationAction == OrientationAction.SET)
        {
            if (ckObject.getOrientation() != orientationGoal)
            {
                ckObject.setOrientation(orientationGoal);
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
            int turnSpeed = (int) (speed * turnRate);

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

            ckObject.setOrientation(newOrientation);
        }
    }

    private void setAnimation(Character character, boolean isMoving, int orientationDifference, double speed)
    {
        CKObject ckObject = character.getCkObject();

        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ANIMATION);
        if (kf == null)
        {
            return;
        }

        AnimationKeyFrame keyFrame = (AnimationKeyFrame) kf;
        int active = keyFrame.getActive();
        int pose = getPoseAnimation(keyFrame, isMoving, orientationDifference, speed);

        clientThread.invokeLater(() ->
        {
            Animation[] animations = ckObject.getAnimations();
            Animation currentActive = animations[0];
            Animation currentPose = animations[1];

            if (active == -1)
            {
                if (currentActive != null && currentActive.getId() != -1)
                {
                    ckObject.unsetAnimation(AnimationType.ACTIVE);
                    ckObject.setFinished(true);
                    ckObject.setLoop(false);
                }
            }

            if (active != -1)
            {
                if (currentActive != ckObject.getActiveAnimation())
                {
                    if (!ckObject.isFinished())
                    {
                        if (currentActive == null || currentActive.getId() != active)
                        {
                            ckObject.setAnimation(AnimationType.ACTIVE, active);
                            ckObject.setAnimationFrame(AnimationType.ACTIVE, keyFrame.getStartFrame(), false);
                            ckObject.setLoop(keyFrame.isLoop());
                        }
                    }
                }
            }

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
                    ckObject.setAnimationFrame(AnimationType.POSE, keyFrame.getStartFrame(), false);

                }
            }
        });
    }

    private int getPoseAnimation(AnimationKeyFrame keyFrame, boolean isMoving, int orientationDifference, double speed)
    {
        if (speed == 0 || !isMoving)
        {
            int animId = keyFrame.getIdle();
            /*
            if (orientationDifference > 0)
            {
                int idleRight = keyFrame.getIdleRight();
                if (idleRight != -1)
                {
                    animId = idleRight;
                }
            }

            if (orientationDifference < 0)
            {
                int idleLeft = keyFrame.getIdleLeft();
                if (idleLeft != -1)
                {
                    animId = idleLeft;
                }
            }

             */

            return animId;
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
                return new MovementComposition(false, last, OrientationAction.FREEZE, 0);
            }

            double directionX = last.getSceneX() - secondLast.getSceneX();
            double directionY = last.getSceneY() - secondLast.getSceneY();
            double angle = Orientation.radiansToJAngle(Math.atan(directionY / directionX), directionX, directionY);
            return new MovementComposition(false, last, OrientationAction.SET, (int) angle);
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

            double angle = getOrientationDifference(start, destination);
            int startX = start.getX();
            int startY = start.getY();

            int changeX = (int) (subSteps * Orientation.orientationX(angle));
            int currentX = startX + changeX;

            int changeY = (int) (subSteps * Orientation.orientationY(angle));
            int currentY = startY + changeY;

            LocalPoint lp = new LocalPoint(currentX, currentY, worldView);
            if (orientationAction == OrientationAction.ADJUST)
            {
                return new MovementComposition(true, lp, OrientationAction.ADJUST, (int) angle);
            }

            int orientationFromTick = getOrientationFromTick(previous, start, angle, clientTicksPassed, currentStep, keyFrame.getSpeed(), turnRate);
            return new MovementComposition(true, lp, OrientationAction.SET, orientationFromTick);
        }

        if (start == null)
        {
            if (previous == null)
            {
                return new MovementComposition(false, destination, OrientationAction.FREEZE, 0);
            }

            double directionX = destination.getSceneX() - previous.getSceneX();
            double directionY = destination.getSceneY() - previous.getSceneY();
            double angle = Orientation.radiansToJAngle(Math.atan(directionY / directionX), directionX, directionY);

            return new MovementComposition(false, destination, orientationAction, (int) angle);
        }

        if (previous == null)
        {
            return new MovementComposition(false, start, OrientationAction.FREEZE, 0);
        }

        double angle = getOrientationDifference(previous, start);
        return new MovementComposition(false, start, OrientationAction.SET, (int) angle);
    }

    private int getOrientationFromTick(LocalPoint previous, LocalPoint start, double angle, int clientTicksPassed, int currentStep, double speed, double turnRate)
    {
        if (previous == null)
        {
            return (int) angle;
        }

        double originalAngle = getOrientationDifference(previous, start);


        int angleDifference = Orientation.subtract((int) angle, (int) originalAngle);
        if (angleDifference == 0)
        {
            return (int) angle;
        }

        double ticksSinceLastStep = clientTicksPassed - (((double) currentStep) * Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH);
        double change = speed * turnRate * ticksSinceLastStep;

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

    private double getOrientationDifference(LocalPoint firstPoint, LocalPoint secondPoint)
    {
        int secondX = secondPoint.getX();
        int secondY = secondPoint.getY();
        int firstX = firstPoint.getX();
        int firstY = firstPoint.getY();
        double differenceX = secondX - firstX;
        double differenceY = secondY - firstY;
        return Orientation.radiansToJAngle(Math.atan(differenceY / differenceX), differenceX, differenceY);
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
            wp = wps.iterator().next();
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

    public void togglePlay()
    {
        togglePlay(!playing);
    }

    public void togglePlay(boolean play)
    {
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
        register3DChanges(character);
        registerModelChanges(character);
        registerSpotAnimChanges(character, KeyFrameType.SPOTANIM, tick);
        registerSpotAnimChanges(character, KeyFrameType.SPOTANIM2, tick);
        registerSpawnChanges(character);
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
                    character.resetMovementKeyFrame(client.getGameCycle(), currentTime);
                    register3DChanges(character);
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
                    registerAnimationChanges(character);
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


            KeyFrame currentSpotAnim = currentFrames[KeyFrameType.getIndex(KeyFrameType.SPOTANIM)];
            double lastSpotAnimTick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentSpotAnim != null)
            {
                lastSpotAnimTick = currentSpotAnim.getTick();
            }

            KeyFrame nextSpotAnim = character.findNextKeyFrame(KeyFrameType.SPOTANIM, lastSpotAnimTick);
            if (nextSpotAnim != null)
            {
                if (nextSpotAnim.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextSpotAnim, KeyFrameType.SPOTANIM);
                }
            }


            KeyFrame currentSpotAnim2 = currentFrames[KeyFrameType.getIndex(KeyFrameType.SPOTANIM2)];
            double lastSpotAnim2Tick = -TimeSheetPanel.ABSOLUTE_MAX_SEQUENCE_LENGTH;
            if (currentSpotAnim2 != null)
            {
                lastSpotAnim2Tick = currentSpotAnim2.getTick();
            }

            KeyFrame nextSpotAnim2 = character.findNextKeyFrame(KeyFrameType.SPOTANIM2, lastSpotAnim2Tick);
            if (nextSpotAnim2 != null)
            {
                if (nextSpotAnim2.getTick() <= currentTime)
                {
                    character.setCurrentKeyFrame(nextSpotAnim2, KeyFrameType.SPOTANIM2);
                }
            }
        }
    }

    private void registerSpawnChanges(Character character)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        SpawnKeyFrame spawnKeyFrame = (SpawnKeyFrame) character.getCurrentKeyFrame(KeyFrameType.SPAWN);
        if (spawnKeyFrame == null)
        {
            return;
        }

        clientThread.invokeLater(() -> ckObject.setActive(spawnKeyFrame.isSpawnActive()));
    }

    public void register3DChanges(Character character)
    {
        WorldView worldView = client.getTopLevelWorldView();
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.MOVEMENT);
        if (kf == null)
        {
            kf = character.findNextKeyFrame(KeyFrameType.MOVEMENT, timeSheetPanel.getCurrentTime());
            if (kf == null)
            {
                plugin.setLocation(character, false, true, LocationOption.TO_SAVED_LOCATION);
                return;
            }

            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            if (keyFrame.getPlane() != worldView.getPlane())
            {
                return;
            }

            int[][] path = keyFrame.getPath();
            if (path.length == 0)
            {
                plugin.setLocation(character, false, true, LocationOption.TO_SAVED_LOCATION);
                return;
            }

            LocalPoint lp = getLocation(worldView, keyFrame, 0);
            ckObject.setLocation(lp, keyFrame.getPlane());
            return;
        }

        MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
        if (keyFrame.getPlane() != worldView.getPlane())
        {
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
        CKObject ckObject = character.getCkObject();

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
        ckObject.setOrientation(orientation);
    }

    private KeyFrameType findLastOrientation(Character character)
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

    private void registerAnimationChanges(Character character)
    {
        CKObject ckObject = character.getCkObject();

        KeyFrame kf = character.getCurrentKeyFrame(KeyFrameType.ANIMATION);
        if (kf == null)
        {
            int animId = (int) character.getAnimationSpinner().getValue();
            Animation current = ckObject.getAnimations()[0];
            if (current == null)
            {
                plugin.setAnimation(character, animId);
                return;
            }

            if (current.getId() != animId)
            {
                plugin.setAnimation(character, animId);
            }

            return;
        }

        AnimationKeyFrame keyFrame = (AnimationKeyFrame) kf;
        clientThread.invokeLater(() ->
        {
            ckObject.setLoop(keyFrame.isLoop());
            ckObject.setActiveAnimation(client.loadAnimation(keyFrame.getActive()));
            ckObject.setFinished(false);
            setAnimationFrames(ckObject, timeSheetPanel.getCurrentTime(), keyFrame.getTick(), keyFrame.getStartFrame(), keyFrame.isLoop());

            if (playing)
            {
                character.play();
            }
            else
            {
                character.pause();
            }
        });
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
            return;
        }

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
                character.setSpotAnim(null, keyFrameType);
            });
            return;
        }

        LocalPoint lp = ckObject.getLocation();
        int plane = ckObject.getLevel();

        updateSpotAnim(keyFrameType, spotAnimKeyFrame.getSpotAnimId(), character, currentTime, spotAnimKeyFrame.getTick(), spotAnimKeyFrame.isLoop(), lp, plane);
    }

    private void updateSpotAnim(KeyFrameType keyFrameType, int spotAnimId, Character character, double currentTime, double startTick, boolean loop, LocalPoint lp, int plane)
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
            if (spotAnimId > -1)
            {
                SpotanimData data = dataFinder.getSpotAnimData(spotAnimId);

                if (data != null)
                {
                    ModelStats[] stats = dataFinder.findSpotAnim(data);
                    clientThread.invokeLater(() ->
                    {
                        LightingStyle ls = LightingStyle.SPOTANIM;
                        CustomLighting cl = new CustomLighting(ls.getAmbient() + data.getAmbient(), ls.getContrast() + data.getContrast(), ls.getX(), ls.getY(), ls.getZ());
                        Model model = plugin.constructModelFromCache(stats, new int[0], false, LightingStyle.CUSTOM, cl);

                        CKObject ckObject = new CKObject(client);
                        client.registerRuneLiteObject(ckObject);
                        ckObject.setModel(model);
                        ckObject.setAnimation(AnimationType.ACTIVE, data.getAnimationId());
                        ckObject.setLocation(lp, plane);
                        ckObject.setActive(true);
                        ckObject.setDrawFrontTilesFirst(true);
                        setActiveAnimationFrame(ckObject, currentTime, startTick, 0, loop);
                        character.setSpotAnim(ckObject, keyFrameType);
                    });
                }
            }
        }
        else
        {
            if (spotAnimId > -1)
            {
                clientThread.invokeLater(() ->
                {
                    setActiveAnimationFrame(spotAnim, currentTime, startTick, 0, loop);
                });
            }
            else
            {
                character.setSpotAnim1(null);
            }
        }
    }

    public void setAnimationFrames(CKObject ckObject, double currentTime, double startTime, int startFrame, boolean loop)
    {
        setActiveAnimationFrame(ckObject, currentTime, startTime, startFrame, loop);
        setPoseAnimationFrame(ckObject, currentTime, startTime, startFrame, loop);
    }

    public void setActiveAnimationFrame(CKObject ckObject, double currentTime, double startTime, int startFrame, boolean loop)
    {
        Animation[] animations = ckObject.getAnimations();

        Animation active = animations[0];

        Animation savedAnimation = ckObject.getActiveAnimation();
        if (active == null)
        {
            if (savedAnimation == null || savedAnimation.getId() == -1)
            {
                ckObject.setFinished(true);
            }
            else
            {
                setActiveAnimationFrame(ckObject, savedAnimation, currentTime, startTime, startFrame, loop);
            }
        }

        if (active != null)
        {
            Animation nextAnimation = active;
            if (active.getId() == -1)
            {
                if (savedAnimation == null)
                {
                    ckObject.setFinished(true);
                }
                else
                {
                    nextAnimation = savedAnimation;
                }
            }

            setActiveAnimationFrame(ckObject, nextAnimation, currentTime, startTime, startFrame, loop);
        }
    }

    public void setActiveAnimationFrame(CKObject ckObject, Animation animation, double currentTime, double startTime, int startFrame, boolean loop)
    {
        int[] animFrame = getAnimFrame(animation, currentTime, startTime, startFrame, loop);
        if (animFrame[0] == -1)
        {
            ckObject.setFinished(true);
            ckObject.unsetAnimation(AnimationType.ACTIVE);
        }
        else
        {
            clientThread.invokeLater(() ->
            {
                ckObject.setActive(true);
                ckObject.setAnimation(AnimationType.ACTIVE, animation);
                ckObject.setAnimationFrame(AnimationType.ACTIVE, animFrame[0], false);
                ckObject.tick(animFrame[1]);
                ckObject.setLoop(loop);
                ckObject.setFinished(false);
            });
        }
    }

    public void setPoseAnimationFrame(CKObject ckObject, double currentTime, double startTime, int startFrame, boolean loop)
    {
        Animation[] animations = ckObject.getAnimations();

        Animation pose = animations[1];
        if (pose != null && pose.getId() != -1)
        {
            int[] animFrame = getAnimFrame(pose, currentTime, startTime, startFrame, loop);
            if (animFrame[0] != -1)
            {
                clientThread.invokeLater(() ->
                {
                    ckObject.setActive(true);
                    ckObject.setAnimationFrame(AnimationType.POSE, animFrame[0], false);
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
        double gameTicksPassed = currentTime - startTime;
        int clientTicksPassed = (int) (gameTicksPassed * 30);

        if (animation.isMayaAnim())
        {
            int duration = animation.getDuration();
            int totalTicksPassed = clientTicksPassed + startFrame;
            if (loop)
            {
                int loops = (totalTicksPassed) / duration;
                totalTicksPassed = totalTicksPassed - loops * duration;

                return new int[]{totalTicksPassed, 0};
            }

            if (totalTicksPassed >= duration)
            {
                return new int[]{-1, 0};
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
                return new int[]{i, ticksPassed};
            }

            framesPassed += frameLength;
        }

        return new int[]{-1, 0};
    }
}
