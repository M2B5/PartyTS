package me._2818.partyTS.listeners;

import me._2818.partyTS.duels.DuelsManager;
import me._2818.partyTS.party.PartyManager;
import me._2818.partyTS.party.PartyRaceManager;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.api.events.HeatFinishEvent;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class DisconnectListener implements Listener {
    private final PartyRaceManager partyRaceManager;
    private final DuelsManager duelsManager;
    private final PartyManager partyManager;


    public DisconnectListener(PartyRaceManager partyRaceManager, DuelsManager duelsManager, PartyManager partyManager) {
        this.partyRaceManager = partyRaceManager;
        this.duelsManager = duelsManager;
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        partyManager.leaveParty(event.getPlayer());

        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(event.getPlayer().getUniqueId());

        if (maybeDriver.isEmpty()) return;
        Driver driver = maybeDriver.get();

        Heat heat = driver.getHeat();

        if (partyRaceManager.getActivePartyHeats().contains(heat) || duelsManager.getActiveDuels().contains(heat)) heat.disqualifyDriver(driver);
    }

    @EventHandler
    public void onHeatFinish(HeatFinishEvent event) {
        if (partyRaceManager.getActivePartyHeats().contains(event.getHeat())) partyRaceManager.endPartyRace(event.getHeat());
        if (duelsManager.getActiveDuels().contains(event.getHeat())) duelsManager.endDuel(event.getHeat());
    }
}
