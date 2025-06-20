package me._2818.partyTS;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {
    private final Map<UUID, Party> leaderParties;
    private final Map<UUID, Party> playerParties;
    private final Map<UUID, PartyInvite> pendingInvites;
    private BukkitTask cleanupTask;
    private final Plugin plugin;


    public PartyManager(Plugin plugin) {
        this.leaderParties = new HashMap<>();
        this.playerParties = new HashMap<>();
        this.pendingInvites = new HashMap<>();
        this.plugin = plugin;
        
        // Start cleanup task
        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredInvites, 20L, 20L);
    }

    private void cleanupExpiredInvites() {
        pendingInvites.entrySet().removeIf(entry -> {
            PartyInvite invite = entry.getValue();
            if (invite.isExpired()) {
                Player invitedPlayer = Bukkit.getPlayer(entry.getKey());
                Player leader = Bukkit.getPlayer(invite.getInviter());
                
                if (invitedPlayer != null && invitedPlayer.isOnline()) {
                    invitedPlayer.sendMessage("§cYour party invite has expired!");
                }
                
                if (leader != null && leader.isOnline()) {
                    leader.sendMessage("§cYour party invite to " + 
                        (invitedPlayer != null ? invitedPlayer.getName() : "a player") + 
                        " has expired!");
                }
                return true;
            }
            return false;
        });
    }

    public void disable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    public Party createParty(Player leader) {
        if (playerParties.containsKey(leader.getUniqueId())) {
            return null;
        }

        Party party = new Party(leader.getUniqueId());
        leaderParties.put(leader.getUniqueId(), party);
        playerParties.put(leader.getUniqueId(), party);
        return party;
    }

    public boolean invitePlayer(Party party, Player player) {
        if (playerParties.containsKey(player.getUniqueId())) {
            return false;
        }

        // Check if player already has a pending invite
        if (pendingInvites.containsKey(player.getUniqueId())) {
            PartyInvite existingInvite = pendingInvites.get(player.getUniqueId());
            if (!existingInvite.isExpired()) {
                return false;
            }
        }

        pendingInvites.put(player.getUniqueId(), new PartyInvite(party, party.getLeader(), plugin));
        return true;
    }

    public boolean acceptInvite(Player player) {
        PartyInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            return false;
        }

        Party party = invite.getParty();
        if (party.addMember(player.getUniqueId())) {
            playerParties.put(player.getUniqueId(), party);
            return true;
        }
        return false;
    }

    public boolean declineInvite(Player player) {
        PartyInvite invite = pendingInvites.remove(player.getUniqueId());
        return invite != null && !invite.isExpired();
    }

    public boolean kickPlayer(Party party, Player player, Player target) {
        if (!party.isLeader(player.getUniqueId())) {
            return false;
        }

        if (party.removeMember(target.getUniqueId())) {
            playerParties.remove(target.getUniqueId());
            return true;
        }
        return false;
    }

    public boolean leaveParty(Player player) {
        Party party = playerParties.get(player.getUniqueId());
        if (party == null) {
            return false;
        }

        if (party.isLeader(player.getUniqueId())) {
            // If leader leaves, disband the party
            for (UUID memberUUID : party.getMembers()) {
                playerParties.remove(memberUUID);
            }
            leaderParties.remove(player.getUniqueId());
        } else {
            party.removeMember(player.getUniqueId());
            playerParties.remove(player.getUniqueId());
        }
        return true;
    }

    public Party getPartyByLeader(Player leader) {
        return leaderParties.get(leader.getUniqueId());
    }

    public Party getPlayerParty(Player player) {
        return playerParties.get(player.getUniqueId());
    }

    public boolean isInParty(Player player) {
        return playerParties.containsKey(player.getUniqueId());
    }

    public boolean hasPendingInvite(Player player) {
        PartyInvite invite = pendingInvites.get(player.getUniqueId());
        return invite != null && !invite.isExpired();
    }

    public UUID getInviter(Player player) {
        PartyInvite invite = pendingInvites.get(player.getUniqueId());
        return invite != null && !invite.isExpired() ? invite.getInviter() : null;
    }
} 