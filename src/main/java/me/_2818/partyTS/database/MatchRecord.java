package me._2818.partyTS.database;

import java.sql.Timestamp;
import java.util.UUID;

public record MatchRecord(
    int id,
    UUID winnerId,
    UUID loserId,
    String winnerName,
    String loserName,
    int winnerEloBefore,
    int winnerEloAfter,
    int loserEloBefore,
    int loserEloAfter,
    Timestamp matchDate
) {
    public int getEloChange() {
        return winnerEloAfter - winnerEloBefore;
    }
    
    public boolean isWinner(UUID playerId) {
        return winnerId.equals(playerId);
    }
    
    public String getOpponentName(UUID playerId) {
        return winnerId.equals(playerId) ? loserName : winnerName;
    }
    
    public int getPlayerEloBefore(UUID playerId) {
        return winnerId.equals(playerId) ? winnerEloBefore : loserEloBefore;
    }
    
    public int getPlayerEloAfter(UUID playerId) {
        return winnerId.equals(playerId) ? winnerEloAfter : loserEloAfter;
    }
    
    public int getPlayerEloChange(UUID playerId) {
        return getPlayerEloAfter(playerId) - getPlayerEloBefore(playerId);
    }
}