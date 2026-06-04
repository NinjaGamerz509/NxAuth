package dev.nxauth.listeners;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.JoinScreenManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.entity.Player;

public class PlayerQuitListener implements Listener {
    private final NxAuth plugin;
    public PlayerQuitListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getAuthManager().isLoggedIn(player)) {
            String ip = player.getAddress().getAddress().getHostAddress();
            plugin.getSessionManager().saveSession(
                player.getUniqueId().toString(), ip,
                plugin.getConfigManager().getIpSessionDuration());
        }
        new JoinScreenManager(plugin).onPlayerQuit(player);
        plugin.getAuthManager().removePlayer(player);
        plugin.getSessionManager().removePlayer(player);
        plugin.getBossBarManager().removeBossBar(player);
    }
}
