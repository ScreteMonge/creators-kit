package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.models.*;
import com.creatorskit.models.datatypes.SpotanimData;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.util.ArrayList;

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
    private final int MOVEMENT_RATE = 30;

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
            if (clientTickAtLastProgramTick == 0 && triggerPause)
            {
                pause();
                return;
            }

            clientTickAtLastProgramTick++;
            if (clientTickAtLastProgramTick >= 3)
            {
                clientTickAtLastProgramTick = 0;
                incrementSubTime();
            }

            updateCharacterLocations();
        }
    }

    private void updateCharacterLocations()
    {
        WorldView worldView = client.getTopLevelWorldView();

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
                continue;
            }

            MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
            if (keyFrame.getPlane() != worldView.getPlane())
            {
                continue;
            }

            int[][] path = keyFrame.getPath();
            int currentStep = keyFrame.getCurrentStep();
            if (currentStep >= path.length || currentStep == -1)
            {
                continue;
            }

            double speed = keyFrame.getSpeed() / Constants.GAME_TICK_LENGTH * Constants.CLIENT_TICK_LENGTH;
            double ticksPassed = client.getGameCycle() - keyFrame.getStepClientTick();
            double stepsComplete = ticksPassed * speed;

            //if animKeyFrame = stallAnim, cancel step progression
            keyFrame.setCurrentStep((int) stepsComplete);
            setLocation(worldView, character, keyFrame, currentStep, stepsComplete);

            /*
            moveLocation(worldView,
                    character,
                    keyFrame,
                    currentStep,
                    stepsComplete);

             */
        }
    }

    /**
     * Set the location for the Character, with the intent to move it to the next subtile based on its current location and the predefined path
     * @param worldView the current worldview
     * @param character the Character to move
     * @param keyFrame the MovementKeyFrame from which the location is being drawn
     * @param currentStep the number of whole steps that have already been performed
     * @param stepsComplete the number of whole + sub steps that have already been performed
     */
    private void moveLocation(WorldView worldView, Character character, MovementKeyFrame keyFrame, int currentStep, double stepsComplete)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        LocalPoint start = getLocation(worldView, keyFrame, currentStep);
        LocalPoint current = ckObject.getLocation();
        LocalPoint destination = getLocation(worldView, keyFrame, currentStep + 1);

        if (!isValidPoint(start) && !isValidPoint(destination))
        {
            character.setVisible(false, clientThread);
            return;
        }

        if (!isValidPoint(current))
        {
            current = destination;
        }

        character.setVisible(true, clientThread);

        if (isValidPoint(start) && isValidPoint(destination))
        {
            int startX = start.getX();
            int startY = start.getY();
            int currentX = current.getX();
            int currentY = current.getY();
            int destX = destination.getX();
            int destY = destination.getY();

            double directionX = destX - startX;
            double directionY = destY - startY;
            double angle = Orientation.radiansToJAngle(Math.atan(directionY / directionX), directionX, directionY);

            double tileLength = TILE_LENGTH;
            if (Orientation.isDiagonal(angle))
            {
                tileLength = TILE_DIAGONAL;
            }

            double difference = Math.sqrt(Math.pow(destX - currentX, 2) + Math.pow(destY - currentY, 2)) + tileLength * (stepsComplete - currentStep);
            double endSpeed = difference / MOVEMENT_RATE;

            int changeX = (int) (endSpeed * Orientation.orientationX(angle));
            int diffX = Math.abs(destX - currentX);
            currentX = currentX + changeX;
            if (Math.abs(changeX) > diffX)
            {
                currentX = destX;
            }

            int changeY = (int) (endSpeed * Orientation.orientationY(angle));
            int diffY = Math.abs(destY - currentY);
            currentY = currentY + changeY;
            if (Math.abs(changeY) > diffY)
            {
                currentY = destY;
            }

            if (currentX == destX && currentY == destY)
            {
                keyFrame.setCurrentStep(currentStep + 1);
            }

            LocalPoint finalPoint = new LocalPoint(currentX, currentY, worldView);
            ckObject.setLocation(finalPoint, keyFrame.getPlane());
            return;
        }

        if (isValidPoint(start))
        {
            ckObject.setLocation(start, keyFrame.getPlane());
            return;
        }

        ckObject.setLocation(destination, keyFrame.getPlane());
    }

    public boolean isValidPoint(LocalPoint lp)
    {
        return lp != null && lp.isInScene();
    }

    /**
     * Set the location for the Character, with the intent to move it to the next subtile based on its current location and the predefined path
     * @param worldView the current worldview
     * @param character the Character to move
     * @param keyFrame the MovementKeyFrame from which the location is being drawn
     * @param currentStep the number of whole steps that have already been performed
     * @param stepsComplete the number of whole + sub steps that have already been performed
     */
    public void setLocation(WorldView worldView, Character character, MovementKeyFrame keyFrame, int currentStep, double stepsComplete)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return;
        }

        if (keyFrame.getPlane() != worldView.getPlane())
        {
            return;
        }

        LocalPoint lp = getLocation(worldView, character, keyFrame, currentStep, stepsComplete);
        if (lp == null)
        {
            character.setVisible(false, clientThread);
            return;
        }

        ckObject.setLocation(lp, keyFrame.getPlane());
        character.setVisible(true, clientThread);
    }

    /**
     * Gets the LocalPoint corresponding to the exact tick for the current time, including both full tile and sub tile coordinates.
     * Used primarily when setting the time in the TimeLine or when setting Location via hotkeys.
     * @param worldView the current WorldView
     * @param character the character to find the location for
     * @param keyFrame the keyframe from the character to get the path from
     * @param currentStep the current whole number step
     * @param stepsComplete the current non-whole number step
     * @return the LocalPoint corresponding to the current tick along the character's currently defined path
     */
    public LocalPoint getLocation(WorldView worldView, Character character, MovementKeyFrame keyFrame, int currentStep, double stepsComplete)
    {
        CKObject ckObject = character.getCkObject();
        if (ckObject == null)
        {
            return null;
        }

        int pathLength = keyFrame.getPath().length;
        if (currentStep >= pathLength)
        {
            return getLocation(worldView, keyFrame, pathLength - 1);
        }

        LocalPoint start = getLocation(worldView, keyFrame, currentStep);
        LocalPoint destination = getLocation(worldView, keyFrame, currentStep + 1);

        if (start == null && destination == null)
        {
            return null;
        }

        if (start != null && destination != null)
        {
            int startX = start.getX();
            int startY = start.getY();
            int destX = destination.getX();
            int destY = destination.getY();

            double percentComplete = stepsComplete - currentStep;

            double directionX = destX - startX;
            double directionY = destY - startY;
            double angle = Orientation.radiansToJAngle(Math.atan(directionY / directionX), directionX, directionY);

            double subSteps = percentComplete * TILE_LENGTH;
            //This algorithm assumes that, if diagonal, the character moves 1 space vertically and 1 space horizontally
            //To make it work with non 45 degree angles, appropriate trigonometry must be used

            int changeX = (int) (subSteps * Orientation.orientationX(angle));
            int currentX = startX + changeX;

            int changeY = (int) (subSteps * Orientation.orientationY(angle));
            int currentY = startY + changeY;

            return new LocalPoint(currentX, currentY, worldView);
        }

        if (start == null)
        {
            return destination;
        }

        return start;
    }

    /**
     * Gets the LocalPoint of a specific integer step. Used primarily to assess the start and end tiles of a path in order to calculate the movement between
     * @param keyFrame the keyframe for which to grab the path
     * @param step the number of whole number steps complete
     * @return the LocalPoint pertaining to the current step for the keyframe
     */
    private LocalPoint getLocation(WorldView worldView, MovementKeyFrame keyFrame, int step)
    {
        boolean isInPOH = MovementManager.isInPOH(worldView);
        if (keyFrame.isPoh() != isInPOH)
        {
            return null;
        }

        int[][] path = keyFrame.getPath();
        if (step >= path.length || step == -1)
        {
            return null;
        }

        int[] currentStep = path[step];

        if (isInPOH)
        {
            LocalPoint lp = new LocalPoint(currentStep[0], currentStep[1], worldView);
            if (!lp.isInScene())
            {
                return null;
            }

            return lp;
        }

        LocalPoint lp = LocalPoint.fromWorld(worldView, currentStep[0], currentStep[1]);
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
            //timeSheetPanel.setCurrentTime(Math.floor(currentTime), false);

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

    private void pause()
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
        registerMovementChanges(character);
        registerModelChanges(character);
        registerSpotAnimChanges(character, KeyFrameType.SPOTANIM, tick);
        registerSpotAnimChanges(character, KeyFrameType.SPOTANIM2, tick);
        registerSpawnChanges(character);
    }

    /**
     * Loops through all Characters and updates their current KeyFrame for the current time
     */
    public void updatePrograms()
    {
        updatePrograms(timeSheetPanel.getCurrentTime());
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
            double lastMovementTick = 0;
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
                }
            }


            KeyFrame currentAnimation = currentFrames[KeyFrameType.getIndex(KeyFrameType.ANIMATION)];
            double lastAnimationTick = 0;
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
                    //register changes
                }
            }


            KeyFrame currentSpawn = currentFrames[KeyFrameType.getIndex(KeyFrameType.SPAWN)];
            double lastSpawnTick = 0;
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
            double lastModelTick = 0;
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
            double lastOrientationTick = 0;
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
                    //register changes
                }
            }


            KeyFrame currentText = currentFrames[KeyFrameType.getIndex(KeyFrameType.TEXT)];
            double lastTextTick = 0;
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
            double lastOverheadTick = 0;
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
            double lastHealthTick = 0;
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
            double lastSpotAnimTick = 0;
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
            double lastSpotAnim2Tick = 0;
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

    public void registerMovementChanges(Character character)
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
            return;
        }

        MovementKeyFrame keyFrame = (MovementKeyFrame) kf;
        if (keyFrame.getPlane() != worldView.getPlane())
        {
            return;
        }

        int[][] path = keyFrame.getPath();
        int pathLength = path.length;

        double speed = keyFrame.getSpeed();
        double stepsComplete = (timeSheetPanel.getCurrentTime() - keyFrame.getTick()) * speed;
        int wholeStepsComplete = (int) Math.floor(stepsComplete);
        if (wholeStepsComplete > pathLength)
        {
            wholeStepsComplete = pathLength;
        }
        keyFrame.setCurrentStep(wholeStepsComplete);

        setLocation(worldView,
                character,
                keyFrame,
                wholeStepsComplete,
                stepsComplete);
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
                        ckObject.setAnimation(data.getAnimationId());
                        ckObject.setLocation(lp, plane);
                        ckObject.setActive(true);
                        ckObject.setDrawFrontTilesFirst(true);
                        setAnimationFrame(ckObject, currentTime, startTick, loop);
                        character.setSpotAnim(ckObject, keyFrameType);
                    });
                }
            }
        }
        else
        {
            if (spotAnimId > -1)
            {
                setAnimationFrame(spotAnim, currentTime, startTick, loop);
            }
            else
            {
                character.setSpotAnim1(null);
            }
        }
    }

    public void setAnimationFrame(CKObject ckObject, double currentTime, double startTime, boolean loop)
    {
        Animation animation = ckObject.getAnimation();
        int[] animFrame = getAnimFrame(animation, currentTime, startTime, loop);
        if (animFrame[0] == -1)
        {
            clientThread.invokeLater(() ->
            {
                ckObject.setActive(false);
                ckObject.setLoop(loop);
            });
        }
        else
        {
            clientThread.invokeLater(() ->
            {
                ckObject.setActive(true);
                ckObject.setAnimationFrame(animFrame[0], false);
                ckObject.tick(animFrame[1]);
                ckObject.setLoop(loop);
            });
        }
    }

    /**
     * Gets the current Animation Frame and Client Ticks that have passed that should be playing from the given time
     * @param animation The animation playing
     * @param currentTime The current time from which to calculate
     * @param startTime The start of the KeyFrame
     * @param loop Whether the animation is looping
     * @return an int[] consisting of {frame from which to play, number of Client Ticks that have passed this frame}
     */
    private int[] getAnimFrame(Animation animation, double currentTime, double startTime, boolean loop)
    {
        double gameTicksPassed = currentTime - startTime;
        int clientTicksPassed = (int) (gameTicksPassed * 30);

        if (animation.isMayaAnim())
        {
            int duration = animation.getDuration();
            if (loop)
            {
                int loops = clientTicksPassed / duration;
                clientTicksPassed = clientTicksPassed - loops * duration;

                return new int[]{clientTicksPassed, 0};
            }

            if (clientTicksPassed >= duration)
            {
                return new int[]{-1, 0};
            }

            return new int[]{clientTicksPassed, 0};
        }

        int[] frameLengths = animation.getFrameLengths();

        int duration = 0;
        for (int frameLength : frameLengths)
        {
            duration += frameLength;
        }

        if (loop)
        {
            int loops = clientTicksPassed / duration;
            clientTicksPassed = clientTicksPassed - loops * duration;

            int framesPassed = 0;
            for (int i = 0; i < frameLengths.length; i++)
            {
                int frameLength = frameLengths[i];
                if (framesPassed + frameLength > clientTicksPassed)
                {
                    int ticksPassed = clientTicksPassed - framesPassed;
                    return new int[]{i, ticksPassed};
                }

                framesPassed += frameLength;
            }

            return new int[]{-1, 0};
        }

        if (clientTicksPassed >= duration)
        {
            return new int[]{-1, 0};
        }

        int framesPassed = 0;
        for (int i = 0; i < frameLengths.length; i++)
        {
            int frameLength = frameLengths[i];
            if (framesPassed + frameLength > clientTicksPassed)
            {
                int ticksPassed = clientTicksPassed - framesPassed;
                return new int[]{i, ticksPassed};
            }

            framesPassed += frameLength;
        }

        return new int[]{-1, 0};
    }
}
