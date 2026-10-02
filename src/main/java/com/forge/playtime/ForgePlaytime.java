package com.forge.playtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ForgePlaytime - playtime tracking with claimable milestone rewards.
 */
public final class ForgePlaytime extends JavaPlugin {

    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smhdw])$");

    private PlaytimeStore store;
    private PlaytimeGui gui;
    private final List<Milestone> milestones = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.store = new PlaytimeStore(this);
        this.gui = new PlaytimeGui(this);
        loadMilestones();

        // Accumulate one second of playtime per online player, every second.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                store.addSecond(player.getUniqueId(), player.getName());
            }
        }, 20L, 20L);

        // Persist periodically so a crash loses at most a few minutes.
        Bukkit.getScheduler().runTaskTimer(this, store::save, 6000L, 6000L);

        Bukkit.getPluginManager().registerEvents(new PlaytimeListener(this), this);
        Bukkit.getPluginManager().registerEvents(gui, this);

        if (getCommand("playtime") != null) {
            getCommand("playtime").setExecutor(new PlaytimeCommand(this));
        }
        if (getCommand("fplaytime") != null) {
            getCommand("fplaytime").setExecutor(new AdminCommand(this));
        }

        getLogger().info("ForgePlaytime enabled with " + milestones.size() + " milestone(s).");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
    }

    public PlaytimeStore store() {
        return store;
    }

    public PlaytimeGui gui() {
        return gui;
    }

    public List<Milestone> milestones() {
        return List.copyOf(milestones);
    }

    /** (Re)loads the milestone list from config.yml. */
    public void loadMilestones() {
        milestones.clear();
        List<Map<?, ?>> raw = getConfig().getMapList("milestones");
        int index = 0;
        for (Map<?, ?> entry : raw) {
            try {
                milestones.add(Milestone.parse(index, entry));
            } catch (IllegalArgumentException ex) {
                getLogger().log(Level.WARNING, "Skipping invalid milestone #" + index + ": " + ex.getMessage());
            }
            index++;
        }
    }

    /**
     * Parses a human time string like {@code 30s}, {@code 45m}, {@code 2h},
     * {@code 1d} or {@code 1w} into seconds.
     */
    public static long parseTime(String raw) {
        Matcher matcher = TIME_PATTERN.matcher(raw.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("bad time '" + raw + "' (use like 30m, 2h, 1d)");
        }
        long amount = Long.parseLong(matcher.group(1));
        long multiplier = switch (matcher.group(2)) {
            case "s" -> 1L;
            case "m" -> 60L;
            case "h" -> 3_600L;
            case "d" -> 86_400L;
            case "w" -> 604_800L;
            default -> throw new IllegalArgumentException("bad unit in '" + raw + "'");
        };
        return Math.multiplyExact(amount, multiplier);
    }

    /** Formats seconds as a compact human string like {@code 1d 2h 3m}. */
    public static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append('d').append(' ');
        }
        if (hours > 0) {
            sb.append(hours).append('h').append(' ');
        }
        if (minutes > 0) {
            sb.append(minutes).append('m').append(' ');
        }
        if (seconds > 0 || sb.isEmpty()) {
            sb.append(seconds).append('s');
        }
        return sb.toString().trim();
    }
}
