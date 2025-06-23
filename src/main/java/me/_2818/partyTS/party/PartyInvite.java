package me._2818.partyTS.party;

import lombok.Getter;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class PartyInvite {
    @Getter
    private final Party party;
    @Getter
    private final UUID inviter;
    @Getter
    private final long timestamp;
    private final Plugin plugin;


    public PartyInvite(Party party, UUID inviter, Plugin plugin) {
        this.party = party;
        this.inviter = inviter;
        this.timestamp = System.currentTimeMillis();
        this.plugin = plugin;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - timestamp) > (plugin.getConfig().getInt("invitetimeout", 30) * 1000L);
    }
} 