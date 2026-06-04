package dev.nxauth.config;

import dev.nxauth.NxAuth;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageManager {

    private final NxAuth plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLang;

    public MessageManager(NxAuth plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        languages.clear();
        defaultLang = plugin.getConfigManager().getDefaultLanguage();

        // Load built-in languages from jar
        String[] builtIn = {"en", "hi", "es", "de", "fr", "pt", "ru", "zh", "tr"};
        for (String lang : builtIn) {
            loadLanguage(lang);
        }

        // Load custom languages from plugin folder
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (langFolder.exists()) {
            File[] files = langFolder.listFiles((d, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String lang = file.getName().replace("messages_", "").replace(".yml", "");
                    if (!languages.containsKey(lang)) {
                        languages.put(lang, YamlConfiguration.loadConfiguration(file));
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + languages.size() + " languages: " + String.join(", ", languages.keySet()));
    }

    private void loadLanguage(String lang) {
        // First try plugin data folder
        File file = new File(plugin.getDataFolder(), "languages/messages_" + lang + ".yml");
        if (file.exists()) {
            languages.put(lang, YamlConfiguration.loadConfiguration(file));
            return;
        }

        // Then try jar resources
        InputStream stream = plugin.getResource("languages/messages_" + lang + ".yml");
        if (stream != null) {
            languages.put(lang, YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
    }

    public String get(String key, Player player) {
        String lang = getPlayerLang(player);
        return get(key, lang);
    }

    public String get(String key, String lang) {
        FileConfiguration config = languages.getOrDefault(lang, languages.get(defaultLang));
        if (config == null) return "&cMissing message: " + key;

        String message = config.getString(key);
        if (message == null) {
            // Fallback to English
            FileConfiguration en = languages.get("en");
            if (en != null) message = en.getString(key);
        }
        if (message == null) return "&cMissing: " + key;

        return colorize(message);
    }

    public String get(String key, Player player, Map<String, String> placeholders) {
        String message = get(key, player);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String get(String key, String lang, Map<String, String> placeholders) {
        String message = get(key, lang);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public void setPlayerLanguage(UUID uuid, String lang) {
        if (languages.containsKey(lang)) {
            playerLanguages.put(uuid, lang);
            // TODO: Save to database
        }
    }

    public String getPlayerLang(Player player) {
        if (player == null) return defaultLang;
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLang);
    }

    public boolean isValidLanguage(String lang) {
        return languages.containsKey(lang);
    }

    public String getAvailableLanguages() {
        return String.join(", ", languages.keySet());
    }

    public void sendMessage(Player player, String key) {
        player.sendMessage(get(key, player));
    }

    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(get(key, player, placeholders));
    }

    private String colorize(String message) {
        return message.replace("&", "\u00A7");
    }
}
