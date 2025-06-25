package me._2818.partyTS.duels;

import lombok.Getter;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.commands.CommandHeat;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelsManager {
    private final Plugin plugin;
    @Getter
    private final Set<Heat> activeDuels = new HashSet<>();
    private final Map<UUID, DuelInvite> pendingInvites = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> inviteCooldowns = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public DuelsManager(Plugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    private void startCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupTasks, 20L, 20L);
    }

    public void disable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    public boolean createDuelInvite(Player challenger, Player target, Track track, int laps, int pits) {
        if (hasPendingInvite(target)) {
            return false;
        }

        DuelInvite invite = new DuelInvite(challenger.getUniqueId(), target.getUniqueId(), track, laps, pits, plugin);
        pendingInvites.put(target.getUniqueId(), invite);
        inviteCooldowns.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(challenger.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    public boolean acceptDuel(Player player) {
        DuelInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            return false;
        }

        Player challenger = Bukkit.getPlayer(invite.getChallenger());
        if (challenger == null || !challenger.isOnline()) {
            player.sendMessage("§cYou don't have any pending duel requests!");
            return false;
        }

        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (maybeDriver.isPresent()) {
            player.sendMessage("§cYou are already in a heat!");
            return false;
        }

        var maybeDriver2 = TimingSystemAPI.getDriverFromRunningHeat(challenger.getUniqueId());
        if (maybeDriver2.isPresent()) {
            player.sendMessage("§cThat player is already in a heat!");
            return false;
        }

        return startDuel(challenger, player, invite.getTrack(), invite.getLaps(), invite.getPits());
    }

    public boolean declineDuel(Player player) {
        DuelInvite invite = pendingInvites.remove(player.getUniqueId());
        return invite != null && !invite.isExpired();
    }

    public boolean hasPendingInvite(Player player) {
        DuelInvite invite = pendingInvites.get(player.getUniqueId());
        return invite != null && !invite.isExpired();
    }

    private boolean startDuel(Player player, Player target, Track track, int laps, int pits) {
        final String name = player.getName() + "_vs_" + target.getName();
        Optional<Event> maybeEvent = EventDatabase.eventNew(player.getUniqueId(), name);
        if(maybeEvent.isEmpty()) return false;
        Event event = maybeEvent.get();
        event.setTrack(track);

        if(!EventDatabase.roundNew(event, RoundType.FINAL, 1)) return false;

        Optional<Round> maybeRound = event.eventSchedule.getRound(1);
        if(maybeRound.isEmpty()) return false;
        Round round = maybeRound.get();

        round.createHeat(1);
        var maybeHeat = round.getHeat("R1F1");
        if(maybeHeat.isEmpty()) return false;
        Heat heat = maybeHeat.get();

        laps = Math.max(1, laps);
        pits = Math.max(0, pits);

        if(track.isStage()) {
            heat.setTotalLaps(1);
            heat.setTotalPits(0);
        } else {
            heat.setTotalLaps(laps);
            heat.setTotalPits(pits);
        }

        HeatState state = heat.getHeatState();
        if (state != HeatState.SETUP && !heat.resetHeat()) return false;

        if (!heat.loadHeat()) {
            EventDatabase.removeEventHard(event);
            event = null;
            round = null;
            heat = null;
            return false;
        }

        EventDatabase.heatDriverNew(player.getUniqueId(), heat, heat.getDrivers().size() + 1);
        EventDatabase.heatDriverNew(target.getUniqueId(), heat, heat.getDrivers().size() + 1);

        CommandHeat.onSortByRandom(player, heat);

        final Heat finalHeat = heat;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finalHeat.startCountdown(10);
        }, 20L);

        int maxDuelTime = plugin.getConfig().getInt("maxdueltime", 900) * 20;

        Bukkit.getScheduler().runTaskLater(plugin, () -> endDuel(finalHeat), maxDuelTime);

        activeDuels.add(heat);
        return true;
    }

    public void endDuel(Heat heat) {
        activeDuels.remove(heat);

        Round round = heat.getRound();
        Event event = round.getEvent();

        heat.finishHeat();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            round.finish(event);
        }, 20L);

        Bukkit.getScheduler().runTaskLater(plugin, event::finish, 40L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            EventDatabase.removeEventHard(event);
        }, 60L);
    }

    private void cleanupTasks() {
        long now = System.currentTimeMillis();
        long cooldownMillis = plugin.getConfig().getLong("duelinvitetimeout", 30) * 1000L;

        pendingInvites.entrySet().removeIf(entry -> {
            DuelInvite invite = entry.getValue();
            if (invite.isExpired()) {
                Player target = Bukkit.getPlayer(entry.getKey());
                Player challenger = Bukkit.getPlayer(invite.getChallenger());

                if (target != null && target.isOnline()) {
                    target.sendMessage("§cYour duel invite has expired!");
                }


                if (challenger != null && challenger.isOnline()) {
                    challenger.sendMessage("§cYour duel invite to " +
                            (target != null ? target.getName() : "a player") +
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

    public void cleanupDuels() {
        plugin.getLogger().info("Cleaning up " + activeDuels.size() + " active duel(s)...");
        for (Heat heat : new HashSet<>(activeDuels)) {
            try {
                plugin.getLogger().info("Force-ending duel: " + heat.getRound().getEvent().getDisplayName());
                Round round = heat.getRound();
                Event event = round.getEvent();

                heat.finishHeat();
                round.finish(event);
                event.finish();
                EventDatabase.removeEventHard(event);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to cleanup a duel: " + e.getMessage());
            }
        }
        activeDuels.clear();
        plugin.getLogger().info("Duels cleanup complete.");
    }
}
