package me._2818.partyTS.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PartyManager {
    private final Map<UUID, Party> leaderParties;
    private final Map<UUID, Party> playerParties;
    private final Map<UUID, PartyInvite> pendingInvites;
    private final Map<UUID, Map<UUID, Long>> inviteCooldowns;
    private BukkitTask cleanupTask;
    private final Plugin plugin;


    public PartyManager(Plugin plugin) {
        this.leaderParties = new HashMap<>();
        this.playerParties = new HashMap<>();
        this.pendingInvites = new HashMap<>();
        this.inviteCooldowns = new HashMap<>();
        this.plugin = plugin;

        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupTasks, 20L, 20L);
    }

    private void cleanupTasks() {
        long now = System.currentTimeMillis();
        long cooldownMillis = plugin.getConfig().getInt("invitetimeout", 30) * 1000L;

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

        inviteCooldowns.entrySet().removeIf(outerEntry -> {
            outerEntry.getValue().entrySet().removeIf(innerEntry -> (now - innerEntry.getValue()) > cooldownMillis);
            return outerEntry.getValue().isEmpty();
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

        if (hasPendingInvite(player)) {
            return false;
        }

        pendingInvites.put(player.getUniqueId(), new PartyInvite(party, party.getLeader(), plugin));
        inviteCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(party.getLeader(), System.currentTimeMillis());
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

    public boolean isOnInviteCooldown(Player inviter, Player invited) {
        Map<UUID, Long> cooldownsForInvited = inviteCooldowns.get(invited.getUniqueId());
        if (cooldownsForInvited == null) {
            return false;
        }

        Long lastInviteTime = cooldownsForInvited.get(inviter.getUniqueId());
        if (lastInviteTime == null) {
            return false;
        }

        long cooldownMillis = plugin.getConfig().getInt("invitetimeout", 30) * 1000L;
        return (System.currentTimeMillis() - lastInviteTime) < cooldownMillis;
    }

    public long getInviteCooldownSeconds(Player inviter, Player invited) {
        Map<UUID, Long> cooldownsForInvited = inviteCooldowns.get(invited.getUniqueId());
        if (cooldownsForInvited == null) {
            return 0;
        }

        Long lastInviteTime = cooldownsForInvited.get(inviter.getUniqueId());
        if (lastInviteTime == null) {
            return 0;
        }

        long cooldownMillis = plugin.getConfig().getInt("invitetimeout", 30) * 1000L;
        long remainingMillis = (lastInviteTime + cooldownMillis) - System.currentTimeMillis();

        return remainingMillis > 0 ? TimeUnit.MILLISECONDS.toSeconds(remainingMillis) + 1 : 0;
    }
}