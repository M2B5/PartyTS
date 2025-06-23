package me._2818.partyTS;

import me._2818.partyTS.commands.PartyCommand;
import me._2818.partyTS.commands.PartyTabCompleter;
import me._2818.partyTS.listeners.PartyListener;
import me._2818.partyTS.listeners.PartyRaceListener;
import me._2818.partyTS.party.PartyManager;
import me._2818.partyTS.party.PartyRaceManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PartyTS extends JavaPlugin {
    private PartyManager partyManager;
    private PartyRaceManager partyRaceManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        partyManager = new PartyManager(this);
        partyRaceManager = new PartyRaceManager(partyManager, this);

        // Register party command with both executor and tab completer
        getCommand("party").setExecutor(new PartyCommand(partyManager, this));
        getCommand("party").setTabCompleter(new PartyTabCompleter(partyManager));

        getServer().getPluginManager().registerEvents(new PartyRaceListener(partyManager, this, partyRaceManager), this);
        getServer().getPluginManager().registerEvents(new PartyListener(partyManager), this);

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
        getLogger().info("PartyTS has been disabled!");
    }
}
