package com.creatorskit.programming;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;

public class Programmer
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final TimeSheetPanel timeSheetPanel;
    private int clientTickAtLastGameTick = -1;
    private double subTick = 0;

    @Inject
    public Programmer(Client client, CreatorsPlugin plugin, TimeSheetPanel timeSheetPanel)
    {
        this.client = client;
        this.plugin = plugin;
        this.timeSheetPanel = timeSheetPanel;
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
        timeSheetPanel.setCurrentTime(time);
    }

    private void incrementTime()
    {
        subTick = 0;
        timeSheetPanel.setCurrentTime(Math.floor(timeSheetPanel.getCurrentTime()) + 1);
    }

}
