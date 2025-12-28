package me._2818.partyTS.database;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private final Plugin plugin;
    private final File databaseFile;
    private Connection connection;
    private final ExecutorService executor;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "partyts.db");
        this.executor = Executors.newFixedThreadPool(2);
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            if (!databaseFile.getParentFile().exists()) {
                databaseFile.getParentFile().mkdirs();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            connection.setAutoCommit(true);

            createTables();
            plugin.getLogger().info("SQLite database initialized successfully");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        // Players table for ELO ratings and stats
        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                player_name TEXT,
                elo INTEGER NOT NULL DEFAULT 1200,
                wins INTEGER NOT NULL DEFAULT 0,
                losses INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """;

        // Matches table for match history
        String createMatchesTable = """
            CREATE TABLE IF NOT EXISTS matches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                winner_uuid TEXT NOT NULL,
                loser_uuid TEXT NOT NULL,
                winner_elo_before INTEGER NOT NULL,
                winner_elo_after INTEGER NOT NULL,
                loser_elo_before INTEGER NOT NULL,
                loser_elo_after INTEGER NOT NULL,
                elo_change INTEGER NOT NULL,
                match_date INTEGER NOT NULL,
                FOREIGN KEY (winner_uuid) REFERENCES players(uuid),
                FOREIGN KEY (loser_uuid) REFERENCES players(uuid)
            )
        """;

        // Create indexes for better performance
        String createPlayerNameIndex = "CREATE INDEX IF NOT EXISTS idx_player_name ON players(player_name)";
        String createEloIndex = "CREATE INDEX IF NOT EXISTS idx_elo ON players(elo DESC)";
        String createMatchDateIndex = "CREATE INDEX IF NOT EXISTS idx_match_date ON matches(match_date DESC)";
        String createWinnerIndex = "CREATE INDEX IF NOT EXISTS idx_winner ON matches(winner_uuid)";
        String createLoserIndex = "CREATE INDEX IF NOT EXISTS idx_loser ON matches(loser_uuid)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createMatchesTable);
            stmt.execute(createPlayerNameIndex);
            stmt.execute(createEloIndex);
            stmt.execute(createMatchDateIndex);
            stmt.execute(createWinnerIndex);
            stmt.execute(createLoserIndex);
        }
    }

    public CompletableFuture<Void> updatePlayerAsync(UUID uuid, String playerName) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO players (uuid, player_name, created_at, updated_at) 
                VALUES (?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET 
                    player_name = excluded.player_name,
                    updated_at = excluded.updated_at
            """;
            
            long currentTime = System.currentTimeMillis();
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, playerName);
                stmt.setLong(3, currentTime);
                stmt.setLong(4, currentTime);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update player: " + e.getMessage());
            }
        }, executor);
    }

    public CompletableFuture<PlayerStats> getPlayerStatsAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT elo, wins, losses FROM players WHERE uuid = ?";
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new PlayerStats(
                        rs.getInt("elo"),
                        rs.getInt("wins"),
                        rs.getInt("losses")
                    );
                }
                return new PlayerStats(1200, 0, 0);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get player stats: " + e.getMessage());
                return new PlayerStats(1200, 0, 0);
            }
        }, executor);
    }

    public CompletableFuture<Void> recordMatchAsync(UUID winnerId, UUID loserId, 
                                                   int winnerEloBefore, int winnerEloAfter,
                                                   int loserEloBefore, int loserEloAfter) {
        return CompletableFuture.runAsync(() -> {
            Connection conn = getConnection();
            try {
                conn.setAutoCommit(false);
                
                // Record the match
                String matchSql = """
                    INSERT INTO matches (winner_uuid, loser_uuid, winner_elo_before, winner_elo_after,
                                       loser_elo_before, loser_elo_after, elo_change, match_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
                
                try (PreparedStatement matchStmt = conn.prepareStatement(matchSql)) {
                    matchStmt.setString(1, winnerId.toString());
                    matchStmt.setString(2, loserId.toString());
                    matchStmt.setInt(3, winnerEloBefore);
                    matchStmt.setInt(4, winnerEloAfter);
                    matchStmt.setInt(5, loserEloBefore);
                    matchStmt.setInt(6, loserEloAfter);
                    matchStmt.setInt(7, winnerEloAfter - winnerEloBefore);
                    matchStmt.setLong(8, System.currentTimeMillis());
                    matchStmt.executeUpdate();
                }
                
                // Update winner stats
                String updateWinnerSql = """
                    UPDATE players SET elo = ?, wins = wins + 1, updated_at = ? WHERE uuid = ?
                """;
                try (PreparedStatement winnerStmt = conn.prepareStatement(updateWinnerSql)) {
                    winnerStmt.setInt(1, winnerEloAfter);
                    winnerStmt.setLong(2, System.currentTimeMillis());
                    winnerStmt.setString(3, winnerId.toString());
                    winnerStmt.executeUpdate();
                }
                
                // Update loser stats
                String updateLoserSql = """
                    UPDATE players SET elo = ?, losses = losses + 1, updated_at = ? WHERE uuid = ?
                """;
                try (PreparedStatement loserStmt = conn.prepareStatement(updateLoserSql)) {
                    loserStmt.setInt(1, loserEloAfter);
                    loserStmt.setLong(2, System.currentTimeMillis());
                    loserStmt.setString(3, loserId.toString());
                    loserStmt.executeUpdate();
                }
                
                conn.commit();
                conn.setAutoCommit(true);
                
            } catch (SQLException e) {
                try {
                    conn.rollback();
                    conn.setAutoCommit(true);
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().severe("Failed to rollback transaction: " + rollbackEx.getMessage());
                }
                plugin.getLogger().warning("Failed to record match: " + e.getMessage());
            }
        }, executor);
    }

    public CompletableFuture<List<LeaderboardEntry>> getLeaderboardAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT uuid, player_name, elo, wins, losses 
                FROM players 
                WHERE wins + losses > 0
                ORDER BY elo DESC 
                LIMIT ?
            """;
            
            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();
                
                int rank = 1;
                while (rs.next()) {
                    leaderboard.add(new LeaderboardEntry(
                        rank++,
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("player_name"),
                        rs.getInt("elo"),
                        rs.getInt("wins"),
                        rs.getInt("losses")
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get leaderboard: " + e.getMessage());
            }
            
            return leaderboard;
        }, executor);
    }

    public CompletableFuture<List<MatchRecord>> getPlayerMatchHistoryAsync(UUID uuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT m.*, 
                       wp.player_name as winner_name, 
                       lp.player_name as loser_name
                FROM matches m
                JOIN players wp ON m.winner_uuid = wp.uuid
                JOIN players lp ON m.loser_uuid = lp.uuid
                WHERE m.winner_uuid = ? OR m.loser_uuid = ?
                ORDER BY m.match_date DESC
                LIMIT ?
            """;
            
            List<MatchRecord> matches = new ArrayList<>();
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, uuid.toString());
                stmt.setInt(3, limit);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    matches.add(new MatchRecord(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("winner_uuid")),
                        UUID.fromString(rs.getString("loser_uuid")),
                        rs.getString("winner_name"),
                        rs.getString("loser_name"),
                        rs.getInt("winner_elo_before"),
                        rs.getInt("winner_elo_after"),
                        rs.getInt("loser_elo_before"),
                        rs.getInt("loser_elo_after"),
                        new Timestamp(rs.getLong("match_date"))
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get match history: " + e.getMessage());
            }
            
            return matches;
        }, executor);
    }

    public CompletableFuture<Void> executeAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    public <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
                connection.setAutoCommit(true);
            }
            return connection;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get database connection: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        executor.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
        }
    }

    // Data classes
    public record PlayerStats(int elo, int wins, int losses) {}
    
    public record LeaderboardEntry(int rank, UUID uuid, String playerName, int elo, int wins, int losses) {}
}