package me._2818.partyTS.duels;

import me._2818.partyTS.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseEloRatingSystem {
    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final int defaultElo;
    private final int kFactor;
    private final int minChange;
    private final int maxChange;

    public DatabaseEloRatingSystem(Plugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.defaultElo = plugin.getConfig().getInt("rankedduels.defaultelo", 1200);
        this.kFactor = plugin.getConfig().getInt("rankedduels.kfactor", 32);
        this.minChange = plugin.getConfig().getInt("rankedduels.minchange", 1);
        this.maxChange = plugin.getConfig().getInt("rankedduels.maxchange", 50);
        
        plugin.getLogger().info("Database ELO System initialized - Default ELO: " + defaultElo + 
                               ", K-Factor: " + kFactor + ", Min Change: " + minChange + ", Max Change: " + maxChange);
    }

    private int applyEloLimits(int change) {
        if (change > 0) {
            return Math.max(minChange, Math.min(maxChange, change));
        } else if (change < 0) {
            return Math.min(-minChange, Math.max(-maxChange, change));
        } else {
            return 0;
        }
    }

    public CompletableFuture<Integer> getPlayerElo(UUID playerId) {
        return databaseManager.getPlayerStatsAsync(playerId)
                .thenApply(stats -> stats.elo());
    }

    public CompletableFuture<Integer> getPlayerWins(UUID playerId) {
        return databaseManager.getPlayerStatsAsync(playerId)
                .thenApply(stats -> stats.wins());
    }

    public CompletableFuture<Integer> getPlayerLosses(UUID playerId) {
        return databaseManager.getPlayerStatsAsync(playerId)
                .thenApply(stats -> stats.losses());
    }

    public CompletableFuture<Void> recordMatch(UUID winnerId, UUID loserId) {
        return CompletableFuture.allOf(
                getPlayerElo(winnerId),
                getPlayerElo(loserId)
        ).thenCompose(v -> {
            return databaseManager.getPlayerStatsAsync(winnerId)
                    .thenCombine(databaseManager.getPlayerStatsAsync(loserId), (winnerStats, loserStats) -> {
                        
                        int winnerElo = winnerStats.elo();
                        int loserElo = loserStats.elo();

                        // Calculate expected scores
                        double expectedWinner = 1.0 / (1.0 + Math.pow(10.0, (loserElo - winnerElo) / 400.0));
                        double expectedLoser = 1.0 / (1.0 + Math.pow(10.0, (winnerElo - loserElo) / 400.0));

                        // Calculate raw ELO changes
                        int rawWinnerChange = (int) Math.round(kFactor * (1.0 - expectedWinner));
                        int rawLoserChange = (int) Math.round(kFactor * (0.0 - expectedLoser));

                        // Apply min/max limits
                        int limitedWinnerChange = applyEloLimits(rawWinnerChange);
                        int limitedLoserChange = applyEloLimits(rawLoserChange);

                        // Calculate new ratings
                        int newWinnerElo = winnerElo + limitedWinnerChange;
                        int newLoserElo = loserElo + limitedLoserChange;

                        // Record the match in database
                        return databaseManager.recordMatchAsync(winnerId, loserId, 
                                                              winnerElo, newWinnerElo,
                                                              loserElo, newLoserElo);
                    }).thenCompose(future -> future);
        });
    }

    public CompletableFuture<EloChange> calculateEloChange(UUID player1Id, UUID player2Id, boolean player1Wins) {
        return CompletableFuture.allOf(
                getPlayerElo(player1Id),
                getPlayerElo(player2Id)
        ).thenCompose(v -> {
            return databaseManager.getPlayerStatsAsync(player1Id)
                    .thenCombine(databaseManager.getPlayerStatsAsync(player2Id), (player1Stats, player2Stats) -> {
                        
                        int player1Elo = player1Stats.elo();
                        int player2Elo = player2Stats.elo();

                        double expectedPlayer1 = 1.0 / (1.0 + Math.pow(10.0, (player2Elo - player1Elo) / 400.0));
                        double expectedPlayer2 = 1.0 / (1.0 + Math.pow(10.0, (player1Elo - player2Elo) / 400.0));

                        int rawPlayer1Change, rawPlayer2Change;
                        if (player1Wins) {
                            rawPlayer1Change = (int) Math.round(kFactor * (1.0 - expectedPlayer1));
                            rawPlayer2Change = (int) Math.round(kFactor * (0.0 - expectedPlayer2));
                        } else {
                            rawPlayer1Change = (int) Math.round(kFactor * (0.0 - expectedPlayer1));
                            rawPlayer2Change = (int) Math.round(kFactor * (1.0 - expectedPlayer2));
                        }

                        // Apply min/max limits
                        int limitedPlayer1Change = applyEloLimits(rawPlayer1Change);
                        int limitedPlayer2Change = applyEloLimits(rawPlayer2Change);

                        int newPlayer1Elo = player1Elo + limitedPlayer1Change;
                        int newPlayer2Elo = player2Elo + limitedPlayer2Change;

                        return new EloChange(
                            player1Elo, newPlayer1Elo, limitedPlayer1Change,
                            player2Elo, newPlayer2Elo, limitedPlayer2Change
                        );
                    });
        });
    }

    public CompletableFuture<DatabaseManager.PlayerStats> getPlayerStatsAsync(UUID playerId) {
        return databaseManager.getPlayerStatsAsync(playerId);
    }

    public CompletableFuture<List<DatabaseManager.LeaderboardEntry>> getTopPlayers(int limit) {
        return databaseManager.getLeaderboardAsync(limit);
    }

    public CompletableFuture<Void> updatePlayerName(UUID playerId, String playerName) {
        return databaseManager.updatePlayerAsync(playerId, playerName);
    }

    // Synchronous methods for backward compatibility (use sparingly)
    public int getPlayerEloSync(UUID playerId) {
        try {
            return getPlayerElo(playerId).get();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player ELO synchronously: " + e.getMessage());
            return defaultElo;
        }
    }

    public int getPlayerWinsSync(UUID playerId) {
        try {
            return getPlayerWins(playerId).get();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player wins synchronously: " + e.getMessage());
            return 0;
        }
    }

    public int getPlayerLossesSync(UUID playerId) {
        try {
            return getPlayerLosses(playerId).get();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player losses synchronously: " + e.getMessage());
            return 0;
        }
    }

    public static class EloChange {
        public final int player1OldElo;
        public final int player1NewElo;
        public final int player1Change;
        public final int player2OldElo;
        public final int player2NewElo;
        public final int player2Change;

        public EloChange(int player1OldElo, int player1NewElo, int player1Change,
                        int player2OldElo, int player2NewElo, int player2Change) {
            this.player1OldElo = player1OldElo;
            this.player1NewElo = player1NewElo;
            this.player1Change = player1Change;
            this.player2OldElo = player2OldElo;
            this.player2NewElo = player2NewElo;
            this.player2Change = player2Change;
        }
    }
}