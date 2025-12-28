package me._2818.partyTS.commands;

import me._2818.partyTS.duels.DuelsManager;
import me._2818.partyTS.utils.FontInfo;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class DuelCommand implements CommandExecutor {
    private final Plugin plugin;
    private final DuelsManager duelsManager;

    private final TextColor primaryColor;
    private final TextColor secondaryColor;
    private final TextColor defaultColor;
    private final TextColor acceptColor;
    private final TextColor denyColor;

    public DuelCommand(Plugin plugin, DuelsManager duelsManager) {
        this.plugin = plugin;
        this.duelsManager = duelsManager;

        this.primaryColor = TextColor.fromHexString("#" + plugin.getConfig().getString("primarycolour", "0260C6"));
        this.secondaryColor = TextColor.fromHexString("#" + plugin.getConfig().getString("secondarycolour", "4cafff"));
        this.defaultColor = TextColor.fromHexString("#" + plugin.getConfig().getString("defaultcolour", "ffffff"));
        this.acceptColor = TextColor.fromHexString("#" + plugin.getConfig().getString("acceptcolour", "23ff00"));
        this.denyColor = TextColor.fromHexString("#" + plugin.getConfig().getString("denycolour", "ff0000"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "accept":
                handleAccept(player);
                break;
            case "deny":
                handleDeny(player);
                break;
            case "help":
                sendHelp(player);
                break;
            case "elo":
            case "rating":
                handleEloCommand(player, args);
                break;
            case "leaderboard":
            case "top":
                handleLeaderboardCommand(player);
                break;
            default:
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /duel <player> [track] [laps] [pits] [collisions] [drs] [ranked]");
                    return true;
                }
                handleChallenge(player, args);
                break;
        }

        return true;
    }

    private void handleChallenge(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUsage: /duel <player> [track] [laps] [pits] [collisions] [drs] [ranked]");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer not found or not online!");
            return;
        }

        if (player.equals(target)) {
            player.sendMessage("§cYou cannot duel yourself!");
            return;
        }

        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());

        if (maybeDriver.isPresent()) {
            player.sendMessage("§cYou are already in a heat!");
            return;
        }

        var maybeDriver2 = TimingSystemAPI.getDriverFromRunningHeat(target.getUniqueId());

        if (maybeDriver2.isPresent()) {
            player.sendMessage("§cThat player is already in a heat!");
            return;
        }

        Track track = null;
        if (args.length >= 2) {
            Optional<Track> possibleTrack = TimingSystemAPI.getTrack(args[1]);
            if (possibleTrack.isEmpty()) {
                player.sendMessage("§cTrack not found!");
                return;
            }
            track = possibleTrack.get();
        } else {
            player.sendMessage("§cPlease specify a track!");
            return;
        }

        if (!track.isOpen()) {
            player.sendMessage("§cTrack is not open!");
            return;
        }

        if (track.getTrackLocations().getLocations(TrackLocation.Type.GRID).size() < 2) {
            player.sendMessage("§cNot enough grids on this track!");
            return;
        }

        int laps = 3;
        if (args.length >= 3) {
            try {
                laps = Math.min(Integer.parseInt(args[2]), plugin.getConfig().getInt("maxlapsduels", 10));
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

        boolean collisions = true;
        if (args.length >= 5) {
            try {
                String collisionMode = args[4].toLowerCase();
                if (!collisionMode.equals("off") && !collisionMode.equals("on")) {
                    player.sendMessage("§cInvalid collision mode! Use 'on' or 'off'.");
                    return;
                }

                if (collisionMode.equals("off")) {
                    collisions = false;
                }
            } catch (Exception e) {
                player.sendMessage("§cInvalid collision mode! Use 'on' or 'off'.");
                return;
            }
        }

        boolean drs = false;
        if (args.length >= 6) {
            try {
                String drsMode = args[5].toLowerCase();
                if (!drsMode.equals("off") && !drsMode.equals("on")) {
                    player.sendMessage("§cInvalid DRS mode! Use 'on' or 'off'.");
                    return;
                }

                if (drsMode.equals("on")) {
                    if (!collisions) {
                        player.sendMessage("§cDRS cannot be enabled when collisions are off.");
                    } else {
                        drs = true;
                    }
                }
            } catch (Exception e) {
                player.sendMessage("§cInvalid DRS mode! Use 'on' or 'off'.");
                return;
            }
        }

        boolean ranked = false;
        if (args.length >= 7) {
            if (!duelsManager.isRankedDuelsEnabled()) {
                player.sendMessage("§cRanked duels are not enabled on this server!");
                return;
            }
            
            try {
                String rankedMode = args[6].toLowerCase();
                if (!rankedMode.equals("false") && !rankedMode.equals("true")) {
                    player.sendMessage("§cInvalid ranked mode! Use 'true' or 'false'.");
                    return;
                }
                ranked = rankedMode.equals("true");
            } catch (Exception e) {
                player.sendMessage("§cInvalid ranked mode! Use 'true' or 'false'.");
                return;
            }
        }

        if (duelsManager.createDuelInvite(player, target, track, laps, pits, collisions, drs, ranked)) {
            sendDuelRequest(player, target, track, laps, pits, collisions, drs, ranked);
        } else {
            player.sendMessage("§cCould not send duel request. The player might already have a pending invite.");
        }
    }

    private void handleAccept(Player player) {
        if (duelsManager.acceptDuel(player)) {
            player.sendMessage(Component.text("Duel accepted! Preparing the race...", acceptColor));
        } else {
            player.sendMessage("§cYou don't have any pending duel requests!");
        }
    }

    private void handleDeny(Player player) {
        if (duelsManager.declineDuel(player)) {
            player.sendMessage(Component.text("Duel request declined.", denyColor));
        } else {
            player.sendMessage("§cYou don't have any pending duel requests!");
        }
    }

    private void handleEloCommand(Player player, String[] args) {
        if (!duelsManager.isRankedDuelsEnabled()) {
            player.sendMessage("§cRanked duels are not enabled on this server!");
            return;
        }

        Player targetPlayer = player;
        UUID targetId = player.getUniqueId();
        String targetName = player.getName();
        
        if (args.length >= 2) {
            targetPlayer = plugin.getServer().getPlayer(args[1]);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                // Player is online
                targetId = targetPlayer.getUniqueId();
                targetName = targetPlayer.getName();
            } else {
                // Player is offline, try to get their UUID from offline player
                org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(args[1]);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetId = offlinePlayer.getUniqueId();
                    targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : args[1];
                    targetPlayer = null; // Set to null to indicate offline player
                } else {
                    player.sendMessage("§cPlayer not found!");
                    return;
                }
            }
        }

        final String finalTargetName = targetName;
        final Player finalTargetPlayer = targetPlayer;
        
        // Get stats asynchronously
        duelsManager.getEloSystem().getPlayerStatsAsync(targetId)
            .thenAccept(stats -> {
                int elo = stats.elo();
                int wins = stats.wins();
                int losses = stats.losses();
                int totalGames = wins + losses;

                String possessive = (finalTargetPlayer != null && finalTargetPlayer.equals(player)) ? "Your" : finalTargetName + "'s";

                Component eloMessage = Component.text()
                        .append(Component.text("=== ", secondaryColor))
                        .append(Component.text(possessive + " ELO Rating", primaryColor, TextDecoration.BOLD))
                        .append(Component.text(" ===", secondaryColor))
                        .append(Component.newline())
                        .append(Component.text("ELO: ", secondaryColor))
                        .append(Component.text(String.valueOf(elo), primaryColor, TextDecoration.BOLD))
                        .append(Component.newline())
                        .append(Component.text("Wins: ", secondaryColor))
                        .append(Component.text(String.valueOf(wins), acceptColor))
                        .append(Component.text(" | Losses: ", secondaryColor))
                        .append(Component.text(String.valueOf(losses), denyColor))
                        .append(Component.newline())
                        .append(Component.text("Total Games: ", secondaryColor))
                        .append(Component.text(String.valueOf(totalGames), defaultColor))
                        .build();

                player.sendMessage(eloMessage);
            })
            .exceptionally(throwable -> {
                player.sendMessage("§cFailed to retrieve ELO data: " + throwable.getMessage());
                return null;
            });
    }

    private void handleLeaderboardCommand(Player player) {
        if (!duelsManager.isRankedDuelsEnabled()) {
            player.sendMessage("§cRanked duels are not enabled on this server!");
            return;
        }

        duelsManager.getEloSystem().getTopPlayers(10)
            .thenAccept(topPlayers -> {
                if (topPlayers.isEmpty()) {
                    player.sendMessage("§cNo ranked duel data available yet!");
                    return;
                }

                TextComponent.Builder leaderboardBuilder = Component.text()
                        .append(Component.text("=== ", secondaryColor))
                        .append(Component.text("ELO Leaderboard", primaryColor, TextDecoration.BOLD))
                        .append(Component.text(" ===", secondaryColor))
                        .append(Component.newline());

                for (var entry : topPlayers) {
                    int position = entry.rank();
                    String playerName = entry.playerName() != null ? entry.playerName() : "Unknown";
                    int elo = entry.elo();
                    int wins = entry.wins();
                    int losses = entry.losses();
                    
                    TextColor positionColor = position <= 3 ? acceptColor : defaultColor;
                    
                    leaderboardBuilder.append(Component.text(position + ". ", positionColor))
                            .append(Component.text(playerName, defaultColor))
                            .append(Component.text(" - ", secondaryColor))
                            .append(Component.text(String.valueOf(elo), primaryColor))
                            .append(Component.text(" (", secondaryColor))
                            .append(Component.text(wins + "W", acceptColor))
                            .append(Component.text("/", secondaryColor))
                            .append(Component.text(losses + "L", denyColor))
                            .append(Component.text(")", secondaryColor))
                            .append(Component.newline());
                }

                player.sendMessage(leaderboardBuilder.build());
            })
            .exceptionally(throwable -> {
                player.sendMessage("§cFailed to retrieve leaderboard data: " + throwable.getMessage());
                return null;
            });
    }

    private void sendHelp(Player player) {
        TextComponent.Builder helpBuilder = Component.text()
                .append(Component.text("=== ", secondaryColor))
                .append(Component.text("Duel Commands", primaryColor, TextDecoration.BOLD))
                .append(Component.text(" ===", secondaryColor))
                .append(Component.newline())
                .append(Component.text("/duel <player> [track] [laps] [pits] [collisions] [drs] [ranked]", secondaryColor))
                .append(Component.text(" - Challenge a player to a duel", defaultColor))
                .append(Component.newline())
                .append(Component.text("/duel accept", secondaryColor))
                .append(Component.text(" - Accept a duel request", defaultColor))
                .append(Component.newline())
                .append(Component.text("/duel deny", secondaryColor))
                .append(Component.text(" - Deny a duel request", defaultColor))
                .append(Component.newline());

        if (duelsManager.isRankedDuelsEnabled()) {
            helpBuilder.append(Component.text("/duel elo [player]", secondaryColor))
                    .append(Component.text(" - View ELO rating", defaultColor))
                    .append(Component.newline())
                    .append(Component.text("/duel leaderboard", secondaryColor))
                    .append(Component.text(" - View ELO leaderboard", defaultColor))
                    .append(Component.newline());
        }

        helpBuilder.append(Component.text("/duel help", secondaryColor))
                .append(Component.text(" - Show this help message", defaultColor));

        player.sendMessage(helpBuilder.build());
    }

    private void sendDuelRequest(Player challenger, Player target, Track track, int laps, int pits, boolean collisions, boolean drsEnabled, boolean ranked) {
        if (ranked) {
            challenger.sendMessage(Component.text("Ranked duel request sent to " + target.getName() + "!", acceptColor));
        } else {
            challenger.sendMessage(Component.text("Duel request sent to " + target.getName() + "!", acceptColor));
        }

        String headerPrefix = "===== ";
        String headerTitle = ranked ? "Ranked Duel Request" : "Duel Request";
        String headerSuffix = " =====";

        String challengerName = challenger.getName();
        String targetName = target.getName();
        String vsText = " vs ";

        String collisionText = collisions ? "Collisions: ON" : "Collisions: OFF";
        String drsText = drsEnabled ? "DRS: ON" : "DRS: OFF";
        String rankedText = ranked ? "RANKED" : "CASUAL";
        String trackInfo = track.getDisplayName() + " | " + laps + " laps | " + pits + " pits | " + collisionText + " | " + drsText + " | " + rankedText;

        String acceptText = "[ACCEPT]";
        String denyText = "[DENY]";
        String buttonSeparator = "   ";

        String expirationText = "This invite will expire in " +
                plugin.getConfig().getInt("duelinvitetimeout", 30) + " seconds";

        String eloText = "";
        String eloChangesText = "";
        if (ranked && duelsManager.isRankedDuelsEnabled()) {
            int challengerElo = duelsManager.getPlayerElo(challenger.getUniqueId());
            int targetElo = duelsManager.getPlayerElo(target.getUniqueId());
            eloText = challengerName + " (" + challengerElo + ") vs " + targetName + " (" + targetElo + ")";
            
            // ELO changes will be calculated when the duel completes
            eloChangesText = "ELO changes will be calculated after the duel";
        }

        int headerWidth = FontInfo.getStringWidth(headerPrefix, false)
                + FontInfo.getStringWidth(headerTitle, true)
                + FontInfo.getStringWidth(headerSuffix, false);

        int playersWidth = FontInfo.getStringWidth(challengerName, false)
                + FontInfo.getStringWidth(vsText, false)
                + FontInfo.getStringWidth(targetName, false);

        int trackInfoWidth = FontInfo.getStringWidth(trackInfo, false);

        int buttonsWidth = FontInfo.getStringWidth(acceptText, true)
                + FontInfo.getStringWidth(buttonSeparator, false)
                + FontInfo.getStringWidth(denyText, true);

        int expirationWidth = FontInfo.getStringWidth(expirationText, false);
        int eloWidth = ranked ? FontInfo.getStringWidth(eloText, false) : 0;
        int eloChangesWidth = ranked ? FontInfo.getStringWidth(eloChangesText, false) : 0;

        int maxContentWidth = Math.max(headerWidth,
                Math.max(playersWidth, Math.max(trackInfoWidth, Math.max(buttonsWidth, Math.max(expirationWidth, Math.max(eloWidth, eloChangesWidth))))));
        int totalWidth = maxContentWidth + 50;

        String headerPadding = FontInfo.getPadding(totalWidth, headerWidth);
        String playersPadding = FontInfo.getPadding(totalWidth, playersWidth);
        String trackInfoPadding = FontInfo.getPadding(totalWidth, trackInfoWidth);
        String buttonsPadding = FontInfo.getPadding(totalWidth, buttonsWidth);
        String expirationPadding = FontInfo.getPadding(totalWidth, expirationWidth);
        String eloPadding = ranked ? FontInfo.getPadding(totalWidth, eloWidth) : "";
        String eloChangesPadding = ranked ? FontInfo.getPadding(totalWidth, eloChangesWidth) : "";

        TextComponent.Builder inviteBuilder = Component.text()
                .append(Component.text(headerPadding))
                .append(Component.text(headerPrefix, secondaryColor))
                .append(Component.text(headerTitle, primaryColor, TextDecoration.BOLD))
                .append(Component.text(headerSuffix, secondaryColor))
                .append(Component.newline())
                .append(Component.newline())

                .append(Component.text(playersPadding))
                .append(Component.text(challengerName, defaultColor))
                .append(Component.text(vsText, denyColor))
                .append(Component.text(targetName, defaultColor))
                .append(Component.newline())
                .append(Component.newline())

                .append(Component.text(trackInfoPadding))
                .append(Component.text(trackInfo, defaultColor))
                .append(Component.newline());

        if (ranked && !eloText.isEmpty()) {
            inviteBuilder.append(Component.newline())
                    .append(Component.text(eloPadding))
                    .append(Component.text(eloText, secondaryColor))
                    .append(Component.newline())
                    .append(Component.text(eloChangesPadding));
            
            // Parse the ELO changes to color them appropriately
            String[] parts = eloChangesText.split(" \\| ");
            if (parts.length == 2) {
                String winPart = parts[0]; // "If you win: +X ELO"
                String losePart = parts[1]; // "If you lose: -X ELO"
                
                inviteBuilder.append(Component.text(winPart, acceptColor))
                        .append(Component.text(" | ", secondaryColor))
                        .append(Component.text(losePart, denyColor));
            } else {
                inviteBuilder.append(Component.text(eloChangesText, NamedTextColor.YELLOW));
            }
        }

        inviteBuilder.append(Component.newline())
                .append(Component.text(buttonsPadding))
                .append(Component.text(acceptText, acceptColor, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/duel accept")))
                .append(Component.text(buttonSeparator, defaultColor))
                .append(Component.text(denyText, denyColor, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/duel deny")))
                .append(Component.newline())
                .append(Component.newline())

                .append(Component.text(expirationPadding))
                .append(Component.text(expirationText, NamedTextColor.GRAY));

        Component inviteMessage = inviteBuilder.build();
        target.sendMessage(inviteMessage);
    }
}
