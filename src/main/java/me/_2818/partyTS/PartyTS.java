package me._2818.partyTS;

import lombok.Getter;
import me._2818.partyTS.commands.DuelCommand;
import me._2818.partyTS.commands.DuelCommandTabCompleter;
import me._2818.partyTS.commands.PartyCommand;
import me._2818.partyTS.commands.PartyTabCompleter;
import me._2818.partyTS.duels.DuelsManager;
import me._2818.partyTS.listeners.DisconnectListener;
import me._2818.partyTS.listeners.DuelsListener;
import me._2818.partyTS.listeners.PartyRaceListener;
import me._2818.partyTS.listeners.ScoreboardListener;
import me._2818.partyTS.party.PartyManager;
import me._2818.partyTS.party.PartyRaceManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PartyTS extends JavaPlugin {
    private PartyManager partyManager;
    private PartyRaceManager partyRaceManager;
    @Getter
    private DuelsManager duelsManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        partyManager = new PartyManager(this);
        partyRaceManager = new PartyRaceManager(partyManager, this);
        duelsManager = new DuelsManager(this);

        getCommand("party").setExecutor(new PartyCommand(partyManager, partyRaceManager, this));
        getCommand("party").setTabCompleter(new PartyTabCompleter(partyManager));
        
        getCommand("duel").setExecutor(new DuelCommand(this, duelsManager));
        getCommand("duel").setTabCompleter(new DuelCommandTabCompleter());

        getServer().getPluginManager().registerEvents(new PartyRaceListener(partyManager, this, partyRaceManager), this);
        getServer().getPluginManager().registerEvents(new DuelsListener(duelsManager, this), this);
        getServer().getPluginManager().registerEvents(new DisconnectListener(partyRaceManager, duelsManager, partyManager), this);
        getServer().getPluginManager().registerEvents(new ScoreboardListener(), this);

        getLogger().info("PartyTS has been enabled!");
    }

    @Override
    public void onDisable() {
        if (partyRaceManager != null) {
            partyRaceManager.cleanupRaces();
        }
        if (partyManager != null) {
            partyManager.disable();
        }
        if (duelsManager != null) {
            duelsManager.disable();
        }
        getLogger().info("PartyTS has been disabled!");
    }

}
