package me._2818.partyTS.commands;

import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PartyRaceTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Track track : TimingSystemAPI.getTracks()) {
                if (track.isOpen()) {
                    completions.add(track.getDisplayName());
                }
            }
        } else if (args.length == 2) {
            completions.add("Laps");
        } else if (args.length == 3) {
            completions.add("Pits");
        }

        if (!args[args.length - 1].isEmpty()) {
            completions.removeIf(s -> !s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()));
        }

        return completions;
    }
} 