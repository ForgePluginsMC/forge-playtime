package com.forge.playtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;

/** One configured playtime milestone. */
public final class Milestone {

    private final int index;
    private final long seconds;
    private final List<String> rewards;
    private final Material guiItem;
    private final String guiName;
    private final List<String> guiLore;

    private Milestone(int index, long seconds, List<String> rewards, Material guiItem, String guiName,
            List<String> guiLore) {
        this.index = index;
        this.seconds = seconds;
        this.rewards = rewards;
        this.guiItem = guiItem;
        this.guiName = guiName;
        this.guiLore = guiLore;
    }

    public static Milestone parse(int index, Map<?, ?> map) {
        Object timeObj = map.get("time");
        if (!(timeObj instanceof String timeStr)) {
            throw new IllegalArgumentException("missing 'time'");
        }
        long seconds = ForgePlaytime.parseTime(timeStr);

        List<String> rewards = new ArrayList<>();
        if (map.get("rewards") instanceof List<?> list) {
            for (Object entry : list) {
                rewards.add(String.valueOf(entry));
            }
        }

        String itemName = String.valueOf(map.containsKey("gui-item") ? map.get("gui-item") : "CHEST");
        Material material = Material.matchMaterial(itemName.toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("bad gui-item '" + itemName + "'");
        }

        String guiName = String.valueOf(map.containsKey("gui-name") ? map.get("gui-name") : "<white>Milestone");
        List<String> lore = new ArrayList<>();
        if (map.get("gui-lore") instanceof List<?> loreList) {
            for (Object entry : loreList) {
                lore.add(String.valueOf(entry));
            }
        }

        return new Milestone(index, seconds, List.copyOf(rewards), material, guiName, List.copyOf(lore));
    }

    public int index() {
        return index;
    }

    public long seconds() {
        return seconds;
    }

    public List<String> rewards() {
        return rewards;
    }

    public Material guiItem() {
        return guiItem;
    }

    public String guiName() {
        return guiName;
    }

    public List<String> guiLore() {
        return guiLore;
    }
}
