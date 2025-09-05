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
            default:
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /duel <player> [track] [laps] [pits]");
                    return true;
                }
                handleChallenge(player, args);
                break;
        }

        return true;
    }

    private void handleChallenge(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUsage: /duel <player> [track] [laps] [pits]");
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

        if (duelsManager.createDuelInvite(player, target, track, laps, pits)) {
            sendDuelRequest(player, target, track, laps, pits);
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

    private void sendHelp(Player player) {
        TextComponent helpMessage = Component.text()
                .append(Component.text("=== ", secondaryColor))
                .append(Component.text("Duel Commands", primaryColor, TextDecoration.BOLD))
                .append(Component.text(" ===", secondaryColor))
                .append(Component.newline())
                .append(Component.text("/duel <player> [track] [laps] [pits]", secondaryColor))
                .append(Component.text(" - Challenge a player to a duel", defaultColor))
                .append(Component.newline())
                .append(Component.text("/duel accept", secondaryColor))
                .append(Component.text(" - Accept a duel request", defaultColor))
                .append(Component.newline())
                .append(Component.text("/duel deny", secondaryColor))
                .append(Component.text(" - Deny a duel request", defaultColor))
                .append(Component.newline())
                .append(Component.text("/duel help", secondaryColor))
                .append(Component.text(" - Show this help message", defaultColor))
                .build();
        player.sendMessage(helpMessage);
    }

    private void sendDuelRequest(Player challenger, Player target, Track track, int laps, int pits) {
        challenger.sendMessage(Component.text("Duel request sent to " + target.getName() + "!", acceptColor));

        String headerPrefix = "===== ";
        String headerTitle = "Duel Request";
        String headerSuffix = " =====";

        String challengerName = challenger.getName();
        String targetName = target.getName();
        String vsText = " vs ";

        String trackInfo = track.getDisplayName() + " | " + laps + " laps | " + pits + " pits";

        String acceptText = "[ACCEPT]";
        String denyText = "[DENY]";
        String buttonSeparator = "   ";

        String expirationText = "This invite will expire in " +
                plugin.getConfig().getInt("duelinvitetimeout", 30) + " seconds";

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

        int maxContentWidth = Math.max(headerWidth,
                Math.max(playersWidth, Math.max(trackInfoWidth, Math.max(buttonsWidth, expirationWidth))));
        int totalWidth = maxContentWidth + 50;

        String headerPadding = FontInfo.getPadding(totalWidth, headerWidth);
        String playersPadding = FontInfo.getPadding(totalWidth, playersWidth);
        String trackInfoPadding = FontInfo.getPadding(totalWidth, trackInfoWidth);
        String buttonsPadding = FontInfo.getPadding(totalWidth, buttonsWidth);
        String expirationPadding = FontInfo.getPadding(totalWidth, expirationWidth);

        Component inviteMessage = Component.text()
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
                .append(Component.newline())
                .append(Component.newline())

                .append(Component.text(buttonsPadding))
                .append(Component.text(acceptText, acceptColor, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/duel accept")))
                .append(Component.text(buttonSeparator, defaultColor))
                .append(Component.text(denyText, denyColor, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/duel deny")))
                .append(Component.newline())
                .append(Component.newline())

                .append(Component.text(expirationPadding))
                .append(Component.text(expirationText, NamedTextColor.GRAY))
                .build();

        target.sendMessage(inviteMessage);
    }
}
