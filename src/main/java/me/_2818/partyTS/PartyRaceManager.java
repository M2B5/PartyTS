package me._2818.partyTS;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.commands.CommandHeat;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.track.Track;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PartyRaceManager {
    private final PartyManager partyManager;
    private final Plugin plugin;

    public PartyRaceManager(PartyManager partyManager, Plugin plugin) {
        this.partyManager = partyManager;
        this.plugin = plugin;
    }

    public boolean startPartyRace(Player player, Track track, int laps, int pits) {
        final String name = "Party_Race_" + track.getDisplayName() + "_" + player.getName();
        Optional<Event> maybeEvent = EventDatabase.eventNew(player.getUniqueId(), name);
        if(maybeEvent.isEmpty()) return false;
        Event event = maybeEvent.get();
        event.setTrack(track);

        if(!EventDatabase.roundNew(event, RoundType.FINAL, 1)) return false;

        Optional<Round> maybeRound = event.eventSchedule.getRound(1);
        if(maybeRound.isEmpty()) return false;
        Round round = maybeRound.get();

        round.createHeat(1);
        var maybeHeat = round.getHeat("R1F1");
        if(maybeHeat.isEmpty()) return false;
        Heat heat = maybeHeat.get();

        laps = Math.max(1, laps);
        pits = Math.max(0, pits);

        if(track.isStage()) {
            heat.setTotalLaps(1);
            heat.setTotalPits(0);
        } else {
            heat.setTotalLaps(laps);
            heat.setTotalPits(pits);
        }

        HeatState state = heat.getHeatState();
        if (state != HeatState.SETUP && !heat.resetHeat()) return false;

        if (!heat.loadHeat()) {
            EventDatabase.removeEventHard(event);
            event = null;
            round = null;
            heat = null;
            return false;
        }

        for (UUID memberUUID : partyManager.getPartyByLeader(player).getMembers()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                EventDatabase.heatDriverNew(memberUUID, heat, heat.getDrivers().size() + 1);
            }
        }

        CommandHeat.onSortByRandom(player, heat);
        
        final Heat finalHeat = heat;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finalHeat.startCountdown(10);
        }, 20L);

        return true;
    }

    public static void endPartyRace(Player player, Track track, int laps, int pits) {

    }
}
