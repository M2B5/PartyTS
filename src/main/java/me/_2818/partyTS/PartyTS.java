package me._2818.partyTS;

import me._2818.partyTS.commands.PartyCommand;
import me._2818.partyTS.commands.PartyRaceCommand;
import me._2818.partyTS.commands.PartyTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class PartyTS extends JavaPlugin {
    private PartyManager partyManager;
    private PartyRaceManager partyRaceManager;

    @Override
    public void onEnable() {
        partyManager = new PartyManager(this);
        partyRaceManager = new PartyRaceManager(partyManager, this);

        getCommand("party").setExecutor(new PartyCommand(partyManager));
        getCommand("party").setTabCompleter(new PartyTabCompleter(partyManager));
        getCommand("partyrace").setExecutor(new PartyRaceCommand(partyManager, this));

        getLogger().info("PartyTS has been enabled!");
    }

    @Override
    public void onDisable() {
        if (partyManager != null) {
            partyManager.disable();
        }
        getLogger().info("PartyTS has been disabled!");
    }
}
