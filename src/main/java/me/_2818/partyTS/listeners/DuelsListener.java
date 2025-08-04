package me._2818.partyTS.listeners;

import me._2818.partyTS.duels.DuelsManager;
import me.makkuusen.timing.system.api.events.driver.DriverFinishHeatEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class DuelsListener implements Listener {
    private final DuelsManager duelsManager;
    private final Plugin plugin;

    public DuelsListener(DuelsManager duelsManager, Plugin plugin) {
        this.duelsManager = duelsManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerFinish(DriverFinishHeatEvent event) {
        if (!duelsManager.getActiveDuels().contains(event.getDriver().getHeat())) {
            return;
        }

        if (event.getDriver().getPosition() == 2) {
            event.getDriver().getHeat().finishHeat();
            return;
        }

        int endDuelAfterWinTime = plugin.getConfig().getInt("endduelafterwin", 30);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (duelsManager.getActiveDuels().contains(event.getDriver().getHeat())) {
                duelsManager.endDuel(event.getDriver().getHeat());
            }
        }, endDuelAfterWinTime * 20L);
    }
}
