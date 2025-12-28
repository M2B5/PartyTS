package me._2818.partyTS.commands;

import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class DuelCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Add subcommands
            completions.add("accept");
            completions.add("deny");
            completions.add("help");
            completions.add("elo");
            completions.add("leaderboard");
            
            // Add player names
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    completions.add(onlinePlayer.getName());
                }
            }
        } else if (args.length == 2) {
            // Handle subcommands
            if (args[0].equalsIgnoreCase("elo")) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    completions.add(onlinePlayer.getName());
                }
            } else {
                // Track names for duel challenges
                return TimingSystemAPI.getTracks().stream()
                        .filter(Track::isOpen)
                        .map(Track::getCommandName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            completions.add("laps");
        } else if (args.length == 4) {
            completions.add("pits");
        } else if (args.length == 5) {
            completions.add("collisions (on/off)");
        } else if (args.length == 6) {
            completions.add("drs (on/off)");
        } else if (args.length == 7) {
            completions.add("ranked (true/false)");
        }

        if (!args[args.length - 1].isEmpty()) {
            completions.removeIf(s -> !s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()));
        }

        return completions;
    }
}