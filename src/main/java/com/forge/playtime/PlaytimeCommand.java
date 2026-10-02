package com.forge.playtime;

import java.util.UUID;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /playtime - opens the milestone GUI, or (admin) shows another player's stats. */
public final class PlaytimeCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final ForgePlaytime plugin;

    public PlaytimeCommand(ForgePlaytime plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MM.deserialize("<red>Console usage: <white>/playtime <player>"));
                return true;
            }
            if (!player.hasPermission("forgeplaytime.use")) {
                player.sendMessage(MM.deserialize("<red>You don't have permission to do that."));
                return true;
            }
            plugin.gui().open(player);
            return true;
        }

        if (!sender.hasPermission("forgeplaytime.admin")) {
            sender.sendMessage(MM.deserialize("<red>You don't have permission to do that."));
            return true;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        UUID id;
        String name;
        if (online != null) {
            id = online.getUniqueId();
            name = online.getName();
        } else {
            UUID stored = plugin.store().findByName(args[0]);
            if (stored == null) {
                sender.sendMessage(MM.deserialize("<red>No playtime data for player '<white>" + args[0] + "<red>'."));
                return true;
            }
            id = stored;
            name = plugin.store().getName(id);
        }
        PlaytimeStore store = plugin.store();
        sender.sendMessage(MM.deserialize("<aqua><bold>" + name + " <gray>playtime: <white>"
                + ForgePlaytime.formatDuration(store.getSeconds(id)) + " <gray>| milestones: <white>"
                + store.getClaimed(id).size() + "<gray>/<white>" + plugin.milestones().size()));
        return true;
    }
}
