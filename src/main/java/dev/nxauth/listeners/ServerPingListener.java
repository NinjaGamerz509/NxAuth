package dev.nxauth.listeners;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.JoinScreenManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.entity.Player;

public class ServerPingListener implements Listener {
    private final NxAuth plugin;
    public ServerPingListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        boolean maintenance = plugin.getMaintenanceManager().isEnabled();
        String motd = plugin.getMotdManager().getMotd(maintenance);
        if (motd != null) event.setMotd(motd);
    }
}
