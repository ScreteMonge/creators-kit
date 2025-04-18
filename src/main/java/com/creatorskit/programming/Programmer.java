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
    private int clientTickAtLastGameTick = -1;
    private double subTick = 0;
    private boolean delayTick = false;
    private int clientTickAtLastProgramTick = 0;
    private final int GOLDEN_CHIN = 29757;

    @Getter
    @Setter
    private boolean playing = false;

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
        }
    }

    private void incrementSubTime()
    {
        double time = TimeSheetPanel.round(timeSheetPanel.getCurrentTime() + 0.1);
        timeSheetPanel.setCurrentTime(time, true);
    }

    private void incrementTime()
    {
        subTick = 0;
        int tick = (int) TimeSheetPanel.round(Math.floor(timeSheetPanel.getCurrentTime()) + 1);
        timeSheetPanel.setCurrentTime(tick, true);
    }

    public void togglePlay()
    {
        togglePlay(!playing);
    }

    public void togglePlay(boolean play)
    {
        if (play)
        {
            playing = true;
            timeSheetPanel.setPlayButtonIcon(true);
            timeSheetPanel.setCurrentTime(Math.floor(timeSheetPanel.getCurrentTime()), false);
            delayTick = true;

            ArrayList<Character> characters = plugin.getCharacters();
            for (int i = 0; i < characters.size(); i++)
            {
                Character character = characters.get(i);
                character.play();
            }
            return;
        }

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
                    //register changes
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

        updateSpotAnim(keyFrameType, spotAnimKeyFrame.getSpotAnimId(), spotAnimKeyFrame.getHeight(), character, currentTime, spotAnimKeyFrame.getTick(), spotAnimKeyFrame.isLoop(), lp, plane);
    }

    private void updateSpotAnim(KeyFrameType keyFrameType, int spotAnimId, int height, Character character, double currentTime, double startTick, boolean loop, LocalPoint lp, int plane)
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
                        for (ModelStats ms : stats)
                        {
                            ms.setTranslateZ(height);
                        }

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
