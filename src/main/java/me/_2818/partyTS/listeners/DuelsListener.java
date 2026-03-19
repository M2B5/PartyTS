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
            UUID winnerId = event.getDriver().getTPlayer().getUniqueId();
            plugin.getLogger().info("Setting duel winner: " + winnerId);
            firstFinishers.put(heat, winnerId);
            duelsManager.setDuelWinner(heat, winnerId);
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
