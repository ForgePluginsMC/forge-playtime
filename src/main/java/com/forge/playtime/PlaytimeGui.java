package com.forge.playtime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

/** The milestone chest GUI: view progress and click to claim rewards. */
public final class PlaytimeGui implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int[] ROW_STARTS = {10, 19, 28, 37};
    private static final int INFO_SLOT = 49;

    private final ForgePlaytime plugin;

    public PlaytimeGui(ForgePlaytime plugin) {
        this.plugin = plugin;
    }

    /** Tags inventories built by this GUI so clicks can be identified safely. */
    static final class Holder implements InventoryHolder {
        private final UUID viewer;
        private Inventory inventory;

        Holder(UUID viewer) {
            this.viewer = viewer;
        }

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        UUID viewer() {
            return viewer;
        }

        @Override
        public @Nullable Inventory getInventory() {
            // Nullable until setInventory() runs: the holder is created first
            // and the inventory is attached immediately after
            // Bukkit.createInventory() returns. (The Bukkit interface declares
            // @NotNull; this deferred population is the one known exception.)
            return inventory;
        }
    }

    public void open(Player player) {
        List<Milestone> milestones = plugin.milestones();
        Holder holder = new Holder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 54, MM.deserialize("<dark_aqua><bold>Playtime Milestones"));
        holder.setInventory(inv);

        ItemStack filler = named(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), Component.empty());
        for (int slot = 0; slot < 54; slot++) {
            inv.setItem(slot, filler);
        }

        long seconds = plugin.store().getSeconds(player.getUniqueId());
        for (Milestone milestone : milestones) {
            inv.setItem(slotFor(milestone.index()), milestoneItem(player, milestone, seconds));
        }
        inv.setItem(INFO_SLOT, infoItem(player));

        player.openInventory(inv);
    }

    private static int slotFor(int milestoneIndex) {
        return ROW_STARTS[milestoneIndex / ROW_STARTS.length] + (milestoneIndex % 7);
    }

    private ItemStack milestoneItem(Player player, Milestone milestone, long seconds) {
        PlaytimeStore store = plugin.store();
        UUID id = player.getUniqueId();
        boolean claimed = store.isClaimed(id, milestone.index());
        boolean unlocked = seconds >= milestone.seconds();

        String prefix = claimed ? "<green>✔ " : unlocked ? "<gold>▶ " : "<gray>🔒 ";
        Component name = MM.deserialize(prefix + milestone.guiName());

        List<Component> lore = new ArrayList<>();
        for (String line : milestone.guiLore()) {
            lore.add(MM.deserialize(line));
        }
        lore.add(Component.empty());
        if (claimed) {
            lore.add(MM.deserialize("<green>Already claimed!"));
        } else if (unlocked) {
            lore.add(MM.deserialize("<gold>Click to claim your rewards!"));
        } else {
            lore.add(MM.deserialize(
                    "<gray>Unlocks in <white>" + ForgePlaytime.formatDuration(milestone.seconds() - seconds)));
        }
        return named(new ItemStack(milestone.guiItem()), name, lore);
    }

    private ItemStack infoItem(Player player) {
        PlaytimeStore store = plugin.store();
        UUID id = player.getUniqueId();
        List<Component> lore = List.of(
                MM.deserialize("<gray>Total: <white>" + ForgePlaytime.formatDuration(store.getSeconds(id))),
                MM.deserialize("<gray>Claimed: <white>" + store.getClaimed(id).size()
                        + "<gray> / <white>" + plugin.milestones().size()));
        return named(new ItemStack(Material.PAPER), MM.deserialize("<aqua><bold>Your Playtime"), lore);
    }

    private static ItemStack named(ItemStack item, Component name) {
        return named(item, name, List.of());
    }

    private static ItemStack named(ItemStack item, Component name, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(holder.viewer())) {
            return;
        }
        // Only the top (GUI) inventory is interactive; the player's own is locked too.
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof Holder)) {
            return;
        }

        int slot = event.getSlot();
        if (slot == INFO_SLOT) {
            return;
        }
        for (Milestone milestone : plugin.milestones()) {
            if (slotFor(milestone.index()) == slot) {
                tryClaim(player, milestone);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private void tryClaim(Player player, Milestone milestone) {
        PlaytimeStore store = plugin.store();
        UUID id = player.getUniqueId();
        if (store.isClaimed(id, milestone.index())) {
            return;
        }
        if (store.getSeconds(id) < milestone.seconds()) {
            player.sendMessage(MM.deserialize("<red>That milestone is not unlocked yet!"));
            return;
        }
        store.setClaimed(id, milestone.index());
        store.save();
        for (String command : milestone.rewards()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
        player.sendMessage(MM.deserialize("<green><bold>Milestone claimed! <gray>Enjoy your rewards."));
        open(player); // refresh the GUI to show the new state
    }
}
