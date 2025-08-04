package me._2818.partyTS.listeners;

import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.api.events.driver.DriverScoreboardTitleUpdateEvent;
import me.makkuusen.timing.system.api.events.driver.SpectatorScoreboardTitleUpdateEvent;
import me.makkuusen.timing.system.heat.ScoreboardUtils;
import me.makkuusen.timing.system.tplayer.TPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ScoreboardListener implements Listener {
    @EventHandler
    public void onDriverScoreboardUpdate(DriverScoreboardTitleUpdateEvent event) {
        String title = event.getTitle();
        if (title.contains("Party_Ra") && (event.getPlayer() != null)) {
            Player player = event.getPlayer();
            TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
            tPlayer.setScoreBoardTitle(Component.text("Party Race").color(ScoreboardUtils.getPrimaryColor(tPlayer.getTheme())).decorate(TextDecoration.BOLD));
        }
    }

    @EventHandler
    public void onSpectatorScoreboardUpdate(SpectatorScoreboardTitleUpdateEvent event) {
        String title = event.getTitle();
        if (title.contains("Party_Ra") && (event.getPlayer() != null)) {
            Player player = event.getPlayer();
            TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
            tPlayer.setScoreBoardTitle(Component.text("Party Race").color(ScoreboardUtils.getPrimaryColor(tPlayer.getTheme())).decorate(TextDecoration.BOLD));
        }
    }
}
