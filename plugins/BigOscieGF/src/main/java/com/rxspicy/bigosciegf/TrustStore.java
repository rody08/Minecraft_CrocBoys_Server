package com.rxspicy.bigosciegf;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Persistent, UUID-keyed player trust. Runtime player data stays in the plugin data directory. */
final class TrustStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Entry> entries = new HashMap<>();

    TrustStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "trust.yml");
        load();
    }

    synchronized int score(UUID playerId, String playerName, String ownerName, int maximum) {
        if (isOwner(playerName, ownerName)) return maximum;
        Entry entry = entries.get(playerId);
        return entry == null ? 50 : normalize(entry.score, maximum);
    }

    synchronized int set(UUID playerId, String playerName, int score, int maximum) {
        int normalized = normalize(score, maximum);
        entries.put(playerId, new Entry(playerName, normalized, System.currentTimeMillis()));
        save();
        return normalized;
    }

    private void load() {
        if (!file.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) return;
        for (String rawId : players.getKeys(false)) {
            try {
                UUID id = UUID.fromString(rawId);
                String path = "players." + rawId;
                entries.put(id, new Entry(
                        yaml.getString(path + ".last-name", "unknown"),
                        yaml.getInt(path + ".score", 0),
                        yaml.getLong(path + ".last-gain", 0L)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignored invalid player UUID in trust.yml: " + rawId);
            }
        }
    }

    synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Entry> saved : entries.entrySet()) {
            String path = "players." + saved.getKey();
            yaml.set(path + ".last-name", saved.getValue().lastName);
            yaml.set(path + ".score", saved.getValue().score);
            yaml.set(path + ".last-gain", saved.getValue().lastGain);
        }
        try {
            if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
                throw new IOException("Could not create plugin data directory");
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save trust.yml", e);
        }
    }

    private static boolean isOwner(String playerName, String ownerName) {
        return playerName != null && ownerName != null && playerName.equalsIgnoreCase(ownerName);
    }

    private static int normalize(int score, int maximum) {
        if (score <= 0) return 0;
        if (score >= maximum) return maximum;
        return 50;
    }

    private record Entry(String lastName, int score, long lastGain) {}
}
