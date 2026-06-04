package dev.nxauth.listeners;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.JoinScreenManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.entity.Player;

public class PlayerMoveListener implements Listener {
    private final NxAuth plugin;
    public PlayerMoveListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        plugin.getSessionManager().updateMovement(player);

        if (!plugin.getAuthManager().isLoggedIn(player)) {
            JoinScreenManager screen = new JoinScreenManager(plugin);
            if (screen.isFrozen(player)) {
                if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
