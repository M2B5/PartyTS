package me._2818.partyTS.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me._2818.partyTS.PartyRaceManager;
import me._2818.partyTS.PartyManager;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;

public class PartyRaceCommand implements CommandExecutor {
    private final PartyManager partyManager;
    private final Plugin plugin;

    public PartyRaceCommand(PartyManager partyManager, Plugin plugin) {
        this.partyManager = partyManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§cUsage: /party race <track> [laps] [pits]");
            return true;
        }

        var possibleTrack = TimingSystemAPI.getTrack(args[0]);

        if (possibleTrack.isEmpty()) {
            player.sendMessage("§cTrack not found!");
            return true;
        }

        Track t = possibleTrack.get();

        if (!t.isOpen()) {
            player.sendMessage("§cTrack is not open!");
            return true;
        }

        if (t.getTrackLocations().getLocations(TrackLocation.Type.GRID).size() < partyManager.getPlayerParty(player).getMembers().size()) {
            player.sendMessage("§cNot enough grids!");
            return true;
        }

        int laps = 3;

        if (args.length >= 2) {
            laps = Integer.parseInt(args[1]);
        }

        int pits = 0;

        if (args.length >= 3) {
            pits = Integer.parseInt(args[2]);
        }

        PartyRaceManager partyRaceManager = new PartyRaceManager(partyManager, plugin);
        partyRaceManager.startPartyRace(player, t, laps, pits);
        
        return true;
    }

}
