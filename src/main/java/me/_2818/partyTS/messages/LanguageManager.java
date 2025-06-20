package me._2818.partyTS.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LanguageManager {
    private static LanguageManager instance;
    private final Map<String, String> messages = new HashMap<>();
    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    private LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new LanguageManager(plugin);
        }
    }
    
    public static LanguageManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LanguageManager has not been initialized!");
        }
        return instance;
    }
    
    private void loadMessages() {
        String lang = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }
        
        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
            new InputStreamReader(
                Objects.requireNonNull(plugin.getResource("lang/en.yml")),
                StandardCharsets.UTF_8
            )
        );
        
        for (String key : defaultConfig.getKeys(true)) {
            if (!langConfig.contains(key)) {
                langConfig.set(key, defaultConfig.get(key));
            }
        }
        
        try {
            langConfig.save(langFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save language file: " + e.getMessage());
        }
        
        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                messages.put(key, langConfig.getString(key));
            }
        }
    }
    
    public String getString(Message message, Object... args) {
        String msg = messages.getOrDefault(message.getKey(), "&cMissing message: " + message.getKey());
        return formatString(msg, args);
    }
    
    public Component getComponent(Message message, Object... args) {
        String msg = messages.getOrDefault(message.getKey(), "&cMissing message: " + message.getKey());
        return miniMessage.deserialize(formatString(msg, args));
    }
    
    private String formatString(String message, Object... args) {
        if (args.length > 0) {
            return String.format(message.replace("&", "§"), args);
        }
        return message.replace("&", "§");
    }
}
