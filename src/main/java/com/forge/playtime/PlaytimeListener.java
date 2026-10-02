package com.forge.playtime;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Tracks joins and guarantees persistence on leave. */
public final class PlaytimeListener implements Listener {

    private final ForgePlaytime plugin;

    public PlaytimeListener(ForgePlaytime plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.store().touch(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.store().save();
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        plugin.store().save();
    }
}
