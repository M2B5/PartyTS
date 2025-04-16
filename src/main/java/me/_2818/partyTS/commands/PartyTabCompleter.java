package me._2818.partyTS.commands;

import me._2818.partyTS.PartyManager;
import me._2818.partyTS.Party;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        } else if (args.length == 2) {
            // Second argument: player name (for invite and kick)
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
            }
        }

        if (!args[args.length - 1].isEmpty()) {
            completions.removeIf(s -> !s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()));
        }

        return completions;
    }
} 