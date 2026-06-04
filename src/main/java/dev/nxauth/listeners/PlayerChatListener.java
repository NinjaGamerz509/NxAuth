package dev.nxauth.listeners;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.JoinScreenManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.entity.Player;

public class PlayerChatListener implements Listener {
    private final NxAuth plugin;
    public PlayerChatListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getAuthManager().isLoggedIn(player)) {
            event.setCancelled(true);
        }
    }
}
