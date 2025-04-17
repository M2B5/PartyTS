package me._2818.partyTS;

import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.track.Track;

import java.util.Optional;

import org.bukkit.entity.Player;

public class PartyRaceManager {
    public static boolean startPartyRace(Player player, Track track, int laps, int pits) {
        final String name = "Party Race " + track.getDisplayName() + " " + player.getName();
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

        return true;
    }

    public static void endPartyRace(Player player, Track track, int laps, int pits) {

    }
}
