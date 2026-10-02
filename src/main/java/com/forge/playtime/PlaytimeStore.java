package com.forge.playtime;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

/** Persists per-player playtime seconds and claimed milestone indexes. */
public final class PlaytimeStore {

    private final ForgePlaytime plugin;
    private final File file;
    private final FileConfiguration data;

    public PlaytimeStore(ForgePlaytime plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playtimes.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    private String path(UUID id, String key) {
        return "players." + id + "." + key;
    }

    public long getSeconds(UUID id) {
        return data.getLong(path(id, "seconds"), 0L);
    }

    public String getName(UUID id) {
        return data.getString(path(id, "name"), "Unknown");
    }

    /** Ensures an entry exists for a joining player. */
    public void touch(UUID id, String name) {
        data.set(path(id, "name"), name);
        if (!data.isSet(path(id, "seconds"))) {
            data.set(path(id, "seconds"), 0L);
        }
    }

    public void addSecond(UUID id, String name) {
        data.set(path(id, "seconds"), getSeconds(id) + 1);
        data.set(path(id, "name"), name);
    }

    public boolean isClaimed(UUID id, int milestoneIndex) {
        return getClaimed(id).contains(milestoneIndex);
    }

    public Set<Integer> getClaimed(UUID id) {
        return new HashSet<>(data.getIntegerList(path(id, "claimed")));
    }

    public void setClaimed(UUID id, int milestoneIndex) {
        Set<Integer> claimed = getClaimed(id);
        claimed.add(milestoneIndex);
        data.set(path(id, "claimed"), claimed.stream().sorted().toList());
    }

    /**
     * Finds a stored UUID by last-known player name (case-insensitive).
     *
     * @return the matching UUID, or {@code null} when no playtime data exists
     *         for that name
     */
    public @Nullable UUID findByName(String name) {
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return null;
        }
        for (String key : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (name.equalsIgnoreCase(data.getString("players." + key + ".name", ""))) {
                return id;
            }
        }
        return null;
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save playtimes.yml", ex);
        }
    }
}
