package me._2818.partyTS;

import java.util.UUID;

public class PartyInvite {
    private final Party party;
    private final UUID inviter;
    private final long timestamp;

    public PartyInvite(Party party, UUID inviter) {
        this.party = party;
        this.inviter = inviter;
        this.timestamp = System.currentTimeMillis();
    }

    public Party getParty() {
        return party;
    }

    public UUID getInviter() {
        return inviter;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 30000; // 30 seconds
    }
} 