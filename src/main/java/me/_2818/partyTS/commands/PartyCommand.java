package me._2818.partyTS.commands;

import me._2818.partyTS.Party;
import me._2818.partyTS.PartyManager;
import me._2818.partyTS.PartyRaceManager;
import me._2818.partyTS.messages.LanguageManager;
import me._2818.partyTS.messages.Message;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PartyCommand implements CommandExecutor {
    private final PartyManager partyManager;
    private final Plugin plugin;

    public PartyCommand(PartyManager partyManager, Plugin plugin) {
        this.partyManager = partyManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.PLAYER_ONLY.asComponent("This command can only be used by players."));
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
                    player.sendMessage(Message.ERROR_USAGE.asComponent("/party invite <player>"));
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
                    player.sendMessage(Message.ERROR_USAGE.asComponent("/party kick <player>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase(player.getName())) {
                    player.sendMessage(Message.ERROR_CANNOT_KICK_SELF.asComponent());
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
            case "race":
                if (args.length < 2) {
                    player.sendMessage(Message.ERROR_USAGE.asComponent("/party race <track> [laps] [pits]"));
                    return true;
                }
                handleRace(player, args);
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
        player.sendMessage("§e/party race <track> §7- Start a party race");
    }

    private void handleCreate(Player player) {
        Party party = partyManager.createParty(player);
        if (party == null) {
            player.sendMessage(Message.PARTY_ALREADY_IN_ONE.asComponent());
            return;
        }
        player.sendMessage(Message.PARTY_CREATED.asComponent());
    }

    private void handleInvite(Player player, String targetName) {
        Party party = partyManager.getPlayerParty(player);

        if (party == null) {
            party = partyManager.createParty(player);
            if (party == null) {
                player.sendMessage(Message.PARTY_CREATE_FAILED.asComponent());
                return;
            }
            player.sendMessage(Message.PARTY_CREATED.asComponent());
        }

        Player target = player.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Message.ERROR_PLAYER_NOT_FOUND.asComponent());
            return;
        }

        if (partyManager.isOnInviteCooldown(player, target)) {
            player.sendMessage(Message.INVITE_COOLDOWN.asComponent(partyManager.getInviteCooldownSeconds(player, target)));
            return;
        }

        if (partyManager.invitePlayer(party, target)) {
            player.sendMessage(Message.INVITE_SENT.asComponent(target.getName()));
            
            Component inviteMessage = Component.text()
                    .append(Message.INVITE_RECEIVED.asComponent(player.getName()))
                    .append(Component.newline())
                    .append(Component.text("         ", NamedTextColor.GRAY))
                    .append(Component.text("[Accept]", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/party accept")))
                    .append(Component.text("  "))
                    .append(Component.text("[Decline]", NamedTextColor.RED, TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/party decline")))
                    .append(Component.newline())
                    .append(Component.text("This invite will expire in " + plugin.getConfig().getInt("invitetimeout") + " seconds", NamedTextColor.GRAY))
                    .build();
            target.sendMessage(inviteMessage);
        } else {
            if (partyManager.hasPendingInvite(target)) {
                player.sendMessage(Message.INVITE_ALREADY_INVITED.asComponent(target.getName()));
            } else {
                player.sendMessage(Message.INVITE_ALREADY_IN_PARTY.asComponent(target.getName()));
            }
        }
    }

    private void handleAccept(Player player) {
        UUID inviterUUID = partyManager.getInviter(player);

        if (partyManager.acceptInvite(player)) {
            Party party = partyManager.getPlayerParty(player);
            Player inviter = inviterUUID != null ? Bukkit.getPlayer(inviterUUID) : null;

            player.sendMessage(Message.INVITE_ACCEPTED.asComponent());
            party.broadcastMessage(Message.MEMBER_JOINED, player.getName());

            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage(Message.INVITE_ACCEPTED_NOTIFICATION.asComponent(player.getName()));
            }
        } else {
            player.sendMessage(Message.ERROR_NO_INVITE.asComponent());
        }
    }

    private void handleDecline(Player player) {
        UUID inviterUUID = partyManager.getInviter(player);

        if (partyManager.declineInvite(player)) {
            Player inviter = inviterUUID != null ? Bukkit.getPlayer(inviterUUID) : null;

            player.sendMessage(Message.INVITE_DECLINED.asComponent());
            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage(Message.INVITE_DECLINED_NOTIFICATION.asComponent(player.getName()));
            }
        } else {
            player.sendMessage(Message.ERROR_NO_INVITE.asComponent());
        }
    }

    private void handleKick(Player player, String targetName) {
        Party party = partyManager.getPlayerParty(player);
        if (party == null) {
            player.sendMessage(Message.ERROR_NOT_IN_PARTY.asComponent());
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage(Message.ERROR_NOT_LEADER.asComponent());
            return;
        }

        Player target = player.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Message.ERROR_PLAYER_NOT_FOUND.asComponent());
            return;
        }

        if (partyManager.kickPlayer(party, player, target)) {
            player.sendMessage(Message.MEMBER_KICKED_OTHER.asComponent(target.getName()));
            target.sendMessage(Message.MEMBER_KICKED.asComponent());
        } else {
            player.sendMessage(Message.ERROR_PLAYER_NOT_FOUND.asComponent());
        }
    }

    private void handleLeave(Player player) {
        if (partyManager.leaveParty(player)) {
            player.sendMessage(Message.MEMBER_LEFT.asComponent(player.getName()));
        } else {
            player.sendMessage(Message.ERROR_NOT_IN_PARTY.asComponent());
        }
    }

    private void handleList(Player player) {
        Party party = partyManager.getPlayerParty(player);
        if (party == null) {
            player.sendMessage(Message.ERROR_NOT_IN_PARTY.asComponent());
            return;
        }

        Component message = Component.text()
                .append(Component.text("=== Party Members ===", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Leader: ", NamedTextColor.YELLOW))
                .append(Component.text(
                        party.getLeader().equals(player.getUniqueId()) ? "You" : 
                        Bukkit.getOfflinePlayer(party.getLeader()).getName(),
                        NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("Members (" + party.getMembers().size() + "):", NamedTextColor.YELLOW))
                .build();
        
        player.sendMessage(message);
        
        for (UUID memberUUID : party.getMembers()) {
            if (!memberUUID.equals(party.getLeader())) {  // Skip leader as they're already listed
                String memberName = Bukkit.getOfflinePlayer(memberUUID).getName();
                if (memberName != null) {
                    player.sendMessage(Component.text("- ", NamedTextColor.GRAY)
                            .append(Component.text(memberName, 
                                    memberUUID.equals(player.getUniqueId()) ? 
                                            NamedTextColor.GREEN : NamedTextColor.GRAY)));
                }
            }
        }
    }

    private void handleRace(Player player, String[] args) {
        Party playerParty = partyManager.getPlayerParty(player);

        if (playerParty == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        if (!playerParty.getLeader().equals(player.getUniqueId())) {
            player.sendMessage("§cOnly the party leader can start a race!");
            return;
        }

        var possibleTrack = TimingSystemAPI.getTrack(args[1]);

        if (possibleTrack.isEmpty()) {
            player.sendMessage("§cTrack not found!");
            return;
        }

        Track t = possibleTrack.get();

        if (!t.isOpen()) {
            player.sendMessage("§cTrack is not open!");
            return;
        }

        if (t.getTrackLocations().getLocations(TrackLocation.Type.GRID).size() < playerParty.getMembers().size()) {
            player.sendMessage("§cNot enough grids!");
            return;
        }

        int laps = 3;

        if (args.length >= 3) {
            try {
                laps = Math.min(Integer.parseInt(args[2]), plugin.getConfig().getInt("maxlaps", 10));
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number of laps!");
                return;
            }
        }

        int pits = 0;

        if (args.length >= 4) {
            try {
                pits = Math.min(Integer.parseInt(args[3]), Math.max(Math.abs(laps - 2), 0));
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number of pits!");
                return;
            }
        }

        PartyRaceManager partyRaceManager = new PartyRaceManager(partyManager, plugin);
        partyRaceManager.startPartyRace(player, t, laps, pits);
    }
}