package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.models.CustomLighting;
import com.creatorskit.models.DataFinder;
import com.creatorskit.models.LightingStyle;
import com.creatorskit.models.ModelStats;
import com.creatorskit.models.datatypes.SpotanimData;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.ModelKeyFrame;
import com.creatorskit.swing.timesheet.keyframe.SpotAnimKeyFrame;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
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

        if (clientTickAtLastGameTick == -1)
        {
            clientTickAtLastGameTick = client.getGameCycle();
        }

        if (timeSheetPanel.isPlayActive())
        {
            int currentClientTick = client.getGameCycle();
            int change = currentClientTick - clientTickAtLastGameTick;
            if (change * Constants.CLIENT_TICK_LENGTH >= Constants.GAME_TICK_LENGTH)
            {
                return;
            }

            double nextSubTick = TimeSheetPanel.round((double) change / 30);
            if (subTick < nextSubTick)
            {
                incrementSubTime();
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (timeSheetPanel.isPlayActive())
        {
            incrementTime();
            clientTickAtLastGameTick = client.getGameCycle();
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
        timeSheetPanel.setCurrentTime(Math.floor(timeSheetPanel.getCurrentTime()) + 1, true);
    }

    /**
     * Updates the program with their current KeyFrame for the given time
     * Intended for use adding or removing a KeyFrame from a specific Character
     * @param tick the tick at which to look for KeyFrames
     */
    public void updateProgram(Character character, double tick)
    {
        character.updateProgram(tick);
        registerModelChanges(character);
        registerSpotAnimChanges(character, tick);
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
    public void updatePrograms()
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
                    //register changes
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
        }
    }

    private void registerModelChanges(Character character)
    {
        CKObject ckObject = character.getCkObject();

        KeyFrame keyFrame = character.getCurrentKeyFrame(KeyFrameType.MODEL);
        if (keyFrame == null)
        {
            plugin.setModel(character, character.isCustomMode(), (int) character.getModelSpinner().getValue());
            return;
        }

        ModelKeyFrame modelKeyFrame = (ModelKeyFrame) keyFrame;

        if (modelKeyFrame.isUseCustomModel())
        {
            ckObject.setModel(modelKeyFrame.getCustomModel().getModel());
        }
        else
        {
            int modelId = modelKeyFrame.getModelId();
            if (modelId == -1)
            {
                return;
            }

            clientThread.invokeLater(() ->
            {
                ckObject.setModel(client.loadModel(modelId));
            });
        }
    }

    private void registerSpotAnimChanges(Character character, double currentTime)
    {
        CKObject ckObject = character.getCkObject();

        KeyFrame keyFrame = character.getCurrentKeyFrame(KeyFrameType.SPOTANIM);
        if (keyFrame == null)
        {
            clientThread.invokeLater(() ->
            {
                CKObject spotAnim1 = character.getSpotAnim1();
                if (spotAnim1 == null)
                {
                    return;
                }

                spotAnim1.setActive(false);
                character.setSpotAnim1(null);
            });
            return;
        }

        SpotAnimKeyFrame spotAnimKeyFrame = (SpotAnimKeyFrame) keyFrame;

        LocalPoint lp = ckObject.getLocation();
        int plane = ckObject.getLevel();

        CKObject spotAnim1 = character.getSpotAnim1();
        updateSpotAnim(spotAnim1, spotAnimKeyFrame.getSpotAnimId1(), character, currentTime, spotAnimKeyFrame.getTick(), spotAnimKeyFrame.isLoop1(), lp, plane);
        CKObject spotAnim2 = character.getSpotAnim2();
        //updateSpotAnim(spotAnim2, spotAnimKeyFrame.getSpotAnimId2(), character, currentTime, lp, plane);
    }

    private void updateSpotAnim(CKObject spotAnim, int spotAnimId, Character character, double currentTime, double startTick, boolean loop, LocalPoint lp, int plane)
    {
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

                        CKObject sp1 = new CKObject(client);
                        character.setSpotAnim1(sp1);
                        client.registerRuneLiteObject(sp1);
                        sp1.setModel(model);
                        sp1.setAnimation(data.getAnimationId());
                        sp1.setLocation(lp, plane);
                        sp1.setActive(true);
                        setAnimationFrame(sp1, currentTime, startTick, loop);
                        character.setSpotAnim1(sp1);
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
            });
        }
        else
        {
            clientThread.invokeLater(() ->
            {
                ckObject.setActive(true);
                ckObject.setAnimationFrame(animFrame[0], false);
                ckObject.tick(animFrame[1]);
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
