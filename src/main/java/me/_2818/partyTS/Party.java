package me._2818.partyTS;

import me._2818.partyTS.messages.LanguageManager;
import me._2818.partyTS.messages.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private final UUID leader;
    private final Set<UUID> members;

    public Party(UUID leader) {
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public boolean isMember(UUID playerUUID) {
        return members.contains(playerUUID);
    }

    public boolean isLeader(UUID playerUUID) {
        return leader.equals(playerUUID);
    }

    public boolean addMember(UUID playerUUID) {
        return members.add(playerUUID);
    }

    public boolean removeMember(UUID playerUUID) {
        return members.remove(playerUUID);
    }

    public void broadcastMessage(Message message, Object... args) {
        Component component = message.asComponent(args);
        for (UUID memberUUID : members) {
            Player player = Bukkit.getPlayer(memberUUID);
            if (player != null && player.isOnline()) {
                player.sendMessage(component);
            }
        }
    }
    
    public void broadcastMessage(String message) {
        for (UUID memberUUID : members) {
            Player player = Bukkit.getPlayer(memberUUID);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
}