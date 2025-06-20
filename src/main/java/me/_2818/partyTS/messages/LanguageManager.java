package me._2818.partyTS.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
        String lang = plugin.getConfig().getString("language", "en_us");
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        File langFile = new File(langFolder, lang + ".yml");
        
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
            plugin.getLogger().info("Created language file: " + langFile.getAbsolutePath());
        }
        
        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        YamlConfiguration defaultConfig;
        try (InputStream defaultStream = plugin.getResource("lang/en_us.yml")) {
            if (defaultStream == null) {
                throw new IllegalStateException("Could not find default language file in JAR");
            }
            defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load default language file", e);
        }
        
        boolean needsSave = false;
        for (String key : defaultConfig.getKeys(true)) {
            if (!langConfig.contains(key)) {
                langConfig.set(key, defaultConfig.get(key));
                needsSave = true;
            }
        }
        
        if (needsSave) {
            try {
                langConfig.save(langFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save updated language file: " + e.getMessage());
            }
        }
        
        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                messages.put(key, langConfig.getString(key));
                plugin.getLogger().fine("Loaded message: " + key + " = " + langConfig.getString(key));
            }
        }
        
        if (messages.isEmpty()) {
            plugin.getLogger().warning("No messages were loaded from the language file!");
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
            return String.format(message, args);
        }
        return message;
    }
}
