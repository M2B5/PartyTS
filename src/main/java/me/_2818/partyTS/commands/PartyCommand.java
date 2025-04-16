package me._2818.partyTS.commands;

import me._2818.partyTS.PartyManager;
import me._2818.partyTS.Party;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PartyCommand implements CommandExecutor {
    private final PartyManager partyManager;

    public PartyCommand(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player);
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party invite <player>");
                    return true;
                }
                handleInvite(player, args[1]);
                break;
            case "accept":
                handleAccept(player);
                break;
            case "decline":
                handleDecline(player);
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party kick <player>");
                    return true;
                }
                handleKick(player, args[1]);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "list":
                handleList(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== Party Commands ===");
        player.sendMessage("§e/party create §7- Create a new party");
        player.sendMessage("§e/party invite <player> §7- Invite a player to your party");
        player.sendMessage("§e/party accept §7- Accept a party invite");
        player.sendMessage("§e/party decline §7- Decline a party invite");
        player.sendMessage("§e/party kick <player> §7- Kick a player from your party");
        player.sendMessage("§e/party leave §7- Leave your current party");
        player.sendMessage("§e/party list §7- List party members");
    }

    private void handleCreate(Player player) {
        Party party = partyManager.createParty(player);
        if (party == null) {
            player.sendMessage("§cYou are already in a party!");
            return;
        }
        player.sendMessage("§aParty created successfully!");
    }

    private void handleInvite(Player player, String targetName) {
        Party party = partyManager.getPlayerParty(player);
        
        // If player is not in a party, create one automatically
        if (party == null) {
            party = partyManager.createParty(player);
            if (party == null) {
                player.sendMessage("§cFailed to create a party!");
                return;
            }
            player.sendMessage("§aParty created automatically!");
        }

        Player target = player.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        if (partyManager.invitePlayer(party, target)) {
            player.sendMessage("§aInvited " + target.getName() + " to your party!");
            target.sendMessage("§aYou have been invited to join " + player.getName() + "'s party!");
            target.sendMessage("§eUse /party accept to join or /party decline to decline the invite");
            target.sendMessage("§7This invite will expire in 30 seconds");
        } else {
            if (partyManager.hasPendingInvite(target)) {
                player.sendMessage("§c" + target.getName() + " already has a pending invite!");
            } else {
                player.sendMessage("§c" + target.getName() + " is already in a party!");
            }
        }
    }

    private void handleAccept(Player player) {
        if (partyManager.acceptInvite(player)) {
            Party party = partyManager.getPlayerParty(player);
            UUID inviterUUID = partyManager.getInviter(player);
            Player inviter = inviterUUID != null ? Bukkit.getPlayer(inviterUUID) : null;
            
            player.sendMessage("§aYou have joined the party!");
            party.broadcastMessage("§a" + player.getName() + " has joined the party!");
            
            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage("§a" + player.getName() + " has accepted your party invite!");
            }
        } else {
            player.sendMessage("§cYou don't have any valid party invites!");
        }
    }

    private void handleDecline(Player player) {
        if (partyManager.declineInvite(player)) {
            UUID inviterUUID = partyManager.getInviter(player);
            Player inviter = inviterUUID != null ? Bukkit.getPlayer(inviterUUID) : null;
            
            player.sendMessage("§aYou have declined the party invite!");
            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage("§c" + player.getName() + " has declined your party invite!");
            }
        } else {
            player.sendMessage("§cYou don't have any valid party invites!");
        }
    }

    private void handleKick(Player player, String targetName) {
        Party party = partyManager.getPlayerParty(player);
        if (party == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the party leader can kick players!");
            return;
        }

        Player target = player.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        if (partyManager.kickPlayer(party, target)) {
            player.sendMessage("§aKicked " + target.getName() + " from the party!");
            target.sendMessage("§cYou have been kicked from the party!");
        } else {
            player.sendMessage("§cCould not kick " + target.getName() + " from the party!");
        }
    }

    private void handleLeave(Player player) {
        if (partyManager.leaveParty(player)) {
            player.sendMessage("§aYou have left the party!");
        } else {
            player.sendMessage("§cYou are not in a party!");
        }
    }

    private void handleList(Player player) {
        Party party = partyManager.getPlayerParty(player);
        if (party == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        player.sendMessage("§6=== Party Members ===");
        Player leader = player.getServer().getPlayer(party.getLeader());
        player.sendMessage("§eLeader: §7" + (leader != null ? leader.getName() : "Offline"));
        player.sendMessage("§eMembers: §7" + party.getMembers().size());
        for (UUID memberUUID : party.getMembers()) {
            Player member = player.getServer().getPlayer(memberUUID);
            if (member != null) {
                String prefix = memberUUID.equals(party.getLeader()) ? "§f" : "§7";
                player.sendMessage(prefix + "- " + member.getName());
            }
        }
    }
} 