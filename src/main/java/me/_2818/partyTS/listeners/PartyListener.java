package me._2818.partyTS.listeners;

import me._2818.partyTS.party.PartyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PartyListener implements Listener {
    private final PartyManager partyManager;

    public PartyListener(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        partyManager.leaveParty(player);
    }
}
