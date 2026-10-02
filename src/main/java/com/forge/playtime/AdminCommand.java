package com.forge.playtime;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /fplaytime reload - admin commands. */
public final class AdminCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final ForgePlaytime plugin;

    public AdminCommand(ForgePlaytime plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("forgeplaytime.admin")) {
            sender.sendMessage(MM.deserialize("<red>You don't have permission to do that."));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.loadMilestones();
            sender.sendMessage(MM.deserialize(
                    "<green>ForgePlaytime reloaded with <white>" + plugin.milestones().size() + "<green> milestone(s)."));
            return true;
        }
        sender.sendMessage(MM.deserialize("<gray>Usage: <white>/fplaytime reload"));
        return true;
    }
}
