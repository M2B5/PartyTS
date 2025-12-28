package me._2818.partyTS.listeners;

import me._2818.partyTS.duels.DuelsManager;
import me.makkuusen.timing.system.api.events.driver.DriverFinishHeatEvent;
import me.makkuusen.timing.system.heat.Heat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class DuelsListener implements Listener {
    private final DuelsManager duelsManager;
    private final Plugin plugin;
    private final Map<Heat, UUID> firstFinishers = new ConcurrentHashMap<>();

    public DuelsListener(DuelsManager duelsManager, Plugin plugin) {
        this.duelsManager = duelsManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerFinish(DriverFinishHeatEvent event) {
        Heat heat = event.getDriver().getHeat();
        if (!duelsManager.getActiveDuels().contains(heat)) {
            return;
        }

        if (event.getDriver().getPosition() == 1) {
            // First place finisher - determine winner from participants
            plugin.getLogger().info("Player finished in position 1 in duel");
            UUID[] participants = duelsManager.getDuelParticipants(heat);
            if (participants != null && participants.length == 2) {
                // For now, use a simple approach: the first online participant is the winner
                // This is a temporary solution until we can properly match drivers to UUIDs
                UUID winnerId = participants[0];
                Player player1 = Bukkit.getPlayer(participants[0]);
                Player player2 = Bukkit.getPlayer(participants[1]);
                
                // Simple heuristic: if only one player is online, they're probably the winner
                if (player1 != null && player1.isOnline() && (player2 == null || !player2.isOnline())) {
                    winnerId = participants[0];
                } else if (player2 != null && player2.isOnline() && (player1 == null || !player1.isOnline())) {
                    winnerId = participants[1];
                } else {
                    // Both online or both offline - use first participant as fallback
                    winnerId = participants[0];
                }
                
                plugin.getLogger().info("Setting duel winner: " + winnerId);
                firstFinishers.put(heat, winnerId);
                duelsManager.setDuelWinner(heat, winnerId);
            }
        }

        if (event.getDriver().getPosition() == 2) {
            firstFinishers.remove(heat);
            event.getDriver().getHeat().finishHeat();
            return;
        }

        int endDuelAfterWinTime = plugin.getConfig().getInt("endduelafterwin", 30);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (duelsManager.getActiveDuels().contains(heat)) {
                firstFinishers.remove(heat);
                duelsManager.endDuel(heat);
            }
        }, endDuelAfterWinTime * 20L);
    }
}
