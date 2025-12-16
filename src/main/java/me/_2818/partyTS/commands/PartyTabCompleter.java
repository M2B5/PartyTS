package me._2818.partyTS.commands;

import me._2818.partyTS.party.PartyManager;
import me._2818.partyTS.party.Party;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyTabCompleter implements TabCompleter {
    private final PartyManager partyManager;

    public PartyTabCompleter(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("create");
            completions.add("invite");
            completions.add("accept");
            completions.add("decline");
            completions.add("kick");
            completions.add("leave");
            completions.add("list");
            completions.add("info");
            completions.add("reload");
            completions.add("race");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "invite":
                    // Only show online players not in a party
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (!onlinePlayer.equals(player) && !partyManager.isInParty(onlinePlayer)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                    break;
                case "kick":
                    // Only show party members (except leader)
                    Party party = partyManager.getPlayerParty(player);
                    if (party != null && party.isLeader(player.getUniqueId())) {
                        for (UUID memberUUID : party.getMembers()) {
                            if (!memberUUID.equals(party.getLeader())) {
                                Player member = Bukkit.getPlayer(memberUUID);
                                if (member != null) {
                                    completions.add(member.getName());
                                }
                            }
                        }
                    }
                    break;
                case "race":
                    return TimingSystemAPI.getTracks().stream()
                            .filter(Track::isOpen)
                            .map(Track::getCommandName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("race")) {
            completions.add("laps");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("race")) {
            completions.add("pits");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("race")) {
            completions.add("collisions");
        } else if (args.length == 6 && args[0].equalsIgnoreCase("race")) {
            completions.add("drs");
        }

        if (!args[args.length - 1].isEmpty()) {
            completions.removeIf(s -> !s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()));
        }

        return completions;
    }
} 